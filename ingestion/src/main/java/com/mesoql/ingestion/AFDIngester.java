package com.mesoql.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mesoql.MesoQLException;
import com.mesoql.ollama.OllamaClient;
import com.mesoql.search.OpenSearchService;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AFDIngester {

    private static final Logger log = LoggerFactory.getLogger(AFDIngester.class);
    private static final int BATCH_SIZE = 32;
    private static final int MAX_TOKENS = 512;
    private static final int OVERLAP_TOKENS = 64;

    private static final Map<String, String> OFFICE_REGION_MAP = Map.ofEntries(
        Map.entry("NWS Seattle WA", "Pacific Northwest"),
        Map.entry("NWS Portland OR", "Pacific Northwest"),
        Map.entry("NWS Boise ID", "Pacific Northwest"),
        Map.entry("NWS Los Angeles CA", "Southern California"),
        Map.entry("NWS San Francisco CA", "Northern California"),
        Map.entry("NWS Sacramento CA", "Northern California"),
        Map.entry("NWS Denver CO", "Central Rockies"),
        Map.entry("NWS Phoenix AZ", "Desert Southwest"),
        Map.entry("NWS Salt Lake City UT", "Great Basin"),
        Map.entry("NWS Houston TX", "Gulf Coast"),
        Map.entry("NWS Dallas TX", "Southern Plains"),
        Map.entry("NWS Norman OK", "Southern Plains"),
        Map.entry("NWS Wichita KS", "Central Plains"),
        Map.entry("NWS Chicago IL", "Great Lakes"),
        Map.entry("NWS Detroit MI", "Great Lakes"),
        Map.entry("NWS Minneapolis MN", "Upper Midwest"),
        Map.entry("NWS New York NY", "Northeast"),
        Map.entry("NWS Boston MA", "Northeast"),
        Map.entry("NWS Philadelphia PA", "Mid-Atlantic"),
        Map.entry("NWS Washington DC", "Mid-Atlantic"),
        Map.entry("NWS Atlanta GA", "Southeast"),
        Map.entry("NWS Miami FL", "Southeast"),
        Map.entry("NWS Jacksonville FL", "Southeast"),
        Map.entry("NWS Nashville TN", "Tennessee Valley"),
        Map.entry("NWS St. Louis MO", "Central"),
        Map.entry("NWS Anchorage AK", "Alaska"),
        Map.entry("NWS Honolulu HI", "Hawaii")
    );

    private final OpenSearchService searchService;
    private final OllamaClient ollamaClient;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public AFDIngester(OpenSearchService searchService, OllamaClient ollamaClient) {
        this.searchService = searchService;
        this.ollamaClient = ollamaClient;
    }

    public void ingest(String sinceDate) {
        try {
            searchService.createForecastDiscussionsIndex();

            final List<Map<String, Object>> docs = fetchAFDs();
            log.info("Fetched {} AFDs from NWS API", docs.size());

            final List<Map<String, Object>> chunks = new ArrayList<>();
            for (final Map<String, Object> doc : docs) {
                final String text = doc.getOrDefault("text", "").toString();
                final List<String> textChunks = chunk(text, MAX_TOKENS, OVERLAP_TOKENS);
                for (int i = 0; i < textChunks.size(); i++) {
                    final Map<String, Object> chunk = new LinkedHashMap<>(doc);
                    chunk.put("text", textChunks.get(i));
                    chunk.put("discussion_id", doc.get("discussion_id") + "_chunk_" + i);
                    chunks.add(chunk);
                }
            }

            final List<Map<String, Object>> toIndex = chunks.stream()
                .filter(c -> {
                    try {
                        return !searchService.documentExists("forecast_discussions", c.get("discussion_id").toString());
                    } catch (IOException ex) {
                        return true;
                    }
                })
                .toList();

            log.info("Indexing {} new chunks (skipping {} already indexed)",
                toIndex.size(), chunks.size() - toIndex.size());

            for (int i = 0; i < toIndex.size(); i += BATCH_SIZE) {
                final int end = Math.min(i + BATCH_SIZE, toIndex.size());
                final List<Map<String, Object>> batch = toIndex.subList(i, end);

                final List<String> texts = batch.stream()
                    .map(c -> c.getOrDefault("text", "").toString())
                    .toList();
                final List<float[]> vectors = ollamaClient.embedBatch(texts);

                final List<BulkOperation> ops = new ArrayList<>();
                for (int j = 0; j < batch.size(); j++) {
                    final Map<String, Object> doc = new HashMap<>(batch.get(j));
                    doc.put("text_vector", vectors.get(j));
                    final String docId = doc.get("discussion_id").toString();
                    ops.add(BulkOperation.of(b -> b
                        .index(idx -> idx.index("forecast_discussions").id(docId).document(doc))
                    ));
                }

                searchService.bulkIndex(ops);
                log.info("Indexed batch {}-{} of {}", i + 1, end, toIndex.size());
            }

            log.info("AFD ingestion complete: {} chunks indexed", toIndex.size());
        } catch (Exception e) {
            throw new MesoQLException("AFD ingestion failed", e);
        }
    }

    private List<Map<String, Object>> fetchAFDs() throws IOException, InterruptedException {
        final String url = "https://api.weather.gov/products?type=AFD&limit=500";
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "MesoQL/0.1.0")
            .header("Accept", "application/json")
            .build();

        final HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        final JsonNode root = mapper.readTree(response.body());
        final JsonNode features = root.path("@graph");

        final List<Map<String, Object>> docs = new ArrayList<>();
        for (final JsonNode feature : features) {
            final String productId = feature.path("id").asText();
            final String productText = feature.path("productText").asText("");
            final String issuanceTime = feature.path("issuanceTime").asText("");
            final String senderName = feature.path("senderName").asText("");

            if (productText.isBlank()) continue;

            final Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("discussion_id", productId);
            doc.put("office", senderName);
            doc.put("region", OFFICE_REGION_MAP.getOrDefault(senderName, "Other"));
            doc.put("issuance_time", issuanceTime);
            doc.put("season", deriveSeason(issuanceTime));
            doc.put("text", productText);
            docs.add(doc);
        }
        return docs;
    }

    static List<String> chunk(String text, int maxTokens, int overlapTokens) {
        final String[] words = text.split("\\s+");
        final int wordsPerChunk = (int) (maxTokens / 1.3);
        final int overlapWords = (int) (overlapTokens / 1.3);

        final List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < words.length) {
            final int end = Math.min(start + wordsPerChunk, words.length);
            chunks.add(String.join(" ", Arrays.copyOfRange(words, start, end)));
            if (end == words.length) break;
            start += wordsPerChunk - overlapWords;
        }
        return chunks;
    }

    static String deriveSeason(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) return "unknown";
        try {
            final int month = LocalDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME).getMonthValue();
            return switch (month) {
                case 3, 4, 5 -> "spring";
                case 6, 7, 8 -> "summer";
                case 9, 10, 11 -> "fall";
                default -> "winter";
            };
        } catch (Exception e) {
            return "unknown";
        }
    }
}

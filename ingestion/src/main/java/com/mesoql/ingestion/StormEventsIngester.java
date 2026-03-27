package com.mesoql.ingestion;

import com.mesoql.MesoQLException;
import com.mesoql.ollama.OllamaClient;
import com.mesoql.search.OpenSearchService;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Ingests NOAA storm event CSV files into the OpenSearch storm_events index.
 */
@Service
public class StormEventsIngester {

    private static final Logger log = LoggerFactory.getLogger(StormEventsIngester.class);
    private static final int BATCH_SIZE = 32;
    private static final Map<String, Long> DAMAGE_MULTIPLIERS = Map.of(
        "K", 1_000L,
        "M", 1_000_000L,
        "B", 1_000_000_000L
    );
    private static final DateTimeFormatter NOAA_DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd-MMM-yy HH:mm:ss", Locale.US);

    private final OpenSearchService searchService;
    private final OllamaClient ollamaClient;

    /**
     * Constructs a StormEventsIngester with the given search service and Ollama client.
     */
    public StormEventsIngester(OpenSearchService searchService, OllamaClient ollamaClient) {
        this.searchService = searchService;
        this.ollamaClient = ollamaClient;
    }

    /**
     * Reads the given CSV file and indexes all new storm events into OpenSearch.
     */
    public void ingest(Path csvFile) {
        try {
            searchService.createStormEventsIndex();
            final List<Map<String, Object>> events = parseCsv(csvFile);
            log.info("Parsed {} events from {}", events.size(), csvFile);

            final List<Map<String, Object>> toIndex = events.stream()
                .filter(e -> {
                    try {
                        return !searchService.documentExists("storm_events", e.get("event_id").toString());
                    } catch (IOException ex) {
                        return true;
                    }
                })
                .toList();

            log.info("Indexing {} new events (skipping {} already indexed)",
                toIndex.size(), events.size() - toIndex.size());

            for (int i = 0; i < toIndex.size(); i += BATCH_SIZE) {
                final int end = Math.min(i + BATCH_SIZE, toIndex.size());
                final List<Map<String, Object>> batch = toIndex.subList(i, end);

                final List<String> narratives = batch.stream()
                    .map(e -> e.getOrDefault("narrative", "").toString())
                    .toList();
                final List<float[]> vectors = ollamaClient.embedBatch(narratives);

                final List<BulkOperation> ops = new ArrayList<>();
                for (int j = 0; j < batch.size(); j++) {
                    final Map<String, Object> doc = new HashMap<>(batch.get(j));
                    doc.put("narrative_vector", vectors.get(j));
                    final String eventId = doc.get("event_id").toString();
                    ops.add(BulkOperation.of(b -> b
                        .index(idx -> idx.index("storm_events").id(eventId).document(doc))
                    ));
                }

                searchService.bulkIndex(ops);
                log.info("Indexed batch {}-{} of {}", i + 1, end, toIndex.size());
            }

            log.info("Ingestion complete: {} events indexed", toIndex.size());
        } catch (IOException | CsvValidationException e) {
            throw new MesoQLException("Storm events ingestion failed", e);
        }
    }

    List<Map<String, Object>> parseCsv(Path csvFile) throws IOException, CsvValidationException {
        final List<Map<String, Object>> events = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(csvFile.toFile(), StandardCharsets.UTF_8))
                .withCSVParser(new CSVParserBuilder().withSeparator(',').build())
                .build()) {

            final String[] headers = reader.readNext();
            final Map<String, Integer> colIndex = IntStream.range(0, headers.length)
                .boxed()
                .collect(Collectors.toMap(i -> headers[i].trim(), i -> i));

            String[] row;
            while ((row = reader.readNext()) != null) {
                final String eventId = getCol(row, colIndex, "EVENT_ID");
                if (eventId == null || eventId.isBlank()) continue;

                final String episodeNarrative = getCol(row, colIndex, "EPISODE_NARRATIVE");
                final String eventNarrative = getCol(row, colIndex, "EVENT_NARRATIVE");
                if (eventNarrative == null || eventNarrative.isBlank()) continue;

                final String narrative = episodeNarrative != null && !episodeNarrative.isBlank()
                        ? episodeNarrative.trim() + " " + eventNarrative.trim()
                        : eventNarrative.trim();

                final Map<String, Object> event = new LinkedHashMap<>();
                event.put("event_id", eventId);
                event.put("state", getCol(row, colIndex, "STATE"));
                event.put("event_type", getCol(row, colIndex, "EVENT_TYPE"));
                event.put("narrative", narrative);

                final String beginDate = getCol(row, colIndex, "BEGIN_DATE_TIME");
                if (beginDate != null && !beginDate.isBlank()) {
                    try {
                        final LocalDateTime dt = LocalDateTime.parse(beginDate, NOAA_DATE_FORMAT);
                        event.put("begin_date", dt.toString());
                        event.put("year", dt.getYear());
                    } catch (DateTimeParseException ignored) {
                        // skip date if unparseable
                    }
                }

                final String deathsDirect = getCol(row, colIndex, "DEATHS_DIRECT");
                final String deathsIndirect = getCol(row, colIndex, "DEATHS_INDIRECT");
                final int fatalities = parseIntOrZero(deathsDirect) + parseIntOrZero(deathsIndirect);
                event.put("fatalities", fatalities);

                final String damageStr = getCol(row, colIndex, "DAMAGE_PROPERTY");
                event.put("damage_property", parseDamage(damageStr));

                events.add(event);
            }
        }
        return events;
    }

    static long parseDamage(String s) {
        if (s == null || s.isBlank()) return 0;
        final String trimmed = s.trim();
        for (final Map.Entry<String, Long> entry : DAMAGE_MULTIPLIERS.entrySet()) {
            if (trimmed.toUpperCase().endsWith(entry.getKey())) {
                final String numPart = trimmed.substring(0, trimmed.length() - entry.getKey().length());
                try {
                    return (long) (Double.parseDouble(numPart) * entry.getValue());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        try {
            return (long) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseIntOrZero(String s) {
        if (s == null || s.isBlank()) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String getCol(String[] row, Map<String, Integer> colIndex, String colName) {
        final Integer idx = colIndex.get(colName);
        if (idx == null || idx >= row.length) return null;
        return row[idx];
    }
}

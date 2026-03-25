package com.mesoql.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mesoql.MesoQLException;
import com.mesoql.config.MesoQLConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    private static final int BATCH_SIZE = 32;
    private static final long BATCH_SLEEP_MS = 100;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String embedModel;
    private final String generateModel;

    public OllamaClient(MesoQLConfig config) {
        this.baseUrl = config.getOllamaBaseUrl();
        this.embedModel = config.getEmbedModel();
        this.generateModel = config.getGenerateModel();
    }

    public float[] embed(String text) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                "model", embedModel,
                "prompt", text
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/embeddings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            JsonNode embeddingNode = root.get("embedding");

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            return vector;
        } catch (IOException | InterruptedException e) {
            throw new MesoQLException("Embedding failed", e);
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            for (int j = i; j < end; j++) {
                results.add(embed(texts.get(j)));
            }
            if (end < texts.size()) {
                try {
                    Thread.sleep(BATCH_SLEEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new MesoQLException("Batch embedding interrupted", e);
                }
            }
            log.info("Embedded {}/{} documents", end, texts.size());
        }
        return results;
    }

    public String generate(String prompt) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                "model", generateModel,
                "prompt", prompt,
                "stream", false
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            return root.get("response").asText();
        } catch (IOException | InterruptedException e) {
            throw new MesoQLException("Generation failed", e);
        }
    }
}

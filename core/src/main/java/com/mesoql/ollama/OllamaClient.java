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

/**
 * HTTP client for the Ollama API, providing embedding and text-generation operations.
 */
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

    /**
     * Constructs the client using connection and model settings from the given config.
     */
    public OllamaClient(MesoQLConfig config) {
        this.baseUrl = config.getOllamaBaseUrl();
        this.embedModel = config.getEmbedModel();
        this.generateModel = config.getGenerateModel();
    }

    /**
     * Returns the embedding vector for the given text using the configured embed model.
     */
    public float[] embed(String text) {
        try {
            final String body = mapper.writeValueAsString(Map.of(
                "model", embedModel,
                "prompt", text
            ));

            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/embeddings"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            final HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            final JsonNode root = mapper.readTree(response.body());
            final JsonNode embeddingNode = root.get("embedding");

            final float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            return vector;
        } catch (IOException | InterruptedException e) {
            throw new MesoQLException("Embedding failed", e);
        }
    }

    /**
     * Embeds a list of texts in rate-limited batches and returns all resulting vectors.
     */
    public List<float[]> embedBatch(List<String> texts) {
        final List<float[]> results = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            final int end = Math.min(i + BATCH_SIZE, texts.size());
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

    /**
     * Generates a text response from the configured generation model for the given prompt.
     */
    public String generate(String prompt) {
        try {
            final String body = mapper.writeValueAsString(Map.of(
                "model", generateModel,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                    "temperature", 0,
                    "seed", 1
                )
            ));

            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            final HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            final JsonNode root = mapper.readTree(response.body());
            return root.get("response").asText();
        } catch (IOException | InterruptedException e) {
            throw new MesoQLException("Generation failed", e);
        }
    }
}

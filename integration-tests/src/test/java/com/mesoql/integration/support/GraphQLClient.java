package com.mesoql.integration.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin HTTP wrapper for posting GraphQL documents to the server.
 */
public final class GraphQLClient {

    private static final String CONTENT_TYPE = "application/json";

    private final HttpClient httpClient;
    private final String endpoint;

    /**
     * Constructs a client targeting the default GraphQL endpoint.
     */
    public GraphQLClient() {
        this(IntegrationEnvironment.graphqlEndpoint());
    }

    /**
     * Constructs a client targeting the given endpoint URL.
     */
    public GraphQLClient(String endpoint) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.endpoint = endpoint;
    }

    /**
     * Sends a GraphQL document as JSON and returns the raw response body.
     *
     * @param query the GraphQL query document (not wrapped in JSON)
     * @return raw JSON response body string
     */
    public String execute(String query) throws IOException, InterruptedException {
        final String body = buildRequestBody(query);
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", CONTENT_TYPE)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * Returns true if the response body contains a top-level {@code "errors"} key.
     */
    public boolean hasErrors(String responseBody) {
        return responseBody.contains("\"errors\"");
    }

    /**
     * Returns true if the response body contains a non-empty {@code "hits"} array.
     */
    public boolean hasHits(String responseBody) {
        final int hitsIdx = responseBody.indexOf("\"hits\"");
        if (hitsIdx < 0) {
            return false;
        }
        final int bracketIdx = responseBody.indexOf('[', hitsIdx);
        if (bracketIdx < 0) {
            return false;
        }
        final int closingIdx = responseBody.indexOf(']', bracketIdx);
        if (closingIdx < 0) {
            return false;
        }
        final String hitsContent = responseBody.substring(bracketIdx + 1, closingIdx).trim();
        return !hitsContent.isEmpty();
    }

    private static String buildRequestBody(String query) {
        final String escaped = query
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
        return "{\"query\":\"" + escaped + "\"}";
    }
}

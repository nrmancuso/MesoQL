package com.mesoql.integration.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Thin HTTP wrapper for posting GraphQL documents and multipart uploads to the server.
 */
public final class GraphQLClient {

    /**
     * CRLF line terminator required by RFC 2046 for multipart boundaries.
     */
    private static final String CRLF = "\r\n";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String MULTIPART_BOUNDARY = "MesoQLTestBoundary";

    private final HttpClient httpClient;
    private final String endpoint;

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
            .header("Content-Type", JSON_CONTENT_TYPE)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * Uploads a file to the given admin index endpoint as multipart/form-data.
     *
     * @param url  the full URL to POST to (e.g. {@code /admin/index/storm-events})
     * @param file path to the file to upload
     * @return raw response body string
     */
    public String uploadFile(String url, Path file) throws IOException, InterruptedException {
        final String fileContent = Files.readString(file);
        final Path fileNamePath = file.getFileName();
        if (fileNamePath == null) {
            throw new IllegalArgumentException("File path has no filename component: " + file);
        }
        final String filename = fileNamePath.toString();
        final String multipartBody = buildMultipartBody(MULTIPART_BOUNDARY, filename, fileContent);

        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY)
            .POST(HttpRequest.BodyPublishers.ofString(multipartBody))
            .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private static String buildRequestBody(String query) {
        final String escaped = query
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\t", "\\t")
            .replace("\r\n", "\\n")
            .replace("\r", "\\n")
            .replace("\n", "\\n");
        return "{\"query\":\"" + escaped + "\"}";
    }

    private static String buildMultipartBody(String boundary, String filename, String fileContent) {
        return "--" + boundary + CRLF
            + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + CRLF
            + "Content-Type: text/csv" + CRLF
            + CRLF
            + fileContent + CRLF
            + "--" + boundary + "--" + CRLF;
    }
}

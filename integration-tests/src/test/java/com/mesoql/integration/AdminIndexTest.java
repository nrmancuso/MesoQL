package com.mesoql.integration;

import com.mesoql.integration.support.AppServerExtension;
import com.mesoql.integration.support.IntegrationEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the admin ingestion job lifecycle endpoints.
 */
@ExtendWith(AppServerExtension.class)
class AdminIndexTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Duration INGEST_TIMEOUT = Duration.ofMinutes(3);
    private static final long POLL_INTERVAL_MS = 2000L;

    @Test
    void testIndexJobLifecycle() throws IOException, InterruptedException {
        final Path fixture = IntegrationEnvironment.repoRoot()
            .resolve("integration-tests/fixtures/storm-events.csv");
        final String fixtureContent = Files.readString(fixture);

        final String boundary = "AdminIndexTestBoundary";
        final String multipartBody = "--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=\"file\"; filename=\"storm-events.csv\"\r\n" +
            "Content-Type: text/csv\r\n" +
            "\r\n" + fixtureContent + "\r\n" +
            "--" + boundary + "--\r\n";

        final HttpRequest postRequest = HttpRequest.newBuilder()
            .uri(URI.create(IntegrationEnvironment.adminIndexEndpoint() + "/storm-events"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofString(multipartBody))
            .build();

        final HttpResponse<String> postResponse = HTTP.send(postRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(202, postResponse.statusCode(),
            "Expected 202 Accepted but got " + postResponse.statusCode()
                + ": " + postResponse.body());

        final String postBody = postResponse.body();
        assertTrue(postBody.contains("\"jobId\""),
            "Response should contain jobId: " + postBody);
        assertTrue(postBody.contains("\"RUNNING\"") || postBody.contains("\"status\""),
            "Response should contain status: " + postBody);

        final String jobId = extractJobId(postBody);
        assertNotNull(jobId, "Job ID should not be null");

        final String finalStatus = pollUntilTerminal(jobId);

        assertTrue(finalStatus.contains("\"DONE\""),
            "Expected DONE status but got: " + finalStatus);
    }

    @Test
    void testUnknownJobId() throws IOException, InterruptedException {
        final String unknownId = "00000000-0000-0000-0000-000000000000";
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(IntegrationEnvironment.adminIndexEndpoint() + "/" + unknownId))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(),
            "Expected 404 for unknown job ID but got " + response.statusCode());
    }

    private static String pollUntilTerminal(String jobId) throws IOException, InterruptedException {
        final Instant deadline = Instant.now().plus(INGEST_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(IntegrationEnvironment.adminIndexEndpoint() + "/" + jobId))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            final HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            final String body = resp.body();

            if (body.contains("\"DONE\"") || body.contains("\"FAILED\"")) {
                return body;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new IllegalStateException("Job " + jobId + " did not reach terminal state within timeout");
    }

    private static String extractJobId(String responseBody) {
        final int idx = responseBody.indexOf("\"jobId\"");
        if (idx < 0) {
            throw new IllegalStateException("No jobId in response: " + responseBody);
        }
        final int quoteStart = responseBody.indexOf('"', idx + 8);
        if (quoteStart < 0) {
            throw new IllegalStateException("Malformed jobId in response: " + responseBody);
        }
        final int quoteEnd = responseBody.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) {
            throw new IllegalStateException("Malformed jobId in response: " + responseBody);
        }
        return responseBody.substring(quoteStart + 1, quoteEnd);
    }
}

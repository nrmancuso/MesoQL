package com.mesoql.integration;

import com.mesoql.integration.support.AppServerExtension;
import com.mesoql.integration.support.GraphQLClient;
import com.mesoql.integration.support.IntegrationEnvironment;
import com.mesoql.integration.support.TestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the admin ingestion job lifecycle endpoints.
 */
@ExtendWith(AppServerExtension.class)
class AdminIndexTest {

    private static final GraphQLClient CLIENT = new GraphQLClient();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Test
    @DisplayName("Index job lifecycle: submit, poll, and verify completion")
    void testIndexJobLifecycle() throws IOException, InterruptedException {
        final Path fixture = IntegrationEnvironment.repoRoot()
            .resolve("integration-tests/fixtures/storm-events.csv");

        final String postBody = CLIENT.uploadFile(
            IntegrationEnvironment.adminIndexEndpoint() + "/storm-events", fixture);

        assertTrue(postBody.contains("\"jobId\""),
            "Response should contain jobId: " + postBody);
        assertTrue(postBody.contains("\"status\""),
            "Response should contain status field: " + postBody);
        assertTrue(postBody.contains("\"data\"") || postBody.contains("RUNNING"),
            "Response should show valid status: " + postBody);

        final String jobId = GraphQLClient.extractJobId(postBody);
        assertNotNull(jobId, "Job ID should not be null");

        final String finalStatus = TestHelper.pollUntilTerminal(jobId);
        assertTrue(finalStatus.contains("\"DONE\""),
            "Expected DONE status but got: " + finalStatus);
        assertTrue(finalStatus.contains("\"jobId\""),
            "Final response should contain jobId: " + finalStatus);
    }

    @Test
    @DisplayName("Query unknown job ID returns 404 with proper response structure")
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
        final String body = response.body();
        assertTrue(body != null && !body.isEmpty(),
            "Response body should not be empty: " + body);
    }
}

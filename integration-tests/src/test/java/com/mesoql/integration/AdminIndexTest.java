package com.mesoql.integration;

import com.mesoql.MesoQLApplication;
import com.mesoql.integration.support.GraphQLClient;
import com.mesoql.integration.support.IntegrationEnvironment;
import com.mesoql.integration.support.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

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
@SpringBootTest(
    classes = MesoQLApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class AdminIndexTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    private GraphQLClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        this.baseUrl = "http://localhost:" + port;
        this.client = new GraphQLClient(baseUrl + "/graphql");
        TestHelper.ensureDataSeeded(baseUrl);
    }

    @Test
    @DisplayName("Index job lifecycle: submit, poll, and verify completion")
    void testIndexJobLifecycle() throws IOException, InterruptedException {
        final Path fixture = IntegrationEnvironment.repoRoot()
            .resolve("integration-tests/fixtures/storm-events.csv");

        final String postBody = client.uploadFile(baseUrl + "/admin/index/storm-events", fixture);

        assertTrue(postBody.contains("\"jobId\""),
            "Response should contain jobId: " + postBody);
        assertTrue(postBody.contains("\"status\""),
            "Response should contain status field: " + postBody);
        assertTrue(postBody.contains("RUNNING"),
            "Response should show RUNNING status: " + postBody);

        final String jobId = GraphQLClient.extractJobId(postBody);
        assertNotNull(jobId, "Job ID should not be null");

        final String finalStatus = TestHelper.pollUntilTerminal(baseUrl, jobId);
        assertTrue(finalStatus.contains("\"DONE\""),
            "Expected DONE status but got: " + finalStatus);
        assertTrue(finalStatus.contains("\"jobId\""),
            "Final response should contain jobId: " + finalStatus);
    }

    @Test
    @DisplayName("Query unknown job ID returns 404")
    void testUnknownJobId() throws IOException, InterruptedException {
        final String unknownId = "00000000-0000-0000-0000-000000000000";
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/admin/index/" + unknownId))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(),
            "Expected 404 for unknown job ID but got " + response.statusCode());
    }
}

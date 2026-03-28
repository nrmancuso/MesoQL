package com.mesoql.integration;

import com.mesoql.integration.support.AppServerExtension;
import com.mesoql.integration.support.GraphQLClient;
import com.mesoql.integration.support.IntegrationEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the GraphQL search endpoint against both data sources.
 * Requires a running stack (OpenSearch, Ollama, and the app server).
 */
@ExtendWith(AppServerExtension.class)
@TestMethodOrder(OrderAnnotation.class)
class GraphQLSearchSmokeTest {

    private static final GraphQLClient CLIENT = new GraphQLClient();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Duration INGEST_TIMEOUT = Duration.ofMinutes(3);
    private static final long POLL_INTERVAL_MS = 2000L;

    @BeforeAll
    static void seedData() throws IOException, InterruptedException {
        indexStormEvents();
        indexForecastDiscussions();
    }

    @Test
    @Order(1)
    void testSearchStormEvents() throws IOException, InterruptedException {
        final String query = """
            {
              search(source: STORM_EVENTS, input: { semantic: "tornado", limit: 2 }) {
                hits { ... on StormEventHit { eventId narrative } }
              }
            }
            """;

        final String response = CLIENT.execute(query);

        assertFalse(CLIENT.hasErrors(response),
            "Expected no errors but got: " + response);
        assertTrue(CLIENT.hasHits(response),
            "Expected at least one hit but got: " + response);
    }

    @Test
    @Order(2)
    void testSearchForecastDiscussions() throws IOException, InterruptedException {
        final String query = """
            {
              search(source: FORECAST_DISCUSSIONS, input: { semantic: "atmospheric river", limit: 2 }) {
                hits { ... on ForecastDiscussionHit { discussionId text } }
              }
            }
            """;

        final String response = CLIENT.execute(query);

        assertFalse(CLIENT.hasErrors(response),
            "Expected no errors but got: " + response);
        assertTrue(CLIENT.hasHits(response),
            "Expected at least one hit but got: " + response);
    }

    private static void indexStormEvents() throws IOException, InterruptedException {
        final Path fixture = IntegrationEnvironment.repoRoot()
            .resolve("integration-tests/fixtures/storm-events.csv");
        final String responseBody = CLIENT.uploadFile(
            IntegrationEnvironment.adminIndexEndpoint() + "/storm-events", fixture);

        pollUntilDone(GraphQLClient.extractJobId(responseBody));
    }

    private static void indexForecastDiscussions() throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(IntegrationEnvironment.adminIndexEndpoint() + "/forecast-discussions"))
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 202) {
            throw new IllegalStateException(
                "Expected 202 from forecast-discussions ingest but got " + response.statusCode()
                    + ": " + response.body());
        }

        pollUntilDone(GraphQLClient.extractJobId(response.body()));
    }

    private static void pollUntilDone(String jobId) throws IOException, InterruptedException {
        final Instant deadline = Instant.now().plus(INGEST_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(IntegrationEnvironment.adminIndexEndpoint() + "/" + jobId))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            final HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            final String respBody = resp.body();

            if (respBody.contains("\"DONE\"")) {
                return;
            }
            if (respBody.contains("\"FAILED\"")) {
                throw new IllegalStateException("Ingestion job failed: " + respBody);
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new IllegalStateException("Ingestion job did not complete within timeout");
    }
}

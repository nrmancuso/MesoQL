package com.mesoql.integration.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Common test utilities for integration tests.
 * Data seeding is thread-safe and happens once per test execution.
 */
public final class TestHelper {

    private static final Duration INGEST_TIMEOUT = Duration.ofMinutes(3);
    private static final long POLL_INTERVAL_MS = 2000L;
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final AtomicBoolean DATA_SEED_INITIALIZED = new AtomicBoolean(false);
    private static final Object SEED_LOCK = new Object();

    private TestHelper() {
        throw new UnsupportedOperationException("TestHelper is a utility class");
    }

    /**
     * Seeds all test data (storm events and forecast discussions) once per JVM run.
     * Subsequent calls are no-ops. Thread-safe.
     *
     * @param baseUrl the server base URL (e.g. {@code http://localhost:8080})
     * @throws IOException if the file cannot be read or HTTP communication fails
     * @throws InterruptedException if ingestion is interrupted
     * @throws IllegalStateException if an ingestion job fails or times out
     */
    public static void ensureDataSeeded(String baseUrl) throws IOException, InterruptedException {
        if (!DATA_SEED_INITIALIZED.get()) {
            synchronized (SEED_LOCK) {
                if (!DATA_SEED_INITIALIZED.get()) {
                    seedData(baseUrl);
                    DATA_SEED_INITIALIZED.set(true);
                }
            }
        }
    }

    /**
     * Seeds all test data. Called once by {@link #ensureDataSeeded(String)}.
     */
    private static void seedData(String baseUrl) throws IOException, InterruptedException {
        final Path fixture = IntegrationEnvironment.repoRoot()
            .resolve("integration-tests/fixtures/storm-events.csv");
        indexStormEvents(baseUrl, fixture);
        indexForecastDiscussions(baseUrl);
    }

    /**
     * Uploads a CSV file to the storm events index endpoint and polls until ingestion is complete.
     *
     * @param baseUrl the server base URL
     * @param fixture path to the CSV file
     */
    public static void indexStormEvents(String baseUrl, Path fixture)
            throws IOException, InterruptedException {
        final GraphQLClient client = new GraphQLClient(baseUrl + "/graphql");
        final String responseBody = client.uploadFile(
            baseUrl + "/admin/index/storm-events", fixture);
        pollUntilDone(baseUrl, GraphQLClient.extractJobId(responseBody));
    }

    /**
     * Triggers forecast discussions ingestion from the NWS API and polls until complete.
     *
     * @param baseUrl the server base URL
     */
    public static void indexForecastDiscussions(String baseUrl)
            throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/admin/index/forecast-discussions"))
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 202) {
            throw new IllegalStateException(
                "Expected 202 from forecast-discussions ingest but got " + response.statusCode()
                    + ": " + response.body());
        }

        pollUntilDone(baseUrl, GraphQLClient.extractJobId(response.body()));
    }

    /**
     * Polls an ingestion job endpoint until it reaches a terminal state (DONE or FAILED).
     *
     * @param baseUrl the server base URL
     * @param jobId the UUID of the ingestion job
     * @return the final response body when the job reaches a terminal state
     */
    public static String pollUntilTerminal(String baseUrl, String jobId)
            throws IOException, InterruptedException {
        final Instant deadline = Instant.now().plus(INGEST_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/index/" + jobId))
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

    private static void pollUntilDone(String baseUrl, String jobId)
            throws IOException, InterruptedException {
        final Instant deadline = Instant.now().plus(INGEST_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/index/" + jobId))
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

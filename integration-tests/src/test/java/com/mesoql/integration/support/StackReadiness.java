package com.mesoql.integration.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Verifies that the external services required by integration tests are ready.
 */
public final class StackReadiness {

    private static final AtomicBoolean VERIFIED = new AtomicBoolean(false);
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String GRAPHQL_PROBE_BODY = "{\"query\":\"{__typename}\"}";

    private StackReadiness() {
    }

    /**
     * Blocks until OpenSearch, Ollama, and the MesoQL server are all ready.
     * Uses double-checked locking so only the first caller does the work.
     */
    public static void awaitReady() {
        if (VERIFIED.get()) {
            return;
        }
        synchronized (StackReadiness.class) {
            if (VERIFIED.get()) {
                return;
            }
            waitForHttpOk(IntegrationEnvironment.openSearchUrl() + "/_cluster/health",
                IntegrationEnvironment.startupTimeout());
            waitForHttpOk(IntegrationEnvironment.ollamaUrl() + "/api/tags",
                IntegrationEnvironment.startupTimeout());
            waitForGraphQL(IntegrationEnvironment.graphqlEndpoint(),
                IntegrationEnvironment.startupTimeout());
            VERIFIED.set(true);
        }
    }

    /**
     * Waits for the given URL to return a 2xx response within the timeout.
     */
    static void waitForHttpOk(String url, Duration timeout) {
        final Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (isReachable(url)) {
                return;
            }
            trySleep(1000L);
        }
        throw new IllegalStateException("Timed out waiting for " + url);
    }

    /**
     * Waits for the GraphQL endpoint to respond to a POST probe without a 5xx error.
     */
    static void waitForGraphQL(String url, Duration timeout) {
        final Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (isGraphQLReachable(url)) {
                return;
            }
            trySleep(1000L);
        }
        throw new IllegalStateException("Timed out waiting for GraphQL endpoint " + url);
    }

    private static boolean isReachable(String url) {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean isGraphQLReachable(String url) {
        try {
            final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GRAPHQL_PROBE_BODY))
                .build();
            final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() < 500;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void trySleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for service readiness", e);
        }
    }
}

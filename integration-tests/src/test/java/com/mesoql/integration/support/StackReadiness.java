package com.mesoql.integration.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Verifies that the external services required by integration tests are ready.
 */
public final class StackReadiness {

    private static final AtomicBoolean VERIFIED = new AtomicBoolean(false);
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private StackReadiness() {
    }

    /**
     * Blocks until the application jar, OpenSearch, and Ollama are all ready.
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
            if (!Files.exists(IntegrationEnvironment.jarPath())) {
                throw new IllegalStateException("MesoQL jar is missing");
            }
            waitForHttpOk(IntegrationEnvironment.openSearchUrl() + "/_cluster/health",
                IntegrationEnvironment.startupTimeout());
            waitForHttpOk(IntegrationEnvironment.ollamaUrl() + "/api/tags",
                IntegrationEnvironment.startupTimeout());
            VERIFIED.set(true);
        }
    }

    private static void waitForHttpOk(String url, Duration timeout) {
        final Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (isReachable(url)) {
                return;
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for " + url, e);
            }
        }
        throw new IllegalStateException("Timed out waiting for " + url);
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
}

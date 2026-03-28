package com.mesoql.integration.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Resolves paths and connection settings for the integration-test environment.
 */
public final class IntegrationEnvironment {

    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(120);
    private static final String DEFAULT_OPENSEARCH_URL = "http://localhost:9200";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    private static final String DEFAULT_SERVER_URL = "http://localhost:8080";
    private static final int DEFAULT_SERVER_PORT = 8080;

    private IntegrationEnvironment() {
    }

    /**
     * Returns the repository root path.
     */
    public static Path repoRoot() {
        final String configuredRoot = System.getProperty("mesoql.repo.root");
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            return Path.of(configuredRoot);
        }
        return Path.of("").toAbsolutePath().getParent();
    }

    /**
     * Returns the application JAR path.
     */
    public static Path jarPath() {
        final String configuredJar = System.getProperty("mesoql.jar.path");
        final Path jarPath = configuredJar != null && !configuredJar.isBlank()
            ? Path.of(configuredJar)
            : repoRoot().resolve("app/build/libs/mesoql-0.1.0.jar");

        if (!Files.exists(jarPath)) {
            throw new IllegalStateException("MesoQL jar not found: " + jarPath);
        }
        return jarPath;
    }

    /**
     * Returns the configured OpenSearch URL.
     */
    public static String openSearchUrl() {
        final String value = System.getenv("MESOQL_OPENSEARCH_URL");
        return value == null || value.isBlank() ? DEFAULT_OPENSEARCH_URL : value;
    }

    /**
     * Returns the configured Ollama URL.
     */
    public static String ollamaUrl() {
        final String value = System.getenv("MESOQL_OLLAMA_URL");
        return value == null || value.isBlank() ? DEFAULT_OLLAMA_URL : value;
    }

    /**
     * Returns the MesoQL server base URL.
     */
    public static String serverUrl() {
        final String value = System.getProperty("mesoql.server.url");
        return value == null || value.isBlank() ? DEFAULT_SERVER_URL : value;
    }

    /**
     * Returns the MesoQL server port.
     */
    public static int serverPort() {
        final String value = System.getProperty("mesoql.server.port");
        if (value == null || value.isBlank()) {
            return DEFAULT_SERVER_PORT;
        }
        return Integer.parseInt(value);
    }

    /**
     * Returns the GraphQL endpoint URL.
     */
    public static String graphqlEndpoint() {
        return serverUrl() + "/graphql";
    }

    /**
     * Returns the admin index endpoint base URL.
     */
    public static String adminIndexEndpoint() {
        return serverUrl() + "/admin/index";
    }

    /**
     * Returns the admin stats endpoint URL.
     */
    public static String adminStatsEndpoint() {
        return serverUrl() + "/admin/stats";
    }

    /**
     * Returns the timeout used while waiting for startup.
     */
    public static Duration startupTimeout() {
        return STARTUP_TIMEOUT;
    }

    /**
     * Returns the timeout used while waiting for command completion.
     */
    public static Duration commandTimeout() {
        return COMMAND_TIMEOUT;
    }

    /**
     * Returns the current Java executable path.
     */
    public static Path javaExecutable() {
        final String javaHome = Objects.requireNonNull(System.getProperty("java.home"));
        return Path.of(javaHome, "bin", "java");
    }
}

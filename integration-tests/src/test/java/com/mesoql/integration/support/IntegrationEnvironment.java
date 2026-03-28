package com.mesoql.integration.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Resolves paths and connection settings for the integration-test environment.
 */
public final class IntegrationEnvironment {

    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(120);
    private static final String DEFAULT_OPENSEARCH_URL = "http://localhost:9200";
    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

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
     * Returns the application JAR path used by shell-based integration tests.
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
     * Returns the timeout used while waiting for shell startup.
     */
    public static Duration startupTimeout() {
        return STARTUP_TIMEOUT;
    }

    /**
     * Returns the timeout used while waiting for shell command completion.
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

    /**
     * Returns the Expect script used to drive the shell through a PTY.
     */
    public static Path expectScriptPath() {
        final Path scriptPath = repoRoot().resolve("integration-tests/scripts/run-shell-session.expect");
        if (!Files.exists(scriptPath)) {
            throw new IllegalStateException("Expect script not found: " + scriptPath);
        }
        return scriptPath;
    }
}

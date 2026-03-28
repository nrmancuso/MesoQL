package com.mesoql.integration.support;

import java.nio.file.Path;

/**
 * Resolves paths for integration test resources.
 */
public final class IntegrationEnvironment {

    private IntegrationEnvironment() {
    }

    /**
     * Returns the repository root path, resolved from the {@code mesoql.repo.root} system
     * property or by walking up from the current working directory.
     */
    public static Path repoRoot() {
        final String configuredRoot = System.getProperty("mesoql.repo.root");
        if (configuredRoot != null && !configuredRoot.isBlank()) {
            return Path.of(configuredRoot);
        }
        return Path.of("").toAbsolutePath().getParent();
    }
}

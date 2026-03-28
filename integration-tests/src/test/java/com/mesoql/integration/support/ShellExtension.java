package com.mesoql.integration.support;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Provides a fresh shell session to each test method.
 */
public final class ShellExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(ShellExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        StackReadiness.awaitReady();

        final Path homeDirectory = Files.createTempDirectory("mesoql-shell-home-");
        final ShellSession session = new ShellSession(homeDirectory);

        final ExtensionContext.Store store = context.getStore(NAMESPACE);
        store.put("homeDirectory", homeDirectory);
        store.put("session", session);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        final ExtensionContext.Store store = context.getStore(NAMESPACE);
        final ShellSession session = store.remove("session", ShellSession.class);
        final Path homeDirectory = store.remove("homeDirectory", Path.class);

        if (session != null) {
            session.close();
        }
        if (homeDirectory != null) {
            deleteRecursively(homeDirectory);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.getParameter().getType().equals(ShellSession.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        final ShellSession session = extensionContext.getStore(NAMESPACE).get("session", ShellSession.class);
        if (session == null) {
            throw new IllegalStateException("Shell session was not initialized");
        }
        return session;
    }

    private static void deleteRecursively(Path path) throws IOException {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(current -> {
                    try {
                        Files.deleteIfExists(current);
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to delete " + current, e);
                    }
                });
        }
    }
}

package com.mesoql.integration.support;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JUnit 5 extension that starts the MesoQL application JAR as a subprocess before all
 * tests in a class and stops it afterwards.
 */
public final class AppServerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(AppServerExtension.class);

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final Process existing = getProcess(context);
        if (existing != null && existing.isAlive()) {
            return;
        }

        final Path jarPath = IntegrationEnvironment.jarPath();
        final Path javaExec = IntegrationEnvironment.javaExecutable();

        final List<String> command = List.of(
            javaExec.toString(),
            "-Dspring.main.banner-mode=off",
            "-Dmesoql.opensearch-url=" + IntegrationEnvironment.openSearchUrl(),
            "-Dmesoql.ollama-base-url=" + IntegrationEnvironment.ollamaUrl(),
            "-Dserver.port=" + IntegrationEnvironment.serverPort(),
            "-jar",
            jarPath.toString()
        );

        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        final Process process = builder.start();
        context.getStore(NAMESPACE).put("process", process);

        waitForServer(IntegrationEnvironment.startupTimeout());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        final Process process = getProcess(context);
        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                final boolean terminated = process.waitFor(15L, TimeUnit.SECONDS);
                if (!terminated) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
    }

    private Process getProcess(ExtensionContext context) {
        return context.getStore(NAMESPACE).get("process", Process.class);
    }

    private static void waitForServer(Duration timeout) {
        StackReadiness.waitForGraphQL(IntegrationEnvironment.graphqlEndpoint(), timeout);
    }

    /**
     * Returns whether the subprocess started by this extension is still running.
     * Useful for tests that need to verify the server is alive.
     */
    public static boolean isServerRunning(ExtensionContext context) {
        final ExtensionContext.Store store = context.getStore(
            ExtensionContext.Namespace.create(AppServerExtension.class));
        final Process process = store.get("process", Process.class);
        return process != null && process.isAlive();
    }
}

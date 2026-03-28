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
 * tests in a class and stops it afterwards. When multiple test classes run in parallel,
 * only the first one actually starts the server; others wait for it to be ready.
 */
public final class AppServerExtension implements BeforeAllCallback, AfterAllCallback {

    private static final ExtensionContext.Namespace NAMESPACE =
        ExtensionContext.Namespace.create(AppServerExtension.class);
    private static final Object SERVER_STARTUP_LOCK = new Object();
    private static volatile Process SHARED_PROCESS = null;
    private static volatile boolean SERVER_OWNER = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        synchronized (SERVER_STARTUP_LOCK) {
            // Check if server is already started by another test class
            if (SHARED_PROCESS != null && SHARED_PROCESS.isAlive()) {
                context.getStore(NAMESPACE).put("process", SHARED_PROCESS);
                context.getStore(NAMESPACE).put("isOwner", false);
                TestHelper.ensureDataSeeded();
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
            SHARED_PROCESS = process;
            context.getStore(NAMESPACE).put("process", process);
            context.getStore(NAMESPACE).put("isOwner", true);
            SERVER_OWNER = true;

            waitForServer(IntegrationEnvironment.startupTimeout());
            TestHelper.ensureDataSeeded();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        final Boolean isOwner = context.getStore(NAMESPACE).get("isOwner", Boolean.class);
        // Only the test class that started the server should destroy it
        if (isOwner != null && isOwner) {
            synchronized (SERVER_STARTUP_LOCK) {
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
                SHARED_PROCESS = null;
                SERVER_OWNER = false;
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

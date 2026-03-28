package com.mesoql.integration.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs shell interactions through an Expect-based PTY wrapper.
 */
public final class ShellSession implements AutoCloseable {

    private static final String PROMPT_MARKER = "__MESOQL_PROMPT_RETURNED__";
    private static final String TIMEOUT_MARKER = "__MESOQL_TIMEOUT__";
    private static final String EARLY_EXIT_MARKER = "__MESOQL_EARLY_EXIT__";
    private static final String ANSI_PATTERN = "\\u001B\\[[;?0-9]*[ -/]*[@-~]|\\u001B=";

    private final Path homeDirectory;

    ShellSession(Path homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    /**
     * Sends a single line to a fresh shell session and captures the transcript.
     */
    public ShellResult sendLine(String line) {
        return sendLine(line, IntegrationEnvironment.commandTimeout());
    }

    /**
     * Sends a single line using a custom timeout.
     */
    public ShellResult sendLine(String line, Duration timeout) {
        return sendLines(List.of(line), timeout);
    }

    /**
     * Sends multiple lines to the shell within the same session.
     */
    public ShellResult sendLines(List<String> lines, Duration timeout) {
        Path commandsFile = null;
        try {
            commandsFile = Files.createTempFile(homeDirectory, "mesoql-commands-", ".txt");
            Files.write(commandsFile, lines, StandardCharsets.UTF_8);

            final ProcessBuilder builder = new ProcessBuilder(
                "expect",
                IntegrationEnvironment.expectScriptPath().toString()
            );
            builder.directory(IntegrationEnvironment.repoRoot().toFile());
            builder.redirectErrorStream(true);
            builder.environment().put("HOME", homeDirectory.toString());
            builder.environment().put("TERM", "dumb");
            builder.environment().put("MESOQL_SHELL_HOME", homeDirectory.toString());
            builder.environment().put("MESOQL_SHELL_JAR", IntegrationEnvironment.jarPath().toString());
            builder.environment().put("MESOQL_SHELL_COMMANDS_FILE", commandsFile.toString());
            builder.environment().put("MESOQL_SHELL_TIMEOUT_SECONDS", Long.toString(timeout.toSeconds()));

            final Process process = builder.start();
            final boolean completed = process.waitFor(timeout.toSeconds() + 10L, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IllegalStateException("Timed out waiting for shell session to finish");
            }

            final String rawOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final String normalizedOutput = normalize(stripMarkers(rawOutput));
            if (rawOutput.contains(TIMEOUT_MARKER)) {
                throw new IllegalStateException("Shell command timed out. Recent output:\n" + normalizedOutput);
            }
            if (rawOutput.contains(EARLY_EXIT_MARKER)) {
                throw new IllegalStateException("Shell exited before reaching the initial prompt.\n" + normalizedOutput);
            }

            final boolean promptSeenAgain = rawOutput.contains(PROMPT_MARKER);
            return new ShellResult(rawOutput, normalizedOutput, promptSeenAgain, process.exitValue());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run shell session", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for shell session", e);
        } finally {
            if (commandsFile != null) {
                try {
                    Files.deleteIfExists(commandsFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void close() {
    }

    private static String stripMarkers(String rawOutput) {
        return rawOutput
            .replace(PROMPT_MARKER, "")
            .replace(TIMEOUT_MARKER, "")
            .replace(EARLY_EXIT_MARKER, "");
    }

    private static String normalize(String text) {
        return text
            .replace("\r", "")
            .replaceAll(ANSI_PATTERN, "")
            .stripTrailing();
    }
}

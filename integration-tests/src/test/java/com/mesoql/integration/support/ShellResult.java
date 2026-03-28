package com.mesoql.integration.support;

/**
 * Captures the observable result of a shell interaction.
 *
 * @param rawOutput raw terminal output including ANSI escape sequences
 * @param text normalized output suitable for assertions
 * @param promptSeenAgain whether the shell prompt returned after the command
 * @param exitCode process exit code when the shell exited, otherwise {@code null}
 */
public record ShellResult(String rawOutput, String text, boolean promptSeenAgain, Integer exitCode) {
}

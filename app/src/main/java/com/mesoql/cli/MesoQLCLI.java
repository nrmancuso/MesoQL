package com.mesoql.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

/**
 * Root Picocli command that exposes the MesoQL subcommands.
 */
@Command(
    name = "mesoql",
    mixinStandardHelpOptions = true,
    version = "MesoQL 0.1.0",
    description = "Semantic search over weather data.",
    subcommands = {
        QueryCommand.class,
        IndexCommand.class,
        ValidateCommand.class,
        StatsCommand.class,
        ShellCommand.class
    }
)
@Component
public class MesoQLCLI implements Runnable {

    private final ShellCommand shell;

    /**
     * Constructs the root CLI command.
     *
     * @param shell the interactive shell command
     */
    public MesoQLCLI(ShellCommand shell) {
        this.shell = shell;
    }

    /**
     * Runs the default shell command when no subcommand is provided.
     */
    @Override
    public void run() {
        try {
            shell.call();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

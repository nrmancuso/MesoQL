package com.mesoql.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

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

    public MesoQLCLI(ShellCommand shell) {
        this.shell = shell;
    }

    @Override
    public void run() {
        try {
            shell.call();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

package com.mesoql.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
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

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}

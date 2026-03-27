package com.mesoql;

import com.mesoql.cli.MesoQLCLI;
import com.mesoql.config.MesoQLConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import picocli.CommandLine;

/**
 * Spring Boot entry point for the MesoQL application.
 */
@SpringBootApplication
@EnableConfigurationProperties(MesoQLConfig.class)
public class MesoQLApplication implements CommandLineRunner, ExitCodeGenerator {

    private final MesoQLCLI cli;
    private final CommandLine.IFactory factory;
    private int exitCode;

    /**
     * Constructs the application with the root CLI command and picocli factory.
     *
     * @param cli the root CLI command
     * @param factory the picocli object factory
     */
    public MesoQLApplication(MesoQLCLI cli, CommandLine.IFactory factory) {
        this.cli = cli;
        this.factory = factory;
    }

    @Override
    public void run(String... args) {
        exitCode = new CommandLine(cli, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

}

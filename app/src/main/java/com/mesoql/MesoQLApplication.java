package com.mesoql;

import com.mesoql.cli.MesoQLCLI;
import com.mesoql.config.MesoQLConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import picocli.CommandLine;

@SpringBootApplication
@EnableConfigurationProperties(MesoQLConfig.class)
public class MesoQLApplication implements CommandLineRunner, ExitCodeGenerator {

    private final MesoQLCLI cli;
    private final CommandLine.IFactory factory;
    private int exitCode;

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

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(
            SpringApplication.run(MesoQLApplication.class, args)
        ));
    }
}

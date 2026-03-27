package com.mesoql;

import org.springframework.boot.SpringApplication;

/**
 * Application entry point for the MesoQL Spring Boot app.
 */
public final class Main {

    private Main() {
    }

    /**
     * Application entry point; delegates to Spring Boot and propagates the exit code.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        System.exit(SpringApplication.exit(
            SpringApplication.run(MesoQLApplication.class, args)
        ));
    }
}

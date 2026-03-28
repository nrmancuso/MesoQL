package com.mesoql;

import com.mesoql.config.MesoQLConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot entry point for the MesoQL HTTP service.
 */
@SpringBootApplication(scanBasePackages = "com.mesoql")
@EnableConfigurationProperties(MesoQLConfig.class)
public class MesoQLApplication {

    /**
     * Starts the MesoQL Spring Boot application.
     */
    public static void main(String[] args) {
        SpringApplication.run(MesoQLApplication.class, args);
    }
}

package com.mesoql.integration.support;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Serves fixture data in place of the live NWS API during integration tests.
 * Registered automatically via component scanning; picked up by all {@code @SpringBootTest} contexts.
 */
@RestController
class NwsMockController {

    /**
     * Returns the forecast discussion fixture as JSON, mimicking the NWS products endpoint.
     *
     * @return the fixture JSON with {@code Content-Type: application/json}
     * @throws IOException if the fixture file cannot be read
     */
    @GetMapping(value = "/nws/products", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> products() throws IOException {
        final String json = Files.readString(
            IntegrationEnvironment.repoRoot()
                .resolve("integration-tests/fixtures/forecast-discussion-products.json")
        );
        return ResponseEntity.ok(json);
    }
}

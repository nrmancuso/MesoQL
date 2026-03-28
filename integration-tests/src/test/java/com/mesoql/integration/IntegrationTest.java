package com.mesoql.integration;

import com.mesoql.MesoQLApplication;
import com.mesoql.integration.support.IntegrationEnvironment;
import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base class for all integration tests.
 *
 * <p>Starts a lightweight HTTP server before the Spring context is created to serve
 * NWS fixture data, then registers {@code mesoql.nws-api-url} via
 * {@link DynamicPropertySource} so {@code AFDIngester} hits the fixture instead of
 * the live NWS API.
 *
 * <p>All subclasses share the same Spring application context (same dynamic property
 * value, same {@link SpringBootTest} configuration).
 */
@SpringBootTest(
    classes = MesoQLApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
abstract class IntegrationTest {

    private static final int HTTP_OK = 200;
    private static final int NWS_MOCK_PORT;

    static {
        try {
            final Path fixture = IntegrationEnvironment.repoRoot()
                .resolve("integration-tests/fixtures/forecast-discussion-products.json");
            final byte[] body = Files.readAllBytes(fixture);
            final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/products", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(HTTP_OK, body.length);
                try (final OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            NWS_MOCK_PORT = server.getAddress().getPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Registers the mock NWS base URL before the Spring context is created.
     *
     * @param registry the dynamic property registry
     */
    @DynamicPropertySource
    static void configureNwsApiUrl(DynamicPropertyRegistry registry) {
        registry.add("mesoql.nws-api-url", () -> "http://localhost:" + NWS_MOCK_PORT);
    }
}

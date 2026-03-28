package com.mesoql.integration;

import com.mesoql.MesoQLApplication;
import com.mesoql.integration.support.GraphQLClient;
import com.mesoql.integration.support.GraphQLUtil;
import com.mesoql.integration.support.IntegrationEnvironment;
import com.mesoql.integration.support.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the admin ingestion job lifecycle endpoints.
 */
@SpringBootTest(
    classes = MesoQLApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class AdminIndexTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    private GraphQLClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        this.baseUrl = "http://localhost:" + port;
        this.client = new GraphQLClient(baseUrl + "/graphql");
        TestHelper.ensureDataSeeded(baseUrl);
    }

    @Test
    @DisplayName("Index job lifecycle: submit, poll, and verify completion")
    void testIndexJobLifecycle() throws Exception {
        final Path fixture = IntegrationEnvironment.repoRoot()
            .resolve("integration-tests/fixtures/storm-events.csv");

        final String postBody = client.uploadFile(baseUrl + "/admin/index/storm-events", fixture);

        JSONAssert.assertEquals("""
            {"status": "RUNNING"}
            """, postBody, JSONCompareMode.NON_EXTENSIBLE);

        final String finalStatus = TestHelper.pollUntilTerminal(baseUrl, GraphQLUtil.extractJobId(postBody));

        JSONAssert.assertEquals("""
            {"status": "DONE"}
            """, finalStatus, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("Query unknown job ID returns 404")
    void testUnknownJobId() throws IOException, InterruptedException {
        final String unknownId = "00000000-0000-0000-0000-000000000000";
        final HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/admin/index/" + unknownId))
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        final HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(),
            "Expected 404 for unknown job ID but got " + response.statusCode());
    }
}

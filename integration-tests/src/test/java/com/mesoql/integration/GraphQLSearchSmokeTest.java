package com.mesoql.integration;

import com.mesoql.MesoQLApplication;
import com.mesoql.integration.support.GraphQLClient;
import com.mesoql.integration.support.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the GraphQL search endpoint against both data sources.
 * Requires a running OpenSearch and Ollama stack.
 */
@SpringBootTest(
    classes = MesoQLApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class GraphQLSearchSmokeTest {

    @LocalServerPort
    private int port;

    private GraphQLClient client;

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        final String baseUrl = "http://localhost:" + port;
        this.client = new GraphQLClient(baseUrl + "/graphql");
        TestHelper.ensureDataSeeded(baseUrl);
    }

    @Test
    @DisplayName("Search storm events by semantic query and verify response structure")
    void testSearchStormEvents() throws IOException, InterruptedException {
        final String query = """
            {
              search(source: STORM_EVENTS, input: { semantic: "tornado", limit: 2 }) {
                hits { ... on StormEventHit { eventId narrative } }
              }
            }
            """;

        final String response = client.execute(query);

        assertFalse(client.hasErrors(response),
            "Expected no errors in response: " + response);
        assertTrue(client.hasHits(response),
            "Expected at least one hit in response: " + response);
        assertTrue(response.contains("\"data\""),
            "Response should contain data object: " + response);
        assertTrue(response.contains("\"search\""),
            "Response should contain search field: " + response);
    }

    @Test
    @DisplayName("Search forecast discussions by semantic query and verify response structure")
    void testSearchForecastDiscussions() throws IOException, InterruptedException {
        final String query = """
            {
              search(source: FORECAST_DISCUSSIONS, input: { semantic: "atmospheric river", limit: 2 }) {
                hits { ... on ForecastDiscussionHit { discussionId text } }
              }
            }
            """;

        final String response = client.execute(query);

        assertFalse(client.hasErrors(response),
            "Expected no errors in response: " + response);
        assertTrue(client.hasHits(response),
            "Expected at least one hit in response: " + response);
        assertTrue(response.contains("\"data\""),
            "Response should contain data object: " + response);
        assertTrue(response.contains("\"search\""),
            "Response should contain search field: " + response);
    }
}

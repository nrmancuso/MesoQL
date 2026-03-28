package com.mesoql.integration;

import com.mesoql.integration.support.AppServerExtension;
import com.mesoql.integration.support.GraphQLClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for the GraphQL search endpoint against both data sources.
 * Requires a running stack (OpenSearch, Ollama, and the app server).
 * Tests run in parallel after data ingestion.
 */
@ExtendWith(AppServerExtension.class)
class GraphQLSearchSmokeTest {

    private static final GraphQLClient CLIENT = new GraphQLClient();

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

        final String response = CLIENT.execute(query);

        assertFalse(CLIENT.hasErrors(response),
            "Expected no errors in response: " + response);
        assertTrue(CLIENT.hasHits(response),
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

        final String response = CLIENT.execute(query);

        assertFalse(CLIENT.hasErrors(response),
            "Expected no errors in response: " + response);
        assertTrue(CLIENT.hasHits(response),
            "Expected at least one hit in response: " + response);
        assertTrue(response.contains("\"data\""),
            "Response should contain data object: " + response);
        assertTrue(response.contains("\"search\""),
            "Response should contain search field: " + response);
    }
}

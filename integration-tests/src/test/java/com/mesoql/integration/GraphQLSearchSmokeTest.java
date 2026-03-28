package com.mesoql.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mesoql.integration.support.GraphQLClient;
import com.mesoql.integration.support.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke tests for the GraphQL search endpoint against both data sources.
 * Requires a running OpenSearch and Ollama stack.
 */
class GraphQLSearchSmokeTest extends IntegrationTest {

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
    void testSearchStormEvents() throws Exception {
        final String query = """
            {
              search(source: STORM_EVENTS, input: { semantic: "tornado", limit: 2 }) {
                hits { ... on StormEventHit { eventId narrative } }
              }
            }
            """;

        final String response = client.execute(query);
        final JsonNode root = new ObjectMapper().readTree(response);
        final JsonNode hits = root.at("/data/search/hits");

        assertNotNull(hits, "Expected data.search.hits to be present");
        assertEquals(2, hits.size(), "Expected exactly 2 hits for limit:2 query");

        for (final JsonNode hit : hits) {
            assertFalse(hit.path("eventId").isMissingNode(), "Each hit must have eventId");
            assertFalse(hit.path("narrative").isMissingNode(), "Each hit must have narrative");
        }
    }

    @Test
    @DisplayName("Search forecast discussions by semantic query and verify response structure")
    void testSearchForecastDiscussions() throws Exception {
        final String query = """
            {
              search(source: FORECAST_DISCUSSIONS, input: { semantic: "atmospheric river", limit: 2 }) {
                hits { ... on ForecastDiscussionHit { discussionId text } }
              }
            }
            """;

        final String response = client.execute(query);
        final String expected = """
            {
              "data": {
                "search": {
                  "hits": [
                    {
                      "discussionId": "afd-1_chunk_0",
                      "text": "An atmospheric river will bring prolonged precipitation and mountain snow to the Pacific Northwest through Wednesday. Strong onshore flow will maintain heavy rain along the coast and rising snow levels inland."
                    },
                    {
                      "discussionId": "afd-2_chunk_0",
                      "text": "A strong moisture plume supports atmospheric river precipitation with heavy rain and periods of mountain snow across northwest Oregon. Hydrologic concerns increase as successive waves reinforce the moist southwest flow."
                    }
                  ]
                }
              }
            }
            """;

        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }
}

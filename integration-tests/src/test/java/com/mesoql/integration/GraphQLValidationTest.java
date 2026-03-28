package com.mesoql.integration;

import com.mesoql.integration.support.GraphQLClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;

/**
 * Tests that the GraphQL API rejects invalid inputs with descriptive error messages.
 * Does not require seeded index data — only the Spring context needs to be running.
 */
class GraphQLValidationTest extends IntegrationTest {

    @LocalServerPort
    private int port;

    private GraphQLClient client;

    @BeforeEach
    void setUp() {
        this.client = new GraphQLClient("http://localhost:" + port + "/graphql");
    }

    @Test
    @DisplayName("Reject unknown field in filter with error response")
    void testUnknownField() throws Exception {
        final String query = """
            {
              search(source: STORM_EVENTS, input: {
                semantic: "tornado"
                filters: {
                  in: [{ field: "unknown_field", values: ["foo"] }]
                }
              }) {
                hits { ... on StormEventHit { eventId } }
              }
            }
            """;

        final String response = client.execute(query);
        final String expected = """
            {
              "data": {"search": null},
              "errors": [{"message": "Unknown field 'unknown_field' for source 'storm_events'"}]
            }
            """;

        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("Reject IN filter on numeric field with error response")
    void testInOnNumericField() throws Exception {
        final String query = """
            {
              search(source: STORM_EVENTS, input: {
                semantic: "tornado"
                filters: {
                  in: [{ field: "fatalities", values: ["3"] }]
                }
              }) {
                hits { ... on StormEventHit { eventId } }
              }
            }
            """;

        final String response = client.execute(query);
        final String expected = """
            {
              "data": {"search": null},
              "errors": [{"message": "IN filter only applies to keyword fields, but 'fatalities' is INTEGER"}]
            }
            """;

        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("Reject BETWEEN filter on keyword field with error response")
    void testBetweenOnKeywordField() throws Exception {
        final String query = """
            {
              search(source: STORM_EVENTS, input: {
                semantic: "tornado"
                filters: {
                  between: [{ field: "state", min: 1.0, max: 5.0 }]
                }
              }) {
                hits { ... on StormEventHit { eventId } }
              }
            }
            """;

        final String response = client.execute(query);
        final String expected = """
            {
              "data": {"search": null},
              "errors": [{"message": "BETWEEN filter does not apply to keyword fields, but 'state' is KEYWORD"}]
            }
            """;

        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("Reject mutually exclusive synthesize and clusterByTheme with error response")
    void testMutualExclusion() throws Exception {
        final String query = """
            {
              search(source: STORM_EVENTS, input: {
                semantic: "tornado"
                synthesize: "Summarize these events"
                clusterByTheme: true
              }) {
                hits { ... on StormEventHit { eventId } }
                synthesis
                clusters
              }
            }
            """;

        final String response = client.execute(query);
        final String expected = """
            {
              "data": {"search": null},
              "errors": [{"message": "synthesize and clusterByTheme cannot both be set"}]
            }
            """;

        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DisplayName("Reject unknown GraphQL source enum with schema error")
    void testUnknownSource() throws Exception {
        final String query = """
            {
              search(source: UNKNOWN_SOURCE, input: { semantic: "tornado" }) {
                hits { ... on StormEventHit { eventId } }
              }
            }
            """;

        final String response = client.execute(query);
        final String expected = """
            {
              "errors": [{"message": "Validation error (WrongType@[search]) : argument 'source' with value 'EnumValue{name='UNKNOWN_SOURCE'}' is not a valid 'Source' - Literal value not in allowable values for enum 'Source' - 'EnumValue{name='UNKNOWN_SOURCE'}'"}]
            }
            """;

        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
    }
}

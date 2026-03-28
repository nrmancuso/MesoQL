package com.mesoql.integration;

import com.mesoql.integration.support.AppServerExtension;
import com.mesoql.integration.support.GraphQLClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the GraphQL API rejects invalid inputs with descriptive error messages.
 * Does not require seeded index data — only the app server needs to be running.
 */
@ExtendWith(AppServerExtension.class)
class GraphQLValidationTest {

    private static final GraphQLClient CLIENT = new GraphQLClient();

    @Test
    void testUnknownField() throws IOException, InterruptedException {
        final String query =
            "{ search(source: STORM_EVENTS, input: {" +
            "  semantic: \"tornado\"," +
            "  filters: { in: [{ field: \"unknown_field\", values: [\"foo\"] }] }" +
            "}) { hits { ... on StormEventHit { eventId } } } }";

        final String response = CLIENT.execute(query);

        assertTrue(CLIENT.hasErrors(response),
            "Expected validation error for unknown field but got: " + response);
    }

    @Test
    void testInOnNumericField() throws IOException, InterruptedException {
        final String query =
            "{ search(source: STORM_EVENTS, input: {" +
            "  semantic: \"tornado\"," +
            "  filters: { in: [{ field: \"fatalities\", values: [\"3\"] }] }" +
            "}) { hits { ... on StormEventHit { eventId } } } }";

        final String response = CLIENT.execute(query);

        assertTrue(CLIENT.hasErrors(response),
            "Expected validation error for IN on numeric field but got: " + response);
    }

    @Test
    void testBetweenOnKeywordField() throws IOException, InterruptedException {
        final String query =
            "{ search(source: STORM_EVENTS, input: {" +
            "  semantic: \"tornado\"," +
            "  filters: { between: [{ field: \"state\", min: 1.0, max: 5.0 }] }" +
            "}) { hits { ... on StormEventHit { eventId } } } }";

        final String response = CLIENT.execute(query);

        assertTrue(CLIENT.hasErrors(response),
            "Expected validation error for BETWEEN on keyword field but got: " + response);
    }

    @Test
    void testMutualExclusion() throws IOException, InterruptedException {
        final String query =
            "{ search(source: STORM_EVENTS, input: {" +
            "  semantic: \"tornado\"," +
            "  synthesize: \"Summarize these events\"," +
            "  clusterByTheme: true" +
            "}) { hits { ... on StormEventHit { eventId } } synthesis clusters } }";

        final String response = CLIENT.execute(query);

        assertTrue(CLIENT.hasErrors(response),
            "Expected error for mutually exclusive synthesize + clusterByTheme but got: " + response);
    }

    @Test
    void testUnknownSource() throws IOException, InterruptedException {
        final String query =
            "{ search(source: UNKNOWN_SOURCE, input: { semantic: \"tornado\" }) {" +
            "  hits { ... on StormEventHit { eventId } }" +
            "} }";

        final String response = CLIENT.execute(query);

        assertTrue(CLIENT.hasErrors(response),
            "Expected GraphQL schema error for unknown source but got: " + response);
    }
}

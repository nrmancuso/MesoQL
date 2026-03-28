package com.mesoql.integration;

import com.mesoql.integration.support.GraphQLClient;
import com.mesoql.integration.support.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;

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
        final String expected = """
            {
              "data": {
                "search": {
                  "hits": [
                    {
                      "eventId": "1272001",
                      "narrative": "Strong to severe thunderstorms develop across northeast Colorado. Large hail was the main impact. A brief tornado form east of Denver over the open plains. No damage was reported with the tornado. Numerous photos and reports were passed along to the National Weather Service. Time and location estimated. Video of water flowing down the entire street from gutter to gutter."
                    },
                    {
                      "eventId": "1253359",
                      "narrative": "A regional outbreak of tornadoes occurred across portions|of western into south-central Kansas during the afternoon to|evening hours of Sunday 18 May 2025.  The first couple of tornadic|supercells occurred near Scott City KS in Scott County during the|late afternoon and progressed northward with both storms generating|EF2 tornadoes and a lone brief EFU tornado. A second tornadic|supercell developed near Dodge City KS in the early evening and|progressed northeast with an EFO tornado occurring near Kinsley KS|in Edwards County and an EF2 tornado near Garfield KS in Pawnee County.|A final long-duration tornadic supercell then formed in the mid|evening hours over Comanche County KS and commenced moving northeast|toward Greensburg KS in Kiowa County, Iuka KS in Pratt County,|north of Preston KS in Pratt County and finally across Stafford|and Reno Counties. This storm generated five EF3 tornadoes along with|several smaller satellite tornadoes.  The final count for this day|was 13 tornadoes just within the NWS Dodge City Warning area. Fairly short-lived tornado. Video of tornado from KAKE chaser Cameron Venable as it was crossing or just after crossing US-50. Just some minor tree damage and one small farm outbuilding. Total path length of nearly 4.0 miles and peak winds of at least 85 mph (EF0)."
                    }
                  ]
                }
              }
            }
            """;

        JSONAssert.assertEquals(expected, response, JSONCompareMode.NON_EXTENSIBLE);
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

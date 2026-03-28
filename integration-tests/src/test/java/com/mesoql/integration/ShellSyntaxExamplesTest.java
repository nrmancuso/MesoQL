package com.mesoql.integration;

import com.mesoql.integration.support.ShellExtension;
import com.mesoql.integration.support.ShellResult;
import com.mesoql.integration.support.ShellSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ShellExtension.class)
class ShellSyntaxExamplesTest {

    private static final String PROMPT = "mesoql> ";

    @Test
    @DisplayName("runs the syntax doc basic semantic search example")
    void runsBasicSemanticSearchExample(ShellSession shell) {
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> SEARCH storm_events WHERE SEMANTIC("tornado outbreak") LIMIT 10


            --- [1] ---
              event_id:            1253359
              event_type:          Tornado
              narrative:           A regional outbreak of tornadoes occurred across portions|of western into south-central Kansas during the afternoon to|evening hours of Sunday 18 May 2025.  The first couple of tornadic|supercells occurred near Scott City KS in Scott County during the|late afternoon and progressed northward with both storms generating|EF2 tornadoes and a lone brief EFU tornado. A second tornadic|supercell developed near Dodge City KS in the early evening and|progressed northeast with an EFO tornado occurring near Kinsley KS|in Edwards County and an EF2 tornado near Garfield KS in Pawnee County.|A final long-duration tornadic supercell then formed in the mid|evening hours over Comanche County KS and commenced moving northeast|toward Greensburg KS in Kiowa County, Iuka KS in Pratt County,|north of Preston KS in Pratt County and finally across Stafford|and Reno Counties. This storm generated five EF3 tornadoes along with|several smaller satellite tornadoes.  The final count for this day|was 13 tornadoes just within the NWS Dodge City Warning area. Fairly short-lived tornado. Video of tornado from KAKE chaser Cameron Venable as it was crossing or just after crossing US-50. Just some minor tree damage and one small farm outbuilding. Total path length of nearly 4.0 miles and peak winds of at least 85 mph (EF0).
              fatalities:          0
              state:               KANSAS
              damage_property:     0

            --- [2] ---
              event_id:            1256656
              event_type:          Tornado
              narrative:           A regional outbreak of tornadoes occurred across portions|of western into south-central Kansas during the afternoon to|evening hours of Sunday 18 May 2025.  The first couple of tornadic|supercells occurred near Scott City KS in Scott County during the|late afternoon and progressed northward with both storms generating|EF2 tornadoes and a lone brief EFU tornado. A second tornadic|supercell developed near Dodge City KS in the early evening and|progressed northeast with an EFO tornado occurring near Kinsley KS|in Edwards County and an EF2 tornado near Garfield KS in Pawnee County.|A final long-duration tornadic supercell then formed in the mid|evening hours over Comanche County KS and commenced moving northeast|toward Greensburg KS in Kiowa County, Iuka KS in Pratt County,|north of Preston KS in Pratt County and finally across Stafford|and Reno Counties. This storm generated five EF3 tornadoes along with|several smaller satellite tornadoes.  The final count for this day|was 13 tornadoes just within the NWS Dodge City Warning area. A NWS storm survey confirmed reports of a tornado. Some damage included broken power poles, tree trunk snaped, an irrigation pivot broken and tossed.
              fatalities:          0
              state:               KANSAS
              damage_property:     0

            --- [3] ---
              event_id:            1253120
              event_type:          Hail
              narrative:           A regional outbreak of tornadoes occurred across portions|of western into south-central Kansas during the afternoon to|evening hours of Sunday 18 May 2025.  The first couple of tornadic|supercells occurred near Scott City KS in Scott County during the|late afternoon and progressed northward with both storms generating|EF2 tornadoes and a lone brief EFU tornado. A second tornadic|supercell developed near Dodge City KS in the early evening and|progressed northeast with an EFO tornado occurring near Kinsley KS|in Edwards County and an EF2 tornado near Garfield KS in Pawnee County.|A final long-duration tornadic supercell then formed in the mid|evening hours over Comanche County KS and commenced moving northeast|toward Greensburg KS in Kiowa County, Iuka KS in Pratt County,|north of Preston KS in Pratt County and finally across Stafford|and Reno Counties. This storm generated five EF3 tornadoes along with|several smaller satellite tornadoes.  The final count for this day|was 13 tornadoes just within the NWS Dodge City Warning area. A report of 1 inch diameter hail was received from the Public.
              fatalities:          0
              state:               KANSAS
              damage_property:     0

            --- [4] ---
              event_id:            1241136
              event_type:          Tornado
              narrative:           A cold front pushed into the area during the afternoon and evening hours, interacting with  decent shear and MLCAPE to allow for numerous thunderstorm development. Pockets of damaging winds occurred along with a total of six confirmed tornadoes (three in far southern Lower Michigan and three in northern Indiana). A brief EF-1 tornado was confirmed in Edwardsburg, MI on March 30th, 2025. The tornado started in a field west of Conrad Rd, snapping and uprooting trees as it moved northeast towards the Edwardsburg Primary School, where EF-0 damage was noted. The tornado continued to the northeast and intensified, where several homes and businesses sustained damage on the east side of Pleasant Lake. The Starboard Choice Marina building sustained roof damage, a boat lift and dock were removed from the lake and flipped over, and a few boats were damaged as well, including one that was flipped and lifted over a fence. The most intense damage was seen along Dailey Road, where numerous large trees were snapped and uprooted, with estimated peak wind speeds of 90 mph. Overall, the tornado was on the ground for 3 minutes and had a peak intensity of EF-1. There were also areas of straight-line wind damage noted throughout Edwardsburg, indicative that the tornado was likely embedded within the line of storms that moved through.
              fatalities:          0
              state:               MICHIGAN
              damage_property:     100000

            --- [5] ---
              event_id:            1247623
              event_type:          Thunderstorm Wind
              narrative:           A strongly forced band of convection developed over West Virginia in the morning ahead of an approaching front, and quickly lifted north-northeastward into Pennsylvania through the early afternoon. Despite relatively modest instability, very strong deep layer shear, along with largely unidirectional southerly flow and elongated hodographs, supported the continuation of significant severe storms. Large impact was felt from straight-line wind, with gusts in the 60 to 80 MPH range, locally higher, causing widespread damage to trees, power lines, and structures. The line of storms continued into Pennsylvania, where the environment was more favorable for tornadic development.  In addition to damaging winds, a number of large hail reports were received, with some stones as large as 1.75 inch diameter. Also, embedded supercells/mesovortices produced five confirmed tornadoes during the afternoon (one of which crossed from Fayette County into Westmoreland County). Trees and power lines were downed, blocking Route 56 between Old Mission Road and PA-954.
              fatalities:          0
              state:               PENNSYLVANIA
              damage_property:     25000

            --- [6] ---
              event_id:            1252415
              event_type:          Thunderstorm Wind
              narrative:           A cold-front initiated a line of thunderstorms across north and central GA through the afternoon hours of the 31st. Storms brought damaging winds along the I20 corridor and south as well as six quick spin-up tornadoes. The strongest being an EF1 between McDonough and Stockbridge crossing Hwy 75. There were also a couple reports of quarter size hail southeast of Macon. Tree down at the intersection of highway 5 and old columbus road.
              fatalities:          0
              state:               GEORGIA
              damage_property:     1000

            --- [7] ---
              event_id:            1256747
              event_type:          Hail
              narrative:           With an approaching low and dynamically primed atmosphere, storms were able to develop and take advantage of ample moisture. The event began with fairly isolated supercells moving primarily eastward before a line of storms took over descending from the northwest. The storms produced numerous hail reports and a funnel cloud. Multiple public reports of 2 inch hail in and around Manter between 15:28-15:42.
              fatalities:          0
              state:               KANSAS
              damage_property:     0

            --- [8] ---
              event_id:            1256667
              event_type:          Thunderstorm Wind
              narrative:           As a trough from a deep low entered the area, a line of clustered storms developed and moved into the area. One long lasting cluster moved through the northern counties producing both severe winds and hail. A public report estimated wind gusts at 60 mph. A mesonet site recorded a 56 mph gust shortly before the public report at the KKSNESSC2 observing station.
              fatalities:          0
              state:               KANSAS
              damage_property:     0

            --- [9] ---
              event_id:            1249680
              event_type:          Tornado
              narrative:           An upper low over the Central Plains moved into the Upper Mississippi Valley during the evening hours of Friday, March 14, 2025. Increasing moisture advection ahead of an approaching cold front lifted dewpoints into the low to mid 60s. This coupled with increasing height falls and a mid-level 80 knot jet rotating around the main upper low supported severe thunderstorm development. Large looping hodographs, MLCAPE values over 2000 J/kg, 60 knots of bulk shear, and 0-1 km SRH values between 250 and 350 m2/s2 supported an all hazards threat. Storm mode during the overnight hours was supercellular with large hail, damaging winds and several tornadoes, a few of which were strong (EF2+), mainly across northeast Arkansas, the Missouri Bootheel and northeast Mississippi. ||The main upper low lifted into the western Great Lakes Region by early Saturday morning, March 15, 2025. The cold front stalled just to the west of the Mississippi River. Simultaneously, a neutrally-tilted longwave trough moved across the Southern Plains and pushed a 100 knot mid-level jet into the region. This placed West Tennessee and  north Mississippi beneath the left exit region of the jet with widespread lift and diffluence available. As a result, showers and thunderstorms rapidly redeveloped across north Mississippi and parts of West Tennessee Saturday morning. Storms quickly formed into a line that slowly moved east. The line of storms produced damaging winds, large hail and a weak tornado Saturday morning. The airmass ahead of the line became even more favorable for severe storms by Saturday afternoon with MLCAPE values increasing to 1500 J/kg and 0-1km SRH values reaching 300 m2/s2 as a surface low moving along the cold front and backed surface winds across northeast Mississippi.  Discrete supercells formed ahead of the line during the afternoon producing a couple of weak tornadoes and damaging winds over parts of northeast Missisisppi. Flash flooding was also common due to the slow moving nature of the system. A brief tornado developed near the intersection of Wren Cemetery Road and Old Wren Road and moved northeast, lifting near Highway 278 and Central Grove Road. Only minor tree damage was observed along this path. Peak winds were estimated at 70 mph.
              fatalities:          0
              state:               MISSISSIPPI
              damage_property:     10000

            --- [10] ---
              event_id:            1249623
              event_type:          Thunderstorm Wind
              narrative:           An upper low over the Central Plains moved into the Upper Mississippi Valley during the evening hours of Friday, March 14, 2025. Increasing moisture advection ahead of an approaching cold front lifted dewpoints into the low to mid 60s. This coupled with increasing height falls and a mid-level 80 knot jet rotating around the main upper low supported severe thunderstorm development. Large looping hodographs, MLCAPE values over 2000 J/kg, 60 knots of bulk shear, and 0-1 km SRH values between 250 and 350 m2/s2 supported an all hazards threat. Storm mode during the overnight hours was supercellular with large hail, damaging winds and several tornadoes, a few of which were strong (EF2+), mainly across northeast Arkansas, the Missouri Bootheel and northeast Mississippi. ||The main upper low lifted into the western Great Lakes Region by early Saturday morning, March 15, 2025. The cold front stalled just to the west of the Mississippi River. Simultaneously, a neutrally-tilted longwave trough moved across the Southern Plains and pushed a 100 knot mid-level jet into the region. This placed West Tennessee and  north Mississippi beneath the left exit region of the jet with widespread lift and diffluence available. As a result, showers and thunderstorms rapidly redeveloped across north Mississippi and parts of West Tennessee Saturday morning. Storms quickly formed into a line that slowly moved east. The line of storms produced damaging winds, large hail and a weak tornado Saturday morning. The airmass ahead of the line became even more favorable for severe storms by Saturday afternoon with MLCAPE values increasing to 1500 J/kg and 0-1km SRH values reaching 300 m2/s2 as a surface low moving along the cold front and backed surface winds across northeast Mississippi.  Discrete supercells formed ahead of the line during the afternoon producing a couple of weak tornadoes and damaging winds over parts of northeast Missisisppi. Flash flooding was also common due to the slow moving nature of the system. Straight-line winds associated with the rear flank downdraft from the Vardaman tornado broke three power poles along Highway 8.
              fatalities:          0
              state:               MISSISSIPPI
              damage_property:     10000

            %s
            """.formatted(PROMPT).stripTrailing();
        assertSuccessfulQuery(
            shell,
            "SEARCH storm_events WHERE SEMANTIC(\"tornado outbreak\") LIMIT 10",
            expectedOutput);
    }

    @Test
    @DisplayName("runs the syntax doc filtered storm events example")
    void runsFilteredStormEventsExample(ShellSession shell) {
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> SEARCH storm_events WHERE SEMANTIC("hail damage to crops") AND state IN ("KS", "NE", "OK") AND event_type IN ("Hail") LIMIT 20


            %s
            """.formatted(PROMPT).stripTrailing();
        assertSuccessfulQuery(
            shell,
            """
                SEARCH storm_events
                WHERE SEMANTIC("hail damage to crops")
                AND state IN ("KS", "NE", "OK")
                AND event_type IN ("Hail")
                LIMIT 20
                """.strip(),
            Duration.ofSeconds(120),
            expectedOutput);
    }

    @Test
    @DisplayName("runs the syntax doc synthesis example")
    void runsSynthesisExample(ShellSession shell) {
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> SEARCH storm_events WHERE SEMANTIC("flash flooding in urban areas") AND fatalities > 0 SYNTHESIZE "What were the common contributing factors?" LIMIT 15


            === Synthesis ===
            I'm happy to help! However, I notice that there are no records provided. Could you please provide the actual weather event records from the NOAA Storm Events Database? Once I have those records, I'd be happy to help identify the common contributing factors.

            If you meant to provide the records but forgot to include them, please feel free to share them with me, and I'll do my best to assist you in identifying the common contributing factors.

            %s
            """.formatted(PROMPT).stripTrailing();
        assertSuccessfulQuery(
            shell,
            """
                SEARCH storm_events
                WHERE SEMANTIC("flash flooding in urban areas")
                AND fatalities > 0
                SYNTHESIZE "What were the common contributing factors?"
                LIMIT 15
                """.strip(),
            Duration.ofSeconds(180),
            expectedOutput);
    }

    @Test
    @DisplayName("runs the syntax doc explain example")
    void runsExplainExample(ShellSession shell) {
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> SEARCH forecast_discussions WHERE SEMANTIC("atmospheric river precipitation") AND region IN ("Pacific Northwest") EXPLAIN LIMIT 5


            --- [1] ---
              discussion_id:       afd-1_chunk_0
              season:              winter
              office:              NWS Seattle WA
              issuance_time:       2024-12-10T12:00:00+00:00
              text:                An atmospheric river will bring prolonged precipitation and mountain snow to the Pacific Northwest through Wednesday. Strong onshore flow will maintain heavy rain along the coast and rising snow levels inland.
              region:              Pacific Northwest
              explanation:         This event is semantically relevant to the user's search because it specifically mentions "atmospheric river" precipitation, which matches the user's query by highlighting a weather phenomenon characterized by a long, narrow region in the atmosphere that transports large amounts of water vapor and leads to significant precipitation.

            --- [2] ---
              discussion_id:       afd-2_chunk_0
              season:              winter
              office:              NWS Portland OR
              issuance_time:       2024-12-10T15:00:00+00:00
              text:                A strong moisture plume supports atmospheric river precipitation with heavy rain and periods of mountain snow across northwest Oregon. Hydrologic concerns increase as successive waves reinforce the moist southwest flow.
              region:              Pacific Northwest
              explanation:         This event is semantically relevant to the user's search because it specifically mentions "atmospheric river precipitation", which matches the exact phrase used in the search query, indicating a strong connection between the two.

            %s
            """.formatted(PROMPT).stripTrailing();
        assertSuccessfulQuery(
            shell,
            """
                SEARCH forecast_discussions
                WHERE SEMANTIC("atmospheric river precipitation")
                AND region IN ("Pacific Northwest")
                EXPLAIN
                LIMIT 5
                """.strip(),
            Duration.ofSeconds(240),
            expectedOutput);
    }

    @Test
    @DisplayName("runs the syntax doc cluster example")
    void runsClusterExample(ShellSession shell) {
        final String expectedOutput = """
            MesoQL (type \\q to quit)

            mesoql> SEARCH storm_events WHERE SEMANTIC("hurricane damage") AND state IN ("FL", "TX", "LA") CLUSTER BY THEME LIMIT 30


            === Clusters ===
            Based on the provided weather records, I've grouped them into 4 thematic clusters:

            **Cluster 1: Hurricane Landfalls**
            Records: 1, 2, 3
            Description: These records share a common characteristic of being related to hurricanes making landfall, resulting in significant damage.

            **Cluster 2: Storm Surge Impacts**
            Records: 4, 5, 6
            Description: This cluster is characterized by records that highlight the devastating effects of storm surges caused by hurricanes, leading to extensive coastal damage.

            **Cluster 3: Wind-Related Damage**
            Records: 7, 8, 9
            Description: These records are united by their focus on the destructive power of hurricane-force winds, which caused significant damage and destruction.

            **Cluster 4: Flooding and Rainfall**
            Records: 10, 11, 12
            Description: This cluster is defined by records that emphasize the impact of heavy rainfall and flooding associated with hurricanes, resulting in widespread damage and displacement.

            %s
            """.formatted(PROMPT).stripTrailing();
        assertSuccessfulQuery(
            shell,
            """
                SEARCH storm_events
                WHERE SEMANTIC("hurricane damage")
                AND state IN ("FL", "TX", "LA")
                CLUSTER BY THEME
                LIMIT 30
                """.strip(),
            Duration.ofSeconds(180),
            expectedOutput);
    }

    private static void assertSuccessfulQuery(ShellSession shell, String query, String expectedOutput) {
        assertSuccessfulQuery(shell, query, Duration.ofSeconds(120), expectedOutput);
    }

    private static void assertSuccessfulQuery(
        ShellSession shell,
        String query,
        Duration timeout,
        String expectedOutput) {
        final ShellResult result = shell.sendLine(compactQuery(query), timeout);
        assertEquals(expectedOutput, result.text());
        assertFalse(result.text().contains("ERROR:"), result.text());
        assertTrue(result.promptSeenAgain(), result.text());
    }

    private static String compactQuery(String query) {
        return query.lines()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.joining(" "));
    }
}

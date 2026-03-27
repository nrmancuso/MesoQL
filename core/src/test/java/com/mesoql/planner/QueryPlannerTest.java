package com.mesoql.planner;

import com.mesoql.ast.QueryAST;
import com.mesoql.parser.MesoQLParserHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryPlannerTest {

    private final QueryPlanner planner = new QueryPlanner();

    @Test
    void validQueryPasses() {
        final QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"tornado\") AND state IN (\"TX\") LIMIT 10");
        assertDoesNotThrow(() -> planner.validate(q));
    }

    @Test
    void unknownFieldRejected() {
        final QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"test\") AND magnitude >= 5");
        assertThrows(MesoQLValidationException.class, () -> planner.validate(q));
    }

    @Test
    void betweenOnKeywordRejected() {
        final QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"test\") AND state BETWEEN 1 AND 10");
        assertThrows(MesoQLValidationException.class, () -> planner.validate(q));
    }

    @Test
    void inOnIntegerRejected() {
        final QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"test\") AND fatalities IN (\"1\", \"2\")");
        assertThrows(MesoQLValidationException.class, () -> planner.validate(q));
    }

    @Test
    void synthesizeAndClusterMutuallyExclusive() {
        final QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"test\") SYNTHESIZE \"question\" CLUSTER BY THEME");
        assertThrows(MesoQLValidationException.class, () -> planner.validate(q));
    }

    @Test
    void invalidSeasonRejected() {
        final QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH forecast_discussions WHERE SEMANTIC(\"test\") AND season IN (\"rainy\")");
        assertThrows(MesoQLValidationException.class, () -> planner.validate(q));
    }

    @Test
    void validSeasonAccepted() {
        final QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH forecast_discussions WHERE SEMANTIC(\"test\") AND season IN (\"spring\", \"summer\")");
        assertDoesNotThrow(() -> planner.validate(q));
    }

    @Test
    void invalidStateRejected() {
        final QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"test\") AND state IN (\"XX\")");
        assertThrows(MesoQLValidationException.class, () -> planner.validate(q));
    }

    @Test
    void forecastDiscussionsFieldsValid() {
        final QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH forecast_discussions WHERE SEMANTIC(\"snow\") AND region IN (\"Northeast\")");
        assertDoesNotThrow(() -> planner.validate(q));
    }

    @Test
    void betweenOnNumericAccepted() {
        final QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"test\") AND fatalities BETWEEN 1 AND 100");
        assertDoesNotThrow(() -> planner.validate(q));
    }
}

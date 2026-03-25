package com.mesoql.parser;

import com.mesoql.ast.QueryAST;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MesoQLParserHelperTest {

    @Test
    void basicQuery() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"tornado damage\") LIMIT 10");
        assertEquals("storm_events", q.search().source());
        assertEquals("tornado damage", q.where().semantic().text());
        assertTrue(q.where().filters().isEmpty());
        assertEquals(1, q.outputs().size());
        assertInstanceOf(QueryAST.LimitClause.class, q.outputs().get(0));
        assertEquals(10, ((QueryAST.LimitClause) q.outputs().get(0)).n());
    }

    @Test
    void forecastDiscussionsSource() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH forecast_discussions WHERE SEMANTIC(\"heavy snow\")");
        assertEquals("forecast_discussions", q.search().source());
        assertEquals("heavy snow", q.where().semantic().text());
    }

    @Test
    void inFilter() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"flood\") AND state IN (\"TX\", \"OK\", \"LA\")");
        assertEquals(1, q.where().filters().size());
        QueryAST.InFilter f = (QueryAST.InFilter) q.where().filters().get(0);
        assertEquals("state", f.field());
        assertEquals(List.of("TX", "OK", "LA"), f.values());
    }

    @Test
    void betweenFilter() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"hail\") AND damage_property BETWEEN 10000 AND 50000");
        QueryAST.BetweenFilter f = (QueryAST.BetweenFilter) q.where().filters().get(0);
        assertEquals("damage_property", f.field());
        assertEquals(10000.0, f.low());
        assertEquals(50000.0, f.high());
    }

    @Test
    void comparisonFilterNumeric() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"wind\") AND fatalities >= 5");
        QueryAST.ComparisonFilter f = (QueryAST.ComparisonFilter) q.where().filters().get(0);
        assertEquals("fatalities", f.field());
        assertEquals(">=", f.op());
        assertEquals("5", f.value());
    }

    @Test
    void comparisonFilterString() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"wind\") AND event_type = \"Tornado\"");
        QueryAST.ComparisonFilter f = (QueryAST.ComparisonFilter) q.where().filters().get(0);
        assertEquals("event_type", f.field());
        assertEquals("=", f.op());
        assertEquals("Tornado", f.value());
    }

    @Test
    void synthesizeClause() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"flooding\") SYNTHESIZE \"What were the main causes?\"");
        assertEquals(1, q.outputs().size());
        QueryAST.SynthesizeClause s = (QueryAST.SynthesizeClause) q.outputs().get(0);
        assertEquals("What were the main causes?", s.question());
    }

    @Test
    void clusterByTheme() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"severe weather\") CLUSTER BY THEME");
        assertEquals(1, q.outputs().size());
        assertInstanceOf(QueryAST.ClusterClause.class, q.outputs().get(0));
    }

    @Test
    void explainClause() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"hurricane\") EXPLAIN");
        assertEquals(1, q.outputs().size());
        assertInstanceOf(QueryAST.ExplainClause.class, q.outputs().get(0));
    }

    @Test
    void multipleFiltersAndOutputs() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"tornado\") " +
            "AND state IN (\"TX\", \"OK\") " +
            "AND fatalities >= 1 " +
            "EXPLAIN LIMIT 20");
        assertEquals(2, q.where().filters().size());
        assertInstanceOf(QueryAST.InFilter.class, q.where().filters().get(0));
        assertInstanceOf(QueryAST.ComparisonFilter.class, q.where().filters().get(1));
        assertEquals(2, q.outputs().size());
        assertInstanceOf(QueryAST.ExplainClause.class, q.outputs().get(0));
        assertInstanceOf(QueryAST.LimitClause.class, q.outputs().get(1));
    }

    @Test
    void caseInsensitive() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "search STORM_EVENTS where semantic(\"test\") limit 5");
        assertEquals("storm_events", q.search().source());
        assertEquals("test", q.where().semantic().text());
    }

    @Test
    void syntaxErrorThrows() {
        assertThrows(MesoQLSyntaxException.class, () ->
            MesoQLParserHelper.parse("SEARCH WHERE"));
    }

    @Test
    void missingSemanticThrows() {
        assertThrows(MesoQLSyntaxException.class, () ->
            MesoQLParserHelper.parse("SEARCH storm_events WHERE state = \"TX\""));
    }

    @Test
    void decimalInBetween() {
        QueryAST.Query q = MesoQLParserHelper.parse(
            "SEARCH storm_events WHERE SEMANTIC(\"rain\") AND damage_property BETWEEN 1.5 AND 100.75");
        QueryAST.BetweenFilter f = (QueryAST.BetweenFilter) q.where().filters().get(0);
        assertEquals(1.5, f.low());
        assertEquals(100.75, f.high());
    }
}

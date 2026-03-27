package com.mesoql.ingestion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StormEventsIngesterTest {

    @Test
    void parseDamageK() {
        assertEquals(10_000L, StormEventsIngester.parseDamage("10.00K"));
    }

    @Test
    void parseDamageM() {
        assertEquals(1_500_000L, StormEventsIngester.parseDamage("1.50M"));
    }

    @Test
    void parseDamageB() {
        assertEquals(2_000_000_000L, StormEventsIngester.parseDamage("2.00B"));
    }

    @Test
    void parseDamageEmpty() {
        assertEquals(0L, StormEventsIngester.parseDamage(""));
        assertEquals(0L, StormEventsIngester.parseDamage(null));
    }

    @Test
    void parseDamageNumericOnly() {
        assertEquals(5000L, StormEventsIngester.parseDamage("5000"));
    }

    @Test
    void parseDamageCaseInsensitive() {
        assertEquals(10_000L, StormEventsIngester.parseDamage("10.00k"));
    }
}

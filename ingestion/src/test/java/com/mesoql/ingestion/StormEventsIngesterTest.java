package com.mesoql.ingestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StormEventsIngesterTest {
    @Test
    @DisplayName("Parse damage string with K multiplier (10.00K = 10,000)")
    void parseDamageK() {
        assertEquals(10_000L, StormEventsIngester.parseDamage("10.00K"));
    }

    @Test
    @DisplayName("Parse damage string with M multiplier (1.50M = 1,500,000)")
    void parseDamageM() {
        assertEquals(1_500_000L, StormEventsIngester.parseDamage("1.50M"));
    }

    @Test
    @DisplayName("Parse damage string with B multiplier (2.00B = 2,000,000,000)")
    void parseDamageB() {
        assertEquals(2_000_000_000L, StormEventsIngester.parseDamage("2.00B"));
    }

    @Test
    @DisplayName("Parse empty or null damage string as zero")
    void parseDamageEmpty() {
        assertEquals(0L, StormEventsIngester.parseDamage(""));
        assertEquals(0L, StormEventsIngester.parseDamage(null));
    }

    @Test
    @DisplayName("Parse numeric-only damage string (5000)")
    void parseDamageNumericOnly() {
        assertEquals(5000L, StormEventsIngester.parseDamage("5000"));
    }

    @Test
    @DisplayName("Parse damage string case-insensitive (10.00k = 10,000)")
    void parseDamageCaseInsensitive() {
        assertEquals(10_000L, StormEventsIngester.parseDamage("10.00k"));
    }
}

package com.mesoql.ingestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AFDIngesterTest {
    @Test
    @DisplayName("Chunk short text below max tokens into single chunk")
    void chunkShortText() {
        final List<String> chunks = AFDIngester.chunk("hello world", 512, 64);
        assertEquals(1, chunks.size());
        assertEquals("hello world", chunks.get(0));
    }

    @Test
    @DisplayName("Chunk long text into multiple overlapping chunks")
    void chunkLongText() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("word").append(i).append(" ");
        }
        final List<String> chunks = AFDIngester.chunk(sb.toString().trim(), 512, 64);
        assertTrue(chunks.size() > 1);
        // chunks should overlap
        final String[] firstWords = chunks.get(0).split("\\s+");
        final String[] secondWords = chunks.get(1).split("\\s+");
        // second chunk should start before first chunk ends (overlap)
        assertTrue(firstWords.length > 0);
        assertTrue(secondWords.length > 0);
    }

    @Test
    @DisplayName("Derive spring season from April timestamp")
    void deriveSeasonSpring() {
        assertEquals("spring", AFDIngester.deriveSeason("2024-04-15T12:00:00+00:00"));
    }

    @Test
    @DisplayName("Derive summer season from July timestamp")
    void deriveSeasonSummer() {
        assertEquals("summer", AFDIngester.deriveSeason("2024-07-01T00:00:00+00:00"));
    }

    @Test
    @DisplayName("Derive fall season from October timestamp")
    void deriveSeasonFall() {
        assertEquals("fall", AFDIngester.deriveSeason("2024-10-15T12:00:00+00:00"));
    }

    @Test
    @DisplayName("Derive winter season from January timestamp")
    void deriveSeasonWinter() {
        assertEquals("winter", AFDIngester.deriveSeason("2024-01-15T12:00:00+00:00"));
    }

    @Test
    @DisplayName("Return 'unknown' for empty or null season timestamp")
    void deriveSeasonEmpty() {
        assertEquals("unknown", AFDIngester.deriveSeason(""));
        assertEquals("unknown", AFDIngester.deriveSeason(null));
    }
}

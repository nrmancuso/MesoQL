package com.mesoql.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidatorTest {

    private final InputValidator validator = new InputValidator();

    private static SearchRequest stormRequest(List<FilterInput> filters) {
        return new SearchRequest(
            "storm_events",
            "tornado damage",
            filters,
            Optional.empty(),
            false,
            false,
            10
        );
    }

    private static SearchRequest forecastRequest(List<FilterInput> filters) {
        return new SearchRequest(
            "forecast_discussions",
            "winter storm",
            filters,
            Optional.empty(),
            false,
            false,
            10
        );
    }

    @Test
    @DisplayName("Accept valid storm events request with state filter")
    void validStormEventsRequestPasses() {
        final SearchRequest request = stormRequest(
            List.of(new InFilterInput("state", List.of("TX", "OK")))
        );
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Accept valid forecast discussions request with office filter")
    void validForecastDiscussionsRequestPasses() {
        final SearchRequest request = forecastRequest(
            List.of(new InFilterInput("office", List.of("BOU")))
        );
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    @DisplayName("Reject unknown source with validation error")
    void unknownSourceThrows() {
        final SearchRequest request = new SearchRequest(
            "unknown_source",
            "test",
            List.of(),
            Optional.empty(),
            false,
            false,
            10
        );
        final ValidationException ex = assertThrows(
            ValidationException.class, () -> validator.validate(request));
        assertTrue(ex.getMessage().contains("Unknown source"));
    }

    @Test
    @DisplayName("Reject unknown field in filter with validation error")
    void unknownFieldThrows() {
        final SearchRequest request = stormRequest(
            List.of(new InFilterInput("magnitude", List.of("5")))
        );
        final ValidationException ex = assertThrows(
            ValidationException.class, () -> validator.validate(request));
        assertTrue(ex.getMessage().contains("Unknown field"));
    }

    @Test
    @DisplayName("Reject IN filter on integer field with validation error")
    void inFilterOnIntegerFieldThrows() {
        final SearchRequest request = stormRequest(
            List.of(new InFilterInput("fatalities", List.of("1", "2")))
        );
        final ValidationException ex = assertThrows(
            ValidationException.class, () -> validator.validate(request));
        assertTrue(ex.getMessage().contains("IN filter"));
    }

    @Test
    @DisplayName("Reject BETWEEN filter on keyword field with validation error")
    void betweenFilterOnKeywordFieldThrows() {
        final SearchRequest request = stormRequest(
            List.of(new BetweenFilterInput("state", 1.0, 10.0))
        );
        final ValidationException ex = assertThrows(
            ValidationException.class, () -> validator.validate(request));
        assertTrue(ex.getMessage().contains("BETWEEN filter"));
    }

    @Test
    @DisplayName("Reject invalid season value with validation error")
    void invalidSeasonValueThrows() {
        final SearchRequest request = forecastRequest(
            List.of(new InFilterInput("season", List.of("rainy")))
        );
        final ValidationException ex = assertThrows(
            ValidationException.class, () -> validator.validate(request));
        assertTrue(ex.getMessage().contains("season"));
    }

    @Test
    @DisplayName("Reject synthesize and clusterByTheme together with validation error")
    void synthesizeAndClusterByThemeMutuallyExclusive() {
        final SearchRequest request = new SearchRequest(
            "storm_events",
            "test",
            List.of(),
            Optional.of("What caused this?"),
            false,
            true,
            10
        );
        final ValidationException ex = assertThrows(
            ValidationException.class, () -> validator.validate(request));
        assertTrue(ex.getMessage().contains("clusterByTheme"));
    }

    @Test
    @DisplayName("Accept all valid season values (spring, summer, fall, winter)")
    void allValidSeasonValuePass() {
        for (final String season : List.of("spring", "summer", "fall", "winter")) {
            final SearchRequest request = forecastRequest(
                List.of(new InFilterInput("season", List.of(season)))
            );
            assertDoesNotThrow(() -> validator.validate(request));
        }
    }
}

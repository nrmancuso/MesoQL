package com.mesoql.search;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Validates a {@link SearchRequest} against static per-source field schemas.
 * Throws {@link ValidationException} on the first violation found.
 */
@Component
public class InputValidator {

    /**
     * Field schema for the {@code storm_events} index.
     */
    static final Map<String, FieldSchema> STORM_EVENTS_FIELDS = Map.of(
        "state",           new FieldSchema(FieldType.KEYWORD),
        "event_type",      new FieldSchema(FieldType.KEYWORD),
        "year",            new FieldSchema(FieldType.INTEGER),
        "begin_date",      new FieldSchema(FieldType.DATE),
        "fatalities",      new FieldSchema(FieldType.INTEGER),
        "damage_property", new FieldSchema(FieldType.LONG)
    );

    /**
     * Field schema for the {@code forecast_discussions} index.
     */
    static final Map<String, FieldSchema> FORECAST_DISCUSSIONS_FIELDS = Map.of(
        "office",        new FieldSchema(FieldType.KEYWORD),
        "region",        new FieldSchema(FieldType.KEYWORD),
        "season",        new FieldSchema(FieldType.KEYWORD),
        "issuance_time", new FieldSchema(FieldType.DATE)
    );

    private static final Set<String> VALID_SEASONS = Set.of("spring", "summer", "fall", "winter");

    private static final Set<String> VALID_STATES = Set.of(
        "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
        "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
        "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
        "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
        "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY",
        "DC", "PR", "VI", "GU", "AS", "MP"
    );

    /**
     * Validates the given request, throwing {@link ValidationException} on the first violation.
     */
    public void validate(SearchRequest request) {
        final Map<String, FieldSchema> schema = schemaFor(request.source());

        for (final FilterInput filter : request.filters()) {
            switch (filter) {
                case InFilterInput f -> {
                    validateFieldExists(f.field(), schema, request.source());
                    final FieldType type = schema.get(f.field()).type();
                    if (type != FieldType.KEYWORD) {
                        throw new ValidationException(
                            "IN filter only applies to keyword fields, but '"
                                + f.field() + "' is " + type);
                    }
                    validateFieldValues(f.field(), f.values());
                }
                case BetweenFilterInput f -> {
                    validateFieldExists(f.field(), schema, request.source());
                    final FieldType type = schema.get(f.field()).type();
                    if (type == FieldType.KEYWORD) {
                        throw new ValidationException(
                            "BETWEEN filter does not apply to keyword fields, but '"
                                + f.field() + "' is KEYWORD");
                    }
                }
                case ComparisonFilterInput f -> validateFieldExists(f.field(), schema, request.source());
            }
        }

        if (request.synthesizePrompt().isPresent() && request.clusterByTheme()) {
            throw new ValidationException("synthesize and clusterByTheme cannot both be set");
        }
    }

    private Map<String, FieldSchema> schemaFor(String source) {
        return switch (source) {
            case "storm_events" -> STORM_EVENTS_FIELDS;
            case "forecast_discussions" -> FORECAST_DISCUSSIONS_FIELDS;
            default -> throw new ValidationException("Unknown source: " + source);
        };
    }

    private void validateFieldExists(String field, Map<String, FieldSchema> schema, String source) {
        if (!schema.containsKey(field)) {
            throw new ValidationException(
                "Unknown field '" + field + "' for source '" + source + "'");
        }
    }

    private void validateFieldValues(String field, java.util.List<String> values) {
        if ("season".equals(field)) {
            for (final String value : values) {
                if (!VALID_SEASONS.contains(value.toLowerCase(java.util.Locale.ROOT))) {
                    throw new ValidationException(
                        "Invalid season value '" + value + "'. Valid: spring, summer, fall, winter");
                }
            }
        }
        if ("state".equals(field)) {
            for (final String value : values) {
                if (!VALID_STATES.contains(value.toUpperCase(java.util.Locale.ROOT))) {
                    throw new ValidationException(
                        "Invalid state value '" + value + "'. Must be a valid US state abbreviation");
                }
            }
        }
    }
}

package com.mesoql.planner;

import com.mesoql.ast.QueryAST;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Validates a parsed MesoQL query against static per-source field schemas before execution.
 */
@Component
public class QueryPlanner {

    public static final Map<String, FieldSchema> STORM_EVENTS_FIELDS = Map.of(
        "state",           new FieldSchema(FieldType.KEYWORD),
        "event_type",      new FieldSchema(FieldType.KEYWORD),
        "year",            new FieldSchema(FieldType.INTEGER),
        "begin_date",      new FieldSchema(FieldType.DATE),
        "fatalities",      new FieldSchema(FieldType.INTEGER),
        "damage_property", new FieldSchema(FieldType.LONG)
    );

    public static final Map<String, FieldSchema> FORECAST_DISCUSSIONS_FIELDS = Map.of(
        "office",          new FieldSchema(FieldType.KEYWORD),
        "region",          new FieldSchema(FieldType.KEYWORD),
        "season",          new FieldSchema(FieldType.KEYWORD),
        "issuance_time",   new FieldSchema(FieldType.DATE)
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
     * Validates the query's filters and output clauses, throwing on any schema or logic violation.
     */
    public void validate(QueryAST.Query query) {
        final String source = query.search().source();
        final Map<String, FieldSchema> schema = schemaFor(source);

        for (final QueryAST.Filter filter : query.where().filters()) {
            switch (filter) {
                case QueryAST.InFilter f -> {
                    validateFieldExists(f.field(), schema, source);
                    final FieldType type = schema.get(f.field()).type();
                    if (type != FieldType.KEYWORD) {
                        throw new MesoQLValidationException(
                            "IN filter only applies to keyword fields, but '" + f.field() + "' is " + type);
                    }
                    validateFieldValues(f.field(), f.values());
                }
                case QueryAST.BetweenFilter f -> {
                    validateFieldExists(f.field(), schema, source);
                    final FieldType type = schema.get(f.field()).type();
                    if (type == FieldType.KEYWORD) {
                        throw new MesoQLValidationException(
                            "BETWEEN filter does not apply to keyword fields, but '" + f.field() + "' is KEYWORD");
                    }
                }
                case QueryAST.ComparisonFilter f -> {
                    validateFieldExists(f.field(), schema, source);
                }
            }
        }

        boolean hasSynthesize = false;
        boolean hasCluster = false;
        for (final QueryAST.OutputClause clause : query.outputs()) {
            if (clause instanceof QueryAST.SynthesizeClause) hasSynthesize = true;
            if (clause instanceof QueryAST.ClusterClause) hasCluster = true;
        }
        if (hasSynthesize && hasCluster) {
            throw new MesoQLValidationException("SYNTHESIZE and CLUSTER BY THEME cannot both be present");
        }
    }

    private Map<String, FieldSchema> schemaFor(String source) {
        return switch (source) {
            case "storm_events" -> STORM_EVENTS_FIELDS;
            case "forecast_discussions" -> FORECAST_DISCUSSIONS_FIELDS;
            default -> throw new MesoQLValidationException("Unknown source: " + source);
        };
    }

    private void validateFieldExists(String field, Map<String, FieldSchema> schema, String source) {
        if (!schema.containsKey(field)) {
            throw new MesoQLValidationException(
                "Unknown field '" + field + "' for source '" + source + "'");
        }
    }

    private void validateFieldValues(String field, java.util.List<String> values) {
        if ("season".equals(field)) {
            for (final String v : values) {
                if (!VALID_SEASONS.contains(v.toLowerCase())) {
                    throw new MesoQLValidationException(
                        "Invalid season value '" + v + "'. Valid: spring, summer, fall, winter");
                }
            }
        }
        if ("state".equals(field)) {
            for (final String v : values) {
                if (!VALID_STATES.contains(v.toUpperCase())) {
                    throw new MesoQLValidationException(
                        "Invalid state value '" + v + "'. Must be a valid US state abbreviation");
                }
            }
        }
    }
}

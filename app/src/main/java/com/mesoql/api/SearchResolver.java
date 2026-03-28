package com.mesoql.api;

import com.mesoql.executor.QueryExecutor;
import com.mesoql.executor.QueryResult;
import com.mesoql.search.BetweenFilterInput;
import com.mesoql.search.ComparisonFilterInput;
import com.mesoql.search.FilterInput;
import com.mesoql.search.InFilterInput;
import com.mesoql.search.InputValidator;
import com.mesoql.search.SearchRequest;
import com.mesoql.search.ValidationException;
import graphql.GraphqlErrorException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Spring GraphQL resolver that handles the {@code search} query.
 */
@Controller
public class SearchResolver {

    private static final int DEFAULT_LIMIT = 10;

    private final InputValidator inputValidator;
    private final QueryExecutor queryExecutor;

    /**
     * Constructs the resolver with the required validator and executor collaborators.
     */
    public SearchResolver(InputValidator inputValidator, QueryExecutor queryExecutor) {
        this.inputValidator = inputValidator;
        this.queryExecutor = queryExecutor;
    }

    /**
     * Executes a semantic search query and returns the combined result.
     */
    @QueryMapping
    public SearchResponse search(@Argument String source, @Argument SearchInput input) {
        final String indexName = source.toLowerCase(Locale.ROOT);
        final SearchRequest request = buildRequest(indexName, input);

        try {
            inputValidator.validate(request);
        } catch (ValidationException e) {
            throw GraphqlErrorException.newErrorException()
                .message(e.getMessage())
                .build();
        }

        final QueryResult result = queryExecutor.execute(request);
        return buildResponse(result, request.source());
    }

    private SearchRequest buildRequest(String source, SearchInput input) {
        final List<FilterInput> filters = buildFilters(input.filters());
        final Optional<String> synthesizePrompt = Optional.ofNullable(input.synthesize());
        final boolean explain = input.explain() != null && input.explain();
        final boolean cluster = input.clusterByTheme() != null && input.clusterByTheme();
        final int limit = input.limit() != null ? input.limit() : DEFAULT_LIMIT;

        return new SearchRequest(source, input.semantic(), filters, synthesizePrompt,
            explain, cluster, limit);
    }

    private List<FilterInput> buildFilters(FiltersInput filtersInput) {
        final List<FilterInput> filters = new ArrayList<>();
        if (filtersInput == null) {
            return filters;
        }
        if (filtersInput.in() != null) {
            for (final com.mesoql.api.InFilterInput f : filtersInput.in()) {
                filters.add(new InFilterInput(f.field(), f.values()));
            }
        }
        if (filtersInput.between() != null) {
            for (final com.mesoql.api.BetweenFilterInput f : filtersInput.between()) {
                filters.add(new BetweenFilterInput(f.field(), f.min(), f.max()));
            }
        }
        if (filtersInput.comparisons() != null) {
            for (final com.mesoql.api.ComparisonFilterInput f : filtersInput.comparisons()) {
                filters.add(new ComparisonFilterInput(f.field(), f.op(), f.value()));
            }
        }
        return filters;
    }

    private SearchResponse buildResponse(QueryResult result, String source) {
        final List<String> explanations = result.explanations();
        final List<Object> hits = new ArrayList<>();

        for (int i = 0; i < result.hits().size(); i++) {
            final Map<String, Object> doc = result.hits().get(i);
            final String explanation = explanations != null && i < explanations.size()
                ? explanations.get(i)
                : null;

            if ("storm_events".equals(source)) {
                hits.add(toStormEventHit(doc, explanation));
            } else {
                hits.add(toForecastDiscussionHit(doc, explanation));
            }
        }

        return new SearchResponse(hits, result.synthesis(), result.clusters());
    }

    private StormEventHit toStormEventHit(Map<String, Object> doc, String explanation) {
        return new StormEventHit(
            toString(doc.get("event_id")),
            toString(doc.get("state")),
            toString(doc.get("event_type")),
            toString(doc.get("begin_date")),
            toInteger(doc.get("fatalities")),
            toLong(doc.get("damage_property")),
            toString(doc.get("narrative")),
            explanation
        );
    }

    private ForecastDiscussionHit toForecastDiscussionHit(Map<String, Object> doc, String explanation) {
        return new ForecastDiscussionHit(
            toString(doc.get("discussion_id")),
            toString(doc.get("office")),
            toString(doc.get("region")),
            toString(doc.get("season")),
            toString(doc.get("issuance_time")),
            toString(doc.get("text")),
            explanation
        );
    }

    private String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        return null;
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return null;
    }
}

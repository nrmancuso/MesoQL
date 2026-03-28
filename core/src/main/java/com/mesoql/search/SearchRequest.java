package com.mesoql.search;

import java.util.List;
import java.util.Optional;

/**
 * Immutable representation of a search request passed to {@link com.mesoql.executor.QueryExecutor}.
 */
public record SearchRequest(
        String source,
        String semantic,
        List<FilterInput> filters,
        Optional<String> synthesizePrompt,
        boolean explain,
        boolean clusterByTheme,
        int limit) {

    /**
     * Compact constructor that makes an immutable copy of the filters list.
     */
    public SearchRequest {
        filters = List.copyOf(filters);
    }
}

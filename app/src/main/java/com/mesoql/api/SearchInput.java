package com.mesoql.api;

/**
 * GraphQL input type for a search request.
 */
public record SearchInput(
        String semantic,
        FiltersInput filters,
        String synthesize,
        Boolean clusterByTheme,
        Boolean explain,
        Integer limit) {
}

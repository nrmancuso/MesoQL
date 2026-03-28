package com.mesoql.api;

import java.util.List;

/**
 * GraphQL input type grouping all filter categories.
 */
public record FiltersInput(
        List<InFilterInput> in,
        List<BetweenFilterInput> between,
        List<ComparisonFilterInput> comparisons) {

    /**
     * Compact constructor that makes immutable copies of all filter lists.
     */
    public FiltersInput {
        in = in == null ? List.of() : List.copyOf(in);
        between = between == null ? List.of() : List.copyOf(between);
        comparisons = comparisons == null ? List.of() : List.copyOf(comparisons);
    }
}

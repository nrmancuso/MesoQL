package com.mesoql.search;

/**
 * Sealed interface representing a single search filter on a named field.
 */
public sealed interface FilterInput permits InFilterInput, BetweenFilterInput, ComparisonFilterInput {
}

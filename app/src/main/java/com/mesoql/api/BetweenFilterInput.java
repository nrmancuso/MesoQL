package com.mesoql.api;

/**
 * GraphQL input type for a BETWEEN filter on a numeric field.
 */
public record BetweenFilterInput(String field, double min, double max) {
}

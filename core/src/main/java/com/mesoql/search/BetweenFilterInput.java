package com.mesoql.search;

/**
 * Filter that matches documents where a numeric field is within [min, max].
 */
public record BetweenFilterInput(String field, double min, double max) implements FilterInput {
}

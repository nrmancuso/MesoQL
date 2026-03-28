package com.mesoql.search;

/**
 * Filter that applies a comparison operator to a named field.
 */
public record ComparisonFilterInput(String field, ComparisonOp op, String value) implements FilterInput {
}

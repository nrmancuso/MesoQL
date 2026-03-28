package com.mesoql.api;

import com.mesoql.search.ComparisonOp;

/**
 * GraphQL input type for a comparison filter on a named field.
 */
public record ComparisonFilterInput(String field, ComparisonOp op, String value) {
}

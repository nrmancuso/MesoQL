package com.mesoql.api;

import java.util.List;

/**
 * GraphQL input type for an IN filter on a keyword field.
 */
public record InFilterInput(String field, List<String> values) {

    /**
     * Compact constructor that makes an immutable copy of the values list.
     */
    public InFilterInput {
        values = values == null ? List.of() : List.copyOf(values);
    }
}

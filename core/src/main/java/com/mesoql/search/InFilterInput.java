package com.mesoql.search;

import java.util.List;

/**
 * Filter that matches documents where a keyword field's value is in the given list.
 */
public record InFilterInput(String field, List<String> values) implements FilterInput {

    /**
     * Compact constructor that makes an immutable copy of the values list.
     */
    public InFilterInput {
        values = List.copyOf(values);
    }
}

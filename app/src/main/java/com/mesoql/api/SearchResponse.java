package com.mesoql.api;

import java.util.List;

/**
 * GraphQL output type wrapping search hits plus any LLM-generated content.
 */
public record SearchResponse(
        List<Object> hits,
        String synthesis,
        String clusters) {

    /**
     * Compact constructor that makes an immutable copy of the hits list.
     */
    public SearchResponse {
        hits = hits == null ? List.of() : List.copyOf(hits);
    }
}

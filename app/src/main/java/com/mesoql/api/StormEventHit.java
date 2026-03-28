package com.mesoql.api;

/**
 * GraphQL output type representing a single storm event search result.
 */
public record StormEventHit(
        String eventId,
        String state,
        String eventType,
        String beginDate,
        Integer fatalities,
        Long damageProperty,
        String narrative,
        String explanation) {
}

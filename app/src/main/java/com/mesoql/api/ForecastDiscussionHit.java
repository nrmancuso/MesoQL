package com.mesoql.api;

/**
 * GraphQL output type representing a single forecast discussion search result.
 */
public record ForecastDiscussionHit(
        String discussionId,
        String office,
        String region,
        String season,
        String issuanceTime,
        String text,
        String explanation) {
}

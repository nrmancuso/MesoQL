package com.mesoql.integration.support;

/**
 * Static utilities for inspecting raw GraphQL JSON response bodies.
 */
public final class GraphQLUtil {

    private GraphQLUtil() {
        throw new UnsupportedOperationException("GraphQLUtil is a utility class");
    }

    /**
     * Extracts the {@code jobId} string value from a JSON response body.
     *
     * @param responseBody JSON containing a {@code "jobId"} field
     * @return the job ID string
     * @throws IllegalStateException if the field is missing or malformed
     */
    public static String extractJobId(String responseBody) {
        final int keyIdx = responseBody.indexOf("\"jobId\"");
        if (keyIdx < 0) {
            throw new IllegalStateException("No jobId in response: " + responseBody);
        }
        final int quoteStart = responseBody.indexOf('"', keyIdx + 8);
        if (quoteStart < 0) {
            throw new IllegalStateException("Malformed jobId in response: " + responseBody);
        }
        final int quoteEnd = responseBody.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) {
            throw new IllegalStateException("Malformed jobId in response: " + responseBody);
        }
        return responseBody.substring(quoteStart + 1, quoteEnd);
    }
}

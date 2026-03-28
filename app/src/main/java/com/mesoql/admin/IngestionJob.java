package com.mesoql.admin;

import java.util.UUID;

/**
 * Immutable snapshot of an ingestion job's current state.
 */
public record IngestionJob(
        UUID jobId,
        IngestionStatus status,
        int docsIndexed,
        String error) {

    /**
     * Creates a new job in the RUNNING state with zero docs indexed.
     */
    public static IngestionJob running(UUID jobId) {
        return new IngestionJob(jobId, IngestionStatus.RUNNING, 0, null);
    }

    /**
     * Returns a copy of this job marked as DONE with the given doc count.
     */
    public IngestionJob withDone(int count) {
        return new IngestionJob(jobId, IngestionStatus.DONE, count, null);
    }

    /**
     * Returns a copy of this job marked as FAILED with the given error message.
     */
    public IngestionJob withFailed(String errorMessage) {
        return new IngestionJob(jobId, IngestionStatus.FAILED, docsIndexed, errorMessage);
    }
}

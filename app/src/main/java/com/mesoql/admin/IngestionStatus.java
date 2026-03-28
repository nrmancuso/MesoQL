package com.mesoql.admin;

/**
 * Lifecycle states for an asynchronous ingestion job.
 */
public enum IngestionStatus {

    /**
     * Ingestion is in progress.
     */
    RUNNING,

    /**
     * Ingestion completed successfully.
     */
    DONE,

    /**
     * Ingestion failed.
     */
    FAILED
}

package com.mesoql.admin;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for tracking asynchronous ingestion jobs.
 */
@Component
public class IngestionJobStore {

    private final ConcurrentHashMap<UUID, IngestionJob> jobs = new ConcurrentHashMap<>();

    /**
     * Creates a new RUNNING job and returns its assigned UUID.
     */
    public UUID create() {
        final UUID id = UUID.randomUUID();
        jobs.put(id, IngestionJob.running(id));
        return id;
    }

    /**
     * Updates the job to DONE status with the given doc count.
     */
    public void markDone(UUID id, int docsIndexed) {
        jobs.computeIfPresent(id, (k, job) -> job.withDone(docsIndexed));
    }

    /**
     * Updates the job to FAILED status with the given error message.
     */
    public void markFailed(UUID id, String error) {
        jobs.computeIfPresent(id, (k, job) -> job.withFailed(error));
    }

    /**
     * Returns the job for the given ID, or empty if not found.
     */
    public Optional<IngestionJob> get(UUID id) {
        return Optional.ofNullable(jobs.get(id));
    }
}

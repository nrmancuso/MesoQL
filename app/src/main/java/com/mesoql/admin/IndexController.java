package com.mesoql.admin;

import com.mesoql.ingestion.AFDIngester;
import com.mesoql.ingestion.StormEventsIngester;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for triggering and monitoring ingestion jobs.
 */
@RestController
@RequestMapping("/admin/index")
public class IndexController {

    private final StormEventsIngester stormEventsIngester;
    private final AFDIngester afdIngester;
    private final IngestionJobStore jobStore;

    /**
     * Constructs the controller with the required ingesters and job store.
     */
    public IndexController(
            StormEventsIngester stormEventsIngester,
            AFDIngester afdIngester,
            IngestionJobStore jobStore) {
        this.stormEventsIngester = stormEventsIngester;
        this.afdIngester = afdIngester;
        this.jobStore = jobStore;
    }

    /**
     * Accepts a NOAA Storm Events CSV and starts background ingestion.
     */
    @PostMapping("/storm-events")
    public ResponseEntity<Map<String, String>> indexStormEvents(
            @RequestParam("file") MultipartFile file) throws IOException {
        final Path tempFile = Files.createTempFile("storm-events-", ".csv");
        file.transferTo(tempFile);
        final UUID jobId = jobStore.create();

        Thread.ofVirtual().start(() -> {
            try {
                final int docsIndexed = stormEventsIngester.ingest(tempFile);
                jobStore.markDone(jobId, docsIndexed);
            } catch (Exception e) {
                jobStore.markFailed(jobId, e.getMessage());
            } finally {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            }
        });

        return ResponseEntity.accepted()
            .body(Map.of("jobId", jobId.toString(), "status", "RUNNING"));
    }

    /**
     * Starts background ingestion of NWS Area Forecast Discussions.
     */
    @PostMapping("/forecast-discussions")
    public ResponseEntity<Map<String, String>> indexForecastDiscussions(
            @RequestParam(required = false) String since) {
        final UUID jobId = jobStore.create();

        Thread.ofVirtual().start(() -> {
            try {
                final int docsIndexed = afdIngester.ingest(since);
                jobStore.markDone(jobId, docsIndexed);
            } catch (Exception e) {
                jobStore.markFailed(jobId, e.getMessage());
            }
        });

        return ResponseEntity.accepted()
            .body(Map.of("jobId", jobId.toString(), "status", "RUNNING"));
    }

    /**
     * Returns the current status of an ingestion job.
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<IngestionJob> getJobStatus(@PathVariable String jobId) {
        final UUID id;
        try {
            id = UUID.fromString(jobId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return jobStore.get(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

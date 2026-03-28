package com.mesoql.admin;

import com.mesoql.search.OpenSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for reporting OpenSearch index statistics.
 */
@RestController
@RequestMapping("/admin")
public class StatsController {

    private final OpenSearchService openSearchService;

    /**
     * Constructs the controller with the required OpenSearch service.
     */
    public StatsController(OpenSearchService openSearchService) {
        this.openSearchService = openSearchService;
    }

    /**
     * Returns doc counts and store sizes for the storm_events and forecast_discussions indices.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() throws IOException {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("storm_events", openSearchService.indexStatsSummary("storm_events"));
        result.put("forecast_discussions",
            openSearchService.indexStatsSummary("forecast_discussions"));
        return ResponseEntity.ok(result);
    }
}

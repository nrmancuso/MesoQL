package com.mesoql.cli;

import com.mesoql.search.OpenSearchService;
import org.opensearch.client.opensearch.indices.IndicesStatsResponse;
import org.opensearch.client.opensearch.indices.stats.IndicesStats;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Displays OpenSearch index statistics for the primary MesoQL indexes.
 */
@Command(name = "stats", description = "Show OpenSearch index stats.")
@Component
public class StatsCommand implements Callable<Integer> {

    private static final long BYTES_PER_KIB = 1024L;
    private static final double BYTES_PER_KIB_DOUBLE = 1024.0;
    private static final String STORAGE_UNITS = "KMGTPE";

    private final OpenSearchService searchService;

    /**
     * Constructs the stats command.
     *
     * @param searchService the OpenSearch service
     */
    public StatsCommand(OpenSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Prints stats for each known index.
     *
     * @return exit code `0` on success
     * @throws Exception if the stats call fails unexpectedly
     */
    @Override
    public Integer call() throws Exception {
        for (final String index : List.of("storm_events", "forecast_discussions")) {
            try {
                final IndicesStatsResponse stats = searchService.indexStats(index);
                final IndicesStats indexStats = stats.indices().get(index);
                if (indexStats != null) {
                    final long docCount = indexStats.total().docs().count();
                    final long sizeBytes = indexStats.total().store().sizeInBytes();
                    System.out.printf("%-30s docs: %,d  size: %s%n",
                        index, docCount, humanReadableBytes(sizeBytes));
                } else {
                    System.out.printf("%-30s (index not found)%n", index);
                }
            } catch (Exception e) {
                System.out.printf("%-30s error: %s%n", index, e.getMessage());
            }
        }
        return 0;
    }

    private static String humanReadableBytes(long bytes) {
        if (bytes < BYTES_PER_KIB) return bytes + " B";
        final int exp = (int) (Math.log(bytes) / Math.log(BYTES_PER_KIB_DOUBLE));
        final String unit = STORAGE_UNITS.charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(BYTES_PER_KIB_DOUBLE, exp), unit);
    }
}

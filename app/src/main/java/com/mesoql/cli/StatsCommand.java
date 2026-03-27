package com.mesoql.cli;

import com.mesoql.search.OpenSearchService;
import org.opensearch.client.opensearch.indices.IndicesStatsResponse;
import org.opensearch.client.opensearch.indices.stats.IndicesStats;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "stats", description = "Show OpenSearch index stats.")
@Component
public class StatsCommand implements Callable<Integer> {

    private final OpenSearchService searchService;

    public StatsCommand(OpenSearchService searchService) {
        this.searchService = searchService;
    }

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
        if (bytes < 1024) return bytes + " B";
        final int exp = (int) (Math.log(bytes) / Math.log(1024));
        final String unit = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), unit);
    }
}

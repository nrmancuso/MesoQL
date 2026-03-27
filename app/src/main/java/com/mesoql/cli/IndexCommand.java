package com.mesoql.cli;

import com.mesoql.ingestion.AFDIngester;
import com.mesoql.ingestion.StormEventsIngester;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Indexes supported data sources into OpenSearch.
 */
@Command(name = "index", description = "Index a data source into OpenSearch.")
@Component
public class IndexCommand implements Callable<Integer> {

    @Option(names = "--source", required = true, description = "storm_events | forecast_discussions")
    private String source;

    @Option(names = "--data", description = "Path to CSV file (storm_events only).")
    private Path dataFile;

    @Option(names = "--since", description = "Backfill AFDs since this date. Format: yyyy-MM-dd")
    private String since;

    private final StormEventsIngester stormIngester;
    private final AFDIngester afdIngester;

    /**
     * Constructs the index command with its ingesters.
     *
     * @param stormIngester the storm events ingester
     * @param afdIngester the AFD ingester
     */
    public IndexCommand(StormEventsIngester stormIngester, AFDIngester afdIngester) {
        this.stormIngester = stormIngester;
        this.afdIngester = afdIngester;
    }

    /**
     * Dispatches to the selected ingester.
     *
     * @return exit code `0` on success
     */
    @Override
    public Integer call() {
        switch (source) {
            case "storm_events" -> stormIngester.ingest(dataFile);
            case "forecast_discussions" -> afdIngester.ingest(since);
            default -> throw new IllegalArgumentException("Unknown source: " + source);
        }
        return 0;
    }
}

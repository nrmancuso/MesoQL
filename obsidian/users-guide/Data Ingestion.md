# Data Ingestion

MesoQL ships with two ingesters. Both are incremental — re-running them skips already-indexed
documents.

## NOAA Storm Events

Download CSV files from the NCEI FTP server. Use the `details` file variant:

```
ftp://ftp.ncdc.noaa.gov/pub/data/swdi/stormevents/csvfiles/
```

File naming pattern:

```
StormEvents_details-ftp_v1.0_d2023_c20240117.csv.gz
```

Decompress and ingest:

```bash
gunzip StormEvents_details-ftp_v1.0_d2023_c20240117.csv.gz
just index-storm ./StormEvents_details-ftp_v1.0_d2023_c20240117.csv
```

Multiple years can be ingested sequentially — already-indexed events are skipped by event ID.

### Performance

Large CSV files (10+ years of data) can take 30-90 minutes depending on GPU availability for
embedding. The bottleneck is the embedding step, not CSV parsing.

## NWS Area Forecast Discussions

No download needed. The ingester fetches directly from `api.weather.gov`:

```bash
just index-afd
```

This pulls the most recent ~500 discussions. For deep historical backfill beyond what the NWS API
provides, use the Iowa Environmental Mesonet (IEM) archive. See [[data-sources/NWS Area Forecast
Discussions]] for details.

## How ingestion works

Both ingesters follow the same pipeline:

1. Check OpenSearch for existing document by ID (skip if present)
2. Batch-embed documents in groups of 32 via Ollama (`nomic-embed-text`)
3. Bulk-index to OpenSearch after each batch

Rate limiting between embedding batches prevents overwhelming Ollama.

## Related

- [[data-sources/NOAA Storm Events]] — CSV field reference
- [[data-sources/NWS Area Forecast Discussions]] — API details and IEM archive
- [[components/Ingestion]] — implementation internals

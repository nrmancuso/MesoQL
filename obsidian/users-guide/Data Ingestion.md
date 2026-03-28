# Data Ingestion

MesoQL ships with two ingesters. Both are incremental — re-running them skips already-indexed
documents. Ingestion is triggered via admin HTTP endpoints and runs asynchronously.

## NOAA Storm Events

Download CSV files from the NCEI FTP server. Use the `details` file variant:

```text
ftp://ftp.ncdc.noaa.gov/pub/data/swdi/stormevents/csvfiles/
```

File naming pattern:

```text
StormEvents_details-ftp_v1.0_d2023_c20240117.csv.gz
```

Decompress and ingest via the admin endpoint:

```bash
gunzip StormEvents_details-ftp_v1.0_d2023_c20240117.csv.gz
curl -X POST http://localhost:8080/admin/index/storm-events \
  -F "file=@StormEvents_details-ftp_v1.0_d2023_c20240117.csv"
```

Or using the Justfile shortcut:

```bash
just index-storm ./StormEvents_details-ftp_v1.0_d2023_c20240117.csv
```

The response includes a job ID for polling:

```json
{"jobId": "550e8400-e29b-41d4-a716-446655440000", "status": "RUNNING"}
```

Multiple years can be ingested sequentially — already-indexed events are skipped by event ID.

### Performance

Large CSV files (10+ years of data) can take 30-90 minutes depending on GPU availability for
embedding. The bottleneck is the embedding step, not CSV parsing.

## NWS Area Forecast Discussions

No download needed. The ingester fetches directly from `api.weather.gov`:

```bash
curl -X POST http://localhost:8080/admin/index/forecast-discussions
```

Or with a date filter:

```bash
curl -X POST "http://localhost:8080/admin/index/forecast-discussions?since=2024-01-01"
```

Or using the Justfile shortcut:

```bash
just index-afd
```

This pulls the most recent ~500 discussions. For deep historical backfill beyond what the NWS API
provides, use the Iowa Environmental Mesonet (IEM) archive. See [[data-sources/NWS Area Forecast
Discussions]] for details.

## Job ID Polling

All ingestion requests return a `202 Accepted` with a job ID. Poll for completion:

```bash
# Start ingestion and capture job ID
JOB=$(curl -s -X POST http://localhost:8080/admin/index/forecast-discussions | jq -r .jobId)

# Poll until done
while true; do
  RESULT=$(curl -s "http://localhost:8080/admin/index/${JOB}")
  STATUS=$(echo "$RESULT" | jq -r .status)
  echo "$RESULT" | jq .
  [ "$STATUS" != "RUNNING" ] && break
  sleep 5
done
```

Or using the Justfile shortcut:

```bash
just index-status $JOB
```

Job status values: `RUNNING`, `DONE`, `FAILED`. The `docsIndexed` field shows progress.

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

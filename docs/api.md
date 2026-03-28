# HTTP API Reference

MesoQL exposes two sets of HTTP endpoints: the GraphQL query API and the admin API for ingestion
and statistics.

---

## Running the Server

```bash
just serve
```

This starts the Spring Boot application on port `8080`. The server is ready when the log shows
the Spring Boot startup banner.

---

## GraphQL Endpoint

### `POST /graphql`

Execute a GraphQL query or mutation.

**Request:**
- `Content-Type: application/json`
- Body: `{"query": "...", "variables": {...}}`

**Response:**
- `200 OK` with `{"data": {...}}` on success
- `200 OK` with `{"errors": [...]}` on validation or execution errors (GraphQL spec: errors are
  always returned with HTTP 200)

**Example:**

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query": "{ search(source: STORM_EVENTS, input: {semantic: \"tornado\", limit: 3}) { hits { ... on StormEventHit { eventId state } } } }"}'
```

---

## GraphiQL Playground

### `GET /graphiql`

Opens the GraphiQL browser-based IDE. Enabled by default. Navigate to
[http://localhost:8080/graphiql](http://localhost:8080/graphiql) to:

- Browse the schema via the built-in documentation explorer
- Write and execute queries interactively
- Use autocomplete on field names and input types

---

## Admin Endpoints

### `POST /admin/index/storm-events`

Trigger ingestion of a NOAA Storm Events CSV file. The request body is multipart form data.

**Request:**
- `Content-Type: multipart/form-data`
- Form field: `file` — the decompressed CSV file

**Response `202 Accepted`:**
```json
{"jobId": "550e8400-e29b-41d4-a716-446655440000", "status": "RUNNING"}
```

**Example:**

```bash
curl -X POST http://localhost:8080/admin/index/storm-events \
  -F "file=@StormEvents_details-ftp_v1.0_d2023_c20240117.csv"
```

---

### `POST /admin/index/forecast-discussions`

Trigger ingestion of NWS Area Forecast Discussions from the live NWS API.

**Request:**
- Optional query parameter: `since=yyyy-MM-dd` — only fetch discussions issued after this date

**Response `202 Accepted`:**
```json
{"jobId": "550e8400-e29b-41d4-a716-446655440001", "status": "RUNNING"}
```

**Example:**

```bash
# Ingest all recent discussions
curl -X POST http://localhost:8080/admin/index/forecast-discussions

# Ingest only discussions since a date
curl -X POST "http://localhost:8080/admin/index/forecast-discussions?since=2024-01-01"
```

---

### `GET /admin/index/{jobId}`

Poll the status of an ingestion job.

**Response `200 OK`:**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "RUNNING",
  "docsIndexed": 1234,
  "error": null
}
```

`status` values:
- `RUNNING` — ingestion is in progress
- `DONE` — ingestion completed successfully
- `FAILED` — ingestion failed; see `error` field for message

**Polling workflow:**

```bash
# 1. Start ingestion
JOB=$(curl -s -X POST http://localhost:8080/admin/index/forecast-discussions | jq -r .jobId)

# 2. Poll until complete
while true; do
  RESULT=$(curl -s "http://localhost:8080/admin/index/${JOB}")
  STATUS=$(echo "$RESULT" | jq -r .status)
  echo "$RESULT" | jq .
  [ "$STATUS" != "RUNNING" ] && break
  sleep 5
done
```

---

### `GET /admin/stats`

Return document counts and index sizes.

**Response `200 OK`:**
```json
{
  "storm_events": {"docCount": 45230, "storeSizeBytes": 524288000},
  "forecast_discussions": {"docCount": 12400, "storeSizeBytes": 209715200}
}
```

**Example:**

```bash
curl -s http://localhost:8080/admin/stats | jq .
```

---

## Configuration

MesoQL is configured via `application.yml` (or environment variables with the same key names
uppercased and dots replaced with underscores).

```yaml
spring:
  graphql:
    graphiql:
      enabled: true
      path: /graphiql

mesoql:
  opensearch-url: http://localhost:9200
  ollama-base-url: http://localhost:11434
  embed-model: nomic-embed-text
  generate-model: llama3
```

Key configuration properties:

| Property | Default | Description |
|---|---|---|
| `mesoql.opensearch-url` | `http://localhost:9200` | OpenSearch base URL |
| `mesoql.ollama-base-url` | `http://localhost:11434` | Ollama base URL |
| `mesoql.embed-model` | `nomic-embed-text` | Ollama model for embeddings |
| `mesoql.generate-model` | `llama3` | Ollama model for synthesis/explanation/clustering |
| `spring.graphql.graphiql.enabled` | `true` | Enable GraphiQL playground |

# OpenSearch

**Version:** 2.x with k-NN plugin
**Client:** `opensearch-java` 2.6.0 (not legacy `RestHighLevelClient`)

## Indices

| Index | Vector Field | Vector Dim | Source |
|---|---|---|---|
| `storm_events` | `narrative_vector` | 768 | NOAA Storm Events CSV |
| `forecast_discussions` | `text_vector` | 768 | NWS API |

Vectors produced by `nomic-embed-text` via Ollama. HNSW params: `m: 16`,
`ef_construction: 128`.

## Hybrid Query

Every query is hybrid: k-NN vector similarity + boolean filters, combined via OpenSearch's `hybrid`
query type (2.10+) which handles score normalization natively.

## Query Planner: Field Validation

`QueryPlanner` validates field names and types against static per-source schemas **before** any
network calls. Supported field types: `KEYWORD`, `INTEGER`, `DATE`.

Validation rules:

- Field must exist in source schema
- `BETWEEN` only on numeric/date fields
- `IN` only on keyword fields
- `SYNTHESIZE` and `CLUSTER BY THEME` cannot both be present
- `season` values: `spring`, `summer`, `fall`, `winter`
- `state` values: valid two-letter US abbreviations

## Local Setup

```bash
docker run -p 9200:9200 -p 9600:9600 \
  -e "discovery.type=single-node" \
  -e "plugins.security.disabled=true" \
  opensearchproject/opensearch:2.11.0
```

Set `knn.memory.circuit_breaker.enabled: false` in `opensearch.yml` for local dev.

## Related

- [[components/Grammar]] — AST filter types that map to bool query clauses
- [[components/Ingestion]] — index creation and bulk indexing

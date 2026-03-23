# Ollama

**Models:** `nomic-embed-text` (768-dim embeddings), `llama3` (generation)
**Client:** Direct HTTP via `java.net.http.HttpClient` — no SDK

## Usage

- **Embeddings**: used at both index time (document narratives) and query time (`SEMANTIC(...)`)
- **Generation**: used for `SYNTHESIZE`, `EXPLAIN`, and `CLUSTER BY THEME` output clauses

## Bulk Embedding

Batch size: 32, with short sleep between batches to avoid overwhelming Ollama. Embedding is the
expensive step — the incremental indexing check happens before embedding to avoid re-embedding
unchanged documents.

## Prompt Design

| Output Clause | Call Pattern | Prompt Style |
|---|---|---|
| `SYNTHESIZE "..."` | One call over all results | Expert meteorologist answering from retrieved records only |
| `EXPLAIN` | One call per result | One-sentence semantic relevance explanation |
| `CLUSTER BY THEME` | One call over all results | Group into 2–5 labeled thematic clusters |

## Config

```yaml
ollama_base_url: http://localhost:11434
embed_model: nomic-embed-text
generate_model: llama3
```

## Related

- [[components/Ingestion]] — embedding during ingestion
- [[components/OpenSearch]] — retrieved records passed to generation prompts

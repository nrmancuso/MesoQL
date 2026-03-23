# CLI

**Framework:** picocli 4.7.5
**Packaging:** Fat JAR (Maven Shade plugin) + shell wrapper `mesoql`

## Commands

| Command | Purpose |
|---|---|
| `mesoql query <file.mql>` | Execute a `.mql` file |
| `mesoql query --inline "..."` | Execute an inline query string |
| `mesoql index --source <src>` | Run ingestion pipeline |
| `mesoql validate <file.mql>` | Parse/validate without executing (no network) |
| `mesoql stats` | Show OpenSearch index doc counts and sizes |
| `mesoql shell` | Interactive REPL |

## Shell (REPL)

`mesoql shell` starts an interactive session that reuses the same `QueryExecutor` (and its
underlying HTTP clients) across queries — avoids reconnection overhead. Type `exit` or `quit` to
leave.

## Output Modes

- **Default (human-readable)**: Numbered entries with metadata and narrative, followed by LLM
  output if an output clause was specified.
- **`--json`**: JSON array; each element has all metadata fields, narrative, and LLM output.
  Useful for piping into `jq`.

## Config

`~/.mesoql/config.yaml` (all fields have defaults; only include overrides):

```yaml
opensearch_url: http://localhost:9200
ollama_base_url: http://localhost:11434
embed_model: nomic-embed-text
generate_model: llama3
```

## Related

- [[architecture/Overview]] — build phase 5

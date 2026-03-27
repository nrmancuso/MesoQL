# CLI

**Framework:** picocli 4.7.5 via `picocli-spring-boot-starter` (Spring Boot 3.4.x)
**Shell library:** JLine 3.27.x (line editing, history, Ctrl-R search)
**Packaging:** `./gradlew bootJar` → executable fat JAR, invoked via `just mesoql`

## Default behavior

Running `mesoql` with no arguments starts the interactive shell (like `psql`). Subcommands are
available for non-interactive use.

## Commands

| Command | Purpose |
|---|---|
| `mesoql` | Start interactive shell (default) |
| `mesoql shell` | Same as above (explicit) |
| `mesoql query <file.mql>` | Execute a `.mql` file |
| `mesoql query --inline "..."` | Execute an inline query string |
| `mesoql index --source <src>` | Run ingestion pipeline |
| `mesoql validate <file.mql>` | Parse/validate without executing (no network) |
| `mesoql stats` | Show OpenSearch index doc counts and sizes |

## Shell

The interactive shell reuses the same `QueryExecutor` (and its underlying HTTP clients) across
queries — avoids reconnection overhead. History persists to `~/.mesoql_history`. Exit with `\q`,
`exit`, `quit`, or Ctrl-D.

## Output Modes

- **Default (human-readable)**: Numbered entries with metadata and narrative, followed by LLM
  output if an output clause was specified.
- **`--json`**: JSON array; each element has all metadata fields, narrative, and LLM output.
  Useful for piping into `jq`.

## Config

Spring Boot binds config from `src/main/resources/application.yml`:

```yaml
mesoql:
  opensearch-url: http://localhost:9200
  ollama-base-url: http://localhost:11434
  embed-model: nomic-embed-text
  generate-model: llama3
```

## Related

- [[architecture/Overview]] — build phase 5

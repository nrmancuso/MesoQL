# CLI

**Framework:** picocli 4.7.5 via `picocli-spring-boot-starter` (Spring Boot 3.3.x)
**Packaging:** `./gradlew bootJar` → executable fat JAR + shell wrapper `mesoql`

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

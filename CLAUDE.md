# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Commits

Do not add `Co-Authored-By` trailers to commit messages.

## Project Overview

MesoQL is a self-hostable query engine for semantic search over weather data. It combines a
SQL-style DSL (parsed with ANTLR4) with vector search (OpenSearch k-NN) and local LLM inference
(Ollama). No external API keys required.

Data sources: NOAA storm event narratives and NWS Area Forecast Discussions.

## Build Commands

```bash
mvn clean package        # Build fat JAR (includes ANTLR4 source generation)
mvn clean compile        # Compile only
mvn test                 # Run tests
mvn test -Dtest=FooTest  # Run a single test class
```

The ANTLR4 Maven plugin auto-generates parser/lexer from `src/main/resources/MesoQL.g4` into
`target/generated-sources/antlr4` during compile.

## Local Stack

```bash
# OpenSearch with k-NN plugin
docker run -p 9200:9200 -p 9600:9600 \
  -e "discovery.type=single-node" \
  -e "plugins.security.disabled=true" \
  opensearchproject/opensearch:2.11.0

# Ollama
ollama serve
ollama pull nomic-embed-text
ollama pull llama3
```

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| ANTLR4 | 4.13.x | Grammar + parser generation |
| OpenSearch Java Client | 2.6.0 | Vector search |
| picocli | 4.7.5 | CLI |
| Jackson | 2.x | JSON |
| OpenCSV | 5.9 | CSV ingestion |

## Architecture

### Component Dependency Order (also the implementation phases)

```text
Grammar (ANTLR4) → Parser/AST → Query Planner → Query Executor
                                                    ├── OpenSearch Client
                                                    └── Ollama Client
                                                         ↑
                                              Ingestion Pipeline
                                                    ↑
                                                  CLI
```

### Grammar (`src/main/resources/MesoQL.g4`)

Case-insensitive ANTLR4 grammar. Every query requires `SEMANTIC(...)` as the mandatory clause;
structured filters are optional enhancements. The `source` rule is the extension point for new data
sources (requires a one-line grammar change + lexer token).

### AST (`MesoQLVisitor.java` + `QueryAST.java`)

Visitor pattern (not listener) over the ANTLR4 parse tree. The AST uses sealed interfaces and
records:

```text
Query(SearchClause, WhereClause, List<OutputClause>)
  WhereClause → SemanticClause (required) + List<Filter>
  Filter subtypes: InFilter, BetweenFilter, ComparisonFilter
  OutputClause subtypes: SynthesizeClause, ClusterClause, ExplainClause, LimitClause
```

### Query Planner

Validates field names and types against static per-source schemas **before** any network calls. Fail
fast before touching OpenSearch or Ollama.

### OpenSearch

Two indices: `storm_events` and `forecast_discussions`. Both use 768-dim k-NN vector fields
(`nomic-embed-text` model). Queries are hybrid: k-NN vector similarity + boolean filters combined.

### Ollama

HTTP calls via `java.net.http.HttpClient` (no SDK). Two models:

- `nomic-embed-text` — embeddings (used at both index time and query time)
- `llama3` — generation for `SYNTHESIZE`, `EXPLAIN`, and `CLUSTER BY THEME` output clauses

### Ingestion

- **StormEventsIngester**: Parses NOAA CSV; damage strings like `"10.00K"` → `10000`; fatalities =
  `DEATHS_DIRECT + DEATHS_INDIRECT`
- **AFDIngester**: Fetches from NWS API; long texts are chunked with 512-token sliding window before
  embedding
- Both ingesters skip already-indexed docs (incremental); batch embed in groups of 32 with
  rate-limiting; bulk index to OpenSearch

### CLI (picocli)

Five commands: `query`, `index`, `validate`, `stats`, `shell`. Packaged as a fat JAR with a shell
wrapper named `mesoql`.

`mesoql shell` starts an interactive REPL — reuses the same `QueryExecutor` across queries to
avoid reconnection overhead. Type `exit` or `quit` to leave.

## Documentation

Detailed implementation specs live in `docs/`:

- `docs/grammar.md` — ANTLR4 setup, visitor pattern, AST type hierarchy
- `docs/opensearch.md` — index mappings, hybrid query construction, field validation
- `docs/ollama.md` — embedding/generation calls, prompt design per output clause
- `docs/ingestion.md` — data acquisition (NOAA FTP, NWS API), chunking, bulk indexing
- `docs/cli.md` — picocli structure, output modes, fat JAR packaging
- `docs/BUILDING.md` — full build order, Maven module layout, stack setup

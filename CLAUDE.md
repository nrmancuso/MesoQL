# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Code Style

- **No `var`**: Always use explicit types for local variable declarations.
- **`final` local variables**: All local variables must be declared `final`.
- **No `final` on parameters**: Method parameters must not use the `final` keyword.

## Commits

Do not add `Co-Authored-By` trailers to commit messages.

Every commit message subject line must:

- Start with one of these prefixes:
  - `Issue #123:` — work tracked by an issue
  - `minor:` — small change not worth an issue
  - `doc:` — documentation-only change
  - `dependency:` — dependency or version update
  - `infra:` — infrastructure or tooling change
  - `ci:` — CI/CD change
- Be at most **72 characters** (enforced by the `commit-message` CI workflow)

## Obsidian Knowledge Base

The `obsidian/` directory is an Obsidian vault that serves as the persistent memory for this
project. Its contents are injected into context at the start of every session via a SessionStart
hook — treat it as the authoritative record of decisions, architecture, and implementation state.

**Reading:** Consult the vault when working on anything architectural, component-level, or related
to past decisions. The notes are interlinked with `[[wikilinks]]`; follow them to find related
context.

**Writing:** At the end of every session, update the relevant notes to reflect any decisions,
new components, implementation details, or changes made. Add new notes for new concepts; update
existing ones rather than duplicating. Keep notes concise and interlinked.

## Project Overview

MesoQL is a self-hostable query engine for semantic search over weather data. It combines a
SQL-style DSL (parsed with ANTLR4) with vector search (OpenSearch k-NN) and local LLM inference
(Ollama). No external API keys required.

Data sources: NOAA storm event narratives and NWS Area Forecast Discussions.

## Build Commands

```bash
./gradlew build             # Build fat JAR (includes ANTLR4 source generation)
./gradlew compileJava       # Compile only
./gradlew test              # Run tests
./gradlew test --tests FooTest  # Run a single test class
./gradlew bootJar           # Build executable Spring Boot JAR
```

The Gradle `antlr` plugin auto-generates parser/lexer from `src/main/antlr/MesoQL.g4` into
`build/generated-sources/antlr/main/java` during compile.

## Local Stack

```bash
just up             # starts OpenSearch + Ollama via Docker Compose
just pull-models    # pulls nomic-embed-text and llama3 into Ollama
just jar            # builds fat JAR
just mesoql         # starts interactive shell
```

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| Spring Boot | 3.4.x | Framework, DI, config binding, fat JAR |
| ANTLR4 | 4.13.x | Grammar + parser generation |
| OpenSearch Java Client | 2.6.0 | Vector search |
| picocli-spring-boot-starter | 4.7.5 | CLI (integrated with Spring Boot) |
| JLine | 3.27.x | Interactive shell (line editing, history) |
| OpenCSV | 5.9 | CSV ingestion |

Java 21. Jackson is provided by Spring Boot.

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

### CLI (picocli + JLine)

`just mesoql` starts the interactive shell (like `psql`). Subcommands available via
`just mesoql <cmd>`: `query`, `index`, `validate`, `stats`, `shell`. Shell uses JLine for line
editing and persistent history (`~/.mesoql_history`).

## Documentation

Detailed implementation specs live in `docs/`:

- `docs/grammar.md` — ANTLR4 setup, visitor pattern, AST type hierarchy
- `docs/opensearch.md` — index mappings, hybrid query construction, field validation
- `docs/ollama.md` — embedding/generation calls, prompt design per output clause
- `docs/ingestion.md` — data acquisition (NOAA FTP, NWS API), chunking, bulk indexing
- `docs/cli.md` — picocli + Spring Boot structure, output modes, fat JAR packaging
- `docs/BUILDING.md` — full build order, Gradle project layout, stack setup

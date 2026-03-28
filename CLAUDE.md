# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository.

## Code Style

- **No `var`**: Always use explicit types for local variable declarations.
- **`final` local variables**: All local variables must be declared `final`.
- **No `final` on parameters**: Method parameters must not use the `final` keyword.

## Testing

- **No `@Order` annotations**: Tests should not depend on execution order. Use `@BeforeAll` for shared
  setup (e.g., seeding data) and keep individual tests independent.
- **`@DisplayName` on all tests**: Every test must have a `@DisplayName` annotation with a clear,
  human-readable description of what it tests (e.g., `@DisplayName("Search storm events by
  semantic query and verify response structure")`).
- **Assert entire response body**: Don't just check for the presence of a field or lack of errors.
  Verify the response contains expected structures and data: `assertTrue(response.contains("\"data\""))`,
  `assertTrue(response.contains("\"search\""))`, etc.
- **Extract static test helpers**: Reusable test logic (e.g., polling, uploading files, seeding
  data) should be moved to `com.mesoql.integration.support.TestHelper`.
- **Parallel test execution**: Both test classes and test methods within classes run in parallel
  (configured via `junit-platform.properties`). `AppServerExtension` uses locking to ensure only
  one test class starts the server; others reuse it. Data is seeded once via `TestHelper.ensureDataSeeded()`
  with double-checked locking. Only the test class that started the server destroys it in `afterAll()`.

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

MesoQL is a self-hostable query engine for semantic search over weather data. It exposes a GraphQL
HTTP API backed by vector search (OpenSearch k-NN) and local LLM inference (Ollama). No external
API keys required.

Data sources: NOAA storm event narratives and NWS Area Forecast Discussions.

## Build Commands

```bash
./gradlew build             # Build fat JAR
./gradlew compileJava       # Compile only
./gradlew test              # Run tests
./gradlew test --tests FooTest  # Run a single test class
./gradlew bootJar           # Build executable Spring Boot JAR
```

## Local Stack

```bash
just up             # starts OpenSearch + Ollama via Docker Compose
just pull-models    # pulls nomic-embed-text and llama3 into Ollama
just jar            # builds fat JAR
just serve          # starts the MesoQL HTTP server at :8080
```

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| Spring Boot | 4.x | Framework, DI, config binding, fat JAR |
| spring-boot-starter-graphql | (BOM) | GraphQL runtime + Spring MVC integration |
| graphql-java-extended-scalars | latest | `Long` scalar for `damageProperty` |
| OpenSearch Java Client | 2.6.0 | Vector search |
| OpenCSV | 5.9 | CSV ingestion |

Java 21. Jackson is provided by Spring Boot.

## Architecture

### Component Dependency Order

```text
HTTP API (spring-graphql)
    └── SearchResolver
            └── InputValidator  ← field schema validation
            └── Query Executor
                    ├── OpenSearch Client
                    └── Ollama Client
                         ↑
               Ingestion Pipeline
```

### InputValidator

Validates field names and types against static per-source schemas **before** any network calls. Fail
fast before touching OpenSearch or Ollama. Lives in `core/src/main/java/com/mesoql/search/`.

### OpenSearch

Two indices: `storm_events` and `forecast_discussions`. Both use 768-dim k-NN vector fields
(`nomic-embed-text` model). Queries are hybrid: k-NN vector similarity + boolean filters combined.

### Ollama

HTTP calls via `java.net.http.HttpClient` (no SDK). Two models:

- `nomic-embed-text` — embeddings (used at both index time and query time)
- `llama3` — generation for `synthesize`, `explain`, and `clusterByTheme` output clauses

### Ingestion

- **StormEventsIngester**: Parses NOAA CSV; damage strings like `"10.00K"` → `10000`; fatalities =
  `DEATHS_DIRECT + DEATHS_INDIRECT`
- **AFDIngester**: Fetches from NWS API; long texts are chunked with 512-token sliding window before
  embedding
- Both ingesters skip already-indexed docs (incremental); batch embed in groups of 32 with
  rate-limiting; bulk index to OpenSearch
- Triggered via `POST /admin/index/*` endpoints; returns job ID for polling

## Documentation

Detailed implementation specs live in `docs/`:

- `docs/graphql.md` — GraphQL schema reference, field tables, example queries
- `docs/api.md` — HTTP API reference: `/graphql`, `/graphiql`, `/admin/*`; running the server
- `docs/opensearch.md` — index mappings, hybrid query construction, field validation
- `docs/ollama.md` — embedding/generation calls, prompt design per output clause
- `docs/ingestion.md` — data acquisition (NOAA FTP, NWS API), chunking, bulk indexing
- `docs/BUILDING.md` — full build order, Gradle project layout, stack setup

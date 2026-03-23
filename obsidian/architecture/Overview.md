# Architecture Overview

## Component Dependency Order

```
Grammar (ANTLR4)
    └── QueryAST (sealed interfaces/records)
            └── Query Planner (field validation)
                    └── Query Executor
                            ├── OpenSearch Client  ← k-NN vector search
                            └── Ollama Client      ← embeddings + generation
                    └── Ingestion Pipeline
                            ├── StormEventsIngester
                            └── AFDIngester
                    └── CLI (picocli)
```

## Key Design Decisions

- **Hybrid-first**: `SEMANTIC(...)` is mandatory on every query; structured filters are optional
  enhancements to semantic ranking.
- **Sealed AST**: Java sealed interfaces + records give compile-time exhaustiveness on AST node
  types.
- **Fail fast**: `QueryPlanner` validates field names and types against static per-source schemas
  before any network calls.
- **Local-only LLM**: All inference via Ollama — `nomic-embed-text` for embeddings, `llama3` for
  generation. No external APIs.
- **Extension via grammar rule**: Adding a new data source is a one-line grammar change in
  `MesoQL.g4` + a new `Ingester` implementation.

## Stack

- **Java 21**, **Spring Boot 3.3.x**, **Gradle**
- Spring Boot provides DI, config binding, and the executable fat JAR (`bootJar`)

## Build Phases

| Phase | Component | Depends On |
|---|---|---|
| 1 | Grammar + AST | — |
| 2 | OpenSearch client | Grammar |
| 3 | Ollama client | Grammar |
| 4 | Ingestion pipeline | OpenSearch + Ollama |
| 5 | CLI (picocli + Spring Boot) | All |

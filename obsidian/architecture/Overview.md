# Architecture Overview

## Component Dependency Order

```text
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

- **Java 21**, **Spring Boot 3.4.x**, **Gradle 8.14.x**
- Spring Boot provides DI, config binding, and the executable fat JAR (`bootJar`)
- Java 21 toolchain via Foojay resolver (`org.gradle.java.home` in `gradle.properties`)

## Build Phases (all implemented)

| Phase | Component | Status |
|---|---|---|
| 1 | Grammar + AST | ✅ |
| 2 | OpenSearch client | ✅ |
| 3 | Ollama client + config | ✅ |
| 4 | Query Planner | ✅ |
| 5 | Query Executor | ✅ |
| 6 | Ingestion pipeline | ✅ |
| 7 | CLI (picocli + Spring Boot) | ✅ |
| 8 | Docker Compose (services) | ✅ |

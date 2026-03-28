# Architecture Overview

## Component Dependency Order

```text
HTTP API (spring-graphql)
    └── SearchResolver
            └── InputValidator  ← field schema validation
            └── Query Executor
                    ├── OpenSearch Client  ← k-NN vector search
                    └── Ollama Client      ← embeddings + generation
            └── Ingestion Pipeline
                    ├── StormEventsIngester
                    └── AFDIngester
```

## Key Design Decisions

- **Hybrid-first**: `semantic` is mandatory on every query; structured filters are optional
  enhancements to semantic ranking.
- **GraphQL-first**: queries arrive as GraphQL documents over HTTP; the grammar and CLI are removed.
- **Fail fast**: `InputValidator` validates field names and types against static per-source schemas
  before any network calls.
- **Local-only LLM**: All inference via Ollama — `nomic-embed-text` for embeddings, `llama3` for
  generation. No external APIs.

## Stack

- **Java 21**, **Spring Boot 4.x**, **Gradle 8.14.x**
- Spring Boot provides DI, config binding, GraphQL runtime, and the executable fat JAR (`bootJar`)
- Java 21 toolchain via Foojay resolver (`org.gradle.java.home` in `gradle.properties`)

## Build Phases (all implemented)

| Phase | Component | Status |
|---|---|---|
| 1 | OpenSearch client | ✅ |
| 2 | Ollama client + config | ✅ |
| 3 | Input Validator | ✅ |
| 4 | Query Executor | ✅ |
| 5 | Ingestion pipeline | ✅ |
| 6 | Docker Compose (services) | ✅ |
| 7 | HTTP API (spring-graphql) | ✅ |

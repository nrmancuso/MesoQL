# Building MesoQL

This document is the entry point for building MesoQL. Read it first; it explains the component
dependencies and the order to build them.

## Build Order

Each phase depends on the one before it. Don't skip ahead.

### 1. [Grammar](grammar.md)

Start here. The ANTLR4 grammar is the core artifact everything else is built around. This covers
Gradle plugin setup, grammar structure, the visitor implementation, and the QueryAST types. Nothing
else compiles until the grammar and parser are working.

### 2. [OpenSearch](opensearch.md)

Set up the OpenSearch indices and the Java client. Covers index mappings, k-NN plugin configuration,
and hybrid query construction. The execution engine depends on this.

### 3. [Ollama](ollama.md)

Wire up the Ollama HTTP client for embeddings and generation. Covers the embedding call used at
both index time and query time, and the prompt design for `SYNTHESIZE`, `EXPLAIN`, and
`CLUSTER BY THEME`. Depends on the grammar (to know what output clauses to handle) and OpenSearch
(to understand what gets retrieved before generation).

### 4. [Ingestion](ingestion.md)

Build the ingestion pipeline for NOAA Storm Events and NWS AFDs. Depends on OpenSearch (index must
exist) and Ollama (embedding happens during ingestion). This is a prerequisite for running any real
queries.

### 5. [CLI](cli.md)

Wire everything together behind the picocli CLI backed by Spring Boot. The `query`, `index`,
`validate`, `stats`, and `shell` commands all depend on components built in steps 1-4.

## Component Dependency Map

```text
Grammar (ANTLR4)
    └── Query Planner (semantic validation)
            └── Execution Engine
                    ├── OpenSearch Client  ← OpenSearch indices
                    └── Ollama Client      ← Ollama models
                            └── Ingestion Pipeline
                                    └── CLI (picocli + Spring Boot)
```

## Gradle Project Layout

MesoQL is a single Gradle project. The Gradle `antlr` plugin generates parser/lexer sources from
`src/main/antlr/MesoQL.g4` into `build/generated-sources/antlr/main/java` at compile time. No
manual code generation step is needed.

Key `build.gradle.kts` structure:

```kotlin
plugins {
    java
    antlr
    id("org.springframework.boot") version "3.3.x"
    id("io.spring.dependency-management") version "1.1.x"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    implementation("org.antlr:antlr4-runtime:4.13.1")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("info.picocli:picocli-spring-boot-starter:4.7.5")
    implementation("org.opensearch.client:opensearch-java:2.6.0")
    implementation("com.opencsv:opencsv:5.9")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

generateGrammarSource {
    arguments = listOf("-visitor", "-no-listener")
}
```

## Key Dependency Versions

| Dependency | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.x |
| ANTLR4 | 4.13.x |
| OpenSearch Java client | 2.6.0 |
| picocli-spring-boot-starter | 4.7.x |
| OpenCSV | 5.x (Storm Events CSV parsing) |

Jackson is managed by the Spring Boot dependency management plugin.

## Running the Full Stack Locally

Requires Docker for OpenSearch:

```bash
# Start OpenSearch with k-NN plugin
docker run -p 9200:9200 -p 9600:9600 \
  -e "discovery.type=single-node" \
  -e "plugins.security.disabled=true" \
  opensearchproject/opensearch:2.11.0

# Verify k-NN plugin is loaded
curl http://localhost:9200/_cat/plugins | grep knn

# Start Ollama (assumes Ollama is installed locally)
ollama serve

# Build MesoQL
./gradlew bootJar

# Index and query
mesoql index --source storm_events --data ./StormEvents_2023.csv
mesoql query --inline "SEARCH storm_events WHERE SEMANTIC(\"flooding\") LIMIT 5"
```

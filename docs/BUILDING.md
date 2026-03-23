# Building MesoQL

This document is the entry point for building MesoQL. Read it first; it explains the component dependencies and the order to build them.

## Build Order

Each phase depends on the one before it. Don't skip ahead.

### 1. [Grammar](grammar.md)
Start here. The ANTLR4 grammar is the core artifact everything else is built around. This covers Maven plugin setup, grammar structure, the visitor implementation, and the QueryAST types. Nothing else compiles until the grammar and parser are working.

### 2. [OpenSearch](opensearch.md)
Set up the OpenSearch indices and the Java client. Covers index mappings, k-NN plugin configuration, and hybrid query construction. The execution engine depends on this.

### 3. [Ollama](ollama.md)
Wire up the Ollama HTTP client for embeddings and generation. Covers the embedding call used at both index time and query time, and the prompt design for `SYNTHESIZE`, `EXPLAIN`, and `CLUSTER BY THEME`. Depends on the grammar (to know what output clauses to handle) and OpenSearch (to understand what gets retrieved before generation).

### 4. [Ingestion](ingestion.md)
Build the ingestion pipeline for NOAA Storm Events and NWS AFDs. Depends on OpenSearch (index must exist) and Ollama (embedding happens during ingestion). This is a prerequisite for running any real queries.

### 5. [CLI](cli.md)
Wire everything together behind the picocli CLI. The `query`, `index`, `validate`, and `stats` commands all depend on components built in steps 1-4.

## Component Dependency Map

```
Grammar (ANTLR4)
    └── Query Planner (semantic validation)
            └── Execution Engine
                    ├── OpenSearch Client  ← OpenSearch indices
                    └── Ollama Client      ← Ollama models
                            └── Ingestion Pipeline
                                    └── CLI (picocli)
```

## Maven Module Layout

MesoQL is a single Maven module for Phase 1. The ANTLR4 Maven plugin generates parser/lexer sources from `src/main/resources/MesoQL.g4` into `target/generated-sources/antlr4` at compile time. No manual code generation step is needed.

## Key Dependency Versions

| Dependency | Version |
|---|---|
| ANTLR4 | 4.13.x |
| OpenSearch Java client | 2.x |
| Ollama Java client (unofficial) | use HTTP directly via `java.net.http.HttpClient` |
| picocli | 4.7.x |
| Jackson | 2.x (JSON serialization) |
| OpenCSV | 5.x (Storm Events CSV parsing) |

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
mvn clean package

# Index and query
mesoql index --source storm_events --data ./StormEvents_2023.csv
mesoql query --inline "SEARCH storm_events WHERE SEMANTIC(\"flooding\") LIMIT 5"
```

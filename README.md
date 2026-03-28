# MesoQL

MesoQL is an open-source query engine for semantic search over weather data. It exposes a
**GraphQL HTTP API** backed by vector search (OpenSearch k-NN) and local LLM inference (Ollama).
Fully self-hostable; no API keys required.

Every query is hybrid: structured filters narrow the search space, semantic similarity drives
ranking, and an optional LLM clause synthesizes or explains results.

## Requirements

- Java 21
- Docker Desktop (for OpenSearch and Ollama)
- [`just`](https://github.com/casey/just) — `brew install just`

## Quick Start

```bash
just up             # start OpenSearch + Ollama
just pull-models    # pull nomic-embed-text and llama3 (~4 GB, first run only)
just jar            # build the fat JAR
just serve          # start the HTTP server at :8080
```

Open the GraphiQL playground at **http://localhost:8080/graphiql** to explore interactively.

## Example Query

```graphql
{
  search(source: STORM_EVENTS, input: {
    semantic: "tornado that formed rapidly without warning"
    filters: {
      in: [{ field: "state", values: ["KS", "OK", "TX"] }]
      comparisons: [{ field: "fatalities", op: GT, value: "0" }]
    }
    synthesize: "what atmospheric conditions allowed rapid tornado formation?"
    limit: 10
  }) {
    hits {
      ... on StormEventHit {
        eventId
        state
        eventType
        fatalities
        narrative
      }
    }
    synthesis
  }
}
```

```graphql
{
  search(source: FORECAST_DISCUSSIONS, input: {
    semantic: "unexpected ridge breakdown over the Pacific"
    filters: {
      in: [
        { field: "region", values: ["Pacific Northwest"] }
        { field: "season", values: ["winter"] }
      ]
    }
    clusterByTheme: true
    limit: 20
  }) {
    hits {
      ... on ForecastDiscussionHit {
        office
        issuanceTime
        text
      }
    }
    clusters
  }
}
```

## Indexing Data

```bash
# Index NOAA Storm Events CSV
just index-storm ./StormEvents_2023.csv

# Poll ingestion job status
just index-status <jobId>

# Index NWS Area Forecast Discussions
just index-afd

# Show index stats
just stats
```

## Data Sources

- [NOAA Storm Events Database](https://www.ncdc.noaa.gov/stormevents/ftp.jsp): 1.8M+ storm records
  from 1950 onward; public domain
- [NWS Area Forecast Discussions](https://api.weather.gov/products/types/AFD): daily
  meteorologist-authored forecast discussions; public domain

## Documentation

- [`docs/graphql.md`](docs/graphql.md) — GraphQL schema reference and example queries
- [`docs/api.md`](docs/api.md) — HTTP API reference: `/graphql`, `/graphiql`, `/admin/*`
- [`docs/opensearch.md`](docs/opensearch.md) — index mappings and hybrid query construction
- [`docs/ingestion.md`](docs/ingestion.md) — data acquisition and ingestion pipeline
- [`docs/BUILDING.md`](docs/BUILDING.md) — build guide and Gradle project layout

## License

Apache 2.0

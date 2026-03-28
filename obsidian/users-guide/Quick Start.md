# Quick Start

## One command

```bash
just quickstart
```

This handles everything:

1. Starts OpenSearch and Ollama via Docker Compose
2. Waits for both services to be healthy
3. Checks for required models and pulls any that are missing (~4 GB on first run)
4. Builds the MesoQL JAR
5. Indexes NWS Area Forecast Discussions from the live API
6. Starts the HTTP server

## Start the server

```bash
just serve
```

The server starts at `http://localhost:8080`. GraphQL endpoint: `POST /graphql`. GraphiQL
playground: `http://localhost:8080/graphiql`.

## First query

Using curl:

```bash
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "query($input: SearchInput!) { search(source: FORECAST_DISCUSSIONS, input: $input) { hits { ... on ForecastDiscussionHit { discussionId office text } } } }",
    "variables": {
      "input": {"semantic": "winter storm warning", "limit": 5}
    }
  }'
```

Or open the GraphiQL playground at [http://localhost:8080/graphiql](http://localhost:8080/graphiql)
and run:

```graphql
{
  search(source: FORECAST_DISCUSSIONS, input: {semantic: "winter storm warning", limit: 5}) {
    hits {
      ... on ForecastDiscussionHit {
        discussionId
        office
        text
      }
    }
  }
}
```

## Stopping

```bash
just down           # stops services (data is preserved)
just clean          # stops services and deletes all data
```

## Next steps

- [[users-guide/GraphQL API]] — full query reference with examples
- [[users-guide/Data Ingestion]] — loading NOAA Storm Events CSVs
- [[users-guide/Deployment]] — service management details

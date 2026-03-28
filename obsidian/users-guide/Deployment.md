# Deployment

MesoQL runs as a host process backed by OpenSearch and Ollama in Docker Compose.

## Prerequisites

- Docker Desktop (running)
- `just` — `brew install just`

## Start services

```bash
just up             # starts OpenSearch (:9200) and Ollama (:11434)
just pull-models    # pulls nomic-embed-text and llama3 into Ollama
```

The first `just pull-models` downloads ~4 GB of models. Subsequent runs are instant because models
are stored in a named Docker volume.

Verify OpenSearch has the k-NN plugin:

```bash
just opensearch-check
```

## Build and run

```bash
just jar            # builds the fat JAR
just serve          # starts the MesoQL HTTP server at :8080
```

The server exposes:

- GraphQL endpoint: `POST /graphql`
- GraphiQL playground: `http://localhost:8080/graphiql`
- Admin endpoints: `POST /admin/index/*`, `GET /admin/stats`

## Service management

```bash
just status         # show running containers
just logs opensearch  # follow OpenSearch logs
just logs ollama    # follow Ollama logs
just down           # stop services (data preserved in volumes)
just clean          # stop services and delete all data volumes
```

## Data volumes

Docker Compose uses named volumes for persistence:

| Volume | Purpose |
|---|---|
| `opensearch-data` | Indexed documents — survives `just down` |
| `ollama-data` | Downloaded models — avoids re-downloading |

Both are wiped by `just clean`.

## Configuration

MesoQL defaults connect to localhost, matching the Docker Compose port mappings:

```yaml
mesoql:
  opensearch-url: http://localhost:9200
  ollama-base-url: http://localhost:11434
  embed-model: nomic-embed-text
  generate-model: llama3
```

Override via environment variables or a Spring Boot config file.

## Related

- [[architecture/Infrastructure]] — Docker Compose details and Justfile reference
- [[users-guide/Data Ingestion]] — loading data into the indices

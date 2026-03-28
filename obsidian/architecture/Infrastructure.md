# Infrastructure

Local deployment via Docker Compose for services, with MesoQL running from the bootJar on the host.

## Stack

| Service | Image | Purpose |
|---|---|---|
| OpenSearch | `opensearchproject/opensearch:2.11.0` | k-NN vector search |
| Ollama | `ollama/ollama:latest` | Embeddings + generation |
| MesoQL | Host process (`bootJar`) | Query engine |

## Docker Compose

`docker-compose.yml` defines OpenSearch and Ollama as services with named volumes for data
persistence and health checks for readiness. Port mappings:

- `8080` — MesoQL HTTP server (GraphQL at `/graphql`, admin at `/admin/*`)
- `9200` — OpenSearch
- `11434` — Ollama

MesoQL runs on the host via `just serve` (the bootJar), connecting to services at
`localhost:9200` and `localhost:11434`.

## Volumes

| Volume | Purpose |
|---|---|
| `opensearch-data` | Index data — survives `docker compose down` |
| `ollama-data` | Downloaded models — survives restarts, avoids re-downloading |

Use `docker compose down -v` to wipe volumes and start fresh.

## Justfile

Common tasks are scripted in the repo `Justfile` (requires `just`):

```bash
just up              # start OpenSearch + Ollama
just down            # stop services
just clean           # stop services and delete data volumes
just status          # docker compose ps
just logs opensearch # follow logs for a service
just pull-models     # pull nomic-embed-text and llama3 into Ollama
just jar             # build fat JAR
just test            # run unit tests
just serve           # start the MesoQL HTTP server at :8080
just index-storm file=<csv>  # index a NOAA Storm Events CSV (returns job ID)
just index-afd       # index NWS forecast discussions (returns job ID)
just index-status job_id=<id>  # poll ingestion job status
just stats           # show index stats
```

## Prerequisites

- Docker Desktop
- just (`brew install just`)

## Related

- [[architecture/Overview]] — component dependency order
- [[components/OpenSearch]] — index mappings and k-NN config
- [[components/Ollama]] — models pulled by `just pull-models`
- [[components/GraphQL]] — HTTP API and resolver design

# Infrastructure

Local deployment via k3d (k3s-in-Docker) with a reusable container registry.

## Stack

| Service | Image | Purpose |
|---|---|---|
| OpenSearch | `opensearchproject/opensearch:2.11.0` | k-NN vector search |
| Ollama | `ollama/ollama:latest` | Embeddings + generation |
| MesoQL | Built from repo (`bootJar`) | Query engine |

## k3d Cluster

- Cluster name: `mesoql`
- Local registry at `localhost:5050` — any project can push images here
- Port mappings: 9200 (OpenSearch), 11434 (Ollama) forwarded to host
- Namespace: `mesoql`

## Kubernetes Manifests (`k8s/`)

```text
k8s/
  k3d-config.yaml
  namespace.yaml
  opensearch/
    deployment.yaml      single-node, security disabled, k-NN plugin
    service.yaml         ClusterIP :9200
    pvc.yaml             data persistence
  ollama/
    deployment.yaml      model server
    service.yaml         ClusterIP :11434
    pvc.yaml             model storage (survives restarts)
    init-job.yaml        pulls nomic-embed-text + llama3
  mesoql/
    deployment.yaml      image from localhost:5050/mesoql:latest
    configmap.yaml       application.yml pointing to in-cluster services
    service.yaml
```

## ConfigMap Overrides

MesoQL's `application.yml` is overridden in-cluster to point at service DNS:

```yaml
mesoql:
  opensearch-url: http://opensearch.mesoql.svc.cluster.local:9200
  ollama-base-url: http://ollama.mesoql.svc.cluster.local:11434
  embed-model: nomic-embed-text
  generate-model: llama3
```

## Containerization

Multi-stage Dockerfile:

1. **Build stage:** `gradle:8.14.1-jdk21` — runs `bootJar`
2. **Runtime stage:** `eclipse-temurin:21-jre-alpine` — copies fat JAR

`.dockerignore` excludes `.git`, `.idea`, `build`, `obsidian`, `docs`.

## Deploy / Teardown

```bash
./deploy.sh      # create cluster, build+push image, apply manifests, wait for readiness
./teardown.sh    # delete cluster
```

`deploy.sh` steps:
1. Create k3d cluster with local registry (idempotent)
2. `docker build` + `docker push localhost:5050/mesoql:latest`
3. `kubectl apply` all manifests
4. Wait for OpenSearch and Ollama rollouts
5. Run Ollama model pull job
6. Deploy MesoQL
7. Print pod status

## Justfile

Common tasks are scripted in the repo `Justfile` (requires `just`):

```bash
just deploy            # full k3d cluster deploy
just teardown          # delete cluster
just status            # kubectl get pods -n mesoql
just forward-opensearch  # port-forward 9200
just forward-ollama    # port-forward 11434
just query "SEARCH storm_events WHERE SEMANTIC(\"tornado\") LIMIT 5"
just test              # run unit tests
just jar               # build fat JAR
```

## Prerequisites

- Docker Desktop
- k3d (`brew install k3d`)
- just (`brew install just`)

## Related

- [[architecture/Overview]] — component dependency order
- [[components/OpenSearch]] — index mappings and k-NN config
- [[components/Ollama]] — models pulled by init job
- [[components/CLI]] — fat JAR packaging

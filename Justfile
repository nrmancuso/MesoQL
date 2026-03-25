# MesoQL development tasks

# Default: show available commands
default:
    @just --list

# ── Build ─────────────────────────────────────────────────────────────────────

# Compile (including ANTLR source generation)
build:
    ./gradlew compileJava

# Run all unit tests
test:
    ./gradlew test

# Build executable fat JAR
jar:
    ./gradlew bootJar

# ── Local stack (Docker, no k8s) ──────────────────────────────────────────────

# Start OpenSearch locally via Docker
opensearch:
    docker run -d --name mesoql-opensearch \
      -p 9200:9200 -p 9600:9600 \
      -e "discovery.type=single-node" \
      -e "plugins.security.disabled=true" \
      -e "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m" \
      opensearchproject/opensearch:2.11.0

# Stop and remove the local OpenSearch container
opensearch-stop:
    docker rm -f mesoql-opensearch || true

# Start Ollama and pull required models
ollama:
    ollama serve &
    sleep 2
    ollama pull nomic-embed-text
    ollama pull llama3

# Verify OpenSearch k-NN plugin is loaded
opensearch-check:
    curl -s http://localhost:9200/_cat/plugins | grep knn

# ── k3d cluster ───────────────────────────────────────────────────────────────

# Deploy full stack to local k3d cluster
deploy:
    ./deploy.sh

# Tear down k3d cluster
teardown:
    ./teardown.sh

# Show pod status in mesoql namespace
status:
    kubectl -n mesoql get pods

# Follow logs for a deployment (usage: just logs opensearch)
logs name:
    kubectl -n mesoql logs -f deploy/{{name}}

# Port-forward OpenSearch to localhost:9200
forward-opensearch:
    kubectl -n mesoql port-forward svc/opensearch 9200:9200

# Port-forward Ollama to localhost:11434
forward-ollama:
    kubectl -n mesoql port-forward svc/ollama 11434:11434

# Exec into the mesoql pod and run a query
query q:
    kubectl -n mesoql exec -it deploy/mesoql -- java -jar mesoql.jar query --inline '{{q}}'

# ── Ingestion ─────────────────────────────────────────────────────────────────

# Index a NOAA Storm Events CSV (usage: just index-storm ./StormEvents_2023.csv)
index-storm file:
    ./gradlew bootJar --quiet
    ./mesoql index --source storm_events --data {{file}}

# Index NWS AFDs from NWS API
index-afd:
    ./gradlew bootJar --quiet
    ./mesoql index --source forecast_discussions

# ── Docker ────────────────────────────────────────────────────────────────────

# Build Docker image
docker-build:
    docker build -t localhost:5050/mesoql:latest .

# Push Docker image to local registry
docker-push:
    docker push localhost:5050/mesoql:latest

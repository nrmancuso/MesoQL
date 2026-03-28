# MesoQL development tasks

jar := "app/build/libs/mesoql-0.1.0.jar"

# Default: show available commands
default:
    @just --list

# Full quickstart: services, models, build, ingest, shell
quickstart:
    ./quickstart.sh

# ── Build ─────────────────────────────────────────────────────────────────────

# Compile (including ANTLR source generation)
build:
    ./gradlew compileJava

# Run all unit tests
test:
    ./gradlew test

# Run Checkstyle on main sources
checkstyle:
    ./gradlew checkstyleMain

# Build executable fat JAR
jar:
    ./gradlew bootJar

# ── Services ──────────────────────────────────────────────────────────────────

# Start OpenSearch and Ollama via Docker Compose
up:
    docker compose up -d

# Stop services
down:
    docker compose down

# Stop services and delete data volumes
clean:
    docker compose down -v

# Show service status
status:
    docker compose ps

# Follow logs for a service (usage: just logs opensearch)
logs name:
    docker compose logs -f {{name}}

# Pull Ollama models (run after 'just up' on first use)
pull-models:
    docker exec mesoql-ollama ollama pull nomic-embed-text
    docker exec mesoql-ollama ollama pull llama3

# Verify OpenSearch k-NN plugin is loaded
opensearch-check:
    curl -s http://localhost:9200/_cat/plugins | grep knn

# ── MesoQL ───────────────────────────────────────────────────────────────────

# Start the MesoQL HTTP server (GraphQL at :8080/graphql, GraphiQL at :8080/graphiql)
serve:
    java -jar {{jar}}

# ── Ingestion ─────────────────────────────────────────────────────────────────

# Index a NOAA Storm Events CSV via admin endpoint (prints job ID)
index-storm file:
    curl -s -X POST "http://localhost:8080/admin/index/storm-events" \
         -F "file=@{{file}}" | jq .

# Poll ingestion job status
index-status job_id:
    curl -s "http://localhost:8080/admin/index/{{job_id}}" | jq .

# Index NWS AFDs via admin endpoint (prints job ID)
index-afd:
    curl -s -X POST "http://localhost:8080/admin/index/forecast-discussions" | jq .

# Show index stats via admin endpoint
stats:
    curl -s "http://localhost:8080/admin/stats" | jq .

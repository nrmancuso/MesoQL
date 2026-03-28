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

# Start the interactive MesoQL shell
mesoql *args:
    java -jar {{jar}} {{args}}

# Start the full integration-test stack and seed fixtures
integration-stack:
    bash integration-tests/scripts/start-stack.sh

# Start the stack and run the shell-based integration tests
integration-test:
    bash integration-tests/scripts/start-stack.sh
    ./gradlew :integration-tests:test

# ── Ingestion ─────────────────────────────────────────────────────────────────

# Index a NOAA Storm Events CSV (usage: just index-storm ./StormEvents_2023.csv)
index-storm file:
    just mesoql index --source storm_events --data {{file}}

# Index NWS forecast discussions from the NWS API
index-forecast-discussions:
    just mesoql index --source forecast_discussions

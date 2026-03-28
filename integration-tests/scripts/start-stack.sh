#!/bin/bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APP_JAR="$REPO_ROOT/app/build/libs/mesoql-0.1.0.jar"
STORM_FIXTURE="$REPO_ROOT/integration-tests/fixtures/storm-events.csv"
FORECAST_DISCUSSION_FIXTURE_PORT="${FORECAST_DISCUSSION_FIXTURE_PORT:-18080}"

cd "$REPO_ROOT"

if [ -n "${OLLAMA_DATA_DIR:-}" ]; then
    mkdir -p "$OLLAMA_DATA_DIR"
fi

echo "Resetting container state..."
docker compose --profile integration down --remove-orphans > /dev/null 2>&1 || true
# Remove only the OpenSearch data volume so each run starts with a clean index.
# The Ollama volume is intentionally preserved to avoid re-downloading models.
docker volume rm -f mesoql_opensearch-data > /dev/null 2>&1 || true

echo "Starting services..."
docker compose --profile integration up -d

echo "Waiting for OpenSearch..."
until curl -fsS http://localhost:9200/_cluster/health > /dev/null 2>&1; do
    sleep 2
done

echo "Waiting for Ollama..."
until curl -fsS http://localhost:11434/api/tags > /dev/null 2>&1; do
    sleep 2
done

echo "Waiting for forecast discussion fixture server..."
until curl -fsS "http://127.0.0.1:${FORECAST_DISCUSSION_FIXTURE_PORT}/products" > /dev/null 2>&1; do
    sleep 1
done

for model in nomic-embed-text llama3; do
    if docker exec mesoql-ollama ollama list 2>/dev/null | grep -q "^${model}[: ]"; then
        echo "Model $model already present."
    else
        echo "Pulling $model..."
        docker exec mesoql-ollama ollama pull "$model"
    fi
done

echo "Building application jar..."
./gradlew :app:bootJar

echo "Ingesting storm event fixtures through the application..."
java -jar "$APP_JAR" index --source storm_events --data "$STORM_FIXTURE"

echo "Ingesting forecast discussion fixtures through the application..."
MESOQL_NWS_API_BASE_URL="http://127.0.0.1:${FORECAST_DISCUSSION_FIXTURE_PORT}" \
    java -jar "$APP_JAR" index --source forecast_discussions

echo "Integration-test stack is ready."

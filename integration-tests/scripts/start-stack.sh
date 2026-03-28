#!/bin/bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$REPO_ROOT/.gradle-test-home}"
APP_JAR="$REPO_ROOT/app/build/libs/mesoql-0.1.0.jar"
STORM_FIXTURE="$REPO_ROOT/integration-tests/fixtures/storm-events.csv"
AFD_FIXTURE="$REPO_ROOT/integration-tests/fixtures/afd-products.json"
AFD_FIXTURE_PORT="${AFD_FIXTURE_PORT:-18080}"
AFD_FIXTURE_PID=""

cd "$REPO_ROOT"

if [ -n "${OLLAMA_DATA_DIR:-}" ]; then
    mkdir -p "$OLLAMA_DATA_DIR"
fi

cleanup() {
    if [ -n "$AFD_FIXTURE_PID" ] && kill -0 "$AFD_FIXTURE_PID" > /dev/null 2>&1; then
        kill "$AFD_FIXTURE_PID" > /dev/null 2>&1 || true
        wait "$AFD_FIXTURE_PID" 2> /dev/null || true
    fi
}

trap cleanup EXIT

echo "Resetting OpenSearch container state..."
docker compose down --remove-orphans > /dev/null 2>&1 || true
# Remove only the OpenSearch data volume so each run starts with a clean index.
# The Ollama volume is intentionally preserved to avoid re-downloading models.
docker volume rm -f mesoql_opensearch-data > /dev/null 2>&1 || true

echo "Starting OpenSearch and Ollama..."
docker compose up -d opensearch ollama

echo "Waiting for OpenSearch..."
until curl -fsS http://localhost:9200/_cluster/health > /dev/null 2>&1; do
    sleep 2
done

echo "Waiting for Ollama..."
until curl -fsS http://localhost:11434/api/tags > /dev/null 2>&1; do
    sleep 2
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

echo "Starting local AFD fixture server..."
python3 integration-tests/scripts/serve-afd-fixtures.py "$AFD_FIXTURE_PORT" "$AFD_FIXTURE" &
AFD_FIXTURE_PID="$!"

until curl -fsS "http://127.0.0.1:${AFD_FIXTURE_PORT}/products?type=AFD&limit=500" > /dev/null 2>&1; do
    sleep 1
done

echo "Ingesting storm event fixtures through the application..."
java -jar "$APP_JAR" index --source storm_events --data "$STORM_FIXTURE"

echo "Ingesting AFD fixtures through the application..."
MESOQL_NWS_API_BASE_URL="http://127.0.0.1:${AFD_FIXTURE_PORT}" \
    java -jar "$APP_JAR" index --source forecast_discussions

echo "Integration-test stack is ready."

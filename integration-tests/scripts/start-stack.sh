#!/bin/bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "$REPO_ROOT"

if [ -n "${OLLAMA_DATA_DIR:-}" ]; then
    mkdir -p "$OLLAMA_DATA_DIR"
fi

echo "Resetting container state..."
docker compose down --remove-orphans > /dev/null 2>&1 || true
# Remove only the OpenSearch data volume so each run starts with a clean index.
# The Ollama volume is intentionally preserved to avoid re-downloading models.
docker volume rm -f mesoql_opensearch-data > /dev/null 2>&1 || true

echo "Starting services..."
docker compose up -d

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

echo "Integration-test stack is ready."

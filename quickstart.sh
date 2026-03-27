#!/bin/bash
set -euo pipefail

JAR="app/build/libs/mesoql-0.1.0.jar"
STORM_GZ="data/StormEvents_2025_sample.csv.gz"
STORM_CSV="data/StormEvents_2025_sample.csv"

# ── Services ──────────────────────────────────────────────────────────────────

echo "Starting services..."
docker compose up -d

echo "Waiting for OpenSearch..."
until curl -sf http://localhost:9200/_cluster/health > /dev/null 2>&1; do
    sleep 2
done
echo "OpenSearch is ready."

echo "Waiting for Ollama..."
until curl -sf http://localhost:11434/api/tags > /dev/null 2>&1; do
    sleep 2
done
echo "Ollama is ready."

# ── Models ────────────────────────────────────────────────────────────────────

for model in nomic-embed-text llama3; do
    if docker exec mesoql-ollama ollama list 2>/dev/null | grep -q "^$model"; then
        echo "Model $model already present."
    else
        echo "Pulling $model..."
        docker exec mesoql-ollama ollama pull "$model"
    fi
done

# ── Build ─────────────────────────────────────────────────────────────────────

echo "Building MesoQL..."
./gradlew bootJar --quiet

# ── Ingest ────────────────────────────────────────────────────────────────────

if [ ! -f "$STORM_CSV" ]; then
    echo "Decompressing $STORM_GZ..."
    gunzip -k "$STORM_GZ"
fi

echo "Indexing NOAA Storm Events (2025 sample, 500 rows)..."
java -jar "$JAR" index --source storm_events --data "$STORM_CSV"

echo "Indexing NWS Area Forecast Discussions..."
java -jar "$JAR" index --source forecast_discussions

# ── Shell ─────────────────────────────────────────────────────────────────────

echo ""
echo "Ready. Dropping into MesoQL shell."
echo ""
exec java -jar "$JAR"

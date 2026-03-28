#!/bin/bash
set -euo pipefail

JAR="app/build/libs/mesoql-0.1.0.jar"
STORM_GZ="data/StormEvents_2025_sample.csv.gz"
STORM_CSV="data/StormEvents_2025_sample.csv"
SERVER_URL="http://localhost:8080"

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

# ── Server ────────────────────────────────────────────────────────────────────

echo "Starting MesoQL server..."
java -jar "$JAR" &
SERVER_PID=$!

echo "Waiting for MesoQL server..."
until curl -sf -X POST "$SERVER_URL/graphql" \
    -H "Content-Type: application/json" \
    -d '{"query":"{ __typename }"}' > /dev/null 2>&1; do
    sleep 2
done
echo "MesoQL server is ready."

# ── Ingest ────────────────────────────────────────────────────────────────────

poll_job() {
    local job_id="$1"
    local label="$2"
    echo "Waiting for $label ingestion (job $job_id)..."
    while true; do
        local status
        status=$(curl -sf "$SERVER_URL/admin/index/$job_id" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$status" = "DONE" ]; then
            echo "$label ingestion complete."
            return 0
        elif [ "$status" = "FAILED" ]; then
            echo "ERROR: $label ingestion failed." >&2
            return 1
        fi
        sleep 3
    done
}

if [ ! -f "$STORM_CSV" ]; then
    echo "Decompressing $STORM_GZ..."
    gunzip -k "$STORM_GZ"
fi

echo "Indexing NOAA Storm Events (2025 sample)..."
STORM_JOB=$(curl -sf -X POST "$SERVER_URL/admin/index/storm-events" \
    -F "file=@$STORM_CSV" | grep -o '"jobId":"[^"]*"' | cut -d'"' -f4)
poll_job "$STORM_JOB" "storm events"

echo "Indexing NWS Area Forecast Discussions..."
AFD_JOB=$(curl -sf -X POST "$SERVER_URL/admin/index/forecast-discussions" | \
    grep -o '"jobId":"[^"]*"' | cut -d'"' -f4)
poll_job "$AFD_JOB" "forecast discussions"

# ── Ready ─────────────────────────────────────────────────────────────────────

echo ""
echo "MesoQL is ready. Server PID: $SERVER_PID"
echo ""
echo "  GraphQL endpoint:  $SERVER_URL/graphql"
echo "  GraphiQL explorer: $SERVER_URL/graphiql"
echo "  Index stats:       $SERVER_URL/admin/stats"
echo ""
echo "Example query:"
echo '  curl -s -X POST '"$SERVER_URL"'/graphql \'
echo '    -H "Content-Type: application/json" \'
echo '    -d '"'"'{"query":"{ search(source: STORM_EVENTS, input: { semantic: \"tornado\", limit: 5 }) { hits { ... on StormEventHit { eventId state narrative } } } }"}'"'"' | jq .'
echo ""
echo "To stop the server: kill $SERVER_PID"

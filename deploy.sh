#!/bin/bash
set -euo pipefail

REGISTRY="localhost:5050"
IMAGE="$REGISTRY/mesoql:latest"
CLUSTER="mesoql"
NAMESPACE="mesoql"

echo "==> Creating k3d cluster (idempotent)..."
if ! k3d cluster list | grep -q "^$CLUSTER"; then
  k3d cluster create --config k8s/k3d-config.yaml
else
  echo "    Cluster '$CLUSTER' already exists, skipping."
fi

echo "==> Building MesoQL image..."
docker build -t "$IMAGE" .

echo "==> Pushing to local registry..."
docker push "$IMAGE"

echo "==> Applying namespace..."
kubectl apply -f k8s/namespace.yaml

echo "==> Deploying OpenSearch..."
kubectl apply -f k8s/opensearch/

echo "==> Deploying Ollama..."
kubectl apply -f k8s/ollama/pvc.yaml
kubectl apply -f k8s/ollama/deployment.yaml
kubectl apply -f k8s/ollama/service.yaml

echo "==> Waiting for OpenSearch to be ready..."
kubectl -n "$NAMESPACE" rollout status deployment/opensearch --timeout=180s

echo "==> Waiting for Ollama to be ready..."
kubectl -n "$NAMESPACE" rollout status deployment/ollama --timeout=120s

echo "==> Pulling Ollama models (this may take a while)..."
# Delete old job if it exists so we can re-run
kubectl -n "$NAMESPACE" delete job ollama-model-pull --ignore-not-found
kubectl apply -f k8s/ollama/model-pull-job.yaml
kubectl -n "$NAMESPACE" wait --for=condition=complete job/ollama-model-pull --timeout=900s

echo "==> Deploying MesoQL..."
kubectl apply -f k8s/mesoql/

echo "==> Waiting for MesoQL to be ready..."
kubectl -n "$NAMESPACE" rollout status deployment/mesoql --timeout=60s

echo ""
echo "==> All pods:"
kubectl -n "$NAMESPACE" get pods

echo ""
echo "Done! To run a query:"
echo "  kubectl -n $NAMESPACE exec -it deploy/mesoql -- java -jar mesoql.jar query --inline 'SEARCH storm_events WHERE SEMANTIC(\"tornado\") LIMIT 5'"
echo ""
echo "Or port-forward OpenSearch for direct access:"
echo "  kubectl -n $NAMESPACE port-forward svc/opensearch 9200:9200"

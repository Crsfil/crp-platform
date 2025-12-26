#!/usr/bin/env bash
set -euo pipefail

CAMUNDA_BASE_URL="${CAMUNDA_BASE_URL:-http://localhost:18081/engine-rest}"
MODELS_PATH="${MODELS_PATH:-$(cd "$(dirname "$0")/../process-models" && pwd)}"
DEPLOYMENT_NAME="${DEPLOYMENT_NAME:-crp-process-models}"
CAMUNDA_USER="${CAMUNDA_USER:-}"
CAMUNDA_PASSWORD="${CAMUNDA_PASSWORD:-}"

mapfile -t MODELS < <(find "$MODELS_PATH" -type f \( -name "*.bpmn" -o -name "*.dmn" \))
if [ "${#MODELS[@]}" -eq 0 ]; then
  echo "No BPMN/DMN models found under $MODELS_PATH" >&2
  exit 1
fi

ARGS=(
  "-s"
  "-X" "POST"
  "$CAMUNDA_BASE_URL/deployment/create"
  "-F" "deployment-name=$DEPLOYMENT_NAME"
  "-F" "enable-duplicate-filtering=true"
  "-F" "deploy-changed-only=true"
)

if [ -n "$CAMUNDA_USER" ] && [ -n "$CAMUNDA_PASSWORD" ]; then
  ARGS+=("-u" "$CAMUNDA_USER:$CAMUNDA_PASSWORD")
fi

for model in "${MODELS[@]}"; do
  ARGS+=("-F" "data=@${model}")
done

echo "[CRP] Deploying ${#MODELS[@]} process models to $CAMUNDA_BASE_URL"
curl "${ARGS[@]}"

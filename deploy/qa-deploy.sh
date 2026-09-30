#!/usr/bin/env bash
# Deploy/redeploy the eFiscal QA stack from the repo root on the VPS.
#
# Usage:
#   bash deploy/qa-deploy.sh            # pull DEPLOY_BRANCH (from .env, default: main) and redeploy
#   bash deploy/qa-deploy.sh <git-ref>  # deploy a specific commit/tag/branch without pulling (rollback)
set -euo pipefail

cd "$(dirname "$0")/.."

COMPOSE_FILE="docker-compose.qa.yml"
ENV_FILE=".env"
HEALTH_TIMEOUT_SECONDS=240

if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: $ENV_FILE not found. Create it: cp .env.qa.example .env && chmod 600 .env" >&2
  exit 1
fi

source deploy/compose-cmd.sh

env_value() {
  local line
  line="$(grep -E "^$1=" "$ENV_FILE" | tail -n 1 || true)"
  echo "${line#*=}"
}

BACKEND_PORT="$(env_value EFISCAL_BACKEND_PORT)"
BACKEND_PORT="${BACKEND_PORT:-8090}"
FRONTEND_PORT="$(env_value EFISCAL_FRONTEND_PORT)"
FRONTEND_PORT="${FRONTEND_PORT:-8091}"
HEALTH_URL="http://127.0.0.1:${BACKEND_PORT}/api/v1/auth/login"

compose() {
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"
}

TARGET_REF="${1:-}"
if [[ -n "$TARGET_REF" ]]; then
  echo ">> Checking out $TARGET_REF (no pull)"
  git fetch --all --tags --prune
  git checkout "$TARGET_REF"
else
  BRANCH="$(env_value DEPLOY_BRANCH)"
  BRANCH="${BRANCH:-main}"
  echo ">> Updating branch $BRANCH"
  git fetch --all --prune
  git checkout "$BRANCH"
  git pull --ff-only
fi
echo ">> Deploying commit $(git rev-parse --short HEAD): $(git log -1 --pretty=%s)"

echo ">> Building images"
compose build

echo ">> Starting containers"
compose up -d --remove-orphans

echo ">> Waiting for backend (up to ${HEALTH_TIMEOUT_SECONDS}s)"
deadline=$(( $(date +%s) + HEALTH_TIMEOUT_SECONDS ))
while true; do
  code="$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' \
    -d '{"email":"deploy-healthcheck@invalid.local","password":"x"}' "$HEALTH_URL" || true)"
  # Any 2xx-4xx means Spring is up and answering (this dummy login is expected to get 401).
  if [[ "$code" =~ ^[234][0-9][0-9]$ ]]; then
    echo ">> Backend is up (HTTP $code)"
    break
  fi
  if (( $(date +%s) >= deadline )); then
    echo "ERROR: backend did not become ready (last HTTP code: ${code:-none}). Recent logs:" >&2
    compose logs --tail=100 backend >&2
    exit 1
  fi
  sleep 5
done

frontend_code="$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${FRONTEND_PORT}/" || true)"
if [[ "$frontend_code" != "200" ]]; then
  echo "WARNING: frontend returned HTTP ${frontend_code:-none} on 127.0.0.1:${FRONTEND_PORT}" >&2
fi

echo ">> Removing dangling images"
docker image prune -f >/dev/null

compose ps
echo ">> Deploy finished. Note: users must log in again (sessions are held in backend memory)."

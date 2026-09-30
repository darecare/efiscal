# Sourced by the QA scripts. Sets COMPOSE_CMD to a Compose v2 command:
# the `docker compose` plugin, or a standalone `docker-compose` binary that is version 2.x.
# Legacy docker-compose 1.x cannot parse docker-compose.qa.yml (top-level `name:`, format without `version:`).

if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD=(docker compose)
elif command -v docker-compose >/dev/null 2>&1 \
  && docker-compose version --short 2>/dev/null | grep -qE '^v?2\.'; then
  COMPOSE_CMD=(docker-compose)
else
  found="$(docker-compose version --short 2>/dev/null || echo none)"
  echo "ERROR: Docker Compose v2 is required (found docker-compose: $found)." >&2
  echo "Install the 'docker compose' plugin - see DEPLOY_QA.md, step 2." >&2
  exit 1
fi

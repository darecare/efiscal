#!/usr/bin/env bash
# Dump the QA PostgreSQL database to a gzipped SQL file and keep the newest $KEEP dumps.
#
# Usage (from anywhere): bash /opt/elef/deploy/qa-backup.sh
# Cron example:          0 2 * * * cd /opt/elef && bash deploy/qa-backup.sh >> /var/backups/elef/backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")/.."

COMPOSE_FILE="docker-compose.qa.yml"
ENV_FILE=".env"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/elef}"
KEEP="${KEEP:-14}"

source deploy/compose-cmd.sh

env_value() {
  local line
  line="$(grep -E "^$1=" "$ENV_FILE" | tail -n 1 || true)"
  echo "${line#*=}"
}

POSTGRES_USER="$(env_value POSTGRES_USER)"
POSTGRES_DB="$(env_value POSTGRES_DB)"
if [[ -z "$POSTGRES_USER" || -z "$POSTGRES_DB" ]]; then
  echo "ERROR: POSTGRES_USER / POSTGRES_DB missing in $ENV_FILE" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
target="$BACKUP_DIR/efiscal-$(date +%F-%H%M).sql.gz"
tmp="$target.partial"

"${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" --env-file "$ENV_FILE" exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner | gzip > "$tmp"
mv "$tmp" "$target"
chmod 600 "$target"
echo "$(date '+%F %T') backup written: $target"

ls -1t "$BACKUP_DIR"/efiscal-*.sql.gz 2>/dev/null | tail -n +$(( KEEP + 1 )) | xargs -r rm -f

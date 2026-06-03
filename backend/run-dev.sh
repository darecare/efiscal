#!/usr/bin/env bash
set -euo pipefail

# Use JDK 17+ (Spring Boot 3.x). Prefer 21 to match project stack.
for candidate in \
  /usr/lib/jvm/java-21-openjdk-amd64 \
  /usr/lib/jvm/java-1.21.0-openjdk-amd64 \
  /usr/lib/jvm/java-17-openjdk-amd64 \
  /usr/lib/jvm/java-1.17.0-openjdk-amd64; do
  if [[ -d "$candidate" ]]; then
    export JAVA_HOME="$candidate"
    break
  fi
done

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "No JDK 17+ found. Install one, e.g.:"
  echo "  sudo apt update && sudo apt install -y openjdk-21-jdk"
  exit 1
fi

if [[ ! -x "$JAVA_HOME/bin/javac" ]]; then
  echo "JAVA_HOME=$JAVA_HOME has java but no javac (JRE only)."
  echo "Maven needs the full JDK. Install it:"
  echo "  sudo apt update && sudo apt install -y openjdk-21-jdk"
  exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"

echo "Using JAVA_HOME=$JAVA_HOME"
java -version
javac -version

cd "$(dirname "$0")"

DEV_PORT="${SERVER_PORT:-8080}"
if command -v ss >/dev/null 2>&1; then
  if ss -tlnH "sport = :${DEV_PORT}" 2>/dev/null | grep -q .; then
    echo "ERROR: Port ${DEV_PORT} is already in use. Another backend instance is probably still running."
    echo "Find it:  ss -tlnp | grep :${DEV_PORT}"
    echo "Stop it:  kill \$(ss -tlnp | grep :${DEV_PORT} | sed -n 's/.*pid=\\([0-9]*\\).*/\\1/p' | head -1)"
    echo "Or set a different port: SERVER_PORT=8081 ./run-dev.sh"
    exit 1
  fi
fi

echo "Starting backend on port ${DEV_PORT} (Ctrl+C to stop)..."
exec mvn spring-boot:run -Dspring-boot.run.profiles=dev "$@"

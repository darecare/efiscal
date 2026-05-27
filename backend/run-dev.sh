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
exec mvn spring-boot:run -Dspring-boot.run.profiles=dev "$@"

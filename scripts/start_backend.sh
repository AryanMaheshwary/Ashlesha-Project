#!/bin/bash
# Starts the Java (Spring Boot) backend. Builds the jar first if it is missing.
set -e
cd /app/backend-java

if [ ! -f target/pharmacy-backend.jar ]; then
  echo "[start_backend] jar not found, building with Maven..."
  mvn -q -DskipTests package
fi

export SERVER_PORT="${SERVER_PORT:-8080}"
exec java -jar target/pharmacy-backend.jar

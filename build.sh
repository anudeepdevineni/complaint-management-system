#!/usr/bin/env bash
# ============================================================
# build.sh — build the whole app into ONE runnable JAR.
#
# It builds the React frontend, copies the output into the Spring Boot
# backend's static folder, then packages the backend. The resulting JAR
# serves BOTH the website and the API on http://localhost:8080.
#
# Usage:  ./build.sh
# Run:    java -jar backend/target/complaint-system-0.0.1-SNAPSHOT.jar
# ============================================================
set -e   # stop on the first error

ROOT="$(cd "$(dirname "$0")" && pwd)"
FRONTEND="$ROOT/frontend"
BACKEND="$ROOT/backend"
STATIC="$BACKEND/src/main/resources/static"

echo "==> 1/3  Building frontend (Vite)"
cd "$FRONTEND"
npm install
npm run build

echo "==> 2/3  Copying frontend build into backend static folder"
# Wipe old build but keep the .gitkeep placeholder.
find "$STATIC" -mindepth 1 ! -name '.gitkeep' -delete
cp -r "$FRONTEND/dist/." "$STATIC/"

echo "==> 3/3  Packaging backend (Maven)"
cd "$BACKEND"
./mvnw -DskipTests clean package

echo ""
echo "Done. Run it with:"
echo "  java -jar backend/target/complaint-system-0.0.1-SNAPSHOT.jar"
echo "Then open http://localhost:8080"

#!/bin/sh
# One-command launcher (macOS / Linux): builds the React frontend, compiles the Java code
# and starts the server. Then open http://localhost:8080
# Needs: JDK 17+  and  Node.js 18+ (only for the first build).
set -e
cd "$(dirname "$0")"

if [ ! -d frontend/dist ]; then
  echo "==> Building frontend (first run only)..."
  (cd frontend && npm install && npm run build)
fi

echo "==> Compiling Java..."
mkdir -p out
javac -d out -sourcepath src src/hospital/api/ApiServer.java src/hospital/Main.java

echo "==> Starting Hospital app on http://localhost:8080  (Ctrl+C to stop; data is saved to hospital.dat)"
java -cp out hospital.api.ApiServer 8080

#!/bin/sh
# Development mode: Java API on :8080 + Vite hot-reload frontend on http://localhost:5173
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out -sourcepath src src/hospital/api/ApiServer.java src/hospital/Main.java
[ -d frontend/node_modules ] || (cd frontend && npm install)
java -cp out hospital.api.ApiServer 8080 &
API=$!
trap "kill $API 2>/dev/null" EXIT INT TERM
cd frontend && npm run dev

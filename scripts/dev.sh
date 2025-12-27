#!/usr/bin/env bash
set -euo pipefail

# Dev helper: start backend (dev profile) and frontend dev server together
# Usage: ./scripts/dev.sh

# Basic system checks
check_requirements() {
  local ok=1
  if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: 'java' not found in PATH. Install a JDK (e.g., OpenJDK 17+)."
    ok=0
  else
    echo -n "Java: "; java -version 2>&1 | sed -n '1p'
  fi

  if [ -x "${ROOT_DIR}/gradlew" ]; then
    echo -n "Gradle wrapper: "; ${ROOT_DIR}/gradlew --version 2>&1 | sed -n '1p'
  elif command -v gradle >/dev/null 2>&1; then
    echo -n "Gradle: "; gradle --version 2>&1 | sed -n '1p'
  else
    echo "ERROR: 'gradle' not found and no './gradlew' wrapper is present. Install Gradle or ensure './gradlew' exists."
    ok=0
  fi

  if ! command -v node >/dev/null 2>&1; then
    echo "ERROR: 'node' not found in PATH. Install Node.js (v16+ recommended)."
    ok=0
  else
    echo -n "Node: "; node -v
  fi

  if ! command -v npm >/dev/null 2>&1; then
    echo "ERROR: 'npm' not found in PATH. Install npm (usually comes with Node.js)."
    ok=0
  else
    echo -n "npm: "; npm -v
  fi

  if ! command -v curl >/dev/null 2>&1; then
    echo "ERROR: 'curl' not found in PATH. Install curl to allow health checks."
    ok=0
  else
    echo -n "curl: "; curl --version 2>/dev/null | head -n1
  fi

  if [ "$ok" -ne 1 ]; then
    echo "\nOne or more required tools are missing; please install them and re-run this script."
    exit 1
  fi
}

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# Support a 'check-only' mode so developers can validate the environment without starting servers
CHECK_ONLY=false
if [ "${1:-}" = "--check" ] || [ "${1:-}" = "-c" ]; then
  CHECK_ONLY=true
fi

# Run checks first
check_requirements

if [ "$CHECK_ONLY" = true ]; then
  echo "Environment checks passed. Exiting (check-only mode)."
  exit 0
fi
LOG_DIR="$ROOT_DIR/logs/dev"
mkdir -p "$LOG_DIR"

BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"

echo "Starting backend (dev profile) -> logs: $BACKEND_LOG"
cd "$ROOT_DIR"
./gradlew bootRunDev > "$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

# Wait until backend responds to health check (timeout)
HEALTH_URL="http://localhost:8080/api/health"
echo "Waiting for backend health endpoint $HEALTH_URL to respond..."
MAX_WAIT=30
COUNT=0
until curl -sSf "$HEALTH_URL" >/dev/null 2>&1; do
  sleep 1
  COUNT=$((COUNT + 1))
  if [ $COUNT -ge $MAX_WAIT ]; then
    echo "ERROR: backend did not become healthy within ${MAX_WAIT}s. Check $BACKEND_LOG for details.";
    echo "Stopping backend (PID $BACKEND_PID)";
    kill "$BACKEND_PID" 2>/dev/null || true
    exit 1
  fi
done
echo "Backend is healthy."

echo "Starting frontend (Nuxt dev) -> logs: $FRONTEND_LOG"
cd "$ROOT_DIR/web"
npm run dev > "$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!
echo "Frontend PID: $FRONTEND_PID"

cleanup() {
  echo "\nStopping dev servers..."
  kill "$BACKEND_PID" 2>/dev/null || true
  kill "$FRONTEND_PID" 2>/dev/null || true
  wait "$BACKEND_PID" 2>/dev/null || true
  wait "$FRONTEND_PID" 2>/dev/null || true
  echo "Stopped"
  exit 0
}

trap cleanup INT TERM EXIT

echo "Tail logs (press Ctrl+C to stop):"
tail -n 50 -f "$BACKEND_LOG" "$FRONTEND_LOG"

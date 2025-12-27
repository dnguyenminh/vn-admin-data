#!/usr/bin/env bash
set -euo pipefail

# Dev helper: start backend (dev profile) and frontend dev server together
# Usage: ./scripts/dev.sh

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs/dev"
mkdir -p "$LOG_DIR"

BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"

echo "Starting backend (dev profile) -> logs: $BACKEND_LOG"
cd "$ROOT_DIR"
./gradlew bootRunDev > "$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

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

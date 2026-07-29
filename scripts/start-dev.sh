#!/usr/bin/env bash
# Starts the two long-running dev processes documented in CLAUDE.md's "Development" section
# (sbt ~fastLinkJS in watch mode, and the Vite dev server) in the background, so they don't need
# two dedicated terminals. Use scripts/stop-dev.sh to stop them.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.dev-run"
SBT_PID_FILE="$RUN_DIR/sbt.pid"
VITE_PID_FILE="$RUN_DIR/vite.pid"
SBT_LOG="$RUN_DIR/sbt.log"
VITE_LOG="$RUN_DIR/vite.log"

is_running() {
  [[ -f "$1" ]] && kill -0 "$(cat "$1")" 2>/dev/null
}

mkdir -p "$RUN_DIR"

if is_running "$SBT_PID_FILE" || is_running "$VITE_PID_FILE"; then
  echo "Dev environment already running. Run scripts/stop-dev.sh first." >&2
  exit 1
fi

cd "$ROOT_DIR"

if [[ ! -d node_modules ]]; then
  echo "==> Installing npm dependencies..."
  npm install
fi

echo "==> Starting sbt ~fastLinkJS (log: ${SBT_LOG#"$ROOT_DIR/"})"
set -m
nohup sbt "~fastLinkJS" </dev/null >"$SBT_LOG" 2>&1 &
echo $! >"$SBT_PID_FILE"
set +m

echo "==> Starting npm run dev (log: ${VITE_LOG#"$ROOT_DIR/"})"
set -m
# </dev/null: Vite's CLI reads stdin for its "press h + enter" REPL, which throws an uncaught EIO
# and kills the process once nohup detaches stdin from a real terminal — give it a real (empty) fd instead.
nohup npm run dev </dev/null >"$VITE_LOG" 2>&1 &
echo $! >"$VITE_PID_FILE"
set +m

echo "==> Dev environment started."
echo "    Vite:  http://localhost:5173"
echo "    Logs:  tail -f .dev-run/sbt.log .dev-run/vite.log"
echo "    Stop:  scripts/stop-dev.sh"

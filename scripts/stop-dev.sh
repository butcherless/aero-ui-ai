#!/usr/bin/env bash
# Stops the background processes started by scripts/start-dev.sh.
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.dev-run"

stop_one() {
  local name="$1" pid_file="$2"

  if [[ ! -f "$pid_file" ]]; then
    echo "==> $name: not running (no pid file)"
    return
  fi

  local pid
  pid="$(cat "$pid_file")"

  if ! kill -0 "$pid" 2>/dev/null; then
    echo "==> $name: not running (stale pid file)"
    rm -f "$pid_file"
    return
  fi

  echo "==> Stopping $name (pid $pid)..."
  # start-dev.sh backgrounds each process in its own process group (via `set -m`), so signaling
  # the negative pid reaches the whole group -- e.g. sbt's underlying java process, or vite's.
  kill -- "-$pid" 2>/dev/null || kill "$pid" 2>/dev/null || true

  for _ in 1 2 3 4 5; do
    kill -0 "$pid" 2>/dev/null || break
    sleep 1
  done

  if kill -0 "$pid" 2>/dev/null; then
    echo "==> $name didn't stop in time, killing (-9)"
    kill -9 -- "-$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null || true
  fi

  rm -f "$pid_file"
}

stop_one "sbt (~fastLinkJS)" "$RUN_DIR/sbt.pid"
stop_one "npm run dev (vite)" "$RUN_DIR/vite.pid"

echo "==> Dev environment stopped."

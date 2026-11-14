#!/system/bin/sh
PIDFILE=/data/adb/bassengine/daemon.pid
if [ -f "$PIDFILE" ]; then
  PID=$(cat "$PIDFILE" 2>/dev/null)
  [ -n "$PID" ] && kill "$PID" 2>/dev/null
  rm -f "$PIDFILE"
fi

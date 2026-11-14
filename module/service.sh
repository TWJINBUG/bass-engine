#!/system/bin/sh
MODDIR=${0%/*}
CONFIG=/sdcard/Android/data/com.bassengine.control/files/global_config.json
STATUS=/sdcard/Android/data/com.bassengine.control/files/global_status.json
PIDFILE=/data/adb/bassengine/daemon.pid
LOGFILE=/data/adb/bassengine/daemon.log
mkdir -p /data/adb/bassengine
if [ -f "$LOGFILE" ] && [ "$(stat -c%s "$LOGFILE" 2>/dev/null || echo 0)" -gt 262144 ]; then
  mv "$LOGFILE" "$LOGFILE.old"
fi
if [ -f "$PIDFILE" ]; then
  OLD=$(cat "$PIDFILE" 2>/dev/null)
  if [ -n "$OLD" ] && kill -0 "$OLD" 2>/dev/null; then
    exit 0
  fi
fi
CLASSPATH="$MODDIR/global-daemon.jar" /system/bin/app_process /system/bin com.bassengine.global.GlobalDaemon "$CONFIG" "$STATUS" >>"$LOGFILE" 2>&1 &
echo $! > "$PIDFILE"

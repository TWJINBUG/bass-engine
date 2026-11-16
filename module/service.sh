#!/system/bin/sh
MODDIR=${0%/*}
CONFIG=/sdcard/Android/data/com.bassengine.control/files/global_config.json
STATUS=/sdcard/Android/data/com.bassengine.control/files/global_status.json
PIDFILE=/data/adb/bassengine/daemon.pid
LOGFILE=/data/adb/bassengine/daemon.log
mkdir -p /data/adb/bassengine

# 模块自带控制 App：版本不一致就静默安装/升级
APP_APK="$MODDIR/app/BassEngine.apk"
APP_VER=7.3.1
if [ -f "$APP_APK" ]; then
  (
    for i in 1 2 3 4 5 6; do
      CUR=$(dumpsys package com.bassengine.control 2>/dev/null | sed -n 's/.*versionName=//p' | head -1)
      [ "$CUR" = "$APP_VER" ] && break
      cp "$APP_APK" /data/local/tmp/bassengine.apk
      chmod 644 /data/local/tmp/bassengine.apk
      pm install -r /data/local/tmp/bassengine.apk >/dev/null 2>&1
      rm -f /data/local/tmp/bassengine.apk
      sleep 8
    done
  ) &
fi
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

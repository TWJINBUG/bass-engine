#!/system/bin/sh
# BASS ENGINE 一键安装（root）：控制 App + KernelSU 模块
# 用法：su -c 'sh install.sh'
set -u

DIR=${0%/*}
APK=$(ls "$DIR"/dist/BASS-ENGINE-control-*.apk 2>/dev/null | head -1)
MODSRC="$DIR/module"
MODDST=/data/adb/modules/bassengine_global

echo "[1/3] 安装控制 App"
[ -n "$APK" ] || { echo "dist/ 里没有 APK"; exit 1; }
cp "$APK" /data/local/tmp/bassengine.apk
chmod 644 /data/local/tmp/bassengine.apk
pm install -r /data/local/tmp/bassengine.apk || { echo "安装失败"; exit 1; }

echo "[2/3] 安装 KernelSU 全局模块"
pkill -f com.bassengine.global.GlobalDaemon 2>/dev/null
sleep 1
rm -rf "$MODDST"
mkdir -p "$MODDST"
cp -r "$MODSRC/." "$MODDST/"
chmod 755 "$MODDST"/*.sh
chmod 644 "$MODDST"/global-daemon.jar "$MODDST"/module.prop "$MODDST"/README.txt
sh "$MODDST/service.sh"

echo "[3/3] 启动守护进程"
sleep 2
PID=$(cat /data/adb/bassengine/daemon.pid 2>/dev/null)
if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
  echo "守护进程已启动（PID $PID）。建议重启一次验证开机自启。"
else
  echo "守护进程没起来，看 /data/adb/bassengine/daemon.log"
fi
echo "完成。"

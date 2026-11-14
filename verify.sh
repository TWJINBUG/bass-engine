#!/system/bin/sh
# BASS ENGINE 全局 DSP 体检（需要 root）
F=/sdcard/Android/data/com.bassengine.control/files
echo "== 系统启动 $(cut -d' ' -f1 /proc/uptime)s"
echo "== 守护进程"
ps -A -o PID,ETIME,ARGS 2>/dev/null | grep -i GlobalDaemon | grep -v grep
echo "== 状态文件"
cat "$F/global_status.json" 2>/dev/null
echo
echo "== 最近日志"
tail -10 /data/adb/bassengine/daemon.log 2>/dev/null
echo "== session 0 效果"
dumpsys media.audio_flinger 2>/dev/null | grep "effects for session 0"
ID=$(cat "$F/global_status.json" 2>/dev/null | sed 's/.*"effectId":\([0-9-]*\).*/\1/')
if [ -n "$ID" ] && [ "$ID" != "-1" ]; then
  dumpsys media.audio_flinger 2>/dev/null | grep -A4 "Effect ID $ID:" | head -6
fi
echo "== 完成"

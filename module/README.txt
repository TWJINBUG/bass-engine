BASS ENGINE Global DSP (KernelSU module) v0.2.0

- Starts a root app_process daemon at boot (service.sh).
- Attaches system DynamicsProcessing to audio session 0, so every app shares
  the same DSP chain. No kernel, no partition, no /vendor changes.
- Reads  /sdcard/Android/data/com.bassengine.control/files/global_config.json
- Writes /sdcard/Android/data/com.bassengine.control/files/global_status.json
- Keeps the last applied config in /data/adb/bassengine/last_config.json, so
  DSP still runs right after reboot even before the app is opened.
- Only reads the config file when it changed; status heartbeat is 2 s.

Limits
- DynamicsProcessing offers EQ, multiband compression and a limiter only.
  Virtual bass and stereo width are approximated with EQ shaping; there is no
  true harmonic bass or mid/side widening.
- Stacks with other session-0 effects (OplusAudioX, JamesDSP). Disable JamesDSP
  when comparing, otherwise the two colour the sound twice.

Troubleshooting
- log: /data/adb/bassengine/daemon.log
- health: sh /sdcard/Download/F/audit/verify-global.sh
- stop:   sh /data/adb/modules/bassengine_global/uninstall.sh

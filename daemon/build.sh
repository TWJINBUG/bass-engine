#!/usr/bin/env bash
# Build global-daemon.jar (classes.dex + manifest) for the KernelSU module.
set -euo pipefail
cd "$(dirname "$0")"

SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}
ANDROID_JAR=$SDK/platforms/android-34/android.jar
D8=$(ls "$SDK"/build-tools/*/d8 2>/dev/null | sort -V | tail -1)

[ -f "$ANDROID_JAR" ] || { echo "missing $ANDROID_JAR"; exit 1; }
[ -n "$D8" ] || { echo "missing d8 in $SDK/build-tools"; exit 1; }

rm -rf out
mkdir -p out
javac -source 11 -target 11 -Xlint:-options \
  -classpath "$ANDROID_JAR" \
  -d out src/com/bassengine/global/GlobalDaemon.java

"$D8" --min-api 29 --output out out/com/bassengine/global/GlobalDaemon.class
( cd out && jar cf ../global-daemon.jar classes.dex )

cp global-daemon.jar ../module/global-daemon.jar
echo "built: $(pwd)/global-daemon.jar"

#!/usr/bin/env bash
# Chạy thử server dev với một phiên bản NeoForge, thành công khi server in ra "Done (".
# Dùng: scripts/smoke.sh 21.1.247
set -u
VER="$1"
LOG="server-$VER.log"
rm -rf run
mkdir -p run
echo "eula=true" > run/eula.txt
./gradlew runServer --console=plain -Pneo_version="$VER" > "$LOG" 2>&1 &
PID=$!
OK=1
for i in $(seq 1 90); do
  if grep -q 'Done (' "$LOG"; then echo "SERVER_STARTED_OK neoforge $VER"; OK=0; break; fi
  if grep -qiE 'Crash report|Failed to start|Mod loading has failed' "$LOG"; then echo "SERVER_CRASHED neoforge $VER"; break; fi
  sleep 5
done
kill $PID 2>/dev/null || true
pkill -f 'net.neoforged' 2>/dev/null || true
sleep 3
mkdir -p /tmp/logs
cp "$LOG" /tmp/logs/
cp -r run/crash-reports "/tmp/logs/crash-$VER" 2>/dev/null || true
grep -iE 'rbmk.*(error|exception|fail)|/FATAL|Caused by' "$LOG" | head -40 || true
exit $OK

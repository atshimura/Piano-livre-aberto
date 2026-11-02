#!/usr/bin/env bash
# One focused debug-only BackupArchiveSelfTest invocation; never the full suite.
set -euo pipefail
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -W -n br.org.pianolivre.opensource/br.org.pianolivre.MainActivity --ez backup_selftest true
mkdir -p build
for attempt in $(seq 1 20); do
  adb logcat -d -s PianoLivreBackup:I AndroidRuntime:E '*:S' > build/c2-backup-smoke.log
  if grep -q 'BACKUP_SELFTEST_PASS assertions=' build/c2-backup-smoke.log; then
    grep 'BACKUP_SELFTEST_PASS assertions=' build/c2-backup-smoke.log
    exit 0
  fi
  if grep -Eq 'BACKUP_SELFTEST_FAILED|FATAL EXCEPTION|Backup self-test failed' build/c2-backup-smoke.log; then
    cat build/c2-backup-smoke.log
    exit 1
  fi
  sleep 2
done
cat build/c2-backup-smoke.log
echo "Targeted backup self-test did not finish" >&2
exit 1

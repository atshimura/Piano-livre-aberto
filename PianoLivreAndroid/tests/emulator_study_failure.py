#!/usr/bin/env python3
"""Full-suite UI scenario, updated for Implementation 4. NOT run in basic CI.
Real OSMD render failure must show a detailed modal + private incident; no old renderer."""
import json,time
from emulator_ui import adb,dump,tap_text,assert_running,screenshot
PACKAGE='br.org.pianolivre.opensource'
adb('shell','am','force-stop',PACKAGE)
adb('logcat','-c')
adb('shell','am','start','-S','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity',
    '--ez','engraving_complex_selftest','true','--ez','study_failure_selftest','true')
for _ in range(90):
    assert_running()
    logs=adb('logcat','-d','-v','brief')
    if 'OSMD_FAILURE PL-NOT-RENDER' in logs:break
    time.sleep(.5)
else:raise AssertionError('IMPL4 injected OSMD failure did not reach structured bridge')
logs=adb('logcat','-d','-v','brief')
assert 'AUTO_TEMPORAL_FALLBACK' not in logs
assert 'TEMPORAL_RENDER_SUCCESS' not in logs
assert 'ACTIVE_RENDERER schematic' not in logs
def visible():
    # UIAutomator exposes some AlertDialog actions through accessibility
    # descriptions rather than the text attribute (Android API/theme dependent).
    return [value for n in dump().iter('node') for value in
            (n.attrib.get('text',''),n.attrib.get('content-desc','')) if value]
for _ in range(30):
    labels=visible()
    if any('Não foi possível exibir a partitura' in s for s in labels):break
    time.sleep(.4)
else:raise AssertionError('IMPL4 OSMD error dialog was not shown')
assert any('PL-NOT-RENDER' in s for s in labels),'Stable code absent from error dialog'
assert any('preservados' in s for s in labels),'Preservation statement missing from dialog'
assert any('copiar diagnóstico' in s.casefold() for s in labels), 'Copy diagnosis action missing: '+repr(labels[-35:])
screenshot('piano-study-failure-osmd-dialog.png')
report=''
for _ in range(30):
    report=adb('exec-out','run-as',PACKAGE,'cat','files/notation-errors.json')
    if 'PL-NOT-RENDER' in report:break
    time.sleep(.3)
else:raise AssertionError('IMPL4 structured error was not persisted privately')
records=json.loads(report)
assert 1<=len(records)<=50,'Local incident ring exceeded bound'
assert any(x.get('code')=='PL-NOT-RENDER' and x.get('xmlSha256') for x in records)
assert 'score-partwise' not in report and '<note>' not in report,'Original MusicXML leaked to diagnostics'
before=logs.count('OSMD_RENDERED generation=')
tap_text(['Tentar novamente'])
for _ in range(80):
    assert_running()
    logs=adb('logcat','-d','-v','brief')
    if logs.count('OSMD_RENDERED generation=')>before:break
    time.sleep(.4)
else:raise AssertionError('IMPL4 explicit retry did not recover OSMD')
assert 'AUTO_TEMPORAL_FALLBACK' not in logs
assert 'TEMPORAL_RENDER_SUCCESS' not in logs
print('IMPL4_OSMD_ERROR_MODAL_PRIVATE_LOG_RETRY_PASS',flush=True)

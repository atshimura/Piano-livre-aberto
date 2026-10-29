#!/usr/bin/env python3
"""Force a real OSMD exception; verify automatic fallback without a selector."""
import time
from emulator_ui import adb,dump,tap_text,assert_running,screenshot
PACKAGE='br.org.pianolivre.opensource'
adb('shell','am','force-stop',PACKAGE)
adb('logcat','-c')
adb('shell','am','start','-S','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity',
    '--ez','engraving_complex_selftest','true','--ez','study_failure_selftest','true')
for _ in range(60):
    assert_running()
    if 'TEMPORAL_RENDER_SUCCESS' in adb('logcat','-d','-v','brief'):break
    time.sleep(.5)
else:raise AssertionError('Real OSMD failure did not automatically render temporal fallback')
logs=adb('logcat','-d','-v','brief')
assert 'AUTO_TEMPORAL_FALLBACK' in logs,'Fallback bypassed the actual OSMD failure'
assert not any(n.attrib.get('text') in ('OSMD · nova','Anterior · temporal','Esquemático · ativo') for n in dump().iter('node')),'Manual selector returned'
screenshot('piano-study-failure.png')
assert_running()
adb('shell','am','force-stop',PACKAGE);adb('logcat','-c')
adb('shell','am','start','-S','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity',
    '--ez','engraving_complex_selftest','true','--ez','study_failure_selftest','true',
    '--ez','study_native_failure_selftest','true')
for _ in range(60):
    assert_running()
    if 'ACTIVE_RENDERER schematic' in adb('logcat','-d','-v','brief'):break
    time.sleep(.5)
else:raise AssertionError('Both renderer failures did not activate disclosed native schematic fallback')
screenshot('piano-study-native-failure.png')
assert not any(n.attrib.get('text') in ('OSMD · nova','Anterior · temporal','Teclado guia') for n in dump().iter('node'))
print('STUDY_RENDERER_FAILURE_UI_PASS',flush=True)

#!/usr/bin/env python3
"""Force a real OSMD exception in debug WebView, verify native fallback/retry UI."""
import time
from emulator_ui import adb,dump,tap_text,assert_running,screenshot
PACKAGE='br.org.pianolivre.opensource'
adb('shell','am','force-stop',PACKAGE)
adb('logcat','-c')
adb('shell','am','start','-S','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity',
    '--ez','engraving_complex_selftest','true','--ez','study_failure_selftest','true')
for _ in range(60):
    assert_running()
    if 'ACTIVE_RENDERER schematic' in adb('logcat','-d','-v','brief'):break
    time.sleep(.5)
else:raise AssertionError('Real OSMD failure did not activate native schematic fallback')
assert any(n.attrib.get('text')=='Esquemático · ativo' for n in dump().iter('node')),'Selector still claims OSMD/temporal'
screenshot('piano-study-failure.png')
tap_text('Esquemático · ativo')
for _ in range(40):
    assert_running()
    if 'TEMPORAL_RENDER_SUCCESS' in adb('logcat','-d','-v','brief'):break
    time.sleep(.5)
else:raise AssertionError('Temporal retry from schematic fallback failed')
assert any(n.attrib.get('text')=='Anterior · temporal' for n in dump().iter('node')),'Temporal renderer label not restored'
print('STUDY_RENDERER_FAILURE_UI_PASS',flush=True)

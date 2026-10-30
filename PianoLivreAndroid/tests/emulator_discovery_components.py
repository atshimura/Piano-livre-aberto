#!/usr/bin/env python3
"""Same production component regression in focused/full CI; unmodified PNG bytes."""
import hashlib,json,re,struct,subprocess,sys,time
from pathlib import Path
from emulator_ui import adb
P='br.org.pianolivre.opensource'
adb('shell','am','force-stop',P);adb('logcat','-c')
adb('shell','am','start','-W','-n',P+'/br.org.pianolivre.MainActivity','--ez','discovery_selftest','true')
deadline=time.monotonic()+180
while time.monotonic()<deadline:
    log=adb('logcat','-d','-v','brief')
    assert 'FATAL EXCEPTION' not in log,log[-8000:]
    if 'DISCOVERY_UI_SELFTEST_PASS' in log:break
    time.sleep(.5)
else:raise AssertionError('Descobrir component regression did not complete')
Path('piano-discovery-selftest.log').write_text(log,encoding='utf-8')
assert 'DISCOVERY_UI_SELFTEST_BEGIN' in log
density=adb('shell','wm','density');values=re.findall(r'(?:Physical|Override) density:\s*(\d+)',density)
assert values,density
scale=int(values[-1])/160
names=['c2','c3','f3','c4','eb4','g4','c6','low-chord','high-chord','grand-chord','grand-sequence','c-major','phone-sequence','tablet-sequence']
if '--representative' in sys.argv:names=['grand-chord','phone-sequence']
report=[]
for name in names:
    data=subprocess.check_output(['adb','exec-out','run-as',P,'cat','cache/discovery-staff-'+name+'.png'])
    assert data[:8]==b'\x89PNG\r\n\x1a\n',name
    width,height=struct.unpack('>II',data[16:24])
    expected=(910,420) if name=='phone-sequence' else (910,600) if name=='tablet-sequence' else (640,300)
    assert (width,height)==tuple(int(v*scale+.5) for v in expected),(name,width,height,expected,scale)
    filename='piano-discovery-staff-'+name+'.png';Path(filename).write_bytes(data)
    report.append(dict(name=filename,width=width,height=height,sha256=hashlib.sha256(data).hexdigest()))
Path('piano-discovery-components.json').write_text(json.dumps(dict(checkout=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),images=report),indent=2),encoding='utf-8')
print('DISCOVERY_COMPONENT_FRAMES_PASS copied='+str(len(report))+' exact_viewports=true',flush=True)

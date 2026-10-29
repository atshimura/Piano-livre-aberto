#!/usr/bin/env python3
"""Independent display profiles, without one-shot self-test launch extras.
A display override recreates Android activities. Initial upgrade/backup test
intents are deliberately absent here; their fixture lifetime ends in UI tests.
"""
import re,time,json
from pathlib import Path
from emulator_ui import adb,dump,tap_text,assert_running,responsive_evidence
PACKAGE='br.org.pianolivre.opensource'
original=adb('shell','wm','size')
previous=re.search(r'Override size: (\d+x\d+)',original)
try:
    for label,size in (('taller','1440x2160'),('tablet-display','1600x2560')):
        adb('shell','am','force-stop',PACKAGE)
        adb('shell','wm','size',size)
        adb('logcat','-c')
        adb('shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity')
        time.sleep(3);assert_running()
        if any('← Biblioteca' in (n.attrib.get('text','')+n.attrib.get('content-desc','')) for n in dump().iter('node')):
            tap_text(['Biblioteca'],scroll=True)
        tap_text(['Gymnopédie nº 1'],scroll=True)
        time.sleep(2)
        buttons=[n for n in dump().iter('node') if n.attrib.get('text')=='Espera']
        assert buttons,'Study mode controls are missing on display profile'
        ready=None
        for attempt in range(40):
            rows=[line for line in adb('logcat','-d','-v','brief').splitlines() if 'POC_OSMD_READY {' in line]
            # Other diagnostic journeys may leave a different ready payload in
            # the emulator log. Wait for the requested long score's complete
            # written-note mapping instead of accepting the first score event.
            for row in reversed(rows):
                candidate=json.loads(row.split('POC_OSMD_READY ',1)[1].split(' @',1)[0])
                if candidate.get('graphicalNotes')==469 and candidate.get('midiMapping',{}).get('matched')==469:
                    ready=candidate
                    break
            if ready:break
            time.sleep(.5)
        assert ready and ready['graphicalNotes']==469 and ready['midiMapping']['matched']==469,'Display profile did not render all written heads'
        assert ready['viewportMaxScrollTop']==0,'Display profile exposes vertical score scrolling'
        assert ready['height']<=ready['availableHeight']+0.5,'Both staves do not fit the display profile'
        assert abs(ready['displayScale']-ready['availableHeight']/ready['nativeSvgHeight'])<0.002,'Display profile used non-uniform scale'
        Path('piano-03d-'+label+'-mapping.json').write_text(json.dumps(ready,indent=2),encoding='utf-8')
        responsive_evidence('gymnopedie-'+label)
        print('PASS: proportional two-staff fit on independent responsive display '+label+' size='+size,flush=True)
finally:
    adb('shell','am','force-stop',PACKAGE)
    adb('shell','wm','size',previous.group(1) if previous else 'reset')

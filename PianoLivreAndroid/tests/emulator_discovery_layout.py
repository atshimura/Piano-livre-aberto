#!/usr/bin/env python3
"""Real Android frames; synthetic MIDI. Focused UI coverage, not full regression."""
import json,time,subprocess
from pathlib import Path
from emulator_ui import adb,screenshot
PACKAGE='br.org.pianolivre.opensource'
reports=[]
try:
    for profile,size in [('phone','1280x720'),('tablet','1920x1080')]:
        adb('shell','am','force-stop',PACKAGE)
        adb('shell','wm','size',size);adb('shell','wm','density','240')
        adb('shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity','--ez','discovery_layout_selftest','true')
        for attempt in range(120):
            process=subprocess.run(['adb','shell','pidof',PACKAGE],capture_output=True,text=True)
            if process.returncode==0:
                pid=process.stdout.strip();break
            assert process.returncode==1,process.stderr
            log=adb('logcat','-d','-v','brief')
            assert 'FATAL EXCEPTION' not in log,log[-7000:]
            time.sleep(.25)
        else:raise AssertionError('Activity did not start after display configuration')
        seen=set()
        for attempt in range(600):
            log=adb('logcat','-d','--pid='+pid,'-v','brief')
            assert 'FATAL EXCEPTION' not in log,log[-7000:]
            rows=[json.loads(line.split('DISCOVERY_LAYOUT_STAGE ',1)[1]) for line in log.splitlines() if 'DISCOVERY_LAYOUT_STAGE ' in line]
            if rows and rows[-1]['stage'] not in seen:
                row=rows[-1];stage=row['stage'];seen.add(stage)
                screenshot('piano-discovery-layout-'+profile+'-'+stage+'.png')
                row['profile']=profile;reports.append(row)
                adb('shell','run-as',PACKAGE,'touch','cache/discovery-next')
            if 'DISCOVERY_LAYOUT_PASS' in log:break
            time.sleep(.25)
        else:raise AssertionError('Descobrir focused test timed out: '+log[-7000:])
        assert len(seen)==9,seen
        Path('piano-discovery-layout-'+profile+'.log').write_text(log,encoding='utf-8')
    Path('piano-discovery-layout.json').write_text(json.dumps(reports,indent=2),encoding='utf-8')
    print('DISCOVERY_FOCUSED_UI_PASS profiles=2 states=18 synthetic_midi=true',flush=True)
except BaseException:
    Path('piano-discovery-layout-failure.log').write_text(adb('logcat','-d','-v','brief'),encoding='utf-8')
    screenshot('piano-discovery-layout-failure.png')
    raise
finally:
    adb('shell','am','force-stop',PACKAGE);adb('shell','wm','size','reset');adb('shell','wm','density','reset')

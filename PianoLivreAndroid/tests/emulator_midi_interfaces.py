#!/usr/bin/env python3
"""Production connection owner + synthetic ports, real Android screens and captures."""
import json,time,subprocess,struct
from pathlib import Path
from emulator_ui import adb,screenshot,dump,reveal_study_controls,current_study_layout
PACKAGE='br.org.pianolivre.opensource'
stages=['home-disconnected','home-opening','home-connected','study-connected','study-hidden','study-restored','study-disconnected','study-unnamed','study-switched','home-reconnected','home-background-return']
reports=[]
def logs():
    try:pid=adb('shell','pidof',PACKAGE).strip()
    except subprocess.CalledProcessError as error:
        if error.returncode==1:return '' # Cold start may not have a PID yet; wait_stage remains bounded.
        raise
    return adb('logcat','-d','--pid='+pid,'-v','brief')
def wait_stage(stage):
    for attempt in range(160):
        text=logs()
        assert 'FATAL EXCEPTION' not in text,text[-5000:]
        records=[json.loads(line.split('PROMPT03_STAGE_READY ',1)[1]) for line in text.splitlines() if 'PROMPT03_STAGE_READY ' in line]
        if records and records[-1]['stage']==stage:return records[-1]
        time.sleep(.25)
    raise AssertionError('MIDI stage missing: '+stage)
def acknowledge(value):
    adb('shell','run-as',PACKAGE,'sh','-c',"'echo "+value+" > cache/prompt03-next'")
def rendered_frame(stage,record):
    layout=current_study_layout();height=layout['bounds'][3]-layout['bounds'][1]
    for attempt in range(60):
        rows=[line for line in logs().splitlines() if 'POC_SVG_FIT {' in line]
        if rows:
            fit=json.loads(rows[-1].split('POC_SVG_FIT ',1)[1].split(' @',1)[0])
            if abs(fit['viewportHeight']-height/layout['density'])<2:break
        time.sleep(.1)
    else:raise AssertionError('Rendered score did not reach the native viewport: '+stage)
    assert fit['viewportMaxScrollTop']==0 and fit['verticalOverflow']==0
    assert abs(fit['scale']-fit['availableHeight']/fit['rawHeight'])<.002
    acknowledge('frame-'+stage)
    for attempt in range(60):
        if 'PROMPT03_FRAME_READY '+stage in logs():break
        time.sleep(.1)
    else:raise AssertionError('WebView did not commit its visual state: '+stage)
    record.update(heightPx=height,heightDp=height/layout['density'],geometry=fit,transportBounds=layout['transportBounds'])
def assert_staff_ink(record):
    # Inspect real framebuffer pixels, independently of DOM/geometry diagnostics.
    # The Ode fixture has two five-line staves spanning the visible viewport.
    data=subprocess.check_output(['adb','exec-out','screencap'])
    width,height,pixel_format=struct.unpack('<III',data[:12])
    assert pixel_format in (1,2),'Unexpected emulator framebuffer format'
    header=len(data)-width*height*4
    assert header in (12,16),'Unexpected framebuffer stride/header'
    box=current_study_layout()['bounds'];x0=max(0,box[0]+(box[2]-box[0])//50);x1=min(width,box[2]-(box[2]-box[0])//50)
    samples=list(range(x0,x1,3));bands=0;was_line=False
    for y in range(max(0,box[1]),min(height,box[3])):
        dark=sum(max(data[header+4*(y*width+x):header+4*(y*width+x)+3])<140 for x in samples)
        is_line=dark>=len(samples)*.85
        if is_line and not was_line:bands+=1
        was_line=is_line
    assert bands>=10,('Incomplete painted grand staff',record['stage'],bands)
    record['paintedStaffBands']=bands
try:
    for label,size,normal,hidden in [('phone','1280x720',417,567),('tablet','1920x1080',777,927)]:
        adb('shell','am','force-stop',PACKAGE);adb('shell','wm','size',size);adb('shell','wm','density','240')
        adb('shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity','--ez','midi_connection_selftest','true')
        measurements=[]
        for stage in stages:
            record=wait_stage(stage)
            assert record['simulated'] is True
            if stage=='study-restored':assert record['running'] and record['controlsVisible']
            if stage=='study-hidden':
                for attempt in range(40):
                    layout=current_study_layout()
                    if not layout['headerVisible'] and not layout['modesVisible'] and layout['bounds'][3]-layout['bounds'][1]>=hidden:break
                    time.sleep(.1)
                assert not layout['headerVisible'] and not layout['modesVisible']
                assert layout['bounds'][3]-layout['bounds'][1]>=hidden,'Prompt 2 hidden score area regressed'
            elif stage.startswith('study-'):
                layout=current_study_layout();assert layout['headerVisible'] and layout['modesVisible']
                assert layout['bounds'][3]-layout['bounds'][1]>=normal,'Prompt 2 normal score area regressed'
            if stage in ('home-disconnected','home-opening','study-disconnected','study-unnamed'):assert record['label']=='USB-MIDI'
            else:assert record['label'].startswith('SIMULADO'),record
            # Continuous animation is inspected with native layout snapshots; stopped views use accessibility.
            if not record['running']:
                tree=dump();buttons=[n for n in tree.iter('node') if n.attrib.get('content-desc','').startswith('Gerenciar')]
                assert buttons and any(n.attrib.get('clickable')=='true' for n in buttons),'MIDI button inaccessible'
            if stage.startswith('study-'):
                rendered_frame(stage,record);assert_staff_ink(record)
            screenshot('piano-prompt03-'+label+'-'+stage+'.png');measurements.append(record)
            if stage=='study-hidden':reveal_study_controls()
            if stage=='study-connected':
                # Actual management is reused for synthetic state too, with full metadata.
                from emulator_ui import tap_text
                tap_text(['Gerenciar MIDI. Conectado a '+record['label']],exact=True)
                tree=dump();texts=[n.attrib.get('text','') for n in tree.iter('node')]
                assert any(record['label'] in value for value in texts),'Full instrument name missing in diagnostics'
                screenshot('piano-prompt03-'+label+'-diagnostics.png');adb('shell','input','keyevent','4')
            acknowledge(stage)
        for attempt in range(40):
            if 'MIDI_INTERFACE_SELFTEST_PASS' in logs():break
            time.sleep(.25)
        assert 'MIDI_INTERFACE_SELFTEST_PASS' in logs()
        Path('piano-prompt03-'+label+'.log').write_text(logs(),encoding='utf-8')
        reports.append(dict(viewport=size,simulated=True,states=measurements))
except BaseException:
    screenshot('piano-prompt03-failure.png')
    Path('piano-prompt03-failure.log').write_text(adb('logcat','-d','-v','brief'),encoding='utf-8')
    raise
finally:
    adb('shell','am','force-stop',PACKAGE);adb('shell','wm','size','reset');adb('shell','wm','density','reset')
Path('piano-prompt03-midi-interfaces.json').write_text(json.dumps(reports,indent=2),encoding='utf-8')
print('MIDI_INTERFACES_EMULATOR_PASS '+json.dumps(reports),flush=True)

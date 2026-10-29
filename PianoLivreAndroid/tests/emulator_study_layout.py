#!/usr/bin/env python3
"""Real UI comparisons with the pinned Prompt 1 baseline and all three states."""
import json,time,re
from pathlib import Path
from emulator_ui import adb,dump,tap_text,home_scroll_to_top,responsive_evidence,assert_study_controls,current_study_layout
PACKAGE='br.org.pianolivre.opensource'
report=[]
def records(marker):
    pid=adb('shell','pidof',PACKAGE).strip()
    return [json.loads(line.split(marker,1)[1].split(' @',1)[0]) for line in adb('logcat','-d','--pid='+pid,'-v','brief').splitlines() if marker in line]
def hidden_note_tap():
    # Wait is deliberately motionless after preparation, without requiring USB.
    adb('logcat','-c');tap_text(['Iniciar'])
    target=None
    for attempt in range(40):
        targets=records('STUDY_TOUCH_TARGET ')
        if targets:target=targets[-1];break
        time.sleep(.4)
    assert target,'Real hidden-state graphical note target missing after Wait preparation'
    before=records('STUDY_CHROME_STATE ')[-1]
    assert not before['visible'] and before['waitStarted']
    layout=current_study_layout();assert layout and not layout['headerVisible']
    box=layout['bounds']
    x=box[0]+round(target['x']*(box[2]-box[0])/target['webviewWidth'])
    y=box[1]+round(target['y']*(box[3]-box[1])/target['viewportHeight'])
    adb('shell','input','tap',str(x),str(y));time.sleep(.5)
    assert_study_controls()
    taps=records('POC_NOTE_TAP ')
    assert taps and taps[-1]['mapped'] and taps[-1]['nativeMidi']==target['midi'],'Reveal consumed or changed the note hit'
    after=records('STUDY_CHROME_STATE ')[-1]
    assert after['visible']
    for key in ('beat','mode','hand','tempo','loopStart','loopEnd','midiGeneration','running','waitStarted','sourceHash'):
        assert before[key]==after[key],('Reveal changed musical state',key,before,after)
    tap_text(['Parar'])
    return dict(before=before,after=after,note=taps[-1])

def restored_note_tap(proof,label):
    # Reuse the original graphical target: returning to the same beat/height
    # must restore the same coordinate system, rather than accepting a new one.
    target=proof['firstNoteViewport'];assert target
    root=dump();view=next(n for n in root.iter('node') if n.attrib.get('class')=='android.webkit.WebView')
    box=[int(v) for v in re.findall(r'\d+',view.attrib['bounds'])]
    x=box[0]+round(target['x']*(box[2]-box[0])/proof['webviewWidth'])
    y=box[1]+round(target['y']*(box[3]-box[1])/proof['viewportHeight'])
    before=len(records('POC_NOTE_TAP '));adb('shell','input','tap',str(x),str(y));time.sleep(.25)
    screenshot_name='piano-prompt02-note-restored-'+label+'.png'
    from emulator_ui import screenshot
    screenshot(screenshot_name)
    taps=records('POC_NOTE_TAP ')
    assert len(taps)>before and taps[-1]['mapped'] and taps[-1]['nativeMidi']==target['midiEstimate'] and taps[-1]['staff']==1,'Restored/rewound score changed the original note target'
    return taps[-1]
try:
    for label,size in [('phone','1280x720'),('tablet','1920x1080')]:
        adb('shell','am','force-stop',PACKAGE);adb('shell','wm','size',size);adb('shell','wm','density','240')
        adb('shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity');time.sleep(2)
        labels=[n.attrib.get('text','') for n in dump().iter('node')]
        if not any('Bem-vindo ao' in text for text in labels):tap_text(['Biblioteca'],scroll=True)
        home_scroll_to_top();tap_text(['Começar pela Ode à Alegria'],scroll=True);time.sleep(3)
        assert_study_controls()
        braces=records('POC_BRACE_FIT ')
        assert braces and braces[-1]['width']>0 and braces[-1]['left']>=3.5,'Grand-staff brace clipped at left edge'
        assert braces[-1]['occludingMasks']==0,'Staff-band masks cover the grand-staff brace'
        proof=records('POC_OSMD_READY ')[-1]
        responsive_evidence('prompt02-'+label)
        normal_tap=restored_note_tap(proof,label)
        before=json.loads(Path('piano-prompt02-before-'+label+'.json').read_text())
        after=json.loads(Path('piano-03d-prompt02-'+label+'.json').read_text())
        assert after[0]['heightPx']>before['heightPx'],'No measured gain over Prompt 1 in '+label
        report.append(dict(viewport=size,before=before,after=after,gainNormalPx=after[0]['heightPx']-before['heightPx'],gainPlayingPx=after[1]['heightPx']-before['heightPx'],restoredNoteTap=normal_tap))
        report[-1]['hiddenNoteTap']=hidden_note_tap()
        # The actual MIDI manager remains accessible, without pretending a USB device exists.
        tap_text(['USB-MIDI'],exact=True);time.sleep(.3);adb('shell','input','keyevent','4')
        tap_text(['Partitura Livre']);time.sleep(2)
        labels=[n.attrib.get('text','') for n in dump().iter('node')]
        assert not any(text in ('Espera','Contínuo','Demonstração') for text in labels)
        tap_text(['Biblioteca'],scroll=True)
finally:
    adb('shell','am','force-stop',PACKAGE);adb('shell','wm','size','reset');adb('shell','wm','density','reset')
Path('piano-prompt02-comparison.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print('STUDY_PROMPT02_LAYOUT_PASS '+json.dumps(report),flush=True)

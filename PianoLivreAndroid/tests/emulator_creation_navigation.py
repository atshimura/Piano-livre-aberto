#!/usr/bin/env python3
"""Criação navigation smoke on phone/tablet landscape; no physical MIDI required."""
import subprocess,time,re
from emulator_ui import adb,wait_for_text,tap_text,screenshot,dump

P='br.org.pianolivre.opensource'
try:
    for form,size in [('phone-landscape','1280x720'),('tablet-landscape','1920x1080')]:
        adb('shell','am','force-stop',P)
        adb('shell','wm','size',size);adb('shell','wm','density','240')
        adb('shell','am','start','-W','-n',P+'/br.org.pianolivre.MainActivity')
        # Other emulator checks may persist the last route (for example Study).
        # Android Back returns from that screen to the real app home.
        for _ in range(4):
            labels=[n.attrib.get('text','') or n.attrib.get('content-desc','') for n in dump().iter('node')]
            if any('Bem-vindo ao' in value for value in labels):
                break
            adb('shell','input','keyevent','4')
            time.sleep(.3)
        wait_for_text('Bem-vindo ao',timeout=30)
        tap_text(['Biblioteca'],scroll=True)
        tap_text(['Laboratório Musical'],exact=True,scroll=True)
        wait_for_text('toque uma nota',timeout=30)
        tap_text(['Criação'],exact=True)
        wait_for_text('Criação Musical',timeout=20)
        home=dump()
        def bounds(node):
            values=[int(x) for x in re.findall(r'\d+',node.attrib.get('bounds',''))]
            assert len(values)==4,('Missing creation action bounds',node.attrib)
            return values
        def action(label):
            hits=[n for n in home.iter('node') if n.attrib.get('class')=='android.widget.Button'
                  and label in (n.attrib.get('text','')+' '+n.attrib.get('content-desc',''))]
            assert len(hits)==1,('Missing/duplicated creation shortcut',label,len(hits))
            return bounds(hits[0])
        record_box=action('Nova gravação')
        projects_box=action('Minhas Criações')
        assert record_box[1]==projects_box[1] and record_box[3]==projects_box[3],('Creation shortcuts must share a row',form,record_box,projects_box)
        assert abs((record_box[2]-record_box[0])-(projects_box[2]-projects_box[0]))<=2,('Creation shortcuts unequal width',form)
        assert record_box[2]<=projects_box[0],('Creation shortcuts overlap',form)
        screenshot('piano-creation-'+form+'-home.png')
        tap_text(['Minhas Criações'],exact=True)
        wait_for_text('Nenhuma composição encontrada',timeout=10)
        adb('shell','input','keyevent','4')
        wait_for_text('Criação Musical',timeout=10)
        tap_text(['＋  Nova gravação'])
        wait_for_text('Nova gravação',timeout=10)
        adb('shell','input','keyevent','4')
        wait_for_text('Criação Musical',timeout=10)
        adb('shell','input','keyevent','4')
        wait_for_text('Bem-vindo ao',timeout=10)
        screenshot('piano-creation-'+form+'-back.png')
        print('CREATION_NAV_PASS '+form,flush=True)
finally:
    adb('shell','am','force-stop',P)
    adb('shell','wm','size','reset');adb('shell','wm','density','reset')

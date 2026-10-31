#!/usr/bin/env python3
"""Four representative navigation frames; does not replace emulator_ui.py."""
import time
from emulator_ui import adb,dump,tap_text,screenshot,assert_study_controls,wait_for_text
P='br.org.pianolivre.opensource'
adb('shell','wm','size','1280x720');adb('shell','wm','density','240')
adb('shell','am','start','-W','-n',P+'/br.org.pianolivre.MainActivity')
wait_for_text('Bem-vindo ao',timeout=30);screenshot('piano-fast-home.png')
tap_text(['Começar pela Ode à Alegria'],scroll=True);time.sleep(3);assert_study_controls();screenshot('piano-fast-study.png')
tap_text(['Biblioteca'],scroll=True);tap_text(['Laboratório Musical'],exact=True,scroll=True)
wait_for_text('toque uma nota',timeout=30);screenshot('piano-fast-discovery.png')
def lab_tabs(active):
    nodes=list(dump().iter('node'))
    def find(name):
        matches=[n for n in nodes if n.attrib.get('class')=='android.widget.Button' and n.attrib.get('text')==name]
        assert len(matches)==1,('Missing or duplicated Laboratory tab',name,len(matches))
        return matches[0]
    d,l,c=(find(name) for name in ('Descobrir','Aprender','Criação'))
    assert l.attrib.get('enabled')=='false','Aprender must remain unavailable'
    assert active in (d.attrib.get('content-desc') if active=='Descobrir' else c.attrib.get('content-desc')),'Selected Laboratory tab missing'
    assert (d if active=='Descobrir' else c).attrib.get('selected')=='true','Wrong Laboratory tab selected'
    assert any(n.attrib.get('content-desc')=='Voltar' for n in nodes),'Shared Back action missing'
    return [n.attrib['bounds'] for n in (d,l,c)]
discover_bounds=lab_tabs('Descobrir')
tap_text(['Criação'],exact=True);wait_for_text('Criação Musical',timeout=30)
creation_bounds=lab_tabs('Criação')
assert discover_bounds==creation_bounds,('Laboratory bar moved between screens',discover_bounds,creation_bounds)
screenshot('piano-fast-creation.png')
tap_text(['Descobrir'],exact=True);wait_for_text('toque uma nota',timeout=30)
assert lab_tabs('Descobrir')==discover_bounds,'Laboratory bar changed after roundtrip'
tap_text(['Voltar'],exact=True);wait_for_text('Bem-vindo ao',timeout=30);screenshot('piano-fast-return.png')
print('FAST_NAVIGATION_PASS captures=5 scope=home-study-discovery-creation-shared-navigation',flush=True)

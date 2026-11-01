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
# Minimalist Creation smoke: home -> searchable library -> recorder -> home.
tap_text(['Minhas Criações'],exact=True)
nodes=list(dump().iter('node'))
assert any(n.attrib.get('content-desc')=='Filtrar composições' for n in nodes),'Creation library filter missing'
assert any(n.attrib.get('content-desc')=='Ordenar composições' for n in nodes),'Creation library sort missing'
screenshot('piano-fast-creation-library.png')
tap_text(['Voltar'],exact=True);wait_for_text('Criação Musical',timeout=20)
tap_text(['＋  Nova gravação'],exact=True);wait_for_text('Nova gravação',timeout=20)
buttons=[n.attrib.get('text','') for n in dump().iter('node') if n.attrib.get('class')=='android.widget.Button']
assert any('Gravar' in n for n in buttons),'Creation record command missing'
screenshot('piano-fast-creation-record-ready.png')
tap_text(['Voltar'],exact=True);wait_for_text('Criação Musical',timeout=20)
tap_text(['Descobrir'],exact=True);wait_for_text('toque uma nota',timeout=30)
assert lab_tabs('Descobrir')==discover_bounds,'Laboratory bar changed after roundtrip'
tap_text(['Voltar'],exact=True);wait_for_text('Bem-vindo ao',timeout=30);screenshot('piano-fast-return.png')
print('FAST_NAVIGATION_PASS captures=7 scope=home-study-discovery-creation-library-record-ready-shared-navigation',flush=True)

# Regression: a saved study session must never hijack a fresh launcher start.
tap_text(['Começar pela Ode à Alegria'],scroll=True)
wait_for_text('Espera',timeout=30)
before=adb('shell','run-as',P,'cat','shared_prefs/piano_data.xml')
assert 'user_0_file' in before and 'ode-to-joy' in before, 'Study session was not persisted before cold start'
adb('shell','am','force-stop',P)
adb('shell','am','start','-W','-n',P+'/br.org.pianolivre.MainActivity')
wait_for_text('Bem-vindo ao',timeout=30)
labels=[n.attrib.get('text','') for n in dump().iter('node')]
assert not ('Espera' in labels or 'Contínuo' in labels), 'Study controls shown instead of Home after restart'
after=adb('shell','run-as',P,'cat','shared_prefs/piano_data.xml')
# Compare the original pref lines rather than whole XML: app may reorder tags.
before_saved=[line.strip() for line in before.splitlines() if 'name="user_0_' in line]
after_saved=[line.strip() for line in after.splitlines() if 'name="user_0_' in line]
assert before_saved and sorted(before_saved)==sorted(after_saved), 'Saved study session changed when opening Home'
screenshot('piano-fast-relaunch-home.png')
tap_text(['Começar pela Ode à Alegria'],scroll=True)
wait_for_text('Espera',timeout=30)
assert_study_controls()
print('STARTUP_HOME_REGRESSION_PASS previous_score=true saved_preferences_unchanged=true explicit_study=true',flush=True)

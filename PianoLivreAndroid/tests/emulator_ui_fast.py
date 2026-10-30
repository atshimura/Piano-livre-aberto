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
tap_text(['Voltar'],exact=True);wait_for_text('Bem-vindo ao',timeout=30);screenshot('piano-fast-return.png')
print('FAST_NAVIGATION_PASS captures=4 scope=home-study-discovery-navigation',flush=True)

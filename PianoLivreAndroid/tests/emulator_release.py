#!/usr/bin/env python3
"""Upgrade the synthetic installation to non-debuggable release, then verify UI.
PR jobs use the runner certificate; trusted-main jobs use the permanent certificate.
No hardware claims.
"""
import json,pathlib,subprocess,time,tempfile
from upgrade_fixture import synthetic_upgrade_preferences
from emulator_ui import adb,dump,tap_text,screenshot,assert_running,wait_for_text,home_scroll_to_top
PACKAGE='br.org.pianolivre.opensource'
apk='app/build/outputs/apk/release/app-release.apk'
adb('shell','am','force-stop',PACKAGE)
# Restore only the known public MXL fixture removed by the preceding UI test.
adb('push','app/src/main/assets/scores/ode-to-joy-ludwig-van-beethoven.mxl','/data/local/tmp/release-upgrade.mxl')
adb('shell','run-as',PACKAGE,'mkdir','-p','files/imported')
adb('shell','run-as',PACKAGE,'cp','/data/local/tmp/release-upgrade.mxl','files/imported/upgrade.mxl')
with tempfile.TemporaryDirectory(prefix='piano-release-upgrade-') as temporary:
    prefs=pathlib.Path(temporary)/'upgrade-prefs.xml'
    prefs.write_text(synthetic_upgrade_preferences(),encoding='utf-8')
    adb('push',str(prefs),'/data/local/tmp/release-upgrade-prefs.xml')
    adb('shell','run-as',PACKAGE,'mkdir','-p','shared_prefs')
    adb('shell','run-as',PACKAGE,'cp','/data/local/tmp/release-upgrade-prefs.xml','shared_prefs/piano_data.xml')
adb('install','-r',apk)
flags=['creation_export_selftest','creation_selftest','creation_reopen_selftest','home_layout_selftest','discovery_layout_selftest','discovery_selftest','backup_selftest','profiles_selftest','library_selftest','history_selftest','midifile_selftest','xml_input_selftest','upgrade_selftest','midi_selftest','midi_connection_selftest','midi_import_ui_selftest','study_temporal_selftest','study_failure_selftest','study_native_failure_selftest']
args=['shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity']
for flag in flags:args.extend(['--ez',flag,'true'])
adb(*args);assert_running()
# Since 0.9.60 every cold start MUST present Home, even for a saved score.
# A 100%/80% speed label is a Study control and must NOT be required on Home.
wait_for_text('Bem-vindo ao',timeout=30)
home=[n.attrib.get('text','') for n in dump().iter('node')]
assert not any(t in ('Espera','Contínuo','Demonstração') for t in home), 'Release unexpectedly reopened Study on cold launch'
assert any('Upgrade B' in t for t in home), 'Release lost active profile ID 7 on Home'
assert not any('Teclado guia' in t or 'Ocultar teclado' in t for t in home), 'Removed guide returned on Home'
screenshot('piano-release-home.png')
# Enter through the public user action and verify the saved tempo is usable,
# rather than merely checking that the XML value survived installation.
home_scroll_to_top()
tap_text(['Ode à Alegria'],scroll=True)
wait_for_text('Espera',timeout=30)
wait_for_text('80%',timeout=30)
labels=[n.attrib.get('text','') for n in dump().iter('node')]
assert any('80%' in t for t in labels),'Saved speed did not restore on first explicit Study opening'
assert not any('Teclado guia' in t or 'Ocultar teclado' in t for t in labels),'Removed guide returned in Study'
probe=subprocess.run(['adb','shell','run-as',PACKAGE,'id'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
assert probe.returncode!=0 and 'not debuggable' in probe.stdout.lower(),'Distribution APK still allows run-as debugging'
pid=adb('shell','pidof',PACKAGE).strip();assert pid.isdigit()
logs=adb('logcat','-d','--pid='+pid,'-v','brief')
assert 'PROMPT03_STAGE_READY' not in logs and 'SELFTEST_PASS' not in logs and 'self-test failed' not in logs,'Internal test intents must be ignored in release'
assert 'AUTO_TEMPORAL_FALLBACK' not in logs and 'ACTIVE_RENDERER schematic' not in logs,'Release accepted forced renderer failures'
assert 'STUDY_CHROME_STATE ' not in logs and 'STUDY_CHROME_LAYOUT ' not in logs and 'HOME_LAYOUT ' not in logs,'Debug-only layout probes appeared in release'
screenshot('piano-release.png')
pathlib.Path('piano-release.json').write_text(json.dumps(dict(package=PACKAGE,variant='release',debuggable=False,debugIntentsIgnored=True,profile='synthetic ID 7',keyboardPreserved=True,speedPreserved=True,scope='API29 emulator, disposable certificate; no physical hardware'),indent=2))
print('PUBLIC_RELEASE_UPGRADE_UI_PASS: non-debuggable, test flags ignored, profile/keyboard/speed preserved',flush=True)

assert "DISCOVERY_LAYOUT_STAGE" not in adb("logcat","-d","--pid="+adb("shell","pidof",PACKAGE).strip(),"-v","brief")

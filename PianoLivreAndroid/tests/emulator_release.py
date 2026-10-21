#!/usr/bin/env python3
"""Upgrade the synthetic installation to non-debuggable release, then verify UI.
PR jobs use the runner certificate; trusted-main jobs use the permanent certificate.
No hardware claims.
"""
import json,pathlib,subprocess,time,tempfile
from upgrade_fixture import synthetic_upgrade_preferences
from emulator_ui import adb,dump,tap_text,screenshot,assert_running
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
flags=['backup_selftest','profiles_selftest','library_selftest','history_selftest','midifile_selftest','xml_input_selftest','upgrade_selftest','midi_selftest','midi_import_ui_selftest']
args=['shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity']
for flag in flags:args.extend(['--ez',flag,'true'])
adb(*args);time.sleep(3);assert_running()
labels=[n.attrib.get('text','') for n in dump().iter('node')]
assert any('Ocultar teclado' in text for text in labels),'Release upgrade lost the synthetic keyboard preference'
tap_text(['Ocultar teclado'])
labels=[n.attrib.get('text','') for n in dump().iter('node')]
assert any('Upgrade B' in text for text in labels),'Release launch did not retain production profile ID 7'
assert any('80%' in text for text in labels),'Release launch did not retain speed preference'
probe=subprocess.run(['adb','shell','run-as',PACKAGE,'id'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
assert probe.returncode!=0 and 'not debuggable' in probe.stdout.lower(),'Distribution APK still allows run-as debugging'
pid=adb('shell','pidof',PACKAGE).strip();assert pid.isdigit()
logs=adb('logcat','-d','--pid='+pid,'-v','brief')
assert 'SELFTEST_PASS' not in logs and 'self-test failed' not in logs,'Internal test intents must be ignored in release'
screenshot('piano-release.png')
pathlib.Path('piano-release.json').write_text(json.dumps(dict(package=PACKAGE,variant='release',debuggable=False,debugIntentsIgnored=True,profile='synthetic ID 7',keyboardPreserved=True,speedPreserved=True,scope='API29 emulator, disposable certificate; no physical hardware'),indent=2))
print('PUBLIC_RELEASE_UPGRADE_UI_PASS: non-debuggable, test flags ignored, profile/keyboard/speed preserved',flush=True)

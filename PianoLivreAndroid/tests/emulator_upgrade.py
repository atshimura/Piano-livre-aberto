#!/usr/bin/env python3
"""Install the prior public-version source build, then seed synthetic upgrade data.
PR runs use the disposable runner key; trusted-main runs use the permanent key.
The public-signed binary update is separately exercised by emulator_public_upgrade.py.
No historical/private repository is used.
"""
import subprocess, os, tempfile, pathlib, struct, json, html, hashlib, argparse, time
BASELINE='15cafe3c8da7c67b9752d14ba0fb6f8f6141177d' # public 0.9.46/code55
parser=argparse.ArgumentParser();parser.add_argument('--baseline-52',action='store_true');parser.add_argument('--baseline-54',action='store_true');options=parser.parse_args()
old_code=63 if options.baseline_54 else 61 if options.baseline_52 else 55
if options.baseline_52: BASELINE='f91c27853be576a353628e220d4e716d02507bed'
if options.baseline_54: BASELINE='cf4b24bfcb7d5ee5e3c19b6ad879429e6c72cc3b'
PACKAGE='br.org.pianolivre.opensource'
def run(*args, capture=False):
    return subprocess.run(args,check=True,text=True,stdout=subprocess.PIPE if capture else None).stdout
root=pathlib.Path.cwd().parent
with tempfile.TemporaryDirectory(prefix='piano-upgrade-') as temporary:
    baseline=pathlib.Path(temporary)/'baseline'
    run('git','fetch','--depth=1','origin',BASELINE)
    run('git','worktree','add','--detach',str(baseline),BASELINE)
    try:
        run('gradle','--no-daemon','-p',str(baseline/'PianoLivreAndroid'),':app:assembleDebug')
        old=baseline/'PianoLivreAndroid/app/build/outputs/apk/debug/app-debug.apk'
        current=pathlib.Path('app/build/outputs/apk/debug/app-debug.apk').resolve()
        signer=pathlib.Path(os.environ['ANDROID_HOME'])/'build-tools/35.0.0/apksigner'
        aapt=signer.with_name('aapt')
        old_info=run(str(aapt),'dump','badging',str(old),capture=True)
        new_info=run(str(aapt),'dump','badging',str(current),capture=True)
        assert f"versionCode='{old_code}'" in old_info and "versionCode='64'" in new_info
        def cert(apk):
            return next(x.strip() for x in run(str(signer),'verify','--print-certs',str(apk),capture=True).splitlines() if 'certificate SHA-256 digest:' in x)
        assert cert(old)==cert(current), 'Upgrade APK certificates must match on this runner'
        run('adb','install',str(old))
        run('adb','shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity')
        run('adb','shell','am','force-stop',PACKAGE)
        # Synthetic state uses the public v0.9.46 storage schema, with stable non-contiguous profile IDs.
        from upgrade_fixture import synthetic_upgrade_preferences
        prefs=pathlib.Path(temporary)/'upgrade-prefs.xml'
        prefs.write_text(synthetic_upgrade_preferences(),encoding='utf-8')
        source=baseline/'PianoLivreAndroid/app/src/main/assets/scores/ode-to-joy-ludwig-van-beethoven.mxl'
        run('adb','push',str(source),'/data/local/tmp/upgrade.mxl')
        run('adb','push',str(prefs),'/data/local/tmp/upgrade-prefs.xml')
        run('adb','shell','run-as',PACKAGE,'mkdir','-p','files/imported','shared_prefs')
        run('adb','shell','run-as',PACKAGE,'cp','/data/local/tmp/upgrade.mxl','files/imported/upgrade.mxl')
        run('adb','shell','run-as',PACKAGE,'cp','/data/local/tmp/upgrade-prefs.xml','shared_prefs/piano_data.xml')
        run('adb','shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity')
        run('adb','shell','pidof',PACKAGE)
        run('adb','shell','am','force-stop',PACKAGE)
        # A read-only debug assertion verifies state after the current APK is installed.
        report=pathlib.Path('piano-upgrade-54.json' if options.baseline_54 else 'piano-upgrade-52.json' if options.baseline_52 else 'piano-upgrade.json')
        report.write_text(json.dumps(dict(baseline=BASELINE,versionCode=old_code,targetVersionCode=64,sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest(),certificate=cert(current),scope='emulator synthetic data; public-signed binary check is separate'),indent=2))
        if options.baseline_52 or options.baseline_54:
            run('adb','install','-r',str(current))
            run('adb','logcat','-c')
            run('adb','shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity','--ez','upgrade_selftest','true')
            for attempt in range(30):
                log=run('adb','logcat','-d','-v','brief',capture=True)
                if 'UPGRADE_SELFTEST_PASS' in log: break
                time.sleep(1)
            else: raise AssertionError('Baseline to 0.9.55 upgrade did not preserve synthetic data: '+log[-4000:])
            print(('UPGRADE_0954_TO_0955_PASS' if options.baseline_54 else 'UPGRADE_0952_TO_0955_PASS')+' profiles=true library=true sessions=true preferences=true',flush=True)
        print('PASS: baseline APK installed and synthetic legacy state seeded; current APK upgrade follows',flush=True)
    finally:
        run('git','worktree','remove','--force',str(baseline))

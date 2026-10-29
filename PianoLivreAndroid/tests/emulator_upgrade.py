#!/usr/bin/env python3
"""Install the prior public-version source build, then seed synthetic upgrade data.
PR runs use the disposable runner key; trusted-main runs use the permanent key.
The public-signed binary update is separately exercised by emulator_public_upgrade.py.
No historical/private repository is used.
"""
import subprocess, os, tempfile, pathlib, struct, json, html, hashlib
BASELINE='15cafe3c8da7c67b9752d14ba0fb6f8f6141177d' # public 0.9.46/code55
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
        assert "versionCode='55'" in old_info and "versionCode='60'" in new_info
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
        pathlib.Path('piano-upgrade.json').write_text(json.dumps(dict(baseline=BASELINE,versionCode=55,targetVersionCode=60,sourceSha256=hashlib.sha256(source.read_bytes()).hexdigest(),certificate=cert(current),scope='emulator disposable debug certificate; synthetic data; permanent-signature check is separate'),indent=2))
        print('PASS: baseline APK installed and synthetic legacy state seeded; current APK upgrade follows',flush=True)
    finally:
        run('git','worktree','remove','--force',str(baseline))

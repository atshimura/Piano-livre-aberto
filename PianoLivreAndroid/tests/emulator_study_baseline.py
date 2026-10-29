#!/usr/bin/env python3
"""Measure Prompt 1 on the same emulator before installing the candidate APK.
Only the disposable CI installation is created/removed; never run against user data.
"""
import json, pathlib, subprocess, tempfile, time, re
from emulator_ui import adb, dump, tap_text, screenshot
BASELINE='32b83a3b6d37a831900de4bafd5ec8cbd406d0d8'
PACKAGE='br.org.pianolivre.opensource'
with tempfile.TemporaryDirectory(prefix='piano-layout-baseline-') as temporary:
    baseline=pathlib.Path(temporary)/'baseline'
    subprocess.run(['git','fetch','--depth=1','origin',BASELINE],check=True)
    subprocess.run(['git','worktree','add','--detach',str(baseline),BASELINE],check=True)
    try:
        subprocess.run(['gradle','--no-daemon','-p',str(baseline/'PianoLivreAndroid'),':app:assembleDebug'],check=True)
        adb('install',str(baseline/'PianoLivreAndroid/app/build/outputs/apk/debug/app-debug.apk'))
        for label,size in [('phone','1280x720'),('tablet','1920x1080')]:
            adb('shell','wm','size',size);adb('shell','wm','density','240')
            adb('shell','am','force-stop',PACKAGE);adb('shell','pm','clear',PACKAGE)
            adb('shell','am','start','-W','-n',PACKAGE+'/br.org.pianolivre.MainActivity');time.sleep(2)
            tap_text(['Começar pela Ode à Alegria'],scroll=True);time.sleep(3)
            root=dump();view=next(n for n in root.iter('node') if n.attrib.get('class')=='android.webkit.WebView')
            box=[int(v) for v in re.findall(r'\d+',view.attrib['bounds'])]
            result=dict(sha=BASELINE,viewport=size,density=240,bounds=box,heightPx=box[3]-box[1],heightDp=(box[3]-box[1])/1.5)
            pathlib.Path('piano-prompt02-before-'+label+'.json').write_text(json.dumps(result,indent=2))
            screenshot('piano-prompt02-before-'+label+'.png')
        adb('shell','am','force-stop',PACKAGE);adb('uninstall',PACKAGE)
    finally:
        adb('shell','wm','size','reset');adb('shell','wm','density','reset')
        subprocess.run(['git','worktree','remove','--force',str(baseline)],check=True)
print('STUDY_PROMPT02_BASELINE_MEASURED',flush=True)

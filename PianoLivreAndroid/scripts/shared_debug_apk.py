#!/usr/bin/env python3
"""Share only a disposable PR debug configuration within one exact-checkout run."""
import hashlib,pathlib,re,shutil,subprocess,sys
folder=pathlib.Path('app/build/outputs/apk/debug')
apk=folder/'app-debug.apk'
source=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
report=subprocess.check_output(['gradle','--no-daemon','-q',':app:signingReport'],text=True)
debug=re.search(r'Variant: debug\s+Config: debug\s+Store: (.+)',report)
assert debug,'Gradle did not identify the disposable debug store'
store=pathlib.Path(debug[1].strip())
if sys.argv[1]=='export':
    assert store.is_file(),'No generated debug keystore at configured path'
    shutil.copyfile(store,folder/'disposable-debug.keystore')
    (folder/'source-sha.txt').write_text(source,encoding='ascii')
    (folder/'apk.sha256').write_text(hashlib.sha256(apk.read_bytes()).hexdigest(),encoding='ascii')
else:
    assert (folder/'source-sha.txt').read_text().strip()==source,'Different source checkout'
    assert hashlib.sha256(apk.read_bytes()).hexdigest()==(folder/'apk.sha256').read_text().strip(),'Different APK bytes'
    if sys.argv[1]=='restore':
        store.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(folder/'disposable-debug.keystore',store)
print('SHARED_DEBUG_APK_'+sys.argv[1].upper()+'_PASS source='+source)

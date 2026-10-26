#!/usr/bin/env python3
"""Install the actual previous public release, seed synthetic user data, then update in place."""
import hashlib, html, json, pathlib, re, subprocess, tempfile, time, urllib.request
from emulator_ui import adb, dump, tap_text, assert_running
from upgrade_fixture import synthetic_upgrade_preferences

PACKAGE = "br.org.pianolivre.opensource"
ACTIVITY = PACKAGE + "/br.org.pianolivre.MainActivity"
PREVIOUS_URL = "https://github.com/atshimura/Piano-livre-aberto/releases/download/v0.9.46-open-beta/PianoLivre-0.9.46-open-beta.apk"
EXPECTED_CERT = "7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290"
NEW_APK = pathlib.Path("app/build/outputs/apk/release/app-release.apk")

def run(*args, check=True):
    return subprocess.run(args, check=check, text=True, stdout=subprocess.PIPE,
                          stderr=subprocess.STDOUT, timeout=90).stdout

def certificate(apksigner, apk):
    output=run(str(apksigner),"verify","--verbose","--print-certs",str(apk))
    line=next((line for line in output.splitlines() if "certificate SHA-256 digest:" in line),"")
    return line.split(":",1)[1].strip().lower()

def version(aapt, apk):
    data=run(str(aapt),"dump","badging",str(apk))
    m=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",data)
    assert m, "Could not read package/version from APK"
    return m.groups()

with tempfile.TemporaryDirectory(prefix="piano-public-upgrade-") as temp:
    temp=pathlib.Path(temp)
    old_apk=temp/"PianoLivre-0.9.46-open-beta.apk"
    urllib.request.urlretrieve(PREVIOUS_URL,old_apk)
    sdk=pathlib.Path(__import__("os").environ["ANDROID_HOME"])
    signer=sdk/"build-tools/35.0.0/apksigner"
    aapt=signer.with_name("aapt")
    assert version(aapt,old_apk)==(PACKAGE,"55","0.9.46-open-beta")
    assert version(aapt,NEW_APK)==(PACKAGE,"58","0.9.49-open-beta")
    assert certificate(signer,old_apk)==EXPECTED_CERT
    assert certificate(signer,NEW_APK)==EXPECTED_CERT

    # This is an isolated emulator. It installs the real downloaded public APK,
    # then seeds only synthetic profile/library/keyboard/speed state for proof.
    run("adb","uninstall",PACKAGE,check=False)
    run("adb","install",str(old_apk))
    run("adb","shell","am","start","-W","-n",ACTIVITY)
    run("adb","shell","am","force-stop",PACKAGE)
    root=run("adb","root")
    assert "cannot run as root" not in root.lower(), "Emulator root is required to seed release-only synthetic fixture"
    time.sleep(2)
    details=run("adb","shell","dumpsys","package",PACKAGE)
    uid_match=re.search(r"userId=(\d+)",details)
    assert uid_match,"Could not determine installed app UID"
    uid=uid_match.group(1)
    source=pathlib.Path("app/src/main/assets/scores/ode-to-joy-ludwig-van-beethoven.mxl")
    prefs=temp/"piano_data.xml"
    prefs.write_text(synthetic_upgrade_preferences(),encoding="utf-8")
    run("adb","push",str(source),"/data/local/tmp/upgrade.mxl")
    run("adb","push",str(prefs),"/data/local/tmp/piano_data.xml")
    private=f"/data/user/0/{PACKAGE}"
    run("adb","shell","mkdir","-p",private+"/files/imported",private+"/shared_prefs")
    run("adb","shell","cp","/data/local/tmp/upgrade.mxl",private+"/files/imported/upgrade.mxl")
    run("adb","shell","cp","/data/local/tmp/piano_data.xml",private+"/shared_prefs/piano_data.xml")
    run("adb","shell","chown","-R",uid+":"+uid,private+"/files",private+"/shared_prefs")
    before=run("adb","shell","sha256sum",private+"/files/imported/upgrade.mxl").split()[0]
    assert before==hashlib.sha256(source.read_bytes()).hexdigest(),"Synthetic imported score was not seeded intact"

    run("adb","install","-r",str(NEW_APK))
    run("adb","shell","am","start","-W","-n",ACTIVITY)
    for _ in range(30):
        assert_running()
        labels=[n.attrib.get("text","") for n in dump().iter("node")]
        if any("Ocultar teclado" in label for label in labels): break
        time.sleep(.5)
    else: raise AssertionError("Updated public app did not restore keyboard/profile state")
    tap_text(["Ocultar teclado"])
    labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Upgrade B" in label for label in labels),"Public release update lost profile ID 7"
    assert any("80%" in label for label in labels),"Public release update lost saved tempo"
    assert any("OSMD · nova"==label for label in labels),"Updated study did not select OSMD by default"
    after=run("adb","shell","sha256sum",private+"/files/imported/upgrade.mxl").split()[0]
    assert after==before,"Public release update changed the imported MusicXML file"
    print(json.dumps({"result":"PUBLIC_APK_UPDATE_PRESERVED_DATA_PASS",
      "previousVersion":"0.9.46-open-beta","previousVersionCode":55,
      "newVersion":"0.9.49-open-beta","newVersionCode":58,"package":PACKAGE,
      "certificateSha256":EXPECTED_CERT,"profile":"synthetic ID 7 preserved",
      "keyboard":"preserved","tempo":"80% preserved","importedScoreSha256":after,
      "scope":"API 29 emulator; actual previous public release APK; synthetic user data"},ensure_ascii=False))

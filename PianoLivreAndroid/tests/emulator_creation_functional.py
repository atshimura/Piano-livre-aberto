#!/usr/bin/env python3
"""Real Android C1 controls, synthetic MIDI, persistence and process restart."""
import json, os, pathlib, struct, time
from emulator_ui import adb, screenshot, dump, tap_text

PACKAGE = "br.org.pianolivre.opensource"
ACTIVITY = PACKAGE + "/br.org.pianolivre.MainActivity"
STAGES = ("home", "recording", "paused", "review", "saved", "renamed",
          "searched", "trash", "restored", "no-output", "no-input", "background",
          "recovered", "deleted", "profile")


def wait_for(marker, pid, timeout=70):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        log = adb("logcat", "-d", "--pid=" + pid, "-v", "brief")
        if marker in log:
            return log
        if "FATAL EXCEPTION" in log or not adb("shell", "pidof", PACKAGE).strip():
            raise AssertionError("Creation process stopped: " + log[-18000:])
        time.sleep(.2)
    raise AssertionError("Creation timed out waiting for " + marker + "\n" + log[-18000:])


def launch(flag, label):
    adb("shell", "am", "force-stop", PACKAGE)
    adb("logcat", "-c")
    adb("shell", "am", "start", "-W", "-n", ACTIVITY, "--ez", flag, "true",
        "--es", "creation_label", label)
    for _ in range(50):
        pid = adb("shell", "pidof", PACKAGE).strip()
        if pid.isdigit():
            return pid
        time.sleep(.1)
    raise AssertionError("Creation test did not start")


reports = []
old_font = adb("shell", "settings", "get", "system", "font_scale").strip()
try:
    for label, size, font in (("phone", "1280x720", "1.0"), ("tablet", "1920x1080", "1.3")):
        adb("shell", "am", "force-stop", PACKAGE)
        adb("shell", "wm", "size", size)
        adb("shell", "wm", "density", "240")
        adb("shell", "settings", "put", "system", "font_scale", font)
        pid = launch("creation_selftest", label)
        images = []
        for stage in STAGES:
            wait_for("CREATION_STAGE " + stage, pid)
            name = "piano-c1-" + label + "-" + stage + ".png"
            screenshot(name)
            data = pathlib.Path(name).read_bytes()
            assert data[:8] == b"\x89PNG\r\n\x1a\n"
            width, height = struct.unpack(">II", data[16:24])
            images.append(dict(name=name, width=width, height=height, bytes=len(data)))
            adb("shell", "run-as", PACKAGE, "touch", "cache/creation-next")
        log = wait_for("CREATION_SELFTEST_PASS", pid)
        pathlib.Path("piano-c1-" + label + ".log").write_text(log, encoding="utf-8")
        proof = json.loads(adb("shell", "run-as", PACKAGE, "cat", "cache/creation-proof.json"))
        pid = launch("creation_reopen_selftest", label)
        wait_for("CREATION_STAGE reopened", pid)
        name = "piano-c1-" + label + "-reopened.png"
        screenshot(name)
        data = pathlib.Path(name).read_bytes()
        width, height = struct.unpack(">II", data[16:24])
        images.append(dict(name=name, width=width, height=height, bytes=len(data)))
        adb("shell", "run-as", PACKAGE, "touch", "cache/creation-next")
        wait_for("CREATION_REOPEN_PASS", pid)
        reports.append(dict(profile=label, fontScale=font, proof=proof, images=images))
        pid = launch("creation_export_selftest", label)
        wait_for("CREATION_EXPORT_PICKER_LAUNCH", pid)
        end = time.monotonic() + 30
        while time.monotonic() < end:
            tree = dump()
            if any(n.attrib.get("package") == "com.android.documentsui" for n in tree.iter("node")):
                break
            time.sleep(.2)
        else:
            raise AssertionError("SAF picker did not open")
        name = "piano-c1-" + label + "-export-picker.png"
        screenshot(name)
        data = pathlib.Path(name).read_bytes()
        width, height = struct.unpack(">II", data[16:24])
        images.append(dict(name=name, width=width, height=height, bytes=len(data)))
        tap_text(["Save", "SAVE", "Salvar", "SALVAR"], exact=True)
        wait_for("CREATION_SAF_EXPORT_PASS", pid)
        name = "piano-c1-" + label + "-exported.png"
        screenshot(name)
        data = pathlib.Path(name).read_bytes()
        width, height = struct.unpack(">II", data[16:24])
        images.append(dict(name=name, width=width, height=height, bytes=len(data)))
        reports[-1]["export"] = json.loads(adb("shell", "run-as", PACKAGE,
                                             "cat", "cache/creation-export-proof.json"))
        print("CREATION_REAL_UI_PASS profile=" + label + " stages=18 restart=true saf=true", flush=True)
    pathlib.Path("piano-c1-report.json").write_text(
        json.dumps(dict(checkout=os.environ.get("GITHUB_SHA"), profiles=reports),
                   ensure_ascii=False, indent=2), encoding="utf-8")
    print("CREATION_FUNCTIONAL_UI_PASS profiles=2 captures=36 synthetic_midi=true saf=true", flush=True)
except BaseException:
    try:
        pathlib.Path("piano-c1-failure.log").write_text(
            adb("logcat", "-d", "-v", "brief"), encoding="utf-8")
        screenshot("piano-c1-failure.png")
    finally:
        raise
finally:
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "wm", "size", "reset")
    adb("shell", "wm", "density", "reset")
    adb("shell", "settings", "put", "system", "font_scale",
        old_font if old_font not in ("", "null") else "1.0")

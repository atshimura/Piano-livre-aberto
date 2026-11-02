#!/usr/bin/env python3
"""One C2-D synthetic emulator UI path; NOT the full Android regression suite."""
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = "br.org.pianolivre.opensource"
ACTIVITY = PACKAGE + "/br.org.pianolivre.MainActivity"
DEST = Path("build/c2-d-evidence")
DEST.mkdir(parents=True, exist_ok=True)

def adb(*args, timeout=30, binary=False):
    result = subprocess.run(["adb", *args], check=True, timeout=timeout,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    return result.stdout if binary else result.stdout.decode("utf-8", "replace")

def ensure_emulator():
    if adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise AssertionError("C2-D UI probe refuses a physical device")

def dump():
    adb("shell", "uiautomator", "dump", "/sdcard/c2-d-window.xml", timeout=45)
    xml = adb("shell", "cat", "/sdcard/c2-d-window.xml")
    (DEST / "window-last.xml").write_text(xml, encoding="utf-8")
    return ET.fromstring(xml)

def nodes(fragment, clickable=False):
    return [node for node in dump().iter("node")
            if fragment.casefold() in (node.attrib.get("text", "") + " "
                                      + node.attrib.get("content-desc", "")).casefold()
            and (not clickable or node.attrib.get("clickable") == "true")]

def center(node):
    found = re.search(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
    if not found:
        raise AssertionError("Button has no bounds: " + str(node.attrib))
    x1, y1, x2, y2 = [int(value) for value in found.groups()]
    if x2 <= x1 or y2 <= y1:
        raise AssertionError("Invalid button bounds")
    return (x1 + x2) // 2, (y1 + y2) // 2

def scroll():
    output = adb("shell", "wm", "size")
    match = re.search(r"(\d+)x(\d+)", output)
    if not match:
        raise AssertionError("Missing emulator dimensions")
    w, h = int(match.group(1)), int(match.group(2))
    adb("shell", "input", "swipe", str(w // 2), str(h * 4 // 5),
        str(w // 2), str(h // 4), "380")

def find(fragment, clickable=False, max_scroll=7):
    for step in range(max_scroll + 1):
        candidates = nodes(fragment, clickable)
        if candidates:
            valid = [node for node in candidates if node.attrib.get("bounds")
                     and not node.attrib["bounds"].startswith("[0,0][0,0]")]
            if valid:
                return valid[0]
        if step < max_scroll:
            scroll()
            time.sleep(0.5)
    raise AssertionError("Missing visible " + ("button" if clickable else "text")
                         + ": " + fragment)

def click(fragment):
    node = find(fragment, clickable=True)
    x, y = center(node)
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(1.1)

def screenshot(name):
    (DEST / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p",
                                             timeout=35, binary=True))
    (DEST / (name + ".xml")).write_text(
        ET.tostring(dump(), encoding="unicode"), encoding="utf-8")

def wait_log(marker, timeout=45):
    until = time.time() + timeout
    while time.time() < until:
        content = adb("logcat", "-d", "-s", "PianoLivreC2D:I", "AndroidRuntime:E", "*:S")
        if "C2_D_FIXTURE_FAILED" in content or "FATAL EXCEPTION" in content:
            raise AssertionError("C2-D fixture failed:\n" + content[-5000:])
        if marker in content:
            return
        time.sleep(1)
    raise AssertionError("Timed out waiting for " + marker)

def main():
    ensure_emulator()
    adb("install", "-r", "app/build/outputs/apk/debug/app-debug.apk", timeout=90)
    adb("shell", "pm", "clear", PACKAGE)
    adb("logcat", "-c")
    adb("shell", "am", "start", "-W", "-n", ACTIVITY,
        "--ez", "creation_c2_d_smoke", "true", timeout=45)
    wait_log("C2_D_FIXTURE_PASS")
    find("C2-D partitura de teste")
    find("Visualizar e ajustar partitura", clickable=True)
    find("Exportar MIDI", clickable=True)
    screenshot("01-detalhes-sinteticos")
    click("Visualizar e ajustar partitura")
    find("Transformar em partitura")
    find("Partitura salva")
    screenshot("02-previa-osmd")
    click("Divisão entre pautas")
    find("Selecionar notas e acordes")
    find("Nota 1 de 4")
    screenshot("02b-ajustes-pautas-acessibilidade")
    click("Concluir ajustes")
    click("Estudar esta partitura")
    find("MODO")
    find("MÃOS")
    screenshot("03-estudar")
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(1.4)
    find("Visualizar e ajustar partitura", clickable=True)
    screenshot("04-retorno-criacao")
    # A second screenshot at landscape checks responsiveness, not real hardware.
    adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
    adb("shell", "settings", "put", "system", "user_rotation", "1")
    time.sleep(2.0)
    find("Visualizar e ajustar partitura", clickable=True)
    screenshot("05-paisagem")
    (DEST / "memory.txt").write_text(adb("shell", "dumpsys", "meminfo", PACKAGE),
                                      encoding="utf-8")
    print("CREATION_C2_D_UI_PASS screenshots=6 actions=preview,split,study,back,landscape"
          " synthetic=true emulator=true")
    adb("shell", "pm", "clear", PACKAGE)

if __name__ == "__main__":
    try:
        main()
    except Exception:
        import traceback
        traceback.print_exc()
        try:
            screenshot("erro-ultimo-estado")
        except Exception:
            pass
        raise

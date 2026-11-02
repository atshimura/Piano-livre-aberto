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

def scroll(direction=1):
    # wm size is the *natural* portrait size. The Piano Livre Activity is
    # fixed to landscape, so use actual accessibility window bounds instead.
    bounds = []
    for node in dump().iter("node"):
        found = re.search(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                          node.attrib.get("bounds", ""))
        if found:
            x1, y1, x2, y2 = [int(value) for value in found.groups()]
            if x2 > x1 and y2 > y1:
                bounds.append((x1, y1, x2, y2))
    if not bounds:
        raise AssertionError("No visible emulator window bounds")
    x1, y1, x2, y2 = max(bounds, key=lambda b: (b[2]-b[0])*(b[3]-b[1]))
    x = (x1 + x2) // 2
    h = y2 - y1
    start, stop = (y1 + h * 4 // 5, y1 + h // 4) if direction > 0 else (y1 + h // 4, y1 + h * 4 // 5)
    adb("shell", "input", "swipe", str(x), str(start), str(x), str(stop), "380")

def find(fragment, clickable=False, max_scroll=7):
    # Inspect from the current position, then search down AND back up.
    # The Exportar MIDI button sits below the Partitura card; a downward-only
    # lookup cannot find the earlier Visualizar button afterward.
    for direction in (1, -1):
        for step in range(max_scroll + 1):
            candidates = nodes(fragment, clickable)
            valid = [node for node in candidates if node.attrib.get("bounds")
                     and not node.attrib["bounds"].startswith("[0,0][0,0]")]
            if valid:
                return valid[0]
            if step < max_scroll:
                scroll(direction)
                time.sleep(0.4)
    visible = [(n.attrib.get("text", ""), n.attrib.get("content-desc", ""))
               for n in dump().iter("node")]
    raise AssertionError("Missing visible " + ("button" if clickable else "text")
                         + ": " + fragment + "; window text=" + repr(visible[:25]))

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
    # Sixth change: generated scores must be explicitly placed in the Library.
    click("Adicionar à Biblioteca")
    find("Na Biblioteca Musical")
    screenshot("03-adicionada-a-biblioteca")
    click("Voltar à composição")
    find("Visualizar e ajustar partitura", clickable=True)
    find("Na Biblioteca Musical")
    screenshot("04-retorno-criacao")
    adb("shell", "input", "keyevent", "KEYCODE_BACK")  # Creation home
    time.sleep(.5)
    adb("shell", "input", "keyevent", "KEYCODE_BACK")  # Library home
    time.sleep(.8)
    title=find("C2-D partitura de teste")
    x,y=center(title)
    adb("shell","input","tap",str(x),str(y))
    find("MODO")
    find("MÃOS")
    screenshot("04b-estudar-pela-biblioteca")
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(1.4)
    find("C2-D partitura de teste")
    # A second screenshot at landscape checks responsiveness, not real hardware.
    adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
    adb("shell", "settings", "put", "system", "user_rotation", "1")
    time.sleep(2.0)
    find("C2-D partitura de teste")
    screenshot("05-paisagem")
    (DEST / "memory.txt").write_text(adb("shell", "dumpsys", "meminfo", PACKAGE),
                                      encoding="utf-8")
    print("CREATION_C2_D_UI_PASS screenshots=7 actions=preview,split,add-library,study-via-library,back,landscape"
          " synthetic=true emulator=true")
    adb("shell", "pm", "clear", PACKAGE)

if __name__ == "__main__":
    # The full suite continues with upgrade/release tests in this SAME AVD.
    # Restore system rotation settings instead of leaking test-specific state.
    old_auto=adb("shell","settings","get","system","accelerometer_rotation").strip()
    old_rotation=adb("shell","settings","get","system","user_rotation").strip()
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
    finally:
        if old_rotation in ("0","1","2","3"):
            adb("shell","settings","put","system","user_rotation",old_rotation)
        if old_auto in ("0","1"):
            adb("shell","settings","put","system","accelerometer_rotation",old_auto)

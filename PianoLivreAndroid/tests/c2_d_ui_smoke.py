#!/usr/bin/env python3
"""One C2-D synthetic emulator UI path; NOT the full Android regression suite."""
import os
import json
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

def click_dialog_item(fragment):
    """AlertDialog text rows need not advertise clickable=true to UiAutomator."""
    item=find(fragment,clickable=False,max_scroll=2)
    x,y=center(item)
    adb("shell","input","tap",str(x),str(y))
    time.sleep(.7)

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

def linked_creation_id():
    """Read the exact synthetic project reference, never infer a random UUID."""
    raw=adb("shell","run-as",PACKAGE,"cat","shared_prefs/piano_data.xml")
    prefs=ET.fromstring(raw)
    item=prefs.find("./string[@name='user_0_library']")
    if item is None or not item.text:
        raise AssertionError("Synthetic Library metadata missing")
    ids=json.loads(item.text).get("addedCreations",[])
    if len(ids)!=1 or not re.fullmatch(r"[0-9a-fA-F-]{36}",ids[0]):
        raise AssertionError("Expected exactly one linked C2-D fixture")
    return ids[0]

def private_file_exists(path):
    return subprocess.run(["adb","shell","run-as",PACKAGE,"ls",path],
                          stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,
                          timeout=15).returncode==0

def main():
    ensure_emulator()
    rotation=adb("shell","settings","get","system","accelerometer_rotation").strip()
    orientation=adb("shell","settings","get","system","user_rotation").strip()
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
    # Implementation 3: reuse the single synthetic C2-D recording, with no
    # additional fixture, emulator session, or change to the captured MIDI.
    click("Selecionar intervalo de compassos")
    find("Selecionar compassos")
    click("Marcar")  # Default 1..N, includes all four source attacks
    find("4 selecionada(s)")
    click("Atribuir mão / voz")
    find("Mão real atribuída pelo compositor")
    click("Cancelar")  # Assignment UI must never silently change the MIDI
    click("Uniformizar trecho")
    find("Figura musical para o trecho")
    click("Cancelar")
    click("Uniformizar trecho")
    click("Ver prévia")
    find("Prévia — Semínima")
    find("Notas afetadas:")
    click("Cancelar")
    click("Uniformizar trecho")
    click("Ver prévia")
    find("Prévia — Semínima")
    click("Aplicar")
    find("Selecionar notas e acordes")
    click("Desfazer")
    find("Selecionar notas e acordes")
    click("Refazer")
    find("Selecionar notas e acordes")
    screenshot("02c-uniformizar-ritmo")
    click("Concluir ajustes")
    # IMPL4: the save path must complete a REAL OSMD Study-path render.
    before_study_proof=adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")
    click("Salvar partitura")
    find("Partitura salva ✓")
    assert adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")>before_study_proof, (
        "IMPL4 save was accepted without a successful OSMD Study proof")
    # Sixth change: generated scores must be explicitly placed in the Library.
    before_library_proof=adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")
    click("Adicionar à Biblioteca")
    # Library linking requires another OSMD Study proof, not only XML parsing.
    find("Partitura compatível com Estudar e adicionada à Biblioteca Musical.")
    assert adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")>before_library_proof, (
        "IMPL4 library linking bypassed OSMD compatibility proof")
    click("OK")
    find("Na Biblioteca Musical")
    screenshot("03-adicionada-a-biblioteca")
    click("Voltar à composição")
    find("Visualizar e ajustar partitura", clickable=True)
    find("Na Biblioteca Musical")
    screenshot("04-retorno-criacao")
    # Creation has nested project/list/home pages. Android BACK is not a
    # guaranteed shortcut from Details to the global Library; follow actual
    # accessible controls and assert each navigation boundary.
    click("Voltar")  # Project Details -> Minhas Criações or Criação home
    if not nodes("← Biblioteca", clickable=True):
        find("Minhas Criações")
        click("Voltar")  # Minhas Criações -> Criação home
    find("Criação Musical")
    find("← Biblioteca", clickable=True)
    click("← Biblioteca")  # Criação home -> Biblioteca Musical
    find("Biblioteca Musical")
    title=find("Estudar C2-D partitura de teste",clickable=True)
    before_open_proof=adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")
    x,y=center(title)
    adb("shell","input","tap",str(x),str(y))
    find("MODO")
    find("MÃOS")
    for _ in range(50):
        if adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")>before_open_proof:break
        time.sleep(.3)
    else:raise AssertionError("IMPL4 generated score did not render with OSMD in Estudar")
    assert "AUTO_TEMPORAL_FALLBACK" not in adb("logcat","-d","-v","brief"), (
        "IMPL4 old temporal renderer was activated")
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
    # The same synthetic generated score also proves safe unlinking from
    # Library. No new C2-D setup, APK, or duplicate emulator session needed.
    project_id=linked_creation_id()
    assert private_file_exists("files/creations/"+project_id+".plc"), "Fixture project absent before unlink"
    assert private_file_exists("files/creation-scores/"+project_id+".pls"), "Fixture MusicXML absent before unlink"
    card=find("Estudar C2-D partitura de teste",clickable=True)
    x,y=center(card)
    adb("shell","input","swipe",str(x),str(y),str(x),str(y),"1100")
    find("Excluir da biblioteca",clickable=False)
    click_dialog_item("Excluir da biblioteca")
    find("A gravação original e a partitura salva continuarão")
    click("Excluir")
    find("Partitura removida da Biblioteca Musical.")
    click("OK")
    assert not nodes("Estudar C2-D partitura de teste",clickable=True), "Unlinked generated score still listed"
    assert private_file_exists("files/creations/"+project_id+".plc"), "Unlink destroyed the original project"
    assert private_file_exists("files/creation-scores/"+project_id+".pls"), "Unlink destroyed the derived score"
    prefs=ET.fromstring(adb("shell","run-as",PACKAGE,"cat","shared_prefs/piano_data.xml"))
    entry=prefs.find("./string[@name='user_0_library']")
    assert entry is not None and project_id not in json.loads(entry.text).get("addedCreations",[]), "Creation catalog entry survived unlink"
    # Implementation 2: revisit the SAME C2-D project, re-add its score, then
    # delete ONLY the derived .pls through Detalhes. No second fixture/AVD.
    original_bytes=subprocess.run(["adb","exec-out","run-as",PACKAGE,"cat",
        "files/creations/"+project_id+".plc"],check=True,stdout=subprocess.PIPE).stdout
    click("Laboratório Musical")
    find("Descobrir")
    click("Criação")
    find("Criação Musical")
    click("Minhas Criações")
    click("C2-D partitura de teste")
    find("Detalhes da composição")
    find("Visualizar e ajustar partitura",clickable=True)
    # Re-linking the saved score must revalidate the OSMD Study path too.
    before_relink_proof=adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")
    click("Adicionar à Biblioteca")
    find("Partitura compatível com Estudar e adicionada à Biblioteca Musical.")
    assert adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")>before_relink_proof, (
        "IMPL4 re-linking a generated score bypassed OSMD Study proof")
    click("OK")
    find("Na Biblioteca Musical")
    find("Mover composição para a Lixeira")
    click("Excluir somente a partitura…")
    find("Excluir somente a partitura?")
    find("A gravação MIDI original será preservada.")
    click("Cancelar")
    assert private_file_exists("files/creation-scores/"+project_id+".pls"), "Cancel deleted generated score"
    click("Excluir somente a partitura…")
    click("Excluir partitura")
    find("Partitura excluída. A gravação original foi preservada.")
    click("OK")
    find("Gerar partitura",clickable=True)
    assert not nodes("Excluir somente a partitura…"), "Delete-score control present without a saved score"
    assert not private_file_exists("files/creation-scores/"+project_id+".pls"), "Generated .pls still exists after confirmed deletion"
    assert private_file_exists("files/creations/"+project_id+".plc"), "Deleting score also deleted original MIDI project"
    after_bytes=subprocess.run(["adb","exec-out","run-as",PACKAGE,"cat",
        "files/creations/"+project_id+".plc"],check=True,stdout=subprocess.PIPE).stdout
    assert after_bytes==original_bytes, "Original MIDI project content changed after score deletion"
    assert not private_file_exists("files/creation-score.delete-pending"), "Completed score deletion left staging files"
    prefs=ET.fromstring(adb("shell","run-as",PACKAGE,"cat","shared_prefs/piano_data.xml"))
    entry=prefs.find("./string[@name='user_0_library']")
    assert entry is not None and project_id not in json.loads(entry.text).get("addedCreations",[]), "Deleted score retained Library link"
    (DEST / "memory.txt").write_text(adb("shell", "dumpsys", "meminfo", PACKAGE),
                                      encoding="utf-8")
    print("CREATION_C2_D_UI_PASS screenshots=7 actions=preview,split,add-library,study-via-library,back,landscape,unlink,score-delete-cancel-confirm,original-midi-preserved"
          " rhythm-uniformize=true synthetic=true emulator=true")
    adb("shell", "pm", "clear", PACKAGE)
    adb("shell","settings","put","system","user_rotation",orientation if orientation not in ("","null") else "0")
    adb("shell","settings","put","system","accelerometer_rotation",rotation if rotation not in ("","null") else "1")

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

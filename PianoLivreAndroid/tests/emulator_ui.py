#!/usr/bin/env python3
"""Automated Android emulator GUI smoke test; no physical MIDI required."""
import os, re, subprocess, time, xml.etree.ElementTree as ET, json
from pathlib import Path
DEFAULT_SCORE_TITLE = json.loads(Path('app/src/main/assets/catalog.json').read_text(encoding='utf-8'))[0]['name']

def adb(*args, timeout=70):
    return subprocess.run(["adb", *args], check=True, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                          timeout=timeout).stdout

def dismiss_keyboard():
    state=adb("shell","dumpsys","input_method")
    if re.search(r"(?:mInputShown|mIsInputViewShown|isInputViewShown)=true",state):
        adb("shell","input","keyevent","4")
        time.sleep(.5)

def screenshot(name):
    adb("shell", "screencap", "-p", "/sdcard/"+name)
    adb("pull", "/sdcard/"+name, name)

def dump():
    for retry in range(3):
        try:
            adb("shell", "rm", "-f", "/sdcard/ui.xml")
            adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
            adb("pull", "/sdcard/ui.xml", "window.xml")
            return ET.parse("window.xml").getroot()
        except Exception as exc:
            print("UI dump retry", retry+1, exc, flush=True)
            time.sleep(4)
    raise AssertionError("Android UI hierarchy inaccessible")

def tap_text(targets, return_point=False, exact=False, scroll=False):
    root=dump()
    for node in root.iter("node"):
        label=node.attrib.get("text","")
        if any((value.casefold()==label.casefold()) if exact else (value.casefold() in label.casefold()) for value in targets):
            nums=[int(x) for x in re.findall(r"\d+",node.attrib.get("bounds",""))]
            if len(nums)==4:
                adb("shell", "input", "tap", str((nums[0]+nums[2])//2),
                    str((nums[1]+nums[3])//2))
                print("Tapped",label,flush=True)
                return (str((nums[0]+nums[2])//2),str((nums[1]+nums[3])//2)) if return_point else label
    if scroll:
        for node in root.iter("node"):
            if node.attrib.get("scrollable")=="true":
                box=[int(x) for x in re.findall(r"\d+",node.attrib.get("bounds",""))]
                if len(box)==4:
                    x=str((box[0]+box[2])//2)
                    adb("shell","input","swipe",x,str(box[3]-15),x,str(box[1]+15),"400")
                    return tap_text(targets,return_point,exact,False)
    labels=[n.attrib.get("text","") for n in root.iter("node") if n.attrib.get("text")]
    print(adb("logcat","-d","-s","PianoLivreUI:I","PianoLivreNotation:I","PianoLivreTimeline:I")[-18000:],flush=True)
    raise AssertionError("Not found: "+str(targets)+"; available: "+str(labels[:40]))

def assert_running():
    try:output=adb("shell","pidof","br.org.pianolivre.opensource")
    except subprocess.CalledProcessError:output=""
    if not output.strip():
        logs=adb("logcat","-d","-v","brief")
        lines=logs.splitlines()
        interesting=[l for l in lines if ("br.org.pianolivre.opensource" in l or "FATAL EXCEPTION" in l or "PianoLivreNotation" in l or "AndroidRuntime" in l or "chromium" in l)]
        print("\\n".join(interesting[-120:]),flush=True)
        raise AssertionError("Piano Livre process is not running; logs shown above")

if __name__=="__main__":
    assert_running()
    midi_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Trilha 1" in t for t in midi_labels) and "Mão esquerda" in midi_labels and "Ignorar esta trilha" in midi_labels, "Explicit MIDI track hand selector missing"
    screenshot("piano-midi-import.png")
    tap_text(["Mão esquerda"],exact=True)
    time.sleep(1)
    tap_text(["OK"],exact=True)
    generated=adb("shell","run-as","br.org.pianolivre.opensource","cat","files/imported/Synthetic.musicxml")
    assert ET.fromstring(generated).find(".//note/staff").text=="2", "Chosen MIDI hand did not reach notation"
    source=subprocess.run(["adb","exec-out","run-as","br.org.pianolivre.opensource","cat","files/imported/Synthetic.musicxml.source.mid"],check=True,stdout=subprocess.PIPE).stdout
    assert source.startswith(b"MThd") and len(source)==35, "Original synthetic MIDI source not preserved"
    tap_text(["Biblioteca"])
    # Remove only these known synthetic files in this fresh emulator installation.
    adb("shell","run-as","br.org.pianolivre.opensource","rm","files/imported/Synthetic.musicxml","files/imported/Synthetic.musicxml.source.mid","files/imported/upgrade.mxl")
    tap_text([DEFAULT_SCORE_TITLE]);tap_text(["Biblioteca"])
    print("PASS: explicit native MIDI hand selector, generated left staff and preserved source",flush=True)
    screenshot("piano-screen.png")
    home=dump()
    names=[n.attrib.get("text","") for n in home.iter("node")]
    assert any(DEFAULT_SCORE_TITLE in n for n in names), "Music library must be visible on launch: "+str(names[:30])
    print("PASS: library visible at startup",flush=True)
    # Laboratory entry and its deliberately limited first Learn/Practice slice.
    tap_text(["Laboratório"],exact=True)
    lab_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Descobrir" in t for t in lab_labels), "Discovery screen not shown"
    assert any("Toque uma nota ou acorde" in t for t in lab_labels), "MusicLab initial state missing"
    tap_text(["Iniciar sequência"],exact=True)
    assert any("Concluir análise" in n.attrib.get("text","") for n in dump().iter("node")), "Manual sequence recording did not start"
    tap_text(["Concluir análise"],exact=True)
    assert any("Nenhuma nota registrada" in n.attrib.get("text","") for n in dump().iter("node")), "Empty sequence must not be guessed"
    tap_text(["Aprender / Praticar"],exact=True)
    learn_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Lá menor" in t for t in learn_labels), "Learn chord example missing"
    tap_text(["Praticar"],exact=True)
    practice_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("classes de altura faltando" in t for t in practice_labels), "Free chord practice feedback missing"
    # Search while practicing must stop old exercise and update the highlights.
    fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    assert fields, "Catalog search field missing"
    coords=[int(x) for x in re.findall(r"\d+",fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((coords[0]+coords[2])//2),str((coords[1]+coords[3])//2))
    adb("shell","input","text","arpejo")
    dismiss_keyboard()
    filtered=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Arpejo ·" in t for t in filtered), "Catalog category filter did not show arpeggios"
    assert "Praticar" in filtered, "Editing search must stop any running practice"
    fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    coords=[int(x) for x in re.findall(r"\d+",fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((coords[0]+coords[2])//2),str((coords[1]+coords[3])//2))
    adb("shell","input","keyevent","123")
    for _ in range(6):adb("shell","input","keyevent","67")
    adb("shell","input","text","zzzx")
    dismiss_keyboard()
    assert any("Nenhum item encontrado" in n.attrib.get("text","") for n in dump().iter("node")), "Catalog no-results state missing"
    fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    coords=[int(x) for x in re.findall(r"\d+",fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((coords[0]+coords[2])//2),str((coords[1]+coords[3])//2))
    adb("shell","input","keyevent","123")
    for _ in range(4):adb("shell","input","keyevent","67")
    dismiss_keyboard()
    assert any("Lá menor" in n.attrib.get("text","") for n in dump().iter("node")), "Catalog reset lost five legacy exercises"
    print("PASS: Laboratory catalog filtering, empty state and practice reset",flush=True)
    # Guided scale/arpeggio starts from zero and resets on filter/mode changes.
    search_fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    assert search_fields, "Laboratory search missing for guided practice"
    coords=[int(x) for x in re.findall(r"\d+",search_fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((coords[0]+coords[2])//2),str((coords[1]+coords[3])//2))
    adb("shell","input","text","escala")
    dismiss_keyboard()
    assert any("Escala ·" in n.attrib.get("text","") for n in dump().iter("node")), "Scale filter missing"
    tap_text(["Praticar"],exact=True,scroll=True)
    assert any("Próxima:" in n.attrib.get("text","") and "0/" in n.attrib.get("text","") and "mão (referência): Ambas" in n.attrib.get("text","") for n in dump().iter("node")), "Sequential practice or hand reference not shown"
    search_fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    coords=[int(x) for x in re.findall(r"\d+",search_fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((coords[0]+coords[2])//2),str((coords[1]+coords[3])//2))
    adb("shell","input","keyevent","123")
    for _ in range(6):adb("shell","input","keyevent","67")
    adb("shell","input","text","arpejo")
    dismiss_keyboard()
    assert any("Arpejo ·" in n.attrib.get("text","") for n in dump().iter("node")), "Arpeggio filter missing"
    assert any(n.attrib.get("text")=="Praticar" for n in dump().iter("node")), "Filter change retained previous practice"
    tap_text(["Praticar"],exact=True,scroll=True)
    assert any("Próxima:" in n.attrib.get("text","") for n in dump().iter("node")), "Arpeggio practice did not restart"
    tap_text(["Descobrir"],exact=True,scroll=True)
    tap_text(["Aprender / Praticar"],exact=True,scroll=True)
    assert any(n.attrib.get("text")=="Praticar" for n in dump().iter("node")), "Mode switch retained old progress"
    print("PASS: Laboratory ordered practice, hand reference, filter and mode reset",flush=True)
    tap_text(["Demonstrar"],scroll=True)
    assert any("Demonstração requer saída USB-MIDI" in n.attrib.get("text","") for n in dump().iter("node")), "Demo erroneously played without MIDI"
    tap_text(["OK"],exact=True)
    assert any("Demonstrar" in n.attrib.get("text","") for n in dump().iter("node")), "Failed demo did not remain stopped"
    print("PASS: Laboratory demonstration unavailable without MIDI output",flush=True)
    tap_text(["Montar"],exact=True,scroll=True)
    screenshot("piano-laboratorio.png")
    tap_text(["Biblioteca"])
    assert_running()
    assert any("BIBLIOTECA MUSICAL" in n.attrib.get("text","") for n in dump().iter("node")), "Laboratory return lost library"
    print("PASS: Laboratory discovery, keyboard mode and guided chord practice",flush=True)
    # MIDI diagnostics must be discoverable even without a physical piano.
    tap_text(["Conecte seu piano","MIDI indisponível"])
    time.sleep(.6)
    labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Mensagens recebidas" in t for t in labels), "MIDI diagnostic counter is missing"
    tap_text(["Fechar"])
    print("PASS: MIDI input/output diagnostic dialog opens",flush=True)
    screenshot("piano-library.png")
    # Chord calibration must be discoverable, with no magic millisecond default.
    tap_text(["Opções"])
    time.sleep(.5)
    options=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Calibrar acordes MIDI" in text for text in options), "Calibrar acordes option missing"
    tap_text(["Calibrar acordes MIDI"])
    time.sleep(.5)
    options=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Calibrar tocando acordes" in text for text in options), "Physical calibration flow missing"
    assert any("Não calibrada" in text for text in options), "Uncalibrated setting not identified"
    tap_text(["Calibrar tocando acordes"])
    time.sleep(.5)
    options=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Conecte primeiro" in text for text in options), "Calibration must require actual MIDI input"
    tap_text(["OK"])
    print("PASS: calibration is discoverable, uncalibrated by default, physical MIDI required",flush=True)
    tap_text(["Histórico"],exact=True)
    history_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Tempo na tela de estudo" in t for t in history_labels), "Native history totals missing"
    tap_text(["Períodos"],exact=True)
    history_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Leitura livre" in t for t in history_labels), "Free-reading period missing"
    tap_text(["Fechar"],exact=True)
    print("PASS: native history totals and free-reading periods",flush=True)
    # Native profile UI: create, avatar and confirmed deletion, with stable identity.
    tap_text(["+"],exact=True)
    fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    assert fields, "New profile name input missing"
    box=[int(x) for x in re.findall(r"\d+",fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((box[0]+box[2])//2),str((box[1]+box[3])//2))
    adb("shell","input","text","Perfil_Teste")
    dismiss_keyboard()
    tap_text(["Criar"],exact=True)
    created=[n.attrib.get("text","") for n in dump().iter("node")]
    assert "Novo perfil" not in created and any("Perfil_Teste" in t for t in created) and any("PIANO LIVRE" in t for t in created), "New profile was not created and selected"
    tap_text(["Opções"]);tap_text(["Gerenciar perfil"],scroll=True);tap_text(["Escolher avatar"]);tap_text(["★"],exact=True)
    assert any("★ Perfil_Teste" in n.attrib.get("text","") for n in dump().iter("node")), "Avatar was not retained"
    tap_text(["Opções"]);tap_text(["Gerenciar perfil"],scroll=True);tap_text(["Excluir perfil"]);tap_text(["Excluir"],exact=True)
    after_delete=[n.attrib.get("text","") for n in dump().iter("node")]
    assert not any("Perfil_Teste" in t for t in after_delete), "Deleted profile remains selected"
    if not any("BIBLIOTECA MUSICAL" in t for t in after_delete):tap_text(["Biblioteca"])
    print("PASS: profile creation, avatar, confirmed deletion and survivor selection",flush=True)
    fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    assert fields, "Library search input missing"
    box=[int(x) for x in re.findall(r"\d+",fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((box[0]+box[2])//2),str((box[1]+box[3])//2))
    adb("shell","input","text","gymnopedie")
    dismiss_keyboard()
    search_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("1 de 10 partituras" in t for t in search_labels), "Accent-insensitive library search did not filter"
    fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    box=[int(x) for x in re.findall(r"\d+",fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((box[0]+box[2])//2),str((box[1]+box[3])//2))
    adb("shell","input","keyevent","123")
    adb("shell","input","keyevent",*(["67"]*10))
    dismiss_keyboard()
    assert any("10 de 10 partituras" in n.attrib.get("text","") for n in dump().iter("node")), "Clearing search did not restore library"
    print("PASS: native library search ignores accents and clearing restores results",flush=True)
    render_baseline=adb("logcat","-d","-v","brief").count("NOTATION_RENDERED")
    tap_text([DEFAULT_SCORE_TITLE])
    time.sleep(3)
    assert_running()
    screenshot("piano-song.png")
    ui=dump()
    labels=[n.attrib.get("text","") for n in ui.iter("node")]
    assert any(DEFAULT_SCORE_TITLE in s for s in labels), "Music title absent: "+str(labels[:35])
    assert any("VELOCIDADE" in s for s in labels), "Study speed controls missing"
    # The legacy Canvas fallback is not sufficient: require OSMD's real
    # MusicXML engraving engine to finish drawing in Android WebView.
    notation_ready=False
    for attempt in range(16):
        logs=adb("logcat","-d","-v","brief")
        if logs.count("NOTATION_RENDERED")>render_baseline:
            notation_ready=True
            break
        time.sleep(2)
    if not notation_ready:
        logs=adb("logcat","-d","-v","brief")
        print("\\n".join(line for line in logs.splitlines() if "PianoLivreNotation" in line)[-15000:],flush=True)
    assert notation_ready, "Real MusicXML notation renderer failed; fallback is not acceptable"
    screenshot("piano-engraved.png")
    # OSMD SVG research mode is opt-in by LONG PRESS; never replaces the default.
    before_memory=adb("shell","dumpsys","meminfo","br.org.pianolivre.opensource")
    free_buttons=[n for n in dump().iter("node") if n.attrib.get("text")=="Partitura Livre"]
    assert free_buttons, "Research preview requires existing Partitura Livre button"
    bb=[int(x) for x in re.findall(r"\d+",free_buttons[0].attrib.get("bounds",""))]
    cx,cy=(bb[0]+bb[2])//2,(bb[1]+bb[3])//2
    adb("shell","input","swipe",str(cx),str(cy),str(cx),str(cy),"1100")
    professional=None
    for attempt in range(24):
        time.sleep(.65)
        proof_logs=adb("logcat","-d","-v","brief")
        entries=[line for line in proof_logs.splitlines() if "POC_OSMD_READY {" in line]
        if entries:
            try:professional=json.loads(entries[-1].split("POC_OSMD_READY ",1)[1].split(" @",1)[0])
            except ValueError:pass
            if professional:break
    assert professional, "Bundled OSMD horizontal SVG proof did not render in Android WebView"
    assert professional["svgNodes"]>50 and professional["width"]>0 and professional["measures"]>=3, "OSMD proof rendered no music"
    assert professional["musicalAnchors"]>=8 and professional["graphicalNotes"]>=12, "OSMD map lost musical events"
    assert "POC_LAYER software" in adb("logcat","-d","-v","brief"), "Preview did not isolate software SVG diagnostic"
    time.sleep(2)
    screenshot("piano-osmd-poc.png")
    # A real adb tap on the HEAD of the first note, not on an arbitrary beat column.
    first=professional.get("firstNoteViewport")
    assert first and first.get("midiEstimate") is not None, "OSMD notehead mapping missing"
    webviews=[n for n in dump().iter("node") if n.attrib.get("class")=="android.webkit.WebView"]
    assert webviews, "Android WebView bounds unavailable for a physical note tap"
    bounds=[int(v) for v in re.findall(r"\d+",webviews[0].attrib.get("bounds",""))]
    assert len(bounds)==4, "Invalid WebView geometry"
    w,h=bounds[2]-bounds[0],bounds[3]-bounds[1]
    nx=bounds[0]+round(first["x"]*w/professional["viewportWidth"])
    ny=bounds[1]+round(first["y"]*h/professional["viewportHeight"])
    assert bounds[0]<=nx<bounds[2] and bounds[1]<=ny<bounds[3], "Mapped notehead outside visible WebView"
    adb("shell","input","tap",str(nx),str(ny))
    time.sleep(.5)
    touches=[line for line in adb("logcat","-d","-v","brief").splitlines() if "POC_NOTE_TAP {" in line]
    assert touches, "Actual Android touch did not reach graphical notehead"
    hit=json.loads(touches[-1].split("POC_NOTE_TAP ",1)[1].split(" @",1)[0])
    assert hit["staff"]==1 and hit["midiEstimate"]==first["midiEstimate"], "Android head-touch resolved wrong note or staff"
    screenshot("piano-osmd-poc-note-tap.png")
    print("PASS: actual Android tap identified first upper-staff OSMD notehead",flush=True)
    tap_text(["Contínuo"],exact=True,scroll=True)
    playback_point=tap_text(["Iniciar"],return_point=True)
    time.sleep(2)
    screenshot("piano-osmd-poc-moving.png")
    motion_log=adb("logcat","-d","-v","brief")
    samples=[]
    for line in motion_log.splitlines():
        if "OPTICAL_SAMPLE {" in line:
            try:samples.append(json.loads(line.split("OPTICAL_SAMPLE ",1)[1].split(" @",1)[0]))
            except ValueError:pass
    assert len(samples)>=2, "No dynamic optical scroll samples"
    assert samples[-1]["beat"]>samples[0]["beat"]+.3, "Optical position did not progress with musical clock"
    assert samples[-1]["scroll"]>samples[0]["scroll"]+3, "Optical SVG did not move horizontally during playback"
    adb("shell","input","tap",*playback_point)
    print("PASS: experimental OSMD optical scroll follows native musical clock",flush=True)
    after_memory=adb("shell","dumpsys","meminfo","br.org.pianolivre.opensource")
    Path("piano-engraving-poc.json").write_text(json.dumps({
      "baseSha":"15cafe3c8da7c67b9752d14ba0fb6f8f6141177d",
      "title":DEFAULT_SCORE_TITLE,
      "studyBaseline":"piano-engraved.png",
      "osmdPreview":"piano-osmd-poc.png",
      "baselineMemoryPssKb":re.search(r"TOTAL PSS:\s*(\d+)",before_memory).group(1) if re.search(r"TOTAL PSS:\s*(\d+)",before_memory) else None,
      "osmdMemoryPssKb":re.search(r"TOTAL PSS:\s*(\d+)",after_memory).group(1) if re.search(r"TOTAL PSS:\s*(\d+)",after_memory) else None,
      "osmd":professional,
      "restriction":"Research-only optical interpolation and test playback. MIDI clock unchanged. Visual clipping, click accuracy, long-scroll performance and editorial quality not yet approved."
    },ensure_ascii=False,indent=2),encoding="utf-8")
    print("PASS: OSMD professional rendering POC screenshot, SVG metrics and Java/JS isolation",flush=True)
    # Restore legacy strict-proportional Estudo before all regression assertions.
    adb("shell","input","swipe",str(cx),str(cy),str(cx),str(cy),"1100")
    for attempt in range(20):
        time.sleep(.6)
        if adb("logcat","-d","-v","brief").count("TIMELINE_READY")>=2:break
    assert "POC_TOGGLE false" in adb("logcat","-d","-v","brief"), "Temporal rendering not restored after POC"
    assert "POC_LAYER original" in adb("logcat","-d","-v","brief"), "WebView layer was not restored"
    assert "TIMELINE_READY" in logs, "Expected proportional timeline renderer, not ordinary print layout"
    # The generated coordinates must follow exactly x = origin + beat * pxPerBeat.
    import json
    frames=[l.split("TIMELINE_READY ",1)[1] for l in logs.splitlines() if "TIMELINE_READY {" in l]
    assert frames, "No time-proportional layout diagnostics available"
    plan=json.loads(frames[-1].split(" @",1)[0].strip())
    beats=plan["firstBeats"];xs=plan["firstXs"];K=plan["pxPerBeat"]
    for i in range(1,len(beats)):
        assert abs((xs[i]-xs[0])-K*(beats[i]-beats[0]))<=2.1, "Beat-proportional x positioning is broken"
    assert abs(plan["cursorFraction"]-.4)<.00001, "Study cursor no longer fixed at 40 percent"
    print("PASS: exact proportional note spacing on shared musical timeline",flush=True)
    print("PASS: temporal MusicXML engraving rendered with fixed 40-percent cursor",flush=True)
    logs=adb("logcat","-d","-v","brief")
    print("\\n".join(l for l in logs.splitlines() if "TEMPORAL_PROBE" in l)[-28000:],flush=True)

    assert any("Contínuo" in s for s in labels), "Mode switch missing"
    assert any("Teclado guia" in s for s in labels), "Keyboard should be hidden by default"
    tap_text(["Teclado guia"])
    time.sleep(.6)
    labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Ocultar teclado" in s for s in labels), "Keyboard toggle did not change to Hide"
    screenshot("piano-with-keyboard.png")
    tap_text(["Ocultar teclado"])
    time.sleep(.4)
    labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Teclado guia" in s for s in labels), "Keyboard toggle did not restore hidden state"
    print("PASS: keyboard hidden by default, shown and hidden by tap",flush=True)
    # Direct score selection is activated from the native study toolbar.
    tap_text(["Trecho ↻"])
    time.sleep(.3)
    labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Selecionando" in t for t in labels), "Score-based loop selection did not activate"
    tap_text(["Selecionando"])
    print("PASS: selection mode toggles from the Trecho button",flush=True)
    assert any("Início" in t for t in labels), "Return-to-start control missing"
    # Traditional score is a separate no-MIDI, no-training reading surface.
    free_baseline=adb("logcat","-d","-v","brief").count("FREE_RENDERED")
    tap_text(["Partitura Livre"])
    time.sleep(3)
    assert_running()
    free_ready=False
    for attempt in range(12):
        logs=adb("logcat","-d","-v","brief")
        if logs.count("FREE_RENDERED")>free_baseline:
            free_ready=True
            break
        time.sleep(2)
    assert free_ready, "Free-reading score did not finish rendering"
    free_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert not any("Contínuo" in s or "Teclado guia" in s for s in free_labels), "Training controls appear in free reading"
    screenshot("piano-free-reading.png")
    # A pixel-size threshold calibrated for the old Ode fixture is invalid
    # for short public exercises. FREE_RENDERED plus real OSMD log is required.
    assert os.path.getsize("piano-free-reading.png") > 5000, "Missing free-reading screenshot"
    print("PASS: traditional free-reading score is visible without training controls",flush=True)
    tap_text(["Biblioteca"])
    time.sleep(1)
    tap_text([DEFAULT_SCORE_TITLE])
    time.sleep(3)
    assert_running()
    print("PASS: selected music loads with interactive study controls",flush=True)
    tap_text(["Teclado guia"])
    time.sleep(.5)
    labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Ocultar teclado" in t for t in labels), "Virtual keyboard toggle did not update"
    tap_text(["Ocultar teclado"])
    print("PASS: optional keyboard show/hide toggles",flush=True)
    tap_text(["Contínuo"])
    time.sleep(.5)
    playback_point=tap_text(["Iniciar"],return_point=True)
    # Capture a moving reference video, not just static screenshots. On very
    # limited emulators, recording is optional; functional assertions remain strict.
    try:
        subprocess.run(["adb","shell","screenrecord","--size","960x540",
            "--bit-rate","1200000","--time-limit","4","/sdcard/piano-motion.mp4"],
            check=True,timeout=24,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        adb("pull","/sdcard/piano-motion.mp4","piano-motion.mp4")
        print("PASS: captured real-time study motion video",flush=True)
    except Exception as exc:
        print("WARNING: emulator video recording unavailable:",exc,flush=True)
    logs=adb("logcat","-d","-v","brief")
    assert re.search(r"PianoLivreUI.*mode=1", logs), "Mode button click was not recorded"
    assert re.search(r"PianoLivreUI.*play=true", logs), "Play button click was not recorded"
    samples=[]
    for line in logs.splitlines():
        if "TEMPORAL_SAMPLE {" in line:
            try:samples.append(json.loads(line.split("TEMPORAL_SAMPLE ",1)[1].split(" @",1)[0].strip()))
            except ValueError:pass
    assert len(samples)>=2, "No continuous rendering frames reported"
    for a,b in zip(samples,samples[1:]):
        if b["beat"]>a["beat"]+.005:
            actual=(b["offset"]-a["offset"])/(b["beat"]-a["beat"])
            assert abs(actual-plan["pxPerBeat"])<1, "Visual velocity not uniform over musical beats"
    print("PASS: continuous horizontal motion at proportional speed",flush=True)
    print("PASS: mode switch and play button accepted touch input",flush=True)
    # UIAutomator requires an idle hierarchy. Pause using the verified button
    # bounds before requesting another dump; never reuse an old XML snapshot.
    adb("shell","input","tap",*playback_point)
    time.sleep(.3)
    # Lifecycle: background during playback must pause and retain that position.
    tap_text(["Início"])
    tap_text(["Iniciar"])
    time.sleep(.4)
    adb("shell","input","keyevent","3")
    time.sleep(.6)
    adb("shell","am","start","-W","-n","br.org.pianolivre.opensource/br.org.pianolivre.MainActivity")
    time.sleep(1)
    resumed=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Iniciar" in t for t in resumed), "Background playback was not paused"
    positions=[t for t in resumed if "posição " in t]
    assert positions, "Position label missing after resume"
    time.sleep(.4)
    later=[n.attrib.get("text","") for n in dump().iter("node")]
    assert [t for t in later if "posição " in t]==positions, "Position advanced while paused after resume"
    logs=adb("logcat","-d","-v","brief")
    assert "LIFECYCLE_PAUSED" in logs and "LIFECYCLE_RESUMED" in logs
    reports=[]
    for line in logs.splitlines():
        if "TEMPORAL_PERF {" in line:
            reports.append(json.loads(line.split("TEMPORAL_PERF ",1)[1].split(" @",1)[0].strip()))
    assert any(r["reason"]=="pause" and r["movingFrames"]>=2 for r in reports), "Moving frame summary missing"
    assert all(r["renderMs"]>=0 and r["maxDrawMs"]>=0 for r in reports)
    Path("piano-performance.json").write_text(json.dumps(reports,indent=2),encoding="utf-8")
    print("PASS: background pauses transport, foreground retains position, local performance evidence captured",flush=True)
    resume_point=tap_text(["Iniciar"],return_point=True)
    time.sleep(.7)
    adb("shell","input","tap",*resume_point)
    time.sleep(.3)
    stopped=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Iniciar" in t for t in stopped), "Manual pause after foreground failed"
    new_positions=[t for t in stopped if "posição " in t]
    before=float(positions[0].split("posição ",1)[1])
    after=float(new_positions[0].split("posição ",1)[1])
    assert after>before+.05, "Transport did not advance after manual restart"
    print("PASS: playback advances and can pause after foreground",flush=True)
    tap_text(["Biblioteca"])
    time.sleep(1)
    assert_running()
    labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("BIBLIOTECA MUSICAL" in s for s in labels), "Returning to library failed"
    print("PASS: navigation returns to library",flush=True)
    logs=adb("logcat","-d","-v","brief")
    if "FATAL EXCEPTION" in logs and "Process: br.org.pianolivre.opensource" in logs:
        raise AssertionError("Fatal exception in Piano Livre")
    print("PASS emulator UI: library, music, study mode, playback, navigation",flush=True)

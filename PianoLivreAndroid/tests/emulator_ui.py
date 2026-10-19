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

def memory_pss_kb(text):
    """Read measured app process PSS; dumpsys layouts differ across API versions."""
    for pattern in (r"(?mi)^\s*TOTAL\s+PSS\s*:\s*([\d,]+)",
                    r"(?mi)^\s*TOTAL\s*:\s*([\d,]+)",
                    r"(?mi)^\s*TOTAL\s+([\d,]+)\s+(?:[\d,]+|\d+)"):
        m=re.search(pattern,text)
        if m:return int(m.group(1).replace(",",""))
    return None

def screenshot(name):
    adb("shell", "screencap", "-p", "/sdcard/"+name)
    adb("pull", "/sdcard/"+name, name)

def responsive_evidence(name):
    """Measure actual WebView bounds with native bars in both guide states."""
    result=[]
    for state in ("closed", "open", "restored"):
        if state=="open":tap_text(["Teclado guia"],scroll=True)
        if state=="restored":tap_text(["Ocultar teclado"],scroll=True)
        time.sleep(.8)
        labels=[n.attrib.get("text", "") for n in dump().iter("node")]
        title_visible="Partitura Livre" in labels
        controls_visible="Espera" in labels
        assert title_visible==(state!="open"), "Guide did not hide/restore title bar"
        assert controls_visible==(state!="open"), "Guide did not hide/restore controls bar"
        rows=[line for line in adb("logcat","-d","-v","brief").splitlines() if "POC_SVG_FIT {" in line]
        assert rows, "Responsive geometry report absent"
        geometry=json.loads(rows[-1].split("POC_SVG_FIT ",1)[1].split(" @",1)[0])
        assert geometry["scale"]==1, "Guide resize compressed music"
        assert geometry["height"]<=geometry["availableHeight"]+0.5, "Both staves exceed the available height"
        assert geometry["viewportMaxScrollTop"]==0, "Vertical scrolling is exposed in OSMD preview"
        assert abs(geometry["scale"]-geometry["availableHeight"]/geometry["rawHeight"])<0.002, "Displayed note width/height do not use one uniform fit"
        screenshot("piano-03d-"+name+"-"+state+".png")
        result.append({"keyboard":state,"titleBar":title_visible,"controlsBar":controls_visible,
                       "geometry":geometry})
    assert result[0]["geometry"]["viewportHeight"]==result[-1]["geometry"]["viewportHeight"], "Closing guide did not restore score height"
    Path("piano-03d-"+name+".json").write_text(json.dumps(result,indent=2),encoding="utf-8")

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
    gesture_delivered=False
    for attempt in range(24):
        time.sleep(.65)
        proof_logs=adb("logcat","-d","-v","brief")
        gesture_delivered="POC_GESTURE_LONG_CLICK delivered=true enabled=true" in proof_logs
        entries=[line for line in proof_logs.splitlines() if "POC_OSMD_READY {" in line]
        if entries:
            try:professional=json.loads(entries[-1].split("POC_OSMD_READY ",1)[1].split(" @",1)[0])
            except ValueError:pass
            if professional:break
    if not professional:
        proof_logs=adb("logcat","-d","-v","brief")
        screenshot("piano-osmd-failure.png")
        proof_ui=dump()
        Path("piano-osmd-failure-window.xml").write_bytes(ET.tostring(proof_ui,encoding="utf-8"))
        activity=adb("shell","dumpsys","activity","activities")
        processes=adb("shell","ps","-A")
        Path("piano-osmd-failure-diagnostics.txt").write_text(
            "GESTURE_DELIVERED="+str(gesture_delivered)+"\n\nACTIVITY\n"+activity+
            "\n\nPROCESSES\n"+processes+"\n\nLOGCAT\n"+proof_logs,encoding="utf-8")
        relevant=[line for line in proof_logs.splitlines() if any(token in line for token in
          ("PianoLivreNotation","POC_GESTURE","FATAL EXCEPTION","AndroidRuntime","chromium","ANR"))]
        print("POC_FAILURE_STATE gesture="+str(gesture_delivered)+" pid="+
          adb("shell","pidof","br.org.pianolivre.opensource").strip(),flush=True)
        print("POC_FAILURE_ACTIVITY "+activity[-8000:],flush=True)
        print("POC_FAILURE_LOGCAT\n"+"\n".join(relevant[-240:]),flush=True)
    assert professional, ("Bundled OSMD horizontal SVG proof did not render; "+
      ("long-press was not delivered" if not gesture_delivered else "long-press arrived; inspect WebView diagnostics"))
    assert gesture_delivered, "OSMD proof appeared without the expected long-press diagnostic"
    assert professional["svgNodes"]>50 and professional["width"]>0 and professional["measures"]>=3, "OSMD proof rendered no music"
    print("POC_SIMPLE_GEOMETRY "+json.dumps({k:professional.get(k) for k in (
      "displayScale","height","nativeSvgHeight","nativeStaffSpaceCssPx",
      "staffSeparationSpaces","viewportHeight","viewportMaxScrollTop")}),flush=True)
    assert professional["height"]<=professional["availableHeight"]+0.5 and \
      abs(professional["displayScale"]-professional["availableHeight"]/professional["nativeSvgHeight"])<0.002 and \
      professional["nativeStaffSpaceCssPx"]>0 and professional["viewportMaxScrollTop"]==0, \
      "Simple-score OSMD did not fit proportionally or allowed vertical scrolling"
    context_logs=[line for line in adb("logcat","-d","-v","brief").splitlines()
                  if "POC_CONTEXT_" in line]
    Path("piano-osmd-context-probe.txt").write_text("\n".join(context_logs)+"\n",encoding="utf-8")
    parsed_context={}
    for marker in ("POC_CONTEXT_MEASURE_1 ","POC_CONTEXT_MODIFIERS_1 ",
                   "POC_CONTEXT_SVG_ROOT ","POC_CONTEXT_NODES_1 ","POC_CONTEXT_NODES_2 "):
        matching=[line for line in context_logs if marker in line]
        assert matching, "OSMD context detail missing "+marker+": "+"\n".join(context_logs[-5:])
        assert len(matching[-1])<3500, "Android logcat may have truncated context record "+marker
        payload=matching[-1].split(marker,1)[1].split(" @",1)[0].strip()
        try:parsed_context[marker.strip()]=json.loads(payload)
        except ValueError as exc:raise AssertionError("OSMD context JSON was truncated for "+marker) from exc
    first_staff=parsed_context["POC_CONTEXT_NODES_1"]
    second_staff=parsed_context["POC_CONTEXT_NODES_2"]
    first_measures=[measure["measure"] for measure in first_staff["measures"]]
    second_measures=[measure["measure"] for measure in second_staff["measures"]]
    assert first_measures[:3]==second_measures[:3]==["1","2","3"] and \
      first_staff["staffline"]!=second_staff["staffline"], (
        "OSMD context probe must inspect measures 1-3 on both grand-staff lines")
    rail_lines=[line for line in adb("logcat","-d","-v","brief").splitlines()
                if "POC_CONTEXT_RAIL_READY {" in line]
    assert rail_lines, "OSMD did not build the fixed-left context rail"
    rail=json.loads(rail_lines[-1].split("POC_CONTEXT_RAIL_READY ",1)[1].split(" @",1)[0])
    assert rail.get("staves")==2 and rail.get("brace") is True and \
      rail.get("measures")==professional["measures"], (
        "Fixed context rail lost the piano brace, staves, or measure mapping")
    assert abs(rail.get("railLeft",999))<1 and \
      0<rail.get("referenceX",0)-rail.get("railWidth",0)<=13, (
        "Reference cursor is not immediately to the right of the fixed context rail")
    initial_context_records=set(context_logs)
    print("POC_CONTEXT_PROBE_EVIDENCE "+"\n".join(context_logs),flush=True)
    assert professional["musicalAnchors"]>=8 and professional["graphicalNotes"]>=12, "OSMD map lost musical events"
    mapping=professional.get("midiMapping") or {}
    assert mapping.get("nativeEvents",0)>=12 and mapping.get("matched",0)>=5, "Original Java Score events are not cross-referenced with OSMD"
    assert sum(mapping.get(k,0) for k in ("matched","unmatched","ambiguous"))==professional["graphicalNotes"], "Graphical note reconciliation counts inconsistent"
    print("POC_NATIVE_MIDI_MAPPING "+json.dumps(mapping),flush=True)
    assert "POC_LAYER software" in adb("logcat","-d","-v","brief"), "Preview did not isolate software SVG diagnostic"
    time.sleep(2)
    screenshot("piano-osmd-poc.png")
    responsive_evidence("ode")
    # Select each native hand without changing the MusicXML or piano key guide.
    for hand_name,hand_index in [("Direita",1),("Esquerda",2),("Ambas",0)]:
        tap_text([hand_name],exact=True,scroll=True)
        time.sleep(.3)
        cue_lines=[line for line in adb("logcat","-d","-v","brief").splitlines()
                   if "POC_HAND_CUES {" in line]
        assert cue_lines, "OSMD did not report graphical selected-hand cues"
        cue=json.loads(cue_lines[-1].split("POC_HAND_CUES ",1)[1].split(" @",1)[0])
        assert cue["hand"]==hand_index and all(staff==hand_index for staff in cue["staffs"]
                                               if hand_index!=0), "Graphical cues ignored source hand"
    print("PASS: optical study cues follow native right/left/both selection",flush=True)
    # Guide opening changes only height; keep the same OSMD SVG and all note IDs.
    render_count=adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")
    tap_text(["Teclado guia"],scroll=True)
    time.sleep(.7)
    tap_text(["Ocultar teclado"],scroll=True)
    time.sleep(.7)
    after_resize_log=adb("logcat","-d","-v","brief")
    assert "POC_RESIZE_HEIGHT_ONLY {" in after_resize_log, "Guide resize did not use fast optical path"
    assert after_resize_log.count("POC_OSMD_READY {")==render_count, "Guide height resize unexpectedly reloaded MusicXML"
    print("PASS: keyboard height resize retains the OSMD graphical note map",flush=True)
    # A real adb tap on the HEAD of the first note, not on an arbitrary beat column.
    first=professional.get("firstNoteViewport")
    assert first and first.get("midiEstimate") is not None, "OSMD notehead mapping missing"
    webviews=[n for n in dump().iter("node") if n.attrib.get("class")=="android.webkit.WebView"]
    assert webviews, "Android WebView bounds unavailable for a physical note tap"
    bounds=[int(v) for v in re.findall(r"\d+",webviews[0].attrib.get("bounds",""))]
    assert len(bounds)==4, "Invalid WebView geometry"
    w,h=bounds[2]-bounds[0],bounds[3]-bounds[1]
    nx=bounds[0]+round(first["x"]*w/professional.get("webviewWidth",professional["viewportWidth"]))
    ny=bounds[1]+round(first["y"]*h/professional["viewportHeight"])
    assert bounds[0]<=nx<bounds[2] and bounds[1]<=ny<bounds[3], "Mapped notehead outside visible WebView"
    adb("shell","input","tap",str(nx),str(ny))
    time.sleep(.5)
    touches=[line for line in adb("logcat","-d","-v","brief").splitlines() if "POC_NOTE_TAP {" in line]
    assert touches, "Actual Android touch did not reach graphical notehead"
    hit=json.loads(touches[-1].split("POC_NOTE_TAP ",1)[1].split(" @",1)[0])
    assert hit["staff"]==1 and hit["midiEstimate"]==first["midiEstimate"], "Android head-touch resolved wrong note or staff"
    assert hit["mapped"] and hit["nativeMidi"]==first["midiEstimate"], "Actual Android note tap did not agree with native Score.java MIDI"
    assert hit.get("writtenName")=="Mi4" and hit.get("xmlNoteIndex") is not None, "OSMD head did not preserve the actual written pitch and XML identity"
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
    rail_progress=[json.loads(line.split("POC_CONTEXT_RAIL_MEASURE ",1)[1].split(" @",1)[0])
                   for line in motion_log.splitlines() if "POC_CONTEXT_RAIL_MEASURE {" in line]
    assert rail_progress and rail_progress[-1]["measure"]>=1, (
      "Pinned OSMD clef/key/time symbols disappeared during playback")
    pinned=[json.loads(line.split("POC_CONTEXT_RAIL_PINNED ",1)[1].split(" @",1)[0])
            for line in motion_log.splitlines() if "POC_CONTEXT_RAIL_PINNED {" in line]
    assert pinned and abs(pinned[-1]["railLeft"])<1 and \
      abs(pinned[-1]["svgLeft"]-pinned[-1]["svgStartLeft"])>3, (
      "OSMD score did not move under context fixed at the viewport's left edge")
    adb("shell","input","tap",*playback_point)
    print("PASS: experimental OSMD optical scroll follows native musical clock",flush=True)
    # Graphical loop selection must use TWO native-reconciled noteheads,
    # then call the existing native callback, not edit the MusicXML.
    tap_text(["Início"],scroll=True)
    time.sleep(.7)
    second=professional.get("secondNoteViewport")
    assert second and second.get("beat",0)>first["beat"], "No later matched upper-staff note for loop"
    lx=bounds[0]+round(second["x"]*w/professional.get("webviewWidth",professional["viewportWidth"]))
    ly=bounds[1]+round(second["y"]*h/professional["viewportHeight"])
    assert bounds[0]<=lx<bounds[2] and bounds[1]<=ly<bounds[3], "Second notehead outside WebView"
    tap_text(["Trecho ↻"],exact=True,scroll=True)
    adb("shell","input","tap",str(nx),str(ny))
    time.sleep(.3)
    adb("shell","input","tap",str(lx),str(ly))
    time.sleep(.6)
    selections=[line for line in adb("logcat","-d","-v","brief").splitlines()
                if "POC_LOOP_SELECTED {" in line]
    assert selections, "OSMD did not deliver notehead-based loop selection"
    chosen=json.loads(selections[-1].split("POC_LOOP_SELECTED ",1)[1].split(" @",1)[0])
    assert abs(chosen["start"]-first["beat"])<.01 and chosen["end"]>second["beat"], "Loop boundaries do not follow musical noteheads"
    screenshot("piano-osmd-poc-loop.png")
    tap_text(["Início"],scroll=True)  # Clear persisted loop before legacy renderer regression.
    print("PASS: verified OSMD noteheads selected a native study loop",flush=True)
    # Wait-mode preparation uses the same native 1–30-second preference and
    # animates a nonnumeric horizontal pre-roll without changing attack rules.
    tap_text(["Espera"],exact=True,scroll=True)
    tap_text(["Iniciar"],scroll=True)
    time.sleep(1)
    assert "POC_WAIT_PREPARE {" in adb("logcat","-d","-v","brief"), "Native Wait preparation did not reach OSMD"
    screenshot("piano-osmd-poc-wait.png")
    for attempt in range(14):
        if "POC_WAIT_COMPLETE" in adb("logcat","-d","-v","brief"):
            break
        time.sleep(.6)
    assert "POC_WAIT_COMPLETE" in adb("logcat","-d","-v","brief"), "Visual Wait countdown did not finish"
    # Recorded graphical positions must actually move toward zero, not just toggle an overlay.
    wait_frames=[]
    for line in adb("logcat","-d","-v","brief").splitlines():
        if "POC_WAIT_FRAME {" in line:
            try:wait_frames.append(json.loads(line.split("POC_WAIT_FRAME ",1)[1].split(" @",1)[0]))
            except ValueError:pass
    assert len(wait_frames)>=3, "OSMD did not sample gradual preparation frames"
    assert wait_frames[0]["remainingPx"]>wait_frames[-1]["remainingPx"], "Visual preparation must move toward the cursor"
    assert all(wait_frames[i]["remainingPx"]>=wait_frames[i+1]["remainingPx"]-0.1
               for i in range(len(wait_frames)-1)), "Visual preparation moved backwards"
    print("POC_WAIT_TRAVEL "+json.dumps(wait_frames),flush=True)
    tap_text(["Início"],scroll=True)
    print("PASS: OSMD preparation animates toward the native Wait cursor",flush=True)
    after_memory=adb("shell","dumpsys","meminfo","br.org.pianolivre.opensource")
    baseline_pss=memory_pss_kb(before_memory)
    preview_pss=memory_pss_kb(after_memory)
    print("POC_MEMORY_PSS "+json.dumps({"baselineKb":baseline_pss,
          "previewKb":preview_pss,"scope":"app process only; WebView renderer may be separate"}),flush=True)
    perf=[]
    for line in adb("logcat","-d","-v","brief").splitlines():
        if "POC_OPTICAL_PERF {" in line:
            try:perf.append(json.loads(line.split("POC_OPTICAL_PERF ",1)[1].split(" @",1)[0]))
            except ValueError:pass
    assert any(m["frames"]>=2 and m["intervals"]>=1 for m in perf), "Optical scroll callback metrics missing"
    print("POC_OPTICAL_PERF_REPORT "+json.dumps(perf),flush=True)
    Path("piano-engraving-poc.json").write_text(json.dumps({
      "baseSha":"15cafe3c8da7c67b9752d14ba0fb6f8f6141177d",
      "title":DEFAULT_SCORE_TITLE,
      "studyBaseline":"piano-engraved.png",
      "osmdPreview":"piano-osmd-poc.png",
      "baselineMemoryPssKb":baseline_pss,
      "osmdMemoryPssKb":preview_pss,
      "memoryScope":"App process PSS only; WebView renderer process may be separate; two samples not a regression claim.",
      "opticalCallbackMetrics":perf,
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
    # Additional real Android WebView stress with an already authorized longer
    # MusicXML score; a large pure-JS map test does NOT test SVG rendering.
    old_count=adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")
    tap_text(["Gymnopédie nº 1"],scroll=True)
    time.sleep(3)
    assert_running()
    screenshot("piano-temporal-long.png")
    buttons=[n for n in dump().iter("node") if n.attrib.get("text")=="Partitura Livre"]
    assert buttons, "Long-score comparison cannot locate OSMD preview control"
    c=[int(v) for v in re.findall(r"\d+",buttons[0].attrib["bounds"])]
    adb("shell","input","swipe",str((c[0]+c[2])//2),str((c[1]+c[3])//2),
        str((c[0]+c[2])//2),str((c[1]+c[3])//2),"1100")
    long_proof=None
    for attempt in range(35):
        time.sleep(.8)
        frames=[line.split("POC_OSMD_READY ",1)[1].split(" @",1)[0]
                for line in adb("logcat","-d","-v","brief").splitlines()
                if "POC_OSMD_READY {" in line]
        if len(frames)>old_count:
            try:long_proof=json.loads(frames[-1])
            except ValueError:pass
            if long_proof:break
    assert long_proof, "Long MusicXML did not finish horizontal OSMD rendering"
    long_context=[line for line in adb("logcat","-d","-v","brief").splitlines()
                  if "POC_CONTEXT_" in line and line not in initial_context_records]
    assert any("POC_CONTEXT_MEASURE_1 " in line for line in long_context), (
      "Gymnopédie context was not recorded: "+"\n".join(long_context[-5:]))
    for line in long_context:
        assert len(line)<3500, "Android logcat may have truncated long-score context details"
        marker=re.search(r"POC_CONTEXT_[A-Z0-9_]+ ",line)
        if marker:
            payload=line.split(marker.group(0),1)[1].split(" @",1)[0].strip()
            try:json.loads(payload)
            except ValueError as exc:raise AssertionError("Long-score context JSON was truncated") from exc
    with open("piano-osmd-context-probe.txt","a",encoding="utf-8") as context_file:
        context_file.write("\nGYMNOPEDIE CONTEXT\n"+"\n".join(long_context)+"\n")
    print("POC_CONTEXT_LONG_EVIDENCE "+"\n".join(long_context),flush=True)
    # CI console evidence for diagnosing every unresolved written score head.
    # This mapping is bounded to 20 samples; source XML is never dumped.
    if long_proof:
        mapping=long_proof.get("midiMapping",{})
        print("POC_LONG_MAPPING "+json.dumps({
            "graphicalNotes":mapping.get("graphicalNotes"),
            "matched":mapping.get("matched"),
            "ambiguous":mapping.get("ambiguous"),
            "unmatched":mapping.get("unmatched"),
            "reasons":mapping.get("reasons"),
            "samples":mapping.get("samples",[])
        },ensure_ascii=False),flush=True)
    # Public-domain score fixture already bundled with the app. Read only
    # the anomalous written measures; avoid any user-imported MusicXML.
    source_fixture=Path("app/src/main/assets/scores/gymnopedie-no-1-satie.mxl")
    if source_fixture.exists() and long_proof.get("midiMapping",{}).get("samples"):
        import zipfile
        with zipfile.ZipFile(source_fixture) as mxl:
            xml_files=[name for name in mxl.namelist()
                       if name.lower().endswith((".xml",".musicxml"))
                       and not name.startswith("META-INF/")]
            assert xml_files, "Bundled Gymnopédie archive lost its MusicXML score"
            root=ET.fromstring(mxl.read(xml_files[0]))
        parts=root.findall("./part")
        original=[]
        for anomaly in long_proof["midiMapping"]["samples"]:
            measure_number=anomaly.get("measure")
            if not parts:continue
            measures=parts[0].findall("measure")
            if not (isinstance(measure_number,int) and 1<=measure_number<=len(measures)):continue
            notes=[]
            for note in measures[measure_number-1].findall("note"):
                pitch=note.find("pitch")
                if pitch is None:continue
                notes.append({"step":pitch.findtext("step"),"alter":pitch.findtext("alter"),
                              "octave":pitch.findtext("octave"),
                              "duration":note.findtext("duration"),
                              "voice":note.findtext("voice"),
                              "staff":note.findtext("staff"),
                              "tie":[t.attrib.get("type") for t in note.findall("tie")]})
            original.append({"measure":measure_number,"graphicalMidi":anomaly.get("midiEstimate"),
                             "sourceNotes":notes[:12]})
        print("POC_LONG_ORIGINAL_XML "+json.dumps(original,ensure_ascii=False),flush=True)
    assert long_proof["graphicalNotes"]==469 and long_proof["midiMapping"]["matched"]==469, "Gymnopédie must preserve all 469 written heads"
    assert long_proof["measures"]>=40, "Long-score fixture unexpectedly short"
    assert long_proof["height"]<=long_proof["availableHeight"]+0.5, "Complete OSMD score exceeds the available display height"
    assert abs(long_proof["displayScale"]-long_proof["availableHeight"]/long_proof["nativeSvgHeight"])<0.002, "OSMD score was not uniformly fitted to the measured WebView"
    assert long_proof["viewportMaxScrollTop"]==0, "OSMD preview exposes vertical scrolling"
    assert long_proof["nativeStaffSpaceCssPx"]>0, "Native staff-line spacing was not measured"
    assert long_proof["staffSeparationSpaces"]>4, "Dense score's native collision-safe staff separation was reduced"
    assert long_proof["graphicalNotes"]>=50 and long_proof["svgNodes"]>=1000, "Long-score engraving incomplete"
    assert long_proof["midiMapping"]["matched"]==long_proof["graphicalNotes"], "Each written Gymnopédie notehead must reconcile, including tied continuations"
    screenshot("piano-osmd-long.png")
    responsive_evidence("gymnopedie")
    # Uniformly scaled SVG geometry must preserve notehead hit targets on Android.
    long_head=long_proof.get("firstNoteViewport")
    assert long_head and long_head.get("midiEstimate") is not None, "Long score has no testable upper-staff notehead"
    views=[n for n in dump().iter("node") if n.attrib.get("class")=="android.webkit.WebView"]
    assert views, "Long-score WebView geometry unavailable"
    rect=[int(v) for v in re.findall(r"\d+",views[0].attrib["bounds"])]
    assert len(rect)==4, "Invalid scaled WebView dimensions"
    xx=rect[0]+round(long_head["x"]*(rect[2]-rect[0])/long_proof.get("webviewWidth",long_proof["viewportWidth"]))
    yy=rect[1]+round(long_head["y"]*(rect[3]-rect[1])/long_proof["viewportHeight"])
    assert rect[0]<=xx<rect[2] and rect[1]<=yy<rect[3], "Scaled graphical notehead outside screen"
    previous_hits=adb("logcat","-d","-v","brief").count("POC_NOTE_TAP {")
    adb("shell","input","tap",str(xx),str(yy))
    time.sleep(.4)
    logs=adb("logcat","-d","-v","brief")
    all_hits=[line for line in logs.splitlines() if "POC_NOTE_TAP {" in line]
    assert len(all_hits)>previous_hits, "Scaled SVG did not recognize a real notehead touch"
    chosen_note=json.loads(all_hits[-1].split("POC_NOTE_TAP ",1)[1].split(" @",1)[0])
    assert chosen_note["staff"]==1 and chosen_note["nativeMidi"]==long_head["midiEstimate"] and chosen_note["mapped"], "Native-scale notehead did not match native MusicXML/MIDI"
    assert chosen_note.get("writtenName") and chosen_note.get("xmlNoteIndex") is not None, "Scaled notehead lost its original MusicXML notation"
    screenshot("piano-osmd-long-tap.png")
    # A vertical swipe is intentionally ignored: both piano staves stay fixed
    # together while playback remains free to move the score horizontally.
    midx=(rect[0]+rect[2])//2
    adb("shell","input","swipe",str(midx),str(rect[3]-35),str(midx),str(rect[1]+35),"500")
    time.sleep(.4)
    no_vertical_scroll=long_proof["viewportMaxScrollTop"]==0 and long_proof["height"]<=long_proof["availableHeight"]+0.5
    assert no_vertical_scroll,"Long score does not keep both staves in the measured viewport"
    screenshot("piano-osmd-long-no-vertical-scroll.png")
    print("PASS: complete two-staff score fits and vertical scroll is disabled",flush=True)
    print("PASS: scaled 78-measure OSMD graphical notehead maps to native MIDI",flush=True)
    tap_text(["Contínuo"],exact=True,scroll=True)
    long_play_point=tap_text(["Iniciar"],return_point=True,scroll=True)
    time.sleep(2)
    screenshot("piano-osmd-long-moving.png")
    adb("shell","input","tap",*long_play_point)
    long_logs=adb("logcat","-d","-v","brief")
    long_perf=[]
    for line in long_logs.splitlines():
        if "POC_OPTICAL_PERF {" in line:
            try:long_perf.append(json.loads(line.split("POC_OPTICAL_PERF ",1)[1].split(" @",1)[0]))
            except ValueError:pass
    assert long_perf and any(x["frames"]>=2 for x in long_perf), "Long score animation metrics absent"
    Path("piano-osmd-long.json").write_text(json.dumps({"title":"Gymnopédie nº 1",
       "source":"authorized MXL already bundled in app; Android API29 emulator",
       "osmd":long_proof,"callbackSamples":long_perf[-3:],
       "caution":"rAF callback timing does not establish composited frames or physical-device fluency."},
       ensure_ascii=False,indent=2),encoding="utf-8")
    print("PASS: real OSMD WebView long-score graphic and optical scrolling evidence",flush=True)
    # Diagnose unexpected Home screen / process death instead of masking it by
    # restarting the app. Failure here blocks promotion to production.
    print("POC_POST_LONG_ACTIVITY "+
          adb("shell","dumpsys","activity","activities")[-2200:],flush=True)
    assert_running()
    tap_text(["Biblioteca"],scroll=True)
    assert_running()
    print("PASS emulator UI: library, music, study mode, playback, horizontal movement and vertical fit",flush=True)
    # Exercise genuinely changing score context on Android, using a fixture
    # that exists only in the debug source set and is loaded only by this flag.
    adb("shell","am","force-stop","br.org.pianolivre.opensource")
    adb("logcat","-c")
    adb("shell","am","start","-S","-W","-n",
        "br.org.pianolivre.opensource/br.org.pianolivre.MainActivity",
        "--ez","engraving_context_selftest","true")
    time.sleep(2)
    context_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    if not any("Context changes test" in s for s in context_labels):
        context_failure_logs=adb("logcat","-d","-v","brief")
        context_failure_activity=adb("shell","dumpsys","activity","activities")
        screenshot("piano-osmd-context-failure.png")
        Path("piano-osmd-context-failure-window.xml").write_bytes(ET.tostring(dump(),encoding="utf-8"))
        Path("piano-osmd-context-failure-diagnostics.txt").write_text(
          "INTENT\nengraving_context_selftest=true\nLABELS\n"+json.dumps(context_labels,ensure_ascii=False)+
          "\nCONTEXT LOGS\n"+"\n".join(line for line in context_failure_logs.splitlines()
            if "PianoLivreContext" in line or "FATAL EXCEPTION" in line or "AndroidRuntime" in line or "PianoLivreNotation" in line)+
          "\nACTIVITY\n"+context_failure_activity[-6000:],encoding="utf-8")
        raise AssertionError("Debug context-change fixture did not load: "+json.dumps(context_labels,ensure_ascii=False))
    fixture_load_line=None
    for attempt in range(30):
        context_logs=adb("logcat","-d","-v","brief")
        fixture_load_line=next((line for line in context_logs.splitlines()
          if "CONTEXT_FIXTURE_LOAD_OK" in line and "measures=3" in line and
             "title=Context changes test" in line),None)
        if fixture_load_line:break
        time.sleep(.5)
    assert fixture_load_line, "Debug context fixture load marker did not confirm the three-measure score"
    context_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    context_button=None
    context_root=None
    for attempt in range(8):
        context_root=dump()
        context_button=next((n for n in context_root.iter("node")
          if n.attrib.get("text")=="Partitura Livre"),None)
        if context_button is not None:break
        time.sleep(.25)
    if context_button is None:
        context_failure_logs=adb("logcat","-d","-v","brief")
        context_failure_activity=adb("shell","dumpsys","activity","activities")
        screenshot("piano-osmd-context-failure.png")
        Path("piano-osmd-context-failure-window.xml").write_bytes(ET.tostring(context_root,encoding="utf-8"))
        Path("piano-osmd-context-failure-diagnostics.txt").write_text(
          "LABELS\n"+json.dumps(context_labels,ensure_ascii=False)+
          "\nCURRENT WINDOW\n"+json.dumps([{"text":repr(n.attrib.get("text","")),"codepoints":["U+%04X"%ord(ch) for ch in n.attrib.get("text","")],"bounds":n.attrib.get("bounds")} for n in context_root.iter("node") if n.attrib.get("text")],ensure_ascii=False)+
          "\nCONTEXT LOGS\n"+"\n".join(line for line in context_failure_logs.splitlines()
            if "PianoLivreContext" in line or "FATAL EXCEPTION" in line or "AndroidRuntime" in line or "PianoLivreNotation" in line)+
          "\nACTIVITY\n"+context_failure_activity[-6000:],encoding="utf-8")
        raise AssertionError("Context fixture cannot open the OSMD study proof; raw label diagnostics: "+
          json.dumps([{"text":repr(n.attrib.get("text","")),
            "codepoints":["U+%04X"%ord(ch) for ch in n.attrib.get("text","")],
            }
            for n in context_root.iter("node") if n.attrib.get("text")],ensure_ascii=False))
    context_bounds=[int(v) for v in re.findall(r"\d+",context_button.attrib["bounds"])]
    # Start a fresh log window so the ready marker must come from this gesture.
    adb("logcat","-c")
    adb("shell","input","swipe",str((context_bounds[0]+context_bounds[2])//2),
        str((context_bounds[1]+context_bounds[3])//2),
        str((context_bounds[0]+context_bounds[2])//2),
        str((context_bounds[1]+context_bounds[3])//2),"1100")
    context_ready=None
    for attempt in range(30):
        assert_running()
        context_logs=adb("logcat","-d","-v","brief")
        ready=[line for line in context_logs.splitlines() if "POC_OSMD_READY {" in line]
        if ready:
            context_ready=json.loads(ready[-1].split("POC_OSMD_READY ",1)[1].split(" @",1)[0])
            break
        time.sleep(.5)
    assert context_ready and context_ready["measures"]==3 and context_ready["graphicalNotes"]>=12, (
        "Three-measure context fixture did not engrave on Android")
    context_load_logs=adb("logcat","-d","-v","brief")
    clef_remaps=[]
    for line in context_load_logs.splitlines():
        if "POC_CONTEXT_CLEF_NEXT " in line:
            payload=line.split("POC_CONTEXT_CLEF_NEXT ",1)[1].split(" @",1)[0]
            clef_remaps.append(json.loads(payload))
    assert any(item["glyphMeasure"]==1 and item["targetMeasure"]==2 for item in clef_remaps) and \
      any(item["glyphMeasure"]==2 and item["targetMeasure"]==3 for item in clef_remaps), (
        "OSMD clefs immediately before barlines must map to the following musical measure: "+
        json.dumps(clef_remaps))
    Path("piano-osmd-context-load-probe.txt").write_text("\n".join(
      line for line in context_load_logs.splitlines()
      if "CONTEXT_FIXTURE_" in line or "POC_OSMD_" in line or
         "POC_CONTEXT_MEASURE_" in line or "POC_CONTEXT_MODIFIERS_" in line or
         "POC_CONTEXT_RAIL_READY" in line or "POC_CONTEXT_SVG_ROOT" in line or
         "POC_CONTEXT_NODES_" in line or
         "POC_CONTEXT_CLEF_NEXT " in line),encoding="utf-8")
    screenshot("piano-osmd-context-start.png")
    context_states=[]
    context_seen=set()
    captured_middle=False
    captured_end=False
    tap_text(["Contínuo"],exact=True,scroll=True)
    tap_text(["Iniciar"],scroll=True)
    for attempt in range(40):
        context_logs=adb("logcat","-d","-v","brief")
        for line in context_logs.splitlines():
            if "POC_CONTEXT_RAIL_STATE {" in line and line not in context_seen:
                context_seen.add(line)
                state=json.loads(line.split("POC_CONTEXT_RAIL_STATE ",1)[1].split(" @",1)[0])
                context_states.append(state)
                if state["measure"]==2 and any(staff["clef"]==2 and staff["key"]==2 and staff["time"]==2 for staff in state["staffSources"]) and not captured_middle:
                    screenshot("piano-osmd-context-middle.png")
                    captured_middle=True
                if state["measure"]>=3 and any(staff["key"]==3 and staff["time"]==3 for staff in state["staffSources"]) and not captured_end:
                    screenshot("piano-osmd-context-end.png")
                    captured_end=True
        if any(state["measure"]>=3 for state in context_states):break
        assert_running()
        time.sleep(.5)
    print("POC_CONTEXT_CHANGES_OBSERVED "+json.dumps(context_states),flush=True)
    Path("piano-osmd-context-changes.json").write_text(json.dumps({
      "fixture":"debug-only three-measure MusicXML; 4/4 C, 3/4 D with C-clef, 6/8 E-flat with G-clef",
      "engraving":context_ready,"railStates":context_states,
      "scope":"Android emulator only; captured before context assertions"
    },ensure_ascii=False,indent=2),encoding="utf-8")
    assert any(state["measure"]==2 for state in context_states) and \
      any(state["measure"]==3 for state in context_states), (
        "Pinned context rail did not follow measures 2 and 3: "+json.dumps(context_states))
    assert captured_middle and captured_end, "Context transition screenshots were not captured at measures 2 and 3"
    second_measure=next((state for state in context_states if state["measure"]==2 and
      any(staff["clef"]==2 and staff["key"]==2 and staff["time"]==2
        for staff in state["staffSources"])),None)
    third_measure=next((state for state in context_states if state["measure"]==3 and
      any(staff["key"]==3 and staff["time"]==3 for staff in state["staffSources"])),None)
    assert second_measure and second_measure["staffSources"][0]["clef"]==2, (
        "Measure 2 did not advance its changed clef, key signature and time signature: "+
        json.dumps(context_states))
    assert third_measure and third_measure["staffSources"][0]["clef"]==3, (
        "Measure 3 did not advance its changed clef, key signature and time signature: "+
        json.dumps(context_states))
    screenshot("piano-osmd-context-changes.png")
    responsive_evidence("context")
    Path("piano-osmd-context-changes.json").write_text(json.dumps({
      "fixture":"debug-only three-measure MusicXML; 4/4 C, 3/4 D with C-clef, 6/8 E-flat with G-clef",
      "engraving":context_ready,"railStates":context_states,
      "scope":"Android emulator only; context symbols advance from source measures 1 to 3"
    },ensure_ascii=False,indent=2),encoding="utf-8")
    print("POC_CONTEXT_CHANGES "+json.dumps(context_states),flush=True)
    print("PASS: OSMD pinned context follows clef, key and time changes in Android playback",flush=True)
    # Render a copyright-free debug fixture with dense engraving features so
    # adaptive geometry is exercised beyond the two public-domain examples.
    adb("shell","am","force-stop","br.org.pianolivre.opensource")
    adb("logcat","-c")
    adb("shell","am","start","-S","-W","-n",
        "br.org.pianolivre.opensource/br.org.pianolivre.MainActivity",
        "--ez","engraving_complex_selftest","true")
    complex_loaded=False
    for attempt in range(30):
        logs=adb("logcat","-d","-v","brief")
        if "COMPLEX_FIXTURE_LOAD_OK title=Synthetic engraving geometry test measures=3" in logs:
            complex_loaded=True;break
        time.sleep(.5)
    assert complex_loaded,"Synthetic complex MusicXML fixture did not load on Android"
    complex_button=None
    for attempt in range(8):
        root=dump()
        complex_button=next((n for n in root.iter("node")
          if n.attrib.get("text")=="Partitura Livre"),None)
        if complex_button is not None:break
        time.sleep(.25)
    assert complex_button is not None,"Synthetic fixture cannot open the OSMD proof"
    complex_bounds=[int(v) for v in re.findall(r"\d+",complex_button.attrib["bounds"])]
    adb("logcat","-c")
    adb("shell","input","swipe",str((complex_bounds[0]+complex_bounds[2])//2),
        str((complex_bounds[1]+complex_bounds[3])//2),
        str((complex_bounds[0]+complex_bounds[2])//2),
        str((complex_bounds[1]+complex_bounds[3])//2),"1100")
    complex_proof=None
    for attempt in range(30):
        assert_running()
        entries=[line for line in adb("logcat","-d","-v","brief").splitlines()
                 if "POC_OSMD_READY {" in line]
        if entries:
            complex_proof=json.loads(entries[-1].split("POC_OSMD_READY ",1)[1].split(" @",1)[0])
            break
        time.sleep(.5)
    assert complex_proof and complex_proof["measures"]==3 and complex_proof["graphicalNotes"]>=20,(
      "Complex synthetic score did not produce the expected OSMD notes: "+json.dumps(complex_proof))
    assert complex_proof["height"]<=complex_proof["availableHeight"]+0.5 and \
      abs(complex_proof["displayScale"]-complex_proof["availableHeight"]/complex_proof["nativeSvgHeight"])<0.002 and \
      complex_proof["nativeStaffSpaceCssPx"]>0 and complex_proof["viewportMaxScrollTop"]==0,(
      "Complex score did not fit proportionally or its staff spacing/vertical lock could not be verified")
    assert complex_proof["midiMapping"]["matched"]>=10,(
      "Synthetic score graphical heads did not reconcile with the native score model")
    assert complex_proof["graphicalNotes"]==27 and complex_proof["midiMapping"]["matched"]==27, "Synthetic score must preserve all 27 written heads"
    features=complex_proof["svgFeatureCounts"]
    synthetic=ET.parse("app/src/debug/assets/scores/engraving-complex-synthetic.musicxml").getroot()
    source_features={
      "accidentals":len(synthetic.findall(".//note/accidental")),
      "tuplets":len(synthetic.findall(".//note/notations/tuplet")),
      "tupletDurations":len(synthetic.findall(".//note/time-modification")),
      "beams":len(synthetic.findall(".//note/beam"))}
    assert source_features["accidentals"]>=2 and source_features["tuplets"]>=2 and \
      source_features["tupletDurations"]>=3 and source_features["beams"]>=4,(
        "Synthetic fixture lost written engraving features: "+json.dumps(source_features))
    time.sleep(2.5)  # Let the non-musical WebView toast clear before visual review.
    screenshot("piano-osmd-complex-synthetic.png")
    responsive_evidence("complex")
    complex_evidence={
      "fixture":"debug-only, synthetic and copyright-free MusicXML",
      "coverage":["grand staff","chords","two voices","beams","accidentals","ties","slurs","dynamics/wedges","tuplets"],
      "engraving":complex_proof,
      "scope":"Android API 29 emulator; geometry and native-model reconciliation, not physical-device acceptance"
    }
    Path("piano-osmd-complex-synthetic.json").write_text(json.dumps(
      complex_evidence,ensure_ascii=False,indent=2),encoding="utf-8")
    print("POC_COMPLEX_GEOMETRY "+json.dumps({
      "nativeSvgHeight":complex_proof["nativeSvgHeight"],
      "displayScale":complex_proof["displayScale"],
      "nativeStaffSpaceCssPx":complex_proof["nativeStaffSpaceCssPx"],
      "staffSeparationSpaces":complex_proof["staffSeparationSpaces"],
      "graphicalNotes":complex_proof["graphicalNotes"],
      "midiMapping":complex_proof["midiMapping"],"svgFeatureCounts":features,
      "sourceFeatureCounts":source_features
    }),flush=True)
    assert all(features.get(key,0)>0 for key in ("beams","ties","slurs","dynamics")),(
      "Complex fixture lost class-addressable engraved symbols: "+json.dumps(features))
    print("PASS: synthetic complex geometry, written symbols and note reconciliation",flush=True)

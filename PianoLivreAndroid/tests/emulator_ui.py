#!/usr/bin/env python3
"""Automated Android emulator GUI smoke test; no physical MIDI required."""
import os, re, subprocess, time, xml.etree.ElementTree as ET, json, sys
from pathlib import Path
DEFAULT_SCORE_TITLE = json.loads(Path('app/src/main/assets/catalog.json').read_text(encoding='utf-8'))[0]['name']

def adb(*args, timeout=70):
    result=subprocess.run(["adb", *args], check=False, text=True,
                          stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                          timeout=timeout)
    if result.returncode:
        print(result.stdout,flush=True)
        raise subprocess.CalledProcessError(result.returncode,["adb",*args],output=result.stdout)
    return result.stdout

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

def capture_emulator_failure(kind,error,traceback):
    try:
        logs=adb("logcat","-d","-v","brief")
        Path("piano-ui-failure.log").write_text(logs,encoding="utf-8")
        screenshot("piano-ui-failure.png")
        relevant=[line for line in logs.splitlines() if any(marker in line for marker in
            ("POC_NOTE_TAP ","POC_LOOP_","STUDY_CHROME_","FATAL EXCEPTION","RENDER_ERROR"))]
        print("ANDROID_FAILURE_DIAGNOSTICS\n"+"\n".join(relevant[-35:]),flush=True)
    except Exception as diagnostic_error:print("Failure diagnostics unavailable:",diagnostic_error,flush=True)
    sys.__excepthook__(kind,error,traceback)

sys.excepthook=capture_emulator_failure

def home_responsive_screenshots():
    from home_layout_checks import capture_home_viewports
    capture_home_viewports()


def home_scroll_to_top():
    for _ in range(20):
        root=dump()
        scroll=next((n for n in root.iter("node") if n.attrib.get("scrollable")=="true" and "Horizontal" not in n.attrib.get("class","")),None)
        if scroll is None:break
        box=[int(x) for x in re.findall(r"\d+",scroll.attrib.get("bounds",""))]
        if len(box)!=4:break
        header_visible=any(n.attrib.get("content-desc")=="Piano Livre" and
                           len(re.findall(r"\d+",n.attrib.get("bounds","")))==4 and
                           int(re.findall(r"\d+",n.attrib["bounds"])[3])>int(re.findall(r"\d+",n.attrib["bounds"])[1])
                           for n in root.iter("node"))
        x=box[0]+max(8,(box[2]-box[0])//100);top=box[1];height=box[3]-box[1]
        adb("shell","input","swipe",str(x),str(top+height//5),str(x),str(top+height*4//5),"220")
        time.sleep(.15)
        if header_visible:break # One final downward swipe clamps a partial header to the top.
    time.sleep(.3)

def current_study_layout():
    pid=adb("shell","pidof","br.org.pianolivre.opensource").strip()
    rows=[line for line in adb("logcat","-d","--pid="+pid,"-v","brief").splitlines() if "STUDY_CHROME_LAYOUT " in line]
    return json.loads(rows[-1].split("STUDY_CHROME_LAYOUT ",1)[1]) if rows else None

def reveal_study_controls():
    layout=current_study_layout()
    if layout and not layout["headerVisible"]:box=layout["bounds"]
    else:
        root=dump()
        views=[n for n in root.iter("node") if n.attrib.get("class")=="android.webkit.WebView"]
        assert views,"Study WebView missing"
        box=[int(v) for v in re.findall(r"\d+",views[0].attrib["bounds"])]
    adb("shell","input","tap",str(box[2]-12),str(box[3]-12));time.sleep(.6)

def study_renderer(temporal):
    """Debug-only renderer fixture; no manual production selector exists."""
    adb("shell","am","force-stop","br.org.pianolivre.opensource")
    adb("shell","am","start","-W","-n","br.org.pianolivre.opensource/br.org.pianolivre.MainActivity",
        "--ez","study_temporal_selftest","true" if temporal else "false")
    time.sleep(2);assert_running()

def assert_study_controls():
    labels=[n.attrib.get("text","") for n in dump().iter("node")]
    forbidden=("Teclado guia","Ocultar teclado","OSMD · nova","Anterior · temporal","Partitura Original")
    assert not any(value in labels for value in forbidden),"Obsolete Study control returned"
    assert "Espera" in labels and "Contínuo" in labels,"Mode controls missing"

def responsive_evidence(name):
    """Measure real normal/playing/revealed WebView bounds and uniform staff fit."""
    result=[]
    normal_transport=None
    tap_text(["Parar"]);tap_text(["Contínuo"],exact=True,scroll=True);tap_text(["Início"])
    root=dump();slider=next(n for n in root.iter("node") if n.attrib.get("class")=="android.widget.SeekBar")
    box=[int(v) for v in re.findall(r"\d+",slider.attrib["bounds"])]
    adb("shell","input","tap",str(box[0]+2),str((box[1]+box[3])//2))
    for state in ("normal","playing","restored"):
        if state=="playing":tap_text(["Iniciar"])
        if state=="restored":reveal_study_controls()
        time.sleep(.8)
        if state!="normal":
            # UiAutomator waits for accessibility idle and cannot dump a live animation.
            # Read the actual native post-layout bounds; never pause/freeze the score.
            layout=current_study_layout()
            assert layout and layout["headerVisible"]==(state=="restored") and layout["modesVisible"]==(state=="restored"),"Chrome hide/reveal failed"
            box=layout["bounds"]
            assert layout["transportBounds"][3]>layout["transportBounds"][1],"Permanent transport lost"
            assert layout["transportBounds"]==normal_transport,"Transport moved during chrome transition"
            assert box[3]==layout["transportBounds"][1],"Score overlaps the permanent transport"
        else:
            root=dump();labels=[n.attrib.get("text","") for n in root.iter("node")]
            assert "Espera" in labels,"Chrome reveal failed"
            assert any("Parar" in (n.attrib.get("text","")+n.attrib.get("content-desc","")) for n in root.iter("node")),"Permanent transport lost"
            view=next(n for n in root.iter("node") if n.attrib.get("class")=="android.webkit.WebView")
            box=[int(v) for v in re.findall(r"\d+",view.attrib["bounds"])]
            normal_transport=current_study_layout()["transportBounds"]
        height=box[3]-box[1]
        density=int(re.findall(r"(?:Override|Physical) density: (\d+)",adb("shell","wm","density"))[-1])/160
        geometry=None
        for attempt in range(25):
            rows=[line for line in adb("logcat","-d","-v","brief").splitlines() if "POC_SVG_FIT {" in line]
            if rows:
                candidate=json.loads(rows[-1].split("POC_SVG_FIT ",1)[1].split(" @",1)[0])
                if abs(candidate["viewportHeight"]-height/density)<2:geometry=candidate;break
            time.sleep(.4)
        assert geometry,"Fresh proportional staff fit missing after "+state
        assert geometry["scale"]>0 and geometry["height"]<=geometry["availableHeight"]+.5
        assert geometry["viewportMaxScrollTop"]==0,"Staff vertical clipping/scroll"
        assert abs(geometry["scale"]-geometry["availableHeight"]/geometry["rawHeight"])<.002
        screenshot("piano-03d-"+name+"-"+state+".png")
        result.append({"state":state,"heightPx":height,"heightDp":height/density,"geometry":geometry})
    assert result[1]["heightPx"]>result[0]["heightPx"],"Execution gained no score area"
    assert result[0]["heightPx"]==result[-1]["heightPx"],"Reveal did not restore height"
    tap_text(["Parar"]);tap_text(["Início"]);tap_text(["Espera"],exact=True,scroll=True)
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

def wait_for_text(target, timeout=8):
    """Wait for a native UI update to become observable after an input event."""
    deadline=time.monotonic()+timeout
    last=[]
    while time.monotonic()<deadline:
        last=[n.attrib.get("text","") or n.attrib.get("content-desc","") for n in dump().iter("node")]
        if any(target.casefold() in value.casefold() for value in last):return last
        time.sleep(.25)
    raise AssertionError("Timed out waiting for UI text "+repr(target)+"; visible="+repr(last[:50]))

def focus_library_search_field(timeout=12):
    """Wait for the library search row to stop moving, tap it, and confirm focus."""
    deadline=time.monotonic()+timeout
    previous=None;stable=0;last=[]
    while time.monotonic()<deadline:
        root=dump()
        fields=[n for n in root.iter("node") if n.attrib.get("class")=="android.widget.EditText"]
        search=next((n for n in fields if "buscar título" in (n.attrib.get("hint","")+n.attrib.get("text","")+n.attrib.get("content-desc","")).casefold()),None)
        if search is None and len(fields)==1:search=fields[0]
        if search is not None:
            bounds=[int(x) for x in re.findall(r"\d+",search.attrib.get("bounds",""))]
            signature=tuple(bounds) if len(bounds)==4 else None
            stable=stable+1 if signature is not None and signature==previous else 0
            previous=signature;last=[n.attrib.get("text","") for n in root.iter("node")]
            if stable>=1 and signature is not None:
                for _ in range(3):
                    adb("shell","input","tap",str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2))
                    time.sleep(.25)
                    focused_root=dump()
                    focused_fields=[n for n in focused_root.iter("node") if n.attrib.get("class")=="android.widget.EditText"]
                    focused=next((n for n in focused_fields if n.attrib.get("focused")=="true"),None)
                    if focused is not None:return focused
                    state=adb("shell","dumpsys","input_method")
                    if re.search(r"(?:mInputShown|mIsInputViewShown|isInputViewShown)=true",state):
                        return next((n for n in focused_fields if "buscar título" in (n.attrib.get("hint","")+n.attrib.get("text","")+n.attrib.get("content-desc","")).casefold()),search)
                time.sleep(.25)
        time.sleep(.2)
    raise AssertionError("LIBRARY_SEARCH_FOCUS_FAILED: field did not settle/focus; labels="+repr(last[:40]))

def assert_collection_filter_option(name, present=True):
    """Inspect the Spinner's real dropdown; closed spinners expose only selection."""
    home_scroll_to_top()
    tap_text(["Biblioteca Musical"],exact=True)
    wait_for_text("Categoria")
    tap_text(["Categoria"],exact=True,scroll=True)
    assert any(n.attrib.get("text")=="Todas as categorias" for n in dump().iter("node")), "Full category option missing in dropdown"
    target="Coleção: "+name
    if present:
        wait_for_text(target)
    else:
        time.sleep(.35)
    labels=[n.attrib.get("text","") for n in dump().iter("node")]
    found=any(value==target for value in labels)
    adb("shell","input","keyevent","4")
    if found!=present:
        raise AssertionError("Collection filter option presence mismatch for "+repr(target)+"; options="+repr(labels))

def return_to_library_if_study_was_restored():
    labels=[n.attrib.get("text","") or n.attrib.get("content-desc","") for n in dump().iter("node")]
    if any("← Biblioteca" in (n.attrib.get("text","")+n.attrib.get("content-desc","")) for n in dump().iter("node")):
        tap_text(["← Biblioteca"],exact=True)
        wait_for_text("Bem-vindo ao")

def tap_text(targets, return_point=False, exact=False, scroll=False, _scroll_pass=0):
    if any(value in ("Parar","Iniciar","Pausar") for value in targets):
        layout=current_study_layout()
        if layout and layout["transportActive"]:
            for button in layout["buttons"]:
                labels=[button["text"],button["description"]]
                if any(any((value.casefold()==label.casefold()) if exact else (value.casefold() in label.casefold()) for value in targets) for label in labels):
                    box=button["bounds"];point=(str((box[0]+box[2])//2),str((box[1]+box[3])//2))
                    adb("shell","input","tap",*point);time.sleep(.2)
                    return point if return_point else button["text"]
    root=dump()
    for node in root.iter("node"):
        labels=[node.attrib.get("text",""),node.attrib.get("content-desc","")]
        match=next((label for label in labels if label and any((value.casefold()==label.casefold()) if exact else (value.casefold() in label.casefold()) for value in targets)),None)
        if match is not None:
            nums=[int(x) for x in re.findall(r"\d+",node.attrib.get("bounds",""))]
            if len(nums)==4:
                x,y=(nums[0]+nums[2])//2,(nums[1]+nums[3])//2
                # Accessibility bounds already use the current rotated display
                # coordinate space. `wm size` reports the unrotated physical
                # dimensions on some Android emulator versions, which can
                # incorrectly reject valid landscape taps.
                adb("shell", "input", "tap", str(x),str(y))
                print("Tapped",match,flush=True)
                return (str(x),str(y)) if return_point else match
    labels=[n.attrib.get("text","") or n.attrib.get("content-desc","") for n in root.iter("node")]
    if _scroll_pass==0 and any("Opções de estudo" in n.attrib.get("content-desc","") for n in root.iter("node")) and targets==["Partitura Livre"]:
        tap_text(["Opções de estudo"],exact=True)
        return tap_text(targets,return_point,exact,scroll,1)
    if _scroll_pass==0 and "Espera" not in labels and any("Pausar" in value or "Esperando" in value for value in labels) and any(value in ("Biblioteca","Contínuo","Espera","Demonstração","Partitura Livre") for value in targets):
        reveal_study_controls()
        return tap_text(targets,return_point,exact,scroll,1)
    if scroll and _scroll_pass<10:
        scroll_nodes=[node for node in root.iter("node") if node.attrib.get("scrollable")=="true"]
        # Study controls use a horizontal scroller; prioritize it so off-screen
        # hand choices remain testable after adding the renderer selectors.
        scroll_nodes.sort(key=lambda node:"Horizontal" not in node.attrib.get("class",""))
        for node in scroll_nodes:
            box=[int(x) for x in re.findall(r"\d+",node.attrib.get("bounds",""))]
            if len(box)==4:
                if "Horizontal" in node.attrib.get("class",""):
                    y=str((box[1]+box[3])//2)
                    if _scroll_pass>=5:
                        adb("shell","input","swipe",str(box[0]+12),y,str(box[2]-12),y,"400")
                    else:
                        adb("shell","input","swipe",str(box[2]-12),y,str(box[0]+12),y,"400")
                else:
                    x=str((box[0]+box[2])//2)
                    # Search toward the bottom first, then reverse and search
                    # toward the top. This handles toolbar controls after a file
                    # picker returns with the library list at its last row.
                    start_y,end_y=(box[3]-15,box[1]+15)
                    if _scroll_pass>=5:
                        start_y,end_y=end_y,start_y
                    adb("shell","input","swipe",x,str(start_y),x,str(end_y),"400")
                return tap_text(targets,return_point,exact,True,_scroll_pass+1)
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
    assert "USB-MIDI" in names, "Disconnected home MIDI control must use the USB-MIDI label"
    assert not any("piano conectado" in n.casefold() or "toque aqui para testar" in n.casefold() for n in names), "Redundant connection text leaked into the home header"
    print("PASS: library visible at startup",flush=True)
    home_responsive_screenshots()
    # The category spinner can be below the fold on short landscape displays.
    # Exercise it through the same scroll view while retaining the top-of-home
    # screenshots above as the visual evidence for each viewport.
    home=dump()
    category_accessible=any(n.attrib.get("text","")=="Categoria" for n in home.iter("node"))
    if not category_accessible:
        tap_text(["Categoria"],exact=True,scroll=True)
        category_accessible=True
        adb("shell","input","keyevent","4")
        home_scroll_to_top()
    assert category_accessible, "Initial library filter label missing"
    tap_text(["USB-MIDI"],exact=True)
    midi_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    if any("Mostrar diagnóstico" in t for t in midi_labels):tap_text(["Mostrar diagnóstico"],scroll=True)
    labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Mensagens recebidas" in t for t in labels), "MIDI management/diagnostics did not open"
    tap_text(["Fechar"])
    tap_text(["Como usar"],scroll=True)
    help_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Escolha uma partitura" in t for t in help_labels), "Local help content missing"
    tap_text(["Entendi"],exact=True)
    tap_text(["Biblioteca Musical"],exact=True);wait_for_text("10 de 10 partituras")
    tap_text(["Favoritar Ode à Alegria"]);tap_text(["Remover favorito Ode à Alegria"])
    adb("shell","am","force-stop","br.org.pianolivre.opensource")
    adb("shell","am","start","-n","br.org.pianolivre.opensource/br.org.pianolivre.MainActivity")
    time.sleep(1.5);assert_running();return_to_library_if_study_was_restored()
    tap_text(["Começar pela Ode à Alegria"],scroll=True)
    time.sleep(2)
    assert any("Ode à Alegria" in n.attrib.get("text","") for n in dump().iter("node")), "Welcome banner did not open Ode à Alegria in Study"
    tap_text(["Biblioteca"])
    time.sleep(.4)
    print("PASS: welcome actions, offline help, and phone/tablet responsive captures",flush=True)
    home_scroll_to_top()
    tap_text(["Importar partitura"],scroll=True)
    time.sleep(.5)
    adb("shell","input","keyevent","4")
    # Returning from the system picker may preserve the library's prior scroll
    # offset; expose the real filter row again before tapping it.
    home_scroll_to_top()
    tap_text(["Coleções"],scroll=True)
    tap_text(["Nova coleção"],exact=True)
    fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    assert fields, "New collection name input missing"
    box=[int(x) for x in re.findall(r"\d+",fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((box[0]+box[2])//2),str((box[1]+box[3])//2))
    adb("shell","input","text","Colecao_Teste_UI")
    dismiss_keyboard();tap_text(["Salvar"],exact=True)
    assert_collection_filter_option("Colecao_Teste_UI")
    tap_text(["Coleções"],scroll=True);tap_text(["Renomear coleção"],exact=True);tap_text(["Colecao_Teste_UI"],exact=True)
    fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    assert fields, "Rename collection input missing"
    box=[int(x) for x in re.findall(r"\d+",fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((box[0]+box[2])//2),str((box[1]+box[3])//2))
    adb("shell","input","keyevent","123");adb("shell","input","keyevent",*( ["67"]*30 ))
    adb("shell","input","text","Colecao_Renomeada")
    dismiss_keyboard();tap_text(["Salvar"],exact=True);assert_collection_filter_option("Colecao_Renomeada")
    adb("shell","am","force-stop","br.org.pianolivre.opensource")
    adb("shell","am","start","-n","br.org.pianolivre.opensource/br.org.pianolivre.MainActivity")
    time.sleep(1.5);assert_running();return_to_library_if_study_was_restored();assert_collection_filter_option("Colecao_Renomeada")
    tap_text(["Coleções"],scroll=True);tap_text(["Excluir coleção"],exact=True);tap_text(["Colecao_Renomeada"],exact=True);tap_text(["Excluir"],exact=True)
    assert_collection_filter_option("Colecao_Renomeada",present=False)
    adb("shell","am","force-stop","br.org.pianolivre.opensource")
    adb("shell","am","start","-n","br.org.pianolivre.opensource/br.org.pianolivre.MainActivity")
    time.sleep(1.5);assert_running();return_to_library_if_study_was_restored();assert_collection_filter_option("Colecao_Renomeada",present=False)
    home_scroll_to_top()
    print("PASS: import entry, collections, and favorite controls",flush=True)
    home_scroll_to_top()
    # Public Laboratory 1.0 exposes only the approved Discover experience.
    tap_text(["Laboratório Musical"],exact=True,scroll=True)
    lab_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any(t=="Descobrir" for t in lab_labels), "Discover screen not shown"
    assert any(t=="Aprender" for t in lab_labels), "Disabled Learn tab missing"
    assert any(t=="toque uma nota, acorde ou escala musical" for t in lab_labels), "Exact initial instruction missing"
    forbidden={"Iniciar","Gravar","Praticar","Metrônomo","Demonstração","Montar","Teclado"}
    assert not (forbidden & set(lab_labels)), "Legacy controls leaked into Discover: "+str(forbidden & set(lab_labels))
    tap_text(["Voltar"],exact=True)
    assert_running()
    assert any("Biblioteca Musical" in n.attrib.get("text","") for n in dump().iter("node")), "Laboratory return lost library"
    print("PASS: public Laboratory Discover entry, minimal empty state and deterministic return",flush=True)
    screenshot("piano-library.png")
    # Chord calibration must be discoverable, with no magic millisecond default.
    home_scroll_to_top();tap_text(["Opções e configurações"],exact=True)
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
    # Create the history entry the assertion checks instead of depending on
    # a seeded record from a previous test or emulator snapshot.
    home_scroll_to_top();tap_text(["Partitura livre"],exact=True,scroll=True)
    wait_for_text("← Biblioteca");time.sleep(1)
    tap_text(["← Biblioteca"],exact=True);wait_for_text("Estudar Ode à Alegria")
    home_scroll_to_top();tap_text(["Histórico"],exact=True,scroll=True)
    history_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Tempo na tela de estudo" in t for t in history_labels), "Native history totals missing"
    tap_text(["Períodos"],exact=True)
    history_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert any("Leitura livre" in t for t in history_labels), "Free-reading period missing"
    tap_text(["Fechar"],exact=True)
    print("PASS: native history totals and free-reading periods",flush=True)
    # Native profile UI: create, avatar and confirmed deletion, with stable identity.
    home_scroll_to_top();tap_text(["+"],exact=True)
    fields=[n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.EditText"]
    assert fields, "New profile name input missing"
    box=[int(x) for x in re.findall(r"\d+",fields[0].attrib["bounds"])]
    adb("shell","input","tap",str((box[0]+box[2])//2),str((box[1]+box[3])//2))
    adb("shell","input","text","Perfil_Teste")
    dismiss_keyboard()
    tap_text(["Criar"],exact=True)
    created=[n.attrib.get("text","") for n in dump().iter("node")]
    assert "Novo perfil" not in created and any("Perfil_Teste" in t for t in created) and any("PIANO LIVRE" in t for t in created), "New profile was not created and selected"
    tap_text(["Opções e configurações"],exact=True);tap_text(["Gerenciar perfil"],scroll=True);tap_text(["Escolher avatar"]);tap_text(["★"],exact=True)
    assert any("★ Perfil_Teste" in n.attrib.get("text","") for n in dump().iter("node")), "Avatar was not retained"
    tap_text(["Opções e configurações"],exact=True);tap_text(["Gerenciar perfil"],scroll=True);tap_text(["Excluir perfil"]);tap_text(["Excluir"],exact=True)
    after_delete=[n.attrib.get("text","") for n in dump().iter("node")]
    assert not any("Perfil_Teste" in t for t in after_delete), "Deleted profile remains selected"
    if any("← Biblioteca" in (n.attrib.get("text","")+n.attrib.get("content-desc","")) for n in dump().iter("node")):
        # Deleting the temporary profile restores home, which owns its own
        # welcome action instead of the in-study score toolbar.
        tap_text(["← Biblioteca"],exact=True);wait_for_text("Começar pela Ode à Alegria")
    tap_text(["Biblioteca Musical"],exact=True)
    print("PASS: profile creation, avatar, confirmed deletion and survivor selection",flush=True)
    wait_for_text("Buscar título ou arquivo")
    focus_library_search_field()
    adb("shell","input","text","gymnopedie")
    dismiss_keyboard()
    wait_for_text("1 de 10 partituras")
    focus_library_search_field()
    adb("shell","input","keyevent","123")
    adb("shell","input","keyevent",*(["67"]*10))
    dismiss_keyboard()
    wait_for_text("10 de 10 partituras")
    focus_library_search_field()
    adb("shell","input","text","lago-dos-cisnes")
    dismiss_keyboard()
    wait_for_text("1 de 10 partituras")
    focus_library_search_field()
    adb("shell","input","keyevent","123")
    adb("shell","input","keyevent",*(["67"]*20))
    dismiss_keyboard()
    wait_for_text("10 de 10 partituras")
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
    # OSMD must render by default; the temporal alternative is visibly selectable.
    before_memory=adb("shell","dumpsys","meminfo","br.org.pianolivre.opensource")
    professional=None
    for attempt in range(24):
        time.sleep(.65)
        proof_logs=adb("logcat","-d","-v","brief")
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
            "OSMD_DEFAULT_READY="+str(bool(professional))+"\n\nACTIVITY\n"+activity+
            "\n\nPROCESSES\n"+processes+"\n\nLOGCAT\n"+proof_logs,encoding="utf-8")
        relevant=[line for line in proof_logs.splitlines() if any(token in line for token in
          ("PianoLivreNotation","FATAL EXCEPTION","AndroidRuntime","chromium","ANR"))]
        print("POC_FAILURE_STATE pid="+
          adb("shell","pidof","br.org.pianolivre.opensource").strip(),flush=True)
        print("POC_FAILURE_ACTIVITY "+activity[-8000:],flush=True)
        print("POC_FAILURE_LOGCAT\n"+"\n".join(relevant[-240:]),flush=True)
    assert professional, "OSMD did not render automatically when Study opened"
    initial_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert_study_controls()
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
    # Reconciliation is already validated above from the parsed OSMD proof.
    # The old layer diagnostic was emitted only by the experimental toggle and
    # is irrelevant now that Study opens directly in OSMD.
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
    # Chrome transitions keep the same SVG and graphical note IDs.
    after_resize_log=adb("logcat","-d","-v","brief")
    assert "POC_RESIZE_HEIGHT_ONLY {" in after_resize_log,"Chrome resize did not use fast optical path"
    print("PASS: chrome height resize retains the OSMD graphical note map",flush=True)
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
    # Capture the ACTUAL visible popup before multiple logcat/ADB round trips
    # consume its required two-second lifetime.
    time.sleep(.25)
    screenshot("piano-osmd-poc-note-tap.png")
    time.sleep(.25)
    touches=[line for line in adb("logcat","-d","-v","brief").splitlines() if "POC_NOTE_TAP {" in line]
    assert touches, "Actual Android touch did not reach graphical notehead"
    hit=json.loads(touches[-1].split("POC_NOTE_TAP ",1)[1].split(" @",1)[0])
    assert hit["staff"]==1 and hit["midiEstimate"]==first["midiEstimate"], "Android head-touch resolved wrong note or staff"
    assert hit["mapped"] and hit["nativeMidi"]==first["midiEstimate"], "Actual Android note tap did not agree with native Score.java MIDI"
    assert hit.get("writtenName")=="Mi4" and hit.get("xmlNoteIndex") is not None, "OSMD head did not preserve the actual written pitch and XML identity"
    visible=[line for line in adb("logcat","-d","-v","brief").splitlines()
             if "POC_NOTE_TIP_VISIBLE {" in line]
    assert visible, "OSMD tap did not show a bounded temporary identification"
    popup=json.loads(visible[-1].split("POC_NOTE_TIP_VISIBLE ",1)[1].split(" @",1)[0])
    assert popup["label"]=="Mi4 · E4", "Note popup lost international written spelling: "+str(popup)
    view=popup["viewport"]
    assert view[0]-1<=popup["left"]<popup["right"]<=view[2]+1 and \
           view[1]-1<=popup["top"]<popup["bottom"]<=view[3]+1, \
           "Note popup escapes the actual score viewport: "+str(popup)
    time.sleep(2.3)
    timeline=adb("logcat","-d","-v","brief").splitlines()
    visible_index=max(i for i,line in enumerate(timeline) if "POC_NOTE_TIP_VISIBLE {" in line)
    assert any("POC_NOTE_TIP_HIDDEN" in line for line in timeline[visible_index+1:]), \
           "Note identification persisted beyond two seconds"
    screenshot("piano-osmd-poc-note-tap-expired.png")
    print("PASS: Android OSMD notehead shows written Mi4/E4 inside viewport and expires",flush=True)
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
    # A user can change loop bounds while the native continuous clock is
    # already running. The manual loop menu must stop it BEFORE changing
    # the selected boundaries; no hanging output or route inconsistency.
    tap_text(["Iniciar"],scroll=True)
    time.sleep(.4)
    loop_nodes=[n for n in current_study_layout()["buttons"] if "Trecho ↻" in n["text"]]
    assert loop_nodes, "Study loop menu button unavailable during continuous playback"
    box=loop_nodes[0]["bounds"]
    assert len(box)==4, "Loop menu has no touchable bounds"
    cx=(box[0]+box[2])//2;cy=(box[1]+box[3])//2
    adb("shell","input","swipe",str(cx),str(cy),str(cx),str(cy),"1050")
    time.sleep(.3)
    tap_text(["Marcar início na posição atual"],exact=True)
    time.sleep(.3)
    assert "STUDY_LOOP_MENU start" in adb("logcat","-d","-v","brief"), \
        "Manual loop start did not use the safe native boundary transaction"
    assert any("Iniciar" in n.attrib.get("text","") for n in dump().iter("node")), \
        "Loop edit left native playback running instead of releasing MIDI"
    print("STUDY_B_MANUAL_LOOP_ANDROID_PASS",flush=True)
    # The musical B marker must also snap to a full sounding-note release and
    # cancel playback before applying the checked route. Reuse cached button
    # bounds so hierarchy dumps do not race a running WebView.
    def open_loop_menu():
        assert_running()
        adb("shell","input","swipe",str(cx),str(cy),str(cx),str(cy),"1000")
        time.sleep(.3)
        assert any("Marcar início na posição atual" in n.attrib.get("text","")
                   for n in dump().iter("node")), "Loop menu did not reopen"
    tap_text(["Iniciar"],scroll=True)
    time.sleep(.8)
    open_loop_menu()
    tap_text(["Marcar fim na posição atual"],exact=True)
    assert any("Iniciar" in n.attrib.get("text","") for n in dump().iter("node")), \
        "Applying B left continuous playback running"
    tap_text(["Iniciar"],scroll=True)
    time.sleep(.45)
    open_loop_menu()
    tap_text(["Remover repetição"],exact=True)
    assert any("Iniciar" in n.attrib.get("text","") for n in dump().iter("node")), \
        "Clearing the active A-B loop left playback running"
    tap_text(["Início"],scroll=True)

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
    # Toggle to the temporal renderer and back using the visible controls.
    study_renderer(True)
    for attempt in range(20):
        if "TIMELINE_READY" in adb("logcat","-d","-v","brief"):break
        time.sleep(.6)
    assert "TIMELINE_READY" in adb("logcat","-d","-v","brief"), "Visible alternative did not restore temporal rendering"
    assert_study_controls()

    logs=adb("logcat","-d","-v","brief")
    assert "TIMELINE_READY" in logs, "Expected proportional timeline renderer, not ordinary print layout"
    study_renderer(False)
    for attempt in range(24):
        entries=[line for line in adb("logcat","-d","-v","brief").splitlines() if "POC_OSMD_READY {" in line]
        if len(entries)>1:break
        time.sleep(.5)
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
    assert_study_controls()
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
    default_render_count=adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")
    tap_text([DEFAULT_SCORE_TITLE])
    time.sleep(3)
    assert_running()
    study_labels=[n.attrib.get("text","") for n in dump().iter("node")]
    assert_study_controls()
    assert adb("logcat","-d","-v","brief").count("POC_OSMD_READY {")>default_render_count, \
      "Reopened Study did not select OSMD by default"
    print("PASS: selected music loads with interactive study controls",flush=True)
    temporal_ready_before=adb("logcat","-d","-v","brief").count("TIMELINE_READY {")
    study_renderer(True)
    for attempt in range(16):
        if adb("logcat","-d","-v","brief").count("TIMELINE_READY {")>temporal_ready_before:break
        time.sleep(.5)
    assert adb("logcat","-d","-v","brief").count("TIMELINE_READY {")>temporal_ready_before, \
      "Visible temporal alternative did not render after OSMD opened Study by default"
    assert_study_controls()
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
    assert any("Biblioteca Musical" in s for s in labels), "Returning to library failed"
    print("PASS: navigation returns to library",flush=True)
    logs=adb("logcat","-d","-v","brief")
    if "FATAL EXCEPTION" in logs and "Process: br.org.pianolivre.opensource" in logs:
        start=logs.rfind("FATAL EXCEPTION")
        fatal=logs[start:start+12000]
        Path("piano-fatal-exception.log").write_text(fatal,encoding="utf-8")
        print("ANDROID_FATAL_EXCEPTION\\n"+fatal,flush=True)
        raise AssertionError("Fatal exception in Piano Livre; see piano-fatal-exception.log")
    # Additional real Android WebView stress with an already authorized longer
    # MusicXML score; a large pure-JS map test does NOT test SVG rendering.
    tap_text(["Gymnopédie nº 1"],scroll=True)
    time.sleep(3)
    assert_running()
    study_renderer(True)
    time.sleep(1)
    screenshot("piano-temporal-long.png")
    study_renderer(False)
    long_pid=adb("shell","pidof","br.org.pianolivre.opensource").strip()
    long_proof=None
    for attempt in range(35):
        time.sleep(.8)
        frames=[line.split("POC_OSMD_READY ",1)[1].split(" @",1)[0]
                for line in adb("logcat","-d","--pid="+long_pid,"-v","brief").splitlines()
                if "POC_OSMD_READY {" in line]
        if frames:
            candidate=json.loads(frames[-1])
            if candidate.get("graphicalNotes")==469 and candidate.get("midiMapping",{}).get("matched")==469:
                long_proof=candidate;break
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
    # Check the exact notehead against the same ready-frame coordinates before
    # resizing the WebView; otherwise the proof and hit target come from
    # different render generations and produce a false negative.
    # Uniformly scaled SVG geometry must preserve notehead hit targets on Android.
    long_head=long_proof.get("firstNoteViewport")
    assert long_head and long_head.get("midiEstimate") is not None, "Long score has no testable upper-staff notehead"
    hierarchy=dump()
    views=[n for n in hierarchy.iter("node") if n.attrib.get("class")=="android.webkit.WebView"]
    if views:
        geometry_node=views[0]
    else:
        # Some Android/WebView accessibility trees expose the score viewport
        # as its hosting FrameLayout. Use the smallest full-size score host,
        # not the application/root frames, while keeping the same hit mapping.
        candidates=[]
        for node in hierarchy.iter("node"):
            if node.attrib.get("class")!="android.widget.FrameLayout":continue
            bounds=[int(v) for v in re.findall(r"\d+",node.attrib.get("bounds",""))]
            if len(bounds)!=4:continue
            width,height=bounds[2]-bounds[0],bounds[3]-bounds[1]
            if width>=long_proof.get("webviewWidth",long_proof["viewportWidth"])*.8 and height>=long_proof["viewportHeight"]*.8:
                candidates.append((width*height,node,bounds))
        assert candidates, "Long-score WebView and score-host geometry unavailable"
        _,geometry_node,_=min(candidates,key=lambda item:item[0])
        print("Using accessible score-host FrameLayout for WebView geometry",flush=True)
    rect=[int(v) for v in re.findall(r"\d+",geometry_node.attrib["bounds"])]
    assert len(rect)==4, "Invalid scaled WebView dimensions"
    xx=rect[0]+round(long_head["x"]*(rect[2]-rect[0])/long_proof.get("webviewWidth",long_proof["viewportWidth"]))
    yy=rect[1]+round(long_head["y"]*(rect[3]-rect[1])/long_proof["viewportHeight"])
    assert rect[0]<=xx<rect[2] and rect[1]<=yy<rect[3], "Scaled graphical notehead outside screen"
    print("POC_TOUCH_TARGET "+json.dumps({"head":long_head,"webviewRect":rect,"x":xx,"y":yy,
        "webviewWidth":long_proof.get("webviewWidth"),"viewportHeight":long_proof["viewportHeight"]}),flush=True)
    previous_hits=adb("logcat","-d","-v","brief").count("POC_NOTE_TAP {")
    adb("shell","input","tap",str(xx),str(yy))
    # On an instrumented emulator, ADB input completion precedes Chromium's
    # JavaScript dispatch and its logcat flush. Poll the actual event instead
    # of imposing an arbitrary 400 ms rendering deadline.
    all_hits=[]
    for attempt in range(20):
        time.sleep(.25)
        logs=adb("logcat","-d","-v","brief")
        all_hits=[line for line in logs.splitlines() if "POC_NOTE_TAP {" in line]
        if len(all_hits)>previous_hits:break
    assert len(all_hits)>previous_hits, ("Scaled SVG did not recognize a real notehead touch at "+
        str((xx,yy))+" after 5s; target="+json.dumps(long_head))
    chosen_note=json.loads(all_hits[-1].split("POC_NOTE_TAP ",1)[1].split(" @",1)[0])
    assert chosen_note["staff"]==1 and chosen_note["nativeMidi"]==long_head["midiEstimate"] and chosen_note["mapped"], "Native-scale notehead did not match native MusicXML/MIDI"
    assert chosen_note.get("writtenName") and chosen_note.get("xmlNoteIndex") is not None, "Scaled notehead lost its original MusicXML notation"
    screenshot("piano-osmd-long-tap.png")
    responsive_evidence("gymnopedie")
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
    # Loading a new Study score must select OSMD without any gesture.
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
    # Regression 04: a backward musical seek must restore the original
    # clef/key/time context, not leave the most recent measure pinned.
    # Preserve earlier POC_SVG_FIT records: responsive_evidence consumes them.
    baseline_logs=adb("logcat","-d","-v","brief")
    baseline_states=sum("POC_CONTEXT_RAIL_STATE {" in line
                        for line in baseline_logs.splitlines())
    tap_text(["Início"],scroll=True)
    returned=False
    for attempt in range(12):
        logs=adb("logcat","-d","-v","brief")
        states=[]
        rail_lines=[line for line in logs.splitlines()
                    if "POC_CONTEXT_RAIL_STATE {" in line]
        for line in rail_lines[baseline_states:]:
                try:
                    states.append(json.loads(line.split("POC_CONTEXT_RAIL_STATE ",1)[1].split(" @",1)[0]))
                except (ValueError, IndexError):
                    pass
        returned=any(state["measure"]==1 and
          all(staff["clef"]==1 and staff["time"]==1 and staff["key"] is None
              for staff in state["staffSources"]) for state in states)
        if returned:break
        time.sleep(.4)
    assert returned,"Backward seek to beginning did not restore both staff contexts"
    screenshot("piano-osmd-context-return.png")
    print("PASS: OSMD context returns to measure 1 for both staves",flush=True)
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
          if n.attrib.get("text")=="Espera"),None)
        if complex_button is not None:break
        time.sleep(.25)
    assert complex_button is not None,"Synthetic fixture must open directly in OSMD Study"
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

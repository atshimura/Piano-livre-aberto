#!/usr/bin/env python3
"""GPL-3.0-only. Full UI state transitions on a SHORT and a LONG public score.
The emulator cannot replace a physical USB-MIDI piano or physical legibility.
"""
import json,re,time
from pathlib import Path
from emulator_ui import adb,dump,tap_text,screenshot,assert_running

PACKAGE="br.org.pianolivre.opensource/br.org.pianolivre.MainActivity"

def labels():
    return [n.attrib.get("text","") for n in dump().iter("node") if n.attrib.get("text")]

def expect(label,reason):
    assert any(label in v for v in labels()),reason

def log_count(marker):
    return adb("logcat","-d","-v","brief").count(marker)

def wait_new(marker,before,seconds=25):
    for i in range(int(seconds*2)):
        if log_count(marker)>before:return
        time.sleep(.5)
    raise AssertionError(marker+" did not arrive within "+str(seconds)+" s")

def wait_for_library(seconds=20):
    """Wait for a stable library screen and classify common UI-state failures."""
    catalog=json.loads(Path("app/src/main/assets/catalog.json").read_text(encoding="utf-8"))
    titles=[item["name"] for item in catalog]
    expected="Estudo livre 01"
    assert expected in titles, "SCORE_MISSING: expected short score is absent from the packaged catalog"
    last=[]
    for _ in range(seconds*2):
        assert_running()
        last=labels()
        if any("← Biblioteca" in value for value in last):
            tap_text(["← Biblioteca"],exact=True)
            time.sleep(.35)
            last=labels()
        if (any("BIBLIOTECA MUSICAL" in value for value in last)
                or any("Buscar título" in value for value in last)
                or any(title in value for title in titles for value in last)):
            break
        # Dismiss only a positively identified profile/filter popup. While the
        # Activity is still rendering, a Back key could exit the app and mask
        # the asynchronous load as a navigation failure.
        profile_popup=bool(last) and all("Upgrade A" in v or "Upgrade B" in v for v in last)
        filter_popup=bool(last) and all(v.strip() in ("Todas","Favoritos") or v.startswith("Coleção:") for v in last)
        if profile_popup or filter_popup:
            adb("shell","input","keyevent","4")
        time.sleep(.35)
    else:
        raise AssertionError("NAVIGATION_FAILED: library header did not appear; labels="+repr(last[:40]))

    # Reopen at the catalog's first rows so title lookup does not inherit a
    # previous screen's ScrollView offset.
    for _ in range(6):
        if any("BIBLIOTECA MUSICAL" in value for value in labels()):break
        root=dump()
        scroll=next((n for n in root.iter("node") if n.attrib.get("scrollable")=="true"
                     and "Horizontal" not in n.attrib.get("class","")),None)
        if scroll is None:break
        bounds=[int(x) for x in re.findall(r"\d+",scroll.attrib.get("bounds",""))]
        if len(bounds)!=4:break
        x=str((bounds[0]+bounds[2])//2)
        adb("shell","input","swipe",x,str(bounds[1]+20),x,str(bounds[3]-20),"350")
        time.sleep(.2)

    # The filter is a screen control, not persisted library data. Explicitly
    # return to All so a prior scenario cannot hide an otherwise valid score.
    spinner=next((n for n in dump().iter("node") if n.attrib.get("class")=="android.widget.Spinner"),None)
    if spinner is not None and spinner.attrib.get("text","").strip()!="Todas":
        bounds=[int(x) for x in re.findall(r"\d+",spinner.attrib.get("bounds",""))]
        if len(bounds)==4:
            adb("shell","input","tap",str((bounds[0]+bounds[2])//2),str((bounds[1]+bounds[3])//2))
            time.sleep(.2)
            tap_text(["Todas"],exact=True)
            last=labels()

    # Give asynchronous layout work a bounded interval. Distinct errors make a
    # missing asset, a filtering issue and a library/navigation failure clear.
    for _ in range(20):
        last=labels()
        if any(expected in value for value in last):
            return
        if any("BIBLIOTECA MUSICAL" in value for value in last) and any(" de " in value and "partituras" in value for value in last):
            break
        time.sleep(.25)
    if not any("BIBLIOTECA MUSICAL" in value for value in last):
        raise AssertionError("NAVIGATION_FAILED: left the library while waiting; labels="+repr(last[:40]))
    result=next((v for v in last if "partituras" in v),"")
    filter_labels=[v for v in last if "Favoritos" in v or "Coleção:" in v or v.strip()=="Todas"]
    if "0 de " in result or any("Favoritos" in v or "Coleção:" in v for v in filter_labels):
        raise AssertionError("SCORE_HIDDEN_BY_FILTER: reset to Todas did not reveal the catalog score; result="+repr(result)+" filters="+repr(filter_labels))
    if not result:
        raise AssertionError("LIBRARY_NOT_LOADED: library shell appeared without the catalog result count; labels="+repr(last[:40]))
    raise AssertionError("SCORE_NOT_VISIBLE: packaged score exists and library is loaded; result="+repr(result)+" labels="+repr(last[:40]))

def library():
    wait_for_library()

def exercise(title,slug):
    library()
    adb("logcat","-c")
    before=log_count("POC_OSMD_READY {")
    tap_text([title],scroll=True)
    wait_new("POC_OSMD_READY {",before)
    expect(title,"Study lost its selected score name")
    for text in ["Espera","Contínuo","Demonstração","Teclado guia"]:
        expect(text,"Study missing essential control: "+text)
    screenshot("piano-entrega-b-"+slug+"-study.png")
    # Hand selection must not filter the entire two-staff visual guide.
    for hand in ["Direita","Esquerda","Ambas"]:
        tap_text([hand],exact=True,scroll=True)
        assert_running()
    tap_text(["Teclado guia"],scroll=True)
    time.sleep(.7)
    opened=labels()
    assert not any("Contínuo"==v for v in opened),"guide should hide top Study controls"
    expect("Ocultar teclado","guide did not open")
    screenshot("piano-entrega-b-"+slug+"-guide.png")
    tap_text(["Ocultar teclado"],scroll=True)
    time.sleep(.5)
    expect("Contínuo","Study controls did not return when closing guide")
    tap_text(["Contínuo"],exact=True,scroll=True)
    base=log_count("play=true mode=1")
    tap_text(["Iniciar"],scroll=True)
    wait_new("play=true mode=1",base)
    time.sleep(.7)
    tap_text(["Pausar"],scroll=True)
    expect("Iniciar","pause did not restore the continuation command")
    base=log_count("play=true mode=1")
    tap_text(["Iniciar"],scroll=True)
    wait_new("play=true mode=1",base)
    tap_text(["Parar"],scroll=True)
    expect("Iniciar","stop left the Study session marked running")
    tap_text(["Início"],scroll=True)
    assert_running()
    # Switch OSMD/temporal/OSMD without changing the MusicXML source.
    tap_text(["Anterior · temporal"],exact=True,scroll=True)
    time.sleep(.7)
    tap_text(["OSMD · nova"],exact=True,scroll=True)
    time.sleep(.7)
    expect(title,"renderer switch lost the current piece")
    # Unsupported demonstration without an output port must refuse playback,
    # rather than silently claiming sound from the visual keyboard.
    tap_text(["Demonstração"],exact=True,scroll=True)
    before=log_count("play=true mode=2")
    tap_text(["Iniciar"],scroll=True)
    time.sleep(.6)
    assert log_count("play=true mode=2")==before,"Demonstration claimed MIDI without an output device"
    # Wait preparation must finish and not require a timeout to play afterward.
    tap_text(["Espera"],exact=True,scroll=True)
    before=log_count("POC_WAIT_COMPLETE")
    tap_text(["Iniciar"],scroll=True)
    wait_new("POC_WAIT_COMPLETE",before,15)
    expect("Esperando","Wait did not arm after gradual preparation")
    tap_text(["Parar"],scroll=True)
    # A warm background/foreground cycle may not create a second playback clock.
    adb("shell","input","keyevent","3")
    time.sleep(.6)
    adb("shell","am","start","-W","-n",PACKAGE)
    time.sleep(1)
    assert_running()
    expect(title,"return from background lost the selected study")
    screenshot("piano-entrega-b-"+slug+"-resumed.png")
    library()
    return {"score":title,"modes":["Espera","Contínuo","Demonstração (without output)"],
            "hands":["Direita","Esquerda","Ambas"],
            "guide":"opened and restored","renderer":"OSMD-temporal-OSMD",
            "pause":"stopped and restarted","background":"returned"}

def main():
    # A force-stop clears transient Android popups/windows while preserving all
    # app preferences and files. This makes this UI journey order-independent
    # after upgrade/profile/library scenarios on the same emulator.
    adb("shell","am","force-stop","br.org.pianolivre.opensource")
    adb("shell","am","start","-S","-W","-n",PACKAGE)
    wait_for_library()
    result=[]
    for title,slug in [("Estudo livre 01","short"),("Gymnopédie nº 1","long")]:
        result.append(exercise(title,slug))
    Path("piano-entrega-b-session.json").write_text(json.dumps({
      "scope":"Android API 29 emulator with synthetic clock; not physical USB-MIDI or visual acceptance",
      "sessions":result},ensure_ascii=False,indent=2),encoding="utf-8")
    print("STUDY_B_ANDROID_SESSION_PASS "+json.dumps(result,ensure_ascii=False),flush=True)

if __name__=="__main__":main()

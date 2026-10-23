#!/usr/bin/env python3
"""GPL-3.0-only. Full UI state transitions on a SHORT and a LONG public score.
The emulator cannot replace a physical USB-MIDI piano or physical legibility.
"""
import json,time
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

def library():
    visible=labels()
    if not any("10 de 10 partituras" in v for v in visible):
        tap_text(["Biblioteca"],scroll=True)
        time.sleep(.5)
    assert_running()
    expect("Ode à Alegria","public score library did not reopen")

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
    adb("shell","am","start","-S","-W","-n",PACKAGE)
    time.sleep(1)
    result=[]
    for title,slug in [("Estudo livre 01","short"),("Gymnopédie nº 1","long")]:
        result.append(exercise(title,slug))
    Path("piano-entrega-b-session.json").write_text(json.dumps({
      "scope":"Android API 29 emulator with synthetic clock; not physical USB-MIDI or visual acceptance",
      "sessions":result},ensure_ascii=False,indent=2),encoding="utf-8")
    print("STUDY_B_ANDROID_SESSION_PASS "+json.dumps(result,ensure_ascii=False),flush=True)

if __name__=="__main__":main()

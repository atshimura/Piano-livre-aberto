#!/usr/bin/env python3
"""Focused Android navigation regression: Study -> Library/Home -> Study.
Uses the real app's accessible Back action on phone/tablet viewports, without synthetic route injection.
"""
import sys,time,traceback
from pathlib import Path
from emulator_ui import adb, dump, tap_text, wait_for_text, screenshot, home_scroll_to_top, assert_running

PKG="br.org.pianolivre.opensource"
ACT=PKG+"/br.org.pianolivre.MainActivity"

def shown():
    return [v for n in dump().iter("node") for v in
            (n.attrib.get("text",""),n.attrib.get("content-desc","")) if v]

def check(condition,message):
    if not condition:
        raise AssertionError(message+"; labels="+repr(shown()[:50]))

def one_viewport(name,size,density):
    adb("shell","am","force-stop",PKG)
    adb("shell","wm","size",size)
    adb("shell","wm","density",density)
    adb("shell","am","start","-W","-n",ACT)
    wait_for_text("Bem-vindo ao",timeout=30)
    home_scroll_to_top()
    tap_text(["Começar pela Ode à Alegria"],scroll=True)
    wait_for_text("Espera",timeout=30)
    check(any("Ode à Alegria" in x for x in shown()), "Expected Study score not opened")
    screenshot("study-back-"+name+"-before.png")
    # A warm resume must keep Study open; not silently reset to Home.
    adb("shell","input","keyevent","3")
    time.sleep(.6)
    adb("shell","am","start","-W","-n",ACT)
    wait_for_text("Espera",timeout=30)
    check(any("Ode à Alegria" in x for x in shown()), "Warm resume lost the selected score")
    tap_text(["Voltar à composição ou à biblioteca"],exact=True)
    wait_for_text("Biblioteca Musical",timeout=30)
    check(not any(v=="Espera" for v in shown()), "Study still shown after explicit Back")
    screenshot("study-back-"+name+"-library.png")
    # Activate the actual Library, then re-enter Study on the same public score.
    home_scroll_to_top()
    tap_text(["Biblioteca Musical"],exact=True)
    wait_for_text("Buscar título",timeout=30)
    home_scroll_to_top()
    tap_text(["Começar pela Ode à Alegria"],scroll=True)
    wait_for_text("Espera",timeout=30)
    check(any("Ode à Alegria" in x for x in shown()), "Could not re-enter Study from Library")
    screenshot("study-back-"+name+"-again.png")
    assert_running()
    print("FOCUSED_STUDY_BACK_PASS viewport="+name+" size="+size,flush=True)

oldsize=adb("shell","wm","size")
olddensity=adb("shell","wm","density")
try:
    for name,size,density in (("phone","1280x720","240"),("tablet","1920x1080","240")):
        one_viewport(name,size,density)
finally:
    # No ignored failures: cleanup must be explicit, but device defaults are
    # restored even when the focused assertion fails.
    adb("shell","am","force-stop",PKG)
    adb("shell","wm","size","reset")
    adb("shell","wm","density","reset")
print("FOCUSED_STUDY_BACK_ALL_PASS profiles=2",flush=True)

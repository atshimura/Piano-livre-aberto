#!/usr/bin/env python3
"""ZAI-012: real Android GUI test of Lab landing in short landscape viewports.

Verifies title/description and all three cards are visible *without* scrolling
and remain fixed after a swipe. Also tests return from Discover. Emulator
evidence does not replace testing on Realme 7 5G or Lenovo Tab M9.
"""
import json, re, subprocess, time
from pathlib import Path
from emulator_ui import adb, dump, screenshot, tap_text, wait_for_text

PKG="br.org.pianolivre.opensource.zai"
ACTIVITY=PKG+"/br.org.pianolivre.MainActivity"
profiles=[
    ("user-screenshot", "1536x691", "320", "1.0"),
    ("short-phone", "1280x720", "320", "1.0"),
    ("landscape-tablet", "1920x1080", "240", "1.2"),
]
results=[]
original_font=adb("shell","settings","get","system","font_scale").strip()

def bounds(node):
    numbers=[int(x) for x in re.findall(r"\d+",node.attrib.get("bounds",""))]
    if len(numbers)!=4:
        raise AssertionError("Node lacks Android screen bounds: "+str(node.attrib))
    return numbers

def find(root,fragment):
    matching=[n for n in root.iter("node")
              if fragment in n.attrib.get("text","")]
    if not matching:
        raise AssertionError("Missing visible "+repr(fragment)+" in "+
          repr([n.attrib.get("text","") for n in root.iter("node") if n.attrib.get("text")][:55]))
    return matching[0]

def inspect():
    root=dump()
    top=bounds(find(root,"← Biblioteca"))
    title=bounds(find(root,"Escolha um modo"))
    desc=bounds(find(root,"Selecione como deseja usar"))
    discover=bounds(find(root,"Abrir Descobrir"))
    learning=bounds(find(root,"Abrir Aprender"))
    creation=bounds(find(root,"Abrir Criação"))
    for name,b in (("title",title),("description",desc),
                   ("Discover",discover),("Learn",learning),("Creation",creation)):
        assert b[2]>b[0] and b[3]>b[1],"clipped "+name+" bounds "+str(b)
    assert title[1]>=top[3]-2,"title occluded by toolbar: "+str((top,title))
    assert desc[1]>=title[3]-2,"instruction overlaps heading: "+str((title,desc))
    assert all(x[1]>=desc[3] for x in (discover,learning,creation)), "card actions overlap intro"
    scrollers=[n for n in root.iter("node")
               if "ScrollView" in n.attrib.get("class","")]
    assert scrollers,"Laboratory content viewport must be an Android ScrollView"
    viewport=bounds(scrollers[0])
    assert title[1]>=viewport[1]-2,"title outside content viewport"
    assert max(x[3] for x in (discover,learning,creation))<=viewport[3]+3, (
        "card action below content viewport: "+str((viewport,discover,learning,creation)))
    return {"toolbar":top,"title":title,"description":desc,
            "discover":discover,"learn":learning,"creation":creation}

try:
    for label,size,density,font in profiles:
        adb("shell","am","force-stop",PKG)
        adb("shell","wm","size",size)
        adb("shell","wm","density",density)
        adb("shell","settings","put","system","font_scale",font)
        adb("shell","am","start","-W","-n",ACTIVITY)
        wait_for_text("Laboratório Musical",timeout=45)
        tap_text(["Laboratório Musical"],exact=True,scroll=True)
        wait_for_text("Escolha um modo",timeout=25)
        time.sleep(.6)
        before=inspect()
        screenshot("zai012-"+label+"-initial.png")
        # An unnecessary scroll would shift title/cards and hide the page heading.
        x=(before["description"][0]+before["description"][2])//2
        top=before["title"][1]
        lower=before["creation"][3]
        adb("shell","input","swipe",str(x),str(max(top+60,lower-8)),
            str(x),str(top+16),"350")
        time.sleep(.5)
        after=inspect()
        assert abs(before["title"][1]-after["title"][1])<=3, (
            label+" title moved on ordinary swipe: "+str((before["title"],after["title"])))
        screenshot("zai012-"+label+"-after-swipe.png")
        # Returning from Discover must not keep an old ScrollView offset.
        tap_text(["Abrir Descobrir"],scroll=False)
        time.sleep(.4)
        adb("shell","input","keyevent","4")
        wait_for_text("Escolha um modo",timeout=30)
        reopened=inspect()
        assert abs(reopened["title"][1]-before["title"][1])<=3, (
            label+" reentered with nonzero offset: "+str((before["title"],reopened["title"])))
        results.append({"profile":label,"size":size,"density":density,"fontScale":font,
            "initial":before,"afterSwipe":after,"reopened":reopened})
        print("ZAI012_ANDROID_VIEWPORT_PASS "+label,flush=True)
    Path("zai012-viewport-report.json").write_text(
        json.dumps(results,ensure_ascii=False,indent=2),encoding="utf-8")
finally:
    adb("shell","am","force-stop",PKG)
    adb("shell","wm","size","reset")
    adb("shell","wm","density","reset")
    adb("shell","settings","put","system","font_scale",
        original_font if original_font not in ("","null") else "1.0")

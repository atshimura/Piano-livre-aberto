#!/usr/bin/env python3
"""ZAI-013 Android UI: generate a synthetic take via existing debug selftest
and verify the score WebView cannot sit beneath top zoom or bottom Save buttons.
No physical instrument or user data is involved.
"""
import json, re, subprocess, time
from pathlib import Path
from emulator_ui import adb,dump,screenshot,tap_text,wait_for_text

PACKAGE="br.org.pianolivre.opensource.zai"
ACTIVITY=PACKAGE+"/br.org.pianolivre.MainActivity"
STAGES=("home","recording","paused","review","saved")

def wait_stage(stage,pid,seconds=75):
    end=time.monotonic()+seconds
    while time.monotonic()<end:
        logs=adb("logcat","-d","--pid="+pid,"-v","brief")
        if "FATAL EXCEPTION" in logs:
            raise AssertionError(logs[-12000:])
        if "CREATION_STAGE "+stage in logs:return
        time.sleep(.25)
    raise AssertionError("Missing stage "+stage+"\n"+logs[-4000:])

def rect(node):
    v=[int(n) for n in re.findall(r"\d+",node.attrib.get("bounds",""))]
    assert len(v)==4 and v[2]>v[0] and v[3]>v[1],node.attrib
    return v

def find(root,term):
    nodes=[n for n in root.iter("node") if term in n.attrib.get("text","")]
    assert nodes, "Missing "+term+" in "+repr([n.attrib.get("text","") for n in root.iter("node")][:70])
    return nodes[0]

def geometry():
    root=dump()
    tempo=rect(find(root,"bpm"))
    minus=rect(find(root,"A−"))
    plus=rect(find(root,"A+"))
    fit=rect(find(root,"Ajustar à largura"))
    save=rect(find(root,"Salvar partitura"))
    back=rect(find(root,"Voltar à composição"))
    views=[n for n in root.iter("node") if n.attrib.get("class")=="android.webkit.WebView"]
    assert len(views)==1,"Expected a single OSMD WebView, found "+str(len(views))
    score=rect(views[0])
    topBottom=max(tempo[3],minus[3],plus[3],fit[3])
    bottomTop=min(save[1],back[1])
    assert abs(minus[1]-tempo[1])<=9,"zoom must be beside BPM at the top"
    assert abs(plus[1]-tempo[1])<=9 and abs(fit[1]-tempo[1])<=9
    assert score[1]>=topBottom-3, "Top buttons obscure the score "+repr((tempo,minus,score))
    assert score[3]<=bottomTop+3, "Bottom actions obscure last staff "+repr((score,save))
    assert score[3]-score[1]>=75, "No usable score viewport "+repr(score)
    return {"tempo":tempo,"zoomOut":minus,"zoomIn":plus,"fit":fit,
            "score":score,"save":save,"back":back}

original_size=adb("shell","wm","size").strip()
original_density=adb("shell","wm","density").strip()
try:
    adb("shell","wm","size","1280x720")
    adb("shell","wm","density","240")
    adb("shell","am","force-stop",PACKAGE)
    adb("logcat","-c")
    adb("shell","am","start","-W","-n",ACTIVITY,
        "--ez","creation_selftest","true","--es","creation_label","zai013")
    pid=adb("shell","pidof",PACKAGE).strip()
    assert pid.isdigit(),"debug CreationSelfTest app did not start"
    for stage in STAGES:
        wait_stage(stage,pid)
        if stage!="saved":
            adb("shell","run-as",PACKAGE,"touch","cache/creation-next")
    tap_text(["Gerar partitura"],scroll=True)
    wait_for_text("Ajustar à largura",timeout=90)
    time.sleep(.8)
    before=geometry()
    screenshot("zai013-preview-initial.png")
    tap_text(["A+"])
    time.sleep(.5)
    enlarged=geometry()
    screenshot("zai013-preview-zoom.png")
    tap_text(["Ajustar à largura"])
    time.sleep(.5)
    restored=geometry()
    screenshot("zai013-preview-fit.png")
    for view in (enlarged,restored):
        assert view["score"]==before["score"],"Zoom changed reserved score box or overlapped actions"
    report={"initial":before,"enlarged":enlarged,"fit":restored}
    Path("zai013-android-geometry.json").write_text(
        json.dumps(report,indent=2,ensure_ascii=False),encoding="utf-8")
    print("ZAI013_ANDROID_SCORE_TOOLBAR_PASS viewport_reserved=true zoom_restores=true",flush=True)
finally:
    adb("shell","am","force-stop",PACKAGE)
    adb("shell","wm","size","reset")
    adb("shell","wm","density","reset")

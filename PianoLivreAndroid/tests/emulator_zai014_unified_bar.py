#!/usr/bin/env python3
"""ZAI-014: assert one bar for Back, title/BPM, zoom, info and primary action.
Uses the existing synthetic MIDI recording/debug selftest, not user data.
"""
import json,re,time
from pathlib import Path
from emulator_ui import adb,dump,screenshot,tap_text,wait_for_text
PACKAGE="br.org.pianolivre.opensource.zai"
ACTIVITY=PACKAGE+"/br.org.pianolivre.MainActivity"
STAGES=("home","recording","paused","review","saved")

def stage(name,pid,seconds=75):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  logs=adb("logcat","-d","--pid="+pid,"-v","brief")
  if "FATAL EXCEPTION" in logs:raise AssertionError(logs[-10000:])
  if "CREATION_STAGE "+name in logs:return
  time.sleep(.3)
 raise AssertionError("Missing stage "+name+"\n"+logs[-4500:])

def rect(node):
 numbers=[int(x) for x in re.findall(r"\d+",node.attrib.get("bounds",""))]
 assert len(numbers)==4 and numbers[2]>numbers[0] and numbers[3]>numbers[1],node.attrib
 return numbers

def find(root,needle,field=None):
 for n in root.iter("node"):
  for key in ([field] if field else ("text","content-desc")):
   if needle in n.attrib.get(key,""):return n
 raise AssertionError("Missing "+repr(needle)+" visible="+repr(
  [n.attrib.get("text","") or n.attrib.get("content-desc","")
    for n in root.iter("node")][:80]))

def measure(compact=False):
 root=dump()
 back=rect(find(root,"Voltar à composição","content-desc"))
 tempo=rect(find(root,"bpm","text"))
 minus=rect(find(root,"A−","text"))
 plus=rect(find(root,"A+","text"))
 fit=rect(find(root,"Ajustar à largura","content-desc"))
 # The info icon is reachable by horizontal pan if the toolbar becomes narrow.
 primary=rect(find(root,"Salvar","text"))
 buttons=(back,tempo,minus,plus,fit,primary)
 middles=[(p[1]+p[3])//2 for p in buttons]
 assert max(middles)-min(middles)<=9, "buttons not in one row: "+str(buttons)
 assert back[0]<tempo[0] and fit[0]<primary[0], "toolbar order wrong: "+str(buttons)
 views=[rect(n) for n in root.iter("node")
   if n.attrib.get("class")=="android.webkit.WebView"]
 barBottom=max(x[3] for x in buttons)
 scoreViews=[v for v in views if v[1]>=barBottom-4 and (v[3]-v[1])>115]
 assert scoreViews,"No unobstructed WebView below the unified header: "+str(views)
 score=max(scoreViews,key=lambda v:(v[2]-v[0])*(v[3]-v[1]))
 assert score[1]>=barBottom-4, "score covered by controls"
 assert score[3]-score[1]>310 if not compact else score[3]-score[1]>210, ("score too short: "+str(score))
 assert not any(n.attrib.get("text","")=="Voltar à composição"
   for n in root.iter("node")), "old bottom Back button remains"
 return {"back":back,"tempo":tempo,"zoomOut":minus,"zoomIn":plus,
  "fit":fit,"primary":primary,"score":score}

def tap_by_desc(description):
 root=dump()
 b=rect(find(root,description,"content-desc"))
 adb("shell","input","tap",str((b[0]+b[2])//2),str((b[1]+b[3])//2))
 time.sleep(.4)

original_font=adb("shell","settings","get","system","font_scale").strip()
try:
 adb("shell","wm","size","1280x720")
 adb("shell","wm","density","240")
 adb("shell","am","force-stop",PACKAGE)
 adb("logcat","-c")
 adb("shell","am","start","-W","-n",ACTIVITY,
     "--ez","creation_selftest","true","--es","creation_label","zai014")
 pid=adb("shell","pidof",PACKAGE).strip()
 assert pid.isdigit(),"Synthetic recording application didn't start"
 for name in STAGES:
  stage(name,pid)
  if name!="saved":adb("shell","run-as",PACKAGE,"touch","cache/creation-next")
 tap_text(["Gerar partitura"],scroll=True)
 wait_for_text("Ajustar à largura",timeout=90)
 time.sleep(.7)
 initial=measure(compact=False)
 screenshot("zai014-unified-initial.png")
 # The dialog contains all MIDI warnings formerly taking up a footer row.
 tap_by_desc("Informações da interpretação MIDI")
 wait_for_text("Informações da partitura",timeout=15)
 wait_for_text("gravação original permanece intacta",timeout=15)
 tap_text(["Fechar"],exact=True)
 afterInfo=measure(compact=False)
 assert initial["score"]==afterInfo["score"],"info dialog reduced score viewport"
 tap_text(["A+"],exact=True)
 time.sleep(.4)
 enlarged=measure(compact=False)
 tap_by_desc("Ajustar à largura")
 fitted=measure(compact=False)
 assert enlarged["score"]==initial["score"]==fitted["score"],"zoom changed viewport"
 screenshot("zai014-unified-zoom.png")
 # Native Android layout at a stable phone landscape resolution. The
 # separate Chromium/OSMD suite exercises narrower/taller score viewports.
 screenshot("zai014-unified-phone.png")
 tap_text(["Salvar"],exact=True)
 wait_for_text("+ Biblioteca",timeout=90)
 root=dump()
 assert find(root,"+ Biblioteca","text") is not None
 assert not any(n.attrib.get("text","")=="Salvar partitura" for n in root.iter("node")), "save footer returned"
 screenshot("zai014-unified-saved.png")
 result={"initial":initial,"info":afterInfo,"zoom":enlarged,"fit":fitted,"compact":narrower,
         "savedAction":"+ Biblioteca"}
 Path("zai014-unified-report.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
 print("ZAI014_ANDROID_UNIFIED_BAR_PASS score_full_height=true info_dialog=true zoom=true save_transition=true",flush=True)
finally:
 adb("shell","am","force-stop",PACKAGE)
 adb("shell","wm","size","reset")
 adb("shell","wm","density","reset")
 adb("shell","settings","put","system","font_scale",
   original_font if original_font not in ("","null") else "1.0")

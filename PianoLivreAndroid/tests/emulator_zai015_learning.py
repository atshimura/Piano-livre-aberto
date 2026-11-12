#!/usr/bin/env python3
"""Android UI smoke, offline: real home > laboratory > Aprender, OSMD and
virtual keyboard exact note recognition, one persisted reading lesson.
Yamaha hardware latency and physical MIDI reception are NOT tested here.
"""
import json,re,time
from pathlib import Path
from emulator_ui import adb,dump,screenshot,tap_text,wait_for_text

PKG="br.org.pianolivre.opensource.zai"
ACT=PKG+"/br.org.pianolivre.MainActivity"
results=[]

def find(root,fragment,key=None):
 for node in root.iter("node"):
  keys=[key] if key else ["text","content-desc"]
  if any(fragment in node.attrib.get(k,"") for k in keys):
   return node
 raise AssertionError("Could not find "+repr(fragment)+" in "+
  repr([n.attrib.get("text","") or n.attrib.get("content-desc","")
        for n in root.iter("node")][:80]))
def rect(node):
 v=[int(n) for n in re.findall(r"\d+",node.attrib.get("bounds",""))]
 assert len(v)==4, node.attrib
 return v
def virtual_key(white_index):
 root=dump()
 key=rect(find(root,"Teclado virtual musical","content-desc"))
 x=round(key[0]+(white_index+.48)*(key[2]-key[0])/22)
 y=round(key[1]+.87*(key[3]-key[1]))
 adb("shell","input","tap",str(x),str(y))
 return {"key":key,"tap":[x,y]}

try:
 adb("shell","wm","size","1280x720")
 adb("shell","wm","density","240")
 adb("shell","am","force-stop",PKG)
 adb("shell","am","start","-W","-n",ACT)
 wait_for_text("Laboratório Musical",timeout=40)
 tap_text(["Laboratório Musical"],exact=True,scroll=True)
 wait_for_text("Escolha um modo",timeout=20)
 wait_for_text("Abrir Aprender",timeout=20)
 screenshot("zai015-lab-active-learn.png")
 tap_text(["Abrir Aprender"],scroll=False)
 wait_for_text("Escolher e tocar",timeout=25)
 wait_for_text("Ler partitura",timeout=25)
 screenshot("zai015-learn-home.png")
 tap_text(["Abrir exercício"],scroll=False)
 wait_for_text("Pesquisar pelo nome",timeout=20)
 wait_for_text("Iniciar prática",timeout=20)
 screenshot("zai015-picker.png")
 tap_text(["Nota"],exact=True,scroll=False)
 wait_for_text("Iniciar prática",timeout=20)
 tap_text(["Iniciar prática"],scroll=False)
 wait_for_text("Andamento livre",timeout=30)
 tap=virtual_key(7)  # C4 (MIDI 60) = eighth white key C3..C6.
 wait_for_text("Você tocou o exercício corretamente",timeout=25)
 screenshot("zai015-free-note-correct.png")
 results.append({"screen":"choose-and-play","virtualC4":tap})
 adb("shell","input","keyevent","4")
 wait_for_text("Iniciar prática",timeout=25)
 adb("shell","input","keyevent","4")
 wait_for_text("Ler partitura",timeout=25)
 tap_text(["Abrir lições"],scroll=False)
 wait_for_text("Notas na pauta",timeout=25)
 screenshot("zai015-reading-path.png")
 tap_text(["Disponível"],exact=True,scroll=False)
 wait_for_text("Lição 1 de 8",timeout=30)
 wait_for_text("Próximo após acerto",timeout=30)
 screenshot("zai015-reading-note-before.png")
 tap=virtual_key(9)  # E4, first lesson.
 wait_for_text("Prossiga para a próxima lição",timeout=25)
 tap_text(["Próximo após acerto"],scroll=False)
 wait_for_text("Lição 2 de 8",timeout=30)
 screenshot("zai015-reading-note-second.png")
 results.append({"screen":"reading-lesson","firstNote":"Mi4","progressSaved":True,"tap":tap})
 adb("shell","input","keyevent","4")
 wait_for_text("Progresso geral",timeout=30)
 results.append({"screen":"reading-path-return","progressVisible":True})
 Path("zai015-android-report.json").write_text(
  json.dumps(results,ensure_ascii=False,indent=2),encoding="utf-8")
 print("ZAI015_ANDROID_LEARNING_PASS screens=4 virtual_notes=2 progress=local",flush=True)
finally:
 adb("shell","am","force-stop",PKG)
 adb("shell","wm","size","reset")
 adb("shell","wm","density","reset")

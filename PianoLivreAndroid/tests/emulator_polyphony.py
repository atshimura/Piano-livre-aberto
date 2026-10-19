#!/usr/bin/env python3
"""Real Android API29 WebView checks for synthetic source-identical polyphony."""
import json, time, re, xml.etree.ElementTree as ET
from pathlib import Path
from emulator_ui import adb, screenshot, tap_text, assert_running, dump, responsive_evidence

def records(marker):
    return [json.loads(line.split(marker,1)[1].split(" @",1)[0])
      for line in adb("logcat","-d","-v","brief").splitlines() if marker in line]
def latest(marker):
    rows=records(marker)
    return rows[-1] if rows else None

def main():
    # The independent oracle uses fixed authored XML, never the production renderer.
    root=ET.parse("app/src/debug/assets/scores/engraving-polyphony-synthetic.musicxml").getroot()
    expected=[]
    semitone={"C":0,"D":2,"E":4,"F":5,"G":7,"A":9,"B":11}
    base=0
    for measure in root.findall(".//part/measure"):
        position=0;last=0
        for node in measure:
            if node.tag=="backup":position-=int(node.findtext("duration"))
            if node.tag=="forward":position+=int(node.findtext("duration"))
            if node.tag!="note":continue
            duration=int(node.findtext("duration","0"))
            chord=node.find("chord") is not None
            onset=last if chord else position
            if not chord:last=position
            pitch=node.find("pitch")
            if pitch is not None:
                midi=12*(int(pitch.findtext("octave"))+1)+semitone[pitch.findtext("step")]
                expected.append({"beat":base+onset/4,"midi":midi,
                    "staff":int(node.findtext("staff")),"voice":node.findtext("voice"),
                    "length":duration/4,"attack":not any(t.get("type")=="stop" for t in node.findall("tie"))})
            if not chord:position+=duration
        base+=4
    assert len(expected)==30 and sum(e["attack"] for e in expected)==29
    assert sum(e["beat"]==0 and e["staff"]==1 and e["voice"]=="1" for e in expected)==3
    assert sum(e["beat"]==4 and e["staff"]==1 and e["midi"]==67 for e in expected)==2
    adb("shell","am","force-stop","br.org.pianolivre.opensource")
    adb("logcat","-c")
    adb("shell","am","start","-S","-W","-n",
       "br.org.pianolivre.opensource/br.org.pianolivre.MainActivity",
       "--ez","engraving_polyphony_selftest","true")
    for attempt in range(40):
        if "POLYPHONY_FIXTURE_LOAD_OK title=Synthetic polyphony fidelity test measures=4 writtenHeads=30 attacks=29" in adb("logcat","-d","-v","brief"):break
        time.sleep(.5)
    else:raise AssertionError("Debug-only native polyphony score did not load")
    button=None
    for _ in range(10):
        button=next((n for n in dump().iter("node") if n.attrib.get("text")=="Partitura Livre"),None)
        if button is not None:break
        time.sleep(.3)
    assert button is not None,"Partitura Livre button is missing on fixture"
    bounds=[int(v) for v in re.findall(r"\d+",button.attrib["bounds"])]
    cx,cy=(bounds[0]+bounds[2])//2,(bounds[1]+bounds[3])//2
    adb("logcat","-c")
    adb("shell","input","swipe",str(cx),str(cy),str(cx),str(cy),"1100")
    proof=None;heads=None
    for _ in range(45):
        assert_running()
        proof=latest("POC_OSMD_READY ")
        pieces=records("POC_POLYPHONY_HEADS_CHUNK ")
        by_part={item["part"]:item for item in pieces}
        if by_part and len(by_part)>=pieces[-1]["total"]:
            heads=[h for i in range(pieces[-1]["total"]) for h in by_part[i]["heads"]]
        if proof is not None and heads is not None:break
        time.sleep(.5)
    assert proof and heads,"Actual OSMD polyphony proof or graphical heads missing"
    mapping=proof["midiMapping"]
    assert proof["measures"]==4 and 29<=proof["graphicalNotes"]<=30
    assert mapping["nativeEvents"]==30
    assert mapping["unmatched"]==0 and mapping["invalidNative"]==0
    assert mapping["matched"]>=28 and mapping["ambiguous"]<=2
    assert mapping["matched"]+mapping["ambiguous"]==proof["graphicalNotes"]
    assert proof["viewportMaxScrollTop"]==0 and proof["height"]<=proof["availableHeight"]+.5
    assert proof["nativeStaffSpaceCssPx"]>0 and proof["staffSeparationSpaces"]>0
    # Every verified native head must preserve the fixture's beat, staff, pitch
    # and original duration. Do not mask false identities under totals.
    from collections import Counter
    source=Counter((n["beat"],n["staff"],n["midi"],n["voice"],n["length"],n["attack"]) for n in expected)
    observed=Counter((n["beat"],n["staff"],n["midi"],n["voice"],n["length"],n["attack"]) for n in heads if n["status"]=="matched")
    assert all(source[k]>=v for k,v in observed.items()),"Incorrect source identity in engraved noteheads: "+repr(observed-source)
    ambiguities=[n for n in heads if n["status"]=="ambiguous"]
    assert all(n["beat"]==4 and n["staff"]==1 and n["midiEstimate"]==67 for n in ambiguities), "Ambiguity outside the polyphonic unison: "+repr(ambiguities)
    assert any(n["beat"]==12 and n["midi"]==79 and n["attack"] is False for n in heads), "Tied continuation missing from written graphical heads"
    triad=[n for n in heads if n["beat"]==0 and n["staff"]==1 and n["voice"]=="1"]
    assert {n["midi"] for n in triad}=={72,74,76} and len(triad)==3
    assert sum(n["beat"]==0 and n["staff"]==2 and n["status"]=="matched" for n in heads)==3
    for hand,index,number in [("Direita",1,4),("Esquerda",2,3),("Ambas",0,7)]:
        tap_text([hand],exact=True,scroll=True)
        time.sleep(.4)
        cue=latest("POC_HAND_CUES ")
        assert cue and cue["hand"]==index and cue["count"]==number,("Missing full simultaneous cue",hand,cue)
        assert (index==0 or all(s==index for s in cue["staffs"]))
    screenshot("piano-osmd-polyphony-triad.png")
    # Actual ADB taps in the WebView: two different adjacent notes in the
    # same three-head chord and one note in the lower staff.
    clicked=[]
    for target in (triad[0],triad[1],next(n for n in heads if n["beat"]==0 and n["staff"]==2 and n["midi"]==48)):
        prior=len(records("POC_NOTE_TAP "))
        adb("shell","input","tap",str(target["x"]),str(target["y"]))
        time.sleep(.7)
        taps=records("POC_NOTE_TAP ")
        assert len(taps)>prior,("Touch not recognized",target)
        response=taps[-1]
        assert response["mapped"] and response["nativeMidi"]==target["midi"] and response["staff"]==target["staff"], ("Wrong MIDI head selected",target,response)
        clicked.append({"expected":target,"tap":response})
    screenshot("piano-osmd-polyphony-adjacent.png")
    # Move clock across sustaining notes and unisons, without extra MIDI attacks.
    tap_text(["Contínuo"],exact=True,scroll=True)
    tap_text(["Iniciar"],scroll=True)
    time.sleep(2.4)
    tap_text(["Pausar"],scroll=True)
    screenshot("piano-osmd-polyphony-unison.png")
    tap_text(["Início"],scroll=True)
    screenshot("piano-osmd-polyphony-both-staves.png")
    responsive_evidence("polyphony")
    screenshot("piano-osmd-polyphony-guide-restored.png")
    # A dedicated named screenshot is copied only from a *real* opening capture.
    Path("piano-osmd-polyphony-guide-open.png").write_bytes(Path("piano-03d-polyphony-open.png").read_bytes())
    Path("piano-polyphony-matrix.json").write_text(json.dumps({
       "sourceWritten":len(expected),"sourceAttacks":29,
       "osmdGraphical":proof["graphicalNotes"],"mapping":mapping,
       "verifiedHeads":observed.total(),"ambiguousHeads":ambiguities,
       "actualAdbTaps":clicked,"geometry":proof,
       "limitation":"API29 emulator; images require human inspection; physical hardware pending"
    },ensure_ascii=False,indent=2),encoding="utf-8")
    print("POLYPHONY_EMULATOR_PASS "+json.dumps({"heads":proof["graphicalNotes"],
       "mapping":mapping,"adbTaps":len(clicked)},ensure_ascii=False),flush=True)

if __name__=="__main__":main()

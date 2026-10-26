#!/usr/bin/env python3
"""Independent MusicXML staff audit; read-only, no production behavior changes."""
import collections
import json
import pathlib
import xml.etree.ElementTree as ET
import zipfile

BASE = pathlib.Path("app/src/main/assets/scores")
items = []
for path in sorted(BASE.glob("*.mxl")):
    with zipfile.ZipFile(path) as z:
        xml = next(z.read(x) for x in z.namelist() if x.lower().endswith(".xml") and not x.startswith("META-INF/"))
    root = ET.fromstring(xml)
    parts = root.findall("./part")
    record = {"file": path.name, "partCount": len(parts), "parts": [], "explicitStaffOneInLaterPart": 0,
              "notesOutsideGuide": 0, "normalNoteCount": 0, "pitchedLocalStaffMapping": collections.Counter()}
    for pi, part in enumerate(parts):
        counts = collections.Counter()
        id_ = part.get("id", "?")
        for note in part.iter("note"):
            if note.find("pitch") is None:
                continue
            record["normalNoteCount"] += 1
            staff = note.findtext("staff")
            actual_staff = int(staff) if staff else (1 if pi == 0 else 2)
            counts[f"explicit_{staff}" if staff is not None else "implicit"] += 1
            record["pitchedLocalStaffMapping"][str(actual_staff)] += 1
            if pi > 0 and staff == "1": record["explicitStaffOneInLaterPart"] += 1
            p=note.find("pitch")
            note_steps={"C":0,"D":2,"E":4,"F":5,"G":7,"A":9,"B":11}
            midi=12*(int(p.findtext("octave","4"))+1)+note_steps[p.findtext("step","C")]+int(p.findtext("alter","0"))
            if midi < 48 or midi > 95:
                record["notesOutsideGuide"] += 1
        record["parts"].append({"id":id_, "localStaffs":dict(counts)})
    record["pitchedLocalStaffMapping"]=dict(record["pitchedLocalStaffMapping"])
    items.append(record)

for rec in items:
    print("XML_PART_AUDIT",json.dumps(rec,ensure_ascii=False))
print("TOTALS",json.dumps({
    "scores":len(items),
    "notes":sum(x["normalNoteCount"] for x in items),
    "partCount2Plus":sum(x["partCount"]>1 for x in items),
    "wrongPartHandRiskNotes":sum(x["explicitStaffOneInLaterPart"] for x in items),
    "outsideGuide":sum(x["notesOutsideGuide"] for x in items)
},ensure_ascii=False))
pathlib.Path("build").mkdir(exist_ok=True)
pathlib.Path("build/recognition-corpus.json").write_text(json.dumps(items,ensure_ascii=False,indent=2),encoding="utf-8")

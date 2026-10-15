#!/usr/bin/env python3
"""Create ten ORIGINAL, non-melodic two-hand MusicXML test exercises.

The published examples must never be derived from the private beta's MXLs.
Usage: python PianoLivreAndroid/scripts/generate_public_exercises.py
"""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import json
import xml.etree.ElementTree as ET

def tag(name, text=None, **attributes):
    item = ET.Element(name, attributes)
    if text is not None:
        item.text = str(text)
    return item

def child(parent, name, text=None, **attributes):
    return ET.SubElement(parent, name, attributes) if text is None else ET.SubElement(parent, name, attributes)

def add_text(parent, name, value):
    e = ET.SubElement(parent, name)
    e.text = str(value)
    return e

def note(parent, step, octave, voice, staff, alter=0):
    n = ET.SubElement(parent, "note")
    p = ET.SubElement(n, "pitch")
    add_text(p, "step", step)
    if alter:
        add_text(p, "alter", alter)
    add_text(p, "octave", octave)
    add_text(n, "duration", 4)
    add_text(n, "voice", voice)
    add_text(n, "type", "quarter")
    add_text(n, "staff", staff)

def render(number):
    root = ET.Element("score-partwise", version="4.0")
    work = ET.SubElement(root, "work")
    add_text(work, "work-title", "Estudo livre " + str(number).zfill(2))
    add_text(root, "movement-title", "Estudo livre " + str(number).zfill(2))
    parts = ET.SubElement(root, "part-list")
    sp = ET.SubElement(parts, "score-part", id="P1")
    add_text(sp, "part-name", "Piano")
    part = ET.SubElement(root, "part", id="P1")
    steps = ["C", "D", "E", "F", "G", "A", "B"]
    for bar in range(4):
        m = ET.SubElement(part, "measure", number=str(bar+1))
        if bar == 0:
            attributes = ET.SubElement(m, "attributes")
            add_text(attributes, "divisions", 4)
            key = ET.SubElement(attributes, "key")
            add_text(key, "fifths", 0)
            meter = ET.SubElement(attributes, "time")
            add_text(meter, "beats", 4)
            add_text(meter, "beat-type", 4)
            add_text(attributes, "staves", 2)
            clef1 = ET.SubElement(attributes, "clef", number="1")
            add_text(clef1, "sign", "G")
            add_text(clef1, "line", 2)
            clef2 = ET.SubElement(attributes, "clef", number="2")
            add_text(clef2, "sign", "F")
            add_text(clef2, "line", 4)
        for beat in range(4):
            idx = (number + bar + beat) % len(steps)
            # Deliberately vary notes, intervals and staff; not a copied tune.
            note(m, steps[idx], 4 + (idx >= 5), 1, 1)
        back = ET.SubElement(m, "backup")
        add_text(back, "duration", 16)
        for beat in range(4):
            idx = (number * 2 + bar * 2 + beat) % len(steps)
            note(m, steps[idx], 3, 2, 2)
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)

def generate(destination):
    destination = Path(destination)
    destination.mkdir(parents=True, exist_ok=True)
    if list(destination.iterdir()):
        raise RuntimeError("Public score destination must be empty")
    catalog = []
    container = (
        '<?xml version="1.0" encoding="UTF-8"?>'
        '<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">'
        '<rootfiles><rootfile full-path="score.xml"'
        ' media-type="application/vnd.recordare.musicxml+xml"/>'
        '</rootfiles></container>'
    )
    for i in range(1, 11):
        filename = f"estudo-livre-{i:02d}.mxl"
        with ZipFile(destination / filename, "w", ZIP_DEFLATED) as z:
            z.writestr("META-INF/container.xml", container)
            z.writestr("score.xml", render(i))
        catalog.append({"file": filename, "name": f"Estudo livre {i:02d}"})
    catalog_file = destination.parent / "catalog.json"
    catalog_file.write_text(json.dumps(catalog, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    return catalog

if __name__ == "__main__":
    path = Path(__file__).resolve().parents[1] / "app/src/main/assets/scores"
    if path.exists() and any(path.iterdir()):
        raise SystemExit("Refusing to overwrite existing scores.")
    print("Generated",len(generate(path)),"independent public MusicXML test exercises")

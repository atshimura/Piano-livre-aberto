#!/usr/bin/env python3
"""ZAI-007: file-level safety and user-facing contract (not an emulator test)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
def java(name):
    return (ROOT / "app/src/main/java/br/org/pianolivre" / name).read_text(encoding="utf-8")

composer = java("ScoreXmlComposer.java")
quantizer = java("ScoreRhythmQuantizer.java")
preview = java("CreationScorePreviewScreen.java")
store = java("CreationScoreStore.java")
assert "ZAI-008-rests-wrap" in composer
assert 'xml.append("<tie' not in composer
assert 'xml.append("<tied' not in composer
assert 'xml.append("<slur' not in composer
assert "singleFigureLength" in quantizer
assert "One MIDI Note On is one visible notehead" in quantizer
assert "hasPrevious" not in quantizer and "hasNext" not in quantizer
assert "ScoreXmlComposer.isCurrentDerivedXml(prior.xml)" in preview
assert "renderOutdated()" in preview
assert 'button("Gerar prévia atualizada"' in preview
assert 'button("Voltar à composição"' in preview
assert "os tempos completos permanecem na gravação MIDI original" in preview
assert "storage.delete(" not in preview
assert "MAGIC=0x504c5332, FORMAT=1" in store, "do not change PLS2 storage format"
assert "data.writeInt(e.status)" in store and "data.writeLong(e.timeNs)" in store
print("ZAI-007: contrato de regeneração segura e saída sem ligaduras PASS")

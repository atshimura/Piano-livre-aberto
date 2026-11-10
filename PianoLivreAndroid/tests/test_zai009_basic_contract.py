#!/usr/bin/env python3
"""ZAI-009: only basic notes and duration symbols; safe derivative regeneration."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def java(name):
 return (root/"app/src/main/java/br/org/pianolivre"/name).read_text(encoding="utf-8")
composer=java("ScoreXmlComposer.java")
preview=java("CreationScorePreviewScreen.java")
quant=java("ScoreRhythmQuantizer.java")
notation=java("NotationView.java")
assert "ZAI-009-basic-notes-duration" in composer
assert 'xml.append(token.rest ? "<note print-object=\\\"no\\\">" : "<note>")' in composer or 'xml.append(token.rest ? "<note print-object=\\"no\\">" : "<note>")' in composer
assert 'xml.append("<rest/>")' in composer, "hidden rests must keep musical duration"
assert 'xml.append("<duration>")' in composer
assert 'xml.append("<staccato' not in composer
assert 'xml.append("<tie' not in composer and 'xml.append("<slur' not in composer
assert "print-object" not in java("ScoreRhythmQuantizer.java"),"do not change source timing in quantizer"
assert "Gerar partitura básica" in preview
assert "isCurrentDerivedXml(prior.xml)" in preview
assert "A partitura antiga não será substituída até que você escolha Salvar partitura" in preview
assert "notation.loadCreationScore(score,0)" in preview
assert "PianoNotation.resize()" in notation
assert "staccato ou ligaduras" in preview
assert "os tempos completos permanecem na gravação MIDI original" in preview
print("ZAI009_BASIC_NOTATION_CONTRACT_PASS")

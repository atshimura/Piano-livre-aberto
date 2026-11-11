#!/usr/bin/env python3
"""ZAI-008 layout isolation and safe regeneration contract."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
def java(name):
    return (ROOT/"app/src/main/java/br/org/pianolivre"/name).read_text(encoding="utf-8")
html=(ROOT/"app/src/main/assets/notation.html").read_text(encoding="utf-8")
notation=java("NotationView.java")
preview=java("CreationScorePreviewScreen.java")
composer=java("ScoreXmlComposer.java")
quant=java("ScoreRhythmQuantizer.java")
assert "layout==='creation-page'" in html
assert "body.creation-page #viewport" in html
assert "overflow-x:hidden" in html and "overflow-y:auto" in html
assert "body.creation-page #score{width:100%" in html
assert "newSystemFromXML:freeLayout&&!creationPage" in html
assert "renderSingleHorizontalStaffline:studyProof" in html
assert "freeLayout=(layout==='free'||creationPage)" in html
assert 'layout=creationPage?"\'creation-page\'"' in notation
assert "loadCreationScore(Score score,double position)" in notation
assert "notation.loadCreationScore(score,0)" in preview
assert "PianoNotation.resize()" in notation
assert "getResources().getDisplayMetrics().heightPixels*0.65f" in preview
assert "ZAI-010-simple-figures" in composer
assert "isCurrentDerivedXml(prior.xml)" in preview
assert "Gerar partitura básica" in preview
assert "A partitura antiga não será substituída até que você escolha Salvar partitura" in preview
assert "restFigures(int from, int to)" in quant
assert "fillRests(tokens, cursor, d.pos)" in quant
assert "tokens.add(new Token(true, 0, figure" in quant
assert "no automatic ties" in composer or "No automatic ties" in composer
print("ZAI008_LAYOUT_AND_RESTS_CONTRACT_PASS")

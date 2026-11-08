#!/usr/bin/env python3
"""Implementation 3 source and future full-suite contracts; no emulator or APK."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
def java(name):
    return (ROOT/"app/src/main/java/br/org/pianolivre"/name).read_text(encoding="utf-8")
class RhythmContracts(unittest.TestCase):
    def test_source_midi_not_mutated(self):
        engine=java("CreationScoreTranscriber.java")
        self.assertIn("interpretAttacks(notes,options,grid)",engine)
        self.assertIn("options.handOverrides.getOrDefault",engine)
        self.assertIn("regularization!=Regularization.RIGOROUS",engine)
        self.assertIn("if(!options.legacyTiming)",engine)
        self.assertNotIn("events.remove(",engine)
    def test_edit_ui_is_connected(self):
        screen=java("CreationScorePreviewScreen.java")
        for action in ('"Uniformizar trecho"','"Atribuir mão / voz"',
                       '"Regularização rítmica"','"Ver prévia"','"Aplicar"',
                       'previewUniformize(duration,hand,voice)','applyUniformize(plan)',
                       '"Restaurar ritmo automático"'):
            self.assertIn(action,screen)
        self.assertIn("setEnabled(review.selectedCount()>0)",screen)
        self.assertIn("Selecionar intervalo de compassos",screen)
        self.assertIn("syncSelection()",screen)
    def test_sidecar_backwards_compatibility(self):
        store=java("CreationScoreStore.java")
        # Formato 4 guarda o modelo moderno completo (grafia/transporte/editor);
        # formatos 1 e 2 (legados) e 3 (Onda 4, quintas da armadura) continuam
        # sendo lidos e regenerados byte a byte.
        self.assertIn("FORMAT=4, FORMAT_LEGACY=1",store)
        self.assertIn("format!=FORMAT_LEGACY&&format!=2&&format!=3",store)
        self.assertIn("Options.legacy(",store)
        self.assertIn("Options.onda4(",store)
        self.assertIn("writeIntMap(data,o.handOverrides)",store)
        self.assertIn("o.rhythmOverrides.size()",store)
        self.assertIn("CreationScoreTranscriber.transcribe(p.events,p.segments,o,savedTitle).xml",store)
    def test_safety_conflicts_and_undo(self):
        review=java("CreationScoreReview.java")
        for code in ('previewUniformize(int duration,int handFilter,int voiceFilter)',
                     'throw new IOException("Selecione todas as notas do acorde',
                     'throw new IOException("A figura não cabe no trecho',
                     'if(!targets.contains(n.sourceId)',
                     'public synchronized void applyUniformize',
                     'public synchronized void assignSelection',
                     'public synchronized boolean undo()','public synchronized boolean redo()'):
            self.assertIn(code,review)
    def test_full_suite_scripts_have_coverage(self):
        suite=(ROOT/"tests/c2_d_ui_smoke.py").read_text(encoding="utf-8")
        native=(ROOT/"app/src/main/java/br/org/pianolivre/CreationSelfTest.java").read_text(encoding="utf-8")
        workflow=(ROOT.parent/".github/workflows/emulator.yml").read_text(encoding="utf-8")
        self.assertIn("python3 tests/c2_d_ui_smoke.py",workflow)
        build=(ROOT.parent/".github/workflows/build.yml").read_text(encoding="utf-8")
        self.assertIn("python3 tests/test_creation_rhythm_contract.py",build)
        self.assertIn("tests/CreationRhythmUniformizeTest.java",build)
        self.assertIn("br.org.pianolivre.CreationRhythmUniformizeTest",build)
        self.assertIn("Uniformizar trecho",suite)
        self.assertIn("Atribuir mão / voz",suite)
        self.assertIn("MIDI",native)
if __name__=="__main__":unittest.main()

#!/usr/bin/env python3
"""Regression contracts for the six interface changes, executable without APK."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
def source(path):
    base=ROOT.parent if path.startswith(".github/") else ROOT
    return (base/path).read_text(encoding="utf-8")

class SixChangesSuiteContracts(unittest.TestCase):
    def test_home_two_shortcuts_and_settings(self):
        screen=source("app/src/main/java/br/org/pianolivre/PianoScreens.java")
        checks=source("tests/home_layout_checks.py")
        self.assertIn('btn("Biblioteca Musical"',screen)
        self.assertIn('btn("Laboratório Musical"',screen)
        self.assertIn("len(shortcuts)==2",checks)
        self.assertIn("('Histórico','Como usar')",checks)
        self.assertIn('"Calibrar acordes MIDI"',screen)

    def test_library_three_controls_and_accessible_search(self):
        checks=source("tests/home_layout_checks.py")
        ui=source("tests/emulator_ui.py")
        for tag in ("home-library-collections","home-library-import","home-library-search"):
            self.assertIn(tag,checks)
        self.assertIn("Toolbar controls not equal width",checks)
        self.assertIn("Toolbar controls not equal height",checks)
        self.assertIn("Buscar por título ou nome de arquivo",ui)
        self.assertNotIn("Buscar título ou arquivo",ui)
        self.assertNotIn('"buscar título"',ui)
        self.assertIn('tap_text(["Coleções"]',ui)

    def test_study_bars_no_longer_hide_in_midi_suite(self):
        fixture=source("app/src/main/java/br/org/pianolivre/MidiInterfaceSelfTest.java")
        midi=source("tests/emulator_midi_interfaces.py")
        geometry=source("tests/emulator_study_layout.py")
        self.assertIn("study-playing",midi)
        self.assertIn("study-note-tap",midi)
        self.assertIn("stable_score_bounds",midi)
        self.assertIn("layout['headerVisible'] and layout['modesVisible']",midi)
        self.assertIn("check(a.running&&a.screens.chrome.visible()",fixture)
        self.assertNotIn("!a.screens.chrome.visible()",fixture)
        self.assertIn("after_layout['bounds']==layout['bounds']",geometry)
        self.assertIn("No measured gain over Prompt 1",geometry)
        # During active playback UIAutomator is not an idle-safe oracle.
        # The old reveal gesture is obsolete: assert native chrome geometry
        # without inspecting hierarchy or tapping the moving score.
        ui=source("tests/emulator_ui.py")
        helper=ui.split("def reveal_study_controls():",1)[1].split("def study_renderer(",1)[0]
        self.assertNotIn("dump()",helper)
        self.assertNotIn('adb("shell","input","tap"',helper)
        self.assertIn('layout["headerVisible"] and layout["modesVisible"]',helper)
        self.assertIn('transport[3]>transport[1]',helper)
        self.assertIn('if state=="restored":reveal_study_controls()',ui)
        self.assertIn('tap_text(["Calibrar acordes MIDI"],scroll=True)',ui)
        accessible_wait=ui.split("def wait_for_text(",1)[1].split("def focus_library_search_field(",1)[0]
        self.assertIn('for value in (n.attrib.get("text",""),n.attrib.get("content-desc",""))',accessible_wait)
        self.assertNotIn(' or n.attrib.get("content-desc","")',accessible_wait)



    def test_library_selftest_checks_empty_before_adding_creation(self):
        native=source("app/src/main/java/br/org/pianolivre/LibrarySelfTest.java")
        # The empty-store precondition was previously checked AFTER an
        # intentional addCreation + toggleFavorite, crashing the emulator.
        self.assertLess(native.index('"empty metadata is read-only"'),
                        native.index('store.addCreation(creationId)'))
        self.assertIn('store.snapshot().addedCreations.size()==1',native)
        self.assertIn('linked created score and favorite survive backup/restore',native)
        # Free Reading now returns to the current Library scroll offset.
        ui=source("tests/emulator_ui.py")
        self.assertIn('wait_for_text("Partitura da biblioteca")',ui)
        self.assertNotIn('tap_text(["← Biblioteca"],exact=True);wait_for_text("Estudar Ode à Alegria")',ui)


    def test_creation_shortcuts_and_note_actions_still_covered(self):
        nav=source("tests/emulator_creation_navigation.py")
        creation=source("app/src/main/java/br/org/pianolivre/MusicCreationScreen.java")
        preview=source("app/src/main/java/br/org/pianolivre/CreationScorePreviewScreen.java")
        self.assertIn("Creation shortcuts unequal width",nav)
        self.assertIn("Creation shortcuts must share a row",nav)
        self.assertIn("creation-responsive-actions",creation)
        self.assertIn("creation-score-note-row",preview)
        self.assertIn("Excluir definitivamente?",creation)

    def test_generated_score_studied_from_library_only(self):
        ui=source("tests/c2_d_ui_smoke.py")
        self.assertIn('click("Adicionar à Biblioteca")',ui)
        self.assertIn('find("Partitura adicionada à Biblioteca Musical.")',ui)
        self.assertIn('click("OK")',ui)
        self.assertIn('find("Estudar C2-D partitura de teste",clickable=True)',ui)
        self.assertIn('find("MODO")',ui)
        self.assertIn('find("MÃOS")',ui)
        self.assertNotIn('click("Estudar esta partitura")',ui)

    def test_midi_visual_evidence_matches_current_fixture_stages(self):
        # The evidence gate must require every screenshot produced by the
        # actual MIDI fixture, not obsolete hidden/revealed chrome states.
        import ast
        import re
        midi=source("tests/emulator_midi_interfaces.py")
        workflow=source(".github/workflows/emulator.yml")
        tree=ast.parse(midi)
        stages=next(ast.literal_eval(node.value) for node in tree.body
                    if isinstance(node,ast.Assign)
                    and any(isinstance(target,ast.Name) and target.id=="stages"
                            for target in node.targets))
        match=re.search(r"for stage in \(([^)]*)\):\s*\n\s*names\.append\('piano-prompt03-",workflow)
        self.assertIsNotNone(match,"Missing mandatory MIDI evidence loop")
        expected=list(ast.literal_eval("("+match.group(1)+")"))
        # Diagnostics is a mandatory screenshot captured inside study-connected,
        # not an additional state in the fixture's stage/acknowledgement loop.
        # Keep the gate's complete requirements, including diagnostics.
        self.assertEqual(expected,stages+["diagnostics"],
                         "Visual gate must include every MIDI stage and its diagnostics capture")
        self.assertIn("screenshot('piano-prompt03-'+label+'-diagnostics.png')",midi)
        self.assertIn("study-playing",stages)
        self.assertIn("study-note-tap",stages)
        self.assertNotIn("study-hidden",stages)
        self.assertNotIn("study-restored",stages)
        self.assertIn("assert_staff_ink(record)",midi)
        self.assertIn("layout['headerVisible'] and layout['modesVisible']",midi)

    def test_historical_baseline_is_not_candidate_ui(self):
        historical=source("tests/emulator_study_baseline.py")
        self.assertIn("Começar pela Ode à Alegria",historical)
        self.assertIn("worktree",historical)

if __name__=="__main__":
    unittest.main()

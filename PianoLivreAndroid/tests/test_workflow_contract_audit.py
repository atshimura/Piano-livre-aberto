#!/usr/bin/env python3
"""Fast static preflight: no obsolete cold-start Study assertions in 0.9.61.

Run before the expensive browser/emulator jobs. This complements, never replaces,
real functional checks and protects build/emulator publishing gates.
"""
import pathlib
import re
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
def source(name):
    # Android test sources live below PianoLivreAndroid; Actions YAML lives
    # at the repository root, one directory above.
    base=ROOT.parent if name.startswith(".github/") else ROOT
    return (base/name).read_text(encoding="utf-8")

class FullSuiteContractAudit(unittest.TestCase):
    def test_all_direct_workflow_test_references_exist(self):
        for wf in ("build.yml","emulator.yml","c2-basic.yml","home-basic.yml"):
            script=source(".github/workflows/"+wf)
            names=set(re.findall(r"tests/[A-Za-z0-9_./-]+\.(?:py|js|java|sh)",script))
            self.assertTrue(names,wf+" has no test references")
            for name in names:
                self.assertTrue((ROOT/name).is_file(), wf+" references missing "+name)

    def test_release_initialization_and_reentry(self):
        src=source("tests/emulator_release.py")
        self.assertLess(src.index("wait_for_text('Bem-vindo ao'"),src.index("wait_for_text('80%'"))
        self.assertIn("tap_text(['Ode à Alegria']",src)
        self.assertIn("run-as",src)
        self.assertIn("STUDY_CHROME_STATE",src)

    def test_public_upgrade_compares_saved_data_before_study(self):
        src=source("tests/emulator_public_upgrade.py")
        self.assertLess(src.index('wait_for_text("Bem-vindo ao"'),src.index('tap_text(["Ode à Alegria"]'))
        self.assertLess(src.index('for key in (\'active_profile\''),src.index('tap_text(["Ode à Alegria"]'))
        self.assertIn('user_7_tempo',src)
        self.assertIn('certificate(signer,NEW_APK)==EXPECTED_CERT',src)

    def test_signed_release_runs_after_debug_only_c2(self):
        wf=source(".github/workflows/emulator.yml")
        self.assertLess(wf.index("python3 tests/c2_d_ui_smoke.py"),wf.index("python3 tests/emulator_release.py"))
        self.assertLess(wf.index("python3 tests/emulator_release.py"),wf.index("python3 tests/emulator_public_upgrade.py"))
        self.assertIn("test \"$FULL_RESULT\" = success",wf)
        self.assertIn("needs: [classify, debug-apk, ui-fast, emulator-suite]",wf)

    def test_six_changes_are_in_full_and_basic_preflight(self):
        full=source(".github/workflows/build.yml")
        basic=source(".github/workflows/home-basic.yml")
        for name in ("test_home_banner_contract.py","test_study_compact_chrome_contract.py",
                     "test_creation_responsive_contract.py","test_creation_library_contract.py",
                     "test_suite_six_changes_contract.py"):
            self.assertIn("python3 tests/"+name,full)
            self.assertIn("python3 tests/"+name,basic)
        self.assertLess(full.index("Guard six approved"),full.index("Adaptive Study profile"))

    def test_midi_suite_expects_bars_permanently_visible(self):
        fixture=source("app/src/main/java/br/org/pianolivre/MidiInterfaceSelfTest.java")
        emulator=source("tests/emulator_midi_interfaces.py")
        self.assertNotIn("!a.screens.chrome.visible()",fixture)
        self.assertNotIn("study-hidden",fixture+emulator)
        self.assertNotIn("study-restored",fixture+emulator)
        self.assertIn("study-playing",fixture)
        self.assertIn("study-note-tap",emulator)
        self.assertIn("layout['headerVisible'] and layout['modesVisible']",emulator)

    def test_c2_d_routes_through_library_not_creation_study(self):
        c2=source("tests/c2_d_ui_smoke.py")
        self.assertNotIn('click("Estudar esta partitura")',c2)
        self.assertIn('click("Adicionar à Biblioteca")',c2)
        self.assertIn('click("OK")',c2)
        self.assertIn('find("Estudar C2-D partitura de teste",clickable=True)',c2)

    def test_historical_baseline_old_button_is_intentional(self):
        old=source("tests/emulator_study_baseline.py")
        self.assertIn("worktree",old)
        self.assertIn("Começar pela Ode à Alegria",old)

    def test_active_profile_loaded_without_study_autostart(self):
        src=source("app/src/main/java/br/org/pianolivre/MainActivity.java")
        self.assertIn('double savedTempo=Double.longBitsToDouble',src)
        self.assertIn('currentHand=Math.max(0,Math.min(2,prefs.getInt(studyPrefix+"hand",0)))',src)
        self.assertIn('mode=Math.max(0,Math.min(2,prefs.getInt(studyPrefix+"mode",0)))',src)
        self.assertIn('scoreView.showKeyboard=prefs.getBoolean(studyPrefix+"keyboard",false)',src)
        self.assertIn('STARTUP_HOME lastScoreSaved=',src)
        self.assertIn('if(tempoBar!=null)tempoBar.setProgress',src)

if __name__=="__main__":
    unittest.main()

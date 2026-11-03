#!/usr/bin/env python3
"""Only static/basic contracts. Full Android emulator scripts are updated, NOT run."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
APP=ROOT/"app/src/main"
def src(s):return (ROOT/s).read_text(encoding="utf8")
class Impl4(unittest.TestCase):
 def test_single_renderer(self):
  html=(APP/"assets/notation.html").read_text(encoding="utf8")
  bridge=src("app/src/main/java/br/org/pianolivre/NotationView.java")
  screens=src("app/src/main/java/br/org/pianolivre/PianoScreens.java")
  self.assertNotIn("temporal-notation.js",html)
  self.assertNotIn("TemporalNotation",html)
  self.assertNotIn("AUTO_TEMPORAL_FALLBACK",bridge)
  self.assertNotIn("legacyFallback",screens)
  self.assertNotIn("PianoView",screens)
  self.assertNotIn("PianoView",src("app/src/main/java/br/org/pianolivre/MainActivity.java"))
  self.assertFalse((APP/"assets/temporal-notation.js").exists())
  self.assertFalse((APP/"java/br/org/pianolivre/PianoView.java").exists())
 def test_study_gate_on_both_paths(self):
  creation=src("app/src/main/java/br/org/pianolivre/CreationScorePreviewScreen.java")
  activity=src("app/src/main/java/br/org/pianolivre/MainActivity.java")
  gate=src("app/src/main/java/br/org/pianolivre/CreationStudyCompatibility.java")
  self.assertIn("CreationStudyCompatibility.check(activity,target",creation)
  self.assertIn("CreationStudyCompatibility.check(this,checked",activity)
  self.assertIn("view[0].loadScore(score,0,false)",gate)
  self.assertIn("view[0].isEngravingProof()",gate)
  self.assertIn("current.xml.equals(checked.sourceXml)",activity)
 def test_error_and_privacy(self):
  view=src("app/src/main/java/br/org/pianolivre/NotationView.java")
  main=src("app/src/main/java/br/org/pianolivre/MainActivity.java")
  data=src("app/src/main/java/br/org/pianolivre/NotationDiagnosticsStore.java")
  self.assertIn("onFailure(long token,String code,String stage,String message)",view)
  self.assertIn("token!=scoreGeneration",view)
  self.assertIn("Não foi possível exibir a partitura",main)
  self.assertIn("Copiar diagnóstico",main)
  self.assertIn("50,MAX_BYTES=256*1024",data)
  self.assertNotIn("http",data)
 def test_full_suite_is_updated(self):
  wf=(ROOT.parent/".github/workflows/build.yml").read_text(encoding="utf8")
  android=(ROOT.parent/".github/workflows/emulator.yml").read_text(encoding="utf8")
  smoke=src("tests/c2_d_ui_smoke.py")
  self.assertIn("CreationOSMDStudyBrowserTest.js",wf)
  self.assertIn("IMPL4",smoke)
  self.assertIn("emulator_study_failure.py",android)
if __name__=="__main__":unittest.main()

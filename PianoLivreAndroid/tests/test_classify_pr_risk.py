import importlib.util
import pathlib
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "scripts" / "classify_pr_risk.py"
spec = importlib.util.spec_from_file_location("risk", SCRIPT)
risk = importlib.util.module_from_spec(spec)
spec.loader.exec_module(risk)


class RiskClassifierTests(unittest.TestCase):
    def test_safe_single_raster(self):
        self.assertEqual("visual", risk.classify([("M", "PianoLivreAndroid/app/src/main/res/drawable/banner.webp")]))

    def test_safe_multiple_rasters(self):
        self.assertEqual("visual", risk.classify([
            ("A", "PianoLivreAndroid/app/src/main/res/drawable-hdpi/badge.png"),
            ("M", "PianoLivreAndroid/app/src/main/res/drawable/background.jpg"),
        ]))

    def test_java_and_layout_are_full(self):
        for name in [
            "PianoLivreAndroid/app/src/main/java/br/org/pianolivre/PianoScreens.java",
            "PianoLivreAndroid/app/src/main/res/layout/main.xml",
            "PianoLivreAndroid/app/src/main/res/values/colors.xml",
            ".github/workflows/emulator.yml",
            "PianoLivreAndroid/scripts/classify_pr_risk.py",
            "docs/10-CONTINUIDADE.md",
            "PianoLivreAndroid/app/src/main/res/mipmap-hdpi/ic_launcher.png",
        ]:
            with self.subTest(name=name):
                self.assertEqual("full", risk.classify([("M", name)]))

    def test_deletion_is_full(self):
        self.assertEqual("full", risk.classify([("D", "PianoLivreAndroid/app/src/main/res/drawable/logo.png")]))

    def test_mixed_is_full(self):
        self.assertEqual("full", risk.classify([
            ("M", "PianoLivreAndroid/app/src/main/res/drawable/banner.png"),
            ("M", "PianoLivreAndroid/app/src/main/java/br/org/pianolivre/MainActivity.java"),
        ]))

    def test_no_changes_is_full(self):
        self.assertEqual("full", risk.classify([]))


if __name__ == "__main__":
    unittest.main()

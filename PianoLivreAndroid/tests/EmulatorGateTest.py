"""Required emulator gate must fail closed for both risk tiers."""
import itertools
import os
import pathlib
import re
import shutil
import subprocess
import textwrap
import unittest


class EmulatorGateTest(unittest.TestCase):
    def test_real_gate_commands_for_all_dependency_outcomes(self):
        workflow = (pathlib.Path(__file__).resolve().parents[2] / ".github/workflows/emulator.yml").read_text(encoding="utf-8")
        block = re.search(r"(?ms)^  emulator:\n(.*?)(?=^  [\w-]+:\n|\Z)", workflow).group(1)
        self.assertIn("needs: [classify, debug-apk, ui-fast, emulator-suite]", block)
        self.assertIn("if: ${{ always() }}", block)
        self.assertNotIn("continue-on-error", block)
        commands = textwrap.dedent(block.split("run: |", 1)[1]).strip()
        bash = shutil.which("bash")
        self.assertIsNotNone(bash)
        states = ("success", "failure", "cancelled", "skipped")
        for tier, classified, debug, fast, full in itertools.product(
            ("visual", "full", "unknown"), states, states, states, states
        ):
            with self.subTest(tier=tier, classified=classified, debug=debug, fast=fast, full=full):
                env = {**os.environ, "TIER": tier, "TIER_RESULT": classified,
                       "DEBUG_RESULT": debug, "FAST_RESULT": fast, "FULL_RESULT": full}
                result = subprocess.run([bash, "-e", "-c", commands], env=env, capture_output=True)
                expected = classified == "success" and debug == "success" and (
                    (tier == "visual" and fast == "success" and full == "skipped") or
                    (tier == "full" and full == "success")
                )
                self.assertEqual(result.returncode == 0, expected)


if __name__ == "__main__":
    unittest.main()

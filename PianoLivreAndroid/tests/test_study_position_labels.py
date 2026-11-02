#!/usr/bin/env python3
"""Offline, deterministic preflight for the Android Study position assertion."""
import unittest
from emulator_study_session import study_position_values

class StudyPositionLabelTest(unittest.TestCase):
    def test_explicit_zero_header(self):
        values=["Estudo livre 01\n♫ 100 BPM × 100% · posição 0.0",
                "Estudo livre 01 · ♫ 100 BPM × 100% · posição 0.0"]
        self.assertEqual(study_position_values(values),[0.0,0.0])

    def test_no_false_positive_for_nonzero(self):
        self.assertEqual(study_position_values(["posição 1.0","posição -2.5"]),[1.0,-2.5])
        self.assertFalse(any(abs(x)<0.05 for x in study_position_values(["posição 1.0"])))

    def test_skip_unrelated_and_stale_labels(self):
        self.assertEqual(study_position_values(["Início","posição 3.0","", "posição 0,0"]),[3.0,0.0])

    def test_missing_and_malformed(self):
        self.assertEqual(study_position_values(["100%","posição --","outro texto"]),[])

if __name__=="__main__":
    unittest.main()

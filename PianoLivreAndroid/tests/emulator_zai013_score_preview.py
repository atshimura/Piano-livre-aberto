#!/usr/bin/env python3
"""Compatibility runner: ZAI-013 score-view regression follows the unified bar
from ZAI-014. Both historic and new CI must exercise the CURRENT UI, not a retired
bottom Save/Back layout.
"""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).with_name("emulator_zai014_unified_bar.py")),run_name="__main__")

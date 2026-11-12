#!/usr/bin/env python3
"""ZAI-012: measured content viewport and explicit top position, no fake full-screen height."""
from pathlib import Path
base=Path(__file__).resolve().parents[1]/"app/src/main/java/br/org/pianolivre"
landing=(base/"MusicLabLandingScreen.java").read_text(encoding="utf-8")
heights=(base/"ZaiAdaptiveHeights.java").read_text(encoding="utf-8")
screens=(base/"PianoScreens.java").read_text(encoding="utf-8")
assert "scroll.addOnLayoutChangeListener" in landing
assert "fitViewportHeight(b-t)" in landing
assert "if(scroll.getHeight()>0)fitViewportHeight(scroll.getHeight())" in landing
assert "ZaiAdaptiveHeights.landingContent(heightDp,fontScale)" in landing
assert "fitVisibleHeight(height)" not in landing
assert "onSizeChanged(int width,int height,int oldWidth,int oldHeight)" not in landing
assert "scroll.setFocusableInTouchMode(true)" in landing
assert "scroll.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS)" in landing
assert "void showAtTop()" in landing
assert "scroll.scrollTo(0,0)" in landing
assert "labLanding.showAtTop()" in screens
assert "static Landing landingContent(int viewportHeightDp,float fontScale)" in heights
assert "int free=viewportHeightDp-top-bottom-title-description-gap" in heights
assert "free-2" in heights
assert 'lab-landing-three-columns' in landing
assert '"Abrir Aprender   →",true,goLearn' in landing
assert 'goDiscover' in landing and 'goCreation' in landing
assert 'creation.requestLeave();return;' in screens
print("ZAI012_CONTENT_VIEWPORT_CONTRACT_PASS")

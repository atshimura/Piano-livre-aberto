#!/usr/bin/env python3
"""ZAI-011: no global scroll for landscape pages; preserve independent list overflow."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def src(name):
    return (root/"app/src/main/java/br/org/pianolivre"/name).read_text(encoding="utf-8")
landing=src("MusicLabLandingScreen.java")
creation=src("MusicCreationScreen.java")
planner=src("ZaiAdaptiveHeights.java")
assert "scroll.addOnLayoutChangeListener" in landing
assert "ZaiAdaptiveHeights.landingContent(heightDp,fontScale)" in landing
assert "setHeight(selectionTitle,size.titleHeight)" in landing
assert "setHeight(selectionDescription,size.descriptionHeight)" in landing
assert "row.height=height;columns.setLayoutParams(row)" in landing
assert "for(LinearLayout card:modeCards)compactCard(card,size.compact,fontScale)" in landing
assert "scroll.setFillViewport(true)" in landing  # emergency overflow only
assert 'lab-landing-three-columns' in landing
assert '"Abrir Aprender   →",true,goLearn' in landing
assert "fitsWithoutScroll" in planner
assert "fontScale" in planner and "usableHeightDp" in planner
assert "body.addView(columns,new LayoutParams(-1,0,1))" in creation
assert "addView(body,new LayoutParams(-1,0,1))" in creation
assert "scroll.addView(body)" not in creation, "screen-wide scrolling must be removed"
assert "leftScroll.addView(dashboardLeft" in creation
assert "scroll.addView(dashboardRight" in creation
assert 'leftScroll.setTag("creation-left-scroll")' in creation
assert 'scroll.setTag("creation-right-scroll")' in creation
assert "ZaiAdaptiveHeights.creationMenu(paneDp,recoveryWarning!=null)" in creation
assert "scroll.getScrollY()" in creation and "scroll.scrollTo(0,listScrollY)" in creation
assert 'creation-dashboard-two-columns' in creation
assert 'creation-dashboard-header' in creation
assert "captureSession" in creation and "persistJournalDelta()" in creation
assert "requestLeave()" in creation and "hasUnsavedTake()" in creation
assert "new CreationJournal(" in creation and "store.deletePermanently(id)" in creation
assert "CreationScoreDeletion.delete(" in creation
print("ZAI011_VERTICAL_LAYOUT_CONTRACT_PASS")

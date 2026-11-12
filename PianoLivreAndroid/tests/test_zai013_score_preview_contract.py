#!/usr/bin/env python3
"""ZAI-013: fixed top tempo/zoom, independent score viewport and fixed save bar."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
screen=(root/"app/src/main/java/br/org/pianolivre/CreationScorePreviewScreen.java").read_text(encoding="utf-8")
html=(root/"app/src/main/assets/notation.html").read_text(encoding="utf-8")

def must(s,source=screen):
    assert s in source, f"Missing UI contract: {s}"
def must_not(s,source=screen):
    assert s not in source, f"Legacy overlap/size contract remains: {s}"

# ZAI-014 retains ZAI-013's unobstructed staff, with a single unified header.
must('header.setTag("creation-score-unified-header")')
must('addView(header,new LayoutParams(-1,dp(56)))')
must('toolbarScroll.setTag("creation-score-inline-tools-scroll")')
must('scoreToolbar.setTag("creation-score-tempo-zoom-toolbar")')
must('headerActionSlot.setTag("creation-score-top-action-slot")')
must('scoreArea.setTag("creation-score-reading-area")')
must('scroll.setVisibility(GONE)')
must('scoreArea.setVisibility(VISIBLE)')
must('scoreArea.addView(sheet,new LayoutParams(-1,0,1))')
must('sheet.addView(view,new LayoutParams(-1,0,1))')
must('view.setTag("creation-score-notation-viewport")')
must('chip.setTag("creation-score-tempo-chip")')
must('smaller.setTag("creation-score-zoom-out")')
must('larger.setTag("creation-score-zoom-in")')
must('fit.setTag("creation-score-zoom-fit")')
must('info.setTag("creation-score-midi-info")')
must('headerActionSlot.addView(primary,new LayoutParams(-1,dp(48)))')
must('primary.setTag("creation-score-top-save")')
must('primary.setTag("creation-score-top-library-add")')
must('primary.setTag("creation-score-top-library-added")')
must('setCreationZoom(zoom)')
must('zoom/1.25f')
must('zoom*1.25f')
must('applyZoom(1.0f)')
must('Math.max(MIN_ZOOM,Math.min(MAX_ZOOM,target))')
must('activity.addCreationScoreToLibrary(project.id,ok->')
must('CreationStudyCompatibility.check(activity,target')
must('new CreationScoreStore(activity.getFilesDir()).save(')
must('window.dismiss()')
must_not('scoreFooter')
must_not('heightPixels*0.65f')
must_not('attach(contents,sheet')
must_not('attach(sheet,view')
must('new RippleDrawable(')
must('GradientDrawable')
must('body.creation-page #viewport{width:100%;overflow-y:auto;overflow-x:hidden',html)
must('padding:4px 0 64px;overflow-x:clip',html)
must('body.free #viewport{overflow:auto;scroll-behavior:auto}',html)
must('body.continuous #viewport{overflow:hidden}',html)
must('body.preview #viewport{overflow-x:auto;overflow-y:hidden',html)
assert screen.index('addView(header,new LayoutParams(-1,dp(56)))') < screen.index('scoreArea.addView(sheet'), "header precedes the musical area"
print("ZAI013_SCORE_PREVIEW_CONTRACT_PASS updated_for_ZAI014")

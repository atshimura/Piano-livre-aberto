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

must('toolbarScroll.setVisibility(VISIBLE)')
must('scoreArea.setVisibility(VISIBLE)')
must('scoreFooter.setVisibility(VISIBLE)')
must('scroll.setVisibility(GONE)')
must('scoreToolbar.setTag("creation-score-tempo-zoom-toolbar")')
must('scoreArea.setTag("creation-score-reading-area")')
must('scoreFooter.setTag("creation-score-fixed-footer")')
must('chip.setTag("creation-score-tempo-chip")')
must('smaller.setTag("creation-score-zoom-out")')
must('larger.setTag("creation-score-zoom-in")')
must('fit.setTag("creation-score-zoom-fit")')
must('scoreArea.addView(sheet,new LayoutParams(-1,0,1))')
must('sheet.addView(view,new LayoutParams(-1,0,1))')
must('view.setTag("creation-score-notation-viewport")')
must('actions(scoreFooter,back,save,add)')
must('actions(scoreFooter,back,save)')
must('tempoLabel.setContentDescription')
must('setCreationZoom(zoom)')
must('zoom/1.25f')
must('zoom*1.25f')
must('applyZoom(1.0f)')
must('Math.max(MIN_ZOOM,Math.min(MAX_ZOOM,target))')
must('activity.addCreationScoreToLibrary(project.id,ok->')
must('CreationStudyCompatibility.check(activity,target')
must('new CreationScoreStore(activity.getFilesDir()).save(')
must('window.dismiss()')
must_not('actions(sheet,button("A−"')
must_not('heightPixels*0.65f')
must_not('attach(contents,sheet')
must_not('attach(sheet,view')
must('new RippleDrawable(')
must('GradientDrawable')
# Only Creation is permitted to alter its own score overflow.
must('body.creation-page #viewport{width:100%;overflow-y:auto;overflow-x:hidden',html)
must('padding:4px 0 64px;overflow-x:clip',html)
must('body.free #viewport{overflow:auto;scroll-behavior:auto}',html)
must('body.continuous #viewport{overflow:hidden}',html)
must('body.preview #viewport{overflow-x:auto;overflow-y:hidden',html)
assert screen.index('renderScoreToolbar();')<screen.index('scoreArea.addView(sheet'), "toolbar must be laid out above score"
assert screen.index('scoreArea.addView(sheet')<screen.index('actions(scoreFooter,back,save'), "footer after score"
print("ZAI013_SCORE_PREVIEW_CONTRACT_PASS checks=36")

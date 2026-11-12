#!/usr/bin/env python3
"""ZAI-014: one toolbar, always-visible Back/Save, score fills remaining height."""
from pathlib import Path
s=(Path(__file__).resolve().parents[1]/"app/src/main/java/br/org/pianolivre/CreationScorePreviewScreen.java").read_text(encoding="utf-8")
must=[
 'header.setTag("creation-score-unified-header")',
 'header.addView(close,new LayoutParams(dp(48),dp(48)))',
 'header.addView(toolbarScroll,new LayoutParams(dp(306),-1))',
 'headerActionSlot.setTag("creation-score-top-action-slot")',
 'header.addView(headerActionSlot,actionSlotParams)',
 'addView(header,new LayoutParams(-1,dp(56)))',
 'nameLabel.setEllipsize(android.text.TextUtils.TruncateAt.END)',
 'nameLabel.setContentDescription("Composição: "+p.title)',
 'toolbarScroll.setHorizontalScrollBarEnabled(false)',
 'fitUnifiedHeader(int screenWidthPx)',
 'int available=Math.max(112,widthDp-48-116-16-5-titleRoom)',
 'toolbarScroll.setLayoutParams(lp)',
 'scoreArea.setTag("creation-score-reading-area")',
 'scoreArea.addView(sheet,new LayoutParams(-1,0,1))',
 'sheet.addView(view,new LayoutParams(-1,0,1))',
 'scroll.setVisibility(GONE)',
 'scoreArea.setVisibility(VISIBLE)',
 'Button smaller=compactTool("A−"',
 'Button larger=compactTool("A+"',
 'Button fit=compactTool("↔","Ajustar à largura"',
 'Button info=compactTool("ⓘ","Informações da interpretação MIDI"',
 'info.setContentDescription("Informações da interpretação MIDI; "+midiMessage())',
 'new AlertDialog.Builder(activity)',
 'return warnings.size()>0',
 'setMessage(midiMessage()+',
 'zoom=Math.max(MIN_ZOOM,Math.min(MAX_ZOOM,target))',
 'button("Salvar",true,this::saveCurrent)',
 'button("+ Biblioteca",true,',
 'button("✓ Biblioteca",false,',
 'headerActionSlot.addView(primary,new LayoutParams(-1,dp(48)))',
 'primary.setEnabled(false)',
 'activity.addCreationScoreToLibrary(project.id,ok->',
 'CreationStudyCompatibility.check(activity,target',
 'new CreationScoreStore(activity.getFilesDir()).save(',
 'CreationScoreGenerator.generate(',
 'isCurrentDerivedXml(prior.xml)',
 'void confirmClose(){window.dismiss();}',
 'setTooltipText("Voltar à composição")',
]
for expected in must:
 assert expected in s, "Missing contract: "+expected
for banned in ['scoreFooter','creation-score-fixed-footer','creation-score-bottom-save',
               'creation-score-bottom-back','actions(scoreFooter','heightPixels*0.65f',
               'addView(toolbarScroll,new LayoutParams(-1,dp(60)))']:
 assert banned not in s, "obsolete bar: "+banned
assert s.count('addView(header,new LayoutParams(-1,dp(56)))')==1
# No score reflow when opening info, and original store untouched.
info=s[s.index('private void showMidiInfo()'):s.index('private void renderScoreToolbar()')]
assert 'renderPreview()' not in info and 'saveCurrent()' not in info
print("ZAI014_UNIFIED_TOOLBAR_CONTRACT_PASS checks="+str(len(must)+7))

#!/usr/bin/env python3
"""Static tests for Study chrome compactness, no APK or emulator required."""
from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
SCREENS=(ROOT/"app/src/main/java/br/org/pianolivre/PianoScreens.java").read_text(encoding="utf-8")
MAIN=(ROOT/"app/src/main/java/br/org/pianolivre/MainActivity.java").read_text(encoding="utf-8")

def segment(start,end):
    a=SCREENS.index(" void "+start+"(){")
    b=SCREENS.index(" void "+end+"(){",a)
    return SCREENS[a:b]

class StudyChromeCompactContract(unittest.TestCase):
    def test_exact_three_strip_heights(self):
        self.assertIn("a.root.addView(header,lp(-1,44))",SCREENS)
        self.assertIn("studio.addView(studyControlsBar,lp(-1,40))",SCREENS)
        self.assertIn("studio.addView(bottom,lp(-1,44))",SCREENS)

    def test_no_font_reduction(self):
        header=segment("buildStudyHeader","loopMenu")
        studio=segment("buildStudio","revealStudyControls")
        self.assertIn('songTitle=text("Selecione uma partitura",compact?15:20',header)
        self.assertIn('a.info=text("",compact?15:11',header)
        self.assertIn("back.setTextSize(compact?22:13)",header)
        self.assertIn("options.setTextSize(20)",header)
        self.assertIn('TextView speed=text("VELOCIDADE",10',studio)
        self.assertIn('tempoLabel=text("100%",12',studio)
        # Existing buttons continue to use compact() = 11sp.
        self.assertIn("b.setTextSize(11)",SCREENS)

    def test_title_and_current_music_info_on_one_line(self):
        header=segment("buildStudyHeader","loopMenu")
        self.assertIn("songTitle.setSingleLine(true);songTitle.setMaxLines(1)",header)
        self.assertIn("a.info.setSingleLine(true);a.info.setMaxLines(1)",header)
        self.assertIn("header.addView(a.info,new LinearLayout.LayoutParams(0,-1,1.6f))",header)
        self.assertNotIn('compact?"\\n"+info',header)
        self.assertIn('String description=title+" · "+info',header)
        self.assertIn('a.info.setContentDescription("Andamento real, velocidade e posição: "+info)',header)
        self.assertIn('studyInfoText.update(',MAIN)

    def test_buttons_shrunk_but_actions_preserved(self):
        header=segment("buildStudyHeader","loopMenu")
        studio=segment("buildStudio","revealStudyControls")
        for marker in ('backFromStudy()', 'showFree()', 'a.selectMidi()', 'options()'):
            self.assertIn(marker,header)
        for marker in ('a.selectHand(idx)','a.toggle()', 'a.stopPlayback()',
                       'a.seekPrevious()', 'a.saveSession()', 'a.tempoBar.setOnSeekBarChangeListener',
                       'notation.setLoopEdit(editingLoop)'):
            self.assertIn(marker,studio)
        self.assertIn('new android.widget.HorizontalScrollView.LayoutParams(-2,d(40))',studio)
        self.assertIn('bottom.addView(playback,lp(compactStudy?76:92,40))',studio)
        self.assertIn('bottom.addView(loopButton,lp(compactStudy?72:80,40))',studio)
        self.assertIn('LinearLayout.LayoutParams(0,d(40),1)',studio)

    def test_study_top_bars_remain_visible_while_playing(self):
        chrome=(ROOT/"app/src/main/java/br/org/pianolivre/StudyChromeState.java").read_text(encoding="utf-8")
        self.assertIn("public boolean update(boolean transportActive){return true;}",chrome)
        self.assertIn("public boolean reveal(){return true;}",chrome)
        self.assertIn("public boolean visible(){return true;}",chrome)
        study=segment("applyStudyChrome","recordStudyLayout")
        self.assertIn("header.setVisibility(View.VISIBLE)",study)
        self.assertIn("studyControlsBar.setVisibility(View.VISIBLE)",study)
        self.assertNotIn("View.GONE",study)
        self.assertIn("recordStudyLayout()",study)
        self.assertIn("state.put(\"visible\",chrome.visible())",study)
        self.assertIn("if(notation!=null)notation.post(()->notation.resizeToFit())",study)

    def test_more_room_for_staves(self):
        self.assertEqual(54+46+54-(44+40+44),26)
        self.assertIn("studio.addView(notationArea,new LinearLayout.LayoutParams(-1,0,1))",SCREENS)
        self.assertIn('if(notation!=null)notation.post(()->notation.resizeToFit())',SCREENS)

if __name__=="__main__":
    unittest.main()

"use strict";
/** The new path is intentionally NOT the default Estudo renderer. */
const assert=require("node:assert/strict"),fs=require("node:fs");
const html=fs.readFileSync("app/src/main/assets/notation.html","utf8");
const bridge=fs.readFileSync("app/src/main/java/br/org/pianolivre/NotationView.java","utf8");
const views=fs.readFileSync("app/src/main/java/br/org/pianolivre/PianoScreens.java","utf8");
const time=fs.readFileSync("app/src/main/assets/temporal-notation.js","utf8");
let checks=0;function ok(cond,msg){checks++;assert.ok(cond,msg)}
ok(html.includes("studyProof=(layout==='engraving-poc')"),"Explicit research mode required");
ok(html.includes("renderSingleHorizontalStaffline:studyProof"),"OSMD horizontal only in PoC");
ok(html.includes("if(!freeLayout&&!studyProof){"),"Default study must still use TemporalNotation");
ok(html.includes("POC_OSMD_READY"),"SVG render metrics required");
ok(html.includes("POC_NOT_PRODUCTION_READY"),"Proof must never claim finished Study");
ok(bridge.includes("engravingProof=false"),"Normal score load disables proof");
ok(bridge.includes("if(freeLayout||disposed||!htmlReady||!scoreRendered)return;"),"Study native seek remains anchored to native clock");
ok(bridge.includes("LAYER_TYPE_SOFTWARE"),"Research layer diagnostic isolated");
ok(html.includes("opticalSeek(beat,bpm,!!playing)"),"OSMD pilot consumes beats, not MIDI output");
ok(html.includes('src="optical-time-map.js"'),"Optical map is a bundled offline asset");
ok(html.includes("opticalPosition"),"Reference-led optical movement required");
ok(html.includes("OpticalTimeMap.hitTest(optical.notes,x,y,hitRadiusX,hitRadiusY)"),"Preview touch consults musical noteheads");
ok(html.includes("POC_NOTE_TAP"),"Preview emits auditable notehead touch");
ok(html.includes("12*vb.width/rect.width")&&html.includes("10*vb.height/rect.height"),
  "Touch radius must remain usable after OSMD SVG scaling");
ok(html.includes("role=\"status\""),"Preview note feedback exposed accessibly");
ok(views.includes("setOnLongClickListener"),"Research activation deliberate");
ok(time.includes("function computeX(beat,pxPerBeat,origin){return origin+beat*pxPerBeat;}"),
   "Strict beat-proportional baseline untouched");
ok(time.includes("function cursorPx(width){return width*.4;}"),"Existing cursor placement untouched");
ok(bridge.includes("JSONArray serialized=new JSONArray()"),
   "Native Score events must be serialized only in engraving preview");
ok(bridge.includes('note.put("midi",event.midi)'),
   "Native MIDI authority travels alongside the graphical proof");
ok(html.includes("OpticalTimeMap.reconcile(optical.notes"),
   "Preview must explicitly cross-reference graphical noteheads with native events");
ok(html.includes("OpticalTimeMap.nextCueHeads(optical.notes,beat,opticalHand)")&&
 html.includes("overlay.setAttribute('pointer-events','none')"),
 "OSMD visual hand cues must not intercept individual notehead taps");
ok(bridge.includes("PianoNotation.studyHand(")&&
 views.includes("notation.setStudyHand(a.currentHand)"),
 "Native initial study-hand filter must reach OSMD without changing MIDI");
ok(html.includes("paintOpticalCues(beat)")&&
 html.includes("paintOpticalCues(seekBeat)"),
 "Preview cues update after native beat seek and SVG resize");
ok(!html.includes("overlay.replaceChildren()")&&
 html.includes("while(overlay.firstChild)overlay.removeChild(overlay.firstChild)"),
 "SVG cue replacement must work in the Android WebView's Chromium 74 baseline");
ok(html.includes("POC_MIDI_MAPPING"),
   "Record mapping completeness and ambiguity for field diagnostics");
ok(html.includes("MIDI não confirmado"),
   "Unmatched and polyphonic-unison heads must be disclosed, never treated as exact");
ok(bridge.includes("for(Score.Event event:graphicEvents)")&&
 bridge.includes("pendingScore.writtenEvents.isEmpty()")&&
 bridge.includes('note.put("playbackAttack",event.playbackAttack)'),
 "Optical mapping must use original MusicXML heads, not MIDI-merged tie attacks");
ok(html.includes("Início de trecho: escolha uma nota com ataque")&&
 html.includes("Ligadura: sem novo ataque MIDI"),
 "Tied continuation may be identified but cannot fabricate a new loop attack");
ok(bridge.includes('note.put("step",event.writtenStep)')&&
 bridge.includes('note.put("xmlNoteIndex",event.sourceNoteIndex)'),
 "Native bridge preserves written pitch and source note index");
ok(html.includes("OpticalTimeMap.writtenPitch(native)"),
 "Preview note tooltip requires exact written pitch, not MIDI chromatic guesses");
ok(!html.includes("const names=['Dó','Dó♯','Ré','Ré♯'"),
 "Chromatic guessed spelling must never be used in the OSMD preview");

ok(html.includes("attachOpticalNoteTouch(document.querySelector('#score svg'))"),
   "Re-rendered SVG must have an active head hit-target after resize");
ok(html.includes("POC_LOOP_SELECTED"),"Proof must log graphical loop start/end");
ok(html.includes("AndroidNotation.onLoopSelected(beginning,finish)"),
   "Native musical controller receives experimental graphical loop bounds");
ok(html.includes("if(studyProof){\n     opticalLoopEditing=!!enabled"),
   "Opt-in proof must handle loop selection without invoking old renderer");
ok(bridge.includes("if(!disposed&&!freeLayout&&htmlReady&&scoreRendered)"),
   "Native loop callback remains guarded by native study lifecycle");
ok(html.includes("POC_WAIT_PREPARE"),"Native Wait preparation must be logged in proof");
ok(html.includes("const preparationDistance=Math.min(100,viewport.clientWidth*.18)"),
 "Preview Wait animates a bounded graphical pre-roll");
ok(html.includes("score.style.transform='translate3d('"),
 "Score moves gradually without changing the musical clock");
ok(html.includes("document.getElementById('score').style.transform='none'"),
 "Wait cancellation or completion restores exact score geometry");
ok(html.includes("textContent='Preparação'")&&
 !html.includes("Math.ceil(remaining/1000)+' s'"),
 "Wait preparation must not display a numerical countdown");
ok(html.includes("if(studyProof){beginOpticalWait(milliseconds);return;}"),
   "Only experimental OSMD preview uses optical Wait preparation");
ok(html.includes("if(studyProof){cancelOpticalWait();return;}"),
   "Native stop cancels OSMD countdown");
ok(bridge.includes("if(disposed||freeLayout||!htmlReady||!scoreRendered||pendingWaitDeadline<=0)return;"),
   "Wait preparation can reach preview through native bridge");
ok(html.includes("POC_RESIZE_HEIGHT_ONLY"),"Height-only guide resize must avoid full OSMD render");
ok(html.includes("osmdRenderedWidth===viewport.clientWidth"),"Width changes must still trigger OSMD re-layout");
ok(html.includes("osmdRenderedHeight=viewport.clientHeight"),"Track rendered viewport height for guide toggles");
ok(html.includes("function fitOpticalSvg(svg)"),"Two-staff SVG must fit vertical space");
ok(html.includes("optical.timeline.xAt(beat)*opticalDisplayScale"),"Musical x movement must honor optical display zoom");
ok(html.includes("svg.dataset.opticalNativeHeight"),"Height resize must retain original OSMD drawing metrics");
ok(!html.includes("svg.getBBox()"),"Failed crop experiment is not left in the OSMD rendering path");
ok(html.includes("displayScale:opticalDisplayScale"),"Piano score proof reports actual visual scaling");
ok(html.includes("Math.abs(viewport.scrollLeft-target)>=0.5"),"WebView skips redundant subpixel scroll updates");
console.log("OSMD_POC_CONTRACT_PASS checks="+checks);

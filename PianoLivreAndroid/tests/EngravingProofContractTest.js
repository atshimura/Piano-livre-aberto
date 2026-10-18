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
ok(html.includes("OpticalTimeMap.hitTest(optical.notes,x,y)"),"Preview touch consults musical noteheads");
ok(html.includes("POC_NOTE_TAP"),"Preview emits auditable notehead touch");
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
ok(html.includes("POC_MIDI_MAPPING"),
   "Record mapping completeness and ambiguity for field diagnostics");
ok(html.includes("MIDI não confirmado"),
   "Unmatched and polyphonic-unison heads must be disclosed, never treated as exact");
ok(html.includes("attachOpticalNoteTouch(document.querySelector('#score svg'))"),
   "Re-rendered SVG must have an active head hit-target after resize");
ok(html.includes("POC_LOOP_SELECTED"),"Proof must log graphical loop start/end");
ok(html.includes("AndroidNotation.onLoopSelected(beginning,finish)"),
   "Native musical controller receives experimental graphical loop bounds");
ok(html.includes("if(studyProof){\n     opticalLoopEditing=!!enabled"),
   "Opt-in proof must handle loop selection without invoking old renderer");
ok(bridge.includes("if(!disposed&&!freeLayout&&htmlReady&&scoreRendered)"),
   "Native loop callback remains guarded by native study lifecycle");
console.log("OSMD_POC_CONTRACT_PASS checks="+checks);

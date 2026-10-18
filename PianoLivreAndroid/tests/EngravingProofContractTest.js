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
ok(bridge.includes("freeLayout||engravingProof||disposed"),"Playback cannot drive unvalidated proof");
ok(views.includes("setOnLongClickListener"),"Research activation deliberate");
ok(time.includes("function computeX(beat,pxPerBeat,origin){return origin+beat*pxPerBeat;}"),
   "Strict beat-proportional baseline untouched");
ok(time.includes("function cursorPx(width){return width*.4;}"),"Existing cursor placement untouched");
console.log("OSMD_POC_CONTRACT_PASS checks="+checks);

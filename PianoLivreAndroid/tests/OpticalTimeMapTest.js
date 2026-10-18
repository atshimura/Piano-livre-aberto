"use strict";
const assert=require("node:assert/strict"),T=require("../app/src/main/assets/optical-time-map.js");
let checks=0;function ok(v,msg){checks++;assert.ok(v,msg);}
const coords=[{beat:0,x:100},{beat:1,x:150},{beat:2,x:240},{beat:3,x:280},
              {beat:2,x:240.5},{beat:0,x:101}]; // two staves; optical spacing varies
const t=T.create(coords);
ok(t.stops.length===4,"Simultaneous notes must share a single moment");
ok(Math.abs(t.xAt(.5)-125.5)<.001,"Linear interpolation on first optical span");
ok(Math.abs(t.xAt(1.5)-195.25)<.001,"Nonlinear x(beat) must respect graphic gap");
ok(t.xAt(0)===101&&t.xAt(-1)===101,"Clamp start");
ok(t.xAt(100)===280,"Clamp end");
ok(Math.abs(t.beatAt(t.xAt(1.7))-1.7)<.00001,"Every interpolated beat round-trips");
ok(t.xAt(2)===240.5&&t.stops[2].voices===2,"Multiple voices at one beat");
ok(t.xAt(1)-t.xAt(0)!==t.xAt(2)-t.xAt(1),"Optical spacing is NOT linear in beat");
for(const [i,values] of [["empty",[]],["invalid",[{beat:1,x:NaN}]],["backward",[{beat:0,x:10},{beat:1,x:5}]]]){
 try{T.create(values);throw Error("Should reject "+i);}catch(e){ok(e.message!=="Should reject "+i,i);}
}
function entry(time,x,notes,staff=1){
 const ns=(notes||[]).map(m=>({sourceNote:{halfTone:m-12,isRestFlag:false,IsGraceNote:false},
  PositionAndShape:{AbsolutePosition:{x:x,y:staff*2}}}));
 return {getAbsoluteTimestamp:()=>({RealValue:time/4}),PositionAndShape:{AbsolutePosition:{x:x,y:0}},
  graphicalVoiceEntries:[{notes:ns}]};
}
const graphic={measureList:[[
 {staffEntries:[entry(0,10,[60]),entry(1,14,[62])]},
 {staffEntries:[entry(0,10.1,[48],2),entry(1,14.1,[50],2)]}
],[
 {staffEntries:[entry(2,18,[64,67]),entry(3,24,[])]},
 {staffEntries:[entry(2,18.1,[52],2)]}
]]};
const mapped=T.fromGraphical(graphic);
const voicedGraph={measureList:[[{staffEntries:[{
 getAbsoluteTimestamp:()=>({RealValue:0}),
 PositionAndShape:{AbsolutePosition:{x:10,y:0}},
 graphicalVoiceEntries:[{parentVoiceEntry:{ParentVoice:{VoiceId:2}},
  notes:[{sourceNote:{halfTone:48,isRestFlag:false,IsGraceNote:false},
   PositionAndShape:{AbsolutePosition:{x:10,y:4}}}]}]
}]}]]};
ok(T.fromGraphical(voicedGraph).notes[0].voiceId==="2",
 "OSMD graphical entry preserves explicit original voice ID");
ok(mapped.timeline.stops.length===4,"Measure and rest positions are in map");
ok(mapped.notes.length===7,"Chord and concurrent staves retained");
ok(mapped.notes.filter(x=>x.beat===2).length===3,"All chord attacks at one musical instant");
ok(mapped.notes[0].midiEstimate===60&&mapped.notes[2].midiEstimate===48,"MIDI translation recorded as estimate");
ok(mapped.timeline.xAt(3)===240,"Last rest anchors the timeline");
try{T.fromGraphical({});throw Error("Should reject");}catch(e){ok(e.message!=="Should reject","No graphic");}
const hitNote=mapped.notes.find(n=>n.staff===1&&n.beat===0);
ok(!!hitNote,"First graphical head exists");
ok(T.hitTest(mapped.notes,hitNote.x,hitNote.y)?.midiEstimate===60,"Click near graphical head identifies exact note");
ok(T.hitTest(mapped.notes,hitNote.x+5,hitNote.y+3)?.midiEstimate===60,"Allow small touch imprecision");
ok(T.hitTest(mapped.notes,hitNote.x,hitNote.y+90)===null,"Reject touches away from both staves");
ok(T.hitTest(mapped.notes,NaN,hitNote.y)===null,"Reject invalid gesture");
ok(T.hitTest(mapped.notes,hitNote.x,hitNote.y,0,7)===null,"Reject invalid hit area");
ok(T.hitTest(mapped.notes,mapped.notes[2].x,mapped.notes[2].y)?.staff===2,"Both staves independently accessible");
const overlappingHeads=[{x:50,y:30,midiEstimate:60},{x:50,y:30,midiEstimate:61}];
ok(T.hitTest(overlappingHeads,50,30)===null,
 "Identical graphical heads must not be given a fabricated identity");
ok(T.hitTest([{x:48,y:30},{x:52,y:30}],50,30)===null,
 "Exact midpoint between two musical heads must be treated as ambiguous");
ok(T.hitTest([{x:48,y:30},{x:52,y:30}],47,30)?.x===48,
 "Nearest unique head remains selectable");

// Conservative cross-parser mapping: never guess a voice for an unison.
const native=mapped.notes.map((n,i)=>({beat:n.beat,staff:n.staff,
 measure:n.measure,midi:n.midiEstimate,length:1,voice:String(i+1)}));
let report=T.reconcile(mapped.notes,native);
ok(report.matched===7&&report.unmatched===0&&report.ambiguous===0,"Unique original events reconcile");
ok(mapped.notes[0].nativeEvent.midi===60&&mapped.notes[0].mappingStatus==="matched",
 "Actual native MIDI overrides the estimate only on exact agreement");
ok(mapped.notes.find(n=>n.staff===2).nativeEvent.staff===2,"Native staff identity is retained");
ok(report.unmatchedNative===0&&report.invalidNative===0,"Every unique native event accounted for");
const solo=[{beat:0,staff:1,measure:1,midiEstimate:60,x:100,y:100}];
const exact=[{beat:0,staff:1,measure:1,midi:60,length:1,voice:"1"}];
report=T.reconcile(solo,exact);
ok(report.matched===1&&solo[0].nativeEvent.index===0,"Matching source event index");
report=T.reconcile(solo,[{...exact[0],midi:61}]);
ok(report.unmatched===1&&solo[0].nativeEvent===undefined,"Never retain a stale matched MIDI");
report=T.reconcile(solo,[{...exact[0],staff:2}]);
ok(report.unmatched===1,"Same pitch from other hand must not match");
report=T.reconcile(solo,[{...exact[0],beat:1}]);
ok(report.unmatched===1,"Different onset must not match");
report=T.reconcile(solo,[{...exact[0],measure:2}]);
ok(report.unmatched===1,"Measure identity is required");
report=T.reconcile(solo,[exact[0],{...exact[0],voice:"2"}]);
ok(report.ambiguous===1&&solo[0].nativeEvent===undefined,
 "Two native voices sharing unison cannot be arbitrarily assigned");
// Same-pitch independent voices can be reconciled ONLY by a proven
// one-to-one source voice identity; graphical position must not imply hand.
const independentVoices=[{...solo[0],voiceId:"1"},{...solo[0],voiceId:"2",x:112,y:104}];
report=T.reconcile(independentVoices,[{...exact[0],voice:"1",step:"C",alter:0,octave:4},
 {...exact[0],voice:"2",step:"C",alter:0,octave:4}]);
ok(report.matched===2&&report.ambiguous===0&&
 independentVoices[0].nativeEvent.voice==="1"&&
 independentVoices[1].nativeEvent.voice==="2",
 "Exact bijective MusicXML voice IDs safely distinguish independent unisons");
const mismatchedVoices=[{...solo[0],voiceId:"1"},{...solo[0],voiceId:"3",x:112}];
report=T.reconcile(mismatchedVoices,[{...exact[0],voice:"1"},{...exact[0],voice:"2"}]);
ok(report.ambiguous===2&&report.matched===0,
 "Different source voice IDs must remain ambiguous");
const sameVoiceTwice=[{...solo[0],voiceId:"1"},{...solo[0],voiceId:"1",x:112}];
report=T.reconcile(sameVoiceTwice,[{...exact[0],voice:"1"},{...exact[0],voice:"2"}]);
ok(report.ambiguous===2&&report.matched===0,
 "Repeated graphical voice ID cannot fabricate a bijection");
const reducedUnison=[{...solo[0],voiceId:"1"}];
report=T.reconcile(reducedUnison,[{...exact[0],voice:"1"},{...exact[0],voice:"2"}]);
ok(report.ambiguous===1&&report.matched===0,
 "One engraved head for two voices must stay unresolved");
const unison=[{...solo[0]},{...solo[0],x:120}];
report=T.reconcile(unison,exact);
ok(report.ambiguous===2&&unison.every(n=>n.nativeEvent===undefined),
 "Duplicate graphical unisons remain ambiguous");
report=T.reconcile(solo,[{...exact[0],length:-1},exact[0]]);
ok(report.matched===1&&report.invalidNative===1,"Invalid native events are excluded");
report=T.reconcile(solo,[{...exact[0],beat:0.00049}]);
ok(report.matched===1,"Parser floating point drift within half-millisecond beat accepted");
report=T.reconcile(solo,[{...exact[0],beat:0.0006}]);
ok(report.unmatched===1,"Mismatch beyond quantization bound is not silently matched");
try{T.reconcile([],null);throw Error("Should reject missing native events");}
catch(e){ok(e.message!=="Should reject missing native events","Null native input rejected");}
// Diagnostics distinguish causes so missing musical events are not hidden by a pass rate.
report=T.reconcile(solo,[{...exact[0],midi:61}]);
ok(report.reasons.noNativeCandidate===1&&report.samples[0].reason==="noNativeCandidate",
 "Pitch discrepancies are reported without guessing a source event");
report=T.reconcile(solo,[exact[0],{...exact[0],voice:"2"}]);
ok(report.reasons.ambiguous===1&&solo[0].mappingReason==="ambiguous",
 "Native unisons must be explicitly diagnosed");
const invalidGraphic=[{beat:0,staff:1,measure:1,midiEstimate:null,x:1,y:2}];
report=T.reconcile(invalidGraphic,exact);
ok(report.reasons.invalidGraphical===1&&report.unmatched===1,
 "Unresolved graphical pitch is diagnosed without a false match");
const manyUnknown=Array.from({length:40},(_,i)=>({beat:i,staff:1,measure:i+1,midiEstimate:70}));
report=T.reconcile(manyUnknown,[]);
ok(report.reasons.noNativeCandidate===40&&report.samples.length===20,
 "Diagnostic samples remain bounded for a large score");
const repeatedGraph=[{...solo[0]},{...solo[0],x:150}];
report=T.reconcile(repeatedGraph,[]);
ok(report.ambiguous===2&&report.reasons.ambiguous===2&&
 report.reasons.noNativeCandidate===0&&
 report.reasons.invalidGraphical===0,
 "A single head cannot be counted under two diagnostic causes");
ok(Object.values(report.reasons).reduce((a,b)=>a+b,0)===report.unmatched+report.ambiguous,
 "All unresolved heads must have exactly one diagnostic cause");
report=T.reconcile(solo,exact);
ok(solo[0].mappingReason===undefined&&report.matched===1,
 "Successful reconciliation must clear stale diagnostic reasons");
// MusicXML retains written spelling even when MIDI semitones agree.
ok(T.writtenPitch({step:"C",alter:1,octave:4})==="Dó♯4",
 "C-sharp must retain its source spelling");
ok(T.writtenPitch({step:"D",alter:-1,octave:4})==="Ré♭4",
 "Enharmonic D-flat must not become C-sharp");
ok(T.writtenPitch({step:"B",alter:1,octave:3})==="Si♯3"&&
 T.writtenPitch({step:"C",alter:-1,octave:4})==="Dó♭4",
 "Naturally adjacent notes must preserve written accidentals");
ok(T.writtenPitch({step:"F",alter:2,octave:5})==="Fá𝄪5"&&
 T.writtenPitch({step:"B",alter:-2,octave:2})==="Si𝄫2",
 "Double accidentals must remain notated");
ok(T.writtenPitch({step:"G",alter:0,octave:4})==="Sol4",
 "Unaltered note keeps exact octave");
ok(T.writtenPitch({step:"H",alter:0,octave:4})===null&&
 T.writtenPitch({step:"C",alter:.5,octave:4})===null&&
 T.writtenPitch({step:"D",alter:1,octave:NaN})===null,
 "Unsupported spelling must not be guessed");
const written=[{...solo[0],midiEstimate:61}];
report=T.reconcile(written,[{...exact[0],midi:61,step:"D",alter:-1,octave:4,xmlNoteIndex:17}]);
ok(report.matched===1&&T.writtenPitch(written[0].nativeEvent)==="Ré♭4"&&
 written[0].nativeEvent.xmlNoteIndex===17,
 "Reconciled head keeps source-note identity and enharmonic spelling");
// A long score must not require a quadratic lookup for musical scrolling.
// This probes only the time/graphics map, NOT Android WebView SVG rendering.
const longEntries=[];
for(let i=0;i<12000;i++)longEntries.push({
 beat:i*.25,x:90+i*2.8+(i%7)*.03
});
const mapStart=performance.now();
const longMap=T.create(longEntries);
ok(longMap.stops.length===longEntries.length,"Long score retains every anchor");
ok(longMap.range.to===2999.75&&longMap.range.right>longMap.range.left,
   "Long score range remains finite and monotonic");
for(let i=0;i<12000;i+=137){
 const beat=i*.25;
 ok(Math.abs(longMap.beatAt(longMap.xAt(beat))-beat)<.00001,
    "Long-score x/beat roundtrip at index "+i);
}
const mappingMs=+(performance.now()-mapStart).toFixed(1);
ok(mappingMs<5000,"12,000 musical anchors must not use quadratic rendering calculations");
console.log("OPTICAL_LONG_MAP_PASS anchors="+longMap.stops.length+
 " mapAndLookupMs="+mappingMs+
 " heapUsedBytes="+process.memoryUsage().heapUsed);
console.log("OPTICAL_TIME_MAP_PASS checks="+checks);

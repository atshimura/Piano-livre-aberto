"use strict";
const assert=require("node:assert/strict"),fs=require("node:fs");
const OpticalTimeMap=require("../app/src/main/assets/optical-time-map.js");
const xml=fs.readFileSync("app/src/debug/assets/scores/engraving-polyphony-synthetic.musicxml","utf8");
// Small independent reader for the deliberately restricted authored test XML.
// Neither OSMD nor Score.java nor OpticalTimeMap supplies this oracle.
function tag(x,k){let m=x.match(new RegExp("<"+k+">([^<]*)</"+k+">"));return m?m[1]:null;}
const semitone={C:0,D:2,E:4,F:5,G:7,A:9,B:11};
const expected=[];
for(const [i,measure] of [...xml.matchAll(/<measure\b[^>]*>([\s\S]*?)<\/measure>/g)].entries()){
 let cursor=0,last=0;
 for(const found of measure[1].matchAll(/<(note|backup)>[\s\S]*?<\/\1>/g)){
  const node=found[0],duration=Number(tag(node,"duration"));
  if(found[1]==="backup"){cursor-=duration;continue;}
  const chord=/<chord\s*\/>/.test(node),rest=/<rest\s*\/>/.test(node);
  const beat=i*4+(chord?last:cursor)/4;
  if(!chord)last=cursor;
  if(!rest){const pitch=node.match(/<pitch>([\s\S]*?)<\/pitch>/)[1];
   const midi=12*(Number(tag(pitch,"octave"))+1)+semitone[tag(pitch,"step")];
   expected.push({beat,staff:Number(tag(node,"staff")),measure:i+1,
    midi,voice:tag(node,"voice"),length:duration/4,
    playbackAttack:!/<tie type="stop"\s*\/>/.test(node),
    step:tag(pitch,"step"),octave:Number(tag(pitch,"octave")),alter:0});
  }
  if(!chord)cursor+=duration;
 }
}
assert.equal(expected.length,30,"Independent MusicXML fixture has 30 written heads");
assert.equal(expected.filter(e=>e.playbackAttack).length,29,"Only one tied continuation has no attack");
assert.equal(expected.filter(e=>e.beat===0&&e.staff===1&&e.voice==="1").length,3,"Triad oracle");
assert.equal(expected.filter(e=>e.beat===4&&e.staff===1&&e.midi===67).length,2,"Unison oracle");
const graph={measureList:Array.from({length:4},()=>[null,null])};
let positions=[];
for(let mi=1;mi<=4;mi++)for(let staff=1;staff<=2;staff++){
 const group=expected.filter(e=>e.measure===mi&&e.staff===staff);
 const entries=[];
 for(const beat of [...new Set(group.map(e=>e.beat))]){
  const byVoice=new Map();
  for(const e of group.filter(n=>n.beat===beat)){
   if(!byVoice.has(e.voice))byVoice.set(e.voice,[]);
   const ordinal=byVoice.get(e.voice).length;
   byVoice.get(e.voice).push({sourceNote:{halfTone:e.midi-12,isRestFlag:false,IsGraceNote:false},
    PositionAndShape:{AbsolutePosition:{x:8+beat*4+ordinal*0.5,y:(110-e.midi)*0.5+staff*40}}});
  }
  entries.push({getAbsoluteTimestamp:()=>({RealValue:beat/4}),
   PositionAndShape:{AbsolutePosition:{x:8+beat*4,y:0}},
   graphicalVoiceEntries:[...byVoice].map(([voice,notes])=>({
    parentVoiceEntry:{ParentVoice:{VoiceId:Number(voice)}},notes}))});
 }
 graph.measureList[mi-1][staff-1]={staffEntries:entries};
}
const mapped=OpticalTimeMap.fromGraphical(graph);
assert.equal(mapped.notes.length,30,"Mock OSMD extracts every head");
const native=expected.map((e,index)=>({...e,xmlNoteIndex:index,sourcePart:0}));
let report=OpticalTimeMap.reconcile(mapped.notes,native);
assert.deepEqual([report.matched,report.ambiguous,report.unmatched,report.unmatchedNative],[30,0,0,0],
 "All independently counted notes must reconcile");
for(const n of mapped.notes){
 const e=n.nativeEvent;
 assert.equal(n.midiEstimate,e.midi);assert.equal(n.beat,e.beat);
 assert.equal(n.staff,e.staff);assert.equal(n.measure,e.measure);
 assert.equal(n.voiceId,e.voice);assert.equal(e.sourcePart,0);
}
const triad=OpticalTimeMap.nextCueHeads(mapped.notes,0,1);
assert.equal(triad.filter(n=>n.nativeEvent.voice==="1").length,3,
 "Highlight all three heads of an upper-staff chord");
assert.equal(OpticalTimeMap.nextCueHeads(mapped.notes,0,0).length,7,
 "Highlight both simultaneous staves and the sustained upper second voice");
assert.equal(OpticalTimeMap.nextCueHeads(mapped.notes,0,2).length,3,
 "Highlight lower staff without inferring hand by pitch");
const upper=triad.filter(n=>n.nativeEvent.voice==="1");
assert.equal(OpticalTimeMap.hitTest(mapped.notes,upper[0].x,upper[0].y)?.nativeEvent.midi,72);
assert.equal(OpticalTimeMap.hitTest(mapped.notes,upper[1].x,upper[1].y)?.nativeEvent.midi,74);
const lower=mapped.notes.find(n=>n.staff===2);
assert.equal(OpticalTimeMap.hitTest(mapped.notes,lower.x,lower.y)?.staff,2);
const tied=mapped.notes.find(n=>n.nativeEvent.playbackAttack===false);
assert(tied && !OpticalTimeMap.nextCueHeads(mapped.notes,12,1).includes(tied),
 "Tied continuation never cues an attack");
const missingOneUnison=mapped.notes.filter((n)=>!(n.beat===4&&n.staff===1&&n.midiEstimate===67&&n.voiceId==="2"))
 .map(n=>({...n}));
report=OpticalTimeMap.reconcile(missingOneUnison,native);
assert.equal(report.matched,28);assert.equal(report.ambiguous,1);
assert.equal(report.unmatchedNative,2);
assert(!missingOneUnison.find(n=>n.beat===4&&n.staff===1&&n.midiEstimate===67).nativeEvent,
 "Engraved collapsed unison is unresolved, never arbitrarily assigned");
const withoutVoices=mapped.notes.map(n=>({...n,voiceId:null}));
report=OpticalTimeMap.reconcile(withoutVoices,native);
assert.equal(report.matched,28);assert.equal(report.ambiguous,2,
 "Two unlabeled unison heads remain ambiguous");
assert.equal(OpticalTimeMap.hitTest([{x:1,y:2},{x:1,y:2}],1,2),null,
 "Coincident heads cannot be attributed from a tap");
console.log("POLYPHONY_FIDELITY_JS_PASS source=30 graphical=30 native=30 attacks=29 ambiguousCollapsed=1");

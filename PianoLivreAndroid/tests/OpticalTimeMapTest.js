"use strict";
const assert=require("node:assert/strict"),T=require("../app/src/main/assets/optical-time-map.js");
let checks=0;function ok(v,msg){checks++;assert.ok(v,msg);}
const coords=[{beat:0,x:100},{beat:1,x:150},{beat:2,x:240},{beat:3,x:280},
              {beat:2,x:240.5},{beat:0,x:101}]; // two staves; optical spacing varies
const t=T.create(coords);
ok(t.stops.length===4,"Simultaneous notes must share a single moment");
ok(Math.abs(t.xAt(.5)-125.5)<.001,"Linear interpolation on first optical span");
ok(Math.abs(t.xAt(1.5)-195.5)<.001,"Nonlinear x(beat) must respect graphic gap");
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
ok(mapped.timeline.stops.length===4,"Measure and rest positions are in map");
ok(mapped.notes.length===7,"Chord and concurrent staves retained");
ok(mapped.notes.filter(x=>x.beat===2).length===3,"All chord attacks at one musical instant");
ok(mapped.notes[0].midiEstimate===60&&mapped.notes[2].midiEstimate===48,"MIDI translation recorded as estimate");
ok(mapped.timeline.xAt(3)===240,"Last rest anchors the timeline");
try{T.fromGraphical({});throw Error("Should reject");}catch(e){ok(e.message!=="Should reject","No graphic");}
console.log("OPTICAL_TIME_MAP_PASS checks="+checks);

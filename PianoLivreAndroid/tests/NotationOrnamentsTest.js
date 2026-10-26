"use strict";
const assert=require("assert");
const T=require("../app/src/main/assets/temporal-notation.js")._test;
let checks=0;
function ok(v,m){checks++;assert(v,m);}
function eq(a,b,m){ok(Math.abs(a-b)<1e-7,m+": "+a+" != "+b);}
class Node{
 constructor(tag){this.tag=tag;this.attrs={};this.children=[];this.textContent="";}
 setAttribute(k,v){this.attrs[k]=String(v);}
 appendChild(x){this.children.push(x);return x;}
}
global.document={createElementNS:(ns,tag)=>new Node(tag)};
function all(node,match){
 let list=[];for(const n of node.children){
  if(match(n))list.push(n);
  list.push(...all(n,match));
 }return list;
}
function note(at,voice,step="F",options={}){
 return Object.assign({at,measure:0,staff:1,voice:String(voice),step,oct:4,alter:0,
  grace:false,graceSlash:false,xmlOrder:0,beam:[],ties:[],slur:[],
  art:[],stem:"",type:"quarter",dot:0,acc:"",clef:"G"},options);
}
// Two grace notes immediately before the same C4 note. No duration is
// assigned and their heads should not overlap the actual notehead.
const ornament1=note(4,1,"B",{grace:true,graceSlash:true,xmlOrder:1,type:"eighth"}),
 ornament2=note(4,1,"D",{grace:true,graceSlash:false,xmlOrder:2,type:"16th"}),
 main=note(4,1,"E",{xmlOrder:3,type:"quarter"});
T.arrangeVoices([ornament1,ornament2,main],[]);
eq(ornament1._graceShift,-42,"first grace precedes the second");
eq(ornament2._graceShift,-21,"second grace precedes main note");
eq(main._graceShift,0,"principal note maintains original position");
ok([ornament1,ornament2,main].every(n=>n.at===4),"all grace events keep same performed score beat");
for(const n of [ornament1,ornament2,main]){
 const holder=new Node("svg");
 T.drawNote(holder,n,140);
 const g=holder.children[0];
 if(n.grace){
  ok(g.attrs.class==="temporal-grace-note","grace must not count as a playing note");
  ok(g.attrs.transform.includes("scale(.67)"),"grace must use small cue-size engraving");
 }else ok(g.attrs.class==="musical-event","main note stays playable and highlightable");
 if(n.graceSlash)
  ok(all(g,x=>x.tag==="line"&&x.attrs["stroke-width"]==="2.1").length===1,
     "acciaccatura receives slash through the stem");
 else
  ok(all(g,x=>x.tag==="line"&&x.attrs["stroke-width"]==="2.1").length===0,
     "unslashed grace/main note must have no grace slash");
}
const natural=note(1,1,"F",{acc:"natural",alter:0}),
 sharp=note(1,2,"G",{acc:"sharp",alter:1}),
 flat=note(1,3,"A",{acc:"flat",alter:-1}),
 far=note(1,4,"C",{acc:"natural",alter:0,oct:6});
T.arrangeVoices([natural,sharp,flat,far],[]);
ok(natural._accVisible&&sharp._accVisible&&flat._accVisible&&far._accVisible,
 "different spellings at one beat should print each sign");
const baseAccPos=n=>140+(n._headShift||0)+n._accOffset;
const positions=[natural,sharp,flat,far].map(n=>({x:baseAccPos(n),y:T.pitchY(n)}));
for(let i=0;i<positions.length;i++)for(let j=i+1;j<positions.length;j++){
 if(Math.abs(positions[i].y-positions[j].y)<26)
  ok(Math.abs(positions[i].x-positions[j].x)>=19,
     "vertically colliding accidentals must have different columns");
}
const duplicateA=note(2,1,"B",{acc:"flat",alter:-1}),
 duplicateB=note(2,2,"B",{acc:"flat",alter:-1});
T.arrangeVoices([duplicateA,duplicateB],[]);
ok(duplicateA._accVisible&&!duplicateB._accVisible,
 "identical accidental duplicated across voices should be shown only once");
const signs=new Node("svg");
T.drawNote(signs,duplicateA,200);
T.drawNote(signs,duplicateB,200);
ok(all(signs,n=>n.tag==="text").length===1,"drawn duplicate accidentals must not overlap");
const conflicting=note(3,1,"F",{acc:"natural",alter:0}),
 conflicting2=note(3,2,"F",{acc:"sharp",alter:1});
T.arrangeVoices([conflicting,conflicting2],[]);
ok(conflicting._accVisible&&conflicting2._accVisible,
 "different pitch alterations in same vertical position must both be indicated");
ok(conflicting._accOffset!==conflicting2._accOffset-(conflicting._headShift-conflicting2._headShift),
 "different accidentals must not share identical engraving positions");
const beamGrace=note(5,1,"C",{grace:true,beam:[{level:1,v:"begin"}]}),
 beamMain1=note(5,1,"E",{beam:[{level:1,v:"begin"}]}),
 beamMain2=note(6,1,"F",{beam:[{level:1,v:"end"}]});
T.arrangeVoices([beamGrace,beamMain1,beamMain2],[]);
const beams=T.collectBeamGroups([beamGrace,beamMain1,beamMain2],t=>t*100);
ok(beams.filter(g=>g.level===1).length===1,
 "grace ornaments must not accidentally join the timed performance beams");
ok(beams[0].notes.every(n=>!n.grace),"global beam grouping must exclude ornament notes");
// Isolated grace notes require their own flag(s); an accidental cannot
// make an eighth-note grace look like a quarter note.
for(const [type,flags] of [["eighth",1],["16th",2]]){
 const single=note(8,1,"A",{grace:true,type,xmlOrder:1});
 T.arrangeVoices([single],[]);
 const holder=new Node("svg");
 T.drawNote(holder,single,180);
 ok(all(holder,n=>n.tag==="path").length===flags,
    type+" grace must display the correct number of flags");
}
// Three grace notes at ONE musical beat: both beam levels cross the
// boundary of two SVG tiles without shifting the actual MIDI beat.
const graceBeamed=[
 note(10.95,1,"D",{grace:true,xmlOrder:1,type:"16th",beam:[
   {level:1,v:"begin"},{level:2,v:"begin"}]}),
 note(10.95,1,"E",{grace:true,xmlOrder:2,type:"16th",beam:[
   {level:1,v:"continue"},{level:2,v:"continue"}]}),
 note(10.95,1,"F",{grace:true,xmlOrder:3,type:"16th",beam:[
   {level:1,v:"end"},{level:2,v:"end"}]})
];
T.arrangeVoices(graceBeamed,[]);
const gx=beat=>100+100*beat;
const graceRuns=T.collectBeamGroups(graceBeamed,gx);
ok(graceRuns.length===2&&graceRuns.every(g=>g._grace),
   "grace beams must form separate levels without joining timed music");
ok(graceBeamed.every(n=>n.at===10.95&&n._graceBeamed&&Number.isFinite(n._beamTip)),
   "grace beaming must preserve beat and attach visible stems");
const gracePrimary=graceRuns.find(g=>g.level===1);
for(const n of graceBeamed){
 const stemX=gx(n.at)+n._graceShift+7.5*.67;
 const actual=.67*n._beamTip+.33*T.pitchY(n);
 const expected=gracePrimary._y0+gracePrimary._slope*(stemX-gracePrimary._x0);
 eq(actual,expected,"scaled grace stem must touch its beam");
 const holder=new Node("svg");
 T.drawNote(holder,n,gx(n.at));
 ok(all(holder,p=>p.tag==="path").length===0,
   "a grace note with an attached beam must not also print standalone flags");
}
const graceTileA=new Node("svg"),graceTileB=new Node("svg");
ok(T.drawScoreBeams(graceTileA,graceRuns,0,1160)===2,
   "both grace beam levels must be visible to the left of a tile boundary");
ok(T.drawScoreBeams(graceTileB,graceRuns,1160,1160)===2,
   "both grace beam levels must survive clipping in the next tile");
console.log("PASS NotationOrnamentsTest "+checks+
 " assertions: grace placement/slash, polyphonic accidental columns, duplicate signs, no MIDI timeline changes");

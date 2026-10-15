"use strict";
const assert=require("assert");
const T=require("../app/src/main/assets/temporal-notation.js")._test;
let checks=0;
function ok(v,s){checks++;assert(v,s);}
function close(a,b,s){ok(Math.abs(a-b)<1e-8,s+": "+a+" !== "+b);}
class Node {
 constructor(tag){this.tag=tag;this.children=[];this.attrs={};this.textContent="";}
 setAttribute(k,v){this.attrs[k]=String(v);}
 appendChild(n){this.children.push(n);return n;}
}
global.document={createElementNS:(ns,tag)=>new Node(tag)};
function descendants(node,test){
 let results=[];
 for(const child of node.children){
  if(test(child))results.push(child);
  results.push(...descendants(child,test));
 }
 return results;
}
function n(at,voice,step="C",opts={}){
 return Object.assign({at,measure:0,staff:1,voice:String(voice),
  step,oct:5,alter:0,clef:"G",beam:[],ties:[],slur:[],art:[],dot:0,acc:"",stem:"",
  tupletActual:0,tupletStart:false,tupletStop:false,tupletMarks:[]},opts);
}
// 4 simultaneous voices: no musical position may be changed by engraving.
const chord=[n(0,1),n(0,2),n(0,3),n(0,4)];
const rests=[1,2,3,4].map(v=>n(1,v,"C",{rest:true}));
T.arrangeVoices(chord,rests);
ok(chord.map(x=>x._visualStem).join(",")==="up,down,up,down",
 "polyphony must alternate independent voice stems");
ok(chord.map(x=>x._headShift).join(",")==="0,12,24,36",
 "all four simultaneous unison heads should be distinguishable");
ok(rests.map(x=>x._restOffset).join(",")==="-15,15,-29,29",
 "rest layers must be separate for 4 voices");
ok(chord.every(x=>x.at===0&&x.oct===5&&x.step==="C"),
 "engraving must not alter performed pitches or timestamps");
// Nested tuplets, including multiple tuplet start/stop marks on one note.
// Explicit per-notation tuplet-actual must win over aggregate time-modification.
const m=n(0,1,"A",{tupletActual:15,tupletMarks:[
 {type:"start",number:"1",actual:3,bracket:"yes",placement:"above"}]});
const i=n(2,1,"B",{tupletActual:15,tupletMarks:[
 {type:"start",number:"2",actual:5,bracket:"yes",placement:"above"}]});
const j=n(4,1,"C",{tupletActual:15,tupletMarks:[{type:"stop",number:"2"}]});
const e=n(6,1,"D",{tupletActual:15,tupletMarks:[{type:"stop",number:"1"}]});
T.arrangeVoices([m,i,j,e],[]);
const nested=T.collectSpanners([m,i,j,e]).filter(x=>x.kind==="tuplet");
ok(nested.length===2,"outer and inner tuplet paired independently");
const outer=nested.find(x=>x.id==="1"),inner=nested.find(x=>x.id==="2");
ok(outer.number===3&&inner.number===5,"must render 3 over 5, not combined 15");
ok(outer._lane===0&&inner._lane===1,"nested group brackets have distinct tracks");
const svg=new Node("svg"),x=beat=>100+100*beat;
T.drawScoreSpanners(svg,nested,0,1000,x);
const glyphs=descendants(svg,n=>n.attrs["class"]==="temporal-tuplet");
ok(glyphs.length===2,"both nested brackets visible");
ok(glyphs.map(n=>n.attrs["data-tuplet-lane"]).sort().join(",")==="0,1",
 "nested bracket SVG should keep lane metadata");
ok(descendants(svg,n=>n.tag==="text").map(n=>n.textContent).sort().join(",")==="3,5",
 "nested tuplet text must show both numerals");
// A single note can close an inner group and an outer group simultaneously.
const begin=n(0,1,"C",{tupletActual:15,tupletMarks:[
 {type:"start",number:"1",actual:3,bracket:"yes"},
 {type:"start",number:"2",actual:5,bracket:"yes"}]});
const end=n(2,1,"D",{tupletActual:15,tupletMarks:[
 {type:"stop",number:"2"},{type:"stop",number:"1"}]});
const common=T.collectSpanners([begin,end]).filter(x=>x.kind==="tuplet");
ok(common.length===2,"two start/stop marks on a single note must be preserved");
ok(common.map(x=>x._lane).sort().join(",")==="0,1",
 "identical boundaries from different nested tuplet numbers still need separate label heights");
// Beam group starts before the 1160px boundary and ends beyond it.
const beams=[
 n(9,1,"C",{type:"16th",beam:[{level:1,v:"begin"},{level:2,v:"begin"}]}),
 n(10,1,"D",{type:"16th",beam:[{level:1,v:"continue"},{level:2,v:"end"}]}),
 n(11,1,"E",{type:"16th",beam:[{level:1,v:"end"},{level:2,v:"backward hook"}]})
];
T.arrangeVoices(beams,[]);
const bx=beat=>100+100*beat;
const runs=T.collectBeamGroups(beams,bx);
ok(runs.filter(b=>b.level===1).length===1,
 "one unbroken primary beam across the tile boundary");
ok(runs.filter(b=>b.level===2).length===2,
 "secondary grouped beam and backward hook coexist");
ok(beams.every(n=>Number.isFinite(n._beamTip)),
 "each beamed note stem has an exact beam attachment");
const tile1=new Node("svg"),tile2=new Node("svg");
ok(T.drawScoreBeams(tile1,runs,0,1160)>=2,
 "left tile must render primary and secondary beams");
ok(T.drawScoreBeams(tile2,runs,1160,1160)>=1,
 "right tile must render the continuing primary beam and hook");
const paths=[...descendants(tile1,n=>n.tag==="path"),...descendants(tile2,n=>n.tag==="path")];
ok(paths.length>=3,"actual SVG beam polygons created");
for(const path of paths){
 const coords=(path.attrs.d.match(/-?\d+(?:\.\d+)?/g)||[]).map(Number);
 ok(coords.every(Number.isFinite),"beam polygon must contain only finite coordinates");
 for(let j=0;j<coords.length;j+=2)ok(coords[j]>=-1e-7&&coords[j]<=1160+1e-7,
  "all beam X points clipped inside local tile");
}
// Different voices should never beam together, even at identical beats.
const lower=[
 n(9,2,"G",{beam:[{level:1,v:"begin"}]}),
 n(11,2,"A",{beam:[{level:1,v:"end"}]})
];
T.arrangeVoices(beams.concat(lower),[]);
const independent=T.collectBeamGroups(beams.concat(lower),bx);
ok(independent.filter(b=>b.level===1).length===2,
 "two independent beams are required for two simultaneous voices");
const copy=Object.assign({},beams[0]);
const head=new Node("svg");
T.drawNote(head,copy,bx(copy.at));
const stem=descendants(head,n=>n.tag==="line").find(x=>+x.attrs.x1===+x.attrs.x2);
ok(!!stem,"beamed head must retain its stem");
close(+stem.attrs.y2,copy._beamTip,
 "drawn stem must actually meet interpolated global primary beam");
console.log("PASS NotationAdvancedTest "+checks+
 " assertions: nested 3:5 tuplets, four-voice collisions and multi-level beams across tiles");

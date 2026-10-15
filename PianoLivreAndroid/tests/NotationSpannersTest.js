"use strict";
const assert=require("assert");
const T=require("../app/src/main/assets/temporal-notation.js")._test;
class Node {
 constructor(tag){this.tag=tag;this.attrs={};this.children=[];}
 setAttribute(k,v){this.attrs[k]=String(v);}
 appendChild(child){this.children.push(child);}
}
global.document={createElementNS:(ns,tag)=>new Node(tag)};
let tests=0;
function check(v,why){tests++;assert(v,why);}
function eq(a,b,why){check(Math.abs(a-b)<1e-8,why+": "+a+" vs "+b);}
function note(at,voice,step,oct,options={}){
 return Object.assign({at,measure:0,staff:1,voice:String(voice),
  step,oct,alter:0,clef:"G",stem:"",rest:false,
  beam:[],ties:[],slur:[],tupletActual:0,tupletNormal:0,
  tupletNumber:"1",tupletBracket:"",tupletPlacement:"",
  tupletStart:false,tupletStop:false},options);
}
function descendants(n,condition){
 let out=[];
 for(const c of n.children){if(condition(c))out.push(c);out.push(...descendants(c,condition));}
 return out;
}
// Two voices share the written beat and pitch: print opposing stems and
// offset one head without changing either musical onset/pitch.
const first=note(0,1,"G",4),second=note(0,2,"G",4),
 other=note(1,1,"B",4);
const upperRest=note(1,1,"C",4,{rest:true}),lowerRest=note(1,2,"C",4,{rest:true});
T.arrangeVoices([first,second,other],[upperRest,lowerRest]);
check(first._visualStem==="up"&&second._visualStem==="down",
 "simultaneous independent voices require opposing stems");
check(first._headShift===0&&second._headShift===12,
 "unison notehead collision needs horizontal displacement");
eq(first.at,second.at,"no performed beat can be changed by visual layout");
check(first.step===second.step&&first.oct===second.oct,
 "visual collision repair cannot change the sounding note");
check(upperRest._restOffset===-15&&lowerRest._restOffset===15,
 "rests of independent voices must be separated vertically");
// Always allow an explicitly notated stem direction on single-voice music.
const lone=note(0,1,"C",5,{stem:"down",measure:1});
T.arrangeVoices([lone],[]);
check(lone._visualStem==="down","single-voice explicit stem preserved");
// Long slur (0 -> 26 quarter beats) spans multiple 1160px tiles,
// long tie also spans; a 3:2 group across the boundary must remain one bracket.
const slurA=note(0,1,"C",5,{slur:[{number:"1",type:"start"}]}),
 slurB=note(26,1,"E",5,{slur:[{number:"1",type:"stop"}]});
const tieA=note(0,2,"F",4,{ties:["start"]}),
 tieB=note(26,2,"F",4,{ties:["stop"]});
const tripA=note(8,1,"G",4,{tupletStart:true,tupletActual:3,
 tupletNumber:"1",tupletBracket:"yes",tupletPlacement:"above"}),
 tripB=note(13,1,"A",4,{tupletActual:3,tupletNumber:"1"}),
 tripC=note(18,1,"B",4,{tupletStop:true,tupletActual:3,tupletNumber:"1"});
const all=[slurA,tieA,tripA,tripB,tripC,slurB,tieB];
T.arrangeVoices(all,[]);
const spans=T.collectSpanners(all);
check(spans.length===3,"slur, tie, tuplet must be paired globally");
check(spans.filter(x=>x.kind==="slur").length===1,"one continuous phrase arc");
check(spans.filter(x=>x.kind==="tie").length===1,"one distinct sustain tie");
check(spans.filter(x=>x.kind==="tuplet"&&x.number===3).length===1,
 "one triplet bracket, not three separate labels");
const left=new Node("svg"),middle=new Node("svg"),right=new Node("svg");
const absoluteX=beat=>200+beat*100;
const painted=[
 T.drawScoreSpanners(left,spans,0,1160,absoluteX),
 T.drawScoreSpanners(middle,spans,1160,1160,absoluteX),
 T.drawScoreSpanners(right,spans,2320,1160,absoluteX)
];
check(painted.every(n=>n>0),"each tile must render overlapping global spans");
for(const tile of [left,middle,right]){
 check(descendants(tile,n=>n.attrs["class"]==="temporal-slur").length===1,
  "long slur persists in all three SVG tiles");
 check(descendants(tile,n=>n.attrs["class"]==="temporal-tie").length===1,
  "long tie persists in all three SVG tiles");
 const paths=descendants(tile,n=>n.tag==="path");
 check(paths.every(n=>!n.attrs.d.includes("NaN")),
  "all clipped arc positions must be finite");
}
check(descendants(left,n=>n.attrs["class"]==="temporal-tuplet").length===1 &&
 descendants(middle,n=>n.attrs["class"]==="temporal-tuplet").length===1,
 "tuplet bracket crosses tile boundary");
check(descendants(right,n=>n.attrs["class"]==="temporal-tuplet").length===0,
 "tuplets do not leak outside their musical range");
const nums=[left,middle,right].flatMap(n=>descendants(n,x=>x.tag==="text"&&x.textContent==="3"));
check(nums.length===1,"one triplet numeral for the entire multi-tile group");
for(const tile of [left,middle,right]){
 for(const path of descendants(tile,n=>n.tag==="path")){
  const coords=(path.attrs.d.match(/-?\d+(?:\.\d+)?/g)||[]).map(Number);
  check(coords.every(Number.isFinite),"no invalid SVG path coordinates");
  // x coordinates clipped to its own tile (except no extra margins).
  check(coords[0]>=-0.01&&coords[0]<=1160.01,
   "clipped SVG path starts inside tile viewport");
 }
}
// Explicit bracket=no keeps the tuplet number but omits horizontal strokes.
const unclipped=new Node("svg");
const noBracket=[{kind:"tuplet",from:note(1,1,"C",5),to:note(2,1,"E",5),
 staff:1,voice:"1",number:5,bracket:false,above:true}];
T.drawScoreSpanners(unclipped,noBracket,0,1160,absoluteX);
check(descendants(unclipped,n=>n.tag==="text"&&n.textContent==="5").length===1,
 "explicit unbracketed tuplet still has its number");
check(descendants(unclipped,n=>n.tag==="line").length===0,
 "MusicXML bracket=no must not draw bracket strokes");
console.log("PASS NotationSpannersTest "+tests+
 " assertions: polyphonic voices/rests/heads, cross-tile slur/tie/tuplet continuity");

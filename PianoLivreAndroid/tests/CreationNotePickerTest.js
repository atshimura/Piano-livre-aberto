"use strict";
// Focused C2-B tests: node PianoLivreAndroid/tests/CreationNotePickerTest.js
const assert=require("node:assert/strict");
const picker=require("../app/src/main/assets/creation-note-picker.js");
let checks=0;const ok=(v,msg)=>{checks++;assert.ok(v,msg)};
function entry(beat,pitch=48,x=10,y=20){
 return {getAbsoluteTimestamp:()=>({RealValue:beat/4}),graphicalVoiceEntries:[{notes:[
  {sourceNote:{halfTone:pitch},PositionAndShape:{AbsolutePosition:{x,y}}}]}]};
}
const graphic={measureList:[[{staffEntries:[entry(1)]}],[{staffEntries:[entry(2)]}]]};
const heads=picker.collect(graphic);
ok(heads.length===2,"collected heads");
ok(heads[0].midi===60&&heads[0].beat===1&&heads[0].staff===1,"mapped pitch/beat/staff");
const original={id:"c2-1",pitch:60,staff:1,start:960,end:2880};
let m=picker.reconcile(heads,[original]);
ok(m.matched===2&&m.ambiguous===0,"tie fragments mapped to source attack");
ok(heads.every(x=>x.id==="c2-1"),"stable ID for all tied fragments");
ok(picker.hit(heads,100,200,12,12)==="c2-1","touch the notehead");
ok(picker.hit(heads,130,200,12,12)===null,"do not guess outside hit radius");
m=picker.reconcile(heads,[original,{id:"c2-2",pitch:60,staff:1,start:960,end:1920}]);
ok(m.ambiguous===1,"unison ambiguity detected");
ok(picker.hit(heads,100,200,12,12)===null,"stale IDs cleared on ambiguity");
ok(picker.hit(heads,100,200,12,12)===null,"ambiguous hit cannot mutate another note");
m=picker.reconcile(heads,[original]);
ok(m.matched===2,"mapping restored");
const listeners={};const node={id:"",style:{},attrs:{},setAttribute(k,v){this.attrs[k]=v},remove(){this.removed=true}};
const doc={createElementNS(){return {...node,children:[],appendChild(ch){this.children.push(ch)}}}};
const svg={
 viewBox:{baseVal:{x:0,y:0,width:320,height:250}},
 getBoundingClientRect(){return {left:0,top:0,width:320,height:250}},
 addEventListener(type,handler){listeners[type]=handler},
 removeEventListener(type){delete listeners[type]},
 querySelector(selector){return this.layer||null},
 appendChild(layer){this.layer=layer}
};
const browserGlobal=globalThis;
const oldDocument=browserGlobal.document,oldPerformance=browserGlobal.performance;
browserGlobal.document=doc;browserGlobal.performance={now:()=>1234};
try{
 let chosen=null;
 const binding=picker.bind(svg,graphic,[original],[],id=>{chosen=id});
 ok(binding.matched===2,"binding heads");
 listeners.click({clientX:100,clientY:200});
 ok(chosen==="c2-1","click callback receives stable ID");
 chosen=null;binding.select(["c2-1"]);ok(svg.layer&&svg.layer.children.length===2,"selection outlines both tied heads");
 binding.dispose();ok(!listeners.click&&!listeners.pointerup,"listeners detached");
}finally{
 if(oldDocument===undefined)delete browserGlobal.document;else browserGlobal.document=oldDocument;
 if(oldPerformance===undefined)delete browserGlobal.performance;else browserGlobal.performance=oldPerformance;
}
console.log("CREATION_C2_B_NOTEHEAD_BASIC_PASS checks="+checks);

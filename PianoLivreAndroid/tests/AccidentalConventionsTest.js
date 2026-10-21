'use strict';
const A=require('node:assert/strict'),C=require('../app/src/main/assets/engraving-conventions.js');
function el(name,nodes=[],attrs={},value=''){return {nodeType:1,localName:name,childNodes:nodes,textContent:value,getAttribute:k=>attrs[k]||null};}
const pitch=el('pitch',[el('step',[],{},'F'),el('octave',[],{},'5')]);
const note=el('note',[pitch,el('voice',[],{},'1'),el('staff',[],{},'1'),el('accidental',[],{parentheses:'yes'},'natural')]);
const measure=el('measure',[{nodeType:3},note]);
const doc={documentElement:el('score-partwise',[el('part',[measure])])};
const event={beat:0,staff:1,measure:1,voice:'1',step:'F',alter:0,octave:5,midi:77,sourcePart:0,xmlNoteIndex:1};
const original=JSON.stringify(event);
let calls=0;const mod={index:0,getCategory:()=> 'accidentals',setAsCautionary(){calls++;this.cautionary=true;}};
const graphic={measureList:[[{staffEntries:[{getAbsoluteTimestamp:()=>({RealValue:0}),graphicalVoiceEntries:[{parentVoiceEntry:{ParentVoice:{VoiceId:1}},notes:[{sourceNote:{Pitch:{FundamentalNote:5,AccidentalHalfTones:0,Octave:2},halfTone:65},vfnote:[{getModifiers:()=>[mod]},0]}]}]}]}]]};
A.deepEqual(C.apply(doc,graphic,[event]),{requested:1,applied:1,unresolved:0});A.equal(calls,1);A.equal(mod.cautionary,true);
A.equal(JSON.stringify(event),original,'Written identity and native time must not be mutated');
for(const bad of [{...event,xmlNoteIndex:0},{...event,step:'E'},{...event,voice:'2'},{...event,sourcePart:1}]){
 calls=0;A.equal(C.apply(doc,graphic,[bad]).applied,0,'Incomplete or conflicting XML/native identity must not style another note');A.equal(calls,0);
}
calls=0;A.equal(C.apply(doc,graphic,[event,event]).applied,0,'Ambiguous source identity must not be guessed');A.equal(calls,0);
console.log('ACCIDENTAL_CONVENTIONS_PASS nativeVexFlow=1 eventMutation=0 conservativeIdentity=5');

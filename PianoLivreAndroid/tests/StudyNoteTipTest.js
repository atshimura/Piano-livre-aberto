'use strict';
const assert=require('assert');
const fs=require('fs');
const path=require('path');
const Tip=require('../app/src/main/assets/study-note-tip.js');
const xmlPitch={step:'C',alter:0,octave:4};
assert.equal(Tip.international(xmlPitch),'C4');
assert.equal(Tip.label(xmlPitch,'Dó4'),'Dó4 · C4');
assert.equal(Tip.label({step:'D',alter:-1,octave:3},'Ré♭3'),'Ré♭3 · D♭3');
assert.equal(Tip.label({step:'F',alter:1,octave:5},'Fá♯5'),'Fá♯5 · F♯5');
assert.equal(Tip.label({step:'Q',alter:0,octave:4},'???'),null);
assert.equal(Tip.label(null,'Dó4'),null);
for(const size of [[738,196],[360,180],[883,409]]){
 const q=Tip.placement({width:size[0],height:size[1]},{width:180,height:32});
 assert(q.left>=4&&q.top===4);
 assert(q.maxWidth<=size[0]&&q.maxHeight<=size[1]);
}
const notation=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/notation.html'),'utf8');
assert(notation.includes('study-note-tip.js'),'JS label module must load');
assert(notation.includes('showOpticalNoteTip((international'),'note tap must use bounded toast');
assert(notation.includes('},2200);'),'note tap must expire');
assert(notation.includes('hideOpticalNoteTip();\n  loaded=false;'),'change score must cancel previous toast');
assert(!notation.includes("ev.clientY-44)+'px';tip.style.display='block';\n   console.log('POC_NOTE_TAP"),'old note badge obscures lower staff');
const temporal=fs.readFileSync(path.join(__dirname,'../app/src/main/assets/temporal-notation.js'),'utf8');
assert(temporal.includes('popup.style.right="4px";')&&temporal.includes('popup.style.top="4px";'),
 'temporal renderer must not anchor identification over the touched lower staff');
assert(!temporal.includes('headRect.top-rootRect.top-57'),
 'temporal popup placement may not cover lower staff');
assert(notation.includes('font:12px/1.1 sans-serif')&&
 notation.includes('white-space:nowrap;overflow:hidden;text-overflow:ellipsis'),
 'OSMD popup must be a single compact line above rather than across the upper staff');
assert(temporal.includes('font:600 12px/1.1')&&temporal.includes('popup.style.whiteSpace="nowrap"'),
 'temporal popup must have compact height even with contextual chord words');
console.log('STUDY_NOTE_TIP_PASS');

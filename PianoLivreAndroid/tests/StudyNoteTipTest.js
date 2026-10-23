'use strict';
/* GPL-3.0-only. Regression: OSMD note labels must fit small Study viewports. */
const assert=require('node:assert/strict'),fs=require('node:fs');
const T=require('../app/src/main/assets/study-note-tip.js');
let checks=0;function eq(actual,expected,msg){checks++;assert.equal(actual,expected,msg)}
function ok(value,msg){checks++;assert.ok(value,msg)}
eq(T.international({step:'C',alter:0,octave:4}),'C4','middle C');
eq(T.international({step:'D',alter:1,octave:5}),'D♯5','written sharp');
eq(T.international({step:'E',alter:-1,octave:3}),'E♭3','written flat');
eq(T.international({step:'B',alter:2,octave:2}),'B𝄪2','written double sharp');
eq(T.international({step:'H',alter:0,octave:4}),null,'unknown musical spelling');
eq(T.international({step:'C',alter:0,octave:NaN}),null,'missing octave');
eq(T.international(null),null,'no fabricated note');
for(const viewport of [{left:0,top:0,right:738,bottom:196},{left:0,top:0,right:360,bottom:180},
                        {left:10,top:30,right:893,bottom:439}]){
 for(const p of [{x:viewport.left+4,y:viewport.top+4},{x:viewport.right-2,y:viewport.bottom-2},
                 {x:(viewport.right+viewport.left)/2,y:(viewport.top+viewport.bottom)/2}]){
  const b=T.place(viewport,p,190,34,[]);
  ok(b.left>=viewport.left+3.9&&b.top>=viewport.top+3.9,'no offscreen top/left');
  ok(b.left+190<=viewport.right-3.9&&b.top+34<=viewport.bottom-3.9,'no offscreen bottom/right');
 }
}
const view={left:0,top:0,right:738,bottom:196},point={x:360,y:136};
const head={left:300,top:112,right:440,bottom:168};
const rail={left:0,top:0,right:152,bottom:196};
const placed=T.place(view,point,180,30,[{rect:head,weight:200},{rect:rail,weight:200}]);
eq(T.overlaps({left:placed.left,top:placed.top,right:placed.left+180,bottom:placed.top+30},head),0,
 'selected notehead must not be obscured when an unoccupied candidate exists');
eq(T.overlaps({left:placed.left,top:placed.top,right:placed.left+180,bottom:placed.top+30},rail),0,
 'context rail must stay visible');
const html=fs.readFileSync('app/src/main/assets/notation.html','utf8');
ok(html.includes('<script src="study-note-tip.js"></script>'),'the real renderer loads layout helper');
ok(html.includes('showOpticalTip(noteName&&international'),'OSMD note touch displays both spellings');
ok(html.includes('opticalTipTimeout=setTimeout(hideOpticalTip,2000)'),'identification automatically expires');
ok(html.includes('hideOpticalTip();\n  loaded=false'),'opening new score clears stale label');
ok(html.includes('tip.textContent=label'),'no unsafe HTML interpolation from MusicXML');
const temporal=fs.readFileSync('app/src/main/assets/temporal-notation.js','utf8');
ok(temporal.includes('StudyNoteTip.place(vp'),'temporal Study shares the same viewport-safe placement');
ok(temporal.includes('st.popupTimer=setTimeout(function(){popup.style.display="none"'),'temporal popup still expires');

console.log('STUDY_NOTE_TIP_PASS checks='+checks);

'use strict';
const assert=require('assert'),W=require('../app/src/main/assets/study-window.js');
// Distant extremes must not force the opening to shrink; a spanning slur is
// nevertheless included wherever any part of its conservative box is visible.
const boxes=[{x:0,y:100,w:10000,h:150},{x:0,y:80,w:10,h:10},
 {x:7000,y:0,w:2500,h:120},{x:4400,y:300,w:1,h:85}];
const m=W.plan(boxes,{bounds:[0,-4,10000,400],width:738,height:196,context:{x:0,y:90,w:100,h:170}});
assert(m.windows[0].scale>m.fullScale,'local opening gains readability');
assert(m.windows.some(w=>w.scale<m.windows[0].scale),'distant extremes must reduce scale where necessary');
for(let x=0;x<10000;x+=13){
 const i=W.select(m,x),w=m.windows[i],v=W.inspect(m,i);
 assert.equal(v.clipped,0,'no visible primitive may be clipped');
 assert(w.left<=x-(Math.min(738*.4,130*w.scale+12)+53)/w.scale,'left of viewport guarded');
 assert(w.right>=x+738/w.scale,'right of viewport guarded');
 assert(w.height*w.scale<=188.000001,'both staves fit with uniform scale');
 assert(w.scale>=m.fullScale,'window cannot be smaller than complete fit');
}
assert.equal(W.select(m,-1),0);assert.equal(W.select(m,1e9),m.windows.length-1);
console.log('STUDY_WINDOW_PASS');

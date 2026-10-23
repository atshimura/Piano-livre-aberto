'use strict';
const assert=require('assert'),W=require('../app/src/main/assets/study-window.js');
// Distant extremes must not force the opening to shrink; a spanning slur is
// nevertheless included wherever any part of its conservative box is visible.
const boxes=[{x:0,y:100,w:10000,h:150},{x:0,y:80,w:10,h:10},
 {x:7000,y:0,w:2500,h:120},{x:4400,y:300,w:1,h:85}];
const m=W.plan(boxes,{bounds:[0,-4,10000,400],width:738,height:196,context:{x:0,y:90,w:100,h:170}});
assert(m.cellWidth<738/.7/5,'window cells must be substantially narrower than the visible viewport');
assert(m.windows.length>40,'inspect enough local envelopes to avoid premature distant extremes');
assert(m.windows[0].scale>m.fullScale,'local opening gains readability');
assert(m.windows.some(w=>w.scale<m.windows[0].scale),'distant extremes must reduce scale where necessary');
for(let x=0;x<10000;x+=13){
 const i=W.select(m,x),w=m.windows[i],v=W.inspect(m,i);
 assert.equal(v.clipped,0,'no visible primitive may be clipped');
 assert(w.left<=x-w.anchorPx/w.scale,'left of viewport guarded');
 assert(w.right>=x+(738-w.anchorPx)/w.scale,'right of viewport guarded');
 assert(w.height*w.scale<=188.000001,'both staves fit with uniform scale');
 assert(w.scale>=m.fullScale,'window cannot be smaller than complete fit');
}
// The reference-line margin changes the visible horizontal envelope. The
// planner must retain all ink while avoiding offscreen extremes.
const refined=W.plan(boxes,{bounds:[0,-4,10000,400],width:738,height:196,
 context:{x:0,y:90,w:100,h:170},firstX:550});
for(let i=0;i<refined.windows.length;i++)
 assert.equal(W.inspect(refined,i).clipped,0,'anchored camera clips ink');
// Position the extreme inside the unanchored camera's sampled envelope but
// outside the anchored camera's envelope, including at the initial fit scale.
// At x=1940 it was outside BOTH cameras and could not distinguish the cases.
const edgeBoxes=[{x:0,y:95,w:10000,h:210},{x:1550,y:0,w:10,h:370}];
const common={bounds:[0,0,10000,400],width:738,height:196};
const surplus=W.plan(edgeBoxes,Object.assign({},common,{firstX:0}));
const anchored=W.plan(edgeBoxes,Object.assign({},common,{firstX:550}));
assert(anchored.windows[0].scale>surplus.windows[0].scale+.001,
 'a glyph beyond the actual right viewport must not shrink the opening');
for(const sample of anchored.windows)assert(sample.staffSpace>=10*anchored.fullScale,
 'an anchored window cannot shrink below full-score fit');
// Continuous camera must stay safe for intermediate beats, including movement
// backward and across every grid boundary. No apparent smoothness may be bought
// with a glyph cut or a musical-coordinate shift.
for(const model of [m,refined,anchored,surplus]){
 let previous=null;
 for(let x=model.bounds[0];x<=model.bounds[0]+model.bounds[2];x+=13){
  const sample=W.sample(model,x);
  assert(sample&&sample.scale>=model.fullScale,'continuous sample below global fit');
  assert.equal(W.inspect(model,sample).clipped,0,'continuous view clips SVG ink at '+x);
  assert(sample.top<=sample.inkTop-3.9999,'sample cuts top ink');
  assert(sample.top+sample.height>=sample.inkBottom+3.9999,'sample cuts bottom ink');
  if(previous)assert(Math.abs(sample.scale-previous.scale)<.075,
   'large scale discontinuity in neighboring sampling positions');
  previous=sample;
 }
 for(let i=1;i<model.windows.length;i++){
  const edge=model.windows[i].start;
  const left=W.sample(model,edge-.00001),right=W.sample(model,edge+.00001);
  assert(Math.abs(left.scale-right.scale)<.0005,
   'cell crossing cannot snap scale at x='+edge);
 }
 for(let x=model.bounds[0]+model.bounds[2];x>=model.bounds[0];x-=17)
  assert.equal(W.inspect(model,W.sample(model,x)).clipped,0,'reverse seek must not clip');
}
assert.equal(W.select(m,-1),0);assert.equal(W.select(m,1e9),m.windows.length-1);
console.log('STUDY_WINDOW_PASS');

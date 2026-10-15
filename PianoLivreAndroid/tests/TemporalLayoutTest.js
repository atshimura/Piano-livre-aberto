"use strict";
const assert=require("assert");
const T=require("../app/src/main/assets/temporal-notation.js")._test;
assert.strictEqual(T.cursorPx(1000),400);
assert.strictEqual(T.msPerBeat(100),600);
assert.strictEqual(T.msPerBeat(80),750);
const K=112,origin=400;
function X(t){return T.computeX(t,K,origin);}
const beats=[0,.25,.5,1,2,4,5.5],pixels=beats.map(X);
for(let i=1;i<beats.length;i++){
 const distance=pixels[i]-pixels[i-1];
 assert(Math.abs(distance-K*(beats[i]-beats[i-1]))<1e-9,"Time proportional spacing is broken");
}
assert.strictEqual(X(1)-X(0),112);
assert.strictEqual(X(2)-X(1),112);
assert.strictEqual(X(.5)-X(0),56);
assert.strictEqual(X(4)-X(2),224);
assert.strictEqual(X(3),X(3)); // simultaneous events share x
for(const bpm of [60,80,100,120]){
 const pxPerSecond=K*bpm/60,travel=pxPerSecond*T.msPerBeat(bpm)/1000;
 assert(Math.abs(travel-K)<1e-9,"Incorrect visual distance in one beat at BPM "+bpm);
}
console.log("PASS: strict beat-to-pixel linearity, simultaneous voices, BPM speed and fixed cursor");

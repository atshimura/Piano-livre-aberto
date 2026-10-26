/* GPL-3.0-only. Small, temporary note identification overlay for Study. */
(function(root){
 'use strict';
 const pitchAlter={'-2':'𝄫','-1':'♭','0':'','1':'♯','2':'𝄪'};
 function international(event){
  if(!event||!/[A-G]/.test(event.step)||String(event.step).length!==1||
     !Number.isInteger(event.octave)||event.octave<0||event.octave>9||
     !Number.isInteger(event.alter)||Math.abs(event.alter)>2)return null;
  return event.step+pitchAlter[event.alter]+event.octave;
 }
 function overlaps(a,b){
  const x=Math.max(0,Math.min(a.right,b.right)-Math.max(a.left,b.left));
  const y=Math.max(0,Math.min(a.bottom,b.bottom)-Math.max(a.top,b.top));
  return x*y;
 }
 function place(view,point,width,height,obstacles){
  if(!view||!point||!(width>0&&height>0))return null;
  const margin=4,limitLeft=view.left+margin,limitTop=view.top+margin;
  const limitRight=Math.max(limitLeft,view.right-margin-width);
  const limitBottom=Math.max(limitTop,view.bottom-margin-height);
  const clamp=(v,lo,hi)=>Math.max(lo,Math.min(hi,v));
  const xs=[point.x-width/2,point.x+14,point.x-width-14,limitLeft,limitRight];
  const ys=[point.y-height-14,point.y+14,limitTop,limitBottom];
  let winner=null,best=Infinity;
  xs.forEach(function(x){
   ys.forEach(function(y){
    const left=clamp(x,limitLeft,limitRight),top=clamp(y,limitTop,limitBottom);
    const box={left:left,top:top,right:left+width,bottom:top+height};
    // Staff strokes can fill the screen on small phones. Penalize their ink
    // but prefer never obscuring a notehead, clef, context rail or selected note.
    let cost=0;
    (obstacles||[]).forEach(function(o){
     if(!o||!o.rect)return;
     cost+=overlaps(box,o.rect)*(o.weight||1);
    });
    // Prefer a label close to the touch, without sacrificing music visibility.
    cost+=Math.hypot(left+width/2-point.x,top+height/2-point.y)*.02;
    if(cost<best){best=cost;winner={left:left,top:top,overlapCost:cost};}
   });
  });
  return winner;
 }
 const api={international:international,place:place,overlaps:overlaps};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.StudyNoteTip=api;
})(typeof window!=='undefined'?window:null);

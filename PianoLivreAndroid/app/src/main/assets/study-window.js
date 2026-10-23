/* GPL-3.0-only. A camera on the complete engraving; never removes musical nodes. */
(function(root){
 'use strict';
 function union(boxes){
  if(!boxes.length)return null;
  let left=Infinity,top=Infinity,right=-Infinity,bottom=-Infinity;
  boxes.forEach(function(b){left=Math.min(left,b.x);top=Math.min(top,b.y);right=Math.max(right,b.x+b.w);bottom=Math.max(bottom,b.y+b.h);});
  return {x:left,y:top,w:right-left,h:bottom-top};
 }
 function collect(svg){
  const inverse=svg.getScreenCTM().inverse(),boxes=[],context=[];
  Array.from(svg.querySelectorAll('path,text,rect,circle,ellipse,line,polygon,polyline')).forEach(function(node){
   if(node.closest('#piano-optical-study-cues')||node.hasAttribute('data-study-hidden-reason'))return;
   const raw=node.getBBox(),style=getComputedStyle(node);
   const stroke=style.stroke&&style.stroke!=='none'?(parseFloat(style.strokeWidth)||0)/2:0;
   const m=inverse.multiply(node.getScreenCTM());
   const corners=[[raw.x-stroke,raw.y-stroke],[raw.x+raw.width+stroke,raw.y-stroke],
    [raw.x-stroke,raw.y+raw.height+stroke],[raw.x+raw.width+stroke,raw.y+raw.height+stroke]].map(function(p){return {x:m.a*p[0]+m.c*p[1]+m.e,y:m.b*p[0]+m.d*p[1]+m.f,w:0,h:0};});
   const b=union(corners);
   if(![b.x,b.y,b.w,b.h].every(Number.isFinite))throw Error('Unmeasurable Study window glyph');
   // Retain primitive identity for reproducible diagnostics; no engraving changes.
   b.kind=(node.closest('g[class]')||node).getAttribute('class')||node.tagName;
   b.tag=node.tagName;
   b.text=node.tagName.toLowerCase()==='text'?(node.textContent||'').trim().slice(0,32):'';
   boxes.push(b);
   if(node.closest('.vf-clef,.vf-keysignature,.vf-timesignature,.vf-brace'))context.push(b);
  });
  return {boxes:boxes,context:union(context)};
 }
 function plan(boxes,options){
  const global=options.bounds,width=options.width,height=options.height,available=Math.max(1,height-8);
  const fullScale=available/global[3],targetScale=Math.max(fullScale,.7);
  // Bound the look-ahead to a fraction of the visible viewport. The earlier
  // half-viewport cells could let distant vertical extremes shrink a camera
  // long before their glyphs could actually enter the painted area.
  // This is a geometric sampling change, not a staff-size acceptance rule.
  const cellWidth=Math.max(80,width/targetScale/6),windows=[];
  const context=options.context,firstX=Number.isFinite(options.firstX)?options.firstX:global[0];
  // Use the actual reference line and the opening's marginLeft. The older
  // planner guarded a full extra viewport to the right of EVERY cell, even
  // when that ink could not reach the painted viewport at any beat in it.
  function frame(start,scale){
   const referenceX=Math.min(Math.max(1,width-24),Math.max(
    Math.min(width*.4,Math.min(width,130*scale)+12),
    (options.railUnits||130)*scale+12));
   const reference=referenceX+53,offset=Math.max(0,reference-firstX*scale);
   const anchor=reference-offset;
   const left=start-anchor/scale-20;
   const right=start+cellWidth+(width-anchor)/scale+20;
   const active=boxes.filter(function(b){return b.x+b.w>=left&&b.x<=right;});
   if(context)active.push(context);
   return {left:left,right:right,anchorPx:anchor,ink:union(active)};
  }
  for(let start=global[0];start<global[0]+global[2];start+=cellWidth){
   let scale=fullScale,f=null;
   // Re-evaluate at the final painted scale: both the viewport width in SVG
   // units and the offset of the fixed reference line depend on that scale.
   for(let pass=0;pass<8;pass++){
    f=frame(start,scale);
    if(!f.ink)return null;
    const desired=Math.max(fullScale,Math.min(targetScale,available/(f.ink.h+8)));
    if(Math.abs(desired-scale)<.0000001)break;
    scale=desired;
   }
   // Never accept a camera based only on the previous iteration's envelope.
   // If an anchor shift exposes ink, fall back monotonically until it fits.
   for(let pass=0;pass<8;pass++){
    f=frame(start,scale);
    if(f.ink.h+8<=available/scale+.000001)break;
    const safer=Math.max(fullScale,Math.min(scale,available/(f.ink.h+8)));
    if(Math.abs(safer-scale)<.0000001){scale=fullScale;break;}
    scale=safer;
   }
   f=frame(start,scale);
   const ink=f.ink,fit=available/scale;
   windows.push({start:start,end:start+cellWidth,left:f.left,right:f.right,
    anchorPx:f.anchorPx,top:ink.y-4-(fit-(ink.h+8))/2,height:fit,scale:scale,
    inkTop:ink.y,inkBottom:ink.y+ink.h,staffSpace:10*scale});
  }
  return {bounds:global.slice(),cellWidth:cellWidth,windows:windows,boxes:boxes,
   firstX:firstX,fullScale:fullScale,width:width,height:height,current:-1};
 }
 function select(model,x){return Math.max(0,Math.min(model.windows.length-1,Math.floor((x-model.bounds[0])/model.cellWidth)));}
 function inspect(model,index){
  const w=model.windows[index],active=model.boxes.filter(function(b){return b.x+b.w>=w.left&&b.x<=w.right;});
  return {checkedPrimitives:active.length,clipped:active.filter(function(b){return b.y<w.top-.5||b.y+b.h>w.top+w.height+.5;}).length};
 }
 const api={collect:collect,plan:plan,select:select,inspect:inspect};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.StudyWindow=api;
})(typeof window!=='undefined'?window:null);

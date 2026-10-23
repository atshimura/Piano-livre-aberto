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
   b.node=node; // Optional browser-only shape-contact analysis.
   b.text=node.tagName.toLowerCase()==='text'?(node.textContent||'').trim().slice(0,32):'';
   b.pathData=b.kind==='vf-curve'&&node.tagName.toLowerCase()==='path'?
    (node.getAttribute('d')||'').slice(0,460):'';
   boxes.push(b);
   if(node.closest('.vf-clef,.vf-keysignature,.vf-timesignature,.vf-brace'))context.push(b);
  });
  return {boxes:boxes,context:union(context)};
 }
 function plan(boxes,options){
  const global=options.bounds,width=options.width,height=options.height,available=Math.max(1,height-8);
  // The full fit must include actual stroked primitives and the 4-unit
  // top/bottom inset; OSMD's viewBox alone can be slightly too short.
  const fullInk=union(boxes.concat(options.context?[options.context]:[]));
  const fullScale=available/Math.max(global[3],fullInk?fullInk.h+8:global[3]);
  const targetScale=Math.max(fullScale,options.probeMaxFit?1:.7);
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
   if(options.probeMaxFit){
    // A higher scale narrows the horizontal viewport. Starting exclusively
    // from the full fit may get trapped in a low-scale envelope containing
    // two distant vertical extremes which do not coexist at higher zoom.
    // Search descending, certifying each candidate using the complete ink.
    for(let step=0;step<=60;step++){
     const candidate=targetScale-(targetScale-fullScale)*step/60;
     const attempt=frame(start,candidate);
     if(attempt.ink&&attempt.ink.h+8<=available/candidate+1e-7){
      scale=candidate;f=attempt;break;
     }
    }
   }
   // Re-evaluate at the final painted scale: both the viewport width in SVG
   // units and the offset of the fixed reference line depend on that scale.
   if(!options.probeMaxFit)for(let pass=0;pass<8;pass++){
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
   context:context,railUnits:options.railUnits||130,firstX:firstX,fullScale:fullScale,width:width,height:height,
   safetyLeadCells:Number.isFinite(options.safetyLeadCells)?Math.max(0,Math.min(1,options.safetyLeadCells)):1,current:-1};
 }
 function select(model,x){return Math.max(0,Math.min(model.windows.length-1,Math.floor((x-model.bounds[0])/model.cellWidth)));}
 // A Lipschitz minorant of the safe per-cell scales: for any position within
 // cell i the result is never larger than the verified scale of that cell.
 // Neighboring cells therefore influence the camera *before* a smaller scale
 // is required, rather than introducing a step at the cell boundary.
 function sample(model,x){
  const index=select(model,x),windows=model.windows,chosen=windows[index];
  const available=Math.max(1,model.height-8);
  const slope=model.fullScale*model.fullScale/Math.max(1,model.width);
  let scale=chosen.scale;
  for(const w of windows){
   const distance=x<w.start?w.start-x:(x>w.end?x-w.end:0);
   scale=Math.min(scale,w.scale+slope*distance);
  }
  scale=Math.max(model.fullScale,scale);
  const fraction=Math.max(0,Math.min(1,(x-chosen.start)/(chosen.end-chosen.start)));
  const following=windows[Math.min(windows.length-1,index+1)];
  // Interpolate guarded window limits as well as the camera: switching the
  // guard itself at a cell boundary previously caused a 0.053-scale jump.
  const lead=model.cellWidth*model.safetyLeadCells;
  const guardedLeft=chosen.left+(following.left-chosen.left)*fraction-lead;
  const guardedRight=chosen.right+(following.right-chosen.right)*fraction+lead;
  const context=model.context,firstX=model.firstX,rail=model.railUnits||130;
  function frameAt(s){
   const referenceX=Math.min(Math.max(1,model.width-24),Math.max(
    Math.min(model.width*.4,Math.min(model.width,130*s)+12),rail*s+12));
   const reference=referenceX+53,offset=Math.max(0,reference-firstX*s),anchor=reference-offset;
   // Keep the entire cell envelope (plus one cell on either side) under
   // inspection while WebView scrollLeft and SVG width settle after relayout.
   // A long curve may paint across the left edge before the next scroll event.
   const left=Math.min(x-anchor/s-20,guardedLeft);
   const right=Math.max(x+(model.width-anchor)/s+20,guardedRight);
   const active=model.boxes.filter(function(b){return b.x+b.w>=left&&b.x<=right;});
   if(context)active.push(context);
   const ink=union(active);
   if(!ink)return {left:left,right:right,ink:null};
   // Introduce outside extremes gradually as they approach the GUARDED
   // viewport. Their weight reaches one before they become visible. A
   // discrete bounding-box enter/exit event must not snap staff size.
   const baselineTop=ink.y,baselineBottom=ink.y+ink.h;
   let top=baselineTop,bottom=baselineBottom;
   for(const b of model.boxes){
    const gap=b.x+b.w<left?left-b.x-b.w:(b.x>right?b.x-right:0);
    if(gap<=0||gap>=model.cellWidth)continue;
    const weight=1-gap/model.cellWidth;
    top=Math.min(top,baselineTop+Math.min(0,b.y-baselineTop)*weight);
    bottom=Math.max(bottom,baselineBottom+Math.max(0,b.y+b.h-baselineBottom)*weight);
   }
   return {left:left,right:right,ink:{x:ink.x,y:top,w:ink.w,h:bottom-top}};
  }
  let f=frameAt(scale);
  // At smaller scales the painted horizontal range becomes wider. Check
  // that new range explicitly; never rely solely on the cell's original ink.
  for(let pass=0;pass<12;pass++){
   if(!f.ink)return null;
   const safe=Math.max(model.fullScale,Math.min(scale,available/(f.ink.h+8)));
   if(Math.abs(safe-scale)<1e-9)break;
   scale=safe;f=frameAt(scale);
  }
  f=frameAt(scale);
  if(f.ink.h+8>available/scale+1e-6){
   scale=model.fullScale;f=frameAt(scale);
  }
  const fit=available/scale,ink=f.ink;
  if(!ink||ink.h+8>fit+1e-6)throw Error('Study continuous camera cannot contain visible ink: '+JSON.stringify({x:x,index:index,scale:scale,fullScale:model.fullScale,ink:ink,fit:fit}));
  const requestedTop=chosen.top+(following.top-chosen.top)*fraction;
  const top=Math.max(ink.y+ink.h+4-fit,Math.min(ink.y-4,requestedTop));
  return {index:index,start:chosen.start,end:chosen.end,scale:scale,staffSpace:10*scale,
   top:top,height:fit,left:f.left,right:f.right,inkTop:ink.y,inkBottom:ink.y+ink.h};
 }
 function inspect(model,index){
  const w=typeof index==='number'?model.windows[index]:index,active=model.boxes.filter(function(b){return b.x+b.w>=w.left&&b.x<=w.right;});
  return {checkedPrimitives:active.length,clipped:active.filter(function(b){return b.y<w.top-.5||b.y+b.h>w.top+w.height+.5;}).length};
 }
 const api={collect:collect,plan:plan,select:select,sample:sample,inspect:inspect};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.StudyWindow=api;
})(typeof window!=='undefined'?window:null);

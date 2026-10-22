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
   boxes.push(b);
   if(node.closest('.vf-clef,.vf-keysignature,.vf-timesignature,.vf-brace'))context.push(b);
  });
  return {boxes:boxes,context:union(context)};
 }
 function plan(boxes,options){
  const global=options.bounds,width=options.width,height=options.height,available=Math.max(1,height-8);
  const fullScale=available/global[3],targetScale=Math.max(fullScale,.7);
  const cellWidth=Math.max(100,width/targetScale/2),windows=[];
  const context=options.context;
  for(let start=global[0];start<global[0]+global[2];start+=cellWidth){
   let scale=fullScale,window=null;
   // Monotone refinement: a larger scale exposes less horizontal content.
   // The last interval was calculated at <= the final scale, so it contains
   // every visible glyph, including long slurs with endpoints outside it.
   for(let pass=0;pass<3;pass++){
    const reference=Math.min(width*.4,130*targetScale+12)+53;
    const left=start-reference/scale-20,right=start+cellWidth+width/scale+20;
    const active=boxes.filter(function(b){return b.x+b.w>=left&&b.x<=right;});
    if(context)active.push(context);
    const ink=union(active);
    if(!ink)return null;
    const top=ink.y-4,bottom=ink.y+ink.h+4;
    scale=Math.max(fullScale,Math.min(targetScale,available/(bottom-top)));
    const fittedHeight=available/scale;
    window={start:start,end:start+cellWidth,left:left,right:right,
     top:top-(fittedHeight-(bottom-top))/2,height:fittedHeight,scale:scale,
     inkTop:ink.y,inkBottom:ink.y+ink.h,staffSpace:10*scale};
   }
   windows.push(window);
  }
  return {bounds:global.slice(),cellWidth:cellWidth,windows:windows,boxes:boxes,
   fullScale:fullScale,width:width,height:height,current:-1};
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

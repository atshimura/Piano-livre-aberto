/* Piano Livre — experimental optical x(beat) mapping; no external dependencies.
 * The musical clock remains native. A nonuniform score position is derived from
 * OSMD's graphical staff entries, NOT from MIDI sample arrival times.
 */
(function(root){
 "use strict";
 function create(positions){
  if(!Array.isArray(positions)||!positions.length)throw Error("Optical timeline without events");
  const groups=new Map();
  for(const entry of positions){
   if(!entry||!Number.isFinite(entry.beat)||!Number.isFinite(entry.x)||entry.beat<0||entry.x<0)
    throw Error("Invalid graphical event");
   const beat=Math.round(entry.beat*1000000)/1000000;
   if(!groups.has(beat))groups.set(beat,[]);
   groups.get(beat).push(entry.x);
  }
  const stops=Array.from(groups.keys()).sort((a,b)=>a-b).map(beat=>{
   const xs=groups.get(beat).slice().sort((a,b)=>a-b);
   return {beat:beat,x:xs[Math.floor(xs.length/2)],voices:xs.length};
  });
  for(let i=1;i<stops.length;i++){
   if(stops[i].x+0.01<stops[i-1].x)
    throw Error("OSMD coordinates move backwards; score may contain system breaks");
   if(stops[i].x<stops[i-1].x)stops[i].x=stops[i-1].x;
  }
  function xAt(beat){
   if(!Number.isFinite(beat))throw Error("Invalid beat");
   if(beat<=stops[0].beat)return stops[0].x;
   const last=stops[stops.length-1];
   if(beat>=last.beat)return last.x;
   let lo=0,hi=stops.length-1;
   while(hi-lo>1){let mid=(hi+lo)>>1;if(stops[mid].beat<=beat)lo=mid;else hi=mid;}
   const a=stops[lo],b=stops[hi],t=(beat-a.beat)/(b.beat-a.beat);
   return a.x+(b.x-a.x)*t;
  }
  function beatAt(x){
   if(!Number.isFinite(x))throw Error("Invalid graphical X");
   if(x<=stops[0].x)return stops[0].beat;
   const last=stops[stops.length-1];
   if(x>=last.x)return last.beat;
   let lo=0,hi=stops.length-1;
   while(hi-lo>1){const mid=(hi+lo)>>1;if(stops[mid].x<=x)lo=mid;else hi=mid;}
   const a=stops[lo],b=stops[hi];
   if(b.x-a.x<0.001)return a.beat;
   return a.beat+(x-a.x)/(b.x-a.x)*(b.beat-a.beat);
  }
  return {stops:stops,xAt:xAt,beatAt:beatAt,
   range:{from:stops[0].beat,to:stops[stops.length-1].beat,
    left:stops[0].x,right:stops[stops.length-1].x}};
 }
 function fromGraphical(graphic){
  if(!graphic||!Array.isArray(graphic.measureList))throw Error("No OSMD graphical measure list");
  const entries=[],notes=[];
  for(let measureIndex=0;measureIndex<graphic.measureList.length;measureIndex++){
   const staves=graphic.measureList[measureIndex];
   for(let staffIndex=0;staffIndex<(staves||[]).length;staffIndex++){
    const measure=staves[staffIndex];
    if(!measure)continue;
    for(const e of measure.staffEntries||[]){
     const timestamp=e.getAbsoluteTimestamp&&e.getAbsoluteTimestamp();
     const pos=e.PositionAndShape&&e.PositionAndShape.AbsolutePosition;
     if(!timestamp||!pos||!Number.isFinite(timestamp.RealValue))continue;
     const beat=timestamp.RealValue*4;
     const x=pos.x*10;
     entries.push({beat:beat,x:x,staff:staffIndex+1,measure:measureIndex+1});
     for(const voice of e.graphicalVoiceEntries||[])for(const gn of voice.notes||[]){
      const src=gn.sourceNote;
      if(!src||src.isRestFlag||src.IsGraceNote)continue;
      const gp=gn.PositionAndShape&&gn.PositionAndShape.AbsolutePosition;
      const pitch=src.halfTone;
      notes.push({beat:beat,staff:staffIndex+1,measure:measureIndex+1,
       x:gp&&Number.isFinite(gp.x)?gp.x*10:x,
       y:gp&&Number.isFinite(gp.y)?gp.y*10:null,
       // OSMD halfTone uses C3=48; legacy Score.java MIDI counts C4=60.
       // Do NOT interpret this as a verified mapping for transposing instruments.
       midiEstimate:Number.isFinite(pitch)?pitch+12:null});
     }
    }
   }
  }
  return {timeline:create(entries),notes:notes,sourceEntries:entries.length};
 }
 const out={create:create,fromGraphical:fromGraphical};
 if(typeof module!=="undefined"&&module.exports)module.exports=out;
 if(root)root.OpticalTimeMap=out;
})(typeof window!=="undefined"?window:null);

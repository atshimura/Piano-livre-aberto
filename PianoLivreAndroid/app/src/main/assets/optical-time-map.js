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
 /** Hit-test the GRAPHICAL note head rather than an onset column or MIDI pitch.
  * Coordinates are SVG viewBox coordinates, not screen CSS coordinates.
  */
 function hitTest(notes,x,y,xRadius,yRadius){
  if(!Array.isArray(notes)||!Number.isFinite(x)||!Number.isFinite(y))return null;
  const rx=xRadius===undefined?11:xRadius,ry=yRadius===undefined?7:yRadius;
  if(!(rx>0&&ry>0))return null;
  let best=null,distance=Infinity;
  for(const n of notes){
   if(!n||!Number.isFinite(n.x)||!Number.isFinite(n.y))continue;
   const dx=(n.x-x)/rx,dy=(n.y-y)/ry;
   if(Math.abs(dx)>1||Math.abs(dy)>1)continue;
   const d=dx*dx+dy*dy;
   if(d<distance){best=n;distance=d;}
  }
  return best;
 }
 /** Match original Android Score events only when graphical onset, staff, measure
  * and MIDI pitch uniquely agree. Tied continuations and polyphonic unisons
  * MUST stay unresolved; never silently assign an arbitrary voice.
  * This is a cross-parser diagnostic, not a replacement for Score.java.
  */
 function reconcile(notes,events){
  if(!Array.isArray(notes)||!Array.isArray(events))throw Error("Missing native/graphical events");
  const key=(beat,staff,measure,midi)=>
   Math.round(beat*1000)+":"+staff+":"+measure+":"+midi;
  const nativeByKey=new Map(),graphicalByKey=new Map();
  let invalidNative=0;
  for(let i=0;i<events.length;i++){
   const e=events[i];
   if(!e||![e.beat,e.midi,e.staff,e.measure,e.length].every(Number.isFinite)
       ||e.beat<0||e.length<0||!Number.isInteger(e.midi)
       ||!Number.isInteger(e.staff)||!Number.isInteger(e.measure)){
    invalidNative++;continue;
   }
   const k=key(e.beat,e.staff,e.measure,e.midi);
   if(!nativeByKey.has(k))nativeByKey.set(k,[]);
   nativeByKey.get(k).push({event:e,index:i});
  }
  for(const n of notes){
   if(!n)continue;
   delete n.nativeEvent;
   delete n.mappingReason;
   n.mappingStatus="unmatched";
   if(![n.beat,n.staff,n.measure,n.midiEstimate].every(Number.isFinite))continue;
   const k=key(n.beat,n.staff,n.measure,n.midiEstimate);
   if(!graphicalByKey.has(k))graphicalByKey.set(k,[]);
   graphicalByKey.get(k).push(n);
  }
  let matched=0,ambiguous=0;
  const reasons={invalidGraphical:0,noNativeCandidate:0,ambiguous:0,beatDisagreement:0};
  const samples=[];
  function flag(n,reason){
   n.mappingReason=reason;
   reasons[reason]++;
   // Small, bounded diagnostic; no source XML or user data logged.
   if(samples.length<20)samples.push({reason:reason,beat:n.beat,staff:n.staff,
    measure:n.measure,midiEstimate:n.midiEstimate});
  }
  for(const n of notes){if(n&&![n.beat,n.staff,n.measure,n.midiEstimate].every(Number.isFinite))
   flag(n,"invalidGraphical");}
  for(const [k,graphNotes] of graphicalByKey){
   const native=nativeByKey.get(k)||[];
   // Two source voices sharing a pitch are NOT interchangeable, even if
   // both appear at the same beat or only one head was engraved.
   if(graphNotes.length!==1||native.length!==1){
    if(graphNotes.length>1||native.length>1){
     for(const n of graphNotes){n.mappingStatus="ambiguous";flag(n,"ambiguous");}
     ambiguous+=graphNotes.length;
    }
    if(native.length===0&&graphNotes.length===1)flag(graphNotes[0],"noNativeCandidate");
    continue;
   }
   const n=graphNotes[0],e=native[0];
   if(Math.abs(n.beat-e.event.beat)>0.001){flag(n,"beatDisagreement");continue;}
   n.nativeEvent={index:e.index,beat:e.event.beat,midi:e.event.midi,
    staff:e.event.staff,measure:e.event.measure,voice:e.event.voice,
    length:e.event.length,tieStart:!!e.event.tieStart};
   n.mappingStatus="matched";
   matched++;
  }
  return {graphicalNotes:notes.length,nativeEvents:events.length,
   reasons:reasons,samples:samples,
   matched:matched,ambiguous:ambiguous,unmatched:notes.length-matched-ambiguous,
   unmatchedNative:events.length-invalidNative-matched,invalidNative:invalidNative};
 }
 const out={create:create,fromGraphical:fromGraphical,hitTest:hitTest,reconcile:reconcile};
 if(typeof module!=="undefined"&&module.exports)module.exports=out;
 if(root)root.OpticalTimeMap=out;
})(typeof window!=="undefined"?window:null);

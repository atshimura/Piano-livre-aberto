/* Piano Livre C2-B — local OSMD note-head selection, no external calls.
 * A graphical head is bound to an original MIDI attack ONLY if the
 * staff/pitch/beat intersection identifies exactly one source event.
 * Ambiguous unisons remain unselected; the accessible note list handles them.
 */
(function(root){
 "use strict";
 const NS="http://www.w3.org/2000/svg";
 function collect(graphic){
  const heads=[];
  if(!graphic||!Array.isArray(graphic.measureList))return heads;
  graphic.measureList.forEach((staves,m)=>{
   (staves||[]).forEach((measure,s)=>{
    if(!measure)return;
    (measure.staffEntries||[]).forEach(entry=>{
     const t=entry.getAbsoluteTimestamp&&entry.getAbsoluteTimestamp();
     if(!t||!Number.isFinite(t.RealValue))return;
     const beat=t.RealValue*4;
     (entry.graphicalVoiceEntries||[]).forEach(voice=>{
      (voice.notes||[]).forEach(g=>{
       const n=g.sourceNote,p=g.PositionAndShape&&g.PositionAndShape.AbsolutePosition;
       if(!n||n.isRestFlag||n.IsGraceNote||!p||!Number.isFinite(p.x)||!Number.isFinite(p.y)
          ||!Number.isFinite(n.halfTone))return;
       heads.push({x:p.x*10,y:p.y*10,beat:beat,midi:n.halfTone+12,staff:s+1,measure:m+1,id:null});
      });
     });
    });
   });
  });
  return heads;
 }
 function reconcile(heads,original){
  if(!Array.isArray(original))return {matched:0,ambiguous:0,missing:heads.length};
  let matched=0,ambiguous=0,missing=0;
  heads.forEach(h=>{
   // A written tie fragment may start after the source Note On.
   const at=Math.round(h.beat*960);
   const matches=original.filter(n=>n&&typeof n.id==="string"&&n.staff===h.staff
     &&n.pitch===h.midi&&n.start<=at+1&&n.end>at);
   if(matches.length===1){h.id=matches[0].id;matched++;}
   else if(matches.length>1)ambiguous++;
   else missing++;
  });
  return {matched:matched,ambiguous:ambiguous,missing:missing};
 }
 function hit(heads,x,y,rx,ry){
  const candidates=heads.filter(n=>Number.isFinite(n.x)&&Number.isFinite(n.y)
    &&Math.abs(x-n.x)<=rx&&Math.abs(y-n.y)<=ry).map(n=>({
     item:n,d:Math.pow((x-n.x)/rx,2)+Math.pow((y-n.y)/ry,2)
    })).sort((a,b)=>a.d-b.d);
  if(!candidates.length||candidates[0].d>1)return null;
  // Never guess when heads overlap or there is no unique MIDI mapping.
  if(candidates.length>1&&candidates[1].d-candidates[0].d<0.12
     &&candidates[0].item.id!==candidates[1].item.id)return null;
  return candidates[0].item.id||null;
 }
 function bind(svg,graphic,original,selected,onTap){
  if(!svg)return {matched:0,ambiguous:0,missing:0,dispose:function(){},select:function(){}};
  const heads=collect(graphic),stats=reconcile(heads,original);
  let chosen=new Set(selected||[]),start=null,lastTouch=-Infinity;
  function paint(){
   const former=svg.querySelector("#c2-selected-noteheads");
   if(former)former.remove();
   if(!chosen.size)return;
   const layer=document.createElementNS(NS,"g");layer.id="c2-selected-noteheads";
   layer.style.pointerEvents="none";
   heads.forEach(n=>{
    if(!n.id||!chosen.has(n.id))return;
    const circle=document.createElementNS(NS,"ellipse");
    circle.setAttribute("cx",String(n.x));circle.setAttribute("cy",String(n.y));
    circle.setAttribute("rx","11");circle.setAttribute("ry","9");
    circle.setAttribute("stroke","#286BCB");circle.setAttribute("stroke-width","2.3");
    circle.setAttribute("fill","none");layer.appendChild(circle);
   });
   svg.appendChild(layer);
  }
  function pick(ev){
   const rect=svg.getBoundingClientRect(),vb=svg.viewBox&&svg.viewBox.baseVal;
   if(!vb||rect.width<=0||rect.height<=0)return;
   const x=vb.x+(ev.clientX-rect.left)*vb.width/rect.width;
   const y=vb.y+(ev.clientY-rect.top)*vb.height/rect.height;
   const rx=Math.min(42,Math.max(9,14*vb.width/rect.width));
   const ry=Math.min(35,Math.max(7,12*vb.height/rect.height));
   const id=hit(heads,x,y,rx,ry);
   if(typeof id==="string"&&onTap)onTap(id);
  }
  function down(e){if(e.isPrimary&&e.pointerType==="touch")start={id:e.pointerId,x:e.clientX,y:e.clientY};}
  function up(e){
   const origin=start;start=null;
   if(!origin||origin.id!==e.pointerId||Math.hypot(e.clientX-origin.x,e.clientY-origin.y)>12)return;
   lastTouch=performance.now();pick(e);
  }
  function cancel(){start=null;}
  function click(e){if(performance.now()-lastTouch<700)return;pick(e);}
  svg.addEventListener("pointerdown",down);svg.addEventListener("pointerup",up);
  svg.addEventListener("pointercancel",cancel);svg.addEventListener("click",click);
  paint();
  return {matched:stats.matched,ambiguous:stats.ambiguous,missing:stats.missing,
   select:function(ids){chosen=new Set(ids||[]);paint();},
   dispose:function(){svg.removeEventListener("pointerdown",down);svg.removeEventListener("pointerup",up);
    svg.removeEventListener("pointercancel",cancel);svg.removeEventListener("click",click);
    const layer=svg.querySelector("#c2-selected-noteheads");if(layer)layer.remove();}
  };
 }
 const api={collect:collect,reconcile:reconcile,hit:hit,bind:bind};
 if(typeof module!=="undefined"&&module.exports)module.exports=api;
 root.CreationNotePicker=api;
})(typeof window!=="undefined"?window:typeof globalThis!=="undefined"?globalThis:this);

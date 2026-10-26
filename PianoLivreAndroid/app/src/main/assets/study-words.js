/* GPL-3.0-only. Reversible direction-word layout in the low Study viewport. */
(function(root){
 'use strict';
 function sourceNumbers(xml){
  const doc=new DOMParser().parseFromString(xml,'application/xml'),values=[];
  Array.from(doc.querySelectorAll('direction-type > words')).forEach(function(n){
   const value=(n.textContent||'').trim();
   if(/^[0-9]{1,2}$/.test(value)&&values.indexOf(value)<0)values.push(value);
  });
  return values;
 }
 function restore(svg){
  if(!svg)return;
  Array.from(svg.querySelectorAll('text[data-study-word-shift]')).forEach(function(node){
   const original=node.dataset.studyWordOriginalTransform;
   if(original===undefined)node.removeAttribute('transform');
   else node.setAttribute('transform',original);
   delete node.dataset.studyWordShift;
   delete node.dataset.studyWordOriginalTransform;
  });
 }
 function apply(svg,source,collect){
  restore(svg);
  if(!source||!source.length)return [];
  const candidates=Array.from(svg.querySelectorAll('g.vf-text > text')).filter(function(n){
   return source.indexOf((n.textContent||'').trim())>=0;
  });
  if(!candidates.length)return [];
  const staff=svg.querySelector('g.staffline'),first=staff&&staff.querySelector('g.vf-measure');
  const rule=first&&Array.from(first.children).find(function(n){return n.tagName.toLowerCase()==='path';});
  if(!rule)return [];
  const lineY=rule.getBBox().y,changed=[];
  function coincides(a,b){
   return Math.min(a.x+a.w,b.x+b.w)>Math.max(a.x,b.x)+1&&
    Math.min(a.y+a.h,b.y+b.h)>Math.max(a.y,b.y)+1;
  }
  // SVG filled slurs have a large conservative bbox, but a thin actual path.
  // Sample the painted path in LOCAL musical units, never just its full bbox.
  function curveTouches(path,rect){
   if(typeof path.getTotalLength!=='function'||typeof path.getPointAtLength!=='function')return true;
   const matrix=svg.getScreenCTM().inverse().multiply(path.getScreenCTM());
   const length=path.getTotalLength(),steps=Math.max(60,Math.min(600,Math.ceil(length/2)));
   for(let i=0;i<=steps;i++){
    const point=path.getPointAtLength(length*i/steps);
    const x=matrix.a*point.x+matrix.c*point.y+matrix.e;
    const y=matrix.b*point.x+matrix.d*point.y+matrix.f;
    if(x>=rect.x-1.5&&x<=rect.x+rect.w+1.5&&
       y>=rect.y-1.5&&y<=rect.y+rect.h+1.5)return true;
   }
   return false;
  }
  for(const node of candidates){
   const values=collect(),sourceBox=values.find(function(b){return b.node===node;});
   if(!sourceBox||sourceBox.y+sourceBox.h>lineY-20)continue;
   const original=node.getAttribute('transform');
   const maxDelta=Math.min(15,Math.floor(lineY-sourceBox.y-sourceBox.h-6));
   let accepted=0;
   const competitors=values.filter(function(b){
    return b.node!==node&&b.node&&
     !b.node.closest('#piano-optical-study-cues')&&
     !(b.kind==='vf-measure'&&b.tag==='path'&&b.w>100&&b.h<2);
   });
   for(let delta=maxDelta;delta>=5;delta--){
    node.setAttribute('transform',(original?original+' ':'')+'translate(0 '+delta+')');
    const moved=collect().find(function(b){return b.node===node;});
    if(!moved){continue;}
    const touches=competitors.some(function(other){
     if(!coincides(moved,other))return false;
     if(other.kind==='vf-curve'&&other.tag==='path')
      return curveTouches(other.node,moved);
     return true;
    });
    if(!touches){accepted=delta;break;}
   }
   if(accepted){
    node.setAttribute('transform',(original?original+' ':'')+'translate(0 '+accepted+')');
    node.dataset.studyWordShift=String(accepted);
    if(original!==null)node.dataset.studyWordOriginalTransform=original;
    changed.push({text:(node.textContent||'').trim(),delta:accepted,x:sourceBox.x,
     originalTop:sourceBox.y,source:'direction/words',introducedContacts:0});
   }else{
    if(original===null)node.removeAttribute('transform');
    else node.setAttribute('transform',original);
   }
  }
  return changed;
 }
 const api={sourceNumbers:sourceNumbers,restore:restore,apply:apply};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.StudyWords=api;
})(typeof window!=='undefined'?window:null);

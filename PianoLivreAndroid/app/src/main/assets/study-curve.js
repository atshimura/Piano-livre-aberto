/* GPL-3.0-only. Experimental editorial reshaping; never edits MusicXML. */
(function(root){
 'use strict';
 function flatten(d,factor){
  if(typeof d!=='string'||!(factor>0&&factor<=1))return null;
  const tokens=d.match(/[MLCZ]|[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?/g);
  if(!tokens||tokens.length!==24||[0,3,10,13,20,23].some(function(i,j){
   return tokens[i]!==['M','C','L','C','L','Z'][j];
  }))return null;
  const p=tokens.map(function(v){return Number.isFinite(+v)&&v!==''?+v:v;});
  const x0=p[1],y0=p[2],x3=p[8],y3=p[9];
  if(![x0,y0,x3,y3].every(Number.isFinite)||Math.abs(x3-x0)<1)return null;
  const motion=[];
  for(const [ix,iy] of [[4,5],[6,7]]){
   if(!Number.isFinite(p[ix])||!Number.isFinite(p[iy]))return null;
   const relative=(p[ix]-x0)/(x3-x0),straight=y0+relative*(y3-y0);
   motion.push((straight-p[iy])*(1-factor));
  }
  // Match the two filled outlines: reverse the control-point displacements
  // on the return cubic, keeping the slur thickness instead of scaling it.
  for(const [i,delta] of [[5,motion[0]],[7,motion[1]],[15,motion[1]],[17,motion[0]]])
   p[i]=+(p[i]+delta).toFixed(6);
  return 'M'+p.slice(1,3).join(' ')+'C'+p.slice(4,10).join(' ')+
   'L'+p.slice(11,13).join(' ')+'C'+p.slice(14,20).join(' ')+
   'L'+p.slice(21,23).join(' ')+'Z';
 }
 function restore(svg){
  if(!svg)return;
  svg.querySelectorAll('path[data-study-curve-original]').forEach(function(node){
   node.setAttribute('d',node.dataset.studyCurveOriginal);
   delete node.dataset.studyCurveOriginal;
  });
 }
 function trial(svg,factor){
  restore(svg);
  if(!(factor>0&&factor<1))return [];
  const results=[];
  Array.from(svg.querySelectorAll('g.vf-curve path')).forEach(function(node){
   const old=node.getAttribute('d'),before=node.getBBox();
   if(before.width<500||before.height<55)return; // Long arched slurs only.
   const candidate=flatten(old,factor);
   if(!candidate)return;
   node.dataset.studyCurveOriginal=old;
   node.setAttribute('d',candidate);
   const after=node.getBBox();
   results.push({x:before.x,width:before.width,beforeTop:before.y,
    afterTop:after.y,unchangedEndpoints:true});
  });
  return results;
 }
 // Diagnostic conservative slur-vs-ink contact sampling in SVG coordinates.
 // Check newly introduced contacts, allowing original endpoint attachments.
 function collisionReport(svg,boxes){
  const cubic=(a,b,c,d,t)=>{const u=1-t;return u*u*u*a+3*u*u*t*b+3*u*t*t*c+t*t*t*d;};
  function meets(d,box){
   const token=d.match(/[MLCZ]|[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?/g);
   if(!token||token.length!==24)return false;
   const v=token.map(Number),xmin=Math.min(v[1],v[8]),xmax=Math.max(v[1],v[8]);
   const a=Math.max(xmin,box.x),b=Math.min(xmax,box.x+box.w);
   if(b<a)return false;
   const count=Math.max(3,Math.min(21,Math.ceil((b-a)/5)));
   for(let i=0;i<=count;i++){
    const x=a+(b-a)*i/count,increasing=v[8]>v[1];
    let low=0,high=1;
    for(let j=0;j<25;j++){
     const t=(low+high)/2,px=cubic(v[1],v[4],v[6],v[8],t);
     if((px<x)===increasing)low=t;else high=t;
    }
    const t=(low+high)/2,upper=cubic(v[2],v[5],v[7],v[9],t),
     lower=cubic(v[12],v[15],v[17],v[19],1-t);
    if(Math.min(upper,lower)-.5<=box.y+box.h&&Math.max(upper,lower)+.5>=box.y)
     return true;
   }
   return false;
  }
  const candidates=Array.from(svg.querySelectorAll('path[data-study-curve-original]'));
  const blockers=boxes.filter(b=>b.node&&
   /vf-(stem|notehead|beam|accidental|text|stavetie|curve|dot|ornament|articulation|dynamic|line|modifier|clef|keysignature|timesignature|rest|barline)/.test(b.kind)&&
   getComputedStyle(b.node).visibility!=='hidden');
  const report=[];
  for(const curve of candidates){
   const current=curve.getAttribute('d'),before=curve.dataset.studyCurveOriginal,
    changed=[];
   const root=svg.getScreenCTM().inverse().multiply(curve.getScreenCTM());
   if(Math.abs(root.a-1)>.0001||Math.abs(root.d-1)>.0001||
      Math.abs(root.b)>.0001||Math.abs(root.c)>.0001||
      Math.abs(root.e)>.0001||Math.abs(root.f)>.0001){
    report.push({unsupportedTransform:true});continue;
   }
   for(const box of blockers){
    if(box.node===curve)continue;
    if(!meets(current,box)||meets(before,box))continue;
    changed.push({kind:box.kind,x:+box.x.toFixed(1),y:+box.y.toFixed(1),
     width:+box.w.toFixed(1),height:+box.h.toFixed(1)});
    if(changed.length>=12)break;
   }
   report.push({x:+curve.getBBox().x.toFixed(1),introducedContacts:changed.length,first:changed});
  }
  return report;
 }
 // Only commit a derived visual edit when it introduces no contact with
 // music engraving or expressive notation. Revert the entire SVG trial on
 // uncertainty: MusicXML, OSMD graph and note positions remain authoritative.
 function applySafe(svg,factor,boxes){
  const changed=trial(svg,factor);
  if(!changed.length)return {status:'not-applicable',accepted:false,edited:0};
  const contacts=collisionReport(svg,boxes);
  const rejected=contacts.filter(function(c){return c.unsupportedTransform||c.introducedContacts>0;});
  if(rejected.length){
   restore(svg);
   return {status:'rejected-new-contact',accepted:false,edited:changed.length,
    rejected:rejected.slice(0,6)};
  }
  return {status:'adapted',accepted:true,edited:changed.length,
   beforeTop:Math.min.apply(null,changed.map(c=>c.beforeTop)),
   afterTop:Math.min.apply(null,changed.map(c=>c.afterTop)),
   introducedContacts:0};
 }
 const api={flatten:flatten,restore:restore,trial:trial,collisionReport:collisionReport,applySafe:applySafe};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.StudyCurve=api;
})(typeof window!=='undefined'?window:null);

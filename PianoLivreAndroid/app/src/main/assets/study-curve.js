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
 const api={flatten:flatten,restore:restore,trial:trial};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.StudyCurve=api;
})(typeof window!=='undefined'?window:null);

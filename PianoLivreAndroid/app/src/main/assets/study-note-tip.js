/* GPL-3.0-only. Study note identification is presentation, not musical data. */
(function(root){
 'use strict';
 const NAMES={C:'C',D:'D',E:'E',F:'F',G:'G',A:'A',B:'B'};
 const ALTER={'-2':'𝄫','-1':'♭','0':'','1':'♯','2':'𝄪'};
 function international(event){
  if(!event||!Object.prototype.hasOwnProperty.call(NAMES,event.step)||
   !Number.isInteger(event.octave)||event.octave<0||event.octave>9||
   !Number.isInteger(event.alter)||Math.abs(event.alter)>2)return null;
  return NAMES[event.step]+ALTER[event.alter]+event.octave;
 }
 function label(event,portuguese){
  const ascii=international(event);
  return ascii&&portuguese?portuguese+' · '+ascii:null;
 }
 function placement(viewport,tip){
  // A temporary note badge belongs to the upper screen edge, never beneath
  // the touched head on the lower staff. Clamp to the WebView viewport.
  const width=Math.max(1,viewport.width),height=Math.max(1,viewport.height);
  const x=Math.max(4,Math.min(width-tip.width-4,width-tip.width-4));
  return {left:x,top:4,maxWidth:Math.max(1,width-8),
   maxHeight:Math.max(1,height-8)};
 }
 const api={international:international,label:label,placement:placement};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.StudyNoteTip=api;
})(typeof window!=='undefined'?window:null);

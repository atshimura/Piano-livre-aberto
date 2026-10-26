/* Explicit XML presentation metadata -> existing VexFlow modifiers BEFORE layout.
 * No XML rewrite, event mutation, custom glyphs, or inference from MIDI alone. */
(function(root){
 'use strict';
 const steps={0:'C',2:'D',4:'E',5:'F',7:'G',9:'A',11:'B'};
 function children(node,tag){return Array.from(node&&node.childNodes||[]).filter(n=>n.nodeType===1&&(n.localName||n.nodeName)===tag);}
 function child(node,tag){return children(node,tag)[0];}
 function text(node,tag,fallback){const n=child(node,tag);return n?n.textContent.trim():fallback;}
 function identity(beat,staff,measure,voice,step,alter,octave,midi){
  if(![beat,staff,measure,alter,octave,midi].every(Number.isFinite)||!step||voice===null||voice===undefined)return null;
  return [Math.round(beat*1e6),staff,measure,String(voice),step,alter,octave,midi].join(':');
 }
 function apply(doc,graphic,events){
  const parts=children(doc.documentElement,'part'),targets=new Map();
  let requested=0;
  for(const part of parts)for(const m of children(part,'measure'))for(const n of children(m,'note')){
   const a=child(n,'accidental');if(a&&a.getAttribute('parentheses')==='yes')requested++;
  }
  for(const e of events||[]){
   if(!Number.isInteger(e.sourcePart)||!Number.isInteger(e.measure)||!Number.isInteger(e.xmlNoteIndex))continue;
   const measure=children(parts[e.sourcePart],'measure')[e.measure-1];
   const note=measure&&measure.childNodes[e.xmlNoteIndex];
   if(!note||note.nodeType!==1||(note.localName||note.nodeName)!=='note')continue;
   const a=child(note,'accidental'),p=child(note,'pitch');
   if(!a||a.getAttribute('parentheses')!=='yes'||!p)continue;
   // The DOM index is authoritative only when it also agrees with the native
   // written identity. Do not attach a convention to a neighbouring XML note.
   if(text(p,'step','')!==e.step||Number(text(p,'alter','0'))!==e.alter||
      Number(text(p,'octave','-1'))!==e.octave||text(note,'voice','1')!==String(e.voice)||
      Number(text(note,'staff','1'))!==e.staff)continue;
   const key=identity(e.beat,e.staff,e.measure,e.voice,e.step,e.alter,e.octave,e.midi);
   if(key!==null)targets.set(key,targets.has(key)?null:e);
  }
  const changed=new Set(),resolved=new Set();
  (graphic.measureList||[]).forEach(function(staves,mi){(staves||[]).forEach(function(m,si){
   for(const entry of m&&m.staffEntries||[]){
    const timestamp=entry.getAbsoluteTimestamp&&entry.getAbsoluteTimestamp();if(!timestamp)continue;
    for(const voice of entry.graphicalVoiceEntries||[])for(const gn of voice.notes||[]){
     const src=gn.sourceNote,p=src&&src.Pitch;if(!p||src.IsGraceNote||src.isRestFlag)continue;
     const parent=voice.parentVoiceEntry&&voice.parentVoiceEntry.ParentVoice;
     const key=identity(timestamp.RealValue*4,si+1,mi+1,parent&&parent.VoiceId,
      steps[p.FundamentalNote],p.AccidentalHalfTones,p.Octave+3,src.halfTone+12);
     if(!targets.get(key))continue;
     const vf=gn.vfnote&&gn.vfnote[0],index=gn.vfnote&&gn.vfnote[1];
     if(!vf||!vf.getModifiers)continue;
     const modifiers=vf.getModifiers().filter(a=>a.getCategory&&a.getCategory()==='accidentals'&&a.index===index);
     if(modifiers.length!==1||typeof modifiers[0].setAsCautionary!=='function')continue;
     const modifier=modifiers[0];
     if(!changed.has(modifier)){modifier.setAsCautionary();changed.add(modifier);}
     resolved.add(key);
    }
   }
  });});
  return {requested:requested,applied:resolved.size,unresolved:requested-resolved.size};
 }
 const api={apply:apply};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.EngravingConventions=api;
})(typeof window!=='undefined'?window:null);

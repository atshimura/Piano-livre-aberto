/* Read-only evidence for authored task-06 fixtures, never a playback engine. */
(function(root){
 'use strict';
 function box(node){if(!node)return null;try{const b=node.getBBox();return [b.x,b.y,b.width,b.height];}catch(e){return null;}}
 function inspect(graphic){
  const rows=[];
  (graphic.measureList||[]).forEach(function(staves,mi){(staves||[]).forEach(function(m,si){
   if(!m)return;
   (m.staffEntries||[]).forEach(function(entry){
    const time=entry.getAbsoluteTimestamp();
    (entry.graphicalVoiceEntries||[]).forEach(function(voice){(voice.notes||[]).forEach(function(gn){
     const src=gn.sourceNote;if(!src||src.isRestFlag)return;
     const vf=gn.vfnote&&gn.vfnote[0],idx=gn.vfnote&&gn.vfnote[1];
     const modifiers=vf&&vf.getModifiers?vf.getModifiers():[];
     const pitch=src.Pitch;
     const tuplet=vf&&vf.tuplet;
     const group=gn.getSVGGElement&&gn.getSVGGElement();
     const head=gn.getNoteheadSVGs&&gn.getNoteheadSVGs();
     rows.push({measure:mi+1,staff:si+1,beat:time.RealValue*4,
      voice:String(voice.parentVoiceEntry.ParentVoice.VoiceId),
      grace:!!src.IsGraceNote,graceSlash:!!voice.GraceSlash,
      midi:src.halfTone+12,length:src.Length&&src.Length.RealValue*4,
      vfpitch:gn.vfpitch&&gn.vfpitch.slice(0,2),
      sourcePitch:pitch?{fundamental:pitch.FundamentalNote,octave:pitch.Octave,accidental:pitch.Accidental}:null,
      stem:vf&&vf.getStemDirection?vf.getStemDirection():null,
      stemBox:box(gn.getStemSVG&&gn.getStemSVG()),headBox:box(head&&head[idx||0]),
      groupBox:box(group),svgId:group&&group.id,
      modifiers:modifiers.map(function(mod){return {category:mod.getCategory&&mod.getCategory(),type:mod.type,index:mod.index,cautionary:mod.cautionary};}),
      beamNotes:vf&&vf.beam&&vf.beam.notes?vf.beam.notes.length:0,
      tuplet:tuplet?{count:tuplet.num_notes,occupied:tuplet.notes_occupied,
       bracket:tuplet.bracketed,notes:tuplet.notes&&tuplet.notes.length}:null});
    });});
   });
  });});
  return rows;
 }
 const api={inspect:inspect};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.EngravingAudit=api;
})(typeof window!=='undefined'?window:null);

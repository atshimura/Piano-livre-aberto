/* GPL-3.0-only. Derived presentation only; never edits the authoritative XML/events. */
(function(root){
 'use strict';
 const VERSION=2;
 let cachedXml=null,cached=null; // One exact-content entry, bounded by the import size limit.
 function count(node,tag){return node.getElementsByTagName(tag).length;}
 function wordKind(text){
  const tokens=text.trim().toLowerCase().split(/\s+/);
  if(tokens.length>=4&&tokens.every(function(t){return /^(do|dó|re|ré|mi|fa|fá|sol|la|lá|si)(#|b|♯|♭)?$/.test(t);}))return 'words:note-names';
  if(tokens.length>=4&&tokens.every(function(t){return /^[1-5]$/.test(t);}))return 'words:fingering-list';
  return null;
 }
 function isChordLabel(text){
  const tokens=String(text||'').trim().split(/\s+/).filter(Boolean);
  const chord=/^[A-G](?:#|b|♯|♭)?(?:m|min|maj|dim|aug|sus\d?|add\d?|\d+)?(?:\/[A-G](?:#|b|♯|♭)?)?$/i;
  return tokens.length>1&&tokens.every(function(token){return chord.test(token);});
 }
 function shouldCompactVertical(profile,height,staffSpace){
  return !!profile&&height<=240&&profile.staffCount===2&&
   (profile.notes>=200||profile.maxNotesPerMeasure>=16)&&staffSpace>0&&staffSpace<6;
 }
 function analyze(xml){
  if(xml===cachedXml&&cached)return cached;
  const doc=new DOMParser().parseFromString(xml,'application/xml');
  if(count(doc,'parsererror')||doc.documentElement.nodeName!=='score-partwise')throw Error('MusicXML inválido para análise');
  const features={};
  ['accidental','chord','tie','slur','beam','time-modification','repeat','lyric','words','harmony','fingering','rehearsal'].forEach(function(tag){features[tag]=count(doc,tag);});
  const measures=Array.from(doc.getElementsByTagName('measure')).map(function(m,index){
   const voices={},staves={};
   Array.from(m.getElementsByTagName('note')).forEach(function(n){
    const v=n.getElementsByTagName('voice')[0],s=n.getElementsByTagName('staff')[0];
    voices[v?v.textContent:'1']=true;staves[s?s.textContent:'1']=true;
   });
   return {index:index,number:m.getAttribute('number'),notes:count(m,'note'),symbols:m.getElementsByTagName('*').length,
    voices:Object.keys(voices),staves:Object.keys(staves)};
  });
  const staffCount=Array.from(doc.getElementsByTagName('part')).reduce(function(max,part){
   const staves=Array.from(part.getElementsByTagName('staff')).map(function(n){return Number(n.textContent)||1;});
   return Math.max(max,staves.length?Math.max.apply(null,staves):1);
  },0);
  const maxNotesPerMeasure=measures.reduce(function(max,measure){return Math.max(max,measure.notes);},0);
  const hiddenTypes=features.lyric?['lyric']:[];
  Array.from(doc.getElementsByTagName('words')).forEach(function(n){const kind=wordKind(n.textContent);if(kind&&hiddenTypes.indexOf(kind)<0)hiddenTypes.push(kind);});
  const profile={version:VERSION,features:features,measures:measures,notes:count(doc,'pitch'),staffCount:staffCount,
   maxNotesPerMeasure:maxNotesPerMeasure,
   // Lyrics are never required to identify a piano note. Retain words, figures,
   // harmony, dynamics, fingerings, tempo and repetition instructions.
   hiddenTypes:hiddenTypes,spacing:1};
  cachedXml=xml;cached=profile;return profile;
 }
 function presentation(xml,profile){
  // Printed page/system breaks are layout hints, not musical content.
  // A horizontal Study copy must not inherit them from the full reading view.
  const doc=new DOMParser().parseFromString(xml,'application/xml');
  Array.from(doc.getElementsByTagName('print')).forEach(function(n){n.removeAttribute('new-system');n.removeAttribute('new-page');});
  profile.hiddenTypes.forEach(function(tag){if(tag==='lyric')Array.from(doc.getElementsByTagName(tag)).forEach(function(n){n.parentNode.removeChild(n);});});
  Array.from(doc.getElementsByTagName('words')).forEach(function(n){if(profile.hiddenTypes.indexOf(wordKind(n.textContent))>=0)n.parentNode.removeChild(n);});
  return new XMLSerializer().serializeToString(doc);
 }
 function inspectSvg(svg){
  const vb=svg.viewBox.baseVal,ink=svg.getBBox(),rect=svg.getBoundingClientRect();
  const inkClipped=ink.x<vb.x-.5||ink.y<vb.y-.5||ink.x+ink.width>vb.x+vb.width+.5||ink.y+ink.height>vb.y+vb.height+.5;
  function tightBox(node){
   try{
    const b=node.getBBox(),m=node.getScreenCTM();
    if(!m)return node.getBoundingClientRect();
    const points=[[b.x,b.y],[b.x+b.width,b.y],[b.x,b.y+b.height],[b.x+b.width,b.y+b.height]].map(function(p){
     return {x:m.a*p[0]+m.c*p[1]+m.e,y:m.b*p[0]+m.d*p[1]+m.f};
    });
    const xs=points.map(function(p){return p.x;}),ys=points.map(function(p){return p.y;});
    return {left:Math.min.apply(null,xs),right:Math.max.apply(null,xs),top:Math.min.apply(null,ys),bottom:Math.max.apply(null,ys)};
   }catch(e){return node.getBoundingClientRect();}
  }
  const heads=Array.from(svg.querySelectorAll('.vf-notehead')).map(function(n){return {node:n,box:tightBox(n)};}).sort(function(a,b){return a.box.left-b.box.left;});
  const annotationOverlaps=[];
  Array.from(svg.querySelectorAll('text')).forEach(function(n,textIndex){
   if(n.style.visibility==='hidden'||n.getAttribute('visibility')==='hidden')return;
   const t=tightBox(n);
   for(const item of heads){const h=item.box;if(h.left>=t.right)break;
    const overlapX=Math.min(h.right,t.right)-Math.max(h.left,t.left),overlapY=Math.min(h.bottom,t.bottom)-Math.max(h.top,t.top);
    if(overlapX>.5&&overlapY>.5){
     annotationOverlaps.push({text:(n.textContent||'').trim(),textIndex:textIndex,chord:isChordLabel(n.textContent),
      noteId:item.node.id||null,overlapCss:[+overlapX.toFixed(3),+overlapY.toFixed(3)],
      textBox:[+t.left.toFixed(3),+t.top.toFixed(3),+(t.right-t.left).toFixed(3),+(t.bottom-t.top).toFixed(3)],
      noteBox:[+h.left.toFixed(3),+h.top.toFixed(3),+(h.right-h.left).toFixed(3),+(h.bottom-h.top).toFixed(3)]});
     break;
    }
   }
  });
  return {inkClipped:inkClipped,annotationOverlaps:annotationOverlaps.length,textNotePairs:annotationOverlaps,renderedTextCount:svg.querySelectorAll('text').length,
   bounds:[vb.x,vb.y,vb.width,vb.height],staffSpace:Number(svg.dataset.opticalStaffSpaceCss),height:rect.height};
 }
 function applyChordCollisionPolicy(svg){
  const hidden=[];
  if(!svg)return hidden;
  const geometry=inspectSvg(svg);
  const texts=Array.from(svg.querySelectorAll('text'));
  geometry.textNotePairs.forEach(function(pair){
   if(!pair.chord)return;
   const node=texts[pair.textIndex];
   if(!node||node.style.visibility==='hidden')return;
   node.style.visibility='hidden';node.setAttribute('data-study-hidden-reason','colisão geométrica texto/cabeça de nota');
   hidden.push({text:pair.text,reason:'colisão geométrica texto/cabeça de nota',overlapCss:pair.overlapCss});
  });
  return hidden;
 }
 function assess(rows,geometry){
  const alerts=[];let overlaps=0,clipped=0;
  const bounds=geometry.bounds;
  rows.forEach(function(r){const b=r.headBox;if(!b||!bounds)return;
   if(b[0]<bounds[0]-.5||b[1]<bounds[1]-.5||b[0]+b[2]>bounds[0]+bounds[2]+.5||b[1]+b[3]>bounds[1]+bounds[3]+.5)clipped++;
  });
  // Compare neighbouring attacks per staff; simultaneous voices/unisons are
  // intentional and excluded. Linear scan after sort, no quadratic corpus walk.
  const sorted=rows.filter(function(r){return r.headBox;}).slice().sort(function(a,b){return a.staff-b.staff||a.headBox[0]-b.headBox[0];});
  for(let i=1;i<sorted.length;i++){
   const a=sorted[i-1],b=sorted[i],x=a.headBox,y=b.headBox;
   if(a.staff===b.staff&&Math.abs(a.beat-b.beat)>1e-6&&Math.min(x[0]+x[2],y[0]+y[2])-Math.max(x[0],y[0])>.5&&Math.min(x[1]+x[3],y[1]+y[3])-Math.max(x[1],y[1])>.5)overlaps++;
  }
  if(clipped)alerts.push('notehead-clipping');
  if(geometry.inkClipped)alerts.push('symbol-envelope-clipping');
  if(geometry.annotationOverlaps)alerts.push('text-note-overlap');
  if(overlaps)alerts.push('adjacent-attack-overlap');
  if(geometry.staffSpace>0&&geometry.staffSpace<6)alerts.push('small-staff-space');
  if(geometry.mapping&&(geometry.mapping.unmatched||geometry.mapping.ambiguous||geometry.mapping.unmatchedNative))alerts.push('native-mapping');
  if(geometry.height>geometry.availableHeight+.5)alerts.push('vertical-overflow');
  return {alerts:alerts,clipped:clipped,overlaps:overlaps,annotationOverlaps:geometry.annotationOverlaps||0,textNotePairs:geometry.textNotePairs||[],inkClipped:!!geometry.inkClipped,renderedTextCount:geometry.renderedTextCount||0,staffSpace:geometry.staffSpace,
   suggestedSpacing:overlaps?1.5:1,limitations:['Bounding boxes are conservative; not glyph contours.','No independent musical reference; source errors cannot be certified.']};
 }
 function compare(rows,events){
  const bag=new Map();
  function signature(n){return JSON.stringify([Math.round(n.beat*1e6),Math.round(n.length*1e6),n.midi,n.staff,n.measure,n.step||n.writtenStep,n.octave===undefined?n.writtenOctave:n.octave]);}
  events.forEach(function(n){const key=signature(n);bag.set(key,(bag.get(key)||0)+1);});
  let unexpected=0;
  rows.filter(function(n){return !n.grace;}).forEach(function(n){const key=signature(n),left=bag.get(key)||0;if(left)bag.set(key,left-1);else unexpected++;});
  let missing=0;bag.forEach(function(value){missing+=value;});
  return {missing:missing,unexpected:unexpected};
 }
 function summary(profile,analysis){
  const names={'lyric':'letras de canto','words:note-names':'listas de solfejo','words:fingering-list':'listas de dedos'};
  const alerts={'small-staff-space':'pauta pequena','adjacent-attack-overlap':'possível sobreposição de notas','notehead-clipping':'possível corte de notas','symbol-envelope-clipping':'possível corte de símbolos','text-note-overlap':'texto sobre notas','vertical-overflow':'altura insuficiente','native-mapping':'correspondência musical incompleta'};
  let text=profile.hiddenTypes.length?'Estudo: '+profile.hiddenTypes.map(function(t){return names[t]||t;}).join(', ')+' ocultas. Consulte Partitura Livre. ':'';
  if(analysis.hiddenChordLabels&&analysis.hiddenChordLabels.length){
   text+='Cifras ocultadas por colisão geométrica: '+analysis.hiddenChordLabels.map(function(item){return item.text;}).join(', ')+'. Consulte Partitura Livre. ';
  }
  if(analysis.alerts.length)text+='Revisar leitura: '+analysis.alerts.map(function(t){return alerts[t]||t;}).join(', ')+'. A apresentação anterior continua disponível.';
  return text;
 }
 const api={applyChordCollisionPolicy:applyChordCollisionPolicy,compare:compare,summary:summary,inspectSvg:inspectSvg,isChordLabel:isChordLabel,shouldCompactVertical:shouldCompactVertical,wordKind:wordKind,version:VERSION,analyze:analyze,presentation:presentation,assess:assess};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.StudyProfile=api;
})(typeof window!=='undefined'?window:null);

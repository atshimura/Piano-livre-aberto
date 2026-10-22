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
 function compactStaffGap(svg,profile,viewportHeight){
  const result={status:'not-needed',accepted:false,delta:0};
  if(!svg||!profile||viewportHeight>240||profile.staffCount!==2||
     !(profile.notes>=200||profile.maxNotesPerMeasure>=16))return result;
  const lines=Array.from(svg.querySelectorAll('g.staffline'));
  let upper=null,lower=null;
  for(const line of lines){
   const id=line.id||'',match=id.match(/^(.+)-1$/);
   if(match){const other=lines.find(function(candidate){return candidate.id===match[1]+'-2';});if(other){upper=line;lower=other;break;}}
  }
  if(!upper||!lower)return Object.assign(result,{status:'unsupported-staff-layout'});
  const paths=function(group){return Array.from(group.children).find(function(n){return n.classList&&n.classList.contains('vf-measure');});};
  const firstUpper=paths(upper),firstLower=paths(lower);
  const staffPaths=function(measure){return measure?Array.from(measure.children).filter(function(n){return n.tagName&&n.tagName.toLowerCase()==='path';}).slice(0,5):[];};
  const topLines=staffPaths(firstUpper),bottomLines=staffPaths(firstLower);
  if(topLines.length<5||bottomLines.length<1)return Object.assign(result,{status:'unsupported-staff-geometry'});
  const box=function(node){
   try{const b=node.getBBox(),m=node.getScreenCTM();if(!m)return {x:b.x,y:b.y,w:b.width,h:b.height};
    const p=[[b.x,b.y],[b.x+b.width,b.y],[b.x,b.y+b.height],[b.x+b.width,b.y+b.height]].map(function(q){return {x:m.a*q[0]+m.c*q[1]+m.e,y:m.b*q[0]+m.d*q[1]+m.f};});
    const xs=p.map(function(q){return q.x;}),ys=p.map(function(q){return q.y;});return {x:Math.min.apply(null,xs),y:Math.min.apply(null,ys),w:Math.max.apply(null,xs)-Math.min.apply(null,xs),h:Math.max.apply(null,ys)-Math.min.apply(null,ys)};
   }catch(e){return null;}
  };
  const a=box(topLines[0]),b=box(topLines[1]),u=box(topLines[4]),l=box(bottomLines[0]);
  if(!a||!b||!u||!l)return Object.assign(result,{status:'unmeasurable-staff-geometry'});
  const screenLineSpace=Math.abs((b.y+b.h/2)-(a.y+a.h/2));
  const matrix=topLines[0].getScreenCTM();
  const unitScale=matrix?Math.sqrt(matrix.c*matrix.c+matrix.d*matrix.d):1;
  const lineSpace=screenLineSpace/Math.max(.0001,unitScale);
  const gap=((l.y+l.h/2)-(u.y+u.h/2))/Math.max(.0001,unitScale);
  const vb=svg.viewBox&&svg.viewBox.baseVal;
  const available=Math.max(1,viewportHeight-8),initialSpace=Number(svg.dataset.opticalStaffSpaceCss);
  result.beforeStaffSpace=initialSpace;result.staffGapSpaces=gap/lineSpace;
  if(!(initialSpace>0&&initialSpace<6))return Object.assign(result,{status:'not-needed'});
  // Leave a five-space optical moat between staves. OSMD's own engraving
  // geometry is retained inside each staff; the remaining reduction is
  // accepted only when primitive SVG bounds introduce no new cross-staff hit.
  const maximum=Math.max(0,gap-5*lineSpace);
  if(maximum<lineSpace)return Object.assign(result,{status:'insufficient-clearance'});
  function nodes(group){return Array.from(group.querySelectorAll('path,text,rect,circle,ellipse,line,polygon,polyline')).map(function(node,index){return {node:node,index:index,box:box(node)};}).filter(function(item){
   const q=item.box;if(!q||q.w<=.4||q.h<=.4)return false;
   // Long horizontal paths are the five staff rules. They intentionally pass
   // through every note and are not possible sources of a new collision.
   return !(item.node.tagName&&item.node.tagName.toLowerCase()==='path'&&q.w>lineSpace*20&&q.h<lineSpace*.2);
  });}
  const topNodes=nodes(upper);
  const originalTransform=lower.getAttribute('transform');
  function setDelta(delta){
   if(!delta){if(originalTransform===null)lower.removeAttribute('transform');else lower.setAttribute('transform',originalTransform);return;}
   lower.setAttribute('transform',(originalTransform?originalTransform+' ':'')+'translate(0 '+(-delta)+')');
  }
  function collisions(delta,collect,baselinePairs){
   setDelta(delta);
   const low=nodes(lower);
   const upperSorted=topNodes.slice().sort(function(a,b){return a.box.x-b.box.x;});
   const lowerSorted=low.slice().sort(function(a,b){return a.box.x-b.box.x;});
   let count=0,first=[],pairs=[];
   let lowerStart=0;
   for(let i=0;i<upperSorted.length;i++){
    const upperItem=upperSorted[i],a=upperItem.box;
    while(lowerStart<lowerSorted.length&&lowerSorted[lowerStart].box.x+lowerSorted[lowerStart].box.w<=a.x+.5)lowerStart++;
    for(let k=lowerStart;k<lowerSorted.length&&lowerSorted[k].box.x<a.x+a.w-.5;k++){
    const lowerItem=lowerSorted[k],b=lowerItem.box;
    // OSMD may draw the two parts of one vertical barline as separate thin
    // SVG shapes. Preserve their connection while the lower part moves; only
    // exempt a shared x-aligned pair where one segment spans the staff gap.
    const sharedBarline=a.w<screenLineSpace*1.5&&b.w<screenLineSpace*1.5&&
     Math.abs((a.x+a.w/2)-(b.x+b.w/2))<screenLineSpace*.5&&
     Math.max(a.h,b.h)>screenLineSpace*8&&Math.min(a.h,b.h)>screenLineSpace*1.4;
    if(sharedBarline)continue;
    const x=Math.min(a.x+a.w,b.x+b.w)-Math.max(a.x,b.x);
    if(x<=.5)continue;
    const y=Math.min(a.y+a.h,b.y+b.h)-Math.max(a.y,b.y);
    if(y>.5){count++;const key=upperItem.index+':'+lowerItem.index;pairs.push(key);if(collect&&first.length<8)first.push({upper:[a.x,a.y,a.w,a.h],lower:[b.x,b.y,b.w,b.h],overlap:[x,y],newPair:baselinePairs?baselinePairs.indexOf(key)<0:false});}
    }
   }
   return {count:count,first:first,pairs:pairs};
  }
  setDelta(0);
  const baselinePairs=collisions(0,false).pairs;
  let safe=0,unsafe=maximum;
  for(let i=0;i<12;i++){
   const candidate=(safe+unsafe)/2,collision=collisions(candidate,false).pairs;
   const introduces=collision.some(function(pair){return baselinePairs.indexOf(pair)<0;});
   if(!introduces)safe=candidate;else unsafe=candidate;
  }
  const finalCollision=collisions(safe,true,baselinePairs),afterInk=svg.getBBox(),afterHeight=Math.max(1,afterInk.height+8);
  const afterSpace=lineSpace*available/afterHeight;
  result.candidateDelta=safe;result.maxAllowedDelta=maximum;result.afterStaffSpace=afterSpace;
  result.newCrossStaffOverlaps=finalCollision.pairs.filter(function(pair){return baselinePairs.indexOf(pair)<0;}).length;result.existingCrossStaffOverlaps=baselinePairs.length;
  result.rejectedPairs=finalCollision.first;
  if(afterSpace<6||safe<lineSpace){setDelta(0);result.status='insufficient-safe-gain';return result;}
  setDelta(safe);
  lower.dataset.studyYOffset=String(safe);
  if(originalTransform!==null)lower.dataset.studyOriginalTransform=originalTransform;
  svg.dataset.studyOriginalViewBox=[vb.x,vb.y,vb.width,vb.height].join(' ');
  svg.dataset.studyOriginalNativeHeight=String(svg.dataset.opticalNativeHeight||'');
  svg.dataset.studyCompressed='true';svg.dataset.studyStaffShiftY=String(safe);
  svg.setAttribute('viewBox',[vb.x,afterInk.y-4,vb.width,afterHeight].join(' '));
  if(svg.dataset.opticalNativeHeight)svg.dataset.opticalNativeHeight=String(afterHeight);
  result.status='compacted';result.accepted=true;result.delta=safe;return result;
 }
 function restoreStaffGap(svg){
  if(!svg)return;
  const lower=svg.querySelector('[data-study-y-offset]');
  if(lower){const original=lower.dataset.studyOriginalTransform;
   if(original===undefined)lower.removeAttribute('transform');else lower.setAttribute('transform',original);
   delete lower.dataset.studyYOffset;delete lower.dataset.studyOriginalTransform;
  }
  if(svg.dataset.studyOriginalViewBox)svg.setAttribute('viewBox',svg.dataset.studyOriginalViewBox);
  if(svg.dataset.studyOriginalNativeHeight)svg.dataset.opticalNativeHeight=svg.dataset.studyOriginalNativeHeight;
  ['studyCompressed','studyStaffShiftY','studyOriginalViewBox','studyOriginalNativeHeight'].forEach(function(key){delete svg.dataset[key];});
 }
 function shiftEngravingRows(rows,staff,delta){
  return (rows||[]).map(function(row){if(row.staff!==staff)return row;
   const shifted=Object.assign({},row);
   ['headBox','stemBox','groupBox'].forEach(function(key){if(row[key]){shifted[key]=row[key].slice();shifted[key][1]-=delta;}});
   return shifted;
  });
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
 function presentationWithOptions(xml,profile,options){
  const presentationXml=presentation(xml,profile);
  if(!options||options.repositionDynamicsAbove!==true)return {xml:presentationXml,repositionedDynamics:0};
  const doc=new DOMParser().parseFromString(presentationXml,'application/xml');
  let repositionedDynamics=0;
  Array.from(doc.getElementsByTagName('direction')).forEach(function(direction){
   if(!direction.getElementsByTagName('dynamics').length)return;
   let changed=direction.getAttribute('placement')!=='above';
   if(changed)direction.setAttribute('placement','above');
   ['default-y','relative-y'].forEach(function(attribute){if(direction.hasAttribute(attribute)){direction.removeAttribute(attribute);changed=true;}});
   Array.from(direction.getElementsByTagName('dynamics')).forEach(function(node){
    ['default-y','relative-y'].forEach(function(attribute){if(node.hasAttribute(attribute)){node.removeAttribute(attribute);changed=true;}});
   });
   if(changed)repositionedDynamics++;
  });
  return {xml:new XMLSerializer().serializeToString(doc),repositionedDynamics:repositionedDynamics};
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
  function visibleTextBox(node){
   const fallback=tightBox(node),value=(node.textContent||'').trim();
   if(!value||typeof node.getStartPositionOfChar!=='function')return fallback;
   try{
    if(typeof node.getNumberOfChars==='function'&&typeof node.getExtentOfChar==='function'){
     let left=Infinity,top=Infinity,right=-Infinity,bottom=-Infinity;
     for(let i=0;i<node.getNumberOfChars();i++){
      const glyph=node.getExtentOfChar(i);
      if(glyph.width<=0||glyph.height<=0)continue;
      left=Math.min(left,glyph.x);top=Math.min(top,glyph.y);right=Math.max(right,glyph.x+glyph.width);bottom=Math.max(bottom,glyph.y+glyph.height);
     }
     const matrix=node.getScreenCTM();
     if(matrix&&[left,top,right,bottom].every(Number.isFinite)){
      const points=[[left,top],[right,top],[left,bottom],[right,bottom]].map(function(p){return {x:matrix.a*p[0]+matrix.c*p[1]+matrix.e,y:matrix.b*p[0]+matrix.d*p[1]+matrix.f};});
      const xs=points.map(function(p){return p.x;}),ys=points.map(function(p){return p.y;});
      return {left:Math.min.apply(null,xs),right:Math.max.apply(null,xs),top:Math.min.apply(null,ys),bottom:Math.max.apply(null,ys)};
     }
    }
    const canvas=document.createElement('canvas'),context=canvas.getContext('2d');
    if(!context)return fallback;
    context.font=getComputedStyle(node).font;context.textAlign='start';context.textBaseline='alphabetic';
    const metrics=context.measureText(value),position=node.getStartPositionOfChar(0),matrix=node.getScreenCTM();
    if(!matrix||![metrics.actualBoundingBoxLeft,metrics.actualBoundingBoxRight,metrics.actualBoundingBoxAscent,metrics.actualBoundingBoxDescent].every(Number.isFinite))return fallback;
    const local=[[position.x-metrics.actualBoundingBoxLeft,position.y-metrics.actualBoundingBoxAscent],
     [position.x+metrics.actualBoundingBoxRight,position.y-metrics.actualBoundingBoxAscent],
     [position.x-metrics.actualBoundingBoxLeft,position.y+metrics.actualBoundingBoxDescent],
     [position.x+metrics.actualBoundingBoxRight,position.y+metrics.actualBoundingBoxDescent]];
    const points=local.map(function(p){return {x:matrix.a*p[0]+matrix.c*p[1]+matrix.e,y:matrix.b*p[0]+matrix.d*p[1]+matrix.f};});
    const xs=points.map(function(p){return p.x;}),ys=points.map(function(p){return p.y;});
    return {left:Math.min.apply(null,xs),right:Math.max.apply(null,xs),top:Math.min.apply(null,ys),bottom:Math.max.apply(null,ys)};
   }catch(e){return fallback;}
  }
  const heads=Array.from(svg.querySelectorAll('.vf-notehead')).map(function(n){return {node:n,box:tightBox(n)};}).sort(function(a,b){return a.box.left-b.box.left;});
  const annotationOverlaps=[];
  Array.from(svg.querySelectorAll('text')).forEach(function(n,textIndex){
   if(n.style.visibility==='hidden'||n.getAttribute('visibility')==='hidden')return;
   const t=visibleTextBox(n);
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
  if(analysis.repositionedDynamics)text+='Dinâmicas reposicionadas acima na visualização estreita. ';
  if(analysis.alerts.length)text+='Revisar leitura: '+analysis.alerts.map(function(t){return alerts[t]||t;}).join(', ')+'. A apresentação anterior continua disponível.';
  return text;
 }
 const api={applyChordCollisionPolicy:applyChordCollisionPolicy,restoreStaffGap:restoreStaffGap,compare:compare,summary:summary,inspectSvg:inspectSvg,isChordLabel:isChordLabel,shouldCompactVertical:shouldCompactVertical,compactStaffGap:compactStaffGap,shiftEngravingRows:shiftEngravingRows,wordKind:wordKind,version:VERSION,analyze:analyze,presentation:presentation,presentationWithOptions:presentationWithOptions,assess:assess};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.StudyProfile=api;
})(typeof window!=='undefined'?window:null);

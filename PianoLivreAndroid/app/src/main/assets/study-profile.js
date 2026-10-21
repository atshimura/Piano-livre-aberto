/* GPL-3.0-only. Derived presentation only; never edits the authoritative XML/events. */
(function(root){
 'use strict';
 const VERSION=1;
 let cachedXml=null,cached=null; // One exact-content entry, bounded by the import size limit.
 function count(node,tag){return node.getElementsByTagName(tag).length;}
 function wordKind(text){
  const tokens=text.trim().toLowerCase().split(/\s+/);
  if(tokens.length>=4&&tokens.every(function(t){return /^(do|dó|re|ré|mi|fa|fá|sol|la|lá|si)(#|b|♯|♭)?$/.test(t);}))return 'words:note-names';
  if(tokens.length>=4&&tokens.every(function(t){return /^[1-5]$/.test(t);}))return 'words:fingering-list';
  return null;
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
  const hiddenTypes=features.lyric?['lyric']:[];
  Array.from(doc.getElementsByTagName('words')).forEach(function(n){const kind=wordKind(n.textContent);if(kind&&hiddenTypes.indexOf(kind)<0)hiddenTypes.push(kind);});
  const profile={version:VERSION,features:features,measures:measures,notes:count(doc,'pitch'),
   // Lyrics are never required to identify a piano note. Retain words, figures,
   // harmony, dynamics, fingerings, tempo and repetition instructions.
   hiddenTypes:hiddenTypes,spacing:1};
  cachedXml=xml;cached=profile;return profile;
 }
 function presentation(xml,profile){
  if(!profile.hiddenTypes.length)return xml;
  const doc=new DOMParser().parseFromString(xml,'application/xml');
  profile.hiddenTypes.forEach(function(tag){if(tag==='lyric')Array.from(doc.getElementsByTagName(tag)).forEach(function(n){n.parentNode.removeChild(n);});});
  Array.from(doc.getElementsByTagName('words')).forEach(function(n){if(profile.hiddenTypes.indexOf(wordKind(n.textContent))>=0)n.parentNode.removeChild(n);});
  return new XMLSerializer().serializeToString(doc);
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
  if(overlaps)alerts.push('adjacent-attack-overlap');
  if(geometry.staffSpace>0&&geometry.staffSpace<6)alerts.push('small-staff-space');
  if(geometry.mapping&&(geometry.mapping.unmatched||geometry.mapping.ambiguous))alerts.push('native-mapping');
  if(geometry.height>geometry.availableHeight+.5)alerts.push('vertical-overflow');
  return {alerts:alerts,clipped:clipped,overlaps:overlaps,staffSpace:geometry.staffSpace,
   suggestedSpacing:overlaps?1.5:1,limitations:['Bounding boxes are conservative; not glyph contours.','No independent musical reference; source errors cannot be certified.']};
 }
 const api={wordKind:wordKind,version:VERSION,analyze:analyze,presentation:presentation,assess:assess};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;
 if(root)root.StudyProfile=api;
})(typeof window!=='undefined'?window:null);

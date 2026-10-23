'use strict';
const fs=require('fs'),path=require('path'),assert=require('assert');
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||'playwright');
const corpus=[];
const StudyProfileWordKind=require('../app/src/main/assets/study-profile.js').wordKind;
for(const line of fs.readFileSync('build/study-corpus.tsv','utf8').trim().split('\n')){
 const v=line.split('\t');
 if(v[0]==='FILE')corpus.push({name:v[1],xml:Buffer.from(v[2],'base64').toString(),events:[]});
 if(v[0]==='NOTE')corpus[corpus.length-1].events.push({beat:+v[1],length:+v[2],midi:+v[3],staff:+v[4],measure:+v[5],voice:v[6],step:v[7],octave:+v[8],alter:+v[9],xmlNoteIndex:+v[10],sourcePart:+v[11]});
}
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox','--allow-file-access-from-files'],...(process.env.PIANO_CHROMIUM?{executablePath:process.env.PIANO_CHROMIUM}:{})});
 fs.mkdirSync('../docs/evidence/study-profile',{recursive:true});
 const remoteRequests=[];
 const checkoutSha=process.env.GITHUB_SHA||require('child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim();
 const sourceSha=process.env.PIANO_SOURCE_SHA||checkoutSha;
 const page=await browser.newPage({viewport:{width:738,height:220}}),reports=[],visualComparison=[];
 await page.route(/^https?:/,route=>{remoteRequests.push(route.request().url());return route.abort();});
 await page.goto('file://'+path.resolve('app/src/main/assets/notation.html'));
 await page.evaluate(()=>{window.AndroidNotation={onReady(){},onRendered(){window.testDone=true;},onPresentation(m){window.presentationMessage=m;},onError(m){window.testError=m;}};});
 for(const item of corpus){
  await page.setViewportSize({width:738,height:220});
  const report=await page.evaluate(async item=>{
   const source=StudyProfile.analyze(item.xml),same=StudyProfile.analyze(item.xml)===source;
   const changed=StudyProfile.analyze(item.xml+'\n')!==source;
   const rendered=StudyProfile.presentation(item.xml,source);
   const musical=xml=>{const d=new DOMParser().parseFromString(xml,'application/xml');return Array.from(d.querySelectorAll('note')).map(n=>{const c=n.cloneNode(true);c.querySelectorAll('lyric').forEach(l=>l.remove());return new XMLSerializer().serializeToString(c);});};
   const intact=JSON.stringify(musical(item.xml))===JSON.stringify(musical(rendered));
   window.testDone=false;window.testError=null;
   PianoNotation.load(item.xml,0,'engraving-poc',item.events);
   for(let i=0;i<300&&!window.testDone&&!window.testError;i++)await new Promise(r=>setTimeout(r,50));
  const analysis=PianoNotation.analysis();
  const svg=document.querySelector('#score svg');
  const targetPairs=(analysis&&analysis.geometry&&analysis.geometry.textNotePairs||[]).map(pair=>{
    const rect=svg.getBoundingClientRect(),vb=svg.viewBox.baseVal;
    const rawX=vb.x+(pair.noteBox[0]+pair.noteBox[2]/2-rect.left)*vb.width/rect.width;
    const rawY=vb.y+(pair.noteBox[1]+pair.noteBox[3]/2-rect.top)*vb.height/rect.height;
    const note=analysis.notes.filter(n=>n.headBox).map(n=>({n,d:Math.hypot(n.headBox[0]+n.headBox[2]/2-rawX,n.headBox[1]+n.headBox[3]/2-rawY)})).sort((a,b)=>a.d-b.d)[0];
    return Object.assign({},pair,{nearbyNote:note?{beat:note.n.beat,midi:note.n.midi,step:note.n.step||note.n.writtenStep,octave:note.n.octave===undefined?note.n.writtenOctave:note.n.octave,staff:note.n.staff,measure:note.n.measure,geometryDistanceSvg:note.d}:null});
  });
  const chordLabels=svg?Array.from(svg.querySelectorAll('text')).map(n=>({text:(n.textContent||'').trim(),hidden:n.style.visibility==='hidden'})).filter(n=>StudyProfile.isChordLabel(n.text)):[];
  return {same,changed,intact,error:window.testError,done:window.testDone,analysis:analysis,textNotePairs:targetPairs,
    renderer:svg?'OSMD SVG':'none',
    chordLabels:chordLabels,message:window.presentationMessage};
  },item);
  if(item.name.endsWith('fur-elise-easy-ver.mxl')){
   const target=report.analysis.notes.find(n=>n.measure===13&&n.beat===36&&n.midi===56&&n.staff===2);
   assert(target,'Für Elise regression note G-sharp 3 at beat 36 must be present');
   const captureDynamic=async(disabled,suffix)=>{
    await page.evaluate(({item,disabled})=>{
     window.__testDisableDynamicCollisionPolicy=disabled;window.testDone=false;window.testError=null;
     PianoNotation.load(item.xml,0,'engraving-poc',item.events);
    },{item,disabled});
    await page.waitForFunction(()=>window.testDone||window.testError,null,{timeout:30000});
    const visible=await page.evaluate(()=>{
     const svg=document.querySelector('#score svg'),text=Array.from(svg.querySelectorAll('text')).find(n=>n.textContent.trim()==='p');
     if(!text)throw Error('Für Elise dynamic p missing from active OSMD SVG');
     const v=document.getElementById('viewport'),r=text.getBoundingClientRect(),x=v.scrollLeft+r.left+r.width/2;
     v.scrollLeft=Math.max(0,x-100);
     return {centerXCss:x,pairs:StudyProfile.inspectSvg(svg).textNotePairs,adjustments:PianoNotation.analysis().geometry.dynamicRepositionDetails||[]};
    });
    await page.screenshot({path:'../docs/evidence/study-profile/fur-elise-dynamic-beat-36.'+suffix+'.png'});
    return visible;
   };
   const beforeDynamic=captureDynamic?await captureDynamic(true,'before'):null;
   const afterDynamic=await captureDynamic(false,'after');
   assert(beforeDynamic.pairs.some(p=>p.text==='p'),'Für Elise baseline must reproduce the text/note collision geometrically');
   assert(afterDynamic.adjustments.some(a=>a.text==='p'),'Study view must reposition the colliding dynamic, preserving the indication: '+JSON.stringify({beforeDynamic,afterDynamic,adjustments:report.analysis.geometry.dynamicRepositionDetails}));
   assert(!afterDynamic.pairs.some(p=>p.text==='p'),'repositioned dynamic must clear the notehead');
   assert(!report.analysis.geometry.alerts.includes('text-note-overlap'),'Für Elise dynamic collision is corrected in Study view');
   assert(report.analysis.geometry.dynamicRepositionDetails.some(a=>a.text==='p'),'analysis must record the Study-only dynamic adjustment');
   const glyphDiagnostics=await page.evaluate(()=>{
    const text=Array.from(document.querySelectorAll('#score svg text')).find(n=>n.textContent.trim()==='p'),canvas=document.createElement('canvas'),ctx=canvas.getContext('2d');
    let extent=null,range=null;
    try{const b=text.getExtentOfChar(0);extent=[b.x,b.y,b.width,b.height];}catch(e){extent={error:e.name+': '+e.message};}
    try{const r=document.createRange();r.selectNodeContents(text);range=Array.from(r.getClientRects()).map(b=>[b.x,b.y,b.width,b.height]);}catch(e){range={error:e.name+': '+e.message};}
    ctx.font=getComputedStyle(text).font;const m=ctx.measureText(text.textContent.trim());
    return {font:ctx.font,bbox:(()=>{const b=text.getBBox();return [b.x,b.y,b.width,b.height];})(),extent,range,
      charCount:text.getNumberOfChars(),start:(()=>{const p=text.getStartPositionOfChar(0);return [p.x,p.y];})(),canvas:{left:m.actualBoundingBoxLeft,right:m.actualBoundingBoxRight,ascent:m.actualBoundingBoxAscent,descent:m.actualBoundingBoxDescent}};
   });
   console.log('STUDY_TEXT_GLYPH_DIAGNOSTICS '+JSON.stringify(glyphDiagnostics));
   const textX=await page.evaluate(()=>{
    const svg=document.querySelector('#score svg'),v=document.getElementById('viewport'),text=Array.from(svg.querySelectorAll('text')).find(n=>n.textContent.trim()==='p');
    if(!text||text.textContent.trim()!=='p')throw Error('Für Elise beat-36 dynamic annotation not found');
    const r=text.getBoundingClientRect(),x=v.scrollLeft+r.left+r.width/2;
    v.scrollLeft=Math.max(0,x-100);return x;
   });
   console.log('STUDY_FUR_ELISE_DYNAMIC_NOTE '+JSON.stringify({text:'p',decision:'preserved and moved upward in Study presentation after reproducible glyph collision',beat:target.beat,midi:target.midi,step:target.step||target.writtenStep,octave:target.octave===undefined?target.writtenOctave:target.octave,staff:target.staff,measure:target.measure,centerXCss:textX,beforePairs:beforeDynamic.pairs,afterPairs:afterDynamic.pairs,adjustments:afterDynamic.adjustments,headBox:target.headBox}));
  }
  if(/(canon-in-d|fur-elise-easy-ver|gymnopedie-no-1-satie)\.mxl$/.test(item.name))
   await page.screenshot({path:'../docs/evidence/study-profile/'+path.basename(item.name)+'.738x220.initial.png'});
  try{
  assert(report.same&&report.changed&&report.intact,item.name+' content invalidation/source preservation');
  console.log('STUDY_CORPUS_FILE '+item.name);
  if(report.error&&item.name.startsWith('tests/fixtures/motor/')){
   // Parser-only fixtures can omit engraving metadata. The actual OSMD error
   // must be explicit, and the retained temporal alternative must remain faithful.
   const fallback=await page.evaluate(async item=>{
    window.testDone=false;window.testError=null;PianoNotation.load(item.xml,0,'study');
    const plan=TemporalNotation.parse(item.xml),steps={C:0,D:2,E:4,F:5,G:7,A:9,B:11};
    return {done:window.testDone,error:window.testError,notes:plan.notes.filter(n=>!n.grace).map(n=>({beat:n.at,length:n.dur,midi:12*(n.oct+1)+steps[n.step]+n.alter,staff:n.staff,step:n.step,octave:n.oct}))};
   },item);
   assert(fallback.done&&!fallback.error,item.name+' temporal alternative: '+fallback.error);
   const sig=e=>JSON.stringify([Math.round(e.beat*1e6),Math.round(e.length*1e6),e.midi,e.staff,e.step,e.octave]);
   assert.deepEqual(fallback.notes.map(sig).sort(),item.events.map(sig).sort(),item.name+' faithful fallback');
   reports.push({name:item.name,renderer:'temporal',osmdError:report.error,notes:item.events.length});
   continue;
  }
  assert(report.done&&!report.error,item.name+' OSMD: '+report.error);
  assert.equal(report.renderer,'OSMD SVG',item.name+' must be checked through the renderer that actually produced the image');
  if(item.name.endsWith('canon-in-d.mxl')){
   assert(report.analysis.profile.hiddenTypes.includes('words:note-names'),'Canon note-name lists are classified as auxiliary');
   assert(report.analysis.profile.hiddenTypes.includes('words:fingering-list'),'Canon fingering lists are classified as auxiliary');
   assert(report.chordLabels.length>0,'Canon chord labels remain in the rendered Study SVG');
  }
  if(item.name.endsWith('fur-elise-easy-ver.mxl'))
   assert(!report.analysis.geometry.alerts.includes('text-note-overlap'),
    'Für Elise text/note warning must be supported by actual glyph geometry: '+JSON.stringify(report.analysis.geometry.textNotePairs));
  const graceCount=report.analysis.notes.filter(n=>n.grace).length;
  assert.equal(report.analysis.profile.notes,item.events.length+graceCount,item.name+' main and grace pitch count');
  const graphic=report.analysis.notes.filter(n=>!n.grace);
  assert.equal(graphic.length,item.events.length,item.name+' graphical note count');
  if(item.name.endsWith('named-voices-ties.xml')){
   const map=report.analysis.mapping;
   assert.equal(map.graphicalNotes,4,'all tied unisons are engraved');
   assert.equal(map.nativeEvents,4,'both named native voices and continuations exist');
   assert.equal(map.matched,0,'unknown graphical voice IDs cannot be guessed');
   assert.equal(map.reasons.ambiguous,4,'ambiguous homophonic heads must be flagged');
   assert.equal(map.unmatchedNative,4,'unverified voices remain unassigned');
   assert(report.analysis.geometry.alerts.includes('native-mapping'),
    'do not hide ambiguous mapping warning');
   assert.deepEqual([...new Set(item.events.map(e=>e.voice))].sort(),['lower','upper'],
    'fixture explicitly has two distinct named voices');
   // The temporal parser is a separately checked recovery path, not a
   // fabricated one-to-one assignment to OSMD's indistinguishable heads.
   const temporal=await page.evaluate(xml=>TemporalNotation.parse(xml).notes
    .filter(n=>!n.grace).map(n=>({beat:n.at,length:n.dur,
     midi:12*(n.oct+1)+({C:0,D:2,E:4,F:5,G:7,A:9,B:11})[n.step]+n.alter,
     staff:n.staff})),item.xml);
   const ts=e=>JSON.stringify([Math.round(e.beat*1e6),Math.round(e.length*1e6),e.midi,e.staff]);
   assert.deepEqual(temporal.map(ts).sort(),item.events.map(ts).sort(),
    'temporal alternative must preserve every named-voice onset and tie');
  }
  const signature=e=>JSON.stringify([Math.round(e.beat*1e6),Math.round(e.length*1e6),e.midi,e.staff,e.measure,e.step||e.writtenStep,e.octave===undefined?e.writtenOctave:e.octave]);
  assert.deepEqual(graphic.map(signature).sort(),item.events.map(signature).sort(),item.name+' complete pitch/octave/duration multiset');
  const dimensions=[];
  for(const size of [{width:738,height:196},{width:738,height:351},{width:883,height:409}]){
   await page.setViewportSize(size);
   // Fresh renders isolate each viewport; never report the state before the
   // debounced width re-render completes. Dynamic resize is tested separately.
   await page.evaluate(item=>{window.testDone=false;window.testError=null;PianoNotation.load(item.xml,0,'engraving-poc',item.events);},item);
   await page.waitForFunction(()=>window.testDone||window.testError,null,{timeout:30000});
   const measured=await page.evaluate(()=>{
    const svg=document.querySelector('#score svg'),v=document.getElementById('viewport'),a=PianoNotation.analysis();
    v.scrollTop=100;
    return {error:window.testError,geometry:a.geometry,mapping:a.mapping,layoutAdaptation:a.layoutAdaptation,
     notes:a.notes,scrollTop:v.scrollTop,availableHeight:+svg.dataset.opticalAvailableHeight,renderedHeight:svg.getBoundingClientRect().height,
     texts:Array.from(svg.querySelectorAll('text')).map(n=>({text:n.textContent.trim(),hidden:n.style.visibility==='hidden'}))};
   });
   assert(!measured.error,item.name+' viewport render: '+measured.error);
   const fitted=measured.geometry;
   assert.deepEqual(fitted.viewport,size,item.name+' measurements must belong to the requested viewport');
   assert.deepEqual(measured.notes.filter(n=>!n.grace).map(signature).sort(),item.events.map(signature).sort(),item.name+' viewport musical fidelity');
   assert.equal(measured.scrollTop,0,item.name+' no vertical scroll');
   assert(!fitted.inkClipped&&fitted.clipped===0,item.name+' complete visible ink');
   assert(!fitted.alerts.includes('vertical-overflow'),item.name+' vertical fit '+JSON.stringify(size));
   if(item.name.endsWith('canon-in-d.mxl')){
    assert(measured.texts.some(n=>/D A Bm F#m/.test(n.text.replace(/\s+/g,' '))&&!n.hidden),'Canon chords must remain visible at each viewport');
    assert(!measured.texts.some(n=>StudyProfileWordKind(n.text)&&!n.hidden),'Canon auxiliary lists must not be rendered');
    assert.equal(fitted.hiddenChordLabels.length,0,'Canon has no measured chord collision');
   }
   if(item.name.endsWith('fur-elise-easy-ver.mxl')){
    assert(measured.texts.some(n=>n.text==='p'&&!n.hidden),'Für Elise dynamic must remain visible');
    assert(!fitted.textNotePairs.some(p=>p.text==='p'),'Für Elise measured dynamic/note clearance in each viewport');
   }
   if(/(canon-in-d|fur-elise-easy-ver|gymnopedie-no-1-satie)\.mxl$/.test(item.name))await page.screenshot({path:'../docs/evidence/study-profile/'+path.basename(item.name)+'.'+size.width+'x'+size.height+'.png'});
   dimensions.push({size,renderer:'OSMD SVG',geometry:fitted,mapping:measured.mapping,layoutAdaptation:measured.layoutAdaptation,availableHeight:measured.availableHeight,renderedHeight:measured.renderedHeight,texts:measured.texts});
  }
  reports.push({dimensions,name:item.name,notes:item.events.length,renderer:report.renderer,profile:report.analysis.profile,geometry:report.analysis.geometry,mapping:report.analysis.mapping,layoutAdaptation:report.analysis.layoutAdaptation});
  fs.writeFileSync('../docs/evidence/study-profile/corpus.json',JSON.stringify({sourceSha,checkoutSha,scope:'Desktop Chromium, not Android or physical acceptance',reports,visualComparison},null,2));
  }catch(error){reports.push({name:item.name,renderer:report.renderer,profile:report.analysis&&report.analysis.profile,geometry:report.analysis&&report.analysis.geometry,mapping:report.analysis&&report.analysis.mapping,layoutAdaptation:report.analysis&&report.analysis.layoutAdaptation,failure:error.message});fs.writeFileSync('../docs/evidence/study-profile/corpus.json',JSON.stringify({sourceSha,checkoutSha,scope:'Desktop Chromium, not Android or physical acceptance',reports,visualComparison},null,2));console.error('STUDY_CORPUS_FAILURE '+error.message);}
 }
 // Reproducible same-build comparison of the geometric staff-gap compaction
 // on three representative viewport sizes. Baseline uses stock OSMD engraving;
 // adapted moves the lower staff only when primitive bounds introduce no new
 // cross-staff intersections, then refits the complete SVG ink envelope.
 const gym=corpus.find(item=>item.name.endsWith('gymnopedie-no-1-satie.mxl'));
 assert(gym,'Gymnopédie source fixture is required for before/after evidence');
 for(const size of [{width:738,height:196},{width:738,height:351},{width:883,height:409}]){
  await page.setViewportSize(size);
  for(const disabled of [true,false]){
   await page.evaluate(({item,disabled})=>{
    window.testDone=false;window.testError=null;window.__testDisableStudyVerticalAdaptation=disabled;
    PianoNotation.load(item.xml,0,'engraving-poc',item.events);
   },{item:gym,disabled});
   await page.waitForFunction(()=>window.testDone||window.testError,null,{timeout:30000});
   const row=await page.evaluate(()=>({error:window.testError,analysis:PianoNotation.analysis()}));
   assert(!row.error,'Gymnopédie comparison render: '+row.error);
   const phase=disabled?'before':'after';
   await page.screenshot({path:'../docs/evidence/study-profile/gymnopedie.'+phase+'.'+size.width+'x'+size.height+'.png'});
   visualComparison.push({phase,size,staffSpaceCssPx:row.analysis.geometry.staffSpace,
    rawSvgHeightCssPx:row.analysis.layoutAdaptation.beforeHeight||row.analysis.geometry.height,
    viewportHeightCssPx:size.height,verticalOverflow:row.analysis.geometry.alerts.includes('vertical-overflow'),
    hiddenChordLabels:row.analysis.geometry.hiddenChordLabels||[],adaptation:row.analysis.layoutAdaptation});
   if(!disabled&&size.height===196){
    assert(row.analysis.geometry.staffSpace>=5,'Gymnopédie low viewport opening must reach at least 5 CSS px without clipping: '+JSON.stringify(row.analysis.layoutAdaptation));
    assert(!row.analysis.geometry.alerts.includes('vertical-overflow'),'Gymnopédie remains fully visible without vertical scroll');
   }
  }
 }
 await page.evaluate(()=>{window.__testDisableStudyVerticalAdaptation=false;});
 // Traverse every authored measure in both directions. Independently measure
 // actual screen boxes, including stems and long slurs, not just the planner.
 await page.setViewportSize({width:738,height:196});
 await page.evaluate(item=>{window.__testDisableStudyVerticalAdaptation=false;window.testDone=false;PianoNotation.load(item.xml,0,'engraving-poc',item.events);},gym);
 await page.waitForFunction(()=>window.testDone,null,{timeout:30000});
 const beats=Array.from(new Set(gym.events.map(n=>n.beat))).sort((a,b)=>a-b);
 const traversal=await page.evaluate(beats=>{
  const samples=[];
  for(const beat of beats.concat(beats.slice().reverse())){
   PianoNotation.seek(beat,false,100,false);
   const a=PianoNotation.analysis(),svg=document.querySelector('#score svg'),v=document.getElementById('viewport'),vp=v.getBoundingClientRect();
   const clipped=[];
   for(const n of Array.from(svg.querySelectorAll('path,text,rect,circle,ellipse,line,polygon,polyline'))){
    if(getComputedStyle(n).visibility==='hidden'||n.closest('#piano-optical-study-cues'))continue;
    const b=n.getBoundingClientRect();
    if(b.right>vp.left&&b.left<vp.right&&(b.top<vp.top-.5||b.bottom>vp.bottom+.5))clipped.push({tag:n.tagName,id:n.id,text:n.textContent,box:[b.left,b.top,b.width,b.height]});
   }
   if(clipped.length)throw Error('Visible ink clipped at beat '+beat+': '+JSON.stringify(clipped.slice(0,3)));
   if(a.geometry.window&&a.geometry.window.clippedPrimitives)throw Error('Window diagnostic clipping');
   if(a.mapping.matched!==469)throw Error('Gymnopédie mapping changed');
   samples.push({beat,staffSpace:a.geometry.staffSpace,window:a.geometry.window});
  }
  return samples;
 },beats);
 const forward=traversal.slice(0,beats.length),transitions=[];
 for(let i=1;i<forward.length;i++){
  const before=forward[i-1],after=forward[i];
  if(before.window&&after.window&&before.window.index!==after.window.index){
   transitions.push({fromBeat:before.beat,toBeat:after.beat,
    fromWindow:before.window.index,toWindow:after.window.index,
    fromStaffSpace:before.staffSpace,toStaffSpace:after.staffSpace,
    changePercent:100*(after.staffSpace/before.staffSpace-1)});
  }
 }
 const largestScaleStep=transitions.reduce((a,b)=>Math.abs(a.changePercent)>=Math.abs(b.changePercent)?a:b,
  {fromBeat:null,toBeat:null,changePercent:0});
 const adjacent=forward.slice(1).map((row,i)=>({
  fromBeat:forward[i].beat,toBeat:row.beat,
  fromStaffSpace:forward[i].staffSpace,toStaffSpace:row.staffSpace,
  changePercent:100*(row.staffSpace/forward[i].staffSpace-1)}));
 const largestAdjacentChange=adjacent.reduce((a,b)=>
  Math.abs(a.changePercent)>=Math.abs(b.changePercent)?a:b,
  {fromBeat:null,toBeat:null,changePercent:0});
 const windowDiagnostic={samples:forward.length,transitions:transitions.length,largestScaleStep,
  largestAdjacentChange,
  minStaffSpace:Math.min(...forward.map(row=>row.staffSpace)),
  maxStaffSpace:Math.max(...forward.map(row=>row.staffSpace))};
 console.log('STUDY_WINDOW_TRANSITIONS '+JSON.stringify(windowDiagnostic));
 fs.writeFileSync('../docs/evidence/study-profile/window-traversal.json',
  JSON.stringify({sourceSha,checkoutSha,diagnostic:windowDiagnostic,transitions,samples:traversal},null,2));
 // Capture both extremes after the browser has painted, not just synchronous
 // seeks; keep the smallest result visible in the evidence as well.
 for(const [label,sample] of [['smallest',traversal.reduce((a,b)=>a.staffSpace<b.staffSpace?a:b)],
                             ['largest',traversal.reduce((a,b)=>a.staffSpace>b.staffSpace?a:b)]]){
  await page.evaluate(async beat=>{PianoNotation.seek(beat,false,100,false);await new Promise(requestAnimationFrame);await new Promise(requestAnimationFrame);},sample.beat);
  const live=await page.evaluate(()=>({space:PianoNotation.analysis().geometry.staffSpace,scrollTop:document.getElementById('viewport').scrollTop}));
  assert(Math.abs(live.space-sample.staffSpace)<.01,'painted window matches measured scale');
  assert.equal(live.scrollTop,0,'window must not introduce vertical scrolling');
  await page.screenshot({path:'../docs/evidence/study-profile/gymnopedie.window-'+label+'.738x196.png'});
 }

 // Comparative B1 captures: opening, middle, first extreme and final
 // extreme for each requested device-like CSS viewport. A screenshot is
 // only evidence of Chromium geometry, never physical readability.
 const b1Views=[],positions=[['opening',0],['middle',110],['extreme',181],['ending',223]];
 for(const size of [{width:738,height:196},{width:738,height:351},{width:883,height:409}]){
  await page.setViewportSize(size);
  await page.evaluate(item=>{window.testDone=false;window.testError=null;
   PianoNotation.load(item.xml,0,'engraving-poc',item.events);},gym);
  await page.waitForFunction(()=>window.testDone||window.testError,null,{timeout:30000});
  for(const [label,beat] of positions){
   await page.evaluate(async t=>{PianoNotation.seek(t,false,100,false);
    await new Promise(requestAnimationFrame);await new Promise(requestAnimationFrame);},beat);
   const row=await page.evaluate(()=>{
    const v=document.getElementById('viewport'),svg=document.querySelector('#score svg');
    const a=PianoNotation.analysis(),vr=v.getBoundingClientRect(),sr=svg.getBoundingClientRect();
    const rail=document.getElementById('optical-context-rail'),rr=rail.getBoundingClientRect();
    const all=Array.from(svg.querySelectorAll('path,text,rect,circle,ellipse,line,polygon,polyline'));
    const clipped=all.filter(n=>{
     if(getComputedStyle(n).visibility==='hidden'||n.closest('#piano-optical-study-cues'))return false;
     const b=n.getBoundingClientRect();
     return b.right>vr.left&&b.left<vr.right&&(b.top<vr.top-.5||b.bottom>vr.bottom+.5);
    });
    return {staffSpace:a.geometry.staffSpace,scale:a.geometry.staffSpace/10,
     camera:a.geometry.window||null,viewBox:svg.getAttribute('viewBox'),
     viewport:[v.clientWidth,v.clientHeight],painted:[sr.width,sr.height],
     rail:[rr.left,rr.top,rr.width,rr.height],scrollTop:v.scrollTop,
     context:a.context,clipped:clipped.length,
     mapping:a.mapping&&a.mapping.matched,
     limiters:(()=>{
      if(!a.geometry.window)return null;
      const region=a.geometry.window,boxes=StudyWindow.collect(svg).boxes.filter(b=>
       b.x+b.w>=region.left&&b.x<=region.right);
      const ident=b=>({kind:b.kind,tag:b.tag,text:b.text,pathData:b.pathData,x:+b.x.toFixed(1),
       y:+b.y.toFixed(1),right:+(b.x+b.w).toFixed(1),bottom:+(b.y+b.h).toFixed(1)});
      return {top:boxes.slice().sort((a,b)=>a.y-b.y).slice(0,5).map(ident),
       bottom:boxes.slice().sort((a,b)=>(b.y+b.h)-(a.y+a.h)).slice(0,5).map(ident),
       count:boxes.length};
     })()};
   });
   assert.equal(row.scrollTop,0,'no vertical scroll in '+label);
   assert.equal(row.clipped,0,'painted glyph clipped in '+label+' '+size.height);
   assert.equal(row.mapping,gym.events.length,'musical mapping in '+label);
   assert(row.painted[1]<=size.height+.5,'window height within viewport');
   assert(row.context&&row.context.measure>=1,'pinned context missing');
   const file='gymnopedie.window-'+label+'.'+size.width+'x'+size.height+'.png';
   await page.screenshot({path:'../docs/evidence/study-profile/'+file});
   b1Views.push({size,label,beat,file,...row});
   if(size.height===196)console.log('STUDY_B1_GEOMETRY '+JSON.stringify({label,beat,...row}));
  }
 }
 fs.writeFileSync('../docs/evidence/study-profile/b1-viewport-comparison.json',
  JSON.stringify({sourceSha,checkoutSha,scope:'Chromium Playwright CSS viewports; no physical acceptance',
   views:b1Views},null,2));

 // Quantify whether wider OSMD engraving separates vertically extreme
 // glyphs enough to improve the actual Study camera. This is diagnostic only;
 // never promote a spacing factor just because the music is more spread out.
 const spacingTrials=[];
 for(const factor of [1.25,1.5]){
  await page.setViewportSize({width:738,height:196});
  await page.evaluate(({item,factor})=>{
   window.__testStudySpacingFactor=factor;window.testDone=false;window.testError=null;
   PianoNotation.load(item.xml,0,'engraving-poc',item.events);
  },{item:gym,factor});
  await page.waitForFunction(()=>window.testDone||window.testError,null,{timeout:30000});
  const samples=[];
  for(const [label,beat] of positions){
   await page.evaluate(async beat=>{PianoNotation.seek(beat,false,100,false);
    await new Promise(requestAnimationFrame);await new Promise(requestAnimationFrame);},beat);
   const measurement=await page.evaluate(()=>{
    const a=PianoNotation.analysis(),v=document.getElementById('viewport');
    return {space:a.geometry.staffSpace,matched:a.mapping&&a.mapping.matched,
     verticalScroll:v.scrollTop,window:a.geometry.window};
   });
   samples.push({label,beat,...measurement});
  }
  spacingTrials.push({factor,samples});
 }
 await page.evaluate(()=>{delete window.__testStudySpacingFactor;});
 console.log('STUDY_B1_SPACING_TRIALS '+JSON.stringify(spacingTrials));
 fs.writeFileSync('../docs/evidence/study-profile/b1-spacing-trials.json',
  JSON.stringify({sourceSha,checkoutSha,baseline:b1Views.filter(v=>v.size.height===196),
   trials:spacingTrials},null,2));

 // Investigate the high-zoom fixed-point hypothesis without changing the
 // normal reader until the full musical corpus and visible ink approve it.
 await page.setViewportSize({width:738,height:196});
 await page.evaluate(item=>{window.__testStudyMaxFit=true;
  window.testDone=false;window.testError=null;
  PianoNotation.load(item.xml,0,'engraving-poc',item.events);},gym);
 await page.waitForFunction(()=>window.testDone||window.testError,null,{timeout:30000});
 const maxFitTrials=[];
 for(const [label,beat] of positions){
  await page.evaluate(async beat=>{PianoNotation.seek(beat,false,100,false);
   await new Promise(requestAnimationFrame);await new Promise(requestAnimationFrame);},beat);
  const result=await page.evaluate(()=>{
   const a=PianoNotation.analysis(),v=document.getElementById('viewport'),vr=v.getBoundingClientRect();
   const clipped=Array.from(document.querySelectorAll('#score svg path,#score svg text,#score svg rect')).filter(n=>{
    if(getComputedStyle(n).visibility==='hidden'||n.closest('#piano-optical-study-cues'))return false;
    const b=n.getBoundingClientRect();return b.right>vr.left&&b.left<vr.right&&
     (b.top<vr.top-.5||b.bottom>vr.bottom+.5);
   });
   return {staffSpace:a.geometry.staffSpace,mapping:a.mapping&&a.mapping.matched,
    camera:a.geometry.window,clipped:clipped.length,scrollTop:v.scrollTop};
  });
  maxFitTrials.push({label,beat,...result});
  await page.screenshot({path:'../docs/evidence/study-profile/gymnopedie.maxfit-'+label+'.738x196.png'});
 }
 await page.evaluate(()=>{delete window.__testStudyMaxFit;});
 console.log('STUDY_B1_MAXFIT_TRIALS '+JSON.stringify(maxFitTrials));
 fs.writeFileSync('../docs/evidence/study-profile/b1-maxfit-trials.json',
  JSON.stringify({sourceSha,checkoutSha,baseline:b1Views.filter(v=>v.size.height===196),maxFitTrials},null,2));

 // Research-only editorial slur trial: all endpoints and original
 // MusicXML notes remain fixed. Visual comparison is mandatory before any
 // approval; this trial does NOT certify collision safety.
 const curveTrials=[];
 for(const factor of [.8,.6]){
  await page.setViewportSize({width:738,height:196});
  await page.evaluate(({item,factor})=>{window.__testStudyCurveFactor=factor;
   window.testDone=false;window.testError=null;
   PianoNotation.load(item.xml,0,'engraving-poc',item.events);}, {item:gym,factor});
  await page.waitForFunction(()=>window.testDone||window.testError,null,{timeout:30000});
  const readings=[];
  for(const [label,beat] of positions){
   await page.evaluate(async beat=>{PianoNotation.seek(beat,false,100,false);
    await new Promise(requestAnimationFrame);await new Promise(requestAnimationFrame);},beat);
   const row=await page.evaluate(()=>{
    const v=document.getElementById('viewport'),a=PianoNotation.analysis();
    const vr=v.getBoundingClientRect();
    const clipped=Array.from(document.querySelectorAll('#score svg path,#score svg text,#score svg rect')).filter(n=>{
     if(getComputedStyle(n).visibility==='hidden'||n.closest('#piano-optical-study-cues'))return false;
     const b=n.getBoundingClientRect();
     return b.right>vr.left&&b.left<vr.right&&(b.top<vr.top-.5||b.bottom>vr.bottom+.5);
    });
    return {staffSpace:a.geometry.staffSpace,mapping:a.mapping&&a.mapping.matched,
     clipped:clipped.length,scrollTop:v.scrollTop};
   });
   const file='gymnopedie.slur-'+factor+'-'+label+'.738x196.png';
   await page.screenshot({path:'../docs/evidence/study-profile/'+file});
   readings.push({label,beat,file,...row});
  }
  curveTrials.push({factor,readings});
 }
 await page.evaluate(()=>{delete window.__testStudyCurveFactor;});
 console.log('STUDY_B1_CURVE_TRIALS '+JSON.stringify(curveTrials));
 fs.writeFileSync('../docs/evidence/study-profile/b1-curve-trials.json',
  JSON.stringify({sourceSha,checkoutSha,baseline:b1Views.filter(v=>v.size.height===196),
   curveTrials},null,2));

 // Named context-change corpus fixture tests clefs, key signatures and time
 // signatures at exact measure boundaries, including backward seeks and
 // height-only resizing, with the pinned rail aligned to the camera.
 const changeFixture=corpus.find(item=>item.name.endsWith('engraving-context-change.musicxml'));
 assert(changeFixture,'context-change MusicXML fixture required');
 const contextEvidence=[];
 for(const size of [{width:738,height:196},{width:883,height:409}]){
  await page.setViewportSize(size);
  await page.evaluate(item=>{window.testDone=false;window.testError=null;
   PianoNotation.load(item.xml,0,'engraving-poc',item.events);},changeFixture);
  await page.waitForFunction(()=>window.testDone||window.testError,null,{timeout:30000});
  for(const [measure,beat] of [[1,0],[2,4],[3,7],[2,4],[1,0]]){
   await page.evaluate(async t=>{PianoNotation.seek(t,false,100,false);
    await new Promise(requestAnimationFrame);await new Promise(requestAnimationFrame);},beat);
   const proof=await page.evaluate(()=>{
    const a=PianoNotation.analysis(),rail=document.getElementById('optical-context-rail');
    const railBox=rail.getBoundingClientRect(),v=document.getElementById('viewport').getBoundingClientRect();
    const svg=document.querySelector('#score svg').getBoundingClientRect();
    const symbols=Array.from(rail.querySelectorAll('.vf-clef,.vf-keysignature,.vf-timesignature'));
    const cropped=symbols.filter(n=>{const b=n.getBoundingClientRect();
     return b.width>0&&(b.left<railBox.left-.7||b.right>railBox.right+.7||
      b.top<railBox.top-.7||b.bottom>railBox.bottom+.7);}).map(n=>n.getAttribute('class'));
    const reference=document.getElementById('optical-reference').getBoundingClientRect();
    return {context:a.context,referenceLeft:reference.left,
     rail:[railBox.left,railBox.top,railBox.width,railBox.height],
     svgTop:svg.top,viewportTop:v.top,cropped,symbols:symbols.length,
     scrollTop:document.getElementById('viewport').scrollTop};
   });
   assert.equal(proof.context&&proof.context.measure,measure,'musical context at beat '+beat);
   assert(proof.symbols>=2,'active clefs missing at beat '+beat);
   assert(proof.rail[0]+proof.rail[2]+4<=proof.referenceLeft,
    'context rail must clear the fixed cursor at '+beat+': '+JSON.stringify(proof));
   assert.equal(proof.cropped.length,0,'context symbols cropped by pinned rail at '+beat+
    ': '+JSON.stringify(proof));
   assert.equal(proof.scrollTop,0,'context cannot scroll vertically');
   assert(Math.abs(proof.rail[1]-proof.viewportTop-Math.max(0,proof.svgTop-proof.viewportTop))<2,
    'pinned rail and score must share vertical origin');
   const file='context-change.measure-'+measure+'.beat-'+beat+'.'+size.width+'x'+size.height+'.png';
   await page.screenshot({path:'../docs/evidence/study-profile/'+file});
   contextEvidence.push({size,measure,beat,file,...proof});
  }
 }
 fs.writeFileSync('../docs/evidence/study-profile/b1-context-transitions.json',
  JSON.stringify({sourceSha,checkoutSha,scope:'Chromium; not real hardware',contextEvidence},null,2));
 // A zero-width SVG path still paints a stem. It must block unsafe staff
 // compaction; the prior bbox-only filter silently discarded this obstacle.
 await page.evaluate(()=>{
  const ns='http://www.w3.org/2000/svg',svg=document.createElementNS(ns,'svg');
  svg.setAttribute('viewBox','0 0 1000 400');svg.setAttribute('width','490');svg.setAttribute('height','196');
  svg.dataset.opticalNativeHeight='400';svg.dataset.opticalStaffSpaceCss='4.9';
  const group=(id,y)=>{const g=document.createElementNS(ns,'g');g.id=id;g.setAttribute('class','staffline');
   const m=document.createElementNS(ns,'g');m.setAttribute('class','vf-measure');g.appendChild(m);svg.appendChild(g);
   for(let i=0;i<5;i++){const p=document.createElementNS(ns,'path');p.setAttribute('d','M 0 '+(y+i*10)+' H 1000');p.setAttribute('stroke','black');m.appendChild(p);}return g;};
  const upper=group('fixture-1',50),lower=group('fixture-2',240);
  const obstacle=document.createElementNS(ns,'rect');for(const [k,v] of Object.entries({x:118,y:130,width:6,height:10}))obstacle.setAttribute(k,v);upper.appendChild(obstacle);
  const stem=document.createElementNS(ns,'path');stem.setAttribute('d','M 120 150 V 285');stem.setAttribute('stroke','black');stem.setAttribute('stroke-width','2');lower.appendChild(stem);
  document.body.appendChild(svg);
  const result=StudyProfile.compactStaffGap(svg,{staffCount:2,notes:200},196);
  if(result.candidateDelta>=11||result.newCrossStaffOverlaps)throw Error('Thin stem was ignored: '+JSON.stringify(result));
  StudyProfile.restoreStaffGap(svg);
  if(lower.getAttribute('transform'))throw Error('Rejected compaction must restore native geometry');
  svg.remove();
 });
 // Height-only guide toggles must be reversible, preserve context and retain
 // musical mapping; a new render at the final size is the independent oracle.
 await page.setViewportSize({width:738,height:351});
 await page.evaluate(item=>{window.testDone=false;PianoNotation.load(item.xml,0,'engraving-poc',item.events);},gym);
 await page.waitForFunction(()=>window.testDone,null,{timeout:30000});
 const resizeEvidence=[];
 for(const height of [196,218,351,196]){
  await page.setViewportSize({width:738,height});
  await page.waitForFunction(height=>PianoNotation.analysis().geometry.viewport.height===height,height,{timeout:30000});
  const live=await page.evaluate(()=>{
   const a=PianoNotation.analysis(),svg=document.querySelector('#score svg'),rail=document.querySelector('#optical-context-rail svg');
   return {geometry:a.geometry,mapping:a.mapping,adaptation:a.layoutAdaptation,
    railVisible:!!rail&&Array.from(rail.querySelectorAll('.vf-clef')).every(n=>getComputedStyle(n).visibility!=='hidden'),
    box:svg.getAttribute('viewBox')};
  });
  assert(live.railVisible,'clefs survive guide resize');
  assert.equal(live.mapping.matched,gym.events.length,'complete mapping after guide resize');
  assert(!live.geometry.clipped&&!live.geometry.inkClipped,'no clipped notes or symbols after guide resize');
  resizeEvidence.push({height,...live});
 }
 await page.evaluate(item=>{window.testDone=false;PianoNotation.load(item.xml,0,'engraving-poc',item.events);},gym);
 await page.waitForFunction(()=>window.testDone,null,{timeout:30000});
 const fresh=await page.evaluate(()=>PianoNotation.analysis());
 assert(Math.abs(fresh.geometry.staffSpace-resizeEvidence[3].geometry.staffSpace)<.01,'repeated resize must equal fresh layout at final size');
 fs.writeFileSync('../docs/evidence/study-profile/resize.json',JSON.stringify({sourceSha,checkoutSha,resizeEvidence},null,2));
 // Direct DOM preservation and annotation classification, independent of title.
 await page.evaluate(()=>{const xml='<score-partwise><part><measure><note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><lyric><text>C</text></lyric></note><direction><direction-type><words>rit.</words></direction-type></direction><harmony><root><root-step>C</root-step></root></harmony></measure></part></score-partwise>';const p=StudyProfile.analyze(xml),d=new DOMParser().parseFromString(StudyProfile.presentation(xml,p),'application/xml');if(d.querySelector('lyric')||!d.querySelector('words')||!d.querySelector('harmony'))throw Error('annotation policy');});
 // Real OSMD failure, not a simulated successful fallback.
 const failed=await page.evaluate(async()=>{window.opensheetmusicdisplay=null;window.testError=null;PianoNotation.load('<score-partwise/>',0,'engraving-poc',[]);return window.testError;});
 assert(failed,'OSMD failure must reach native renderer callback');
 assert.equal(remoteRequests.length,0,'No imported XML may trigger a network request');
 fs.mkdirSync('../docs/evidence/study-profile',{recursive:true});
 fs.writeFileSync('../docs/evidence/study-profile/corpus.json',JSON.stringify({sourceSha,checkoutSha,scope:'Desktop Chromium, not Android or physical acceptance',reports,visualComparison},null,2));
 assert.equal(reports.filter(r=>r.failure).length,0,'Corpus failures: '+JSON.stringify(reports.filter(r=>r.failure)));
 console.log('STUDY_PROFILE_BROWSER_PASS files='+reports.length);
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1);});

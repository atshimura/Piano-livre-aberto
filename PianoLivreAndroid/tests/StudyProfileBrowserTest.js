'use strict';
const fs=require('fs'),path=require('path'),assert=require('assert');
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||'playwright');
const corpus=[];
for(const line of fs.readFileSync('build/study-corpus.tsv','utf8').trim().split('\n')){
 const v=line.split('\t');
 if(v[0]==='FILE')corpus.push({name:v[1],xml:Buffer.from(v[2],'base64').toString(),events:[]});
 if(v[0]==='NOTE')corpus[corpus.length-1].events.push({beat:+v[1],length:+v[2],midi:+v[3],staff:+v[4],measure:+v[5],voice:v[6],step:v[7],octave:+v[8],alter:+v[9],xmlNoteIndex:+v[10],sourcePart:+v[11]});
}
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox','--allow-file-access-from-files'],...(process.env.PIANO_CHROMIUM?{executablePath:process.env.PIANO_CHROMIUM}:{})});
 fs.mkdirSync('../docs/evidence/study-profile',{recursive:true});
 const remoteRequests=[];
 const page=await browser.newPage({viewport:{width:738,height:220}}),reports=[];
 await page.route(/^https?:/,route=>{remoteRequests.push(route.request().url());return route.abort();});
 await page.goto('file://'+path.resolve('app/src/main/assets/notation.html'));
 await page.evaluate(()=>{window.AndroidNotation={onReady(){},onRendered(){window.testDone=true;},onPresentation(m){window.presentationMessage=m;},onError(m){window.testError=m;}};});
 for(const item of corpus){
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
  const chordLabels=svg?Array.from(svg.querySelectorAll('text')).map(n=>({text:(n.textContent||'').trim(),hidden:n.style.visibility==='hidden'})).filter(n=>StudyProfile.isChordLabel(n.text)):[];
  return {same,changed,intact,error:window.testError,done:window.testDone,analysis:analysis,
    chordLabels:chordLabels,message:window.presentationMessage};
  },item);
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
  const signature=e=>JSON.stringify([Math.round(e.beat*1e6),Math.round(e.length*1e6),e.midi,e.staff,e.measure,e.step||e.writtenStep,e.octave===undefined?e.writtenOctave:e.octave]);
  assert.deepEqual(graphic.map(signature).sort(),item.events.map(signature).sort(),item.name+' complete pitch/octave/duration multiset');
  const dimensions=[];
  for(const size of [{width:738,height:196},{width:738,height:351},{width:883,height:409}]){
   await page.setViewportSize(size);await page.waitForTimeout(350);
   const fitted=await page.evaluate(()=>{PianoNotation.resize();return PianoNotation.analysis().geometry;});
   assert(!fitted.alerts.includes('vertical-overflow'),item.name+' vertical fit '+JSON.stringify(size));
   if(/(canon-in-d|fur-elise-easy-ver|gymnopedie-no-1-satie)\.mxl$/.test(item.name))await page.screenshot({path:'../docs/evidence/study-profile/'+path.basename(item.name)+'.'+size.width+'x'+size.height+'.png'});
   dimensions.push({size,geometry:fitted});
  }
  reports.push({dimensions,name:item.name,notes:item.events.length,profile:report.analysis.profile,geometry:report.analysis.geometry,mapping:report.analysis.mapping});
  }catch(error){reports.push({name:item.name,failure:error.message});console.error('STUDY_CORPUS_FAILURE '+error.message);}
 }
 // Reproducible same-build comparison of the compact-staff fit on three
 // representative viewport sizes. Baseline uses stock OSMD staff clearance;
 // adapted uses the bounded, skyline-checked spacing rule in the Study view.
 const gym=corpus.find(item=>item.name.endsWith('gymnopedie-no-1-satie.mxl'));
 assert(gym,'Gymnopédie source fixture is required for before/after evidence');
 const visualComparison=[];
 for(const size of [{width:738,height:196},{width:738,height:351},{width:883,height:409}]){
  await page.setViewportSize(size);
  for(const disabled of [true,false]){
   await page.evaluate(({item,disabled})=>{
    window.testDone=false;window.testError=null;window.__testDisableStudyVerticalAdaptation=disabled;
    PianoNotation.load(item.xml,0,'engraving-poc',item.events);
   },{item:gym,disabled});
   await page.waitForFunction(()=>window.testDone||window.testError,{timeout:30000});
   const row=await page.evaluate(()=>({error:window.testError,analysis:PianoNotation.analysis()}));
   assert(!row.error,'Gymnopédie comparison render: '+row.error);
   const phase=disabled?'before':'after';
   await page.screenshot({path:'../docs/evidence/study-profile/gymnopedie.'+phase+'.'+size.width+'x'+size.height+'.png'});
   visualComparison.push({phase,size,staffSpaceCssPx:row.analysis.geometry.staffSpace,
    rawSvgHeightCssPx:row.analysis.layoutAdaptation.beforeHeight||row.analysis.geometry.height,
    viewportHeightCssPx:size.height,verticalOverflow:row.analysis.geometry.alerts.includes('vertical-overflow'),
    hiddenChordLabels:row.analysis.geometry.hiddenChordLabels||[],adaptation:row.analysis.layoutAdaptation});
   if(!disabled&&size.height===196){
    assert(row.analysis.geometry.staffSpace>=6,'Gymnopédie low viewport must reach at least 6 CSS px after adaptation: '+JSON.stringify(row.analysis.layoutAdaptation));
    assert(!row.analysis.geometry.alerts.includes('vertical-overflow'),'Gymnopédie remains fully visible without vertical scroll');
   }
  }
 }
 window.__testDisableStudyVerticalAdaptation=false;
 // Direct DOM preservation and annotation classification, independent of title.
 await page.evaluate(()=>{const xml='<score-partwise><part><measure><note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><lyric><text>C</text></lyric></note><direction><direction-type><words>rit.</words></direction-type></direction><harmony><root><root-step>C</root-step></root></harmony></measure></part></score-partwise>';const p=StudyProfile.analyze(xml),d=new DOMParser().parseFromString(StudyProfile.presentation(xml,p),'application/xml');if(d.querySelector('lyric')||!d.querySelector('words')||!d.querySelector('harmony'))throw Error('annotation policy');});
 // Real OSMD failure, not a simulated successful fallback.
 const failed=await page.evaluate(async()=>{window.opensheetmusicdisplay=null;window.testError=null;PianoNotation.load('<score-partwise/>',0,'engraving-poc',[]);return window.testError;});
 assert(failed,'OSMD failure must reach native renderer callback');
 assert.equal(remoteRequests.length,0,'No imported XML may trigger a network request');
 fs.mkdirSync('../docs/evidence/study-profile',{recursive:true});
 fs.writeFileSync('../docs/evidence/study-profile/corpus.json',JSON.stringify({scope:'Desktop Chromium, not Android or physical acceptance',reports,visualComparison},null,2));
 assert.equal(reports.filter(r=>r.failure).length,0,'Corpus failures: '+JSON.stringify(reports.filter(r=>r.failure)));
 console.log('STUDY_PROFILE_BROWSER_PASS files='+reports.length);
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1);});

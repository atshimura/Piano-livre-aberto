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
 const page=await browser.newPage({viewport:{width:738,height:220}}),reports=[];
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
   const svg=document.querySelector('#score svg');
   return {same,changed,intact,error:window.testError,done:window.testDone,analysis:PianoNotation.analysis(),
    message:window.presentationMessage};
   // Kept outside private page state: audit notes through the public analysis
   // result instead; renderer exports reconciliation in its diagnostics.
   function nullSafeGraphic(){return {};}
  },item);
  assert(report.same&&report.changed&&report.intact,item.name+' content invalidation/source preservation');
  assert(report.done&&!report.error,item.name+' OSMD: '+report.error);
  assert(report.analysis.profile.notes===item.events.length,item.name+' written pitch count');
  const graphic=report.analysis.notes;
  assert.equal(graphic.length,item.events.length,item.name+' graphical note count');
  for(const e of item.events){
   assert(graphic.some(g=>g.midi===e.midi&&g.writtenOctave===e.octave&&g.writtenStep===e.step&&Math.abs(g.beat-e.beat)<1e-5&&Math.abs(g.length-e.length)<1e-5),item.name+' pitch/octave/duration '+JSON.stringify(e));
  }
  reports.push({name:item.name,notes:item.events.length,profile:report.analysis.profile,geometry:report.analysis.geometry,mapping:report.analysis.mapping});
 }
 // Direct DOM preservation and annotation classification, independent of title.
 await page.evaluate(()=>{const xml='<score-partwise><part><measure><note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><lyric><text>C</text></lyric></note><direction><direction-type><words>rit.</words></direction-type></direction><harmony><root><root-step>C</root-step></root></harmony></measure></part></score-partwise>';const p=StudyProfile.analyze(xml),d=new DOMParser().parseFromString(StudyProfile.presentation(xml,p),'application/xml');if(d.querySelector('lyric')||!d.querySelector('words')||!d.querySelector('harmony'))throw Error('annotation policy');});
 // Real OSMD failure, not a simulated successful fallback.
 const failed=await page.evaluate(async()=>{window.opensheetmusicdisplay=null;window.testError=null;PianoNotation.load('<score-partwise/>',0,'engraving-poc',[]);return window.testError;});
 assert(failed,'OSMD failure must reach native renderer callback');
 fs.mkdirSync('../docs/evidence/study-profile',{recursive:true});
 fs.writeFileSync('../docs/evidence/study-profile/corpus.json',JSON.stringify({scope:'Desktop Chromium, not Android or physical acceptance',reports},null,2));
 console.log('STUDY_PROFILE_BROWSER_PASS files='+reports.length);
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1);});

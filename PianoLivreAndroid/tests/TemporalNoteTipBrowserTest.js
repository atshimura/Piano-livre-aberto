'use strict';
/* GPL-3.0-only. Exercise the temporal renderer's real notehead click UI. */
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||'playwright');

const xml=`<score-partwise version="4.0">
 <part-list><score-part id="P1"><part-name>Piano</part-name></score-part></part-list>
 <part id="P1"><measure number="1">
  <attributes><divisions>1</divisions><key><fifths>0</fifths></key>
   <time><beats>4</beats><beat-type>4</beat-type></time>
   <clef><sign>G</sign><line>2</line></clef></attributes>
  <note><pitch><step>E</step><octave>4</octave></pitch><duration>1</duration><type>quarter</type></note>
  <note><pitch><step>G</step><octave>4</octave></pitch><duration>1</duration><type>quarter</type></note>
 </measure></part></score-partwise>`;

(async()=>{
 fs.mkdirSync('../docs/evidence/study-profile',{recursive:true});
 const browser=await chromium.launch({headless:true,args:['--no-sandbox','--allow-file-access-from-files'],
  ...(process.env.PIANO_CHROMIUM?{executablePath:process.env.PIANO_CHROMIUM}:{})});
 try{
  const page=await browser.newPage({viewport:{width:738,height:220}});
  await page.goto('file://'+path.resolve('app/src/main/assets/notation.html'));
  await page.evaluate(()=>document.body.classList.add('continuous'));
  await page.evaluate(xml=>TemporalNotation.render(xml,document.getElementById('score'),
   document.getElementById('viewport'),0),xml);
  await page.waitForFunction(()=>{
   const head=document.querySelector('.musical-event ellipse');
   return head&&head.getBoundingClientRect().width>0;
  },null,{timeout:10000});
  await page.locator('.musical-event ellipse').first().click();
  const tip=page.locator('#temporal-note-identification');
  await page.waitForFunction(()=>{
   const tip=document.getElementById('temporal-note-identification');
   return tip&&tip.style.display==='block';
  },null,{timeout:3000});
  assert.equal((await tip.innerText()).trim(),'Mi4 · E4','temporal click preserves Portuguese and international written pitch');
  const geometry=await page.evaluate(()=>{
   const tip=document.getElementById('temporal-note-identification');
   const head=document.querySelector('.musical-event ellipse');
   const a=tip.getBoundingClientRect(),b=head.getBoundingClientRect(),v=document.getElementById('viewport').getBoundingClientRect();
   return {tip:{left:a.left,top:a.top,right:a.right,bottom:a.bottom},head:{left:b.left,top:b.top,right:b.right,bottom:b.bottom},
    viewport:{left:v.left,top:v.top,right:v.right,bottom:v.bottom}};
  });
  const overlaps=geometry.tip.left<geometry.head.right&&geometry.tip.right>geometry.head.left&&
   geometry.tip.top<geometry.head.bottom&&geometry.tip.bottom>geometry.head.top;
  assert(!overlaps,'temporal note label must not cover the touched notehead');
  assert(geometry.tip.left>=geometry.viewport.left&&geometry.tip.top>=geometry.viewport.top&&
   geometry.tip.right<=geometry.viewport.right&&geometry.tip.bottom<=geometry.viewport.bottom,
   'temporal note label must stay inside the visible score viewport');
  await page.screenshot({path:'../docs/evidence/study-profile/temporal-note-identification.png'});
  await page.waitForFunction(()=>{
   const tip=document.getElementById('temporal-note-identification');
   return tip&&tip.style.display==='none';
  },null,{timeout:4000});
  console.log('TEMPORAL_NOTE_TIP_BROWSER_PASS Mi4 · E4 viewport-safe and expires');
 }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});

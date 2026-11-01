'use strict';
// E4: OSMD offline display regression for automatic camera transitions.
// Selection is tested in pure Java and in the production Android UI; this
// browser test verifies each requested tier displays complete engraved ink.
const assert=require('assert'),fs=require('fs'),path=require('path');
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||'playwright');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.PIANO_CHROMIUM?{executablePath:process.env.PIANO_CHROMIUM}:{})});
 const out=path.resolve('build/implementacao-e-baseline/visual-ec');
 fs.mkdirSync(out,{recursive:true});
 const fixtures=fs.readFileSync('build/discovery-fixtures.txt','utf8').trim().split(/\r?\n/);
 assert(fixtures.length>=15,'simultaneous MIDI 0+127 fixture missing');
 const reports=[];
 try{
  for(const p of [{name:'phone',width:625,height:356},{name:'tablet',width:945,height:596}]){
   const page=await browser.newPage({viewport:{width:p.width,height:p.height},deviceScaleFactor:1});
   await page.route(/^https?:/,r=>r.abort());
   await page.goto('file://'+path.resolve('app/src/main/assets/discovery.html'));
   await page.evaluate(()=>{window.e4={};window.DiscoveryNotation={rendered(r){window.e4.report=JSON.parse(r);},error(e){window.e4.error=e;}};});
   let revision=0;
   async function render(name,fixture,tier,expectedCount,screenshot=true){
    const xml=Buffer.from(fixtures[fixture],'base64').toString('utf8'),rev=++revision;
    await page.evaluate(([xml,tier,expectedCount,rev])=>{window.e4={};Discovery.render(xml,tier,false,expectedCount,rev);},[xml,tier,expectedCount,rev]);
    await page.waitForFunction(rev=>window.e4.report?.revision===rev||window.e4.error,rev);
    const err=await page.evaluate(()=>window.e4.error);
    assert(!err,p.name+' '+name+' '+err);
    const result=await page.evaluate(()=>{
     const report=window.e4.report;
     const viewport=document.getElementById('viewport').getBoundingClientRect();
     const svg=document.querySelector('#score svg');
     function bound(n){const b=n.getBoundingClientRect();return {x:b.x-viewport.x,y:b.y-viewport.y,w:b.width,h:b.height};}
     const heads=Array.from(svg.querySelectorAll('.vf-notehead')).map(bound).filter(b=>b.w>0&&b.h>0&&b.w>=b.h*.85);
     const ink=Array.from(svg.querySelectorAll('.vf-stavenote,.vf-ledgers,.vf-clef')).map(bound).filter(b=>b.w>0&&b.h>0);
     const minY=Math.min(...ink.map(b=>b.y)),maxY=Math.max(...ink.map(b=>b.y+b.h));
     return {...report,heads:heads.length,minY,maxY,clefs:svg.querySelectorAll('.vf-clef').length,rail:document.getElementById('rail').getBoundingClientRect().width};
    });
    assert.equal(result.tier,tier,p.name+' '+name+' tier');
    assert.equal(result.notes,expectedCount,p.name+' '+name+' note report count');
    assert.equal(result.heads,expectedCount,p.name+' '+name+' real pitches engraved');
    assert.equal(result.clefs,2,p.name+' '+name+' two engraved clefs');
    assert.equal(result.staves.length,2,p.name+' '+name+' staves');
    assert(result.staves.every(s=>s.length===5),p.name+' '+name+' staff rules');
    assert(result.minY>=-0.5&&result.maxY<=p.height+0.5,
     p.name+' '+name+' notes or ledger ink clipped '+JSON.stringify({minY:result.minY,maxY:result.maxY,height:p.height}));
    reports.push({profile:p.name,scenario:name,...result});
    if(screenshot)await page.screenshot({path:path.join(out,p.name+'-'+name+'.png')});
    return result;
   }
   const normal=await render('normal',1,0,1);
   const extreme=await render('real-piano-21-108',13,1,2);
   const retained=await render('extremes-retained',13,1,2);
   assert(Math.abs(extreme.scale-retained.scale)<.001,p.name+' retained extreme must remain legible');
   assert(normal.staffSpace>extreme.staffSpace,p.name+' normal stays larger');
   const restored=await render('normal-restored',1,0,1);
   assert(Math.abs(restored.scale-normal.scale)<.001,p.name+' new common must restore exact normal scale');
   assert(Math.abs(restored.staffSpace-normal.staffSpace)<.001,p.name+' staff space return');
   for(let s=0;s<2;s++)for(let l=0;l<5;l++)
    assert(Math.abs(restored.staves[s][l].y-normal.staves[s][l].y)<1,
     p.name+' staff origin stable on return');
   await render('full-midi-0-127',14,2,2);
   const fullRestored=await render('normal-after-full',1,0,1);
   assert(Math.abs(fullRestored.scale-normal.scale)<.001,p.name+' full MIDI restores normal');
   // Repeated visual state transitions must not accumulate a camera offset.
   for(let i=0;i<6;i++){
    const a=await render('repeat-extreme-'+i,13,1,2,false);
    const b=await render('repeat-normal-'+i,1,0,1,false);
    assert(Math.abs(a.scale-extreme.scale)<.001&&Math.abs(b.scale-normal.scale)<.001,
     p.name+' no oscillation / accumulated zoom');
   }
   console.log('DISCOVERY_E4_BROWSER_PASS '+JSON.stringify({profile:p.name,normalSpace:normal.staffSpace,extremeSpace:extreme.staffSpace,restoredSpace:restored.staffSpace,fullMidi:2,cycles:6}));
   await page.close();
  }
  fs.writeFileSync(path.join(out,'measurements.json'),JSON.stringify({scenario:'E4-tier-transitions',profiles:2,reports},null,2));
  console.log('DISCOVERY_E4_BROWSER_ALL_PASS profiles=2 captures=12 zero_clip=true');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

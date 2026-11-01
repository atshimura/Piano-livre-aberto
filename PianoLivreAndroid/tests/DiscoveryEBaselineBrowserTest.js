'use strict';
// E-A / E1: non-destructive baseline of the existing OSMD SVG, not a fix.
// Browser-only measurements complement the actual Android emulator captures.
const fs=require('fs'),path=require('path'),assert=require('assert');
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||'playwright');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.PIANO_CHROMIUM?{executablePath:process.env.PIANO_CHROMIUM}:{})});
 const output=path.resolve('build/implementacao-e-baseline/browser');
 fs.mkdirSync(output,{recursive:true});
 const all=[];
 try {
  const fixtures=fs.readFileSync('build/discovery-fixtures.txt','utf8').trim().split(/\r?\n/);
  assert(fixtures.length>=14,'E-A requires simultaneous extreme MIDI fixture');
  const cases=[
   {name:'empty',index:0,tier:0,ordered:false,notes:0},
   {name:'c4',index:1,tier:0,ordered:false,notes:1},
   {name:'sequence',index:10,tier:0,ordered:true,notes:4},
   {name:'extremes-together',index:13,tier:1,ordered:false,notes:2},
   // Current Java framing deliberately remains sticky until reopening;
   // this explicit render demonstrates how the same common note appears at tier 1.
   {name:'c4-after-extreme-sticky',index:1,tier:1,ordered:false,notes:1}
  ];
  for(const p of [{name:'phone',width:680,height:350},{name:'tablet',width:1024,height:600}]){
   const page=await browser.newPage({viewport:{width:p.width,height:p.height},deviceScaleFactor:1});
   await page.route(/^https?:/,request=>request.abort()); // offline-only OSMD
   await page.goto('file://'+path.resolve('app/src/main/assets/discovery.html'));
   await page.evaluate(()=>{window.DiscoveryNotation={rendered(json){window.eBaselineReport=JSON.parse(json);},error(e){window.eBaselineError=e;}};});
   for(let i=0;i<cases.length;i++){
    const c=cases[i],xml=Buffer.from(fixtures[c.index],'base64').toString('utf8');
    await page.evaluate(([xml,info,i])=>{window.eBaselineReport=null;window.eBaselineError=null;Discovery.render(xml,info.tier,info.ordered,info.notes,i+1);},[xml,c,i]);
    await page.waitForFunction(()=>window.eBaselineReport||window.eBaselineError,{timeout:20000});
    const error=await page.evaluate(()=>window.eBaselineError);
    assert(!error,p.name+' '+c.name+': '+error);
    const m=await page.evaluate(()=>{
     const report=window.eBaselineReport,viewport=document.querySelector('#viewport'),v=viewport.getBoundingClientRect(),svg=document.querySelector('#score svg');
     const rects=(selector)=>Array.from(svg.querySelectorAll(selector)).map(n=>{
      const b=n.getBoundingClientRect();
      return {x:+(b.x-v.x).toFixed(2),y:+(b.y-v.y).toFixed(2),w:+b.width.toFixed(2),h:+b.height.toFixed(2)};
     }).filter(b=>b.w>0&&b.h>0);
     const notes=rects('.vf-notehead');
     const ledgers=rects('.vf-ledgers');
     const accidentals=rects('.vf-accidental');
     const stems=rects('.vf-stem');
     const clefs=rects('.vf-clef');
     const visible=[...notes,...ledgers,...accidentals,...stems,...clefs];
     const bounds=visible.length?{
      xMin:Math.min(...visible.map(b=>b.x)),xMax:Math.max(...visible.map(b=>b.x+b.w)),
      yMin:Math.min(...visible.map(b=>b.y)),yMax:Math.max(...visible.map(b=>b.y+b.h))
     }:null;
     const rail=document.querySelector('#rail').getBoundingClientRect();
     return {...report,noteheads:notes,ledgers:ledgers.length,accidentals:accidentals.length,stems:stems.length,clefs:clefs.length,
      inkBounds:bounds,clipVertical:!!bounds&&(bounds.yMin<0||bounds.yMax>v.height),
      clipHorizontal:!!bounds&&(bounds.xMin<0||bounds.xMax>v.width),
      railWidth:+rail.width.toFixed(2),firstNoteX:notes.length?notes[0].x:null,viewportActual:{width:v.width,height:v.height}};
    });
    all.push({profile:p.name,state:c.name,fixtureIndex:c.index,...m});
    await page.screenshot({path:path.join(output,p.name+'-'+c.name+'.png')});
    console.log('E_A_BASELINE '+JSON.stringify({profile:p.name,state:c.name,scale:m.scale,staffSpace:m.staffSpace,tier:m.tier,firstNoteX:m.firstNoteX,railWidth:m.railWidth,clipVertical:m.clipVertical,clipHorizontal:m.clipHorizontal,inkBounds:m.inkBounds}));
   }
   await page.close();
  }
  fs.writeFileSync(path.join(output,'measurements.json'),JSON.stringify({generatedAt:new Date().toISOString(),kind:'offline_osmd_browser_baseline',scenarios:all},null,2));
  assert.equal(all.length,10,'five states in two profiles required');
  console.log('E_A_BROWSER_BASELINE_PASS profiles=2 states=10 json_and_screenshots=true (no visual fix performed)');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});

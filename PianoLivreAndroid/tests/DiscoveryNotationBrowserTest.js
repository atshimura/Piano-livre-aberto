'use strict';
const fs=require('fs'),path=require('path'),assert=require('assert');
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||'playwright');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.PIANO_CHROMIUM?{executablePath:process.env.PIANO_CHROMIUM}:{})});
 const page=await browser.newPage({viewport:{width:680,height:350}});
 await page.route(/^https?:/,r=>r.abort());
 await page.goto('file://'+path.resolve('app/src/main/assets/discovery.html'));
 await page.evaluate(()=>{window.DiscoveryNotation={rendered(r){window.report=JSON.parse(r);},error(e){window.failure=e;}};});
 const fixtures=fs.readFileSync('build/discovery-fixtures.txt','utf8').trim().split(/\r?\n/);let normal;
 for(let i=0;i<fixtures.length;i++){
  const tier=i>=7?2:0;
  await page.evaluate(([xml,tier,i])=>{window.report=null;window.failure=null;Discovery.render(xml,tier,i===10,i===0?0:i===4?3:i===10?4:1);},[Buffer.from(fixtures[i],'base64').toString('utf8'),tier,i]);
  await page.waitForFunction(()=>window.report||window.failure);assert(!await page.evaluate(()=>window.failure),await page.evaluate(()=>window.failure));
  const r=await page.evaluate(()=>window.report);assert.equal(r.staves.length,2);assert(r.staves.every(s=>s.length===5));
  if(i===0)normal=r;
  if(i<=6){assert.equal(r.scale,normal.scale);for(let j=0;j<2;j++)assert(Math.abs(r.staves[j][0].y-normal.staves[j][0].y)<.1);}
  const ink=await page.evaluate(()=>Array.from(document.querySelectorAll('#score .vf-stavenote')).map(n=>{const b=n.getBoundingClientRect();return {top:b.top,bottom:b.bottom};}));
  assert(ink.every(b=>b.top>=0&&b.bottom<=350),'note ink clipped '+i+JSON.stringify(ink));
 }
 await browser.close();console.log('DISCOVERY_NOTATION_BROWSER_PASS stable=7 extremes=2 staves=2 offline=true');
})().catch(e=>{console.error(e);process.exit(1);});

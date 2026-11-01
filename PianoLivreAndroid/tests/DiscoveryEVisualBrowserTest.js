'use strict';
// Implementação E-B: real OSMD geometry and before/after UI measurements.
// Keep this separate from the E-A historical baseline fixture.
const fs=require('fs'),path=require('path'),assert=require('assert');
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||'playwright');
(async()=>{
 const browser=await chromium.launch({headless:true,...(process.env.PIANO_CHROMIUM?{executablePath:process.env.PIANO_CHROMIUM}:{})});
 const root=path.resolve('build/implementacao-e-baseline/visual-eb');
 fs.mkdirSync(root,{recursive:true});
 const fixtures=fs.readFileSync('build/discovery-fixtures.txt','utf8').trim().split(/\r?\n/);
 const results=[];
 try{
  for(const profile of [{name:'phone',w:680,h:350,oldSpace:9.28},{name:'tablet',w:1024,h:600,oldSpace:16.22}]){
   const page=await browser.newPage({viewport:{width:profile.w,height:profile.h},deviceScaleFactor:1});
   await page.route(/^https?:/,request=>request.abort());
   await page.goto('file://'+path.resolve('app/src/main/assets/discovery.html'));
   await page.evaluate(()=>{window.DiscoveryNotation={rendered(json){window.ebRender=JSON.parse(json);},error(e){window.ebError=e;}};});
   async function render(name,index,tier,count,ordered,xmlOverride){
    const xml=xmlOverride||Buffer.from(fixtures[index],'base64').toString('utf8');
    await page.evaluate(([xml,tier,count,ordered])=>{window.ebRender=null;window.ebError=null;Discovery.render(xml,tier,ordered,count);},[xml,tier,count,ordered]);
    await page.waitForFunction(()=>window.ebRender||window.ebError);
    const err=await page.evaluate(()=>window.ebError);
    assert(!err,profile.name+' '+name+' '+err);
    const result=await page.evaluate(()=>{
     const report=window.ebRender;
     const svg=document.querySelector('#score svg');
     const vp=document.getElementById('viewport').getBoundingClientRect();
     const rail=document.getElementById('rail').getBoundingClientRect();
     function bounds(n){const b=n.getBoundingClientRect();return {x:b.x-vp.x,y:b.y-vp.y,w:b.width,h:b.height};}
     const heads=Array.from(svg.querySelectorAll('.vf-notehead')).map(bounds);
     const ledgers=Array.from(svg.querySelectorAll('.vf-ledgers')).map(bounds);
     const accidental=Array.from(svg.querySelectorAll('.vf-accidental')).map(bounds);
     const stems=Array.from(svg.querySelectorAll('.vf-stem')).map(bounds);
     const clefs=Array.from(svg.querySelectorAll('.vf-clef')).map(bounds);
     const ink=[...heads,...ledgers,...accidental,...stems,...clefs].filter(b=>b.w>0&&b.h>0);
     return {...report,heads,ledgers,accidental,stems,clefs,
      railWidth:rail.width,viewportReal:{w:vp.width,h:vp.height},
      inkBounds:ink.length?{top:Math.min(...ink.map(n=>n.y)),bottom:Math.max(...ink.map(n=>n.y+n.h)),
       left:Math.min(...ink.map(n=>n.x)),right:Math.max(...ink.map(n=>n.x+n.w))}:null,
      firstX:heads.length?Math.min(...heads.map(b=>b.x+b.w/2)):null};
    });
    assert.equal(result.staves.length,2,profile.name+' '+name+' needs Sol and Fa');
    assert(result.staves.every(s=>s.length===5),'each staff has five rules');
    if(result.inkBounds){
     assert(result.inkBounds.top>=-.5&&result.inkBounds.bottom<=profile.h+.5,
      profile.name+' '+name+' vertical ink clipped '+JSON.stringify(result.inkBounds));
    }
    results.push({profile:profile.name,name,...result});
    await page.screenshot({path:path.join(root,profile.name+'-'+name+'.png')});
    return result;
   }
   const empty=await render('empty',0,0,0,false);
   const c4=await render('c4',1,0,1,false);
   const chord=await render('c-major-chord',4,0,3,false);
   const sequence=await render('sequence',10,0,4,true);
   const c2=await render('c2-ledgers',5,0,1,false);
   const c6=await render('c6-ledgers',6,0,1,false);
   // E-flat is an alteration of the known C4 fixture, not a replacement
   // for the application's MusicXML-to-engraving production path.
   const ebXml=Buffer.from(fixtures[1],'base64').toString('utf8')
    .replace('<pitch><step>C</step><octave>4</octave></pitch>',
             '<pitch><step>E</step><alter>-1</alter><octave>4</octave></pitch>')
    .replace('<type>quarter</type><staff>1</staff></note>',
             '<type>quarter</type><accidental>flat</accidental><staff>1</staff></note>');
   assert(ebXml.includes('<accidental>flat</accidental>'),'E-flat fixture must include accidental');
   const accident=await render('eb4-accidental',1,0,1,false,ebXml);
   const extreme=await render('extremes-together',13,1,2,false);
   assert(empty.staffSpace>profile.oldSpace*1.18,
    profile.name+' normal staff must grow >=18% from E-A baseline');
   assert(Math.abs(empty.staffSpace-c4.staffSpace)<.03,'normal scale stable between empty and note');
   assert(Math.abs(empty.staffSpace-sequence.staffSpace)<.03,'normal scale stable for sequence');
   assert(Math.abs(c4.firstX-sequence.firstX)<1.5,
    profile.name+' isolated and first sequence note must share column: '+c4.firstX+' vs '+sequence.firstX);
   assert(Math.abs(c4.firstX-chord.firstX)<1.5,
    profile.name+' chord and single note must share beginning');
   assert(Math.abs(c2.firstX-c4.firstX)<1.5&&Math.abs(c6.firstX-c4.firstX)<1.5,
    profile.name+' different clefs and ledger notes should share initial column');
   assert(Math.abs(accident.firstX-c4.firstX)<1.5,
    profile.name+' note with accidental still has same initial column');
   assert(c4.firstX>c4.railWidth+8,
    profile.name+' first note cannot appear under the clef rail');
   assert(accident.accidental.length>0&&accident.accidental.every(b=>b.x>accident.railWidth-2),
    profile.name+' accidental must clear the fixed clef rail');
   assert(chord.heads.length>=3,'chord heads remain engraved');
   assert(Math.max(...chord.heads.map(b=>b.x+b.w/2))-Math.min(...chord.heads.map(b=>b.x+b.w/2))<2,
    profile.name+' chord heads must share a single attack column');
   assert(c4.ledgers.length>0&&c2.ledgers.length>0&&c6.ledgers.length>0,
    profile.name+' ledger rules preserved for all out-of-staff notes');
   const l=c4.ledgers[0],h=c4.heads[0];
   assert(Math.abs((l.x+l.w/2)-(h.x+h.w/2))<1,
    profile.name+' ledger and head must move together');
   assert(extreme.heads.length===2,'both piano extremes remain present in preview');
   console.log('E_B_VISUAL_PASS '+JSON.stringify({
    profile:profile.name,staffSpaceBefore:profile.oldSpace,staffSpaceAfter:empty.staffSpace,
    increasePercent:+((empty.staffSpace/profile.oldSpace-1)*100).toFixed(1),
    firstX:c4.firstX,sequenceX:sequence.firstX,chordX:chord.firstX,
    railWidth:c4.railWidth,extremeTier:extreme.tier}));
   await page.close();
  }
  fs.writeFileSync(path.join(root,'measurements.json'),JSON.stringify({kind:'implementacao-e-b-visual',scenarios:results},null,2));
  console.log('E_B_VISUAL_BROWSER_PASS profiles=2 pngs=16 all_svg_geometry=true');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1);});

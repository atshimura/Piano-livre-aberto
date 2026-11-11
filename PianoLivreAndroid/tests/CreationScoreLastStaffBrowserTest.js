"use strict";
/** ZAI-013: the final staff is still reachable after zoom in short viewports.
 * Real bundled OSMD, Chromium rendering; no synthetic mock layout.
 */
const assert=require("assert"), path=require("path");
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||"playwright");

function fixture(){
 let result='<?xml version="1.0" encoding="utf-8"?><score-partwise version="4.0"><work><work-title>Final staff test</work-title></work><movement-title>Final staff test</movement-title><part-list><score-part id="P1"><part-name>Piano</part-name></score-part></part-list><part id="P1">';
 const pitches=["C","E","G","F","D","A","B"];
 for(let i=0;i<24;i++){
  result+='<measure number="'+(i+1)+'">';
  if(i===0)result+='<attributes><divisions>960</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><staves>2</staves><clef number="1"><sign>G</sign><line>2</line></clef><clef number="2"><sign>F</sign><line>4</line></clef></attributes>';
  for(let n=0;n<4;n++)
   result+='<note><pitch><step>'+pitches[(i+n)%pitches.length]+'</step><octave>4</octave></pitch><duration>960</duration><voice>1</voice><type>quarter</type><staff>1</staff></note>';
  result+='<backup><duration>3840</duration></backup>';
  for(let n=0;n<4;n++)
   result+='<note><pitch><step>'+pitches[(i+n+2)%pitches.length]+'</step><octave>3</octave></pitch><duration>960</duration><voice>2</voice><type>quarter</type><staff>2</staff></note>';
  result+='</measure>';
 }
 return result+'</part></score-partwise>';
}
(async()=>{
 const browser=await chromium.launch({headless:true,args:["--no-sandbox","--allow-file-access-from-files"]});
 try{
  const page=await browser.newPage({viewport:{width:950,height:240}});
  const external=[];
  await page.route(/^https?:/,route=>{external.push(route.request().url());return route.abort()});
  await page.goto("file://"+path.resolve("app/src/main/assets/notation.html"));
  await page.evaluate(()=>{window.AndroidNotation={
   onReady(){},onRendered(t){window.z13Done=t;},
   onFailure(t,code,stage,message){window.z13Error={t,code,stage,message};},
   onError(message){window.z13Error={message};},
   onPresentation(){},onStudyTap(){}
  };});
  const xml=fixture();
  let generation=200, checks=0;
  for(const geometry of [{width:950,height:240},{width:800,height:170},{width:1200,height:310}]){
   await page.setViewportSize(geometry);
   await page.evaluate(({xml,generation})=>{
    window.z13Done=null;window.z13Error=null;
    PianoNotation.load(xml,0,"creation-page",[],generation);
   },{xml,generation:++generation});
   await page.waitForFunction(()=>window.z13Done!==null||window.z13Error!==null,
      null,{timeout:60000});
   for(const zoom of [1,1.5,0.7]){
    await page.evaluate(z=>PianoNotation.zoom(z),zoom);
    await page.waitForTimeout(250);
    const result=await page.evaluate(()=>{
     const vp=document.getElementById("viewport");
     vp.scrollTop=vp.scrollHeight-vp.clientHeight;
     const nodes=[...document.querySelectorAll("#score svg .vf-notehead")];
     const rect=vp.getBoundingClientRect();
     const geometries=nodes.map(n=>n.getBoundingClientRect());
     const finalInkBottom=Math.max(...geometries.map(b=>b.bottom));
     const atEnd=Math.max(0,vp.scrollHeight-vp.clientHeight);
     const padding=parseFloat(getComputedStyle(document.getElementById("score")).paddingBottom);
     return {error:window.z13Error,rendered:window.z13Done,headCount:nodes.length,
      pageClass:document.body.className,padding,viewportHeight:vp.clientHeight,
      scrollHeight:vp.scrollHeight,scrollTop:vp.scrollTop,maxScroll:atEnd,
      finalInkBottom,viewportTop:rect.top,viewportBottom:rect.bottom,
      bottomGap:rect.bottom-finalInkBottom,
      overflowY:getComputedStyle(vp).overflowY,
      overflowX:getComputedStyle(vp).overflowX,
      scrollWidth:vp.scrollWidth,clientWidth:vp.clientWidth};
    });
    assert.strictEqual(result.error,null,"OSMD failed "+JSON.stringify(result));
    assert.strictEqual(result.pageClass,"creation-page");
    assert(result.headCount>=190,"notes disappeared "+JSON.stringify(result));
    assert.strictEqual(result.overflowY,"auto");
    assert.strictEqual(result.overflowX,"hidden");
    assert(result.padding>=60,"missing trailing score clearance");
    assert(result.scrollWidth<=result.clientWidth+2,"horizontal pan returned "+JSON.stringify(result));
    assert(Math.abs(result.scrollTop-result.maxScroll)<3,"cannot navigate to last staff "+JSON.stringify(result));
    assert(result.finalInkBottom<result.viewportBottom-10,
      "last line remains below bottom/covered by controls "+JSON.stringify(result));
    assert(result.finalInkBottom>result.viewportTop+3,
      "last line cannot be seen after reaching end "+JSON.stringify(result));
    checks++;
    console.log("ZAI013_LAST_STAFF_PASS "+JSON.stringify({geometry,zoom,
      noteheads:result.headCount,scroll:Math.round(result.maxScroll),
      bottomGap:Math.round(result.bottomGap)}));
   }
  }
  assert.equal(external.length,0,"must run bundled OSMD completely offline");
  console.log("ZAI013_CREATION_SCORE_BOTTOM_PASS checks="+checks);
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});

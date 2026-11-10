"use strict";
/* ZAI-008: actual bundled OSMD, Chrome viewport, no horizontal pan or note loss. */
const fs=require("fs"),path=require("path"),assert=require("assert");
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||"playwright");

function fixture(){
 const start='<?xml version="1.0" encoding="utf-8"?><score-partwise version="4.0"><work><work-title>Responsive Creation</work-title></work><movement-title>Responsive Creation</movement-title><part-list><score-part id="P1"><part-name>Piano</part-name></score-part></part-list><part id="P1">';
 let measures='';
 for(let i=0;i<14;i++){
  measures+='<measure number="'+(i+1)+'">';
  if(i===0)measures+='<attributes><divisions>960</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><staves>2</staves><clef number="1"><sign>G</sign><line>2</line></clef><clef number="2"><sign>F</sign><line>4</line></clef></attributes>';
  for(let n=0;n<4;n++){
   const steps=['C','D','E','F','G','A','B'];
   measures+='<note><pitch><step>'+steps[(i+n)%7]+'</step><octave>4</octave></pitch><duration>960</duration><voice>1</voice><type>quarter</type><staff>1</staff></note>';
  }
  measures+='<backup><duration>3840</duration></backup><note><rest/><duration>3840</duration><voice>5</voice><type>whole</type><staff>2</staff></note></measure>';
 }
 return start+measures+'</part></score-partwise>';
}
(async()=>{
 let browser=null;
 try{
  browser=await chromium.launch({headless:true,args:["--no-sandbox","--allow-file-access-from-files"]});
  const page=await browser.newPage({viewport:{width:360,height:660}});
  const network=[];
  await page.route(/^https?:/,r=>{network.push(r.request().url());return r.abort()});
  await page.goto("file://"+path.resolve("app/src/main/assets/notation.html"));
  await page.evaluate(()=>{window.AndroidNotation={
   onReady(){},onRendered(token){window.z8Done=token;},
   onFailure(token,code,stage,message){window.z8Error={token,code,stage,message};},
   onError(message){window.z8Error={message};},
   onPresentation(){},onStudyTap(){}};});
  const xml=fixture(), measurements=[];let token=30;
  async function report(width,height,renderNew,zoom){
   await page.setViewportSize({width,height});
   if(renderNew){
    const next=++token;
    await page.evaluate(({xml,next})=>{
     window.z8Done=null;window.z8Error=null;
     PianoNotation.load(xml,0,"creation-page",null,next);
    },{xml,next});
    await page.waitForFunction(()=>window.z8Done!==null||window.z8Error!==null,
         null,{timeout:45000});
   }else{
    await page.evaluate(zoom===undefined?()=>PianoNotation.resize():
      (z)=>PianoNotation.zoom(z),zoom);
    await page.waitForTimeout(800);
   }
   const state=await page.evaluate(()=>{
    const vp=document.getElementById("viewport");
    const svg=document.querySelector("#score svg");
    const heads=Array.from(document.querySelectorAll("#score svg .vf-notehead"));
    const rect=vp.getBoundingClientRect();
    const rows=Array.from(document.querySelectorAll("#score svg g.staffline"))
      .map(node=>Math.round(node.getBoundingClientRect().top));
    return {error:window.z8Error||null,done:window.z8Done,className:document.body.className,
      scrollWidth:vp.scrollWidth,clientWidth:vp.clientWidth,
      scrollLeft:vp.scrollLeft,overflowX:getComputedStyle(vp).overflowX,
      overflowY:getComputedStyle(vp).overflowY,headCount:heads.length,
      rowCount:new Set(rows).size,
      clippedHeads:heads.filter(n=>{
       const b=n.getBoundingClientRect();
       return b.left<rect.left-3||b.right>rect.right+3;
      }).length,svgWidth:svg&&svg.getBoundingClientRect().width,viewportWidth:rect.width};
   });
   assert.equal(state.error,null,"OSMD render failure "+JSON.stringify(state));
   assert.equal(state.className,"creation-page","Creation has its own layout");
   assert(state.headCount>=56,"all 56 noteheads engraved "+JSON.stringify(state));
   assert(state.scrollWidth<=state.clientWidth+2,
      "no horizontal scroll at "+width+"px "+JSON.stringify(state));
   assert.equal(state.clippedHeads,0,
      "noteheads must not be clipped horizontally "+JSON.stringify(state));
   assert(state.rowCount>=3,
      "14 measures should wrap into multiple systems "+JSON.stringify(state));
   measurements.push({width,height,zoom, ...state});
  }
  await report(360,660,true);
  await report(800,420,false);
  await report(1100,720,false);
  await report(360,660,false,1.5);
  await report(360,660,false,0.7);

  // Free reading uses its previously established width/pinch behavior.
  const next=++token;
  await page.evaluate(({xml,next})=>{
   window.z8Done=null;window.z8Error=null;
   PianoNotation.load(xml,0,"free",null,next);
  },{xml,next});
  await page.waitForFunction(()=>window.z8Done!==null||window.z8Error!==null,
       null,{timeout:45000});
  const free=await page.evaluate(()=>({
   name:document.body.className,
   overflow:getComputedStyle(document.getElementById("viewport")).overflowX}));
  assert.equal(free.name,"free","imported/free mode not changed");
  assert.equal(free.overflow,"auto","free layout keeps horizontal scroll");
  assert.equal(network.length,0,"offline OSMD only");
  console.log("ZAI008_RESPONSIVE_OSMD_PASS "+
   JSON.stringify({measurements:measurements.map(m=>({
      width:m.width,notes:m.headCount,rows:m.rowCount,scrollWidth:m.scrollWidth,
      viewport:m.clientWidth,clipped:m.clippedHeads,zoom:m.zoom})),
    freeMode:free.name,offline:true}));
 }finally{if(browser)await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});

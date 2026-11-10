"use strict";
/**
 * ZAI-009: real bundled OSMD in Chromium, not a mock: hidden MusicXML rests
 * must NOT draw glyphs, while their time positions and pitched notes remain.
 * External visible-rest score is the control; no global CSS hiding.
 */
const assert=require("assert");
const path=require("path");
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||"playwright");

function makeXml(hidden){
 let s='<?xml version="1.0" encoding="utf-8"?><score-partwise version="4.0"><work><work-title>Basic notation</work-title></work><movement-title>Basic notation</movement-title><part-list><score-part id="P1"><part-name>Piano</part-name></score-part></part-list><part id="P1">';
 const rest=(len,type,staff,voice)=>'<note'+(hidden?' print-object="no"':'')+'><rest/><duration>'+len+'</duration><voice>'+voice+'</voice><type>'+type+'</type><staff>'+staff+'</staff></note>';
 for(let m=0;m<8;m++){
  s+='<measure number="'+(m+1)+'">';
  if(!m)s+='<attributes><divisions>960</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><staves>2</staves><clef number="1"><sign>G</sign><line>2</line></clef><clef number="2"><sign>F</sign><line>4</line></clef></attributes>';
  for(let n=0;n<4;n++){
   if(n===1) s+=rest(960,'quarter',1,1);
   else {
    const pitch=['C','D','E','F','G','A','B'][(m+n)%7];
    s+='<note><pitch><step>'+pitch+'</step><octave>4</octave></pitch><duration>960</duration><voice>1</voice><type>quarter</type><staff>1</staff></note>';
   }
  }
  s+='<backup><duration>3840</duration></backup>'+rest(3840,'whole',2,5)+'</measure>';
 }
 return s+'</part></score-partwise>';
}
async function main(){
 const browser=await chromium.launch({headless:true,args:["--no-sandbox","--allow-file-access-from-files"]});
 try{
  const page=await browser.newPage({viewport:{width:360,height:660}});
  const remote=[];
  await page.route(/^https?:/,r=>{remote.push(r.request().url());return r.abort()});
  await page.goto("file://"+path.resolve("app/src/main/assets/notation.html"));
  await page.evaluate(()=>{
   window.AndroidNotation={
    onReady(){},onRendered(n){window.z9Done=n},
    onFailure(n,c,stage,msg){window.z9Error={n,c,stage,msg}},
    onError(msg){window.z9Error={msg}},
    onPresentation(){},onStudyTap(){}
   };
  });
  let token=40;
  async function render(xml,layout,width,height){
   await page.setViewportSize({width,height});
   const n=++token;
   await page.evaluate(({xml,layout,n})=>{
    window.z9Done=null;window.z9Error=null;
    PianoNotation.load(xml,0,layout,null,n);
   },{xml,layout,n});
   await page.waitForFunction(()=>window.z9Done!==null||window.z9Error!==null,null,{timeout:45000});
   return await page.evaluate(()=>{
    const viewport=document.getElementById("viewport");
    const heads=Array.from(document.querySelectorAll("#score svg .vf-notehead"));
    const rect=viewport.getBoundingClientRect();
    const visible=heads.filter(el=>{
      const style=getComputedStyle(el),box=el.getBoundingClientRect();
      return style.display!=="none" && style.visibility!=="hidden" &&
        style.opacity!=="0" && box.width>0 && box.height>0;
    });
    const lines=document.querySelectorAll("#score svg g.staffline");
    return {error:window.z9Error||null,done:window.z9Done,
      body:document.body.className,heads:heads.length,visible:visible.length,
      width:viewport.clientWidth,scrollWidth:viewport.scrollWidth,
      clipped:visible.filter(e=>{const b=e.getBoundingClientRect();return b.left<rect.left-3||b.right>rect.right+3}).length,
      systems:lines.length,
      detail:heads.slice(0,6).map(e=>({cls:e.getAttribute("class"),style:e.getAttribute("style"),
        parent:e.parentElement?.getAttribute("class"),html:e.outerHTML.slice(0,160)}))};
   });
  }
  const sourceVisible=makeXml(false),sourceHidden=makeXml(true);
  const visible=await render(sourceVisible,"creation-page",360,660);
  const narrow=await render(sourceHidden,"creation-page",360,660);
  const wide=await render(sourceHidden,"creation-page",800,420);
  // Do not force a success based on DOM fixtures. The hidden rendered glyphs
  // must be fewer than the identical score with visible rests.
  console.log("ZAI009_OSMD_OBSERVED "+JSON.stringify({visible,narrow,wide}));
  for(const s of [visible,narrow,wide]){
   assert.equal(s.error,null,"OSMD render failed "+JSON.stringify(s.error));
   assert(s.scrollWidth<=s.width+2,"horizontal scroll not allowed in Creation");
   assert.equal(s.clipped,0,"no clipped score notes");
  }
  assert.equal(narrow.visible,24,"only 24 pitched heads expected; not 16 hidden rests");
  assert.equal(wide.visible,24,"rest hiding persists after reflow");
  assert(visible.visible>narrow.visible,
     "visible MusicXML rests must show only in the control, not the generated basic score");
  assert.equal(remote.length,0,"must use offline bundled OSMD, no network requests");
  // Imported score remains untouched by generator's exported representation.
  const imported=await render(sourceVisible,"free",800,420);
  assert.equal(imported.error,null);
  assert.equal(imported.body,"free");
  assert(imported.visible>wide.visible,"external scores keep visible rests");
  console.log("ZAI009_NO_REST_GLYPHS_PASS "+
    JSON.stringify({hiddenNotes:narrow.visible,control:visible.visible,landscape:wide.visible,offline:true}));
 }finally{await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});

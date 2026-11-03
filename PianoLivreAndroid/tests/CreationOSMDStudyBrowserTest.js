"use strict";
/** Targeted BASIC browser test: actual generated MusicXML in both OSMD layouts. */
const fs=require("fs"),path=require("path"),assert=require("assert");
const {chromium}=require(process.env.PIANO_PLAYWRIGHT||"playwright");
(async()=>{
 let browser=null;
 try{
 browser=await chromium.launch({headless:true,args:["--no-sandbox","--allow-file-access-from-files"]});
 const page=await browser.newPage({viewport:{width:738,height:245}});
 const requests=[];
 await page.route(/^https?:/,route=>{requests.push(route.request().url());return route.abort();});
 await page.goto("file://"+path.resolve("app/src/main/assets/notation.html"));
 await page.evaluate(()=>{window.AndroidNotation={
   onReady(){},onRendered(token){window.testReady=token;},
   onFailure(token,code,stage,msg){window.testFailure={token,code,stage,msg};},
   onError(message){window.testFailure={code:"PL-NOT-UNKNOWN",msg:message};},
   onPresentation(){},onStudyTap(){}};
 });
 let checks=0;let token=10;const failures=[];
 for(const name of ["solo-created","melody","legacy","chords","crossing","long","assigned","midi-single-staff1","midi-single-staff2"]){
  const dir=path.resolve("build/impl4-fixtures"),xml=fs.readFileSync(path.join(dir,name+".musicxml"),"utf8");
  const notes=JSON.parse(fs.readFileSync(path.join(dir,name+".json"),"utf8"));
  for(const layout of ["free","engraving-poc"]){
   token++;
   await page.evaluate(({xml,notes,layout,token})=>{
      window.testReady=null;window.testFailure=null;
      PianoNotation.load(xml,0,layout,layout==="free"?null:notes,token);
   },{xml,notes,layout,token});
   await page.waitForFunction(()=>window.testReady!==null||window.testFailure!==null,null,{timeout:45000});
   const report=await page.evaluate(()=>({
     ready:window.testReady,failure:window.testFailure,
     svg:!!document.querySelector("#score svg"),
     heads:document.querySelectorAll("#score svg .vf-notehead").length,
     mapping:PianoNotation.analysis().mapping
   }));
   if(report.failure!==null||!report.svg||report.heads<notes.length/2) {
     const debug=await page.evaluate(()=>{
       const graph=typeof osmd==="undefined"?null:osmd&&osmd.graphic;
       const svg=document.querySelector("#score svg");
       return {
         graphicalMeasures:graph&&graph.measureList?
           graph.measureList.map(group=>(group||[]).map(m=>({
             entries:m&&m.staffEntries?m.staffEntries.length:0,
             nodes:m&&m.staffEntries?m.staffEntries.slice(0,4).map(e=>({
               timestamp:e.getAbsoluteTimestamp&&e.getAbsoluteTimestamp()?.RealValue,
               position:e.PositionAndShape&&e.PositionAndShape.AbsolutePosition,
               voices:(e.graphicalVoiceEntries||[]).map(v=>({notes:(v.notes||[]).length}))
             })):[]
           }))):null,
         geometricNoteCount:PianoNotation.analysis()?.notes?.length,
         noteheadClassCount:svg&&svg.querySelectorAll('[class*="notehead"]').length,
         svgPaths:svg&&svg.querySelectorAll("path").length,
         svgPreview:svg&&svg.outerHTML.slice(0,2200)
       };
     });
     console.error("IMPL4_OSMD_FIXTURE_DIAGNOSTIC "+JSON.stringify({name,layout,report,debug}));
   }
   const correct=report.failure===null&&report.ready===token&&
      report.svg&&report.heads>=Math.ceil(notes.length/2)&&
      (layout==="free"||!name.startsWith("midi-single-")||
       report.mapping&&report.mapping.matched===notes.length&&report.mapping.unmatched===0);
   if(!correct)failures.push({name,layout,report});
   else checks++;
  }
 }
 assert.deepEqual(failures,[],"OSMD compatibility regression across generated/imported music");
 token++;
 await page.evaluate(token=>{
    window.testReady=null;window.testFailure=null;
    PianoNotation.load("<score-partwise/>",0,"engraving-poc",[],token);
 },token);
 await page.waitForFunction(()=>window.testFailure!==null||window.testReady!==null,null,{timeout:20000});
 const failure=await page.evaluate(()=>window.testFailure);
 assert(failure&&failure.code.startsWith("PL-NOT-"),"OSMD error must be structured");
 assert.equal(await page.evaluate(()=>typeof window.TemporalNotation),"undefined",
    "The discontinued temporal engine cannot be loaded as a fallback");
 assert.equal(requests.length,0,"Offline only; network requests forbidden");
 console.log("IMPL4_OSMD_CREATION_STUDY_BROWSER_PASS cases="+checks+" forcedFailure="+failure.code);
 }finally{
   if(browser)await browser.close();
 }
})().catch(err=>{console.error(err);process.exitCode=1;});

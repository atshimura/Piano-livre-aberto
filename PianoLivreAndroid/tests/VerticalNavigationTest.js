'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('app/src/main/assets/notation.html','utf8');
function source(name){const start=html.indexOf(' function '+name+'('),end=html.indexOf('\n function ',start+1);return html.slice(start,end)}
let cases=0;
for(const width of [738,1024])for(const height of [196,300,500])for(const native of [381.591,136.5,195.898,151.5]){
 const elements=Object.fromEntries(['score','optical-up','optical-down','optical-position','optical-note-tip'].map(id=>[id,{style:{},addEventListener(){}}]));
 const classes=new Set();
 const viewport={clientWidth:width,clientHeight:height,scrollHeight:Math.ceil(native),scrollTop:0};
 const document={getElementById:id=>elements[id],documentElement:{clientWidth:width,clientHeight:height},body:{classList:{toggle(name,on){if(on)classes.add(name);else classes.delete(name);viewport.clientWidth=width-(on?48:0)}}}};
 const svg={dataset:{opticalNativeWidth:'12000',opticalNativeHeight:String(native)},style:{},querySelectorAll:()=>[]};
 const ctx=vm.createContext({viewport,document,console:{log(){}},studyProof:true,seekBeat:17,opticalDisplayScale:1,osmdRenderedWidth:width});
 vm.runInContext(source('updateVerticalNavigation')+source('navigateVertically')+source('fitOpticalSvg'),ctx);
 ctx.fitOpticalSvg(svg);
 assert.equal(classes.has('vertical-navigation'),native>height);
 assert.equal(viewport.clientHeight,height);assert.equal(svg.style.height,native+'px');assert.equal(ctx.opticalDisplayScale,1);
 if(native>height){ctx.navigateVertically(1);assert.ok(viewport.scrollTop>0);assert.equal(ctx.seekBeat,17);assert.equal(elements['optical-note-tip'].style.display,'none');ctx.navigateVertically(-1);assert.equal(viewport.scrollTop,0);}
 document.documentElement.clientHeight=600;viewport.clientHeight=600;ctx.fitOpticalSvg(svg);
 assert.equal(classes.has('vertical-navigation'),false);assert.equal(viewport.clientWidth,width);cases++;
}
console.log('VERTICAL_NAVIGATION_PASS scenarios='+cases+' (mock DOM; not rendered scores)');

'use strict';
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('app/src/main/assets/notation.html','utf8');
function source(name){const start=html.indexOf(' function '+name+'('),end=html.indexOf('\n function ',start+1);return html.slice(start,end)}
let cases=0;
for(const width of [690,883])for(const height of [196,218,351,373,409,431])for(const native of [381.591,136.5,195.898,151.5]){
 const elements={score:{style:{}}};
 const viewport={clientWidth:width,clientHeight:height,scrollHeight:height,scrollTop:0,scrollWidth:width};
 const document={getElementById:id=>elements[id],documentElement:{clientWidth:width,clientHeight:height},body:{}};
 const svg={dataset:{opticalNativeWidth:'12000',opticalNativeHeight:String(native)},style:{},querySelectorAll:()=>[]};
 const fitLogs=[];
 const ctx=vm.createContext({viewport,document,console:{log(...args){fitLogs.push(args)}},getComputedStyle:()=>({overflowY:'hidden'}),studyProof:true,seekBeat:17,opticalDisplayScale:1,osmdRenderedWidth:width});
 vm.runInContext(source('fitOpticalSvg'),ctx);
 ctx.fitOpticalSvg(svg);
 const expected=(height-8)/native;
 assert.ok(Math.abs(ctx.opticalDisplayScale-expected)<1e-12,'largest uniform scale for '+width+'x'+height+' and '+native);
 assert.ok(Math.abs(parseFloat(svg.style.height)-native*expected)<1e-8,'complete SVG height scaled proportionally');
 assert.ok(Math.abs(parseFloat(svg.style.width)-12000*expected)<1e-8,'complete SVG width uses the same scale');
 assert.equal(parseFloat(elements.score.style.height),parseFloat(svg.style.height));
 assert.equal(ctx.seekBeat,17,'visual fit does not change musical beat');
 assert.ok(native*expected<=height-8+1e-8,'all native SVG content fits within safety inset');
 assert.equal(viewport.scrollTop,0,'fit never changes vertical position');
 assert.equal(viewport.scrollHeight,height,'vertical overflow is not introduced by the score fit');
 const fit=JSON.parse(fitLogs.at(-1)[0].replace('POC_SVG_FIT ',''));
 assert.equal(fit.viewportMaxScrollTop,0,'fit evidence reports no reachable vertical scrolling');
 assert.ok(Math.abs(fit.availableHeight-(height-8))<1e-12,'fit evidence records measured height minus safety inset');
 // A second measurement models guide open/close or WebView height changes.
 viewport.clientHeight=height+22;document.documentElement.clientHeight=height+22;
 ctx.fitOpticalSvg(svg);
 assert.ok(Math.abs(ctx.opticalDisplayScale-((height+14)/native))<1e-12,'height resize recomputes from immutable native geometry');
 cases++;
}
assert.match(html,/overflow-y:hidden/);
assert.match(html,/touch-action:pan-x/);
assert.doesNotMatch(html,/optical-navigation|navigateVertically|vertical-navigation/);
function staffline(positions){return {children:[{classList:{contains:name=>name==='vf-measure'},children:positions.map(y=>({tagName:'path',getBBox:()=>({y,height:0})}))}]};}
for(const height of [196,351,409]){
 const elements={score:{style:{}}},viewport={clientWidth:738,clientHeight:height,scrollHeight:height,scrollTop:0,scrollWidth:738};
 const document={getElementById:id=>elements[id],documentElement:{clientWidth:738,clientHeight:height},body:{}};
 const svg={dataset:{opticalNativeWidth:'12000',opticalNativeHeight:'381.591'},style:{},
  querySelectorAll:()=>[staffline([100,110,120,130,140]),staffline([290,300,310,320,330])]};
 const ctx=vm.createContext({viewport,document,console:{log(){}},getComputedStyle:()=>({overflowY:'hidden'}),studyProof:true,seekBeat:0,opticalDisplayScale:1,osmdRenderedWidth:738});
 vm.runInContext(source('fitOpticalSvg'),ctx);ctx.fitOpticalSvg(svg);
 assert.ok(Math.abs(Number(svg.dataset.opticalStaffSpaceCss)-10*((height-8)/381.591))<1e-10,'staff-line spacing scales with SVG');
 assert.ok(Math.abs(Number(svg.dataset.opticalStaffGapSpaces)-15)<1e-10,'dimensionless staff separation remains invariant across fit scales');
}
console.log('PROPORTIONAL_SCORE_FIT_PASS scenarios='+cases+' (mock DOM; Android rendering verified separately)');

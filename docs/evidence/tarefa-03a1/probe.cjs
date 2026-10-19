const fs=require('fs');
const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const [bundle,chrome,scores,output]=process.argv.slice(2);
if(!output)throw Error('Uso: node probe.cjs BUNDLE CHROME SCORES_JSON OUTPUT_JSON');
(async()=>{
const browser=await chromium.launch({executablePath:chrome,headless:true,args:['--no-sandbox']});
const out={environment:{browser:browser.version(),node:process.version,viewport:[738,196],osmd:'1.9.8',bundleSha256:require('crypto').createHash('sha256').update(fs.readFileSync(bundle)).digest('hex'),sourceHead:'6435255d93d4cc782a54274851003b1c4f77f4c0'},scores:[]};
for(const [name,xml] of Object.entries(JSON.parse(fs.readFileSync(scores)))){
 const p=await browser.newPage({viewport:{width:738,height:196}});
 await p.setContent('<html><body style="margin:0"><div id="score" style="width:738px"></div></body></html>');
 await p.addScriptTag({path:bundle});
 await p.evaluate(async xml=>{window.o=new opensheetmusicdisplay.OpenSheetMusicDisplay('score',{backend:'svg',autoResize:false,drawTitle:false,drawComposer:false,drawPartNames:false,drawingParameters:'compacttight',renderSingleHorizontalStaffline:true,newSystemFromXML:false,disableCursor:true,cursorsOptions:[{type:1,color:'#008b78',alpha:0.8,follow:false}]});await o.load(xml);o.render();await document.fonts.ready;},xml);
 const r=await p.evaluate(()=>{
 const svg=document.querySelector('#score svg'),root=svg.getScreenCTM(),inv=root.inverse();
 const matrix=m=>[m.a,m.b,m.c,m.d,m.e,m.f];
 const box=e=>{const b=e.getBBox(),m=inv.multiply(e.getScreenCTM()),pts=[[b.x,b.y],[b.x+b.width,b.y],[b.x,b.y+b.height],[b.x+b.width,b.y+b.height]].map(([x,y])=>new DOMPoint(x,y).matrixTransform(m));return [Math.min(...pts.map(p=>p.x)),Math.min(...pts.map(p=>p.y)),Math.max(...pts.map(p=>p.x)),Math.max(...pts.map(p=>p.y))]};
 const union=a=>a.length?[Math.min(...a.map(b=>b[0])),Math.min(...a.map(b=>b[1])),Math.max(...a.map(b=>b[2])),Math.max(...a.map(b=>b[3]))]:null;
 const leaves=Array.from(svg.querySelectorAll('path,rect,line,polyline,polygon,ellipse,circle,text')).filter(e=>!e.closest('defs,clipPath'));
 const records=leaves.map((e,i)=>{const ancestors=[];for(let n=e;n&&n!==svg;n=n.parentElement)if(n.id||n.getAttribute('class'))ancestors.push({tag:n.tagName,id:n.id,cls:n.getAttribute('class')});let b=null;try{b=box(e)}catch{}return {i,tag:e.tagName,staff:e.closest('g.staffline')?.id||null,measure:e.closest('g.vf-measure')?.id||null,ancestors,box:b,stroke:e.getAttribute('stroke'),strokeWidth:e.getAttribute('stroke-width'),text:e.textContent||undefined,d:e.getAttribute('d')||undefined};});
 const staves=Array.from(svg.querySelectorAll('g.staffline')).map(g=>{const measures=Array.from(g.children).filter(n=>n.classList.contains('vf-measure'));const first=measures[0];return {id:g.id,box:box(g),lines:Array.from(first.children).filter(n=>n.tagName.toLowerCase()==='path').slice(0,5).map(n=>({d:n.getAttribute('d'),box:box(n)})),measures:measures.map(m=>({id:m.id,box:box(m)}))}});
 const rules={};for(const k of Object.keys(o.EngravingRules))if(/Staff|SkyBottom|Page.*Margin|System.*Distance|WedgeVertical|Dynamic.*Distance|Slur.*Offset|LabelMargin|LyricsY/.test(k))if(typeof o.EngravingRules[k]!=='object')rules[k]=o.EngravingRules[k];
 const model=o.graphic.MusicPages?.[0]?.MusicSystems?.[0];
 const models=model?.StaffLines?.map(s=>({keys:Object.keys(s),y:s.PositionAndShape.AbsolutePosition.y,relativeY:s.PositionAndShape.RelativePosition.y,x:s.PositionAndShape.AbsolutePosition.x,width:s.PositionAndShape.Size.width,sampling:o.EngravingRules.SamplingUnit,sky:s.SkyLine,bottom:s.BottomLine}));
 const noteGroups=[];for(const [mi,col] of o.graphic.measureList.entries())for(const [si,me] of col.entries())for(const en of me.staffEntries)for(const ve of en.graphicalVoiceEntries)for(const gn of ve.notes){let vf=gn.vfnote?.[0];noteGroups.push({column:mi+1,staff:si+1,rest:gn.sourceNote.isRestFlag,id:vf?.getAttribute?.('id'),index:gn.vfnote?.[1]});}
 const semantic=new Map(noteGroups.map(n=>['vf-'+n.id,n.rest?'rest':'pitched']));for(const rec of records){const parent=rec.ancestors.find(a=>a.cls==='vf-stavenote');if(parent)rec.semantic=semantic.get(parent.id)||'unknown';}
 return {noteGroups,svg:{viewBox:svg.getAttribute('viewBox'),width:svg.getAttribute('width'),height:svg.getAttribute('height'),css:[svg.getBoundingClientRect().width,svg.getBoundingClientRect().height],rootScreenCTM:matrix(root)},measureCount:o.graphic.measureList.length,rules,staves,models,union:union(records.filter(r=>r.box).map(r=>r.box)),records};
 });r.name=name;r.xmlSha256=require('crypto').createHash('sha256').update(xml).digest('hex');out.scores.push(r);await p.close();
}
for(const s of out.scores)for(const r of s.records)delete r.d;
fs.writeFileSync(output,JSON.stringify(out));await browser.close();console.log(out.scores.map(s=>({name:s.name,svg:s.svg,staves:s.staves.map(x=>({id:x.id,lines:x.lines})),union:s.union,models:s.models?.map(x=>({y:x.y,keys:x.keys,skyN:x.sky?.length}))})));
})();

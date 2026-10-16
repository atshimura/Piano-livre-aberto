'use strict';
// Production renderer with deterministic DOM/clock; not an FPS benchmark.
const assert=require('assert'),fs=require('fs'),vm=require('vm'),path=require('path');
const {DOMParser}=require(path.resolve('build/musicxml-audit/node_modules/@xmldom/xmldom'));
const proto=Object.getPrototypeOf(new DOMParser().parseFromString('<root/>','text/xml').documentElement);
if(!Object.getOwnPropertyDescriptor(proto,'children'))Object.defineProperty(proto,'children',{get(){return Array.from(this.childNodes||[]).filter(n=>n.nodeType===1);}});
let queries=0,dimensions=0,now=0,nextId=0;const frames=new Map();
class Node {
 constructor(tag){this.tag=tag;this.children=[];this.attrs={};this.style={};this.textContent='';}
 setAttribute(k,v){this.attrs[k]=String(v);} getAttribute(k){return this.attrs[k]??null;}
 appendChild(n){this.children.push(n);return n;} removeChild(n){this.children.splice(this.children.indexOf(n),1);}
 get firstChild(){return this.children[0]||null;} set innerHTML(v){assert.strictEqual(v,'');this.children=[];}
 addEventListener(){} querySelector(s){return this.querySelectorAll(s)[0]||null;}
 querySelectorAll(s){queries++;const result=[];function walk(n){for(const c of n.children){if(s.startsWith('.')?(c.attrs.class||'').split(' ').includes(s.slice(1)):c.tag===s)result.push(c);walk(c);}}walk(this);return result;}
}
const document={createElement:t=>new Node(t),createElementNS:(ns,t)=>new Node(t)};
const ctx={document,DOMParser,performance:{now:()=>now},console:{log(){}},requestAnimationFrame:fn=>{frames.set(++nextId,fn);return nextId;},cancelAnimationFrame:id=>frames.delete(id),innerWidth:900};ctx.window=ctx;vm.createContext(ctx);vm.runInContext(fs.readFileSync('app/src/main/assets/temporal-notation.js','utf8'),ctx);
const T=ctx.TemporalNotation,container=new Node('div');let width=900,height=250;
const viewport={get clientWidth(){dimensions++;return width;},get clientHeight(){dimensions++;return height;}};
const xml=`<score-partwise version="4.0"><part-list><score-part id="P1"><part-name>Piano</part-name></score-part></part-list><part id="P1"><measure number="1"><attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time><staves>2</staves><clef number="1"><sign>G</sign><line>2</line></clef><clef number="2"><sign>F</sign><line>4</line></clef></attributes><note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><type>quarter</type><staff>1</staff></note><note><pitch><step>D</step><octave>4</octave></pitch><duration>15</duration><type>whole</type><staff>1</staff></note></measure></part></score-partwise>`;
function frame(ms){now=ms;const work=[...frames.values()];frames.clear();for(const fn of work)fn(now);assert(frames.size<=1,'at most one animation loop');}
function track(){return container.children[0].children.find(n=>n.id==='timeline-track');}
function offset(){return Number(track().children[0].style.transform.match(/translate3d\(([-\d.]+)/)[1]);}
const layout=T.render(xml,container,viewport,0);frame(0);assert.strictEqual(frames.size,0,'initial paused render sleeps');const original=track(),root=container.children[0];
const note=root.querySelectorAll('.musical-event')[0].querySelector('ellipse');assert.strictEqual(note.attrs.stroke,'#008E78');
const reads=dimensions,searches=queries;T.sync(0,120,true);frame(250);frame(500);
assert.strictEqual(dimensions,reads,'no geometry reads per frame');assert.strictEqual(queries,searches,'indexed highlighting');assert.strictEqual(note.attrs.stroke,'#1E3049');assert(Math.abs(offset()+layout.pixelsPerBeat)<.01);
height=160;T.resize();assert.strictEqual(track(),original,'height retains tiles');assert.strictEqual(container.children[0],root);assert(original.style.transform.startsWith('scaleY('));T.resize();assert.strictEqual(track(),original);
frame(750);assert(Math.abs(offset()+1.5*layout.pixelsPerBeat)<.01,'height retains running clock');
width=1000;T.resize();assert.notStrictEqual(track(),original);frame(1000);assert(Math.abs(offset()+2*layout.pixelsPerBeat)<.01,'width preserves running clock');
T.sync(3,120,false);frame(1200);const paused=offset();assert.strictEqual(frames.size,0,'pause drains scheduled frame');for(let i=0;i<240;i++)frame(1200+i);T.sync(3,120,false);assert.strictEqual(frames.size,0,'unchanged paused sample stays asleep');frame(1600);assert.strictEqual(offset(),paused);
T.sync(0,120,false);frame(1700);assert.strictEqual(offset(),0);
T.sync(10,120,true);frame(1800);T.sync(0,120,true);frame(1800);assert.strictEqual(offset(),0);
// Native tempo multiplied by study speed must not be clamped visually.
for(const bpm of [6,600]){
 T.sync(0,bpm,false);frame(now);
 T.sync(0,bpm,true);frame(now+100);
 assert(Math.abs(offset()+layout.pixelsPerBeat*bpm/600)<.01,'exact effective BPM '+bpm);
}
T.sync(16,120,true);frame(now+1);assert.strictEqual(frames.size,0,'end of score sleeps');
T.sync(0,120,true);assert.strictEqual(frames.size,1,'loop wakes from end');frame(now+10);
T.teardown();assert.strictEqual(frames.size,0);
T.render(xml,container,viewport,2);frame(now);assert.strictEqual(frames.size,0,'replacement score starts paused');T.teardown();
console.log('PASS: indexed highlights, no frame geometry reads, resize, clock, pause, seek, loop and teardown');

// Long synthetic score: tile count grows, per-frame DOM queries/layout reads do not.
const measure=xml.match(/<measure[^>]*>.*?<\/measure>/)[0];
const longXml=xml.replace(measure,Array.from({length:64},(_,i)=>measure.replace('number="1"','number="'+(i+1)+'"')).join(''));
T.render(longXml,container,viewport,0);frame(now);
assert(track().children.length>50,'synthetic workload spans many tiles');
const longReads=dimensions,longQueries=queries;
for(const beat of [0,400,800,50]){
 T.sync(beat,120,true);frame(now+16);
 const visible=track().children.filter(n=>n.style.display==='block');
 assert(visible.length<=3,'only viewport tiles visible on long score');
}
assert.strictEqual(dimensions,longReads,'long score: no frame layout reads');
assert.strictEqual(queries,longQueries,'long score: no frame DOM searches');
T.sync(50,120,false);frame(now+16);assert.strictEqual(frames.size,0);T.teardown();
console.log('PASS: long synthetic score, bounded visible tiles, forward/backward seek and idle drain');

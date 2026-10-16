'use strict';
const assert=require('assert'),fs=require('fs'),path=require('path'),cp=require('child_process');
const {DOMParser}=require(path.resolve('build/musicxml-audit/node_modules/@xmldom/xmldom'));
const proto=Object.getPrototypeOf(new DOMParser().parseFromString('<root/>','text/xml').documentElement);
if(!Object.getOwnPropertyDescriptor(proto,'children'))Object.defineProperty(proto,'children',{get(){return Array.from(this.childNodes||[]).filter(n=>n.nodeType===1);}});
global.DOMParser=DOMParser;
const T=require('../app/src/main/assets/temporal-notation.js');
const rows=cp.execFileSync('java',['-cp','build/score-tests','MusicXmlFixtureInventory'],{encoding:'utf8'}).trim().split('\n').map(s=>s.split('\t'));
const step={C:0,D:2,E:4,F:5,G:7,A:9,B:11};
function close(a,b,why){assert(Math.abs(a-b)<1e-7,why+': '+a+' vs '+b);}
const expected={
 'named-voices-ties.xml':{end:2,notes:[[0,60,1,2],[0,60,1,2]],tempo:[]},
 'ties-voices-grace.xml':{end:4,notes:[[0,60,1,3],[0,52,2,1],[3,64,1,1]],tempo:[]},
 'tuplet-pickup.xml':{end:11/3,notes:[[0,60,1,1/3],[1/3,62,1,1/3],[2/3,64,1,3]],tempo:[]},
 'decimal-divisions.xml':{end:7,notes:[[0,60,1,.5],[.25,64,1,.75],[.5,62,1,.5],[4,65,1,3]],tempo:[[.75,90]]}
};
expected['integer-equivalent.xml']=expected['decimal-divisions.xml'];
for(const [file,want] of Object.entries(expected)){
 const java=rows.filter(r=>r[1]===file),plan=T.parse(fs.readFileSync('tests/fixtures/motor/'+file,'utf8'));
 const native=java.filter(r=>r[0]==='NOTE').map(r=>r.slice(2).map(Number));
 const visual=[],held=new Map();
 for(const n of plan.notes.filter(n=>!n.grace)){
  const midi=12*(n.oct+1)+step[n.step]+n.alter,key=n.staff+':'+n.voice+':'+midi;
  const prior=n.ties.includes('stop')?held.get(key):null;
  if(prior){prior[3]=Math.max(prior[3],n.at+n.dur-prior[0]);if(!n.ties.includes('start'))held.delete(key);}
  else{const event=[n.at,midi,n.staff,n.dur];visual.push(event);if(n.ties.includes('start'))held.set(key,event);}
 }
 for(const [label,notes] of [['Java',native],['JS',visual]]){
  assert.strictEqual(notes.length,want.notes.length,label+' count '+file);
  notes.forEach((n,i)=>n.forEach((v,j)=>close(v,want.notes[i][j],label+' event '+i+'/'+j)));
 }
 close(+java.find(r=>r[0]==='END')[2],want.end,'Java end');close(plan.duration,want.end,'JS end');
 const tempos=java.filter(r=>r[0]==='TEMPO').map(r=>r.slice(2).map(Number));
 assert.strictEqual(tempos.length,want.tempo.length);
 want.tempo.forEach((t,i)=>{close(tempos[i][0],t[0],'Java tempo beat');close(tempos[i][1],t[1],'Java BPM');close(plan.tempo[i].beat,t[0],'JS tempo beat');close(plan.tempo[i].bpm,t[1],'JS BPM');});
 console.log('PASS shared MusicXML fixture',file,'independent expected onsets, durations, tempo and end');
}

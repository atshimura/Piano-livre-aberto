"use strict";
const assert=require("assert");
const fs=require("fs"),path=require("path"),cp=require("child_process");
const {DOMParser}=require(path.resolve("build/musicxml-audit/node_modules/@xmldom/xmldom"));
const T=require("../app/src/main/assets/temporal-notation.js");
const inventory=cp.execFileSync("java",["-cp","build/score-tests","ScoreInventory"],{encoding:"utf8",maxBuffer:3000000});
const groups=new Map();
for(const row of inventory.trim().split("\n")){
 const [file,beat,midi,staff,duration]=row.split("\t");
 if(!groups.has(file))groups.set(file,[]);
 groups.get(file).push({beat:+beat,midi:+midi,staff:+staff,len:+duration});
}
function decodeXml(filename){
 const file=path.resolve("app/src/main/assets/scores",filename);
 const names=cp.execFileSync("unzip",["-Z1",file],{encoding:"utf8"}).trim().split("\n");
 const xml=names.find(n=>n.toLowerCase().endsWith(".xml")&&!n.startsWith("META-INF/"));
 assert(xml,"MXL without XML "+filename);
 return cp.execFileSync("unzip",["-p",file,xml],{encoding:"utf8",maxBuffer:12000000});
}
const prototype=Object.getPrototypeOf(new DOMParser().parseFromString("<root><child/></root>","text/xml").documentElement);
if(!Object.getOwnPropertyDescriptor(prototype,"children"))
 Object.defineProperty(prototype,"children",{get:function(){
  return Array.from(this.childNodes||[]).filter(n=>n.nodeType===1);
 }});
global.DOMParser=DOMParser;
const step={C:0,D:2,E:4,F:5,G:7,A:9,B:11};
function signature(n){return n.beat.toFixed(6)+"|"+n.midi+"|"+n.staff;}
let compared=0;
for(const [filename,scoreEvents] of groups){
 const plan=T.parse(decodeXml(filename));
 const visual=plan.notes
  .filter(n=>!n.grace&&n.dur>0&&(!n.ties||n.ties.indexOf("stop")<0))
  .map(n=>({beat:n.at,midi:12*(n.oct+1)+step[n.step]+n.alter,staff:n.staff,duration:n.dur}));
 const a=scoreEvents.map(signature).sort(),b=visual.map(signature).sort();
 const mismatch=a.filter((v,i)=>v!==b[i]).slice(0,8);
 if(a.length!==b.length||mismatch.length){
  console.error("FAIL",filename,"Java events",a.length,"JS attacks",b.length);
  console.error("First missing/different",mismatch,"JS examples",b.slice(0,10));
  process.exitCode=1;
 }else console.log("PASS Java vs JS MusicXML pitches and onsets",filename,a.length);
 compared+=Math.min(a.length,b.length);
}
if(!process.exitCode)console.log("PASS MusicXML visual crosscheck "+groups.size+" pieces, "+compared+" attacks/pitches/pautas.");


// Regress temporal engraving's absolute beat mapping for intra-measure BPM changes.
// The Java reference for these same beats lives in TempoMapTest.java.
const timedXml="<score-partwise version='4.0'><part-list>"+
 "<score-part id='P1'><part-name>Piano</part-name></score-part></part-list><part id='P1'>"+
 "<measure number='1'><attributes><divisions>4</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"+
 "<direction><sound tempo='120'/></direction>"+
 "<note><pitch><step>C</step><octave>4</octave></pitch><duration>8</duration></note>"+
 "<direction><sound tempo='60'/></direction>"+
 "<note><pitch><step>D</step><octave>4</octave></pitch><duration>8</duration></note>"+
 "</measure><measure number='2'>"+
 "<direction><sound tempo='180'/></direction>"+
 "<note><pitch><step>E</step><octave>4</octave></pitch><duration>16</duration></note>"+
 "</measure></part></score-partwise>";
const timeline=T.parse(timedXml);
assert.deepStrictEqual(timeline.tempo.map(e=>[e.beat,e.bpm]),[[0,120],[2,60],[4,180]],
 "JS engraving must report tempo positions in global quarter-note beats");
console.log("PASS visual tempo chronology at beats 0, 2 and 4");
const printedXml=timedXml
 .replace("<direction><sound tempo='120'/></direction>",
   "<direction><direction-type><metronome><beat-unit>quarter</beat-unit><beat-unit-dot/><per-minute>72</per-minute></metronome></direction-type></direction>")
 .replace("<direction><sound tempo='60'/></direction>",
   "<direction><direction-type><metronome><beat-unit>half</beat-unit><per-minute>60</per-minute></metronome></direction-type></direction>");
assert.deepStrictEqual(T.parse(printedXml).tempo.map(t=>[t.beat,t.bpm]),
 [[0,108],[2,120],[4,180]],
 "printed metronome note and dots must match Android's quarter-note BPM");
console.log("PASS metronome beat-unit and dotted tempo conversion");



const navXml="<score-partwise version='4.0'><part-list>"+
 "<score-part id='P1'><part-name>Piano</part-name></score-part></part-list><part id='P1'>"+
 "<measure number='1'><attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"+
 "<barline location='left'><repeat direction='forward'/></barline>"+
 "<direction><direction-type><segno/></direction-type></direction>"+
 "<note><pitch><step>C</step><octave>4</octave></pitch><duration>4</duration></note></measure>"+
 "<measure number='2'><note><pitch><step>D</step><octave>4</octave></pitch><duration>4</duration></note></measure>"+
 "<measure number='3'><barline location='left'><ending number='1' type='start'/></barline>"+
 "<note><pitch><step>E</step><octave>4</octave></pitch><duration>4</duration></note>"+
 "<barline location='right'><ending number='1' type='stop'/><repeat direction='backward'/></barline></measure>"+
 "<measure number='4'><barline location='left'><ending number='2' type='start'/></barline>"+
 "<direction><direction-type><coda/></direction-type></direction>"+
 "<note><pitch><step>F</step><octave>4</octave></pitch><duration>4</duration></note>"+
 "<barline location='right'><ending number='2' type='discontinue'/></barline></measure>"+
 "</part></score-partwise>";
const navPlan=T.parse(navXml);
assert.deepStrictEqual(navPlan.endings.map(e=>[e.start,e.end,e.number]),
 [[8,12,"1"],[12,16,"2"]],"1st and 2nd ending brackets must share original quarter-note coordinates");
assert(navPlan.marks.some(x=>x.text==="Segno"&&x.at===0),
 "Segno must be labelled on temporal partitura");
assert(navPlan.marks.some(x=>x.text==="Coda"&&x.at===12),
 "Coda must be labelled on temporal partitura");
console.log("PASS MusicXML alternate-ending brackets and Segno/Coda labels in temporal score");


const semanticsXml="<score-partwise version='4.0'><part-list>"+
 "<score-part id='P1'><part-name>Piano</part-name></score-part></part-list><part id='P1'>"+
 "<measure number='1'><attributes><divisions>6</divisions>"+
 "<time><beats>4</beats><beat-type>4</beat-type></time>"+
 "<key><fifths>1</fifths></key></attributes>"+
 [1,0,0,1].map(alter=>"<note><pitch><step>F</step><alter>"+alter+"</alter><octave>4</octave></pitch>"+
 "<duration>6</duration><type>quarter</type></note>").join("")+
 "</measure><measure number='2'>"+
 "<note><pitch><step>F</step><alter>1</alter><octave>4</octave></pitch>"+
 "<duration>6</duration><type>quarter</type></note>"+
 "<note><pitch><step>F</step><alter>0</alter><octave>4</octave></pitch>"+
 "<duration>6</duration><type>quarter</type></note>"+
 "<note><pitch><step>G</step><octave>4</octave></pitch>"+
 "<duration>2</duration><type>quarter</type><time-modification>"+
 "<actual-notes>3</actual-notes><normal-notes>2</normal-notes>"+
 "</time-modification><notations><tuplet type='start'/></notations></note>"+
 "<note><pitch><step>A</step><octave>4</octave></pitch><duration>2</duration><type>quarter</type>"+
 "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>"+
 "<note><pitch><step>B</step><octave>4</octave></pitch><duration>2</duration><type>quarter</type>"+
 "<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>"+
 "<notations><tuplet type='stop'/></notations></note>"+
 "</measure></part></score-partwise>";
const semantics=T.parse(semanticsXml),written=semantics.notes;
assert.deepStrictEqual(written.slice(0,6).map(n=>n.acc),
 ["","natural","","sharp","","natural"],
 "automatic notation must obey F# signature, F-natural cancellation and bar reset");
assert.deepStrictEqual(written.slice(6).map(n=>[n.tupletActual,n.tupletNormal,n.tupletStart,n.tupletStop]),
 [[3,2,true,false],[3,2,false,false],[3,2,false,true]],
 "MusicXML 3:2 quarter triplet requires written type and precise fractions");
assert(Math.abs(written[6].dur-1/3)<1e-8 && T._test.writtenDuration(written[6])===1,
 "triplet quarter note sounds one-third beat but must be engraved as quarter note");
assert(Math.abs(written[7].at-written[6].at-1/3)<1e-8,
 "tuplet positions must retain fractional musical beats");
console.log("PASS MusicXML display accidentals, key signature reset and 3:2 quarter-note tuplets");


const voiceSlurXml="<score-partwise version='4.0'><part-list>"+
 "<score-part id='P1'><part-name>Piano</part-name></score-part></part-list><part id='P1'>"+
 "<measure number='1'><attributes><divisions>6</divisions>"+
 "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"+
 "<note><pitch><step>G</step><octave>4</octave></pitch><duration>24</duration>"+
 "<voice>1</voice><staff>1</staff><type>whole</type>"+
 "<notations><slur type='start' number='1'/></notations></note>"+
 "<backup><duration>24</duration></backup>"+
 "<note><pitch><step>G</step><octave>4</octave></pitch><duration>24</duration>"+
 "<voice>2</voice><staff>1</staff><type>whole</type></note></measure>"+
 "<measure number='2'>"+
 "<note><pitch><step>C</step><octave>5</octave></pitch><duration>4</duration>"+
 "<voice>1</voice><type>quarter</type><time-modification>"+
 "<actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>"+
 "<notations><tuplet type='start' number='1' bracket='yes' placement='above'/></notations></note>"+
 "<note><pitch><step>D</step><octave>5</octave></pitch><duration>4</duration>"+
 "<voice>1</voice><type>quarter</type><time-modification>"+
 "<actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification></note>"+
 "<note><pitch><step>E</step><octave>5</octave></pitch><duration>4</duration>"+
 "<voice>1</voice><type>quarter</type><time-modification>"+
 "<actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>"+
 "<notations><tuplet type='stop' number='1'/></notations></note>"+
 "<note><rest/><duration>12</duration><voice>1</voice><type>half</type></note></measure>"+
 "<measure number='3'><note><pitch><step>E</step><octave>4</octave></pitch>"+
 "<duration>24</duration><voice>1</voice><type>whole</type>"+
 "<notations><slur type='stop' number='1'/></notations></note></measure>"+
 "</part></score-partwise>";
const advanced=T.parse(voiceSlurXml);
const atStart=advanced.notes.filter(n=>Math.abs(n.at)<1e-8);
assert.strictEqual(atStart.length,2,"polyphonic XML backup must share exact beat");
assert.deepStrictEqual(atStart.map(n=>n._visualStem),["up","down"],
 "independent G4 voices must have opposing stems");
assert.strictEqual(atStart[0]._headShift,0,"first voice should retain pitch center");
assert.strictEqual(atStart[1]._headShift,12,"second unison voice must be offset");
assert(advanced.rests.some(n=>n.type==="half"),"second measure rest survives parsing");
const advancedSpans=T._test.collectSpanners(advanced.notes);
assert.strictEqual(advancedSpans.filter(s=>s.kind==="slur").length,1,
 "slur from measure 1 to 3 must be paired despite intervening measures");
assert.strictEqual(advancedSpans.filter(s=>s.kind==="tuplet").length,1,
 "explicit MusicXML tuplet bracket must span all three notes");
const tuplet=advancedSpans.find(s=>s.kind==="tuplet");
assert(tuplet.bracket&&tuplet.above&&tuplet.number===3,
 "MusicXML placement and bracket=yes must determine SVG bracket");
assert(Math.abs(tuplet.from.at-4)<1e-8&&Math.abs(tuplet.to.at-4-4/3)<1e-8,
 "tuplet bracket endpoints must follow fractional quarter-note positions");
console.log("PASS XML independent voices, colliding heads, multi-measure slur and explicit tuplet brackets");


// Complex polyphony with 4 independent voices and nested 3:5 tuplets:
// actual audio duration remains defined by MusicXML duration, regardless
// of the multiple printed tuplets layered on the same notes.
const denseMeasures=[];
for(let v=1;v<=4;v++){
 const parts=[];
 parts.push("<note><pitch><step>C</step><octave>5</octave></pitch><duration>12</duration>"+
  "<voice>"+v+"</voice><type>half</type></note>");
 if(v!==4)parts.push("<backup><duration>12</duration></backup>");
 denseMeasures.push(parts.join(""));
}
const nestedPart=[
 "<note><pitch><step>D</step><octave>5</octave></pitch><duration>4</duration>"+
  "<voice>1</voice><type>eighth</type><time-modification><actual-notes>15</actual-notes>"+
  "<normal-notes>8</normal-notes></time-modification><notations>"+
  "<tuplet type='start' number='1' bracket='yes' placement='above'>"+
  "<tuplet-actual><tuplet-number>3</tuplet-number><tuplet-type>quarter</tuplet-type></tuplet-actual>"+
  "</tuplet></notations></note>",
 "<note><pitch><step>E</step><octave>5</octave></pitch><duration>4</duration>"+
  "<voice>1</voice><type>eighth</type><time-modification><actual-notes>15</actual-notes>"+
  "<normal-notes>8</normal-notes></time-modification><notations>"+
  "<tuplet type='start' number='2' bracket='yes' placement='above'>"+
  "<tuplet-actual><tuplet-number>5</tuplet-number><tuplet-type>eighth</tuplet-type></tuplet-actual>"+
  "</tuplet></notations></note>",
 "<note><pitch><step>F</step><octave>5</octave></pitch><duration>4</duration>"+
  "<voice>1</voice><type>eighth</type><time-modification><actual-notes>15</actual-notes>"+
  "<normal-notes>8</normal-notes></time-modification><notations>"+
  "<tuplet type='stop' number='2'/><tuplet type='stop' number='1'/>"+
  "</notations></note>"
].join("");
const denseXml="<score-partwise version='4.0'><part-list>"+
 "<score-part id='P1'><part-name>Piano</part-name></score-part></part-list>"+
 "<part id='P1'><measure number='1'><attributes>"+
 "<divisions>12</divisions><time><beats>1</beats><beat-type>4</beat-type></time>"+
 "</attributes>"+denseMeasures.join("")+"</measure><measure number='2'>"+
 nestedPart+"</measure></part></score-partwise>";
const dense=T.parse(denseXml);
const simultaneous=dense.notes.filter(n=>Math.abs(n.at)<1e-8);
assert.strictEqual(simultaneous.length,4,"MusicXML backup retains four independent voices");
assert.deepStrictEqual(simultaneous.map(n=>n.voice),["1","2","3","4"],
 "voices remain separate on same pitch and beat");
assert.deepStrictEqual(simultaneous.map(n=>n._headShift),[0,12,24,36],
 "four voice noteheads must not overlap");
assert.deepStrictEqual(simultaneous.map(n=>n._visualStem),["up","down","up","down"],
 "four voice alternating stem directions");
const complex=dense.notes.filter(n=>n.at>=1-1e-8);
assert.deepStrictEqual(complex[2].tupletMarks.map(n=>[n.type,n.number]),
 [["stop","2"],["stop","1"]],
 "nested MusicXML end mark pairs must both survive XML parsing");
const spansComplex=T._test.collectSpanners(dense.notes).filter(x=>x.kind==="tuplet");
assert.strictEqual(spansComplex.length,2,"nested tuplets each have one closing bracket");
assert.deepStrictEqual(spansComplex.map(n=>n.number).sort(),[3,5],
 "tuplets must use their own notation actual counts, not combined 15");
assert(spansComplex.some(s=>s._lane===1)&&spansComplex.some(s=>s._lane===0),
 "nested tuplets must be separated in height");
console.log("PASS MusicXML 4-voice chords, nested 3:5 printed tuplets and unaltered performance beats");


// MusicXML grace notes should be visible but must not consume quarter-note
// time or become sounding MIDI events. Two independent voices may print
// different accidentals at the same written onset.
const graceXml="<score-partwise version='4.0'><part-list>"+
 "<score-part id='P1'><part-name>Piano</part-name></score-part></part-list><part id='P1'>"+
 "<measure number='1'><attributes><divisions>4</divisions>"+
 "<time><beats>1</beats><beat-type>4</beat-type></time></attributes>"+
 "<note><grace slash='yes'/><pitch><step>B</step><octave>4</octave></pitch>"+
 "<voice>1</voice><type>eighth</type></note>"+
 "<note><grace/><pitch><step>D</step><octave>5</octave></pitch>"+
 "<voice>1</voice><type>16th</type></note>"+
 "<note><pitch><step>E</step><octave>5</octave></pitch><duration>4</duration>"+
 "<voice>1</voice><type>quarter</type></note>"+
 "<backup><duration>4</duration></backup>"+
 "<note><pitch><step>E</step><alter>1</alter><octave>5</octave></pitch>"+
 "<duration>4</duration><voice>2</voice><type>quarter</type><accidental>sharp</accidental></note>"+
 "</measure></part></score-partwise>";
const gracePlan=T.parse(graceXml);
const graceNotes=gracePlan.notes.filter(n=>n.grace);
assert.strictEqual(graceNotes.length,2,"both grace notes must survive visual MusicXML parser");
assert.deepStrictEqual(graceNotes.map(n=>n._graceShift),[-42,-21],
 "grace heads precede main note rather than all sitting on original beat");
assert(graceNotes[0].graceSlash&&!graceNotes[1].graceSlash,
 "printed acciaccatura slash respected");
assert(gracePlan.notes.every(n=>Math.abs(n.at)<1e-8),
 "ornamentation does not advance the musical clock");
const mainNotes=gracePlan.notes.filter(n=>!n.grace);
assert.deepStrictEqual(mainNotes.map(n=>n._headShift),[0,12],
 "main voice heads displaced without changing their sound");
assert.strictEqual(mainNotes[0].alter,0,"first E major spelling stays natural");
assert.strictEqual(mainNotes[1].alter,1,"other voice E-sharp remains different pitch");
console.log("PASS MusicXML grace note slashes, pre-onset placement and simultaneous altered voice");

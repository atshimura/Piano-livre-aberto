'use strict';
const assert=require('assert'),path=require('path');
const {DOMParser}=require(path.resolve('build/musicxml-audit/node_modules/@xmldom/xmldom'));
const proto=Object.getPrototypeOf(new DOMParser().parseFromString('<root/>','text/xml').documentElement);
if(!Object.getOwnPropertyDescriptor(proto,'children'))
 Object.defineProperty(proto,'children',{get(){return Array.from(this.childNodes||[]).filter(n=>n.nodeType===1);}});
global.DOMParser=DOMParser;
const T=require('../app/src/main/assets/temporal-notation.js');
const test=T._test;
assert.strictEqual(test.noteIdentification({step:'C',alter:0,oct:4}),'Dó4 · C4');
assert.strictEqual(test.noteIdentification({step:'B',alter:-1,oct:3}),'Si♭3 · B♭3');
assert.strictEqual(test.noteIdentification({step:'F',alter:1,oct:5}),'Fá♯5 · F♯5');
assert.strictEqual(test.noteIdentification({step:'D',alter:-2,oct:4}),'Ré𝄫4 · D𝄫4');
assert.strictEqual(test.noteIdentification({step:'Z',alter:0,oct:4}),null,'invalid step');
assert.strictEqual(test.noteIdentification({step:'C',alter:.5,oct:4}),null,'microtones not mislabeled');
assert.strictEqual(test.noteIdentification({step:'C',alter:3,oct:4}),null,'unsupported spellings');
const xml=`<score-partwise version="4.0">
 <part-list><score-part id="P1"><part-name>Piano</part-name></score-part></part-list>
 <part id="P1">
 <measure number="1">
  <attributes><divisions>1</divisions><key><fifths>1</fifths></key>
   <time><beats>4</beats><beat-type>4</beat-type></time>
   <clef><sign>G</sign><line>2</line></clef></attributes>
  <harmony><root><root-step>F</root-step><root-alter>1</root-alter></root><kind>minor</kind></harmony>
  <note><pitch><step>F</step><alter>1</alter><octave>4</octave></pitch><duration>1</duration><type>quarter</type></note>
  <note><pitch><step>A</step><octave>4</octave></pitch><duration>1</duration><type>quarter</type></note>
  <harmony><root><root-step>C</root-step></root><kind>major-seventh</kind></harmony>
  <note><pitch><step>C</step><octave>5</octave></pitch><duration>1</duration><type>quarter</type></note>
 </measure>
 <measure number="2">
  <note><pitch><step>C</step><octave>5</octave></pitch><duration>1</duration><type>quarter</type></note>
 </measure>
 </part></score-partwise>`;
const parsed=T.parse(xml);
assert.strictEqual(parsed.notes.length,4);
assert.strictEqual(test.noteIdentification(parsed.notes[0]),'Fá♯4 · F♯4');
assert.strictEqual(parsed.notes[0].harmony.symbol,'F♯m');
assert.strictEqual(parsed.notes[0].harmony.name,'Fá♯ menor');
assert.strictEqual(parsed.notes[1].harmony.symbol,'F♯m');
assert.strictEqual(parsed.notes[2].harmony.symbol,'Cmaj7');
assert.strictEqual(parsed.notes[3].harmony,undefined,'do not project harmony across bars');
assert.strictEqual(test.parseHarmony(new DOMParser().parseFromString('<harmony><root><root-step>C</root-step></root><kind>nonsense</kind></harmony>','text/xml').documentElement),null,'unknown chord not guessed');
console.log('NOTE_IDENTIFICATION_TEST_PASS checks=15');

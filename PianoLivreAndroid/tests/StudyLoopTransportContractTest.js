'use strict';
const assert=require('assert');
const fs=require('fs'),path=require('path');
const root=path.join(__dirname,'../app/src/main/java/br/org/pianolivre');
const main=fs.readFileSync(path.join(root,'MainActivity.java'),'utf8');
const screens=fs.readFileSync(path.join(root,'PianoScreens.java'),'utf8');
function body(name,next){
 const start=main.indexOf(' void '+name+'(');
 assert(start>=0,'missing '+name);
 const end=main.indexOf('\n void '+next+'(',start+1);
 assert(end>start,'missing method terminator '+next);
 return main.slice(start,end);
}
const start=body('markLoopStart','markLoopEnd');
const end=body('markLoopEnd','clearLoop');
const clear=body('clearLoop','toggle');
assert(start.indexOf('stopPlayback();')<start.indexOf('loopStart='),
 'changing start of A-B loop must first release MIDI playback');
assert(start.includes('StudyLoopPosition.start(score,position)'),
 'A marker aligns to a real note onset');
assert(start.includes('loopEnd=Double.MAX_VALUE;'),
 'new A marker cannot retain stale B marker');
assert(start.includes('resetWrittenRoute();'),'new A resets written itinerary');
assert(end.includes('double release=StudyLoopPosition.end(score,position);')&&
 end.includes('applyLoopSelection(Math.min(loopStart,release),Math.max(loopStart,release));'),
 'B marker uses checked loop selection and a complete note release');
assert(clear.indexOf('stopPlayback();')<clear.indexOf('loopStart=0;'),
 'removing A-B releases MIDI before clearing itinerary');
assert(screens.includes('a.markLoopStart();')&&screens.includes('a.markLoopEnd();'),
 'native loop menu calls safe transport methods');
assert(!screens.includes('a.loopStart=a.position')&&!screens.includes('a.loopEnd=a.position'),
 'UI must not bypass transport safety');
console.log('STUDY_LOOP_TRANSPORT_CONTRACT_PASS');

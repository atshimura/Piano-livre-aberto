/* Piano Livre - time-proportional piano score renderer.
 * All x coordinates derive from MusicXML quarter-note timestamps, never from
 * the conventional print engraving. The cursor is a fixed view overlay.
 * Pure math exported in _test for regression tests without Android.
 */
(function(global){
"use strict";
var NS="http://www.w3.org/2000/svg";
var STEP={C:0,D:1,E:2,F:3,G:4,A:5,B:6};
var SHARPS=["F","C","G","D","A","E","B"],FLATS=["B","E","A","D","G","C","F"];
var NOTE_PT={C:"Dó",D:"Ré",E:"Mi",F:"Fá",G:"Sol",A:"Lá",B:"Si"};
var HARMONY_KIND={
 major:["","maior"],minor:["m","menor"],augmented:["aug","aumentado"],
 diminished:["dim","diminuto"],dominant:["7","com sétima"],
 "major-seventh":["maj7","maior com sétima maior"],
 "minor-seventh":["m7","menor com sétima"],
 "diminished-seventh":["dim7","diminuto com sétima diminuta"],
 "half-diminished":["m7♭5","meio-diminuto"],
 "suspended-fourth":["sus4","suspenso com quarta"],
 "suspended-second":["sus2","suspenso com segunda"]
};
function accidentalSymbol(alter){
 return {"-2":"𝄫","-1":"♭","0":"","1":"♯","2":"𝄪"}[alter]||"";
}
function noteIdentification(n){
 if(!n||!Object.prototype.hasOwnProperty.call(NOTE_PT,n.step)||!Number.isInteger(n.oct)
   ||!Number.isInteger(n.alter)||Math.abs(n.alter)>2)return null;
 var altered=accidentalSymbol(n.alter),oct=n.oct;
 return NOTE_PT[n.step]+altered+oct+" · "+n.step+altered+oct;
}
function parseHarmony(node){
 var root=direct(node,"root"),step=value(root,"root-step","");
 var alter=num(root,"root-alter",0),kind=value(node,"kind","");
 if(!Object.prototype.hasOwnProperty.call(NOTE_PT,step)||!Number.isInteger(alter)
   ||Math.abs(alter)>2||!Object.prototype.hasOwnProperty.call(HARMONY_KIND,kind))return null;
 var quality=HARMONY_KIND[kind],symbol=step+accidentalSymbol(alter)+quality[0];
 return {symbol:symbol,name:NOTE_PT[step]+accidentalSymbol(alter)+" "+quality[1]};
}
var state=null;

function direct(n,tag){if(!n)return null;var c=n.children;for(var i=0;i<c.length;i++)if(c[i].tagName===tag)return c[i];return null;}
function children(n,tag){var a=[];if(!n)return a;for(var i=0;i<n.children.length;i++)if(n.children[i].tagName===tag)a.push(n.children[i]);return a;}
function value(n,tag,def){var e=direct(n,tag);return e?e.textContent.trim():def;}
function num(n,tag,def){var x=+value(n,tag,"");return isFinite(x)&&value(n,tag,"")!==""?x:def;}
function att(n,tag,def){return n&&n.hasAttribute(tag)?n.getAttribute(tag):def;}
function xmlTimeMap(xml) {
 var dom=new DOMParser().parseFromString(xml,"application/xml");
 if(dom.getElementsByTagName("parsererror").length)throw Error("MusicXML inválido");
 if(!dom.documentElement||dom.documentElement.tagName!=="score-partwise")
  throw Error("Esta versão da partitura temporal requer MusicXML score-partwise");
 var parts=children(dom.documentElement,"part");
 if(!parts.length)throw Error("MusicXML sem partes");
 if(parts.length>2)throw Error("Mais de duas partes requerem atribuição de mãos");
 var splitHands=parts.length===2;
 if(splitHands)parts.forEach(function(p){
  Array.prototype.forEach.call(p.getElementsByTagName("staves"),function(n){
   if(+n.textContent>1)throw Error("Parte com múltiplas pautas requer atribuição de mãos");
  });
  Array.prototype.forEach.call(p.getElementsByTagName("staff"),function(n){
   if(+n.textContent>1)throw Error("Parte com múltiplas pautas requer atribuição de mãos");
  });
 });
 var parsed=[],lengths=[],tempo=[],dynamics=[],changed=[],unsupported=[];
 parts.forEach(function(part,pi){
  var cfg={div:1,beats:4,unit:4,fifths:0,clefs:{1:"G",2:"F"}}, measures=[];
  children(part,"measure").forEach(function(m,mi){
   var cursor=0,last=0,max=0,measureAcc={};
   var result={notes:[],rests:[],marks:[],harmony:[],time:[],key:[],clef:[],bar:[],dur:0,index:mi};
   for(var ci=0;ci<m.children.length;ci++){
    var node=m.children[ci],tag=node.tagName;
    if(tag==="attributes"){
     var newDivisions=num(node,"divisions",cfg.div);
     if(newDivisions>0)cfg.div=newDivisions;
     var t=direct(node,"time");if(t){
      cfg.beats=num(t,"beats",cfg.beats);cfg.unit=num(t,"beat-type",cfg.unit);
      result.time.push({at:cursor,text:cfg.beats+"/"+cfg.unit});
     }
     var k=direct(node,"key");if(k){
      cfg.fifths=num(k,"fifths",0);measureAcc={};
      result.key.push({at:cursor,fifths:cfg.fifths});
     }
     children(node,"clef").forEach(function(c){
      var staff=splitHands?pi+1:+att(c,"number","1");
      cfg.clefs[staff]=value(c,"sign",staff===1?"G":"F");
      result.clef.push({at:cursor,staff:staff,sign:cfg.clefs[staff]});
     });
    }else if(tag==="harmony"){
     // Only documented MusicXML harmony is shown. Never guess missing chords.
     var harmonic=parseHarmony(node);
     if(harmonic){
      harmonic.at=cursor+num(node,"offset",0)/cfg.div;
      harmonic.staff=splitHands?pi+1:num(node,"staff",0);
      result.harmony.push(harmonic);
     }
    }else if(tag==="backup"){cursor-=num(node,"duration",0)/cfg.div;}
    else if(tag==="forward"){cursor+=num(node,"duration",0)/cfg.div;max=Math.max(max,cursor);}
    else if(tag==="note"){
     var chord=!!direct(node,"chord"),grace=!!direct(node,"grace"),rest=!!direct(node,"rest");
     var dur=num(node,"duration",0)/cfg.div,beat=chord?last:cursor;
     if(!chord)last=cursor;
     var staff=splitHands?pi+1:num(node,"staff",pi===0?1:2);
     var voice=value(node,"voice","1");
     var mark={at:beat,dur:dur,staff:staff,voice:voice,measure:mi,sourcePart:pi,xmlOrder:ci,grace:grace,
       velocity:80,dynamicsOverride:+att(node,"dynamics","NaN"),
       graceSlash:grace&&att(direct(node,"grace"),"slash","no")==="yes",
       rest:rest,beam:children(node,"beam").map(function(b){return {level:+att(b,"number","1"),v:b.textContent.trim()};}),
       dot:children(node,"dot").length,stem:value(node,"stem",""),
       type:value(node,"type",""),ties:children(node,"tie").map(function(t){return att(t,"type","");}),
       slur:[],acc:value(node,"accidental",""),art:[],
       tupletActual:0,tupletNormal:0,tupletStart:false,tupletStop:false,
       tupletNumber:"1",tupletBracket:"",tupletPlacement:"",tupletMarks:[]};
     var timeMod=direct(node,"time-modification");
     if(timeMod){
      mark.tupletActual=num(timeMod,"actual-notes",0);
      mark.tupletNormal=num(timeMod,"normal-notes",0);
     }
     var nts=direct(node,"notations");
     if(nts){
      mark.slur=children(nts,"slur").map(function(s){return {type:att(s,"type",""),number:att(s,"number","1")};});
      var ar=direct(nts,"articulations");
      if(ar)mark.art=Array.prototype.map.call(ar.children,function(x){return x.tagName;});
      children(nts,"tuplet").forEach(function(t){
       var notation={type:att(t,"type",""),number:att(t,"number","1"),
         bracket:att(t,"bracket",""),placement:att(t,"placement",""),
         actual:num(direct(t,"tuplet-actual"),"tuplet-number",0),
         normal:num(direct(t,"tuplet-normal"),"tuplet-number",0)};
       mark.tupletMarks.push(notation);
       if(notation.type==="start")mark.tupletStart=true;
       if(notation.type==="stop")mark.tupletStop=true;
       mark.tupletNumber=notation.number;
       mark.tupletBracket=notation.bracket;
       mark.tupletPlacement=notation.placement;
      });
     }
     if(!rest){
      var pitch=direct(node,"pitch");
      if(pitch){
       mark.step=value(pitch,"step","C");mark.oct=num(pitch,"octave",4);
       mark.alter=num(pitch,"alter",0);
       // The pitch's chromatic alteration and its PRINTED accidental differ.
       // Compare against the key/measure state without changing MIDI pitch.
       var spellKey=staff+":"+mark.step+":"+mark.oct;
       var baseline=Object.prototype.hasOwnProperty.call(measureAcc,spellKey)
        ?measureAcc[spellKey]:keyDefaultAlter(mark.step,cfg.fifths);
       if(!mark.acc&&mark.ties.indexOf("stop")<0&&mark.alter!==baseline)
        mark.acc=accidentalFromAlter(mark.alter);
       measureAcc[spellKey]=mark.alter;
       (result.notes).push(mark);
      }else{unsupported.push("nota sem altura no compasso "+(mi+1));}
     }else{result.rests.push(mark);}
     if(!chord&&!grace){cursor+=dur;max=Math.max(max,cursor);}
    }else if(tag==="direction"||tag==="sound"){
     var sound=tag==="sound"?node:direct(node,"sound"),quarterBpm=NaN;
     var soundOffset=(direct(sound,"offset")?num(sound,"offset",0):num(node,"offset",0))/cfg.div;
     var percent=sound?+att(sound,"dynamics","NaN"):NaN;
     if(Number.isFinite(percent)&&percent>=0)dynamics.push({part:pi,measure:mi,at:cursor+soundOffset,
      staff:splitHands?pi+1:num(node,"staff",0),voice:value(node,"voice",""),percent:percent});
     if(sound&&sound.hasAttribute("tempo"))quarterBpm=+sound.getAttribute("tempo");
     else{
      var printed=direct(direct(node,"direction-type"),"metronome");
      if(printed){
       var unit=value(printed,"beat-unit","quarter"),
        factors={whole:4,half:2,quarter:1,eighth:.5,"16th":.25,"32nd":.125};
       var factor=factors[unit],dots=children(printed,"beat-unit-dot").length,dotFactor=1,portion=.5;
       for(var di=0;di<Math.min(3,dots);di++){dotFactor+=portion;portion/=2;}
       if(typeof factor==="number")
        quarterBpm=+value(printed,"per-minute","")*factor*dotFactor;
      }
     }
     if(isFinite(quarterBpm)&&quarterBpm>=10&&quarterBpm<=600){
      var tempoOffset=soundOffset;
      tempo.push({part:pi,measure:mi,at:cursor+tempoOffset,bpm:quarterBpm});
     }
     var dt=direct(node,"direction-type");
     if(dt){
      var words=value(dt,"words","");
      var printedDynamics=direct(dt,"dynamics");
      if(printedDynamics&&printedDynamics.children.length)words=printedDynamics.children[0].tagName;
      if(direct(dt,"pedal"))words="Ped.";
      if(direct(dt,"segno")&&!words)words="Segno";
      if(direct(dt,"coda")&&!words)words="Coda";
      if(words)result.marks.push({at:cursor,text:words});
     }
    }else if(tag==="barline"){
     var rp=direct(node,"repeat"),ending=direct(node,"ending");
     result.bar.push({position:att(node,"location","right"),
       repeat:att(rp,"direction",""),ending:att(ending,"number",""),
       endingType:att(ending,"type","")});
    }
   }
   var nominal=cfg.beats*4/cfg.unit;
   result.dur=Math.max(max,nominal);
   if(mi===0&&att(m,"implicit","no")==="yes"&&max>0)result.dur=max;
   if(result.dur<=0)result.dur=Math.max(max,1);
   lengths[mi]=Math.max(lengths[mi]||0,result.dur);
   measures.push(result);
  });
  parsed.push(measures);
 });
 var starts=[0];
 for(var i=0;i<lengths.length;i++)starts.push(starts[i]+lengths[i]);
 // Tempo directions share the same absolute score-quarter-note clock as Java.
 tempo.forEach(function(entry){entry.beat=Math.max(0,starts[entry.measure]+entry.at);});
 tempo.sort(function(a,b){return a.beat-b.beat||a.part-b.part;});
 tempo=tempo.filter(function(entry,index){
  return index===0 || Math.abs(entry.beat-tempo[index-1].beat)>0.00000001;
 });
 dynamics.forEach(function(e){e.beat=Math.max(0,starts[e.measure]+e.at);});
 dynamics.sort(function(a,b){return a.beat-b.beat;});
 // Ending brackets remain on their ORIGINAL measures; musical playback may
 // revisit/skip them, but the score rendering must never duplicate notes.
 var endings=[],openEnding=null;
 (parsed[0]||[]).forEach(function(m,mi){
  m.bar.forEach(function(b){
   if(b.endingType==="start"){
    if(openEnding)endings.push({start:openEnding.start,end:starts[mi],number:openEnding.number});
    openEnding={start:starts[mi]+(b.position==="right"?m.dur:0),number:b.ending};
   }
   if((b.endingType==="stop"||b.endingType==="discontinue")&&openEnding){
    var endingAt=b.position==="left"?starts[mi]:starts[mi+1];
    endings.push({start:openEnding.start,end:endingAt,number:openEnding.number});
    openEnding=null;
   }
  });
 });
 if(openEnding)endings.push({start:openEnding.start,end:starts[starts.length-1],
  number:openEnding.number});
 var notes=[],rests=[],bars=[],marks=[],times=[],keys=[],clefs=[];
 parsed.forEach(function(measures,pi){
  measures.forEach(function(m,mi){
   var base=starts[mi],add=function(a,out){a.forEach(function(e){e.at+=base;out.push(e);});};
   // Harmonic indication belongs only to its written measure and staff.
   // It is not extrapolated into later measures or invented from pitches.
   m.notes.forEach(function(n){
    var nearest=null;
    m.harmony.forEach(function(h){
     if(h.at<=n.at+1e-8&&(!h.staff||h.staff===n.staff)
        &&(!nearest||nearest.at<=h.at))nearest=h;
    });
    if(nearest)n.harmony={symbol:nearest.symbol,name:nearest.name};
   });
   add(m.notes,notes);add(m.rests,rests);add(m.marks,marks);
   add(m.time,times);add(m.key,keys);add(m.clef,clefs);
   if(pi===0){
    bars.push({at:base,number:mi+1,start:true,repeat:m.bar.filter(function(x){return x.position==="left";})});
    if(mi===lengths.length-1)bars.push({at:starts[mi+1],number:mi+2,end:true,repeat:m.bar.filter(function(x){return x.position!=="left";})});
    else if(m.bar.some(function(x){return x.repeat==="backward";}))marks.push({at:starts[mi+1],text:":||"});
   }
  });
 });
 notes.forEach(function(n){
  var percent=null;
  dynamics.forEach(function(d){if(d.beat<=n.at+1e-8&&d.part===n.sourcePart&&(!d.staff||d.staff===n.staff)&&(!d.voice||d.voice===n.voice))percent=d.percent;});
  if(Number.isFinite(n.dynamicsOverride)&&n.dynamicsOverride>=0)percent=n.dynamicsOverride;
  if(percent!==null)n.velocity=Math.round(Math.max(0,Math.min(127,percent*.9)));
 });
 notes.sort(function(a,b){return a.at-b.at || a.staff-b.staff;});
 // Notes are engraved against the clef active at their own musical timestamp.
 // Otherwise a mid-score clef change would silently draw pitches incorrectly.
 clefs.sort(function(a,b){return a.at-b.at;});
 notes.forEach(function(n){
  var sign=n.staff===1?"G":"F";
  clefs.forEach(function(c){if(c.staff===n.staff&&c.at<=n.at+0.00001)sign=c.sign;});
  n.clef=sign;
 });
 rests.sort(function(a,b){return a.at-b.at || a.staff-b.staff;});
 arrangeVoices(notes,rests);
 notes.forEach(function(n,i){n._noteIndex=i;});
 bars.sort(function(a,b){return a.at-b.at;});
 var partStaves=new Set(notes.map(function(n){return n.staff;}));
 if(partStaves.size>2)throw Error("Partitura com mais de duas pautas: modo de estudo temporal ainda não compatível.");
 return {notes:notes,rests:rests,bars:bars,marks:marks,times:times,keys:keys,clefs:clefs,
  duration:starts[starts.length-1],tempo:tempo,dynamics:dynamics,endings:endings,
  unsupported:unsupported};
}
function computeX(beat,pxPerBeat,origin){return origin+beat*pxPerBeat;}
function cursorPx(width){return width*.4;}
function cursorLeadPx(width){return Math.max(45,Math.min(70,width*.08));}
function msPerBeat(bpm){return 60000/Math.max(1,bpm);}
function svgEl(type,props,parent){
 var e=document.createElementNS(NS,type);for(var key in props)if(props.hasOwnProperty(key))e.setAttribute(key,props[key]);
 if(parent)parent.appendChild(e);return e;
}
function drawText(parent,txt,x,y,sz,col,extra){
 var a={x:x,y:y,"font-size":sz,fill:col||"#243851","font-family":"sans-serif"};
 if(extra)for(var k in extra)a[k]=extra[k];
 var e=svgEl("text",a,parent);e.textContent=txt;return e;
}
function line(p,x1,y1,x2,y2,color,w){
 return svgEl("line",{x1:x1,y1:y1,x2:x2,y2:y2,stroke:color||"#8B9BAC","stroke-width":w||1},p);
}
function keyDefaultAlter(step,fifths){
 var order=fifths>0?SHARPS:FLATS,idx=order.indexOf(step);
 return idx>=0&&idx<Math.abs(fifths)?(fifths>0?1:-1):0;
}
function accidentalFromAlter(alter){
 if(alter===0)return "natural";
 if(alter===1)return "sharp";
 if(alter===-1)return "flat";
 if(alter===2)return "double-sharp";
 if(alter===-2)return "double-flat";
 return "";
}
/** Written symbol is not the actual duration when there are dots or tuplets. */
function writtenDuration(n){
 var units={maxima:32,long:16,breve:8,whole:4,half:2,quarter:1,
  eighth:.5,"16th":.25,"32nd":.125,"64th":.0625,"128th":.03125,"256th":.015625};
 return n.type&&units[n.type]!==undefined?units[n.type]:n.dur;
}
function pitchDegree(n){return n.oct*7+(STEP[n.step]||0);}
function pitchY(n){
 // Treble lower line E4 (30), bass lower line G2 (18).
 var top=n.staff===1?49:171,space=13.2;
 // Lower-line pitch: treble=E4, bass=G2, alto C-clef=F3.
 var lower=n.clef==="F"?18:n.clef==="C"?24:30;
 return top+4*space-(pitchDegree(n)-lower)*space/2;
}
function staffY(staff){return staff===1?49:171;}
function noteColor(){return "#1E3049";}
/** Print-time voice layout: pitch and performed time remain unchanged. */
function arrangeVoices(notes,rests){
 var byMoment={},byMeasure={};
 notes.concat(rests).forEach(function(n){
  var key=n.staff+":"+n.measure;
  if(!byMeasure[key])byMeasure[key]={};
  byMeasure[key][String(n.voice)]=true;
 });
 var graceGroups={};
 notes.forEach(function(n){
  var id=n.staff+":"+n.at.toFixed(6);
  if(n.grace){
   var graceKey=id+":"+n.voice;
   if(!graceGroups[graceKey])graceGroups[graceKey]=[];
   graceGroups[graceKey].push(n);
  }else{
   if(!byMoment[id])byMoment[id]=[];
   byMoment[id].push(n);
  }
 });
 function ranks(group){
  return Object.keys(group).sort(function(a,b){
   var na=+a,nb=+b;
   return isFinite(na)&&isFinite(nb)?na-nb:String(a).localeCompare(String(b));
  });
 }
 notes.concat(rests).forEach(function(n){
  var voices=ranks(byMeasure[n.staff+":"+n.measure]||{}),
    rank=voices.indexOf(String(n.voice)),multi=voices.length>1;
  n._engraveMultiVoice=multi;
  n._visualStem=multi?(rank%2===0?"up":"down"):(n.stem||"");
  n._headShift=0;
  n._graceShift=0;
  n._accOffset=-24;
  n._accVisible=!!n.acc;
  n._voiceRank=rank;
  if(n.rest){
   var voiceLevel=Math.floor(rank/2);
   n._restOffset=multi?(rank%2===0?-15-14*voiceLevel:15+14*voiceLevel):0;
  }
 });
 Object.keys(byMoment).forEach(function(id){
  var group=byMoment[id].slice().sort(function(a,b){return pitchY(a)-pitchY(b);});
  for(var i=1;i<group.length;i++){
   var a=group[i-1],b=group[i];
   if(Math.abs(pitchY(a)-pitchY(b))<=6.61)
    b._headShift=a._headShift+12;
  }
  // Stagger simultaneous accidentals in independent columns, without
  // changing onset or chromatic pitch. Two identical signs share one glyph.
  var columns=[],same={};
  group.filter(function(n){return !!n.acc;}).forEach(function(n){
   var signature=n.step+":"+n.oct+":"+n.alter+":"+n.acc;
   if(same[signature]){n._accVisible=false;return;}
   same[signature]=true;
   var col=0;
   while(columns.some(function(p){return p.col===col&&Math.abs(p.y-pitchY(n))<26;}))
    col++;
   columns.push({col:col,y:pitchY(n)});
   n._accOffset=-24-col*19-(n._headShift||0);
  });
 });
 Object.keys(graceGroups).forEach(function(id){
  var group=graceGroups[id].sort(function(a,b){return a.xmlOrder-b.xmlOrder;});
  group.forEach(function(n,i){n._graceShift=-(group.length-i)*21;});
 });
 return notes;
}
function drawNote(parent,n,x){
 var y=pitchY(n),duration=writtenDuration(n);
 x+=(n._headShift||0)+(n._graceShift||0);
 var hollow=duration>=1.999,whole=duration>=3.999;
 var col=noteColor();
 // MusicXML quarter-note timestamp controls x regardless of printed note width.
 var g=svgEl("g",{"class":n.grace?"temporal-grace-note":"musical-event",
  "data-beat":n.at.toFixed(4),"data-staff":n.staff,"data-symbolic-type":n.type||"",
  "data-tuplet-actual":n.tupletActual||0,"data-note-index":n._noteIndex},parent);
 // Cue-size grace figures do not alter the MIDI performance timeline.
 if(n.grace)g.setAttribute("transform","translate("+x+" "+y+") scale(.67) translate("+(-x)+" "+(-y)+")");
 // Ledger lines are not stretched with the duration.
 var top=staffY(n.staff),bottom=top+52.8;
 if(y<top-2)for(var ly=top-13.2;ly>=y-0.1;ly-=13.2)line(g,x-10.5,ly,x+10.5,ly,"#8B9BAC",1);
 if(y>bottom+2)for(var ly2=bottom+13.2;ly2<=y+0.1;ly2+=13.2)line(g,x-10.5,ly2,x+10.5,ly2,"#8B9BAC",1);
 var head=svgEl("ellipse",{cx:x,cy:y,rx:whole?9.8:8.8,ry:whole?6:5.7,fill:hollow?"white":col,
  stroke:col,"stroke-width":hollow?1.9:1,transform:"rotate(-16 "+x+" "+y+")",
  "aria-label":noteIdentification(n)||""},g);
 head.setAttribute("style","pointer-events:all;cursor:pointer;");
 if(!whole){
  var direction=n._engraveMultiVoice?n._visualStem:(n.stem||n._visualStem||"");
   var up=direction==="up"||(direction!=="down"&&y>=top+26);
  var stemX=x+(up?7.5:-7.5),
       stemTop=Number.isFinite(n._beamTip)?n._beamTip:y+(up?-38:38);
  line(g,stemX,y,stemX,stemTop,col,1.7);
  n._stemX=stemX;n._stemY=stemTop;n._stemUp=up;
  var beamAttached=n.grace?!!n._graceBeamed:n.beam.some(function(b){return b.level===1&&(b.v==="begin"||b.v==="continue"||b.v==="end");});
  // Unbeamed eighth/sixteenth grace notes still need printed flags.
  if(!beamAttached&&duration<0.75){
   var path=up?("M"+stemX+" "+stemTop+" q 18 8 9 24"):("M"+stemX+" "+stemTop+" q -18 -8 -9 -24");
   svgEl("path",{d:path,fill:"none",stroke:col,"stroke-width":3},g);
   if(duration<=.25){
    svgEl("path",{d:up?("M"+stemX+" "+(stemTop+9)+" q 15 8 8 21"):("M"+stemX+" "+(stemTop-9)+" q -15 -8 -8 -21"),
     fill:"none",stroke:col,"stroke-width":2.4},g);
   }
  }
 }
 if(n.dot)for(var di=0;di<n.dot;di++)svgEl("circle",{cx:x+13+di*6,cy:y-3,r:2.1,fill:col},g);
 var accidentals={"sharp":"♯","flat":"♭","natural":"♮","double-sharp":"×","double-flat":"♭♭"};
 if(n._accVisible!==false&&n.acc&&accidentals[n.acc])
  drawText(g,accidentals[n.acc],x+(n._accOffset===undefined?-24:n._accOffset),y+5,24,col);
 if(n.art.indexOf("staccato")>=0)svgEl("circle",{cx:x,cy:y+(n._stemUp?11:-12),r:2,fill:col},g);
 if(n.art.indexOf("accent")>=0)drawText(g,">",x-4,y+(n._stemUp?20:-20),14,col);
 // Tuplet numbers/brackets are drawn as continuous spans across SVG tiles.
 if(n.grace&&n.graceSlash&&!whole){
  var slashX=x+(n._stemUp?3:-16),slashY=y+(n._stemUp?-23:23);
  line(g,slashX,slashY+7,slashX+16,slashY-8,col,2.1);
 }
}
function drawRest(parent,n,x){
 var y=staffY(n.staff)+26+(n._restOffset||0),clr="#1E3049";
 var g=svgEl("g",{"class":"temporal-rest","data-beat":n.at.toFixed(4)},parent);
 var displayDuration=writtenDuration(n);
 if(displayDuration>=1.75){
  // Whole and half rests attach by notated type, not performed tuplet duration.
  var ry=(displayDuration>=3.5?staffLineY(n.staff,1):
   staffLineY(n.staff,2)-6)+(n._restOffset||0);
  svgEl("rect",{x:x-7,y:ry,width:14,height:6,fill:clr},g);
 }else{
  // All remaining rests use hand-crafted vector outlines, avoiding
  // supplementary Unicode glyphs that crash older Android GPU drivers.
  svgEl("path",{d:"M"+(x+4)+" "+(y-14)+" L"+(x-5)+" "+(y-2)+
   " L"+(x+4)+" "+(y+6)+" L"+(x-6)+" "+(y+22),
   fill:"none",stroke:clr,"stroke-width":3.3,"stroke-linejoin":"round"},g);
  if(displayDuration<.85)svgEl("circle",{cx:x+7,cy:y-15,r:3.7,fill:clr},g);
  if(displayDuration<.35)svgEl("circle",{cx:x+8,cy:y-3,r:3.2,fill:clr},g);
 }
}
function drawBeamGroups(parent,notes,xp){
 var grouped={};
 notes.forEach(function(n){
  if(!n._stemX||!n.beam.some(function(b){return b.level===1;}))return;
  var k=n.staff+":"+n.voice;
  if(!grouped[k])grouped[k]=[];
  grouped[k].push(n);
 });
 Object.keys(grouped).forEach(function(k){
  var current=[];
  function commit(){
   if(current.length<2){current=[];return;}
   var a=current[0],b=current[current.length-1],slope=(b._stemY-a._stemY)/Math.max(1,b._stemX-a._stemX);
   // Keep beam slant modest, preserving rhythmic grouping.
   slope=Math.max(-.12,Math.min(.12,slope));
   var mid=(a._stemY+b._stemY)/2,ay=mid-slope*(b._stemX-a._stemX)/2,by=mid+slope*(b._stemX-a._stemX)/2;
   var delta=a._stemUp?5:-5;
   var d="M"+a._stemX+" "+ay+" L"+b._stemX+" "+by+" L"+b._stemX+" "+(by+delta)+" L"+a._stemX+" "+(ay+delta)+" Z";
   svgEl("path",{d:d,fill:"#1E3049"},parent);
   current=[];}
  grouped[k].forEach(function(n){
   var b=n.beam.filter(function(e){return e.level===1;})[0];
   if(!b)return;
   if(b.v==="begin"){commit();current=[n];}
   else if(b.v==="continue")current.push(n);
   else if(b.v==="end"){current.push(n);commit();}
   else commit();
  });
  commit();
 });
}
/** Assemble MusicXML beaming once for the complete timeline, not per tile.
 * Return continuous level-1 beams, additional levels and standalone hooks.
 * Data remain keyed by staff/voice so a lower-voice beam never joins upper.
 */
function collectBeamGroups(notes,absoluteX){
 var byVoice={},runs=[];
 notes.forEach(function(n){
  // Grace groups share one performed beat but have independent engraved
  // positions and their own small beams, never combined with timed notes.
  var key=n.staff+":"+n.voice+(n.grace?":grace:"+n.at.toFixed(6):"");
  if(!byVoice[key])byVoice[key]=[];
  byVoice[key].push(n);
 });
 Object.keys(byVoice).forEach(function(key){
  var active={};
  byVoice[key].sort(function(a,b){return a.at-b.at||(a.xmlOrder||0)-(b.xmlOrder||0);}).forEach(function(n){
   (n.beam||[]).forEach(function(mark){
    var level=+mark.level||1,kind=mark.v;
    if(level<1||level>5)return;
    if(kind==="forward hook"||kind==="backward hook"){
     runs.push({staff:n.staff,voice:n.voice,level:level,
       notes:[n],hook:kind==="forward hook"?1:-1});
    }else if(kind==="begin"){
     if(active[level]&&active[level].length>1)
      runs.push({staff:n.staff,voice:n.voice,level:level,notes:active[level]});
     active[level]=[n];
    }else if(kind==="continue"||kind==="end"){
     if(!active[level])return;
     active[level].push(n);
     if(kind==="end"){
      if(active[level].length>1)
       runs.push({staff:n.staff,voice:n.voice,level:level,notes:active[level]});
      delete active[level];
     }
    }
   });
  });
 });
 // First beams determine shared stems: perform this before rendering notes.
 runs.filter(function(g){return g.level===1&&g.notes.length>1;})
 .forEach(function(g){
  var a=g.notes[0],b=g.notes[g.notes.length-1];
  var firstX=absoluteX(a.at)+(a._headShift||0)+(a._graceShift||0),
      lastX=absoluteX(b.at)+(b._headShift||0)+(b._graceShift||0);
  if(lastX<=firstX)return;
  function up(n){
   if(n._engraveMultiVoice)return n._visualStem!=="down";
   var direction=n.stem||n._visualStem||"";
   return direction==="up"||(direction!=="down"&&pitchY(n)>=staffY(n.staff)+26);
  }
  var grace=!!a.grace,scale=grace?.67:1,upward=up(a),
      stemDelta=grace?(upward?7.5:-7.5)*scale:0,
      x0=firstX+stemDelta,x1=lastX+stemDelta,
      baseA=pitchY(a)+(upward?-38:38)*scale,
      baseB=pitchY(b)+(upward?-38:38)*scale;
  var slope=Math.max(-.12,Math.min(.12,(baseB-baseA)/(x1-x0)));
  var average=(baseA+baseB)/2,mid=(x0+x1)/2;
  g._grace=grace;g._up=upward;g._x0=x0;g._x1=x1;g._slope=slope;
  g._y0=average+slope*(x0-mid);
  g.notes.forEach(function(n){
   var x=absoluteX(n.at)+(n._headShift||0)+(n._graceShift||0)+stemDelta;
   var visualTip=g._y0+slope*(x-x0);
   // drawNote scales the grace group around its own notehead:
   // visibleTip = .67 * originalTip + .33 * pitchY(note).
   n._beamTip=grace?(visualTip-(1-scale)*pitchY(n))/scale:visualTip;
   if(grace)n._graceBeamed=true;
  });
 });
 // More distant sub-levels use the first-level beam's common direction.
 runs.forEach(function(g){
  if(g._x0!==undefined)return;
  var a=g.notes[0],b=g.notes[g.notes.length-1],
      grace=!!a.grace,scale=grace?.67:1,
      up=a._engraveMultiVoice?a._visualStem!=="down":
       a._visualStem==="up"||(a._visualStem!=="down"&&pitchY(a)>=staffY(a.staff)+26),
      stemDelta=grace?(up?7.5:-7.5)*scale:0,
      x0=absoluteX(a.at)+(a._headShift||0)+(a._graceShift||0)+stemDelta,
      x1=absoluteX(b.at)+(b._headShift||0)+(b._graceShift||0)+stemDelta;
  function visibleTip(n){
   var tip=Number.isFinite(n._beamTip)?n._beamTip:
      pitchY(n)+(up?-38:38);
   return grace?scale*tip+(1-scale)*pitchY(n):tip;
  }
  var primaryA=visibleTip(a),primaryB=visibleTip(b);
  g._grace=grace;g._up=up;
  g._x0=x0;
  g._x1=g.hook?x0+g.hook*13*scale:x1;
  g._slope=x1>x0?Math.max(-.12,Math.min(.12,(primaryB-primaryA)/(x1-x0))):0;
  g._y0=primaryA+(g.level-1)*(up?8:-8)*scale;
 });
 return runs;
}
/** Clip a filled 5px beam polygon to one SVG tile. */
function drawScoreBeams(parent,runs,tileStart,tileWidth){
 var right=tileStart+tileWidth,count=0;
 runs.forEach(function(g){
  if(!Number.isFinite(g._x0)||!Number.isFinite(g._x1))return;
  var left=Math.max(tileStart,Math.min(g._x0,g._x1)),
      end=Math.min(right,Math.max(g._x0,g._x1));
  if(end<=left)return;
  var y0=g._y0+g._slope*(left-g._x0),
      y1=g._y0+g._slope*(end-g._x0),
      delta=(g._up?5:-5)*(g._grace?.67:1);
  svgEl("path",{"class":"temporal-beam",
    "data-beam-level":g.level,"data-beam-voice":g.voice,
    d:"M"+(left-tileStart)+" "+y0+" L"+(end-tileStart)+" "+y1+
      " L"+(end-tileStart)+" "+(y1+delta)+" L"+(left-tileStart)+" "+(y0+delta)+" Z",
    fill:"#1E3049"},parent);
  count++;
 });
 return count;
}
function drawTies(parent,notes,xp){
 var starts={};
 notes.forEach(function(n){
  var key=n.staff+":"+n.voice+":"+n.step+":"+n.oct+":"+n.alter;
  if(n.ties.indexOf("stop")>=0&&starts[key]){
   var a=starts[key],x1=xp(a.at)+8,x2=xp(n.at)-8,y=pitchY(n)+14;
   if(x2>x1+7)svgEl("path",{d:"M"+x1+" "+y+" Q"+((x1+x2)/2)+" "+(y+13)+" "+x2+" "+y,
    fill:"none",stroke:"#1E3049","stroke-width":1.8},parent);
   delete starts[key];
  }
  if(n.ties.indexOf("start")>=0)starts[key]=n;
 });
}
/**
 * Conventional five-line staff, indexed 0 at the TOP line.
 * Reference pitches: G4 = treble line 2 from bottom (row 3),
 * F3 = bass line 4 from bottom (row 1).
 * Clef outlines below are original SVG geometry and require no system font.
 */
var STAFF_STEP=13.2;
function staffLineY(staff,row){return staffY(staff)+row*STAFF_STEP;}
function clefAnchor(staff,sign){
 var row=sign==="F"?1:sign==="C"?2:3;
 return {x:0,y:staffLineY(staff,row),row:row,
  midi:sign==="F"?53:sign==="C"?60:67};
}
/** Phrasing slurs are engravings, not sustained MIDI ties.
 * Visible arcs are rendered for start/stop pairs within the current tile. */
function drawSlurs(parent,notes,xp){
 var beginnings={};
 notes.forEach(function(n){
  (n.slur||[]).forEach(function(mark){
   var id=n.staff+":"+n.voice+":"+mark.number;
   if(mark.type==="start")beginnings[id]=n;
   else if(mark.type==="stop"&&beginnings[id]){
    var a=beginnings[id],x1=xp(a.at),x2=xp(n.at);
    if(x2>x1+18){
     var above=(a._stemUp||n._stemUp)?false:true;
     var shift=above?-12:12;
     var y1=pitchY(a)+shift,y2=pitchY(n)+shift;
     var middle=(y1+y2)/2+(above?-17:17);
     svgEl("path",{"class":"temporal-slur",
      d:"M"+x1+" "+y1+" Q"+((x1+x2)/2)+" "+middle+" "+x2+" "+y2,
      fill:"none",stroke:"#314A67","stroke-width":1.7,
      "stroke-linecap":"round"},parent);
    }
    delete beginnings[id];
   }
  });
 });
}
/** Pair musical spans over the entire score, not the notes in one SVG tile. */
function collectSpanners(notes){
 var slurs={},ties={},tuplets={},spans=[];
 notes.forEach(function(n){
  var voice=n.staff+":"+n.voice;
  (n.slur||[]).forEach(function(m){
   var key=voice+":"+m.number;
   if(m.type==="start")slurs[key]=n;
   else if(m.type==="stop"&&slurs[key]){
    spans.push({kind:"slur",from:slurs[key],to:n,staff:n.staff,voice:n.voice});
    delete slurs[key];
   }
  });
  var tied=voice+":"+n.step+":"+n.oct+":"+n.alter;
  if((n.ties||[]).indexOf("stop")>=0&&ties[tied]){
   spans.push({kind:"tie",from:ties[tied],to:n,staff:n.staff,voice:n.voice});
   delete ties[tied];
  }
  if((n.ties||[]).indexOf("start")>=0)ties[tied]=n;
  var marks=n.tupletMarks&&n.tupletMarks.length?n.tupletMarks:[];
  // Legacy test fixtures can still specify just one start/stop.
  if(!marks.length){
   if(n.tupletStart)marks.push({type:"start",number:n.tupletNumber||"1",
     bracket:n.tupletBracket||"",placement:n.tupletPlacement||""});
   if(n.tupletStop)marks.push({type:"stop",number:n.tupletNumber||"1"});
  }
  marks.forEach(function(m){
   if(m.type!=="start")return;
   var key=voice+":"+m.number;
   if(!tuplets[key])tuplets[key]=[];
   tuplets[key].push({note:n,notation:m});
  });
  marks.forEach(function(m){
   if(m.type!=="stop")return;
   var key=voice+":"+m.number,stack=tuplets[key];
   if(!stack||!stack.length)return;
   var start=stack.pop(),from=start.note;
   var ratio=start.notation.actual||from.tupletActual||n.tupletActual||0;
   if(n.at>=from.at&&ratio>1)
    spans.push({kind:"tuplet",from:from,to:n,staff:n.staff,voice:n.voice,
     number:ratio,id:m.number,bracket:start.notation.bracket!=="no",
     above:start.notation.placement?start.notation.placement==="above":
       from._visualStem==="up"});
  });
 });
 // Separate overlapping tuplet brackets into vertical tracks, including
 // nested groups with different MusicXML number identifiers.
 var groups=spans.filter(function(sp){return sp.kind==="tuplet";});
 groups.sort(function(a,b){return a.from.at-b.from.at||b.to.at-a.to.at;});
 groups.forEach(function(group){
  var lane=0;
  groups.forEach(function(parent){
   if(parent===group||parent._lane===undefined)return;
   if(parent.staff!==group.staff||parent.above!==group.above)return;
   if(parent.from.at<=group.from.at&&parent.to.at>=group.to.at&&
       (parent.from.at<group.from.at||parent.to.at>group.to.at||
        parent.id!==group.id))
    lane=Math.max(lane,parent._lane+1);
  });
  group._lane=lane;
 });
 return spans;
}
/** Exact quadratic section with x(t) linear between its endpoints. */
function bezierPiece(y1,y2,arch,left,right,start,end){
 if(end-start<1||right<=start||left>=end)return null;
 var a=Math.max(0,(left-start)/(end-start)),
     b=Math.min(1,(right-start)/(end-start)),control=(y1+y2)/2+arch;
 if(b<=a+1e-9)return null;
 function y(t){return (1-t)*(1-t)*y1+2*(1-t)*t*control+t*t*y2;}
 function derivative(t){return 2*((1-t)*(control-y1)+t*(y2-control));}
 return {x1:start+(end-start)*a,y1:y(a),
   cx:start+(end-start)*(a+b)/2,cy:y(a)+derivative(a)*(b-a)/2,
   x2:start+(end-start)*b,y2:y(b)};
}
/** Draw arcs and tuplet brackets only within the corresponding tile.
 * Endpoints are sourced from the complete MusicXML score. */
function drawScoreSpanners(parent,spans,tileStart,tileWidth,absoluteX){
 var right=tileStart+tileWidth,painted=0;
 spans.forEach(function(s){
  var leftX=absoluteX(s.from.at)+(s.kind==="tuplet"?0:(s.from._headShift||0)),
      rightX=absoluteX(s.to.at)+(s.kind==="tuplet"?0:(s.to._headShift||0));
  if(rightX<leftX){var tmp=rightX;rightX=leftX;leftX=tmp;}
  if(rightX<tileStart||leftX>right||rightX-leftX<1)return;
  if(s.kind==="tuplet"){
   var lane=s._lane||0;
   var y=s.above?staffY(s.staff)-16-17*lane:
     staffY(s.staff)+70+17*lane;
   var group=svgEl("g",{"class":"temporal-tuplet","data-tuplet-number":s.number,
     "data-span-start":s.from.at,"data-span-end":s.to.at,
     "data-tuplet-lane":lane,"data-tuplet-id":s.id||"1"},parent);
   var x0=leftX-6,x1=rightX+6,mid=(x0+x1)/2,
       lo=Math.max(x0,tileStart),hi=Math.min(x1,right);
   if(s.bracket){
    if(lo<Math.min(hi,mid-9))
     line(group,lo-tileStart,y,Math.min(hi,mid-9)-tileStart,y,"#314A67",1.6);
    if(Math.max(lo,mid+9)<hi)
     line(group,Math.max(lo,mid+9)-tileStart,y,hi-tileStart,y,"#314A67",1.6);
    if(x0>=tileStart&&x0<=right)
     line(group,x0-tileStart,y,x0-tileStart,y+(s.above?8:-8),"#314A67",1.5);
    if(x1>=tileStart&&x1<=right)
     line(group,x1-tileStart,y,x1-tileStart,y+(s.above?8:-8),"#314A67",1.5);
   }
   if(mid>=tileStart&&mid<right)
    drawText(group,String(s.number),mid-tileStart-4,
      y+(s.above?-4:12),14,"#263F5A",{"font-weight":"700"});
   painted++;return;
  }
  var shift=s.kind==="tie"?14:
   (s.from._engraveMultiVoice?s.from._visualStem==="up":
     s.from._visualStem==="down")?-13:13;
  var piece=bezierPiece(pitchY(s.from)+shift,pitchY(s.to)+shift,
    s.kind==="tie"?12:(shift<0?-16:16),
    tileStart,right,leftX+8,rightX-8);
  if(!piece)return;
  svgEl("path",{"class":s.kind==="tie"?"temporal-tie":"temporal-slur",
    "data-span-start":s.from.at,"data-span-end":s.to.at,
    d:"M"+(piece.x1-tileStart)+" "+piece.y1+
      " Q"+(piece.cx-tileStart)+" "+piece.cy+
      " "+(piece.x2-tileStart)+" "+piece.y2,
    fill:"none",stroke:"#314A67","stroke-width":s.kind==="tie"?1.8:1.7,
    "stroke-linecap":"round"},parent);
  painted++;
 });
 return painted;
}
function drawClef(p,staff,at,xp,sign){
 var y=staffY(staff),x=xp(at)-26,clr="#1C3450";
 var anchor=clefAnchor(staff,sign),local=anchor.y-y;
 var g=svgEl("g",{"class":"temporal-clef",
  "data-clef":sign,"data-staff":staff,"data-reference-midi":anchor.midi,
  "data-reference-line":anchor.row,
  "data-reference-y":anchor.y.toFixed(3),
  transform:"translate("+x+" "+y+")"},p);
 if(sign==="F"){
  // The two dots are symmetrically ABOVE and BELOW the F3 line.
  // Unlike a generic decorative curl, the actual symbol marks row 1.
  var dotX=45,halfSpace=STAFF_STEP/2;
  svgEl("path",{d:
   "M11 "+local+" C9 "+(local-8)+" 15 "+(local-16)+" 25 "+(local-17)+
   " C39 "+(local-19)+" 43 "+(local-8)+" 39 "+(local+2)+
   " C36 "+(local+12)+" 27 "+(local+21)+" 11 "+(local+26),
   fill:"none",stroke:clr,"stroke-width":4.4,
   "stroke-linecap":"round","stroke-linejoin":"round"},g);
  svgEl("circle",{cx:11,cy:local,r:4.4,fill:clr},g);
  svgEl("circle",{cx:dotX,cy:local-halfSpace,r:3.15,fill:clr,
   "data-clef-dot":"above"},g);
  svgEl("circle",{cx:dotX,cy:local+halfSpace,r:3.15,fill:clr,
   "data-clef-dot":"below"},g);
 }else if(sign==="C"){
  // The central convergence identifies C4 on the middle line (alto clef).
  svgEl("path",{d:"M15 1 L15 53 M33 1 L33 53 "+
   "M15 "+local+" C31 "+(local-18)+" 42 "+(local-12)+" 34 "+local+
   " C42 "+(local+13)+" 30 "+(local+18)+" 15 "+local,
   fill:"none",stroke:clr,"stroke-width":3.7,
   "stroke-linecap":"round","stroke-linejoin":"round"},g);
  svgEl("circle",{cx:25,cy:local,r:2.5,fill:clr},g);
 }else{
  // Entire descending G clef is a single original scalable spiral:
  // its inner coil encloses the G4 reference line at 'local'.
  var d="M26 "+(local+43)+
   " C40 "+(local+53)+" 42 "+(local+38)+" 36 "+(local+20)+
   " C30 "+(local+5)+" 27 "+(local-12)+" 30 "+(local-31)+
   " C33 "+(local-51)+" 45 "+(local-71)+" 43 "+(local-61)+
   " C42 "+(local-51)+" 31 "+(local-40)+" 25 "+(local-26)+
   " C17 "+(local-8)+" 8 "+(local+2)+" 13 "+(local+15)+
   " C19 "+(local+34)+" 42 "+(local+31)+" 45 "+(local+14)+
   " C50 "+(local-2)+" 39 "+(local-18)+" 27 "+(local-16)+
   " C15 "+(local-15)+" 10 "+(local-2)+" 16 "+(local+7)+
   " C23 "+(local+18)+" 38 "+(local+13)+" 37 "+(local+2)+
   " C36 "+(local-5)+" 27 "+(local-7)+" 24 "+local;
  svgEl("path",{d:d,fill:"none",stroke:clr,"stroke-width":3.8,
   "stroke-linecap":"round","stroke-linejoin":"round",
   "data-clef-coil-y":local.toFixed(3)},g);
  svgEl("circle",{cx:24,cy:local,r:2.3,fill:clr},g);
 }
 return g;
}

/** Conventional sharp/flat placement follows the ACTUAL current clef. */
function keySignatureNotes(fifths,clef){
 if(!fifths)return [];
 var names=(fifths>0?SHARPS:FLATS).slice(0,Math.min(7,Math.abs(fifths)));
 var octaves;
 if(clef==="F")
  octaves=fifths>0?[3,3,3,3,2,3,2]:[2,3,2,3,2,3,2];
 else if(clef==="C")
  octaves=fifths>0?[4,4,4,4,3,4,3]:[3,4,3,4,3,4,3];
 else
  octaves=fifths>0?[5,5,5,5,4,5,4]:[4,5,4,5,4,5,4];
 return names.map(function(step,i){return {step:step,oct:octaves[i]};});
}
function drawKey(p,at,fifths,xp,plan,contextBeat){
 if(!fifths)return;
 var textSymbol=fifths>0?"♯":"♭";
 for(var staff=1;staff<=2;staff++){
  var active=plan?activeAt(plan.clefs,contextBeat===undefined?at:contextBeat,function(c){return c.staff===staff;},
   {sign:staff===1?"G":"F"}):{sign:staff===1?"G":"F"};
  var sign=active.sign;
  keySignatureNotes(fifths,sign).forEach(function(n,j){
   var y=pitchY({staff:staff,clef:sign,step:n.step,oct:n.oct});
   drawText(p,textSymbol,xp(at)+16+j*14,y+6,27,noteColor());
  });
 }
}
function activeAt(events,beat,predicate,fallback){
 var result=fallback;
 events.forEach(function(e){if(e.at<=beat+0.000001&&(!predicate||predicate(e)))result=e;});
 return result;
}
function fixedRailWidth(plan){
 var maxFifths=0;
 plan.keys.forEach(function(k){maxFifths=Math.max(maxFifths,Math.min(7,Math.abs(k.fifths)));});
 return 140+maxFifths*14;
}
function buildLayout(xml,viewportWidth){
 var plan=xmlTimeMap(xml),minDelta=1;
 var distinct=[];
 plan.notes.forEach(function(n){if(!distinct.length||n.at-distinct[distinct.length-1]>.0001)distinct.push(n.at);});
 for(var i=1;i<distinct.length;i++)if(distinct[i]-distinct[i-1]>.001)minDelta=Math.min(minDelta,distinct[i]-distinct[i-1]);
 var pixelsPerBeat=Math.max(104,Math.min(360,20/Math.max(.0625,minDelta)));
 var railWidth=fixedRailWidth(plan);
 // Separate the fixed guide and the actual note attack coordinate.
 var cursorX=Math.max(cursorPx(viewportWidth),railWidth+16);
 var origin=cursorX+cursorLeadPx(viewportWidth);
 var totalWidth=Math.ceil(origin+plan.duration*pixelsPerBeat+viewportWidth*.65);
 return {plan:plan,pixelsPerBeat:pixelsPerBeat,origin:origin,cursorX:cursorX,railWidth:railWidth,width:totalWidth,
  x:function(t){return computeX(t,pixelsPerBeat,origin);}};
}

/** Static reading context. Clefs, key signatures and time signature never scroll away. */
function paintRail(st,beat){
 var plan=st.layout.plan;
 var key=activeAt(plan.keys,beat,null,{fifths:0}),time=activeAt(plan.times,beat,null,{text:"4/4"});
 var c1=activeAt(plan.clefs,beat,function(e){return e.staff===1;},{sign:"G"});
 var c2=activeAt(plan.clefs,beat,function(e){return e.staff===2;},{sign:"F"});
 var id=[key.fifths,time.text,c1.sign,c2.sign].join("|");
 if(id===st.railLabel)return;
 st.railLabel=id;
 var svg=st.rail.querySelector("svg");while(svg.firstChild)svg.removeChild(svg.firstChild);
 var grid=svgEl("g",{},svg),musical=svgEl("g",{},svg);
 for(var staff=1;staff<=2;staff++){
  for(var row=0;row<5;row++){
   var y=staffY(staff)+row*13.2;
   line(grid,0,y,st.layout.railWidth,y,"#8998AA",1);
  }
  drawClef(musical,staff,0,function(){return 37;},staff===1?c1.sign:c2.sign);
 }
 if(key.fifths)drawKey(musical,0,key.fifths,function(){return 66;},plan,beat);
 var fifths=Math.min(7,Math.abs(key.fifths)),tx=89+fifths*14;
 var ch=String(time.text||"4/4").split("/");
 for(var i=1;i<=2;i++){
  drawText(musical,ch[0],tx,staffY(i)+25,22,noteColor());
  drawText(musical,ch[1]||"4",tx,staffY(i)+52,22,noteColor());
 }
}
function moveTiles(st,offset){
 var width=st.viewportWidth;
 var min=Math.max(0,Math.floor((offset-30)/st.tileWidth));
 var max=Math.min(st.tiles.length-1,Math.floor((offset+width+30)/st.tileWidth));
 var prev=st.visibleTiles||[],next=[];
 for(var k=min;k<=max;k++){
  var item=st.tiles[k];if(!item)continue;
  if(!item.visible){item.node.style.display="block";item.visible=true;}
  item.node.style.transform="translate3d("+(item.x-offset).toFixed(3)+"px,0,0)";
  next.push(k);
 }
 prev.forEach(function(k){if(next.indexOf(k)<0){st.tiles[k].node.style.display="none";st.tiles[k].visible=false;}});
 st.visibleTiles=next;
}
function highlightNotes(st,beat){
 var notes=st.layout.plan.notes;
 if(!notes.length)return;
 var lo=0,hi=notes.length;
 while(lo<hi){var mid=(lo+hi)>>1;if(notes[mid].at<beat-0.017)lo=mid+1;else hi=mid;}
 var target=lo<notes.length?notes[lo].at:-1;
 var stamp=target<0?"":target.toFixed(4);
 if(st.selectedStamp===stamp)return;
 var set=function(k,on){
  if(!k)return;
  (st.noteHeads[k]||[]).forEach(function(el){
   el.setAttribute("stroke",on?"#008E78":"#1E3049");
   el.setAttribute("stroke-width",on?"3.2":(el.getAttribute("fill")==="white"?"1.9":"1"));
  });
 };
 set(st.selectedStamp,false);st.selectedStamp=stamp;set(stamp,true);
}


function snapLoopBeat(plan,raw){
 raw=Math.max(0,Math.min(plan.duration,raw));
 // Prefer actual note onsets and measure boundaries; permit quarter-beat precision.
 var possibilities=[0,plan.duration,Math.round(raw*4)/4];
 plan.bars.forEach(function(bar){if(Math.abs(bar.at-raw)<.33)possibilities.push(bar.at);});
 plan.notes.forEach(function(note){if(Math.abs(note.at-raw)<.33)possibilities.push(note.at);});
 var best=possibilities[0],distance=Infinity;
 possibilities.forEach(function(p){var d=Math.abs(p-raw);if(d<distance){distance=d;best=p;}});
 return Math.max(0,Math.min(plan.duration,best));
}
function updateLoopOverlay(st){
 if(!st.loopUI)return;
 var ui=st.loopUI,range=st.loop,visible=!!range;
 ui.shade.style.display=visible?"block":"none";
 ui.first.style.display=visible&&st.editingLoop?"block":"none";
 ui.last.style.display=visible&&st.editingLoop?"block":"none";
 ui.controls.style.display=st.editingLoop?"flex":"none";
 if(!visible)return;
 var offset=st.layout.pixelsPerBeat*st.beat;
 var from=st.layout.x(range.start)-offset,to=st.layout.x(range.end)-offset;
 ui.shade.style.left=from+"px";ui.shade.style.width=Math.max(1,to-from)+"px";
 ui.first.style.left=(from-7)+"px";ui.last.style.left=(to-7)+"px";
 ui.label.textContent="Trecho: "+range.start.toFixed(2)+" – "+range.end.toFixed(2)+" tempos";
}
function setupLoopControls(st){
 var root=st.root;
 function block(css,tag){var e=document.createElement(tag||"div");e.style.cssText=css;root.appendChild(e);return e;}
 var shade=block("position:absolute;top:0;height:98%;background:rgba(20,142,122,.10);"+
  "border-left:2px solid #0a9b82;border-right:2px solid #0a9b82;"+
  "pointer-events:none;z-index:5;display:none;");
 var handleCss="position:absolute;top:6%;height:83%;width:15px;border-radius:7px;"+
  "background:rgba(2,139,119,.36);border:2px solid #068b76;"+
  "touch-action:none;z-index:6;display:none;cursor:ew-resize;";
 var first=block(handleCss),last=block(handleCss);
 var controls=block("position:absolute;right:8px;bottom:4px;display:none;align-items:center;"+
  "gap:6px;padding:5px 7px;background:#fff;border:1px solid #b6d6d0;border-radius:8px;"+
  "box-shadow:0 1px 6px #b4c4cc;z-index:8;font:12px system-ui;");
 var label=document.createElement("span");label.style.color="#176355";controls.appendChild(label);
 function button(text,action){
  var b=document.createElement("button");b.type="button";b.textContent=text;
  b.style.cssText="border:0;border-radius:5px;padding:7px 9px;background:#007f6b;"+
   "color:#fff;font:12px system-ui;touch-action:manipulation;";
  b.addEventListener("pointerdown",function(e){e.stopPropagation();});
  b.addEventListener("click",function(e){e.stopPropagation();action();});
  controls.appendChild(b);return b;
 }
 button("Repetir ✓",function(){
  if(!st.loop||st.loop.end-st.loop.start<.01)return;
  if(window.AndroidNotation&&AndroidNotation.onLoopSelected){
   AndroidNotation.onLoopSelected(st.loop.start,st.loop.end);
   setLoopEditing(false);
  }else{alert("Seleção pronta: conecte o visualizador ao controle de repetição.");}
 });
 button("Cancelar",function(){setLoopEditing(false);});
 st.loopUI={shade:shade,first:first,last:last,controls:controls,label:label};
 var dragging="";
 function beatFrom(clientX){
  var rect=root.getBoundingClientRect(),relative=clientX-rect.left;
  var raw=st.beat+(relative-st.layout.origin)/st.layout.pixelsPerBeat;
  return snapLoopBeat(st.layout.plan,raw);
 }
 root.addEventListener("pointerdown",function(e){
  if(!st.editingLoop||e.target.closest("button"))return;
  e.preventDefault();
  if(e.target===first)dragging="start";
  else if(e.target===last)dragging="end";
  else {
   var value=beatFrom(e.clientX);
   dragging=Math.abs(value-st.loop.start)<=Math.abs(value-st.loop.end)?"start":"end";
  }
  try{root.setPointerCapture(e.pointerId);}catch(err){}
  move(e);
 });
 function move(e){
  if(!dragging||!st.loop)return;
  var point=beatFrom(e.clientX),minDur=Math.min(.125,st.layout.plan.duration/8);
  if(dragging==="start")st.loop.start=Math.max(0,Math.min(st.loop.end-minDur,point));
  else st.loop.end=Math.min(st.layout.plan.duration,Math.max(st.loop.start+minDur,point));
  updateLoopOverlay(st);
 }
 root.addEventListener("pointermove",function(e){if(dragging){e.preventDefault();move(e);}});
 var stop=function(){dragging="";};
 root.addEventListener("pointerup",stop);root.addEventListener("pointercancel",stop);
}
/**
 * Delegate events instead of installing one listener per SVG notehead/tile.
 * Clickable hit target is the notehead only; unaffected by the study clock.
 */
function setupNoteIdentification(st){
 var popup=document.createElement("div");
 popup.id="temporal-note-identification";
 popup.setAttribute("role","status");
 popup.style.cssText="position:absolute;display:none;z-index:10;pointer-events:none;"+
  "background:#17324e;color:#fff;border-radius:8px;padding:7px 10px;"+
  "font:600 14px system-ui,sans-serif;max-width:60%;line-height:1.4;"+
  "box-shadow:0 2px 9px #55718c66;";
 st.root.appendChild(popup);st.popup=popup;st.popupTimer=null;
 st.root.addEventListener("click",function(e){
  if(st.editingLoop||!e.target||!e.target.tagName
     ||String(e.target.tagName).toLowerCase()!=="ellipse")return;
  var group=e.target.closest?e.target.closest(".musical-event"):null;
  if(!group)return;
  var index=Number(group.getAttribute("data-note-index"));
  if(!Number.isInteger(index)||index<0)return;
  var note=st.layout.plan.notes[index],identity=noteIdentification(note);
  if(!identity)return;
  var text=identity+(note.harmony?"\nAcorde: "+note.harmony.symbol+" — "+note.harmony.name:"");
  popup.textContent=text;
  popup.style.whiteSpace="pre-line";
  popup.style.display="block";
  var rootRect=st.root.getBoundingClientRect(),headRect=e.target.getBoundingClientRect();
  var preferredX=headRect.left-rootRect.left-18;
  popup.style.left=Math.max(st.layout.railWidth+5,Math.min(st.viewportWidth-180,preferredX))+"px";
  popup.style.top=Math.max(4,headRect.top-rootRect.top-57)+"px";
  if(st.popupTimer)clearTimeout(st.popupTimer);
  st.popupTimer=setTimeout(function(){popup.style.display="none";st.popupTimer=null;},2000);
 });
}
function setLoopEditing(enabled){
 if(!state)return;
 var st=state;st.editingLoop=!!enabled;st.root.style.touchAction=enabled?"none":"auto";
 if(enabled&&st.popup)st.popup.style.display="none";
 if(enabled&&!st.loop){
  var bars=st.layout.plan.bars.filter(function(e){return e.start;}).map(function(e){return e.at;});
  var start=0,end=Math.min(st.layout.plan.duration,4);
  for(var i=0;i<bars.length;i++){
   if(bars[i]<=st.beat+.0001)start=bars[i];
   else{end=bars[i];break;}
  }
  if(end<=start)end=Math.min(st.layout.plan.duration,start+4);
  st.loop={start:start,end:end};
 }
 updateLoopOverlay(st);
}
function clearLoop(){
 if(!state)return;
 state.loop=null;setLoopEditing(false);
}

function verticalScale(viewport){
 var h=viewport.clientHeight||window.innerHeight||255;
 return Math.max(.25,Math.min(.67,(h-7)/245));
}
function render(xml,container,viewport,startBeat){
 teardown();
 var renderStarted=performance.now();
 if(!container||!viewport)throw Error("Visualização sem contêiner");
 var width=Math.max(300,viewport.clientWidth||window.innerWidth);
 var layout=buildLayout(xml,width),plan=layout.plan,scale=verticalScale(viewport);
 var root=document.createElement("div");root.className="timeline-root";
 root.style.cssText="position:relative;width:100%;height:100%;overflow:hidden;background:white;";
 var sheet=document.createElement("div");sheet.id="timeline-track";
 // Tiles move independently: WebView never composites one huge score texture.
 sheet.style.cssText="position:absolute;top:0;left:0;height:100%;width:100%;pointer-events:auto;"+
  "transform-origin:top left;transform:scaleY("+scale+");";
 // Android WebView/GPU cannot always rasterize a 10,000+ px-wide SVG.
 // Small consecutive SVG tiles keep the entire piece continuous without
 // oversized textures and preserve the shared exact musical-time coordinate.
 var allSpanners=collectSpanners(plan.notes);
 var allBeams=collectBeamGroups(plan.notes,layout.x);
 var tileWidth=1160,tiles=Math.ceil(layout.width/tileWidth),tileNodes=[];
 for(var tile=0;tile<tiles;tile++){
  (function(tileStart){
   var tw=Math.min(tileWidth,layout.width-tileStart);
   var svg=svgEl("svg",{width:tw,height:255,viewBox:"0 0 "+tw+" 255",
    xmlns:NS,role:"img","aria-label":"Trecho da partitura temporal"});
   svg.style.cssText="position:absolute;left:0;top:0;overflow:hidden;display:none;will-change:transform;";
   sheet.appendChild(svg);tileNodes.push({node:svg,x:tileStart,width:tw,visible:false});
   var grid=svgEl("g",{"class":"timeline-grid"},svg);
   var musical=svgEl("g",{"class":"timeline-notes"},svg);
   var lx=function(t){return layout.x(t)-tileStart;};
   var near=function(t,pad){var x=lx(t);return x>=-pad&&x<=tw+pad;};
   for(var st=1;st<=2;st++){
    var top=staffY(st);
    for(var row=0;row<5;row++)
     line(grid,0,top+row*13.2,tw,top+row*13.2,"#8998AA",1);
   }
   plan.bars.filter(function(e){return near(e.at,170);}).forEach(function(e){
    var x=lx(e.at)-(e.start&&e.number===1?155:0),top=staffY(1),bottom=staffY(2)+53;
    line(grid,x,top,x,bottom,"#72869A",1.25);
    if(e.start){
     drawText(musical,String(e.number),x+5,top-11,13,"#73859A");
     // Initial clefs are drawn exclusively in the fixed left rail.
    }
    if(e.end)line(grid,x+3,top,x+3,bottom,"#334B63",3);
   });
   // Show first/second ending brackets, not only an unlabelled repeat sign.
   (plan.endings||[]).forEach(function(e){
    var from=lx(e.start),to=lx(e.end);
    if(to<0||from>tw)return;
    var visibleFrom=Math.max(0,from),visibleTo=Math.min(tw,to);
    line(grid,visibleFrom,19,visibleTo,19,"#526982",1.6);
    if(from>=-16&&from<=tw){
     line(grid,from,19,from,28,"#526982",1.6);
     drawText(musical,e.number+"ª",from+6,14,12,"#344C66");
    }
    if(to>=0&&to<=tw)line(grid,to,19,to,28,"#526982",1.6);
   });
   plan.clefs.filter(function(e){return e.at>0&&near(e.at,80);})
     .forEach(function(e){drawClef(musical,e.staff,e.at,lx,e.sign);});
   plan.times.filter(function(e){return e.at>0.00001&&near(e.at,100);}).forEach(function(e){
    var ch=e.text.split("/"),bx=lx(e.at)-(e.at===0?43:0);
    for(var sn=1;sn<=2;sn++){
     drawText(musical,ch[0],bx+13,staffY(sn)+25,23,noteColor());
     drawText(musical,ch[1],bx+13,staffY(sn)+52,23,noteColor());
    }
   });
   plan.keys.filter(function(e){return e.at>0.00001&&near(e.at,130);}).forEach(function(e){
    var kx=e.at===0?function(){return lx(0)-83;}:lx;
    drawKey(musical,e.at,e.fifths,kx,plan);
   });
   plan.rests.filter(function(e){return near(e.at,25);})
     .forEach(function(e){drawRest(musical,e,lx(e.at));});
   var localNotes=plan.notes.filter(function(e){return near(e.at,e.grace?160:65);})
     .map(function(e){return Object.assign({},e);});
   localNotes.forEach(function(e){drawNote(musical,e,lx(e.at));});
   drawScoreBeams(musical,allBeams,tileStart,tw);
   drawScoreSpanners(musical,allSpanners,tileStart,tw,layout.x);
   plan.marks.filter(function(e){return near(e.at,70);})
     .forEach(function(e){drawText(musical,e.text,lx(e.at),40,15,"#4D6076");});
  })(tile*tileWidth);
 }
 console.log("TEMPORAL_TILES",tiles,"total-width",layout.width);
 // The indicator is a separate overlay; never move it or use OSMD's note cursor.
 var cursor=document.createElement("div");cursor.id="fixed-temporal-cursor";
 cursor.style.cssText="position:absolute;left:"+layout.cursorX+"px;top:1%;height:98%;width:3px;background:#009D82;"+
  "pointer-events:none;z-index:3;box-shadow:0 0 2px #00826C;";
 var dot=document.createElement("div");dot.style.cssText="position:absolute;left:-4px;top:0;width:11px;height:11px;border-radius:50%;background:#009D82;";
 cursor.appendChild(dot);root.appendChild(sheet);
 // Opaque fixed reading context above the moving music tiles.
 var rail=document.createElement("div");
 rail.id="fixed-musical-context";
 rail.style.cssText="position:absolute;left:0;top:0;width:"+layout.railWidth+
  "px;height:100%;z-index:4;background:#fff;border-right:1px solid #c6d5e2;pointer-events:none;";
 var railSvg=svgEl("svg",{width:layout.railWidth,height:255,viewBox:"0 0 "+layout.railWidth+" 255"},rail);
 railSvg.style.cssText="position:absolute;top:0;left:0;transform-origin:top left;transform:scaleY("+scale+");";
 root.appendChild(rail);root.appendChild(cursor);
 container.innerHTML="";container.appendChild(root);
 var status=document.createElement("div");status.style.cssText="position:absolute;bottom:0;right:9px;color:#61788B;font:10px sans-serif;background:#ffffffba;padding:4px;";
 status.textContent="♪ Espaçamento proporcional ao tempo";root.appendChild(status);
 // Index noteheads once, including duplicates drawn across tile boundaries.
 // Playback must not search the entire SVG tree at every note onset.
 var noteHeads=Object.create(null);
 root.querySelectorAll('.musical-event').forEach(function(event){
  var stamp=event.getAttribute("data-beat");
  var heads=noteHeads[stamp]||(noteHeads[stamp]=[]);
  event.querySelectorAll('ellipse').forEach(function(head){heads.push(head);});
 });
 var initial=Number.isFinite(startBeat)?startBeat:0;
 state={layout:layout,viewport:viewport,container:container,root:root,sheet:sheet,cursor:cursor,beat:initial,
  anchorBeat:initial,anchorMs:performance.now(),bpm:100,moving:false,raf:0,
  lastPosition:NaN,unsupported:plan.unsupported,sampleCount:0,lastSampleMs:0,xml:xml,
  tiles:tileNodes,tileWidth:tileWidth,rail:rail,railLabel:"",loop:null,editingLoop:false,
  selectedStamp:"",noteHeads:noteHeads,viewportWidth:width,viewportHeight:viewport.clientHeight,
  railSvg:railSvg,lastFrameTime:null,renderStarted:renderStarted,
  metrics:{renderMs:performance.now()-renderStarted,firstPaintMs:null,frames:0,
   movingFrames:0,intervals:0,gapTotalMs:0,maxGapMs:0,gapsOver50Ms:0,maxDrawMs:0,peakVisibleTiles:0}};
 if(plan.unsupported.length){status.textContent="Partitura temporal parcial: "+plan.unsupported.length+" elementos não interpretados"; }
 setupLoopControls(state);
 setupNoteIdentification(state);
 paintRail(state,initial);
 function animation(now){
  if(!state)return;
  var st=state;st.raf=0;
  var started=performance.now(),m=st.metrics;
  m.frames++;
  if(m.firstPaintMs===null)m.firstPaintMs=started-st.renderStarted;
  if(st.moving){
   m.movingFrames++;
   if(st.lastFrameTime!==null){
    var gap=Math.max(0,now-st.lastFrameTime);
    m.intervals++;m.gapTotalMs+=gap;m.maxGapMs=Math.max(m.maxGapMs,gap);
    if(gap>50)m.gapsOver50Ms++;
   }
   st.lastFrameTime=now;
  }else st.lastFrameTime=null;
  var current=st.moving?st.anchorBeat+(now-st.anchorMs)*st.bpm/60000:st.anchorBeat;
  current=Math.max(0,Math.min(st.layout.plan.duration,current));
  var offset=st.layout.pixelsPerBeat*current;
  if(st.waitPrelude){
   var fraction=Math.max(0,Math.min(1,(now-st.waitPrelude.at)/st.waitPrelude.duration));
   offset-=(1-fraction)*st.viewportWidth*.58;
   if(fraction>=1)st.waitPrelude=null;
  }
  st.beat=current;
  if(!isFinite(st.lastPosition)||Math.abs(st.lastPosition-offset)>0.001){
   moveTiles(st,offset);
   paintRail(st,current);
   highlightNotes(st,current);
   updateLoopOverlay(st);
   st.lastPosition=offset;
  }
  if(st.moving&&st.sampleCount<7&&now-st.lastSampleMs>=350){
   console.log("TEMPORAL_SAMPLE "+JSON.stringify({beat:+current.toFixed(5),
    offset:+offset.toFixed(3),bpm:st.bpm,cursor:st.layout.cursorX/(st.viewportWidth||1)}));
   st.sampleCount++;st.lastSampleMs=now;
  }
  m.peakVisibleTiles=Math.max(m.peakVisibleTiles,(st.visibleTiles||[]).length);
  m.maxDrawMs=Math.max(m.maxDrawMs,performance.now()-started);
  // Pause and the end of the score sleep until another native sample.
  if((st.moving&&current<st.layout.plan.duration)||st.waitPrelude)requestPaint(st);
 }
 state.animation=animation;
 requestPaint(state);
 console.log("TIMELINE_READY "+JSON.stringify({notes:plan.notes.length,bars:plan.bars.length,
  pxPerBeat:layout.pixelsPerBeat,origin:layout.origin,verticalScale:scale,viewportHeight:viewport.clientHeight,firstBeats:plan.notes.slice(0,12).map(function(n){return n.at;}),
  firstXs:plan.notes.slice(0,12).map(function(n){return Math.round(layout.x(n.at));}),
  cursorFraction:layout.cursorX/width,cursorLeadPx:layout.origin-layout.cursorX,unsupported:plan.unsupported}));
 return layout;
}
// Fixed-size, local-only counters. Callback spacing is not GPU/raster FPS.
function diagnostics(){
 if(!state)return null;
 var st=state,m=st.metrics;
 return Object.assign({},m,{tiles:st.tiles.length,notes:st.layout.plan.notes.length,
  meanGapMs:m.intervals?m.gapTotalMs/m.intervals:0,pendingFrame:!!st.raf,moving:st.moving});
}
function reportDiagnostics(reason){
 var data=diagnostics();if(data)console.log("TEMPORAL_PERF "+JSON.stringify(Object.assign({reason:reason},data)));
}
function requestPaint(st){
 if(!st.raf)st.raf=requestAnimationFrame(st.animation);
}
function sync(beat,bpm,playing){
 if(!state||!isFinite(beat))return;
 var now=performance.now(),rate=Number(bpm);
 // Native BPM already includes the study-speed factor. Clamping it here
 // creates a second, incorrect tempo and recurring corrective jumps.
 if(!Number.isFinite(rate)||rate<=0)rate=100;
 var st=state;
 if(!playing){
  var wasMoving=st.moving;st.lastFrameTime=null;
  st.moving=false;st.anchorBeat=beat;st.anchorMs=now;st.bpm=rate;
  if(st.beat!==Math.max(0,Math.min(st.layout.plan.duration,beat)))requestPaint(st);
  if(wasMoving)reportDiagnostics("pause");
  return;
 }
 var predicted=st.moving?st.anchorBeat+(now-st.anchorMs)*st.bpm/60000:st.anchorBeat;
 var error=beat-predicted,rateChanged=Math.abs(st.bpm-rate)>0.001;
 // Explicit seek, loop boundary, start or tempo changes must reposition exactly.
 if(!st.moving||Math.abs(error)>.36||rateChanged){
  st.anchorBeat=beat;st.anchorMs=now;
 }else{
  // Never rebuild the animation origin at every native clock tick.
  // Gently slew phase, avoiding abrupt 2–10 px frame jumps.
  st.anchorBeat+=Math.max(-.003,Math.min(.003,error*0.035));
 }
 if(!st.moving||!st.raf)st.lastFrameTime=null;
 st.bpm=rate;st.moving=true;requestPaint(st);
}
function waitPrepare(ms){
 if(!state||!isFinite(ms)||ms<=0)return;
 state.waitPrelude={at:performance.now(),duration:Math.max(1,ms)};
 state.lastPosition=NaN;requestPaint(state);
}
function waitCancel(){
 if(!state)return;
 state.waitPrelude=null;state.lastPosition=NaN;requestPaint(state);
}
function resize(){
 if(!state)return;
 var st=state,viewport=st.viewport;
 var width=Math.max(300,viewport.clientWidth||window.innerWidth),height=viewport.clientHeight;
 if(width===st.viewportWidth&&height===st.viewportHeight)return;
 if(width===st.viewportWidth){
  // Opening the guide only changes height: keep SVG, clock and loop intact.
  var transform="scaleY("+verticalScale(viewport)+")";
  st.sheet.style.transform=transform;st.railSvg.style.transform=transform;
  st.viewportHeight=height;
  return;
 }
 // Width changes affect cursor and tiles. Preserve the running visual clock.
 var now=performance.now();
 var saved=st.moving?st.anchorBeat+(now-st.anchorMs)*st.bpm/60000:st.anchorBeat;
 saved=Math.max(0,Math.min(st.layout.plan.duration,saved));
 var existing=st.loop,editing=st.editingLoop,moving=st.moving,bpm=st.bpm;
 render(st.xml,st.container,viewport,saved);
 if(state){state.loop=existing;setLoopEditing(editing);sync(saved,bpm,moving);}
}
function teardown(){if(!state)return;reportDiagnostics("teardown");if(state.popupTimer)clearTimeout(state.popupTimer);cancelAnimationFrame(state.raf);state=null;}
var exported={parse:xmlTimeMap,buildLayout:buildLayout,render:render,sync:sync,resize:resize,teardown:teardown,
 loopEdit:setLoopEditing,clearLoop:clearLoop,waitPrepare:waitPrepare,waitCancel:waitCancel,diagnostics:diagnostics,
 _test:{computeX:computeX,cursorPx:cursorPx,cursorLeadPx:cursorLeadPx,msPerBeat:msPerBeat,
  staffY:staffY,staffLineY:staffLineY,clefAnchor:clefAnchor,
  pitchY:pitchY,drawClef:drawClef,
  keyDefaultAlter:keyDefaultAlter,accidentalFromAlter:accidentalFromAlter,
  writtenDuration:writtenDuration,noteIdentification:noteIdentification,parseHarmony:parseHarmony,
  keySignatureNotes:keySignatureNotes,drawKey:drawKey,
  drawSlurs:drawSlurs,drawNote:drawNote,drawRest:drawRest,
  arrangeVoices:arrangeVoices,collectSpanners:collectSpanners,
  bezierPiece:bezierPiece,drawScoreSpanners:drawScoreSpanners,
  collectBeamGroups:collectBeamGroups,drawScoreBeams:drawScoreBeams}};
global.TemporalNotation=exported;
if(typeof module!=="undefined"&&module.exports)module.exports=exported;
})(typeof window!=="undefined"?window:this);

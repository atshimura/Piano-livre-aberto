package br.org.pianolivre;

import java.util.*;

/** MusicXML for original offline educational exercises, using the bundled OSMD. */
public final class MusicLearningScore {
 private MusicLearningScore(){}
 private static String escape(String v){
  return v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
   .replace("\"","&quot;").replace("'","&apos;");
 }
 private static void pitch(StringBuilder b,int note,int staff,int rhythm){
  String[] steps={"C","C","D","E","E","F","F","G","A","A","B","B"};
  int[] alter={0,1,0,-1,0,0,1,0,-1,0,-1,0};
  int pc=note%12;
  b.append("<pitch><step>").append(steps[pc]).append("</step>");
  if(alter[pc]!=0)b.append("<alter>").append(alter[pc]).append("</alter>");
  b.append("<octave>").append(note/12-1).append("</octave></pitch>");
 }
 private static void note(StringBuilder b,int note,boolean chord,int staff,int rhythm){
  b.append("<note>");if(chord)b.append("<chord/>");
  pitch(b,note,staff,rhythm);
  int dur=MusicLearningEngine.RHYTHM_LENGTHS[rhythm];
  b.append("<duration>").append(dur).append("</duration><voice>1</voice><type>")
   .append(MusicLearningEngine.RHYTHM_TYPES[rhythm]).append("</type>");
  // A rhythmic quiz may use a displayed eighth/16th while duration is only
  // one abstract division; no playback timing is evaluated from this source.
  b.append("<staff>").append(staff).append("</staff></note>");
 }
 public static String xml(MusicLearningEngine.Exercise e){
  if(e==null)throw new IllegalArgumentException("Exercício inexistente");
  if(e.hand==2&&e.kind!=MusicLearningEngine.RHYTHM)
   return DiscoveryScoreBuilder.build(toList(e.pitches),e.ordered);
  String title=escape(e.title);
  StringBuilder b=new StringBuilder();
  b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
   .append("<score-partwise version=\"4.0\"><work><work-title>").append(title)
   .append("</work-title></work><movement-title>").append(title)
   .append("</movement-title><part-list><score-part id=\"P1\"><part-name>Piano</part-name>")
   .append("</score-part></part-list><part id=\"P1\">");
  int count=e.ordered?(e.pitches.length+3)/4:1;
  for(int m=0;m<count;m++){
   b.append("<measure number=\"").append(m+1).append("\">");
   if(m==0){
    b.append("<attributes><divisions>1</divisions><key><fifths>0</fifths></key>")
     .append("<time><beats>4</beats><beat-type>4</beat-type></time><staves>1</staves>")
     .append("<clef number=\"1\"><sign>").append(e.hand==1?"F":"G")
     .append("</sign><line>").append(e.hand==1?4:2).append("</line></clef></attributes>");
   }
   if(e.kind==MusicLearningEngine.CHORD){
    for(int i=0;i<e.pitches.length;i++)note(b,e.pitches[i],i>0,1,0); // whole-note chord
   }else if(e.kind==MusicLearningEngine.RHYTHM){
    note(b,e.pitches[0],false,1,e.rhythm);
   }else{
    int start=m*4,limit=e.ordered?Math.min(e.pitches.length,start+4):1;
    for(int i=start;i<limit;i++)note(b,e.pitches[i],false,1,2);
   }
   b.append("</measure>");
  }
  return b.append("</part></score-partwise>").toString();
 }
 private static List<Integer> toList(int[] notes){
  ArrayList<Integer> copy=new ArrayList<>();for(int n:notes)copy.add(n);return copy;
 }
}

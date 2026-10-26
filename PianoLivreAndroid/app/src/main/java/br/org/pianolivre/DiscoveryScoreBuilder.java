package br.org.pianolivre;

import java.util.*;

/** Builds a small offline MusicXML score for the shared Study/OSMD engraver. */
final class DiscoveryScoreBuilder {
 private DiscoveryScoreBuilder(){}

 static String build(List<Integer> notes,boolean sequence){
  if(notes==null||notes.isEmpty())throw new IllegalArgumentException("Descobrir needs at least one pitch");
  for(Integer n:notes)if(n==null||n<0||n>127)throw new IllegalArgumentException("Invalid MIDI pitch");
  int min=Collections.min(notes),max=Collections.max(notes);boolean grand=min<60&&max>=60;
  boolean bassOnly=!grand&&max<60;
  ArrayList<Integer> written=new ArrayList<>(notes);if(!sequence)Collections.sort(written);
  StringBuilder xml=new StringBuilder(512+notes.size()*180);
  xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\">")
   .append("<work><work-title>Descobrir</work-title></work><part-list><score-part id=\"P1\"><part-name>Piano</part-name><score-instrument id=\"P1-I1\"><instrument-name>Piano</instrument-name></score-instrument><midi-instrument id=\"P1-I1\"><midi-channel>1</midi-channel><midi-program>1</midi-program></midi-instrument></score-part></part-list><part id=\"P1\">");
  int measureCount=sequence?(written.size()+3)/4:1;
  for(int measure=0;measure<measureCount;measure++){
   int start=sequence?measure*4:0,end=sequence?Math.min(written.size(),start+4):written.size();
   xml.append("<measure number=\"").append(measure+1).append("\">");
   if(measure==0){
    xml.append("<attributes><divisions>1</divisions><key><fifths>0</fifths><mode>major</mode></key><time><beats>4</beats><beat-type>4</beat-type></time>");
    if(grand)xml.append("<staves>2</staves><part-symbol>brace</part-symbol><clef number=\"1\"><sign>G</sign><line>2</line></clef><clef number=\"2\"><sign>F</sign><line>4</line></clef>");
    else if(bassOnly)xml.append("<clef><sign>F</sign><line>4</line></clef>");
    else xml.append("<clef><sign>G</sign><line>2</line></clef>");
    xml.append("</attributes>");
   }
   for(int i=start;i<end;i++)appendNote(xml,written.get(i),grand,!sequence&&i>start);
   xml.append("</measure>");
  }
  return xml.append("</part></score-partwise>").toString();
 }

 private static void appendNote(StringBuilder xml,int midi,boolean grand,boolean chord){
  int pc=midi%12;String[] steps={"C","C","D","E","E","F","F","G","A","A","B","B"};
  int[] alters={0,1,0,-1,0,0,1,0,-1,0,-1,0};int alter=alters[pc],staff=grand&&midi<60?2:1;
  xml.append("<note>");if(chord)xml.append("<chord/>");
  xml.append("<pitch><step>").append(steps[pc]).append("</step>");if(alter!=0)xml.append("<alter>").append(alter).append("</alter>");
  xml.append("<octave>").append(midi/12-1).append("</octave></pitch><duration>1</duration><voice>1</voice><type>quarter</type>");
  if(alter==1)xml.append("<accidental>sharp</accidental>");else if(alter==-1)xml.append("<accidental>flat</accidental>");
  xml.append("<staff>").append(staff).append("</staff></note>");
 }
}

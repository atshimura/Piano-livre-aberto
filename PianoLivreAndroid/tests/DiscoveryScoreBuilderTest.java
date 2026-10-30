package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DiscoveryScoreBuilderTest {
 private static int checks;
 private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 private static Score score(int[] pitches,boolean sequence)throws Exception{
  ArrayList<Integer> values=new ArrayList<>();for(int n:pitches)values.add(n);
  byte[] xml=DiscoveryScoreBuilder.build(values,sequence).getBytes(StandardCharsets.UTF_8);
  return Score.read(new ByteArrayInputStream(xml),"Descobrir");
 }
 private static void pitches(Score score,int...expected){
  check(score.writtenEvents.size()==expected.length,"every identified pitch becomes a rendered MusicXML note");
  List<Score.Event> ordered=new ArrayList<>(score.writtenEvents);ordered.sort(Comparator.comparingDouble((Score.Event e)->e.beat).thenComparingInt(e->e.midi));
  for(int i=0;i<expected.length;i++)check(ordered.get(i).midi==expected[i],"pitch/order at index "+i+" expected="+expected[i]+" actual="+score.writtenEvents.get(i).midi+" beat="+score.writtenEvents.get(i).beat+" staff="+score.writtenEvents.get(i).staff);
 }
 public static void main(String[]args)throws Exception{
  String empty=DiscoveryScoreBuilder.build(Collections.emptyList(),false);check(empty.contains("<staves>2</staves>")&&empty.contains("<sign>G</sign>")&&empty.contains("<sign>F</sign>"),"empty state engraves both clefs");
  for(int midi:new int[]{0,21,108,127})pitches(score(new int[]{midi},false),midi);
  Score c2=score(new int[]{36},false);pitches(c2,36);check(c2.sourceXml.contains("<sign>F</sign>"),"bass-only score uses bass clef");
  Score c4=score(new int[]{60},false);pitches(c4,60);check(c4.sourceXml.contains("<sign>G</sign>"),"middle C uses treble clef and ledger line from OSMD");
  Score eb=score(new int[]{63},false);pitches(eb,63);check(eb.writtenEvents.get(0).writtenStep.equals("E")&&eb.writtenEvents.get(0).writtenAlter==-1,"Eb4 preserves flat spelling/accidental");
  Score c6=score(new int[]{84},false);pitches(c6,84);check(c6.writtenEvents.get(0).writtenOctave==6,"C6 preserves octave");
  Score chord=score(new int[]{60,64,67},false);pitches(chord,60,64,67);
  check(chord.writtenEvents.get(0).beat==0&&chord.writtenEvents.get(1).beat==0&&chord.writtenEvents.get(2).beat==0,"chord tones share one onset");
  Score grand=score(new int[]{55,60,67,72},false);
  check(grand.writtenEvents.size()==4,"grand staff contains every chord pitch");
  Set<Integer> grandPitches=new HashSet<>();for(Score.Event event:grand.writtenEvents)grandPitches.add(event.midi);
  check(grandPitches.equals(new HashSet<>(Arrays.asList(55,60,67,72))),"grand staff preserves all chord pitches");
  for(Score.Event event:grand.writtenEvents)check(event.staff==(event.midi<60?2:1),"grand staff associates note with matching clef");
  check(grand.sourceXml.contains("<sign>F</sign>")&&grand.sourceXml.contains("<sign>G</sign>"),"grand staff declares both clefs");
  int[] cMajor={60,62,64,65,67,69,71,72};Score scale=score(cMajor,true);pitches(scale,cMajor);
  for(int i=0;i<cMajor.length;i++)check(scale.writtenEvents.get(i).beat==i,"scale order/onset retained at "+i);
  System.out.println("DISCOVERY_SCORE_BUILDER_PASS checks="+checks);
 }
}

package br.org.pianolivre;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Z-P3 MusicXML/Score interoperability and bounded synthetic MIDI corpus for the
 *  new generator. Every written attack must survive the Study parser with its pitch. */
public final class CreationC2DCorpusTest {
 private static int checks;
 private static void ok(boolean yes,String why){checks++;if(!yes)throw new AssertionError("Z corpus: "+why);}
 private static MidiTakeRecorder.Event ev(int status,int pitch,int value,long time,int order){
  return new MidiTakeRecorder.Event(status,pitch,value,time,7,order,false);
 }
 private static MidiTakeRecorder.Segment segment(long start,long end){
  return new MidiTakeRecorder.Segment(start,end);
 }
 private static Score parse(CreationScoreGenerator.Generation result,String name)throws Exception{
  Score score=Score.read(new ByteArrayInputStream(result.xml.getBytes(StandardCharsets.UTF_8)),"creation-score:"+name);
  ok(score.measures==result.measures,"measure count survives Score.read");
  ok(score.writtenEvents.size()>=result.notes.size(),"no written note lost at Score.read");
  Map<Integer,Long> expected=result.notes.stream().collect(Collectors.groupingBy(n->n.pitch,Collectors.counting()));
  Map<Integer,Long> actual=score.writtenEvents.stream().collect(Collectors.groupingBy(n->n.midi,Collectors.counting()));
  for(Map.Entry<Integer,Long> e:expected.entrySet())
   ok(actual.getOrDefault(e.getKey(),0L)>=e.getValue(),"pitch "+e.getKey()+" survives with its duplicates");
  return score;
 }
 private static void reject(RunnableChecked operation,String description)throws Exception{
  boolean failed=false;
  try{operation.run();}catch(IOException|IllegalArgumentException expected){failed=true;}
  ok(failed,description);
 }
 private interface RunnableChecked{void run()throws Exception;}

 public static void main(String[] args)throws Exception{
  long t0=System.nanoTime();
  // --- Cross-staff polyphony with pedal: attacks on both staves, overlaps kept. ---
  List<MidiTakeRecorder.Event> complex=Arrays.asList(
   ev(0x90,48,95,0,0),ev(0x90,60,80,0,1),ev(0x91,60,70,0,2),
   ev(0xB0,64,127,150_000_000L,3),
   ev(0x80,60,0,600_000_000L,4),
   ev(0x80,48,0,1_200_000_000L,5),
   ev(0x81,60,0,1_500_000_000L,6),
   ev(0x80,48,0,1_800_000_000L,7));
  CreationScoreGenerator.Generation cross=CreationScoreGenerator.generate(complex,
     Collections.singletonList(segment(0,2_000_000_000L)),"Corpus cruzado");
  parse(cross,"corpus-cruzado");
  ok(cross.notes.size()==3,"three source attacks (retrigger closes the unison)");
  ok(cross.xml.contains("<staves>2</staves>"),"two staves always present");
  ok(cross.xml.contains("<sign>G</sign>")&&cross.xml.contains("<sign>F</sign>"),"both clefs written");

  // --- Bounded synthetic corpus: 320 mixed attacks within limits. ---
  ArrayList<MidiTakeRecorder.Event> corpus=new ArrayList<>();
  long quarter=500_000_000L;int order=0;
  for(int i=0;i<320;i++){
   int pitch=48+(i*5)%29;                 // crosses the MIDI 60 boundary
   long at=(long)(i%40)*quarter;          // loops inside 40 quarters, creating overlaps
   long dur=(i%7==0)?2*quarter:quarter;
   corpus.add(ev(0x90,pitch,80,at,order++));
   corpus.add(ev(0x80,pitch,0,at+dur,order++));
  }
  corpus.sort(Comparator.comparingLong((MidiTakeRecorder.Event e)->e.timeNs).thenComparingInt(e->e.order));
  CreationScoreGenerator.Generation big=CreationScoreGenerator.generate(corpus,
     Collections.singletonList(segment(0,41*quarter)),"Corpus 320");
  parse(big,"corpus-320");
  ok(big.notes.size()==320,"all 320 attacks paired");
  ok(big.xml.length()<ScoreXmlComposer.MAX_XML_CHARS,"bounded output");
  long elapsedMs=(System.nanoTime()-t0)/1_000_000L;
  ok(elapsedMs<30_000,"corpus generation under 30 s, took "+elapsedMs+" ms");

  // --- Determinism. ---
  CreationScoreGenerator.Generation again=CreationScoreGenerator.generate(corpus,
     Collections.singletonList(segment(0,41*quarter)),"Corpus 320");
  ok(again.xml.equals(big.xml),"identical recording produces byte-identical MusicXML");

  // --- Clear rejections, never silent corruption. ---
  reject(()->CreationScoreGenerator.generate(new ArrayList<>(),
     Collections.singletonList(segment(0,quarter)),"vazio"),"empty recording rejected");
  reject(()->CreationScoreGenerator.generate(
     Arrays.asList(ev(0x90,60,80,5_000_000_000L,0)),Collections.singletonList(segment(0,quarter)),
     "fora"),"event outside segments rejected");
  reject(()->CreationScoreGenerator.generate(
     Arrays.asList(ev(0x90,60,80,0,0)),new ArrayList<>(),"sem segmentos"),"missing segments rejected");

  System.out.println("CREATION_ZP3_CORPUS_PASS checks="+checks+" elapsedMs="+elapsedMs);
 }
}

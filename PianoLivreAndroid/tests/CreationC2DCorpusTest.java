package br.org.pianolivre;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Focused C2-D MusicXML/Score interoperability and bounded synthetic MIDI corpus. */
public final class CreationC2DCorpusTest {
 private static int checks;
 private static void ok(boolean yes,String why){checks++;if(!yes)throw new AssertionError("C2-D: "+why);}
 private static MidiTakeRecorder.Event ev(int status,int pitch,int value,long time,int order){
  return new MidiTakeRecorder.Event(status,pitch,value,time,7,order,false);
 }
 private static MidiTakeRecorder.Segment segment(long start,long end){
  return new MidiTakeRecorder.Segment(start,end);
 }
 private static Score parse(CreationScoreTranscriber.Result result)throws Exception{
  Score score=Score.read(new ByteArrayInputStream(result.xml.getBytes(StandardCharsets.UTF_8)),"creation-score:c2-d-test");
  ok(score.measures==result.measures,"measure count survives Score.read");
  ok(score.events.size()==result.notes.size(),"every original attack survives Score.read");
  Map<Integer,Long> expected=result.notes.stream().collect(Collectors.groupingBy(n->n.pitch,Collectors.counting()));
  Map<Integer,Long> actual=score.events.stream().collect(Collectors.groupingBy(n->n.midi,Collectors.counting()));
  ok(actual.equals(expected),"pitches and duplicate attacks survive Score.read");
  return score;
 }
 private static void reject(RunnableChecked operation,String description)throws Exception{
  boolean failed=false;
  try{operation.run();}catch(IOException|IllegalArgumentException expected){failed=true;}
  ok(failed,description);
 }
 private interface RunnableChecked{void run()throws Exception;}
 private static void complex()throws Exception{
  List<MidiTakeRecorder.Event> events=Arrays.asList(
   ev(0x90,48,95,0,0),ev(0x90,60,80,0,1),ev(0x91,60,70,0,2),
   ev(0xB0,64,127,150_000_000L,3),
   ev(0x80,60,0,600_000_000L,4),
   ev(0x80,48,0,1_200_000_000L,5),
   ev(0x81,60,0,1_500_000_000L,6),
   ev(0x90,62,90,1_800_000_000L,7),
   ev(0x80,62,0,4_800_000_000L,8),
   ev(0xB0,64,0,5_000_000_000L,9));
  List<MidiTakeRecorder.Segment> segments=Collections.singletonList(segment(0,5_100_000_000L));
  List<MidiTakeRecorder.Event> copy=new ArrayList<>(events);
  for(int[] meter:new int[][]{{2,4},{3,4},{4,4},{6,8}}){
   for(int grid:new int[]{8,16,32}){
    CreationScoreTranscriber.Options o=new CreationScoreTranscriber.Options(100,meter[0],meter[1],grid,CreationScoreTranscriber.StaffMode.SUGGESTED,60,null);
    CreationScoreTranscriber.Result transcribed=CreationScoreTranscriber.transcribe(events,segments,o,"C2-D & <Piano>");
    ok(transcribed.notes.size()==4,"crossing/channel attacks preserved");
    ok(transcribed.notes.stream().map(n->n.sourceId).distinct().count()==4,"stable identities, including same pitch across channels");
    ok(transcribed.xml.contains("<beats>"+meter[0]+"</beats>"),"meter encoded");
    Score score=parse(transcribed);
    ok(score.events.stream().anyMatch(n->n.midi==48),"bass line retained");
    ok(transcribed.notes.get(0).endTick<transcribed.notes.get(3).endTick,"independent note releases");
    ok(transcribed.xml.contains("<backup>"),"multi-voice timeline uses backup");
    ok(CreationScoreTranscriber.transcribe(events,segments,o,"C2-D & <Piano>").xml.equals(transcribed.xml),"deterministic MusicXML");
   }
  }
  ok(events.equals(copy),"MIDI event container not changed");
  ok(events.get(0).timeNs==0&&events.get(6).status==0x81,"MIDI contents unchanged");
  CreationScoreTranscriber.Result defaultScore=CreationScoreTranscriber.transcribe(events,segments,CreationScoreTranscriber.Options.defaults(),"Long");
  String first=defaultScore.notes.get(0).sourceId;
  CreationScoreReview review=new CreationScoreReview(events,segments,"Long");
  ok(review.toggle(first),"select note for staff correction");
  review.moveSelection(1);
  parse(review.result());
  ok(review.result().notes.stream().filter(n->n.sourceId.equals(first)).allMatch(n->n.staff==1),"manual staff move");
  ok(review.undo(),"undo");
  ok(review.result().xml.equals(defaultScore.xml),"undo exact score");
  ok(review.redo(),"redo");
  ok(review.result().notes.size()==4,"redo retains attacks");
 }
 private static void longCorpus()throws Exception{
  List<MidiTakeRecorder.Event> source=new ArrayList<>();
  final int notes=320;
  for(int i=0;i<notes;i++){
   long start=i*250_000_000L;
   int pitch=48+(i%30);
   source.add(ev(0x90,pitch,80,start,2*i));
   source.add(ev(0x80,pitch,0,start+150_000_000L,2*i+1));
  }
  long begin=System.nanoTime();
  CreationScoreTranscriber.Options options=new CreationScoreTranscriber.Options(120,4,4,16,CreationScoreTranscriber.StaffMode.PITCH_THRESHOLD,60,null);
  CreationScoreTranscriber.Result result=CreationScoreTranscriber.transcribe(source,Collections.singletonList(segment(0,notes*250_000_000L)),options,"C2-D corpus de 320 notas");
  long millis=(System.nanoTime()-begin)/1_000_000L;
  ok(result.notes.size()==notes,"long source attacks retained");
  ok(result.measures>=30&&result.measures<=50,"long score measures bounded");
  ok(result.xml.length()<CreationScoreTranscriber.MAX_XML_CHARS,"XML within limit");
  ok(millis<30_000,"synthetic transcription completes inside 30 seconds");
  Score score=parse(result);
  ok(score.events.size()==notes,"all long-score attacks survive parser");
  ok(source.size()==notes*2&&source.get(0).status==0x90&&source.get(source.size()-1).status==0x80,"original events intact");
  reject(()->CreationScoreTranscriber.transcribe(source,Collections.singletonList(segment(0,notes*250_000_000L)),new CreationScoreTranscriber.Options(120,4,4,16,CreationScoreTranscriber.StaffMode.SUGGESTED,60,Collections.singletonMap("unknown-c2-id",2)),"invalid"),"orphan note override rejected");
 }
 public static void main(String[] args)throws Exception{
  complex();longCorpus();
  System.out.println("CREATION_C2_D_CORPUS_PASS checks="+checks);
 }
}

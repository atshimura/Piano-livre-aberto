package br.org.pianolivre;

import java.io.*;
import java.util.*;

/** Implementation 3 targeted JVM tests; no Android/emulator/APK. */
public final class CreationRhythmUniformizeTest {
 static int checks;
 static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 static MidiTakeRecorder.Event ev(int status,int pitch,int velocity,long ns,int order){
  return new MidiTakeRecorder.Event(status,pitch,velocity,ns,7,order,false);
 }
 static List<MidiTakeRecorder.Segment> segment(long end){
  return Collections.singletonList(new MidiTakeRecorder.Segment(0,end));
 }
 static List<MidiTakeRecorder.Event> melody(){
  List<MidiTakeRecorder.Event> events=new ArrayList<>();
  long[] at={0,570_000_000L,1_210_000_000L,1_900_000_000L};
  for(int i=0;i<at.length;i++){
   events.add(ev(0x90,60+i*2,80,at[i],i*2));
   events.add(ev(0x80,60+i*2,0,at[i]+(i==3?550_000_000L:100_000_000L),i*2+1));
  }
  events.sort(Comparator.comparingLong((MidiTakeRecorder.Event e)->e.timeNs).thenComparingInt(e->e.order));
  return events;
 }
 static CreationScoreReview review()throws Exception{
  return new CreationScoreReview(melody(),segment(2_600_000_000L),"Semínimas humanas");
 }
 static void expectError(RunnableChecked code)throws Exception{
  boolean failed=false;try{code.run();}catch(IOException expected){failed=true;}
  check(failed,"conflicting or incomplete selection rejected");
 }
 interface RunnableChecked{void run()throws Exception;}
 static void attacks()throws Exception{
  CreationScoreReview work=review();
  List<CreationScoreTranscriber.WrittenNote> notes=work.result().notes;
  check(notes.size()==4,"four distinct MIDI attacks");
  check(notes.get(0).endTick==notes.get(1).startTick,"first release short but written until next attack");
  check(notes.get(1).endTick==notes.get(2).startTick,"second attack decides the written duration");
  check(notes.get(2).endTick==notes.get(3).startTick,"third attack decides the written duration");
  check(notes.get(0).sourceEndNs-notes.get(0).sourceStartNs==100_000_000L,"original short key press preserved");
  check(notes.get(0).endTick==960&&notes.get(1).endTick==1920,"human quarter-note timing regularized");
  check(!work.options().legacyTiming,"new projects use onset-based interpretation");
 }
 static void hands()throws Exception{
  List<MidiTakeRecorder.Event> events=Arrays.asList(
   ev(0x90,48,80,0,0),ev(0x90,72,80,0,1),
   ev(0x80,72,0,100_000_000L,2),ev(0x80,48,0,100_000_000L,3),
   ev(0x90,74,80,600_000_000L,4),ev(0x80,74,0,700_000_000L,5),
   ev(0x90,50,80,1_200_000_000L,6),ev(0x80,50,0,1_300_000_000L,7));
  Map<String,Integer> hand=new HashMap<>();
  hand.put("c2-0-7-0-48",2);hand.put("c2-6-7-0-50",2);
  hand.put("c2-1-7-0-72",1);hand.put("c2-4-7-0-74",1);
  CreationScoreTranscriber.Options base=CreationScoreTranscriber.Options.defaults();
  CreationScoreTranscriber.Options options=new CreationScoreTranscriber.Options(
   100,4,4,16,base.staffMode,base.splitPitch,null,
   CreationScoreTranscriber.Regularization.BALANCED,hand,null,null,false);
  CreationScoreTranscriber.Result score=CreationScoreTranscriber.transcribe(events,segment(1_500_000_000L),options,"duas mãos");
  Map<String,CreationScoreTranscriber.WrittenNote> byId=new HashMap<>();
  for(CreationScoreTranscriber.WrittenNote n:score.notes)byId.put(n.sourceId,n);
  check(byId.get("c2-0-7-0-48").endTick==1920,"left-hand rhythm ignores right-hand attack at 600ms");
  check(byId.get("c2-1-7-0-72").endTick==960,"right-hand rhythm independent from left");
  check(byId.get("c2-0-7-0-48").hand==2&&byId.get("c2-1-7-0-72").hand==1,
        "explicit hands retained, not inferred from staff");
 }
 static void uniformize()throws Exception{
  CreationScoreReview work=review();
  byte[] immutable=work.result().xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
  work.selectMeasures(1,2);
  check(work.selectedCount()==4,"selection by measures");
  CreationScoreReview.Uniformization plan=work.previewUniformize(960,0,0);
  check(plan.noteCount==4&&plan.voiceCount>=1,"quarter-note preview plans all selected attacks");
  check(!work.isDirty()&&Arrays.equals(immutable,work.result().xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        "preview is read-only");
  work.applyUniformize(plan);
  List<CreationScoreTranscriber.WrittenNote> n=work.result().notes;
  for(int i=0;i<4;i++)check(n.get(i).startTick==i*960&&n.get(i).endTick==(i+1)*960,
                            "all four written as consecutive quarter notes");
  check(work.options().rhythmOverrides.size()==4,"manual rhythm by stable Note On IDs");
  check(work.canUndo()&&work.isDirty(),"atomic adjustment is undoable");
  check(work.undo(),"undo succeeds");
  check(Arrays.equals(immutable,work.result().xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        "undo exact previous engraving");
  check(work.redo(),"redo succeeds");
  check(work.result().notes.get(3).startTick==2880,"redo reapplies quarter-note notation");
  work.restoreAutomaticRhythm();
  check(work.options().rhythmOverrides.isEmpty(),"restore automatic removes rhythm overrides");
  check(work.result().notes.get(3).startTick!=2880,"restored automatic attack pattern");

  CreationScoreReview conflict=review();
  String first=conflict.result().notes.get(0).sourceId;
  conflict.toggle(first);
  expectError(()->conflict.previewUniformize(3840,0,0));
  check(!conflict.isDirty()&&conflict.result().notes.get(0).startTick==0,
        "conflict leaves previous score unchanged");
 }
 static void chord()throws Exception{
  List<MidiTakeRecorder.Event> events=Arrays.asList(
   ev(0x90,60,80,0,0),ev(0x90,64,80,0,1),
   ev(0x80,60,0,100_000_000L,2),ev(0x80,64,0,100_000_000L,3),
   ev(0x90,62,80,600_000_000L,4),ev(0x80,62,0,700_000_000L,5));
  CreationScoreReview work=new CreationScoreReview(events,segment(900_000_000L),"Acorde");
  String first=work.result().notes.get(0).sourceId;
  work.toggle(first);
  expectError(()->work.previewUniformize(480,0,0));
  work.selectChord(first);
  check(work.selectedCount()==2,"select whole chord");
  CreationScoreReview.Uniformization plan=work.previewUniformize(480,0,0);
  work.applyUniformize(plan);
  check(work.result().notes.get(0).startTick==work.result().notes.get(1).startTick,
      "chord retains common onset");
  check(work.result().xml.contains("<chord/>"),"chord stays in MusicXML");
 }
 public static void main(String[] args)throws Exception{
  attacks();hands();uniformize();chord();
  System.out.println("CREATION_RHYTHM_BASIC_PASS checks="+checks);
 }
}

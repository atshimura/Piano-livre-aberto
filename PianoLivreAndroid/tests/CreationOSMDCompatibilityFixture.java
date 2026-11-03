package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Build synthetic real Creation MusicXML and native Score head events for OSMD. */
public final class CreationOSMDCompatibilityFixture {
 static MidiTakeRecorder.Event event(int status,int pitch,long t,int order){
  return new MidiTakeRecorder.Event(status,pitch,(status&0xf0)==0x90?85:0,t,7,order,false);
 }
 static void write(Path dir,String name,List<MidiTakeRecorder.Event> events,long end,
                   CreationScoreTranscriber.Options options)throws Exception{
  events.sort(Comparator.comparingLong((MidiTakeRecorder.Event e)->e.timeNs).thenComparingInt(e->e.order));
  CreationScoreTranscriber.Result derived=CreationScoreTranscriber.transcribe(
    events,Collections.singletonList(new MidiTakeRecorder.Segment(0,end)),options,name);
  Score score=Score.read(new ByteArrayInputStream(derived.xml.getBytes(StandardCharsets.UTF_8)),name+".musicxml");
  if(score.writtenEvents.isEmpty()||score.events.size()!=derived.notes.size())
   throw new AssertionError("Creation fixture notes lost at Score.read: "+name);
  StringBuilder values=new StringBuilder("[");
  for(Score.Event n:score.writtenEvents){
   if(values.length()>1)values.append(',');
   values.append('{').append("\"beat\":").append(n.beat)
    .append(",\"length\":").append(n.length).append(",\"midi\":").append(n.midi)
    .append(",\"staff\":").append(n.staff).append(",\"measure\":").append(n.measure)
    .append(",\"voice\":\"").append(n.voiceId).append("\"")
    .append(",\"sourcePart\":").append(n.originalPartIndex())
    .append(",\"playbackAttack\":").append(n.playbackAttack);
   if(n.writtenStep!=null)values.append(",\"step\":\"").append(n.writtenStep)
     .append("\",\"octave\":").append(n.writtenOctave)
     .append(",\"alter\":").append(Double.isFinite(n.writtenAlter)?n.writtenAlter:0);
   values.append('}');
  }
  values.append(']');
  Files.writeString(dir.resolve(name+".musicxml"),derived.xml,StandardCharsets.UTF_8);
  Files.writeString(dir.resolve(name+".json"),values.toString(),StandardCharsets.UTF_8);
  System.out.println("IMPL4_FIXTURE "+name+" notes="+score.writtenEvents.size()+" measures="+score.measures);
 }
 static List<MidiTakeRecorder.Event> melodic(int count,int step,long interval,long press){
  List<MidiTakeRecorder.Event> result=new ArrayList<>();
  for(int i=0;i<count;i++){
   long time=i*interval;
   result.add(event(0x90,48+(i*step)%36,time,2*i));
   result.add(event(0x80,48+(i*step)%36,time+press,2*i+1));
  }
  return result;
 }
 public static void main(String[] args)throws Exception{
  Path dir=Paths.get("build/impl4-fixtures");Files.createDirectories(dir);
  CreationScoreTranscriber.Options d=CreationScoreTranscriber.Options.defaults();
  write(dir,"melody",melodic(8,2,600_000_000L,100_000_000L),5_000_000_000L,d);
  write(dir,"legacy",melodic(8,1,600_000_000L,270_000_000L),5_000_000_000L,
    CreationScoreTranscriber.Options.legacy(100,4,4,16,
       CreationScoreTranscriber.StaffMode.SUGGESTED,60,null));
  List<MidiTakeRecorder.Event> chords=new ArrayList<>();
  int ix=0;
  for(int t=0;t<4;t++){
   long at=t*600_000_000L;
   for(int pitch:new int[]{48+t,60+t*2,64+t*2,67+t*2}){
    chords.add(event(0x90,pitch,at,ix++));
    chords.add(event(0x80,pitch,at+190_000_000L,ix++));
   }
  }
  write(dir,"chords",chords,3_100_000_000L,d);
  List<MidiTakeRecorder.Event> voices=new ArrayList<>();ix=0;
  for(int i=0;i<8;i++){
   long at=i*450_000_000L;
   voices.add(event(0x90,i%2==0?48+i:72-i,at,ix++));
   voices.add(event(0x80,i%2==0?48+i:72-i,at+150_000_000L,ix++));
  }
  write(dir,"crossing",voices,4_100_000_000L,d);
  write(dir,"long",melodic(120,1,240_000_000L,140_000_000L),29_000_000_000L,
     new CreationScoreTranscriber.Options(120,4,4,16,
         CreationScoreTranscriber.StaffMode.PITCH_THRESHOLD,60,null));
  // Two physically assigned independent hands (source IDs are canonical).
  CreationScoreTranscriber.Result original=CreationScoreTranscriber.transcribe(
    voices,Collections.singletonList(new MidiTakeRecorder.Segment(0,4_100_000_000L)),d,"assigned");
  Map<String,Integer> hands=new HashMap<>(),explicitVoices=new HashMap<>();
  for(CreationScoreTranscriber.WrittenNote n:original.notes){
   hands.put(n.sourceId,n.pitch<60?2:1);explicitVoices.put(n.sourceId,1);
  }
  write(dir,"assigned",voices,4_100_000_000L,
    new CreationScoreTranscriber.Options(100,4,4,16,d.staffMode,60,null,
        CreationScoreTranscriber.Regularization.BALANCED,hands,explicitVoices,null,false));
  System.out.println("IMPL4_CREATION_FIXTURES_PASS count=6");
 }
}

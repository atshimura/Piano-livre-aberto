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
  exportXml(dir,name,derived.xml,score.writtenEvents.size());
 }
 static void exportXml(Path dir,String name,String xml,int expected)throws Exception{
  Score score=Score.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),name+".musicxml");
  if(score.writtenEvents.size()!=expected)
   throw new AssertionError("MusicXML notes missing after native parse: "+name);
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
  Files.writeString(dir.resolve(name+".musicxml"),xml,StandardCharsets.UTF_8);
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
  write(dir,"solo-created",melodic(1,0,600_000_000L,600_000_000L),1_000_000_000L,d);
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
  // Regression #108: original Standard MIDI import into lower staff only.
  // This is exactly the shape that once made OpticalTimeMap report
  // "Optical timeline without events" in Study after a successful MIDI import.
  byte[] track={0,(byte)0x90,60,80,(byte)0x83,0x60,(byte)0x80,60,0,0,(byte)0xff,0x2f,0};
  ByteArrayOutputStream midiBytes=new ByteArrayOutputStream();
  DataOutputStream midiOut=new DataOutputStream(midiBytes);
  midiOut.writeBytes("MThd");midiOut.writeInt(6);midiOut.writeShort(0);
  midiOut.writeShort(1);midiOut.writeShort(480);
  midiOut.writeBytes("MTrk");midiOut.writeInt(track.length);midiOut.write(track);
  byte[] source=midiBytes.toByteArray();
  MidiFileImport.Result imported=MidiFileImport.read(new ByteArrayInputStream(source));
  for(int assignedStaff:new int[]{1,2}){
   String name="midi-single-staff"+assignedStaff;
   String xml=new String(MidiFileImport.musicXml(imported,
     Collections.singletonMap(0,assignedStaff),name),StandardCharsets.UTF_8);
   exportXml(dir,name,xml,1);
   if(!Arrays.equals(imported.original,source))
    throw new AssertionError("MIDI original changed while writing "+name);
  }
  System.out.println("IMPL4_CREATION_FIXTURES_PASS count=8 (includes MIDI imported on either staff)");
 }
}

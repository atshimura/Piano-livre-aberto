package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Build synthetic real Creation MusicXML and native Score head events for OSMD,
 *  using the Z restart generator (ZAI-001). Parser survival is mandatory: every
 *  fixture must round-trip through Score.read with all written notes preserved. */
public final class CreationOSMDCompatibilityFixture {
 static MidiTakeRecorder.Event event(int status,int pitch,long t,int order){
  return new MidiTakeRecorder.Event(status,pitch,(status&0xf0)==0x90?85:0,t,7,order,false);
 }
 static void write(Path dir,String name,List<MidiTakeRecorder.Event> events,long end)throws Exception{
  events.sort(Comparator.comparingLong((MidiTakeRecorder.Event e)->e.timeNs).thenComparingInt(e->e.order));
  CreationScoreGenerator.Generation derived=CreationScoreGenerator.generate(
    events,Collections.singletonList(new MidiTakeRecorder.Segment(0,end)),name);
  exportXml(dir,name,derived.xml,derived.notes.size());
 }
 static void exportXml(Path dir,String name,String xml,int expected)throws Exception{
  Score score=Score.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),name+".musicxml");
  // Tied fragments legitimately multiply written events; the source note itself must never vanish.
  if(score.writtenEvents.size()<expected)
   throw new AssertionError("MusicXML notes missing after native parse: "+name
     +" ("+score.writtenEvents.size()+" of "+expected+")");
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
 /** ZAI-001 §3.4: eight quarters followed by eight halves on a steady pulse. */
 static List<MidiTakeRecorder.Event> quartersThenHalves(long quarter,int startOrder){
  int[] scale={60,62,64,65,67,69,71,72};
  ArrayList<MidiTakeRecorder.Event> events=new ArrayList<>();
  for(int i=0;i<8;i++){
   long at=i*quarter;
   events.add(event(0x90,scale[i],at,startOrder+2*i));
   events.add(event(0x80,scale[i],at+quarter,startOrder+2*i+1));
  }
  for(int i=0;i<8;i++){
   long at=(8+2L*i)*quarter;
   events.add(event(0x90,scale[i],at,startOrder+16+2*i));
   events.add(event(0x80,scale[i],at+2*quarter,startOrder+16+2*i+1));
  }
  return events;
 }
 public static void main(String[] args)throws Exception{
  Path dir=Paths.get("build/impl4-fixtures");Files.createDirectories(dir);
  write(dir,"melody",melodic(8,2,600_000_000L,100_000_000L),5_000_000_000L);
  write(dir,"solo-created",melodic(1,0,600_000_000L,600_000_000L),1_000_000_000L);
  write(dir,"quarters-halves-120",quartersThenHalves(500_000_000L,0),13_000_000_000L);
  write(dir,"quarters-halves-90",quartersThenHalves(666_666_666L,0),17_000_000_000L);
  List<MidiTakeRecorder.Event> chords=new ArrayList<>();
  int ix=0;
  for(int t=0;t<4;t++){
   long at=t*600_000_000L;
   for(int pitch:new int[]{48+t,60+t*2,64+t*2,67+t*2}){
    chords.add(event(0x90,pitch,at,ix++));
    chords.add(event(0x80,pitch,at+190_000_000L,ix++));
   }
  }
  write(dir,"chords",chords,3_100_000_000L);
  List<MidiTakeRecorder.Event> voices=new ArrayList<>();ix=0;
  for(int i=0;i<8;i++){
   long at=i*450_000_000L;
   voices.add(event(0x90,i%2==0?48+i:72-i,at,ix++));
   voices.add(event(0x80,i%2==0?48+i:72-i,at+150_000_000L,ix++));
  }
  write(dir,"crossing",voices,4_100_000_000L);
  write(dir,"long",melodic(120,1,240_000_000L,140_000_000L),29_000_000_000L);
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
  System.out.println("IMPL4_CREATION_FIXTURES_PASS count=9 (includes MIDI imported on either staff)");
 }
}

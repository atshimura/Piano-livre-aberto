import br.org.pianolivre.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Deterministic quarter-note timeline tests; no instrument required. */
public final class TempoMapTest {
 static int assertions=0;
 static void check(boolean ok,String s){assertions++;if(!ok)throw new AssertionError(s);}
 static void eq(double actual,double expected,String why){
  check(Math.abs(actual-expected)<1e-7,why+": "+actual+" vs "+expected);
 }
 static Score xml(String measures)throws Exception{
  String text="<?xml version='1.0'?><score-partwise version='4.0'><part-list>"+
   "<score-part id='P1'><part-name>Piano</part-name></score-part></part-list>"+
   "<part id='P1'>"+measures+"</part></score-partwise>";
  return Score.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),"tempos.xml");
 }
 static String measure(int n,String body){
  return "<measure number='"+n+"'><attributes><divisions>4</divisions>"+
   "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"+
   body+"</measure>";
 }
 static String note(int duration){
  return "<note><pitch><step>C</step><octave>4</octave></pitch><duration>"+duration+"</duration></note>";
 }
 static String tempo(int bpm){
  return "<direction><sound tempo='"+bpm+"'/></direction>";
 }
 public static void main(String[] args)throws Exception{
  Score s=xml(measure(1,tempo(120)+note(8)+tempo(60)+note(8))+
    measure(2,tempo(180)+note(16)));
  check(s.tempoChanges.size()==3,"three tempo directions");
  eq(s.tempoAt(0),120,"initial bpm");
  eq(s.tempoAt(1.999),120,"old bpm before beat 2");
  eq(s.tempoAt(2),60,"new bpm at beat 2");
  eq(s.tempoAt(3.999),60,"slow before next measure");
  eq(s.tempoAt(4),180,"next measure bpm");
  eq(s.advanceBeats(0,1000,1),2,"1 s at 120");
  eq(s.advanceBeats(1.5,1000,1),2.75,"cross 120 to 60 within 1 second");
  eq(s.advanceBeats(3.5,1500,1),7,"cross 60 to 180 within 1.5 s");
  eq(s.advanceBeats(0,1000,.5),1,"half speed");
  eq(s.advanceBeats(2,1000,2),4,"double speed crosses next tempo");
  eq(s.advanceBeats(0,0,1),0,"zero elapsed");
  eq(s.advanceBeats(2,-5,1),2,"negative elapsed");
  eq(s.advanceBeats(0,1000,0),0,"zero speed");
  eq(s.advanceBeats(3.5,500,.5),3.75,"slow effective tempo");
  eq(s.tempo,120,"legacy initial tempo not last");
  Score defaultTempo=xml(measure(1,note(4)+tempo(90)+note(12)));
  eq(defaultTempo.tempoAt(0),100,"fallback before first tempo direction");
  eq(defaultTempo.tempoAt(1),90,"direction at one quarter");
  check(defaultTempo.tempo==100,"fallback legacy tempo");
  Score measureBoundary=xml(measure(1,tempo(75)+note(16))+
   measure(2,tempo(150)+note(16)));
  eq(measureBoundary.tempoAt(3.999),75,"measure boundary old");
  eq(measureBoundary.tempoAt(4),150,"measure boundary new");
  eq(measureBoundary.advanceBeats(3,1600,1),6,"consume remaining tempo in second measure");
  Score invalid=xml(measure(1,tempo(0)+note(16)));
  check(invalid.tempoChanges.isEmpty(),"ignore invalid zero bpm");
  eq(invalid.advanceBeats(0,600,1),1,"invalid uses default");
  Score printed=xml(measure(1,
   "<direction><direction-type><metronome><beat-unit>quarter</beat-unit>"+
   "<beat-unit-dot/><per-minute>72</per-minute></metronome></direction-type></direction>"+
   note(8)+
   "<direction><direction-type><metronome><beat-unit>half</beat-unit>"+
   "<per-minute>60</per-minute></metronome></direction-type></direction>"+note(8)));
  eq(printed.tempoAt(0),108,"dotted quarter 72 means quarter BPM 108");
  eq(printed.tempoAt(2),120,"half note 60 means quarter BPM 120");
  System.out.println("PASS TempoMapTest "+assertions+" assertions for initial, intra-measure and cross-measure changes");
 }
}

import br.org.pianolivre.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Reproducible regression tests for physical note attacks and score hands. */
public final class WaitRecognizerTest {
 static int count=0;
 static void ok(boolean v,String why){if(!v)throw new AssertionError(why);count++;}
 static Score.Event event(double beat,int pitch,int staff){return new Score.Event(beat,1,pitch,staff,1,1);}
 static void waitTests(){
  for(int repeat=0;repeat<5;repeat++)for(int hand=0;hand<3;hand++)for(int pitch=21;pitch<=108;pitch++){
   WaitRecognizer w=new WaitRecognizer();
   w.arm(Arrays.asList(event(0,pitch,hand==2?2:1)));
   int wrong=pitch==108?21:pitch+1;
   Set<Integer> keys=new HashSet<>(Arrays.asList(pitch,wrong));
   ok(!w.noteOn(wrong,true,keys),"wrong attack may not count");
   ok(!w.noteOn(pitch,false,keys),"held attack may not count");
   keys.remove(pitch);w.noteOff(pitch);keys.add(pitch);
   ok(w.noteOn(pitch,true,keys),"new correct attack must count");
   w.arm(Arrays.asList(event(1,pitch,hand==2?2:1)));
   ok(!w.noteOn(wrong,true,keys),"previous pitch must not count at new position");
   keys.remove(pitch);w.noteOff(pitch);keys.add(pitch);
   ok(w.noteOn(pitch,true,keys),"repeated pitch should count only with off/new on");
  }
  WaitRecognizer chord=new WaitRecognizer();
  chord.arm(Arrays.asList(event(0,60,1),event(0,64,1),event(0,67,1)));
  Set<Integer> keys=new HashSet<>();
  keys.add(60);ok(!chord.noteOn(60,true,keys),"chord too early");
  keys.add(64);ok(!chord.noteOn(64,true,keys),"chord too early 2");
  keys.remove(64);chord.noteOff(64);
  keys.add(67);ok(!chord.noteOn(67,true,keys),"released chord must not pass");
  keys.add(64);ok(chord.noteOn(64,true,keys),"held simultaneous chord should pass");
  chord.arm(Collections.emptyList());ok(!chord.noteOn(60,true,keys),"empty expected not completed");
 }
 static void selectedTeachingPolicyTests(){
  for(int hand=0;hand<3;hand++)for(int repeat=0;repeat<20;repeat++){
   WaitRecognizer w=new WaitRecognizer();
   Score.Event c=new Score.Event(0,2,60,hand==2?2:1,1,1);
   Score.Event e=new Score.Event(0,3,64,hand==2?2:1,1,1);
   w.arm(Arrays.asList(c,e));
   ok(w.chordWindowMs()==null,"uncalibrated chord window must never be guessed");
   Set<Integer> held=new HashSet<>();
   held.add(67);
   WaitRecognizer.Result result=w.noteOnTimed(67,true,held,100);
   ok(result.extra&&!result.advance&&result.extraPitches.contains(67),
    "an extra note must be indicated but not advance by itself");
   held.add(60);
   result=w.noteOnTimed(60,true,held,120);
   ok(!result.advance,"incomplete chord not accepted");
   held.add(64);
   result=w.noteOnTimed(64,true,held,90000);
   ok(result.advance&&result.extraPitches.contains(67),
    "uncalibrated chords must permit slow completion and extras");
   w.arm(Arrays.asList(c,e));
   w.setChordWindowMs(70L); // SYNTHETIC TEST VALUE only, never product default
   held=new HashSet<>();
   held.add(60);
   result=w.noteOnTimed(60,true,held,1000);
   ok(!result.advance,"first chord note not enough");
   held.add(64);
   result=w.noteOnTimed(64,true,held,1060);
   ok(result.advance,"measured chord within configured window must advance");
   w.arm(Arrays.asList(c,e));
   held=new HashSet<>(Arrays.asList(60,64));
   w.noteOnTimed(60,true,held,2000);
   result=w.noteOnTimed(64,true,held,2071);
   ok(!result.advance&&result.windowExceeded,"spread beyond calibrated window must not advance");
   // Both note attacks have to occur AFTER the chord is rearmed.
   w.noteOff(60);w.noteOff(64);held.clear();
   held.add(60);w.noteOnTimed(60,true,held,2080);
   held.add(64);result=w.noteOnTimed(64,true,held,2100);
   ok(result.advance,"replayed chord within window must advance");
   w.arm(Collections.singletonList(c));
   held=new HashSet<>(Collections.singletonList(60));
   result=w.noteOnTimed(60,true,held,3000);
   ok(result.advance,"single note attack passes regardless chord window/duration");
  }
  System.out.println("PASS selected pedagogy: extra pitches warn but permit progress; measured chord spread; no duration/pedal gate");
 }
 static void playbackTests(){
  Score s=new Score();
  s.events.add(event(0,60,1));s.events.add(event(0,48,2));
  for(int hand=0;hand<3;hand++){
   List<String> events=new ArrayList<>();
   MidiPlayback player=new MidiPlayback(s,hand,(pitch,on,velocity)->events.add((on?"+":"-")+pitch));
   player.seek(0);
   ok(events.contains("+60")== (hand!=2),"RH demo selection incorrect");
   ok(events.contains("+48")== (hand!=1),"LH demo selection incorrect");
   player.stop();
   ok(!events.contains("-60")||hand!=2,"wrong stopped note");
  }
 }
 static void navigationTests()throws Exception{
  Score fixture=new Score();
  fixture.events.add(event(0,60,1));fixture.events.add(event(1,48,2));fixture.events.add(event(4,62,1));
  ok(HandNavigation.nextPosition(fixture,2,1,0,Double.MAX_VALUE)==4,"switch to right should advance locally");
  ok(HandNavigation.nextPosition(fixture,2,2,0,Double.MAX_VALUE)==2,"no later left should remain here");
  ok(HandNavigation.nextPosition(fixture,2,1,0,3)==0,"active loop may wrap inside loop");
  ok(HandNavigation.nextPosition(fixture,4,1,0,Double.MAX_VALUE)==4,"current note must be preserved");
  java.io.File[] files=new java.io.File("app/src/main/assets/scores").listFiles((d,n)->n.endsWith(".mxl"));
  if(files==null||files.length!=10)throw new AssertionError("Missing 10 scores");
  int checked=0;
  for(java.io.File file:files){
   Score s;try(java.io.FileInputStream in=new java.io.FileInputStream(file)){s=Score.read(in,file.getName());}
   java.util.TreeSet<Double> beats=new java.util.TreeSet<>();
   for(Score.Event e:s.events)beats.add(e.beat);
   for(double beat:beats)for(int hand=0;hand<=2;hand++){
    double result=HandNavigation.nextPosition(s,beat,hand,0,Double.MAX_VALUE);
    ok(result>=beat-.000001,"unexpected backward jump on "+file+" hand="+hand+" at="+beat);
    ok(result<=s.end()+.000001,"position exceeds duration");
    if(!s.at(beat,hand).isEmpty())ok(Math.abs(result-beat)<.000001,"onset shifted unexpectedly");
    checked++;
   }
  }
  System.out.println("PASS navigation across "+checked+" positions/hand in ten scores");
 }
 static void splitPartTests()throws Exception{
  String xml="<?xml version='1.0'?><score-partwise version='4.0'><part-list>"+
  "<score-part id='P1'><part-name>RH</part-name></score-part><score-part id='P2'><part-name>LH</part-name></score-part>"+
  "</part-list><part id='P1'><measure number='1'><attributes><divisions>1</divisions></attributes>"+
  "<note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><staff>1</staff></note></measure></part>"+
  "<part id='P2'><measure number='1'><attributes><divisions>1</divisions></attributes>"+
  "<note><pitch><step>G</step><octave>3</octave></pitch><duration>1</duration><staff>1</staff></note></measure></part></score-partwise>";
  Score s=Score.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),"two.musicxml");
  ok(s.at(0,0).size()==2,"both must include both parts");
  ok(s.at(0,1).size()==1&&s.at(0,1).get(0).midi==60,"right split-part incorrect");
  ok(s.at(0,2).size()==1&&s.at(0,2).get(0).midi==55,"left split-part incorrect");
 }
 public static void main(String[] args)throws Exception{
  waitTests();selectedTeachingPolicyTests();playbackTests();splitPartTests();navigationTests();
  System.out.println("PASS WaitRecognizerTest "+count+" assertions, 88 pitches x 3 hands x 5 cycles, chords, repeated notes, separate MusicXML parts, MIDI demo filtering");
 }
}

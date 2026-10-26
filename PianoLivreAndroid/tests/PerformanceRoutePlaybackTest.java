import br.org.pianolivre.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Run actual demonstration MIDI actions through a written-score route. */
public final class PerformanceRoutePlaybackTest {
 static int checks=0;
 static void ok(boolean cond,String msg){checks++;if(!cond)throw new AssertionError(msg);}
 static Score piece()throws Exception{
  String[] steps={"C","D","E","F","G"};
  StringBuilder xml=new StringBuilder(
   "<score-partwise version='4.0'><part-list><score-part id='P1'><part-name>Piano</part-name></score-part></part-list><part id='P1'>");
  for(int n=0;n<5;n++){
   xml.append("<measure number='").append(n+1)
    .append("'><attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>");
   if(n==0)xml.append("<barline location='left'><repeat direction='forward'/></barline>");
   if(n==2)xml.append("<barline location='left'><ending number='1' type='start'/></barline>");
   if(n==3)xml.append("<barline location='left'><ending number='2' type='start'/></barline>");
   xml.append("<note><pitch><step>").append(steps[n]).append("</step><octave>4</octave></pitch><duration>4</duration><staff>1</staff></note>");
   if(n==2)xml.append("<barline location='right'><ending number='1' type='stop'/><repeat direction='backward' times='2'/></barline>");
   if(n==3)xml.append("<barline location='right'><ending number='2' type='discontinue'/></barline>");
   xml.append("</measure>");
  }
  xml.append("</part></score-partwise>");
  Score score=Score.read(new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8)),"route.xml");
  for(int n=0;n<5;n++)
   score.events.add(new Score.Event(n*4,4,48+n,2,1,n+1));
  score.events.sort(Comparator.comparingDouble(e->e.beat));
  return score;
 }
 static void verify(int hand,int...notes){
  Score score;
  try{score=piece();}catch(Exception e){throw new RuntimeException(e);}
  PerformanceRoute.Result plan=PerformanceRoute.build(score);
  ok(plan.supported(),"route not supported");
  ArrayList<Integer> on=new ArrayList<>();
  ArrayList<Integer> off=new ArrayList<>();
  MidiPlayback midi=new MidiPlayback(score,hand,(pitch,pressed,velocity)->{
   if(pressed)on.add(pitch);else off.add(pitch);
  });
  PerformanceRoute.Cursor cursor=new PerformanceRoute.Cursor(plan);
  cursor.align(0);
  midi.seek(0);
  PerformanceRoute.Movement result=cursor.tick(score,0,20000,1);
  for(PerformanceRoute.Move move:result.moves){
   if(move.seek)midi.seek(move.beat);
   else midi.advance(move.beat);
  }
  ok(result.finished,"full performance should end after seven measures");
  midi.stop();
  ArrayList<Integer> expected=new ArrayList<>();for(int n:notes)expected.add(n);
  ok(on.equals(expected),"MIDI note-on route hand="+hand+" expected "+expected+" got "+on);
  ok(off.size()==on.size(),"all MIDI keys must receive note-off in routed demo");
  ok(midi.soundingKeys()==0,"no hanging voices after the final note");
 }
 static void waitAndGuide()throws Exception{
  Score score=piece();
  KeyboardGuide guide=new KeyboardGuide(score);
  PerformanceRoute.Cursor cursor=new PerformanceRoute.Cursor(PerformanceRoute.build(score));
  cursor.align(0);
  ok(guide.at(0).both.contains(60)&&guide.at(0).both.contains(48),
     "keyboard represents two hands irrespective of teaching filter");
  int[] expected={4,8,0,4,12,16};
  double pos=0;
  for(int want:expected){
   pos=cursor.nextWait(score,pos,1);
   ok(Math.abs(pos-want)<1e-7,"Wait itinerary for RH: "+pos+" vs "+want);
   ok(!guide.at(pos).both.isEmpty(),"keyboard has active music at traversed beat");
  }
  PerformanceRoute.Cursor left=new PerformanceRoute.Cursor(PerformanceRoute.build(score));
  left.align(0);pos=0;
  for(int want:expected){
   pos=left.nextWait(score,pos,2);
   ok(Math.abs(pos-want)<1e-7,"Wait itinerary for LH: "+pos+" vs "+want);
  }
 }
 public static void main(String[] args)throws Exception{
  verify(1,60,62,64,60,62,65,67);
  verify(2,48,49,50,48,49,51,52);
  verify(0,48,60,49,62,50,64,48,60,49,62,51,65,52,67);
  waitAndGuide();
  System.out.println("PASS PerformanceRoutePlaybackTest "+checks+
    " assertions: actual MIDI Note On/Off order, two-hand visual guide, Right/Left/Both Wait navigation");
 }
}

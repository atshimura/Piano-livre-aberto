import br.org.pianolivre.*;
import java.io.*;
import java.util.*;

/** A short/long deterministic Study journey, using the actual catalog scores. */
public final class StudyJourneyTest {
 static int checks=0;
 static void ok(boolean c,String msg){checks++;if(!c)throw new AssertionError(msg);}
 static final class Sink implements MidiPlayback.Sink {
  final Set<Integer> active=new HashSet<>();int ons,offs;
  public void note(int pitch,boolean on,int velocity){
   if(on){ok(active.add(pitch),"duplicate MIDI note-on "+pitch);ons++;}
   else{ok(active.remove(pitch),"orphan MIDI note-off "+pitch);offs++;}
  }
  void silent(){ok(active.isEmpty(),"stuck MIDI pitches "+active);}
 }
 static Score load(String file)throws Exception{
  try(InputStream in=new FileInputStream("app/src/main/assets/scores/"+file)){
   return Score.read(in,file);
  }
 }
 static void run(String file)throws Exception{
  Score score=load(file);
  ok(score.events.size()>40,file+" has no meaningful musical events");
  ok(score.end()>0,file+" has no duration");
  KeyboardGuide guide=new KeyboardGuide(score);
  int originalEvents=score.events.size();
  for(int hand=0;hand<=2;hand++){
   double start=score.firstOnset(hand);
   List<Score.Event> expected=score.at(start,hand);
   ok(!expected.isEmpty(),file+" hand "+hand+" has no starting note");
   Set<Integer> expectedPitches=new HashSet<>();
   for(Score.Event e:expected)if(e.midi>=0&&e.midi<=127)expectedPitches.add(e.midi);
   ok(!expectedPitches.isEmpty(),file+" hand "+hand+" missing valid attacks");
   WaitRecognizer wait=new WaitRecognizer();
   wait.arm(expected);
   Set<Integer> held=new HashSet<>();
   int extra=21;while(expectedPitches.contains(extra))extra++;
   held.add(extra);
   WaitRecognizer.Result mistake=wait.noteOnTimed(extra,true,held,0);
   ok(mistake.extra&&!mistake.advance,"extra attack incorrectly passed Wait");
   long t=1000;WaitRecognizer.Result last=null;
   for(int pitch:expectedPitches){held.add(pitch);last=wait.noteOnTimed(pitch,true,held,t++);}
   ok(last!=null&&last.advance,"expected chord did not pass without imposed time penalty");
   KeyboardGuide.Frame first=guide.at(start);
   Set<Integer> union=new HashSet<>(first.right);union.addAll(first.left);
   ok(first.both.equals(union),"guide lost a hand at start");
   Sink sink=new Sink();
   MidiPlayback demo=new MidiPlayback(score,hand,sink);
   demo.seek(start);
   double step=score.nextForHand(start,hand);
   if(step>start){demo.advance(step);ok(step<=score.end(),"invalid hand seek");}
   // Pause and restart in the middle of the musical timeline.
   demo.stop();sink.silent();
   double middle=Math.max(start,score.end()*.48);
   demo.seek(middle);
   demo.advance(Math.min(score.end(),middle+1));
   demo.stop();sink.silent();
   // Return to an earlier note, loop A-B twice, cancel and restart whole score.
   double b=Math.min(score.end(),start+Math.min(8,score.end()-start));
   for(int rep=0;rep<2;rep++){demo.seek(start);demo.advance(b);demo.stop();sink.silent();}
   demo.seek(start);demo.advance(score.end());demo.stop();sink.silent();
   ok(sink.ons==sink.offs,"MIDI did not balance all notes in "+file+" hand "+hand);
   ok(score.events.size()==originalEvents,"Study changed source event list");
   KeyboardGuide.Frame end=guide.at(Math.max(0,score.end()-.01));
   ok(end.both.containsAll(end.right)&&end.both.containsAll(end.left),
    "guide simultaneous hands lost near end");
  }
  System.out.println("STUDY_JOURNEY_SCORE_PASS "+file+" events="+originalEvents);
 }
 public static void main(String[] args)throws Exception{
  run("ode-to-joy-ludwig-van-beethoven.mxl");
  run("gymnopedie-no-1-satie.mxl");
  System.out.println("STUDY_JOURNEY_PASS assertions="+checks);
 }
}

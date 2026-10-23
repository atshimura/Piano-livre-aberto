import br.org.pianolivre.*;
import java.util.*;

/** Actual musical attack/release boundaries, never arbitrary clock fractions. */
public final class StudyLoopPositionTest {
 static int checks=0;
 static void eq(double actual,double expected,String msg){checks++;if(Math.abs(actual-expected)>1e-6)throw new AssertionError(msg+" expected "+expected+" got "+actual);}
 public static void main(String[] args){
  Score s=new Score();
  s.events.add(new Score.Event(0,4,60,1,1,1)); // held melody
  s.events.add(new Score.Event(1,1,64,1,1,1)); // short simultaneous voice
  s.events.add(new Score.Event(1,2,48,2,1,1)); // bass chord member
  s.events.add(new Score.Event(5,1,67,1,1,2)); // next phrase after rest
  eq(StudyLoopPosition.start(s,1.7),1,"A must align to a written attack");
  eq(StudyLoopPosition.start(s,4.9),1,"A during rest returns prior attack");
  eq(StudyLoopPosition.start(s,-20),0,"A outside piece clamps to zero");
  eq(StudyLoopPosition.end(s,1.7),4,"B must not cut sustained upper note at 1.7");
  eq(StudyLoopPosition.end(s,2.3),4,"B protects held note after shorter voice releases");
  eq(StudyLoopPosition.end(s,4.5),4,"B in a rest follows last valid note-off");
  eq(StudyLoopPosition.end(s,5.2),6,"B in next phrase includes its release");
  eq(StudyLoopPosition.end(s,-10),4,"B at start includes held note");
  eq(StudyLoopPosition.end(s,100),6,"B beyond piece clamps to its last release");
  eq(StudyLoopPosition.start(null,3),0,"A without score safe");
  eq(StudyLoopPosition.end(null,3),0,"B without score safe");
  System.out.println("STUDY_LOOP_POSITION_PASS assertions="+checks);
 }
}

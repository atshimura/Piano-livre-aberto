import br.org.pianolivre.MusicLabPlaybackPlan;
import java.util.*;
public class MusicLabPlaybackPlanTest{
 static int checks=0;
 static void ok(boolean x){checks++;if(!x)throw new AssertionError("check "+checks);}
 public static void main(String[] args){
  List<MusicLabPlaybackPlan.Event> melody=MusicLabPlaybackPlan.build(new int[]{60,62,64},"Escala",100);
  ok(melody.size()==6);ok(melody.get(0).on&&melody.get(0).atMillis==0);
  ok(melody.get(1).atMillis==540&&!melody.get(1).on);
  ok(melody.get(2).atMillis==600&&melody.get(2).on);
  ok(MusicLabPlaybackPlan.endMillis(melody)==1740);
  List<MusicLabPlaybackPlan.Event> chord=MusicLabPlaybackPlan.build(new int[]{60,64,67},"Acorde",100);
  ok(chord.size()==6);ok(chord.stream().filter(e->e.on).allMatch(e->e.atMillis==0));
  ok(chord.stream().filter(e->!e.on).allMatch(e->e.atMillis==600));
  ok(MusicLabPlaybackPlan.endMillis(MusicLabPlaybackPlan.build(new int[]{60,62},"Escala",50))>
     MusicLabPlaybackPlan.endMillis(MusicLabPlaybackPlan.build(new int[]{60,62},"Escala",150)));
  try{MusicLabPlaybackPlan.build(new int[]{130},"Nota",100);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
  try{MusicLabPlaybackPlan.build(new int[]{60},"Nota",0);throw new AssertionError();}catch(IllegalArgumentException expected){checks++;}
  for(MusicLabPlaybackPlan.Event e:chord)ok(e.atMillis>=0);
  // A silent gap must still count as playing; pressing Stop cannot spawn a second run.
  MusicLabPlaybackPlan.Session session=new MusicLabPlaybackPlan.Session();
  int first=session.start();
  ok(session.isPlaying());ok(session.isCurrent(first));
  ok(session.apply(first,melody.get(0)));
  ok(session.heldNotes().contains(60));
  ok(session.apply(first,melody.get(1)));
  ok(session.heldNotes().isEmpty());ok(session.isPlaying());
  ok(session.cancel().isEmpty());ok(!session.isPlaying());
  ok(!session.apply(first,melody.get(2)));ok(!session.shouldRepeat(first,true));
  int second=session.start();ok(second!=first);
  ok(!session.apply(first,melody.get(0)));
  ok(session.apply(second,melody.get(0)));
  Set<Integer> heldBeforeStop=session.cancel();
  ok(heldBeforeStop.size()==1&&heldBeforeStop.contains(60));
  ok(session.heldNotes().isEmpty());ok(!session.apply(second,melody.get(2)));
  // Loop boundary is silent, yet active until explicitly cancelled or unchecked.
  int repeatToken=session.start();
  for(MusicLabPlaybackPlan.Event e:chord)ok(session.apply(repeatToken,e));
  ok(session.heldNotes().isEmpty());
  ok(session.shouldRepeat(repeatToken,true));
  ok(!session.shouldRepeat(repeatToken,false));
  ok(session.cancel().isEmpty());
  ok(!session.shouldRepeat(repeatToken,true));
  // A selection switch / disconnect cannot leak late events into the next run.
  int newChoice=session.start();
  ok(newChoice!=repeatToken);
  ok(!session.apply(repeatToken,melody.get(0)));
  ok(session.apply(newChoice,melody.get(0)));
  ok(session.cancel().contains(60));
  ok(!session.isPlaying());
  ok(!session.apply(newChoice,melody.get(2)));
  System.out.println("MUSIC_LAB_PLAYBACK_PLAN_PASS checks="+checks);
 }
}

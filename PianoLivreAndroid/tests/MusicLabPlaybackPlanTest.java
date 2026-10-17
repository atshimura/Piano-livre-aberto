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
  System.out.println("MUSIC_LAB_PLAYBACK_PLAN_PASS checks="+checks);
 }
}

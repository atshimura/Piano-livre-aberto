import br.org.pianolivre.MusicPatternTracker;
import br.org.pianolivre.MusicSequenceAnalyzer;
import java.util.*;
public class MusicPatternTrackerTest{
 private static int checks=0;
 static void ok(boolean value){checks++;if(!value)throw new AssertionError("Teste "+checks);}
 public static void main(String[] args){
  MusicPatternTracker tracker=new MusicPatternTracker();
  int[] major={60,62,64,65,67,69,71,72};
  for(int i=0;i<major.length-1;i++)ok(tracker.attack(major[i],true).isEmpty());
  List<MusicSequenceAnalyzer.Finding> r=tracker.attack(72,true);
  ok(r.stream().anyMatch(f->f.description.equals("maior")&&f.category.equals("Escala")));
  ok(tracker.size()==8);
  ok(tracker.attack(74,false).isEmpty());ok(tracker.size()==0);
  int[] arpeggio={60,64,67,72};
  for(int i=0;i<arpeggio.length-1;i++)tracker.attack(arpeggio[i],true);
  ok(tracker.attack(72,true).stream().anyMatch(f->f.category.equals("Arpejo")));
  tracker.clear();ok(tracker.size()==0);
  for(int n=0;n<30;n++)tracker.attack(60+n%5,true);
  ok(tracker.size()<=13);
  tracker.clear();
  for(int i=major.length-1;i>0;i--)tracker.attack(major[i],true);
  ok(tracker.attack(60,true).stream().anyMatch(f->f.direction.equals("descendente")));
  ok(tracker.attack(-1,true).isEmpty());ok(tracker.size()==0);
  tracker.attack(60,true);tracker.attack(62,true);
  ok(tracker.attack(64,false).isEmpty());ok(tracker.size()==0);
  System.out.println("MUSIC_PATTERN_TRACKER_PASS checks="+checks);
 }
}

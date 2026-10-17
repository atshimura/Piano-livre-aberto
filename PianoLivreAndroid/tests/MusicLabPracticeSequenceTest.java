import br.org.pianolivre.MusicLabPracticeSequence;
import java.util.*;
public class MusicLabPracticeSequenceTest{
 private static int c=0;
 private static void ok(boolean b){c++;if(!b)throw new AssertionError("check "+c);}
 public static void main(String[] args){
  MusicLabPracticeSequence p=new MusicLabPracticeSequence(new int[]{60,62,64,65});
  ok(p.correct()==0&&p.total()==4&&p.next()==60);
  ok(!p.attack(61));ok(p.extras()==1&&p.next()==60);
  ok(p.attack(60));ok(p.correct()==1&&p.next()==62);
  ok(!p.attack(64));ok(p.correct()==1&&p.extras()==2);
  ok(p.attack(62));ok(p.attack(64));ok(!p.complete());
  ok(p.attack(65));ok(p.complete()&&p.next()==-1);
  ok(!p.attack(65));ok(p.extras()==2);
  p.reset();ok(p.correct()==0&&p.extras()==0&&!p.complete());
  try{new MusicLabPracticeSequence(new int[]{130,60});throw new AssertionError();}catch(IllegalArgumentException e){c++;}
  // A distinct Note On is required even for repeated pitches.
  MusicLabPracticeSequence repeat=new MusicLabPracticeSequence(new int[]{60,60,62});
  Set<Integer> one=new HashSet<>(Collections.singleton(60)),empty=Collections.emptySet();
  ok(repeat.observe(Collections.singleton(60),one,empty,false)==MusicLabPracticeSequence.Observation.CORRECT);
  ok(repeat.correct()==1&&repeat.next()==60);
  ok(repeat.observe(empty,one,empty,false)==MusicLabPracticeSequence.Observation.IGNORED);
  ok(repeat.correct()==1); // Held key is not another attack.
  ok(repeat.observe(Collections.singleton(60),one,one,false)==MusicLabPracticeSequence.Observation.IGNORED);
  ok(repeat.correct()==1); // Same MIDI pitch from BOTH sources is not two notes.
  ok(repeat.observe(Collections.singleton(60),one,empty,true)==MusicLabPracticeSequence.Observation.IGNORED);
  ok(repeat.correct()==1); // Demonstration does not advance the student's work.
  ok(repeat.observe(Collections.singleton(60),one,empty,false)==MusicLabPracticeSequence.Observation.CORRECT);
  ok(repeat.correct()==2&&repeat.next()==62);
  ok(repeat.observe(Collections.singleton(65),Collections.singleton(65),empty,false)==MusicLabPracticeSequence.Observation.EXTRA);
  ok(repeat.extras()==1&&repeat.correct()==2);
  ok(repeat.observe(Arrays.asList(62,65),new HashSet<>(Arrays.asList(62,65)),empty,false)==MusicLabPracticeSequence.Observation.IGNORED);
  ok(repeat.correct()==2&&repeat.extras()==1); // Simultaneous attacks do not imply an order.
  ok(repeat.observe(Collections.singleton(62),Collections.singleton(62),Collections.singleton(65),false)==MusicLabPracticeSequence.Observation.IGNORED);
  ok(repeat.correct()==2); // Legato/overlap is indeterminate, not mis-scored.
  ok(repeat.observe(Collections.singleton(62),Collections.singleton(62),empty,false)==MusicLabPracticeSequence.Observation.CORRECT);
  ok(repeat.complete()&&repeat.next()==-1);
  ok(repeat.observe(Collections.singleton(65),Collections.singleton(65),empty,false)==MusicLabPracticeSequence.Observation.IGNORED);
  ok(repeat.extras()==1);
  repeat.reset();ok(!repeat.complete()&&repeat.next()==60&&repeat.extras()==0);
  ok(repeat.observe(Collections.singleton(60),empty,Collections.singleton(60),false)==MusicLabPracticeSequence.Observation.CORRECT);
  ok(repeat.correct()==1); // Virtual Tocar, independent of MIDI source.
  repeat.reset();
  ok(repeat.observe(Collections.singleton(62),Collections.singleton(62),empty,false)==MusicLabPracticeSequence.Observation.EXTRA);
  ok(repeat.next()==60&&repeat.extras()==1);
  ok(repeat.observe(Collections.singleton(60),Collections.singleton(60),empty,false)==MusicLabPracticeSequence.Observation.CORRECT);
  ok(repeat.next()==60&&repeat.extras()==1); // Wrong note never blocks correct retry.
  try{repeat.observe(null,one,empty,false);throw new AssertionError();}catch(IllegalArgumentException ex){c++;}
  try{repeat.observe(one,null,empty,false);throw new AssertionError();}catch(IllegalArgumentException ex){c++;}
  try{new MusicLabPracticeSequence(new int[]{});throw new AssertionError();}catch(IllegalArgumentException ex){c++;}
  System.out.println("MUSIC_LAB_PRACTICE_SEQUENCE_PASS checks="+c);
 }
}

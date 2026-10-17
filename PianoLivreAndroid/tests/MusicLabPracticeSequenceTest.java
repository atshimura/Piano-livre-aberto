import br.org.pianolivre.MusicLabPracticeSequence;
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
  System.out.println("MUSIC_LAB_PRACTICE_SEQUENCE_PASS checks="+c);
 }
}

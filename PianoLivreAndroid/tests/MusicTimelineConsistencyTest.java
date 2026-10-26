import br.org.pianolivre.*;
import java.io.*;
import java.util.*;

/**
 * Three independent consumers of Score must agree on sounding notes:
 * - KeyboardGuide (visual, always both hands)
 * - MidiPlayback (actual Note On/Off for all/right/left)
 * - reference interval scan of Score.events.
 * No Yamaha or UI required; these checks validate internal musical state.
 */
public final class MusicTimelineConsistencyTest {
 static int assertions=0,checkpoints=0;
 static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
 static Set<Integer> expected(Score score,double beat,int hand){
  Set<Integer> out=new HashSet<>();
  for(Score.Event e:score.events){
   if(e.length<=0||e.midi<0||e.midi>127)continue;
   if(hand==1&&e.staff!=1)continue;
   if(hand==2&&e.staff==1)continue;
   if(e.beat<=beat+1e-8&&e.beat+e.length>beat+1e-8)out.add(e.midi);
  }
  return out;
 }
 static void audit(Score score,String name){
  KeyboardGuide keyboard=new KeyboardGuide(score);
  TreeSet<Double> beats=new TreeSet<>();
  beats.add(0.0);
  beats.add(score.end());
  for(Score.Event e:score.events){
   beats.add(e.beat);
   beats.add(e.beat+e.length);
   if(e.length>0.02){
    beats.add(e.beat+e.length*.5);
    beats.add(e.beat+Math.min(e.length*.25,.02));
   }
  }
  for(int hand=0;hand<3;hand++){
   Set<Integer> output=new HashSet<>();
   MidiPlayback playback=new MidiPlayback(score,hand,(pitch,on,velocity)->{
    if(on)output.add(pitch);else output.remove(pitch);
   });
   for(double time:beats){
    Set<Integer> want=expected(score,time,hand);
    playback.seek(time);
    check(output.equals(want),name+": demo MIDI differs at "+time+" hand="+hand+" want="+want+" got="+output);
    KeyboardGuide.Frame frame=keyboard.at(time);
    Set<Integer> guide=hand==0?frame.both:(hand==1?frame.right:frame.left);
    check(guide.equals(want),name+": visual keyboard differs at "+time+" hand="+hand);
    if(hand==0){
     Set<Integer> union=new HashSet<>(frame.right);union.addAll(frame.left);
     check(frame.both.equals(union),name+": both not union of right+left at "+time);
     KeyboardGuide.Range r=KeyboardGuide.range(frame.both,Collections.emptySet());
     for(int note:frame.both)if(note>=21&&note<=108)
      check(note>=r.low&&note<=r.high,name+": sounding note not in guide keyboard "+note);
    }
    checkpoints++;
   }
  }
  System.out.println("PASS "+name+" "+beats.size()+" timeline checkpoints per hand");
 }
 public static void main(String[] args)throws Exception{
  File dir=new File("app/src/main/assets/scores");
  File[] mxls=dir.listFiles((d,n)->n.endsWith(".mxl"));
  check(mxls!=null&&mxls.length==10,"missing 10 internal scores");
  Arrays.sort(mxls,Comparator.comparing(File::getName));
  for(File mxl:mxls){
   Score score;
   try(InputStream in=new FileInputStream(mxl)){score=Score.read(in,mxl.getName());}
   audit(score,mxl.getName());
  }
  System.out.println("PASS MusicTimelineConsistencyTest "+assertions+" assertions and "+checkpoints+
    " beat/hand samples; score MIDI output and BOTH-hand keyboard agree.");
 }
}

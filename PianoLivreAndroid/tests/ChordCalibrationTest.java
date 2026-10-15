import br.org.pianolivre.*;
import java.util.*;

public final class ChordCalibrationTest {
 static int checks=0;
 static void ok(boolean yes,String message){checks++;if(!yes)throw new AssertionError(message);}
 static void sample(ChordCalibration c,long begin,long spread){
  c.noteOn(60,begin);c.noteOn(64,begin+spread);
  c.noteOff(60,begin+spread+25);
  ok(c.noteOff(64,begin+spread+26),"complete valid trial not stored");
 }
 public static void main(String[] args){
  ChordCalibration c=new ChordCalibration();
  ok(c.observedWindowMs()==null,"should have no guessed window");
  c.start();
  c.noteOn(60,10);c.noteOff(60,25);
  ok(c.sampleCount()==0,"single note cannot calibrate chord");
  sample(c,100,23);sample(c,200,61);sample(c,400,37);
  ok(c.sampleCount()==3,"count");
  ok(c.observedWindowMs()!=null&&c.observedWindowMs()==61L,
   "window must be maximum of observed attacks, not arbitrary default");
  c.noteOn(60,500);c.noteOn(64,515);
  ok(!c.noteOff(60,516),"must not record before all keys released");
  ok(c.sampleCount()==3,"premature sample");
  ok(c.noteOff(64,520),"finished chord not recorded");
  ok(c.observedWindowMs()==61,"older maximum remains");
  c.stop();c.noteOn(60,700);c.noteOn(64,750);c.noteOff(60,760);c.noteOff(64,770);
  ok(c.sampleCount()==4,"inactive should not record");
  c.start();ok(c.observedWindowMs()==null&&c.sampleCount()==0,"recalibration should reset samples");
  for(int i=0;i<32;i++)sample(c,1000+i*200,1+i);
  for(int i=32;i<40;i++){
   long time=1000+i*200;
   c.noteOn(60,time);c.noteOn(64,time+1+i);
   c.noteOff(60,time+30+i);
   ok(!c.noteOff(64,time+32+i),"samples beyond bounded capacity must be ignored");
  }
  ok(c.sampleCount()==32,"bounded samples");
  ok(c.observedWindowMs()==32,"max derived from stored 32");
  System.out.println("PASS ChordCalibrationTest "+checks+" assertions: no default, repeated measured chords, max and limits");
 }
}

import br.org.pianolivre.*;
import java.util.*;

/** Duration and sustain observation without arbitrary pedagogical grading. */
public final class MidiPerformanceTest {
 static int checks=0;
 static void ok(boolean b,String reason){checks++;if(!b)throw new AssertionError(reason);}
 public static void main(String[] args){
  for(int cycle=0;cycle<20;cycle++)for(int midi=21;midi<=108;midi++){
   MidiPerformance m=new MidiPerformance();
   m.noteOn(0,midi,90,1000,2.0);
   ok(m.physicalKeysDown()==1,"attack lost");
   m.noteOff(0,midi,1300);
   ok(m.physicalKeysDown()==0,"release lost");
   ok(m.completed().size()==1,"completed span missing");
   MidiPerformance.Span s=m.completed().get(0);
   ok(s.physicalDurationMs()==300&&s.soundingDurationMs()==300&&!s.pedalExtended,"duration incorrect");
  }
  MidiPerformance pedal=new MidiPerformance();
  pedal.control64(0,127,1000);
  pedal.noteOn(0,48,80,1010,0);
  pedal.noteOff(0,48,1200);
  ok(pedal.physicalKeysDown()==0&&pedal.soundingVoices()==1,"pedal release should sustain");
  pedal.control64(1,127,1250);
  pedal.control64(0,0,1400);
  ok(pedal.soundingVoices()==0,"pedal up must terminate sustained notes");
  MidiPerformance.Span span=pedal.completed().get(0);
  ok(span.physicalDurationMs()==190&&span.soundingDurationMs()==390&&span.pedalExtended,
   "pedal needs distinct physical and sounding durations");
  ok(pedal.pedalDown(1)&&!pedal.pedalDown(0),
   "releasing channel 0 must leave independent channel 1 sustain active");
  pedal.control64(1,0,1420);
  ok(!pedal.pedalDown(1),"channel 1 sustain must release independently");
  MidiPerformance multiPedal=new MidiPerformance();
  multiPedal.control64(0,127,10);
  multiPedal.control64(1,127,20);
  multiPedal.control64(0,0,30);
  ok(!multiPedal.pedalDown(0)&&multiPedal.pedalDown(1),
   "one channel CC64 release must not clear the other channel state");
  multiPedal.disconnect();
  ok(!multiPedal.pedalDown(0)&&!multiPedal.pedalDown(1),
   "USB disconnect must reset pedal state on every channel");
  MidiPerformance channels=new MidiPerformance();
  channels.noteOn(0,60,80,10,0);
  channels.noteOn(1,60,80,20,0);
  channels.noteOff(0,60,30);
  ok(channels.physicalKeysDown()==1,"channel independence broken");
  channels.noteOff(1,60,40);
  ok(channels.physicalKeysDown()==0&&channels.completed().size()==2,"second channel lost");
  MidiPerformance repeats=new MidiPerformance();
  repeats.noteOn(0,60,80,1,0);repeats.noteOn(0,60,80,30,0.5);
  ok(repeats.completed().size()==1,"reattack must close earlier note");
  repeats.noteOff(0,60,50);repeats.disconnect();
  ok(repeats.soundingVoices()==0&&repeats.completed().size()==2,"disconnect must clean live notes without invented completion");
  MidiPerformance overflow=new MidiPerformance();
  for(int i=0;i<1200;i++){overflow.noteOn(0,60,80,i*5,0);overflow.noteOff(0,60,i*5+1);}
  ok(overflow.completed().size()==1024,"history must remain bounded");
  System.out.println("PASS MidiPerformanceTest "+checks+" assertions on 88 keys, 20 repetitions, durations, CC64 pedal, channels and memory limits");
 }
}

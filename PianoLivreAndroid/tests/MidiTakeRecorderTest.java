import br.org.pianolivre.MidiTakeRecorder;
import java.util.*;

public final class MidiTakeRecorderTest {
 private static int checks;
 private static void ok(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 public static void main(String[] args){
  MidiTakeRecorder r=new MidiTakeRecorder();r.start(7,1_000_000_000L);
  ok(r.accept(0x90,60,90,1_100_000_000L,7),"note on accepted");
  ok(r.accept(0x91,60,70,1_100_000_000L,7),"same pitch on second channel accepted");
  ok(!r.accept(0x80,60,0,1_150_000_000L,6),"stale epoch ignored");
  ok(r.accept(0xB0,64,127,1_200_000_000L,7),"sustain captured");
  ok(r.accept(0x90,60,0,1_300_000_000L,7),"velocity zero captured");
  ok(r.accept(0x90,60,100,1_300_000_000L,7),"rearticulation accepted");
  r.pause(1_400_000_000L);ok(!r.accept(0x90,62,80,1_500_000_000L,7),"pause blocks capture");
  r.resume(3_400_000_000L,7);
  ok(r.accept(0x80,60,0,3_500_000_000L,7),"post-pause event captured");
  r.stop(3_600_000_000L);
  List<MidiTakeRecorder.Event> e=r.events();
  ok(e.size()==9,"recorded messages plus pause safety events");
  ok(e.get(0).timeNs==1_100_000_000L&&e.get(1).timeNs==1_100_000_000L,"simultaneous events retained");
  ok(e.get(2).command()==0xb0&&e.get(2).data1==64,"CC64 retained");
  ok(e.get(3).command()==0x90&&e.get(3).data2==0,"velocity zero retained");
  ok(e.get(4).data2==100,"reattack velocity retained");
  boolean channelOneOff=false,pedalOff=false,rawPostPause=false;
  for(MidiTakeRecorder.Event x:e){channelOneOff|=x.generated&&x.command()==0x80&&x.channel()==1;pedalOff|=x.generated&&x.command()==0xb0&&x.data1==64&&x.data2==0;rawPostPause|=!x.generated&&x.timeNs==3_500_000_000L;}
  ok(channelOneOff,"held channel safely closed at pause");ok(pedalOff,"pedal safely released at pause");
  ok(rawPostPause,"raw monotonic time retained across pause");
  ok(r.state()==MidiTakeRecorder.State.REVIEW,"stopped take enters review");
  ok(r.durationNs()==600_000_000L,"duration excludes paused time");
  MidiTakeRecorder lost=new MidiTakeRecorder();lost.start(2,10);lost.accept(0x90,48,50,20,2);lost.interrupt(30,"disconnect");
  ok(lost.state()==MidiTakeRecorder.State.INTERRUPTED&&lost.eventCount()==2,"disconnect preserves captured event and adds safety note-off");
  ok(!lost.events().get(0).generated&&lost.events().get(1).generated&&lost.events().get(1).timeNs==30,"disconnect closure uses monotonic loss time without changing source event");
  ok(!lost.accept(0x90,49,50,30,2),"interrupted epoch rejects later bytes");
  MidiTakeRecorder pausedLost=new MidiTakeRecorder();pausedLost.start(3,100);pausedLost.accept(0x90,50,60,200,3);pausedLost.pause(300);pausedLost.interrupt(900,"disconnect while paused");
  ok(pausedLost.durationNs()==200,"disconnect during pause excludes paused wall time");
  System.out.println("MIDI_TAKE_RECORDER_PASS checks="+checks);
 }
}

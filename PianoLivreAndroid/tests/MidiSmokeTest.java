import br.org.pianolivre.*;
import java.util.*;
public class MidiSmokeTest {
 static void ok(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static Score sample(){
  Score s=new Score();
  s.events.add(new Score.Event(0,1,60,1,1,1));
  s.events.add(new Score.Event(0,2,60,2,1,1)); // Both hands hold same pitch: no early note-off.
  s.events.add(new Score.Event(.25,.15,64,1,1,1)); // Short note between 25ms ticks.
  s.events.add(new Score.Event(2,.5,67,1,1,1));
  return s;
 }
 public static void main(String[] args){
  MidiProtocol protocol=new MidiProtocol();
  List<String> parsed=new ArrayList<>();
  MidiProtocol.Listener listen=(st,a,b)->parsed.add(st+":"+a+":"+b);
  protocol.feed(new byte[]{(byte)0x91,60},0,2,listen);
  ok(parsed.isEmpty(),"Fragmented MIDI packet triggered incomplete event");
  protocol.feed(new byte[]{110,(byte)0xF8,64,95,67,80,(byte)0x81,60,64},0,9,listen);
  ok(parsed.size()==4,"Running-status and realtime packets: "+parsed);
  ok(parsed.get(0).equals("145:60:110"),"Wrong note-on");
  ok(parsed.get(1).equals("145:64:95"),"Wrong running status");
  ok(parsed.get(2).equals("145:67:80"),"Wrong second running status");
  ok(parsed.get(3).equals("129:60:64"),"Wrong note-off");
  protocol.feed(new byte[]{(byte)0xB0,64,127,(byte)0x90,60,0},0,6,listen);
  ok(parsed.get(4).equals("176:64:127"),"Sustain pedal MIDI missed");
  ok(parsed.get(5).equals("144:60:0"),"Velocity-zero note-off missed");
  protocol.feed(new byte[]{(byte)0xF0,0x12,0x34,(byte)0xF7,(byte)0x90,72,90},0,7,listen);
  ok(parsed.get(6).equals("144:72:90"),"SysEx ignored channel message");
  System.out.println("PASS MIDI byte parsing and running status");

  List<String> tx=new ArrayList<>();
  MidiPlayback player=new MidiPlayback(sample(),(pitch,on,velocity)->tx.add((on?"+":"-")+pitch));
  player.seek(0);
  ok(tx.equals(Arrays.asList("+60")),"Initial notes must play once: "+tx);
  player.advance(.5);
  ok(tx.equals(Arrays.asList("+60","+64","-64")),"Short notes lost: "+tx);
  player.advance(1.0);
  ok(tx.equals(Arrays.asList("+60","+64","-64")),"Overlapping pitch turned off early: "+tx);
  player.advance(2.1);
  ok(tx.contains("-60")&&tx.contains("+67"),"Missing transition: "+tx);
  player.stop();
  ok(tx.get(tx.size()-1).equals("-67"),"All-notes-off not sent");
  int beforeSeek=tx.size();
  player.seek(2.2);
  ok(tx.subList(beforeSeek,tx.size()).equals(Arrays.asList("+67")),
   "Seek must never send ghost Note-On for any skipped passage: "+tx.subList(beforeSeek,tx.size()));
  player.stop();
  beforeSeek=tx.size();
  player.seek(2.8);
  ok(tx.size()==beforeSeek,"Seek after final release must not replay any historical note");
  System.out.println("PASS MIDI demo note-on/off, overlap, seek, short events and stop");
 }
}

import br.org.pianolivre.*;
import java.io.*;
import java.util.*;

public final class MusicXmlDynamicsTest {
 public static void main(String[] args)throws Exception{
  Score s;try(InputStream in=new FileInputStream("tests/fixtures/motor/explicit-dynamics.xml")){s=Score.read(in,"fixture.xml");}
  List<String> attacks=new ArrayList<>();
  MidiPlayback p=new MidiPlayback(s,(pitch,on,velocity)->{if(on){if(velocity<=0)throw new AssertionError("Note On zero is a MIDI off");attacks.add(pitch+":"+velocity);}});
  p.seek(0);p.advance(4);p.stop();
  if(!attacks.equals(Arrays.asList("55:45","60:45","57:90","62:113","65:127")))throw new AssertionError(attacks.toString());
  if(s.at(2,1).size()!=1||s.at(2,1).get(0).midi!=64)throw new AssertionError("Silent demonstration note must stay visible/playable in Wait");
  KeyboardGuide.Frame frame=new KeyboardGuide(s).at(2.5);
  if(!frame.right.contains(64)||!frame.left.contains(57))throw new AssertionError("Both visual hands retained");
  System.out.println("PASS MusicXmlDynamicsTest: explicit numeric velocities, chronological scope, zero silence, MIDI and both-hand guide/Wait");
 }
}

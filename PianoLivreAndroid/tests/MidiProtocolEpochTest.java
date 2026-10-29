import br.org.pianolivre.MidiProtocol;
import java.util.*;

public final class MidiProtocolEpochTest {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 public static void main(String[] args){
  for(int channel=0;channel<16;channel++)for(int pitch=21;pitch<=108;pitch++){
   MidiProtocol decoder=new MidiProtocol();List<String> received=new ArrayList<>();
   MidiProtocol.EpochListener listen=(status,a,b,epoch)->received.add(status+":"+a+":"+b+":"+epoch);
   decoder.feed(new byte[]{(byte)(0x90|channel),(byte)pitch},0,2,1,listen);
   decoder.feed(new byte[]{(byte)0xF8,80},0,2,3,listen);
   check(received.equals(Arrays.asList((0x90|channel)+":"+pitch+":80:1")),"fragment keeps first-data epoch across background");
   decoder.feed(new byte[]{(byte)pitch,90},0,2,3,listen);
   check(received.get(1).equals((0x90|channel)+":"+pitch+":90:3"),"new running-status attack has current epoch");
   decoder.feed(new byte[]{(byte)(0x80|channel),(byte)pitch,0},0,3,3,listen);
   check(received.get(2).equals((0x80|channel)+":"+pitch+":0:3"),"fresh note off retained");
  }
  MidiProtocol decoder=new MidiProtocol();List<Long> epochs=new ArrayList<>();
  MidiProtocol.EpochListener listen=(s,a,b,e)->epochs.add(e);
  decoder.feed(new byte[]{(byte)0x90},0,1,1,listen);
  decoder.feed(new byte[]{60,80},0,2,2,listen);
  check(epochs.equals(Arrays.asList(2L)),"status byte alone does not fabricate a pre-pause attack");
  decoder.feed(new byte[]{(byte)0xF0,1,2},0,3,2,listen);
  decoder.feed(new byte[]{3,(byte)0xF7,(byte)0xC0,4},0,4,3,listen);
  check(epochs.equals(Arrays.asList(2L,3L)),"sysex and one-byte channel message stay framed");
  System.out.println("MIDI_PROTOCOL_EPOCH_PASS checks="+checks);
 }
}

package br.org.pianolivre;

import android.media.midi.MidiReceiver;
import android.util.Log;
import java.util.*;

/** Debug-only emulator integration: real Activity/Handler/decoder, no USB claims. */
final class MidiSessionSelfTest {
 private static int checks;
 private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 private static void notes(MainActivity a,List<Score.Event> events){
  for(Score.Event e:events)a.onMidiMessage(0x90,e.midi,80);
 }
 static void run(MainActivity a){
  try{
   final String previousDescription=a.connectionDescription;
   check(a.device==null&&a.inputPort==null&&a.outputPort==null&&a.activeInfo==null&&a.openingMidiId<0,"self-test requires no MIDI device");
   a.loadScore("scores/ode-to-joy-ludwig-van-beethoven.mxl",true);a.mode=0;a.currentHand=0;
   a.position=a.score.firstOnset(0);a.screens.showStudy();a.rearmWaitIfStudying();
   List<Score.Event> expected=a.score.at(a.position,0);
   check(!expected.isEmpty()&&!a.waitRecognizer.expected().isEmpty(),"initial Wait armed");
   a.disconnectMidi();check(a.waitRecognizer.expected().isEmpty(),"disconnect clears prior attacks");
   a.rearmWaitIfStudying();check(!a.waitRecognizer.expected().isEmpty(),"reconnect re-arms current written onset");
   double start=a.position;
   a.screens.showHome();a.disconnectMidi();a.waitRecognizer.arm(expected);notes(a,expected);
   check(a.position==start&&!a.waitTraveling,"library MIDI does not advance hidden study");
   a.screens.showStudy();a.disconnectMidi();a.waitRecognizer.arm(expected);a.foreground=false;notes(a,expected);
   check(a.position==start&&!a.waitTraveling,"background MIDI does not advance study");
   a.foreground=true;a.disconnectMidi();a.rearmWaitIfStudying();notes(a,expected);
   check(a.waitTraveling||a.position!=start,"fresh foreground attacks advance after reconnect");
   a.stopPlayback();a.screens.showHome();a.disconnectMidi();
   int before=a.receivedMessages;
   MidiReceiver old=a.createReceiver(a.midiGeneration);old.send(new byte[]{(byte)0x90,30,80},0,3);
   a.disconnectMidi();
   a.handler.post(()->{
    try{
     check(a.receivedMessages==before&&!a.downKeys.contains(30),"queued old-session MIDI discarded");
     MidiReceiver current=a.createReceiver(a.midiGeneration);current.send(new byte[]{(byte)0x90,30,80},0,3);
     a.handler.post(()->{
      check(a.receivedMessages==before+1&&a.downKeys.contains(30),"current-session decoder reaches Activity");
      a.disconnectMidi();a.position=start;a.stopPlayback();a.screens.showHome();a.setMidiStatus(previousDescription);
      Log.i("PianoLivreMIDI","MIDI_SESSION_SELFTEST_PASS assertions="+checks);
     });
    }catch(Exception ex){throw new IllegalStateException(ex);}
   });
  }catch(Exception ex){throw new IllegalStateException(ex);}
 }
}

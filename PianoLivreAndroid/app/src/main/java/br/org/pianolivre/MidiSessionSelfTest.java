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
   check(!a.waitStarted&&a.waitRecognizer.expected().isEmpty(),"Espera não arma antes de Iniciar");
   // Simulate the completion of the visual preparation without waiting five real seconds.
   a.waitStarted=true;a.rearmWaitIfStudying();
   List<Score.Event> expected=a.score.at(a.position,0);
   check(!expected.isEmpty()&&!a.waitRecognizer.expected().isEmpty(),"initial Wait armed");
   a.disconnectMidi();check(a.waitRecognizer.expected().isEmpty(),"disconnect clears prior attacks");
   a.rearmWaitIfStudying();check(!a.waitRecognizer.expected().isEmpty(),"reconnect re-arms current written onset");
   double start=a.position;
   a.screens.showHome();a.disconnectMidi();a.waitRecognizer.arm(expected);notes(a,expected);
   check(a.position==start&&!a.waitTraveling,"library MIDI does not advance hidden study");
   a.screens.showStudy();a.disconnectMidi();a.waitRecognizer.arm(expected);a.foreground=false;notes(a,expected);
   check(a.position==start&&!a.waitTraveling,"background MIDI does not advance study");
   a.foreground=true;a.disconnectMidi();a.waitStarted=true;a.rearmWaitIfStudying();notes(a,expected);
   check(a.waitTraveling||a.position!=start,"fresh foreground attacks advance after reconnect");
   a.stopPlayback();a.screens.showHome();a.disconnectMidi();
   a.onMidiMessage(0x90,60,80);a.onMidiMessage(0x91,60,90);a.onMidiMessage(0xB0,64,127);
   a.onMidiMessage(0xB0,123,0);
   check(a.downKeys.contains(60)&&a.performance.physicalKeysDown()==1,"CC123 isolates channel, same pitch still held elsewhere");
   a.onMidiMessage(0xB1,120,0);
   check(!a.downKeys.contains(60)&&a.performance.soundingVoices()==1&&a.sustainDown,"CC120 clears pressed keys without discarding other-channel sustain");
   a.onMidiMessage(0xB0,121,0);
   check(a.performance.soundingVoices()==0&&!a.sustainDown,"CC121 releases retained pedal voices");
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
      if(a.getIntent().getBooleanExtra("history_selftest",false))HistorySelfTest.activity(a);
     });
    }catch(Exception ex){throw new IllegalStateException(ex);}
   });
  }catch(Exception ex){throw new IllegalStateException(ex);}
 }
}

package br.org.pianolivre;

import android.view.View;
import android.util.Log;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Debug-gated synthetic backend; the emulator captures each real UI before ACK. */
final class MidiInterfaceSelfTest {
 private final MainActivity a;private final File ack;private int checks;
 private MidiConnection.Completion pending;private FakePorts first,current,oldOutput;
 private String source;private double disconnectedBeat;
 private static final String LONG_NAME="SIMULADO · Instrumento MIDI com nome muito longo para validar acessibilidade e diagnóstico";
 private static final String[] STAGES={"home-disconnected","home-opening","home-connected","study-connected","study-hidden","study-restored","study-disconnected","study-unnamed","study-switched","home-reconnected","home-background-return"};
 static final class FakePorts implements MidiConnection.Ports {
  final boolean rx,tx;int closed;final List<String> sent=new ArrayList<>();
  FakePorts(boolean rx,boolean tx){this.rx=rx;this.tx=tx;}
  public boolean canReceive(){return rx;}public boolean canSend(){return tx;}
  public void send(byte[] bytes){sent.add((bytes[0]&255)+":"+(bytes[1]&127)+":"+(bytes[2]&127));}
  public void close(){closed++;}
 }
 private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError("Prompt 3: "+message);}
 private MidiInterfaceSelfTest(MainActivity a){this.a=a;ack=new File(a.getCacheDir(),"prompt03-next");ack.delete();}
 static void run(MainActivity a){
  if((a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
  if(a.midiConnection.snapshot().connected()||a.midiManager!=null&&a.midiManager.getDevices().length>0)throw new IllegalStateException("Synthetic MIDI test requires no physical devices");
  new MidiInterfaceSelfTest(a).stage(0);
 }
 private void connect(int id,String name,boolean rx,boolean tx){
  a.midiConnection.connect(new MidiConnection.Target(id,name,true),(target,generation,completion)->pending=completion);
  current=new FakePorts(rx,tx);pending.complete(current);
 }
 private void stage(int index){
  if(index==STAGES.length){
   a.disconnectMidi();a.screens.showHome();ack.delete();
   Log.i("PianoLivreMIDI","MIDI_INTERFACE_SELFTEST_PASS checks="+checks+" simulated=true");return;
  }
  switch(index){
   case 0:a.disconnectMidi();a.screens.showHome();break;
   case 1:a.midiConnection.connect(new MidiConnection.Target(100,LONG_NAME,true),(target,generation,completion)->pending=completion);break;
   case 2:current=new FakePorts(true,true);first=current;pending.complete(current);break;
   case 3:
    a.loadScore("scores/ode-to-joy-ludwig-van-beethoven.mxl",true);
    a.screens.modeButtons[1].performClick();a.selectHand(2);a.tempoBar.setProgress(0);a.loopStart=0;a.loopEnd=8;a.position=0;
    a.screens.followScore(0,false);
    source=a.score.sourceXml;a.screens.updateSelections();a.updateInfo();break;
   case 4:a.toggle();check(a.running&&!a.screens.chrome.visible(),"start hides bars");break;
   case 5:check(a.running&&a.screens.chrome.visible(),"real score tap restores without pausing");break;
   case 6:
    disconnectedBeat=a.position;a.onMidiMessage(0x90,60,90);a.onMidiMessage(0xB0,64,127);
    a.midiConnection.disconnect("Instrumento simulado desconectado · sessão pausada");
    check(!a.running&&!a.waitStarted&&a.position==disconnectedBeat,"disconnect pauses without rewind");
    check(first.closed==1&&a.downKeys.isEmpty()&&!a.sustainDown&&a.performance.soundingVoices()==0,"disconnect clears held notes/pedal and closes resources");
    check(a.screens.chrome.visible(),"disconnect restores controls");break;
   case 7:connect(101,"",true,false);check(a.midiConnection.snapshot().connected(),"unnamed input-only connection is real state");break;
   case 8:
    connect(102,"SIMULADO · Saída de teste",false,true);oldOutput=current;a.testMidiOutput();
    check(oldOutput.sent.contains("144:60:88"),"test note sent to old output");
    connect(103,LONG_NAME,true,true);
    check(oldOutput.closed==1&&oldOutput.sent.contains("128:60:0"),"switch releases test note on old output");break;
   case 9:
    check(!current.sent.contains("128:60:0"),"delayed test Note Off cannot reach new connection");
    a.screens.showHome();break;
   case 10:
    a.onPause();a.onResume();
    check(!a.running&&a.position==disconnectedBeat,"background return preserves paused musical position");
    // Reconcile correctly notices that this synthetic target isn't in Android's inventory.
    check(!a.midiConnection.snapshot().connected(),"foreground verifies actual device inventory");
    connect(104,LONG_NAME,true,true);break;
  }
  if(index>=3&&index<=8){
   check(a.mode==1&&a.currentHand==2&&a.tempoFactor==.25&&a.loopStart==0&&a.loopEnd==8,"UI/connection transitions preserve study settings");
   check(source.equals(a.score.sourceXml),"source MusicXML unchanged");
  }
  // Layout frames, not a pedagogical hide timer. A slow render is polled explicitly.
  a.root.postOnAnimation(()->a.root.postOnAnimation(()->ready(index,0)));
 }
 private void ready(int index,int attempt){
  if(index>=3&&index<=8&&a.screens.notation.getVisibility()!=View.VISIBLE){
   if(attempt>=200)throw new AssertionError("Study rendering did not complete");
   a.handler.postDelayed(()->ready(index,attempt+1),100);return;
  }
  try{
   MidiConnection.Snapshot state=a.midiConnection.snapshot();
   String expected=HomeMidiIndicator.label(state.connected(),state.target==null?null:state.target.name);
   check(a.screens.homeMidiIndicator.getText().toString().equals(expected)&&a.screens.studyMidiIndicator.getText().toString().equals(expected),"both headers use identical connection state");
   check(a.screens.homeMidiIndicator.isClickable()&&a.screens.studyMidiIndicator.isClickable(),"connection management always accessible");
   if(state.connected()&&!state.target.name.isEmpty())check(a.screens.studyMidiIndicator.getContentDescription().toString().contains(state.target.name),"full long name accessible");
   JSONObject report=new JSONObject();report.put("stage",STAGES[index]);report.put("simulated",true);report.put("phase",state.phase.name());report.put("label",expected);
   report.put("generation",state.generation);report.put("rx",state.canReceive);report.put("tx",state.canSend);report.put("beat",a.position);
   report.put("running",a.running);report.put("controlsVisible",a.screens.chrome.visible());report.put("checks",checks);
   Log.i("PianoLivreMIDI","PROMPT03_STAGE_READY "+report);
   awaitAck(index);
  }catch(Exception error){throw new IllegalStateException(error);}
 }
 private void awaitAck(int index){
  try{
   if(ack.isFile()&&new String(Files.readAllBytes(ack.toPath()),StandardCharsets.UTF_8).trim().equals(STAGES[index])){
    ack.delete();
    // Let the old output-test deadline pass before verifying isolation.
    if(index==8)a.handler.postDelayed(()->stage(index+1),650);else stage(index+1);
    return;
   }
  }catch(Exception error){throw new IllegalStateException(error);}
  a.handler.postDelayed(()->awaitAck(index),100);
 }
}

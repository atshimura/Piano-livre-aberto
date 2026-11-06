package br.org.pianolivre;
import android.app.AlertDialog;
import android.media.midi.MidiReceiver;
import android.os.SystemClock;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashSet;

/** Bloco C da refatoração do MainActivity: recepção e interpretação das
 * mensagens MIDI (createReceiver, onMidiMessage, onKey), envio ao
 * instrumento (send, allOff), pedal sustain e diagnóstico USB-MIDI
 * (midiDiagnosticReport, showMidiDiagnostics, testMidiOutput). O estado
 * (midiConnection, downKeys, midiSources, activeNotes, performance,
 * contadores, midiPacketSequence) permanece no MainActivity, acessado
 * package-private; os autotestes continuam chamando a.createReceiver(...),
 * a.onMidiMessage(...) e a.testMidiOutput(). Comportamento idêntico à
 * versão inline no MainActivity. */
final class ZMidiInput{
 private final MainActivity a;
 ZMidiInput(MainActivity activity){a=activity;}
 MidiReceiver createReceiver(final long generation){
  final int sourceId=a.midiConnection.snapshot().target==null?-1:a.midiConnection.snapshot().target.id;
  final MidiProtocol decoder=new MidiProtocol();
  return new MidiReceiver(){
   @Override public synchronized void onSend(byte[] data,int offset,int count,long timestamp){
    if(generation!=a.midiGeneration)return;
    final long arrivalTimeNs=SystemClock.elapsedRealtimeNanos();
    final long inputEpoch=a.midiInputEpoch;
    final boolean arrivedInForeground=a.foreground;
    final ArrayList<int[]> decoded=new ArrayList<>();
    // Drain background bytes to retain stream framing, but never deliver them.
    decoder.feed(data,offset,count,inputEpoch,(st,d1,d2,startedEpoch)->{
     if(startedEpoch==inputEpoch)decoded.add(new int[]{st,d1,d2});
    });
    if(!arrivedInForeground||!a.foreground||decoded.isEmpty())return;
    final long packet=a.midiPacketSequence.incrementAndGet();
    final long arrivalTime=SystemClock.uptimeMillis();
    final long uiSession=a.screens==null?-1:a.screens.discoveryGeneration();
    if(generation!=a.midiGeneration||inputEpoch!=a.midiInputEpoch||!a.foreground)return;
    if(a.screens!=null)for(int[] event:decoded)a.screens.captureCreationMidi(event[0],event[1],event[2],arrivalTimeNs,inputEpoch,timestamp,sourceId);
    a.handler.post(()->{
     if(generation!=a.midiGeneration||inputEpoch!=a.midiInputEpoch||!a.foreground)return;
     for(int[] event:decoded)onMidiMessage(event[0],event[1],event[2],packet,arrivalTime,arrivalTimeNs,timestamp,uiSession,false);
     if(a.screens!=null)a.screens.onLabMidiBatchEnd(packet,arrivalTime,uiSession);
    });
   }
  };
 }
 void clearMidiObservation(){
  a.downKeys.clear();a.midiSources.clear();a.waitRecognizer.clear();a.performance.disconnect();a.sustainDown=false;
  if(a.studyState!=null){a.studyState.pressed.clear();}
  if(a.screens!=null)a.screens.updatePressed(a.downKeys);
 }
 void send(int pitch,boolean on){send(pitch,on,74);}
 void send(int pitch,boolean on,int velocity){
  byte[] bytes={(byte)(on?0x90:0x80),(byte)pitch,(byte)(on?velocity:0)};
  if(a.midiConnection.send(bytes)){
   a.sentMessages++;refreshMidiDiagnostics();
  }
 }
 void allOff(){
  a.outputTestSequence++;
  if(a.midiPlayback!=null){MidiPlayback playing=a.midiPlayback;a.midiPlayback=null;playing.stop();}
  for(int note:new HashSet<>(a.activeNotes))send(note,false);
  a.activeNotes.clear();a.midiConnection.silence();
 }
 void updateSustainState(){
  a.sustainDown=false;for(int ch=0;ch<16;ch++)if(a.performance.pedalDown(ch)){a.sustainDown=true;break;}
 }
 void onMidiMessage(int statusCode,int data1,int data2){
  onMidiMessage(statusCode,data1,data2,a.midiPacketSequence.incrementAndGet(),SystemClock.uptimeMillis(),SystemClock.elapsedRealtimeNanos(),-1,a.screens==null?-1:a.screens.discoveryGeneration(),true);
 }
 void onMidiMessage(int statusCode,int data1,int data2,long packet,long eventTime,long eventTimeNs,long androidTime,long uiSession,boolean recordCreation){
  a.currentMidiPacket=packet;a.currentMidiEventTime=eventTime;a.currentUiSession=uiSession;
  if(recordCreation&&a.screens!=null)a.screens.onCreationMidi(statusCode,data1,data2,eventTimeNs,a.midiInputEpoch,androidTime,a.midiConnection.snapshot().target==null?-1:a.midiConnection.snapshot().target.id);
  int command=statusCode&0xF0;
  a.receivedMessages++;a.lastMidiChannel=(statusCode&0x0f)+1;
  if(command==0x90&&data2>0){
   a.lastMidiNote=data1;a.lastMidiVelocity=data2;
   a.lastMidiMessage="Nota "+midiNoteName(data1)+" ("+data1+") · canal "+a.lastMidiChannel+" · velocidade "+data2;
   if(a.status!=null)a.status.setText("♪ MIDI RX: "+midiNoteName(data1));
   a.performance.noteOn(statusCode&15,data1,data2,SystemClock.uptimeMillis(),a.position);
   onKey(data1,true,statusCode&15);
  }else if(command==0x80||(command==0x90&&data2==0)){
   a.lastMidiNote=data1;a.lastMidiVelocity=0;
   a.lastMidiMessage="Soltou "+midiNoteName(data1)+" ("+data1+") · canal "+a.lastMidiChannel;
   a.performance.noteOff(statusCode&15,data1,SystemClock.uptimeMillis());
   onKey(data1,false,statusCode&15);
  }else if(command==0xB0&&(data1==120||data1==121||data1==123)){
   int channel=statusCode&15;
   a.performance.channelMode(channel,data1,SystemClock.uptimeMillis());
   if(data1!=121)for(int source:new HashSet<>(a.midiSources))
    if((source>>7)==channel)onKey(source&127,false,channel);
   updateSustainState();
   a.lastMidiMessage=(data1==120?"Som interrompido":data1==123?"Notas liberadas":"Controladores redefinidos")+" · canal "+(channel+1);
  }else if(command==0xB0&&data1==64){
   a.performance.control64(statusCode&15,data2,SystemClock.uptimeMillis());
   // CC64 belongs to a MIDI channel. Releasing one channel must not
   // report the pedal as up while another channel still holds sustain.
   updateSustainState();
   a.lastMidiMessage="Pedal sustain "+(a.sustainDown?"pressionado":"solto");
   if(a.mode==0&&a.screens!=null&&a.screens.isStudyOpen()&&!a.chordCalibration.recording())
    a.screens.showWaitFeedback("Pedal "+(a.sustainDown?"acionado":"liberado")+" · orientação, não bloqueia o Espera");
  }else{
   a.lastMidiMessage="MIDI 0x"+Integer.toHexString(statusCode)+" · "+data1+" · "+data2;
  }
  android.util.Log.i("PianoLivreMIDI","RX "+a.lastMidiMessage);
  refreshMidiDiagnostics();
 }
 String midiNoteName(int n){
  String[] notes={"Dó","Dó#","Ré","Ré#","Mi","Fá","Fá#","Sol","Sol#","Lá","Lá#","Si"};
  return notes[n%12]+(n/12-1);
 }
 String midiDiagnosticReport(){
  MidiConnection.Snapshot connection=a.midiConnection.snapshot();
  return "Dispositivo: "+(connection.target==null?"Nenhum":connection.target.name.isEmpty()?"Dispositivo sem nome (ID "+connection.target.id+")":connection.target.name)
   +"\nEstado MIDI: "+connection.phase
   +"\nSituação: "+a.connectionDescription
   +"\nPorta de entrada (piano → app): "+(!connection.canReceive?"não conectada":"aberta")
   +"\nPorta de saída (app → piano): "+(!connection.canSend?"não conectada":"aberta")
   +"\nMensagens recebidas: "+a.receivedMessages
   +"\nMensagens enviadas: "+a.sentMessages
   +"\nÚltima mensagem: "+a.lastMidiMessage
   +"\nNotas pressionadas: "+a.downKeys
   +"\nPedal: "+(a.sustainDown?"pressionado":"solto")
   +"\nNotas fisicamente pressionadas: "+a.performance.physicalKeysDown()
   +"\nVozes retidas por tecla/pedal: "+a.performance.soundingVoices()
   +"\nNotas concluídas registradas: "+a.performance.completed().size()
   +"\nJanela de acorde: "+(a.waitRecognizer.chordWindowMs()==null?"aguardando calibração":a.waitRecognizer.chordWindowMs()+" ms (ajustável)")
   +"\n\nToque algumas teclas do instrumento e verifique se o contador aumenta."
   +"\nO teste de saída envia Dó4 ao instrumento por meio segundo."
   +"\nO volume e a configuração de recepção MIDI do piano precisam permitir som.";
 }
 void refreshMidiDiagnostics(){
  if(a.midiDiagnosticText!=null)a.midiDiagnosticText.setText(midiDiagnosticReport());
 }
 void showMidiDiagnostics(){
  TextView body=new TextView(a);body.setTextSize(14);
  body.setPadding(24,20,24,20);a.midiDiagnosticText=body;refreshMidiDiagnostics();
  ScrollView scroll=new ScrollView(a);scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Diagnóstico USB-MIDI")
   .setView(scroll).setPositiveButton("Testar saída: Dó4",(d,which)->testMidiOutput())
   .setNeutralButton("Escolher dispositivo",(d,which)->a.selectMidi())
   .setNegativeButton("Fechar",null).create();
  dialog.setOnDismissListener(d->a.midiDiagnosticText=null);
  dialog.show();
 }
 void testMidiOutput(){
  if(!a.midiConnection.snapshot().canSend){a.message("Sem porta de saída MIDI. Selecione um instrumento em USB-MIDI.");return;}
  a.stopPlayback();final long generation=a.midiGeneration,sequence=++a.outputTestSequence;
  send(60,true,88);
  a.handler.postDelayed(()->{if(generation==a.midiGeneration&&sequence==a.outputTestSequence&&a.foreground)send(60,false,0);},550);
  if(a.status!=null)a.status.setText("Teste MIDI enviado: Dó4 por 0,55 s");
 }
 /** MIDI sources are tracked by channel+pitch. New attacks only. */
 void onKey(int pitch,boolean pressed,int channel){
  if(pitch<0||pitch>127||channel<0||channel>15)return;
  int source=(channel<<7)|pitch;
  boolean newAttack=false;
  boolean releasedLast=false;
  long now=a.currentMidiEventTime>0?a.currentMidiEventTime:SystemClock.uptimeMillis();
  if(pressed){
   if(a.midiSources.add(source))newAttack=a.downKeys.add(pitch);
   if(newAttack&&a.foreground&&a.chordCalibration.recording())a.chordCalibration.noteOn(pitch,now);
  }else{
   a.midiSources.remove(source);
   boolean stillPressed=false;
   for(int ch=0;ch<16;ch++)if(a.midiSources.contains((ch<<7)|pitch)){stillPressed=true;break;}
   if(!stillPressed){
    releasedLast=true;
    a.downKeys.remove(pitch);a.waitRecognizer.noteOff(pitch);
    if(a.chordCalibration.recording()&&a.chordCalibration.noteOff(pitch,now))a.updateChordCalibrationStatus();
   }
  }
  a.studyState.pressed.clear();a.studyState.pressed.addAll(a.downKeys);
  if(a.screens!=null){
   a.screens.updatePressed(a.downKeys);
   if(a.foreground){
    if(newAttack)a.screens.onLabMidiAttack(pitch,now,a.currentMidiPacket,a.midiGeneration,a.currentUiSession);
    else if(releasedLast)a.screens.onLabMidiRelease(pitch,a.midiGeneration,a.currentUiSession);
   }
  }
  if(!a.foreground||a.chordCalibration.recording()||a.score==null||a.mode!=0||!a.waitStarted||a.waitPreparing||!pressed||a.waitTraveling||
      (a.screens!=null&&!a.screens.isStudyOpen()))return;
  WaitRecognizer.Result result=a.waitRecognizer.noteOnTimed(pitch,newAttack,a.downKeys,now);
  if(a.screens!=null&&a.screens.isStudyOpen()){
   if(result.extra){
    a.screens.showWaitFeedback("Nota extra: "+midiNoteName(pitch)+" · continue com as notas da partitura");
   }else if(result.windowExceeded){
    a.screens.showWaitFeedback("Acorde fora da janela calibrada · solte e repita as notas");
   }else if(result.advance){
    String suffix=result.extraPitches.isEmpty()?"":" · extras indicadas (não bloqueiam)";
    if(a.waitRecognizer.expected().size()>1)
     a.screens.showWaitFeedback("Acorde reconhecido ✓ · observe a duração escrita"+suffix);
    else if(!result.extraPitches.isEmpty())
     a.screens.showWaitFeedback("Nota reconhecida ✓"+suffix);
   }
  }
  if(result.advance)a.nextWait(); // Duration/pedal are guidance, NOT a gate.
 }
}

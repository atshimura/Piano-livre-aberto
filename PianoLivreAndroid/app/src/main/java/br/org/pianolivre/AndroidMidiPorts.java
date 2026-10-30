package br.org.pianolivre;

import android.media.midi.*;
import android.util.Log;

/** Android resources; each port is opened/closed independently. */
final class AndroidMidiPorts implements MidiConnection.Ports {
 private final MidiDevice device;private final MidiReceiver receiver;
 private MidiOutputPort incoming;private MidiInputPort outgoing;
 AndroidMidiPorts(MidiDevice device,MidiReceiver receiver){
  this.device=device;this.receiver=receiver;
  try{if(device.getInfo().getOutputPortCount()>0){incoming=device.openOutputPort(0);if(incoming!=null)incoming.connect(receiver);}}
  catch(Exception error){Log.w("PianoLivreMIDI","Porta de recepção indisponível",error);closeIncoming();}
  try{if(device.getInfo().getInputPortCount()>0)outgoing=device.openInputPort(0);}
  catch(Exception error){Log.w("PianoLivreMIDI","Porta de envio indisponível",error);}
 }
 public boolean canReceive(){return incoming!=null;}
 public boolean canSend(){return outgoing!=null;}
 public void send(byte[] bytes)throws Exception{if(outgoing==null)throw new java.io.IOException("Porta de envio fechada");outgoing.send(bytes,0,bytes.length);}
 private void closeIncoming(){
  if(incoming==null)return;
  try{incoming.disconnect(receiver);}catch(Exception error){Log.w("PianoLivreMIDI","Desligar receptor",error);}
  try{incoming.close();}catch(Exception error){Log.w("PianoLivreMIDI","Fechar entrada",error);}finally{incoming=null;}
 }
 public void close(){
  closeIncoming();
  if(outgoing!=null)try{outgoing.close();}catch(Exception error){Log.w("PianoLivreMIDI","Fechar saída",error);}finally{outgoing=null;}
  try{device.close();}catch(Exception error){Log.w("PianoLivreMIDI","Fechar dispositivo",error);}
 }
}

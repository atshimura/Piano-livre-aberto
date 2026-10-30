package br.org.pianolivre;

import java.util.*;

/** One connection owner. Calls and completions are serialized on the UI thread. */
public final class MidiConnection {
 public enum Phase { DISCONNECTED, OPENING, CONNECTED, FAILED }
 public static final class Target {
  public final int id;public final String name;public final boolean usb;
  public Target(int id,String name,boolean usb){this.id=id;this.name=name==null?"":name.trim();this.usb=usb;}
 }
 public interface Ports {
  boolean canReceive();boolean canSend();
  void send(byte[] bytes)throws Exception;
  void close()throws Exception;
 }
 public interface Completion {void complete(Ports ports);}
 public interface Opener {void open(Target target,long generation,Completion completion)throws Exception;}
 public interface Listener {
  void invalidated(long generation);
  void changed(Snapshot snapshot);
 }
 public static final class Snapshot {
  public final Phase phase;public final Target target;public final long generation;
  public final boolean canReceive,canSend;public final String detail;
  Snapshot(Phase phase,Target target,long generation,boolean rx,boolean tx,String detail){
   this.phase=phase;this.target=target;this.generation=generation;canReceive=rx;canSend=tx;this.detail=detail;
  }
  public boolean connected(){return phase==Phase.CONNECTED&&(canReceive||canSend);}
 }
 private final Listener listener;
 private Phase phase=Phase.DISCONNECTED;private Target target;private Ports ports;
 private long generation;private boolean destroyed,terminating;
 private String detail="Nenhum dispositivo conectado";
 private final Set<Integer> sounding=new HashSet<>();
 private final Set<Integer> usedChannels=new HashSet<>();
 public MidiConnection(Listener listener){this.listener=listener;}
 public Snapshot snapshot(){return new Snapshot(phase,target,generation,ports!=null&&ports.canReceive(),ports!=null&&ports.canSend(),detail);}
 private void publish(){listener.changed(snapshot());}
 private static void close(Ports value){if(value!=null)try{value.close();}catch(Exception ignored){}}
 private void reset(){
  generation++;terminating=true;
  // Stop transport/clear receivers before closing, while the old output can be silenced.
  try{listener.invalidated(generation);}finally{silencePorts();close(ports);ports=null;terminating=false;}
 }
 public void connect(Target next,Opener opener){
  if(destroyed||next==null)return;
  if(target!=null&&target.id==next.id&&(phase==Phase.OPENING||phase==Phase.CONNECTED)){refresh(next);return;}
  reset();target=next;phase=Phase.OPENING;detail="Abrindo conexão MIDI…";publish();
  final long token=generation;
  try{opener.open(next,token,opened->complete(next.id,token,opened));}
  catch(Exception error){if(token==generation&&phase==Phase.OPENING)complete(next.id,token,null);}
 }
 private void complete(int id,long token,Ports opened){
  if(opened!=null&&opened==ports)return;
  if(destroyed||token!=generation||target==null||target.id!=id||phase!=Phase.OPENING){close(opened);return;}
  if(opened==null||(!opened.canReceive()&&!opened.canSend())){
   close(opened);reset();phase=Phase.FAILED;detail="Não foi possível abrir portas MIDI";publish();return;
  }
  ports=opened;phase=Phase.CONNECTED;
  detail="Entrada "+(opened.canReceive()?"OK":"INDISPONÍVEL")+" / Saída "+(opened.canSend()?"OK":"INDISPONÍVEL");publish();
 }
 public void refresh(Target updated){if(target!=null&&updated.id==target.id){target=updated;publish();}}
 public void reconcile(List<Target> available,Opener opener){
  if(destroyed)return;
  Target current=null;for(Target value:available)if(target!=null&&value.id==target.id)current=value;
  if(target!=null&&current==null)disconnect("Instrumento desconectado · sessão pausada");
  else if(current!=null)refresh(current);
  if(phase==Phase.DISCONNECTED)for(Target value:available)if(value.usb){connect(value,opener);break;}
 }
 public void disconnect(String reason){reset();target=null;phase=Phase.DISCONNECTED;detail=reason;publish();}
 public boolean send(byte[] bytes){
  if(terminating||ports==null||!ports.canSend())return false;
  try{
   ports.send(bytes);
   if(bytes.length>=3){int command=bytes[0]&0xf0,channel=bytes[0]&15,pitch=bytes[1]&127;
    usedChannels.add(channel);int key=(channel<<7)|pitch;
    if(command==0x90&&(bytes[2]&127)>0)sounding.add(key);
    else if(command==0x80||command==0x90)sounding.remove(key);
   }
   return true;
  }catch(Exception error){
   reset();phase=Phase.FAILED;detail="Falha de envio MIDI · sessão pausada";publish();return false;
  }
 }
 /** Best effort: independent messages, pedal release before All Notes Off. */
 public void silence(){
  if(!terminating)silencePorts();
 }
 private void silencePorts(){
  Ports current=ports;
  if(current!=null&&current.canSend()){
   for(int key:new HashSet<>(sounding))raw(current,new byte[]{(byte)(0x80|(key>>7)),(byte)(key&127),0});
   Set<Integer> channels=new HashSet<>(usedChannels);channels.add(0);
   for(int channel:channels){raw(current,new byte[]{(byte)(0xB0|channel),64,0});raw(current,new byte[]{(byte)(0xB0|channel),123,0});}
  }
  sounding.clear();usedChannels.clear();
 }
 private static void raw(Ports current,byte[] bytes){try{current.send(bytes);}catch(Exception ignored){}}
 public void destroy(){destroyed=true;disconnect("Conexão MIDI encerrada");}
}

package br.org.pianolivre;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** One pending wakeup; ports and stop are serialized by the supplied dispatcher. */
public final class MidiTakePlayer implements AutoCloseable {
 public enum State { STOPPED, PLAYING, PAUSED }
 public interface Sink { boolean available(); boolean send(byte[] message); void silence(); }
 public interface Listener { void changed(State state,long positionNs,long durationNs); }
 private final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"PianoLivre-MIDI-playback");t.setDaemon(true);return t;});
 private final Sink sink; private final Listener listener; private final Executor dispatcher;
 private final Object lock=new Object(); private ScheduledFuture<?> pending;
 private CreationProjectStore.Project project; private long[] times;
 private State state=State.STOPPED; private long generation,baseNs,anchorNs,durationNs,trimOffsetNs;
 private int index;
 public MidiTakePlayer(Sink sink,Listener listener){this(sink,listener,Runnable::run);}
 public MidiTakePlayer(Sink sink,Listener listener,Executor dispatcher){this.sink=Objects.requireNonNull(sink);this.listener=Objects.requireNonNull(listener);this.dispatcher=Objects.requireNonNull(dispatcher);}
 public State state(){synchronized(lock){return state;}}
 public void play(CreationProjectStore.Project p,boolean trim){synchronized(lock){
  stop();if(p==null||p.events.isEmpty()||!sink.available())return;
  project=p;times=new long[p.events.size()];long first=Long.MAX_VALUE,last=0;
  try{MidiStandardFileWriter.Timeline timeline=new MidiStandardFileWriter.Timeline(p.segments);
   for(int i=0;i<times.length;i++){MidiTakeRecorder.Event e=p.events.get(i);times[i]=timeline.map(e.timeNs);
    if(e.command()==0x90&&e.data2>0)first=Math.min(first,times[i]);
    if(e.command()==0x80||e.command()==0x90&&e.data2==0||e.command()==0xb0&&e.data1==64&&e.data2<64)last=Math.max(last,times[i]);}
  }catch(IOException invalid){stop();return;}
  trimOffsetNs=trim&&first!=Long.MAX_VALUE?first:0;durationNs=trim?Math.max(0,last-trimOffsetNs):p.durationNs;
  baseNs=trimOffsetNs;anchorNs=System.nanoTime();index=0;state=State.PLAYING;
  long token=++generation;publish(state,0,durationNs);schedule(token,0);
 }}
 public void pause(){synchronized(lock){if(state!=State.PLAYING)return;baseNs=trimOffsetNs+position();cancel();sink.silence();state=State.PAUSED;publish(state,baseNs-trimOffsetNs,durationNs);}}
 public void resume(){synchronized(lock){if(state!=State.PAUSED||!sink.available())return;
  Map<Integer,MidiTakeRecorder.Event> held=new TreeMap<>(),sustained=new TreeMap<>();Map<Integer,MidiTakeRecorder.Event> controllers=new TreeMap<>();Set<Integer> pedals=new HashSet<>();
  index=0;while(index<times.length&&times[index]<baseNs){MidiTakeRecorder.Event e=project.events.get(index++);int key=e.channel()*128+e.data1;
   if(e.command()==0x90&&e.data2>0){held.put(key,e);sustained.remove(key);}
   else if(e.command()==0x80||e.command()==0x90){MidiTakeRecorder.Event attack=held.remove(key);if(attack!=null&&pedals.contains(e.channel()))sustained.put(key,attack);}
   else if(e.command()==0xb0){
    if(e.data1==121){controllers.keySet().removeIf(k->k/128==e.channel());pedals.remove(e.channel());sustained.keySet().removeIf(k->k/128==e.channel());}
    controllers.put(key,e);
    if(e.data1==64){if(e.data2>=64)pedals.add(e.channel());else{pedals.remove(e.channel());sustained.keySet().removeIf(k->k/128==e.channel());}}
    if(e.data1==123){if(pedals.contains(e.channel()))for(Map.Entry<Integer,MidiTakeRecorder.Event> entry:held.entrySet())if(entry.getKey()/128==e.channel())sustained.put(entry.getKey(),entry.getValue());held.keySet().removeIf(k->k/128==e.channel());}
    if(e.data1==120){held.keySet().removeIf(k->k/128==e.channel());sustained.keySet().removeIf(k->k/128==e.channel());}}}
  List<MidiTakeRecorder.Event> controls=new ArrayList<>(controllers.values());controls.sort(Comparator.comparingInt(e->e.order));
  for(MidiTakeRecorder.Event e:controls)if(e.data1!=120&&e.data1!=123&&!sink.send(message(e))){stop();return;}
  for(MidiTakeRecorder.Event e:held.values())if(!sink.send(message(e))){stop();return;}
  for(MidiTakeRecorder.Event e:sustained.values())if(!sink.send(message(e))||!sink.send(new byte[]{(byte)(0x80|e.channel()),(byte)e.data1,0})){stop();return;}
  anchorNs=System.nanoTime();state=State.PLAYING;long token=++generation;publish(state,baseNs-trimOffsetNs,durationNs);schedule(token,0);
 }}
 public void stop(){synchronized(lock){cancel();sink.silence();state=State.STOPPED;project=null;times=null;index=0;baseNs=0;publish(state,0,durationNs);}}
 private long position(){return Math.min(durationNs,Math.max(0,baseNs-trimOffsetNs+System.nanoTime()-anchorNs));}
 private void schedule(long token,long delay){pending=clock.schedule(()->dispatcher.execute(()->tick(token)),Math.max(0,delay),TimeUnit.NANOSECONDS);}
 private void tick(long token){synchronized(lock){
  if(token!=generation||state!=State.PLAYING||project==null)return;
  if(!sink.available()){stop();return;}
  long position=position(),now=trimOffsetNs+position;int batch=0;
  while(index<times.length&&times[index]<=now&&batch++<128){MidiTakeRecorder.Event e=project.events.get(index++);
   if(!sink.send(message(e))){stop();return;}if(token!=generation||state!=State.PLAYING)return;}
  publish(state,position,durationNs);
  if(position>=durationNs&&(index==times.length||times[index]>now)){cancel();sink.silence();state=State.STOPPED;project=null;times=null;publish(state,durationNs,durationNs);return;}
  long delay=index<times.length?Math.min(50_000_000L,Math.max(0,times[index]-now)):Math.min(50_000_000L,Math.max(0,durationNs-position));schedule(token,delay);
 }}
 private void cancel(){generation++;if(pending!=null)pending.cancel(false);pending=null;}
 private static byte[] message(MidiTakeRecorder.Event e){return e.command()==0xc0||e.command()==0xd0?new byte[]{(byte)e.status,(byte)e.data1}:new byte[]{(byte)e.status,(byte)e.data1,(byte)e.data2};}
 private void publish(State s,long p,long d){try{listener.changed(s,p,d);}catch(RuntimeException ignored){}}
 @Override public void close(){stop();clock.shutdownNow();}
}

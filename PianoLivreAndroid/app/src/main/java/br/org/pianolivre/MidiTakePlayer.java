package br.org.pianolivre;

import java.io.IOException;import java.util.*;import java.util.concurrent.*;

/** One-shot scheduler for a saved take. Pause/stop invalidate queued callbacks and silence output. */
public final class MidiTakePlayer implements AutoCloseable {
 public enum State { STOPPED,PLAYING,PAUSED }
 public interface Sink {boolean available();boolean send(byte[] message);void silence();}
 public interface Listener {void changed(State state,long positionNs,long durationNs);}
 private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"PianoLivre-MIDI-playback");t.setDaemon(true);return t;});
 private final Sink sink;private final Listener listener;private final Object lock=new Object();private final List<ScheduledFuture<?>> jobs=new ArrayList<>();
 private CreationProjectStore.Project project;private State state=State.STOPPED;private long generation,baseNs,anchorNs,durationNs,trimOffsetNs;private boolean ignoreSilence=true;
 public MidiTakePlayer(Sink s,Listener l){sink=Objects.requireNonNull(s);listener=Objects.requireNonNull(l);}
 public State state(){synchronized(lock){return state;}}
 public void play(CreationProjectStore.Project p,boolean ignoreEdges){synchronized(lock){cancelLocked();sink.silence();if(p==null||p.events.isEmpty()||!sink.available()){publish(State.STOPPED,0,0);return;}project=p;ignoreSilence=ignoreEdges;trimOffsetNs=ignoreSilence?firstSoundingTime():0;durationNs=ignoreSilence?Math.max(0,lastSoundingTime()-trimOffsetNs):p.durationNs;baseNs=trimOffsetNs;anchorNs=System.nanoTime();state=State.PLAYING;scheduleLocked(baseNs);publish(state,0,durationNs);}}
 public void pause(){synchronized(lock){if(state!=State.PLAYING)return;long position=positionLocked();baseNs=trimOffsetNs+position;cancelJobsLocked();sink.silence();state=State.PAUSED;publish(state,position,durationNs);}}
 public void resume(){synchronized(lock){if(state!=State.PAUSED||!sink.available())return;anchorNs=System.nanoTime();state=State.PLAYING;scheduleLocked(baseNs);publish(state,baseNs-trimOffsetNs,durationNs);}}
 public void stop(){synchronized(lock){cancelLocked();sink.silence();baseNs=0;state=State.STOPPED;publish(state,0,durationNs);project=null;}}
 private void scheduleLocked(long offset){long token=++generation;for(MidiTakeRecorder.Event event:project.events){long t;try{t=MidiStandardFileWriter.playbackTime(project.segments,event.timeNs);}catch(IOException ignored){continue;}if(t<offset)continue;long delay=Math.max(0,t-offset);byte[] bytes=message(event);jobs.add(executor.schedule(()->dispatch(token,event,bytes,t),delay,TimeUnit.NANOSECONDS));}long finish=Math.max(0,durationNs-Math.max(0,offset-trimOffsetNs));jobs.add(executor.schedule(()->finish(token),finish,TimeUnit.NANOSECONDS));}
 private void dispatch(long token,MidiTakeRecorder.Event event,byte[] bytes,long pos){synchronized(lock){if(token!=generation||state!=State.PLAYING||project==null)return;}if(!sink.available()||!sink.send(bytes)){stop();return;}publish(State.PLAYING,Math.max(0,pos-trimOffsetNs),durationNs);}
 private void finish(long token){synchronized(lock){if(token!=generation||state!=State.PLAYING)return;sink.silence();cancelJobsLocked();baseNs=0;state=State.STOPPED;project=null;publish(state,durationNs,durationNs);}}
 private long positionLocked(){return Math.min(durationNs,Math.max(0,baseNs-trimOffsetNs+System.nanoTime()-anchorNs));}
 private long firstSoundingTime(){long first=Long.MAX_VALUE;for(MidiTakeRecorder.Event e:project.events)if(isSoundingStart(e))try{first=Math.min(first,MidiStandardFileWriter.playbackTime(project.segments,e.timeNs));}catch(IOException ignored){}return first==Long.MAX_VALUE?0:first;}
 private long lastSoundingTime(){long last=0;for(MidiTakeRecorder.Event e:project.events)if(isSoundingEnd(e))try{last=Math.max(last,MidiStandardFileWriter.playbackTime(project.segments,e.timeNs));}catch(IOException ignored){}return last;}
 private static boolean isSoundingStart(MidiTakeRecorder.Event e){return e.command()==0x90&&e.data2>0;}
 private static boolean isSoundingEnd(MidiTakeRecorder.Event e){return e.command()==0x80||e.command()==0x90&&e.data2==0||e.command()==0xb0&&e.data1==64&&e.data2<64;}
 private static byte[] message(MidiTakeRecorder.Event e){return e.command()==0xc0||e.command()==0xd0?new byte[]{(byte)e.status,(byte)e.data1}:new byte[]{(byte)e.status,(byte)e.data1,(byte)e.data2};}
 private void cancelLocked(){generation++;cancelJobsLocked();state=State.STOPPED;project=null;baseNs=0;}
 private void cancelJobsLocked(){for(ScheduledFuture<?> f:jobs)f.cancel(false);jobs.clear();}
 private void publish(State s,long p,long d){try{listener.changed(s,p,d);}catch(RuntimeException ignored){}}
 @Override public void close(){stop();executor.shutdownNow();}
}

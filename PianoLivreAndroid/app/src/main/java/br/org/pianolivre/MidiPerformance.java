package br.org.pianolivre;

import java.util.*;

/**
 * Observes physical MIDI note attacks, key releases and CC64 sustain separately
 * from the teaching mode. The observation is NOT a score/grade.
 *
 * Timestamps are monotonic arrival time in milliseconds, not music beats.
 * A note can be physically released while the sounding voice is still held by
 * the damper pedal. Same-pitch notes on different channels are independent.
 */
public final class MidiPerformance {
 public static final class Span {
  public final int pitch,channel,velocity;
  public final long startedAtMs,physicalReleasedAtMs,soundingEndedAtMs;
  public final double beatAtAttack;
  public final boolean pedalExtended;
  Span(int pitch,int channel,int velocity,long start,long physical,long sounding,double beat,boolean pedal){
   this.pitch=pitch;this.channel=channel;this.velocity=velocity;
   startedAtMs=start;physicalReleasedAtMs=physical;soundingEndedAtMs=sounding;
   beatAtAttack=beat;pedalExtended=pedal;
  }
  public long physicalDurationMs(){return Math.max(0,physicalReleasedAtMs-startedAtMs);}
  public long soundingDurationMs(){return Math.max(0,soundingEndedAtMs-startedAtMs);}
 }
 private static final class Pending {
  final int pitch,channel,velocity;final long started;final double beat;
  long keyReleased=-1;
  Pending(int pitch,int channel,int velocity,long now,double beat){
   this.pitch=pitch;this.channel=channel;this.velocity=velocity;this.started=now;this.beat=beat;
  }
 }
 private final Map<Integer,Pending> physicallyDown=new HashMap<>();
 private final Map<Integer,Pending> sustainedByPedal=new HashMap<>();
 private final boolean[] pedal=new boolean[16];
 private final ArrayDeque<Span> completed=new ArrayDeque<>();
 private static final int MAX_HISTORY=1024;
 private long lastTimestamp=-1;
 private int negativeOrOutOfOrder=0;
 private long time(long ms){
  if(ms<0){negativeOrOutOfOrder++;ms=Math.max(0,lastTimestamp);}
  if(lastTimestamp>=0&&ms<lastTimestamp){negativeOrOutOfOrder++;ms=lastTimestamp;}
  lastTimestamp=ms;return ms;
 }
 private static int key(int channel,int pitch){return channel*128+pitch;}
 private static boolean valid(int channel,int pitch){return channel>=0&&channel<16&&pitch>=0&&pitch<128;}
 private void complete(Pending p,long ms){
  long released=p.keyReleased<0?ms:p.keyReleased;
  completed.addLast(new Span(p.pitch,p.channel,p.velocity,p.started,released,ms,p.beat,ms>released));
  if(completed.size()>MAX_HISTORY)completed.removeFirst();
 }
 public void noteOn(int channel,int pitch,int velocity,long whenMs,double beat){
  if(!valid(channel,pitch)||velocity<=0||velocity>127)return;
  long when=time(whenMs),id=key(channel,pitch);
  Pending old=physicallyDown.remove((int)id);
  if(old!=null)complete(old,when);
  old=sustainedByPedal.remove((int)id);
  if(old!=null)complete(old,when);
  physicallyDown.put((int)id,new Pending(pitch,channel,velocity,when,beat));
 }
 public void noteOff(int channel,int pitch,long whenMs){
  if(!valid(channel,pitch))return;
  long when=time(whenMs);
  Pending p=physicallyDown.remove(key(channel,pitch));
  if(p==null)return;
  p.keyReleased=when;
  if(pedal[channel])sustainedByPedal.put(key(channel,pitch),p);
  else complete(p,when);
 }
 public void control64(int channel,int value,long whenMs){
  if(channel<0||channel>15||value<0||value>127)return;
  long when=time(whenMs);
  boolean next=value>=64;
  if(pedal[channel]&&!next){
   for(Iterator<Map.Entry<Integer,Pending>> it=sustainedByPedal.entrySet().iterator();it.hasNext();){
    Map.Entry<Integer,Pending> e=it.next();
    if(e.getValue().channel==channel){complete(e.getValue(),when);it.remove();}
   }
  }
  pedal[channel]=next;
 }
 /** MIDI channel-mode input: CC123 releases keys with sustain; CC120 ends sound immediately.
  * CC121 resets the controller state tracked here (CC64), not held keys. */
 public boolean channelMode(int channel,int controller,long whenMs){
  if(channel<0||channel>15||(controller!=120&&controller!=121&&controller!=123))return false;
  if(controller==121){control64(channel,0,whenMs);return true;}
  long when=time(whenMs);
  for(Iterator<Map.Entry<Integer,Pending>> it=physicallyDown.entrySet().iterator();it.hasNext();){
   Map.Entry<Integer,Pending> e=it.next();Pending p=e.getValue();
   if(p.channel!=channel)continue;
   p.keyReleased=when;it.remove();
   if(controller==123&&pedal[channel])sustainedByPedal.put(e.getKey(),p);
   else complete(p,when);
  }
  if(controller==120)for(Iterator<Map.Entry<Integer,Pending>> it=sustainedByPedal.entrySet().iterator();it.hasNext();){
   Pending p=it.next().getValue();if(p.channel==channel){complete(p,when);it.remove();}
  }
  return true;
 }
 /** Safe clear on USB unplug. Still-unreleased notes are discarded, not graded. */
 public void disconnect(){
  physicallyDown.clear();sustainedByPedal.clear();Arrays.fill(pedal,false);
  lastTimestamp=-1;
 }
 public List<Span> completed(){return Collections.unmodifiableList(new ArrayList<>(completed));}
 public boolean pedalDown(int channel){return channel>=0&&channel<16&&pedal[channel];}
 public int physicalKeysDown(){return physicallyDown.size();}
 public int soundingVoices(){return physicallyDown.size()+sustainedByPedal.size();}
 public int outOfOrderTimestamps(){return negativeOrOutOfOrder;}
 public void clearHistory(){completed.clear();}
}

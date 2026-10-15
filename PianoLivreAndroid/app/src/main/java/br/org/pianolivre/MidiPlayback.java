package br.org.pianolivre;

import java.util.*;

/** Sample-accurate-in-beats playback event ordering without Android dependencies. */
public final class MidiPlayback {
 public interface Sink {void note(int pitch,boolean on,int velocity);}
 private static final class Action {
  final double beat;final int pitch, velocity;final boolean on;
  Action(double beat,int pitch,int velocity,boolean on){
   this.beat=beat;this.pitch=pitch;this.velocity=velocity;this.on=on;
  }
 }
 private final ArrayList<Action> actions=new ArrayList<>();
 private final Map<Integer,Integer> active=new HashMap<>();
 private final Sink sink;
 private int cursor=0;
 public MidiPlayback(Score score,Sink sink){this(score,0,sink);}
 /** hand: 0 both, 1 upper stave/right, 2 lower stave/left. */
 public MidiPlayback(Score score,int hand,Sink sink){
  this.sink=sink;
  for(Score.Event e:score.events){
   if(e.midi<0||e.midi>127||e.length<=0)continue;
   if(hand==1&&e.staff!=1)continue;
   if(hand==2&&e.staff==1)continue;
   actions.add(new Action(e.beat,e.midi,e.velocity,true));
   actions.add(new Action(e.beat+e.length,e.midi,0,false));
  }
  actions.sort((a,b)->{
   int r=Double.compare(a.beat,b.beat);
   if(r!=0)return r;
   if(a.on!=b.on)return a.on?1:-1; // Off before on at the same instant.
   return Integer.compare(a.pitch,b.pitch);
  });
 }
 private void apply(Action action){
  int pitch=action.pitch;
  int old=active.getOrDefault(pitch,0);
  if(action.on){
   active.put(pitch,old+1);
   if(old==0)sink.note(pitch,true,action.velocity);
  }else if(old>0){
   if(old==1){active.remove(pitch);sink.note(pitch,false,0);}
   else active.put(pitch,old-1);
  }
 }
 /**
  * Seek must NOT emit prior On/Off events while reconstructing held notes:
  * doing so audibly plays the entire skipped passage during a repeat/Coda jump.
  * Reconstruct the final counts silently and send On only for sounding pitches.
  */
 public void seek(double beat){
  stop();
  cursor=0;
  Map<Integer,Integer> counts=new HashMap<>();
  Map<Integer,Integer> velocities=new HashMap<>();
  while(cursor<actions.size()&&actions.get(cursor).beat<=beat+1e-8){
   Action a=actions.get(cursor++);
   int count=counts.getOrDefault(a.pitch,0);
   if(a.on){
    counts.put(a.pitch,count+1);
    velocities.put(a.pitch,a.velocity);
   }else if(count>1)counts.put(a.pitch,count-1);
   else{counts.remove(a.pitch);velocities.remove(a.pitch);}
  }
  ArrayList<Integer> pitches=new ArrayList<>(counts.keySet());
  Collections.sort(pitches);
  for(int pitch:pitches){
   active.put(pitch,counts.get(pitch));
   sink.note(pitch,true,velocities.getOrDefault(pitch,80));
  }
 }
 /** Send every transition crossed by advancing the playhead, never dropping short notes. */
 public void advance(double beat){
  while(cursor<actions.size()&&actions.get(cursor).beat<=beat+1e-8){
   apply(actions.get(cursor++));
  }
 }
 /** Ensure no hanging notes on pause, disconnection or lifecycle interruption. */
 public void stop(){
  for(int pitch:new ArrayList<>(active.keySet()))sink.note(pitch,false,0);
  active.clear();
 }
 public int soundingKeys(){return active.size();}
}

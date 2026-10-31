package br.org.pianolivre;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bounded, single-take MIDI event recorder. Times are monotonic nanoseconds. */
public final class MidiTakeRecorder {
 public enum State { READY, RECORDING, PAUSED, REVIEW, INTERRUPTED, SAVED, DISCARDED }
 public static final int MAX_EVENTS=100_000;
 public static final long MAX_DURATION_NS=4L*60*60*1_000_000_000L;
 public static final class Event {
  public final int status,data1,data2,order,sourceId; public final long timeNs,epoch,androidTimestampNs; public final boolean generated;
  Event(int s,int a,int b,long t,long e,int o,boolean g){this(s,a,b,t,e,o,g,-1,-1);}
  Event(int s,int a,int b,long t,long e,int o,boolean g,long androidTime,int source){status=s;data1=a;data2=b;timeNs=t;epoch=e;order=o;generated=g;androidTimestampNs=androidTime;sourceId=source;}
  public int command(){return status&0xf0;} public int channel(){return status&15;}
 }
 public static final class Segment {
  public final long startNs,endNs; Segment(long s,long e){startNs=s;endNs=e;}
 }
 private State state=State.READY; private long epoch=-1,startNs,segmentStartNs,lastNs,pauseStartedNs,pausedNs; private int order,notes;
 private final ArrayList<Event> events=new ArrayList<>(); private final ArrayList<Segment> segments=new ArrayList<>();
 private final Map<Integer,Event> held=new HashMap<>(); private final Map<Integer,Integer> pedals=new HashMap<>(); private String interruption="";
 public synchronized State state(){return state;} public synchronized int eventCount(){return events.size();}
 public synchronized String interruption(){return interruption;}
 public synchronized void start(long connectionEpoch,long nowNs){
  if(state!=State.READY)throw new IllegalStateException("Tomada já iniciada");
  if(connectionEpoch<0||nowNs<0)throw new IllegalArgumentException("Época/relógio inválido");
  epoch=connectionEpoch;startNs=segmentStartNs=lastNs=nowNs;state=State.RECORDING;
 }
 /** Accepts only channel voice events from the current input epoch. */
 public synchronized boolean accept(int status,int a,int b,long nowNs,long inputEpoch){return accept(status,a,b,nowNs,inputEpoch,-1,-1);}
 public synchronized boolean accept(int status,int a,int b,long nowNs,long inputEpoch,long androidTime,int source){
  if(state!=State.RECORDING||inputEpoch!=epoch||status<0x80||status>0xef||a<0||a>127||b<0||b>127)return false;
  if(nowNs<startNs||nowNs<lastNs){interrupt(nowNs,"Relógio monotônico inválido");return false;}
  if(nowNs-startNs-pausedNs>MAX_DURATION_NS){interrupt(startNs+pausedNs+MAX_DURATION_NS,"Limite de gravação atingido");return false;}
  if(events.size()>=MAX_EVENTS-2064){interrupt(nowNs,"Limite de eventos atingido");return false;}
  int command=status&0xf0,ch=status&15,key=(ch<<7)|a;
  if(command==0x90&&b>0&&held.containsKey(key))append(0x80|ch,a,0,nowNs,true);
  events.add(new Event(status,a,b,nowNs,epoch,order++,false,androidTime>0?androidTime:-1,source));lastNs=nowNs;
  if(command==0x90&&b>0)notes++;
  if(command==0x90&&b>0)held.put(key,events.get(events.size()-1));
  else if(command==0x80||(command==0x90&&b==0))held.remove(key);
  else if(command==0xb0&&a==64){if(b>=64)pedals.put(ch,b);else pedals.remove(ch);}
  else if(command==0xb0&&(a==120||a==121||a==123)){
   // Reset All Controllers and All Notes Off may release a pedal as well as
   // notes. Keep the received controller message, then close our safety state.
   if(a!=121)for(int k:new ArrayList<>(held.keySet()))if((k>>7)==ch){append(0x80|ch,k&127,0,nowNs,true);held.remove(k);}
   if(a==121)pedals.remove(ch);
  }
  return true;
 }
 public synchronized void pause(long nowNs){if(state!=State.RECORDING)throw new IllegalStateException("Não está gravando");if(nowNs<lastNs)throw new IllegalArgumentException("Relógio regressivo");if(nowNs-startNs-pausedNs>MAX_DURATION_NS)nowNs=startNs+pausedNs+MAX_DURATION_NS;segments.add(new Segment(segmentStartNs,nowNs));silenceForBoundary(nowNs);lastNs=nowNs;pauseStartedNs=nowNs;state=State.PAUSED;}
 public synchronized void resume(long nowNs,long connectionEpoch){
  if(state!=State.PAUSED)throw new IllegalStateException("Gravação não pausada");
  if(connectionEpoch!=epoch){interrupt(nowNs,"Conexão MIDI mudou durante a pausa");return;}
  if(nowNs<pauseStartedNs||nowNs-startNs-pausedNs-(nowNs-pauseStartedNs)>=MAX_DURATION_NS){interrupt(nowNs,"Limite de gravação atingido");return;}
  pausedNs+=nowNs-pauseStartedNs;lastNs=nowNs;segmentStartNs=nowNs;state=State.RECORDING;
 }
 /** Explicit foreground continuation, only after caller verifies the same port generation. */
 public synchronized void resumeForeground(long nowNs,long previousEpoch,long newEpoch){
  if(state!=State.PAUSED||epoch!=previousEpoch||newEpoch<0)throw new IllegalStateException("Retomada inválida");
  epoch=newEpoch;resume(nowNs,newEpoch);
 }
 public synchronized void stop(long nowNs){
  if(state!=State.RECORDING&&state!=State.PAUSED)throw new IllegalStateException("Tomada não ativa");
  if(state==State.PAUSED){nowNs=pauseStartedNs;}
  if(nowNs<lastNs)nowNs=lastNs;
  if(state==State.RECORDING&&nowNs-startNs-pausedNs>MAX_DURATION_NS)nowNs=startNs+pausedNs+MAX_DURATION_NS;
  if(state==State.RECORDING)segments.add(new Segment(segmentStartNs,nowNs));
  // Safety closures are derived events; received source events remain untouched.
  silenceForBoundary(nowNs);held.clear();pedals.clear();lastNs=nowNs;state=State.REVIEW;
 }
 public synchronized void interrupt(String reason){interrupt(lastNs,reason);}
 public synchronized void interrupt(long nowNs,String reason){if(state!=State.RECORDING&&state!=State.PAUSED)return;boolean recording=state==State.RECORDING;long boundary=recording?Math.max(lastNs,nowNs):lastNs;interruption=reason==null?"Interrompida":reason;if(recording)segments.add(new Segment(segmentStartNs,boundary));silenceForBoundary(boundary);lastNs=boundary;state=State.INTERRUPTED;}
 public synchronized void discard(){if(state==State.SAVED)throw new IllegalStateException("Projeto salvo não pode ser descartado como tomada");events.clear();segments.clear();held.clear();state=State.DISCARDED;}
 public synchronized void markSaved(){if(state!=State.REVIEW&&state!=State.INTERRUPTED)throw new IllegalStateException("Tomada não revisada");state=State.SAVED;}
 public synchronized List<Event> events(){ArrayList<Event> copy=new ArrayList<>(events);copy.sort(Comparator.comparingLong((Event e)->e.timeNs).thenComparingInt(e->e.order));return Collections.unmodifiableList(copy);}
 public synchronized List<Segment> segments(){return Collections.unmodifiableList(new ArrayList<>(segments));}
 public synchronized List<Event> eventsSince(int index){return new ArrayList<>(events.subList(index,events.size()));}
 public synchronized int noteCount(){return notes;}
 public synchronized List<Segment> segmentsSince(int index){return new ArrayList<>(segments.subList(index,segments.size()));}
 public synchronized long elapsedNs(long now){return state==State.RECORDING?Math.min(MAX_DURATION_NS,Math.max(0,now-startNs-pausedNs)):durationNs();}
 public synchronized long durationNs(){return Math.max(0,lastNs-startNs-pausedNs);}
 public synchronized long epoch(){return epoch;}
 private void append(int s,int a,int b,long t,boolean generated){events.add(new Event(s,a,b,t,epoch,order++,generated));}
 private void silenceForBoundary(long nowNs){
  for(int key:new ArrayList<>(held.keySet()))append(0x80|(key>>7),key&127,0,nowNs,true);
  for(Map.Entry<Integer,Integer> p:pedals.entrySet())append(0xb0|p.getKey(),64,0,nowNs,true);
  held.clear();pedals.clear();
 }
}

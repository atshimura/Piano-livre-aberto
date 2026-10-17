package br.org.pianolivre;
import java.util.*;

/** Offline MIDI demonstration schedule; timings describe notation, never grading. */
public final class MusicLabPlaybackPlan {
 private MusicLabPlaybackPlan(){}
 public static final class Event {
  public final long atMillis;public final int note;public final boolean on;
  Event(long at,int note,boolean on){this.atMillis=at;this.note=note;this.on=on;}
 }
 public static List<Event> build(int[] pitches,String category,int ratePercent){
  if(pitches==null||pitches.length==0||pitches.length>40||ratePercent<25||ratePercent>200)
   throw new IllegalArgumentException("Parâmetros inválidos para demonstração");
  long beat=60000L*100/(100L*ratePercent); // 100 BPM at 100%
  long gate=Math.max(1L,beat*9/10);
  ArrayList<Event> events=new ArrayList<>();
  boolean chord="Acorde".equals(category)||"Intervalo".equals(category);
  for(int i=0;i<pitches.length;i++){
   int note=pitches[i];
   if(note<0||note>127)throw new IllegalArgumentException("Nota fora do MIDI");
   long at=chord?0:i*beat;
   events.add(new Event(at,note,true));
   events.add(new Event(at+(chord?beat:gate),note,false));
  }
  events.sort((a,b)->{
   int t=Long.compare(a.atMillis,b.atMillis);
   if(t!=0)return t;
   return Boolean.compare(a.on,b.on); // release before retrigger
  });
  return Collections.unmodifiableList(events);
 }
 public static long endMillis(List<Event> events){
  long end=0;for(Event e:events)end=Math.max(end,e.atMillis);
  return end;
 }
}

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
  Set<Integer> simultaneous=new HashSet<>();
  for(int i=0;i<pitches.length;i++){
   int note=pitches[i];
   if(note<0||note>127)throw new IllegalArgumentException("Nota fora do MIDI");
   // Unison intervals must not send duplicate simultaneous Note Ons on one MIDI channel.
   if(chord&&!simultaneous.add(note))continue;
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
 /**
  * Pure lifecycle guard shared by delayed Android callbacks. Playing stays true
  * during the silent gaps between note events and between loop iterations.
  * Cancellation invalidates every previously scheduled callback.
  */
 public static final class Session {
  private int generation=0;
  private boolean playing=false;
  private final Set<Integer> held=new LinkedHashSet<>();
  public boolean isPlaying(){return playing;}
  public int start(){
   if(playing)throw new IllegalStateException("Demonstração já está em andamento");
   held.clear();playing=true;return ++generation;
  }
  public boolean isCurrent(int token){return playing&&token==generation;}
  public boolean apply(int token,Event event){
   if(!isCurrent(token))return false;
   if(event.on)held.add(event.note);
   else held.remove(event.note);
   return true;
  }
  public boolean shouldRepeat(int token,boolean repeat){return isCurrent(token)&&repeat;}
  public Set<Integer> heldNotes(){return Collections.unmodifiableSet(new LinkedHashSet<>(held));}
  /** Returns only the notes that may need Note Off; always invalidates callbacks. */
  public Set<Integer> cancel(){
   ++generation;playing=false;
   Set<Integer> previous=new LinkedHashSet<>(held);
   held.clear();
   return previous;
  }
 }
 public static long endMillis(List<Event> events){
  long end=0;for(Event e:events)end=Math.max(end,e.atMillis);
  return end;
 }
}

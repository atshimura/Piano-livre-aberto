package br.org.pianolivre;
import java.util.*;

/**
 * Pedagogical Wait mode. The score determines the expected pitches; NOT physical
 * right/left hand identification. Each position requires fresh note attacks.
 *
 * Extra pitches are reported, never disqualifying. Chord simultaneity is only
 * enforced after explicit on-piano calibration: null window = uncalibrated,
 * non-blocking learning mode. No duration or CC64 gate exists here.
 */
public final class WaitRecognizer {
 public static final class Result {
  public final boolean advance,extra,windowExceeded;
  public final Set<Integer> extraPitches;
  Result(boolean advance,boolean extra,boolean windowExceeded,Set<Integer> extras){
   this.advance=advance;this.extra=extra;this.windowExceeded=windowExceeded;
   this.extraPitches=Collections.unmodifiableSet(new HashSet<>(extras));
  }
 }
 private final Set<Integer> required=new HashSet<>(),fresh=new HashSet<>();
 private final Set<Integer> mistakes=new HashSet<>();
 private final Map<Integer,Long> attackAt=new HashMap<>();
 private Long chordWindowMs=null;
 public void setChordWindowMs(Long ms){
  if(ms!=null&&ms<=0)throw new IllegalArgumentException("Janela deve ser positiva");
  chordWindowMs=ms;
 }
 public Long chordWindowMs(){return chordWindowMs;}
 public void clear(){required.clear();fresh.clear();attackAt.clear();mistakes.clear();}
 public void arm(Collection<Score.Event> notes){
  clear();
  if(notes!=null)for(Score.Event e:notes)if(e.midi>=0&&e.midi<=127)required.add(e.midi);
 }
 public Result noteOnTimed(int pitch,boolean newlyPressed,Set<Integer> held,long nowMs){
  if(!newlyPressed||pitch<0||pitch>127)
   return new Result(false,false,false,collectExtras(held));
  if(!required.contains(pitch)){
   mistakes.add(pitch);
   return new Result(false,true,false,collectExtras(held));
  }
  fresh.add(pitch);
  attackAt.put(pitch,nowMs);
  // Drop attacks older than the calibrated window; they must be retriggered
  // before the complete chord can pass. No mandatory delay for single notes.
  boolean expired=false;
  if(chordWindowMs!=null&&required.size()>1){
   for(Iterator<Map.Entry<Integer,Long>> it=attackAt.entrySet().iterator();it.hasNext();){
    Map.Entry<Integer,Long> e=it.next();
    if(nowMs-e.getValue()>chordWindowMs){
     fresh.remove(e.getKey());it.remove();expired=true;
    }
   }
  }
  boolean ready=!required.isEmpty()&&fresh.containsAll(required)&&held.containsAll(required);
  if(ready&&chordWindowMs!=null&&required.size()>1){
   long lo=Long.MAX_VALUE,hi=Long.MIN_VALUE;
   for(int n:required){Long when=attackAt.get(n);if(when==null){ready=false;break;}
    lo=Math.min(lo,when);hi=Math.max(hi,when);
   }
   if(ready&&hi-lo>chordWindowMs){ready=false;expired=true;}
  }
  return new Result(ready,false,expired,collectExtras(held));
 }
 /** Older test callers exercise non-calibrated mode without absolute time. */
 public boolean noteOn(int pitch,boolean newlyPressed,Set<Integer> held){
  return noteOnTimed(pitch,newlyPressed,held,0).advance;
 }
 public void noteOff(int pitch){fresh.remove(pitch);attackAt.remove(pitch);}
 public Set<Integer> expected(){return Collections.unmodifiableSet(required);}
 public Set<Integer> wrongPitches(){return Collections.unmodifiableSet(mistakes);}
 private Set<Integer> collectExtras(Set<Integer> held){
  Set<Integer> extra=new HashSet<>(mistakes);
  if(held!=null)for(int n:held)if(!required.contains(n))extra.add(n);
  return extra;
 }
}

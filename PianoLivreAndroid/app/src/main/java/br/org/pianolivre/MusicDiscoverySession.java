package br.org.pianolivre;

import java.util.*;

/** Deterministic MIDI-only discovery state. Timing is used only when the active
 * user has a measured chord window; otherwise packet boundaries are decisive. */
public final class MusicDiscoverySession {
 public enum Phase { EMPTY, PENDING, PRESENTED, PENDING_WITH_RESULT }
 public enum Kind { NOTE, CHORD, SEQUENCE, AMBIGUOUS }
 public static final class Result {
  public final Kind kind;
  public final List<Integer> notes;
  public final String title,detail;
  public final boolean ordered;
  Result(Kind kind,List<Integer> notes,String title,String detail){this(kind,notes,title,detail,kind==Kind.SEQUENCE);}
  Result(Kind kind,List<Integer> notes,String title,String detail,boolean ordered){
   this.kind=kind;this.notes=Collections.unmodifiableList(new ArrayList<>(notes));
   this.title=title;this.detail=detail;this.ordered=ordered;
  }
 }
 private static final class Attack {final int pitch;final long time,batch;
  Attack(int p,long t,long b){pitch=p;time=t;batch=b;}}
 private final Set<Integer> down=new HashSet<>();
 private final ArrayList<Attack> pending=new ArrayList<>();
 private final MusicPatternTracker patterns=new MusicPatternTracker();
 private final ArrayList<Integer> phraseNotes=new ArrayList<>();
 private final ArrayList<Long> phraseTimes=new ArrayList<>();
 private long generation=0; private Long chordWindowMs;
 private Result result;
 private boolean chordEvidence=false;
 public Phase phase(){boolean pendingInput=!pending.isEmpty()||!phraseNotes.isEmpty();return pendingInput?(result==null?Phase.PENDING:Phase.PENDING_WITH_RESULT):(result==null?Phase.EMPTY:Phase.PRESENTED);}
 public Result result(){return result;}
 public List<Integer> pendingNotes(){ArrayList<Integer> out=new ArrayList<>();for(Attack a:pending)out.add(a.pitch);return Collections.unmodifiableList(out);}
 public Set<Integer> downNotes(){return Collections.unmodifiableSet(new HashSet<>(down));}
 public void reconnect(long newGeneration,Long measuredChordWindowMs){
  generation=newGeneration;chordWindowMs=measuredChordWindowMs!=null&&measuredChordWindowMs>0?measuredChordWindowMs:null;
  down.clear();pending.clear();patterns.clear();patternStream.clear();phraseNotes.clear();phraseTimes.clear();chordEvidence=false;
 }
 public void reset(){generation++;down.clear();pending.clear();patterns.clear();patternStream.clear();phraseNotes.clear();phraseTimes.clear();chordEvidence=false;result=null;}
 public boolean attack(int pitch,long timeMs,long batchId,long eventGeneration){
  if(eventGeneration!=generation||pitch<0||pitch>127)return false;
  boolean priorHeld=!down.isEmpty();
  if(!down.add(pitch))return false; // duplicate Note On while held
  if(!priorHeld&&!phraseTimes.isEmpty()&&inactivityDelayMs()>0&&timeMs-phraseTimes.get(phraseTimes.size()-1)>inactivityDelayMs())finishPhrase();
  if(!pending.isEmpty()){
   boolean repeatedPitch=false;for(Attack old:pending)if(old.pitch==pitch){repeatedPitch=true;break;}
   long elapsed=timeMs-pending.get(0).time;
   boolean sameBatch=pending.get(0).batch==batchId;
   if(repeatedPitch||elapsed<0||(!sameBatch&&!priorHeld&&chordWindowMs==null)||
      (!sameBatch&&chordWindowMs!=null&&elapsed>chordWindowMs))commitPending();
   else if(priorHeld)chordEvidence=true;
  }
  pending.add(new Attack(pitch,timeMs,batchId));
  if(down.size()!=1){patterns.clear();phraseNotes.clear();phraseTimes.clear();}
  else {
   List<MusicSequenceAnalyzer.Finding> found=patterns.attack(pitch,true);
   if(!found.isEmpty()){
    MusicSequenceAnalyzer.Finding f=found.get(0);
    List<Integer> all=new ArrayList<>();
    // The tracker recognizes the exact suffix length in the current stream.
    ArrayList<Integer> stream=new ArrayList<>(patternStream);
    stream.add(pitch);
    int start=Math.max(0,stream.size()-f.noteCount);
    all.addAll(stream.subList(start,stream.size()));
    result=new Result(Kind.SEQUENCE,all,f.display(),"");pending.clear();patternStream.clear();phraseNotes.clear();phraseTimes.clear();chordEvidence=false;
    return true;
   }
   phraseNotes.add(pitch);phraseTimes.add(timeMs);while(phraseNotes.size()>40){phraseNotes.remove(0);phraseTimes.remove(0);}
  }
  patternStream.add(pitch);while(patternStream.size()>13)patternStream.remove(0);
  return true;
 }
 private final ArrayList<Integer> patternStream=new ArrayList<>();
 public void release(int pitch,long eventGeneration){if(eventGeneration!=generation)return;down.remove(pitch);if(down.isEmpty()&&(chordWindowMs==null||(pending.size()>1&&!chordEvidence)))commitPending();}
 public boolean finishBatch(long batchId,long nowMs){
  if(pending.isEmpty())return false;
  if(chordWindowMs==null){if(down.isEmpty()){commitPending();return true;}return false;}
  if(pending.get(0).batch==batchId&&nowMs-pending.get(0).time>=chordWindowMs){commitPending();return true;}
  return false;
 }
 public boolean advance(long nowMs){
  long quiet=inactivityDelayMs();
  if(quiet>0&&!phraseNotes.isEmpty()&&nowMs-phraseTimes.get(phraseTimes.size()-1)>=quiet){finishPhrase();return true;}
  if(pending.isEmpty()||chordWindowMs==null||nowMs-pending.get(0).time<chordWindowMs)return false;
  // A chord calibration separates attacks; it is not a phrase timeout.
  if(pending.size()>1&&!chordEvidence)return false;
  commitPending();return true;
 }
 /** Pause boundary is twice the median of this player's observed inter-onset intervals. */
 public long inactivityDelayMs(){
  if(phraseTimes.size()<4)return 0;
  ArrayList<Long> intervals=new ArrayList<>();for(int i=1;i<phraseTimes.size();i++){long d=phraseTimes.get(i)-phraseTimes.get(i-1);if(d>0)intervals.add(d);}
  if(intervals.size()<3)return 0;Collections.sort(intervals);long median=intervals.get(intervals.size()/2);
  return median>Long.MAX_VALUE/2?Long.MAX_VALUE:median*2;
 }
 public long nextDelayMs(long nowMs){
  long best=Long.MAX_VALUE;
  if(!pending.isEmpty()&&chordWindowMs!=null&&(pending.size()==1||chordEvidence)){long due=pending.get(0).time+chordWindowMs;best=Math.min(best,Math.max(1,due-nowMs));}
  long pause=inactivityDelayMs();if(pause>0&&!phraseTimes.isEmpty()){long due=phraseTimes.get(phraseTimes.size()-1)+pause;best=Math.min(best,Math.max(1,due-nowMs));}
  return best==Long.MAX_VALUE?0:best;
 }
 private void finishPhrase(){
  if(phraseNotes.size()>=4){StringBuilder names=new StringBuilder("Padrão incompleto ou ambíguo · ");
   for(int i=0;i<phraseNotes.size();i++){if(i>0)names.append(" · ");names.append(MusicNoteSpelling.international(phraseNotes.get(i)));}
   result=new Result(Kind.AMBIGUOUS,new ArrayList<>(phraseNotes),"Sequência não classificada",names.toString(),true);
  }
  phraseNotes.clear();phraseTimes.clear();patterns.clear();patternStream.clear();pending.clear();chordEvidence=false;
 }
 public void cancelPending(){pending.clear();patterns.clear();patternStream.clear();phraseNotes.clear();phraseTimes.clear();chordEvidence=false;}
 private void commitPending(){
  if(pending.isEmpty())return;
  ArrayList<Integer> exact=new ArrayList<>();for(Attack a:pending)exact.add(a.pitch);
  if(exact.size()==1){int n=exact.get(0);
   // Every released isolated pitch is a valid note result. PatternTracker has
   // its own rolling stream, so presenting this note cannot consume or block a
   // later complete scale/arpeggio. The complete pattern replaces it on the
   // attack that finishes the exact pattern.
   result=new Result(Kind.NOTE,exact,
    MusicNoteSpelling.portuguese(n)+" - "+MusicNoteSpelling.international(n),"");
   pending.clear();chordEvidence=false;return;}
  else {
   List<MusicTheory.Chord> chords=chordEvidence?MusicTheory.recognize(exact):Collections.emptyList();
   if(chords.size()==1)result=new Result(Kind.CHORD,exact,chords.get(0).symbol+" · "+chords.get(0).description,chords.get(0).inversion);
   else if(chords.size()>1){StringBuilder s=new StringBuilder("Possibilidades: ");
    for(int i=0;i<chords.size();i++){if(i>0)s.append(" / ");s.append(chords.get(i).symbol);}
    result=new Result(Kind.AMBIGUOUS,exact,"Acorde ambíguo",s.toString());
   }else result=new Result(Kind.AMBIGUOUS,exact,chordEvidence?"Notas simultâneas":"Sequência não classificada",
      chordEvidence?"Sem classificação única defensável":"Padrão incompleto ou ambíguo",!chordEvidence);
  }
  pending.clear();
  if(chordEvidence){patterns.clear();patternStream.clear();phraseNotes.clear();phraseTimes.clear();}
  chordEvidence=false;
 }
}

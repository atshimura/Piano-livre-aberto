package br.org.pianolivre;

/** GPL-3.0-only. Manual loop-menu boundaries, without mutating musical events. */
public final class StudyLoopEditing {
 private StudyLoopEditing(){}
 public static final class Bounds {
  public final double start,end;
  Bounds(double start,double end){this.start=start;this.end=end;}
  public boolean looping(){return end<Double.MAX_VALUE&&end-start>=.01;}
 }
 /** start=true marks the beginning. A stale end before the new start is
  * discarded rather than silently swapping the just-chosen beginning.
  * end selection may be made before start selection, as with the old menu.
  */
 public static Bounds mark(double position,double oldStart,double oldEnd,double scoreEnd,boolean start){
  if(!Double.isFinite(position)||!Double.isFinite(oldStart)||
     !(scoreEnd>.01)||!Double.isFinite(scoreEnd))return null;
  double p=Math.max(0,Math.min(scoreEnd,position));
  if(start){
   double end=oldEnd<=p+.01?Double.MAX_VALUE:oldEnd;
   return new Bounds(p,end);
  }
  if(p<=.001&&oldStart<=.001)return null; // cannot repeat a zero-length passage
  double lo=Math.min(oldStart,p),hi=Math.max(oldStart,p);
  if(hi-lo<.01)return null;
  return new Bounds(lo,hi);
 }
}

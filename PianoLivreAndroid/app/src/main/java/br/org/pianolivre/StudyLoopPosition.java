package br.org.pianolivre;

/** Conservative A-B marker alignment against actual written note events. */
public final class StudyLoopPosition {
 private StudyLoopPosition(){}
 private static boolean playable(Score.Event e){
  return e!=null&&e.midi>=0&&e.midi<=127&&e.length>0&&
   Double.isFinite(e.beat)&&Double.isFinite(e.length);
 }
 private static double clamped(Score score,double beat){
  return Math.max(0,Math.min(score.end(),Double.isFinite(beat)?beat:0));
 }
 /** A is the last fresh attack at/before the cursor, never half a note. */
 public static double start(Score score,double beat){
  if(score==null)return 0;
  double cursor=clamped(score,beat),latest=Double.NEGATIVE_INFINITY,first=Double.POSITIVE_INFINITY;
  for(Score.Event e:score.events){
   if(!playable(e)||e.tieStop)continue;
   first=Math.min(first,e.beat);
   if(e.beat<=cursor+1e-5)latest=Math.max(latest,e.beat);
  }
  if(Double.isFinite(latest))return latest;
  return Double.isFinite(first)?first:0;
 }
 /** B includes every note already sounding at the cursor; never slices a chord. */
 public static double end(Score score,double beat){
  if(score==null)return 0;
  double cursor=clamped(score,beat),activeEnd=Double.NEGATIVE_INFINITY;
  double priorEnd=Double.NEGATIVE_INFINITY,nextEnd=Double.POSITIVE_INFINITY;
  for(Score.Event e:score.events){
   if(!playable(e))continue;
   double finish=e.beat+e.length;
   if(e.beat<=cursor+1e-5&&finish>cursor+1e-5)
    activeEnd=Math.max(activeEnd,finish);
   if(finish<=cursor+1e-5)priorEnd=Math.max(priorEnd,finish);
   else nextEnd=Math.min(nextEnd,finish);
  }
  if(Double.isFinite(activeEnd))return Math.min(score.end(),activeEnd);
  if(Double.isFinite(priorEnd))return priorEnd;
  return Double.isFinite(nextEnd)?nextEnd:score.end();
 }
}

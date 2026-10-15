package br.org.pianolivre;
import java.util.*;

/**
 * Interprets only simple written forward/backward repeat barlines.
 * No alt endings / segno / coda expansion: Score explicitly flags these.
 *
 * Tracks cycles for each barline separately so normal flow can proceed once
 * the requested play count is exhausted. AB loop always has precedence.
 */
public final class RepeatNavigator {
 private final IdentityHashMap<Score.RepeatSection,Integer> visits=new IdentityHashMap<>();
 public void reset(){visits.clear();}
 public Score.RepeatSection crossing(Score score,double from,double to){
  if(score==null||to<from)return null;
  return score.firstRepeatBetween(from,to);
 }
 /** True if crossing this end should return to the repeat's first measure. */
 public boolean shouldJump(Score.RepeatSection section){
  if(section==null)return false;
  int previous=visits.getOrDefault(section,0);
  visits.put(section,previous+1);
  return previous<section.times-1;
 }
 /** Next Espera onset honoring AB loop (priority) and written repetitions. */
 public double nextWaitTarget(Score score,double position,int hand,
                              double loopStart,double loopEnd){
  if(score==null)return position;
  double next=score.nextForHand(position,hand);
  if(loopEnd<Double.MAX_VALUE&&loopStart<loopEnd){
   if(next<=position+.0001||next>=loopEnd)return loopStart;
   return next;
  }
  double through=next>position+.0001?next:score.end();
  Score.RepeatSection section=crossing(score,position,through);
  if(section!=null&&shouldJump(section))return section.start;
  return next;
 }
 public int visits(Score.RepeatSection section){return visits.getOrDefault(section,0);}
}

package br.org.pianolivre;

/**
 * Choosing another hand must preserve the passage: never rewind to the first
 * note of the entire composition. The only intentional rewind is within an
 * active loop when the chosen hand has no later note inside that loop.
 */
public final class HandNavigation {
 private HandNavigation(){}
 public static double nextPosition(Score s,double position,int hand,double loopStart,double loopEnd){
  if(s==null||!s.at(position,hand).isEmpty())return position;
  double next=s.nextForHand(position,hand);
  if(next>position+.005&&(loopEnd==Double.MAX_VALUE||next<loopEnd-.000001))return next;
  if(loopEnd<Double.MAX_VALUE&&loopEnd>loopStart){
   double firstInLoop=s.nextForHand(loopStart-.006,hand);
   if(firstInLoop>=loopStart-.00001&&firstInLoop<loopEnd-.000001)return firstInLoop;
  }
  return position;
 }
}

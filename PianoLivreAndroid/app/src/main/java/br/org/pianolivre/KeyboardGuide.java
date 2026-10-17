package br.org.pianolivre;

import java.util.*;

/**
 * Visual grand-piano accompaniment, ALWAYS both staves.
 *
 * Selection of right/left/both belongs to teaching (Wait and MIDI demonstration);
 * it must NEVER filter the notes displayed by the optional virtual keyboard.
 * At time t, display pitches sounding in the written interval [onset, onset+length).
 * Ties are already consolidated by Score.read. The keyboard is visual; no audio.
 */
public final class KeyboardGuide {
 private static final int P45_LOW=21, P45_HIGH=108;
 private final ArrayList<Score.Event> ordered;
 private final double[] maximumEnd;
 private final int leaves;private int visitedNodes;
 /** Local diagnostic: tree nodes inspected by the last query, excluding onset binary search. */
 public int visitedNodesLastQuery(){return visitedNodes;}

 public static final class Frame {
  public final Set<Integer> right, left, both;
  public final Map<Integer,Integer> rightVelocity,leftVelocity;
  public final Map<Integer,Double> rightRemaining,leftRemaining;
  Frame(Set<Integer> right,Set<Integer> left,Map<Integer,Integer> rv,Map<Integer,Integer> lv,
    Map<Integer,Double> rr,Map<Integer,Double> lr){
   rightVelocity=Collections.unmodifiableMap(rv);leftVelocity=Collections.unmodifiableMap(lv);
   rightRemaining=Collections.unmodifiableMap(rr);leftRemaining=Collections.unmodifiableMap(lr);
   this.right=Collections.unmodifiableSet(right);
   this.left=Collections.unmodifiableSet(left);
   Set<Integer> union=new HashSet<>(right);
   union.addAll(left);
   this.both=Collections.unmodifiableSet(union);
  }
 }
 public static final class Range {
  public final int low,high;
  public final boolean outsidePiano;
  Range(int low,int high,boolean outside){this.low=low;this.high=high;this.outsidePiano=outside;}
 }
 public KeyboardGuide(Score score){
  ordered=new ArrayList<>(score==null?Collections.emptyList():score.events);
  ordered.sort(Comparator.comparingDouble(e->e.beat));
  int size=1;while(size<ordered.size())size*=2;leaves=size;
  maximumEnd=new double[size*2];Arrays.fill(maximumEnd,Double.NEGATIVE_INFINITY);
  for(int i=0;i<ordered.size();i++){Score.Event e=ordered.get(i);maximumEnd[size+i]=e.beat+e.length;}
  for(int i=size-1;i>0;i--)maximumEnd[i]=Math.max(maximumEnd[i*2],maximumEnd[i*2+1]);
 }
 public Frame at(double beat){
  Set<Integer> right=new HashSet<>(),left=new HashSet<>();
  Map<Integer,Integer> rv=new HashMap<>(),lv=new HashMap<>();
  Map<Integer,Double> rr=new HashMap<>(),lr=new HashMap<>();
  visitedNodes=0;
  if(!Double.isFinite(beat))return new Frame(right,left,rv,lv,rr,lr);
  // Limit to onsets at/before playhead, then prune expired subtrees. A single
  // very long bass note no longer keeps every intervening short note in the scan.
  int lo=0,hi=ordered.size();
  while(lo<hi){
   int m=(lo+hi)>>>1;
   if(ordered.get(m).beat<=beat+1e-7)lo=m+1;
   else hi=m;
  }
  collect(1,0,leaves,lo,beat,right,left,rv,lv,rr,lr);
  return new Frame(right,left,rv,lv,rr,lr);
 }
 private void collect(int node,int start,int end,int limit,double beat,Set<Integer> right,Set<Integer> left,
   Map<Integer,Integer> rv,Map<Integer,Integer> lv,Map<Integer,Double> rr,Map<Integer,Double> lr){
  visitedNodes++;
  if(start>=limit||maximumEnd[node]<=beat+1e-7)return;
  if(end-start==1){
   Score.Event e=ordered.get(start);
   if(e.length>0&&e.beat<=beat+1e-7&&e.beat+e.length>beat+1e-7){
    boolean upper=e.staff==1;(upper?right:left).add(e.midi);(upper?rv:lv).merge(e.midi,e.velocity,Math::max);
    double remaining=Math.max(0,Math.min(1,(e.beat+e.length-beat)/e.length));
    (upper?rr:lr).merge(e.midi,remaining,Math::max);
   }
   return;
  }
  int middle=(start+end)>>>1;
  collect(node*2,start,middle,limit,beat,right,left,rv,lv,rr,lr);
  collect(node*2+1,middle,end,limit,beat,right,left,rv,lv,rr,lr);
 }
 /**
  * Display ALL score-sounding and user-pressed P-45 keys, not just the next
  * attack. Minimum window is ~4 octaves, but it expands for wide two-hand spans.
  */
 public static Range range(Set<Integer> sounding,Set<Integer> pressed){
  int min=127,max=0;boolean outside=false;
  for(Set<Integer> src:Arrays.asList(sounding,pressed)){
   if(src==null)continue;
   for(int n:src){
    if(n<P45_LOW||n>P45_HIGH){outside=true;continue;}
    min=Math.min(min,n);max=Math.max(max,n);
   }
  }
  if(min>max)return new Range(48,96,outside);
  int wantedMin=Math.max(P45_LOW,min-2),wantedMax=Math.min(P45_HIGH,max+2);
  int span=Math.min(87,Math.max(48,wantedMax-wantedMin));
  int first=Math.max(P45_LOW,Math.min(P45_HIGH-span,(wantedMin+wantedMax-span)/2));
  // Ensure no active part of either hand is clipped by the viewport.
  if(first>wantedMin)first=wantedMin;
  if(first+span<wantedMax)first=wantedMax-span;
  first=Math.max(P45_LOW,Math.min(P45_HIGH-span,first));
  return new Range(first,first+span,outside);
 }
}

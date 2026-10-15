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
 private final double[] prefixMaxEnd;

 public static final class Frame {
  public final Set<Integer> right, left, both;
  Frame(Set<Integer> right,Set<Integer> left){
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
  prefixMaxEnd=new double[ordered.size()];
  double max=Double.NEGATIVE_INFINITY;
  for(int i=0;i<ordered.size();i++){
   Score.Event e=ordered.get(i);
   max=Math.max(max,e.beat+e.length);
   prefixMaxEnd[i]=max;
  }
 }
 public Frame at(double beat){
  Set<Integer> right=new HashSet<>(),left=new HashSet<>();
  if(!Double.isFinite(beat))return new Frame(right,left);
  // Latest onset not later than the playhead. Sorting + prefix maxima avoids
  // redrawing/scanning whole long scores at every Android animation tick.
  int lo=0,hi=ordered.size();
  while(lo<hi){
   int m=(lo+hi)>>>1;
   if(ordered.get(m).beat<=beat+1e-7)lo=m+1;
   else hi=m;
  }
  for(int i=lo-1;i>=0&&prefixMaxEnd[i]>beat+1e-7;i--){
   Score.Event e=ordered.get(i);
   if(e.length>0&&e.beat<=beat+1e-7&&e.beat+e.length>beat+1e-7){
    if(e.staff==1)right.add(e.midi);
    else left.add(e.midi);
   }
  }
  return new Frame(right,left);
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

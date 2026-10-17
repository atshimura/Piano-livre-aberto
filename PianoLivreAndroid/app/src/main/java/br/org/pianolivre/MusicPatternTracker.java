package br.org.pianolivre;

import java.util.*;

/**
 * Conservative offline rolling analysis of MONOPHONIC isolated note attacks.
 * No guessed timing thresholds, key signatures or functional tonality.
 */
public final class MusicPatternTracker {
 private static final int MAX_ATTACKS=13; // chromatic octave is 13 pitches
 private final ArrayDeque<Integer> recent=new ArrayDeque<>();
 public void clear(){recent.clear();}
 public int size(){return recent.size();}
 public List<MusicSequenceAnalyzer.Finding> attack(int pitch,boolean isolated){
  if(pitch<0||pitch>127||!isolated){clear();return Collections.emptyList();}
  recent.addLast(pitch);
  while(recent.size()>MAX_ATTACKS)recent.removeFirst();
  List<Integer> sample=new ArrayList<>(recent);
  // Prefer the longest exact suffix; a complete scale is not reinterpreted as
  // a smaller arpeggio merely because its last few notes happen to coincide.
  for(int count=sample.size();count>=4;count--){
   List<MusicSequenceAnalyzer.Finding> findings=
     MusicSequenceAnalyzer.analyze(sample.subList(sample.size()-count,sample.size()));
   if(!findings.isEmpty())return findings;
  }
  return Collections.emptyList();
 }
}

package br.org.pianolivre;

import java.util.List;

/**
 * Selects the smallest existing Descobrir engraving camera containing ALL
 * pitches currently displayed. This is a visual decision only; it never
 * changes MusicXML, MIDI recognition, or the retained musical result.
 */
final class DiscoveryFraming {
 private DiscoveryFraming(){}

 static int tierFor(List<Integer> visiblePitches){
  if(visiblePitches==null||visiblePitches.isEmpty())return 0;
  int tier=0;
  for(int pitch:visiblePitches){
   if(pitch<21||pitch>108)return 2;
   if(pitch<36||pitch>84)tier=1;
  }
  return tier;
 }
}

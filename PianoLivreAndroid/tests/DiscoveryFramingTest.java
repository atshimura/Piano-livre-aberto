package br.org.pianolivre;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Pure-JVM regression for the reversible camera policy, independent of UI. */
public final class DiscoveryFramingTest {
 private static int checks=0;
 private static void is(int expected,List<Integer> notes,String explanation){
  int actual=DiscoveryFraming.tierFor(notes);checks++;
  if(actual!=expected)throw new AssertionError(explanation+": expected "+expected+" got "+actual);
 }
 public static void main(String[] args){
  is(0,null,"null/no visible result");
  is(0,Collections.emptyList(),"reset");
  is(0,Arrays.asList(60),"C4 normal");
  is(0,Arrays.asList(60,64,67),"C-major chord");
  is(0,Arrays.asList(36,84),"normal range endpoints together");
  is(1,Arrays.asList(21,108),"real-piano endpoints simultaneously");
  is(1,Arrays.asList(35,36,60),"one low note requires expansion");
  is(1,Arrays.asList(60,85),"one high note requires expansion");
  is(2,Arrays.asList(0,127),"full-MIDI-range endpoints");
  is(2,Arrays.asList(60,127),"extreme with common held at once");
  is(1,Arrays.asList(21,64),"real-piano low extreme with common");
  is(0,Arrays.asList(64),"new common note restores normal");
  is(1,Arrays.asList(21,108),"retained extreme is still tier 1");
  is(0,Arrays.asList(60,62,64,65,67,69,71,72),"new scale restores normal");
  is(2,Arrays.asList(0,60,127),"all visible notes must fit");
  is(0,Arrays.asList(72),"normal after full extreme");
  for(int i=0;i<20;i++){
   is(1,Arrays.asList(21,108),"repeat extended "+i);
   is(0,Arrays.asList(60),"repeat normal "+i);
  }
  System.out.println("DISCOVERY_FRAMING_REVERSIBLE_PASS checks="+checks+" range=0..127 repeated=20");
 }
}

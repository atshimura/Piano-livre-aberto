package br.org.pianolivre;
import java.util.*;

/** No grading from wall-clock time; notes advance only on exact MIDI pitch. */
public final class MusicLabPracticeSequence {
 private final int[] expected;
 private int matched,extras;
 public MusicLabPracticeSequence(int[] melody){
  if(melody==null||melody.length<2||melody.length>40)
   throw new IllegalArgumentException("Sequência inválida");
  expected=melody.clone();
  for(int x:expected)if(x<0||x>127)throw new IllegalArgumentException("Altura MIDI inválida");
 }
 /** An observation is a NEW physical Note On, not a snapshot of held keys. */
 public enum Observation {CORRECT, EXTRA, IGNORED}
 /**
  * Requires exactly one newly attacked pitch and one held key across both
  * sources. Simultaneous/overlapping or echoed notes are indeterminate, not
  * counted as correct or extra. Demonstration output never counts as practice.
  * The callers detect a new attack on virtual state transition or MIDI Note On.
  */
 public Observation observe(Collection<Integer> newAttacks,Collection<Integer> midiHeld,
                            Collection<Integer> virtualHeld,boolean demoPlaying){
  if(newAttacks==null||midiHeld==null||virtualHeld==null)
   throw new IllegalArgumentException("Estado de entrada ausente");
  if(complete()||demoPlaying||newAttacks.size()!=1||midiHeld.size()+virtualHeld.size()!=1)
   return Observation.IGNORED;
  Integer pitch=newAttacks.iterator().next();
  if(pitch==null||pitch<0||pitch>127||
     !(midiHeld.contains(pitch)||virtualHeld.contains(pitch)))
   return Observation.IGNORED;
  return attack(pitch)?Observation.CORRECT:Observation.EXTRA;
 }
 public boolean attack(int midi){
  if(midi<0||midi>127||complete())return false;
  if(midi==expected[matched]){matched++;return true;}
  extras++;return false;
 }
 public int correct(){return matched;}
 public int extras(){return extras;}
 public int total(){return expected.length;}
 public int next(){return complete()?-1:expected[matched];}
 public boolean complete(){return matched==expected.length;}
 public void reset(){matched=0;extras=0;}
}

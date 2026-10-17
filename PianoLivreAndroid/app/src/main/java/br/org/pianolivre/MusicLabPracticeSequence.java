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

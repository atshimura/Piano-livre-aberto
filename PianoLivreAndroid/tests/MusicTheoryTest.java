import br.org.pianolivre.MusicTheory;
import java.util.*;

/** Deterministic JVM regression suite, no Android runtime or network. */
public final class MusicTheoryTest {
 private static int checks=0;
 private static void ok(boolean yes,String label){checks++;if(!yes)throw new AssertionError(label);}
 private static MusicTheory.Chord first(Integer... pitches){
  List<MusicTheory.Chord> chords=MusicTheory.recognize(Arrays.asList(pitches));
  ok(!chords.isEmpty(),"chord missing: "+Arrays.toString(pitches));return chords.get(0);
 }
 public static void main(String[] args){
  ok(MusicTheory.noteName(60).equals("Dó4"),"middle C");
  ok(MusicTheory.internationalName(69).equals("A4"),"A440");
  ok(MusicTheory.noteName(61).equals("Dó♯4"),"sharp");
  ok(MusicTheory.intervalName(7).equals("quinta justa"),"fifth");
  ok(MusicTheory.intervalName(6).contains("trítono"),"enharmonic ambiguity");
  ok(MusicTheory.intervalName(19).contains("19 semitons"),"compound");
  ok(first(57,60,64).symbol.equals("Am"),"A minor");
  ok(first(60,64,67).symbol.equals("C"),"C major");
  ok(first(64,67,72).symbol.equals("C"),"inverted C major");
  ok(first(64,67,72).inversion.equals("1ª inversão"),"inversion");
  ok(first(55,59,62,65).symbol.equals("G7"),"dominant seventh");
  ok(first(60,64,67,71).symbol.equals("Cmaj7"),"major seventh");
  ok(first(57,60,64,67).symbol.equals("Am7"),"minor seventh");
  ok(first(60,63,66,69).symbol.equals("Cdim7"),"diminished seventh bass");
  ok(first(60,64,68).symbol.equals("Caug"),"augmented");
  ok(first(60,62,67).symbol.equals("Csus2"),"suspended bass root");
  ok(MusicTheory.recognize(Arrays.asList(60,62,67)).size()>1,"show suspended alternatives");
  ok(first(60,64,67,72,76).symbol.equals("C"),"octave doubling");
  ok(MusicTheory.recognize(Arrays.asList(60)).isEmpty(),"single note is not chord");
  ok(MusicTheory.recognize(Arrays.asList(60,64)).isEmpty(),"two notes not chord");
  ok(MusicTheory.recognize(Arrays.asList(60,61,62,63,64)).isEmpty(),"clusters not guessed");
  ok(MusicTheory.recognize(Collections.emptyList()).isEmpty(),"empty input");
  boolean rejects=false;try{MusicTheory.noteName(128);}catch(IllegalArgumentException expected){rejects=true;}
  ok(rejects,"invalid MIDI note must fail");
  System.out.println("MUSIC_THEORY_TEST_PASS checks="+checks);
 }
}

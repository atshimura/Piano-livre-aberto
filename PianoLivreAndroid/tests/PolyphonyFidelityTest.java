import br.org.pianolivre.Score;
import java.io.*;
import java.util.*;

/** Independent, hand-enumerated oracle for a synthetic, debug-only MusicXML. */
public final class PolyphonyFidelityTest {
 private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 private static String sig(Score.Event e){
  return String.format(Locale.ROOT,"%.0f:%d:%s:%d:%.0f:%s",
    e.beat,e.staff,e.voiceId,e.midi,e.length,e.playbackAttack?"A":"T");
 }
 public static void main(String[] args)throws Exception{
  File fixture=new File("app/src/debug/assets/scores/engraving-polyphony-synthetic.musicxml");
  Score score;try(FileInputStream stream=new FileInputStream(fixture)){score=Score.read(stream,fixture.getName());}
  // Enumerated independently from the file parser and the graphical engine.
  String[] expected={
   "0:1:1:72:1:A","0:1:1:74:1:A","0:1:1:76:1:A",
   "1:1:1:77:1:A","1:1:1:79:1:A","2:1:1:81:2:A",
   "0:1:2:67:4:A","0:2:5:48:4:A","0:2:5:52:4:A","0:2:5:55:4:A",
   "4:1:1:67:2:A","6:1:1:69:2:A","4:1:2:67:1:A",
   "6:1:2:71:2:A","4:2:5:48:4:A",
   "4:2:6:52:1:A","5:2:6:53:1:A","6:2:6:55:2:A",
   "8:1:1:72:1:A","9:1:1:74:1:A","10:1:1:76:1:A",
   "11:1:1:79:1:A","8:1:2:64:2:A","10:1:2:65:2:A",
   "8:2:5:48:4:A",
   "12:1:1:79:2:T","14:1:1:77:2:A",
   "12:1:2:64:1:A","14:1:2:64:2:A","12:2:5:43:4:A"
  };
  List<String> actual=new ArrayList<>();
  for(Score.Event e:score.writtenEvents){
   actual.add(sig(e));
   check(e.originalPartIndex()==0,"Original part must remain P1");
   check(e.sourceNoteIndex>=0,"Native source note index missing");
  }
  Arrays.sort(expected);Collections.sort(actual);
  check(Arrays.asList(expected).equals(actual),"Written heads differ from independent expected MusicXML: "+actual);
  check(score.writtenEvents.size()==30,"Exactly 30 written noteheads");
  check(score.events.size()==29,"One tie continuation must not add an attack");
  check(Math.abs(score.end()-16)<1e-8,"Four 4/4 measures");
  check(score.at(0,1).size()==4&&score.at(0,2).size()==3,"Independent right/left simultaneous chords");
  check(score.at(4,1).size()==2,"Polyphonic G4 unison must retain two events");
  check(score.at(4,2).size()==2,"Lower staff has two independent voices");
  check(score.at(12,1).size()==1,"Tied G5 continuation must not become an Espera attack");
  Score.Event extended=null;
  for(Score.Event e:score.events)if(e.beat==11&&e.midi==79)extended=e;
  check(extended!=null&&extended.length==3,"Tied G5 must sound continuously through measure boundary");
  check(score.writtenEvents.stream().filter(e->!e.playbackAttack).count()==1,"Only legitimate tie continuation has no new MIDI attack");
  System.out.println("POLYPHONY_FIDELITY_JVM_PASS written=30 attacks=29 unisonVoices=2 tieContinuation=1");
 }
}

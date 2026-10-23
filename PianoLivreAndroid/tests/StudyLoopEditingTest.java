import br.org.pianolivre.StudyLoopEditing;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public final class StudyLoopEditingTest {
 static int count=0;
 static void ok(boolean yes,String message){count++;if(!yes)throw new AssertionError(message);}
 static void eq(double actual,double expected,String why){ok(Math.abs(actual-expected)<.0000001,why);}
 public static void main(String[] args)throws Exception{
  StudyLoopEditing.Bounds a=StudyLoopEditing.mark(6,0,Double.MAX_VALUE,20,true);
  eq(a.start,6,"mark start at current clock");
  ok(!a.looping(),"single boundary cannot invent loop");
  a=StudyLoopEditing.mark(9,a.start,a.end,20,false);
  eq(a.start,6,"chosen start retained");
  eq(a.end,9,"chosen end retained");
  ok(a.looping(),"valid end enables loop");
  a=StudyLoopEditing.mark(14,a.start,a.end,20,true);
  eq(a.start,14,"new start must be respected");
  ok(!a.looping()&&a.end==Double.MAX_VALUE,"stale end cannot become new start");
  a=StudyLoopEditing.mark(12,a.start,a.end,20,false);
  eq(a.start,12,"backwards selection ordered safely");
  eq(a.end,14,"backwards selection preserves useful range");
  a=StudyLoopEditing.mark(500,a.start,a.end,20,false);
  eq(a.end,20,"end limited to score duration");
  ok(StudyLoopEditing.mark(0,0,Double.MAX_VALUE,20,false)==null,
    "zero-length interval refused");
  ok(StudyLoopEditing.mark(12,12,Double.MAX_VALUE,20,false)==null,
    "same beat cannot trigger a zero-duration loop");
  ok(StudyLoopEditing.mark(Double.NaN,0,Double.MAX_VALUE,20,true)==null,
    "nonfinite clock refused");
  ok(StudyLoopEditing.mark(1,0,Double.MAX_VALUE,0,false)==null,
    "invalid duration refused");
  String ui=Files.readString(Path.of("app/src/main/java/br/org/pianolivre/PianoScreens.java"),
    StandardCharsets.UTF_8);
  ok(ui.contains("a.stopPlayback();\n    a.loopStart=selected.start;a.loopEnd=selected.end;"),
    "the actual menu must send Note Off before changing MIDI loop bounds");
  ok(ui.contains("a.resetWrittenRoute();\n    a.saveSession();"),
    "manual loop editing must rearm repeat navigation and persist boundaries");
  System.out.println("STUDY_LOOP_EDITING_PASS checks="+count);
 }
}

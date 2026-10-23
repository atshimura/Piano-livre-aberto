import br.org.pianolivre.Score;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Export authoritative parser results to the read-only browser regression. */
public class StudyProfileCorpus {
 public static void main(String[] args)throws Exception{
  List<Path> paths=new ArrayList<>();
  for(String dir:new String[]{"app/src/main/assets/scores","app/src/debug/assets/scores","tests/fixtures/motor"})
   try(var stream=Files.walk(Path.of(dir))){stream.filter(p->p.toString().matches(".*\\.(mxl|musicxml|xml)$")).forEach(paths::add);}
  Collections.sort(paths);
  for(Path path:paths){
   byte[] before=Files.readAllBytes(path);Score score=Score.read(new ByteArrayInputStream(before),path.getFileName().toString());
   System.out.println("FILE\t"+path+"\t"+Base64.getEncoder().encodeToString(score.sourceXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
   for(Score.Event e:score.writtenEvents){
    System.out.println("NOTE\t"+e.beat+"\t"+e.length+"\t"+e.midi+"\t"+e.staff+"\t"+e.measure+"\t"+e.voiceId+"\t"+e.writtenStep+"\t"+e.writtenOctave+"\t"+e.writtenAlter+"\t"+e.sourceNoteIndex+"\t"+e.originalPartIndex());
   }
   // Attack events must retain the authored MIDI pitch and onset; a tied
   // playback event may have a longer duration than one written head.
   for(Score.Event e:score.events)if(score.writtenEvents.stream().noneMatch(w->w.midi==e.midi&&Math.abs(w.beat-e.beat)<1e-6&&w.staff==e.staff))throw new AssertionError("Unwritten playback attack: "+path);
   if(!Arrays.equals(before,Files.readAllBytes(path)))throw new AssertionError("Source mutated: "+path);
  }
 }
}

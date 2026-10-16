import br.org.pianolivre.Score;
import java.io.*;
import java.util.*;

/** Shared synthetic fixtures, machine-readable reference from the production parser. */
public final class MusicXmlFixtureInventory {
 public static void main(String[] args)throws Exception{
  File[] files=new File("tests/fixtures/motor").listFiles((d,n)->n.endsWith(".xml"));
  if(files==null||files.length==0)throw new AssertionError("Fixtures missing");
  Arrays.sort(files,Comparator.comparing(File::getName));
  for(File file:files){
   Score score;try(InputStream in=new FileInputStream(file)){score=Score.read(in,file.getName());}
   if(!score.sourceXml.equals(new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8)))throw new AssertionError("Source XML changed");
   for(String warning:score.importWarnings)System.out.printf("WARNING\t%s\t%s%n",file.getName(),warning);
   System.out.printf(Locale.US,"END\t%s\t%.9f%n",file.getName(),score.end());
   for(Score.Event e:score.events)System.out.printf(Locale.US,"NOTE\t%s\t%.9f\t%d\t%d\t%.9f%n",file.getName(),e.beat,e.midi,e.staff,e.length);
   TreeSet<Double> onsets=new TreeSet<>();for(Score.Event e:score.events)onsets.add(e.beat);
   for(double beat:onsets)for(Score.Event e:score.at(beat,0))System.out.printf(Locale.US,"ATTACK\t%s\t%.9f\t%d\t%d\t%.9f%n",file.getName(),e.beat,e.midi,e.staff,e.length);
   for(Score.TempoChange t:score.tempoChanges)System.out.printf(Locale.US,"TEMPO\t%s\t%.9f\t%.9f%n",file.getName(),t.beat,t.bpm);
  }
 }
}

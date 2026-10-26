import br.org.pianolivre.*;
import java.io.*;
import java.util.*;

/** One row for each true sounding-onset event from the Android music model. */
public final class ScoreInventory {
 public static void main(String[] args)throws Exception{
  File root=new File("app/src/main/assets/scores");
  File[] scores=root.listFiles((d,n)->n.endsWith(".mxl"));
  if(scores==null||scores.length!=10)throw new IOException("Ten scores are required");
  Arrays.sort(scores,Comparator.comparing(File::getName));
  for(File input:scores){
   Score score;
   try(InputStream in=new FileInputStream(input)){score=Score.read(in,input.getName());}
   for(Score.Event event:score.events)if(event.length>0)
    System.out.printf(Locale.US,"%s\t%.8f\t%d\t%d\t%.8f%n",
      input.getName(),event.beat,event.midi,event.staff,event.length);
  }
 }
}

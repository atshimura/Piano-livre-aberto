import br.org.pianolivre.Score;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** JVM regression smoke test for the built-in MusicXML library. */
public class ScoreSmokeTest {
 public static void main(String[] args) throws Exception {
  File dir=new File("app/src/main/assets/scores");
  File[] scores=dir.listFiles((d,n)->n.toLowerCase().endsWith(".mxl"));
  if(scores==null || scores.length!=10)throw new AssertionError("Expected 10 MXL scores, got "+(scores==null?0:scores.length));
  for(File file:scores){
   Score score;
   try(InputStream in=new FileInputStream(file)){score=Score.read(in,file.getName());}
   if(score.events.isEmpty()||score.measures<=0||score.end()<=0)throw new AssertionError("Empty score: "+file.getName());
   double previous=-1;
   int right=0,left=0;
   for(Score.Event event:score.events){
    if(event.beat<previous || event.length<0 || event.midi<0 || event.midi>127)throw new AssertionError("Bad MIDI event: "+file.getName());
    if(event.staff==1)right++;else left++;
    previous=event.beat;
   }
   if(right==0||left==0)throw new AssertionError("Missing separate hands: "+file.getName());
   System.out.println("OK "+file.getName()+" | measures="+score.measures+" notes="+score.events.size()+" right="+right+" left="+left);
  }
  byte[] malicious=("<?xml version=\"1.0\"?><!DOCTYPE score-partwise [<!ENTITY xx SYSTEM \"file:///etc/passwd\">]><score-partwise>&xx;</score-partwise>").getBytes(StandardCharsets.UTF_8);
  try{Score.read(new ByteArrayInputStream(malicious),"test.xml");throw new AssertionError("Unsafe DOCTYPE was accepted");}catch(IOException expected){System.out.println("OK malicious DTD rejected");}
 }
}
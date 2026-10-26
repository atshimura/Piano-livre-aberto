import br.org.pianolivre.StudyInfoText;
import java.util.Locale;

public final class StudyInfoTextTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  Locale.setDefault(new Locale("pt","BR"));
  StudyInfoText label=new StudyInfoText();
  String first=label.update(true,120,1,0,0,false);
  check(first.equals("♫ 120 BPM × 100% · posição 0.0"),"preserve original label and locale");
  int formatted=1;String previous=first;
  // Forty transport ticks: formatting at most ten times; no transport time is skipped.
  for(int i=1;i<40;i++){
   String next=label.update(true,120,1,i*.05,i*25,false);
   if(next!=previous)formatted++;
   previous=next;
  }
  check(formatted==10,"40 transport ticks require ten formatted labels");
  String seek=label.update(true,120,1,0,976,true);
  check(seek.endsWith("posição 0.0"),"explicit seek updates immediately inside cadence");
  String tempo=label.update(true,60,1,.1,977,false);
  check(tempo.startsWith("♫ 60 BPM"),"tempo-map change bypasses cadence");
  String speed=label.update(true,60,.25,.2,978,false);
  check(speed.contains("25%"),"speed change bypasses cadence");
  String empty=label.update(false,0,.25,0,979,false);
  check(empty.startsWith("♫ — BPM"),"score removal bypasses cadence");
  String reset=label.update(true,90,1,2,0,false);
  check(reset.endsWith("posição 2.0"),"clock reset refreshes");
  check(label.update(true,90,1,3,1,true).endsWith("posição 3.0"),"paused manual update is immediate");
  System.out.println("PASS StudyInfoTextTest: cadence, seek, tempo map, speed, score, locale and clock reset");
 }
}

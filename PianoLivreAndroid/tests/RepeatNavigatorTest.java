import br.org.pianolivre.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class RepeatNavigatorTest {
 static int checks=0;
 static void ok(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
 static void eq(double a,double b,String why){ok(Math.abs(a-b)<1e-7,why+": "+a+" != "+b);}
 static String measure(int n,String notes,String bars){
  return "<measure number='"+n+"'><attributes><divisions>1</divisions>"+
   "<time><beats>4</beats><beat-type>4</beat-type></time></attributes>"+
   notes+bars+"</measure>";
 }
 static String note(String step,int dur){
  return "<note><pitch><step>"+step+"</step><octave>4</octave></pitch><duration>"+dur+"</duration></note>";
 }
 static String forward(){return "<barline location='left'><repeat direction='forward'/></barline>";}
 static String backward(int times){return "<barline location='right'><repeat direction='backward' times='"+times+"'/></barline>";}
 static Score parse(String measures)throws Exception{
  String source="<score-partwise version='4.0'><part-list><score-part id='P1'><part-name>Piano</part-name></score-part></part-list>"+
   "<part id='P1'>"+measures+"</part></score-partwise>";
  return Score.read(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)),"repeats.musicxml");
 }
 public static void main(String[] args)throws Exception{
  Score s=parse(measure(1,note("C",4),"")+measure(2,forward()+note("D",4),"")+
   measure(3,note("E",4),backward(3))+measure(4,note("F",4),""));
  ok(s.repeatSections.size()==1,"expected one repeated section");
  Score.RepeatSection r=s.repeatSections.get(0);
  eq(r.start,4,"starts at 2nd measure");eq(r.end,12,"ends after 3rd");
  ok(r.times==3,"three plays");
  RepeatNavigator nav=new RepeatNavigator();
  ok(nav.crossing(s,4,11.5)==null,"cannot cross a not yet reached bar");
  ok(nav.crossing(s,11.5,12)==r,"should cross at exact end");
  ok(nav.shouldJump(r),"first play repeats");
  ok(nav.shouldJump(r),"second play repeats");
  ok(!nav.shouldJump(r),"third play continues");
  ok(!nav.shouldJump(r),"does not repeat again");
  nav.reset();
  ok(nav.shouldJump(r),"fresh play resets loop counter");
  // The last note of a written repeat must jump BACK, even though its
  // target beat is numerically smaller than the current note's beat.
  RepeatNavigator wait=new RepeatNavigator();
  eq(wait.nextWaitTarget(s,8,0,0,Double.MAX_VALUE),4,
   "Espera first repeat must return to written start");
  eq(wait.nextWaitTarget(s,8,0,0,Double.MAX_VALUE),4,
   "Espera second repeat must return to written start");
  eq(wait.nextWaitTarget(s,8,0,0,Double.MAX_VALUE),12,
   "Espera third traversal must proceed to next measure");
  RepeatNavigator manual=new RepeatNavigator();
  eq(manual.nextWaitTarget(s,8,0,5,9),5,
   "manual study AB repeat must override written repetition");
  ok(manual.visits(r)==0,"manual repeat must not consume written count");

  Score implicit=parse(measure(1,note("C",4),"")+measure(2,note("D",4),backward(2)));
  ok(implicit.repeatSections.size()==1,"implicit opening barline is song start");
  eq(implicit.repeatSections.get(0).start,0,"implicit start beat zero");
  eq(implicit.repeatSections.get(0).end,8,"implicit repeat ends at 8");
  Score endings=parse(measure(1,forward()+note("C",4),"")+
   measure(2,note("D",4),"<barline><ending number='1' type='stop'/>"+backward(2).replace("<barline location='right'>","").replace("</barline>","")+"</barline>"));
  ok(!endings.playbackWarnings.isEmpty(),"alternate endings must be announced as unsupported");
  ok(endings.repeatSections.isEmpty(),"must not replay incorrect simple-repeats for first/second endings");
  Score noRepeat=parse(measure(1,note("C",4),""));
  ok(noRepeat.repeatSections.isEmpty(),"plain score unaffected");
  System.out.println("PASS RepeatNavigatorTest "+checks+" assertions: explicit/implicit starts, times, stop and unsupported endings");
 }
}

import br.org.pianolivre.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Deliberately independent MusicXML navigation fixtures, not just simple MXL files. */
public final class PerformanceRouteTest {
 static int checks=0;
 static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void eq(double got,double want,String m){ok(Math.abs(got-want)<.000001,m+" got "+got+" expected "+want);}
 static String note(String step){
  return "<note><pitch><step>"+step+"</step><octave>4</octave></pitch><duration>4</duration></note>";
 }
 static String measure(int n,String left,String center,String right){
  return "<measure number='"+n+"'><attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>"+
   left+center+right+"</measure>";
 }
 static String start(){return "<barline location='left'><repeat direction='forward'/></barline>";}
 static String back(int n){return "<barline location='right'><repeat direction='backward' times='"+n+"'/></barline>";}
 static String ending(String position,int number,String type){
  return "<barline location='"+position+"'><ending number='"+number+"' type='"+type+"'/></barline>";
 }
 static String direction(String words){
  return "<direction><direction-type><words>"+words+"</words></direction-type></direction>";
 }
 static String symbol(String tag){
  return "<direction><direction-type><"+tag+"/></direction-type></direction>";
 }
 static Score parse(String body)throws Exception{
  String src="<score-partwise version='4.0'><part-list><score-part id='P1'><part-name>Piano</part-name></score-part></part-list><part id='P1'>"+body+"</part></score-partwise>";
  return Score.read(new ByteArrayInputStream(src.getBytes(StandardCharsets.UTF_8)),"fixture.musicxml");
 }
 static void itinerary(Score score,int...expected){
  PerformanceRoute.Result route=PerformanceRoute.build(score);
  ok(route.supported(),"unexpected warnings: "+route.warnings);
  ok(route.visits.size()==expected.length,"visits "+route.visits.size()+" expected "+expected.length);
  for(int i=0;i<expected.length;i++)ok(route.visits.get(i).measure==expected[i],
   "visit["+i+"]: "+route.visits.get(i).measure+" vs "+expected[i]);
 }
 static void firstSecondEnding()throws Exception{
  Score s=parse(
   measure(1,start(),note("C"),"")+
   measure(2,"",note("D"),"")+
   measure(3,ending("left",1,"start"),note("E"),
      ending("right",1,"stop")+back(2))+
   measure(4,ending("left",2,"start"),note("F"),
      ending("right",2,"discontinue"))+
   measure(5,"",note("G"),""));
  itinerary(s,0,1,2,0,1,3,4);
  PerformanceRoute.Result route=PerformanceRoute.build(s);
  PerformanceRoute.Cursor wait=new PerformanceRoute.Cursor(route);
  eq(wait.nextWait(s,0,0),4,"wait to 2nd written measure");
  eq(wait.nextWait(s,4,0),8,"wait to 1st ending");
  eq(wait.nextWait(s,8,0),0,"wait repeats to opening");
  eq(wait.nextWait(s,0,0),4,"wait on repeat pass 2");
  eq(wait.nextWait(s,4,0),12,"wait skips first ending on second pass");
  eq(wait.nextWait(s,12,0),16,"wait next normal measure");
  eq(wait.nextWait(s,16,0),16,"wait at last note stops");
  PerformanceRoute.Cursor restored=new PerformanceRoute.Cursor(route);
  ok(restored.restore(3,0),"restore second passage of first measure");
  eq(restored.nextWait(s,0,0),4,"restored pass next onsets");
  eq(restored.nextWait(s,4,0),12,"restored pass skips first ending");
  ok(!restored.restore(999,0),"invalid saved pass must be rejected");
  ok(!restored.restore(2,0),"saved pass must match the written beat");
  PerformanceRoute.Cursor clock=new PerformanceRoute.Cursor(route);
  clock.align(0);
  PerformanceRoute.Movement m=clock.tick(s,0,12500,1);
  // 7 written measure visits * 4 quarter notes at fallback 100 BPM = 16.8s.
  ok(!m.finished,"12.5s not enough to end seven bars");
  // At 0 -> 4 -> 8 -> 12 (jump to 0) ->4 -> 8 (skip to 12) ...
  boolean backward=false,skip=false;
  for(PerformanceRoute.Move step:m.moves){
   if(step.seek&&Math.abs(step.beat-0)<.000001)backward=true;
   if(step.seek&&Math.abs(step.beat-12)<.000001)skip=true;
  }
  ok(backward&&skip,"clock must traverse both repeat and skipped ending via explicit seek");
 }
 static void nested()throws Exception{
  Score score=parse(
   measure(1,start(),note("C"),"")+
   measure(2,start(),note("D"),"")+
   measure(3,"",note("E"),back(2))+
   measure(4,"",note("F"),back(2))+
   measure(5,"",note("G"),""));
  itinerary(score,0,1,2,1,2,3,0,1,2,1,2,3,4);
 }
 static void dcFine()throws Exception{
  Score score=parse(
   measure(1,"",note("C"),"")+
   measure(2,"",direction("Fine")+note("D"),"")+
   measure(3,"",direction("D.C. al Fine")+note("E"),""));
  itinerary(score,0,1,2,0,1);
 }
 static void dsCoda()throws Exception{
  Score score=parse(
   measure(1,"",symbol("segno")+note("C"),"")+
   measure(2,"",direction("To Coda")+note("D"),"")+
   measure(3,"",direction("D.S. al Coda")+note("E"),"")+
   measure(4,"",symbol("coda")+note("F"),"")+
   measure(5,"",note("G"),""));
  itinerary(score,0,1,2,0,1,3,4);
 }
 static void unsafeRefusal()throws Exception{
  Score missing=parse(measure(1,"",direction("D.S.")+note("C"),""));
  PerformanceRoute.Result failed=PerformanceRoute.build(missing);
  ok(!failed.supported()&&failed.visits.isEmpty(),"do not guess missing Segno target");
  Score overlap=parse(measure(1,start(),note("C"),"")+
   measure(2,"",direction("D.C.")+note("D"),back(2)));
  ok(!PerformanceRoute.build(overlap).supported(),"mixed repeats with D.C. require explicit rules");
  Score incomplete=parse(measure(1,start(),note("C"),""));
  ok(!PerformanceRoute.build(incomplete).supported(),"unterminated repeat must be rejected");
 }
 public static void main(String[] args)throws Exception{
  firstSecondEnding();nested();dcFine();dsCoda();unsafeRefusal();
  System.out.println("PASS PerformanceRouteTest "+checks+
   " assertions covering 1st/2nd endings, nested loops, DC/Fine, DS/Coda, clocks and Wait jumps");
 }
}

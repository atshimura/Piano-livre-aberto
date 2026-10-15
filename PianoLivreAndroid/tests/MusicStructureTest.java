import br.org.pianolivre.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * MusicXML test-score fixtures for independent voices, rests, pickups, ties,
 * and simple written repeats. A fixture is deliberately NOT one of the 10
 * simple library songs, so regression is visible before users import scores.
 */
public final class MusicStructureTest {
 static int checks=0;
 static void ok(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static void eq(double actual,double want,String context){
  ok(Math.abs(actual-want)<1e-7,context+" got "+actual+" expected "+want);
 }
 static String head(){
  return "<score-partwise version='4.0'><part-list>"+
   "<score-part id='P1'><part-name>Piano</part-name></score-part></part-list>"+
   "<part id='P1'>";
 }
 static Score parse(String measures)throws Exception{
  String src=head()+measures+"</part></score-partwise>";
  return Score.read(new ByteArrayInputStream(src.getBytes(StandardCharsets.UTF_8)),"music.xml");
 }
 static String notes(int octave,String letter,int dur,int staff,int voice,String tie){
  return "<note><pitch><step>"+letter+"</step><octave>"+octave+"</octave></pitch>"+
   "<duration>"+dur+"</duration><voice>"+voice+"</voice><staff>"+staff+"</staff>"+
   (tie.isEmpty()?"":"<tie type='"+tie+"'/>")+"</note>";
 }
 static String attrs(){
  return "<attributes><divisions>4</divisions><time><beats>4</beats><beat-type>4</beat-type></time></attributes>";
 }
 static void polyphonyAndTies()throws Exception{
  String first="<measure number='1'>"+attrs()+
   notes(4,"C",8,1,1,"start")+notes(4,"D",8,1,1,"")+
   "<backup><duration>16</duration></backup>"+
   "<note><rest/><duration>4</duration><voice>2</voice><staff>2</staff></note>"+
   notes(3,"G",12,2,2,"")+"</measure>";
  String second="<measure number='2'>"+attrs()+
   notes(4,"C",4,1,1,"stop")+notes(4,"E",12,1,1,"")+"</measure>";
  Score score=parse(first+second);
  ok(score.events.size()==4,"merged tie should not attack twice");
  eq(score.durationBeats,8,"two measures duration");
  eq(score.end(),8,"written score duration");
  eq(score.events.get(0).beat,0,"first RH onset");
  eq(score.events.get(0).length,5,"tied duration extends across bar");
  eq(score.events.get(1).beat,1,"LH voice starts after rest");
  eq(score.events.get(1).length,3,"LH voice duration");
  eq(score.events.get(2).beat,2,"upper voice continues independently");
  eq(score.events.get(3).beat,5,"second measure upper voice");
  ok(score.at(4,0).isEmpty(),"tie continuation should not retrigger");
  ok(score.at(1,2).size()==1&&score.at(1,1).isEmpty(),"right/left filtering independent");
 }
 static void pickupsAndEndRest()throws Exception{
  Score pickup=parse("<measure number='1' implicit='yes'>"+attrs()+
   notes(4,"E",2,1,1,"")+"</measure>"+
   "<measure number='2'>"+attrs()+notes(4,"F",4,1,1,"")+
   "<note><rest/><duration>12</duration></note></measure>");
  eq(pickup.events.get(1).beat,.5,"pickup occupies half a quarter");
  eq(pickup.end(),4.5,"last rest extends written timeline");
  eq(pickup.durationBeats,4.5,"timeline duration includes rest");
 }
 static void unsupportedReturns()throws Exception{
  Score dc=parse("<measure number='1'>"+attrs()+
   "<direction><direction-type><words>D.C. al Fine</words></direction-type></direction>"+
   notes(4,"C",16,1,1,"")+"</measure>");
  ok(dc.playbackWarnings.contains("D.C./D.S./Coda/Fine"),
   "advanced repeat jump must be disclosed");
  ok(dc.repeatSections.isEmpty(),"do not falsely expand unsupported jumps");
  Score dcAlone=parse("<measure number='1'>"+attrs()+
   "<direction><direction-type><words>D.C.</words></direction-type></direction>"+
   notes(4,"C",16,1,1,"")+"</measure>");
  ok(dcAlone.playbackWarnings.contains("D.C./D.S./Coda/Fine"),
   "standalone D.C. without fine must be marked unsupported");

 }
 public static void main(String[] args)throws Exception{
  polyphonyAndTies();
  pickupsAndEndRest();
  unsupportedReturns();
  System.out.println("PASS MusicStructureTest "+checks+
   " assertions: multivoice, rests, tied notes, pickup, unsupported jumps");
 }
}

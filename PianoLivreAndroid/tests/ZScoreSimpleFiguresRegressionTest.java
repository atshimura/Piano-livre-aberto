package br.org.pianolivre;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** ZAI-010: pitched MusicXML notes have simple, undotted figures only. */
public final class ZScoreSimpleFiguresRegressionTest {
 private static int checks;
 private static void ok(boolean value,String name){
  checks++;if(!value)throw new AssertionError(name);
 }
 private static MidiTakeRecorder.Event on(int pitch,long time,int order){
  return new MidiTakeRecorder.Event(0x90,pitch,88,time,7,order,false);
 }
 private static MidiTakeRecorder.Event off(int pitch,long time,int order){
  return new MidiTakeRecorder.Event(0x80,pitch,0,time,7,order,false);
 }
 private static CreationScoreGenerator.Generation make(List<MidiTakeRecorder.Event> events,long until)
   throws Exception{
  return CreationScoreGenerator.generate(events,
    Collections.singletonList(new MidiTakeRecorder.Segment(0,until)),"Figuras simples");
 }
 private static Document xml(String data)throws Exception{
  DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
  factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
  return factory.newDocumentBuilder().parse(
    new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
 }
 private static void assertSimple(CreationScoreGenerator.Generation g,int count,String name)throws Exception{
  ok(g.notes.size()==count,name+": original MIDI attacks preserved");
  Document doc=xml(g.xml);
  NodeList nodes=doc.getElementsByTagName("note");int seen=0,hiddenRests=0;
  for(int i=0;i<nodes.getLength();i++){
   Element node=(Element)nodes.item(i);
   if(node.getElementsByTagName("rest").getLength()>0){
    hiddenRests++;
    ok("no".equals(node.getAttribute("print-object")),name+": rest timing invisible");
    continue;
   }
   seen++;
   ok(node.getElementsByTagName("dot").getLength()==0,
     name+": no dot on pitched note "+seen);
   Element type=(Element)node.getElementsByTagName("type").item(0);
   String value=type.getTextContent();
   ok(Arrays.asList("whole","half","quarter","eighth","16th").contains(value),
     name+": legal simple note figure "+value);
  }
  ok(hiddenRests>0,name+": preserve hidden rests including blank bass");
  ok(seen==count,name+": exactly one notehead per MIDI attack");
  ok(ScoreXmlComposer.isCurrentDerivedXml(g.xml),name+": ZAI-010 marker");
  ok(!g.xml.contains("<staccato")&&!g.xml.contains("<slur")&&!g.xml.contains("<tie"),
    name+": no automatic expressive marks");
  Score read=Score.read(new ByteArrayInputStream(g.xml.getBytes(StandardCharsets.UTF_8)),name);
  ok(read.events.size()==count,name+": Study keeps attack count");
 }
 public static void main(String[] args)throws Exception{
  long q=500_000_000L;
  // A 3/16 attack used to become a dotted eighth. Now one plain eighth.
  CreationScoreGenerator.Generation three=make(Arrays.asList(
    on(60,0,0),off(60,375_000_000L,1)),q);
  assertSimple(three,1,"3/16");
  ok(three.notes.get(0).durationNs()==375_000_000L,
    "3/16: original Note Off unchanged");
  ok(!three.xml.contains("<note><pitch><step>C</step><octave>4</octave></pitch>"+
    "<duration>720</duration>"),
    "3/16: dotted duration not emitted");

  // Previously a dotted half for six sixteenth-cells.
  CreationScoreGenerator.Generation six=make(Arrays.asList(
    on(60,0,0),off(60,750_000_000L,1)),q*2);
  assertSimple(six,1,"6/16");
  ok(six.notes.get(0).durationNs()==750_000_000L,
    "6/16: original physical duration unchanged");

  // Three simultaneous notes must remain one C major chord.
  CreationScoreGenerator.Generation chord=make(Arrays.asList(
    on(60,0,0),on(64,0,1),on(67,0,2),
    off(60,q,3),off(64,q,4),off(67,q,5)),q*2);
  assertSimple(chord,3,"C chord");
  ok(chord.xml.contains("<chord/>"),"Chord keeps stacked pitches");

  // A long pressed key must never create 600 new note attacks.
  CreationScoreGenerator.Generation longHeld=make(Arrays.asList(
    on(60,0,0),off(60,300_000_000_000L,1)),300_100_000_000L);
  assertSimple(longHeld,1,"five minute hold");
  ok(longHeld.notes.get(0).durationNs()==300_000_000_000L,
    "5min: raw note persists, written duration simplified");

  // Source-derived older scores must be flagged, never silently reused.
  ok(!ScoreXmlComposer.isCurrentDerivedXml(
      chord.xml.replace(ScoreXmlComposer.GENERATOR_MARKER,"")),
    "old PLS2 derivative requires explicit regeneration");
  System.out.println("ZScoreSimpleFiguresRegressionTest: "+checks+" checks PASS");
 }
}

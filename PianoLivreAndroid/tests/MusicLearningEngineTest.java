package br.org.pianolivre;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public final class MusicLearningEngineTest {
 private static int checks=0;
 private static void yes(boolean okay,String why){checks++;if(!okay)throw new AssertionError(why);}
 private static void equal(Object a,Object b,String why){yes(Objects.equals(a,b),why+" expected="+b+" got="+a);}
 private static void verifyXml(MusicLearningEngine.Exercise e,String figure,int count)throws Exception{
  String xml=MusicLearningScore.xml(e);
  DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
  f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
  f.setFeature("http://xml.org/sax/features/external-general-entities",false);
  f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
  Document doc=f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  equal(doc.getDocumentElement().getNodeName(),"score-partwise","MusicXML root");
  NodeList notes=doc.getElementsByTagName("note");
  equal(notes.getLength(),count,"XML exact attack count for "+e.id);
  NodeList kinds=doc.getElementsByTagName("type");
  yes(kinds.getLength()>0,"type missing");
  equal(kinds.item(0).getTextContent(),figure,"note duration type");
  equal(doc.getElementsByTagName("divisions").item(0).getTextContent(),"4","divisions constant");
  yes(!xml.contains("<dot/>"),"learning example should start with plain figure");
  yes(!xml.contains("<!DOCTYPE"),"network DTD prohibited");
 }
 public static void main(String[] args)throws Exception {
  equal(MusicLearningEngine.MODULE_COUNT,5,"five planned chapters");
  int total=0;
  for(int m=0;m<MusicLearningEngine.MODULE_COUNT;m++){
   List<MusicLearningEngine.Exercise> all=MusicLearningEngine.lessons(m);
   yes(!all.isEmpty(),"empty chapter "+m);total+=all.size();
   for(MusicLearningEngine.Exercise item:all)yes(item.id!=null&&item.pitches.length>0,"invalid "+item.id);
  }
  equal(total,28,"total actual lessons, never hardcode 32% demo data");
  int[] progress={0,0,0,0,0};
  yes(MusicLearningEngine.unlocked(0,progress),"notes available");
  for(int i=1;i<5;i++)yes(!MusicLearningEngine.unlocked(i,progress),"locked too early "+i);
  for(int i=0;i<8;i++)progress[0]|=1<<i;
  yes(MusicLearningEngine.unlocked(1,progress),"intervals after notes");
  yes(!MusicLearningEngine.unlocked(2,progress),"rhythm locked until interval completion");
  equal(MusicLearningEngine.completeCount(0,progress),8,"note mastery");
  equal(MusicLearningEngine.nextIndex(0,progress),0,"review already completed");
  progress[1]=63;
  yes(MusicLearningEngine.unlocked(2,progress),"rhythm unlocked");
  progress[2]=63;yes(MusicLearningEngine.unlocked(3,progress),"chords unlocked");
  progress[3]=15;yes(MusicLearningEngine.unlocked(4,progress),"reading unlocked");
  progress[4]=15;
  equal(MusicLearningEngine.completeCount(4,progress),4,"reading completion");

  MusicLearningEngine.Session note=new MusicLearningEngine.Session(MusicLearningEngine.lesson(0,2)); // Sol4=67
  equal(note.attack(66,Arrays.asList(66),false),MusicLearningEngine.Feedback.EXTRA,"wrong pitch");
  yes(!note.complete()&&note.extras()==1,"wrong note never completes");
  equal(note.attack(67,Arrays.asList(66,67),false),MusicLearningEngine.Feedback.EXTRA,"extra key ambiguity");
  equal(note.attack(67,Arrays.asList(67),true),MusicLearningEngine.Feedback.IGNORED,"demonstration forbidden");
  equal(note.attack(67,Arrays.asList(67),false),MusicLearningEngine.Feedback.COMPLETE,"fresh exact pitch");
  yes(note.complete(),"note completed");
  equal(note.attack(67,Arrays.asList(67),false),MusicLearningEngine.Feedback.IGNORED,"no second credit");
  note.reset();yes(!note.complete()&&note.extras()==0,"reset clear");

  MusicLearningEngine.Session sequence=new MusicLearningEngine.Session(MusicLearningEngine.lesson(1,2));
  int[] seq={67,65,64};
  equal(sequence.attack(65,Arrays.asList(65),false),MusicLearningEngine.Feedback.EXTRA,"out of order");
  for(int i=0;i<seq.length;i++){
   equal(sequence.attack(seq[i],Arrays.asList(seq[i]),false),
     i==seq.length-1?MusicLearningEngine.Feedback.COMPLETE:MusicLearningEngine.Feedback.CORRECT,
     "order "+i);
  }
  yes(sequence.complete()&&sequence.correct()==3,"full sequence");
  MusicLearningEngine.Session repeated=new MusicLearningEngine.Session(
   new MusicLearningEngine.Exercise("same","Dó-Dó","Repeat",MusicLearningEngine.SEQUENCE,0,2,60,60));
  equal(repeated.attack(60,Arrays.asList(60),false),MusicLearningEngine.Feedback.CORRECT,"first Do");
  equal(repeated.attack(60,Arrays.asList(60),false),MusicLearningEngine.Feedback.COMPLETE,"repeat needs new attack from caller");

  MusicLearningEngine.Session chord=new MusicLearningEngine.Session(MusicLearningEngine.lesson(3,0));
  equal(chord.attack(60,Arrays.asList(60),false),MusicLearningEngine.Feedback.PROGRESS,"begin chord");
  equal(chord.attack(64,Arrays.asList(60,64),false),MusicLearningEngine.Feedback.PROGRESS,"add third");
  equal(chord.attack(67,Arrays.asList(60,64,67,69),false),MusicLearningEngine.Feedback.EXTRA,"extra invalidates chord");
  yes(!chord.complete(),"extra must not count as chord");
  equal(chord.attack(67,Arrays.asList(60,64,67),false),MusicLearningEngine.Feedback.COMPLETE,"chord exact held set");
  yes(chord.complete(),"chord done");
  MusicLearningEngine.Session rhythm=new MusicLearningEngine.Session(MusicLearningEngine.lesson(2,0));
  equal(rhythm.attack(67,Arrays.asList(67),false),MusicLearningEngine.Feedback.IGNORED,"rhythm quiz not graded via MIDI");
  equal(rhythm.identifyFigure(0),MusicLearningEngine.Feedback.EXTRA,"wrong figure");
  equal(rhythm.identifyFigure(2),MusicLearningEngine.Feedback.COMPLETE,"correct quarter");
  equal(rhythm.identifyFigure(2),MusicLearningEngine.Feedback.IGNORED,"no duplicate credit");

  yes(MusicLearningEngine.catalog(MusicLearningEngine.NOTE,"dó").size()>0,"note name");
  yes(MusicLearningEngine.catalog(MusicLearningEngine.CHORD,"maior").size()>0,"chord name");
  yes(MusicLearningEngine.catalog(MusicLearningEngine.SEQUENCE,"escala").size()>0,"scale name");
  yes(MusicLearningEngine.catalog(MusicLearningEngine.SEQUENCE,"arpejo").size()>0,"arpeggio name");
  MusicLabCatalog.Entry one=MusicLearningEngine.catalog(MusicLearningEngine.CHORD,"Dó maior").get(0);
  MusicLearningEngine.Exercise free=MusicLearningEngine.choose(one,1);
  equal(free.kind,MusicLearningEngine.CHORD,"choose chord");
  equal(free.hand,1,"hand reference");
  equal(free.pitches[0],one.notes[0],"hand cannot transpose existing pitches");
  yes(MusicLearningScore.xml(free).contains("<sign>F</sign>"),"bass clef left");
  MusicLearningEngine.Exercise both=MusicLearningEngine.choose(one,2);
  yes(MusicLearningScore.xml(both).contains("<staves>2</staves>"),"two staves for both");
  verifyXml(MusicLearningEngine.lesson(0,2),"quarter",1);
  verifyXml(MusicLearningEngine.lesson(3,0),"whole",3);
  verifyXml(MusicLearningEngine.lesson(1,2),"quarter",3);
  for(int i=0;i<MusicLearningEngine.RHYTHM_NAMES.length;i++){
   MusicLearningEngine.Exercise e=new MusicLearningEngine.Exercise("t"+i,"Figura","test",MusicLearningEngine.RHYTHM,0,i,67);
   verifyXml(e,MusicLearningEngine.RHYTHM_TYPES[i],1);
  }
  System.out.println("ZAI015_LEARN_ENGINE_XML_PASS checks="+checks);
 }
}

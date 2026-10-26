import br.org.pianolivre.*;
import java.io.*;
import java.util.*;

/** Both-hands score-to-virtual-keyboard invariant and duration regression tests. */
public final class KeyboardGuideTest {
 static long assertions=0;
 static void ok(boolean pass,String text){assertions++;if(!pass)throw new AssertionError(text);}
 static Score.Event n(double onset,double len,int pitch,int staff){
  return new Score.Event(onset,len,pitch,staff,1,1);
 }
 static Set<Integer> set(int...notes){
  Set<Integer> out=new HashSet<>();for(int n:notes)out.add(n);return out;
 }
 static void equal(Set<Integer> a,Set<Integer> b,String msg){ok(a.equals(b),msg+": expected "+b+", got "+a);}
 static void synthetic(){
  Score s=new Score();
  s.events.add(n(0,4,60,1)); // right hand held across four quarter notes
  s.events.add(n(0,1,48,2)); // left hand
  s.events.add(n(1,1,52,2)); // left hand changes while right still sounding
  s.events.add(n(2,2,64,1)); // chord with held right 60
  s.events.add(n(2,2,43,2)); // lower left hand
  s.events.add(n(4,1,60,1)); // repeated pitch has a NEW onset at 4
  KeyboardGuide g=new KeyboardGuide(s);
  equal(g.at(0).right,set(60),"right at beat zero");
  equal(g.at(0).left,set(48),"left at beat zero");
  equal(g.at(.5).both,set(48,60),"both half-beat");
  equal(g.at(1).both,set(52,60),"old left must release at beat one");
  equal(g.at(1.5).right,set(60),"long right sustain");
  equal(g.at(2).both,set(60,64,43),"chord with independent hands");
  equal(g.at(3.99).both,set(60,64,43),"right+left sustain");
  equal(g.at(4).both,set(60),"previous chord releases, new note begins");
  equal(g.at(5).both,set(),"end of notes");
  // All three UI hand choices must see the same full grand-staff frame.
  for(int hand=0;hand<3;hand++){
   // The visual guide does not even accept a hand-filter parameter.
   equal(g.at(2).both,set(60,64,43),"both visual notes regardless learning hand "+hand);
  }
  KeyboardGuide.Range r=KeyboardGuide.range(set(21,108),set(60,100));
  ok(r.low<=21&&r.high>=108,"entire P-45 span must be visible when both hands demand it");
  r=KeyboardGuide.range(set(43,96),set(52));
  ok(r.low<=43&&r.high>=96,"bass and treble must fit in one window");
  r=KeyboardGuide.range(set(48,60),set(108));
  ok(r.low<=48&&r.high>=108,"user-pressed key outside score must remain visible");
  r=KeyboardGuide.range(set(120),set(60));
  ok(r.outsidePiano,"out-of-instrument pitch must be flagged");
 }
 static Set<Integer> brute(Score s,double beat,int hand){
  Set<Integer> result=new HashSet<>();
  for(Score.Event e:s.events){
   if((hand==0||(hand==1&&e.staff==1)||(hand==2&&e.staff!=1))
      &&e.length>0&&e.beat<=beat+1e-7&&e.beat+e.length>beat+1e-7)
      result.add(e.midi);
  }
  return result;
 }
 static void corpus()throws Exception{
  File dir=new File("app/src/main/assets/scores");
  File[] files=dir.listFiles((d,name)->name.endsWith(".mxl"));
  ok(files!=null&&files.length==10,"expected 10 pieces in the internal library");
  Arrays.sort(files,Comparator.comparing(File::getName));
  long timestamps=0;
  for(File file:files){
   Score score;
   try(InputStream input=new FileInputStream(file)){score=Score.read(input,file.getName());}
   KeyboardGuide guide=new KeyboardGuide(score);
   TreeSet<Double> beats=new TreeSet<>();beats.add(0.0);beats.add(score.end());
   for(Score.Event e:score.events){
    beats.add(e.beat);
    beats.add(e.beat+e.length);
    beats.add(Math.max(0,e.beat+.25*Math.min(e.length,1)));
    if(e.length>.005)beats.add(Math.max(e.beat,e.beat+e.length-.003));
   }
   for(double beat:beats){
    KeyboardGuide.Frame frame=guide.at(beat);
    Set<Integer> right=brute(score,beat,1),left=brute(score,beat,2),both=brute(score,beat,0);
    equal(frame.right,right,file+" right beat "+beat);
    equal(frame.left,left,file+" left beat "+beat);
    equal(frame.both,both,file+" both beat "+beat);
    KeyboardGuide.Range r=KeyboardGuide.range(frame.both,Collections.emptySet());
    for(int pitch:frame.both){
     if(pitch>=21&&pitch<=108)ok(pitch>=r.low&&pitch<=r.high,"lost a note in keyboard window at "+file+" beat "+beat);
    }
    timestamps++;
   }
   System.out.println("PASS "+file.getName()+" events="+score.events.size()+" checkpoints="+beats.size());
  }
  System.out.println("PASS keyboard guide "+timestamps+" temporal checkpoints across ten MusicXML scores");
 }
 public static void main(String[] args)throws Exception{
  synthetic();corpus();
  System.out.println("PASS KeyboardGuideTest "+assertions+" assertions; both hands, sustain, onsets, chords, full keyboard coverage.");
 }
}

import br.org.pianolivre.Score;
import java.io.*;
import java.util.*;
/** Hand-enumerated expectations, independent of OSMD and source traversal. */
public final class ComplexNotationFidelityTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static Score read(String name)throws Exception{
  File f=new File("app/src/debug/assets/scores/engraving-"+name+"-06.musicxml");
  try(InputStream in=new FileInputStream(f)){return Score.read(in,f.getName());}
 }
 static void voice(Score s,String v,int staff,double[] beats,int[] pitches,double[] lengths){
  List<Score.Event> events=new ArrayList<>();
  for(Score.Event e:s.writtenEvents)if(e.staff==staff&&e.voiceId.equals(v))events.add(e);
  check(events.size()==beats.length,"Voice cardinality "+v+": "+events.size());
  for(int i=0;i<beats.length;i++){
   Score.Event e=events.get(i);
   check(Math.abs(e.beat-beats[i])<1e-8&&e.midi==pitches[i]&&Math.abs(e.length-lengths[i])<1e-8,
    "Voice "+v+" event "+i+" actual="+e.beat+":"+e.midi+":"+e.length);
   check(e.playbackAttack,"Unexpected artificial continuation");
  }
 }
 public static void main(String[] args)throws Exception{
  String[] names={"accidentals","beams","tuplets","grace","adversarial"};
  int[] counts={22,11,14,6,12};
  for(int i=0;i<names.length;i++){
   Score s=read(names[i]);check(s.writtenEvents.size()==counts[i],names[i]+" written count");
   check(s.events.size()==counts[i],names[i]+" attacks; grace must not enter MIDI/Espera");
   check(Math.abs(s.end()-(i==0?16:4))<1e-8,names[i]+" duration");
   System.out.println("TASK06_JVM "+names[i]+" written="+counts[i]+" attacks="+s.events.size());
  }
  Score a=read("accidentals");
  voice(a,"1",1,new double[]{0,1,2,3,4,5,6,7,8,9,10,11,12,12,12,13,14,15},
   new int[]{73,73,77,77,72,72,71,71,79,77,78,77,85,85,87,77,78,77},
   new double[]{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1});
  String[] steps={"C","D","E","F","B","C","C","B","F","G","F","F"};
  int[] alters={1,-1,1,0,1,0,-1,0,2,-2,1,0};
  List<Score.Event> top=new ArrayList<>();for(Score.Event e:a.writtenEvents)if(e.staff==1)top.add(e);
  for(int i=0;i<steps.length;i++)check(top.get(i).writtenStep.equals(steps[i])&&top.get(i).writtenAlter==alters[i],"Lost enharmonic spelling "+i);
  Score b=read("beams");
  voice(b,"1",1,new double[]{0,.25,.5,.75,1,1.5,3},new int[]{72,74,76,77,79,81,83},new double[]{.25,.25,.25,.25,.5,.5,1});
  voice(b,"2",1,new double[]{0,1,2},new int[]{64,65,67},new double[]{1,1,2});
  Score t=read("tuplets");
  voice(t,"1",1,new double[]{0,1./3,2./3,1,1.4,1.8,2.2,2.6,3},new int[]{72,74,76,72,74,76,77,79,81},new double[]{1./3,1./3,1./3,.4,.4,.4,.4,.4,1});
  voice(t,"2",1,new double[]{0,1./3,2./3,1},new int[]{60,62,64,67},new double[]{1./3,1./3,1./3,3});
  Score g=read("grace");
  voice(g,"1",1,new double[]{0,1,2,2,3},new int[]{76,81,83,86,84},new double[]{1,1,1,1,1});
  check(g.sourceXml.contains("<grace slash=\"yes\"")&&g.sourceXml.contains("<grace slash=\"no\""),"Ornamental notation must remain in XML");
  Score c=read("adversarial");
  voice(c,"1",1,new double[]{0,1./3,2./3,1,1.25,1.5,2,3},new int[]{72,74,76,78,79,81,70,73},new double[]{1./3,1./3,1./3,.25,.25,.5,1,1});
  voice(c,"2",1,new double[]{0,1,2},new int[]{64,65,67},new double[]{1,1,2});
  System.out.println("COMPLEX_NOTATION_FIDELITY_JVM_PASS enarmonia=4pares graceAttacks=0 ratios=3:2,5:4");
 }
}

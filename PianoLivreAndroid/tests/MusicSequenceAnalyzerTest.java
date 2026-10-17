import br.org.pianolivre.MusicSequenceAnalyzer;
import java.util.*;

public final class MusicSequenceAnalyzerTest {
 private static int checks=0;
 private static void yes(boolean good,String explanation){
  checks++;if(!good)throw new AssertionError(explanation);
 }
 private static List<MusicSequenceAnalyzer.Finding> a(int... pitches){
  ArrayList<Integer> list=new ArrayList<>();for(int x:pitches)list.add(x);
  return MusicSequenceAnalyzer.analyze(list);
 }
 private static void contains(String name,int... pitches){
  List<MusicSequenceAnalyzer.Finding> found=a(pitches);
  yes(found.stream().anyMatch(x->x.description.equals(name)),
   "not recognized "+name+" "+Arrays.toString(pitches));
 }
 public static void main(String[] args){
  contains("maior",60,62,64,65,67,69,71,72);
  contains("menor natural",57,59,60,62,64,65,67,69);
  contains("menor harmônica",57,59,60,62,64,65,68,69);
  contains("menor melódica (forma ascendente)",57,59,60,62,64,66,68,69);
  contains("dórica",62,64,65,67,69,71,72,74);
  contains("frígia",64,65,67,69,71,72,74,76);
  contains("lídia",65,67,69,71,72,74,76,77);
  contains("mixolídia",67,69,71,72,74,76,77,79);
  contains("lócria",71,72,74,76,77,79,81,83);
  contains("pentatônica maior",60,62,64,67,69,72);
  contains("pentatônica menor",57,60,62,64,67,69);
  contains("tríade maior",60,64,67,72);
  contains("tríade menor",57,60,64,69);
  contains("tríade diminuta",60,63,66,72);
  contains("tríade aumentada",60,64,68,72);
  contains("acorde dominante com sétima",55,59,62,65,67);
  contains("acorde maior com sétima maior",60,64,67,71,72);
  contains("acorde menor com sétima",57,60,64,67,69);
  contains("acorde meio-diminuto com sétima",59,62,65,69,71);
  yes(a(72,71,69,67,65,64,62,60).get(0).direction.equals("descendente"),"descending major");
  yes(a(60,62,64).isEmpty(),"incomplete scale must not be guessed");
  yes(a(60,64,60,67,72).isEmpty(),"nonmonotonic ambiguous melody");
  yes(a(60,64,67,73).isEmpty(),"span larger than octave");
  yes(a(60,64,67,71,72,74).isEmpty(),"more than octave");
  yes(a(60,64,67,72,127).isEmpty(),"unrecognized additional note");
  yes(a(60,64,67,72,128).isEmpty(),"invalid MIDI");
  yes(a(60,64,67,72,-1).isEmpty(),"invalid negative MIDI");
  yes(a().isEmpty(),"empty");
  yes(MusicSequenceAnalyzer.analyze(null).isEmpty(),"null");
  yes(a(60,60,64,67,72).isEmpty(),"repeated note not a complete arpeggio");
  System.out.println("MUSIC_SEQUENCE_ANALYZER_PASS checks="+checks);
 }
}

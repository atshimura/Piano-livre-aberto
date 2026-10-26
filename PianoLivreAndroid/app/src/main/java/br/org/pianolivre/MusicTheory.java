package br.org.pianolivre;

import java.util.*;

/**
 * Small deterministic, offline music-theory kernel shared by the Laboratory.
 * Chords are recognized by exact pitch-class sets, not inferred from a lone
 * note or from hypothetical omitted tones. Tonal function is NOT inferred.
 * Enharmonic spelling requires harmonic context and is not available here.
 */
public final class MusicTheory {
 private MusicTheory() {}
 private static final String[] PT={"Dó","Dó♯","Ré","Ré♯","Mi","Fá","Fá♯","Sol","Sol♯","Lá","Lá♯","Si"};
 private static final String[] EN={"C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B"};
 private static final String[] ROOT={"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
 private static final class Pattern {
  final String suffix,label;final int[] intervals;
  Pattern(String suffix,String label,int... intervals){this.suffix=suffix;this.label=label;this.intervals=intervals;}
 }
 private static final Pattern[] PATTERNS={
  new Pattern("","maior",0,4,7),new Pattern("m","menor",0,3,7),
  new Pattern("dim","diminuto",0,3,6),new Pattern("aug","aumentado",0,4,8),
  new Pattern("sus2","suspenso com segunda",0,2,7),
  new Pattern("sus4","suspenso com quarta",0,5,7),
  new Pattern("7","dominante com sétima",0,4,7,10),
  new Pattern("maj7","maior com sétima maior",0,4,7,11),
  new Pattern("m7","menor com sétima",0,3,7,10),
  new Pattern("m(maj7)","menor com sétima maior",0,3,7,11),
  new Pattern("m7b5","meio-diminuto",0,3,6,10),
  new Pattern("dim7","diminuto com sétima diminuta",0,3,6,9)
 };
 public static String noteName(int midi){
  check(midi);return PT[midi%12]+(midi/12-1);
 }
 public static String internationalName(int midi){
  check(midi);return EN[midi%12]+(midi/12-1);
 }
 private static void check(int midi){
  if(midi<0||midi>127)throw new IllegalArgumentException("Nota MIDI fora de 0..127");
 }
 public static String intervalName(int semitones){
  if(semitones<0)throw new IllegalArgumentException("Intervalo negativo");
  String[] names={"uníssono","segunda menor","segunda maior","terça menor",
   "terça maior","quarta justa","trítono (quarta aumentada/quinta diminuta)",
   "quinta justa","sexta menor","sexta maior","sétima menor","sétima maior","oitava justa"};
  if(semitones<=12)return names[semitones];
  return semitones+" semitons (intervalo composto)";
 }
 public static final class Chord {
  public final String symbol,description,inversion;
  public final int root,bass;
  Chord(String symbol,String description,String inversion,int root,int bass){
   this.symbol=symbol;this.description=description;this.inversion=inversion;
   this.root=root;this.bass=bass;
  }
 }
 /**
  * Returns all exact interpretations. First prefers the played bass as root;
  * later alternatives remain visible rather than claiming an unproved key.
  * Repeated notes and octave doublings never create extra chord tones.
  */
 public static List<Chord> recognize(Collection<Integer> notes){
  if(notes==null||notes.isEmpty())return Collections.emptyList();
  TreeSet<Integer> ordered=new TreeSet<>();
  boolean[] mask=new boolean[12];int distinct=0;
  for(Integer n:notes){
   if(n==null)throw new IllegalArgumentException("Nota nula");
   check(n);ordered.add(n);
  }
  for(int n:ordered)if(!mask[n%12]){mask[n%12]=true;distinct++;}
  if(distinct<3||distinct>4)return Collections.emptyList();
  int bass=ordered.first()%12;
  List<Chord> found=new ArrayList<>();
  for(Pattern pattern:PATTERNS){
   if(pattern.intervals.length!=distinct)continue;
   for(int root=0;root<12;root++){
    boolean matches=true;
    for(int interval:pattern.intervals)if(!mask[(root+interval)%12]){matches=false;break;}
    if(!matches)continue;
    int index=-1;
    for(int i=0;i<pattern.intervals.length;i++)
     if((root+pattern.intervals[i])%12==bass){index=i;break;}
    String inversion=index==0?"posição fundamental":index==1?"1ª inversão":
     index==2?"2ª inversão":"3ª inversão";
    found.add(new Chord(ROOT[root]+pattern.suffix,PT[root]+" "+pattern.label,inversion,root,bass));
   }
  }
  // Prefer roots supported by the bass, preserving deterministic ordering for
  // symmetric diminished/augmented and suspended pitch-class collections.
  found.sort((x,y)->{
   int a=x.root==bass?0:1,b=y.root==bass?0:1;
   return Integer.compare(a,b);
  });
  return Collections.unmodifiableList(found);
 }
}

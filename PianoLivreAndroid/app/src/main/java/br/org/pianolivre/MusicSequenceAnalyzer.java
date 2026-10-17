package br.org.pianolivre;

import java.util.*;

/**
 * Offline exact recognizer for complete melodic sequences. It recognizes
 * octave-complete scale/arpeggio PATTERNS, not tonal key or harmonic function.
 * No timing windows, note velocities, MIDI controller state or internet.
 */
public final class MusicSequenceAnalyzer {
 private MusicSequenceAnalyzer(){}
 private static final class Pattern {
  final String type,name;final int[] steps;
  Pattern(String type,String name,int... steps){this.type=type;this.name=name;this.steps=steps;}
 }
 private static final Pattern[] PATTERNS={
  new Pattern("Escala","maior",2,2,1,2,2,2,1),
  new Pattern("Escala","menor natural",2,1,2,2,1,2,2),
  new Pattern("Escala","menor harmônica",2,1,2,2,1,3,1),
  new Pattern("Escala","menor melódica (forma ascendente)",2,1,2,2,2,2,1),
  new Pattern("Escala","dórica",2,1,2,2,2,1,2),
  new Pattern("Escala","frígia",1,2,2,2,1,2,2),
  new Pattern("Escala","lídia",2,2,2,1,2,2,1),
  new Pattern("Escala","mixolídia",2,2,1,2,2,1,2),
  new Pattern("Escala","lócria",1,2,2,1,2,2,2),
  new Pattern("Escala","pentatônica maior",2,2,3,2,3),
  new Pattern("Escala","pentatônica menor",3,2,2,3,2),
  new Pattern("Escala","cromática",1,1,1,1,1,1,1,1,1,1,1,1),
  new Pattern("Arpejo","tríade maior",4,3,5),
  new Pattern("Arpejo","tríade menor",3,4,5),
  new Pattern("Arpejo","tríade diminuta",3,3,6),
  new Pattern("Arpejo","tríade aumentada",4,4,4),
  new Pattern("Arpejo","acorde dominante com sétima",4,3,3,2),
  new Pattern("Arpejo","acorde maior com sétima maior",4,3,4,1),
  new Pattern("Arpejo","acorde menor com sétima",3,4,3,2),
  new Pattern("Arpejo","acorde meio-diminuto com sétima",3,3,4,2)
 };
 public static final class Finding {
  public final String category,description,tonic,direction;
  Finding(String category,String description,String tonic,String direction){
   this.category=category;this.description=description;this.tonic=tonic;this.direction=direction;
  }
  public String display(){
   return category+": "+description+" iniciada em "+tonic+" · "+direction;
  }
 }
 public static List<Finding> analyze(List<Integer> attacks){
  if(attacks==null||attacks.size()<4||attacks.size()>40)return Collections.emptyList();
  boolean ascending=true,descending=true;
  for(Integer n:attacks)if(n==null||n<0||n>127)return Collections.emptyList();
  for(int i=1;i<attacks.size();i++){
   int a=attacks.get(i-1),b=attacks.get(i);
   if(b<=a)ascending=false;
   if(b>=a)descending=false;
  }
  if(!ascending&&!descending)return Collections.emptyList();
  List<Integer> pitches=new ArrayList<>(attacks);
  if(descending)Collections.reverse(pitches);
  if(pitches.get(pitches.size()-1)-pitches.get(0)!=12)return Collections.emptyList();
  int[] differences=new int[pitches.size()-1];
  for(int i=1;i<pitches.size();i++)differences[i-1]=pitches.get(i)-pitches.get(i-1);
  String tonic=MusicTheory.noteName(pitches.get(0));
  List<Finding> found=new ArrayList<>();
  for(Pattern p:PATTERNS){
   if(!Arrays.equals(differences,p.steps))continue;
   // Descending classical melodic minor generally differs from ascending.
   if(descending&&p.name.startsWith("menor melódica"))continue;
   found.add(new Finding(p.type,p.name,tonic,ascending?"ascendente":"descendente"));
  }
  return Collections.unmodifiableList(found);
 }
}

package br.org.pianolivre;

import java.util.*;

/** A local, deterministic and editable-free study reference (not a harmonic analysis). */
public final class MusicLabCatalog {
 private MusicLabCatalog(){}
 public static final class Entry {
  public final String kind,title,description;
  public final int[] notes;
  Entry(String kind,String title,String description,int... notes){
   this.kind=kind;this.title=title;this.description=description;this.notes=notes.clone();
  }
 }
 private static final String[] INTERVAL_NAMES={
   "uníssono", "segunda menor", "segunda maior", "terça menor",
   "terça maior", "quarta justa", "trítono", "quinta justa",
   "sexta menor", "sexta maior", "sétima menor", "sétima maior", "oitava justa"
 };
 private static final String[] CHORD_NAMES={
   "maior", "menor", "diminuto", "aumentado", "sus2", "sus4",
   "sétima dominante", "maior com sétima maior", "menor com sétima",
   "menor com sétima maior", "meio-diminuto", "diminuto com sétima diminuta"
 };
 private static final int[][] CHORDS={
  {0,4,7},{0,3,7},{0,3,6},{0,4,8},{0,2,7},{0,5,7},
  {0,4,7,10},{0,4,7,11},{0,3,7,10},{0,3,7,11},{0,3,6,10},{0,3,6,9}
 };
 private static final String[] SCALE_NAMES={
  "maior","menor natural","menor harmônica","menor melódica ascendente",
  "dórica","frígia","lídia","mixolídia","lócria",
  "pentatônica maior","pentatônica menor","cromática"
 };
 private static final int[][] SCALE_STEPS={
  {2,2,1,2,2,2,1},{2,1,2,2,1,2,2},{2,1,2,2,1,3,1},
  {2,1,2,2,2,2,1},{2,1,2,2,2,1,2},{1,2,2,2,1,2,2},
  {2,2,2,1,2,2,1},{2,2,1,2,2,1,2},{1,2,2,1,2,2,2},
  {2,2,3,2,3},{3,2,2,3,2},{1,1,1,1,1,1,1,1,1,1,1,1}
 };
 private static int[] fromIntervals(int root,int[] intervals){
  int[] n=new int[intervals.length];
  for(int i=0;i<intervals.length;i++)n[i]=root+intervals[i];
  return n;
 }
 private static int[] scale(int root,int[] steps){
  int[] n=new int[steps.length+1];n[0]=root;
  for(int i=0;i<steps.length;i++)n[i+1]=n[i]+steps[i];
  return n;
 }
 private static int[] invert(int[] notes,int inversion){
  int[] result=notes.clone();
  for(int i=0;i<inversion;i++){int first=result[0];System.arraycopy(result,1,result,0,result.length-1);result[result.length-1]=first+12;}
  return result;
 }
 public static List<Entry> all(){
  ArrayList<Entry> result=new ArrayList<>();
  // Preserve the five previously shipped educational examples first.
  result.add(new Entry("Acorde","Lá menor · Am","Tríade menor · fundamental",57,60,64));
  result.add(new Entry("Acorde","Dó maior · C","Tríade maior · fundamental",60,64,67));
  result.add(new Entry("Acorde","Fá maior · F","Tríade maior · fundamental",53,57,60));
  result.add(new Entry("Acorde","Sol com sétima · G7","Dominante com sétima menor",55,59,62,65));
  result.add(new Entry("Acorde","Ré menor · Dm","Tríade menor",50,53,57));
  for(int root=60;root<=71;root++){
   String name=MusicTheory.noteName(root)+" · "+MusicTheory.internationalName(root);
   result.add(new Entry("Nota",name,"Altura escrita e oitava, sem impor dedilhado",root));
   for(int i=0;i<INTERVAL_NAMES.length;i++)
    result.add(new Entry("Intervalo",name+" · "+INTERVAL_NAMES[i],"Distância cromática de "+i+" semitons",root,root+i));
   for(int q=0;q<CHORDS.length;q++){
    int[] notes=fromIntervals(root,CHORDS[q]);
    for(int inversion=0;inversion<notes.length;inversion++){
     String inv=inversion==0?"fundamental":inversion+"ª inversão";
     result.add(new Entry("Acorde",name+" · "+CHORD_NAMES[q]+" · "+inv,
       "Estrutura cromática · "+inv+". Grafia enarmônica depende da tonalidade.",invert(notes,inversion)));
    }
   }
   for(int q=0;q<SCALE_STEPS.length;q++)result.add(new Entry("Escala",name+" · "+SCALE_NAMES[q],
     "Padrão ascendente de uma oitava; tonalidade funcional e grafia enarmônica não inferidas",scale(root,SCALE_STEPS[q])));
   for(int q=0;q<CHORDS.length;q++){
    int[] chord=fromIntervals(root,CHORDS[q]);
    int[] notes=Arrays.copyOf(chord,chord.length+1);notes[notes.length-1]=root+12;
    result.add(new Entry("Arpejo",name+" · "+CHORD_NAMES[q],
      "Arpejo ascendente de uma oitava, em posição fundamental",notes));
   }
  }
  return Collections.unmodifiableList(result);
 }
 /** Case- and accent-insensitive search, accepting C# and C♯ without assuming a key. */
 private static String normalize(String value){
  if(value==null)return "";
  String folded=java.text.Normalizer.normalize(value.trim(),java.text.Normalizer.Form.NFD)
   .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
  return folded.replace("♯","#");
 }
 /** Preserve an existing selection when it is still visible. */
 public static int selectionIndex(List<Entry> entries,Entry prior){
  if(entries.isEmpty())return -1;
  int index=entries.indexOf(prior);
  return index<0?0:index;
 }
 public static List<Entry> filter(List<Entry> all,String text){
  String needle=normalize(text);
  if(needle.isEmpty())return new ArrayList<>(all);
  ArrayList<Entry> out=new ArrayList<>();
  for(Entry e:all)if(normalize(e.title).contains(needle)
   ||normalize(e.kind).contains(needle))out.add(e);
  return out;
 }
}

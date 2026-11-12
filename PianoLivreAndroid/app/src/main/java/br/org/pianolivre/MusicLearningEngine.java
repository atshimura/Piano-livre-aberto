package br.org.pianolivre;

import java.util.*;

/** Offline, deterministic pedagogy core. No Android, timers or persistence. */
public final class MusicLearningEngine {
 private MusicLearningEngine(){}
 public static final int NOTE=0,CHORD=1,SEQUENCE=2,RHYTHM=3;
 public static final String[] MODULES={
  "Notas na pauta","Movimentos e intervalos","Ritmo","Acordes","Leitura livre"
 };
 public static final String[] MODULE_DETAILS={
  "Reconheça notas na clave de sol","Subidas, descidas e saltos",
  "Figuras e pulsação","Formas e combinações","Pequenas melodias"
 };
 public static final String[] RHYTHM_NAMES={"Semibreve","Mínima","Semínima","Colcheia","Semicolcheia"};
 public static final String[] RHYTHM_TYPES={"whole","half","quarter","eighth","16th"};
 public static final int[] RHYTHM_LENGTHS={16,8,4,2,1};
 public static final int MODULE_COUNT=5;

 public static final class Exercise {
  public final String id,title,description;
  public final int kind,hand,rhythm;
  public final int[] pitches;
  public final boolean ordered;
  Exercise(String id,String title,String description,int kind,int hand,int rhythm,int...pitches){
   if(id==null||id.isEmpty()||pitches==null||pitches.length==0||pitches.length>24)
    throw new IllegalArgumentException("Exercício inválido");
   for(int p:pitches)if(p<0||p>127)throw new IllegalArgumentException("Altura MIDI inválida");
   this.id=id;this.title=title;this.description=description;this.kind=kind;
   this.hand=hand;this.rhythm=rhythm;this.pitches=pitches.clone();this.ordered=kind==SEQUENCE;
   if(kind<NOTE||kind>RHYTHM)throw new IllegalArgumentException("Tipo inválido");
   if(kind==RHYTHM&&(rhythm<0||rhythm>=RHYTHM_NAMES.length))
    throw new IllegalArgumentException("Figura inválida");
  }
  public int[] notes(){return pitches.clone();}
 }
 private static Exercise ex(String id,String title,int kind,int...notes){
  return new Exercise(id,title,"Toque no seu ritmo, sem limite de tempo",kind,0,2,notes);
 }
 private static final Exercise[][] LESSONS=new Exercise[][]{
  {ex("n1","Mi4",NOTE,64),ex("n2","Fá4",NOTE,65),
   ex("n3","Sol4",NOTE,67),ex("n4","Ré4",NOTE,62),
   ex("n5","Dó4",NOTE,60),ex("n6","Lá4",NOTE,69),
   ex("n7","Si4",NOTE,71),ex("n8","Dó5",NOTE,72)},
  {ex("i1","Dó–Ré",SEQUENCE,60,62),ex("i2","Mi–Sol",SEQUENCE,64,67),
   ex("i3","Sol–Fá–Mi",SEQUENCE,67,65,64),ex("i4","Dó–Mi–Sol",SEQUENCE,60,64,67),
   ex("i5","Lá–Sol–Fá",SEQUENCE,69,67,65),ex("i6","Mi–Fá–Sol–Dó",SEQUENCE,64,65,67,72)},
  {rhythm("r1",2),rhythm("r2",1),rhythm("r3",3),
   rhythm("r4",0),rhythm("r5",4),rhythm("r6",2)},
  {ex("a1","Dó maior",CHORD,60,64,67),ex("a2","Lá menor",CHORD,57,60,64),
   ex("a3","Fá maior",CHORD,65,69,72),ex("a4","Sol7",CHORD,55,59,62,65)},
  {ex("l1","Subida",SEQUENCE,60,62,64,65),ex("l2","Descida",SEQUENCE,69,67,65,64),
   ex("l3","Salto e retorno",SEQUENCE,60,67,64,60),
   ex("l4","Melodia curta",SEQUENCE,64,65,67,72,71,67)}
 };
 private static Exercise rhythm(String id,int figure){
  return new Exercise(id,RHYTHM_NAMES[figure],"Identifique a figura mostrada, sem avaliação de duração tocada",RHYTHM,0,figure,67);
 }
 public static List<Exercise> lessons(int module){
  if(module<0||module>=MODULE_COUNT)throw new IllegalArgumentException("Módulo inválido");
  return Collections.unmodifiableList(Arrays.asList(LESSONS[module].clone()));
 }
 public static int lessonCount(int module){return lessons(module).size();}
 public static Exercise lesson(int module,int index){
  if(index<0||index>=LESSONS[module].length)throw new IllegalArgumentException("Lição inválida");
  return LESSONS[module][index];
 }
 public static Exercise choose(MusicLabCatalog.Entry entry,int hand){
  if(entry==null)throw new IllegalArgumentException("Selecione conteúdo");
  int kind=entry.kind.equals("Nota")?NOTE:entry.kind.equals("Acorde")?CHORD:SEQUENCE;
  int[] original=entry.notes.clone();
  if(original.length==1)kind=NOTE;
  if(kind==SEQUENCE&&original.length<2)throw new IllegalArgumentException("Sequência curta");
  if(hand<0||hand>2)throw new IllegalArgumentException("Mão inválida");
  // "Mão" is a reference only; never transpose or infer which physical hand played.
  return new Exercise("free:"+entry.title,entry.title,entry.description,kind,hand,2,original);
 }
 public static List<MusicLabCatalog.Entry> catalog(int kind,String query){
  List<MusicLabCatalog.Entry> list=new ArrayList<>();
  for(MusicLabCatalog.Entry item:MusicLabCatalog.all()){
   boolean selected=kind==NOTE&&item.kind.equals("Nota")||
    kind==CHORD&&item.kind.equals("Acorde")||
    kind==SEQUENCE&&(item.kind.equals("Escala")||item.kind.equals("Arpejo")||
                     item.kind.equals("Intervalo"));
   if(selected)list.add(item);
  }
  return MusicLabCatalog.filter(list,query==null?"":query);
 }
 public static boolean unlocked(int module,int[] masks){
  if(module==0)return true;
  if(masks==null||masks.length!=MODULE_COUNT)return false;
  return Integer.bitCount(masks[module-1]&((1<<lessonCount(module-1))-1))==lessonCount(module-1);
 }
 public static int completeCount(int module,int[] masks){
  return masks==null||masks.length<=module?0:
   Integer.bitCount(masks[module]&((1<<lessonCount(module))-1));
 }
 public static int nextIndex(int module,int[] masks){
  for(int i=0;i<lessonCount(module);i++)if(masks==null||masks.length<=module||
       (masks[module]&(1<<i))==0)return i;
  return 0; // allow review without inventing more lessons
 }
 public static int totalCount(){int result=0;for(int i=0;i<MODULE_COUNT;i++)result+=lessonCount(i);return result;}
 public enum Feedback{CORRECT,PROGRESS,EXTRA,IGNORED,COMPLETE}
 public static final class Session {
  public final Exercise exercise;
  private int position=0,extras=0;
  private boolean complete=false;
  private final Set<Integer> chordHeld=new HashSet<>();
  public Session(Exercise e){if(e==null)throw new IllegalArgumentException("Exercício ausente");exercise=e;}
  public int correct(){return position;}
  public int total(){return exercise.pitches.length;}
  public int next(){return complete?-1:exercise.pitches[Math.min(position,exercise.pitches.length-1)];}
  public int extras(){return extras;}
  public boolean complete(){return complete;}
  public void reset(){complete=false;position=0;extras=0;chordHeld.clear();}
  public Feedback identifyFigure(int rhythm){
   if(exercise.kind!=RHYTHM||complete)return Feedback.IGNORED;
   if(rhythm==exercise.rhythm){complete=true;position=exercise.pitches.length;return Feedback.COMPLETE;}
   extras++;return Feedback.EXTRA;
  }
  /** At least one fresh Note On is mandatory. Held-only snapshots never advance. */
  public Feedback attack(int note,Collection<Integer> allHeld,boolean demonstration){
   if(complete||demonstration||exercise.kind==RHYTHM||note<0||note>127||
      allHeld==null||!allHeld.contains(note))return Feedback.IGNORED;
   HashSet<Integer> held=new HashSet<>(allHeld);
   if(exercise.kind==CHORD){
    HashSet<Integer> wanted=new HashSet<>();for(int p:exercise.pitches)wanted.add(p);
    if(!wanted.contains(note)||!wanted.containsAll(held)){extras++;return Feedback.EXTRA;}
    chordHeld.add(note);
    if(held.equals(wanted)&&chordHeld.containsAll(wanted)){
     position=exercise.pitches.length;complete=true;return Feedback.COMPLETE;
    }
    return Feedback.PROGRESS;
   }
   // Do not reward ambiguous simultaneous presses for a single melodic attack.
   if(held.size()!=1){extras++;return Feedback.EXTRA;}
   if(note!=exercise.pitches[position]){extras++;return Feedback.EXTRA;}
   position++;
   if(position==exercise.pitches.length){complete=true;return Feedback.COMPLETE;}
   return Feedback.CORRECT;
  }
 }
}

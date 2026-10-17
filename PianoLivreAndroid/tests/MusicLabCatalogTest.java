import br.org.pianolivre.MusicLabCatalog;
import java.util.*;
public class MusicLabCatalogTest{
 static int tests=0;
 static void ok(boolean v){tests++;if(!v)throw new AssertionError("check "+tests);}
 static boolean contains(int[] a,int... b){return Arrays.equals(a,b);}
 public static void main(String[] args){
  List<MusicLabCatalog.Entry> all=MusicLabCatalog.all();
  ok(all.size()>700);ok(all.get(0).title.startsWith("Lá menor"));
  ok(contains(all.get(0).notes,57,60,64));
  ok(all.stream().anyMatch(e->e.kind.equals("Nota")&&e.notes.length==1));
  ok(all.stream().anyMatch(e->e.kind.equals("Intervalo")&&contains(e.notes,60,72)));
  ok(all.stream().anyMatch(e->e.kind.equals("Escala")&&e.title.contains("Dó4")&&contains(e.notes,60,62,64,65,67,69,71,72)));
  ok(all.stream().anyMatch(e->e.kind.equals("Acorde")&&e.title.contains("inversão")&&e.notes.length>=3));
  ok(all.stream().anyMatch(e->e.kind.equals("Arpejo")&&contains(e.notes,60,64,67,72)));
  ok(MusicLabCatalog.filter(all,"arpejo").size()>=12);
  ok(!MusicLabCatalog.filter(all,"Lá menor").isEmpty());
  ok(MusicLabCatalog.filter(all,"não-existe").isEmpty());
  ok(MusicLabCatalog.filter(all,"").size()==all.size());
  ok(all.stream().allMatch(e->e.notes.length>0&&Arrays.stream(e.notes).allMatch(n->n>=0&&n<=127)));
  boolean readonly=false;try{all.add(all.get(0));}catch(UnsupportedOperationException ex){readonly=true;}
  ok(readonly);
  int original=all.get(0).notes[0];int[] snapshot=all.get(0).notes.clone();snapshot[0]=-1;
  ok(all.get(0).notes[0]==original);
  ok(all.size()==965); // 5 legacy plus 12 pitch classes x (1 note, 13 intervals, 42 chord voicings, 12 scales, 12 arpeggios)
  String[] names={"Lá menor · Am","Dó maior · C","Fá maior · F","Sol com sétima · G7","Ré menor · Dm"};
  int[][] pitches={{57,60,64},{60,64,67},{53,57,60},{55,59,62,65},{50,53,57}};
  for(int i=0;i<5;i++){ok(all.get(i).title.equals(names[i]));ok(contains(all.get(i).notes,pitches[i]));}
  for(int root=60;root<72;root++){
   final int tonic=root;
   String rootLabel=br.org.pianolivre.MusicTheory.noteName(root);
   ok(all.stream().anyMatch(e->e.kind.equals("Nota")&&contains(e.notes,tonic)));
   ok(all.stream().anyMatch(e->e.kind.equals("Intervalo")&&e.title.contains("uníssono")&&contains(e.notes,tonic,tonic)));
   ok(all.stream().anyMatch(e->e.kind.equals("Intervalo")&&e.title.contains("oitava justa")&&contains(e.notes,tonic,tonic+12)));
   ok(all.stream().filter(e->e.kind.equals("Escala")&&e.notes[0]==tonic).count()==12);
   ok(all.stream().filter(e->e.kind.equals("Arpejo")&&e.notes[0]==tonic).count()==12);
   ok(all.stream().filter(e->e.kind.equals("Acorde")&&e.title.startsWith(rootLabel+" · ")&&e.title.contains("inversão")).count()==30);
   ok(all.stream().filter(e->e.kind.equals("Acorde")&&e.title.startsWith(rootLabel+" · ")&&e.description.contains("Estrutura cromática")).count()==42);
   ok(all.stream().anyMatch(e->e.kind.equals("Arpejo")&&e.title.contains("diminuto com sétima diminuta")&&contains(e.notes,tonic,tonic+3,tonic+6,tonic+9,tonic+12)));
   ok(all.stream().anyMatch(e->e.kind.equals("Acorde")&&e.title.startsWith(rootLabel+" · ")&&e.title.contains("menor com sétima maior")&&contains(e.notes,tonic,tonic+3,tonic+7,tonic+11)));
  }
  ok(MusicLabCatalog.filter(all,"DO4").stream().anyMatch(e->e.kind.equals("Nota")&&e.notes[0]==60));
  ok(MusicLabCatalog.filter(all,"C#4").stream().anyMatch(e->e.kind.equals("Nota")&&e.notes[0]==61));
  ok(MusicLabCatalog.filter(all,"acOrDe").stream().allMatch(e->e.kind.equals("Acorde")));
  ok(MusicLabCatalog.selectionIndex(all,all.get(4))==4);
  ok(MusicLabCatalog.selectionIndex(MusicLabCatalog.filter(all,"arpejo"),all.get(4))==0);
  ok(MusicLabCatalog.selectionIndex(Collections.emptyList(),all.get(4))==-1);
  ok(MusicLabCatalog.filter(all,"   ").size()==all.size());
  System.out.println("MUSIC_LAB_CATALOG_PASS checks="+tests+" entries="+all.size());
 }
}

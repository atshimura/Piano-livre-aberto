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
  System.out.println("MUSIC_LAB_CATALOG_PASS checks="+tests+" entries="+all.size());
 }
}

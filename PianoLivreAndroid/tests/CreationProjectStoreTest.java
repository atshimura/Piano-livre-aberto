import br.org.pianolivre.*;
import java.io.*;import java.util.*;
public final class CreationProjectStoreTest {
 static int checks;static void ok(boolean x,String m){checks++;if(!x)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  File root=new File(System.getProperty("java.io.tmpdir"),"creation-store-test-"+UUID.randomUUID());root.mkdirs();
  try{
   CreationProjectStore s=new CreationProjectStore(root);MidiTakeRecorder r=new MidiTakeRecorder();r.start(3,100);r.accept(0x90,60,88,110,3);r.accept(0x80,60,0,210,3);r.stop(220);
   CreationProjectStore.Project p=s.save("Canção Árvore",8,1000,120,r.events(),r.segments());
   ok(p.id.length()==36&&s.get(p.id).events.size()==2,"stable id and event bytes roundtrip");
   s.save("cancao arvore",8,1100,120,r.events(),r.segments());
   ok(s.list(8,CreationProjectStore.Filter.ALL,"CANCAO ARVORE",null).size()==2,"accent/case insensitive title search and duplicate titles");
   ok(s.list(9,CreationProjectStore.Filter.ALL,"",null).isEmpty(),"profile isolation");
   s.rename(p.id,"Nome novo",1200);ok(s.get(p.id).title.equals("Nome novo"),"rename preserves project id");
   s.moveToTrash(p.id,1300);ok(s.list(8,CreationProjectStore.Filter.TRASH,"",null).size()==1,"soft delete in trash");
   s.restore(p.id,1400);ok(s.get(p.id).id.equals(p.id),"restore retains id");
   s.moveToTrash(p.id,1500);s.deletePermanently(p.id);ok(s.list(8,CreationProjectStore.Filter.ALL,"",null).size()==1,"permanent deletion only for trash");
   File f=new File(new File(root,"creations"),s.list(8,CreationProjectStore.Filter.ALL,"",null).get(0).id+".plc");try(RandomAccessFile raf=new RandomAccessFile(f,"rw")){raf.seek(f.length()-1);raf.write(0x7f);}
   ok(s.list(8,CreationProjectStore.Filter.ALL,"",null).isEmpty(),"corrupt project never appears as saved");
   CreationProjectStore bounded=new CreationProjectStore(new File(root,"bounded"));
   for(int i=0;i<CreationProjectStore.MAX_PROJECTS;i++)bounded.save("Limit "+i,0,2000+i,120,r.events(),r.segments());
   boolean rejected=false;try{bounded.save("Beyond limit",0,3000,120,r.events(),r.segments());}catch(IOException ex){rejected=true;}
   ok(rejected&&bounded.list(0,CreationProjectStore.Filter.ALL,"",null).size()==CreationProjectStore.MAX_PROJECTS,"200-project library limit is enforced without hiding valid files");
   CreationProjectStore oversized=new CreationProjectStore(new File(root,"oversized"));
   ArrayList<MidiTakeRecorder.Event> tooMany=new ArrayList<>(Collections.nCopies(CreationProjectStore.MAX_EVENTS+1,r.events().get(0)));
   rejected=false;try{oversized.save("Too many events",0,4000,120,tooMany,r.segments());}catch(IOException ex){rejected=true;}
   ok(rejected&&oversized.list(0,CreationProjectStore.Filter.ALL,"",null).isEmpty(),"event-count limit rejects oversized projects before writing");
  }finally{remove(root);}
  System.out.println("CREATION_PROJECT_STORE_PASS checks="+checks);
 }
 static void remove(File f){if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File x:c)remove(x);}f.delete();}
}

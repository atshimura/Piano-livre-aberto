import br.org.pianolivre.*;import java.io.*;import java.util.*;
public final class CreationJournalTest {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  File root=new File(System.getProperty("java.io.tmpdir"),"creation-journal-test-"+UUID.randomUUID());root.mkdirs();String id=UUID.randomUUID().toString();
  try{
   CreationJournal j=new CreationJournal(root,id,"Recuperação",4,123,1_000_000_000L);MidiTakeRecorder r=new MidiTakeRecorder();r.start(9,1_000_000_000L);r.accept(0x91,61,75,1_100_000_000L,9);
   for(MidiTakeRecorder.Event e:r.events())ok(j.append(e),"event queued");j.flush();j.close();
   File f=new File(new File(root,"creation-journals"),id+".journal");CreationJournal.Recovery recovery=CreationJournal.read(f);
   ok(recovery.events.size()==1&&recovery.events.get(0).channel()==1,"valid event recovered");ok(recovery.profileId==4&&recovery.createdAt==123,"profile/date metadata retained");ok(recovery.segments.size()==1&&recovery.segments.get(0).startNs==1_000_000_000L,"active prefix segment reconstructed");ok(!recovery.truncated,"complete journal accepted");
   try(FileOutputStream out=new FileOutputStream(f,true)){out.write(new byte[]{0,0,0,20,1,2});}
   recovery=CreationJournal.read(f);ok(recovery.events.size()==1&&recovery.truncated,"damaged tail preserves valid prefix");
   f.delete();
  }finally{remove(root);}
  System.out.println("CREATION_JOURNAL_PASS checks="+checks);
 }
 static void remove(File f){if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File x:c)remove(x);}f.delete();}
}

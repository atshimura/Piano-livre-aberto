package br.org.pianolivre;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Narrow C2-C JVM smoke tests: derivative persistence and protection of the original. */
public final class CreationScoreStoreTest {
 static int checks;
 static void ok(boolean yes,String msg){checks++;if(!yes)throw new AssertionError(msg);}
 static void reject(RunnableChecked r,String msg)throws Exception{boolean bad=false;try{r.run();}catch(Exception expected){bad=true;}ok(bad,msg);}
 interface RunnableChecked{void run()throws Exception;}
 static void delete(File file){if(file.isDirectory()){File[] children=file.listFiles();if(children!=null)for(File c:children)delete(c);}file.delete();}
 public static void main(String[] args)throws Exception{
  File root=new File(System.getProperty("java.io.tmpdir"),"piano-c2c-test-"+UUID.randomUUID());
  if(!root.mkdirs())throw new IOException("Cannot create test root");
  try{
   CreationProjectStore originals=new CreationProjectStore(root);
   MidiTakeRecorder recording=new MidiTakeRecorder();
   recording.start(1,1_000_000_000L);recording.accept(0x90,60,88,1_100_000_000L,1);
   recording.accept(0x80,60,0,1_700_000_000L,1);recording.stop(1_800_000_000L);
   CreationProjectStore.Project project=originals.save("Original",0,1,recording.durationNs(),recording.events(),recording.segments());
   byte[] before=originals.rawFile(project.id);
   CreationScoreStore scores=new CreationScoreStore(root);
   String session="creation-score:"+project.id;
   ok(SessionFiles.portable(session).equals(session),"derivative session portable");
   ok(SessionFiles.resolve(root,session).equals(session),"session never expands to a file path");
   reject(()->SessionFiles.portable("creation-score:../../etc"),"invalid derivative session rejected");
   CreationScoreTranscriber.Options opts=CreationScoreTranscriber.Options.defaults();
   String xml=CreationScoreTranscriber.transcribe(project.events,project.segments,opts,project.title).xml;
   ok(!scores.has(project.id),"not generated initially");
   scores.save(project.id,0,opts,xml);
   ok(scores.has(project.id),"score sidecar stored");
   ok(Arrays.equals(before,originals.rawFile(project.id)),"canonical PLC unchanged");
   CreationScoreStore.Saved saved=scores.read(project.id);
   ok(saved.xml.equals(xml)&&saved.options.bpm==100&&saved.options.grid==16,"roundtrip XML/options");
   ok(saved.projectId.equals(project.id)&&saved.profileId==0,"owner and UUID");
   byte[] unchanged=scores.raw(project.id);
   reject(()->scores.save(project.id,1,opts,xml),"wrong profile must not save");
   reject(()->scores.save(project.id,0,opts,xml+"<fake/>"),"derived XML mismatch rejected");
   ok(Arrays.equals(unchanged,scores.raw(project.id)),"failed save preserves prior score");
   originals.rename(project.id,"New name",3);
   ok(scores.read(project.id).xml.equals(xml),"title rename keeps original derivative valid");
   originals.moveToTrash(project.id,4);
   ok(scores.has(project.id),"soft trash preserves derivative");
   reject(()->scores.save(project.id,0,opts,xml),"trash cannot save");
   originals.restore(project.id,5);
   ok(scores.read(project.id).xml.equals(xml),"trash restore retains derivative");
   // Simulate a process killed after moving the score into deletion quarantine.
   File quarantine=new File(new File(root,CreationScoreStore.DIRECTORY),project.id+".deleting");
   File actual=new File(new File(root,CreationScoreStore.DIRECTORY),project.id+CreationScoreStore.EXT);
   Files.move(actual.toPath(),quarantine.toPath());
   new CreationScoreStore(root);
   ok(actual.exists()&&!quarantine.exists(),"restart recovers score while PLC still exists");
   File f=new File(new File(root,CreationScoreStore.DIRECTORY),project.id+CreationScoreStore.EXT);
   byte[] corrupted=unchanged.clone();corrupted[corrupted.length-5]^=1;Files.write(f.toPath(),corrupted);
   reject(()->scores.read(project.id),"corruption detected before use");
   Files.write(f.toPath(),unchanged);
   ok(scores.read(project.id).xml.equals(xml),"recover unmodified original score");
   String hash=CreationScoreStore.fingerprint(originals.get(project.id));
   ok(hash.length()==64,"canonical MIDI SHA-256");
   originals.moveToTrash(project.id,6);originals.deletePermanently(project.id);
   ok(!scores.has(project.id),"permanent deletion removes score");
   reject(()->originals.get(project.id),"deleted source unavailable");
   ok(scores.ids().isEmpty(),"no orphan score");
   System.out.println("CREATION_C2_C_BASIC_PASS checks="+checks);
  }finally{delete(root);}
 }
}

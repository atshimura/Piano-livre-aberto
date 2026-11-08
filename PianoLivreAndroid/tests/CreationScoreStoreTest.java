package br.org.pianolivre;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/** Z-P3 JVM smoke tests: PLS2 derivative persistence, legacy rejection and protection of the original. */
public final class CreationScoreStoreTest {
 static int checks;
 static void ok(boolean yes,String msg){checks++;if(!yes)throw new AssertionError(msg);}
 static void reject(RunnableChecked r,String msg)throws Exception{boolean bad=false;try{r.run();}catch(Exception expected){bad=true;}ok(bad,msg);}
 interface RunnableChecked{void run()throws Exception;}
 static void delete(File file){if(file.isDirectory()){File[] children=file.listFiles();if(children!=null)for(File c:children)delete(c);}file.delete();}
 public static void main(String[] args)throws Exception{
  File root=new File(System.getProperty("java.io.tmpdir"),"piano-zp3-test-"+UUID.randomUUID());
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
   CreationScoreGenerator.Generation generated=CreationScoreGenerator.generate(
      project.events,project.segments,project.title);
   String xml=generated.xml;
   int bpm=(int)Math.round(generated.bpm);
   ok(!scores.has(project.id),"not generated initially");
   scores.save(project.id,0,xml,bpm);
   ok(scores.has(project.id),"score sidecar stored");
   ok(Arrays.equals(before,originals.rawFile(project.id)),"canonical PLC unchanged");
   CreationScoreStore.Saved saved=scores.read(project.id);
   ok(saved.xml.equals(xml)&&saved.bpm==bpm,"roundtrip XML and estimated tempo");
   ok(saved.projectId.equals(project.id)&&saved.profileId==0,"owner and UUID");
   ok(saved.fingerprint.equals(CreationScoreStore.fingerprint(project)),"recording fingerprint");
   byte[] unchanged=scores.raw(project.id);
   reject(()->scores.save(project.id,1,xml,bpm),"wrong profile must not save");
   reject(()->scores.save(project.id,0,"<curto/>",bpm),"too short XML rejected");
   ok(Arrays.equals(unchanged,scores.raw(project.id)),"failed save preserves prior score");
   originals.rename(project.id,"New name",3);
   ok(scores.read(project.id).xml.equals(xml),"title rename keeps original derivative valid");
   originals.moveToTrash(project.id,4);
   ok(scores.has(project.id),"soft trash preserves derivative");
   reject(()->scores.save(project.id,0,xml,bpm),"trash cannot save");
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
   // Historical PLS1 bytes are recognised as legacy: never parsed, never deleted.
   ByteArrayOutputStream legacyBytes=new ByteArrayOutputStream();
   DataOutputStream data=new DataOutputStream(legacyBytes);
   data.writeInt(0x504c5331);data.writeInt(1);
   data.writeUTF(project.id);data.writeInt(0);data.writeUTF(hash);data.writeUTF(project.title);
   data.writeInt(100);data.writeInt(4);data.writeInt(4);data.writeInt(16);
   data.writeInt(0);data.writeInt(60);data.writeInt(0);
   byte[] historicMusic=xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
   data.writeInt(historicMusic.length);data.write(historicMusic);data.flush();
   legacyBytes.write(java.security.MessageDigest.getInstance("SHA-256").digest(legacyBytes.toByteArray()));
   Files.write(f.toPath(),legacyBytes.toByteArray());
   ok(scores.has(project.id)&&scores.isLegacy(project.id),"PLS1 file detected as legacy, never deleted");
   boolean legacyRejected=false;
   try{scores.read(project.id);}catch(CreationScoreStore.LegacyScoreException expected){legacyRejected=true;}
   ok(legacyRejected,"PLS1 raises LegacyScoreException instead of parsing old music");
   // Saving the new derivation replaces the legacy file without touching the .plc.
   byte[] midiBefore=originals.rawFile(project.id);
   scores.save(project.id,0,xml,bpm);
   ok(scores.read(project.id).xml.equals(xml)&&!scores.isLegacy(project.id),"regeneration replaces legacy file");
   ok(Arrays.equals(midiBefore,originals.rawFile(project.id)),"source PLC unchanged across legacy replacement");
   originals.moveToTrash(project.id,6);originals.deletePermanently(project.id);
   ok(!scores.has(project.id),"permanent deletion removes score");
   reject(()->originals.get(project.id),"deleted source unavailable");
   ok(scores.ids().isEmpty(),"no orphan score");
   System.out.println("CREATION_ZP3_STORE_PASS checks="+checks);
  }finally{delete(root);}
 }
}

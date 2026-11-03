import br.org.pianolivre.ImportedScoreDeletion;
import java.nio.file.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Pure JVM crash-window assertions for imported XML + paired MIDI deletion. */
public final class ImportedScoreDeletionTest {
 static int checks=0;
 static void ok(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 static void put(Path file)throws Exception{Files.write(file,new byte[]{1,2,3});}
 static void manifest(Path stage)throws Exception{Files.writeString(stage.resolve("score-name"),"teste.musicxml");}
 static Path stage(Path root)throws Exception{return Files.createDirectory(root.resolve("imported.delete-pending"));}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("piano-delete-"),imported=Files.createDirectory(root.resolve("imported"));
  try{
   String key="imported/teste.musicxml";
   Path score=imported.resolve("teste.musicxml"),source=imported.resolve("teste.musicxml.source.mid"),other=imported.resolve("outro.mxl");
   put(score);put(source);put(other);
   AtomicInteger updates=new AtomicInteger();
   ImportedScoreDeletion.delete(root.toFile(),key,actual->{ok(key.equals(actual),"metadata key on success");updates.incrementAndGet();});
   ok(!Files.exists(score)&&!Files.exists(source)&&Files.exists(other),"delete XML+MIDI without touching others");
   ok(updates.get()==1&&!Files.exists(root.resolve("imported.delete-pending")),"one metadata commit on normal path");
   put(score);put(source);

   // Failure after durable marker: no silent rollback of only the file half.
   try{
    ImportedScoreDeletion.delete(root.toFile(),key,actual->{throw new IllegalStateException("prefs unavailable");});
    throw new AssertionError("Missing pending-commit failure");
   }catch(IOException expected){ok(expected.getMessage().contains("pendente"),"post-marker error explicitly pending");}
   Path staged=root.resolve("imported.delete-pending");
   ok(!Files.exists(score)&&Files.exists(staged.resolve("teste.musicxml"))&&Files.exists(staged.resolve("committed")),
      "failed metadata commit retains staged bytes + marker");
   ok(Files.exists(staged.resolve("teste.musicxml.source.mid")),"paired MIDI remains recoverable");
   ImportedScoreDeletion.recover(root.toFile(),actual->{ok(key.equals(actual),"metadata replay key");updates.incrementAndGet();});
   ok(!Files.exists(score)&&!Files.exists(source)&&!Files.exists(staged),"replay completes both metadata and file deletion");
   ok(updates.get()==2,"metadata replay occurs only when needed");

   // Crash before commit marker must restore both original files, without mutating prefs.
   put(score);put(source);
   staged=stage(root);manifest(staged);
   Files.move(score,staged.resolve("teste.musicxml"));
   Files.move(source,staged.resolve("teste.musicxml.source.mid"));
   ImportedScoreDeletion.recover(root.toFile(),actual->{throw new AssertionError("precommit recovery must not update metadata");});
   ok(Files.isRegularFile(score)&&Files.isRegularFile(source)&&!Files.exists(staged),
      "precommit crash restores score+MIDI, does not touch prefs");

   // Crash immediately after marker but before prefs: finish metadata before dropping staged files.
   staged=stage(root);manifest(staged);
   Files.move(score,staged.resolve("teste.musicxml"));
   Files.move(source,staged.resolve("teste.musicxml.source.mid"));
   Files.writeString(staged.resolve("committed"),"ok");
   AtomicInteger resumed=new AtomicInteger();
   try{
    ImportedScoreDeletion.recover(root.toFile(),actual->{resumed.incrementAndGet();throw new IllegalStateException("still unavailable");});
    throw new AssertionError("Recovery must fail closed while prefs inaccessible");
   }catch(IOException expected){}
   ok(resumed.get()==1&&Files.exists(staged.resolve("teste.musicxml"))&&Files.exists(staged.resolve("committed")),
      "failed metadata recovery retains committed staged files");
   ImportedScoreDeletion.recover(root.toFile(),actual->{ok(key.equals(actual),"replay correct score");resumed.incrementAndGet();});
   ok(resumed.get()==2&&!Files.exists(score)&&!Files.exists(source)&&!Files.exists(staged),
      "replayed metadata lets recovered cleanup finish");
   ImportedScoreDeletion.recover(root.toFile(),actual->{throw new AssertionError("idempotent empty recovery");});
   ok(!Files.exists(staged),"repeating recovery is harmless");

   for(String invalid:new String[]{"scores/teste.mxl","imported/../a.mxl","imported/teste.musicxml.source.mid",
     "creation-score:123e4567-e89b-12d3-a456-426614174000"}){
    try{ImportedScoreDeletion.delete(root.toFile(),invalid,k->{});throw new AssertionError("accepted "+invalid);}
    catch(IOException expected){checks++;}
   }
   ImportedScoreDeletion.delete(root.toFile(),"imported/outro.mxl",k->{});
   ok(!Files.exists(other),"MusicXML without MIDI source can be removed");
   System.out.println("PASS ImportedScoreDeletionTest: "+checks+" checks");
  }finally{
   try(java.util.stream.Stream<Path> walk=Files.walk(root)){
    walk.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});
   }
  }
 }
}

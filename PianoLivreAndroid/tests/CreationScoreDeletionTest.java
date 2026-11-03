import br.org.pianolivre.CreationScoreDeletion;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

/** Pure JVM tests: delete only generated .pls; crash recovery and metadata replay. */
public final class CreationScoreDeletionTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static void put(Path p)throws IOException{Files.write(p,new byte[]{11,22,33,44});}
 interface Action{void run()throws Exception;}
 static void expectFailure(Action action)throws Exception{
  boolean threw=false;try{action.run();}catch(IOException expected){threw=true;}
  check(threw,"expected IOException");
 }
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("piano-creation-delete-");
  String id="123e4567-e89b-12d3-a456-426614174000";
  try{
   Path projects=Files.createDirectory(root.resolve("creations")),scores=Files.createDirectory(root.resolve("creation-scores"));
   Path original=projects.resolve(id+".plc"),score=scores.resolve(id+".pls"),other=scores.resolve("ffffffff-ffff-ffff-ffff-ffffffffffff.pls");
   put(original);put(score);put(other);
   AtomicInteger updates=new AtomicInteger();
   CreationScoreDeletion.delete(root.toFile(),id,found->{check(found.equals(id),"metadata id");updates.incrementAndGet();});
   check(Files.isRegularFile(original)&&!Files.exists(score)&&Files.isRegularFile(other),"only derivative removed");
   check(updates.get()==1&&!Files.exists(root.resolve("creation-score.delete-pending")),"one metadata commit and no staging");
   expectFailure(()->CreationScoreDeletion.delete(root.toFile(),"../invalid",value->{}));
   expectFailure(()->CreationScoreDeletion.delete(root.toFile(),id,value->{}));
   put(score);
   // Crash before commit: recover score without changing metadata.
   Path stage=Files.createDirectory(root.resolve("creation-score.delete-pending"));
   Files.writeString(stage.resolve("project-id"),id);
   Files.move(score,stage.resolve(id+".pls"));
   CreationScoreDeletion.recover(root.toFile(),value->{throw new AssertionError("uncommitted update");});
   check(Files.isRegularFile(score)&&!Files.exists(stage),"precommit crash restores derivative");

   // Crash after commit: if prefs unavailable, leave bytes safe for next startup.
   stage=Files.createDirectory(root.resolve("creation-score.delete-pending"));
   Files.writeString(stage.resolve("project-id"),id);
   Files.move(score,stage.resolve(id+".pls"));
   Files.writeString(stage.resolve("committed"),"ok");
   final Path pending=stage;
   AtomicInteger replay=new AtomicInteger();
   expectFailure(()->CreationScoreDeletion.recover(root.toFile(),value->{replay.incrementAndGet();throw new IllegalStateException("prefs failure");}));
   check(Files.isRegularFile(pending.resolve(id+".pls"))&&Files.exists(pending.resolve("committed")),"failure retains staging and marker");
   CreationScoreDeletion.recover(root.toFile(),value->{check(value.equals(id),"correct recovery id");replay.incrementAndGet();});
   check(replay.get()==2&&!Files.exists(score)&&!Files.exists(pending)&&Files.exists(original),"replayed metadata precedes score deletion");
   CreationScoreDeletion.recover(root.toFile(),value->{throw new AssertionError("replay on empty stage");});
   put(score);

   // Metadata failure during normal delete is deliberately recoverable.
   expectFailure(()->CreationScoreDeletion.delete(root.toFile(),id,value->{throw new IllegalStateException("cannot commit prefs");}));
   check(!Files.exists(score)&&Files.exists(root.resolve("creation-score.delete-pending").resolve(id+".pls")),"postcommit failure retains score");
   CreationScoreDeletion.recover(root.toFile(),value->{updates.incrementAndGet();});
   check(Files.exists(original)&&!Files.exists(score)&&updates.get()==2,"restart finalizes deletion, MIDI untouched");
   System.out.println("CREATION_SCORE_DELETION_BASIC_PASS checks="+checks);
  }finally{
   try(java.util.stream.Stream<Path> tree=Files.walk(root)){
    tree.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});
   }
  }
 }
}

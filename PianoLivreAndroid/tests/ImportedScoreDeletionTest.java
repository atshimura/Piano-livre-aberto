import br.org.pianolivre.ImportedScoreDeletion;
import java.nio.file.*;
import java.io.*;
public final class ImportedScoreDeletionTest {
 static int checks=0;
 static void ok(boolean check,String why){checks++;if(!check)throw new AssertionError(why);}
 static void put(Path p)throws Exception{Files.write(p,new byte[]{1,2,3});}
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("piano-delete-"),imported=Files.createDirectory(root.resolve("imported"));
  try{
   Path score=imported.resolve("teste.musicxml"),source=imported.resolve("teste.musicxml.source.mid"),other=imported.resolve("outro.mxl");
   put(score);put(source);put(other);
   final int[] calls={0};
   ImportedScoreDeletion.delete(root.toFile(),"imported/teste.musicxml",()->calls[0]++);
   ok(!Files.exists(score)&&!Files.exists(source)&&Files.exists(other),"pair deleted; unrelated retained");
   ok(calls[0]==1&&!Files.exists(root.resolve("imported.delete-pending")),"metadata once, no staging");
   put(score);put(source);
   try{ImportedScoreDeletion.delete(root.toFile(),"imported/teste.musicxml",()->{throw new IllegalStateException("failed commit");});throw new AssertionError("expected rollback");}
   catch(IOException expected){}
   ok(Files.isRegularFile(score)&&Files.isRegularFile(source),"failed metadata commit restores score and source");
   Path stage=Files.createDirectory(root.resolve("imported.delete-pending"));
   Files.write(stage.resolve("score-name"),"teste.musicxml".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   Files.move(score,stage.resolve("teste.musicxml"));
   ImportedScoreDeletion.recover(root.toFile());
   ok(Files.isRegularFile(score)&&!Files.exists(stage),"precommit crash rollback");
   Files.createDirectory(stage);Files.write(stage.resolve("score-name"),"teste.musicxml".getBytes(java.nio.charset.StandardCharsets.UTF_8));
   Files.move(score,stage.resolve("teste.musicxml"));Files.move(source,stage.resolve("teste.musicxml.source.mid"));
   Files.write(stage.resolve("committed"),new byte[]{1});
   ImportedScoreDeletion.recover(root.toFile());
   ok(!Files.exists(score)&&!Files.exists(source)&&!Files.exists(stage),"postcommit crash cleanup");
   for(String invalid:new String[]{"scores/teste.mxl","imported/../a.mxl","imported/teste.musicxml.source.mid","creation-score:123e4567-e89b-12d3-a456-426614174000"}){
    try{ImportedScoreDeletion.delete(root.toFile(),invalid,()->{});throw new AssertionError("accepted "+invalid);}catch(IOException expected){checks++;}
   }
   ImportedScoreDeletion.delete(root.toFile(),"imported/outro.mxl",()->{});
   ok(!Files.exists(other),"MusicXML without source deleted");
   System.out.println("PASS ImportedScoreDeletionTest: "+checks+" checks");
  }finally{try(java.util.stream.Stream<Path> walk=Files.walk(root)){walk.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}}
 }
}

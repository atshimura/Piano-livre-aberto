package br.org.pianolivre;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Bounded take/library/export exercised under a 128 MiB JVM heap. */
public final class CreationLimitsTest {
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory("piano-c1-limits-");
  try{
   long start=System.nanoTime();MidiTakeRecorder take=new MidiTakeRecorder();take.start(1,0);
   int count=MidiTakeRecorder.MAX_EVENTS-3000;
   for(int i=0;i<count;i++){if(!take.accept(i%2==0?0x90:0x80,60,i%2==0?91:0,i*1_000_000L,1,100+i,9))throw new AssertionError("bounded capture");if(take.eventsSince(i).size()!=1)throw new AssertionError("incremental delta");}
   take.stop(count*1_000_000L);long captured=System.nanoTime();
   CreationProjectStore store=new CreationProjectStore(root.toFile());
   CreationProjectStore.Project p=store.save(UUID.randomUUID().toString(),"Limite medido",0,1,take.durationNs(),take.events(),take.segments(),"SIMULADO");
   long saved=System.nanoTime();byte[] smf=MidiStandardFileWriter.write(store.get(p.id));long exported=System.nanoTime();
   if(smf.length>MidiStandardFileWriter.MAX_BYTES)throw new AssertionError("SMF bound");
   boolean rejected=false;try{store.save("Second full take",0,2,take.durationNs(),take.events(),take.segments());}catch(IOException expected){rejected=true;}
   if(!rejected||store.get(p.id).events.size()!=count)throw new AssertionError("Library budget preserves the valid take");
   System.out.println("CREATION_LIMITS_PASS events="+count+" projectBytes="+store.rawFile(p.id).length+" smfBytes="+smf.length+" receiveMs="+(captured-start)/1_000_000+" saveMs="+(saved-captured)/1_000_000+" exportMs="+(exported-saved)/1_000_000);
  }finally{try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path p:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.delete(p);}}
 }
}

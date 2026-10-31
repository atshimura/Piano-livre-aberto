package br.org.pianolivre;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Fault/lifecycle cases independent of a physical instrument. */
public final class CreationSafetyRegressionTest {
 static int checks;
 static void ok(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("piano-creation-regression-").toFile();
  try{
   MidiTakeRecorder take=new MidiTakeRecorder();take.start(9,100);
   take.accept(0x91,60,91,110,9,17,44);take.accept(0x91,60,0,120,9,18,44);take.pause(130);
   String id=UUID.randomUUID().toString();CreationJournal journal=new CreationJournal(root,id,"Interrupted",7,1000,100);
   for(MidiTakeRecorder.Event e:take.events())journal.append(e);
   for(MidiTakeRecorder.Segment s:take.segments())journal.append(s);
   ok(journal.beginSegment(1000),"resume boundary queued");take.resume(1000,9);
   take.accept(0x92,62,85,1010,9,99,44);
   for(MidiTakeRecorder.Event e:take.eventsSince(2))journal.append(e);
   journal.flush();journal.close();
   File file=new File(root,"creation-journals/"+id+".journal");
   CreationJournal.Recovery recovered=CreationJournal.read(file);
   ok(recovered.segments.size()==2&&recovered.segments.get(1).startNs==1000,"crash after resume does not include paused wall time");
   ok(recovered.events.get(0).androidTimestampNs==17&&recovered.events.get(0).sourceId==44,"Android timestamp and input source retained separately");
   CreationProjectStore store=new CreationProjectStore(root);
   store.saveRecoverable(id,"Interrupted",7,1000,40,recovered.events,recovered.segments);
   CreationProjectStore.Project saved=store.get(id);
   ok(saved.events.size()==4&&saved.events.get(3).generated,"recovery closes an unpaired attack with separate provenance");
   ok(saved.events.get(0).androidTimestampNs==17&&saved.events.get(2).timeNs==1010,"project serialization retains original metadata and timing");
   MidiFileImport.Result imported=MidiFileImport.read(new ByteArrayInputStream(MidiStandardFileWriter.write(saved)));
   ok(imported.notes.size()==2&&imported.notes.get(1).channel==2,"recovered recording exports an interoperable SMF including short notes");
   boolean rejected=false;
   try{store.saveRecoverable(id,"Different",7,1000,40,take.events().subList(0,1),recovered.segments);}catch(IOException expected){rejected=true;}
   ok(rejected&&store.get(id).events.size()==4,"different journal with same ID cannot replace or silently discard a valid project");
   try(RandomAccessFile out=new RandomAccessFile(file,"rw")){out.seek(out.length());out.writeInt(50);out.writeByte(1);}
   ok(CreationJournal.read(file).truncated,"truncated tail is identified");
   MidiTakeRecorder pedal=new MidiTakeRecorder();pedal.start(1,0);pedal.accept(0xb0,64,127,1,1);pedal.accept(0xb0,123,0,2,1);pedal.stop(3);
   ok(pedal.events().stream().anyMatch(e->e.generated&&e.command()==0xb0&&e.data1==64&&e.data2==0),"CC123 does not erase pedal safety state");
   MidiTakeRecorder reset=new MidiTakeRecorder();reset.start(1,0);reset.accept(0x90,60,90,1,1);reset.accept(0xb0,121,0,2,1);reset.stop(3);
   ok(reset.events().stream().anyMatch(e->e.generated&&e.command()==0x80),"Reset Controllers does not erase physically held keys");
   // A queued callback from a stopped take must not reach a new playback.
   BlockingQueue<Runnable> queue=new LinkedBlockingQueue<>();List<byte[]> sent=new ArrayList<>();
   MidiTakeRecorder phrase=new MidiTakeRecorder();phrase.start(1,0);phrase.accept(0x90,65,70,0,1);phrase.accept(0x80,65,0,1_000_000_000L,1);phrase.stop(1_000_000_000L);
   CreationProjectStore.Project p=store.save("Queue",7,1001,phrase.durationNs(),phrase.events(),phrase.segments());
   try(MidiTakePlayer player=new MidiTakePlayer(new MidiTakePlayer.Sink(){public boolean available(){return true;}public boolean send(byte[] b){sent.add(b);return true;}public void silence(){}},(s,pos,d)->{},queue::add)){
    player.play(p,false);Runnable stale=queue.poll(2,TimeUnit.SECONDS);ok(stale!=null,"callback scheduled");
    player.stop();player.play(p,false);stale.run();ok(sent.isEmpty(),"stale callback cannot emit after stop/new generation");
    Runnable current=queue.poll(2,TimeUnit.SECONDS);ok(current!=null,"new callback scheduled");current.run();ok(sent.size()==1,"current take emits exactly one attack");
    player.pause();int before=sent.size();Runnable paused=queue.poll(100,TimeUnit.MILLISECONDS);if(paused!=null)paused.run();ok(sent.size()==before,"queued callback cannot emit while paused");
   }
   System.out.println("CREATION_SAFETY_REGRESSION_PASS checks="+checks);
  }finally{try(java.util.stream.Stream<Path> stream=Files.walk(root.toPath())){for(Path p:(Iterable<Path>)stream.sorted(Comparator.reverseOrder())::iterator)Files.delete(p);}}
 }
}

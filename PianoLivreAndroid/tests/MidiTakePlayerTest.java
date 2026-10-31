import br.org.pianolivre.*;import java.io.*;import java.util.*;import java.util.concurrent.*;
public final class MidiTakePlayerTest {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  File dir=new File(System.getProperty("java.io.tmpdir"),"take-player-test-"+UUID.randomUUID());dir.mkdirs();
  try{
   MidiTakeRecorder r=new MidiTakeRecorder();r.start(1,1_000_000_000L);r.accept(0x90,60,90,1_100_000_000L,1);r.accept(0x80,60,0,1_400_000_000L,1);r.stop(1_500_000_000L);
   CreationProjectStore.Project p=new CreationProjectStore(dir).save("Playback",0,1,r.durationNs(),r.events(),r.segments());
   List<byte[]> sent=Collections.synchronizedList(new ArrayList<>());CountDownLatch off=new CountDownLatch(1);CountDownLatch paused=new CountDownLatch(1);
   MidiTakePlayer player=new MidiTakePlayer(new MidiTakePlayer.Sink(){public boolean available(){return true;}public boolean send(byte[] m){sent.add(m);if((m[0]&0xf0)==0x80)off.countDown();return true;}public void silence(){paused.countDown();}},(s,pos,d)->{});
   try{player.play(p,false);Thread.sleep(170);player.pause();int countAtPause=sent.size();Thread.sleep(350);ok(player.state()==MidiTakePlayer.State.PAUSED,"pause state retained");ok(sent.size()==countAtPause,"paused scheduler emits no MIDI");ok(paused.getCount()==0,"pause silenced output");player.resume();ok(off.await(1,TimeUnit.SECONDS),"resume schedules remaining Note Off");player.stop();ok(player.state()==MidiTakePlayer.State.STOPPED,"stop state");}
   finally{player.close();}
  }finally{remove(dir);}
  System.out.println("MIDI_TAKE_PLAYER_PASS checks="+checks);
 }
 static void remove(File f){if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File x:c)remove(x);}f.delete();}
}

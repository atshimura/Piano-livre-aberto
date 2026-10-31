import br.org.pianolivre.*;import java.io.*;import java.util.*;
public final class MidiStandardFileWriterTest {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  File dir=new File(System.getProperty("java.io.tmpdir"),"midi-writer-test-"+UUID.randomUUID());dir.mkdirs();
  try{
   MidiTakeRecorder r=new MidiTakeRecorder();r.start(4,1_000_000_000L);r.accept(0x92,60,91,1_100_000_000L,4);r.accept(0xB2,64,127,1_200_000_000L,4);r.accept(0x82,60,0,1_900_000_000L,4);r.pause(2_000_000_000L);r.resume(5_000_000_000L,4);r.accept(0x92,64,73,5_100_000_000L,4);r.accept(0x82,64,0,5_900_000_000L,4);r.stop(6_000_000_000L);
   CreationProjectStore store=new CreationProjectStore(dir);CreationProjectStore.Project p=store.save("Round trip",2,10,r.durationNs(),r.events(),r.segments());byte[] mid=MidiStandardFileWriter.write(p);
   ok(mid.length>30&&mid[0]=='M'&&mid[1]=='T'&&mid[2]=='h'&&mid[3]=='d',"SMF header");
   MidiFileImport.Result parsed=MidiFileImport.read(new ByteArrayInputStream(mid));
   ok(parsed.ppqn==MidiStandardFileWriter.PPQN,"documented PPQN");ok(parsed.notes.size()==2,"two notes roundtrip");
   MidiFileImport.Note n0=parsed.notes.get(0),n1=parsed.notes.get(1);
   ok(n0.channel==2&&n0.pitch==60&&n0.velocity==91,"channel/pitch/velocity roundtrip");
   ok(n0.start==192&&n0.end==1728,"first note timing roundtrip");
   ok(n1.channel==2&&n1.pitch==64&&n1.velocity==73&&n1.start==2112&&n1.end==3648,"post-pause note maps to compressed playback time");
   ok(parsed.tempos.size()==1&&parsed.tempos.get(0).tick==0&&parsed.tempos.get(0).micros==500000,"explicit tempo map");
  }finally{remove(dir);}
  System.out.println("MIDI_STANDARD_FILE_WRITER_PASS checks="+checks);
 }
 static void remove(File f){if(f.isDirectory()){File[] fs=f.listFiles();if(fs!=null)for(File x:fs)remove(x);}f.delete();}
}

import br.org.pianolivre.*;
import java.io.*;
import java.util.*;
public class MidiFileImportTest {
 static int checks;static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static byte[] file(int format,int ppqn,byte[]... tracks)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(o);d.writeBytes("MThd");d.writeInt(6);d.writeShort(format);d.writeShort(tracks.length);d.writeShort(ppqn);for(byte[] t:tracks){d.writeBytes("MTrk");d.writeInt(t.length);d.write(t);}return o.toByteArray();}
 static byte[] track(int pitch,int velocity){return new byte[]{0,(byte)0x90,(byte)pitch,(byte)velocity,(byte)0x83,0x60,(byte)0x80,(byte)pitch,0,0,(byte)0xff,0x2f,0};}
 static void reject(byte[] b,String s)throws Exception{boolean bad=false;try{MidiFileImport.read(new ByteArrayInputStream(b));}catch(IOException e){bad=true;}check(bad,s);}
 public static void main(String[] args)throws Exception{
  byte[] bytes=file(1,480,track(36,100),track(84,50));MidiFileImport.Result r=MidiFileImport.read(new ByteArrayInputStream(bytes));
  check(r.notes.size()==2&&r.ppqn==480&&r.noteTracks().equals(Arrays.asList(0,1)),"tracks retained");
  Map<Integer,Integer> hands=new HashMap<>();hands.put(0,1);hands.put(1,2);
  byte[] xml=MidiFileImport.musicXml(r,hands,"A & B");Score s=Score.read(new ByteArrayInputStream(xml),"midi.musicxml");
  check(s.events.size()==2&&s.events.get(0).staff==1&&s.events.get(0).midi==36&&s.events.get(1).staff==2&&s.events.get(1).midi==84,"explicit low right/high left, no pitch inference");
  check(s.events.get(0).velocity==100&&s.events.get(1).velocity==50&&s.events.get(0).length==1,"exact ticks/velocity");
  check(s.title.equals("A & B")&&Arrays.equals(r.original,bytes),"title escaped and source preserved");
  boolean missing=false;try{MidiFileImport.musicXml(r,Collections.singletonMap(0,1),"x");}catch(IOException e){missing=true;}check(missing,"missing hand not guessed");
  byte[] running={0,(byte)0x90,60,80,0,64,70,(byte)0x83,0x60,60,0,0,64,0,0,(byte)0xff,0x2f,0};
  check(MidiFileImport.read(new ByteArrayInputStream(file(0,480,running))).notes.size()==2,"running status and note-on zero");
  byte[] longNote={0,(byte)0x90,60,80,(byte)0x92,0x60,(byte)0x80,60,0,0,(byte)0xff,0x2f,0};
  MidiFileImport.Result longR=MidiFileImport.read(new ByteArrayInputStream(file(0,480,longNote)));
  Score longS=Score.read(new ByteArrayInputStream(MidiFileImport.musicXml(longR,Collections.singletonMap(0,2),"Long")),"long.musicxml");
  check(longS.events.size()==1&&longS.events.get(0).length==5&&longS.events.get(0).staff==2,"ties across generated barlines retain duration");
  byte[] tempo={0,(byte)0xff,0x51,3,7,(byte)0xa1,0x20,0,(byte)0xff,0x2f,0};
  MidiFileImport.Result withTempo=MidiFileImport.read(new ByteArrayInputStream(file(1,480,tempo,track(60,80))));
  Score tempoS=Score.read(new ByteArrayInputStream(MidiFileImport.musicXml(withTempo,Collections.singletonMap(1,1),"Tempo")),"tempo.musicxml");check(tempoS.tempoAt(0)==120,"tempo track retained");
  reject(file(2,480,track(60,80)),"format2 rejected");reject(file(0,0xe728,track(60,80)),"SMPTE rejected");reject(Arrays.copyOf(bytes,bytes.length-1),"truncated track rejected");
  reject(file(0,480,new byte[]{0,60,80,0,(byte)0xff,0x2f,0}),"missing running status rejected");
  reject(file(0,480,new byte[]{0,(byte)0x90,60,80,0,(byte)0xff,0x2f,0}),"hanging note rejected");
  reject(file(0,480,new byte[]{(byte)0x80,(byte)0x80,(byte)0x80,(byte)0x80,0}),"excessive VLQ rejected");
  reject(file(0,480,new byte[]{0,(byte)0x80,60,0,0,(byte)0xff,0x2f,0}),"orphan note-off rejected");
  reject(file(0,480,new byte[]{0,(byte)0x90,60,80,0,(byte)0x90,60,70,0,(byte)0xff,0x2f,0}),"ambiguous overlapping same-pitch rejected");
  java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("midi-pairs-");
  try{
   File first=MidiImportStorage.save(dir.toFile(),"Arquivo.MID",r,hands),second=MidiImportStorage.save(dir.toFile(),"Arquivo.MID",r,hands);
   check(!first.equals(second)&&Arrays.equals(java.nio.file.Files.readAllBytes(new File(dir.toFile(),first.getName()+".source.mid").toPath()),bytes),"stored sources preserved with duplicate names");
  }finally{for(File f:dir.toFile().listFiles())f.delete();java.nio.file.Files.delete(dir);}
  System.out.println("PASS MidiFileImportTest "+checks+" assertions: explicit hands, ticks, velocities, ties, tempo, source and malformed inputs");
 }
}

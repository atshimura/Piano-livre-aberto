import br.org.pianolivre.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
public class ScoreImportTest {
 static byte[] xml=("<score-partwise><part-list/><part id='P1'><measure number='1'><attributes><divisions>1</divisions></attributes><note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration></note></measure></part></score-partwise>").getBytes(java.nio.charset.StandardCharsets.UTF_8);
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static byte[] zip(String... entries)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){for(int i=0;i<entries.length;i+=2){z.putNextEntry(new ZipEntry(entries[i]));z.write(entries[i+1].getBytes("UTF-8"));z.closeEntry();}}return out.toByteArray();}
 static void reject(byte[] data,String name)throws Exception{boolean bad=false;try{Score.read(new ByteArrayInputStream(data),name);}catch(Exception ex){bad=true;}check(bad,"must reject "+name);}
 public static void main(String[] args)throws Exception{
  File dir=Files.createTempDirectory("score-import-").toFile();
  try{
   File first=ScoreImport.save(dir,"Teste.XML",new ByteArrayInputStream(xml));File second=ScoreImport.save(dir,"Teste.XML",new ByteArrayInputStream(xml));
   check(!first.equals(second)&&Arrays.equals(Files.readAllBytes(first.toPath()),xml),"same name preserves original bytes");
   File normalized=ScoreImport.save(dir,"é..XML",new ByteArrayInputStream(xml));check(normalized.getName().matches("[A-Za-z0-9][A-Za-z0-9._-]*")&&!normalized.getName().contains(".."),"import filename remains backup-compatible");
   int count=dir.list().length;boolean bad=false;try{ScoreImport.save(dir,"Teste.XML",new ByteArrayInputStream("<bad/>".getBytes()));}catch(Exception ex){bad=true;}
   check(bad&&dir.list().length==count,"invalid import creates no file");
   String container="<container><rootfiles><rootfile full-path='score.musicxml' media-type='application/vnd.recordare.musicxml+xml'/></rootfiles></container>";
   byte[] mxl=zip("decoy.xml","<bad/>","META-INF/container.xml",container,"score.musicxml",new String(xml,"UTF-8"));
   check(Score.read(new ByteArrayInputStream(mxl),"score.MXL").events.size()==1,"container root selected rather than first XML");
   reject(zip("a.xml",new String(xml),"b.xml",new String(xml)),"ambiguous.mxl");
   reject(zip("../score.xml",new String(xml)),"path.mxl");
   reject("<score-timewise/>".getBytes(),"timewise.xml");
   reject(zip("META-INF/container.xml","<!DOCTYPE container [<!ENTITY x 'bad'>]><container/>","score.xml",new String(xml)),"dtd.mxl");
   reject(("<score-partwise>"+"<a>".repeat(130)+"</a>".repeat(130)+"</score-partwise>").getBytes(),"deep.xml");
   reject(zip("META-INF/container.xml","<container>"+"<a>".repeat(130)+"</a>".repeat(130)+"</container>","score.xml",new String(xml)),"deep-container.mxl");
   reject(new String(xml).replace("<divisions>1</divisions>","<divisions>1</divisions><transpose><chromatic>2</chromatic></transpose>").getBytes(),"transpose.xml");
   reject(new String(xml).replace("<octave>4</octave>","<alter>0.5</alter><octave>4</octave>").getBytes(),"microtone.xml");
   reject(new String(xml).replace("<divisions>1</divisions>","<divisions>1</divisions><time><beats>3+2</beats><beat-type>8</beat-type></time>").getBytes(),"additive.xml");
   Score decimalAlter=Score.read(new ByteArrayInputStream(new String(xml).replace("<octave>4</octave>","<alter>-1.0</alter><octave>4</octave>").getBytes()),"integer-alter.xml");check(decimalAlter.events.get(0).midi==59,"integral decimal alteration retained");
   String enharmonic="<score-partwise><part-list/><part id='P1'><measure number='1'>"+
    "<attributes><divisions>1</divisions></attributes>"+
    "<note><pitch><step>C</step><alter>1</alter><octave>4</octave></pitch><duration>1</duration></note>"+
    "<note><pitch><step>D</step><alter>-1</alter><octave>4</octave></pitch><duration>1</duration></note>"+
    "</measure></part></score-partwise>";
   Score spelled=Score.read(new ByteArrayInputStream(enharmonic.getBytes("UTF-8")),"enharmonic.xml");
   check(spelled.events.size()==2&&spelled.events.get(0).midi==61&&spelled.events.get(1).midi==61,
    "enharmonic written pitches share MIDI pitch");
   check("C".equals(spelled.events.get(0).writtenStep)&&spelled.events.get(0).writtenAlter==1&&
    "D".equals(spelled.events.get(1).writtenStep)&&spelled.events.get(1).writtenAlter==-1&&
    spelled.events.get(0).writtenOctave==4&&spelled.events.get(1).writtenOctave==4,
    "MusicXML written spelling is preserved without MIDI guess");
   check(spelled.events.get(0).sourceNoteIndex!=spelled.events.get(1).sourceNoteIndex,
    "original XML note positions remain distinguishable");
   String ties="<score-partwise><part-list/><part id='P1'>"+
    "<measure number='1'><attributes><divisions>1</divisions><time><beats>1</beats><beat-type>4</beat-type></time></attributes>"+
    "<note><pitch><step>F</step><alter>1</alter><octave>4</octave></pitch><duration>1</duration><tie type='start'/></note></measure>"+
    "<measure number='2'><note><pitch><step>F</step><alter>1</alter><octave>4</octave></pitch><duration>1</duration><tie type='stop'/></note></measure>"+
    "</part></score-partwise>";
   Score tied=Score.read(new ByteArrayInputStream(ties.getBytes("UTF-8")),"tied.xml");
   check(tied.events.size()==1&&tied.events.get(0).midi==66&&
    tied.events.get(0).length==2.0,
    "a tie continuation never adds a second MIDI attack");
   check(tied.writtenEvents.size()==2&&
    tied.writtenEvents.get(0).beat==0&&tied.writtenEvents.get(0).length==1&&
    tied.writtenEvents.get(1).beat==1&&tied.writtenEvents.get(1).length==1,
    "both original graphical note segments survive without mutated durations");
   check(tied.writtenEvents.get(0).playbackAttack&&
    !tied.writtenEvents.get(1).playbackAttack&&tied.writtenEvents.get(1).tieStop,
    "tie continuation is a visible notehead, not a new sounding attack");


   String encoded="<?xml version='1.0' encoding='ISO-8859-1'?>"+new String(xml,"UTF-8").replace("<part-list/>","<work><work-title>Canção</work-title></work><part-list/>");
   check(Score.read(new ByteArrayInputStream(encoded.getBytes("ISO-8859-1")),"latin.xml").title.equals("Canção"),"declared Latin-1 title preserved");
   String utf16=encoded.replace("ISO-8859-1","UTF-16");
   check(Score.read(new ByteArrayInputStream(utf16.getBytes("UTF-16")),"utf16.xml").title.equals("Canção"),"UTF16 BOM decoded without reinterpretation");
   for(String charset:new String[]{"UTF-16LE","UTF-16BE"}){
    String text=encoded.replace("ISO-8859-1",charset);check(Score.read(new ByteArrayInputStream(text.getBytes(charset)),"utf16.xml").title.equals("Canção"),"UTF16 without BOM "+charset);
   }
   byte[] badUtf8=Arrays.copyOf(xml,xml.length+1);badUtf8[xml.length]=(byte)0xc3;reject(badUtf8,"malformed-utf8.xml");
   ByteArrayOutputStream encodedZip=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(encodedZip)){
    z.putNextEntry(new ZipEntry("META-INF/container.xml"));z.write(("<?xml version='1.0' encoding='UTF-16'?><!DOCTYPE container [<!ENTITY x 'bad'>]><container/>").getBytes("UTF-16"));z.closeEntry();
    z.putNextEntry(new ZipEntry("score.xml"));z.write(xml);z.closeEntry();
   }
   reject(encodedZip.toByteArray(),"utf16-dtd-container.mxl");
   byte[] large=new byte[16*1024*1024+1];boolean limited=false;try{Score.readLimited(new ByteArrayInputStream(large),16*1024*1024);}catch(IOException ex){limited=true;}check(limited,"input bounded");
   reject(zip("score.xml"," ".repeat(16*1024*1024+1)),"bomb.mxl");
   File capacityDir=Files.createTempDirectory("score-capacity-").toFile();
   try{
    for(int i=0;i<199;i++)Files.write(new File(capacityDir,"old_"+i+".xml").toPath(),new byte[]{1});
    ScoreImport.save(capacityDir,"last.xml",new ByteArrayInputStream(xml));
    bad=false;try{ScoreImport.save(capacityDir,"extra.xml",new ByteArrayInputStream(xml));}catch(IOException ex){bad=true;}
    check(bad&&capacityDir.list().length==200,"accepted library remains within backup file-count capacity");
    for(File f:capacityDir.listFiles())f.delete();
    File largeFile=new File(capacityDir,"old.mxl");try(RandomAccessFile f=new RandomAccessFile(largeFile,"rw")){f.setLength(ScoreImport.MAX_TOTAL);}
    bad=false;try{ScoreImport.save(capacityDir,"extra.xml",new ByteArrayInputStream(xml));}catch(IOException ex){bad=true;}
    check(bad&&largeFile.length()==ScoreImport.MAX_TOTAL&&capacityDir.list().length==1,"capacity rejection preserves large existing source");
   }finally{for(File f:capacityDir.listFiles())f.delete();capacityDir.delete();}
   System.out.println("PASS ScoreImportTest: non-overwrite, original bytes, invalid input, container, ambiguity, traversal, DTD, size and expansion limits");
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
 }
}

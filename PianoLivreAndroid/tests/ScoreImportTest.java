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
   byte[] large=new byte[16*1024*1024+1];boolean limited=false;try{Score.readLimited(new ByteArrayInputStream(large),16*1024*1024);}catch(IOException ex){limited=true;}check(limited,"input bounded");
   reject(zip("score.xml"," ".repeat(16*1024*1024+1)),"bomb.mxl");
   System.out.println("PASS ScoreImportTest: non-overwrite, original bytes, invalid input, container, ambiguity, traversal, DTD, size and expansion limits");
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
 }
}

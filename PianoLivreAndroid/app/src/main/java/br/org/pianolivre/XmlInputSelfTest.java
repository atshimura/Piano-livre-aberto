package br.org.pianolivre;
import java.io.*;import java.util.zip.*;
/** Synthetic encodings exercised on Android's XML provider, no files/prefs. */
final class XmlInputSelfTest {
 static int run()throws Exception{
  String body="<score-partwise><work><work-title>Canção</work-title></work><part-list/><part id='P1'><measure number='1'><attributes><divisions>1</divisions></attributes><note><pitch><step>C</step><octave>4</octave></pitch><duration>1</duration></note></measure></part></score-partwise>";int count=0;
  for(String encoding:new String[]{"UTF-8","UTF-16","UTF-16LE","UTF-16BE","ISO-8859-1"}){
   byte[] bytes=("<?xml version='1.0' encoding='"+encoding+"'?>"+body).getBytes(encoding);
   Score score=Score.read(new ByteArrayInputStream(bytes),"synthetic.xml");
   if(!score.title.equals("Canção")||score.events.size()!=1||score.events.get(0).midi!=60)throw new AssertionError("XML encoding "+encoding);count++;
  }
  ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){
   zip.putNextEntry(new ZipEntry("META-INF/container.xml"));zip.write("<?xml version='1.0' encoding='UTF-16'?><!DOCTYPE container [<!ENTITY x 'bad'>]><container/>".getBytes("UTF-16"));zip.closeEntry();
   zip.putNextEntry(new ZipEntry("score.xml"));zip.write(body.getBytes("UTF-8"));zip.closeEntry();
  }
  boolean rejected=false;try{Score.read(new ByteArrayInputStream(out.toByteArray()),"synthetic.mxl");}catch(IOException ex){rejected=true;}
  if(!rejected)throw new AssertionError("Encoded container DTD rejected");return count+1;
 }
}

package br.org.pianolivre;
import java.io.*;
import java.util.*;
/** Source and derived notation are included by the existing backup format. */
public final class MidiImportStorage {
 public static File save(File directory,String name,MidiFileImport.Result midi,Map<Integer,Integer> hands)throws Exception{
  String title=name.replaceFirst("(?i)\\.midi?$","");
  byte[] xml=MidiFileImport.musicXml(midi,hands,title);
  ScoreImport.capacity(directory,2,(long)xml.length+midi.original.length);
  File score=ScoreImport.save(directory,title+".musicxml",new ByteArrayInputStream(xml));
  File source=new File(directory,score.getName()+".source.mid");boolean reserved=false,complete=false;
  try{
   if(!source.createNewFile())throw new IOException("Nome da fonte MIDI já existe");reserved=true;
   try(FileOutputStream out=new FileOutputStream(source)){out.write(midi.original);out.getFD().sync();}
   complete=true;return score;
  }finally{if(!complete){score.delete();if(reserved)source.delete();}}
 }
}

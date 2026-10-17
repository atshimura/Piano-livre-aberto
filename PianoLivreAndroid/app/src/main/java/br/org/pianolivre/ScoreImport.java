package br.org.pianolivre;
import java.io.*;
import java.util.Locale;
/** Validate before publishing; reserve a unique filename without overwriting. */
public final class ScoreImport {
 public static File save(File directory,String display,InputStream input)throws Exception{
  String lower=display.toLowerCase(Locale.ROOT);
  if(!(lower.endsWith(".mxl")||lower.endsWith(".xml")||lower.endsWith(".musicxml")))throw new IOException("Selecione MusicXML (.xml, .musicxml ou .mxl)");
  byte[] bytes=Score.readLimited(input,16*1024*1024);Score parsed=Score.read(new ByteArrayInputStream(bytes),display);
  if(parsed.events.isEmpty())throw new IOException("A partitura não contém notas suportadas");
  if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Não foi possível abrir a biblioteca importada");
  String raw=new File(display.replace('\\','/')).getName();int dot=raw.lastIndexOf('.');
  String extension=lower.substring(lower.lastIndexOf('.'));
  String base=raw.substring(0,dot).replaceAll("[^a-zA-Z0-9._-]","_");
  while(base.contains(".."))base=base.replace("..","_");
  base=base.replaceAll("[.]+$", "_");
  if(!base.matches("^[A-Za-z0-9].*"))base="Importada_"+base;
  if(base.length()>80)base=base.substring(0,80);File target=null;
  for(int n=0;n<10000;n++){File candidate=new File(directory,base+(n==0?"":"_"+n)+extension);if(candidate.createNewFile()){target=candidate;break;}}
  if(target==null)throw new IOException("Não foi possível reservar um nome livre");
  boolean complete=false;
  try(FileOutputStream out=new FileOutputStream(target)){out.write(bytes);out.getFD().sync();complete=true;}
  finally{if(!complete)target.delete();}
  return target;
 }
}

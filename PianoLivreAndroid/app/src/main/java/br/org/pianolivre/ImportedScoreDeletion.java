package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Crash-recoverable removal of app-owned imported MusicXML and its optional MIDI source. */
public final class ImportedScoreDeletion {
 private static final String STAGE="imported.delete-pending", NAME="score-name", COMMITTED="committed";
 private ImportedScoreDeletion(){}
 private static String name(String key)throws IOException{
  try{if(!key.equals(SessionFiles.portable(key))||!key.startsWith("imported/"))throw new IllegalArgumentException();}
  catch(IllegalArgumentException ex){throw new IOException("Somente partituras importadas podem ser excluídas",ex);}
  String name=key.substring(9);
  if(name.length()>255||!name.matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.(?i:mxl|xml|musicxml)"))
   throw new IOException("Nome de partitura importada inválido");
  return name;
 }
 private static void move(File from,File to)throws IOException{
  if(Files.isSymbolicLink(from.toPath())||Files.isSymbolicLink(to.toPath()))throw new IOException("Arquivo simbólico não suportado");
  Files.move(from.toPath(),to.toPath(),StandardCopyOption.ATOMIC_MOVE);
 }
 private static void drop(File file)throws IOException{
  if(file.exists()&&!file.delete())throw new IOException("Não foi possível concluir exclusão segura");
 }
 private static void marker(File file,String content)throws IOException{
  try(FileOutputStream out=new FileOutputStream(file)){
   out.write(content.getBytes(StandardCharsets.UTF_8));out.getFD().sync();
  }
 }
 /** Recover interrupted operations before Library is opened or backup is generated. */
 public static synchronized void recover(File root)throws IOException{
  File stage=new File(root,STAGE);
  if(!stage.exists())return;
  if(!stage.isDirectory()||Files.isSymbolicLink(stage.toPath()))throw new IOException("Exclusão anterior inválida");
  File[] entries=stage.listFiles();if(entries==null)throw new IOException("Exclusão anterior inacessível");
  File manifest=new File(stage,NAME);
  if(!manifest.exists()){
   if(entries.length!=0)throw new IOException("Exclusão anterior incompleta: arquivos preservados");
   drop(stage);return;
  }
  if(!manifest.isFile()||manifest.length()>260||Files.isSymbolicLink(manifest.toPath()))throw new IOException("Identificador de exclusão inválido");
  String filename=new String(Files.readAllBytes(manifest.toPath()),StandardCharsets.UTF_8);
  name("imported/"+filename);
  for(File entry:entries){
   String n=entry.getName();
   if(!(n.equals(NAME)||n.equals(COMMITTED)||n.equals(filename)||n.equals(filename+".source.mid"))||!entry.isFile()||Files.isSymbolicLink(entry.toPath()))
    throw new IOException("Arquivo inesperado em exclusão pendente");
  }
  File imported=new File(root,"imported");
  boolean committed=new File(stage,COMMITTED).isFile();
  for(String filenameToCheck:new String[]{filename,filename+".source.mid"}){
   File staged=new File(stage,filenameToCheck);
   if(!staged.exists())continue;
   if(committed)drop(staged);
   else{
    if(!imported.isDirectory())throw new IOException("Biblioteca importada não disponível para recuperação");
    File original=new File(imported,filenameToCheck);
    if(original.exists())throw new IOException("Conflito ao recuperar partitura removida; dados preservados");
    move(staged,original);
   }
  }
  drop(new File(stage,COMMITTED));drop(manifest);drop(stage);
 }
 /** The callback must commit metadata atomically; failure restores staged files. */
 public static synchronized void delete(File root,String key,Runnable updateMetadata)throws IOException{
  if(updateMetadata==null)throw new IOException("Atualização da biblioteca ausente");
  String filename=name(key);
  recover(root);
  File imported=new File(root,"imported"),score=new File(imported,filename),source=new File(imported,filename+".source.mid");
  if(!imported.isDirectory()||!score.isFile()||Files.isSymbolicLink(score.toPath())||Files.isSymbolicLink(source.toPath()))
   throw new IOException("Partitura importada indisponível ou inválida");
  if(source.exists()&&!source.isFile())throw new IOException("Fonte MIDI inválida");
  File stage=new File(root,STAGE);
  if(!stage.mkdir())throw new IOException("Não foi possível preparar exclusão segura");
  boolean committed=false;
  try{
   marker(new File(stage,NAME),filename);
   move(score,new File(stage,filename));
   if(source.exists())move(source,new File(stage,filename+".source.mid"));
   updateMetadata.run();
   marker(new File(stage,COMMITTED),"ok");committed=true;
  }catch(Exception error){
   if(!committed){
    try{recover(root);}catch(IOException rollback){error.addSuppressed(rollback);}
   }
   throw new IOException("Exclusão cancelada; dados preservados quando possível",error);
  }
  // If cleanup fails, the durable committed marker lets next startup finish it.
  recover(root);
 }
}

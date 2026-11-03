package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Consumer;

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
 public static synchronized void recover(File root,Consumer<String> finishMetadata)throws IOException{
  recover(root,finishMetadata,false);
 }
 private static void recover(File root,Consumer<String> finishMetadata,boolean metadataAlreadyUpdated)throws IOException{
  if(finishMetadata==null)throw new IOException("Recuperação de biblioteca sem metadados");
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
  // The durable commit marker precedes the prefs update. If Android stopped
  // between those steps, finish the idempotent metadata update before removing
  // any staged bytes. A failure retains the staging directory for retry.
  if(committed&&!metadataAlreadyUpdated){
   try{finishMetadata.accept("imported/"+filename);}
   catch(RuntimeException failure){throw new IOException("Exclusão pendente: não foi possível concluir os metadados",failure);}
  }
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
 /** The callback must atomically and idempotently detach the supplied score key. */
 public static synchronized void delete(File root,String key,Consumer<String> updateMetadata)throws IOException{
  if(updateMetadata==null)throw new IOException("Atualização da biblioteca ausente");
  String filename=name(key);
  recover(root,updateMetadata);
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
   // No preference is modified until the durable marker exists. Crash before:
   // restore staged files. Crash after: finish metadata and delete bytes.
   marker(new File(stage,COMMITTED),"ok");committed=true;
   updateMetadata.accept(key);
  }catch(Exception error){
   if(!committed){
    try{recover(root,updateMetadata);}catch(IOException rollback){error.addSuppressed(rollback);}
   }
   throw new IOException(committed?"Exclusão pendente de finalização segura":"Exclusão cancelada; arquivos restaurados quando possível",error);
  }
  // Do not commit metadata twice in the normal path. On crash, startup
  // recovery replays it exactly as needed before cleaning remaining bytes.
  recover(root,updateMetadata,true);
 }
}

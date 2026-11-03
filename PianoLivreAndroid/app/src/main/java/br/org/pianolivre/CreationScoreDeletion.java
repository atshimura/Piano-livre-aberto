package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Delete only a generated score while preserving its original .plc MIDI project.
 * The durable marker precedes metadata removal: restart either restores the
 * score (uncommitted) or replays the idempotent library detach (committed).
 */
public final class CreationScoreDeletion {
 private static final String STAGE="creation-score.delete-pending", MANIFEST="project-id", COMMITTED="committed";
 private CreationScoreDeletion(){}
 private static String id(String value)throws IOException{
  if(value==null||!value.matches("[0-9a-fA-F-]{36}"))throw new IOException("Identificador de composição inválido");
  return value.toLowerCase(Locale.ROOT);
 }
 private static void move(File source,File destination)throws IOException{
  if(Files.isSymbolicLink(source.toPath())||Files.isSymbolicLink(destination.toPath()))
   throw new IOException("Arquivo simbólico não suportado");
  Files.move(source.toPath(),destination.toPath(),StandardCopyOption.ATOMIC_MOVE);
 }
 private static void marker(File file,String value)throws IOException{
  try(FileOutputStream output=new FileOutputStream(file)){
   output.write(value.getBytes(StandardCharsets.UTF_8));output.getFD().sync();
  }
 }
 private static void drop(File file)throws IOException{
  if(file.exists()&&!file.delete())throw new IOException("Não foi possível finalizar exclusão da partitura");
 }
 /** Recover before the Library or a backup can access a partial score deletion. */
 public static synchronized void recover(File root,Consumer<String> detach)throws IOException{
  recover(root,detach,false);
 }
 private static void recover(File root,Consumer<String> detach,boolean metadataApplied)throws IOException{
  if(detach==null)throw new IOException("Metadados de recuperação ausentes");
  File stage=new File(root,STAGE);
  if(!stage.exists())return;
  if(!stage.isDirectory()||Files.isSymbolicLink(stage.toPath()))throw new IOException("Staging de partitura inválido");
  File[] entries=stage.listFiles();
  if(entries==null)throw new IOException("Staging de partitura inacessível");
  File manifest=new File(stage,MANIFEST);
  if(!manifest.exists()){
   if(entries.length!=0)throw new IOException("Exclusão incompleta sem identificador; arquivos preservados");
   drop(stage);return;
  }
  if(!manifest.isFile()||Files.isSymbolicLink(manifest.toPath())||manifest.length()>36)
   throw new IOException("Identificador de exclusão corrompido");
  String projectId=id(new String(Files.readAllBytes(manifest.toPath()),StandardCharsets.UTF_8));
  File committedFile=new File(stage,COMMITTED);
  boolean committed=committedFile.exists();
  if(committed&&(!committedFile.isFile()||Files.isSymbolicLink(committedFile.toPath())
   ||!"ok".equals(new String(Files.readAllBytes(committedFile.toPath()),StandardCharsets.UTF_8))))
   throw new IOException("Confirmação de exclusão inválida; bytes preservados");
  for(File entry:entries){
   String name=entry.getName();
   if(!(name.equals(MANIFEST)||name.equals(COMMITTED)||name.equals(projectId+".pls"))
    ||!entry.isFile()||Files.isSymbolicLink(entry.toPath()))
    throw new IOException("Arquivo inesperado na exclusão pendente");
  }
  File sourceDir=new File(root,"creation-scores"),staged=new File(stage,projectId+".pls");
  if(!committed){
   if(staged.exists()){
    if(!sourceDir.isDirectory()||Files.isSymbolicLink(sourceDir.toPath()))
     throw new IOException("Diretório de partituras indisponível para recuperação");
    File original=new File(sourceDir,projectId+".pls");
    if(original.exists())throw new IOException("Conflito ao restaurar partitura; dados preservados");
    move(staged,original);
   }
  }else{
   // Complete metadata before dropping bytes. If prefs fail, leave stage intact.
   if(!metadataApplied)try{detach.accept(projectId);}
   catch(RuntimeException ex){throw new IOException("Metadados ainda não recuperados",ex);}
   if(staged.exists())drop(staged);
  }
  drop(committedFile);drop(manifest);drop(stage);
 }
 /** Caller verifies the active profile owns a saved, non-trashed project. */
 public static synchronized void delete(File root,String identifier,Consumer<String> detach)throws IOException{
  if(detach==null)throw new IOException("Metadados de exclusão ausentes");
  String projectId=id(identifier);
  recover(root,detach);
  File project=new File(new File(root,"creations"),projectId+".plc");
  File sourceDir=new File(root,"creation-scores"),score=new File(sourceDir,projectId+".pls");
  if(!project.isFile()||Files.isSymbolicLink(project.toPath())
   ||!score.isFile()||Files.isSymbolicLink(score.toPath())
   ||!sourceDir.isDirectory()||Files.isSymbolicLink(sourceDir.toPath()))
   throw new IOException("A gravação original e a partitura salva devem estar disponíveis");
  File stage=new File(root,STAGE);
  if(!stage.mkdir())throw new IOException("Não foi possível preparar exclusão segura");
  boolean committed=false;
  try{
   marker(new File(stage,MANIFEST),projectId);
   move(score,new File(stage,projectId+".pls"));
   marker(new File(stage,COMMITTED),"ok");
   committed=true;
   detach.accept(projectId);
  }catch(Exception error){
   if(!committed)try{recover(root,detach);}catch(IOException rollback){error.addSuppressed(rollback);}
   throw new IOException(committed?"Exclusão pendente de recuperação segura":"Exclusão cancelada; partitura preservada quando possível",error);
  }
  // No duplicate prefs commit on the ordinary path. Recovery after a crash
  // replays idempotently because metadataApplied defaults to false.
  recover(root,detach,true);
 }
}

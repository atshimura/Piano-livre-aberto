package br.org.pianolivre;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.io.*;
import java.util.UUID;

/** Exportação MIDI de composições: grava os bytes em cache com SHA-256,
 * abre o seletor de destino e escreve no Uri escolhido. O estado pendente
 * sobrevive a recriação da Activity via saveState/restoreState (mesmas
 * chaves históricas "creationExportId"/"creationExportFile"/"creationExportSha").
 * Comportamento idêntico à versão inline no MainActivity. */
final class ZCreationExport{
 private final MainActivity a;
 private String pendingId,pendingFile,pendingSha;
 private boolean preparing;
 private final java.util.concurrent.ExecutorService disk=java.util.concurrent.Executors.newSingleThreadExecutor();
 private interface Source{byte[] prepare()throws Exception;}
 ZCreationExport(MainActivity activity){a=activity;}
 void saveState(Bundle state){
  state.putString("creationExportId",pendingId);
  state.putString("creationExportFile",pendingFile);
  state.putString("creationExportSha",pendingSha);
 }
 void restoreState(Bundle state){
  pendingId=state.getString("creationExportId");
  pendingFile=state.getString("creationExportFile");
  pendingSha=state.getString("creationExportSha");
  preparing=pendingFile!=null;
 }
 void exportMidi(String id,String title){prepare(id,title,()->MidiStandardFileWriter.write(new CreationProjectStore(a.getFilesDir()).get(id)));}
 void exportTake(CreationProjectStore.Project p){prepare(p.id,p.title,()->MidiStandardFileWriter.write(p));}
 /** Trata o resultado do seletor; devolve true somente para CREATION_MIDI_EXPORT. */
 boolean onActivityResult(int request,int result,Intent data){
  if(request!=MainActivity.CREATION_MIDI_EXPORT)return false;
  if(result!=MainActivity.RESULT_OK||data==null||data.getData()==null){clear();return true;}
  final Uri destination=data.getData();
  final String file=pendingFile,id=pendingId,expected=pendingSha;
  pendingFile=pendingId=pendingSha=null;preparing=false;
  disk.execute(()->{try{
   byte[] bytes=null;
   if(file!=null&&file.matches("creation-export-[a-f0-9-]{36}\\.mid")){
    File cached=new File(a.getCacheDir(),file);
    if(cached.isFile())try(InputStream in=new FileInputStream(cached)){
     byte[] candidate=BackupArchive.readLimited(in,MidiStandardFileWriter.MAX_BYTES);
     if(sha256(candidate).equals(expected))bytes=candidate;
    }
   }
   if(bytes==null)bytes=MidiStandardFileWriter.write(new CreationProjectStore(a.getFilesDir()).get(id));
   try(OutputStream out=a.getContentResolver().openOutputStream(destination,"w")){
    if(out==null)throw new IOException("Destino indisponível");
    out.write(bytes);out.flush();
   }
   if((a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&a.getIntent().getBooleanExtra("creation_export_selftest",false))
    CreationSelfTest.verifyExport(a,destination,bytes);
   deleteCache(file);a.handler.post(()->a.message("Arquivo MIDI exportado com sucesso."));
  }catch(Exception error){
   deleteCache(file);
   a.handler.post(()->a.message("Não foi possível exportar a composição: "+error.getMessage()));
  }});
  return true;
 }
 private void prepare(String id,String title,Source source){
  if(preparing){a.message("Aguarde a exportação em andamento.");return;}
  preparing=true;pendingId=id;
  disk.execute(()->{try{
   byte[] bytes=source.prepare();
   String name="creation-export-"+UUID.randomUUID()+".mid";
   File target=new File(a.getCacheDir(),name);
   try(FileOutputStream out=new FileOutputStream(target)){out.write(bytes);out.getFD().sync();}
   String digest=sha256(bytes);
   a.handler.post(()->{if(a.isDestroyed())return;pendingFile=name;pendingSha=digest;
    String fileName=title.replaceAll("[^\\p{L}\\p{N}._-]+","_").replaceAll("^[_ .-]+|[_ .-]+$","");
    if(fileName.isEmpty())fileName="composicao";
    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("audio/midi");intent.putExtra(Intent.EXTRA_TITLE,fileName+".mid");
    a.startActivityForResult(intent,MainActivity.CREATION_MIDI_EXPORT);});
  }catch(Exception error){
   a.handler.post(()->{clear();a.message("Composição não validada para exportação: "+error.getMessage());});
  }});
 }
 private void deleteCache(String name){
  if(name!=null&&name.matches("creation-export-[a-f0-9-]{36}\\.mid"))new File(a.getCacheDir(),name).delete();
 }
 private void clear(){
  if(pendingFile!=null&&pendingFile.matches("creation-export-[a-f0-9-]{36}\\.mid"))
   new File(a.getCacheDir(),pendingFile).delete();
  pendingFile=pendingSha=pendingId=null;preparing=false;
 }
 static String sha256(byte[] bytes)throws Exception{
  byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
  StringBuilder text=new StringBuilder();
  for(byte b:hash)text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  return text.toString();
 }
 void shutdown(){disk.shutdown();}
}

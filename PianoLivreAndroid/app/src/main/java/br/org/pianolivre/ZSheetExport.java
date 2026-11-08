package br.org.pianolivre;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.io.*;
import java.util.UUID;

/** Exportação da partitura gerada em MusicXML: grava os bytes em cache com
 * SHA-256, abre o seletor de destino e escreve no Uri escolhido (MuseScore,
 * Finale, Flat…). Mesmo padrão de ZCreationExport: estado pendente sobrevive
 * à recriação da Activity via saveState/restoreState. PDF/PNG têm fluxo
 * próprio (CreationScoreExport + ScoreExportProvider). */
final class ZSheetExport{
 private final MainActivity a;
 private String pendingFile,pendingSha;
 private boolean preparing;
 private final java.util.concurrent.ExecutorService disk=java.util.concurrent.Executors.newSingleThreadExecutor();
 ZSheetExport(MainActivity activity){a=activity;}
 void saveState(Bundle state){
  state.putString("sheetExportFile",pendingFile);
  state.putString("sheetExportSha",pendingSha);
 }
 void restoreState(Bundle state){
  pendingFile=state.getString("sheetExportFile");
  pendingSha=state.getString("sheetExportSha");
  preparing=pendingFile!=null;
 }
 /** Música em MusicXML para o usuário escolher onde salvar (.musicxml). */
 void exportMusicXml(byte[] xml,String title){
  if(preparing){a.message("Aguarde a exportação em andamento.");return;}
  preparing=true;
  disk.execute(()->{try{
   if(xml==null||xml.length<40||xml.length>CreationScoreTranscriber.MAX_XML_CHARS)
    throw new IOException("Partitura inválida para exportar");
   String name="sheet-export-"+UUID.randomUUID()+".musicxml";
   File target=new File(a.getCacheDir(),name);
   try(FileOutputStream out=new FileOutputStream(target)){out.write(xml);out.getFD().sync();}
   String digest=sha256(xml);
   a.handler.post(()->{if(a.isDestroyed())return;pendingFile=name;pendingSha=digest;
    String fileName=sanitize(title);
    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("application/vnd.recordare.musicxml+xml");
    intent.putExtra(Intent.EXTRA_TITLE,fileName+".musicxml");
    a.startActivityForResult(intent,MainActivity.CREATION_SHEET_EXPORT);});
  }catch(Exception error){
   a.handler.post(()->{clear();a.message("Não foi possível preparar o MusicXML: "+error.getMessage());});
  }});
 }
 boolean onActivityResult(int request,int result,Intent data){
  if(request!=MainActivity.CREATION_SHEET_EXPORT)return false;
  if(result!=MainActivity.RESULT_OK||data==null||data.getData()==null){clear();return true;}
  final Uri destination=data.getData();
  final String file=pendingFile,expected=pendingSha;
  pendingFile=pendingSha=null;preparing=false;
  disk.execute(()->{try{
   byte[] bytes=null;
   if(file!=null&&file.matches("sheet-export-[a-f0-9-]{36}\\.musicxml")){
    File cached=new File(a.getCacheDir(),file);
    if(cached.isFile())try(InputStream in=new FileInputStream(cached)){
     ByteArrayOutputStream buffer=new ByteArrayOutputStream();
     byte[] chunk=new byte[8192];int n;
     while((n=in.read(chunk))>0)buffer.write(chunk,0,n);
     byte[] candidate=buffer.toByteArray();
     if(candidate.length<=CreationScoreTranscriber.MAX_XML_CHARS&&sha256(candidate).equals(expected))
      bytes=candidate;
    }
   }
   if(bytes==null)throw new IOException("Arquivo de exportação indisponível; gere a partitura novamente");
   try(OutputStream out=a.getContentResolver().openOutputStream(destination,"w")){
    if(out==null)throw new IOException("Destino indisponível");
    out.write(bytes);out.flush();
   }
   deleteCache(file);
   a.handler.post(()->a.message("MusicXML exportado com sucesso. Abra este arquivo no MuseScore, Finale, Flat ou outro editor de partituras."));
  }catch(Exception error){
   deleteCache(file);
   a.handler.post(()->a.message("Não foi possível exportar o MusicXML: "+error.getMessage()));
  }});
  return true;
 }
 static String sanitize(String title){
  String fileName=(title==null?"":title).replaceAll("[^\\p{L}\\p{N}._-]+","_").replaceAll("^[_ .-]+|[_ .-]+$","");
  return fileName.isEmpty()?"partitura":fileName;
 }
 private void deleteCache(String name){
  if(name==null)return;
  if(name.matches("sheet-export-[a-f0-9-]{36}\\.musicxml"))new File(a.getCacheDir(),name).delete();
 }
 private void clear(){
  if(pendingFile!=null&&pendingFile.matches("sheet-export-[a-f0-9-]{36}\\.musicxml"))
   new File(a.getCacheDir(),pendingFile).delete();
  pendingFile=pendingSha=null;preparing=false;
 }
 static String sha256(byte[] bytes)throws Exception{
  byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
  StringBuilder text=new StringBuilder();
  for(byte b:hash)text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
  return text.toString();
 }
 void shutdown(){disk.shutdownNow();}
}

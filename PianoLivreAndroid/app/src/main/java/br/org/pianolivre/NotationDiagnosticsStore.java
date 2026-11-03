package br.org.pianolivre;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import org.json.JSONArray;
import org.json.JSONObject;

/** Local, bounded and atomic; no upload, telemetry, or score contents. */
public final class NotationDiagnosticsStore {
 private static final int MAX_EVENTS=50,MAX_BYTES=256*1024;
 private static final Object IO_LOCK=new Object();
 private final File destination;
 public NotationDiagnosticsStore(File appFiles){destination=new File(appFiles,"notation-errors.json");}
 public JSONArray read()throws IOException{
  synchronized(IO_LOCK){
  if(!destination.isFile())return new JSONArray();
  if(destination.length()>MAX_BYTES)throw new IOException("Histórico de erros excedeu o limite");
  try{return new JSONArray(new String(Files.readAllBytes(destination.toPath()),StandardCharsets.UTF_8));}
  catch(org.json.JSONException e){throw new IOException("Histórico de erros corrompido",e);}
  }
 }
 public void add(NotationFailure failure)throws IOException{
  synchronized(IO_LOCK){
  JSONArray source;
  try{source=read();}catch(IOException corrupted){source=new JSONArray();}
  JSONArray next=new JSONArray();
  int start=Math.max(0,source.length()-MAX_EVENTS+1);
  for(int i=start;i<source.length();i++){
   JSONObject previous=source.optJSONObject(i);
   if(previous!=null)next.put(previous);
  }
  next.put(failure.json());
  while(next.length()>MAX_EVENTS||next.toString().getBytes(StandardCharsets.UTF_8).length>MAX_BYTES){
   JSONArray smaller=new JSONArray();for(int i=1;i<next.length();i++)smaller.put(next.opt(i));
   next=smaller;
  }
  atomic(next.toString());
  }
 }
 private void atomic(String value)throws IOException{
  File temp=new File(destination.getParentFile(),destination.getName()+".tmp");
  try{
   try(FileOutputStream stream=new FileOutputStream(temp)){
    stream.write(value.getBytes(StandardCharsets.UTF_8));stream.getFD().sync();
   }
   Files.move(temp.toPath(),destination.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
  }finally{temp.delete();}
 }
 public void clear()throws IOException{synchronized(IO_LOCK){atomic("[]");}}
 public String report()throws IOException{
  synchronized(IO_LOCK){
  JSONArray list=read();StringBuilder output=new StringBuilder("Piano Livre — incidentes de notação (sem partituras e sem MIDI)\n");
  for(int i=0;i<list.length();i++){
   JSONObject item=list.optJSONObject(i);if(item==null)continue;
   output.append("\n").append(item.optString("incident")).append(" · ")
    .append(item.optString("code")).append(" · ").append(item.optLong("at"))
    .append("\nEtapa: ").append(item.optString("stage"))
    .append("\nDetalhe: ").append(item.optString("details"))
    .append("\nOrigem: ").append(item.optString("origin"))
    .append("\nSHA-256 XML: ").append(item.optString("xmlSha256")).append("\n");
  }
  return output.toString();
  }
 }
}

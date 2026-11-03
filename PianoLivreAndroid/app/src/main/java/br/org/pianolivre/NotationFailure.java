package br.org.pianolivre;

import android.os.Build;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONObject;

/** Private metadata for a failed OSMD attempt, never MusicXML or MIDI bytes. */
public final class NotationFailure {
 public final String id,code,stage,details,origin,xmlSha256,appVersion,device;
 public final long occurredAt;
 public final int measureCount,noteCount;
 public final long generation;
 public final int width,height;
 private static String clean(String text,int cap){
  if(text==null)return "";
  String s=text.replaceAll("(?is)<[^>]{0,2048}>","[conteúdo omitido]")
   .replaceAll("(?i)(?:file|content|https?)://[^\\s]+","[local omitido]")
   .replaceAll("(?:[A-Za-z]:\\\\|/)[^\\s:]{4,}","[caminho omitido]")
   .replaceAll("[\\p{Cntrl}&&[^\\n\\t]]"," ");
  return s.length()>cap?s.substring(0,cap):s;
 }
 private static String hash(String source){
  if(source==null)return "";
  try{
   byte[] digest=MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
   StringBuilder hex=new StringBuilder(64);
   for(byte x:digest)hex.append(String.format(Locale.ROOT,"%02x",x&255));
   return hex.toString();
  }catch(Exception ignored){return "";}
 }
 public NotationFailure(String code,String stage,String details,String origin,String xml,
                       int measures,int notes,long generation,int width,int height,String appVersion){
  id=UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
  this.code=code!=null&&code.matches("PL-NOT-[A-Z-]{2,28}")?code:"PL-NOT-UNKNOWN";
  this.stage=clean(stage,64);this.details=clean(details,300);
  this.origin=origin!=null&&origin.matches("creation|study|free|library")?origin:"unknown";
  xmlSha256=hash(xml);measureCount=Math.max(0,measures);noteCount=Math.max(0,notes);
  this.generation=generation;this.width=Math.max(0,width);this.height=Math.max(0,height);
  this.appVersion=clean(appVersion,48);device=clean(Build.MANUFACTURER+" "+Build.MODEL+" / Android "+Build.VERSION.RELEASE,100);
  occurredAt=System.currentTimeMillis();
 }
 public JSONObject json(){
  JSONObject record=new JSONObject();
  try{
   record.put("incident",id);record.put("at",occurredAt);record.put("code",code);
   record.put("stage",stage);record.put("details",details);record.put("origin",origin);
   record.put("xmlSha256",xmlSha256);record.put("measures",measureCount);
   record.put("notes",noteCount);record.put("generation",generation);
   record.put("viewportWidth",width);record.put("viewportHeight",height);
   record.put("appVersion",appVersion);record.put("device",device);
  }catch(org.json.JSONException error){throw new IllegalStateException(error);}
  return record;
 }
 public String report(){
  return "Ocorrência "+id+" · "+code+"\nEtapa: "+stage+"\nDetalhe: "+details+
   "\nOrigem: "+origin+"\nVersão: "+appVersion+"\nDispositivo: "+device+
   "\nSHA-256 do MusicXML: "+xmlSha256+"\nCompassos: "+measureCount+
   " · notas escritas: "+noteCount+"\nViewport: "+width+"×"+height+
   "\nA partitura e o MIDI original foram preservados.";
 }
}

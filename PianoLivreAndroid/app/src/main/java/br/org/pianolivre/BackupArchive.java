package br.org.pianolivre;

import android.content.SharedPreferences;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Offline, manually initiated, all-profiles archive with v1 compatibility.
 * Files are fully decoded/verified BEFORE current data is touched.
 * A persistent journal allows rollback if Android kills the process mid-restore.
 *
 * No sync, network, automatic merge, or background activity is introduced.
 */
public final class BackupArchive {
 private BackupArchive(){}
 public static final int VERSION=2;
 private static final int MAX_ITEMS=200;
 private static final int MAX_FILE=16*1024*1024;
 private static final int MAX_TOTAL=64*1024*1024;
 private static final int MAX_JSON=95*1024*1024;
 private static final String STAGE="imported.restore-stage";
 private static final String ROLLBACK="imported.restore-rollback";
 private static final String JOURNAL="restore-pending.json";
 private static final String JOURNAL_TMP="restore-pending.tmp";
 private static final String FILES="imported";
 public static byte[] readLimited(InputStream input,int max)throws IOException{
  if(input==null)throw new IOException("Não foi possível abrir o documento");
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();
  byte[] block=new byte[8192];int n;
  while((n=input.read(block))!=-1){
   if(n+buffer.size()>max)throw new IOException("Backup excede o limite permitido");
   buffer.write(block,0,n);
  }
  return buffer.toByteArray();
 }
 private static byte[] readFile(File f,int max)throws IOException{
  try(InputStream in=new FileInputStream(f)){return readLimited(in,max);}
 }
 private static String sha256(byte[] content)throws Exception{
  byte[] hash=MessageDigest.getInstance("SHA-256").digest(content);
  StringBuilder out=new StringBuilder();
  for(byte b:hash)out.append(String.format(Locale.US,"%02x",b&255));
  return out.toString();
 }
 private static boolean safeName(String name){
  return name!=null&&name.length()>0&&name.length()<=120&&
    !name.equals(".")&&!name.equals("..")&&
    name.matches("[A-Za-z0-9][A-Za-z0-9._-]*")&&
    !name.contains("..");
 }
 private static void validPrefsKey(String key)throws IOException{
  if(key==null||key.isEmpty()||key.length()>180||key.indexOf('\u0000')>=0)
   throw new IOException("Nome de preferência inválido");
 }
 private static JSONObject typedPrefs(Map<String,?> values)throws Exception{
  JSONObject out=new JSONObject();
  for(Map.Entry<String,?> ent:values.entrySet()){
   String key=ent.getKey();validPrefsKey(key);Object v=ent.getValue();
   if(v==null)continue;
   JSONObject record=new JSONObject();
   if(v instanceof Boolean)record.put("type","boolean").put("value",v);
   else if(v instanceof Integer)record.put("type","int").put("value",String.valueOf(v));
   else if(v instanceof Long)record.put("type","long").put("value",String.valueOf(v));
   else if(v instanceof Float)record.put("type","float").put("value",String.valueOf(v));
   else if(v instanceof String)record.put("type","string").put("value",v);
   else if(v instanceof Set){
    JSONArray list=new JSONArray();
    for(Object s:(Set<?>)v){
     if(!(s instanceof String))throw new IOException("Tipo de preferência inválido");
     list.put(s);
    }
    record.put("type","strings").put("value",list);
   }else throw new IOException("Tipo de preferência não suportado");
   out.put(key,record);
  }
  return out;
 }
 private static Map<String,Object> decodePrefs(JSONObject obj,boolean typed)throws Exception{
  if(obj==null||obj.length()>5000)throw new IOException("Preferências inválidas");
  Map<String,Object> result=new LinkedHashMap<>();
  Iterator<String> names=obj.keys();
  while(names.hasNext()){
   String k=names.next();validPrefsKey(k);
   Object raw=obj.get(k),v;
   if(typed){
    if(!(raw instanceof JSONObject))throw new IOException("Preferência sem tipo: "+k);
    JSONObject record=(JSONObject)raw;String type=record.getString("type");
    Object encoded=record.get("value");
    switch(type){
     case "boolean":if(!(encoded instanceof Boolean))throw new IOException("Boolean inválido");v=encoded;break;
     case "int":v=Integer.parseInt(String.valueOf(encoded));break;
     case "long":v=Long.parseLong(String.valueOf(encoded));break;
     case "float":v=Float.parseFloat(String.valueOf(encoded));break;
     case "string":v=record.getString("value");break;
     case "strings":{
      JSONArray values=record.getJSONArray("value");
      if(values.length()>10000)throw new IOException("Conjunto de valores grande demais");
      Set<String> set=new HashSet<>();
      for(int i=0;i<values.length();i++)set.add(values.getString(i));
      v=set;break;
     }
     default:throw new IOException("Tipo desconhecido: "+type);
    }
   }else{
    // Legacy v1 serialized primitive prefs as untyped JSON.
    // Session position is the raw IEEE-754 long bits, not a JSON decimal.
    if(k.matches("user_\\d+_position"))v=Long.parseLong(String.valueOf(raw));
    else if(raw instanceof Boolean||raw instanceof String)v=raw;
    else if(raw instanceof Number){
     long n=Long.parseLong(String.valueOf(raw));
     if(n>=Integer.MIN_VALUE&&n<=Integer.MAX_VALUE)v=Integer.valueOf((int)n);
     else v=Long.valueOf(n);
    }else throw new IOException("Preferência antiga inválida: "+k);
   }
   result.put(k,v);
  }
  Object p=result.get("profiles");
  if(p!=null){
   if(!(p instanceof String))throw new IOException("Perfis inválidos");
   JSONArray profiles=new JSONArray((String)p);
   if(profiles.length()<1||profiles.length()>20)throw new IOException("Quantidade inválida de perfis");
   for(int i=0;i<profiles.length();i++)
    if(profiles.getString(i).trim().isEmpty())throw new IOException("Nome de perfil vazio");
  }
  for(Map.Entry<String,Object> item:result.entrySet())if(item.getKey().matches("user_\\d+_library")){
   if(!(item.getValue() instanceof String))throw new IOException("Biblioteca inválida");
   try{LibraryStore.decode((String)item.getValue());}catch(IllegalArgumentException ex){throw new IOException("Biblioteca inválida",ex);}
  }
  Object profileRecords=result.get("profiles_v2");
  if(profileRecords!=null){
   if(!(profileRecords instanceof String))throw new IOException("Perfis v2 inválidos");
   try{
    List<LocalProfiles.Profile> list=LocalProfiles.decode((String)profileRecords);
    Object next=result.get("profiles_next_id");int maxId=0;for(LocalProfiles.Profile profile:list)maxId=Math.max(maxId,profile.id);
    if(next!=null&&(!(next instanceof Integer)||((Integer)next)<=maxId))throw new IOException("Sequência de perfis inválida");
    Object active=result.get("active_profile");
    if(active!=null){boolean found=false;for(LocalProfiles.Profile profile:list)if(active instanceof Integer&&profile.id==((Integer)active))found=true;
     if(!found)throw new IOException("Perfil ativo inexistente");}
   }catch(IllegalArgumentException ex){throw new IOException("Perfis v2 inválidos",ex);}
  }
  return result;
 }
 private static void applyPrefs(SharedPreferences prefs,Map<String,Object> values)throws IOException{
  SharedPreferences.Editor ed=prefs.edit().clear();
  for(Map.Entry<String,Object> item:values.entrySet()){
   String k=item.getKey();Object v=item.getValue();
   if(v instanceof Boolean)ed.putBoolean(k,(Boolean)v);
   else if(v instanceof Integer)ed.putInt(k,(Integer)v);
   else if(v instanceof Long)ed.putLong(k,(Long)v);
   else if(v instanceof Float)ed.putFloat(k,(Float)v);
   else if(v instanceof String)ed.putString(k,(String)v);
   else if(v instanceof Set){
    @SuppressWarnings("unchecked") Set<String> strings=(Set<String>)v;
    ed.putStringSet(k,strings);
   }else throw new IOException("Preferência inválida");
  }
  if(!ed.commit())throw new IOException("Falha ao gravar preferências");
 }
 public static JSONObject create(SharedPreferences prefs,File root)throws Exception{
  JSONObject backup=new JSONObject();
  backup.put("version",VERSION).put("prefs",typedPrefs(prefs.getAll()));
  JSONArray files=new JSONArray();long total=0;
  File dir=new File(root,FILES);File[] list=dir.listFiles();
  if(list!=null){
   Arrays.sort(list,Comparator.comparing(File::getName));
   if(list.length>MAX_ITEMS)throw new IOException("Muitos arquivos importados para um backup");
   for(File file:list){
    if(!file.isFile()||!safeName(file.getName()))throw new IOException("Arquivo importado inválido");
    if(file.length()>MAX_FILE||total+file.length()>MAX_TOTAL)throw new IOException("Backup grande demais");
    byte[] bytes=readFile(file,MAX_FILE);total+=bytes.length;
    files.put(new JSONObject().put("name",file.getName())
     .put("content",Base64.encodeToString(bytes,Base64.NO_WRAP))
     .put("sha256",sha256(bytes)));
   }
  }
  backup.put("imported",files);
  if(backup.toString().getBytes(StandardCharsets.UTF_8).length>MAX_JSON)
   throw new IOException("Backup excede limite de exportação");
  return backup;
 }
 private static final class Validated {
  final Map<String,Object> prefs;
  final LinkedHashMap<String,byte[]> files;
  Validated(Map<String,Object> p,LinkedHashMap<String,byte[]> f){prefs=p;files=f;}
 }
 private static Validated validate(JSONObject backup)throws Exception{
  if(backup==null)throw new IOException("Backup vazio");
  int version=backup.getInt("version");
  if(version!=1&&version!=VERSION)throw new IOException("Versão do backup não suportada: "+version);
  Map<String,Object> p=decodePrefs(backup.getJSONObject("prefs"),version==VERSION);
  JSONArray imported=backup.optJSONArray("imported");
  if(imported==null)throw new IOException("Lista de arquivos ausente");
  if(imported.length()>MAX_ITEMS)throw new IOException("Muitos arquivos no backup");
  LinkedHashMap<String,byte[]> files=new LinkedHashMap<>();
  long total=0;
  for(int i=0;i<imported.length();i++){
   JSONObject file=imported.getJSONObject(i);String name=file.getString("name");
   if(!safeName(name)||files.containsKey(name))throw new IOException("Nome de arquivo repetido ou inválido");
   String content=file.getString("content");
   if(content.length()>MAX_FILE*4L/3L+100)throw new IOException("Arquivo grande demais");
   byte[] bytes;
   try{bytes=Base64.decode(content,Base64.DEFAULT);}catch(IllegalArgumentException ex){
    throw new IOException("Dados de arquivo inválidos",ex);
   }
   total+=bytes.length;
   if(bytes.length>MAX_FILE||total>MAX_TOTAL)throw new IOException("Arquivos excedem o limite");
   if(version==VERSION&&!sha256(bytes).equalsIgnoreCase(file.getString("sha256")))
    throw new IOException("Integridade incorreta: "+name);
   files.put(name,bytes);
  }
  return new Validated(p,files);
 }
 public static String summary(JSONObject backup)throws Exception{
  Validated v=validate(backup);
  return "Perfis e preferências: "+v.prefs.size()+" registros. Partituras importadas: "+v.files.size()+
    ". A restauração substituirá os dados locais deste aparelho.";
 }
 private static void deleteTree(File dir)throws IOException{
  if(!dir.exists())return;
  if(dir.isDirectory()){
   File[] list=dir.listFiles();if(list==null)throw new IOException("Não foi possível listar "+dir.getName());
   for(File f:list)deleteTree(f);
  }
  if(!dir.delete())throw new IOException("Não foi possível remover "+dir.getName());
 }
 private static void writeFile(File target,byte[] bytes)throws IOException{
  try(FileOutputStream out=new FileOutputStream(target)){out.write(bytes);out.getFD().sync();}
 }
 private static void writeJournal(File root,boolean hadFiles,SharedPreferences prefs)throws Exception{
  JSONObject json=new JSONObject().put("previousPrefs",typedPrefs(prefs.getAll()))
    .put("hadImported",hadFiles);
  File tmp=new File(root,JOURNAL_TMP);
  writeFile(tmp,json.toString().getBytes(StandardCharsets.UTF_8));
  File marker=new File(root,JOURNAL);
  if(!tmp.renameTo(marker))throw new IOException("Não foi possível criar registro de recuperação");
 }
 /**
  * Roll back an interrupted restore before any user data is accessed.
  * A completed transaction deletes its journal BEFORE deleting the old files.
  */
 public static void recover(SharedPreferences prefs,File root)throws Exception{
  File marker=new File(root,JOURNAL);
  File old=new File(root,ROLLBACK);
  File dest=new File(root,FILES);
  if(marker.isFile()){
   JSONObject journal=new JSONObject(new String(readFile(marker,MAX_JSON),StandardCharsets.UTF_8));
   Map<String,Object> before=decodePrefs(journal.getJSONObject("previousPrefs"),true);
   boolean had=journal.getBoolean("hadImported");
   if(old.exists()){
    deleteTree(dest);
    if(!old.renameTo(dest))throw new IOException("Não foi possível restaurar arquivos anteriores");
   }else if(!had){
    deleteTree(dest);
   }
   applyPrefs(prefs,before);
   if(!marker.delete())throw new IOException("Não foi possível terminar recuperação");
  }
  deleteTree(new File(root,STAGE));
  deleteTree(new File(root,JOURNAL_TMP));
  // A transaction with no marker committed successfully; remove old leftovers.
  deleteTree(old);
 }
 public static void restore(SharedPreferences prefs,File root,JSONObject backup)throws Exception{
  // Validation is deliberately BEFORE any deletion or preference mutation.
  Validated target=validate(backup);
  recover(prefs,root);
  File stage=new File(root,STAGE),old=new File(root,ROLLBACK),dest=new File(root,FILES);
  if(!stage.mkdir())throw new IOException("Não foi possível preparar a restauração");
  boolean had=dest.exists();
  try{
   for(Map.Entry<String,byte[]> file:target.files.entrySet())
    writeFile(new File(stage,file.getKey()),file.getValue());
   writeJournal(root,had,prefs);
   if(had&&!dest.renameTo(old))throw new IOException("Não foi possível preservar os arquivos anteriores");
   if(!stage.renameTo(dest))throw new IOException("Não foi possível instalar os arquivos");
   applyPrefs(prefs,target.prefs);
   File marker=new File(root,JOURNAL);
   if(!marker.delete())throw new IOException("Não foi possível finalizar restauração");
   // The swap is already committed; stale rollback files are safe to retry
   // removing on the next launch. Do not report a committed restore as failed.
   try{deleteTree(old);}catch(IOException cleanup){
    android.util.Log.w("PianoLivreBackup","Limpeza de cópia anterior será repetida",cleanup);
   }
  }catch(Exception error){
   try{recover(prefs,root);}catch(Exception rollback){error.addSuppressed(rollback);}
   throw error;
  }
 }
}

package br.org.pianolivre;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Debug-only isolated, device-side tests of real SharedPreferences and JSON.
 * Uses a unique cache directory and test-only preferences: NEVER user data.
 */
public final class BackupArchiveSelfTest {
 private BackupArchiveSelfTest(){}
 private static int count=0;
 private static void ok(boolean valid,String message){
  count++;if(!valid)throw new AssertionError(message);
 }
 private static byte[] bytes(String value){return value.getBytes(StandardCharsets.UTF_8);}
 private static void write(File path,byte[] content)throws IOException{
  try(FileOutputStream out=new FileOutputStream(path)){out.write(content);}
 }
 private static byte[] read(File path)throws IOException{
  try(InputStream in=new FileInputStream(path)){
   ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buff=new byte[4096];int n;
   while((n=in.read(buff))!=-1)b.write(buff,0,n);
   return b.toByteArray();
  }
 }
 private static void rm(File file){
  if(file.isDirectory()){File[] files=file.listFiles();if(files!=null)for(File f:files)rm(f);}
  file.delete();
 }
 public static int run(Context context)throws Exception{
  File root=new File(context.getCacheDir(),"backup_selftest_v2");
  rm(root);
  if(!root.mkdirs())throw new IOException("Could not create isolated test dir");
  File imported=new File(root,"imported");if(!imported.mkdirs())throw new IOException("No import dir");
  File note=new File(imported,"partitura.xml");
  write(note,bytes("<music>teste</music>"));
  SharedPreferences prefs=context.getSharedPreferences("piano_backup_selftest",Context.MODE_PRIVATE);
  long position=Double.doubleToLongBits(124.375);
  prefs.edit().clear().putString("profiles","[\"Usuário 1\",\"Aluno 2\"]")
    .putInt("user_0_mode",2).putLong("user_0_position",position)
    .putBoolean("user_0_keyboard",true).putFloat("user_0_volume",.5f)
    .putStringSet("test_set",new HashSet<>(Arrays.asList("A","B"))).commit();
  try{
   JSONObject original=BackupArchive.create(prefs,root);
   ok(original.getInt("version")==2,"v2");
   ok(original.getJSONArray("imported").length()==1,"saved one file");
   prefs.edit().putInt("user_0_mode",0).commit();
   write(note,bytes("overwritten"));
   write(new File(imported,"stale.xml"),bytes("must be removed"));
   BackupArchive.restore(prefs,root,original);
   ok(prefs.getInt("user_0_mode",-1)==2,"int recovered");
   ok(prefs.getLong("user_0_position",0)==position,"long float bits recovered");
   ok(prefs.getBoolean("user_0_keyboard",false),"boolean recovered");
   ok(Math.abs(prefs.getFloat("user_0_volume",0)-.5f)<.0001,"float recovered");
   ok(prefs.getStringSet("test_set",Collections.emptySet()).size()==2,"set recovered");
   ok(Arrays.equals(read(note),bytes("<music>teste</music>")),"file bytes recovered");
   ok(!new File(imported,"stale.xml").exists(),"old files removed");
   ok(!new File(root,"restore-pending.json").exists(),"journal removed after success");

   JSONObject tampered=new JSONObject(original.toString());
   tampered.getJSONArray("imported").getJSONObject(0).put("content",
    Base64.encodeToString(bytes("corrupted"),Base64.NO_WRAP));
   boolean rejected=false;
   try{BackupArchive.restore(prefs,root,tampered);}catch(Exception ex){rejected=true;}
   ok(rejected,"SHA-256 corruption rejected");
   ok(Arrays.equals(read(note),bytes("<music>teste</music>")),"rejection preserves original file");
   ok(prefs.getInt("user_0_mode",-1)==2,"rejection preserves prefs");

   JSONObject unsafe=new JSONObject(original.toString());
   unsafe.getJSONArray("imported").getJSONObject(0).put("name","../escape.xml");
   rejected=false;
   try{BackupArchive.summary(unsafe);}catch(Exception ex){rejected=true;}
   ok(rejected,"path traversal rejected");

   for(String key:new String[]{"user_0_mode","user_0_keyboard","user_0_position"}){
    JSONObject wrong=new JSONObject(original.toString());wrong.getJSONObject("prefs").put(key,new JSONObject().put("type","string").put("value","errado"));
    rejected=false;try{BackupArchive.restore(prefs,root,wrong);}catch(Exception ex){rejected=true;}
    ok(rejected&&prefs.getInt("user_0_mode",-1)==2&&Arrays.equals(read(note),bytes("<music>teste</music>")),"wrong session type rejected before mutation: "+key);
   }
   JSONObject nan=new JSONObject(original.toString());nan.getJSONObject("prefs").put("user_0_tempo",new JSONObject().put("type","long").put("value",String.valueOf(Double.doubleToLongBits(Double.NaN))));
   rejected=false;try{BackupArchive.restore(prefs,root,nan);}catch(Exception ex){rejected=true;}
   ok(rejected&&prefs.getInt("user_0_mode",-1)==2,"nonfinite tempo rejected before mutation");
   JSONObject paths=new JSONObject(original.toString());paths.getJSONObject("prefs").put("user_0_file",new JSONObject().put("type","string").put("value","/data/user/0/br.org.pianolivre/files/imported/partitura.xml"));
   BackupArchive.restore(prefs,root,paths);
   ok(prefs.getString("user_0_file","").equals("imported/partitura.xml")&&SessionFiles.resolve(root,prefs.getString("user_0_file","")).equals(note.getAbsolutePath()),"portable path restored into current package, original package untouched");
   JSONObject v1=new JSONObject().put("version",1);
   v1.put("prefs",new JSONObject().put("profiles","[\"Usuário 1\"]")
    .put("user_0_mode",2).put("user_0_position",position));
   v1.put("imported",new JSONArray().put(new JSONObject()
    .put("name","legacy.xml").put("content",Base64.encodeToString(bytes("legacy"),Base64.NO_WRAP))));
   BackupArchive.restore(prefs,root,v1);
   ok(prefs.getInt("user_0_mode",-1)==2,"legacy int type");
   ok(prefs.getLong("user_0_position",0)==position,"legacy long type");
   ok(Arrays.equals(read(new File(imported,"legacy.xml")),bytes("legacy")),"legacy file restored");

   // Simulate power loss AFTER current prefs/files were replaced but BEFORE
   // restore completion. Recovery must reinstate the previous local snapshot.
   JSONObject journal=new JSONObject();
   journal.put("hadImported",true).put("previousPrefs",v1.getJSONObject("prefs"));
   // Legacy prefs need typed encoding in the journal (v2).
   JSONObject prior=BackupArchive.create(prefs,root).getJSONObject("prefs");
   journal.put("previousPrefs",prior);
   File old=new File(root,"imported.restore-rollback");
   ok(imported.renameTo(old),"move old files to rollback dir");
   ok(imported.mkdir(),"create half-restored dir");
   write(new File(imported,"incorrect.xml"),bytes("bad"));
   prefs.edit().putString("profiles","[\"Errado\"]").commit();
   write(new File(root,"restore-pending.json"),bytes(journal.toString()));
   BackupArchive.recover(prefs,root);
   ok(prefs.getString("profiles","").equals("[\"Usuário 1\"]"),"crash restored profiles");
   ok(new File(imported,"legacy.xml").exists(),"crash restored files");
   ok(!new File(imported,"incorrect.xml").exists(),"crash removed partial files");
   ok(!new File(root,"restore-pending.json").exists(),"crash removed marker");
   String[] legacyNames={"_etude..mxl","-file.mxl",".hidden.mxl",new String(new char[240]).replace('\0','_')+".mxl"};
   for(String name:legacyNames)write(new File(imported,name),bytes("synthetic legacy filename"));
   prefs.edit().putString("user_0_file","/data/data/previous.package/files/imported/"+legacyNames[0]).commit();
   JSONObject named=BackupArchive.create(prefs,root);BackupArchive.restore(prefs,root,named);
   for(String name:legacyNames)ok(Arrays.equals(read(new File(imported,name)),bytes("synthetic legacy filename")),"legacy flat filename backed up byte-for-byte");
   ok(prefs.getString("user_0_file","").equals("imported/"+legacyNames[0]),"legacy filename session remapped without renaming source");
   LibraryStore metadata=new LibraryStore(prefs,0);metadata.toggleFavorite("imported/"+legacyNames[3]);
   ok(metadata.snapshot().favorites.contains("imported/"+legacyNames[3]),"long legacy filename favorite preserved");
   File invalidName=new File(imported,"unsafe name.xml");write(invalidName,bytes("synthetic invalid name"));
   final int[] opened={0};rejected=false;
   try{BackupArchive.export(prefs,root,()->{opened[0]++;return new ByteArrayOutputStream();});}catch(Exception ex){rejected=true;}
   finally{invalidName.delete();}
   ok(rejected&&opened[0]==0,"archive preparation failure never opens/truncates external backup destination");
   final boolean[] closed={false};ByteArrayOutputStream destination=new ByteArrayOutputStream(){@Override public void close()throws IOException{closed[0]=true;super.close();}};
   BackupArchive.export(prefs,root,()->destination);
   JSONObject exported=new JSONObject(new String(destination.toByteArray(),StandardCharsets.UTF_8));
   ok(closed[0]&&exported.getInt("version")==2&&exported.getJSONArray("imported").length()==5,"prepared archive written completely and destination closed");
   return count;
  }finally{
   rm(root);prefs.edit().clear().commit();
  }
 }
}

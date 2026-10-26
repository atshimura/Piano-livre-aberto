package br.org.pianolivre;

import android.content.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Isolated organization + transactional backup test, no real scores/preferences. */
final class LibrarySelfTest {
 private static int checks;
 private static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static int run(Context context)throws Exception{
  SharedPreferences prefs=context.getSharedPreferences("piano_library_selftest",Context.MODE_PRIVATE);
  File root=new File(context.getCacheDir(),"library_selftest");root.mkdirs();
  File folder=new File(root,"imported");folder.mkdirs();File shared=new File(folder,"meu.mxl");
  byte[] content="synthetic-fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  try(FileOutputStream out=new FileOutputStream(shared)){out.write(content);}
  prefs.edit().clear().putString("profiles","[\"A\",\"B\"]").putInt("active_profile",0).commit();
  try{
   LibraryStore store=new LibraryStore(prefs,0),other=new LibraryStore(prefs,1);
   String asset="scores/exercicio.mxl",imported=LibrarySearch.key(shared.getAbsolutePath());
   check(store.snapshot().favorites.isEmpty()&&!prefs.contains("user_0_library"),"empty metadata is read-only");
   store.toggleFavorite(asset);store.toggleFavorite(imported);
   check(store.snapshot().favorites.contains(imported)&&other.snapshot().favorites.isEmpty(),"portable identity and profile isolation");
   String id=store.create("Estudos");store.toggleMember(id,asset);store.rename(id,"Leitura");
   LibraryStore.Snapshot snapshot=store.snapshot();
   check(snapshot.collections.get(id).name.equals("Leitura")&&snapshot.collections.get(id).scores.contains(asset),"rename retains collection identity and membership");
   try{snapshot.favorites.clear();throw new AssertionError("mutable favorites");}catch(UnsupportedOperationException expected){checks++;}
   try{snapshot.collections.get(id).scores.clear();throw new AssertionError("mutable collection");}catch(UnsupportedOperationException expected){checks++;}
   Map<String,?> before=prefs.getAll();boolean rejected=false;
   try{store.create("leitura");}catch(IllegalArgumentException ex){rejected=true;}
   check(rejected&&before.equals(prefs.getAll()),"duplicate collection rejected before writing");
   JSONObject backup=BackupArchive.create(prefs,root);store.delete(id);store.toggleFavorite(asset);
   check(store.snapshot().collections.isEmpty()&&store.snapshot().favorites.contains(imported)&&shared.exists()&&shared.length()==content.length,"delete metadata keeps favorites and shared file");
   BackupArchive.restore(prefs,root,backup);store=new LibraryStore(prefs,0);
   check(store.snapshot().collections.get(id).scores.contains(asset)&&store.snapshot().favorites.contains(asset)&&Arrays.equals(content,java.nio.file.Files.readAllBytes(shared.toPath())),"organization and file bytes survive backup/restore");
   before=prefs.getAll();JSONObject bad=new JSONObject(backup.toString());
   bad.getJSONObject("prefs").getJSONObject("user_0_library").put("value","{\"version\":1,\"favorites\":[\"imported/../secret\"],\"collections\":[]}");
   rejected=false;try{BackupArchive.restore(prefs,root,bad);}catch(Exception ex){rejected=true;}
   check(rejected&&before.equals(prefs.getAll()),"invalid library key rejected before restore");
   store.toggleMember(id,asset);check(!store.snapshot().collections.get(id).scores.contains(asset),"membership toggles independently");
   return checks;
  }finally{prefs.edit().clear().commit();}
 }
}

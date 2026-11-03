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
   String creationId="123e4567-e89b-12d3-a456-426614174000";
   // Reading a fresh profile must not create metadata. Assert this BEFORE
   // exercising explicit additions and favorites, which intentionally persist.
   check(store.snapshot().favorites.isEmpty()&&!prefs.contains("user_0_library"),"empty metadata is read-only");
   check(!store.hasCreation(creationId),"creation not automatically added");
   store.addCreation(creationId);store.addCreation(creationId);
   check(store.hasCreation(creationId)&&store.snapshot().addedCreations.size()==1,"explicit addition idempotent");
   check(!other.hasCreation(creationId),"creation catalog is profile-isolated");
   store.toggleFavorite("creation-score:"+creationId);
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
   check(store.hasCreation(creationId)&&store.snapshot().favorites.contains("creation-score:"+creationId),"linked created score and favorite survive backup/restore");
   before=prefs.getAll();JSONObject bad=new JSONObject(backup.toString());
   bad.getJSONObject("prefs").getJSONObject("user_0_library").put("value","{\"version\":1,\"favorites\":[\"imported/../secret\"],\"collections\":[]}");
   rejected=false;try{BackupArchive.restore(prefs,root,bad);}catch(Exception ex){rejected=true;}
   check(rejected&&before.equals(prefs.getAll()),"invalid library key rejected before restore");
   store.toggleMember(id,asset);check(!store.snapshot().collections.get(id).scores.contains(asset),"membership toggles independently");
   store.setHidden(asset,true);
   check(store.snapshot().hiddenScores.contains(asset)&&!other.snapshot().hiddenScores.contains(asset),"bundled score hidden per profile");
   check(store.snapshot().favorites.contains(asset),"hiding preserves favorite");
   // Old v1 JSON still decodes; new hiddenScores is saved in existing backup v4.
   JSONObject hiddenBackup=BackupArchive.create(prefs,root);
   store.setHidden(asset,false);
   BackupArchive.restore(prefs,root,hiddenBackup);
   check(store.snapshot().hiddenScores.contains(asset),"hidden state survives backup/restore");
   store.setHidden(asset,false);
   check(!store.snapshot().hiddenScores.contains(asset),"hidden built-in score can be restored");
   store.toggleMember(id,"creation-score:"+creationId);
   store.removeCreation(creationId);
   check(!store.hasCreation(creationId)&&!store.snapshot().favorites.contains("creation-score:"+creationId),"unlinked creation cleans stale favorite");
   check(!store.snapshot().collections.get(id).scores.contains("creation-score:"+creationId),"unlinked creation cleans stale collection");
   other.toggleFavorite(imported);String otherCollection=other.create("Outro");
   other.toggleMember(otherCollection,imported);
   File midiSource=new File(folder,"meu.mxl.source.mid");
   try(FileOutputStream out=new FileOutputStream(midiSource)){out.write(content);}
   prefs.edit().putString("user_0_file",shared.getAbsolutePath()).putString("user_1_file",imported).commit();
   ImportedScoreDeletion.delete(root,imported,scoreKey->LibraryStore.removeImportedEveryProfile(prefs,scoreKey));
   check(!shared.exists()&&!midiSource.exists(),"imported score and matching MIDI source deleted");
   check(!store.snapshot().favorites.contains(imported)&&!other.snapshot().favorites.contains(imported),"all profiles detach imported favorite");
   check(!other.snapshot().collections.get(otherCollection).scores.contains(imported),"all profiles detach imported collection membership");
   check(!prefs.contains("user_0_file")&&!prefs.contains("user_1_file"),"saved study references cleared across profiles");
   boolean invalidHidden=false;
   try{LibraryStore.decode("{\"version\":1,\"favorites\":[],\"collections\":[],\"hiddenScores\":[\"imported/meu.mxl\"]}");}
   catch(IllegalArgumentException expected){invalidHidden=true;}
   check(invalidHidden,"imported files cannot be hidden as built-in scores");
   return checks;
  }finally{prefs.edit().clear().commit();}
 }
}

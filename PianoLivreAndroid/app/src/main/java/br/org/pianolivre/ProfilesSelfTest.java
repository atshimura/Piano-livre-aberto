package br.org.pianolivre;

import android.content.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Isolated Android preferences + backup test. Never touches piano_data/imported. */
final class ProfilesSelfTest {
 private static int checks;
 private static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static int run(Context context)throws Exception{
  SharedPreferences prefs=context.getSharedPreferences("piano_profiles_selftest",Context.MODE_PRIVATE);
  File root=new File(context.getCacheDir(),"profiles_selftest");root.mkdirs();
  prefs.edit().clear().putString("profiles","[\"A\",\"B\"]").putInt("active_profile",1)
   .putLong("user_1_position",Double.doubleToLongBits(12.75)).putLong("user_1_chord_window_ms",91)
   .putString("user_0_file","old.xml").commit();
  try{
   LocalProfiles store=new LocalProfiles(prefs);List<LocalProfiles.Profile> old=store.list();
   check(old.get(0).id==0&&old.get(1).id==1,"legacy identities retained");
   check(!prefs.contains("profiles_v2"),"read does not rewrite legacy data");
   store.update(1,"Renomeado",2);check(store.list().get(1).id==1&&store.list().get(1).avatar==2,"rename/avatar preserves identity");
   check(Double.longBitsToDouble(prefs.getLong("user_1_position",0))==12.75,"position retained");
   Map<String,?> before=prefs.getAll();boolean rejected=false;
   try{store.update(1,"",0);}catch(IllegalArgumentException ex){rejected=true;}
   check(rejected&&before.equals(prefs.getAll()),"invalid mutation is atomic");
   check(store.delete(0)==1&&store.list().get(0).id==1,"delete does not renumber survivor");
   check(!prefs.contains("user_0_file")&&prefs.getLong("user_1_chord_window_ms",0)==91,"delete scoped to selected profile");
   int fresh=store.create("Novo");check(fresh==2,"deleted identity never reused");
   prefs.edit().putString("user_10_extra","preserved").commit();
   check(store.delete(1)==fresh&&!prefs.contains("user_1_position"),"active deletion selects survivor and removes own study data");
   check(prefs.getString("user_10_extra","").equals("preserved"),"prefix deletion does not match user_10");
   int reserved=store.create("Outro");check(reserved==11,"orphan namespaces are reserved, never adopted");
   JSONObject backup=BackupArchive.create(prefs,root);store.update(fresh,"Changed",0);
   BackupArchive.restore(prefs,root,backup);store=new LocalProfiles(prefs);
   check(store.list().get(0).id==2&&store.list().get(0).name.equals("Novo"),"v2 backup restores stable identities");
   before=prefs.getAll();JSONObject bad=new JSONObject(backup.toString());
   bad.getJSONObject("prefs").getJSONObject("profiles_v2").put("value","[{\"id\":2,\"name\":\"X\",\"avatar\":0},{\"id\":2,\"name\":\"Y\",\"avatar\":1}]");
   rejected=false;try{BackupArchive.restore(prefs,root,bad);}catch(Exception ex){rejected=true;}
   check(rejected&&before.equals(prefs.getAll()),"duplicate identity rejected before restoring data");
   store.delete(reserved);before=prefs.getAll();rejected=false;
   try{store.delete(fresh);}catch(IllegalArgumentException ex){rejected=true;}
   check(rejected&&before.equals(prefs.getAll()),"last profile cannot be deleted");
   return checks;
  }finally{prefs.edit().clear().commit();}
 }
}

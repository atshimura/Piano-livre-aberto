package br.org.pianolivre;

import android.content.*;
import android.util.Log;
import org.json.*;
import java.io.*;
import java.util.*;

/** Isolated persistence checks + real Activity lifecycle-boundary integration. */
final class HistorySelfTest {
 private static int checks;
 private static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static int run(Context context)throws Exception{
  SharedPreferences prefs=context.getSharedPreferences("piano_history_selftest",Context.MODE_PRIVATE);
  File root=new File(context.getCacheDir(),"history_selftest");root.mkdirs();
  prefs.edit().clear().putString("profiles","[\"A\",\"B\"]").putInt("active_profile",0).commit();
  try{
   StudyClock clock=new StudyClock();StudyHistory first=new StudyHistory(prefs,0),second=new StudyHistory(prefs,1);
   String score="scores/exercicio.mxl";
   clock.start(100,1000,0,score,"Exercício",0,0,0,1);first.record(clock.finish(600,2,1));
   JSONObject piece=first.snapshot().getJSONObject("pieces").getJSONObject(score);
   check(piece.getLong("timeMs")==500&&piece.getLong("periods")==1,"elapsed time and period count");
   check(second.snapshot().getJSONArray("records").length()==0,"profile isolation");
   first.goal(score,1);clock.start(700,1,0,score,"Exercício",1,1,2,.5);first.record(clock.finish(60700,4,1));
   piece=first.snapshot().getJSONObject("pieces").getJSONObject(score);
   check(piece.getLong("timeMs")==60500&&piece.getInt("goalMinutes")==1,"chosen time goal and accumulated progress");
   Map<String,?> before=prefs.getAll();clock.start(70000,10,1,score,"Exercício",0,0,0,1);
   boolean rejected=false;try{first.record(clock.finish(71000,1,1));}catch(IllegalArgumentException ex){rejected=true;}
   check(rejected&&before.equals(prefs.getAll()),"wrong owner cannot receive a period");
   JSONObject backup=BackupArchive.create(prefs,root);first.clear();BackupArchive.restore(prefs,root,backup);
   check(first.snapshot().getJSONArray("records").length()==2,"history restored without merging/duplicating");
   before=prefs.getAll();JSONObject bad=new JSONObject(backup.toString());
   JSONObject value=new JSONObject(bad.getJSONObject("prefs").getJSONObject("user_0_history").getString("value"));
   value.getJSONArray("records").getJSONObject(0).put("elapsedMs",-1);
   bad.getJSONObject("prefs").getJSONObject("user_0_history").put("value",value.toString());
   rejected=false;try{BackupArchive.restore(prefs,root,bad);}catch(Exception ex){rejected=true;}
   check(rejected&&before.equals(prefs.getAll()),"invalid duration rejected before restore");
   first.goal(score,0);check(first.snapshot().getJSONObject("pieces").getJSONObject(score).getInt("goalMinutes")==0,"goal removable without deleting progress");
   first.clear();check(first.snapshot().getJSONArray("records").length()==0&&prefs.getString("profiles","").contains("A"),"explicit clear scoped to history");
   return checks;
  }finally{prefs.edit().clear().commit();}
 }
 static void activity(MainActivity a){
  a.screens.showStudy();a.beginStudyWindow();
  a.handler.postDelayed(()->{
   a.screens.showFree();
   a.handler.postDelayed(()->{
    a.screens.showHome();
    try{
     JSONArray rows=new StudyHistory(a.prefs,a.user).snapshot().getJSONArray("records");boolean study=false,free=false;
     for(int i=0;i<rows.length();i++){int mode=rows.getJSONObject(i).getInt("mode");if(mode==3)free=true;else study=true;}
     check(study&&free&&!a.studyClock.active(),"Activity boundaries record study/free and close on library");
     Log.i("PianoLivreHistory","HISTORY_ACTIVITY_SELFTEST_PASS assertions="+checks);
     if(a.getIntent().getBooleanExtra("midi_import_ui_selftest",false)){
      MidiFileImport.Result midi=MidiFileImport.read(new ByteArrayInputStream(MidiFileSelfTest.sample()));
      a.chooseMidiHands(midi,"Synthetic.MID",midi.noteTracks(),0,new HashMap<>());
     }
    }catch(Exception ex){throw new IllegalStateException(ex);}
   },30);
  },30);
 }
}

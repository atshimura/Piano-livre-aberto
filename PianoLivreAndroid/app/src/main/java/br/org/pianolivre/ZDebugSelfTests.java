package br.org.pianolivre;

import android.app.Activity;
import android.content.Context;

/** Linha Z (Tarefa 2): concentra o disparo dos self-tests de depuracao que antes
 * viviam espalhados dentro de MainActivity.onCreate. Refatoracao pura, sem
 * mudanca de comportamento: mesmos gatilhos por intent extra, mesma ordem,
 * mesmas mensagens de log, mesmas excecoes. Nada daqui executa em builds de
 * producao (tudo limitado a FLAG_DEBUGGABLE). As strings de log sao contrato
 * dos emuladores (tests/emulator_*.py) e nao devem ser alteradas.
 * Registro: ZAI-CONTINUIDADE.md (zai/feat/refactor-selftest-launcher). */
final class ZDebugSelfTests {
 private ZDebugSelfTests(){}

 private static boolean debug(MainActivity a){
  return (a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0;
 }

 /** Self-tests sincronos executados antes da recuperacao de backup; podem lancar. */
 static void runPreRecovery(MainActivity a){
  if(!debug(a))return;
  if(a.getIntent().getBooleanExtra("backup_selftest",false)){
   try{
    int verified=BackupArchiveSelfTest.run(a);
    android.util.Log.i("PianoLivreBackup","BACKUP_SELFTEST_PASS assertions="+verified);
   }catch(Exception ex){
    android.util.Log.e("PianoLivreBackup","BACKUP_SELFTEST_FAILED",ex);
    throw new IllegalStateException("Backup self-test failed",ex);
   }
  }
  if(a.getIntent().getBooleanExtra("profiles_selftest",false)){
   try{android.util.Log.i("PianoLivreProfiles","PROFILES_SELFTEST_PASS assertions="+ProfilesSelfTest.run(a));}
   catch(Exception ex){throw new IllegalStateException("Profile self-test failed",ex);}
  }
  if(a.getIntent().getBooleanExtra("library_selftest",false)){
   try{android.util.Log.i("PianoLivreLibrary","LIBRARY_SELFTEST_PASS assertions="+LibrarySelfTest.run(a));}
   catch(Exception ex){throw new IllegalStateException("Library self-test failed",ex);}
  }
  if(a.getIntent().getBooleanExtra("history_selftest",false)){
   try{android.util.Log.i("PianoLivreHistory","HISTORY_SELFTEST_PASS assertions="+HistorySelfTest.run(a));}
   catch(Exception ex){throw new IllegalStateException("History self-test failed",ex);}
  }
  if(a.getIntent().getBooleanExtra("midifile_selftest",false)){
   try{android.util.Log.i("PianoLivreMidiFile","MIDIFILE_SELFTEST_PASS assertions="+MidiFileSelfTest.run(a));}
   catch(Exception ex){throw new IllegalStateException("MIDI file self-test failed",ex);}
  }
  if(a.getIntent().getBooleanExtra("xml_input_selftest",false)){
   try{android.util.Log.i("PianoLivreXmlInput","XML_INPUT_SELFTEST_PASS assertions="+XmlInputSelfTest.run());}
   catch(Exception ex){throw new IllegalStateException("XML input self-test failed",ex);}
  }
 }

 /** Upgrade self-test: roda apos a recuperacao de backup. */
 static void runUpgrade(MainActivity a){
  if(!debug(a)||!a.getIntent().getBooleanExtra("upgrade_selftest",false))return;
  try{android.util.Log.i("PianoLivreUpgrade","UPGRADE_SELFTEST_PASS assertions="+UpgradeSelfTest.run(a,a.prefs));}
  catch(Exception ex){throw new IllegalStateException("Upgrade self-test failed",ex);}
 }

 /** Device-side MIDI tests use separate preferences, never the user's session. */
 static void redirectMidiSelfTestPrefs(MainActivity a){
  if(!debug(a))return;
  if(a.getIntent().getBooleanExtra("midi_selftest",false)||a.getIntent().getBooleanExtra("midi_connection_selftest",false)){
   a.prefs=a.getSharedPreferences(a.getIntent().getBooleanExtra("midi_connection_selftest",false)?"piano_midi_connection_selftest":"piano_midi_selftest",Context.MODE_PRIVATE);
   a.prefs.edit().clear().commit();
  }
 }

 /** Fixtures de gravacao (engraving): abrem Study com partituras de teste. */
 static void launchEngravingFixtures(MainActivity a,String previous){
  if(!debug(a))return;
  boolean contextSelfTest=a.getIntent().getBooleanExtra("engraving_context_selftest",false);
  boolean complexEngravingSelfTest=a.getIntent().getBooleanExtra("engraving_complex_selftest",false);
  boolean polyphonySelfTest=a.getIntent().getBooleanExtra("engraving_polyphony_selftest",false);
  String notation06=a.getIntent().getStringExtra("engraving_notation06_selftest");
  boolean notation06SelfTest=notation06!=null&&java.util.Arrays.asList("accidentals","beams","tuplets","grace","adversarial").contains(notation06);
  if(contextSelfTest){
   android.util.Log.i("PianoLivreContext","CONTEXT_FIXTURE_INTENT accepted previousPath="+previous);
   a.handler.post(()->{
    String fixture="scores/engraving-context-change.musicxml";
    a.loadScore(fixture,true);
    if(a.score==null||!fixture.equals(a.currentFile))
     android.util.Log.e("PianoLivreContext","CONTEXT_FIXTURE_LOAD_FAILED currentFile="+a.currentFile+" score="+(a.score==null?"null":a.score.title));
    else android.util.Log.i("PianoLivreContext","CONTEXT_FIXTURE_LOAD_OK title="+a.score.title+" measures="+a.score.measures+" writtenHeads="+a.score.writtenEvents.size());
   });
  }
  if(complexEngravingSelfTest){
   a.handler.post(()->{
    String fixture="scores/engraving-complex-synthetic.musicxml";
    a.loadScore(fixture,true);
    if(a.score==null||!fixture.equals(a.currentFile))
     android.util.Log.e("PianoLivreContext","COMPLEX_FIXTURE_LOAD_FAILED currentFile="+a.currentFile+" score="+(a.score==null?"null":a.score.title));
    else android.util.Log.i("PianoLivreContext","COMPLEX_FIXTURE_LOAD_OK title="+a.score.title+" measures="+a.score.measures+" writtenHeads="+a.score.writtenEvents.size());
   });
  }
  if(notation06SelfTest){
   final String fixture="scores/engraving-"+notation06+"-06.musicxml";
   a.handler.post(()->{
    a.loadScore(fixture,true);
    if(a.score==null||!fixture.equals(a.currentFile))android.util.Log.e("PianoLivreNotation06","NOTATION06_LOAD_FAILED");
    else android.util.Log.i("PianoLivreNotation06","NOTATION06_LOAD_OK written="+a.score.writtenEvents.size()+" attacks="+a.score.events.size());
   });
  }
  if(polyphonySelfTest){
   a.handler.post(()->{
    String fixture="scores/engraving-polyphony-synthetic.musicxml";
    a.loadScore(fixture,true);
    if(a.score==null||!fixture.equals(a.currentFile))
     android.util.Log.e("PianoLivrePolyphony","POLYPHONY_FIXTURE_LOAD_FAILED currentFile="+a.currentFile);
    else android.util.Log.i("PianoLivrePolyphony","POLYPHONY_FIXTURE_LOAD_OK title="+a.score.title+" measures="+a.score.measures+" writtenHeads="+a.score.writtenEvents.size()+" attacks="+a.score.events.size());
   });
  }
 }

 /** Demais self-tests assincronos pos-startup. */
 static void postRuntimeSelfTests(MainActivity a){
  if(!debug(a))return;
  if(a.getIntent().getBooleanExtra("midi_selftest",false))a.handler.post(()->MidiSessionSelfTest.run(a));
  if(a.getIntent().getBooleanExtra("midi_connection_selftest",false))a.handler.post(()->MidiInterfaceSelfTest.run(a));
  if(a.getIntent().getBooleanExtra("discovery_layout_selftest",false))a.handler.post(()->DiscoveryLayoutSelfTest.run(a));
  if(a.getIntent().getBooleanExtra("creation_selftest",false)||a.getIntent().getBooleanExtra("creation_reopen_selftest",false))a.handler.post(()->CreationSelfTest.run(a,a.getIntent().getBooleanExtra("creation_reopen_selftest",false)));
  if(a.getIntent().getBooleanExtra("creation_export_selftest",false))a.handler.post(()->CreationSelfTest.export(a));
  if(a.getIntent().getBooleanExtra("discovery_selftest",false))a.handler.post(()->{android.util.Log.i("PianoLivreDiscovery","DISCOVERY_UI_SELFTEST_BEGIN");MusicLabDiscoverySelfTest.run(a);});
  if(a.getIntent().getBooleanExtra("creation_c2_d_smoke",false))a.handler.post(()->CreationC2DProbe.run(a));
 }
}

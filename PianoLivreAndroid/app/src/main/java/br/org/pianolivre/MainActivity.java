package br.org.pianolivre;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.midi.*;
import android.net.Uri;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Prototype native Android piano trainer. Needs real-device USB MIDI testing. */
public class MainActivity extends Activity {
 static final int IMPORT=100,EXPORT=101,RESTORE=102,CREATION_MIDI_EXPORT=103;
 private String pendingCreationExportId,pendingCreationExportFile,pendingCreationExportSha;private boolean creationExportPreparing;
 private final java.util.concurrent.ExecutorService creationExportDisk=java.util.concurrent.Executors.newSingleThreadExecutor();
 AlertDialog lastMessageDialog;
 PianoScreens screens;LinearLayout root,toolbar;PianoView scoreView;TextView status,info;Spinner hands,modes,profiles;SeekBar tempoBar;
 Score score;String currentFile="";String currentName="";int currentHand=0;int mode=0;boolean running=false,waitTraveling=false;double waitTravelTarget=0,position=0;double tempoFactor=1,loopStart=0,loopEnd=Double.MAX_VALUE;
 MidiManager midiManager;private MidiManager.DeviceCallback midiDeviceCallback;
 final MidiConnection midiConnection=new MidiConnection(new MidiConnection.Listener(){
  public void invalidated(long generation){
   midiGeneration=generation;midiInputEpoch++;stopPlayback();
   if(screens!=null)screens.stopLabDemonstration();
   if(screens!=null)screens.connectionLostCreation();
   clearMidiObservation();chordCalibration.stop();
   if(screens!=null)screens.onMidiConnectionChanged(generation);
  }
  public void changed(MidiConnection.Snapshot state){
   setMidiStatus(state.detail);
  }
 });
 private volatile long midiInputEpoch;private boolean midiDestroyed;
 long midiInputEpoch(){return midiInputEpoch;}
 private long outputTestSequence;
 Set<Integer> activeNotes=new HashSet<>(); Set<Integer> downKeys=new HashSet<>();Set<Integer> midiSources=new HashSet<>();
 final WaitRecognizer waitRecognizer=new WaitRecognizer();
 final WaitPreparation waitPreparation=new WaitPreparation();
 boolean waitStarted=false,waitPreparing=false;
 long waitPreparationSequence=0;
 final ChordCalibration chordCalibration=new ChordCalibration();
 TextView chordCalibrationStatus;
 final MidiPerformance performance=new MidiPerformance();
 MidiPlayback midiPlayback;
 final StudyClock studyClock=new StudyClock();String pendingHistoryError=null;
 final RepeatNavigator writtenRepeats=new RepeatNavigator();
 PerformanceRoute.Result route;
 PerformanceRoute.Cursor routeCursor;
 boolean routeHasMarkers=false;
 boolean writtenRouteActive(){
  return score!=null&&routeHasMarkers&&route!=null&&route.supported()
   &&routeCursor!=null&&loopEnd==Double.MAX_VALUE;
 }
 void resetWrittenRoute(){
  writtenRepeats.reset();
  if(routeCursor!=null){routeCursor.reset();routeCursor.align(position);}
 }
 volatile long midiGeneration=0;
 private final java.util.concurrent.atomic.AtomicLong midiPacketSequence=new java.util.concurrent.atomic.AtomicLong();
 private long currentMidiPacket=0,currentUiSession=-1,currentMidiEventTime=0;
 volatile boolean foreground=false;
 int receivedMessages=0, sentMessages=0, lastMidiChannel=0;
 int lastMidiNote=-1, lastMidiVelocity=0;
 boolean sustainDown=false;
 String connectionDescription="Nenhum dispositivo conectado",lastMidiMessage="Nenhuma mensagem recebida";
 TextView midiDiagnosticText;

 Handler handler=new Handler(Looper.getMainLooper());long previousTick;int user=0;JSONArray catalog;
 android.content.SharedPreferences prefs;
 MidiReceiver createReceiver(final long generation){
  final MidiProtocol decoder=new MidiProtocol();
  return new MidiReceiver(){
   @Override public synchronized void onSend(byte[] data,int offset,int count,long timestamp){
    if(generation!=midiGeneration)return;
    final long inputEpoch=midiInputEpoch;
    final boolean arrivedInForeground=foreground;
    final ArrayList<int[]> decoded=new ArrayList<>();
    // Drain background bytes to retain stream framing, but never deliver them.
    decoder.feed(data,offset,count,inputEpoch,(st,d1,d2,startedEpoch)->{
     if(startedEpoch==inputEpoch)decoded.add(new int[]{st,d1,d2});
    });
    if(!arrivedInForeground||!foreground||decoded.isEmpty())return;
    final long packet=midiPacketSequence.incrementAndGet();
    final long arrivalTime=SystemClock.uptimeMillis();
    final long arrivalTimeNs=SystemClock.elapsedRealtimeNanos();
    final long uiSession=screens==null?-1:screens.discoveryGeneration();
    handler.post(()->{
     if(generation!=midiGeneration||inputEpoch!=midiInputEpoch||!foreground)return;
     for(int[] event:decoded)onMidiMessage(event[0],event[1],event[2],packet,arrivalTime,arrivalTimeNs,timestamp,uiSession);
     if(screens!=null)screens.onLabMidiBatchEnd(packet,arrivalTime,uiSession);
    });
   }
  };
 }
 void rearmWaitIfStudying(){
  if(foreground&&waitStarted&&!waitPreparing&&score!=null&&mode==0&&screens!=null&&screens.isStudyOpen())prepareWaiting();
 }
 @Override public void onCreate(Bundle state){super.onCreate(state);if(state!=null){pendingCreationExportId=state.getString("creationExportId");pendingCreationExportFile=state.getString("creationExportFile");pendingCreationExportSha=state.getString("creationExportSha");creationExportPreparing=pendingCreationExportFile!=null;}getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
  // Keep screen awake only while the app is visible (no persistent wakelock).
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  prefs=getSharedPreferences("piano_data",MODE_PRIVATE);
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("backup_selftest",false)){
   try{
    int verified=BackupArchiveSelfTest.run(this);
    android.util.Log.i("PianoLivreBackup","BACKUP_SELFTEST_PASS assertions="+verified);
   }catch(Exception ex){
    android.util.Log.e("PianoLivreBackup","BACKUP_SELFTEST_FAILED",ex);
    throw new IllegalStateException("Backup self-test failed",ex);
   }
  }
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("profiles_selftest",false)){
   try{android.util.Log.i("PianoLivreProfiles","PROFILES_SELFTEST_PASS assertions="+ProfilesSelfTest.run(this));}
   catch(Exception ex){throw new IllegalStateException("Profile self-test failed",ex);}
  }
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("library_selftest",false)){
   try{android.util.Log.i("PianoLivreLibrary","LIBRARY_SELFTEST_PASS assertions="+LibrarySelfTest.run(this));}
   catch(Exception ex){throw new IllegalStateException("Library self-test failed",ex);}
  }
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("history_selftest",false)){
   try{android.util.Log.i("PianoLivreHistory","HISTORY_SELFTEST_PASS assertions="+HistorySelfTest.run(this));}
   catch(Exception ex){throw new IllegalStateException("History self-test failed",ex);}
  }
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("midifile_selftest",false)){
   try{android.util.Log.i("PianoLivreMidiFile","MIDIFILE_SELFTEST_PASS assertions="+MidiFileSelfTest.run(this));}
   catch(Exception ex){throw new IllegalStateException("MIDI file self-test failed",ex);}
  }
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("xml_input_selftest",false)){
   try{android.util.Log.i("PianoLivreXmlInput","XML_INPUT_SELFTEST_PASS assertions="+XmlInputSelfTest.run());}
   catch(Exception ex){throw new IllegalStateException("XML input self-test failed",ex);}
  }
  try{BackupArchive.recover(prefs,getFilesDir());}
  catch(Exception recovery){
   android.util.Log.e("PianoLivreBackup","Não foi possível recuperar restauração anterior",recovery);
   new AlertDialog.Builder(this).setTitle("Recuperação de dados")
    .setMessage("Não foi possível verificar os arquivos do último backup. Os dados não serão alterados. Detalhes: "+recovery.getMessage())
    .setPositiveButton("Fechar",(d,w)->finish()).setCancelable(false).show();
   return;
  }
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("upgrade_selftest",false)){
   try{android.util.Log.i("PianoLivreUpgrade","UPGRADE_SELFTEST_PASS assertions="+UpgradeSelfTest.run(this,prefs));}
   catch(Exception ex){throw new IllegalStateException("Upgrade self-test failed",ex);}
  }
  // Device-side MIDI tests use separate preferences, never the user's session.
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&(getIntent().getBooleanExtra("midi_selftest",false)||getIntent().getBooleanExtra("midi_connection_selftest",false))){
   prefs=getSharedPreferences(getIntent().getBooleanExtra("midi_connection_selftest",false)?"piano_midi_connection_selftest":"piano_midi_selftest",MODE_PRIVATE);prefs.edit().clear().commit();
  }
  midiManager=(MidiManager)getSystemService(MIDI_SERVICE);try{catalog=new JSONArray(readAsset("catalog.json"));}catch(Exception e){catalog=new JSONArray();}
  buildUi();loadProfiles();String previous=prefs.getString("user_"+user+"_file","");
  boolean contextSelfTest=(getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("engraving_context_selftest",false);
  boolean complexEngravingSelfTest=(getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("engraving_complex_selftest",false);
  boolean polyphonySelfTest=(getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("engraving_polyphony_selftest",false);
  String notation06=(getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     ?getIntent().getStringExtra("engraving_notation06_selftest"):null;
  boolean notation06SelfTest=notation06!=null&&java.util.Arrays.asList("accidentals","beams","tuplets","grace","adversarial").contains(notation06);
  if(!contextSelfTest&&!complexEngravingSelfTest&&!polyphonySelfTest&&!notation06SelfTest&&!previous.isEmpty())loadScore(previous,false);
  setupMidi();
  handler.post(ticker);
  if(contextSelfTest){
   android.util.Log.i("PianoLivreContext","CONTEXT_FIXTURE_INTENT accepted previousPath="+previous);
   handler.post(()->{
    String fixture="scores/engraving-context-change.musicxml";
    loadScore(fixture,true);
    if(score==null||!fixture.equals(currentFile))
     android.util.Log.e("PianoLivreContext","CONTEXT_FIXTURE_LOAD_FAILED currentFile="+currentFile+" score="+(score==null?"null":score.title));
    else android.util.Log.i("PianoLivreContext","CONTEXT_FIXTURE_LOAD_OK title="+score.title+" measures="+score.measures+" writtenHeads="+score.writtenEvents.size());
   });
  }
  if(complexEngravingSelfTest){
   handler.post(()->{
    String fixture="scores/engraving-complex-synthetic.musicxml";
    loadScore(fixture,true);
    if(score==null||!fixture.equals(currentFile))
     android.util.Log.e("PianoLivreContext","COMPLEX_FIXTURE_LOAD_FAILED currentFile="+currentFile+" score="+(score==null?"null":score.title));
    else android.util.Log.i("PianoLivreContext","COMPLEX_FIXTURE_LOAD_OK title="+score.title+" measures="+score.measures+" writtenHeads="+score.writtenEvents.size());
   });
  }
  if(notation06SelfTest){
   final String fixture="scores/engraving-"+notation06+"-06.musicxml";
   handler.post(()->{
    loadScore(fixture,true);
    if(score==null||!fixture.equals(currentFile))android.util.Log.e("PianoLivreNotation06","NOTATION06_LOAD_FAILED");
    else android.util.Log.i("PianoLivreNotation06","NOTATION06_LOAD_OK written="+score.writtenEvents.size()+" attacks="+score.events.size());
   });
  }
  if(polyphonySelfTest){
   handler.post(()->{
    String fixture="scores/engraving-polyphony-synthetic.musicxml";
    loadScore(fixture,true);
    if(score==null||!fixture.equals(currentFile))
     android.util.Log.e("PianoLivrePolyphony","POLYPHONY_FIXTURE_LOAD_FAILED currentFile="+currentFile);
    else android.util.Log.i("PianoLivrePolyphony","POLYPHONY_FIXTURE_LOAD_OK title="+score.title+" measures="+score.measures+" writtenHeads="+score.writtenEvents.size()+" attacks="+score.events.size());
   });
  }
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("midi_selftest",false))handler.post(()->MidiSessionSelfTest.run(this));
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("midi_connection_selftest",false))handler.post(()->MidiInterfaceSelfTest.run(this));
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("discovery_layout_selftest",false))handler.post(()->DiscoveryLayoutSelfTest.run(this));
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&(getIntent().getBooleanExtra("creation_selftest",false)||getIntent().getBooleanExtra("creation_reopen_selftest",false)))handler.post(()->CreationSelfTest.run(this,getIntent().getBooleanExtra("creation_reopen_selftest",false)));
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0&&getIntent().getBooleanExtra("creation_export_selftest",false))handler.post(()->CreationSelfTest.export(this));
  if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
     &&getIntent().getBooleanExtra("discovery_selftest",false))handler.post(()->{android.util.Log.i("PianoLivreDiscovery","DISCOVERY_UI_SELFTEST_BEGIN");MusicLabDiscoverySelfTest.run(this);});
 }
 private String readAsset(String asset)throws IOException{try(InputStream in=getAssets().open(asset)){return new String(all(in),StandardCharsets.UTF_8);}}
 private byte[] all(InputStream in)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)o.write(b,0,n);return o.toByteArray();}
 void buildUi(){screens=new PianoScreens(this);screens.build();}
 interface OnSelected{void accept(int index);}static class Listener implements android.widget.AdapterView.OnItemSelectedListener{OnSelected cb;Listener(OnSelected c){cb=c;}public void onItemSelected(android.widget.AdapterView<?> a,View v,int i,long l){cb.accept(i);}public void onNothingSelected(android.widget.AdapterView<?> a){}}
 Spinner spin(String[] items,LinearLayout parent,int width){Spinner sp=new Spinner(this);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items);sp.setAdapter(adapter);parent.addView(sp,new LinearLayout.LayoutParams(width,48));return sp;}
 void button(String text,Runnable r,LinearLayout parent){Button b=new Button(this);b.setText(text);b.setTextSize(11);b.setAllCaps(false);b.setPadding(4,0,4,0);b.setOnClickListener(v->r.run());parent.addView(b,new LinearLayout.LayoutParams(-2,44));}
 void chooseScore(){screens.showHome();}
 void loadScore(String path,boolean reset){loadScore(path,reset,false);}
 void openFreeScore(String path){loadScore(path,true,true);}
 void loadScore(String path,boolean reset,boolean free){stopPlayback();finishStudyWindow();writtenRepeats.reset();try{path=SessionFiles.resolve(getFilesDir(),path);}catch(IllegalArgumentException ex){message(ex.getMessage());return;}try(InputStream input=path.startsWith("scores/")?getAssets().open(path):new FileInputStream(path)){score=Score.read(input,path);currentFile=path;currentName=score.title;
  route=PerformanceRoute.build(score);
  routeCursor=route.supported()?new PerformanceRoute.Cursor(route):null;
  routeHasMarkers=false;
  for(Score.MeasureInfo m:score.measureInfo){
   if(m.forwardRepeat||m.backwardRepeat||!m.endings.isEmpty()||
     m.daCapo||m.dalSegno||m.segno||m.toCoda||m.coda||m.fine){
    routeHasMarkers=true;break;
   }
  }
  for(int i=0;i<catalog.length();i++){JSONObject entry=catalog.optJSONObject(i);if(entry!=null&&path.equals("scores/"+entry.optString("file"))){currentName=entry.optString("name",score.title);break;}}
  String prefix="user_"+user+"_";
  if(reset){
   position=score.firstOnset(currentHand);loopStart=0;loopEnd=Double.MAX_VALUE;
  }else{
   currentHand=Math.max(0,Math.min(2,prefs.getInt(prefix+"hand",0)));
   mode=Math.max(0,Math.min(2,prefs.getInt(prefix+"mode",0)));
   tempoFactor=Double.longBitsToDouble(prefs.getLong(prefix+"tempo",
    Double.doubleToLongBits(1.0)));
   if(!Double.isFinite(tempoFactor))tempoFactor=1.0;
   tempoFactor=Math.max(.25,Math.min(2,tempoFactor));
   loopStart=Double.longBitsToDouble(prefs.getLong(prefix+"loop_start",Double.doubleToLongBits(0)));
   loopEnd=Double.longBitsToDouble(prefs.getLong(prefix+"loop_end",Double.doubleToLongBits(Double.MAX_VALUE)));
   position=Double.longBitsToDouble(prefs.getLong(prefix+"position",
    Double.doubleToLongBits(score.firstOnset(currentHand))));
   if(!Double.isFinite(position))position=score.firstOnset(currentHand);
   if(!Double.isFinite(loopStart)||loopStart<0||!Double.isFinite(loopEnd)
      ||loopEnd<=loopStart){loopStart=0;loopEnd=Double.MAX_VALUE;}
   scoreView.showKeyboard=prefs.getBoolean(prefix+"keyboard",false);
  }
  position=Math.max(0,Math.min(position,score.end()));
  if(routeCursor!=null){
   if(reset||!routeCursor.restore(prefs.getInt(prefix+"route_visit",-1),position))
    routeCursor.align(position);
  }
  scoreView.score=score;scoreView.hand=currentHand;scoreView.playhead=position;scoreView.invalidate();
  if(tempoBar!=null)tempoBar.setProgress((int)Math.round(tempoFactor*100)-25);
  if(free){screens.showFree();}
  else{screens.showStudy();screens.displayScore(score,position);if(mode==0)prepareWaiting();}
  updateInfo();saveSession();
  if(!score.importWarnings.isEmpty())message("Aviso de interpretação: "+String.join("; ",score.importWarnings));
  if(routeHasMarkers&&!route.supported())
   message("Atenção: não é possível determinar com segurança todos os retornos: "+
     String.join(", ",route.warnings)+". A reprodução desta partitura será linear.");
 }catch(Exception e){message("Não foi possível importar: "+e.getMessage());}}
 void chooseMidiHands(MidiFileImport.Result midi,String name,List<Integer> tracks,int index,Map<Integer,Integer> hands){
  if(index<tracks.size()){
   int track=tracks.get(index);long count=midi.notes.stream().filter(n->n.track==track).count();
   new AlertDialog.Builder(this).setTitle("Trilha "+(track+1)+" · "+count+" notas")
    .setItems(new String[]{"Mão direita","Mão esquerda","Ignorar esta trilha"},(d,choice)->{
     hands.put(track,choice==0?1:choice==1?2:0);chooseMidiHands(midi,name,tracks,index+1,hands);
    }).setNegativeButton("Cancelar",null).show();return;
  }
  try{
   File dest=MidiImportStorage.save(new File(getFilesDir(),"imported"),name,midi,hands);
   loadScore(dest.getAbsolutePath(),true);
   message("MIDI importado como notação aproximada. Fonte original preservada; pedal e controladores não fazem parte da conversão.");
  }catch(Exception ex){message("Não foi possível importar o MIDI: "+ex.getMessage());}
 }
 void importScore(){Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("*/*");in.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(in,IMPORT);}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(request==CREATION_MIDI_EXPORT){
   if(result!=RESULT_OK||data==null||data.getData()==null){clearCreationExport();return;}
   final Uri destination=data.getData();final String file=pendingCreationExportFile,id=pendingCreationExportId,expected=pendingCreationExportSha;
   creationExportDisk.execute(()->{try{
    byte[] bytes=null;
    if(file!=null&&file.matches("creation-export-[a-f0-9-]{36}\\.mid")){File cached=new File(getCacheDir(),file);if(cached.isFile())try(InputStream in=new FileInputStream(cached)){byte[] candidate=BackupArchive.readLimited(in,MidiStandardFileWriter.MAX_BYTES);if(sha256Creation(candidate).equals(expected))bytes=candidate;}}
    if(bytes==null)bytes=MidiStandardFileWriter.write(new CreationProjectStore(getFilesDir()).get(id));
    try(OutputStream out=getContentResolver().openOutputStream(destination,"w")){if(out==null)throw new IOException("Destino indisponível");out.write(bytes);out.flush();}
    if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0&&getIntent().getBooleanExtra("creation_export_selftest",false))CreationSelfTest.verifyExport(this,destination,bytes);
    handler.post(()->{clearCreationExport();message("Arquivo MIDI exportado com sucesso.");});
   }catch(Exception error){handler.post(()->{clearCreationExport();message("Não foi possível exportar a composição: "+error.getMessage());});}});
   return;
  }
  if(result!=RESULT_OK||data==null)return;
  Uri uri=data.getData();if(uri==null)return;
  try {
   if(request==IMPORT){
    String display="Importada.musicxml";
    try(android.database.Cursor cursor=getContentResolver().query(uri,null,null,null,null)){
     if(cursor!=null&&cursor.moveToFirst()){
      int idx=cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
      if(idx>=0&&cursor.getString(idx)!=null)display=cursor.getString(idx);
     }
    }
    if(display.toLowerCase(Locale.ROOT).endsWith(".mid")||display.toLowerCase(Locale.ROOT).endsWith(".midi")){
     MidiFileImport.Result midi;try(InputStream in=getContentResolver().openInputStream(uri)){midi=MidiFileImport.read(in);}
     final String midiName=display;
     new AlertDialog.Builder(this).setTitle("Importar MIDI como partitura aproximada")
      .setMessage("MIDI não contém a escrita original. A conversão usa compassos 4/4 e grafia aproximada; preserva tempos, ataques, durações e velocidades. Pedal, instrumentos, controladores, tonalidade e compassos originais não serão reproduzidos. O arquivo .mid original será conservado no backup. Escolha a mão de cada trilha; nenhuma mão será inferida pela altura.")
      .setPositiveButton("Escolher trilhas",(dialog,which)->chooseMidiHands(midi,midiName,midi.noteTracks(),0,new HashMap<>()))
      .setNegativeButton("Cancelar",null).show();
    }else{
     File dest;try(InputStream in=getContentResolver().openInputStream(uri)){dest=ScoreImport.save(new File(getFilesDir(),"imported"),display,in);}
     loadScore(dest.getAbsolutePath(),true);
    }
   }else if(request==EXPORT){
    if(screens!=null)screens.prepareCreationBackup();
    BackupArchive.export(prefs,getFilesDir(),()->getContentResolver().openOutputStream(uri));
    message("Backup exportado com verificação de integridade");
   }else if(request==RESTORE){
    JSONObject backup;
    try(InputStream in=getContentResolver().openInputStream(uri)){
     String text=new String(BackupArchive.readLimited(in,95*1024*1024),StandardCharsets.UTF_8);JsonLimits.check(text,95*1024*1024,64);backup=new JSONObject(text);
    }
    String summary=BackupArchive.summary(backup);
    new AlertDialog.Builder(this).setTitle("Substituir dados locais?")
     .setMessage(summary+"\n\nPreferências e partituras serão substituídas; composições serão mescladas após validação. Preserve um backup do estado atual.")
     .setPositiveButton("Substituir",(dialog,which)->{
      stopPlayback();
      try{
       if(screens!=null)screens.prepareCreationBackup();
       BackupArchive.restore(prefs,getFilesDir(),backup);
       score=null;scoreView.score=null;scoreView.pressed.clear();
       position=0;currentFile="";currentName="";user=0;
       scoreView.invalidate();
       screens.showHome();loadProfiles();
       message("Backup restaurado e verificado. Os dados anteriores foram substituídos.");
      }catch(Exception restoreError){
       android.util.Log.e("PianoLivreBackup","Restauração não concluída",restoreError);
       message("Backup não restaurado: "+restoreError.getMessage()+". Seus dados anteriores foram preservados ou recuperados.");
      }
     }).setNegativeButton("Cancelar",null).show();
   }
  }catch(Exception e){message("Erro: "+e.getMessage());}
 }
 void setupMidi(){
  if(midiManager==null){setMidiStatus("MIDI indisponível neste Android");return;}
  midiDeviceCallback=new MidiManager.DeviceCallback(){
   @Override public void onDeviceAdded(MidiDeviceInfo info){reconcileMidiDevices();}
   @Override public void onDeviceRemoved(MidiDeviceInfo info){reconcileMidiDevices();}
   @Override public void onDeviceStatusChanged(MidiDeviceStatus status){
    reconcileMidiDevices();
   }
  };
  midiManager.registerDeviceCallback(midiDeviceCallback,handler);
  reconcileMidiDevices();
 }
 MidiConnection.Target midiTarget(MidiDeviceInfo info){return new MidiConnection.Target(info.getId(),midiInstrumentName(info),info.getType()==MidiDeviceInfo.TYPE_USB);}
 void reconcileMidiDevices(){
  if(midiDestroyed||midiManager==null)return;
  ArrayList<MidiConnection.Target> available=new ArrayList<>();
  for(MidiDeviceInfo info:midiManager.getDevices())available.add(midiTarget(info));
  midiConnection.reconcile(available,(target,generation,completion)->{
   MidiDeviceInfo selected=null;
   for(MidiDeviceInfo info:midiManager.getDevices())if(info.getId()==target.id){selected=info;break;}
   if(selected==null){completion.complete(null);return;}
   midiManager.openDevice(selected,opened->{
    // Do not open ports or attach a receiver to an obsolete async completion.
    if(opened==null){completion.complete(null);return;}
    if(midiDestroyed||generation!=midiGeneration){try{opened.close();}catch(Exception ignored){}return;}
    completion.complete(new AndroidMidiPorts(opened,createReceiver(generation)));
   },handler);
  });
 }
 String midiName(MidiDeviceInfo info){
  if(info==null)return "Nenhum";
  String name=midiInstrumentName(info);
  return name.isEmpty()?"Dispositivo MIDI "+info.getId():name;
 }
 String midiInstrumentName(MidiDeviceInfo info){
  if(info==null)return "";
  String name=info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
  return name==null?"":name.trim();
 }
 void setMidiStatus(String description){
  connectionDescription=description;
  if(status!=null)status.setText(description);
  if(screens!=null)screens.updateMidiIndicator();
  refreshMidiDiagnostics();
 }
 void selectMidi(){
  if(midiManager==null){message("O Android não disponibiliza MIDI");return;}
  MidiDeviceInfo[] infos=midiManager.getDevices();
  if(infos.length==0){showMidiDiagnostics();return;}
  String[] choices=new String[infos.length+1];
  for(int i=0;i<infos.length;i++){
   MidiDeviceInfo info=infos[i];
   choices[i]=midiName(info)+" · entrada "+info.getOutputPortCount()+" / saída "+info.getInputPortCount();
  }
  choices[infos.length]="Mostrar diagnóstico e teste de áudio";
  new AlertDialog.Builder(this).setTitle("Conexão USB-MIDI")
   .setItems(choices,(dialog,which)->{
    if(which==infos.length)showMidiDiagnostics();
    else connectMidi(infos[which]);
   }).setNegativeButton("Cancelar",null).show();
 }
 void connectMidi(MidiDeviceInfo info){
  if(info==null||midiManager==null||midiDestroyed)return;
  midiConnection.connect(midiTarget(info),(target,generation,completion)->
   midiManager.openDevice(info,opened->{
    if(opened==null){completion.complete(null);return;}
    if(midiDestroyed||generation!=midiGeneration){try{opened.close();}catch(Exception ignored){}return;}
    completion.complete(new AndroidMidiPorts(opened,createReceiver(generation)));
   },handler));
 }
 void disconnectMidi(){midiConnection.disconnect("Nenhum dispositivo conectado");}
 void clearMidiObservation(){
  downKeys.clear();midiSources.clear();waitRecognizer.clear();performance.disconnect();sustainDown=false;
  if(scoreView!=null){scoreView.pressed.clear();scoreView.invalidate();}
  if(screens!=null)screens.updatePressed(downKeys);
 }
 void send(int pitch,boolean on){send(pitch,on,74);}
 void send(int pitch,boolean on,int velocity){
  byte[] bytes={(byte)(on?0x90:0x80),(byte)pitch,(byte)(on?velocity:0)};
  if(midiConnection.send(bytes)){
   sentMessages++;refreshMidiDiagnostics();
  }
 }
 void allOff(){
  outputTestSequence++;
  if(midiPlayback!=null){MidiPlayback playing=midiPlayback;midiPlayback=null;playing.stop();}
  for(int note:new HashSet<>(activeNotes))send(note,false);
  activeNotes.clear();midiConnection.silence();
 }
 void updateSustainState(){
  sustainDown=false;for(int ch=0;ch<16;ch++)if(performance.pedalDown(ch)){sustainDown=true;break;}
 }
 void onMidiMessage(int statusCode,int data1,int data2){
  onMidiMessage(statusCode,data1,data2,midiPacketSequence.incrementAndGet(),SystemClock.uptimeMillis(),SystemClock.elapsedRealtimeNanos(),-1,screens==null?-1:screens.discoveryGeneration());
 }
 private void onMidiMessage(int statusCode,int data1,int data2,long packet,long eventTime,long eventTimeNs,long androidTime,long uiSession){
  currentMidiPacket=packet;currentMidiEventTime=eventTime;currentUiSession=uiSession;
  if(screens!=null)screens.onCreationMidi(statusCode,data1,data2,eventTimeNs,midiInputEpoch,androidTime,midiConnection.snapshot().target==null?-1:midiConnection.snapshot().target.id);
  int command=statusCode&0xF0;
  receivedMessages++;lastMidiChannel=(statusCode&0x0f)+1;
  if(command==0x90&&data2>0){
   lastMidiNote=data1;lastMidiVelocity=data2;
   lastMidiMessage="Nota "+midiNoteName(data1)+" ("+data1+") · canal "+lastMidiChannel+" · velocidade "+data2;
   if(status!=null)status.setText("♪ MIDI RX: "+midiNoteName(data1));
   performance.noteOn(statusCode&15,data1,data2,SystemClock.uptimeMillis(),position);
   onKey(data1,true,statusCode&15);
  }else if(command==0x80||(command==0x90&&data2==0)){
   lastMidiNote=data1;lastMidiVelocity=0;
   lastMidiMessage="Soltou "+midiNoteName(data1)+" ("+data1+") · canal "+lastMidiChannel;
   performance.noteOff(statusCode&15,data1,SystemClock.uptimeMillis());
   onKey(data1,false,statusCode&15);
  }else if(command==0xB0&&(data1==120||data1==121||data1==123)){
   int channel=statusCode&15;
   performance.channelMode(channel,data1,SystemClock.uptimeMillis());
   if(data1!=121)for(int source:new HashSet<>(midiSources))
    if((source>>7)==channel)onKey(source&127,false,channel);
   updateSustainState();
   lastMidiMessage=(data1==120?"Som interrompido":data1==123?"Notas liberadas":"Controladores redefinidos")+" · canal "+(channel+1);
  }else if(command==0xB0&&data1==64){
   performance.control64(statusCode&15,data2,SystemClock.uptimeMillis());
   // CC64 belongs to a MIDI channel. Releasing one channel must not
   // report the pedal as up while another channel still holds sustain.
   updateSustainState();
   lastMidiMessage="Pedal sustain "+(sustainDown?"pressionado":"solto");
   if(mode==0&&screens!=null&&screens.isStudyOpen()&&!chordCalibration.recording())
    screens.showWaitFeedback("Pedal "+(sustainDown?"acionado":"liberado")+" · orientação, não bloqueia o Espera");
  }else{
   lastMidiMessage="MIDI 0x"+Integer.toHexString(statusCode)+" · "+data1+" · "+data2;
  }
  android.util.Log.i("PianoLivreMIDI","RX "+lastMidiMessage);
  refreshMidiDiagnostics();
 }
 String midiNoteName(int n){
  String[] notes={"Dó","Dó#","Ré","Ré#","Mi","Fá","Fá#","Sol","Sol#","Lá","Lá#","Si"};
  return notes[n%12]+(n/12-1);
 }
 String midiDiagnosticReport(){
  MidiConnection.Snapshot connection=midiConnection.snapshot();
  return "Dispositivo: "+(connection.target==null?"Nenhum":connection.target.name.isEmpty()?"Dispositivo sem nome (ID "+connection.target.id+")":connection.target.name)
   +"\nEstado MIDI: "+connection.phase
   +"\nSituação: "+connectionDescription
   +"\nPorta de entrada (piano → app): "+(!connection.canReceive?"não conectada":"aberta")
   +"\nPorta de saída (app → piano): "+(!connection.canSend?"não conectada":"aberta")
   +"\nMensagens recebidas: "+receivedMessages
   +"\nMensagens enviadas: "+sentMessages
   +"\nÚltima mensagem: "+lastMidiMessage
   +"\nNotas pressionadas: "+downKeys
   +"\nPedal: "+(sustainDown?"pressionado":"solto")
   +"\nNotas fisicamente pressionadas: "+performance.physicalKeysDown()
   +"\nVozes retidas por tecla/pedal: "+performance.soundingVoices()
   +"\nNotas concluídas registradas: "+performance.completed().size()
   +"\nJanela de acorde: "+(waitRecognizer.chordWindowMs()==null?"aguardando calibração":waitRecognizer.chordWindowMs()+" ms (ajustável)")
   +"\n\nToque algumas teclas do instrumento e verifique se o contador aumenta."
   +"\nO teste de saída envia Dó4 ao instrumento por meio segundo."
   +"\nO volume e a configuração de recepção MIDI do piano precisam permitir som.";
 }
 void refreshMidiDiagnostics(){
  if(midiDiagnosticText!=null)midiDiagnosticText.setText(midiDiagnosticReport());
 }
 void showMidiDiagnostics(){
  TextView body=new TextView(this);body.setTextSize(14);
  body.setPadding(24,20,24,20);midiDiagnosticText=body;refreshMidiDiagnostics();
  ScrollView scroll=new ScrollView(this);scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Diagnóstico USB-MIDI")
   .setView(scroll).setPositiveButton("Testar saída: Dó4",(d,which)->testMidiOutput())
   .setNeutralButton("Escolher dispositivo",(d,which)->selectMidi())
   .setNegativeButton("Fechar",null).create();
  dialog.setOnDismissListener(d->midiDiagnosticText=null);
  dialog.show();
 }
 void testMidiOutput(){
  if(!midiConnection.snapshot().canSend){message("Sem porta de saída MIDI. Selecione um instrumento em USB-MIDI.");return;}
  stopPlayback();final long generation=midiGeneration,sequence=++outputTestSequence;
  send(60,true,88);
  handler.postDelayed(()->{if(generation==midiGeneration&&sequence==outputTestSequence&&foreground)send(60,false,0);},550);
  if(status!=null)status.setText("Teste MIDI enviado: Dó4 por 0,55 s");
 }
 /** MIDI sources are tracked by channel+pitch. New attacks only. */
 void onKey(int pitch,boolean pressed,int channel){
  if(pitch<0||pitch>127||channel<0||channel>15)return;
  int source=(channel<<7)|pitch;
  boolean newAttack=false;
  boolean releasedLast=false;
  long now=currentMidiEventTime>0?currentMidiEventTime:SystemClock.uptimeMillis();
  if(pressed){
   if(midiSources.add(source))newAttack=downKeys.add(pitch);
   if(newAttack&&foreground&&chordCalibration.recording())chordCalibration.noteOn(pitch,now);
  }else{
   midiSources.remove(source);
   boolean stillPressed=false;
   for(int ch=0;ch<16;ch++)if(midiSources.contains((ch<<7)|pitch)){stillPressed=true;break;}
   if(!stillPressed){
    releasedLast=true;
    downKeys.remove(pitch);waitRecognizer.noteOff(pitch);
    if(chordCalibration.recording()&&chordCalibration.noteOff(pitch,now))updateChordCalibrationStatus();
   }
  }
  scoreView.pressed.clear();scoreView.pressed.addAll(downKeys);scoreView.invalidate();
  if(screens!=null){
   screens.updatePressed(downKeys);
   if(foreground){
    if(newAttack)screens.onLabMidiAttack(pitch,now,currentMidiPacket,midiGeneration,currentUiSession);
    else if(releasedLast)screens.onLabMidiRelease(pitch,midiGeneration,currentUiSession);
   }
  }
  if(!foreground||chordCalibration.recording()||score==null||mode!=0||!waitStarted||waitPreparing||!pressed||waitTraveling||
      (screens!=null&&!screens.isStudyOpen()))return;
  WaitRecognizer.Result result=waitRecognizer.noteOnTimed(pitch,newAttack,downKeys,now);
  if(screens!=null&&screens.isStudyOpen()){
   if(result.extra){
    screens.showWaitFeedback("Nota extra: "+midiNoteName(pitch)+" · continue com as notas da partitura");
   }else if(result.windowExceeded){
    screens.showWaitFeedback("Acorde fora da janela calibrada · solte e repita as notas");
   }else if(result.advance){
    String suffix=result.extraPitches.isEmpty()?"":" · extras indicadas (não bloqueiam)";
    if(waitRecognizer.expected().size()>1)
     screens.showWaitFeedback("Acorde reconhecido ✓ · observe a duração escrita"+suffix);
    else if(!result.extraPitches.isEmpty())
     screens.showWaitFeedback("Nota reconhecida ✓"+suffix);
   }
  }
  if(result.advance)nextWait(); // Duration/pedal are guidance, NOT a gate.
 }
 /** Keep the current passage when changing the active musical hand. */
 void refreshChordWindow(){
  long ms=prefs.getLong("user_"+user+"_chord_window_ms",-1);
  waitRecognizer.setChordWindowMs(ms>0?ms:null);
 }
 void updateChordCalibrationStatus(){
  if(chordCalibrationStatus!=null)
   chordCalibrationStatus.setText("Amostras: "+chordCalibration.sampleCount()+
    "\nJanela observada: "+(chordCalibration.observedWindowMs()==null?"aguardando acordes válidos":
    chordCalibration.observedWindowMs()+" ms")+
    "\nToque um acorde com pelo menos duas notas e solte todas as teclas. Repita em ritmos naturais.");
 }
 /** User-confirmed calibration; no preconfigured millisecond values. */
 void showChordSettings(){
  String current=waitRecognizer.chordWindowMs()==null?"Não calibrada":
   waitRecognizer.chordWindowMs()+" ms";
  String[] choices={"Calibrar tocando acordes","Ajustar janela calibrada","Remover calibração"};
  new AlertDialog.Builder(this).setTitle("Janela de acordes · "+current)
   .setItems(choices,(dlg,index)->{
    if(index==0)beginChordCalibration();
    else if(index==1){
     Long observed=waitRecognizer.chordWindowMs();
     if(observed==null)message("Toque primeiro os acordes de calibração. Nenhum número será presumido.");
     else editChordWindow(observed);
    }else{
     prefs.edit().remove("user_"+user+"_chord_window_ms").apply();
     waitRecognizer.setChordWindowMs(null);
     message("Calibração removida. Os acordes não terão limite temporal até uma nova medição.");
    }
   }).setNegativeButton("Fechar",null).show();
 }
 void beginChordCalibration(){
  if(!midiConnection.snapshot().canReceive){message("Conecte primeiro a entrada MIDI do instrumento.");return;}
  stopPlayback();
  chordCalibration.start();
  TextView body=new TextView(this);chordCalibrationStatus=body;
  body.setPadding(28,22,28,22);body.setTextSize(15);
  updateChordCalibrationStatus();
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Calibrar simultaneidade")
   .setView(body).setPositiveButton("Concluir",null)
   .setNegativeButton("Cancelar",(d,w)->chordCalibration.stop()).create();
  dialog.setOnDismissListener(d->{chordCalibration.stop();chordCalibrationStatus=null;});
  dialog.show();
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   if(chordCalibration.sampleCount()<3){
    body.setText("Faça pelo menos três acordes completos, soltando todas as teclas entre eles.\n\n"+
     "Amostras válidas: "+chordCalibration.sampleCount());
    return;
   }
   Long measured=chordCalibration.observedWindowMs();
   if(measured==null){body.setText("Ainda não foi possível medir os intervalos. Experimente acordes distintos.");return;}
   dialog.dismiss();
   editChordWindow(measured);
  });
 }
 void editChordWindow(long measuredMs){
  EditText ms=new EditText(this);
  ms.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
  ms.setSingleLine(true);ms.setText(String.valueOf(measuredMs));
  ms.setSelectAllOnFocus(true);
  ms.setPadding(28,10,28,10);
  AlertDialog dlg=new AlertDialog.Builder(this).setTitle("Janela de simultaneidade")
   .setMessage("O valor inicial é o maior intervalo observado nos seus acordes, não uma tolerância presumida. Pode ajustá-lo em milissegundos.")
   .setView(ms).setPositiveButton("Salvar",null).setNegativeButton("Cancelar",null).create();
  dlg.show();
  dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   try{
    long value=Long.parseLong(ms.getText().toString().trim());
    if(value<=0||value>60000){ms.setError("Informe um valor positivo de até 60.000 ms.");return;}
    prefs.edit().putLong("user_"+user+"_chord_window_ms",value).apply();
    waitRecognizer.setChordWindowMs(value);
    if(mode==0&&score!=null)prepareWaiting();
    dlg.dismiss();message("Janela configurada: "+value+" ms. Notas extras continuam permitidas; duração e pedal são orientativos.");
   }catch(NumberFormatException invalid){ms.setError("Informe milissegundos em número inteiro.");}
  });
 }
 void selectHand(int selected){
  if(selected<0||selected>2)return;
  stopPlayback();finishStudyWindow();
  currentHand=selected;scoreView.hand=selected;
  if(screens!=null&&screens.notation!=null)screens.notation.setStudyHand(selected);
  if(score!=null)position=HandNavigation.nextPosition(score,position,selected,loopStart,loopEnd);
  if(routeCursor!=null)routeCursor.align(position);
  scoreView.playhead=position;scoreView.invalidate();
  if(mode==0)prepareWaiting();else if(screens!=null)screens.followScore(position,true);
  saveSession();updateInfo();beginStudyWindow();
 }
 void nextWait(){
  if(score==null||waitTraveling)return;
  double next=writtenRouteActive()?
    routeCursor.nextWait(score,position,currentHand):
    writtenRepeats.nextWaitTarget(score,position,currentHand,loopStart,loopEnd);
  // Returning to an earlier bar is intentional; only an unchanged position
  // means that no further written onset/repeat is available.
  if(Math.abs(next-position)<.0001)return;
  waitTravelTarget=next;
  if(waitTravelTarget<=position){
   position=waitTravelTarget;scoreView.playhead=position;
   prepareWaiting();
   if(screens!=null)screens.followScore(position,true);
   return;
  }
  waitTraveling=true;previousTick=SystemClock.uptimeMillis();
  if(screens!=null)screens.followScore(position,true);
  android.util.Log.i("PianoLivreTimeline","WAIT_TRAVEL_BEGIN from="+position+" to="+waitTravelTarget);
 }
 void prepareWaiting(){
  if(score==null||mode!=0)return;
  waitRecognizer.clear();
  if(!waitStarted||waitPreparing){
   waitTraveling=false;
   if(screens!=null)screens.followScore(position,true);
   return;
  }
  double next=score.at(position,currentHand).isEmpty()?score.nextForHand(position,currentHand):position;
  if(next>position+.0001){waitTravelTarget=next;waitTraveling=true;previousTick=SystemClock.uptimeMillis();}
  else{waitTraveling=false;waitRecognizer.arm(score.at(position,currentHand));}
  if(screens!=null)screens.followScore(position,true);
 }
 /** Retorna ao início da música e suspende qualquer repetição anterior. */
 void seekPrevious(){
  if(score==null)return;
  stopPlayback();
  loopStart=0;loopEnd=Double.MAX_VALUE;writtenRepeats.reset();
  position=0;
  if(routeCursor!=null){routeCursor.reset();routeCursor.align(position);}
  scoreView.playhead=position;scoreView.invalidate();
  if(mode==0)prepareWaiting();
  else if(screens!=null)screens.followScore(position,true);
  if(screens!=null)screens.clearLoopSelection();
  saveSession();updateInfo();
 }
 /** Intervalo escolhido por toque diretamente sobre a partitura temporal. */
 void applyLoopSelection(double start,double end){
  if(score==null||!Double.isFinite(start)||!Double.isFinite(end))return;
  start=Math.max(0,Math.min(score.end(),start));
  end=Math.max(0,Math.min(score.end(),end));
  if(end-start<0.01){message("Escolha um trecho com início e fim diferentes.");return;}
  stopPlayback();
  loopStart=start;loopEnd=end;resetWrittenRoute();
  position=start;
  scoreView.playhead=position;scoreView.invalidate();
  if(mode==0)prepareWaiting();
  else if(screens!=null)screens.followScore(position,true);
  saveSession();updateInfo();
 }
 /** Menu start is a pending A marker; it must not mutate a running MIDI route. */
 void markLoopStart(){
  if(score==null)return;
  stopPlayback();
  loopStart=StudyLoopPosition.start(score,position);
  loopEnd=Double.MAX_VALUE;
  resetWrittenRoute();
  saveSession();updateInfo();
  android.util.Log.i("PianoLivreUI","STUDY_LOOP_MENU start from="+loopStart);
 }
 /** Commit B at a complete musical release using the validated loop path. */
 void markLoopEnd(){
  if(score==null)return;
  double release=StudyLoopPosition.end(score,position);
  applyLoopSelection(Math.min(loopStart,release),Math.max(loopStart,release));
  android.util.Log.i("PianoLivreUI","STUDY_LOOP_MENU end from="+loopStart+" to="+loopEnd);
 }
 void clearLoop(){
  // Release active MIDI notes before replacing the route and loop bounds.
  stopPlayback();
  loopStart=0;loopEnd=Double.MAX_VALUE;resetWrittenRoute();
  if(screens!=null)screens.clearLoopSelection();
  saveSession();updateInfo();
 }
 void toggle(){
  if(score==null){chooseScore();return;}
  if(mode==0){
   if(!waitStarted){beginWaitPreparation();return;}
   stopPlayback();
   return;
  }
  if(running){
   stopPlayback();return;
  }
  if(position>=score.end()-0.001){
   position=score.firstOnset(currentHand);
   scoreView.playhead=position;scoreView.invalidate();
   if(screens!=null)screens.followScore(position,true);
  }
  // A paused session preserves its current repeat/Coda pass.
  // Restart the itinerary only when the piece actually finished (which can
  // occur at Fine BEFORE the end of the written score).
  if(routeCursor!=null&&routeCursor.finished()){
   position=score.firstOnset(currentHand);
   routeCursor.reset();routeCursor.align(position);
   scoreView.playhead=position;scoreView.invalidate();
   if(screens!=null)screens.followScore(position,true);
  }
  if(mode==2){
   if(!midiConnection.snapshot().canSend){
    message("Demonstração requer uma saída MIDI aberta para o instrumento. Toque em USB-MIDI e selecione o piano.");
    return;
   }
   midiPlayback=new MidiPlayback(score,currentHand,(pitch,on,velocity)->send(pitch,on,velocity));
   midiPlayback.seek(position); // Includes notes beginning exactly at position zero.
  }
  running=true;
  previousTick=SystemClock.uptimeMillis();
  android.util.Log.i("PianoLivreUI","play=true mode="+mode);
  updateInfo();if(screens!=null)screens.updateSelections();
 }
 void beginWaitPreparation(){
  if(score==null||mode!=0||waitPreparing)return;
  waitStarted=true;waitPreparing=true;waitTraveling=false;waitRecognizer.clear();
  int seconds=WaitPreparation.configuredSeconds(prefs.getInt("user_"+user+"_wait_prep_seconds",5));
  long now=SystemClock.uptimeMillis();
  waitPreparation.start(now,seconds);
  final long run=++waitPreparationSequence;
  if(screens!=null){screens.startWaitPreparation(waitPreparation.durationMs());screens.updateSelections();}
  handler.postDelayed(()->{
   if(run!=waitPreparationSequence||!waitPreparing||!waitPreparation.finish(SystemClock.uptimeMillis()))return;
   waitPreparing=false;
   if(mode==0&&waitStarted&&foreground&&score!=null&&screens!=null&&screens.isStudyOpen()){
    prepareWaiting();screens.updateSelections();
   }else{waitStarted=false;if(screens!=null)screens.updateSelections();}
  },waitPreparation.durationMs());
 }
 void configureWaitPreparation(){
  EditText seconds=new EditText(this);seconds.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
  seconds.setSingleLine(true);
  seconds.setText(String.valueOf(WaitPreparation.configuredSeconds(prefs.getInt("user_"+user+"_wait_prep_seconds",5))));
  new AlertDialog.Builder(this).setTitle("Preparação do modo Espera")
   .setMessage("Duração da entrada visual antes de aguardar notas (1 a 30 segundos). Não haverá prazo para tocar após a preparação.")
   .setView(seconds).setPositiveButton("Salvar",(d,w)->{
    try{
     int selected=Integer.parseInt(seconds.getText().toString().trim());
     if(selected<1||selected>30)throw new NumberFormatException();
     prefs.edit().putInt("user_"+user+"_wait_prep_seconds",selected).apply();
    }catch(NumberFormatException ex){message("Informe entre 1 e 30 segundos.");}
   }).setNegativeButton("Cancelar",null).show();
 }
 void stopPlayback(){
  waitStarted=false;waitPreparing=false;waitPreparationSequence++;waitPreparation.cancel();
  if(screens!=null)screens.stopWaitPreparation();
  running=false;waitTraveling=false;allOff();saveSession();updateInfo();
  // A paused clock must stop the renderer's requestAnimationFrame extrapolation.
  if(screens!=null){screens.followScore(position,true);screens.updateSelections();}
 }
 Runnable ticker=new Runnable(){
  public void run(){
   if((running||waitTraveling)&&score!=null){
    long now=SystemClock.uptimeMillis();
    double elapsedMs=Math.max(0,Math.min(now-previousTick,250));
    double target=score.advanceBeats(position,elapsedMs,tempoFactor);
    previousTick=now;
    if(waitTraveling&&mode==0){
     position=Math.min(waitTravelTarget,target);
     if(position>=waitTravelTarget-.000001){
      position=waitTravelTarget;waitTraveling=false;
      waitRecognizer.arm(score.at(position,currentHand));
       android.util.Log.i("PianoLivreTimeline","WAIT_TRAVEL_END at="+position);
     }
    }else if(running){
     if(writtenRouteActive()){
      PerformanceRoute.Movement movement=routeCursor.tick(score,position,elapsedMs,tempoFactor);
      if(mode==2&&midiPlayback!=null)for(PerformanceRoute.Move step:movement.moves){
       if(step.seek)midiPlayback.seek(step.beat);
       else midiPlayback.advance(step.beat);
      }
      position=movement.position;
      if(movement.finished){running=false;allOff();}
     }else{
      double finish=Math.min(score.end(),loopEnd);
      if(finish<=loopStart)finish=score.end();
      double next=Math.min(target,finish);
      // A–B loop has priority and the non-routed path is linear.
      if(mode==2&&midiPlayback!=null)midiPlayback.advance(next);
      position=next;
      if(position>=finish-.000001){
       if(loopEnd<Double.MAX_VALUE&&loopStart<finish){
        position=loopStart;
        if(mode==2&&midiPlayback!=null)midiPlayback.seek(loopStart);
       }else{running=false;allOff();}
      }
     }
    }
    scoreView.playhead=position;
    scoreView.invalidate();
    if(screens!=null)screens.followScore(position,false);
    updateInfo(!running&&!waitTraveling);
    if(!running&&screens!=null)screens.updateSelections();
   }
   handler.postDelayed(this,25);
  }
 };
 final StudyInfoText studyInfoText=new StudyInfoText();
 void updateInfo(){updateInfo(true);}
 void updateInfo(boolean immediate){
  if(info==null)return;
  String text=studyInfoText.update(score!=null,score==null?0:score.tempoAt(position),
    tempoFactor,position,SystemClock.uptimeMillis(),immediate);
  if(!android.text.TextUtils.equals(info.getText(),text))info.setText(text);
  if(screens!=null)screens.updateStudyInfo(text);
 }
 LocalProfiles profileStore(){return new LocalProfiles(prefs);}
 void newProfile(){
  EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Nome do perfil");
  new AlertDialog.Builder(this).setTitle("Novo perfil").setView(input).setPositiveButton("Criar",(d,w)->{
   try{int id=profileStore().create(input.getText().toString());activateProfile(id);loadProfiles();}
   catch(RuntimeException ex){message(ex.getMessage());}
  }).setNegativeButton("Cancelar",null).show();
 }
 void loadProfiles(){
  try{
   List<LocalProfiles.Profile> list=profileStore().list();String[] names=new String[list.size()];int selected=0;
   int active=prefs.getInt("active_profile",0);
   for(int i=0;i<list.size();i++){LocalProfiles.Profile p=list.get(i);names[i]=LocalProfiles.AVATARS[p.avatar]+" "+p.name;if(p.id==active)selected=i;}
   user=list.get(selected).id;
   if(active!=user)prefs.edit().putInt("active_profile",user).apply();
   profiles.setOnItemSelectedListener(null);
   profiles.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));profiles.setSelection(selected);
   if(screens!=null&&screens.homeProfiles!=null){
    screens.homeProfiles.setOnItemSelectedListener(null);
    screens.homeProfiles.setAdapter(screens.homeProfileAdapter(names));screens.homeProfiles.setSelection(selected);
   }
   refreshChordWindow();if(screens!=null)screens.refreshLibrary();
   profiles.setOnItemSelectedListener(new Listener(index->selectProfileFromSpinner(index,list)));
   if(screens!=null&&screens.homeProfiles!=null)
    screens.homeProfiles.setOnItemSelectedListener(new Listener(index->selectProfileFromSpinner(index,list)));
  }catch(RuntimeException ex){message("Não foi possível ler os perfis: "+ex.getMessage());}
 }
 void selectProfileFromSpinner(int index,List<LocalProfiles.Profile> list){
  if(index<0||index>=list.size())return;
  int id=list.get(index).id;
  if(user!=id)activateProfile(id);
  if(profiles!=null&&profiles.getSelectedItemPosition()!=index)profiles.setSelection(index);
  if(screens!=null&&screens.homeProfiles!=null&&screens.homeProfiles.getSelectedItemPosition()!=index)screens.homeProfiles.setSelection(index);
 }
 void activateProfile(int id){if(user==id)return;if(screens!=null)screens.connectionLostCreation();stopPlayback();finishStudyWindow();applyProfile(id);}
 void applyProfile(int id){
  user=id;prefs.edit().putInt("active_profile",id).apply();refreshChordWindow();
  score=null;scoreView.score=null;scoreView.pressed.clear();currentFile="";currentName="";
  route=null;routeCursor=null;routeHasMarkers=false;waitRecognizer.clear();
  currentHand=0;mode=0;tempoFactor=1;scoreView.hand=0;scoreView.showKeyboard=false;
  String file=prefs.getString("user_"+id+"_file","");
  if(!file.isEmpty())loadScore(file,false);
  else{position=0;loopStart=0;loopEnd=Double.MAX_VALUE;scoreView.invalidate();screens.showHome();updateInfo();}
 }
 void manageProfile(){
  try{
   LocalProfiles.Profile current=null;for(LocalProfiles.Profile p:profileStore().list())if(p.id==user)current=p;
   if(current==null)return;final LocalProfiles.Profile selected=current;
   new AlertDialog.Builder(this).setTitle(selected.name).setItems(new String[]{"Renomear perfil","Escolher avatar","Excluir perfil"},(dialog,which)->{
    if(which==0){
     EditText input=new EditText(this);input.setSingleLine(true);input.setText(selected.name);
     new AlertDialog.Builder(this).setTitle("Renomear perfil").setView(input).setPositiveButton("Salvar",(d,w)->{
      try{profileStore().update(selected.id,input.getText().toString(),selected.avatar);loadProfiles();}catch(RuntimeException ex){message(ex.getMessage());}
     }).setNegativeButton("Cancelar",null).show();
    }else if(which==1){
     new AlertDialog.Builder(this).setTitle("Avatar").setItems(LocalProfiles.AVATARS,(d,index)->{
      try{profileStore().update(selected.id,selected.name,index);loadProfiles();}catch(RuntimeException ex){message(ex.getMessage());}
     }).show();
    }else new AlertDialog.Builder(this).setTitle("Excluir perfil?")
     .setMessage("Excluir este perfil e seus registros de estudo? As partituras compartilhadas serão mantidas.")
     .setPositiveButton("Excluir",(d,w)->{
      try{if(hasCreationDataForProfile(selected.id)){message("Este perfil ainda possui composições, inclusive itens na Lixeira ou recuperáveis. Exporte ou exclua definitivamente cada projeto antes de excluir o perfil.");return;}stopPlayback();finishStudyWindow();int active=profileStore().delete(selected.id);score=null;applyProfile(active);loadProfiles();}
      catch(RuntimeException ex){message(ex.getMessage());}
      catch(IOException ex){message("Não foi possível verificar as composições deste perfil; o perfil foi mantido.");}
     }).setNegativeButton("Cancelar",null).show();
   }).show();
  }catch(RuntimeException ex){message(ex.getMessage());}
 }
 void beginStudyWindow(){
  if(!foreground||score==null||currentFile.isEmpty()||screens==null||(!screens.isStudyOpen()&&!screens.isFreeOpen()))return;
  studyClock.start(SystemClock.uptimeMillis(),System.currentTimeMillis(),user,LibrarySearch.key(currentFile),
   currentName.isEmpty()?score.title:currentName,screens.isFreeOpen()?3:mode,currentHand,position,tempoFactor);
 }
 void finishStudyWindow(){
  StudyClock.Window window=studyClock.finish(SystemClock.uptimeMillis(),position,tempoFactor);
  if(window==null||prefs==null)return;
  try{new StudyHistory(prefs,window.user).record(window);}
  catch(RuntimeException ex){pendingHistoryError=ex.getMessage();android.util.Log.w("PianoLivreHistory","Período não registrado; dados anteriores mantidos");}
 }
 void showHistory(){
  if(pendingHistoryError!=null){String error=pendingHistoryError;pendingHistoryError=null;message(error);return;}
  try{
   StudyHistory store=new StudyHistory(prefs,user);JSONObject data=store.snapshot(),pieces=data.getJSONObject("pieces");
   List<String> keys=new ArrayList<>();Iterator<String> iterator=pieces.keys();long total=0,periods=0;
   while(iterator.hasNext()){String key=iterator.next();keys.add(key);total=Math.addExact(total,pieces.getJSONObject(key).getLong("timeMs"));periods=Math.addExact(periods,pieces.getJSONObject(key).getLong("periods"));}
   keys.sort((left,right)->Long.compare(pieces.optJSONObject(right).optLong("lastAt"),pieces.optJSONObject(left).optLong("lastAt")));
   if(keys.isEmpty()){message("Ainda não há registros. O tempo é registrado quando uma partitura fica aberta em primeiro plano.");return;}
   String[] items=new String[keys.size()+1];items[0]="Tempo na tela de estudo: "+StudyHistory.duration(total)+" · "+periods+" períodos";
   for(int i=0;i<keys.size();i++){
    JSONObject piece=pieces.getJSONObject(keys.get(i));long goal=piece.getLong("goalMinutes"),time=piece.getLong("timeMs");
    items[i+1]=piece.getString("title")+"\n"+StudyHistory.duration(time)+(goal>0?" · meta: "+goal+" min"+(time>=goal*60000L?" ✓":""):"");
   }
   new AlertDialog.Builder(this).setTitle("Histórico deste perfil").setItems(items,(d,index)->{
    if(index==0){message("Tempo em partitura aberta, incluindo pausas para leitura. Não representa avaliação de notas, ritmo ou esforço.");return;}
    String key=keys.get(index-1);JSONObject piece=pieces.optJSONObject(key);EditText minutes=new EditText(this);
    minutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);minutes.setHint("Minutos escolhidos por você");
    if(piece.optInt("goalMinutes")>0)minutes.setText(String.valueOf(piece.optInt("goalMinutes")));
    new AlertDialog.Builder(this).setTitle("Meta de tempo · "+piece.optString("title")).setView(minutes)
     .setPositiveButton("Salvar",(dd,w)->{try{int value=Integer.parseInt(minutes.getText().toString().trim());if(value<=0)throw new NumberFormatException();store.goal(key,value);}catch(NumberFormatException ex){message("Informe um número inteiro positivo de minutos.");}catch(RuntimeException ex){message(ex.getMessage());}})
     .setNeutralButton("Remover meta",(dd,w)->{try{store.goal(key,0);}catch(RuntimeException ex){message(ex.getMessage());}}).setNegativeButton("Cancelar",null).show();
   }).setNeutralButton("Períodos",(d,w)->showHistoryPeriods(data.optJSONArray("records")))
    .setNegativeButton("Apagar histórico e metas",(d,w)->new AlertDialog.Builder(this).setTitle("Apagar histórico deste perfil?")
     .setMessage("Os períodos, o tempo acumulado e as metas serão removidos. Faça backup se quiser conservá-los. As partituras e os favoritos serão mantidos.")
     .setPositiveButton("Apagar",(dd,ww)->{try{store.clear();}catch(RuntimeException ex){message(ex.getMessage());}}).setNegativeButton("Cancelar",null).show())
    .setPositiveButton("Fechar",null).show();
  }catch(Exception ex){message("Não foi possível ler o histórico: "+ex.getMessage());}
 }
 void showHistoryPeriods(JSONArray records){
  try{
   List<String> labels=new ArrayList<>();String[] modes={"Espera","Contínuo","Demonstração","Leitura livre"};
   java.text.DateFormat date=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT);
   for(int i=records.length()-1;i>=0;i--){JSONObject r=records.getJSONObject(i);labels.add(date.format(new Date(r.getLong("at")))+" · "+r.getString("title")+"\n"+modes[r.getInt("mode")]+" · "+StudyHistory.duration(r.getLong("elapsedMs")));}
   new AlertDialog.Builder(this).setTitle("Períodos registrados").setItems(labels.toArray(new String[0]),(d,index)->{}).setPositiveButton("Fechar",null).show();
  }catch(Exception ex){message("Não foi possível ler os períodos");}
 }
 void saveSession(){
  if(prefs==null||score==null)return;
  String key="user_"+user+"_";
  prefs.edit().putString(key+"file",SessionFiles.portable(currentFile))
   .putLong(key+"position",Double.doubleToLongBits(position))
   .putInt(key+"hand",currentHand).putInt(key+"mode",mode)
   .putLong(key+"tempo",Double.doubleToLongBits(tempoFactor))
   .putLong(key+"loop_start",Double.doubleToLongBits(loopStart))
   .putLong(key+"loop_end",Double.doubleToLongBits(loopEnd))
   .putBoolean(key+"keyboard",scoreView.showKeyboard)
   .putInt(key+"route_visit",routeCursor==null?0:routeCursor.index()).apply();
 }
 JSONObject makeBackup()throws Exception{
  saveSession();
  if(screens!=null)screens.prepareCreationBackup();
  return BackupArchive.create(prefs,getFilesDir());
 }
 void exportBackup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"pianolivre-backup.json");startActivityForResult(i,EXPORT);}
 boolean hasCreationDataForProfile(int id)throws IOException{
  if(new CreationProjectStore(getFilesDir()).hasProjectsForProfile(id))return true;
  File[] journals=new File(getFilesDir(),"creation-journals").listFiles((d,n)->n.endsWith(".journal"));
  if(journals!=null)for(File file:journals)if(CreationJournal.read(file).profileId==id)return true;
  return false;
 }
 private interface CreationExportSource{byte[] prepare()throws Exception;}
 static String sha256Creation(byte[] bytes)throws Exception{byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder text=new StringBuilder();for(byte b:hash)text.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return text.toString();}
 void exportCreationMidi(String id,String title){prepareCreationExport(id,title,()->MidiStandardFileWriter.write(new CreationProjectStore(getFilesDir()).get(id)));}
 void exportCreationTake(CreationProjectStore.Project p){prepareCreationExport(p.id,p.title,()->MidiStandardFileWriter.write(p));}
 private void prepareCreationExport(String id,String title,CreationExportSource source){
  if(creationExportPreparing){message("Aguarde a exportação em andamento.");return;}
  creationExportPreparing=true;pendingCreationExportId=id;
  creationExportDisk.execute(()->{try{
   byte[] bytes=source.prepare();String name="creation-export-"+UUID.randomUUID()+".mid";File target=new File(getCacheDir(),name);
   try(FileOutputStream out=new FileOutputStream(target)){out.write(bytes);out.getFD().sync();}
   String digest=sha256Creation(bytes);
   handler.post(()->{if(isDestroyed())return;pendingCreationExportFile=name;pendingCreationExportSha=digest;String fileName=title.replaceAll("[^\\p{L}\\p{N}._-]+","_").replaceAll("^[_ .-]+|[_ .-]+$","");if(fileName.isEmpty())fileName="composicao";
    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("audio/midi");intent.putExtra(Intent.EXTRA_TITLE,fileName+".mid");startActivityForResult(intent,CREATION_MIDI_EXPORT);});
  }catch(Exception error){handler.post(()->{clearCreationExport();message("Composição não validada para exportação: "+error.getMessage());});}});
 }
 private void clearCreationExport(){if(pendingCreationExportFile!=null&&pendingCreationExportFile.matches("creation-export-[a-f0-9-]{36}\\.mid"))new File(getCacheDir(),pendingCreationExportFile).delete();pendingCreationExportFile=pendingCreationExportSha=pendingCreationExportId=null;creationExportPreparing=false;}
 @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("creationExportId",pendingCreationExportId);state.putString("creationExportFile",pendingCreationExportFile);state.putString("creationExportSha",pendingCreationExportSha);}
 void restoreBackup(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,RESTORE);}
 void message(String m){lastMessageDialog=new AlertDialog.Builder(this).setMessage(m).setPositiveButton("OK",null).show();}
 @Override public void onBackPressed(){
  if(screens!=null&&screens.isLabOpen()){screens.backFromLab();return;}
  if(screens!=null&&(screens.isFreeOpen()||screens.isStudyOpen())){screens.showHome();return;}
  super.onBackPressed();
 }
 @Override public void onWindowFocusChanged(boolean hasFocus){
  super.onWindowFocusChanged(hasFocus);
  // Dialog windows may clear the Activity's fullscreen UI flag on API 29.
  if(hasFocus)getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
 }
 @Override protected void onResume(){
  super.onResume();foreground=true;midiInputEpoch++;
  clearMidiObservation();
  // Exactly one ticker chain, including after repeated background/foreground.
  handler.removeCallbacks(ticker);previousTick=SystemClock.uptimeMillis();handler.post(ticker);
  if(screens!=null)screens.resumeNotation();
  if(screens!=null)screens.resumeDiscovery();
  reconcileMidiDevices();if(screens!=null)screens.updateMidiIndicator();
  rearmWaitIfStudying();beginStudyWindow();
  android.util.Log.i("PianoLivreUI","LIFECYCLE_RESUMED");
 }
 @Override protected void onPause(){
  foreground=false;midiInputEpoch++;super.onPause();if(screens!=null){screens.stopLabDemonstration();screens.pauseDiscovery();screens.pauseCreation();}
  // Drop held-key bookkeeping across backgrounding; a later Note Off remains harmless.
  clearMidiObservation();
  stopPlayback();finishStudyWindow();saveSession();handler.removeCallbacks(ticker);
  chordCalibration.stop();
  if(screens!=null)screens.pauseNotation();
  android.util.Log.i("PianoLivreUI","LIFECYCLE_PAUSED");
 }
 @Override protected void onDestroy(){
  finishStudyWindow();
  midiDestroyed=true;
  if(midiManager!=null&&midiDeviceCallback!=null)midiManager.unregisterDeviceCallback(midiDeviceCallback);
  handler.removeCallbacks(ticker);midiConnection.destroy();
  if(screens!=null)screens.destroyNotation();creationExportDisk.shutdown();
  super.onDestroy();
 }
}

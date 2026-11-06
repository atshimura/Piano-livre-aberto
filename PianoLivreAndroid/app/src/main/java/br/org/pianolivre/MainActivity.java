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
 private final ZCreationExport creationExport=new ZCreationExport(this);
 private final ZStudyHistoryUi studyHistory=new ZStudyHistoryUi(this);
 private final ZProfilesUi profilesUi=new ZProfilesUi(this);
 private final ZChordCalibrationUi chordUi=new ZChordCalibrationUi(this);
 AlertDialog lastMessageDialog;
 PianoScreens screens;LinearLayout root,toolbar;final StudyInputState studyState=new StudyInputState();TextView status,info;Spinner hands,modes,profiles;SeekBar tempoBar;
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
 long beginCreationCapture(){midiInputEpoch++;clearMidiObservation();return midiInputEpoch;}
 private long outputTestSequence;
 Set<Integer> activeNotes=new HashSet<>(); Set<Integer> downKeys=new HashSet<>();Set<Integer> midiSources=new HashSet<>();
 final WaitRecognizer waitRecognizer=new WaitRecognizer();
 final WaitPreparation waitPreparation=new WaitPreparation();
 boolean waitStarted=false,waitPreparing=false;
 long waitPreparationSequence=0;
 final ChordCalibration chordCalibration=new ChordCalibration();
 final MidiPerformance performance=new MidiPerformance();
 MidiPlayback midiPlayback;
 final StudyClock studyClock=new StudyClock();
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
  final int sourceId=midiConnection.snapshot().target==null?-1:midiConnection.snapshot().target.id;
  final MidiProtocol decoder=new MidiProtocol();
  return new MidiReceiver(){
   @Override public synchronized void onSend(byte[] data,int offset,int count,long timestamp){
    if(generation!=midiGeneration)return;
    final long arrivalTimeNs=SystemClock.elapsedRealtimeNanos();
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
    final long uiSession=screens==null?-1:screens.discoveryGeneration();
    if(generation!=midiGeneration||inputEpoch!=midiInputEpoch||!foreground)return;
    if(screens!=null)for(int[] event:decoded)screens.captureCreationMidi(event[0],event[1],event[2],arrivalTimeNs,inputEpoch,timestamp,sourceId);
    handler.post(()->{
     if(generation!=midiGeneration||inputEpoch!=midiInputEpoch||!foreground)return;
     for(int[] event:decoded)onMidiMessage(event[0],event[1],event[2],packet,arrivalTime,arrivalTimeNs,timestamp,uiSession,false);
     if(screens!=null)screens.onLabMidiBatchEnd(packet,arrivalTime,uiSession);
    });
   }
  };
 }
 void rearmWaitIfStudying(){
  if(foreground&&waitStarted&&!waitPreparing&&score!=null&&mode==0&&screens!=null&&screens.isStudyOpen())prepareWaiting();
 }
 @Override public void onCreate(Bundle state){super.onCreate(state);if(state!=null)creationExport.restoreState(state);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);ZEdgeInsets.apply(this);
  // Keep screen awake only while the app is visible (no persistent wakelock).
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  prefs=getSharedPreferences("piano_data",MODE_PRIVATE);
  ZDebugSelfTests.runPreRecovery(this);
  try{BackupArchive.recover(prefs,getFilesDir());ImportedScoreDeletion.recover(getFilesDir(),scoreKey->LibraryStore.removeImportedEveryProfile(prefs,scoreKey));CreationScoreDeletion.recover(getFilesDir(),id->LibraryStore.removeCreationEveryProfile(prefs,id));}
  catch(Exception recovery){
   android.util.Log.e("PianoLivreBackup","Não foi possível recuperar restauração anterior",recovery);
   new AlertDialog.Builder(this).setTitle("Recuperação de dados")
    .setMessage("Não foi possível verificar os arquivos do último backup. Os dados não serão alterados. Detalhes: "+recovery.getMessage())
    .setPositiveButton("Fechar",(d,w)->finish()).setCancelable(false).show();
   return;
  }
  ZDebugSelfTests.runUpgrade(this);
  ZDebugSelfTests.redirectMidiSelfTestPrefs(this);
  midiManager=(MidiManager)getSystemService(MIDI_SERVICE);try{catalog=new JSONArray(readAsset("catalog.json"));}catch(Exception e){catalog=new JSONArray();}
  buildUi();loadProfiles();
  // Since 0.9.60 the launcher opens Home, not Study. Restore the active
  // profile's in-memory study preferences independently of score loading;
  // otherwise the first explicit selection overwrites saved 80% tempo with
  // the default 100% when loadScore(reset=true) calls saveSession().
  String studyPrefix="user_"+user+"_";
  double savedTempo=Double.longBitsToDouble(prefs.getLong(studyPrefix+"tempo",
   Double.doubleToLongBits(1.0)));
  tempoFactor=Double.isFinite(savedTempo)?Math.max(.25,Math.min(2.0,savedTempo)):1.0;
  currentHand=Math.max(0,Math.min(2,prefs.getInt(studyPrefix+"hand",0)));
  mode=Math.max(0,Math.min(2,prefs.getInt(studyPrefix+"mode",0)));
  // Legacy keyboard preference remains stored even though Study no longer
  // renders the visual keyboard guide. Keep it when opening a new score.
  studyState.showKeyboard=prefs.getBoolean(studyPrefix+"keyboard",false);
  String previous=prefs.getString("user_"+user+"_file","");
  // Every new Activity starts on Home, even when a profile has a saved score.
  // Keep user_N_file, position and all study preferences untouched. Loading a
  // score remains an explicit user action; debug engraving fixtures below may
  // still open Study intentionally when invoked with their test-only flags.
  android.util.Log.i("PianoLivreUI","STARTUP_HOME lastScoreSaved="+!previous.isEmpty());
  setupMidi();
  handler.post(ticker);
  ZDebugSelfTests.launchEngravingFixtures(this,previous);
  ZDebugSelfTests.postRuntimeSelfTests(this);
  registerPredictiveBack();
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
 void loadCreationScore(String id){loadScore("creation-score:"+id,true,false);}
 void addCreationScoreToLibrary(String id,java.util.function.Consumer<Boolean> onDone){
  final int owner=user;
  // Read and validate the exact saved MusicXML without blocking the UI.
  // Before linking, run the same live OSMD engraving as the Study screen.
  new Thread(()->{
   String problem=null;Score parsed=null;
   try{
    LibraryStore.validateCreationId(id);
    CreationProjectStore.Project source=new CreationProjectStore(getFilesDir()).get(id);
    if(source.profileId!=owner||source.trash||source.recoverable)
     throw new IOException("Composição indisponível para o perfil ativo");
    CreationScoreStore.Saved saved=new CreationScoreStore(getFilesDir()).read(id);
    if(saved.profileId!=owner)throw new IOException("Partitura de outro perfil");
    parsed=Score.read(new ByteArrayInputStream(saved.xml.getBytes(StandardCharsets.UTF_8)),
      "creation-score:"+id);
   }catch(Exception error){problem=error.getMessage();}
   final String failure=problem;final Score checked=parsed;
   runOnUiThread(()->{
    if(failure!=null||user!=owner){
     reportNotationIssue("PL-NOT-STORAGE","Validação para Biblioteca",
       failure==null?"Perfil ativo alterado":failure,null,
       ()->addCreationScoreToLibrary(id,onDone));
     onDone.accept(false);return;
    }
    CreationStudyCompatibility.check(this,checked,compatible->{
     if(!compatible){onDone.accept(false);return;}
     if(user!=owner){message("Perfil ativo alterado");onDone.accept(false);return;}
     try{
      // Detect a concurrent edit/delete between the probe and library update.
      CreationScoreStore.Saved current=new CreationScoreStore(getFilesDir()).read(id);
      if(!current.xml.equals(checked.sourceXml))
       throw new IOException("Partitura alterada durante a validação; verifique novamente");
      new LibraryStore(prefs,owner).addCreation(id);
      if(screens!=null)screens.refreshLibrary();
      message("Partitura compatível com Estudar e adicionada à Biblioteca Musical.");
      onDone.accept(true);
     }catch(Exception e){
      message("Não foi possível adicionar à Biblioteca: "+e.getMessage());onDone.accept(false);
     }
    });
   });
  },"PianoLivre-AdicionarBiblioteca").start();
 }
 void loadScore(String path,boolean reset,boolean free){
  stopPlayback();finishStudyWindow();writtenRepeats.reset();
  try{path=SessionFiles.resolve(getFilesDir(),path);}catch(IllegalArgumentException ex){message(ex.getMessage());return;}
  CreationProjectStore.Project original=null;CreationScoreStore.Saved saved=null;
  try{
   if(path.startsWith("creation-score:")){
    String id=path.substring(15);original=new CreationProjectStore(getFilesDir()).get(id);
    if(original.profileId!=user||original.trash||original.recoverable)
     throw new IOException("Partitura não disponível para o perfil ativo");
    saved=new CreationScoreStore(getFilesDir()).read(id);
   }
   try(InputStream input=saved!=null
       ?new ByteArrayInputStream(saved.xml.getBytes(StandardCharsets.UTF_8))
       :path.startsWith("scores/")?getAssets().open(path):new FileInputStream(path)){
    score=Score.read(input,path);
    currentFile=path;currentName=original!=null?original.title:score.title;
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
  ZSessionState.Study restored=ZSessionState.readStudy(prefs,user);
  if(reset){
   position=score.firstOnset(currentHand);loopStart=0;loopEnd=Double.MAX_VALUE;
  }else{
   currentHand=restored.hand;mode=restored.mode;tempoFactor=restored.tempo;
   loopStart=restored.loopStart;loopEnd=restored.loopEnd;
   studyState.showKeyboard=restored.showKeyboard;
   position=ZSessionState.readPosition(prefs,user,score.firstOnset(currentHand));
  }
  position=Math.max(0,Math.min(position,score.end()));
  if(routeCursor!=null){
   if(reset||!routeCursor.restore(restored.routeVisit,position))
    routeCursor.align(position);
  }
  studyState.score=score;studyState.hand=currentHand;studyState.playhead=position;
  if(tempoBar!=null)tempoBar.setProgress((int)Math.round(tempoFactor*100)-25);
  if(free){screens.showFree();}
  else{screens.showStudy();screens.displayScore(score,position);}
  updateInfo();saveSession();
  if(!score.importWarnings.isEmpty())message("Aviso de interpretação: "+String.join("; ",score.importWarnings));
  if(routeHasMarkers&&!route.supported())
   message("Atenção: não é possível determinar com segurança todos os retornos: "+
     String.join(", ",route.warnings)+". A reprodução desta partitura será linear.");
  }
 }catch(Exception e){
   final String retryPath=path;
   reportNotationIssue("PL-NOT-PARSE","Leitura MusicXML",e.getMessage(),null,
    ()->loadScore(retryPath,true,free));
  }}
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
  if(creationExport.onActivityResult(request,result,data))return;
  if(ZBackupFlow.onActivityResult(this,request,result,data))return;
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
    CreationScoreDeletion.recover(getFilesDir(),id->LibraryStore.removeCreationEveryProfile(prefs,id));
    if(screens!=null)screens.prepareCreationBackup();
    BackupArchive.export(prefs,getFilesDir(),()->getContentResolver().openOutputStream(uri));
    message("Backup exportado com verificação de integridade");
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
  if(studyState!=null){studyState.pressed.clear();}
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
  onMidiMessage(statusCode,data1,data2,midiPacketSequence.incrementAndGet(),SystemClock.uptimeMillis(),SystemClock.elapsedRealtimeNanos(),-1,screens==null?-1:screens.discoveryGeneration(),true);
 }
 private void onMidiMessage(int statusCode,int data1,int data2,long packet,long eventTime,long eventTimeNs,long androidTime,long uiSession,boolean recordCreation){
  currentMidiPacket=packet;currentMidiEventTime=eventTime;currentUiSession=uiSession;
  if(recordCreation&&screens!=null)screens.onCreationMidi(statusCode,data1,data2,eventTimeNs,midiInputEpoch,androidTime,midiConnection.snapshot().target==null?-1:midiConnection.snapshot().target.id);
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
  studyState.pressed.clear();studyState.pressed.addAll(downKeys);
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
 void refreshChordWindow(){chordUi.refreshWindow();}
 void showChordSettings(){chordUi.settings();}
 void beginChordCalibration(){chordUi.beginCalibration();}
 void updateChordCalibrationStatus(){chordUi.updateStatus();}
 void editChordWindow(long measuredMs){chordUi.editWindow(measuredMs);}
 void selectHand(int selected){
  if(selected<0||selected>2)return;
  stopPlayback();finishStudyWindow();
  currentHand=selected;studyState.hand=selected;
  if(screens!=null&&screens.notation!=null)screens.notation.setStudyHand(selected);
  if(score!=null)position=HandNavigation.nextPosition(score,position,selected,loopStart,loopEnd);
  if(routeCursor!=null)routeCursor.align(position);
  studyState.playhead=position;
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
   position=waitTravelTarget;studyState.playhead=position;
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
  studyState.playhead=position;
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
  studyState.playhead=position;
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
   studyState.playhead=position;
   if(screens!=null)screens.followScore(position,true);
  }
  // A paused session preserves its current repeat/Coda pass.
  // Restart the itinerary only when the piece actually finished (which can
  // occur at Fine BEFORE the end of the written score).
  if(routeCursor!=null&&routeCursor.finished()){
   position=score.firstOnset(currentHand);
   routeCursor.reset();routeCursor.align(position);
   studyState.playhead=position;
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
    studyState.playhead=position;
    
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
 void newProfile(){profilesUi.create();}
 void loadProfiles(){profilesUi.load();}
 void selectProfileFromSpinner(int index,List<LocalProfiles.Profile> list){profilesUi.selectFromSpinner(index,list);}
 void activateProfile(int id){profilesUi.activate(id);}
 void applyProfile(int id){profilesUi.apply(id);}
 void manageProfile(){profilesUi.manage();}
 void beginStudyWindow(){studyHistory.begin();}
 void finishStudyWindow(){studyHistory.finish();}
 void showHistory(){studyHistory.show();}
 void showHistoryPeriods(JSONArray records){studyHistory.showPeriods(records);}
/** Drop an in-memory last-study reference after its score is hidden or removed. */
 void clearRemovedScoreInMemory(String scoreKey){
  if(currentFile==null||currentFile.isEmpty()||!scoreKey.equals(LibrarySearch.key(currentFile)))return;
  finishStudyWindow();
  waitStarted=false;waitPreparing=false;waitPreparationSequence++;waitPreparation.cancel();
  running=false;waitTraveling=false;allOff();
  score=null;currentFile="";currentName="";position=0;
  studyState.score=null;studyState.pressed.clear();
  route=null;routeCursor=null;routeHasMarkers=false;waitRecognizer.clear();updateInfo();
 }
 void saveSession(){
  if(prefs==null||score==null)return;
  // Delegado package-private: o contrato test_study_compact_chrome_contract
  // crava a chamada a.saveSession() vinda de PianoScreens.
  ZSessionState.save(prefs,user,currentFile,position,currentHand,mode,tempoFactor,
   loopStart,loopEnd,studyState.showKeyboard,routeCursor==null?0:routeCursor.index());
 }
 JSONObject makeBackup()throws Exception{
  saveSession();
  ImportedScoreDeletion.recover(getFilesDir(),scoreKey->LibraryStore.removeImportedEveryProfile(prefs,scoreKey));
  CreationScoreDeletion.recover(getFilesDir(),id->LibraryStore.removeCreationEveryProfile(prefs,id));
  if(screens!=null)screens.prepareCreationBackup();
  return BackupArchive.create(prefs,getFilesDir());
 }
 void exportBackup(){ZBackupFlow.export(this);}
 boolean hasCreationDataForProfile(int id)throws IOException{return ZBackupFlow.hasCreationDataForProfile(this,id);}
 /** Delegados package-private: MusicCreationScreen chama a.exportCreation*;
  * CreationSelfTest chama MainActivity.sha256Creation (contrato
  * test_creation_responsive_contract + self-tests). */
 void exportCreationMidi(String id,String title){creationExport.exportMidi(id,title);}
 void exportCreationTake(CreationProjectStore.Project p){creationExport.exportTake(p);}
 static String sha256Creation(byte[] bytes)throws Exception{return ZCreationExport.sha256(bytes);}
 @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);creationExport.saveState(state);}
 void restoreBackup(){ZBackupFlow.restore(this);}
 // Textos do fluxo de falhas de notação: fonte da verdade permanece aqui
 // (contratos tests/test_impl4_osmd_contract.py verificam estes literais);
 // a implementação do diálogo vive em ZNotationFailures.
 static final String NOTATION_FAILURE_TITLE="Não foi possível exibir a partitura";
 static final String NOTATION_COPY_BUTTON="Copiar diagnóstico";
 void reportNotationIssue(String code,String stage,String details,String xml,Runnable retry){ZNotationFailures.reportNotationIssue(this,code,stage,details,xml,retry);}
 void showNotationFailure(NotationFailure failure,Runnable retry){ZNotationFailures.showNotationFailure(this,failure,retry);}
 void showNotationFailure(NotationFailure failure,Runnable retry,Runnable abort){ZNotationFailures.showNotationFailure(this,failure,retry,abort);}
 void showNotationHistory(){ZNotationFailures.showNotationHistory(this);}
 void message(String m){if(isFinishing()||isDestroyed())return;lastMessageDialog=new AlertDialog.Builder(this).setMessage(m).setPositiveButton("OK",null).show();}
 @Override public void onBackPressed(){
  onSystemBack();
 }
 /** Navegação do botão voltar, compartilhada pelo caminho legado (API 26-32,
  * via onBackPressed) e pelo predictive back (API 33+, via OnBackInvokedCallback).
  * Ordem idêntica à histórica: Laboratório, depois Estudo/Leitura livre, e por
  * fim o comportamento padrão do sistema (fechar a activity, como
  * Activity.onBackPressed faz). */
 private void onSystemBack(){
  if(screens!=null&&screens.isLabOpen()){screens.backFromLab();return;}
  if(screens!=null&&(screens.isFreeOpen()||screens.isStudyOpen())){screens.backFromStudy();return;}
  finish();
 }
 private android.window.OnBackInvokedCallback backInvokedCallback;
 /** Predictive back (Android 13+/API 33+): o sistema entrega o gesto/botão ao
  * dispatcher quando android:enableOnBackInvokedCallback="true"; onBackPressed
  * deixa de ser chamado nessas versões. O registro é protegido por SDK_INT,
  * então Androids 8-12 seguem 100% no caminho legado. */
 private void registerPredictiveBack(){
  if(android.os.Build.VERSION.SDK_INT<33)return;
  backInvokedCallback=this::onSystemBack;
  getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
   android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,backInvokedCallback);
 }
 private void unregisterPredictiveBack(){
  if(android.os.Build.VERSION.SDK_INT<33||backInvokedCallback==null)return;
  getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backInvokedCallback);
  backInvokedCallback=null;
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
  if(screens!=null)screens.resumeCreation();
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
  unregisterPredictiveBack();
  finishStudyWindow();
  midiDestroyed=true;
  if(midiManager!=null&&midiDeviceCallback!=null)midiManager.unregisterDeviceCallback(midiDeviceCallback);
  handler.removeCallbacks(ticker);midiConnection.destroy();
  if(screens!=null)screens.destroyNotation();creationExport.shutdown();
  super.onDestroy();
 }
}

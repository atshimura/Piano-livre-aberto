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
 private final ZMidiDevices midiDevices=new ZMidiDevices(this);
 private final ZMidiInput midiInput=new ZMidiInput(this);
 private final ZScoreLoader scoreLoader=new ZScoreLoader(this);
 private final ZScoreImport scoreImport=new ZScoreImport(this);
 AlertDialog lastMessageDialog;
 PianoScreens screens;LinearLayout root,toolbar;final StudyInputState studyState=new StudyInputState();TextView status,info;Spinner hands,modes,profiles;SeekBar tempoBar;
 Score score;String currentFile="";String currentName="";int currentHand=0;int mode=0;boolean running=false,waitTraveling=false;double waitTravelTarget=0,position=0;double tempoFactor=1,loopStart=0,loopEnd=Double.MAX_VALUE;
 MidiManager midiManager;MidiManager.DeviceCallback midiDeviceCallback;
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
 volatile long midiInputEpoch;boolean midiDestroyed;
 long midiInputEpoch(){return midiInputEpoch;}
 long beginCreationCapture(){midiInputEpoch++;clearMidiObservation();return midiInputEpoch;}
 long outputTestSequence;
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
 final java.util.concurrent.atomic.AtomicLong midiPacketSequence=new java.util.concurrent.atomic.AtomicLong();
 long currentMidiPacket=0,currentUiSession=-1,currentMidiEventTime=0;
 volatile boolean foreground=false;
 int receivedMessages=0, sentMessages=0, lastMidiChannel=0;
 int lastMidiNote=-1, lastMidiVelocity=0;
 boolean sustainDown=false;
 String connectionDescription="Nenhum dispositivo conectado",lastMidiMessage="Nenhuma mensagem recebida";
 TextView midiDiagnosticText;

 Handler handler=new Handler(Looper.getMainLooper());long previousTick;int user=0;JSONArray catalog;
 android.content.SharedPreferences prefs;
 MidiReceiver createReceiver(final long generation){
  // Delegado package-private: CreationSelfTest/MidiSessionSelfTest chamam
  // a.createReceiver(...) e a reconciliação usa o retorno desta assinatura.
  return midiInput.createReceiver(generation);
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

 void loadScore(String path,boolean reset,boolean free){scoreLoader.loadScore(path,reset,free);}
 /** Barra de tempo reflete o fator de tempo aplicado à sessão (estado da
  * casca); literal cravado pelo contrato test_workflow_contract_audit. */
 void applyTempoFactorToBar(){
  if(tempoBar!=null)tempoBar.setProgress((int)Math.round(tempoFactor*100)-25);
 }
 void chooseMidiHands(MidiFileImport.Result midi,String name,List<Integer> tracks,int index,Map<Integer,Integer> hands){scoreImport.chooseMidiHands(midi,name,tracks,index,hands);}
 void importScore(){scoreImport.importScore();}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(creationExport.onActivityResult(request,result,data))return;
  if(ZBackupFlow.onActivityResult(this,request,result,data))return;
  if(result!=RESULT_OK||data==null)return;
  Uri uri=data.getData();if(uri==null)return;
  try {
   if(request==IMPORT){scoreImport.handleImport(uri);}
   else if(request==EXPORT){
    CreationScoreDeletion.recover(getFilesDir(),id->LibraryStore.removeCreationEveryProfile(prefs,id));
    if(screens!=null)screens.prepareCreationBackup();
    BackupArchive.export(prefs,getFilesDir(),()->getContentResolver().openOutputStream(uri));
    message("Backup exportado com verificação de integridade");
   }
  }catch(Exception e){message("Erro: "+e.getMessage());}
 }
 void setupMidi(){midiDevices.setupMidi();}
 void reconcileMidiDevices(){midiDevices.reconcileMidiDevices();}
 void setMidiStatus(String description){midiDevices.setMidiStatus(description);}
 void selectMidi(){midiDevices.selectMidi();}
 void disconnectMidi(){midiDevices.disconnectMidi();}
 void clearMidiObservation(){midiInput.clearMidiObservation();}
 void send(int pitch,boolean on){send(pitch,on,74);}
 void send(int pitch,boolean on,int velocity){midiInput.send(pitch,on,velocity);}
 void allOff(){midiInput.allOff();}
 void onMidiMessage(int statusCode,int data1,int data2){
  // Delegado package-private: os autotestes gravam eventos com a.onMidiMessage(...).
  midiInput.onMidiMessage(statusCode,data1,data2);
 }
 void refreshMidiDiagnostics(){midiInput.refreshMidiDiagnostics();}
 void showMidiDiagnostics(){midiInput.showMidiDiagnostics();}
 void testMidiOutput(){midiInput.testMidiOutput();}
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

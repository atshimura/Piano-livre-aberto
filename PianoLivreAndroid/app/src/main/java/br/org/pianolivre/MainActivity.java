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
 static final int IMPORT=100,EXPORT=101,RESTORE=102;
 PianoScreens screens;LinearLayout root,toolbar;PianoView scoreView;TextView status,info;Spinner hands,modes,profiles;SeekBar tempoBar;
 Score score;String currentFile="";String currentName="";int currentHand=0;int mode=0;boolean running=false,waitTraveling=false;double waitTravelTarget=0,position=0;double tempoFactor=1,loopStart=0,loopEnd=Double.MAX_VALUE;
 MidiManager midiManager;MidiDevice device;MidiInputPort outputPort;MidiOutputPort inputPort;MidiDeviceInfo activeInfo;
 Set<Integer> activeNotes=new HashSet<>(); Set<Integer> downKeys=new HashSet<>();Set<Integer> midiSources=new HashSet<>();
 final WaitRecognizer waitRecognizer=new WaitRecognizer();
 final ChordCalibration chordCalibration=new ChordCalibration();
 TextView chordCalibrationStatus;
 final MidiPerformance performance=new MidiPerformance();
 MidiPlayback midiPlayback;
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
 final MidiProtocol midiDecoder=new MidiProtocol();
 int openingMidiId=-1, receivedMessages=0, sentMessages=0, lastMidiChannel=0;
 int lastMidiNote=-1, lastMidiVelocity=0;
 boolean sustainDown=false;
 String connectionDescription="Nenhum dispositivo conectado",lastMidiMessage="Nenhuma mensagem recebida";
 TextView midiDiagnosticText;

 Handler handler=new Handler(Looper.getMainLooper());long previousTick;int user=0;JSONArray catalog;
 android.content.SharedPreferences prefs;
 private final MidiReceiver receiver=new MidiReceiver(){
  @Override public void onSend(byte[] data,int offset,int count,long timestamp){
   midiDecoder.feed(data,offset,count,(st,d1,d2)->handler.post(()->onMidiMessage(st,d1,d2)));
  }
 };
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
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
  try{BackupArchive.recover(prefs,getFilesDir());}
  catch(Exception recovery){
   android.util.Log.e("PianoLivreBackup","Não foi possível recuperar restauração anterior",recovery);
   new AlertDialog.Builder(this).setTitle("Recuperação de dados")
    .setMessage("Não foi possível verificar os arquivos do último backup. Os dados não serão alterados. Detalhes: "+recovery.getMessage())
    .setPositiveButton("Fechar",(d,w)->finish()).setCancelable(false).show();
   return;
  }
  midiManager=(MidiManager)getSystemService(MIDI_SERVICE);try{catalog=new JSONArray(readAsset("catalog.json"));}catch(Exception e){catalog=new JSONArray();}
  buildUi();loadProfiles();setupMidi();String previous=prefs.getString("user_"+user+"_file","");if(!previous.isEmpty())loadScore(previous,false);
  handler.post(ticker);
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
 void loadScore(String path,boolean reset,boolean free){stopPlayback();writtenRepeats.reset();try(InputStream input=path.startsWith("scores/")?getAssets().open(path):new FileInputStream(path)){score=Score.read(input,path);currentFile=path;currentName=score.title;
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
  if(routeHasMarkers&&!route.supported())
   message("Atenção: não é possível determinar com segurança todos os retornos: "+
     String.join(", ",route.warnings)+". A reprodução desta partitura será linear.");
 }catch(Exception e){message("Não foi possível importar: "+e.getMessage());}}
 void importScore(){Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("*/*");in.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(in,IMPORT);}
 @Override protected void onActivityResult(int request,int result,Intent data){
  super.onActivityResult(request,result,data);
  if(result!=RESULT_OK||data==null)return;
  Uri uri=data.getData();if(uri==null)return;
  try {
   if(request==IMPORT){
    File dir=new File(getFilesDir(),"imported");dir.mkdirs();
    String name="Importada_"+System.currentTimeMillis()+".mxl";
    String display="";
    try(android.database.Cursor cursor=getContentResolver().query(uri,null,null,null,null)){
     if(cursor!=null&&cursor.moveToFirst()){
      int idx=cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
      if(idx>=0)display=cursor.getString(idx);
     }
    }
    if(display.endsWith(".xml")||display.endsWith(".musicxml"))name=name.replace(".mxl",".xml");
    else if(display.endsWith(".mxl"))name=display.replaceAll("[^a-zA-Z0-9._-]","_");
    File dest=new File(dir,name);
    try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(dest)){
     out.write(all(in));
    }
    loadScore(dest.getAbsolutePath(),true);
   }else if(request==EXPORT){
    try(OutputStream out=getContentResolver().openOutputStream(uri)){
     if(out==null)throw new IOException("Não foi possível abrir o arquivo de backup");
     byte[] backup=makeBackup().toString().getBytes(StandardCharsets.UTF_8);
     out.write(backup);out.flush();
    }
    message("Backup exportado com verificação de integridade");
   }else if(request==RESTORE){
    JSONObject backup;
    try(InputStream in=getContentResolver().openInputStream(uri)){
     backup=new JSONObject(new String(BackupArchive.readLimited(in,95*1024*1024),StandardCharsets.UTF_8));
    }
    String summary=BackupArchive.summary(backup);
    new AlertDialog.Builder(this).setTitle("Substituir dados locais?")
     .setMessage(summary+"\n\nEsta operação não mescla os dados. Confirme apenas se você já salvou um backup do estado atual.")
     .setPositiveButton("Substituir",(dialog,which)->{
      stopPlayback();
      try{
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
  midiManager.registerDeviceCallback(new MidiManager.DeviceCallback(){
   @Override public void onDeviceAdded(MidiDeviceInfo info){
    handler.post(()->{
     if(info.getType()==MidiDeviceInfo.TYPE_USB){
      if(device==null&&openingMidiId<0)connectMidi(info);
     }
    });
   }
   @Override public void onDeviceRemoved(MidiDeviceInfo info){
    if(activeInfo!=null&&activeInfo.getId()==info.getId())
     handler.post(()->{
      if(activeInfo!=null&&activeInfo.getId()==info.getId()){
       stopPlayback();disconnectMidi();
       setMidiStatus("Piano desconectado · sessão pausada");
      }
     });
   }
  },handler);
  for(MidiDeviceInfo info:midiManager.getDevices()){
   if(info.getType()==MidiDeviceInfo.TYPE_USB){
    connectMidi(info);return;
   }
  }
  setMidiStatus("Conecte seu piano · toque aqui para diagnóstico");
 }
 String midiName(MidiDeviceInfo info){
  if(info==null)return "Nenhum";
  String name=info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
  return name==null||name.trim().isEmpty()?"Dispositivo MIDI "+info.getId():name;
 }
 void setMidiStatus(String description){
  connectionDescription=description;
  if(status!=null)status.setText(description);
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
  if(info==null||midiManager==null)return;
  if(device!=null&&activeInfo!=null&&activeInfo.getId()==info.getId())return;
  if(openingMidiId==info.getId())return;
  stopPlayback();disconnectMidi();
  activeInfo=info;openingMidiId=info.getId();
  setMidiStatus("Abrindo "+midiName(info)+"…");
  midiManager.openDevice(info,opened->{
   openingMidiId=-1;
   if(activeInfo==null||activeInfo.getId()!=info.getId()){
    if(opened!=null)try{opened.close();}catch(Exception ignored){}
    return;
   }
   if(opened==null){setMidiStatus("Não foi possível abrir "+midiName(info));return;}
   device=opened;
   try{
    if(info.getOutputPortCount()>0){
     inputPort=opened.openOutputPort(0); // Instrument -> Android
     if(inputPort!=null)inputPort.connect(receiver);
    }
    if(info.getInputPortCount()>0){
     outputPort=opened.openInputPort(0); // Android -> Instrument
    }
   }catch(Exception error){
    android.util.Log.e("PianoLivreMIDI","Erro nas portas MIDI",error);
   }
   String connections="Entrada "+(inputPort==null?"INDISPONÍVEL":"OK")
                      +" / Saída "+(outputPort==null?"INDISPONÍVEL":"OK");
   setMidiStatus(midiName(info)+": "+connections);
  },handler);
 }
 void disconnectMidi(){
  try{
   if(inputPort!=null){inputPort.disconnect(receiver);inputPort.close();}
   if(outputPort!=null)outputPort.close();
   if(device!=null)device.close();
  }catch(Exception error){android.util.Log.w("PianoLivreMIDI","Desconexão",error);}
  inputPort=null;outputPort=null;device=null;activeInfo=null;openingMidiId=-1;
  midiDecoder.reset();downKeys.clear();midiSources.clear();waitRecognizer.clear();performance.disconnect();chordCalibration.stop();activeNotes.clear();sustainDown=false;
  if(scoreView!=null){scoreView.pressed.clear();scoreView.invalidate();}
  if(screens!=null)screens.updatePressed(downKeys);
 }
 void send(int pitch,boolean on){send(pitch,on,74);}
 void send(int pitch,boolean on,int velocity){
  if(outputPort==null)return;
  try{
   byte[] bytes={(byte)(on?0x90:0x80),(byte)pitch,(byte)(on?velocity:0)};
   outputPort.send(bytes,0,bytes.length);sentMessages++;
   android.util.Log.d("PianoLivreMIDI","TX "+(on?"ON ":"OFF ")+pitch);
   refreshMidiDiagnostics();
  }catch(Exception error){
   android.util.Log.e("PianoLivreMIDI","Falha ao enviar MIDI",error);
   setMidiStatus("Erro de envio MIDI · abra diagnóstico");
  }
 }
 void allOff(){
  if(midiPlayback!=null){midiPlayback.stop();midiPlayback=null;}
  for(int note:new HashSet<>(activeNotes))send(note,false);
  activeNotes.clear();
  if(outputPort!=null)try{
   outputPort.send(new byte[]{(byte)0xB0,123,0},0,3);
  }catch(Exception error){android.util.Log.w("PianoLivreMIDI","All notes off",error);}
 }
 void onMidiMessage(int statusCode,int data1,int data2){
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
  }else if(command==0xB0&&data1==64){
   performance.control64(statusCode&15,data2,SystemClock.uptimeMillis());
   // CC64 belongs to a MIDI channel. Releasing one channel must not
   // report the pedal as up while another channel still holds sustain.
   sustainDown=false;
   for(int ch=0;ch<16;ch++)if(performance.pedalDown(ch)){sustainDown=true;break;}
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
  return "Dispositivo: "+midiName(activeInfo)
   +"\nSituação: "+connectionDescription
   +"\nPorta de entrada (piano → app): "+(inputPort==null?"não conectada":"aberta")
   +"\nPorta de saída (app → piano): "+(outputPort==null?"não conectada":"aberta")
   +"\nMensagens recebidas: "+receivedMessages
   +"\nMensagens enviadas: "+sentMessages
   +"\nÚltima mensagem: "+lastMidiMessage
   +"\nNotas pressionadas: "+downKeys
   +"\nPedal: "+(sustainDown?"pressionado":"solto")
   +"\nNotas fisicamente pressionadas: "+performance.physicalKeysDown()
   +"\nVozes retidas por tecla/pedal: "+performance.soundingVoices()
   +"\nNotas concluídas registradas: "+performance.completed().size()
   +"\nJanela de acorde: "+(waitRecognizer.chordWindowMs()==null?"aguardando calibração":waitRecognizer.chordWindowMs()+" ms (ajustável)")
   +"\n\nToque algumas teclas do Yamaha e verifique se o contador aumenta."
   +"\nO teste de saída envia Dó4 ao instrumento por meio segundo."
   +"\nO volume e a configuração de recepção MIDI do piano precisam permitir som.";
 }
 void refreshMidiDiagnostics(){
  if(midiDiagnosticText!=null)midiDiagnosticText.setText(midiDiagnosticReport());
 }
 void showMidiDiagnostics(){
  TextView body=new TextView(this);body.setTextSize(14);
  body.setPadding(24,20,24,20);midiDiagnosticText=body;refreshMidiDiagnostics();
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Diagnóstico do Yamaha P-45")
   .setView(body).setPositiveButton("Testar saída: Dó4",(d,which)->testMidiOutput())
   .setNeutralButton("Escolher dispositivo",(d,which)->selectMidi())
   .setNegativeButton("Fechar",null).create();
  dialog.setOnDismissListener(d->midiDiagnosticText=null);
  dialog.show();
 }
 void testMidiOutput(){
  if(outputPort==null){message("Sem porta de saída MIDI. Selecione e conecte o Yamaha em USB-MIDI.");return;}
  send(60,true,88);
  handler.postDelayed(()->send(60,false,0),550);
  if(status!=null)status.setText("Teste MIDI enviado: Dó4 por 0,55 s");
 }
 /** MIDI sources are tracked by channel+pitch. New attacks only. */
 void onKey(int pitch,boolean pressed,int channel){
  if(pitch<0||pitch>127||channel<0||channel>15)return;
  int source=(channel<<7)|pitch;
  boolean newAttack=false;
  long now=SystemClock.uptimeMillis();
  if(pressed){
   if(midiSources.add(source))newAttack=downKeys.add(pitch);
   if(newAttack&&chordCalibration.recording())chordCalibration.noteOn(pitch,now);
  }else{
   midiSources.remove(source);
   boolean stillPressed=false;
   for(int ch=0;ch<16;ch++)if(midiSources.contains((ch<<7)|pitch)){stillPressed=true;break;}
   if(!stillPressed){
    downKeys.remove(pitch);waitRecognizer.noteOff(pitch);
    if(chordCalibration.recording()&&chordCalibration.noteOff(pitch,now))updateChordCalibrationStatus();
   }
  }
  scoreView.pressed.clear();scoreView.pressed.addAll(downKeys);scoreView.invalidate();
  if(screens!=null)screens.updatePressed(downKeys);
  if(chordCalibration.recording()||score==null||mode!=0||!pressed||waitTraveling||
      (screens!=null&&screens.isFreeOpen()))return;
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
  if(inputPort==null){message("Conecte primeiro a entrada MIDI do Yamaha P-45.");return;}
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
  stopPlayback();
  currentHand=selected;scoreView.hand=selected;
  if(score!=null)position=HandNavigation.nextPosition(score,position,selected,loopStart,loopEnd);
  if(routeCursor!=null)routeCursor.align(position);
  scoreView.playhead=position;scoreView.invalidate();
  if(mode==0)prepareWaiting();else if(screens!=null)screens.followScore(position,true);
  saveSession();updateInfo();
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
 void clearLoop(){
  loopStart=0;loopEnd=Double.MAX_VALUE;resetWrittenRoute();
  if(screens!=null)screens.clearLoopSelection();
  saveSession();updateInfo();
 }
 void toggle(){
  if(score==null){chooseScore();return;}
  if(mode==0){
   if(inputPort==null)showMidiDiagnostics();
   else message("Modo de espera ativo: toque as notas no Yamaha. O estudo avança quando as notas esperadas são reconhecidas.");
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
   if(outputPort==null){
    message("Demonstração requer uma saída MIDI aberta para o Yamaha. Toque em USB-MIDI e selecione o piano.");
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
 void stopPlayback(){
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
    updateInfo();
    if(!running&&screens!=null)screens.updateSelections();
   }
   handler.postDelayed(this,25);
  }
 };
 void updateInfo(){if(info==null)return;info.setText("♫ "+(score==null?"—":Math.round(score.tempoAt(position)))+" BPM × "+Math.round(tempoFactor*100)+"% · posição "+String.format(Locale.US,"%.1f",position));}
 void newProfile(){if(getProfileArray().length()>=20){message("Limite de 20 perfis neste protótipo");return;}EditText t=new EditText(this);t.setHint("Nome do usuário");new AlertDialog.Builder(this).setTitle("Novo perfil").setView(t).setPositiveButton("Criar",(d,w)->{String name=t.getText().toString().trim();if(name.isEmpty())return;JSONArray arr=getProfileArray();arr.put(name);prefs.edit().putString("profiles",arr.toString()).apply();loadProfiles();profiles.setSelection(arr.length()-1);}).setNegativeButton("Cancelar",null).show();}
 JSONArray getProfileArray(){try{return new JSONArray(prefs.getString("profiles","[\"Usuário 1\"]"));}catch(Exception e){return new JSONArray().put("Usuário 1");}}
 void loadProfiles(){
  JSONArray arr=getProfileArray();String[] names=new String[arr.length()];
  for(int i=0;i<arr.length();i++)names[i]=arr.optString(i,"Usuário "+(i+1));
  ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names);
  profiles.setAdapter(adapter);
  user=Math.max(0,Math.min(prefs.getInt("active_profile",user),names.length-1));
  profiles.setSelection(user);
  refreshChordWindow();
  profiles.setOnItemSelectedListener(new Listener(p->{
   if(user==p)return;
   stopPlayback(); // IMPORTANT: save old user's song BEFORE switching its index.
   user=p;
   prefs.edit().putInt("active_profile",user).apply();
   refreshChordWindow();
   score=null;scoreView.score=null;scoreView.pressed.clear();currentFile="";currentName="";
   String file=prefs.getString("user_"+user+"_file","");
   if(!file.isEmpty())loadScore(file,false);
   else{
    position=0;loopStart=0;loopEnd=Double.MAX_VALUE;scoreView.invalidate();
    screens.showHome();updateInfo();
   }
  }));
 }
 void saveSession(){
  if(prefs==null||score==null)return;
  String key="user_"+user+"_";
  prefs.edit().putString(key+"file",currentFile)
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
  return BackupArchive.create(prefs,getFilesDir());
 }
 void exportBackup(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"pianolivre-backup.json");startActivityForResult(i,EXPORT);}
 void restoreBackup(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");startActivityForResult(i,RESTORE);}
 void message(String m){new AlertDialog.Builder(this).setMessage(m).setPositiveButton("OK",null).show();}
 @Override public void onBackPressed(){
  if(screens!=null&&(screens.isFreeOpen()||screens.isStudyOpen())){screens.showHome();return;}
  super.onBackPressed();
 }
 @Override protected void onPause(){super.onPause();stopPlayback();saveSession();}
 @Override protected void onDestroy(){
  handler.removeCallbacks(ticker);disconnectMidi();
  if(screens!=null)screens.destroyNotation();
  super.onDestroy();
 }
}

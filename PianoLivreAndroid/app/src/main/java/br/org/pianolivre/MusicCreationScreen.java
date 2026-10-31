package br.org.pianolivre;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.os.Handler;
import java.io.File;import java.io.IOException;
import java.util.*;

/** Native responsive entry point and one-take workflow for Laboratory → Creation. */
final class MusicCreationScreen extends LinearLayout {
 AlertDialog activeDialog;
 private final MainActivity a;private final Runnable back;private final LinearLayout body,bar;private final ScrollView scroll;private int listScrollY,recoveryRetries;private long formRevision,homeRevision;private volatile String recoveryWarning;
 private final java.util.concurrent.ExecutorService disk=java.util.concurrent.Executors.newSingleThreadExecutor();private boolean busy;private long listRevision;private int listProfile=-1;
 private volatile CaptureSession captureSession;
 private static final class CaptureSession {
  final MidiTakeRecorder recorder;final CreationJournal journal;boolean notified;int eventIndex,segmentIndex;
  CaptureSession(MidiTakeRecorder r,CreationJournal j){recorder=r;journal=j;}
  synchronized void persist()throws IOException{synchronized(recorder){if(recorder.state()==MidiTakeRecorder.State.DISCARDED)return;
   for(MidiTakeRecorder.Event e:recorder.eventsSince(eventIndex)){if(!journal.append(e))throw new IOException("Fila do diário indisponível");eventIndex++;}
   for(MidiTakeRecorder.Segment segment:recorder.segmentsSince(segmentIndex)){if(!journal.append(segment))throw new IOException("Fila do diário indisponível");segmentIndex++;}
  }}
  synchronized boolean checkpoint(long now){synchronized(recorder){return recorder.state()!=MidiTakeRecorder.State.RECORDING||journal.checkpoint(now);}}
 }
 private CreationProjectStore store;private MidiTakeRecorder take;private CreationJournal journal;private int takeProfileId;private long takeCreatedAt,takeConnectionGeneration;private String selectedId;private String page="home",detailsBack="list";private boolean restoreListPosition;private final MidiTakePlayer player;private TextView playbackStatus,connectionStatus;private EditText listSearch;private boolean ignoreSilence=true,journalsScanned;
 private CreationProjectStore.Filter filter=CreationProjectStore.Filter.ALL;private String query="";private int sortMode=0;
 private final int navy=0xff101C31,ink=0xff172A44,blue=0xff286BCB,muted=0xff57708A;
 MusicCreationScreen(MainActivity activity,Runnable goBack){super(activity);a=activity;back=goBack;setOrientation(VERTICAL);setVisibility(GONE);setBackgroundColor(0xffF7F9FC);
  player=new MidiTakePlayer(new MidiTakePlayer.Sink(){public boolean available(){return a.midiConnection.snapshot().canSend;}public boolean send(byte[] data){return a.midiConnection.send(data);}public void silence(){a.midiConnection.silence();}},(state,pos,duration)->post(()->{if(playbackStatus!=null)playbackStatus.setText((state==MidiTakePlayer.State.PLAYING?"Reproduzindo":state==MidiTakePlayer.State.PAUSED?"Pausada":"Parada")+" · "+String.format(Locale.ROOT,"%.1f / %.1f s",pos/1e9,duration/1e9));}),command->a.handler.post(command));
  bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),dp(3),dp(8),dp(3));bar.setBackgroundColor(navy);addView(bar,new LayoutParams(-1,dp(50)));
  body=new LinearLayout(a);body.setOrientation(VERTICAL);body.setPadding(dp(16),dp(12),dp(16),dp(16));scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(body);addView(scroll,new LayoutParams(-1,0,1));
  try{store=new CreationProjectStore(a.getFilesDir());}catch(IOException ex){a.message("Não foi possível abrir Minhas Criações: "+ex.getMessage());}
  showHome();
 }
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size,boolean bold){TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(ink);if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);return t;}
 private Button button(String s,boolean primary,Runnable action){Button b=new Button(a){@Override public void setEnabled(boolean enabled){super.setEnabled(enabled);setAlpha(enabled?1f:.45f);}};b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(primary?Color.WHITE:ink);b.setMinHeight(dp(48));b.setOnClickListener(v->{if(!busy)action.run();});b.setBackgroundColor(primary?blue:0xffEAF0F7);return b;}
 private void header(String title,Runnable returnAction){bar.removeAllViews();Button b=button("← Voltar",false,returnAction);bar.addView(b,new LayoutParams(dp(110),-1));TextView t=text(title,18,true);t.setTextColor(Color.WHITE);t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setContentDescription(title);t.setGravity(Gravity.CENTER_VERTICAL);bar.addView(t,new LayoutParams(0,-1,1));}
 private void base(String title,Runnable returnAction){header(title,returnAction);body.removeAllViews();}
 void enter(){setVisibility(VISIBLE);if(hasUnsavedTake()){showRecorder();return;}if(listProfile!=a.user){listProfile=a.user;query="";filter=CreationProjectStore.Filter.ALL;sortMode=0;listScrollY=0;}if(!journalsScanned){journalsScanned=true;busy=true;disk.execute(()->{recoverJournals();post(()->{busy=false;showHome();});});}showHome();}
 boolean hasUnsavedTake(){return take!=null&&(take.state()==MidiTakeRecorder.State.RECORDING||take.state()==MidiTakeRecorder.State.PAUSED||take.state()==MidiTakeRecorder.State.REVIEW||take.state()==MidiTakeRecorder.State.INTERRUPTED);}
 void requestLeave(){if(busy){a.message("Aguarde a confirmação dos dados locais.");return;}player.stop();if("details".equals(page)){if("home".equals(detailsBack))showHome();else showList();return;}if("list".equals(page)){showHome();return;}if("recording".equals(page)){if(!hasUnsavedTake()){showHome();return;}activeDialog=new AlertDialog.Builder(a).setTitle("Gravação não salva").setMessage("Salve o trecho recuperável antes de sair?")
  .setPositiveButton("Salvar trecho",(d,w)->{if(saveRecoverable("Trecho recuperável"))showHome();else a.message("O trecho continua nesta tela e no diário local; tente salvar novamente.");}).setNegativeButton("Descartar",(d,w)->confirmDiscard()).setNeutralButton("Continuar",null).show();return;}back.run();}
 void onBackground(){
  player.stop();if(busy||take==null)return;
  if(take.state()==MidiTakeRecorder.State.RECORDING){try{take.pause(android.os.SystemClock.elapsedRealtimeNanos());persistJournalDelta();confirmJournalAsync();}catch(Exception error){take.interrupt("Falha ao preservar pausa");if(saveRecoverable("Gravação interrompida"))showHome();}}
  else if(take.state()==MidiTakeRecorder.State.REVIEW||take.state()==MidiTakeRecorder.State.INTERRUPTED){if(saveRecoverable("Gravação interrompida"))showHome();}
 }
 void onMidi(int status,int a1,int a2,long timeNs,long epoch,long androidTime,int source){
  final CaptureSession session=captureSession;if(session==null)return;
  boolean accepted=session.recorder.accept(status,a1,a2,timeNs,epoch,androidTime,source);
  if(!accepted&&session.recorder.state()!=MidiTakeRecorder.State.INTERRUPTED)return;
  try{session.persist();}catch(IOException error){synchronized(session.recorder){session.recorder.interrupt(android.os.SystemClock.elapsedRealtimeNanos(),"Falha de persistência");}}
  if(session.recorder.state()==MidiTakeRecorder.State.INTERRUPTED){synchronized(session){if(session.notified)return;session.notified=true;}
   post(()->{if(captureSession==session&&take==session.recorder){if(saveRecoverable("Gravação interrompida"))showHome();else a.message("O trecho foi preservado. Tente salvar ou exportar novamente.");}});
  }
 }
 void onConnectionLost(){player.stop();if(busy)return;if(take!=null&&(take.state()==MidiTakeRecorder.State.RECORDING||take.state()==MidiTakeRecorder.State.PAUSED||take.state()==MidiTakeRecorder.State.REVIEW||take.state()==MidiTakeRecorder.State.INTERRUPTED)){if(saveRecoverable("Gravação interrompida"))showHome();}}
 private String profileName(){for(LocalProfiles.Profile p:a.profileStore().list())if(p.id==a.user)return p.name;return "Perfil "+a.user;}
 private String midiState(){MidiConnection.Snapshot s=a.midiConnection.snapshot();return "MIDI · Entrada: "+(s.canReceive?"conectada":"indisponível")+" · Saída: "+(s.canSend?"conectada":"indisponível")+(s.connected()&&s.target!=null&&!s.target.name.isEmpty()?"\n"+s.target.name:"");}
 private void showHome(){page="home";player.stop();take=null;captureSession=null;selectedId=null;base("Criação Musical",this::requestLeave);bar.removeAllViews();bar.addView(button("← Voltar",false,this::requestLeave),new LayoutParams(0,-1,1));bar.addView(button("Descobrir",false,()->a.screens.showLab()),new LayoutParams(0,-1,1));Button learn=button("Aprender",false,()->{});learn.setEnabled(false);bar.addView(learn,new LayoutParams(0,-1,1));Button creation=button("Criação",true,()->{});creation.setSelected(true);bar.addView(creation,new LayoutParams(0,-1,1));body.addView(text("Criação Musical",22,true));body.addView(text("Perfil ativo · "+profileName(),17,true));connectionStatus=text(midiState(),14,false);body.addView(connectionStatus);body.addView(button("Gerenciar USB-MIDI",false,()->a.selectMidi()),new LayoutParams(-1,dp(48)));
  Button record=button("＋  Nova gravação",true,()->showRecorder());body.addView(record,new LayoutParams(-1,dp(58)));
  Button list=button("Minhas Criações",false,()->showList());body.addView(list,new LayoutParams(-1,dp(54)));
  if(recoveryWarning!=null)body.addView(text(recoveryWarning,15,false));body.addView(text("Recentes",18,true));addProjects(CreationProjectStore.Filter.ALL,3);
  if(body.getChildCount()<=4)body.addView(text("Suas composições aparecerão aqui. Conecte seu piano para começar uma gravação.",15,false));
 }
 private void showRecorder(){final long revision=++formRevision;page="recording";base("Nova gravação",this::requestLeave);if(take==null){take=new MidiTakeRecorder();takeCreatedAt=System.currentTimeMillis();}body.addView(text("Perfil · "+profileName(),15,true));connectionStatus=text(midiState(),15,false);body.addView(connectionStatus);
  TextView state=text("Pronta para gravar · uma tomada por composição",18,true);body.addView(state);
  TextView count=text("Eventos MIDI: 0",16,false);body.addView(count);
  Button start=button("●  Gravar",true,()->{if(hasUnsavedTake()){a.message("Pare e salve ou descarte a tomada atual antes de gravar novamente.");return;}MidiConnection.Snapshot s=a.midiConnection.snapshot();if(!s.canReceive){a.message("Conecte uma entrada MIDI funcional para gravar.");return;}try{take=new MidiTakeRecorder();takeProfileId=a.user;takeCreatedAt=System.currentTimeMillis();long startNs=android.os.SystemClock.elapsedRealtimeNanos();take.start(a.beginCreationCapture(),startNs);takeConnectionGeneration=s.generation;String session=UUID.randomUUID().toString();journal=new CreationJournal(a.getFilesDir(),session,"Gravação recuperável",takeProfileId,takeCreatedAt,startNs,s.target==null?"":s.target.name.substring(0,Math.min(120,s.target.name.length())));captureSession=new CaptureSession(take,journal);state.setText("Gravando… toque no piano conectado");}catch(IOException ex){take=null;a.message("Não foi possível iniciar com recuperação segura: "+ex.getMessage());}});body.addView(start,new LayoutParams(-1,dp(54)));
  Button pause=button("Pausar",false,()->{if(player.state()==MidiTakePlayer.State.PLAYING){player.pause();return;}if(take!=null&&take.state()==MidiTakeRecorder.State.RECORDING){try{take.pause(android.os.SystemClock.elapsedRealtimeNanos());persistJournalDelta();confirmJournalAsync();state.setText("Pausada · "+take.eventCount()+" eventos recebidos");}catch(IOException ex){a.message("O diário não foi confirmado; a gravação foi interrompida com segurança.");take.interrupt("Falha ao confirmar pausa");if(saveRecoverable("Trecho recuperável"))showHome();}}});body.addView(pause,new LayoutParams(-1,dp(48)));
  Button resume=button("Continuar",false,()->{if(player.state()==MidiTakePlayer.State.PAUSED){player.resume();return;}if(take!=null&&take.state()==MidiTakeRecorder.State.PAUSED){long now=android.os.SystemClock.elapsedRealtimeNanos();if(journal==null||!journal.beginSegment(now)){a.message("Não foi possível iniciar um segmento seguro.");return;}if(a.user!=takeProfileId||!a.midiConnection.snapshot().canReceive||a.midiConnection.snapshot().generation!=takeConnectionGeneration){take.interrupt("A conexão ou o perfil mudou");state.setText("Salve o trecho antes de iniciar outra tomada");return;}take.resumeForeground(now,take.epoch(),a.beginCreationCapture());state.setText(take.state()==MidiTakeRecorder.State.RECORDING?"Gravando…":"Trecho recuperável · conexão mudou");}});body.addView(resume,new LayoutParams(-1,dp(48)));
  Button save=button("Salvar composição",true,()->saveTake());save.setEnabled(false);
  Button discard=button("Descartar…",false,()->confirmDiscard());discard.setEnabled(false);
  Button stop=button("■  Parar",false,()->{player.stop();if(take!=null&&(take.state()==MidiTakeRecorder.State.RECORDING||take.state()==MidiTakeRecorder.State.PAUSED)){take.stop(android.os.SystemClock.elapsedRealtimeNanos());try{persistJournalDelta();confirmJournalAsync();}catch(IOException ex){a.message("A revisão continua local, mas o diário não foi confirmado; salve o trecho recuperável.");}state.setText("Revisão · "+take.eventCount()+" eventos · "+(take.durationNs()/1_000_000_000.0)+" s");count.setText("Eventos MIDI: "+take.eventCount());save.setEnabled(take.eventCount()>0);discard.setEnabled(true);}});body.addView(stop,new LayoutParams(-1,dp(48)));
  body.addView(save,new LayoutParams(-1,dp(52)));body.addView(discard,new LayoutParams(-1,dp(48)));
  Button preview=button("Reproduzir gravação",false,()->{if(take==null||take.state()!=MidiTakeRecorder.State.REVIEW&&take.state()!=MidiTakeRecorder.State.INTERRUPTED)return;if(!a.midiConnection.snapshot().canSend){a.message("Conecte uma saída MIDI funcional para reproduzir.");return;}player.play(new CreationProjectStore.Project(UUID.randomUUID().toString(),"Nova composição",takeProfileId,takeCreatedAt,takeCreatedAt,take.durationNs(),false,false,take.events(),take.segments()),false);});
  playbackStatus=text("Reprodução parada",14,false);body.addView(playbackStatus);body.addView(preview,new LayoutParams(-1,dp(48)));
  Button export=button("Exportar tomada MIDI",false,()->{if(take!=null&&(take.state()==MidiTakeRecorder.State.REVIEW||take.state()==MidiTakeRecorder.State.INTERRUPTED)){String id=journal==null?UUID.randomUUID().toString():journal.sessionId();a.exportCreationTake(new CreationProjectStore.Project(id,"Nova composição",takeProfileId,takeCreatedAt,takeCreatedAt,take.durationNs(),false,false,take.events(),take.segments(),journal==null?"":journal.instrument()));}});body.addView(export,new LayoutParams(-1,dp(48)));

  Runnable ticker=new Runnable(){public void run(){if(revision!=formRevision||!a.foreground||!"recording".equals(page)||take==null)return;MidiTakeRecorder.State current=take.state();if(current==MidiTakeRecorder.State.RECORDING&&take.elapsedNs(android.os.SystemClock.elapsedRealtimeNanos())>=MidiTakeRecorder.MAX_DURATION_NS){take.stop(android.os.SystemClock.elapsedRealtimeNanos());try{persistJournalDelta();confirmJournalAsync();}catch(IOException ignored){}current=take.state();}if(current==MidiTakeRecorder.State.RECORDING&&captureSession!=null&&!captureSession.checkpoint(android.os.SystemClock.elapsedRealtimeNanos())){take.interrupt("Falha no diário; salve o trecho");current=take.state();}boolean recording=current==MidiTakeRecorder.State.RECORDING,paused=current==MidiTakeRecorder.State.PAUSED,review=current==MidiTakeRecorder.State.REVIEW||current==MidiTakeRecorder.State.INTERRUPTED;
   start.setEnabled(current==MidiTakeRecorder.State.READY);pause.setEnabled(recording||player.state()==MidiTakePlayer.State.PLAYING);resume.setEnabled(paused||player.state()==MidiTakePlayer.State.PAUSED);stop.setEnabled(recording||paused||player.state()!=MidiTakePlayer.State.STOPPED);save.setEnabled(review&&take.eventCount()>0);discard.setEnabled(current!=MidiTakeRecorder.State.READY);preview.setEnabled(review);export.setEnabled(review);if(current==MidiTakeRecorder.State.INTERRUPTED){String label="Trecho interrompido · "+take.interruption();if(!state.getText().toString().equals(label))state.setText(label);}String counter=String.format(Locale.getDefault(),"%d s · %d notas",take.elapsedNs(android.os.SystemClock.elapsedRealtimeNanos())/1_000_000_000L,take.noteCount());if(!count.getText().toString().equals(counter))count.setText(counter);a.handler.postDelayed(this,100);}};ticker.run();

 }
 private void saveTake(){
  if(take==null||(take.state()!=MidiTakeRecorder.State.REVIEW&&take.state()!=MidiTakeRecorder.State.INTERRUPTED)){a.message("Pare a gravação antes de salvar.");return;}
  EditText title=new EditText(a);title.setSingleLine(true);title.setText("Nova composição");
  activeDialog=new AlertDialog.Builder(a).setTitle("Salvar composição").setView(title).setPositiveButton("Salvar",(d,w)->{
   final MidiTakeRecorder source=take;final CreationJournal log=journal;final String name=title.getText().toString().trim();final int owner=takeProfileId;final long created=takeCreatedAt;
   try{persistJournalDelta();}catch(IOException ignored){}
   final List<MidiTakeRecorder.Event> events=source.events();final List<MidiTakeRecorder.Segment> segments=source.segments();final long duration=source.durationNs();
   busy=true;disk.execute(()->{try{
    if(log!=null)try{log.flush();}catch(IOException ignored){}
    CreationProjectStore.Project project=log==null?store.save(name,owner,created,duration,events,segments):store.save(log.sessionId(),name,owner,created,duration,events,segments,log.instrument());
    if(log!=null)try{log.deleteAfterCommit();}catch(IOException cleanup){log.close();}
    post(()->{busy=false;source.markSaved();if(take==source){take=null;journal=null;}showDetails(project.id);});
   }catch(Exception error){post(()->{busy=false;a.message("A composição não foi salva. O trecho e o diário foram preservados: "+error.getMessage());});}});
  }).setNegativeButton("Cancelar",null).show();
 }
 private boolean saveRecoverable(String title){
  if(take==null)return true;if(busy)return false;
  take.interrupt(android.os.SystemClock.elapsedRealtimeNanos(),"Interrupção do app/conexão");
  try{persistJournalDelta();}catch(IOException ignored){}
  final MidiTakeRecorder source=take;final CreationJournal log=journal;final int owner=takeProfileId;final long created=takeCreatedAt,duration=source.durationNs();
  final List<MidiTakeRecorder.Event> events=source.events();final List<MidiTakeRecorder.Segment> segments=source.segments();
  busy=true;disk.execute(()->{try{
   if(log!=null)try{log.flush();}catch(IOException ignored){}
   if(!events.isEmpty()){if(store==null)throw new IOException("Biblioteca indisponível");if(log!=null)store.saveRecoverable(log.sessionId(),title,owner,created,duration,events,segments,log.instrument());else store.saveRecoverable(title,owner,created,duration,events,segments);}
   if(log!=null)try{log.deleteAfterCommit();}catch(IOException cleanup){log.close();}
   post(()->{busy=false;if(take==source){take=null;journal=null;}if("home".equals(page))showHome();});
  }catch(Exception error){post(()->{busy=false;take=source;journal=log;showRecorder();a.message("O trecho continua na memória e no diário. Exporte ou tente salvar novamente: "+error.getMessage());});}});
  return true;
 }
 private void persistJournalDelta()throws IOException{CaptureSession session=captureSession;if(session!=null)session.persist();}
 private void recoverJournals(){if(store==null)return;File dir=new File(a.getFilesDir(),"creation-journals");File[] pending=dir.listFiles((d,n)->n.endsWith(".journal"));if(pending==null)return;for(File f:pending){if(CreationJournal.isActive(f)){if(recoveryRetries++<20)postDelayed(()->{if(!disk.isShutdown())disk.execute(()->{recoverJournals();post(()->{if(!busy&&"home".equals(page))showHome();});});},200);continue;}try{CreationJournal.Recovery r=CreationJournal.read(f);if(r.events.isEmpty()){if(!f.delete())android.util.Log.w("PianoLivreCreation","Diário vazio preservado");continue;}store.saveRecoverable(r.sessionId,r.title,r.profileId,r.createdAt,segmentDuration(r.segments),r.events,r.segments,r.instrument);if(!f.delete())android.util.Log.w("PianoLivreCreation","Journal promovido; arquivo duplicado será conferido novamente");if(r.truncated)recoveryWarning="Foi recuperado apenas o prefixo íntegro de uma gravação interrompida. Revise o trecho antes de salvar.";if(r.truncated)android.util.Log.w("PianoLivreCreation","Recuperado prefixo íntegro de journal truncado");}catch(Exception ex){recoveryWarning="Há uma gravação interrompida que não pôde ser validada. Seus arquivos foram preservados.";android.util.Log.e("PianoLivreCreation","Journal preservado para diagnóstico; não foi promovido",ex);}}}
 void prepareBackup()throws IOException{if(busy)throw new IOException("Aguarde a confirmação dos projetos antes do backup");if(take!=null&&(take.state()==MidiTakeRecorder.State.RECORDING||take.state()==MidiTakeRecorder.State.PAUSED||take.state()==MidiTakeRecorder.State.REVIEW))throw new IOException("Salve ou interrompa a gravação antes do backup");recoverJournals();File dir=new File(a.getFilesDir(),"creation-journals");File[] remaining=dir.listFiles((d,n)->n.endsWith(".journal"));if(remaining!=null&&remaining.length>0)throw new IOException("Há trecho recuperável ainda não validado; o backup não foi iniciado");}
 private static long segmentDuration(List<MidiTakeRecorder.Segment> segments){long total=0;for(MidiTakeRecorder.Segment s:segments)total+=s.endNs-s.startNs;return total;}
 boolean debugIdle(){return !busy;}
 String debugPage(){return page;}
 String debugSelected(){return selectedId;}
 MidiTakeRecorder debugTake(){if((a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)==0)throw new IllegalStateException();return take;}
 void onForeground(){if(!busy&&hasUnsavedTake())showRecorder();refreshConnection();}
 void refreshConnection(){if(connectionStatus!=null)connectionStatus.setText(midiState());}
 private void confirmJournalAsync(){final CreationJournal log=journal;if(log==null)return;disk.execute(()->{try{log.flush();}catch(IOException error){post(()->{if(journal==log)a.message("Não foi possível confirmar o diário. Salve ou exporte o trecho antes de sair.");});}});}
 private void confirmDiscard(){if(take==null)return;activeDialog=new AlertDialog.Builder(a).setTitle("Descartar gravação?").setMessage("Os eventos desta tomada serão removidos.").setPositiveButton("Descartar",(d,w)->{
  player.stop();final MidiTakeRecorder source=take;final CreationJournal log=journal;source.interrupt(android.os.SystemClock.elapsedRealtimeNanos(),"Descarte confirmado");busy=true;
  disk.execute(()->{try{if(log!=null)log.discardAndDelete();source.discard();post(()->{busy=false;take=null;journal=null;showHome();});}catch(IOException error){post(()->{busy=false;a.message("Não foi possível concluir o descarte; o trecho foi mantido: "+error.getMessage());});}});
 }).setNegativeButton("Continuar",null).show();}
 private void showList(){player.stop();restoreListPosition=true;page="list";base("Minhas Criações",this::showHome);EditText search=new EditText(a);listSearch=search;search.setSingleLine(true);search.setHint("Buscar composição…");search.setText(query);body.addView(search,new LayoutParams(-1,dp(48)));Spinner filters=new Spinner(a);String[] names={"Todas","Salvas","Recuperáveis","Lixeira"};filters.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));filters.setSelection(filter.ordinal());body.addView(filters,new LayoutParams(-1,dp(48)));Spinner sorting=new Spinner(a);String[] sortNames={"Mais recentes","Mais antigos","Nome A–Z","Nome Z–A","Maior duração","Menor duração"};sorting.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,sortNames));sorting.setSelection(sortMode);body.addView(sorting,new LayoutParams(-1,dp(48)));LinearLayout results=new LinearLayout(a);results.setOrientation(VERTICAL);body.addView(results,new LayoutParams(-1,-2));Runnable refresh=()->{query=search.getText().toString();filter=CreationProjectStore.Filter.values()[filters.getSelectedItemPosition()];sortMode=sorting.getSelectedItemPosition();results.removeAllViews();populate(results,filter,query);};search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int af){}public void onTextChanged(CharSequence s,int st,int before,int count){restoreListPosition=false;refresh.run();}public void afterTextChanged(android.text.Editable e){}});android.widget.AdapterView.OnItemSelectedListener changed=new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}};filters.setOnItemSelectedListener(changed);sorting.setOnItemSelectedListener(changed);refresh.run();scroll.post(()->scroll.scrollTo(0,listScrollY));}
 private void addProjects(CreationProjectStore.Filter f,int limit){
  if(store==null)return;final int owner=a.user;final long revision=++homeRevision;
  disk.execute(()->{try{List<CreationProjectStore.Project> projects=store.list(owner,f,"",null);post(()->{if(revision!=homeRevision||!"home".equals(page)||a.user!=owner)return;if(projects.isEmpty())body.addView(text("Suas composições aparecerão aqui. Conecte seu piano para começar uma gravação.",15,false));for(int i=0;i<Math.min(limit,projects.size());i++)addProjectRow(body,projects.get(i));});}catch(IOException error){post(()->a.message("Não foi possível ler os projetos: "+error.getMessage()));}});
 }
 private void populate(LinearLayout target,CreationProjectStore.Filter f,String q){
  final int owner=a.user,sort=sortMode;final long revision=++listRevision;target.addView(text("Carregando composições…",15,false));
  disk.execute(()->{try{
   Comparator<CreationProjectStore.Project> cmp;switch(sort){case 1:cmp=Comparator.comparingLong((CreationProjectStore.Project p)->p.editedAt);break;case 2:cmp=Comparator.comparing(p->p.title.toLowerCase(Locale.ROOT));break;case 3:cmp=Comparator.comparing((CreationProjectStore.Project p)->p.title.toLowerCase(Locale.ROOT)).reversed();break;case 4:cmp=Comparator.comparingLong((CreationProjectStore.Project p)->p.durationNs).reversed();break;case 5:cmp=Comparator.comparingLong((CreationProjectStore.Project p)->p.durationNs);break;default:cmp=null;}
   if(cmp!=null)cmp=cmp.thenComparing(p->p.id);final List<CreationProjectStore.Project> projects=store.list(owner,f,q,cmp);
   post(()->{if(revision!=listRevision||!"list".equals(page)||a.user!=owner)return;target.removeAllViews();if(projects.isEmpty()){target.addView(text("Nenhuma composição encontrada.",15,false));if(!q.isEmpty())target.addView(button("Limpar busca",false,()->{if(listSearch!=null)listSearch.setText("");}));}for(CreationProjectStore.Project p:projects)addProjectRow(target,p);if(restoreListPosition){restoreListPosition=false;scroll.post(()->scroll.scrollTo(0,listScrollY));}});
  }catch(IOException error){post(()->a.message("Não foi possível ler Minhas Criações: "+error.getMessage()));}});
 }
 private void addProjectRow(LinearLayout target,CreationProjectStore.Project p){Button row=button(p.title+"\n"+new Date(p.createdAt)+" · "+(p.durationNs/1_000_000_000.0)+" s"+(p.recoverable?" · Recuperável":p.trash?" · Lixeira":""),false,()->showDetails(p.id));row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.setMinHeight(dp(68));target.addView(row,new LayoutParams(-1,-2));}
 private void showDetails(String id){if("list".equals(page))listScrollY=scroll.getScrollY();detailsBack="home".equals(page)?"home":"list";page="details";selectedId=id;base("Abrindo composição…",this::showList);disk.execute(()->{try{CreationProjectStore.Project project=store.get(id);post(()->{if("details".equals(page)&&id.equals(selectedId))renderDetails(project);});}catch(IOException error){post(()->{a.message("Não foi possível abrir a composição: "+error.getMessage());showList();});}});}
 private void renderDetails(CreationProjectStore.Project p){String id=p.id;try{base(p.title,this::requestLeave);body.addView(text(p.title,22,true));if(!p.instrument.isEmpty())body.addView(text("Instrumento observado · "+p.instrument,15,false));body.addView(text("Criada · "+new Date(p.createdAt)+"\nÚltima alteração · "+new Date(p.editedAt)+"\nDuração · "+(p.durationNs/1_000_000_000.0)+" s\nNotas · "+p.events.stream().filter(e->!e.generated&&e.command()==0x90&&e.data2>0).count()+"\nEventos · "+p.events.size()+"\nPerfil · "+profileName(),15,false));
  body.addView(button("Renomear",false,()->rename(p)),new LayoutParams(-1,dp(48)));
  if(p.recoverable&&!p.trash)body.addView(button("Revisar e salvar trecho recuperado",true,()->mutate(()->store.finalizeRecovery(id,System.currentTimeMillis()))),new LayoutParams(-1,dp(52)));
  if(!p.trash)body.addView(button("Exportar MIDI (.mid)",false,()->a.exportCreationMidi(id,p.title)),new LayoutParams(-1,dp(48)));
  if(!p.trash){CheckBox trim=new CheckBox(a);trim.setText("Ignorar silêncio inicial e final na reprodução");trim.setChecked(ignoreSilence);trim.setOnCheckedChangeListener((v,checked)->ignoreSilence=checked);body.addView(trim,new LayoutParams(-1,-2));playbackStatus=text("Parado",14,false);body.addView(playbackStatus,new LayoutParams(-1,dp(40)));
   Button play=button("▶ Reproduzir no piano",true,()->{if(!a.midiConnection.snapshot().canSend){a.message("Conecte uma saída MIDI funcional para reproduzir no instrumento.");return;}player.play(p,ignoreSilence);});body.addView(play,new LayoutParams(-1,dp(50)));
   body.addView(button("Pausar",false,()->player.pause()),new LayoutParams(-1,dp(48)));body.addView(button("Continuar",false,()->player.resume()),new LayoutParams(-1,dp(48)));body.addView(button("Parar",false,()->player.stop()),new LayoutParams(-1,dp(48)));}
  if(p.trash){body.addView(button("Restaurar",false,()->mutate(()->store.restore(id,System.currentTimeMillis()))),new LayoutParams(-1,dp(48)));body.addView(button("Excluir definitivamente…",false,()->activeDialog=new AlertDialog.Builder(a).setTitle("Excluir definitivamente?").setMessage("Excluir definitivamente "+p.title+"? Esta ação não pode ser desfeita.").setPositiveButton("Excluir",(d,w)->mutate(()->store.deletePermanently(id))).setNegativeButton("Cancelar",null).show()),new LayoutParams(-1,dp(48)));}
  else body.addView(button("Mover para Lixeira",false,()->mutate(()->store.moveToTrash(id,System.currentTimeMillis()))),new LayoutParams(-1,dp(48)));
 }catch(Exception ex){a.message("Não foi possível abrir a composição: "+ex.getMessage());showList();}}
 private interface Mutation{void run()throws Exception;}private void mutate(Mutation m){player.stop();busy=true;disk.execute(()->{try{m.run();post(()->{busy=false;showList();});}catch(Exception ex){post(()->{busy=false;a.message("A alteração não foi aplicada: "+ex.getMessage());});}});}
 private void rename(CreationProjectStore.Project p){EditText input=new EditText(a);input.setSingleLine(true);input.setText(p.title);activeDialog=new AlertDialog.Builder(a).setTitle("Renomear composição").setView(input).setPositiveButton("Salvar",(d,w)->mutate(()->store.rename(p.id,input.getText().toString().trim(),System.currentTimeMillis()))).setNegativeButton("Cancelar",null).show();}
 void close(){onBackground();player.close();disk.shutdown();}
}

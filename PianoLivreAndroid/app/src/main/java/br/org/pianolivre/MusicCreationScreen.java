package br.org.pianolivre;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
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
 private final int navy=0xff101C31,ink=0xff172A44,blue=0xff286BCB,muted=0xff57708A,light=0xffF0F5FB,white=Color.WHITE;
 private TextView recordingClock,recordingNotes,recordingEvents;private Button detailPlay,detailPause,detailResume,detailStop;
 private LinearLayout dashboardLeft,dashboardRight,recordingReviewPanel;
 private String rightContext="recent";private int catalogFilter=0;
 MusicCreationScreen(MainActivity activity,Runnable goBack){super(activity);a=activity;back=goBack;setOrientation(VERTICAL);setVisibility(GONE);setBackgroundColor(0xffF7F9FC);
  player=new MidiTakePlayer(new MidiTakePlayer.Sink(){public boolean available(){return a.midiConnection.snapshot().canSend;}public boolean send(byte[] data){return a.midiConnection.send(data);}public void silence(){a.midiConnection.silence();}},(state,pos,duration)->post(()->{if(playbackStatus!=null)playbackStatus.setText((state==MidiTakePlayer.State.PLAYING?"Reproduzindo":state==MidiTakePlayer.State.PAUSED?"Pausada":"Parada")+" · "+String.format(Locale.ROOT,"%.1f / %.1f s",pos/1e9,duration/1e9));if("details".equals(page))updatePlaybackControls();}),command->a.handler.post(command));
  bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),dp(3),dp(8),dp(3));bar.setBackgroundColor(navy);addView(bar,new LayoutParams(-1,dp(56)));
  body=new LinearLayout(a);body.setOrientation(VERTICAL);body.setPadding(dp(16),dp(12),dp(16),dp(16));scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(body);addView(scroll,new LayoutParams(-1,0,1));
  try{store=new CreationProjectStore(a.getFilesDir());}catch(IOException ex){a.message("Não foi possível abrir Minhas Criações: "+ex.getMessage());}
  showHome();
 }
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size,boolean bold){
  TextView t=new TextView(a);t.setText(s);t.setTextSize(size);t.setTextColor(ink);
  t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);
  return t;
 }
 private GradientDrawable shape(int color,int radius,int stroke){
  GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
  if(stroke!=0)d.setStroke(dp(1),stroke);return d;
 }
 private LinearLayout column(){LinearLayout x=new LinearLayout(a);x.setOrientation(VERTICAL);return x;}
 private LinearLayout horizontal(){LinearLayout x=new LinearLayout(a);x.setOrientation(HORIZONTAL);x.setGravity(Gravity.CENTER_VERTICAL);return x;}
 private LinearLayout card(){LinearLayout x=column();x.setPadding(dp(14),dp(13),dp(14),dp(13));x.setBackground(shape(white,16,0xffE0E9F4));return x;}
 private void space(LinearLayout target,int height){View v=new View(a);target.addView(v,new LayoutParams(dp(1),dp(height)));}
 private void attach(LinearLayout parent,View view,int height,int margin){
  LayoutParams p=new LayoutParams(-1,height<0?height:dp(height));p.bottomMargin=dp(margin);parent.addView(view,p);
 }
 private TextView mutedText(String s,int size){TextView t=text(s,size,false);t.setTextColor(muted);return t;}
 private void label(LinearLayout target,String s,int size,boolean bold,int bottom){attach(target,text(s,size,bold),-2,bottom);}
 private Button button(String s,boolean primary,Runnable action){
  Button b=new Button(a){@Override public void setEnabled(boolean enabled){super.setEnabled(enabled);setAlpha(enabled?1f:.45f);}};
  b.setText(s);b.setContentDescription(s);b.setAllCaps(false);b.setTextSize(14);b.setSingleLine(true);b.setEllipsize(TextUtils.TruncateAt.END);
  b.setTypeface(null,android.graphics.Typeface.BOLD);b.setTextColor(primary?white:ink);
  b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setPadding(dp(9),0,dp(9),0);
  b.setOnClickListener(v->{if(!busy)action.run();});
  b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22286BCB),shape(primary?blue:light,12,primary?0:0xffDCE7F2),null));
  return b;
 }
 private LinearLayout actionRow(LinearLayout target,Button aButton,Button bButton,int bottom){
  LinearLayout row=horizontal();LayoutParams lp=new LayoutParams(-1,dp(50));lp.bottomMargin=dp(bottom);target.addView(row,lp);
  row.addView(aButton,new LayoutParams(0,-1,1));View gap=new View(a);row.addView(gap,new LayoutParams(dp(8),dp(1)));
  row.addView(bButton,new LayoutParams(0,-1,1));return row;
 }

 // Use measured container width, not whole-screen width, for nested columns.
 private LinearLayout responsiveActions(LinearLayout parent,int bottom,Button... buttons){
  LinearLayout group=column();group.setTag("creation-responsive-actions");attach(parent,group,-2,bottom);
  final int[] prev={-1};
  java.util.function.IntConsumer reflow=width->{
   int gap=dp(8);float scale=getResources().getConfiguration().fontScale;
   int cell=dp(Math.max(142,Math.round(142*scale)));
   int columns=width<=0?1:Math.max(1,Math.min(buttons.length,(width+gap)/(cell+gap)));
   if(columns==prev[0])return;
   // Buttons belong to existing child rows; detach before reparenting them.
   for(Button button:buttons)if(button.getParent() instanceof android.view.ViewGroup)
    ((android.view.ViewGroup)button.getParent()).removeView(button);
   prev[0]=columns;group.removeAllViews();
   for(int start=0;start<buttons.length;start+=columns){
    LinearLayout row=horizontal();LayoutParams rp=new LayoutParams(-1,dp(56));
    rp.bottomMargin=dp(start+columns<buttons.length?6:0);group.addView(row,rp);
    int end=Math.min(buttons.length,start+columns);
    for(int i=start;i<end;i++){
     Button button=buttons[i];button.setSingleLine(false);button.setMaxLines(2);
     LayoutParams cp=new LayoutParams(0,-1,1);cp.rightMargin=dp(i<end-1?8:0);
     row.addView(button,cp);
    }
   }
  };
  // Buttons must exist before the first layout, especially during synthetic
  // and real MIDI recording state changes.
  reflow.accept(0);
  group.addOnLayoutChangeListener((view,l,t,r,b,ol,ot,or,ob)->{
   if(r>l)group.post(()->{if(group.getWidth()>0)reflow.accept(group.getWidth());});
  });
  return group;
 }
 private void header(String title,Runnable returnAction){
  bar.removeAllViews();bar.setBackgroundColor(navy);
  Button b=button("← Voltar",false,returnAction);b.setTextColor(white);
  b.setBackground(shape(0xff243955,11,0));b.setContentDescription("Voltar");
  LayoutParams backLp=new LayoutParams(dp(110),dp(44));backLp.rightMargin=dp(12);bar.addView(b,backLp);
  TextView t=text(title,18,true);t.setTextColor(white);t.setSingleLine(true);
  t.setEllipsize(TextUtils.TruncateAt.END);t.setContentDescription(title);t.setGravity(Gravity.CENTER_VERTICAL);
  bar.addView(t,new LayoutParams(0,-1,1));
 }
 private void base(String title,Runnable returnAction){header(title,returnAction);body.removeAllViews();detailPlay=detailPause=detailResume=detailStop=null;playbackStatus=null;}
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
 private String midiState(){
  MidiConnection.Snapshot s=a.midiConnection.snapshot();
  return "Entrada: "+(s.canReceive?"conectada":"indisponível")+"  ·  Saída: "+(s.canSend?"conectada":"indisponível")
   +(s.connected()&&s.target!=null&&!s.target.name.isEmpty()?"\n"+s.target.name:"");
 }
 private LinearLayout connectionCard(){
  LinearLayout connection=card();
  label(connection,"USB-MIDI",15,true,4);
  connectionStatus=mutedText(midiState(),13);attach(connection,connectionStatus,-2,7);
  attach(connection,button("Gerenciar USB-MIDI",false,()->a.selectMidi()),48,0);
  return connection;
 }
 private String duration(long nanos){long seconds=Math.max(0,nanos/1_000_000_000L);return String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60);}
 private boolean wide(){return getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density>=850;}
 private void setShown(View view,boolean shown){int target=shown?View.VISIBLE:View.GONE;if(view.getVisibility()!=target)view.setVisibility(target);}

 /** ZAI-010: single persistent landscape surface with contextual right column. */
 private void dashboard(){
  base("Criação Musical",this::requestLeave);
  MusicLabNavigationBar.populate(bar,MusicLabNavigationBar.CREATION,
    this::requestLeave,()->a.screens.showDiscover(),()->{});
  body.setPadding(dp(15),dp(10),dp(15),dp(14));
  LinearLayout heading=horizontal();heading.setTag("creation-dashboard-header");
  LinearLayout intro=column();
  TextView title=text("Criação Musical",24,true);
  attach(intro,title,-2,2);attach(intro,mutedText("Perfil ativo · "+profileName(),12),-2,0);
  heading.addView(intro,new LayoutParams(0,-2,3));
  connectionStatus=mutedText(compactMidiState(),12);
  connectionStatus.setSingleLine(true);
  connectionStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
  connectionStatus.setContentDescription("Status da conexão USB-MIDI");
  heading.addView(connectionStatus,new LayoutParams(0,-2,2));
  Button info=button("ⓘ  Informações USB-MIDI",false,()->a.showMidiDiagnostics());
  info.setTextSize(12);info.setTag("creation-usb-information");
  LayoutParams ilp=new LayoutParams(dp(192),dp(44));ilp.leftMargin=dp(8);
  heading.addView(info,ilp);
  attach(body,heading,-2,15);

  LinearLayout columns=horizontal();columns.setGravity(Gravity.TOP);
  columns.setTag("creation-dashboard-two-columns");
  attach(body,columns,-2,0);
  dashboardLeft=column();dashboardLeft.setTag("creation-dashboard-actions");
  dashboardRight=card();dashboardRight.setTag("creation-dashboard-context");
  dashboardRight.setMinimumHeight(dp(255));
  LayoutParams lp=new LayoutParams(0,-2,2);lp.rightMargin=dp(15);
  columns.addView(dashboardLeft,lp);
  columns.addView(dashboardRight,new LayoutParams(0,-2,3));
 }
 private String compactMidiState(){
  MidiConnection.Snapshot s=a.midiConnection.snapshot();
  return "USB-MIDI: "+(s.canReceive&&s.canSend?"conectado":s.canReceive?"entrada conectada":s.canSend?"saída conectada":"indisponível");
 }
 /** Leave the recording/revision state reachable; never destroy unsaved takes. */
 private void leftMenu(){
  dashboardLeft.removeAllViews();
  Button record=button(hasUnsavedTake()?"◉  Retomar gravação / revisão":"＋  Nova gravação",true,this::showRecorder);
  record.setTag("creation-new-recording");
  record.setTextSize(20);record.setMinHeight(dp(152));record.setMinimumHeight(dp(152));
  record.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22ffffff),
    shape(0xff0966D5,18,0),null));
  attach(dashboardLeft,record,152,12);
  Button list=button("▱  Minhas criações",false,this::requestList);
  list.setTag("creation-my-projects");list.setTextSize(18);
  attach(dashboardLeft,list,64,10);
  if(recoveryWarning!=null){
   TextView warning=mutedText(recoveryWarning,13);warning.setTextColor(0xff986425);
   attach(dashboardLeft,warning,-2,10);
  }
 }
 private void showRightRecent(){
  if(dashboardRight==null)return;
  rightContext="recent";dashboardRight.removeAllViews();
  LinearLayout head=horizontal();
  TextView title=text("Gravações recentes",18,true);
  head.addView(title,new LayoutParams(0,dp(43),1));
  Button all=button("Ver todas  ›",false,this::requestList);
  all.setTag("creation-view-all");all.setTextSize(12);
  head.addView(all,new LayoutParams(dp(122),dp(43)));
  attach(dashboardRight,head,43,10);
  addProjects(dashboardRight,CreationProjectStore.Filter.ALL,3);
 }
 private void requestList(){
  if(busy){a.message("Aguarde a confirmação dos dados locais.");return;}
  if(take!=null&&(take.state()==MidiTakeRecorder.State.RECORDING
      ||take.state()==MidiTakeRecorder.State.PAUSED)){
   a.message("Pause e finalize a gravação antes de abrir a biblioteca.");return;
  }
  if(hasUnsavedTake()){
   activeDialog=new AlertDialog.Builder(a).setTitle("Gravação não salva")
    .setMessage("Você pode consultar Minhas criações sem descartar a tomada atual. Para retomá-la, use o botão da coluna esquerda.")
    .setPositiveButton("Abrir Minhas criações",(d,w)->showList())
    .setNegativeButton("Continuar revisão",null).show();
   return;
  }
  showList();
 }
 private void showHome(){
  if(hasUnsavedTake()){showRecorder();return;}
  page="home";player.stop();take=null;captureSession=null;selectedId=null;
  dashboard();leftMenu();showRightRecent();
 }
 private String captureLabel(MidiTakeRecorder.State current){switch(current){case RECORDING:return "Gravando… toque no piano conectado";case PAUSED:return "Pausada · "+take.eventCount()+" eventos recebidos";case REVIEW:return take.noteCount()==0?"Nenhuma nota gravada · descarte a tomada ou preserve os controles MIDI recebidos":"Revisão · "+take.noteCount()+" notas · "+take.eventCount()+" eventos";case INTERRUPTED:return "Trecho interrompido · "+take.interruption();case SAVED:return "Composição salva";case DISCARDED:return "Tomada descartada";default:return "Nova composição · pronta para gravar";}}
 private void showRecorder(){final long revision=++formRevision;page="recording";if(take==null){take=new MidiTakeRecorder();takeCreatedAt=System.currentTimeMillis();}
  dashboard();showRightRecent();attach(dashboardLeft,mutedText("Gravação MIDI · "+profileName(),13),-2,8);
  LinearLayout recordingCard=card();recordingCard.setGravity(Gravity.CENTER);
  recordingCard.setBackground(shape(0xff126BD9,18,0));
  recordingCard.setTag("creation-live-recording-card");
  TextView state=text(take==null?"Pronta para gravar":captureLabel(take.state()),17,true);
  state.setTextColor(white);state.setGravity(Gravity.CENTER);attach(recordingCard,state,-2,8);
  recordingClock=text("00:00",34,true);recordingClock.setTextColor(white);recordingClock.setGravity(Gravity.CENTER);attach(recordingCard,recordingClock,-2,8);
  LinearLayout stats=horizontal();stats.setGravity(Gravity.CENTER);
  recordingNotes=mutedText("0 notas",14);recordingNotes.setTextColor(white);stats.addView(recordingNotes);
  TextView count=mutedText("Eventos MIDI: 0",14);count.setTextColor(white);stats.addView(count);
  recordingEvents=count;recordingNotes.setPadding(0,0,dp(18),0);
  recordingCard.addView(stats);attach(dashboardLeft,recordingCard,-2,12);
  Button start=button("●  Gravar",true,()->{if(hasUnsavedTake()){a.message("Pare e salve ou descarte a tomada atual antes de gravar novamente.");return;}MidiConnection.Snapshot s=a.midiConnection.snapshot();if(!s.canReceive){a.message("Conecte uma entrada MIDI funcional para gravar.");return;}try{take=new MidiTakeRecorder();takeProfileId=a.user;takeCreatedAt=System.currentTimeMillis();long startNs=android.os.SystemClock.elapsedRealtimeNanos();take.start(a.beginCreationCapture(),startNs);takeConnectionGeneration=s.generation;String session=UUID.randomUUID().toString();journal=new CreationJournal(a.getFilesDir(),session,"Gravação recuperável",takeProfileId,takeCreatedAt,startNs,s.target==null?"":s.target.name.substring(0,Math.min(120,s.target.name.length())));captureSession=new CaptureSession(take,journal);state.setText("Gravando… toque no piano conectado");}catch(IOException ex){take=null;a.message("Não foi possível iniciar com recuperação segura: "+ex.getMessage());}});
  Button pause=button("Pausar",false,()->{if(player.state()==MidiTakePlayer.State.PLAYING){player.pause();return;}if(take!=null&&take.state()==MidiTakeRecorder.State.RECORDING){try{take.pause(android.os.SystemClock.elapsedRealtimeNanos());persistJournalDelta();confirmJournalAsync();state.setText("Pausada · "+take.eventCount()+" eventos recebidos");}catch(IOException ex){a.message("O diário não foi confirmado; a gravação foi interrompida com segurança.");take.interrupt("Falha ao confirmar pausa");if(saveRecoverable("Trecho recuperável"))showHome();}}});
  Button resume=button("Continuar",false,()->{if(player.state()==MidiTakePlayer.State.PAUSED){player.resume();return;}if(take!=null&&take.state()==MidiTakeRecorder.State.PAUSED){long now=android.os.SystemClock.elapsedRealtimeNanos();if(journal==null||!journal.beginSegment(now)){a.message("Não foi possível iniciar um segmento seguro.");return;}if(a.user!=takeProfileId||!a.midiConnection.snapshot().canReceive||a.midiConnection.snapshot().generation!=takeConnectionGeneration){take.interrupt("A conexão ou o perfil mudou");state.setText("Salve o trecho antes de iniciar outra tomada");return;}take.resumeForeground(now,take.epoch(),a.beginCreationCapture());state.setText(take.state()==MidiTakeRecorder.State.RECORDING?"Gravando…":"Trecho recuperável · conexão mudou");}});
  Button save=button("Salvar composição",true,()->saveTake());save.setEnabled(false);
  Button discard=button("Descartar…",false,()->confirmDiscard());discard.setEnabled(false);
  Button stop=button("■  Parar",false,()->{player.stop();if(take!=null&&(take.state()==MidiTakeRecorder.State.RECORDING||take.state()==MidiTakeRecorder.State.PAUSED)){take.stop(android.os.SystemClock.elapsedRealtimeNanos());try{persistJournalDelta();confirmJournalAsync();}catch(IOException ex){a.message("A revisão continua local, mas o diário não foi confirmado; salve o trecho recuperável.");}state.setText(captureLabel(take.state()));count.setText("Eventos MIDI: "+take.eventCount());save.setEnabled(take.eventCount()>0);discard.setEnabled(true);}});
  
  Button preview=button("Reproduzir gravação",false,()->{if(take==null||take.state()!=MidiTakeRecorder.State.REVIEW&&take.state()!=MidiTakeRecorder.State.INTERRUPTED)return;if(!a.midiConnection.snapshot().canSend){a.message("Conecte uma saída MIDI funcional para reproduzir.");return;}player.play(new CreationProjectStore.Project(UUID.randomUUID().toString(),"Nova composição",takeProfileId,takeCreatedAt,takeCreatedAt,take.durationNs(),false,false,take.events(),take.segments()),false);});
  playbackStatus=mutedText("Reprodução parada",13);
  Button export=button("Exportar tomada MIDI",false,()->{if(take!=null&&(take.state()==MidiTakeRecorder.State.REVIEW||take.state()==MidiTakeRecorder.State.INTERRUPTED)){String id=journal==null?UUID.randomUUID().toString():journal.sessionId();a.exportCreationTake(new CreationProjectStore.Project(id,"Nova composição",takeProfileId,takeCreatedAt,takeCreatedAt,take.durationNs(),false,false,take.events(),take.segments(),journal==null?"":journal.instrument()));}});LinearLayout captureActions=horizontal();captureActions.setTag("creation-capture-actions");
  for(Button control:new Button[]{start,pause,resume,stop}){
   LayoutParams lp=new LayoutParams(0,dp(52),1);lp.rightMargin=dp(5);captureActions.addView(control,lp);
  }
  attach(dashboardLeft,captureActions,52,9);
  recordingReviewPanel=column();recordingReviewPanel.setTag("creation-review-actions");
  LinearLayout revisionActions=responsiveActions(recordingReviewPanel,9,save,preview,export);
  attach(recordingReviewPanel,playbackStatus,-2,8);
  discard.setTextColor(0xff9D4444);attach(recordingReviewPanel,discard,48,7);
  Button reviewShortcut=button("Revisar gravação  →",false,
    ()->showRightReview(recordingReviewPanel));
  reviewShortcut.setTag("creation-return-to-review");
  attach(dashboardLeft,reviewShortcut,46,10);
  Button all=button("▱  Minhas criações",false,this::requestList);
  all.setTag("creation-my-projects");
  attach(dashboardLeft,all,52,8);
  final boolean[] reviewActivated={false};


  Runnable ticker=new Runnable(){public void run(){if(revision!=formRevision||!a.foreground||!"recording".equals(page)||take==null)return;MidiTakeRecorder.State current=take.state();if(current==MidiTakeRecorder.State.RECORDING&&take.elapsedNs(android.os.SystemClock.elapsedRealtimeNanos())>=MidiTakeRecorder.MAX_DURATION_NS){take.stop(android.os.SystemClock.elapsedRealtimeNanos());try{persistJournalDelta();confirmJournalAsync();}catch(IOException ignored){}current=take.state();}if(current==MidiTakeRecorder.State.RECORDING&&captureSession!=null&&!captureSession.checkpoint(android.os.SystemClock.elapsedRealtimeNanos())){take.interrupt("Falha no diário; salve o trecho");current=take.state();}boolean recording=current==MidiTakeRecorder.State.RECORDING,paused=current==MidiTakeRecorder.State.PAUSED,review=current==MidiTakeRecorder.State.REVIEW||current==MidiTakeRecorder.State.INTERRUPTED;
   start.setEnabled(current==MidiTakeRecorder.State.READY);pause.setEnabled(recording||player.state()==MidiTakePlayer.State.PLAYING);resume.setEnabled(paused||player.state()==MidiTakePlayer.State.PAUSED);stop.setEnabled(recording||paused||player.state()!=MidiTakePlayer.State.STOPPED);save.setEnabled(review&&take.eventCount()>0);discard.setEnabled(current!=MidiTakeRecorder.State.READY);preview.setEnabled(review&&take.eventCount()>0);export.setEnabled(review&&take.eventCount()>0);boolean interrupted=current==MidiTakeRecorder.State.INTERRUPTED;
    setShown(start,current==MidiTakeRecorder.State.READY);
    setShown(pause,recording||review&&player.state()==MidiTakePlayer.State.PLAYING);
    setShown(resume,paused||review&&player.state()==MidiTakePlayer.State.PAUSED);
    setShown(stop,recording||paused||player.state()!=MidiTakePlayer.State.STOPPED);
    if(review&&!reviewActivated[0]){
      showRightReview(recordingReviewPanel);reviewActivated[0]=true;
     }
     setShown(reviewShortcut,review);
     all.setEnabled(!recording&&!paused);
     setShown(revisionActions,review);
    setShown(playbackStatus,review);setShown(discard,review||paused||recording);
    discard.setEnabled(review||paused||recording);
    String label=captureLabel(current);if(!state.getText().toString().equals(label))state.setText(label);
    String clock=duration(take.elapsedNs(android.os.SystemClock.elapsedRealtimeNanos()));
    if(!recordingClock.getText().toString().equals(clock))recordingClock.setText(clock);
    String notes=take.noteCount()+" notas";if(!recordingNotes.getText().toString().equals(notes))recordingNotes.setText(notes);
    String counter="Eventos MIDI: "+take.eventCount();if(!count.getText().toString().equals(counter))count.setText(counter);
a.handler.postDelayed(this,100);}};ticker.run();

 }
 /** Contextual review reuses original recorder save, export and playback actions. */
 private void showRightReview(LinearLayout panel){
  if(panel==null||dashboardRight==null||!"recording".equals(page))return;
  if(panel.getParent() instanceof android.view.ViewGroup)
   ((android.view.ViewGroup)panel.getParent()).removeView(panel);
  rightContext="review";dashboardRight.removeAllViews();
  LinearLayout head=horizontal();head.setGravity(Gravity.CENTER_VERTICAL);
  TextView title=text("Revisar gravação",19,true);
  head.addView(title,new LayoutParams(0,dp(44),1));
  Button backButton=button("← Voltar às gravações recentes",false,this::leaveReviewPanel);
  backButton.setTag("creation-review-back");backButton.setTextSize(11);
  head.addView(backButton,new LayoutParams(dp(215),dp(44)));
  attach(dashboardRight,head,44,12);
  attach(dashboardRight,mutedText("Salve, reproduza ou exporte esta gravação sem sair do Laboratório.",12),-2,10);
  attach(dashboardRight,panel,-2,0);
 }
 private void leaveReviewPanel(){
  if(!hasUnsavedTake()){showRightRecent();return;}
  activeDialog=new AlertDialog.Builder(a).setTitle("Gravação ainda não salva")
   .setMessage("A tomada será mantida na memória e no diário local. Você poderá retomar a revisão pelo botão da esquerda. Sair não descarta nenhum evento MIDI.")
   .setPositiveButton("Ver gravações recentes",(d,w)->showRightRecent())
   .setNegativeButton("Continuar revisão",null).show();
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
 void refreshConnection(){if(connectionStatus!=null)connectionStatus.setText(compactMidiState());}
 private void confirmJournalAsync(){final CreationJournal log=journal;if(log==null)return;disk.execute(()->{try{log.flush();}catch(IOException error){post(()->{if(journal==log)a.message("Não foi possível confirmar o diário. Salve ou exporte o trecho antes de sair.");});}});}
 private void confirmDiscard(){if(take==null)return;activeDialog=new AlertDialog.Builder(a).setTitle("Descartar gravação?").setMessage("Os eventos desta tomada serão removidos.").setPositiveButton("Descartar",(d,w)->{
  player.stop();final MidiTakeRecorder source=take;final CreationJournal log=journal;source.interrupt(android.os.SystemClock.elapsedRealtimeNanos(),"Descarte confirmado");busy=true;
  disk.execute(()->{try{if(log!=null)log.discardAndDelete();source.discard();post(()->{busy=false;take=null;journal=null;showHome();});}catch(IOException error){post(()->{busy=false;a.message("Não foi possível concluir o descarte; o trecho foi mantido: "+error.getMessage());});}});
 }).setNegativeButton("Continuar",null).show();}
 private void showList(){
  player.stop();restoreListPosition=true;page="list";dashboard();leftMenu();
  rightContext="list";
  LinearLayout head=horizontal();
  TextView title=text("Minhas criações",20,true);
  head.addView(title,new LayoutParams(0,dp(42),1));
  Button backButton=button("← Voltar às gravações recentes",false,this::showHome);
  backButton.setTag("creation-list-back");backButton.setTextSize(11);
  head.addView(backButton,new LayoutParams(dp(215),dp(42)));
  attach(dashboardRight,head,42,10);
  EditText search=new EditText(a);listSearch=search;search.setSingleLine(true);
  search.setTextSize(15);search.setHint("Buscar composição…");search.setContentDescription("Buscar composição");
  search.setPadding(dp(12),0,dp(12),0);search.setBackground(shape(white,13,0xffDCE7F2));
  search.setText(query);
  boolean wideList=getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density>=780
     &&getResources().getConfiguration().fontScale<=1.25f;
  LinearLayout filters=horizontal();
  if(wideList){
   LinearLayout toolbar=horizontal();toolbar.setTag("creation-project-toolbar");
   toolbar.addView(search,new LayoutParams(0,dp(48),2));
   View gap=new View(a);toolbar.addView(gap,new LayoutParams(dp(8),dp(1)));
   toolbar.addView(filters,new LayoutParams(0,dp(48),3));attach(dashboardRight,toolbar,48,12);
  }else{attach(dashboardRight,search,50,8);attach(dashboardRight,filters,48,12);}

  Spinner categories=new Spinner(a);String[] names={"Todas","Com partitura","Só gravação","Recuperáveis","Lixeira"};
  categories.setContentDescription("Filtrar composições");
  categories.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));
  categories.setSelection(catalogFilter);
  Spinner sorting=new Spinner(a);String[] sortNames={"Mais recentes","Mais antigos","Nome A–Z","Nome Z–A","Maior duração","Menor duração"};
  sorting.setContentDescription("Ordenar composições");
  sorting.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,sortNames));
  sorting.setSelection(sortMode);
  int gap=dp(8);View space=new View(a);
  filters.addView(categories,new LayoutParams(0,dp(48),1));filters.addView(space,new LayoutParams(gap,dp(1)));
  filters.addView(sorting,new LayoutParams(0,dp(48),1));
  LinearLayout results=column();attach(dashboardRight,results,-2,0);
  Runnable refresh=()->{
   query=search.getText().toString();
   catalogFilter=categories.getSelectedItemPosition();
   filter=catalogFilter==3?CreationProjectStore.Filter.RECOVERABLE:
     catalogFilter==4?CreationProjectStore.Filter.TRASH:CreationProjectStore.Filter.ALL;
   sortMode=sorting.getSelectedItemPosition();
   results.removeAllViews();populate(results,filter,query);
  };
  search.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int st,int c,int af){}
   public void onTextChanged(CharSequence s,int st,int before,int count){restoreListPosition=false;refresh.run();}
   public void afterTextChanged(android.text.Editable e){}
  });
  android.widget.AdapterView.OnItemSelectedListener changed=new android.widget.AdapterView.OnItemSelectedListener(){
   public void onNothingSelected(android.widget.AdapterView<?> p){}
   public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}
  };
  categories.setOnItemSelectedListener(changed);sorting.setOnItemSelectedListener(changed);
  refresh.run();scroll.post(()->scroll.scrollTo(0,listScrollY));
 }
 private void addProjects(LinearLayout parent,CreationProjectStore.Filter f,int limit){
  if(store==null)return;
  LinearLayout recent=column();attach(parent,recent,-2,0);
  final int owner=a.user;final long revision=++homeRevision;
  disk.execute(()->{try{
   List<CreationProjectStore.Project> projects=store.list(owner,f,"",null);
   post(()->{
    if(revision!=homeRevision||!"recent".equals(rightContext)||a.user!=owner)return;
    recent.removeAllViews();
    if(projects.isEmpty()){
     LinearLayout empty=card();
     attach(empty,mutedText("Suas composições aparecerão aqui. Conecte seu piano para começar uma gravação.",14),-2,0);
     attach(recent,empty,-2,0);
    }else for(int i=0;i<Math.min(limit,projects.size());i++)addProjectRow(recent,projects.get(i));
   });
  }catch(IOException error){post(()->a.message("Não foi possível ler os projetos: "+error.getMessage()));}});
 }
 private void populate(LinearLayout target,CreationProjectStore.Filter f,String q){
  final int owner=a.user,sort=sortMode;final long revision=++listRevision;target.addView(text("Carregando composições…",15,false));
  disk.execute(()->{try{
   Comparator<CreationProjectStore.Project> cmp;switch(sort){case 1:cmp=Comparator.comparingLong((CreationProjectStore.Project p)->p.editedAt);break;case 2:cmp=Comparator.comparing(p->p.title.toLowerCase(Locale.ROOT));break;case 3:cmp=Comparator.comparing((CreationProjectStore.Project p)->p.title.toLowerCase(Locale.ROOT)).reversed();break;case 4:cmp=Comparator.comparingLong((CreationProjectStore.Project p)->p.durationNs).reversed();break;case 5:cmp=Comparator.comparingLong((CreationProjectStore.Project p)->p.durationNs);break;default:cmp=null;}
   if(cmp!=null)cmp=cmp.thenComparing(p->p.id);
   List<CreationProjectStore.Project> all=store.list(owner,f,q,cmp);
   ArrayList<CreationProjectStore.Project> projects=new ArrayList<>();
   final int selected=catalogFilter;
   CreationScoreStore scoreStore=new CreationScoreStore(a.getFilesDir());
   for(CreationProjectStore.Project item:all){
    boolean hasScore=scoreStore.has(item.id);
    if(selected==1&&(!hasScore||item.trash||item.recoverable))continue;
    if(selected==2&&(hasScore||item.trash||item.recoverable))continue;
    projects.add(item);
   }
   post(()->{if(revision!=listRevision||!"list".equals(page)||a.user!=owner)return;target.removeAllViews();if(projects.isEmpty()){
     LinearLayout empty=card();attach(empty,mutedText("Nenhuma composição encontrada.",16),-2,10);
     if(!q.isEmpty())attach(empty,button("Limpar busca",false,()->{if(listSearch!=null)listSearch.setText("");}),48,0);
     attach(target,empty,-2,0);
    }for(CreationProjectStore.Project p:projects)addProjectRow(target,p);if(restoreListPosition){restoreListPosition=false;scroll.post(()->scroll.scrollTo(0,listScrollY));}});
  }catch(IOException error){post(()->a.message("Não foi possível ler Minhas Criações: "+error.getMessage()));}});
 }
 private void addProjectRow(LinearLayout target,CreationProjectStore.Project p){
  String status=p.recoverable?" · Recuperável":p.trash?" · Lixeira":"";
  String updated=p.editedAt!=p.createdAt?" · Editada "+new java.text.SimpleDateFormat("dd/MM",Locale.getDefault()).format(new Date(p.editedAt)):"";
  String subtitle=new java.text.SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(new Date(p.createdAt))
    +" · "+duration(p.durationNs)+updated+status;
  Button row=button(p.title+"\n"+subtitle,false,()->{if(!busy)showDetails(p.id);});
  row.setSingleLine(false);row.setMaxLines(2);row.setTextSize(14);
  row.setTypeface(null,android.graphics.Typeface.NORMAL);row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
  row.setContentDescription(p.title+". "+subtitle+". Abrir composição");
  row.setMinHeight(dp(72));row.setMinimumHeight(dp(72));
  row.setBackground(shape(white,14,0xffE0E9F4));
  attach(target,row,72,8);
 }
 void showDetails(String id){if("list".equals(page))listScrollY=scroll.getScrollY();detailsBack="home".equals(page)?"home":"list";page="details";selectedId=id;dashboard();leftMenu();rightContext="details";label(dashboardRight,"Abrindo composição…",17,true,8);disk.execute(()->{try{CreationProjectStore.Project project=store.get(id);post(()->{if("details".equals(page)&&id.equals(selectedId))renderDetails(project);});}catch(IOException error){post(()->{a.message("Não foi possível abrir a composição: "+error.getMessage());showList();});}});}
 private void renderDetails(CreationProjectStore.Project p){
  String id=p.id;
  try{
   // Do not offer Gerar partitura while an interrupted deletion is pending.
   CreationScoreDeletion.recover(a.getFilesDir(),projectId->LibraryStore.removeCreationEveryProfile(a.prefs,projectId));
   dashboard();leftMenu();rightContext="details";
    LinearLayout panel=dashboardRight;
    LinearLayout head=horizontal();
    TextView panelTitle=text("Detalhes da criação",19,true);
    head.addView(panelTitle,new LayoutParams(0,dp(42),1));
    Button backButton=button("← Minhas criações",false,this::showList);
    backButton.setTag("creation-details-back");backButton.setTextSize(11);
    head.addView(backButton,new LayoutParams(dp(176),dp(42)));
    attach(panel,head,42,9);
   label(panel,p.title,23,true,3);
   attach(panel,mutedText(p.trash?"Na Lixeira":p.recoverable?"Trecho recuperável":"Composição salva",13),-2,12);
   LinearLayout summary=card();
   label(summary,"Duração  "+duration(p.durationNs),17,true,4);
   long noteCount=p.events.stream().filter(e->!e.generated&&e.command()==0x90&&e.data2>0).count();
   attach(summary,mutedText(noteCount+" notas · "+p.events.size()+" eventos MIDI",14),-2,0);
   attach(panel,summary,-2,12);
   if(p.trash){
    attach(panel,button("Restaurar",true,()->mutate(()->store.restore(id,System.currentTimeMillis()))),50,9);
    Button remove=button("Excluir definitivamente…",false,()->activeDialog=new AlertDialog.Builder(a)
     .setTitle("Excluir definitivamente?")
     .setMessage("Excluir definitivamente "+p.title+"? Esta ação não pode ser desfeita.")
     .setPositiveButton("Excluir",(d,w)->mutate(()->store.deletePermanently(id)))
     .setNegativeButton("Cancelar",null).show());
    remove.setTextColor(0xff9D4444);attach(panel,remove,48,12);
   }else{
    if(p.recoverable){
     LinearLayout warning=card();
     attach(warning,mutedText("Gravação recuperável · revise o trecho íntegro antes de finalizar.",14),-2,7);
     attach(warning,button("Revisar e salvar trecho recuperado",true,()->mutate(()->store.finalizeRecovery(id,System.currentTimeMillis()))),50,0);
     attach(panel,warning,-2,12);
    }
    LinearLayout playerCard=card();label(playerCard,"Reprodução no piano",17,true,6);
    playbackStatus=mutedText("Parado",13);attach(playerCard,playbackStatus,-2,9);
    Button play=button("▶ Reproduzir no piano",true,()->{
     if(!a.midiConnection.snapshot().canSend){a.message("Conecte uma saída MIDI funcional para reproduzir no instrumento.");return;}
     player.play(p,ignoreSilence);
    });
    Button pause=button("Pausar",false,()->player.pause());
    Button resume=button("Continuar",false,()->player.resume());
    Button stop=button("Parar",false,()->player.stop());
    LinearLayout controls=horizontal();controls.setTag("creation-detail-transport");
    for(Button control:new Button[]{play,pause,resume,stop}){
     LayoutParams lp=new LayoutParams(0,dp(48),1);lp.rightMargin=dp(5);controls.addView(control,lp);
    }
    attach(playerCard,controls,48,8);
    detailPlay=play;detailPause=pause;detailResume=resume;detailStop=stop;
    CheckBox trim=new CheckBox(a);trim.setText("Ignorar silêncio inicial e final");trim.setTextSize(13);
    trim.setTextColor(ink);trim.setChecked(ignoreSilence);
    trim.setOnCheckedChangeListener((v,checked)->ignoreSilence=checked);
    attach(playerCard,trim,-2,0);
    attach(panel,playerCard,-2,12);
    updatePlaybackControls();
   }
   // A stored score follows a trashed project, but it cannot be opened or studied
   // until the original recording is explicitly restored.
   if(!p.recoverable&&!p.trash){
    CreationScoreStore scoreStore=new CreationScoreStore(a.getFilesDir());
    boolean hasScore=scoreStore.has(id);
    boolean legacyScore=false;
    if(hasScore)try{legacyScore=scoreStore.isLegacy(id);}catch(IOException ignored){}
    LinearLayout scoreCard=card();label(scoreCard,"Partitura",17,true,6);
    attach(scoreCard,mutedText(hasScore?
      (legacyScore?"Partitura do gerador anterior. Gere novamente a partir da gravação; nada será apagado.":
      "Partitura vinculada à gravação original. Adicione à Biblioteca para estudar sem alterar o MIDI."):
      "Transforme a gravação em partitura com duas pautas.",13),-2,8);
    Button viewScore=button(legacyScore?"Gerar nova partitura":hasScore?"Visualizar partitura":"Gerar partitura",
      true,()->CreationScorePreviewScreen.open(a,p));
    if(hasScore&&!new LibraryStore(a.prefs,a.user).hasCreation(id)){
     Button add=button("Adicionar à Biblioteca",false,()->
       a.addCreationScoreToLibrary(id,success->{
        if(success&&"details".equals(page)&&id.equals(selectedId))showDetails(id);
       }));
     responsiveActions(scoreCard,5,viewScore,add);
    }else{
     attach(scoreCard,viewScore,48,5);
     if(hasScore)attach(scoreCard,mutedText("✓ Na Biblioteca Musical",13),-2,4);
    }
    if(hasScore){
     Button deleteScore=button("Excluir somente a partitura…",false,()->confirmDeleteOnlyScore(p));
     deleteScore.setTextColor(0xff9D4444);
     attach(scoreCard,deleteScore,48,10);
    }
    attach(panel,scoreCard,-2,12);
   }
   Button rename=button("Renomear",false,()->rename(p));
   Button infoButton=button("Informações da gravação  ▾",false,()->{});
   if(!p.trash){
    Button export=button("Exportar MIDI (.mid)",false,()->a.exportCreationMidi(id,p.title));
    responsiveActions(panel,12,rename,export,infoButton);
   }else responsiveActions(panel,12,rename,infoButton);
   LinearLayout infoCard=card();infoCard.setVisibility(View.GONE);
   label(infoCard,"Criada · "+new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(p.createdAt)),14,false,6);
   label(infoCard,"Última alteração · "+new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(p.editedAt)),14,false,6);
   label(infoCard,"Duração · "+duration(p.durationNs),14,false,6);
   label(infoCard,"Notas · "+noteCount+" · Eventos · "+p.events.size(),14,false,6);
   label(infoCard,"Perfil · "+profileName(),14,false,6);
   if(!p.instrument.isEmpty())label(infoCard,"Instrumento observado · "+p.instrument,14,false,0);
   infoButton.setOnClickListener(v->{boolean open=infoCard.getVisibility()!=View.VISIBLE;setShown(infoCard,open);infoButton.setText(open?"Informações da gravação  ▴":"Informações da gravação  ▾");});
   attach(panel,infoCard,-2,12);
   if(!p.trash){
    Button trash=button("Mover composição para a Lixeira",false,()->mutate(()->store.moveToTrash(id,System.currentTimeMillis())));
    trash.setTextColor(0xff9D4444);attach(panel,trash,48,0);
   }
  }catch(Exception ex){a.message("Não foi possível abrir a composição: "+ex.getMessage());showList();}
 }
 private void confirmDeleteOnlyScore(CreationProjectStore.Project project){
  activeDialog=new AlertDialog.Builder(a).setTitle("Excluir somente a partitura?")
   .setMessage("A partitura gerada será excluída definitivamente, incluindo seu vínculo com a Biblioteca Musical, Favoritos e Coleções. A gravação MIDI original será preservada. Você poderá gerar outra partitura a partir dela. Faça backup antes se quiser guardá-la.")
   .setPositiveButton("Excluir partitura",(dialog,which)->deleteOnlyScore(project.id,project.profileId))
   .setNegativeButton("Cancelar",null).show();
 }
 private void deleteOnlyScore(String id,int owner){
  if(busy)return;
  player.stop();busy=true;
  disk.execute(()->{
   try{
    CreationProjectStore.Project original=store.get(id);
    if(owner!=a.user||original.profileId!=owner||original.trash||original.recoverable)
     throw new IOException("Composição indisponível para exclusão neste perfil");
    // Validate ownership, source fingerprint and derived MusicXML before deletion.
    CreationScoreStore.Saved saved=new CreationScoreStore(a.getFilesDir()).read(id);
    if(saved.profileId!=owner)throw new IOException("Partitura de outro perfil");
    CreationScoreDeletion.delete(a.getFilesDir(),id,projectId->LibraryStore.removeCreationEveryProfile(a.prefs,projectId));
    post(()->{
     busy=false;
     a.clearRemovedScoreInMemory("creation-score:"+id);
     if(a.screens!=null)a.screens.refreshLibrary();
     a.message("Partitura excluída. A gravação original foi preservada.");
     showDetails(id);
    });
   }catch(Exception error){
    post(()->{busy=false;a.message("Não foi possível excluir a partitura: "+error.getMessage());showDetails(id);});
   }
  });
 }
 private void updatePlaybackControls(){
  if(detailPlay==null)return;
  boolean playing=player.state()==MidiTakePlayer.State.PLAYING;
  boolean paused=player.state()==MidiTakePlayer.State.PAUSED;
  setShown(detailPlay,!playing&&!paused);
  setShown(detailPause,playing);
  setShown(detailResume,paused);
  setShown(detailStop,playing||paused);
 }
 private interface Mutation{void run()throws Exception;}private void mutate(Mutation m){player.stop();busy=true;disk.execute(()->{try{m.run();post(()->{busy=false;showList();});}catch(Exception ex){post(()->{busy=false;a.message("A alteração não foi aplicada: "+ex.getMessage());});}});}
 private void rename(CreationProjectStore.Project p){EditText input=new EditText(a);input.setSingleLine(true);input.setText(p.title);activeDialog=new AlertDialog.Builder(a).setTitle("Renomear composição").setView(input).setPositiveButton("Salvar",(d,w)->mutate(()->store.rename(p.id,input.getText().toString().trim(),System.currentTimeMillis()))).setNegativeButton("Cancelar",null).show();}
 void close(){onBackground();player.close();disk.shutdown();}
}

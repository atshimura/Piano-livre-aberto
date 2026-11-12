package br.org.pianolivre;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Four native landscape surfaces approved for Aprender:
 * entry, choose-and-play, reading path and active score lesson.
 * One local OSMD engine, USB-MIDI or virtual keyboard, no paid services.
 */
final class MusicLearningScreen extends LinearLayout {
 private static final int HOME=0,PICKER=1,PATH=2,PRACTICE=3;
 private static final int NAVY=0xff102344,BLUE=0xff0969D2,INK=0xff102344,
  MUTED=0xff5B718C,PALE=0xffF5F8FF,LINE=0xffD8E3F1;
 private final MainActivity activity;
 private final Runnable goLab;
 private final LinearLayout header,body;
 private final MusicLearningAudio audio=new MusicLearningAudio();
 private final HashSet<Integer> midiHeld=new HashSet<>(),virtualHeld=new HashSet<>();
 private final ArrayDeque<String> recent=new ArrayDeque<>();
 private int page=HOME,selectionType=MusicLearningEngine.CHORD,hand=0,module=0,lessonIndex=0;
 private int[] masks=new int[MusicLearningEngine.MODULE_COUNT];
 private MusicLabCatalog.Entry selected;
 private MusicLearningEngine.Session session;
 private boolean reading=false,hintShown=false;
 private int shownProfile=-1;
 private DiscoveryStaffView pickerStaff;
 private NotationView notation;
 private LearningKeyboardView keyboard;
 private LinearLayout pickerResults;
 private EditText searchInput;
 private TextView pickerSummary,feedback,helpCaption,progressCaption,recentCaption;
 private Button advanceButton;
 MusicLearningScreen(MainActivity a,Runnable goLab){
  super(a);activity=a;this.goLab=goLab;
  setOrientation(VERTICAL);setVisibility(GONE);setBackgroundColor(PALE);
  setTag("learning-root");
  header=horizontal();header.setGravity(Gravity.CENTER_VERTICAL);
  header.setPadding(dp(9),dp(4),dp(9),dp(4));header.setBackgroundColor(NAVY);
  addView(header,new LayoutParams(-1,dp(56)));
  body=vertical();body.setTag("learning-body");
  addView(body,new LayoutParams(-1,0,1));
 }
 private int dp(float size){return Math.round(size*getResources().getDisplayMetrics().density);}
 private boolean shortScreen(){return getResources().getDisplayMetrics().heightPixels/
      getResources().getDisplayMetrics().density<445;}
 private LinearLayout vertical(){LinearLayout l=new LinearLayout(activity);l.setOrientation(VERTICAL);return l;}
 private LinearLayout horizontal(){LinearLayout l=new LinearLayout(activity);l.setOrientation(HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 private GradientDrawable background(int color,int radius,int border){
  GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
  if(border!=0)d.setStroke(dp(1),border);return d;
 }
 private TextView text(String value,int sp,boolean bold,int color){
  TextView view=new TextView(activity);view.setText(value);view.setTextSize(sp);
  view.setTextColor(color);view.setGravity(Gravity.CENTER_VERTICAL);
  if(bold)view.setTypeface(null,Typeface.BOLD);return view;
 }
 private Button button(String label,boolean primary,Runnable onClick){
  Button b=new Button(activity);b.setText(label);b.setAllCaps(false);
  b.setTextSize(shortScreen()?12:14);b.setTypeface(null,Typeface.BOLD);
  b.setTextColor(primary?Color.WHITE:INK);
  b.setPadding(dp(7),0,dp(7),0);
  b.setMinHeight(dp(44));b.setMinimumHeight(dp(44));
  GradientDrawable d=background(primary?BLUE:0xffEEF4FC,12,primary?0:LINE);
  b.setBackground(new RippleDrawable(ColorStateList.valueOf(
      primary?0x25ffffff:0x25075BFA),d,null));
  b.setOnClickListener(v->onClick.run());b.setElevation(dp(1));return b;
 }
 private LinearLayout card(){
  LinearLayout l=vertical();l.setPadding(dp(12),dp(9),dp(12),dp(10));
  l.setBackground(background(Color.WHITE,18,LINE));l.setElevation(dp(2));return l;
 }
 private void weighted(LinearLayout row,View view,int weight,int gap){
  LayoutParams lp=new LayoutParams(0,-1,weight);lp.rightMargin=dp(gap);row.addView(view,lp);
 }
 private void header(String title,String backText,Runnable back){
  header.removeAllViews();
  Button b=button("← "+backText,false,back);
  b.setTextSize(13);b.setTextColor(Color.WHITE);
  b.setBackground(background(0xff283F5C,13,0));
  b.setTag("learning-back");
  header.addView(b,new LayoutParams(dp(shortScreen()?112:144),dp(46)));
  TextView label=text("♫  "+title,shortScreen()?18:22,true,Color.WHITE);
  label.setSingleLine(true);label.setEllipsize(TextUtils.TruncateAt.END);
  label.setPadding(dp(14),0,0,0);label.setTag("learning-title");
  header.addView(label,new LayoutParams(0,-1,1));
 }
 private void clearSurface(){
  audio.stop();
  if(notation!=null){notation.dispose();notation=null;}
  if(pickerStaff!=null){pickerStaff.dispose();pickerStaff=null;}
  if(keyboard!=null){keyboard.reset();keyboard=null;}
  body.removeAllViews();
  feedback=null;helpCaption=null;progressCaption=null;recentCaption=null;advanceButton=null;
  pickerResults=null;searchInput=null;pickerSummary=null;
 }
 private void open(int target){
  clearSurface();page=target;
  if(page==HOME)renderHome();
  else if(page==PICKER)renderPicker();
  else if(page==PATH)renderPath();
  else renderPractice();
 }
 private void loadProgress(){
  shownProfile=activity.user;
  for(int i=0;i<masks.length;i++){
   int allowed=(1<<MusicLearningEngine.lessonCount(i))-1;
   masks[i]=activity.prefs.getInt("user_"+shownProfile+"_learning_module_"+i+"_v1",0)&allowed;
  }
 }
 private void recordSuccess(){
  if(!reading||module<0||module>=masks.length)return;
  if(shownProfile!=activity.user)return;
  int flag=1<<lessonIndex;
  if((masks[module]&flag)!=0)return;
  masks[module]|=flag;
  activity.prefs.edit().putInt("user_"+shownProfile+"_learning_module_"+module+"_v1",masks[module]).apply();
 }
 void enter(){
  loadProgress();midiHeld.clear();virtualHeld.clear();session=null;recent.clear();
  reading=false;setVisibility(VISIBLE);open(HOME);
 }
 void leave(){
  audio.stop();midiHeld.clear();virtualHeld.clear();session=null;
  if(notation!=null){notation.dispose();notation=null;}
  if(pickerStaff!=null){pickerStaff.dispose();pickerStaff=null;}
  setVisibility(GONE);
 }
 void pause(){audio.stop();midiHeld.clear();virtualHeld.clear();}
 void resume(){if(getVisibility()==VISIBLE&&shownProfile!=activity.user)enter();}
 boolean back(){
  if(getVisibility()!=VISIBLE)return false;
  if(page==PRACTICE){session=null;open(reading?PATH:PICKER);return true;}
  if(page==PATH||page==PICKER){open(HOME);return true;}
  leave();goLab.run();return true;
 }
 void onMidiPressed(Collection<Integer> notes){
  if(getVisibility()!=VISIBLE)return;
  midiHeld.clear();if(notes!=null)midiHeld.addAll(notes);
 }
 void onMidiAttack(int pitch){
  if(getVisibility()==VISIBLE&&page==PRACTICE)observe(pitch);
 }
 private void onVirtualKeys(Set<Integer> notes){
  if(page!=PRACTICE||session==null)return;
  HashSet<Integer> entering=new HashSet<>(notes);
  entering.removeAll(virtualHeld);
  virtualHeld.clear();virtualHeld.addAll(notes);
  for(int n:entering)observe(n);
 }
 private void observe(int pitch){
  if(session==null||session.complete()||session.exercise.kind==MusicLearningEngine.RHYTHM)return;
  HashSet<Integer> combined=new HashSet<>(midiHeld);combined.addAll(virtualHeld);
  MusicLearningEngine.Feedback result=session.attack(pitch,combined,false);
  if(result==MusicLearningEngine.Feedback.IGNORED)return;
  if(result==MusicLearningEngine.Feedback.COMPLETE){
   recordSuccess();recent.addFirst(MusicTheory.noteName(pitch)+" ✓");
   while(recent.size()>3)recent.removeLast();
   feedback.setText("✓ Correto! "+(reading?"Prossiga para a próxima lição.":"Você tocou o exercício corretamente."));
   advanceButton.setEnabled(true);advanceButton.setAlpha(1f);
  }else if(result==MusicLearningEngine.Feedback.CORRECT||
            result==MusicLearningEngine.Feedback.PROGRESS){
   feedback.setText("✓ Muito bem! "+session.correct()+"/"+session.total()+
       " · continue tocando a próxima nota.");
  }else feedback.setText("Ainda não. Verifique a nota e tente novamente; sem limite de tempo.");
  refreshExerciseHelp();
 }
 private void acceptRhythm(int choice){
  if(session==null||session.exercise.kind!=MusicLearningEngine.RHYTHM)return;
  MusicLearningEngine.Feedback result=session.identifyFigure(choice);
  if(result==MusicLearningEngine.Feedback.COMPLETE){
   recordSuccess();feedback.setText("✓ Figura correta: "+MusicLearningEngine.RHYTHM_NAMES[choice]+".");
   advanceButton.setEnabled(true);advanceButton.setAlpha(1f);
  }else feedback.setText("Ainda não. Observe a cabeça, a haste e as bandeirolas.");
 }
 private void cueTitle(LinearLayout root,String message){
  TextView label=text(message,shortScreen()?12:14,false,MUTED);
  label.setGravity(Gravity.CENTER);root.addView(label,new LayoutParams(-1,dp(shortScreen()?22:30)));
 }
 private void renderHome(){
  header("Aprender","Laboratório",()->back());
  LinearLayout root=vertical();root.setPadding(dp(12),dp(4),dp(12),dp(5));body.addView(root,new LayoutParams(-1,-1));
  cueTitle(root,"Escolha como deseja aprender no Piano Livre.");
  LinearLayout row=horizontal();root.addView(row,new LayoutParams(-1,0,1));
  LinearLayout freely=modeCard("♫","Escolher e tocar",
   "Escolha uma nota, acorde ou sequência pelo nome e pratique na partitura.",
   "Abrir exercício →",()->{selectionType=MusicLearningEngine.CHORD;selected=null;open(PICKER);});
  LinearLayout read=modeCard("♬","Ler partitura",
   "Aprenda a reconhecer notas, ritmos e padrões diretamente da partitura.",
   "Abrir lições →",()->open(PATH));
  weighted(row,freely,1,10);weighted(row,read,1,0);
  TextView hint=text("ⓘ  Estude no seu ritmo e acompanhe seu progresso.",11,false,MUTED);
  hint.setGravity(Gravity.CENTER);root.addView(hint,new LayoutParams(-1,dp(shortScreen()?18:27)));
 }
 private LinearLayout modeCard(String icon,String name,String desc,String action,Runnable callback){
  LinearLayout card=card();card.setTag("learning-home-"+name);
  TextView symbol=text(icon,shortScreen()?26:35,true,BLUE);symbol.setGravity(Gravity.CENTER);
  card.addView(symbol,new LayoutParams(-1,dp(shortScreen()?40:60)));
  TextView title=text(name,shortScreen()?17:21,true,INK);title.setGravity(Gravity.CENTER);
  card.addView(title,new LayoutParams(-1,dp(shortScreen()?33:43)));
  TextView description=text(desc,shortScreen()?12:14,false,MUTED);description.setGravity(Gravity.CENTER);description.setMaxLines(4);
  card.addView(description,new LayoutParams(-1,0,1));
  Button go=button(action,true,callback);go.setTag("learning-home-open-"+name);
  card.addView(go,new LayoutParams(-1,dp(48)));
  return card;
 }
 private void pickerKind(int type){
  selectionType=type;selected=null;open(PICKER);
 }
 private void pickerHand(int chosen){hand=chosen;open(PICKER);}
 private void renderPicker(){
  header("Escolher e tocar","Aprender",()->back());
  LinearLayout root=vertical();root.setPadding(dp(8),dp(2),dp(8),dp(7));body.addView(root,new LayoutParams(-1,-1));
  cueTitle(root,"Monte um exercício para praticar na partitura.");
  LinearLayout columns=horizontal();root.addView(columns,new LayoutParams(-1,0,1));
  LinearLayout types=card();types.setTag("learning-picker-type");
  TextView typeTitle=text("♫   Tipo",18,true,INK);typeTitle.setGravity(Gravity.CENTER);types.addView(typeTitle,new LayoutParams(-1,dp(35)));
  String[] names={"Nota","Acorde","Sequência"};
  for(int i=0;i<3;i++){
   final int choice=i;
   Button b=button(names[i],selectionType==i,()->pickerKind(choice));
   b.setTag("learning-picker-kind-"+i);
   LayoutParams lp=new LayoutParams(-1,0,1);lp.topMargin=dp(6);types.addView(b,lp);
  }
  weighted(columns,types,9,8);
  LinearLayout content=card();content.setTag("learning-picker-content");
  TextView contentTitle=text("☷   Conteúdo",18,true,INK);contentTitle.setGravity(Gravity.CENTER);
  content.addView(contentTitle,new LayoutParams(-1,dp(34)));
  searchInput=new EditText(activity);searchInput.setSingleLine(true);searchInput.setTextSize(13);
  searchInput.setHint("Pesquisar pelo nome");
  searchInput.setContentDescription("Pesquisar nota, acorde, escala ou arpejo por nome");
  searchInput.setTag("learning-search");
  content.addView(searchInput,new LayoutParams(-1,dp(44)));
  ScrollView list=new ScrollView(activity);list.setFillViewport(true);list.setOverScrollMode(OVER_SCROLL_NEVER);
  pickerResults=vertical();list.addView(pickerResults);
  content.addView(list,new LayoutParams(-1,0,1));
  TextView handTitle=text("Mão de referência",13,true,INK);
  handTitle.setGravity(Gravity.CENTER);content.addView(handTitle,new LayoutParams(-1,dp(25)));
  LinearLayout hands=horizontal();
  String[] handNames={"Direita","Esquerda","Ambas"};
  for(int i=0;i<3;i++){
   final int chosen=i;Button b=button(handNames[i],hand==i,()->pickerHand(chosen));
   b.setTextSize(11);LayoutParams lp=new LayoutParams(0,dp(41),1);lp.rightMargin=dp(i==2?0:3);
   hands.addView(b,lp);
  }
  content.addView(hands,new LayoutParams(-1,dp(41)));
  weighted(columns,content,10,8);
  LinearLayout visual=card();visual.setTag("learning-picker-preview");
  TextView visualTitle=text("◉   Visualização",18,true,INK);visualTitle.setGravity(Gravity.CENTER);
  visual.addView(visualTitle,new LayoutParams(-1,dp(34)));
  pickerStaff=new DiscoveryStaffView(activity);pickerStaff.setTag("learning-picker-score");
  visual.addView(pickerStaff,new LayoutParams(-1,0,1));
  pickerSummary=text("Clave de sol · andamento livre\nToque no seu ritmo",12,false,MUTED);
  pickerSummary.setGravity(Gravity.CENTER);
  visual.addView(pickerSummary,new LayoutParams(-1,dp(55)));
  weighted(columns,visual,10,0);
  LinearLayout commands=horizontal();commands.setPadding(0,dp(6),0,0);
  Button hear=button("▷  Ouvir exemplo",false,()->{
   if(selected!=null)audio.play(selected.notes,
     !(selected.kind.equals("Nota")||selected.kind.equals("Acorde")));
  });
  Button start=button("Iniciar prática  →",true,()->startFree());
  start.setTag("learning-picker-start");
  LayoutParams left=new LayoutParams(0,dp(47),1);left.rightMargin=dp(8);commands.addView(hear,left);
  commands.addView(start,new LayoutParams(0,dp(47),2));
  root.addView(commands,new LayoutParams(-1,dp(54)));
  searchInput.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){showPickerResults(s.toString());}
   public void afterTextChanged(android.text.Editable e){}
  });
  showPickerResults("");
 }
 private void showPickerResults(String query){
  if(pickerResults==null)return;
  List<MusicLabCatalog.Entry> options=MusicLearningEngine.catalog(selectionType,query);
  // MusicLabCatalog creates fresh immutable Entry objects on each query.
  // Compare semantic keys, not object identity, or tapping a choice resets it.
  MusicLabCatalog.Entry match=null;
  if(selected!=null)for(MusicLabCatalog.Entry e:options)
   if(e.kind.equals(selected.kind)&&e.title.equals(selected.title)){match=e;break;}
  if(match==null&&selectionType==MusicLearningEngine.CHORD&&query.isEmpty())
   for(MusicLabCatalog.Entry e:options)if(e.title.startsWith("Dó maior")){match=e;break;}
  selected=match!=null?match:options.isEmpty()?null:options.get(0);
  pickerResults.removeAllViews();
  if(options.isEmpty()){
   pickerResults.addView(text("Nenhum conteúdo corresponde à busca.",12,false,MUTED),new LayoutParams(-1,dp(45)));
  }else{
   for(int i=0;i<Math.min(options.size(),60);i++){
    MusicLabCatalog.Entry entry=options.get(i);
    Button item=button(entry.title,selected==entry,()->{
     selected=entry;showPickerResults(searchInput.getText().toString());
    });
    item.setTextSize(11);item.setTag("learning-catalog-item");
    LayoutParams lp=new LayoutParams(-1,dp(42));lp.bottomMargin=dp(3);pickerResults.addView(item,lp);
   }
   if(options.size()>60){
    TextView more=text("Refine a busca para encontrar outras opções.",11,false,MUTED);
    more.setGravity(Gravity.CENTER);pickerResults.addView(more,new LayoutParams(-1,dp(27)));
   }
  }
  if(pickerStaff!=null){
   List<Integer> notes=new ArrayList<>();if(selected!=null)for(int n:selected.notes)notes.add(n);
   pickerStaff.setNotes(notes,selectionType==MusicLearningEngine.SEQUENCE);
  }
  if(pickerSummary!=null){
   pickerSummary.setText((hand==1?"Clave de fá":hand==2?"Duas claves":"Clave de sol")+
    " · andamento livre\n"+(selected==null?"Escolha um conteúdo":"Tocar no seu ritmo · "+selected.notes.length+" notas"));
  }
 }
 private void startFree(){
  if(selected==null){activity.message("Escolha uma nota, acorde ou sequência.");return;}
  reading=false;hintShown=false;session=new MusicLearningEngine.Session(MusicLearningEngine.choose(selected,hand));
  midiHeld.clear();virtualHeld.clear();open(PRACTICE);
 }
 private void renderPath(){
  header("Ler partitura","Aprender",()->back());
  LinearLayout root=vertical();root.setPadding(dp(10),dp(3),dp(10),dp(6));body.addView(root,new LayoutParams(-1,-1));
  cueTitle(root,"Siga uma trilha progressiva para aprender a leitura musical.");
  LinearLayout lessons=horizontal();lessons.setTag("learning-lesson-path");
  root.addView(lessons,new LayoutParams(-1,0,1));
  String[] icons={"𝄞","↑↓","♫","♬","▤"};
  for(int i=0;i<MusicLearningEngine.MODULE_COUNT;i++){
   final int selection=i;
   boolean open=MusicLearningEngine.unlocked(i,masks);
   int total=MusicLearningEngine.lessonCount(i),done=MusicLearningEngine.completeCount(i,masks);
   LinearLayout card=card();card.setTag("learning-path-module-"+i);
   if(i==0)card.setBackground(background(0xffEEF6FF,16,BLUE));
   TextView glyph=text(icons[i],shortScreen()?22:29,true,open?BLUE:MUTED);glyph.setGravity(Gravity.CENTER);
   card.addView(glyph,new LayoutParams(-1,dp(shortScreen()?37:51)));
   TextView name=text(MusicLearningEngine.MODULES[i],shortScreen()?12:15,true,INK);
   name.setMaxLines(2);name.setGravity(Gravity.CENTER);
   card.addView(name,new LayoutParams(-1,dp(shortScreen()?45:57)));
   TextView desc=text(MusicLearningEngine.MODULE_DETAILS[i],shortScreen()?10:12,false,MUTED);
   desc.setGravity(Gravity.CENTER);desc.setMaxLines(3);
   card.addView(desc,new LayoutParams(-1,0,1));
   String state=!open?"🔒 Bloqueado":done==total?"Concluído ✓":done>0?"Em andamento":"Disponível";
   Button choose=button(state,false,()->startLesson(selection,MusicLearningEngine.nextIndex(selection,masks)));
   choose.setTag("learning-module-open-"+i);choose.setEnabled(open);choose.setAlpha(open?1f:.66f);
   card.addView(choose,new LayoutParams(-1,dp(43)));
   weighted(lessons,card,1,i==MusicLearningEngine.MODULE_COUNT-1?0:dp(0));
   if(i!=MusicLearningEngine.MODULE_COUNT-1){
    // a small gutter is applied through the card's own layout params
    LayoutParams lp=(LayoutParams)card.getLayoutParams();lp.rightMargin=dp(7);card.setLayoutParams(lp);
   }
  }
  LinearLayout totals=horizontal();totals.setPadding(dp(9),dp(5),dp(9),dp(5));
  totals.setBackground(background(Color.WHITE,14,LINE));
  int achieved=0;for(int i=0;i<masks.length;i++)achieved+=MusicLearningEngine.completeCount(i,masks);
  int pct=(int)Math.round(100.0*achieved/MusicLearningEngine.totalCount());
  TextView totalText=text("Progresso geral: "+pct+"%",shortScreen()?13:16,true,INK);
  totals.addView(totalText,new LayoutParams(0,-1,1));
  ProgressBar bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
  bar.setMax(MusicLearningEngine.totalCount());bar.setProgress(achieved);
  bar.setProgressTintList(ColorStateList.valueOf(BLUE));
  totals.addView(bar,new LayoutParams(0,dp(10),1));
  TextView doneText=text("   ✓ "+achieved+" exercícios  ·  ♫ "+
    MusicLearningEngine.completeCount(0,masks)+" notas",shortScreen()?11:13,false,MUTED);
  totals.addView(doneText,new LayoutParams(0,-1,1));
  LayoutParams totalLp=new LayoutParams(-1,dp(shortScreen()?51:67));totalLp.topMargin=dp(6);
  root.addView(totals,totalLp);
  LinearLayout buttons=horizontal();
  Button review=button("Revisar notas",false,()->startLesson(0,0));
  Button next=button("Continuar lição  →",true,()->{
   int first=0;
   for(int i=0;i<MUSIC_MODULE_COUNT();i++)if(MusicLearningEngine.unlocked(i,masks)&&
       MusicLearningEngine.completeCount(i,masks)<MusicLearningEngine.lessonCount(i)){first=i;break;}
   startLesson(first,MusicLearningEngine.nextIndex(first,masks));
  });
  LayoutParams left=new LayoutParams(0,dp(47),1);left.rightMargin=dp(8);buttons.addView(review,left);
  buttons.addView(next,new LayoutParams(0,dp(47),2));
  LayoutParams btnLp=new LayoutParams(-1,dp(51));btnLp.topMargin=dp(7);root.addView(buttons,btnLp);
 }
 private int MUSIC_MODULE_COUNT(){return MusicLearningEngine.MODULE_COUNT;}
 private void startLesson(int which,int index){
  if(!MusicLearningEngine.unlocked(which,masks))return;
  reading=true;module=which;lessonIndex=index;hintShown=false;
  session=new MusicLearningEngine.Session(MusicLearningEngine.lesson(module,lessonIndex));
  midiHeld.clear();virtualHeld.clear();open(PRACTICE);
 }
 private void nextExercise(){
  if(session==null||!session.complete())return;
  if(!reading){session.reset();hintShown=false;open(PRACTICE);return;}
  if(lessonIndex+1<MusicLearningEngine.lessonCount(module)){
   startLesson(module,lessonIndex+1);return;
  }
  open(PATH);
 }
 private void renderPractice(){
  if(session==null){open(HOME);return;}
  final MusicLearningEngine.Exercise ex=session.exercise;
  header(reading?MusicLearningEngine.MODULES[module]:"Praticar: "+ex.title,
    reading?"Ler partitura":"Escolher e tocar",()->back());
  LinearLayout root=vertical();root.setPadding(dp(9),dp(4),dp(9),dp(6));root.setTag("learning-practice-screen");
  body.addView(root,new LayoutParams(-1,-1));
  LinearLayout top=horizontal();top.setPadding(dp(6),0,dp(6),0);
  String progress=reading?"Lição "+(lessonIndex+1)+" de "+MusicLearningEngine.lessonCount(module):
    "Andamento livre · toque no seu ritmo";
  progressCaption=text(progress,13,true,INK);top.addView(progressCaption,new LayoutParams(0,-1,1));
  recentCaption=text("Últimos acertos: "+(recent.isEmpty()?"—":TextUtils.join(" · ",recent)),11,false,MUTED);
  top.addView(recentCaption,new LayoutParams(0,-1,1));
  root.addView(top,new LayoutParams(-1,dp(32)));
  FrameLayout scoreCard=new FrameLayout(activity);
  scoreCard.setTag("learning-practice-score");
  scoreCard.setPadding(dp(5),dp(4),dp(5),dp(3));
  scoreCard.setBackground(background(Color.WHITE,17,LINE));
  root.addView(scoreCard,new LayoutParams(-1,0,1));
  notation=new NotationView(activity,new NotationView.Listener(){
   public void onRendered(){android.util.Log.i("PianoLivreLearning","LEARN_OSMD_RENDERED "+ex.id);}
   public void onError(String message){if(page==PRACTICE&&feedback!=null)feedback.setText(
     "Não foi possível desenhar a partitura: "+message);}
   @Override public void onFailure(NotationFailure failure){
    if(page==PRACTICE)activity.showNotationFailure(failure,
      ()->{if(page==PRACTICE)renderScore(ex);},()->{});
   }
  });
  notation.setTag("learning-score-osmd");
  scoreCard.addView(notation,new FrameLayout.LayoutParams(-1,-1));
  scoreCard.post(()->{if(page==PRACTICE)renderScore(ex);});
  TextView instruction=text(ex.kind==MusicLearningEngine.RHYTHM?
    "Identifique a figura mostrada na partitura":
    ex.kind==MusicLearningEngine.CHORD?"Toque todas as notas do acorde":
    ex.kind==MusicLearningEngine.SEQUENCE?"Toque as notas na ordem mostrada":
    "Toque a nota mostrada",shortScreen()?13:17,true,INK);
  instruction.setGravity(Gravity.CENTER);root.addView(instruction,new LayoutParams(-1,dp(32)));
  if(ex.kind==MusicLearningEngine.RHYTHM){
   LinearLayout choices=horizontal();
   for(int i=0;i<MusicLearningEngine.RHYTHM_NAMES.length;i++){
    final int selection=i;
    Button b=button(MusicLearningEngine.RHYTHM_NAMES[i],false,()->acceptRhythm(selection));
    b.setTextSize(10);
    LayoutParams lp=new LayoutParams(0,dp(43),1);lp.rightMargin=dp(i==4?0:4);choices.addView(b,lp);
   }
   root.addView(choices,new LayoutParams(-1,dp(49)));
  }else{
   keyboard=new LearningKeyboardView(activity,this::onVirtualKeys);
   keyboard.setTag("learning-virtual-keyboard");
   root.addView(keyboard,new LayoutParams(-1,dp(shortScreen()?74:102)));
  }
  LinearLayout info=horizontal();
  helpCaption=text("",shortScreen()?11:13,false,MUTED);
  helpCaption.setTag("learning-help-text");
  info.addView(helpCaption,new LayoutParams(0,dp(37),1));
  feedback=text("Aguardando sua resposta.",shortScreen()?11:13,false,MUTED);
  feedback.setTag("learning-feedback");info.addView(feedback,new LayoutParams(0,dp(37),1));
  root.addView(info,new LayoutParams(-1,dp(37)));
  LinearLayout actions=horizontal();actions.setTag("learning-practice-actions");
  Button hint=button("Mostrar dica",false,()->{hintShown=true;refreshExerciseHelp();});
  Button hear=button("▷ Ouvir nota",false,()->audio.play(ex.pitches,ex.ordered));
  advanceButton=button(reading?"Próximo após acerto  →":"Repetir exercício",true,this::nextExercise);
  advanceButton.setTag("learning-next-after-correct");
  advanceButton.setEnabled(session.complete());advanceButton.setAlpha(session.complete()?1f:.55f);
  LayoutParams first=new LayoutParams(0,dp(47),1);first.rightMargin=dp(6);actions.addView(hint,first);
  LayoutParams middle=new LayoutParams(0,dp(47),1);middle.rightMargin=dp(6);actions.addView(hear,middle);
  actions.addView(advanceButton,new LayoutParams(0,dp(47),2));
  root.addView(actions,new LayoutParams(-1,dp(49)));
  refreshExerciseHelp();
 }
 private void renderScore(MusicLearningEngine.Exercise ex){
  if(notation==null||page!=PRACTICE)return;
  try{
   byte[] data=MusicLearningScore.xml(ex).getBytes(StandardCharsets.UTF_8);
   Score parsed=Score.read(new ByteArrayInputStream(data),"aprender.musicxml");
   notation.loadCreationScore(parsed,0);
  }catch(Exception error){
   if(feedback!=null)feedback.setText("Falha ao preparar a lição: "+error.getMessage());
   android.util.Log.e("PianoLivreLearning","LEARN_SCORE_FAILURE",error);
  }
 }
 private void refreshExerciseHelp(){
  if(session==null||page!=PRACTICE)return;
  MusicLearningEngine.Exercise ex=session.exercise;
  boolean basic=reading&&module==0&&lessonIndex<2;
  boolean keyboardGuide=reading&&module==0&&lessonIndex<4;
  boolean namesVisible=hintShown||basic||!reading;
  String text;
  if(ex.kind==MusicLearningEngine.RHYTHM)
   text=hintShown?"Dica: observe a cabeça e a haste. A resposta é "+
      MusicLearningEngine.RHYTHM_NAMES[ex.rhythm]+".":"Observe cabeça, haste e bandeirolas da figura.";
  else if(ex.kind==MusicLearningEngine.CHORD)
   text=namesVisible?"Acorde: "+ex.title+" · "+ex.pitches.length+" notas":"Observe as notas simultâneas.";
  else if(ex.kind==MusicLearningEngine.SEQUENCE)
   text=namesVisible?"Próxima: "+(session.complete()?"concluído":MusicTheory.noteName(session.next()))+
      " · "+session.correct()+"/"+session.total():"Observe a direção e os intervalos.";
  else text=namesVisible?"Nome da nota: "+MusicTheory.noteName(ex.pitches[0]):
   "Reconheça a posição na pauta sem consultar o nome.";
  if(helpCaption!=null)helpCaption.setText(text);
  if(keyboard!=null)keyboard.hint(ex.pitches,hintShown||keyboardGuide||!reading);
 }
 void dispose(){leave();}
}

package br.org.pianolivre;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.content.Context;
import java.util.*;

/**
 * First usable Laboratory slice: offline Discovery and a small Learn/Practice
 * reference set. No MIDI output, grading or speculative scale recognition.
 * The piano keyboard is a multitouch INPUT; the staff is a simple pitch map.
 */
final class MusicLabScreen extends LinearLayout {
 private final MainActivity activity;
 private final TextView heading,description,readout,detail,practiceResult,rangeLabel;
 private final LinearLayout learner,sequenceTools;
 private final Button discoverButton,learnButton,touchButton,assembleButton,practiceButton,sequenceButton,demoButton;
 private final CheckBox demoRepeat;
 private final MusicLabPlaybackPlan.Session demoSession=new MusicLabPlaybackPlan.Session();
 private int demoSpeed=100;
 private final MusicLabKeyboard keyboard;
 private final Set<Integer> midi=new HashSet<>(),virtual=new HashSet<>();
 private final ArrayList<Integer> sequenceAttacks=new ArrayList<>();
 private final MusicPatternTracker rollingPatterns=new MusicPatternTracker();
 private final TextView sequenceStatus;
 private boolean recordingSequence=false;
 private boolean learn=false,assembling=false,practicing=false;
 private int presetIndex=0;
 private final List<MusicLabCatalog.Entry> fullCatalog=MusicLabCatalog.all();
 private final List<MusicLabCatalog.Entry> visibleCatalog=new ArrayList<>();
 private Spinner choices;
 private MusicLabCatalog.Entry currentEntry(){return visibleCatalog.isEmpty()?null:
   visibleCatalog.get(Math.max(0,Math.min(presetIndex,visibleCatalog.size()-1)));}
 private void filterCatalog(String text){
  stopDemonstration(); // Cancels even in the silent interval between notes.
  MusicLabCatalog.Entry prior=currentEntry();
  visibleCatalog.clear();visibleCatalog.addAll(MusicLabCatalog.filter(fullCatalog,text));
  ArrayList<String> names=new ArrayList<>();for(MusicLabCatalog.Entry e:visibleCatalog)names.add(e.kind+" · "+e.title);
  if(names.isEmpty())names.add("Nenhum item encontrado");
  choices.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,names));
  presetIndex=MusicLabCatalog.selectionIndex(visibleCatalog,prior);
  choices.setSelection(Math.max(0,presetIndex));practicing=false;
  if(practiceButton!=null)practiceButton.setEnabled(!visibleCatalog.isEmpty());
  refresh();
 }
 private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView label(String str,int size,boolean bold){
  TextView t=new TextView(activity);t.setText(str);t.setTextSize(size);t.setTextColor(0xff1B304D);
  if(bold)t.setTypeface(null,1);return t;
 }
 private Button button(String title,Runnable action){
  Button b=new Button(activity);b.setAllCaps(false);b.setTextSize(12);b.setText(title);
  b.setOnClickListener(v->action.run());return b;
 }
 private void item(LinearLayout parent,View child,int w,int h){
  parent.addView(child,new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h)));
 }
 MusicLabScreen(MainActivity activity,Runnable goHome){
  super(activity);this.activity=activity;setOrientation(VERTICAL);setBackgroundColor(0xffEEF4FA);
  setVisibility(GONE);
  LinearLayout top=new LinearLayout(activity);top.setGravity(Gravity.CENTER_VERTICAL);
  top.setPadding(dp(10),0,dp(12),0);top.setBackgroundColor(0xffffffff);
  item(this,top,-1,43);
  item(top,button("← Biblioteca",goHome),120,36);
  TextView title=label("Laboratório Musical",17,true);title.setPadding(dp(12),0,0,0);
  top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);
  addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout body=new LinearLayout(activity);body.setPadding(dp(14),dp(7),dp(14),dp(18));
  body.setOrientation(VERTICAL);scroll.addView(body);
  LinearLayout actions=new LinearLayout(activity);actions.setGravity(Gravity.CENTER_VERTICAL);
  item(body,actions,-1,40);
  discoverButton=button("Descobrir",()->setLearn(false));
  learnButton=button("Aprender / Praticar",()->setLearn(true));
  item(actions,discoverButton,130,36);item(actions,learnButton,170,36);
  // Keep the virtual input mode visible even on short landscape displays.
  LinearLayout touchModes=new LinearLayout(activity);touchModes.setGravity(Gravity.CENTER_VERTICAL);
  item(body,touchModes,-1,38);
  touchButton=button("Tocar",()->setAssembling(false));
  assembleButton=button("Montar",()->setAssembling(true));
  item(touchModes,touchButton,92,35);item(touchModes,assembleButton,92,35);
  heading=label("",18,true);item(body,heading,-1,-2);
  description=label("",12,false);item(body,description,-1,-2);
  readout=label("",19,true);readout.setPadding(0,dp(5),0,0);item(body,readout,-1,-2);
  detail=label("",12,false);item(body,detail,-1,-2);
  // Explicit sequence boundaries avoid guessing where an arpeggio starts/ends.
  sequenceTools=new LinearLayout(activity);sequenceTools.setGravity(Gravity.CENTER_VERTICAL);
  item(body,sequenceTools,-1,48);
  sequenceButton=button("Iniciar sequência",()->toggleSequence());
  item(sequenceTools,sequenceButton,145,38);
  sequenceStatus=label("Sequência: toque para iniciar a análise",11,false);
  sequenceTools.addView(sequenceStatus,new LinearLayout.LayoutParams(0,-2,1));
  learner=new LinearLayout(activity);learner.setOrientation(VERTICAL);
  item(body,learner,-1,-2);
  EditText search=new EditText(activity);search.setSingleLine(true);
  search.setTextSize(12);search.setHint("Buscar nota, intervalo, acorde, escala ou arpejo");
  search.setContentDescription("Pesquisar no catálogo musical");
  item(learner,search,-1,40);
  choices=new Spinner(activity);
  choices.setContentDescription("Exercícios do catálogo musical");
  item(learner,choices,-1,39);
  search.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence s,int start,int count,int after){}
   public void onTextChanged(CharSequence s,int start,int before,int count){filterCatalog(s.toString());}
   public void afterTextChanged(android.text.Editable e){}
  });
  filterCatalog("");
  choices.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
   public void onNothingSelected(AdapterView<?> parent){}
   public void onItemSelected(AdapterView<?> parent,View view,int index,long id){
    stopDemonstration();presetIndex=index;practicing=false;refresh();
   }
  });
  LinearLayout commands=new LinearLayout(activity);commands.setGravity(Gravity.CENTER_VERTICAL);
  item(learner,commands,-1,41);
  practiceButton=button("Praticar",()->{practicing=!practicing;refresh();});
  item(commands,practiceButton,115,37);
  demoButton=button("▶ Demonstrar",()->toggleDemo());
  item(commands,demoButton,125,37);
  practiceResult=label("",12,true);commands.addView(practiceResult,new LinearLayout.LayoutParams(0,-2,1));
  LinearLayout demonstration=new LinearLayout(activity);demonstration.setGravity(Gravity.CENTER_VERTICAL);
  item(learner,demonstration,-1,40);
  item(demonstration,label("Velocidade:",11,false),80,-2);
  Spinner speeds=new Spinner(activity);
  speeds.setContentDescription("Velocidade de demonstração");
  String[] speedNames={"50%","75%","100%","125%","150%"};
  speeds.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,speedNames));
  item(demonstration,speeds,97,39);speeds.setSelection(2);
  speeds.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
   public void onNothingSelected(AdapterView<?> parent){}
   public void onItemSelected(AdapterView<?> parent,View view,int index,long id){
    demoSpeed=50+index*25;stopDemonstration();
   }
  });
  demoRepeat=new CheckBox(activity);demoRepeat.setText("Repetir");demoRepeat.setTextSize(12);
  item(demonstration,demoRepeat,-2,-2);
  TextView keyboardHeading=label("Pauta e teclado · verde: tocado · dourado: referência",11,false);
  keyboardHeading.setPadding(0,dp(7),0,0);item(body,keyboardHeading,-1,-2);
  keyboard=new MusicLabKeyboard(activity,notes->{
   if(!learn&&!assembling){
    ArrayList<Integer> newAttacks=new ArrayList<>();
    for(int note:notes)if(!virtual.contains(note))newAttacks.add(note);
    // Simultaneous fingers have no unambiguous order: skip rather than
    // manufacturing one from HashSet iteration order.
    if(newAttacks.size()==1){
     if(recordingSequence)addSequenceAttack(newAttacks.get(0));
     else observeSpontaneous(newAttacks.get(0),MusicPatternTracker.isIsolated(newAttacks.get(0),midi,notes));
    }else if(newAttacks.size()>1){
     rollingPatterns.clear();
     sequenceStatus.setText(recordingSequence?
      "Notas simultâneas: ordem indeterminada; toque uma de cada vez.":
      "Notas simultâneas: reconhecimento livre reiniciado.");
    }
   }
   virtual.clear();virtual.addAll(notes);refresh();
  });
  item(body,keyboard,-1,210);
  LinearLayout keyboardOptions=new LinearLayout(activity);
  keyboardOptions.setGravity(Gravity.CENTER_VERTICAL);item(body,keyboardOptions,-1,45);
  item(keyboardOptions,button("◀ Oitava",()->shift(-12)),96,36);
  rangeLabel=label("",11,true);rangeLabel.setGravity(Gravity.CENTER);
  keyboardOptions.addView(rangeLabel,new LinearLayout.LayoutParams(0,-2,1));
  item(keyboardOptions,button("Oitava ▶",()->shift(12)),96,36);
  item(keyboardOptions,button("Limpar",()->keyboard.clear()),75,36);
  TextView caveat=label("Entrada USB-MIDI ou teclado virtual. Sem som sintetizado pelo teclado virtual. " +
   "Escalas e arpejos exigem uma sequência completa de uma oitava; Iniciar/Concluir é opcional. " +
   "Demonstração opcional pela saída USB-MIDI, sem sintetizador no Android. Padrões de catálogo, não durações de partituras. " +
   "Prática só compara classes de altura, sem ritmo, ordem, oitava ou dedilhado.",11,false);
  caveat.setPadding(0,dp(8),0,0);item(body,caveat,-1,-2);
  setLearn(false);setAssembling(false);
 }
 private void shift(int amount){
  keyboard.shift(amount);rangeLabel.setText(keyboard.rangeLabel());keyboard.clear();
 }
 private void setAssembling(boolean value){
  if(assembling!=value)stopDemonstration();
  if(assembling!=value){
   rollingPatterns.clear();
   if(!recordingSequence&&!learn)sequenceStatus.setText("Reconhecimento livre: aguardando sequência completa.");
  }
  assembling=value;keyboard.setAssembling(value);
  touchButton.setEnabled(value);assembleButton.setEnabled(!value);
  refresh();
 }
 private void setLearn(boolean value){
  stopDemonstration();
  learn=value;practicing=false;recordingSequence=false;rollingPatterns.clear();
  learner.setVisibility(learn?VISIBLE:GONE);
  sequenceTools.setVisibility(learn?GONE:VISIBLE);sequenceButton.setText("Iniciar sequência");
  discoverButton.setEnabled(learn);learnButton.setEnabled(!learn);
  heading.setText(learn?"Aprender · catálogo musical":"Descobrir · o que estou tocando?");
  description.setText(learn?"Busque notas, intervalos, acordes, escalas e arpejos. Veja as notas e pratique livremente.":
   "Toque notas no piano MIDI ou no teclado virtual; acordes completos são identificados.");
  refresh();
 }
 /** MIDI attack arrives independently of the set of currently held notes. */
 void onMidiAttack(int pitch){
  if(getVisibility()!=VISIBLE||learn)return;
  if(recordingSequence)addSequenceAttack(pitch);
  else observeSpontaneous(pitch,MusicPatternTracker.isIsolated(pitch,midi,virtual));
 }
 private void observeSpontaneous(int pitch,boolean isolated){
  List<MusicSequenceAnalyzer.Finding> findings=rollingPatterns.attack(pitch,isolated);
  if(!findings.isEmpty()){
   StringBuilder sb=new StringBuilder("Reconhecimento livre: ");
   for(MusicSequenceAnalyzer.Finding f:findings){
    if(sb.length()>"Reconhecimento livre: ".length())sb.append(" | ");
    sb.append(f.display());
   }
   sequenceStatus.setText(sb.toString());
  }else if(!isolated){
   sequenceStatus.setText("Notas sobrepostas: sequência indeterminada. Para reconhecer automaticamente, toque notas isoladas.");
  }else{
   sequenceStatus.setText("Reconhecimento livre: "+rollingPatterns.size()+" ataques recentes; sem padrão completo neste momento.");
  }
 }
 /** Clear automatic and delimited tracking when reopening the Laboratory. */
 void beginObservation(){
  recordingSequence=false;sequenceAttacks.clear();rollingPatterns.clear();
  sequenceButton.setText("Iniciar sequência");
  sequenceStatus.setText("Reconhecimento livre: aguardando sequência completa.");
 }
 private void addSequenceAttack(int pitch){
  if(sequenceAttacks.size()>=40){sequenceStatus.setText("Limite de 40 ataques. Toque Concluir análise.");return;}
  sequenceAttacks.add(pitch);
  sequenceStatus.setText("Sequência: "+sequenceAttacks.size()+" notas · última "+MusicTheory.noteName(pitch));
 }
 private void toggleSequence(){
  if(!recordingSequence){
   sequenceAttacks.clear();rollingPatterns.clear();recordingSequence=true;
   sequenceButton.setText("Concluir análise");
   sequenceStatus.setText("Gravando ataques… Toque uma escala ou arpejo completo.");
   return;
  }
  recordingSequence=false;sequenceButton.setText("Iniciar sequência");
  List<MusicSequenceAnalyzer.Finding> findings=MusicSequenceAnalyzer.analyze(sequenceAttacks);
  if(sequenceAttacks.isEmpty())sequenceStatus.setText("Nenhuma nota registrada. Tente novamente.");
  else if(findings.isEmpty())sequenceStatus.setText("Sequência de "+sequenceAttacks.size()+
    " notas: sem correspondência exata neste catálogo; não é possível afirmar escala ou arpejo.");
  else{
   StringBuilder result=new StringBuilder("Padrão compatível com ");
   for(MusicSequenceAnalyzer.Finding f:findings){
    if(result.length()>"Padrão compatível com ".length())result.append(" | ");
    result.append(f.display());
   }
   sequenceStatus.setText(result.toString());
  }
 }
 void onMidiPressed(Set<Integer> notes){
  midi.clear();midi.addAll(notes);refresh();
 }
 private void refresh(){
  if(keyboard==null)return;
  Set<Integer> held=new HashSet<>(midi);held.addAll(virtual);
  Set<Integer> shown=new HashSet<>(held);shown.addAll(demoSession.heldNotes());
  MusicLabCatalog.Entry chosen=currentEntry();
  int[] expected=learn&&chosen!=null?chosen.notes:new int[0];
  keyboard.setNotes(shown,expected);
  rangeLabel.setText(keyboard.rangeLabel());
  if(learn){
   if(chosen==null){
    readout.setText("Nenhum item encontrado");detail.setText("Altere a busca para selecionar um exercício.");
    practiceButton.setText("Praticar");practiceButton.setEnabled(false);
    practiceResult.setText("");return;
   }
   practiceButton.setEnabled(true);
   readout.setText(chosen.title);
   StringBuilder sb=new StringBuilder(chosen.description).append(" · Notas: ");
   for(int i=0;i<chosen.notes.length;i++){
    if(i>0)sb.append(", ");
    sb.append(MusicTheory.noteName(chosen.notes[i])).append(" (").append(MusicTheory.internationalName(chosen.notes[i])).append(")");
   }
   detail.setText(sb.toString());
   practiceButton.setText(practicing?"Encerrar prática":"Praticar");
   if(practicing){
    Set<Integer> desired=new HashSet<>(),actual=new HashSet<>();
    for(int n:chosen.notes)desired.add(n%12);for(int n:held)actual.add(n%12);
    Set<Integer> missing=new HashSet<>(desired),extra=new HashSet<>(actual);
    missing.removeAll(actual);extra.removeAll(desired);
    practiceResult.setText(missing.isEmpty()&&extra.isEmpty()?" ✓ Classes de altura correspondentes (sem ritmo/ordem/oitava)!":
     missing.size()+" classes de altura faltando · "+extra.size()+" extras · sem ritmo/ordem/oitava");
   }else practiceResult.setText("Escolha Praticar para conferir suas notas");
  }else{
   if(held.isEmpty()){readout.setText("Toque uma nota ou acorde");detail.setText("Aguardando notas…");return;}
   TreeSet<Integer> notes=new TreeSet<>(held);StringBuilder sb=new StringBuilder();
   for(int n:notes){
    if(sb.length()>0)sb.append(" · ");
    sb.append(MusicTheory.noteName(n)).append(" / ").append(MusicTheory.internationalName(n));
   }
   readout.setText(sb.toString());
   if(notes.size()==2)detail.setText("Intervalo: "+MusicTheory.intervalName(notes.last()-notes.first()));
   else{
    List<MusicTheory.Chord> found=MusicTheory.recognize(notes);
    if(found.isEmpty())detail.setText(notes.size()==1?"Nota identificada":"Sem acorde completo identificado");
    else{
     MusicTheory.Chord chord=found.get(0);
     detail.setText("Acorde: "+chord.symbol+" — "+chord.description+" · "+chord.inversion+
      (found.size()>1?" · Outras interpretações possíveis: "+alternatives(found):""));
    }
   }
  }
 }
 /** Running remains true between attacks and across repeated iterations. */
 private void toggleDemo(){
  if(demoSession.isPlaying()){stopDemonstration();return;}
  MusicLabCatalog.Entry item=currentEntry();
  if(!learn||item==null)return;
  if(activity.outputPort==null){
   activity.message("Demonstração requer saída USB-MIDI no piano conectado; o Android não sintetiza áudio.");
   return;
  }
  final List<MusicLabPlaybackPlan.Event> events;
  try{events=MusicLabPlaybackPlan.build(item.notes,item.kind,demoSpeed);}
  catch(IllegalArgumentException ex){activity.message(ex.getMessage());return;}
  int generation=demoSession.start();
  demoButton.setText("■ Parar demo");
  scheduleDemo(events,generation);
 }
 private void scheduleDemo(List<MusicLabPlaybackPlan.Event> events,int generation){
  for(MusicLabPlaybackPlan.Event event:events)activity.handler.postDelayed(()->{
   if(!demoSession.isCurrent(generation))return;
   if(getVisibility()!=VISIBLE||!activity.foreground||activity.outputPort==null){stopDemonstration();return;}
   if(!demoSession.apply(generation,event))return;
   activity.send(event.note,event.on);
   refresh();
  },event.atMillis);
  activity.handler.postDelayed(()->{
   if(!demoSession.isCurrent(generation))return;
   if(getVisibility()!=VISIBLE||!activity.foreground||activity.outputPort==null){stopDemonstration();return;}
   if(demoSession.shouldRepeat(generation,demoRepeat.isChecked()))scheduleDemo(events,generation);
   else stopDemonstration();
  },MusicLabPlaybackPlan.endMillis(events)+250);
 }
 void stopDemonstration(){
  Set<Integer> held=demoSession.cancel();
  for(int pitch:held)activity.send(pitch,false);
  if(demoButton!=null)demoButton.setText("▶ Demonstrar");
  refresh();
 }
 private static String alternatives(List<MusicTheory.Chord> found){
  StringBuilder b=new StringBuilder();
  for(int i=1;i<found.size()&&i<=3;i++){
   if(i>1)b.append(", ");
   b.append(found.get(i).symbol);
  }
  return b.toString();
 }
}

/** Canvas keyboard with real multitouch and latched explore mode. */
final class MusicLabKeyboard extends View {
 interface Listener {void changed(Set<Integer> notes);}
 private final Listener listener;
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Map<Integer,Integer> pointerNotes=new HashMap<>();
 private final Set<Integer> latched=new HashSet<>(),held=new HashSet<>(),target=new HashSet<>();
 private boolean assembling=false;
 private int start=48; // C3..C6, shiftable by octaves.
 MusicLabKeyboard(Context context,Listener callback){
  super(context);listener=callback;setBackgroundColor(Color.WHITE);
  setContentDescription("Teclado musical interativo, três oitavas; deslize ou toque nas teclas");
 }
 String rangeLabel(){return MusicTheory.internationalName(start)+"–"+MusicTheory.internationalName(start+36);}
 void shift(int octaves){start=Math.max(24,Math.min(72,start+octaves));invalidate();}
 void clear(){pointerNotes.clear();latched.clear();emit();}
 void setAssembling(boolean enabled){assembling=enabled;clear();}
 void setNotes(Set<Integer> input,int[] reference){
  held.clear();held.addAll(input);target.clear();for(int n:reference)target.add(n);invalidate();
 }
 private static boolean black(int n){int p=n%12;return p==1||p==3||p==6||p==8||p==10;}
 private int whiteCount(){int count=0;for(int n=start;n<=start+36;n++)if(!black(n))count++;return count;}
 private float x0(){return getResources().getDisplayMetrics().density*18;}
 private float keyTop(){return getHeight()*.56f;}
 private float keyWidth(){return (getWidth()-2*x0())/whiteCount();}
 private int noteAt(float x,float y){
  if(y<keyTop()||y>getHeight()||x<x0()||x>=getWidth()-x0())return -1;
  float kw=keyWidth();int whites=0;
  if(y<keyTop()+(getHeight()-keyTop())*.62f){
   for(int n=start;n<=start+36;n++){
    if(!black(n)){whites++;continue;}
    float bw=kw*.62f,left=x0()+whites*kw-bw*.5f;
    if(x>=left&&x<left+bw)return n;
   }
  }
  int index=(int)((x-x0())/kw),w=0;
  for(int n=start;n<=start+36;n++)if(!black(n)&&w++==index)return n;
  return -1;
 }
 private void emit(){
  Set<Integer> next=new HashSet<>();
  if(assembling)next.addAll(latched);
  else next.addAll(pointerNotes.values());
  next.remove(-1);
  listener.changed(next);invalidate();
 }
 @Override public boolean onTouchEvent(android.view.MotionEvent event){
  int action=event.getActionMasked(),idx=event.getActionIndex(),id=event.getPointerId(idx);
  if(action==MotionEvent.ACTION_DOWN&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
  if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
   int note=noteAt(event.getX(idx),event.getY(idx));
   if(note>=0){
    if(assembling){if(!latched.add(note))latched.remove(note);}
    else pointerNotes.put(id,note);
    emit();
   }
   return true;
  }
  if(action==MotionEvent.ACTION_MOVE&&!assembling){
   pointerNotes.clear();
   for(int i=0;i<event.getPointerCount();i++){
    int note=noteAt(event.getX(i),event.getY(i));
    if(note>=0)pointerNotes.put(event.getPointerId(i),note);
   }
   emit();return true;
  }
  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){
   pointerNotes.remove(id);if(!assembling)emit();return true;
  }
  if(action==MotionEvent.ACTION_CANCEL){pointerNotes.clear();if(!assembling)emit();return true;}
  return true;
 }
 private void stroke(Canvas c,float x,float y,float endX,float endY,int color,float width){
  paint.setColor(color);paint.setStrokeWidth(width);paint.setStyle(Paint.Style.STROKE);
  c.drawLine(x,y,endX,endY,paint);paint.setStyle(Paint.Style.FILL);
 }
 private void drawString(Canvas c,String s,float x,float y,int size,int color){
  paint.setColor(color);paint.setTextSize(size*getResources().getDisplayMetrics().scaledDensity);
  paint.setTypeface(Typeface.DEFAULT);c.drawText(s,x,y,paint);
 }
 private int degree(int midi){
  int[] k={0,0,1,1,2,3,3,4,4,5,5,6};
  return (midi/12-1)*7+k[midi%12];
 }
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);float w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
  float d=getResources().getDisplayMetrics().density,space=8*d,base1=14*d,base2=70*d;
  drawString(c,"Sol",4*d,base1+space*2,10,0xff3C526D);
  drawString(c,"Fá",4*d,base2+space*2,10,0xff3C526D);
  float left=34*d,right=w-8*d;
  for(int i=0;i<5;i++){
   stroke(c,left,base1+i*space,right,base1+i*space,0xff9BAFC4,d);
   stroke(c,left,base2+i*space,right,base2+i*space,0xff9BAFC4,d);
  }
  TreeSet<Integer> preview=new TreeSet<>(target);preview.addAll(held);
  int index=0;
  for(int n:preview){
   boolean isTreble=n>=60;float base=isTreble?base1:base2;
   int bottomDegree=degree(isTreble?64:43); // E4 and G2 are bottom staff lines.
   float y=base+4*space-(degree(n)-bottomDegree)*space*.5f;
   float x=left+17*d+(index++%16)*18*d;
   if(y<2*d||y>keyTop()-3*d)continue;
   int color=held.contains(n)?0xff087E6B:0xffC68A2B;
   if((n%12)==1||(n%12)==3||(n%12)==6||(n%12)==8||(n%12)==10)
    drawString(c,"♯",x-11*d,y+3*d,9,color);
   paint.setColor(color);c.drawOval(x-6*d,y-4*d,x+6*d,y+4*d,paint);
  }
  float top=keyTop(),bottom=h-4*d,kw=keyWidth(),startX=x0();
  int white=0;
  for(int n=start;n<=start+36;n++){
   if(black(n))continue;
   float x=startX+white++*kw;
   paint.setColor(held.contains(n)?0xff8FE0CA:target.contains(n)?0xffF7D383:0xffF9FCFF);
   c.drawRect(x+1,top,x+kw-1,bottom,paint);
   paint.setStyle(Paint.Style.STROKE);paint.setColor(0xff738AA0);
   c.drawRect(x+1,top,x+kw-1,bottom,paint);paint.setStyle(Paint.Style.FILL);
   if(n%12==0&&kw>=20*d)drawString(c,"C"+(n/12-1),x+2*d,bottom-6*d,8,0xff304C6B);
  }
  white=0;
  for(int n=start;n<=start+36;n++){
   if(!black(n)){white++;continue;}
   float bw=kw*.62f,x=startX+white*kw-bw*.5f;
   paint.setColor(held.contains(n)?0xff087E6B:target.contains(n)?0xffB77E20:0xff25364B);
   c.drawRoundRect(x,top,x+bw,top+(bottom-top)*.62f,2*d,2*d,paint);
  }
 }
}

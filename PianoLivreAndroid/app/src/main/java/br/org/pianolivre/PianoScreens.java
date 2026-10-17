package br.org.pianolivre;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.io.File;
import java.util.Locale;

/** Native responsive landscape screens: library home and dedicated study view. */
class PianoScreens {
 final MainActivity a;
 final int navy=0xff101C31, ink=0xff172A44, muted=0xff57708A, blue=0xff286BCB, pale=0xffEEF4FA, teal=0xff0C8A78;
 ScrollView library;LinearLayout libraryInner,grid,studio,freeRoot,header,studyTitlebar;HorizontalScrollView studyControlsBar;TextView songTitle,modeNote,tempoLabel,freeTitle,waitFeedback;
 int feedbackSequence=0;
 Button[] modeButtons=new Button[3],handButtons=new Button[3];
 Button playback,keyboardToggle,loopButton;
 boolean editingLoop=false;
 EditText librarySearch;Spinner libraryFilter;TextView libraryResult;
 String libraryQuery="",selectedLibraryFilter="";int libraryUser=-1;boolean libraryFiltersDirty=true;
 NotationView notation,freeNotation;
 MusicLabScreen lab;
 PianoView virtualKeys,legacyFallback;
 PianoScreens(MainActivity a){this.a=a;}
 int d(float n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
 GradientDrawable bg(int color,int radius,int stroke){
  GradientDrawable x=new GradientDrawable();x.setColor(color);x.setCornerRadius(d(radius));
  if(stroke!=0)x.setStroke(d(1),stroke);return x;
 }
 TextView text(String value,int size,int color,boolean bold){
  TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(color);
  t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(null,Typeface.BOLD);return t;
 }
 LinearLayout column(){LinearLayout x=new LinearLayout(a);x.setOrientation(1);return x;}
 LinearLayout row(){LinearLayout x=new LinearLayout(a);x.setOrientation(0);x.setGravity(Gravity.CENTER_VERTICAL);return x;}
 Button btn(String name,boolean primary,Runnable action){
  Button b=new Button(a);b.setText(name);b.setTextSize(13);b.setAllCaps(false);b.setTypeface(null,Typeface.BOLD);
  b.setTextColor(primary?Color.WHITE:ink);b.setBackground(bg(primary?blue:0xffF1F5FA,12,primary?0:0xffD9E5F1));
  b.setPadding(d(14),0,d(14),0);b.setMinHeight(d(46));b.setMinimumHeight(d(46));
  b.setOnClickListener(v->action.run());b.setElevation(d(1));
  return b;
 }
 Button compact(String name,boolean primary,Runnable action){
  Button b=btn(name,primary,action);
  b.setTextSize(11);b.setMinHeight(0);b.setMinimumHeight(0);
  b.setPadding(d(8),0,d(8),0);b.setElevation(0);
  return b;
 }
 LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:d(w),h<0?h:d(h));}
 void addGap(LinearLayout l,int w){View v=new View(a);l.addView(v,lp(w,1));}
 void build(){
  a.root=column();a.root.setBackgroundColor(pale);a.setContentView(a.root);
  header=row();header.setBackgroundColor(navy);header.setPadding(d(13),0,d(13),0);
  a.root.addView(header,lp(-1,45));
  TextView brand=text("♫  PIANO LIVRE",16,Color.WHITE,true);header.addView(brand,new LinearLayout.LayoutParams(0,-1,1));
  a.status=text("Sem MIDI conectado",10,0xffC0D6EF,false);a.status.setSingleLine(true);
  a.status.setEllipsize(TextUtils.TruncateAt.END);
  a.status.setContentDescription("Abrir diagnóstico MIDI");
  a.status.setClickable(true);a.status.setOnClickListener(v->a.showMidiDiagnostics());
  header.addView(a.status,lp(140,-1));
  addGap(header,7);
  TextView userLabel=text("PERFIL",11,0xffB7CBDE,true);header.addView(userLabel,lp(43,-1));
  a.profiles=new Spinner(a);a.profiles.setBackground(bg(0xffEDF4FA,10,0));header.addView(a.profiles,lp(105,34));
  addGap(header,5);
  header.addView(compact("+",false,()->a.newProfile()),lp(35,34));addGap(header,5);
  header.addView(compact("USB-MIDI",false,()->a.selectMidi()),lp(90,34));

  FrameLayout screens=new FrameLayout(a);
  a.root.addView(screens,new LinearLayout.LayoutParams(-1,0,1));
  buildLibrary();screens.addView(library,new FrameLayout.LayoutParams(-1,-1));
  buildStudio();screens.addView(studio,new FrameLayout.LayoutParams(-1,-1));
  buildFree();screens.addView(freeRoot,new FrameLayout.LayoutParams(-1,-1));
  lab=new MusicLabScreen(a,()->showHome());screens.addView(lab,new FrameLayout.LayoutParams(-1,-1));
  showHome();
 }
 void buildLibrary(){
  library=new ScrollView(a);library.setFillViewport(true);
  libraryInner=column();libraryInner.setPadding(d(12),d(9),d(12),d(12));library.addView(libraryInner);
  LinearLayout hero=row();hero.setPadding(d(13),d(8),d(13),d(8));hero.setBackground(bg(0xffE1ECFB,18,0xffC9DDF6));
  LinearLayout heroText=column();hero.addView(heroText,new LinearLayout.LayoutParams(0,-2,1));
  TextView eyebrow=text("SEU ESPAÇO DE ESTUDO",11,teal,true);heroText.addView(eyebrow,lp(-1,18));
  heroText.addView(text("Escolha uma música e comece a tocar",17,ink,true),lp(-1,-2));
  TextView sub=text("Biblioteca local · estudo individual · sem internet",12,muted,false);
  sub.setPadding(0,d(2),0,0);heroText.addView(sub,lp(-1,-2));
  hero.addView(btn("▶  Começar pela Ode à Alegria",true,()->a.loadScore("scores/ode-to-joy-ludwig-van-beethoven.mxl",true)),lp(212,39));
  libraryInner.addView(hero,lp(-1,-2));
  LinearLayout section=row();section.setPadding(0,d(8),0,d(5));
  section.addView(text("BIBLIOTECA MUSICAL",14,ink,true),new LinearLayout.LayoutParams(0,d(28),1));
  section.addView(btn("Importar partitura",false,()->a.importScore()),lp(147,36));addGap(section,8);
  section.addView(btn("Laboratório",true,()->showLab()),lp(118,36));addGap(section,8);
  section.addView(btn("Histórico",false,()->a.showHistory()),lp(87,36));addGap(section,8);
  section.addView(btn("Opções",false,()->options()),lp(73,36));
  libraryInner.addView(section,lp(-1,-2));
  LinearLayout search=row();
  librarySearch=new EditText(a);librarySearch.setSingleLine(true);librarySearch.setTextSize(14);librarySearch.setHint("Buscar título ou arquivo");
  search.addView(librarySearch,new LinearLayout.LayoutParams(0,d(36),1));addGap(search,8);
  libraryFilter=new Spinner(a);search.addView(libraryFilter,lp(145,36));addGap(search,8);
  search.addView(btn("Coleções",false,()->manageCollections()),lp(97,36));libraryInner.addView(search,lp(-1,40));
  libraryResult=text("",11,muted,false);libraryInner.addView(libraryResult,lp(-1,20));
  librarySearch.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence t,int start,int count,int after){}
   public void onTextChanged(CharSequence t,int start,int before,int count){}
   public void afterTextChanged(android.text.Editable value){libraryQuery=value.toString();refreshLibrary();}
  });
  grid=column();libraryInner.addView(grid,lp(-1,-2));
  TextView footer=text("DICA  ·  Em um emulador, teste a biblioteca e o modo contínuo. Para reconhecer suas teclas, conecte o Yamaha P-45 a um Android compatível com USB-MIDI.",12,muted,false);
  footer.setPadding(d(6),d(9),0,d(4));libraryInner.addView(footer,lp(-1,-2));
 }
 void refreshLibrary(){
  if(grid==null)return;grid.removeAllViews();
  final LibraryStore.Snapshot metadata;
  try{metadata=new LibraryStore(a.prefs,a.user).snapshot();refreshLibraryFilters(metadata);}
  catch(RuntimeException ex){grid.addView(text("Não foi possível ler a organização da biblioteca. Os dados não foram alterados.",13,ink,false),lp(-1,-2));return;}
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);
  int columns=LibraryGridLayout.columnsForWidthDp(widthDp);
  java.util.ArrayList<String[]> entries=new java.util.ArrayList<>();
  for(int i=0;i<a.catalog.length();i++){
   JSONObject j=a.catalog.optJSONObject(i);if(j!=null)entries.add(new String[]{j.optString("name"),"scores/"+j.optString("file")});
  }
  File dir=new File(a.getFilesDir(),"imported");File[] imports=dir.listFiles();
  if(imports!=null)for(File f:imports)if(f.isFile()&&!f.getName().endsWith(".source.mid"))entries.add(new String[]{f.getName(),f.getAbsolutePath()});
  int total=entries.size();
  entries.removeIf(entry->{
   String key=LibrarySearch.key(entry[1]);
   if(!LibrarySearch.matches(entry[0],libraryQuery))return true;
   if(selectedLibraryFilter.equals("*"))return !metadata.favorites.contains(key);
   LibraryStore.Collection collection=metadata.collections.get(selectedLibraryFilter);
   return collection!=null&&!collection.scores.contains(key);
  });
  libraryResult.setText(entries.size()+" de "+total+" partituras");
  if(entries.isEmpty())grid.addView(text("Nenhuma partitura neste filtro. Ajuste a busca ou escolha Todas.",13,muted,false),lp(-1,44));
  for(int i=0;i<entries.size();i+=columns){
   LinearLayout row=row();grid.addView(row,lp(-1,99));
   for(int k=0;k<columns;k++){
    if(i+k>=entries.size()){View spacer=new View(a);row.addView(spacer,new LinearLayout.LayoutParams(0,d(92),1));continue;}
    String[] entry=entries.get(i+k);
    LinearLayout card=column();card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(d(9),d(4),d(8),d(4));
    card.setBackground(bg(Color.WHITE,16,0xffDBE5F0));card.setElevation(d(2));
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(92),1);p.setMargins(d(3),d(2),d(7),d(3));row.addView(card,p);
    TextView name=text("♫  "+entry[0],13,ink,true);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);
    LinearLayout title=row();title.addView(name,new LinearLayout.LayoutParams(0,-2,1));
    final String scoreKey=LibrarySearch.key(entry[1]);
    Button favorite=compact(metadata.favorites.contains(scoreKey)?"★":"☆",false,()->libraryAction(()->new LibraryStore(a.prefs,a.user).toggleFavorite(scoreKey)));
    favorite.setContentDescription((metadata.favorites.contains(scoreKey)?"Remover favorito ":"Favoritar ")+entry[0]);
    title.addView(favorite,lp(32,29));card.addView(title,lp(-1,31));
    LinearLayout actions=row();
    TextView study=text("Estudar  ›",11,blue,true);actions.addView(study,new LinearLayout.LayoutParams(0,d(34),1));
    TextView free=text("Partitura livre  ↗",10,teal,true);free.setGravity(Gravity.CENTER_VERTICAL);
    free.setContentDescription("Partitura Livre "+entry[0]);
    actions.addView(free,lp(102,34));
    free.setOnClickListener(v->a.openFreeScore(entry[1]));
    card.addView(actions,lp(-1,-2));
    card.setContentDescription("Estudar "+entry[0]);card.setClickable(true);card.setFocusable(true);
    card.setOnClickListener(v->a.loadScore(entry[1],true));
    card.setOnLongClickListener(v->{organizeScore(entry[0],scoreKey);return true;});
   }
  }
 }
 void refreshLibraryFilters(LibraryStore.Snapshot metadata){
  if(libraryUser!=a.user){libraryUser=a.user;selectedLibraryFilter="";libraryFiltersDirty=true;}
  if(!libraryFiltersDirty)return;libraryFiltersDirty=false;
  java.util.List<String> ids=new java.util.ArrayList<>(),names=new java.util.ArrayList<>();
  ids.add("");names.add("Todas");ids.add("*");names.add("Favoritos");
  for(LibraryStore.Collection c:metadata.collections.values()){ids.add(c.id);names.add("Coleção: "+c.name);}
  int selected=ids.indexOf(selectedLibraryFilter);if(selected<0){selected=0;selectedLibraryFilter="";}
  libraryFilter.setOnItemSelectedListener(null);
  libraryFilter.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));libraryFilter.setSelection(selected);
  libraryFilter.setOnItemSelectedListener(new MainActivity.Listener(index->{
   if(index>=0&&index<ids.size()&&!selectedLibraryFilter.equals(ids.get(index))){selectedLibraryFilter=ids.get(index);refreshLibrary();}
  }));
 }
 void libraryAction(Runnable action){
  try{action.run();libraryFiltersDirty=true;refreshLibrary();}catch(RuntimeException ex){a.message(ex.getMessage());}
 }
 void collectionName(String title,String initial,java.util.function.Consumer<String> action){
  EditText name=new EditText(a);name.setSingleLine(true);name.setText(initial);
  new AlertDialog.Builder(a).setTitle(title).setView(name).setPositiveButton("Salvar",(d,w)->libraryAction(()->action.accept(name.getText().toString())))
   .setNegativeButton("Cancelar",null).show();
 }
 void manageCollections(){
  new AlertDialog.Builder(a).setTitle("Coleções").setItems(new String[]{"Nova coleção","Renomear coleção","Excluir coleção"},(dialog,which)->{
   if(which==0){collectionName("Nova coleção","",name->new LibraryStore(a.prefs,a.user).create(name));return;}
   try{
    java.util.List<LibraryStore.Collection> list=new java.util.ArrayList<>(new LibraryStore(a.prefs,a.user).snapshot().collections.values());
    if(list.isEmpty()){a.message("Crie uma coleção primeiro. Para organizar uma partitura, mantenha seu cartão pressionado.");return;}
    String[] names=new String[list.size()];for(int i=0;i<names.length;i++)names[i]=list.get(i).name;
    new AlertDialog.Builder(a).setTitle(which==1?"Renomear coleção":"Excluir coleção").setItems(names,(d,index)->{
     LibraryStore.Collection selected=list.get(index);
     if(which==1)collectionName("Renomear coleção",selected.name,name->new LibraryStore(a.prefs,a.user).rename(selected.id,name));
     else new AlertDialog.Builder(a).setTitle("Excluir coleção?").setMessage("As partituras continuarão na biblioteca.")
      .setPositiveButton("Excluir",(dd,w)->libraryAction(()->new LibraryStore(a.prefs,a.user).delete(selected.id))).setNegativeButton("Cancelar",null).show();
    }).show();
   }catch(RuntimeException ex){a.message(ex.getMessage());}
  }).show();
 }
 void organizeScore(String title,String scoreKey){
  try{
   java.util.List<LibraryStore.Collection> list=new java.util.ArrayList<>(new LibraryStore(a.prefs,a.user).snapshot().collections.values());
   if(list.isEmpty()){a.message("Crie uma coleção pelo botão Coleções e depois mantenha uma partitura pressionada para adicioná-la.");return;}
   String[] names=new String[list.size()];for(int i=0;i<names.length;i++)names[i]=(list.get(i).scores.contains(scoreKey)?"Remover de: ":"Adicionar a: ")+list.get(i).name;
   new AlertDialog.Builder(a).setTitle(title).setItems(names,(d,index)->libraryAction(()->new LibraryStore(a.prefs,a.user).toggleMember(list.get(index).id,scoreKey))).show();
  }catch(RuntimeException ex){a.message(ex.getMessage());}
 }
 void options(){
  new AlertDialog.Builder(a).setTitle("Ferramentas")
   .setItems(new String[]{"Fazer backup","Restaurar backup","Importar MusicXML","Calibrar acordes MIDI","Preparação do Espera","Sobre esta versão","Gerenciar perfil"},(dialog,i)->{
    if(i==0)a.exportBackup();else if(i==1)a.restoreBackup();else if(i==2)a.importScore();
    else if(i==3)a.showChordSettings();
    else if(i==4)a.configureWaitPreparation();
    else if(i==6)a.manageProfile();
    else {
     String version="beta";
     try{version=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;}
     catch(Exception ignored){}
     a.message("Piano Livre "+version+". Claves Sol/Fá revisadas, armaduras e quiálteras; acordes calibráveis no Yamaha.");
    }
   }).show();
 }
 void buildStudio(){
  studio=column();studio.setBackgroundColor(pale);studio.setVisibility(View.GONE);
  studyTitlebar=row();LinearLayout titlebar=studyTitlebar;titlebar.setPadding(d(10),0,d(12),0);titlebar.setBackgroundColor(Color.WHITE);
  studio.addView(titlebar,lp(-1,40));
  titlebar.addView(compact("← Biblioteca",false,()->showHome()),lp(115,32));addGap(titlebar,12);
  songTitle=text("Selecione uma partitura",16,ink,true);songTitle.setSingleLine(true);songTitle.setEllipsize(TextUtils.TruncateAt.END);
  titlebar.addView(songTitle,new LinearLayout.LayoutParams(0,-1,1));
  titlebar.addView(compact("Partitura Livre",false,()->showFree()),lp(122,32));addGap(titlebar,10);
  a.info=text("",11,muted,false);titlebar.addView(a.info,lp(203,-1));
  LinearLayout controls=row();controls.setPadding(d(10),0,d(10),0);controls.setBackgroundColor(0xffE8F0F9);
  studio.addView(controls,lp(-1,42));
  controls.addView(text("MODO",10,muted,true),lp(40,-1));
  String[] modes={"Espera","Contínuo","Demonstração"};
  for(int i=0;i<3;i++){final int idx=i;modeButtons[i]=compact(modes[i],false,()->{
    a.stopPlayback();a.finishStudyWindow();a.mode=idx;android.util.Log.i("PianoLivreUI","mode="+idx);
    if(idx==0)a.prepareWaiting();updateSelections();a.saveSession();a.beginStudyWindow();
  });LinearLayout.LayoutParams p=lp(i==2?112:82,34);p.rightMargin=d(4);controls.addView(modeButtons[i],p);}
  modeButtons[0].setOnLongClickListener(v->{a.showChordSettings();return true;});
  addGap(controls,13);controls.addView(text("MÃOS",10,muted,true),lp(40,-1));
  String[] hands={"Ambas","Direita","Esquerda"};
  for(int i=0;i<3;i++){final int idx=i;handButtons[i]=compact(hands[i],false,()->{
   a.selectHand(idx);updateSelections();
  });LinearLayout.LayoutParams p=lp(75,34);p.rightMargin=d(4);controls.addView(handButtons[i],p);}
  studyControlsBar=new HorizontalScrollView(a);studyControlsBar.setHorizontalScrollBarEnabled(true);
  studio.removeView(controls);studio.addView(studyControlsBar,lp(-1,42));studyControlsBar.addView(controls,new android.widget.HorizontalScrollView.LayoutParams(-2,d(42)));
  FrameLayout notationArea=new FrameLayout(a);
  legacyFallback=new PianoView(a);
  a.scoreView=legacyFallback;
  notationArea.addView(legacyFallback,new FrameLayout.LayoutParams(-1,-1));
  notation=new NotationView(a,new NotationView.Listener(){
   public void onRendered(){
    legacyFallback.setVisibility(View.INVISIBLE);
    followScore(a.position,true);
    android.util.Log.i("PianoLivreNotation","NOTATION_RENDERED");
   }
   public void onError(String cause){
    notation.setVisibility(View.INVISIBLE);
    legacyFallback.setVisibility(View.VISIBLE);
    android.util.Log.w("PianoLivreNotation","Notation fallback: "+cause);
   }
   @Override public void onLoopSelected(double start,double end){
    editingLoop=false;
    a.applyLoopSelection(start,end);
    notation.setLoopEdit(false);
    updateSelections();
   }
  });
  notationArea.addView(notation,new FrameLayout.LayoutParams(-1,-1));
  waitFeedback=text("",12,0xff614105,true);
  waitFeedback.setBackground(bg(0xffffefc7,10,0xffefd9a1));
  waitFeedback.setPadding(d(12),d(6),d(12),d(6));
  waitFeedback.setVisibility(View.GONE);
  FrameLayout.LayoutParams feedbackPlacement=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);
  feedbackPlacement.setMargins(d(8),d(8),d(16),d(8));
  notationArea.addView(waitFeedback,feedbackPlacement);
  studio.addView(notationArea,new LinearLayout.LayoutParams(-1,0,1));
  virtualKeys=new PianoView(a);
  virtualKeys.keyboardOnly=true;
  virtualKeys.hand=0; // Visual accompaniment never follows the learning hand filter.
  virtualKeys.setVisibility(View.GONE);
  studio.addView(virtualKeys,lp(-1,105));
  LinearLayout bottom=row();bottom.setPadding(d(9),d(3),d(9),d(3));bottom.setBackgroundColor(Color.WHITE);
  studio.addView(bottom,lp(-1,46));
  bottom.addView(compact("|◀ Início",false,()->a.seekPrevious()),lp(85,37));addGap(bottom,6);
  playback=compact("▶  Iniciar",true,()->{a.toggle();updateSelections();});
  playback.setOnLongClickListener(v->{if(a.mode==0){a.configureWaitPreparation();return true;}return false;});
  bottom.addView(playback,lp(114,37));addGap(bottom,6);
  bottom.addView(compact("■ Parar",false,()->{a.stopPlayback();updateSelections();}),lp(77,37));addGap(bottom,10);
  TextView speed=text("VELOCIDADE",10,muted,true);bottom.addView(speed,lp(77,-1));
  a.tempoBar=new SeekBar(a);a.tempoBar.setMax(175);a.tempoBar.setProgress(75);
  bottom.addView(a.tempoBar,new LinearLayout.LayoutParams(0,d(37),1));
  tempoLabel=text("100%",12,ink,true);tempoLabel.setGravity(Gravity.CENTER);bottom.addView(tempoLabel,lp(52,-1));
  a.tempoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar s,int progress,boolean by){a.tempoFactor=(progress+25)/100.0;tempoLabel.setText((progress+25)+"%");a.updateInfo();}
   public void onStartTrackingTouch(SeekBar s){}
   public void onStopTrackingTouch(SeekBar s){a.saveSession();}
  });
  addGap(bottom,6);
  keyboardToggle=compact("Teclado guia",false,()->setKeyboardOpen(!a.scoreView.showKeyboard));
  bottom.addView(keyboardToggle,lp(133,37));addGap(bottom,6);
  loopButton=compact("Trecho ↻",false,()->{
    if(a.score==null)return;
    editingLoop=!editingLoop;
    if(editingLoop)a.stopPlayback();
    notation.setLoopEdit(editingLoop);
    updateSelections();
  });
  loopButton.setOnLongClickListener(v->{loopMenu();return true;});
  bottom.addView(loopButton,lp(95,37));
  updateSelections();
 }
 /**
  * Teclado expandido: cabeçalho geral, barra da música e modos/mãos ficam
  * ocultos. A barra inferior permanece visível para permitir fechar o teclado
  * e controlar a execução sem interromper a sessão.
  */
 void setKeyboardOpen(boolean open){
  if(a.scoreView==null||virtualKeys==null)return;
  a.scoreView.showKeyboard=open;
  virtualKeys.setVisibility(open?View.VISIBLE:View.GONE);
  virtualKeys.playhead=a.position; // Opening while paused must not show beat zero.
  if(studyTitlebar!=null)studyTitlebar.setVisibility(open?View.GONE:View.VISIBLE);
  if(studyControlsBar!=null)studyControlsBar.setVisibility(open?View.GONE:View.VISIBLE);
  if(header!=null&&studio!=null&&studio.getVisibility()==View.VISIBLE)
   header.setVisibility(open?View.GONE:View.VISIBLE);
  virtualKeys.invalidate();a.scoreView.invalidate();
  updateSelections();
  // WebView must rebuild its vertical staff scale after the new bounds settle.
  if(notation!=null)notation.postDelayed(()->{
   if(studio!=null&&studio.getVisibility()==View.VISIBLE&&notation!=null)notation.resizeToFit();
  },160);
  android.util.Log.i("PianoLivreUI","keyboard="+open+" immersive="+open);
 }
 void loopMenu(){
  new AlertDialog.Builder(a).setTitle("Repetição de trecho")
   .setItems(new String[]{"Marcar início na posição atual","Marcar fim na posição atual","Remover repetição","Voltar ao início"},(dlg,i)->{
    if(i==0)a.loopStart=a.position;else if(i==1)a.loopEnd=a.position;
    else if(i==2){a.clearLoop();return;}
    else {a.seekPrevious();return;}
    if(a.loopEnd<a.loopStart){double tmp=a.loopEnd;a.loopEnd=a.loopStart;a.loopStart=tmp;}
    a.saveSession();a.updateInfo();
   }).show();
 }
 void showWaitFeedback(String message){
  if(waitFeedback==null||!isStudyOpen())return;
  final int serial=++feedbackSequence;
  waitFeedback.setText(message);
  waitFeedback.setVisibility(View.VISIBLE);
  a.handler.postDelayed(()->{
   if(serial==feedbackSequence&&waitFeedback!=null)waitFeedback.setVisibility(View.GONE);
  },2400);
 }
 void updateSelections(){
  for(int i=0;i<3;i++){
   if(modeButtons[i]!=null){boolean on=a.mode==i;modeButtons[i].setBackground(bg(on?blue:Color.WHITE,12,on?0:0xffCCD9E8));modeButtons[i].setTextColor(on?Color.WHITE:ink);}
   if(handButtons[i]!=null){boolean on=a.currentHand==i;handButtons[i].setBackground(bg(on?teal:Color.WHITE,12,on?0:0xffCCD9E8));handButtons[i].setTextColor(on?Color.WHITE:ink);}
  }
  if(playback!=null)playback.setText(a.mode==0?(a.waitPreparing?"Preparando…":a.waitStarted?"♪ Esperando":"▶ Iniciar"):a.running?"❚❚  Pausar":"▶  Iniciar");
  if(keyboardToggle!=null&&a.scoreView!=null)keyboardToggle.setText(a.scoreView.showKeyboard?"Ocultar teclado":"Teclado guia");
  if(loopButton!=null)loopButton.setText(editingLoop?"Selecionando…":"Trecho ↻");
 }
 void buildFree(){
  freeRoot=column();freeRoot.setBackgroundColor(Color.WHITE);freeRoot.setVisibility(View.GONE);
  LinearLayout freeBar=row();freeBar.setPadding(d(12),0,d(12),0);freeBar.setBackgroundColor(0xffF3F7FB);
  freeRoot.addView(freeBar,lp(-1,40));
  freeBar.addView(compact("← Biblioteca",false,()->showHome()),lp(118,32));addGap(freeBar,14);
  freeTitle=text("Partitura Livre",15,ink,true);freeTitle.setSingleLine(true);
  freeTitle.setEllipsize(TextUtils.TruncateAt.END);
  freeBar.addView(freeTitle,new LinearLayout.LayoutParams(0,-1,1));
  freeNotation=new NotationView(a,new NotationView.Listener(){
   public void onRendered(){android.util.Log.i("PianoLivreNotation","FREE_RENDERED");}
   public void onError(String msg){a.message("Erro ao abrir a partitura: "+msg);}
  });
  freeRoot.addView(freeNotation,new LinearLayout.LayoutParams(-1,0,1));
 }
 void stopLabDemonstration(){
  if(lab!=null){lab.stopDemonstration();lab.cancelPractice();}
 }
 void showFree(){
  if(a.score==null)return;
  stopLabDemonstration();
  a.stopPlayback();a.finishStudyWindow();
  if(a.scoreView!=null&&a.scoreView.showKeyboard)setKeyboardOpen(false);
  header.setVisibility(View.GONE);library.setVisibility(View.GONE);studio.setVisibility(View.GONE);
  if(lab!=null)lab.setVisibility(View.GONE);
  freeRoot.setVisibility(View.VISIBLE);
  freeTitle.setText((a.currentName==null?a.score.title:a.currentName)+" · Partitura Livre");
  freeNotation.loadScore(a.score,0,true);
  a.beginStudyWindow();
  android.util.Log.i("PianoLivreUI","free_open");
 }
 void showLab(){
  a.stopPlayback();a.finishStudyWindow();
  if(a.scoreView!=null&&a.scoreView.showKeyboard)setKeyboardOpen(false);
  header.setVisibility(View.VISIBLE);
  library.setVisibility(View.GONE);studio.setVisibility(View.GONE);freeRoot.setVisibility(View.GONE);
  if(lab!=null){lab.beginObservation();lab.onMidiPressed(a.downKeys);lab.setVisibility(View.VISIBLE);}
 }
 void showHome(){
  stopLabDemonstration();
  a.stopPlayback();a.finishStudyWindow();
  if(a.scoreView!=null&&a.scoreView.showKeyboard)setKeyboardOpen(false);
  header.setVisibility(View.VISIBLE);
  freeRoot.setVisibility(View.GONE);
  studio.setVisibility(View.GONE);if(lab!=null)lab.setVisibility(View.GONE);
  library.setVisibility(View.VISIBLE);refreshLibrary();
 }
 void showStudy(){
  if(a.score==null)return;
  stopLabDemonstration();
  header.setVisibility(View.VISIBLE);
  freeRoot.setVisibility(View.GONE);
  library.setVisibility(View.GONE);if(lab!=null)lab.setVisibility(View.GONE);
  studio.setVisibility(View.VISIBLE);
  songTitle.setText(a.currentName==null||a.currentName.isEmpty()?a.score.title:a.currentName);
  // Restores the expected mode if a prior view closed while keyboard was open.
  if(studyTitlebar!=null)studyTitlebar.setVisibility(a.scoreView.showKeyboard?View.GONE:View.VISIBLE);
  if(studyControlsBar!=null)studyControlsBar.setVisibility(a.scoreView.showKeyboard?View.GONE:View.VISIBLE);
  header.setVisibility(a.scoreView.showKeyboard?View.GONE:View.VISIBLE);
  if(virtualKeys!=null){
   virtualKeys.setVisibility(a.scoreView.showKeyboard?View.VISIBLE:View.GONE);
   virtualKeys.playhead=a.position;
   virtualKeys.invalidate();
  }
  if(notation!=null)notation.postDelayed(()->notation.resizeToFit(),180);
  modeNote=null;updateSelections();a.beginStudyWindow();
 }
 void displayScore(Score score,double beat){
  editingLoop=false;updateSelections();
  legacyFallback.setVisibility(View.VISIBLE);
  notation.loadScore(score,beat);
  virtualKeys.score=score;
  virtualKeys.playhead=beat;
  virtualKeys.hand=0;
  virtualKeys.invalidate();
 }
 void clearLoopSelection(){
  editingLoop=false;
  if(notation!=null){notation.setLoopEdit(false);notation.clearLoop();}
  updateSelections();
 }
 void startWaitPreparation(long durationMs){if(notation!=null)notation.startWaitPreparation(durationMs);}
 void stopWaitPreparation(){if(notation!=null)notation.cancelWaitPreparation();}
 void followScore(double beat,boolean smooth){
  if(virtualKeys!=null){
   double previous=virtualKeys.playhead;
   virtualKeys.playhead=beat;
   if(virtualKeys.getVisibility()==View.VISIBLE&&Math.abs(previous-beat)>.00001)
    virtualKeys.invalidate();
  }
  if(notation!=null)notation.seek(beat,smooth,
   a.score==null?100:a.score.tempoAt(beat)*a.tempoFactor,
   a.mode==0?a.waitTraveling:a.running);
 }
 void onLabMidiAttack(int pitch){
  if(lab!=null&&lab.getVisibility()==View.VISIBLE)lab.onMidiAttack(pitch);
 }
 void updatePressed(java.util.Set<Integer> notes){
  if(lab!=null)lab.onMidiPressed(notes);
  if(virtualKeys==null)return;
  virtualKeys.pressed.clear();virtualKeys.pressed.addAll(notes);
  virtualKeys.invalidate();
 }
 void pauseNotation(){if(notation!=null)notation.onPause();if(freeNotation!=null)freeNotation.onPause();}
 void resumeNotation(){
  if(notation!=null)notation.onResume();if(freeNotation!=null)freeNotation.onResume();
  if(isStudyOpen())followScore(a.position,true);
 }
 void destroyNotation(){if(notation!=null){notation.dispose();notation=null;}
  if(freeNotation!=null){freeNotation.dispose();freeNotation=null;}}
 boolean isFreeOpen(){return freeRoot!=null&&freeRoot.getVisibility()==View.VISIBLE;}
 boolean isStudyOpen(){return studio!=null&&studio.getVisibility()==View.VISIBLE;}

}

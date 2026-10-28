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
 ScrollView library;LinearLayout libraryInner,grid,studio,freeRoot,header,homeHeader,studyTitlebar;HorizontalScrollView studyControlsBar;TextView songTitle,modeNote,tempoLabel,freeTitle,waitFeedback,homeMidiIndicator;Spinner homeProfiles;View libraryPanel;
 int feedbackSequence=0;
 Button[] modeButtons=new Button[3],handButtons=new Button[3],rendererButtons=new Button[2];
 Button playback,keyboardToggle,loopButton;
 boolean editingLoop=false;
 EditText librarySearch;Spinner libraryFilter;TextView libraryResult;
 String libraryQuery="",selectedLibraryFilter="";int libraryUser=-1;boolean libraryFiltersDirty=true;
 NotationView notation,freeNotation;
 MusicLabScreen lab;
 MusicLabDiscoveryScreen discovery;
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
  discovery=new MusicLabDiscoveryScreen(a,()->showHome());screens.addView(discovery,new FrameLayout.LayoutParams(-1,-1));
  showHome();
 }
 void buildLibrary(){
  library=new ScrollView(a);library.setFillViewport(true);
  libraryInner=column();libraryInner.setPadding(d(20),d(0),d(20),d(16));library.addView(libraryInner);
  buildHomeHeader();
  buildWelcomeBanner();
  buildNavigationCards();
  buildLibraryPanel();
  TextView footer=text("Biblioteca local · estudo individual · funciona sem internet.",11,muted,false);
  footer.setPadding(d(8),d(12),0,d(4));libraryInner.addView(footer,lp(-1,-2));
 }
 void buildHomeHeader(){
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);boolean compact=widthDp<620;
  homeHeader=row();homeHeader.setPadding(d(compact?8:24),d(4),d(compact?8:24),d(4));homeHeader.setBackgroundColor(navy);
  libraryInner.addView(homeHeader,lp(-1,compact?54:58));
  TextView brand=text(compact?"♫":"♫  PIANO LIVRE",compact?20:22,Color.WHITE,true);brand.setContentDescription("Piano Livre");homeHeader.addView(brand,new LinearLayout.LayoutParams(0,-1,1));
  homeProfiles=new Spinner(a);homeProfiles.setBackground(bg(0xffF6F9FD,12,0xffD7E4F3));homeProfiles.setPadding(d(7),0,d(7),0);
  homeProfiles.setMinimumWidth(d(90));homeProfiles.setContentDescription("Selecionar perfil");homeHeader.addView(homeProfiles,lp(compact?92:138,40));
  addGap(homeHeader,compact?4:8);
  Button profileAdd=compact("+",false,()->a.newProfile());profileAdd.setContentDescription("Criar perfil");
  profileAdd.setBackground(bg(0xff253956,12,0xff526b8c));profileAdd.setTextColor(Color.WHITE);homeHeader.addView(profileAdd,lp(compact?34:38,40));
  addGap(homeHeader,compact?4:8);
  Button settings=compact("⚙",false,()->options());settings.setTextSize(19);settings.setContentDescription("Opções e configurações");
  settings.setBackground(bg(0xff253956,12,0xff526b8c));settings.setTextColor(Color.WHITE);homeHeader.addView(settings,lp(compact?38:48,40));
  addGap(homeHeader,compact?4:8);
  homeMidiIndicator=compact("USB-MIDI",false,()->a.selectMidi());homeMidiIndicator.setTextSize(12);
  homeMidiIndicator.setSingleLine(true);homeMidiIndicator.setEllipsize(TextUtils.TruncateAt.END);
  homeMidiIndicator.setContentDescription("Gerenciar conexão USB-MIDI");homeMidiIndicator.setBackground(bg(0xffF6F9FD,18,0xffD7E4F3));
  homeMidiIndicator.setTextColor(navy);homeHeader.addView(homeMidiIndicator,lp(compact?88:154,40));
  updateMidiIndicator();
 }
 void buildWelcomeBanner(){
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);
  final boolean compact=widthDp<620;
  FrameLayout hero=new FrameLayout(a);hero.setBackground(bg(0xffD9EAFB,22,0xffC9DDF6));hero.setClipToOutline(true);
  int heroHeight=compact?285:215;
  ImageView art=new ImageView(a);art.setImageResource(R.drawable.home_hero_art);art.setScaleType(ImageView.ScaleType.CENTER_CROP);
  art.setAlpha(compact?.67f:.96f);
  FrameLayout.LayoutParams artLp=new FrameLayout.LayoutParams(compact?-1:d(widthDp<900?410:590),-1,compact?Gravity.RIGHT|Gravity.TOP:Gravity.RIGHT);
  hero.addView(art,artLp);
  View fade=new View(a);android.graphics.drawable.GradientDrawable fadeDrawable=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xffDFEDFC,0xffDFEDFC,0x00DFEDFC});fade.setBackground(fadeDrawable);
  hero.addView(fade,new FrameLayout.LayoutParams(compact?d(250):d(widthDp<900?430:610),-1,Gravity.LEFT));
  LinearLayout copy=column();copy.setGravity(Gravity.CENTER_VERTICAL);copy.setPadding(d(30),d(20),d(18),d(20));
  FrameLayout.LayoutParams copyLp=new FrameLayout.LayoutParams(compact?-1:d(widthDp<900?500:660),-1,Gravity.LEFT);
  hero.addView(copy,copyLp);
  TextView title=text("Bem-vindo ao\nPiano Livre!",compact?27:35,0xff102344,true);title.setLineSpacing(0,0.94f);copy.addView(title,lp(-1,-2));
  TextView description=text("Escolha uma música, pratique no seu ritmo e descubra o prazer de tocar.",compact?14:16,0xff455F7D,false);
  description.setPadding(0,d(5),0,d(13));copy.addView(description,lp(-1,-2));
  LinearLayout actions=compact?column():row();
  Button start=btn("▶  Começar pela Ode à Alegria",true,()->a.loadScore("scores/ode-to-joy-ludwig-van-beethoven.mxl",true));
  start.setContentDescription("Começar pela Ode à Alegria no modo Estudar");actions.addView(start,lp(compact?-1:250,compact?45:46));
  if(!compact)addGap(actions,10);
  Button help=btn("▣  Como usar",false,()->showHelp());help.setContentDescription("Abrir ajuda local do Piano Livre");
  LinearLayout.LayoutParams helpLp=lp(compact?-1:154,compact?43:46);if(compact)helpLp.topMargin=d(7);actions.addView(help,helpLp);
  copy.addView(actions,lp(compact?-1:-2,-2));
  LinearLayout.LayoutParams heroParams=lp(-1,heroHeight);heroParams.topMargin=d(12);libraryInner.addView(hero,heroParams);
 }
 void buildNavigationCards(){
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);
  int cols=widthDp>=840?4:widthDp>=520?2:1;
  LinearLayout nav=column();LinearLayout navRow=null;
  String[] titles={"Biblioteca Musical","Laboratório Musical","Histórico","Opções"};
  String[] descriptions={"Suas partituras para estudar e tocar","Notas, escalas, acordes e exercícios","Acompanhe seu progresso","Configurações e personalização"};
  String[] icons={"♫","♜","◴","⚙"};int[] colors={0xff2471D5,0xff128263,0xff6540CC,0xff9A6500};int[] tints={0xffE5F1FF,0xffE5F8F0,0xffF0EAFE,0xfffff5df};
  for(int i=0;i<4;i++){
   if(i%cols==0){navRow=row();LinearLayout.LayoutParams rp=lp(-1,-2);rp.topMargin=d(12);nav.addView(navRow,rp);}
   final int index=i;LinearLayout card=row();card.setPadding(d(12),d(10),d(10),d(10));
   GradientDrawable cardShape=bg(tints[i],17,new int[]{0xffC8DFFF,0xffCBEFDD,0xffDED4FB,0xffF4E2B9}[i]);
   card.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x22345A80),cardShape,null));
   LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(0,d(widthDp>=600?104:92),1);cardLp.setMargins(d(3),d(2),d(7),d(2));navRow.addView(card,cardLp);
   TextView icon=text(icons[i],29,colors[i],true);icon.setGravity(Gravity.CENTER);icon.setBackground(bg(0x55ffffff,50,0));card.addView(icon,lp(66,66));addGap(card,12);
   LinearLayout labels=column();labels.addView(text(titles[i],15,ink,true),lp(-1,-2));TextView sub=text(descriptions[i],12,muted,false);sub.setMaxLines(2);sub.setEllipsize(TextUtils.TruncateAt.END);labels.addView(sub,lp(-1,-2));card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
   TextView arrow=text("›",27,colors[i],true);arrow.setGravity(Gravity.CENTER);card.addView(arrow,lp(22,-1));card.setElevation(d(1));card.setClickable(true);card.setFocusable(true);
   card.setContentDescription(titles[i]+". "+descriptions[i]);card.setOnClickListener(v->{if(index==0)focusLibrary();else if(index==1)showLab();else if(index==2)a.showHistory();else options();});
  }
  libraryInner.addView(nav,lp(-1,-2));
 }
 void buildLibraryPanel(){
  libraryPanel=column();libraryPanel.setPadding(d(16),d(12),d(16),d(13));libraryPanel.setBackground(bg(Color.WHITE,20,0xffE1EAF4));libraryPanel.setElevation(d(1));
  LinearLayout.LayoutParams panelLp=lp(-1,-2);panelLp.topMargin=d(13);libraryInner.addView(libraryPanel,panelLp);
  LinearLayout section=row();section.addView(text("♫",25,blue,true),lp(35,40));
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);boolean compact=widthDp<620;
  TextView title=text("Biblioteca Musical",compact?18:23,0xff102344,true);section.addView(title,new LinearLayout.LayoutParams(0,d(42),1));
  section.addView(btn("Importar partitura",false,()->a.importScore()),lp(compact?132:151,40));
  libraryPanel.addView(section,lp(-1,44));
  LinearLayout search=compact?column():row();
  librarySearch=new EditText(a);librarySearch.setSingleLine(true);librarySearch.setTextSize(14);librarySearch.setHint("⌕  Buscar título ou arquivo");librarySearch.setContentDescription("Buscar por título ou nome de arquivo");
  libraryFilter=new Spinner(a);libraryFilter.setContentDescription("Filtrar biblioteca");
  Button collections=btn("▱  Coleções",false,()->manageCollections());
  if(compact){search.addView(librarySearch,lp(-1,42));LinearLayout filterRow=row();filterRow.addView(libraryFilter,new LinearLayout.LayoutParams(0,d(42),1));addGap(filterRow,7);filterRow.addView(collections,lp(114,42));search.addView(filterRow,lp(-1,46));libraryPanel.addView(search,lp(-1,92));}
  else{search.addView(librarySearch,new LinearLayout.LayoutParams(0,d(42),1));addGap(search,8);search.addView(libraryFilter,lp(174,42));addGap(search,8);search.addView(collections,lp(116,42));libraryPanel.addView(search,lp(-1,48));}
  libraryResult=text("",12,muted,false);libraryPanel.addView(libraryResult,lp(-1,26));
  librarySearch.addTextChangedListener(new android.text.TextWatcher(){
   public void beforeTextChanged(CharSequence t,int start,int count,int after){}
   public void onTextChanged(CharSequence t,int start,int before,int count){}
   public void afterTextChanged(android.text.Editable value){libraryQuery=value.toString();refreshLibrary();}
  });
  grid=column();libraryPanel.addView(grid,lp(-1,-2));
 }
 void focusLibrary(){
  library.post(()->library.smoothScrollTo(0,libraryPanel.getTop()));
 }
 void showHelp(){
  new AlertDialog.Builder(a).setTitle("Como usar o Piano Livre")
   .setMessage("• Escolha uma partitura na Biblioteca Musical e toque em Estudar para acompanhar a pauta.\n\n• Use Partitura Livre para ler sem acompanhamento. Pesquise, favorite e organize partituras em Coleções.\n\n• Importe seus próprios arquivos MusicXML/MXL ou MIDI pelo botão Importar partitura.\n\n• Conecte um piano compatível por USB-MIDI para reconhecer suas notas. A biblioteca e a ajuda funcionam sem internet.")
   .setPositiveButton("Entendi",null).show();
 }
 void updateMidiIndicator(){
  if(homeMidiIndicator==null)return;
  boolean connected=a.device!=null&&a.activeInfo!=null;
  String actualName=connected?a.midiInstrumentName(a.activeInfo):"";
  String label=HomeMidiIndicator.label(connected,actualName);
  homeMidiIndicator.setText(label);homeMidiIndicator.setContentDescription(connected&&!actualName.isEmpty()?"Gerenciar MIDI. Conectado a "+label:"Gerenciar conexão USB-MIDI");
 }
 void refreshLibrary(){
  if(grid==null)return;grid.removeAllViews();
  final LibraryStore.Snapshot metadata;
  try{metadata=new LibraryStore(a.prefs,a.user).snapshot();refreshLibraryFilters(metadata);}
  catch(RuntimeException ex){grid.addView(text("Não foi possível ler a organização da biblioteca. Os dados não foram alterados.",13,ink,false),lp(-1,-2));return;}
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);
  int columns=widthDp>=900?3:widthDp>=620?2:1;
  java.util.ArrayList<String[]> entries=new java.util.ArrayList<>();
  for(int i=0;i<a.catalog.length();i++){
   JSONObject j=a.catalog.optJSONObject(i);if(j!=null)entries.add(new String[]{j.optString("name"),"scores/"+j.optString("file")});
  }
  File dir=new File(a.getFilesDir(),"imported");File[] imports=dir.listFiles();
  if(imports!=null)for(File f:imports)if(f.isFile()&&!f.getName().endsWith(".source.mid"))entries.add(new String[]{f.getName(),f.getAbsolutePath()});
  int total=entries.size();
  entries.removeIf(entry->{
   String key=LibrarySearch.key(entry[1]);
   if(!LibrarySearch.matches(entry[0]+" "+new File(entry[1]).getName(),libraryQuery))return true;
   if(selectedLibraryFilter.equals("*"))return !metadata.favorites.contains(key);
   LibraryStore.Collection collection=metadata.collections.get(selectedLibraryFilter);
   return collection!=null&&!collection.scores.contains(key);
  });
  libraryResult.setText(entries.size()+" de "+total+" partituras");
  if(entries.isEmpty())grid.addView(text(total==0?"Sua biblioteca está vazia. Importe uma partitura para começar.":"Nenhuma partitura encontrada. Ajuste a busca ou escolha Todas as categorias.",13,muted,false),lp(-1,54));
  for(int i=0;i<entries.size();i+=columns){
   LinearLayout row=row();grid.addView(row,lp(-1,164));
   for(int k=0;k<columns;k++){
    if(i+k>=entries.size()){View spacer=new View(a);row.addView(spacer,new LinearLayout.LayoutParams(0,d(154),1));continue;}
    String[] entry=entries.get(i+k);
    LinearLayout card=column();card.setPadding(d(10),d(9),d(10),d(9));
    card.setBackground(bg(Color.WHITE,17,0xffE0E9F2));card.setElevation(d(1));
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(154),1);p.setMargins(d(3),d(3),d(7),d(3));row.addView(card,p);
    final String scoreKey=LibrarySearch.key(entry[1]);
    Button favorite=compact(metadata.favorites.contains(scoreKey)?"★":"☆",false,()->libraryAction(()->new LibraryStore(a.prefs,a.user).toggleFavorite(scoreKey)));
    favorite.setContentDescription((metadata.favorites.contains(scoreKey)?"Remover favorito ":"Favoritar ")+entry[0]);
    LinearLayout title=row();TextView name=text(entry[0],14,ink,true);name.setSingleLine(true);name.setEllipsize(TextUtils.TruncateAt.END);
    title.addView(name,new LinearLayout.LayoutParams(0,d(34),1));title.addView(favorite,lp(36,32));card.addView(title,lp(-1,36));
    LinearLayout details=row();ImageView thumbnail=new ImageView(a);thumbnail.setImageResource(R.drawable.home_score_placeholder);
    thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);thumbnail.setBackground(bg(0xffE8F1FA,10,0xffDAE6F2));details.addView(thumbnail,lp(70,54));addGap(details,9);
    TextView source=text(entry[1].startsWith("scores/")?"Partitura da biblioteca":"Partitura importada",11,muted,false);source.setSingleLine(true);source.setEllipsize(TextUtils.TruncateAt.END);details.addView(source,new LinearLayout.LayoutParams(0,-2,1));card.addView(details,lp(-1,58));
    LinearLayout actions=row();Button study=compact("▶  Estudar",true,()->a.loadScore(entry[1],true));actions.addView(study,new LinearLayout.LayoutParams(0,d(37),1));addGap(actions,7);
    Button free=compact("Partitura livre",false,()->a.openFreeScore(entry[1]));free.setTextColor(blue);free.setContentDescription("Abrir "+entry[0]+" em Partitura Livre");
    actions.addView(free,new LinearLayout.LayoutParams(0,d(37),1));card.addView(actions,lp(-1,39));
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
  ids.add("");names.add("Todas as categorias");ids.add("*");names.add("Favoritos");
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
  controls.addView(text("PARTITURA",10,muted,true),lp(66,-1));
  String[] rendererLabels={"OSMD · nova","Anterior · temporal"};
  for(int i=0;i<rendererLabels.length;i++){
   final int index=i;
   rendererButtons[i]=compact(rendererLabels[i],false,()->selectStudyRenderer(index==0));
   rendererButtons[i].setContentDescription(i==0?"OSMD — nova partitura":"Partitura anterior — renderizador temporal");
   LinearLayout.LayoutParams rendererLp=lp(i==0?105:143,34);rendererLp.rightMargin=d(4);
   controls.addView(rendererButtons[i],rendererLp);
  }
  addGap(controls,10);
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
    if((a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
       &&a.getIntent().getBooleanExtra("study_failure_selftest",false)){
     a.getIntent().removeExtra("study_failure_selftest");
     notation.evaluateJavascript("window.opensheetmusicdisplay=null;PianoNotation.load('<score-partwise/>',0,'engraving-poc',[])",null);
    }
    if(rendererButtons[1]!=null)rendererButtons[1].setText("Anterior · temporal");
    updateSelections();
    followScore(a.position,true);
    android.util.Log.i("PianoLivreNotation","NOTATION_RENDERED");
   }
   public void onError(String cause){
    notation.setVisibility(View.INVISIBLE);
    legacyFallback.setVisibility(View.VISIBLE);
    if(rendererButtons[1]!=null)rendererButtons[1].setText("Esquemático · ativo");
    updateSelections();
    android.widget.Toast.makeText(a,"Visualização esquemática ativa; falha na notação: "+cause,android.widget.Toast.LENGTH_LONG).show();
    android.util.Log.w("PianoLivreNotation","ACTIVE_RENDERER schematic; Notation fallback: "+cause);
   }
   @Override public void onPresentation(String message){if(!message.isEmpty())android.widget.Toast.makeText(a,message,android.widget.Toast.LENGTH_LONG).show();}
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
    if(i==0)a.markLoopStart();
    else if(i==1)a.markLoopEnd();
    else if(i==2)a.clearLoop();
    else a.seekPrevious();
    updateSelections();
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
  for(int i=0;i<rendererButtons.length;i++){
   if(rendererButtons[i]!=null){boolean on=(i==0)==(notation!=null&&notation.isEngravingProof());
    rendererButtons[i].setBackground(bg(on?blue:Color.WHITE,12,on?0:0xffCCD9E8));
    rendererButtons[i].setTextColor(on?Color.WHITE:ink);
   }
  }
  for(int i=0;i<3;i++){
   if(modeButtons[i]!=null){boolean on=a.mode==i;modeButtons[i].setBackground(bg(on?blue:Color.WHITE,12,on?0:0xffCCD9E8));modeButtons[i].setTextColor(on?Color.WHITE:ink);}
   if(handButtons[i]!=null){boolean on=a.currentHand==i;handButtons[i].setBackground(bg(on?teal:Color.WHITE,12,on?0:0xffCCD9E8));handButtons[i].setTextColor(on?Color.WHITE:ink);}
  }
  if(playback!=null)playback.setText(a.mode==0?(a.waitPreparing?"Preparando…":a.waitStarted?"♪ Esperando":"▶ Iniciar"):a.running?"❚❚  Pausar":"▶  Iniciar");
  if(keyboardToggle!=null&&a.scoreView!=null)keyboardToggle.setText(a.scoreView.showKeyboard?"Ocultar teclado":"Teclado guia");
  if(loopButton!=null)loopButton.setText(editingLoop?"Selecionando…":"Trecho ↻");
 }
 void selectStudyRenderer(boolean useOsmd){
  if(notation==null||a.score==null||notation.isEngravingProof()==useOsmd&&!notation.hasRendererFailure())return;
  // A renderer change is presentational: leave beat, playback, MusicXML,
  // profile, and saved study data untouched.
  notation.setEngravingProof(useOsmd);
  updateSelections();
  android.widget.Toast.makeText(a,useOsmd?"OSMD — nova partitura":"Partitura anterior — renderizador temporal",android.widget.Toast.LENGTH_SHORT).show();
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
  if(homeHeader!=null)homeHeader.setVisibility(View.GONE);
  if(lab!=null)lab.setVisibility(View.GONE);
  if(discovery!=null){discovery.leave();discovery.setVisibility(View.GONE);}
  freeRoot.setVisibility(View.VISIBLE);
  freeTitle.setText((a.currentName==null?a.score.title:a.currentName)+" · Partitura Livre");
  freeNotation.loadScore(a.score,0,true);
  a.beginStudyWindow();
  android.util.Log.i("PianoLivreUI","free_open");
 }
 void showLab(){
  stopLabDemonstration();
  a.stopPlayback();a.finishStudyWindow();
  if(a.scoreView!=null&&a.scoreView.showKeyboard)setKeyboardOpen(false);
  header.setVisibility(View.GONE);
  if(homeHeader!=null)homeHeader.setVisibility(View.GONE);
  library.setVisibility(View.GONE);studio.setVisibility(View.GONE);freeRoot.setVisibility(View.GONE);
  if(lab!=null)lab.setVisibility(View.GONE);
  if(discovery!=null){discovery.setVisibility(View.VISIBLE);discovery.enter();}
 }
 void showHome(){
  stopLabDemonstration();
  a.stopPlayback();a.finishStudyWindow();
  if(a.scoreView!=null&&a.scoreView.showKeyboard)setKeyboardOpen(false);
  header.setVisibility(View.GONE);
  if(homeHeader!=null)homeHeader.setVisibility(View.VISIBLE);
  freeRoot.setVisibility(View.GONE);
  studio.setVisibility(View.GONE);if(lab!=null)lab.setVisibility(View.GONE);
  if(discovery!=null){discovery.leave();discovery.setVisibility(View.GONE);}
  library.setVisibility(View.VISIBLE);refreshLibrary();
 }
 void showStudy(){
  if(a.score==null)return;
  stopLabDemonstration();
  if(homeHeader!=null)homeHeader.setVisibility(View.GONE);
  header.setVisibility(View.VISIBLE);
  freeRoot.setVisibility(View.GONE);
  library.setVisibility(View.GONE);if(lab!=null)lab.setVisibility(View.GONE);
  if(discovery!=null){discovery.leave();discovery.setVisibility(View.GONE);}
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
  notation.setStudyHand(a.currentHand);
  notation.loadScore(score,beat);
  updateSelections(); // Every newly opened Study score starts in OSMD.
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
 void onLabMidiAttack(int pitch,long time,long batch,long generation,long uiGeneration){if(discovery!=null)discovery.midiAttack(pitch,time,batch,generation,uiGeneration);}
 void onLabMidiRelease(int pitch,long generation,long uiGeneration){if(discovery!=null)discovery.midiRelease(pitch,generation,uiGeneration);}
 void onLabMidiBatchEnd(long batch,long now,long uiGeneration){if(discovery!=null)discovery.midiBatchEnd(batch,now,uiGeneration);}
 long discoveryGeneration(){return discovery==null?-1:discovery.sessionGeneration();}
 void onMidiConnectionChanged(long generation){if(discovery!=null)discovery.connectionChanged(generation);}
 void pauseDiscovery(){if(discovery!=null)discovery.pause();}
 void resumeDiscovery(){if(discovery!=null)discovery.resume();}
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
  if(freeNotation!=null){freeNotation.dispose();freeNotation=null;}
  if(discovery!=null)discovery.dispose();}
 boolean isFreeOpen(){return freeRoot!=null&&freeRoot.getVisibility()==View.VISIBLE;}
 boolean isLabOpen(){return discovery!=null&&discovery.getVisibility()==View.VISIBLE;}
 boolean isStudyOpen(){return studio!=null&&studio.getVisibility()==View.VISIBLE;}

}

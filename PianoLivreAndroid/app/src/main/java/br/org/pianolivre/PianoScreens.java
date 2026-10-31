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
 final int studyBlue=0xff075BFA;
 ScrollView library;LinearLayout libraryInner,grid,studio,freeRoot,header,homeHeader,libraryPanel;HorizontalScrollView studyControlsBar;TextView songTitle,modeNote,tempoLabel,freeTitle,waitFeedback,homeMidiIndicator,studyMidiIndicator;Spinner homeProfiles;
 int feedbackSequence=0;
 Button[] modeButtons=new Button[3],handButtons=new Button[3];
 Button playback,loopButton;
 boolean editingLoop=false;
 EditText librarySearch;Spinner libraryFilter;TextView libraryResult;
 String libraryQuery="",selectedLibraryFilter="";int libraryUser=-1;boolean libraryFiltersDirty=true;
 NotationView notation,freeNotation;
 MusicLabScreen lab;
 MusicLabDiscoveryScreen discovery;
 MusicCreationScreen creation;
 PianoView legacyFallback;
 final StudyChromeState chrome=new StudyChromeState();
 String lastStudyLayout="";
 String lastHomeLayout="";
 int homeLayoutSequence=0;
 PianoScreens(MainActivity a){this.a=a;}
 int d(float n){return (int)(n*a.getResources().getDisplayMetrics().density+.5f);}
 boolean shortLandscape(){android.util.DisplayMetrics m=a.getResources().getDisplayMetrics();return m.widthPixels>m.heightPixels&&m.heightPixels/m.density<480;}
 ArrayAdapter<String> homeProfileAdapter(String[] names){
  ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,names){
   @Override public View getView(int position,View convertView,android.view.ViewGroup parent){TextView item=(TextView)super.getView(position,convertView,parent);if(shortLandscape())item.setTextSize(9);return item;}
  };adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);return adapter;
 }
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
  a.root.addView(header,lp(-1,54));
  a.status=text("",10,Color.WHITE,false);
  a.profiles=new Spinner(a);
  buildStudyHeader();

  FrameLayout screens=new FrameLayout(a);
  a.root.addView(screens,new LinearLayout.LayoutParams(-1,0,1));
  buildLibrary();screens.addView(library,new FrameLayout.LayoutParams(-1,-1));
  buildStudio();screens.addView(studio,new FrameLayout.LayoutParams(-1,-1));
  buildFree();screens.addView(freeRoot,new FrameLayout.LayoutParams(-1,-1));
  lab=new MusicLabScreen(a,()->showHome());screens.addView(lab,new FrameLayout.LayoutParams(-1,-1));
  discovery=new MusicLabDiscoveryScreen(a,()->showHome(),()->showCreation());screens.addView(discovery,new FrameLayout.LayoutParams(-1,-1));
  creation=new MusicCreationScreen(a,()->showHome());screens.addView(creation,new FrameLayout.LayoutParams(-1,-1));
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
  if((a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0&&a.getIntent().getBooleanExtra("home_layout_selftest",false))library.getViewTreeObserver().addOnGlobalLayoutListener(()->logHomeLayout());
 }
 void logHomeLayout(){
  if((a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)==0||!a.getIntent().getBooleanExtra("home_layout_selftest",false)||library==null||library.getVisibility()!=View.VISIBLE||library.getWidth()==0)return;
  try{
   JSONObject report=new JSONObject();report.put("density",a.getResources().getDisplayMetrics().density);
   org.json.JSONArray items=new org.json.JSONArray();collectHomeLayout(libraryInner,items);report.put("items",items);
   String value=report.toString();if(!value.equals(lastHomeLayout)){
    lastHomeLayout=value;int sequence=++homeLayoutSequence;
    for(int i=0;i<items.length();i++)android.util.Log.i("PianoLivreHome","HOME_ITEM "+sequence+" "+items.getJSONObject(i).toString());
    report.remove("items");report.put("sequence",sequence);report.put("itemCount",items.length());android.util.Log.i("PianoLivreHome","HOME_LAYOUT "+report.toString());
   }
  }catch(org.json.JSONException ignored){}
 }
 void collectHomeLayout(View view,org.json.JSONArray items)throws org.json.JSONException{
  Object tag=view.getTag();
  if(tag instanceof String&&((String)tag).startsWith("home-")){
   JSONObject item=new JSONObject();item.put("kind",tag);int[] xy=new int[2];view.getLocationOnScreen(xy);
   item.put("x",xy[0]);item.put("y",xy[1]);item.put("width",view.getWidth());item.put("height",view.getHeight());
   if(view instanceof android.view.ViewGroup){org.json.JSONArray labels=new org.json.JSONArray();collectHomeText((android.view.ViewGroup)view,labels);item.put("labels",labels);}
   items.put(item);
  }
  if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)collectHomeLayout(group.getChildAt(i),items);}
 }
 void collectHomeText(android.view.ViewGroup group,org.json.JSONArray labels)throws org.json.JSONException{
  for(int i=0;i<group.getChildCount();i++){
   View child=group.getChildAt(i);
   if(child instanceof TextView){TextView t=(TextView)child;JSONObject label=new JSONObject();label.put("text",t.getText());label.put("sizeSp",t.getTextSize()/a.getResources().getDisplayMetrics().scaledDensity);int[] xy=new int[2];t.getLocationOnScreen(xy);label.put("x",xy[0]);label.put("y",xy[1]);label.put("width",t.getWidth());label.put("height",t.getHeight());label.put("textHeight",t.getLayout()==null?0:t.getLayout().getHeight()+t.getCompoundPaddingTop()+t.getCompoundPaddingBottom());labels.put(label);}
   else if(child instanceof android.view.ViewGroup)collectHomeText((android.view.ViewGroup)child,labels);
  }
 }
 void buildHomeHeader(){
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);boolean compact=widthDp<620;
  boolean shortScreen=shortLandscape();homeHeader=row();homeHeader.setPadding(d(compact?8:shortScreen?12:24),d(shortScreen?1:4),d(compact?8:shortScreen?12:24),d(shortScreen?1:4));homeHeader.setBackgroundColor(navy);
  libraryInner.addView(homeHeader,lp(-1,shortScreen?36:compact?54:58));
  TextView brand=text("♫  PIANO LIVRE",shortScreen?11:compact?12:22,Color.WHITE,true);brand.setContentDescription("Piano Livre");brand.setSingleLine(true);brand.setEllipsize(TextUtils.TruncateAt.END);homeHeader.addView(brand,new LinearLayout.LayoutParams(0,-1,1));
  homeProfiles=new Spinner(a);homeProfiles.setBackground(bg(0xffF6F9FD,12,0xffD7E4F3));homeProfiles.setPadding(d(7),0,d(7),0);
  homeProfiles.setMinimumWidth(d(shortScreen?68:90));homeProfiles.setContentDescription("Selecionar perfil");homeHeader.addView(homeProfiles,lp(shortScreen?100:compact?92:138,shortScreen?28:40));
  addGap(homeHeader,compact?4:shortScreen?5:8);
  Button profileAdd=compact("+",false,()->a.newProfile());profileAdd.setContentDescription("Criar perfil");
  profileAdd.setBackground(bg(0xff253956,12,0xff526b8c));profileAdd.setTextColor(Color.WHITE);homeHeader.addView(profileAdd,lp(compact?34:shortScreen?30:38,shortScreen?28:40));
  addGap(homeHeader,compact?4:shortScreen?5:8);
  Button settings=compact("⚙",false,()->options());settings.setTextSize(shortScreen?14:19);settings.setContentDescription("Opções e configurações");
  settings.setBackground(bg(0xff253956,12,0xff526b8c));settings.setTextColor(Color.WHITE);homeHeader.addView(settings,lp(compact?38:shortScreen?32:48,shortScreen?28:40));
  addGap(homeHeader,compact?4:shortScreen?5:8);
  homeMidiIndicator=compact("USB-MIDI",false,()->a.selectMidi());homeMidiIndicator.setTextSize(shortScreen?9:12);
  homeMidiIndicator.setSingleLine(true);homeMidiIndicator.setEllipsize(TextUtils.TruncateAt.END);
  homeMidiIndicator.setContentDescription("Gerenciar conexão USB-MIDI");homeMidiIndicator.setBackground(bg(0xffF6F9FD,18,0xffD7E4F3));
  homeMidiIndicator.setTextColor(navy);homeHeader.addView(homeMidiIndicator,lp(compact?88:shortScreen?92:154,shortScreen?28:40));
  updateMidiIndicator();
 }
 void buildWelcomeBanner(){
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);
  final boolean shortScreen=shortLandscape();final boolean compact=widthDp<620;
  FrameLayout hero=new FrameLayout(a);hero.setBackground(bg(0xffD9EAFB,22,0xffC9DDF6));hero.setClipToOutline(true);
  int heroHeight=Math.round((shortScreen?220:compact?285:215)*.7f);
  int heroWidth=widthDp-40;
  ImageView art=new ImageView(a);art.setImageResource(R.drawable.home_hero_art);art.setScaleType(ImageView.ScaleType.CENTER_CROP);
  art.setAlpha(1f);
  int artWidth=Math.round(heroWidth*.64f);
  hero.setTag("home-banner");art.setTag("home-art");
  FrameLayout.LayoutParams artLp=new FrameLayout.LayoutParams(d(artWidth),-1,Gravity.RIGHT|Gravity.CENTER_VERTICAL);
  hero.addView(art,artLp);
  View fade=new View(a);android.graphics.drawable.GradientDrawable fadeDrawable=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xffDFEDFC,0xffDFEDFC,0x00DFEDFC});fade.setBackground(fadeDrawable);
  hero.addView(fade,new FrameLayout.LayoutParams(d(Math.round(heroWidth*(compact?.78f:.60f))),-1,Gravity.LEFT));
  LinearLayout copy=column();copy.setGravity(Gravity.CENTER_VERTICAL);copy.setPadding(d(compact?12:20),d(8),d(12),d(8));
  copy.setTag("home-banner-copy");
  FrameLayout.LayoutParams copyLp=new FrameLayout.LayoutParams(compact?-1:d(Math.max(428,Math.round(heroWidth*.58f))),-1,Gravity.LEFT);
  hero.addView(copy,copyLp);
  TextView title=text("Bem-vindo ao Piano Livre!",compact?20:shortScreen?24:28,0xff102344,true);title.setMaxWidth(d(compact?Math.round(heroWidth*.82f):heroWidth));title.setLineSpacing(0,0.94f);copy.addView(title,lp(-1,-2));
  TextView description=text("Escolha uma música, pratique no seu ritmo e descubra o prazer de tocar.",compact?12:shortScreen?14:16,0xff455F7D,false);
  description.setPadding(0,d(3),0,d(7));copy.addView(description,lp(compact?Math.round(heroWidth*.66f):-1,-2));
  LinearLayout actions=row();
  Button start=btn("▶  Começar pela Ode à Alegria",true,()->a.loadScore("scores/ode-to-joy-ludwig-van-beethoven.mxl",true));
  start.setContentDescription("Começar pela Ode à Alegria no modo Estudar");actions.addView(start,lp(compact?204:250,44));if(compact)start.setTextSize(11);
  addGap(actions,8);
  Button help=btn("▣  Como usar",false,()->showHelp());help.setContentDescription("Abrir ajuda local do Piano Livre");
  LinearLayout.LayoutParams helpLp=lp(compact?104:138,44);if(compact)help.setTextSize(11);actions.addView(help,helpLp);
  copy.addView(actions,lp(compact?-1:-2,-2));
  LinearLayout.LayoutParams heroParams=lp(-1,heroHeight);heroParams.topMargin=d(12);libraryInner.addView(hero,heroParams);
 }
 void buildNavigationCards(){
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);boolean shortScreen=shortLandscape();
  int cols=widthDp>=720?3:1;
  LinearLayout nav=column();LinearLayout navRow=null;
  String[] titles={"Biblioteca Musical","Laboratório Musical","Histórico"};
  String[] descriptions={"Suas partituras para estudar e tocar","Notas, escalas, acordes e exercícios","Acompanhe seu progresso"};
  String[] icons={"♫","⚗","◴"};int[] colors={0xff2471D5,0xff128263,0xff6540CC};int[] tints={0xffE5F1FF,0xffE5F8F0,0xffF0EAFE};
  for(int i=0;i<titles.length;i++){
   if(i%cols==0){navRow=row();LinearLayout.LayoutParams rp=lp(-1,-2);rp.topMargin=d(shortLandscape()?7:12);nav.addView(navRow,rp);}
   final int index=i;LinearLayout card=row();card.setPadding(d(12),d(10),d(10),d(10));
   GradientDrawable cardShape=bg(tints[i],17,new int[]{0xffC8DFFF,0xffCBEFDD,0xffDED4FB,0xffF4E2B9}[i]);
   card.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x22345A80),cardShape,null));
   card.setPadding(d(14),d(12),d(12),d(12));card.setTag("home-shortcut");
   LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(0,d(shortScreen?96:108),1);cardLp.setMargins(d(3),d(2),d(7),d(2));navRow.addView(card,cardLp);
   TextView icon=text(icons[i],shortScreen?28:34,colors[i],true);icon.setGravity(Gravity.CENTER);icon.setBackground(bg(0x55ffffff,50,0));card.addView(icon,lp(shortScreen?48:70,shortScreen?48:70));addGap(card,12);
   LinearLayout labels=column();labels.addView(text(titles[i],shortScreen?16:18,ink,true),lp(-1,-2));TextView sub=text(descriptions[i],shortScreen?12:14,muted,false);sub.setMaxLines(3);labels.addView(sub,lp(-1,-2));card.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
   TextView arrow=text("›",shortScreen?18:27,colors[i],true);arrow.setGravity(Gravity.CENTER);card.addView(arrow,lp(shortScreen?16:22,-1));card.setElevation(d(1));card.setClickable(true);card.setFocusable(true);
   card.setContentDescription(titles[i]+". "+descriptions[i]);card.setOnClickListener(v->{if(index==0)focusLibrary();else if(index==1)showLab();else a.showHistory();});
  }
  libraryInner.addView(nav,lp(-1,-2));
 }
 void buildLibraryPanel(){
  boolean shortScreen=shortLandscape();libraryPanel=column();libraryPanel.setPadding(d(16),d(14),d(16),d(14));libraryPanel.setBackground(bg(Color.WHITE,20,0xffE1EAF4));libraryPanel.setElevation(d(1));
  LinearLayout.LayoutParams panelLp=lp(-1,-2);panelLp.topMargin=d(shortScreen?7:13);libraryInner.addView(libraryPanel,panelLp);
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);boolean compact=widthDp<720;
  LinearLayout section=row();section.addView(text("♫",25,blue,true),lp(35,44));
  TextView title=text("Biblioteca Musical",compact?22:26,0xff102344,true);title.setSingleLine(true);
  section.addView(title,new LinearLayout.LayoutParams(0,d(48),1));
  Button importButton=btn("Importar partitura",false,()->a.importScore());importButton.setTextSize(14);
  LinearLayout search=compact?column():row();
  librarySearch=new EditText(a);librarySearch.setSingleLine(true);librarySearch.setTextSize(16);librarySearch.setHint("⌕  Buscar título ou arquivo");librarySearch.setContentDescription("Buscar por título ou nome de arquivo");librarySearch.setPadding(d(10),0,d(10),0);librarySearch.setBackground(bg(0xffF6F9FD,12,0xffD9E5F1));
  libraryFilter=new Spinner(a);libraryFilter.setContentDescription("Filtrar biblioteca");
  libraryFilter.setMinimumHeight(d(48));libraryFilter.setTag("home-filter");
  libraryFilter.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{bg(0xffF6F9FD,12,0xffD9E5F1),libraryFilter.getBackground()}));
  Button collections=btn("▱  Coleções",false,()->manageCollections());collections.setTextSize(14);
  if(compact){libraryPanel.addView(section,lp(-1,48));LinearLayout actions=row();actions.addView(importButton,new LinearLayout.LayoutParams(0,d(48),1));addGap(actions,10);actions.addView(collections,new LinearLayout.LayoutParams(0,d(48),1));libraryPanel.addView(actions,lp(-1,56));
   search.addView(librarySearch,lp(-1,48));LinearLayout.LayoutParams filterLp=lp(-1,48);filterLp.topMargin=d(10);search.addView(libraryFilter,filterLp);}
  else{section.addView(collections,lp(132,48));addGap(section,12);section.addView(importButton,lp(166,48));libraryPanel.addView(section,lp(-1,56));
   search.addView(librarySearch,new LinearLayout.LayoutParams(0,d(48),1));addGap(search,16);search.addView(libraryFilter,lp(widthDp<1000?245:320,48));}
  LinearLayout.LayoutParams searchLp=lp(-1,-2);searchLp.topMargin=d(8);libraryPanel.addView(search,searchLp);
  libraryResult=text("",13,muted,false);libraryPanel.addView(libraryResult,lp(-1,32));
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
  if(creation!=null)creation.refreshConnection();
  if(homeMidiIndicator==null&&studyMidiIndicator==null)return;
  MidiConnection.Snapshot state=a.midiConnection.snapshot();
  boolean connected=state.connected();
  String actualName=connected?state.target.name:"";
  String label=HomeMidiIndicator.label(connected,actualName);
  for(TextView indicator:new TextView[]{homeMidiIndicator,studyMidiIndicator})if(indicator!=null){indicator.setText(label);indicator.setContentDescription(connected?actualName.isEmpty()?"Gerenciar MIDI. Dispositivo conectado sem nome":"Gerenciar MIDI. Conectado a "+label:"Gerenciar conexão USB-MIDI");}
 }
 void refreshLibrary(){
  if(grid==null)return;grid.removeAllViews();
  final LibraryStore.Snapshot metadata;
  try{metadata=new LibraryStore(a.prefs,a.user).snapshot();refreshLibraryFilters(metadata);}
  catch(RuntimeException ex){grid.addView(text("Não foi possível ler a organização da biblioteca. Os dados não foram alterados.",13,ink,false),lp(-1,-2));return;}
  int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);boolean shortScreen=shortLandscape();
  int columns=LibraryGridLayout.columnsForWidthDp((int)(widthDp/a.getResources().getConfiguration().fontScale));
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
   int cardHeight=shortScreen?184:208;LinearLayout row=row();grid.addView(row,lp(-1,cardHeight+12));
   for(int k=0;k<columns;k++){
    if(i+k>=entries.size()){View spacer=new View(a);row.addView(spacer,new LinearLayout.LayoutParams(0,d(cardHeight-10),1));continue;}
    String[] entry=entries.get(i+k);
    LinearLayout card=column();card.setTag("home-score");card.setPadding(d(12),d(10),d(12),d(10));
    card.setBackground(bg(Color.WHITE,17,0xffE0E9F2));card.setElevation(d(1));
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(cardHeight-10),1);p.setMargins(d(3),d(3),d(7),d(3));row.addView(card,p);
    final String scoreKey=LibrarySearch.key(entry[1]);
    Button favorite=compact(metadata.favorites.contains(scoreKey)?"★":"☆",false,()->libraryAction(()->new LibraryStore(a.prefs,a.user).toggleFavorite(scoreKey)));
    favorite.setContentDescription((metadata.favorites.contains(scoreKey)?"Remover favorito ":"Favoritar ")+entry[0]);
    LinearLayout title=row();TextView name=text(entry[0],shortScreen?17:19,ink,true);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);
    title.addView(name,new LinearLayout.LayoutParams(0,d(48),1));title.addView(favorite,lp(44,44));card.addView(title,lp(-1,48));
    LinearLayout details=row();ImageView thumbnail=new ImageView(a);thumbnail.setImageResource(scoreThumbnail(entry[1]));
    thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);thumbnail.setBackground(bg(0xffE8F1FA,10,0xffDAE6F2));details.addView(thumbnail,lp(shortScreen?68:82,shortScreen?48:62));addGap(details,12);
    TextView source=text(entry[1].startsWith("scores/")?"Partitura da biblioteca":"Partitura importada",shortScreen?13:14,muted,false);source.setMaxLines(2);details.addView(source,new LinearLayout.LayoutParams(0,-2,1));card.addView(details,lp(-1,shortScreen?60:80));
    LinearLayout actions=row();Button study=compact("▶  Estudar",true,()->a.loadScore(entry[1],true));study.setTextSize(14);actions.addView(study,new LinearLayout.LayoutParams(0,d(44),1));addGap(actions,8);
    Button free=compact("Partitura livre",false,()->a.openFreeScore(entry[1]));free.setTextColor(blue);free.setContentDescription("Abrir "+entry[0]+" em Partitura Livre");
    free.setTextSize(14);actions.addView(free,new LinearLayout.LayoutParams(0,d(44),1));card.addView(actions,lp(-1,44));
    card.setContentDescription("Estudar "+entry[0]);card.setClickable(true);card.setFocusable(true);
    card.setOnClickListener(v->a.loadScore(entry[1],true));
    card.setOnLongClickListener(v->{organizeScore(entry[0],scoreKey);return true;});
   }
  }
  // Searching replaces the score cards. When the user returns from studying a
  // lower row, the old ScrollView offset can hide the newly filtered result.
  // Keep the search row and matching cards in the viewport after each rebuild.
  if(!libraryQuery.isEmpty()){
   final String queryAtRender=libraryQuery;
   library.post(()->{if(queryAtRender.equals(libraryQuery))library.scrollTo(0,libraryPanel.getTop());});
  }
 }
 int scoreThumbnail(String scorePath){
  String key=scorePath==null?"":scorePath.toLowerCase(Locale.ROOT);
  if(key.endsWith("ode-to-joy-ludwig-van-beethoven.mxl"))return R.drawable.home_score_ode;
  if(key.endsWith("fur-elise-easy-ver.mxl"))return R.drawable.home_score_fur_elise;
  if(key.endsWith("lago-dos-cisnes.mxl"))return R.drawable.home_score_swan;
  return R.drawable.home_score_placeholder;
 }
 void refreshLibraryFilters(LibraryStore.Snapshot metadata){
  if(libraryUser!=a.user){libraryUser=a.user;selectedLibraryFilter="";libraryFiltersDirty=true;}
  if(!libraryFiltersDirty)return;libraryFiltersDirty=false;
  java.util.List<String> ids=new java.util.ArrayList<>(),names=new java.util.ArrayList<>();
  ids.add("");names.add("Todas as categorias");ids.add("*");names.add("Favoritos");
  for(LibraryStore.Collection c:metadata.collections.values()){ids.add(c.id);names.add("Coleção: "+c.name);}
  int selected=ids.indexOf(selectedLibraryFilter);if(selected<0){selected=0;selectedLibraryFilter="";}
  libraryFilter.setOnItemSelectedListener(null);
  ArrayAdapter<String> filterAdapter=new ArrayAdapter<>(a,android.R.layout.simple_spinner_item,names){
   @Override public View getView(int position,View convertView,android.view.ViewGroup parent){TextView item=(TextView)super.getView(position,convertView,parent);item.setTextSize(16);return item;}
  };filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
  libraryFilter.setAdapter(filterAdapter);libraryFilter.setSelection(selected);
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
  LinearLayout controls=row();controls.setPadding(d(12),0,d(12),0);controls.setBackgroundColor(0xffF1F5FC);
  controls.setGravity(Gravity.CENTER);
  controls.addView(text("MODO",10,muted,true),lp(40,-1));
  String[] modes={"Espera","Contínuo","Demonstração"};
  for(int i=0;i<3;i++){final int idx=i;modeButtons[i]=compact(modes[i],false,()->{
    a.stopPlayback();a.finishStudyWindow();a.mode=idx;android.util.Log.i("PianoLivreUI","mode="+idx);
    if(idx==0)a.prepareWaiting();updateSelections();a.saveSession();a.beginStudyWindow();
  });LinearLayout.LayoutParams p=lp(i==2?112:82,44);p.rightMargin=d(4);controls.addView(modeButtons[i],p);}
  modeButtons[0].setOnLongClickListener(v->{a.showChordSettings();return true;});
  addGap(controls,13);controls.addView(text("MÃOS",10,muted,true),lp(40,-1));
  String[] hands={"Ambas","Esquerda","Direita"};
  for(int i=0;i<3;i++){final int idx=i==0?0:i==1?2:1;handButtons[idx]=compact(hands[i],false,()->{
   a.selectHand(idx);updateSelections();
  });LinearLayout.LayoutParams p=lp(82,44);p.rightMargin=d(4);controls.addView(handButtons[idx],p);}
  studyControlsBar=new HorizontalScrollView(a);studyControlsBar.setHorizontalScrollBarEnabled(true);
  studyControlsBar.setFillViewport(true);
  studio.removeView(controls);studio.addView(studyControlsBar,lp(-1,46));studyControlsBar.addView(controls,new android.widget.HorizontalScrollView.LayoutParams(-2,d(46)));
  FrameLayout notationArea=new FrameLayout(a);
  legacyFallback=new PianoView(a);legacyFallback.allowKeyboard=false;
  legacyFallback.setOnTouchListener((view,event)->{
   if(event.getActionMasked()==android.view.MotionEvent.ACTION_UP)revealStudyControls();
   return true;
  });
  a.scoreView=legacyFallback;
  notationArea.addView(legacyFallback,new FrameLayout.LayoutParams(-1,-1));
  notation=new NotationView(a,new NotationView.Listener(){
   public void onRendered(){
    legacyFallback.setVisibility(View.INVISIBLE);
    if((a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
       &&a.getIntent().getBooleanExtra("study_failure_selftest",false)){
     a.getIntent().removeExtra("study_failure_selftest");
     String failTemporal=a.getIntent().getBooleanExtra("study_native_failure_selftest",false)?"window.TemporalNotation=null;":"";
     notation.evaluateJavascript(failTemporal+"window.opensheetmusicdisplay=null;PianoNotation.load('<score-partwise/>',0,'engraving-poc',[])",null);
    }
    updateSelections();
    followScore(a.position,true);
    android.util.Log.i("PianoLivreNotation","NOTATION_RENDERED");
   }
   public void onError(String cause){
    notation.setVisibility(View.INVISIBLE);
    legacyFallback.setVisibility(View.VISIBLE);
    updateSelections();
    android.widget.Toast.makeText(a,"Visualização esquemática ativa; falha na notação: "+cause,android.widget.Toast.LENGTH_LONG).show();
    android.util.Log.w("PianoLivreNotation","ACTIVE_RENDERER schematic; Notation fallback: "+cause);
   }
   @Override public void onStudyTap(){revealStudyControls();}
   @Override public void onPresentation(String message){if(!message.isEmpty())android.widget.Toast.makeText(a,message,android.widget.Toast.LENGTH_LONG).show();}
   @Override public void onLoopSelected(double start,double end){
    editingLoop=false;
    a.applyLoopSelection(start,end);
    notation.setLoopEdit(false);
    updateSelections();
   }
  });
  notationArea.addView(notation,new FrameLayout.LayoutParams(-1,-1));
  notation.addOnLayoutChangeListener((view,l,t,r,b,oldL,oldT,oldR,oldB)->view.post(()->recordStudyLayout()));
  waitFeedback=text("",12,0xff614105,true);
  waitFeedback.setBackground(bg(0xffffefc7,10,0xffefd9a1));
  waitFeedback.setPadding(d(12),d(6),d(12),d(6));
  waitFeedback.setVisibility(View.GONE);
  FrameLayout.LayoutParams feedbackPlacement=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);
  feedbackPlacement.setMargins(d(8),d(8),d(16),d(8));
  notationArea.addView(waitFeedback,feedbackPlacement);
  studio.addView(notationArea,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout bottom=row();bottom.setPadding(d(9),d(3),d(9),d(3));bottom.setBackgroundColor(Color.WHITE);
  boolean compactStudy=a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density<650;
  studio.addView(bottom,lp(-1,54));
  Button rewind=compact(compactStudy?"|◀":"|◀ Início",false,()->a.seekPrevious());rewind.setContentDescription("Início");bottom.addView(rewind,lp(compactStudy?44:76,44));addGap(bottom,6);
  playback=compact("▶  Iniciar",true,()->{a.toggle();updateSelections();});
  playback.setBackground(bg(studyBlue,12,0));
  playback.setOnLongClickListener(v->{if(a.mode==0){a.configureWaitPreparation();return true;}return false;});
  bottom.addView(playback,lp(compactStudy?82:100,44));addGap(bottom,6);
  Button stop=compact(compactStudy?"■":"■ Parar",false,()->{a.stopPlayback();updateSelections();});stop.setContentDescription("Parar");bottom.addView(stop,lp(compactStudy?44:68,44));addGap(bottom,10);
  TextView speed=text("VELOCIDADE",10,muted,true);if(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density>=650)bottom.addView(speed,lp(77,-1));
  a.tempoBar=new SeekBar(a);a.tempoBar.setMax(175);a.tempoBar.setProgress(75);
  a.tempoBar.setContentDescription("Velocidade de reprodução");
  bottom.addView(a.tempoBar,new LinearLayout.LayoutParams(0,d(44),1));
  tempoLabel=text("100%",12,ink,true);tempoLabel.setGravity(Gravity.CENTER);bottom.addView(tempoLabel,lp(compactStudy?42:52,-1));
  a.tempoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar s,int progress,boolean by){a.tempoFactor=(progress+25)/100.0;tempoLabel.setText((progress+25)+"%");a.updateInfo();}
   public void onStartTrackingTouch(SeekBar s){}
   public void onStopTrackingTouch(SeekBar s){a.saveSession();}
  });
  addGap(bottom,6);
  loopButton=compact("Trecho ↻",false,()->{
    if(a.score==null)return;
    editingLoop=!editingLoop;
    if(editingLoop)a.stopPlayback();
    notation.setLoopEdit(editingLoop);
    updateSelections();
  });
  loopButton.setOnLongClickListener(v->{loopMenu();return true;});
  bottom.addView(loopButton,lp(compactStudy?74:86,44));
  updateSelections();
 }
 void revealStudyControls(){
  chrome.reveal();applyStudyChrome();
 }
 void applyStudyChrome(){
  if(!isStudyOpen())return;
  int visibility=chrome.visible()?View.VISIBLE:View.GONE;
  if(header.getVisibility()==visibility&&studyControlsBar.getVisibility()==visibility){recordStudyLayout();return;}
  header.setVisibility(visibility);studyControlsBar.setVisibility(visibility);
  if(notation!=null)notation.post(()->notation.resizeToFit());
  android.util.Log.i("PianoLivreUI","STUDY_CHROME visible="+chrome.visible()+" beat="+a.position);
  if((a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0)try{
   JSONObject state=new JSONObject();state.put("visible",chrome.visible());state.put("beat",a.position);
   state.put("mode",a.mode);state.put("hand",a.currentHand);state.put("tempo",a.tempoFactor);
   state.put("loopStart",a.loopStart);state.put("loopEnd",a.loopEnd);state.put("midiGeneration",a.midiGeneration);
   state.put("running",a.running);state.put("waitStarted",a.waitStarted);
   state.put("sourceHash",a.score==null?0:a.score.sourceXml.hashCode());
   android.util.Log.i("PianoLivreUI","STUDY_CHROME_STATE "+state);
  }catch(org.json.JSONException impossible){throw new IllegalStateException(impossible);}
 }
 void recordStudyLayout(){
  if(notation==null||(a.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
  try{
   int[] xy=new int[2];notation.getLocationOnScreen(xy);
   JSONObject layout=new JSONObject();layout.put("bounds",new org.json.JSONArray(new int[]{xy[0],xy[1],xy[0]+notation.getWidth(),xy[1]+notation.getHeight()}));
   layout.put("density",a.getResources().getDisplayMetrics().density);
   layout.put("headerVisible",header.getVisibility()==View.VISIBLE);
   layout.put("modesVisible",studyControlsBar.getVisibility()==View.VISIBLE);
   layout.put("transportActive",a.running||a.waitStarted);
   View transport=(View)playback.getParent();transport.getLocationOnScreen(xy);
   layout.put("transportBounds",new org.json.JSONArray(new int[]{xy[0],xy[1],xy[0]+transport.getWidth(),xy[1]+transport.getHeight()}));
   org.json.JSONArray buttons=new org.json.JSONArray();
   android.view.ViewGroup bar=(android.view.ViewGroup)transport;
   for(int i=0;i<bar.getChildCount();i++)if(bar.getChildAt(i) instanceof Button){
    Button button=(Button)bar.getChildAt(i);button.getLocationOnScreen(xy);
    JSONObject item=new JSONObject();item.put("text",button.getText().toString());item.put("description",String.valueOf(button.getContentDescription()));
    item.put("bounds",new org.json.JSONArray(new int[]{xy[0],xy[1],xy[0]+button.getWidth(),xy[1]+button.getHeight()}));buttons.put(item);
   }
   layout.put("buttons",buttons);
   String json=layout.toString();if(!json.equals(lastStudyLayout)){lastStudyLayout=json;android.util.Log.i("PianoLivreUI","STUDY_CHROME_LAYOUT "+json);}
  }catch(org.json.JSONException impossible){throw new IllegalStateException(impossible);}
 }
 void buildStudyHeader(){
  boolean compact=a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density<900;
  header.setPadding(d(8),0,d(8),0);
  header.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xff0D2C55,0xff082445}));
  Button back=compact(compact?"←":"← Biblioteca",false,()->showHome());back.setContentDescription("← Biblioteca");
  back.setTextColor(Color.WHITE);back.setBackgroundColor(Color.TRANSPARENT);header.addView(back,lp(compact?44:115,44));addGap(header,8);
  back.setTextSize(compact?22:13);
  songTitle=text("Selecione uma partitura",compact?15:20,Color.WHITE,true);songTitle.setSingleLine(!compact);songTitle.setMaxLines(compact?2:1);songTitle.setEllipsize(TextUtils.TruncateAt.END);
  header.addView(songTitle,new LinearLayout.LayoutParams(0,-1,1));addGap(header,8);
  a.info=text("",11,0xffDCE8FA,false);a.info.setSingleLine(true);a.info.setEllipsize(TextUtils.TruncateAt.END);
  a.info.setContentDescription("Andamento real, velocidade e posição; não é zoom");
  if(!compact){
   Button free=compact("♫ Partitura Livre",false,()->showFree());free.setContentDescription("Partitura Livre");free.setTextColor(Color.WHITE);free.setBackground(bg(0xff253F64,12,0));header.addView(free,lp(144,44));addGap(header,10);
   header.addView(a.info,lp(220,44));addGap(header,8);
   a.profiles.setBackground(bg(0xffEDF4FA,12,0));header.addView(a.profiles,lp(125,44));addGap(header,8);
  }
  Button options=compact(compact?"⋮":"⚙",false,()->{
   if(!compact){options();return;}
   new AlertDialog.Builder(a).setTitle("Estudar · "+a.currentName).setItems(new String[]{"Partitura Livre","Perfil","Opções",a.info.getText().toString()},(dlg,i)->{
    if(i==0)showFree();else if(i==1){
     String[] names=new String[a.profiles.getCount()+1];for(int n=0;n<a.profiles.getCount();n++)names[n]=a.profiles.getItemAtPosition(n).toString();names[names.length-1]="Gerenciar perfil";
     new AlertDialog.Builder(a).setTitle("Selecionar perfil").setItems(names,(pd,index)->{if(index<a.profiles.getCount())a.selectProfileFromSpinner(index,a.profileStore().list());else a.manageProfile();}).show();
    }else if(i==2)options();
   }).show();
  });options.setContentDescription("Opções de estudo");options.setTextColor(Color.WHITE);options.setTextSize(20);options.setBackground(bg(0xff253F64,12,0));header.addView(options,lp(44,44));addGap(header,8);
  studyMidiIndicator=compact("USB-MIDI",false,()->a.selectMidi());studyMidiIndicator.setSingleLine(true);studyMidiIndicator.setEllipsize(TextUtils.TruncateAt.END);header.addView(studyMidiIndicator,lp(compact?98:150,44));
 }
 void updateStudyInfo(String info){
  if(songTitle==null||a.score==null)return;
  String title=a.currentName==null||a.currentName.isEmpty()?a.score.title:a.currentName;
  boolean compact=a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density<900;
  String display=title+(compact?"\n"+info:"");
  if(!TextUtils.equals(songTitle.getText(),display))songTitle.setText(display);
  String description=title+" · "+info;
  if(!TextUtils.equals(songTitle.getContentDescription(),description))songTitle.setContentDescription(description);
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
  for(int i=0;i<3;i++){
   if(modeButtons[i]!=null){boolean on=a.mode==i;modeButtons[i].setBackground(bg(on?studyBlue:Color.WHITE,12,on?0:0xffCCD9E8));modeButtons[i].setTextColor(on?Color.WHITE:ink);}
   if(handButtons[i]!=null){boolean on=a.currentHand==i;handButtons[i].setBackground(bg(on?studyBlue:Color.WHITE,12,on?0:0xffCCD9E8));handButtons[i].setTextColor(on?Color.WHITE:ink);}
  }
  if(playback!=null)playback.setText(a.mode==0?(a.waitPreparing?"Preparando…":a.waitStarted?"♪ Esperando":"▶ Iniciar"):a.running?"❚❚  Pausar":"▶  Iniciar");
  chrome.update(a.running||a.waitStarted);applyStudyChrome();
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
  if(creation!=null){creation.onBackground();creation.setVisibility(View.GONE);}
  stopLabDemonstration();
  a.stopPlayback();a.finishStudyWindow();
  header.setVisibility(View.GONE);
  if(homeHeader!=null)homeHeader.setVisibility(View.GONE);
  library.setVisibility(View.GONE);studio.setVisibility(View.GONE);freeRoot.setVisibility(View.GONE);
  if(lab!=null)lab.setVisibility(View.GONE);
  if(discovery!=null){discovery.setVisibility(View.VISIBLE);discovery.enter();}
 }
 void showCreation(){
  stopLabDemonstration();a.stopPlayback();a.finishStudyWindow();header.setVisibility(View.GONE);
  if(homeHeader!=null)homeHeader.setVisibility(View.GONE);library.setVisibility(View.GONE);studio.setVisibility(View.GONE);freeRoot.setVisibility(View.GONE);
  if(lab!=null)lab.setVisibility(View.GONE);if(discovery!=null){discovery.leave();discovery.setVisibility(View.GONE);}
  if(creation!=null)creation.enter();
 }
 void showHome(){
  stopLabDemonstration();
  a.stopPlayback();a.finishStudyWindow();
  header.setVisibility(View.GONE);
  if(homeHeader!=null)homeHeader.setVisibility(View.VISIBLE);
  freeRoot.setVisibility(View.GONE);
  studio.setVisibility(View.GONE);if(lab!=null)lab.setVisibility(View.GONE);
  if(discovery!=null){discovery.leave();discovery.setVisibility(View.GONE);}
  if(creation!=null)creation.setVisibility(View.GONE);
  library.setVisibility(View.VISIBLE);refreshLibrary();updateMidiIndicator();
 }
 void showStudy(){
  if(a.score==null)return;
  stopLabDemonstration();
  if(homeHeader!=null)homeHeader.setVisibility(View.GONE);
  header.setVisibility(View.VISIBLE);
  freeRoot.setVisibility(View.GONE);
  library.setVisibility(View.GONE);if(lab!=null)lab.setVisibility(View.GONE);
  if(discovery!=null){discovery.leave();discovery.setVisibility(View.GONE);}
  if(creation!=null)creation.setVisibility(View.GONE);
  studio.setVisibility(View.VISIBLE);
  songTitle.setText(a.currentName==null||a.currentName.isEmpty()?a.score.title:a.currentName);
  if(notation!=null)notation.postDelayed(()->notation.resizeToFit(),180);
  modeNote=null;updateSelections();updateMidiIndicator();a.beginStudyWindow();
 }
 void displayScore(Score score,double beat){
  editingLoop=false;updateSelections();
  legacyFallback.setVisibility(View.VISIBLE);
  notation.setStudyHand(a.currentHand);
  notation.loadScore(score,beat);
  updateSelections(); // Every newly opened Study score starts in OSMD.

 }
 void clearLoopSelection(){
  editingLoop=false;
  if(notation!=null){notation.setLoopEdit(false);notation.clearLoop();}
  updateSelections();
 }
 void startWaitPreparation(long durationMs){if(notation!=null)notation.startWaitPreparation(durationMs);}
 void stopWaitPreparation(){if(notation!=null)notation.cancelWaitPreparation();}
 void followScore(double beat,boolean smooth){
  if(notation!=null)notation.seek(beat,smooth,
   a.score==null?100:a.score.tempoAt(beat)*a.tempoFactor,
   a.mode==0?a.waitTraveling:a.running);
 }
 void onLabMidiAttack(int pitch){
  if(lab!=null&&lab.getVisibility()==View.VISIBLE)lab.onMidiAttack(pitch);
 }
 void onLabMidiAttack(int pitch,long time,long batch,long generation,long uiGeneration){if(discovery!=null)discovery.midiAttack(pitch,time,batch,generation,uiGeneration);}
 void captureCreationMidi(int status,int d1,int d2,long nowNs,long epoch,long androidTime,int source){if(creation!=null)creation.onMidi(status,d1,d2,nowNs,epoch,androidTime,source);}
 void onCreationMidi(int status,int d1,int d2,long nowNs,long epoch,long androidTime,int source){if(creation!=null&&creation.getVisibility()==View.VISIBLE)creation.onMidi(status,d1,d2,nowNs,epoch,androidTime,source);}
 void resumeCreation(){if(creation!=null&&creation.getVisibility()==View.VISIBLE)creation.onForeground();}
 void pauseCreation(){if(creation!=null&&creation.getVisibility()==View.VISIBLE)creation.onBackground();}
 void connectionLostCreation(){if(creation!=null&&creation.getVisibility()==View.VISIBLE)creation.onConnectionLost();}
 void prepareCreationBackup()throws java.io.IOException{if(creation!=null)creation.prepareBackup();}
 void backFromLab(){if(creation!=null&&creation.getVisibility()==View.VISIBLE)creation.requestLeave();else showHome();}
 void onLabMidiRelease(int pitch,long generation,long uiGeneration){if(discovery!=null)discovery.midiRelease(pitch,generation,uiGeneration);}
 void onLabMidiBatchEnd(long batch,long now,long uiGeneration){if(discovery!=null)discovery.midiBatchEnd(batch,now,uiGeneration);}
 long discoveryGeneration(){return discovery==null?-1:discovery.sessionGeneration();}
 void onMidiConnectionChanged(long generation){if(discovery!=null)discovery.connectionChanged(generation);}
 void pauseDiscovery(){if(discovery!=null)discovery.pause();}
 void resumeDiscovery(){if(discovery!=null)discovery.resume();}
 void updatePressed(java.util.Set<Integer> notes){
  if(lab!=null)lab.onMidiPressed(notes);

 }
 void pauseNotation(){if(notation!=null)notation.onPause();if(freeNotation!=null)freeNotation.onPause();}
 void resumeNotation(){
  if(notation!=null)notation.onResume();if(freeNotation!=null)freeNotation.onResume();
  if(isStudyOpen())followScore(a.position,true);
 }
 void destroyNotation(){if(notation!=null){notation.dispose();notation=null;}
  if(freeNotation!=null){freeNotation.dispose();freeNotation=null;}
  if(discovery!=null)discovery.dispose();if(creation!=null)creation.close();}
 boolean isFreeOpen(){return freeRoot!=null&&freeRoot.getVisibility()==View.VISIBLE;}
 boolean isLabOpen(){return (discovery!=null&&discovery.getVisibility()==View.VISIBLE)||(creation!=null&&creation.getVisibility()==View.VISIBLE);}
 boolean isStudyOpen(){return studio!=null&&studio.getVisibility()==View.VISIBLE;}

}

package br.org.pianolivre;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/** Debug-only emulator UI contract test; score glyphs come from the Study renderer. */
final class MusicLabDiscoverySelfTest {
 private static int checks;
 private static void check(boolean ok,String explanation){checks++;if(!ok)throw new IllegalStateException("Discovery UI: "+explanation);}
 private static void collect(View v,List<View> out){out.add(v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collect(g.getChildAt(i),out);}}
 private static List<View> tree(View root){ArrayList<View> out=new ArrayList<>();collect(root,out);return out;}
 private static void clickHomeEntry(Activity a,String label){for(View v:tree(a.getWindow().getDecorView())){
  if(v instanceof Button&&label.equals(((Button)v).getText().toString())){v.performClick();return;}
  CharSequence description=v.getContentDescription();
  if(v.isClickable()&&description!=null&&description.toString().startsWith(label+".")){v.performClick();return;}
 }throw new IllegalStateException("Missing clickable home entry "+label);}

 static void run(MainActivity a){
  checks=0;PianoScreens screens=a.screens;MusicLabDiscoveryScreen d=screens.discovery;
  clickHomeEntry(a,"Laboratório Musical");check(screens.isLabOpen(),"home entry opens Descobrir");
  check(d.discover.getVisibility()==View.VISIBLE&&!d.discover.isEnabled(),"Descobrir selected");
  check(d.learn.getVisibility()==View.VISIBLE&&!d.learn.isEnabled(),"Aprender visible and disabled");
  check(d.instruction.getText().toString().equals(MusicLabDiscoveryScreen.EMPTY_TEXT),"exact empty instruction");
  List<View> elements=tree(d);int buttons=0;for(View v:elements)if(v instanceof Button)buttons++;
  check(buttons==3,"only toolbar controls in empty state");
  for(View v:elements)if(v instanceof TextView){String t=((TextView)v).getText().toString();check(!Arrays.asList("Tocar","Montar","Iniciar","Gravar","Praticar","Metrônomo","Demonstração").contains(t),"legacy control absent: "+t);}
  long conn=a.midiGeneration,ui=screens.discoveryGeneration();d.session.reconnect(conn,null);
  d.midiAttack(60,100,1,conn,ui);d.midiRelease(60,conn,ui);d.midiBatchEnd(1,101,ui);
  check(d.title.getText().toString().equals("DÓ - C4"),"single note label");
  check(d.ciphers.getText().toString().equals("Cifras: C4"),"note cipher is visible");

  d.session.reconnect(conn,null);d.midiAttack(64,200,2,conn,ui);d.midiAttack(67,200,2,conn,ui);d.midiAttack(72,200,2,conn,ui);d.midiRelease(64,conn,ui);d.midiRelease(67,conn,ui);d.midiRelease(72,conn,ui);d.midiBatchEnd(2,201,ui);
  check(d.session.result().kind==MusicDiscoverySession.Kind.CHORD&&d.session.result().detail.equals("1ª inversão"),"chord symbol includes its inversion");
  check(d.ciphers.getText().toString().equals("Cifras: E4 · G4 · C5"),"chord note ciphers are visible in pitch order");

  d.session.reconnect(conn,null);int[] scale={60,62,64,65,67,69,71,72};long batch=3,time=300;
  for(int n:scale){d.midiAttack(n,time,batch,conn,ui);d.midiRelease(n,conn,ui);d.midiBatchEnd(batch,time+1,ui);batch++;time+=2000;}
  check(d.session.result().kind==MusicDiscoverySession.Kind.SEQUENCE&&d.session.result().notes.size()==8,"complete C major classified through live MIDI events with two-second inter-onset intervals");
  check(d.ciphers.getText().toString().equals("Cifras: C4 · D4 · E4 · F4 · G4 · A4 · B4 · C5"),"all scale pitch ciphers are visible in order");
  d.back.performClick();check(!screens.isLabOpen(),"Voltar returns to library");
  clickHomeEntry(a,"Laboratório Musical");
  d.staff.setRenderListener(new DiscoveryStaffView.RenderListener(){
   @Override public void onRendered(){ }
   @Override public void onError(String message){throw new IllegalStateException("Discovery OSMD rendering failed: "+message);}
  });
  renderCases(a,d,staffCases(),0,new HashMap<>());
 }

 private static List<StaffCase> staffCases(){return Arrays.asList(
  new StaffCase("discovery-staff-c2.png",Arrays.asList(36),false,"F",1),
  new StaffCase("discovery-staff-c3.png",Arrays.asList(48),false,"F",1),
  new StaffCase("discovery-staff-f3.png",Arrays.asList(53),false,"F",1),
  new StaffCase("discovery-staff-c4.png",Arrays.asList(60),false,"G",1),
  new StaffCase("discovery-staff-eb4.png",Arrays.asList(63),false,"G",1),
  new StaffCase("discovery-staff-g4.png",Arrays.asList(67),false,"G",1),
  new StaffCase("discovery-staff-c6.png",Arrays.asList(84),false,"G",1),
  new StaffCase("discovery-staff-low-chord.png",Arrays.asList(36,48,53),false,"F",1),
  new StaffCase("discovery-staff-high-chord.png",Arrays.asList(60,64,67,72),false,"G",1),
  new StaffCase("discovery-staff-grand-chord.png",Arrays.asList(48,60,67,72),false,"GF",2),
  new StaffCase("discovery-staff-grand-sequence.png",Arrays.asList(55,59,60,62),true,"GF",2),
  new StaffCase("discovery-staff-c-major.png",Arrays.asList(60,62,64,65,67,69,71,72),true,"G",1),
  new StaffCase("discovery-staff-phone-sequence.png",longSequence(),true,"GF",2,910,420),
  new StaffCase("discovery-staff-tablet-sequence.png",longSequence(),true,"GF",2,910,600));
 }
 private static List<Integer> longSequence(){ArrayList<Integer> values=new ArrayList<>();for(int n=48;n<=66;n++)values.add(n);values.add(84);return values;}
 private static final class StaffCase{
  final String file;final List<Integer> notes;final boolean sequence;final String clefs;final int clefCount,widthDp,heightDp;
  StaffCase(String file,List<Integer> notes,boolean sequence,String clefs,int count){this(file,notes,sequence,clefs,count,640,300);}
  StaffCase(String file,List<Integer> notes,boolean sequence,String clefs,int count,int widthDp,int heightDp){this.file=file;this.notes=notes;this.sequence=sequence;this.clefs=clefs;clefCount=count;this.widthDp=widthDp;this.heightDp=heightDp;}
 }
 private static void renderCases(MainActivity a,MusicLabDiscoveryScreen d,List<StaffCase> cases,int index,Map<String,String> glyphs){
  if(index>=cases.size()){
   check(glyphs.containsKey("G")&&glyphs.containsKey("F")&&!glyphs.get("G").equals(glyphs.get("F")),"rendered treble and bass glyphs are distinct actual SVG symbols");
   d.session.reconnect(a.midiGeneration,null);d.midiAttack(60,900,90,a.midiGeneration,d.sessionGeneration());d.midiRelease(60,a.midiGeneration,d.sessionGeneration());d.midiBatchEnd(90,901,d.sessionGeneration());
   d.midiAttack(62,902,91,a.midiGeneration,d.sessionGeneration());
   check(d.session.result().kind==MusicDiscoverySession.Kind.NOTE&&d.session.result().notes.equals(Collections.singletonList(60)),"held note result remains until next note is released");
   d.midiRelease(62,a.midiGeneration,d.sessionGeneration());
   check(d.session.result().notes.equals(Collections.singletonList(62))&&d.title.getText().toString().equals("RÉ - D4"),"next isolated note updates immediately on release");
   d.pause();d.resume();check(d.session.result().notes.equals(Collections.singletonList(62)),"pause and return preserve last result");
   d.back.performClick();check(!a.screens.isLabOpen(),"Voltar returns to library");
   android.util.Log.i("PianoLivreDiscovery","DISCOVERY_UI_SELFTEST_PASS checks="+checks+" synthetic_midi=true clefs=OSMD-study-renderer captures="+cases.size()+" c_major=true ciphers=true");return;
  }
  StaffCase sample=cases.get(index);DiscoveryStaffView staff=d.staff;
  // Clear the prior fixture so equal notes at different viewport sizes still trigger a fresh OSMD render callback.
  staff.setNotes(Collections.emptyList(),false);staff.setNotes(sample.notes,sample.sequence);
  layoutStaff(a,staff,sample.widthDp,sample.heightDp);List<DiscoveryStaffView.NotePosition> positions=staff.layoutSnapshot();
  check(positions.size()==sample.notes.size(),"all notes retained in rendered layout: "+sample.file);
  check(staff.renderedMusicXml().contains("<sign>G</sign>")==sample.clefs.contains("G"),"MusicXML requests treble clef where expected: "+sample.file);
  check(staff.renderedMusicXml().contains("<sign>F</sign>")==sample.clefs.contains("F"),"MusicXML requests bass clef where expected: "+sample.file);
  if(sample.notes.contains(63))check(staff.renderedMusicXml().contains("<step>E</step><alter>-1</alter><octave>4</octave>")&&staff.renderedMusicXml().contains("<accidental>flat</accidental>"),"Eb4 pitch and flat accidental survive Study engraving input");
  int chordTags=count(staff.renderedMusicXml(),"<chord/>");check(chordTags==(sample.sequence?0:sample.notes.size()-1),"chord simultaneity/order encoded in MusicXML: "+sample.file);
  float density=a.getResources().getDisplayMetrics().density;float top=sample.clefs.equals("GF")?Math.max(18*density,staff.getHeight()*.14f):Math.max(18*density,staff.getHeight()*.25f);
  float bassBottom=top+(sample.clefs.equals("GF")?Math.max(76*density,staff.getHeight()*.43f):0)+40*density,trebleBottom=top+40*density;
  for(int i=0;i<positions.size();i++){
   DiscoveryStaffView.NotePosition p=positions.get(i);check(p.midi==sample.notes.get(i),"staff preserves MIDI pitch/order: "+sample.file);
   check(p.clef.equals(sample.clefs.equals("F")||sample.clefs.equals("GF")&&p.midi<60?"fá":"sol"),"pitch remains assigned to its correct clef: "+sample.file);
   if(i>0)check(sample.sequence?p.x>positions.get(i-1).x:p.x==positions.get(0).x,"sequence order or chord simultaneity preserved: "+sample.file);
   check(p.x>=0&&p.x<=staff.getWidth()&&p.y>=0&&p.y<=staff.getHeight(),"note remains inside scrollable staff content: "+sample.file);
   if((sample.file.contains("phone-sequence")||sample.file.contains("tablet-sequence"))&&i>0)check(p.x-positions.get(i-1).x>=41*density,"long sequence keeps cipher/accidental clearance in responsive content: "+sample.file);
   if(p.midi==36)check(p.ledgerLines==2&&Math.abs(p.y-(bassBottom+20*density))<density,"C2 uses two bass ledger lines at correct height");
   if(p.midi==48)check(p.ledgerLines==0,"C3 lies on bass staff");
   if(p.midi==53)check(Math.abs(p.y-(bassBottom-30*density))<density,"F3 lies on the bass clef reference line");
   if(p.midi==60)check(p.ledgerLines==1&&Math.abs(p.y-(trebleBottom+10*density))<density,"C4 has its treble ledger line");
   if(p.midi==67)check(Math.abs(p.y-(trebleBottom-10*density))<density,"G4 lies on the treble clef reference line");
   if(p.midi==84)check(p.ledgerLines==2&&Math.abs(p.y-(trebleBottom-60*density))<density,"C6 has two correct treble ledger lines");
  }
  staff.setRenderListener(new DiscoveryStaffView.RenderListener(){
   @Override public void onError(String message){throw new IllegalStateException("Discovery OSMD rendering failed "+sample.file+": "+message);}
   @Override public void onRendered(){staff.setRenderListener(null);staff.inspectRenderedClefs(raw->{
    try{
     String payload=(String)new JSONTokener(raw).nextValue();JSONArray array=new JSONArray(payload);
     check(array.length()==sample.clefCount,"actual OSMD clef glyph count matches staff count: "+sample.file);
     StringBuilder joined=new StringBuilder();for(int i=0;i<array.length();i++){String mark=array.getString(i);check(mark.contains("vf-clef")&&(mark.contains("path")||mark.contains("text")),"actual SVG clef symbol was rendered, not just a clef metadata field: "+sample.file);joined.append(mark);}
     if(sample.clefs.equals("G"))glyphs.put("G",joined.toString());if(sample.clefs.equals("F"))glyphs.put("F",joined.toString());
     Bitmap image=Bitmap.createBitmap(staff.getWidth(),staff.getHeight(),Bitmap.Config.ARGB_8888);staff.draw(new Canvas(image));
     try(FileOutputStream out=new FileOutputStream(new File(a.getCacheDir(),sample.file))){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}
     renderCases(a,d,cases,index+1,glyphs);
    }catch(Exception ex){throw new IllegalStateException("Unable to verify rendered clef "+sample.file,ex);}
   });}
  });
 }
 private static int count(String source,String part){int found=0,at=0;while((at=source.indexOf(part,at))>=0){found++;at+=part.length();}return found;}
 private static void layoutStaff(Activity a,DiscoveryStaffView v,int widthDp,int heightDp){float density=a.getResources().getDisplayMetrics().density;int width=(int)(widthDp*density),height=(int)(heightDp*density);v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));v.layout(0,0,width,height);}
}

package br.org.pianolivre;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/** Debug-only emulator UI contract test. All notes are synthetic, never hardware evidence. */
final class MusicLabDiscoverySelfTest {
 private static int checks;
 private static void check(boolean ok,String label){checks++;if(!ok)throw new IllegalStateException("Discovery UI: "+label);}
 private static void collect(View v,List<View> out){out.add(v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collect(g.getChildAt(i),out);}}
 private static List<View> tree(View root){ArrayList<View> out=new ArrayList<>();collect(root,out);return out;}
 private static void clickButton(Activity a,String label){for(View v:tree(a.getWindow().getDecorView()))if(v instanceof Button&&label.equals(((Button)v).getText().toString())){v.performClick();return;}throw new IllegalStateException("Missing button "+label);}
 static void run(MainActivity a){
  PianoScreens s=a.screens;MusicLabDiscoveryScreen d=s.discovery;
  clickButton(a,"Laboratório Musical");check(s.isLabOpen(),"home entry opens Descobrir");
  check(d.discover.getVisibility()==View.VISIBLE&&!d.discover.isEnabled(),"Descobrir selected");
  check(d.learn.getVisibility()==View.VISIBLE&&!d.learn.isEnabled(),"Aprender visible and disabled");
  check(d.instruction.getText().toString().equals(MusicLabDiscoveryScreen.EMPTY_TEXT),"exact empty instruction");
  List<View> elements=tree(d);int buttons=0;for(View v:elements)if(v instanceof Button)buttons++;
  check(buttons==3,"only toolbar controls in empty state");
  for(View v:elements)if(v instanceof TextView){String t=((TextView)v).getText().toString();check(!Arrays.asList("Tocar","Montar","Iniciar","Gravar","Praticar","Metrônomo","Demonstração").contains(t),"legacy control absent: "+t);}
  long conn=a.midiGeneration,ui=s.discoveryGeneration();d.session.reconnect(conn,null);
  d.midiAttack(60,100,1,conn,ui);d.midiRelease(60,conn,ui);d.midiBatchEnd(1,101,ui);
  check(d.title.getText().toString().equals("DÓ - C4"),"single note label");
  setStaff(a,d.staff,Collections.singletonList(60),false);List<DiscoveryStaffView.NotePosition> pos=d.staff.layoutSnapshot();
  check(pos.size()==1&&pos.get(0).midi==60&&pos.get(0).clef.equals("sol")&&pos.get(0).ledgerLines==1,"C4 treble ledger geometry");
  setStaff(a,d.staff,Collections.singletonList(36),false);pos=d.staff.layoutSnapshot();
  check(pos.get(0).clef.equals("fá")&&pos.get(0).ledgerLines==2,"C2 bass ledger geometry");
  check(Math.abs(pos.get(0).y-(bottom(d.staff)+20*density(d.staff)))<1,"C2 is on second bass ledger");captureAndCheck(a,d.staff,false,"discovery-staff-c2.png");
  setStaff(a,d.staff,Collections.singletonList(48),false);pos=d.staff.layoutSnapshot();
  check(pos.get(0).clef.equals("fá")&&pos.get(0).ledgerLines==0,"C3 remains on bass staff");captureAndCheck(a,d.staff,false,"discovery-staff-c3.png");
  setStaff(a,d.staff,Collections.singletonList(53),false);pos=d.staff.layoutSnapshot();
  check(pos.get(0).clef.equals("fá")&&Math.abs(pos.get(0).y-(bottom(d.staff)-30*density(d.staff)))<1,"F3 is on fourth bass line");captureAndCheck(a,d.staff,false,"discovery-staff-f3.png");
  setStaff(a,d.staff,Collections.singletonList(67),false);pos=d.staff.layoutSnapshot();
  check(pos.get(0).clef.equals("sol")&&Math.abs(pos.get(0).y-(bottom(d.staff)-10*density(d.staff)))<1,"G4 is on second treble line");captureAndCheck(a,d.staff,true,"discovery-staff-g4.png");
  setStaff(a,d.staff,Collections.singletonList(84),false);pos=d.staff.layoutSnapshot();
  check(pos.get(0).clef.equals("sol")&&pos.get(0).ledgerLines==2,"C6 treble ledger geometry");captureAndCheck(a,d.staff,true,"discovery-staff-c6.png");
  setStaff(a,d.staff,Arrays.asList(36,48,53),false);pos=d.staff.layoutSnapshot();
  check(pos.size()==3&&pos.get(0).x==pos.get(2).x&&pos.get(0).clef.equals("fá")&&pos.get(2).clef.equals("fá"),"low chord shares a bass-clef column");captureAndCheck(a,d.staff,false,"discovery-staff-low-chord.png");
  setStaff(a,d.staff,Arrays.asList(60,64,67,72),false);pos=d.staff.layoutSnapshot();
  check(pos.size()==4&&pos.get(0).clef.equals("sol")&&pos.get(3).clef.equals("sol"),"high chord uses treble clef");captureAndCheck(a,d.staff,true,"discovery-staff-high-chord.png");
  setStaff(a,d.staff,Arrays.asList(48,60,67,72),false);pos=d.staff.layoutSnapshot();
  check(pos.get(0).clef.equals("fá")&&pos.get(1).clef.equals("sol")&&pos.get(2).clef.equals("sol"),"wide chord assigns bass below C4 and treble from C4");captureAndCheck(a,d.staff,false,"discovery-staff-grand-chord.png");
  setStaff(a,d.staff,Arrays.asList(55,59,60,62),true);pos=d.staff.layoutSnapshot();
  check(pos.size()==4&&pos.get(0).clef.equals("fá")&&pos.get(2).clef.equals("sol")&&pos.get(0).x<pos.get(3).x,"sequence crossing C4 preserves order and both staves");captureAndCheck(a,d.staff,true,"discovery-staff-grand-sequence.png");
  d.session.reconnect(conn,null);d.midiAttack(60,200,2,conn,ui);d.midiAttack(64,200,2,conn,ui);d.midiAttack(67,200,2,conn,ui);d.midiRelease(60,conn,ui);d.midiRelease(64,conn,ui);d.midiRelease(67,conn,ui);d.midiBatchEnd(2,201,ui);
  MusicDiscoverySession.Result chord=d.session.result();check(chord.kind==MusicDiscoverySession.Kind.CHORD&&chord.notes.size()==3,"synthetic chord classification");setStaff(a,d.staff,chord.notes,false);pos=d.staff.layoutSnapshot();
  check(pos.size()==3&&pos.get(0).x==pos.get(2).x,"chord heads share a simultaneity column");
  d.session.reconnect(conn,null);int[] scale={60,62,64,65,67,69,71,72};long batch=3,time=300;
  for(int n:scale){d.midiAttack(n,time,batch,conn,ui);d.midiBatchEnd(batch,time+1,ui);d.midiRelease(n,conn,ui);batch++;time+=2;}
  MusicDiscoverySession.Result seq=d.session.result();check(seq.kind==MusicDiscoverySession.Kind.SEQUENCE&&seq.notes.size()==8,"full sequence displayed");setStaff(a,d.staff,seq.notes,true);pos=d.staff.layoutSnapshot();
  check(pos.size()==8&&pos.get(0).midi==60&&pos.get(7).midi==72&&pos.get(0).x<pos.get(7).x,"ordered sequence staff geometry");
  d.session.reconnect(conn,null);d.midiAttack(60,time,batch,conn,ui);d.midiRelease(60,conn,ui);d.midiBatchEnd(batch++,time+1,ui);
  String prior=d.title.getText().toString();d.midiAttack(62,time+2,batch++,conn,ui);
  check(d.session.phase()==MusicDiscoverySession.Phase.PENDING_WITH_RESULT&&d.session.result().notes.size()==1,"old result persists while new note is pending");
  d.pause();d.resume();check(d.session.result().notes.size()==1,"pause and return preserve last result");
  d.back.performClick();check(!s.isLabOpen(),"Voltar returns to library");
  android.util.Log.i("PianoLivreDiscovery","DISCOVERY_UI_SELFTEST_PASS checks="+checks+" synthetic_midi=true treble=C4 ledger=1 bass=C2,C3,F3 chord_heads=3 sequence_heads=8 title="+prior);
 }
 private static float density(DiscoveryStaffView v){return v.getResources().getDisplayMetrics().density;}
 private static float bottom(DiscoveryStaffView v){float d=density(v);return Math.max(18*d,v.getHeight()*.25f)+40*d;}
 private static void setStaff(Activity a,DiscoveryStaffView v,List<Integer> notes,boolean sequence){v.setNotes(notes,sequence);layoutStaff(a,v);}
 private static void layoutStaff(Activity a,DiscoveryStaffView v){float d=a.getResources().getDisplayMetrics().density;int w=(int)(360*d),h=(int)(160*d);v.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));v.layout(0,0,w,h);}
 private static void captureAndCheck(Activity a,DiscoveryStaffView staff,boolean treble,String name){
  float d=density(staff);List<DiscoveryStaffView.NotePosition> positions=staff.layoutSnapshot();boolean grand=positions.stream().anyMatch(n->n.clef.equals("fá"))&&positions.stream().anyMatch(n->n.clef.equals("sol"));
  float top=grand?Math.max(18*d,staff.getHeight()*.14f):Math.max(18*d,staff.getHeight()*.25f),trebleBottom=top+40*d;
  float bassBottom=grand?top+Math.max(76*d,staff.getHeight()*.43f)+40*d:trebleBottom;
  Bitmap image=Bitmap.createBitmap(staff.getWidth(),staff.getHeight(),Bitmap.Config.ARGB_8888);staff.draw(new Canvas(image));
  if(treble){check(inkNear(image,19*d,trebleBottom-10*d,d),"actual treble symbol contains G4 curl anchor: "+name);}
  if(!grand&&!treble)check(!inkNear(image,19*d,trebleBottom-10*d,d),"actual bass rendering omits treble curl: "+name);
  if(grand||!treble){float f3=bassBottom-30*d;check(inkNear(image,29*d,f3-5*d,d),"actual bass symbol upper dot flanks F3: "+name);check(inkNear(image,29*d,f3+5*d,d),"actual bass symbol lower dot flanks F3: "+name);}
  for(DiscoveryStaffView.NotePosition n:positions)if(n.midi==36||n.midi==84){float staffBottom=n.clef.equals("sol")?trebleBottom:bassBottom;float y=n.midi==36?staffBottom+20*d:staffBottom-60*d;check(inkNear(image,50*d,y,d),"actual supplementary ledger is at C2/C6 pitch: "+name);}
  try(FileOutputStream out=new FileOutputStream(new File(a.getCacheDir(),name))){image.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception ex){throw new IllegalStateException("Unable to save staff capture "+name,ex);}finally{image.recycle();}
 }
 private static boolean inkNear(Bitmap image,float x,float y,float d){int cx=Math.round(x),cy=Math.round(y),r=Math.max(1,Math.round(d*1.5f));for(int yy=Math.max(0,cy-r);yy<=Math.min(image.getHeight()-1,cy+r);yy++)for(int xx=Math.max(0,cx-r);xx<=Math.min(image.getWidth()-1,cx+r);xx++){int p=image.getPixel(xx,yy);if(Color.red(p)<100&&Color.green(p)<120&&Color.blue(p)<150)return true;}return false;}
}

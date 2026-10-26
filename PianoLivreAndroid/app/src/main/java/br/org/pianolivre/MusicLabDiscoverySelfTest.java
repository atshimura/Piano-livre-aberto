package br.org.pianolivre;

import android.app.Activity;
import android.view.*;
import android.widget.*;
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
  layoutStaff(a,d.staff);List<DiscoveryStaffView.NotePosition> pos=d.staff.layoutSnapshot();
  check(pos.size()==1&&pos.get(0).midi==60&&pos.get(0).clef.equals("sol")&&pos.get(0).ledgerLines==1,"C4 treble ledger geometry");
  d.staff.setNotes(Collections.singletonList(36),false);layoutStaff(a,d.staff);pos=d.staff.layoutSnapshot();
  check(pos.get(0).clef.equals("fá")&&pos.get(0).ledgerLines==2,"C2 bass ledger geometry");
  d.staff.setNotes(Collections.singletonList(84),false);layoutStaff(a,d.staff);pos=d.staff.layoutSnapshot();
  check(pos.get(0).clef.equals("sol")&&pos.get(0).ledgerLines==2,"C6 treble ledger geometry");
  d.staff.setNotes(Arrays.asList(48,72),true);layoutStaff(a,d.staff);pos=d.staff.layoutSnapshot();
  check(pos.get(0).clef.equals("fá")&&pos.get(1).clef.equals("sol"),"grand staff switches clef across C4");
  d.session.reconnect(conn,null);d.midiAttack(60,200,2,conn,ui);d.midiAttack(64,200,2,conn,ui);d.midiAttack(67,200,2,conn,ui);d.midiRelease(60,conn,ui);d.midiRelease(64,conn,ui);d.midiRelease(67,conn,ui);d.midiBatchEnd(2,201,ui);
  MusicDiscoverySession.Result chord=d.session.result();check(chord.kind==MusicDiscoverySession.Kind.CHORD&&chord.notes.size()==3,"synthetic chord classification");layoutStaff(a,d.staff);pos=d.staff.layoutSnapshot();
  check(pos.size()==3&&pos.get(0).x==pos.get(2).x,"chord heads share a simultaneity column");
  d.session.reconnect(conn,null);int[] scale={60,62,64,65,67,69,71,72};long batch=3,time=300;
  for(int n:scale){d.midiAttack(n,time,batch,conn,ui);d.midiBatchEnd(batch,time+1,ui);d.midiRelease(n,conn,ui);batch++;time+=2;}
  MusicDiscoverySession.Result seq=d.session.result();check(seq.kind==MusicDiscoverySession.Kind.SEQUENCE&&seq.notes.size()==8,"full sequence displayed");
  layoutStaff(a,d.staff);pos=d.staff.layoutSnapshot();
  check(pos.size()==8&&pos.get(0).midi==60&&pos.get(7).midi==72&&pos.get(0).x<pos.get(7).x,"ordered sequence staff geometry");
  String prior=d.title.getText().toString();d.session.attack(61,time,batch,conn);
  check(d.session.phase()==MusicDiscoverySession.Phase.PENDING_WITH_RESULT&&d.session.result().notes.size()==8,"old result persists while new note is pending");
  d.pause();d.resume();check(d.session.result().notes.size()==8,"pause and return preserve last result");
  d.back.performClick();check(!s.isLabOpen(),"Voltar returns to library");
  android.util.Log.i("PianoLivreDiscovery","DISCOVERY_UI_SELFTEST_PASS checks="+checks+" synthetic_midi=true treble=C4 ledger=1 chord_heads=3 sequence_heads=8 title="+prior);
 }
 private static void layoutStaff(Activity a,DiscoveryStaffView staff){int w=Math.max(320,a.getResources().getDisplayMetrics().widthPixels),h=Math.max(160,(int)(140*a.getResources().getDisplayMetrics().density));staff.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));staff.layout(0,0,w,h);}
}

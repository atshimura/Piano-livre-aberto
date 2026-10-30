package br.org.pianolivre;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.util.*;

/** Minimal public Descobrir surface. The legacy catalog/practice view remains intact. */
final class MusicLabDiscoveryScreen extends LinearLayout {
 static final String EMPTY_TEXT="toque uma nota, acorde ou escala musical";
 final MainActivity activity;
 final MusicDiscoverySession session=new MusicDiscoverySession();
 final Button back,discover,learn;
 final TextView instruction,title,detail,ciphers;
 final HorizontalScrollView ciphersScroll;
 final DiscoveryStaffView staff;
 private final Handler handler=new Handler();private volatile long callbackGeneration=0;
 private boolean observing=false;
 MusicLabDiscoveryScreen(MainActivity a,Runnable goBack){super(a);activity=a;setOrientation(VERTICAL);setBackgroundColor(0xffF7F9FC);setVisibility(GONE);
  LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),dp(3),dp(8),dp(3));bar.setBackgroundColor(0xff101C31);addView(bar,new LayoutParams(-1,dp(50)));
  back=button("Voltar",false);back.setOnClickListener(v->goBack.run());bar.addView(back,new LayoutParams(0,-1,1));
  discover=button("Descobrir",true);discover.setEnabled(false);discover.setSelected(true);discover.setContentDescription("Descobrir selecionado");bar.addView(discover,new LayoutParams(0,-1,1));
  learn=button("Aprender",false);learn.setEnabled(false);learn.setContentDescription("Aprender indisponível nesta versão");bar.addView(learn,new LayoutParams(0,-1,1));
  LinearLayout content=new LinearLayout(a);content.setPadding(dp(10),dp(10),dp(10),dp(10));addView(content,new LayoutParams(-1,0,1));
  staff=new DiscoveryStaffView(a);content.addView(staff,new LayoutParams(0,-1,3));
  ScrollView information=new ScrollView(a);information.setFillViewport(true);information.setBackgroundColor(0xffEAF1FA);content.addView(information,new LayoutParams(0,-1,1));
  LinearLayout panel=new LinearLayout(a);panel.setOrientation(VERTICAL);panel.setPadding(dp(12),dp(16),dp(12),dp(16));information.addView(panel);
  instruction=new TextView(a);instruction.setText(EMPTY_TEXT);instruction.setTextSize(18);instruction.setTextColor(0xff314963);instruction.setPadding(0,0,0,dp(16));panel.addView(instruction,new LayoutParams(-1,-2));
  LinearLayout result=new LinearLayout(a);result.setOrientation(VERTICAL);result.setVisibility(GONE);panel.addView(result,new LayoutParams(-1,-2));
  title=new TextView(a);title.setTextSize(22);title.setTypeface(null,Typeface.BOLD);title.setTextColor(0xff172A44);result.addView(title,new LayoutParams(-1,-2));
  detail=new TextView(a);detail.setTextSize(16);detail.setTextColor(0xff52677F);detail.setPadding(0,dp(12),0,dp(12));result.addView(detail,new LayoutParams(-1,-2));
  ciphers=new TextView(a);ciphers.setTextSize(16);ciphers.setTextColor(0xff314963);ciphers.setContentDescription("Cifras das notas identificadas");
  ciphersScroll=new HorizontalScrollView(a); // Kept as an internal compatibility handle; text now wraps in the side panel.
  result.addView(ciphers,new LayoutParams(-1,-2));
  this.resultView=result;
 }
 private final LinearLayout resultView;
 long sessionGeneration(){return callbackGeneration;}
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private Button button(String text,boolean selected){Button b=new Button(activity);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(null,Typeface.BOLD);b.setTextColor(selected?Color.WHITE:0xffDCE6F1);GradientDrawable bg=new GradientDrawable();bg.setColor(selected?0xff286BCB:Color.TRANSPARENT);bg.setCornerRadius(dp(8));b.setBackground(bg);b.setPadding(dp(4),0,dp(4),0);return b;}
 void enter(){staff.onResume();staff.resetFraming();observing=true;callbackGeneration++;shownKey="";session.reset();session.reconnect(activity.midiGeneration,activity.waitRecognizer.chordWindowMs());showState();}
 void leave(){observing=false;callbackGeneration++;session.cancelPending();staff.onPause();}
 void pause(){observing=false;callbackGeneration++;session.cancelPending();staff.onPause();}
 void resume(){if(getVisibility()==VISIBLE){staff.onResume();observing=true;callbackGeneration++;session.reconnect(activity.midiGeneration,activity.waitRecognizer.chordWindowMs());}}
 void midiAttack(int pitch,long time,long batch,long generation,long uiGeneration){if(!observing||getVisibility()!=VISIBLE||uiGeneration!=callbackGeneration)return;session.attack(pitch,time,batch,generation);showState();scheduleBoundary();}
 void midiRelease(int pitch,long generation,long uiGeneration){if(observing&&uiGeneration==callbackGeneration){session.release(pitch,generation);showState();}}
 void midiBatchEnd(long batch,long now,long uiGeneration){if(!observing||uiGeneration!=callbackGeneration)return;session.finishBatch(batch,now);showState();}
 private void scheduleBoundary(){long delay=session.nextDelayMs(android.os.SystemClock.uptimeMillis());if(delay<=0)return;long token=callbackGeneration;handler.postDelayed(()->{if(token==callbackGeneration&&observing){session.advance(android.os.SystemClock.uptimeMillis());showState();scheduleBoundary();}},delay);}
 void connectionChanged(long generation){session.reconnect(generation,activity.waitRecognizer.chordWindowMs());showState();}
 private String shownKey="";
 private void showState(){
  MusicDiscoverySession.Result r=session.result();
  ArrayList<Integer> active=new ArrayList<>(session.downNotes());Collections.sort(active);
  boolean preview=!active.isEmpty()&&(r==null||!r.notes.containsAll(active));
  if(r==null&&!preview){instruction.setText(EMPTY_TEXT);instruction.setVisibility(VISIBLE);resultView.setVisibility(GONE);return;}
  instruction.setVisibility(GONE);resultView.setVisibility(VISIBLE);
  List<Integer> notes=preview?active:r.notes;boolean ordered=!preview&&r.ordered;
  String name=preview?(active.size()==1?MusicNoteSpelling.portuguese(active.get(0))+" - "+MusicNoteSpelling.international(active.get(0)):"Notas em execu\u00e7\u00e3o"):r.title;
  String auxiliary=preview?"Entrada atual":r.detail;
  String key=notes.toString()+ordered+name+auxiliary+active.isEmpty();
  if(key.equals(shownKey))return;shownKey=key;title.setText(name);
  detail.setText(auxiliary.isEmpty()?(active.isEmpty()?"\u00daltimo resultado":"Entrada atual"):auxiliary);detail.setVisibility(VISIBLE);
  ArrayList<Integer> pitches=new ArrayList<>(notes);if(!ordered)Collections.sort(pitches);
  StringBuilder symbols=new StringBuilder("Cifras: ");for(int i=0;i<pitches.size();i++){if(i>0)symbols.append(" \u00b7 ");symbols.append(MusicNoteSpelling.international(pitches.get(i)));}

  ciphers.setText(symbols.toString());staff.setNotes(notes,ordered);
 }

 void dispose(){observing=false;callbackGeneration++;staff.dispose();}
}

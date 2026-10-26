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
 final TextView instruction,title,detail;
 final DiscoveryStaffView staff;
 private final Handler handler=new Handler();private volatile long callbackGeneration=0;
 private boolean observing=false;
 MusicLabDiscoveryScreen(MainActivity a,Runnable goBack){super(a);activity=a;setOrientation(VERTICAL);setBackgroundColor(0xffF7F9FC);setVisibility(GONE);
  LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),dp(3),dp(8),dp(3));bar.setBackgroundColor(0xff101C31);addView(bar,new LayoutParams(-1,dp(50)));
  back=button("Voltar",false);back.setOnClickListener(v->goBack.run());bar.addView(back,new LayoutParams(0,-1,1));
  discover=button("Descobrir",true);discover.setEnabled(false);discover.setSelected(true);discover.setContentDescription("Descobrir selecionado");bar.addView(discover,new LayoutParams(0,-1,1));
  learn=button("Aprender",false);learn.setEnabled(false);learn.setContentDescription("Aprender indisponível nesta versão");bar.addView(learn,new LayoutParams(0,-1,1));
  FrameLayout content=new FrameLayout(a);addView(content,new LayoutParams(-1,0,1));
  instruction=new TextView(a);instruction.setText(EMPTY_TEXT);instruction.setTextSize(20);instruction.setTypeface(null,Typeface.BOLD);instruction.setTextColor(0xff314963);instruction.setGravity(Gravity.CENTER);instruction.setPadding(dp(24),dp(18),dp(24),dp(18));content.addView(instruction,new FrameLayout.LayoutParams(-1,-1));
  LinearLayout result=new LinearLayout(a);result.setOrientation(VERTICAL);result.setPadding(dp(12),dp(10),dp(12),dp(12));result.setVisibility(GONE);content.addView(result,new FrameLayout.LayoutParams(-1,-1));
  title=new TextView(a);title.setTextSize(22);title.setTypeface(null,Typeface.BOLD);title.setTextColor(0xff172A44);title.setGravity(Gravity.CENTER);result.addView(title,new LayoutParams(-1,dp(40)));
  detail=new TextView(a);detail.setTextSize(15);detail.setTextColor(0xff52677F);detail.setGravity(Gravity.CENTER);detail.setVisibility(GONE);result.addView(detail,new LayoutParams(-1,dp(30)));
  HorizontalScrollView staffScroll=new HorizontalScrollView(a);staffScroll.setFillViewport(true);staffScroll.setHorizontalScrollBarEnabled(true);result.addView(staffScroll,new LayoutParams(-1,0,1));
  staff=new DiscoveryStaffView(a);staffScroll.addView(staff,new HorizontalScrollView.LayoutParams(-1,-1));
  this.resultView=result;
 }
 private final LinearLayout resultView;
 long sessionGeneration(){return callbackGeneration;}
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private Button button(String text,boolean selected){Button b=new Button(activity);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(null,Typeface.BOLD);b.setTextColor(selected?Color.WHITE:0xffDCE6F1);GradientDrawable bg=new GradientDrawable();bg.setColor(selected?0xff286BCB:Color.TRANSPARENT);bg.setCornerRadius(dp(8));b.setBackground(bg);b.setPadding(dp(4),0,dp(4),0);return b;}
 void enter(){observing=true;callbackGeneration++;session.reset();session.reconnect(activity.midiGeneration,activity.waitRecognizer.chordWindowMs());showState();}
 void leave(){observing=false;callbackGeneration++;session.cancelPending();}
 void pause(){observing=false;callbackGeneration++;session.cancelPending();}
 void resume(){if(getVisibility()==VISIBLE){observing=true;callbackGeneration++;session.reconnect(activity.midiGeneration,activity.waitRecognizer.chordWindowMs());}}
 void midiAttack(int pitch,long time,long batch,long generation,long uiGeneration){if(!observing||getVisibility()!=VISIBLE||uiGeneration!=callbackGeneration)return;session.attack(pitch,time,batch,generation);showState();scheduleBoundary();}
 void midiRelease(int pitch,long generation,long uiGeneration){if(observing&&uiGeneration==callbackGeneration){session.release(pitch,generation);showState();}}
 void midiBatchEnd(long batch,long now,long uiGeneration){if(!observing||uiGeneration!=callbackGeneration)return;session.finishBatch(batch,now);showState();}
 private void scheduleBoundary(){long delay=session.nextDelayMs(android.os.SystemClock.uptimeMillis());if(delay<=0)return;long token=callbackGeneration;handler.postDelayed(()->{if(token==callbackGeneration&&observing){session.advance(android.os.SystemClock.uptimeMillis());showState();scheduleBoundary();}},delay);}
 void connectionChanged(long generation){session.reconnect(generation,activity.waitRecognizer.chordWindowMs());showState();}
 private void showState(){MusicDiscoverySession.Result r=session.result();if(r==null){instruction.setText(session.phase()==MusicDiscoverySession.Phase.PENDING?"analisando a entrada musical…":EMPTY_TEXT);instruction.setVisibility(VISIBLE);resultView.setVisibility(GONE);return;}instruction.setVisibility(GONE);resultView.setVisibility(VISIBLE);title.setText(r.title);detail.setText(r.detail);detail.setVisibility(r.detail.isEmpty()?GONE:VISIBLE);staff.setNotes(r.notes,r.ordered);int need=dp(r.ordered?70+r.notes.size()*42:300);int width=Math.max(getResources().getDisplayMetrics().widthPixels-dp(24),need);ViewGroup.LayoutParams lp=staff.getLayoutParams();lp.width=width;staff.setLayoutParams(lp);}
}

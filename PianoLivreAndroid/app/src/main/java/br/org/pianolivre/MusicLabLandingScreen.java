package br.org.pianolivre;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * ZAI-010: a landscape-first, offline entry point for the Musical Laboratory.
 * This is navigation only: it does not create MIDI events or activate
 * the not-yet-implemented Learn mode.
 */
final class MusicLabLandingScreen extends LinearLayout {
 private final MainActivity activity;
 private static final int NAVY=0xff0C2445,BLUE=0xff0969D2,INK=0xff09224A,MUTED=0xff54677F;
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private GradientDrawable background(int color,int radius,int stroke){
  GradientDrawable drawable=new GradientDrawable();
  drawable.setColor(color);drawable.setCornerRadius(dp(radius));
  if(stroke!=0)drawable.setStroke(dp(1),stroke);
  return drawable;
 }
 private TextView text(String content,int size,int color,boolean bold){
  TextView result=new TextView(activity);
  result.setText(content);result.setTextSize(size);result.setTextColor(color);
  result.setGravity(Gravity.CENTER);
  if(bold)result.setTypeface(null,Typeface.BOLD);
  return result;
 }
 private Button action(String label,boolean enabled,Runnable click){
  Button result=new Button(activity);
  result.setText(label);result.setAllCaps(false);result.setTextSize(16);
  result.setTypeface(null,Typeface.BOLD);result.setTextColor(enabled?Color.WHITE:MUTED);
  result.setMinHeight(0);result.setMinimumHeight(0);
  result.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22ffffff),
    background(enabled?BLUE:0xffDBE6F4,12,0),null));
  result.setEnabled(enabled);
  if(enabled)result.setOnClickListener(v->click.run());
  result.setContentDescription(enabled?label:"Aprender — Em breve, ainda não implementado");
  return result;
 }
 private LinearLayout card(String icon,String heading,String detail,String actionName,boolean available,Runnable click){
  LinearLayout surface=new LinearLayout(activity);surface.setOrientation(VERTICAL);
  surface.setGravity(Gravity.CENTER_HORIZONTAL);
  surface.setPadding(dp(14),dp(14),dp(14),dp(14));
  surface.setBackground(background(available?Color.WHITE:0xffF1F5FB,18,0xffE0E8F3));
  surface.setElevation(dp(2));surface.setTag("lab-landing-"+heading.toLowerCase(java.util.Locale.ROOT));
  TextView symbol=text(icon,46,available?BLUE:0xff8DA4BE,true);
  surface.addView(symbol,new LayoutParams(-1,dp(62)));
  TextView title=text(heading,22,available?INK:MUTED,true);
  surface.addView(title,new LayoutParams(-1,dp(39)));
  TextView explanation=text(detail,13,MUTED,false);explanation.setMaxLines(3);
  explanation.setPadding(dp(4),dp(5),dp(4),dp(5));
  surface.addView(explanation,new LayoutParams(-1,0,1));
  Button open=action(actionName,available,click);
  surface.addView(open,new LayoutParams(-1,dp(46)));
  if(!available){
   TextView badge=text("Disponível em uma atualização futura",11,MUTED,false);
   surface.addView(badge,new LayoutParams(-1,dp(24)));
  }
  return surface;
 }
 MusicLabLandingScreen(MainActivity a,Runnable goBack,Runnable goDiscover,Runnable goCreation){
  super(a);activity=a;setTag("lab-landing");setOrientation(VERTICAL);
  setBackgroundColor(0xffF6F9FF);setVisibility(GONE);
  LinearLayout top=new LinearLayout(a);top.setGravity(Gravity.CENTER_VERTICAL);
  top.setPadding(dp(10),dp(5),dp(12),dp(5));top.setBackgroundColor(NAVY);
  addView(top,new LayoutParams(-1,dp(56)));
  Button back=action("← Biblioteca",true,goBack);back.setTextSize(13);
  back.setBackground(background(0xff283F5C,12,0));back.setTag("lab-landing-back");
  top.addView(back,new LayoutParams(dp(136),dp(44)));
  TextView label=text("♫  Laboratório Musical",20,Color.WHITE,true);
  label.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
  label.setPadding(dp(14),0,0,0);
  top.addView(label,new LayoutParams(0,-1,1));
  ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);
  addView(scroll,new LayoutParams(-1,0,1));
  LinearLayout content=new LinearLayout(a);content.setOrientation(VERTICAL);
  content.setPadding(dp(14),dp(8),dp(14),dp(14));
  scroll.addView(content);
  TextView choice=text("Escolha um modo",25,INK,true);
  content.addView(choice,new LayoutParams(-1,dp(43)));
  TextView description=text("Selecione como deseja usar o Laboratório Musical.",14,MUTED,false);
  content.addView(description,new LayoutParams(-1,dp(30)));
  LinearLayout columns=new LinearLayout(a);columns.setOrientation(HORIZONTAL);
  columns.setGravity(Gravity.TOP);columns.setTag("lab-landing-three-columns");
  LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(-1,dp(260));
  rowLp.topMargin=dp(9);content.addView(columns,rowLp);
  LinearLayout discover=card("♫","Descobrir",
    "Explore notas, acordes e ideias musicais de forma livre.",
    "Abrir Descobrir   →",true,goDiscover);
  LinearLayout learn=card("♧","Aprender",
    "Modo voltado ao estudo guiado e acompanhamento da prática.",
    "◷  Em breve",false,()->{});
  LinearLayout creation=card("♬","Criação",
    "Grave ideias musicais e transforme suas gravações em partitura.",
    "Abrir Criação   →",true,goCreation);
  LinearLayout[] cards={discover,learn,creation};
  for(int i=0;i<cards.length;i++){
   LinearLayout.LayoutParams item=new LinearLayout.LayoutParams(0,-1,1);
   if(i<2)item.rightMargin=dp(10);
   columns.addView(cards[i],item);
  }
 }
}

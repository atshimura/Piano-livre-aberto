package br.org.pianolivre;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Single navigation component shared by the Laboratory's main tabs. */
final class MusicLabNavigationBar {
 static final int DISCOVER=0, LEARN=1, CREATION=2;
 private static final int NAVY=0xff101C31, PANEL=0xff243955, ACTIVE=0xff286BCB, WHITE=0xffF4F8FF, MUTED=0xff899CB4;
 private MusicLabNavigationBar(){}

 private static int dp(LinearLayout parent,float value){
  return Math.round(value*parent.getResources().getDisplayMetrics().density);
 }
 private static GradientDrawable shape(LinearLayout parent,int color,int radius){
  GradientDrawable d=new GradientDrawable();
  d.setColor(color);d.setCornerRadius(dp(parent,radius));return d;
 }
 private static Button button(LinearLayout root,String label,String accessible,int color,int fontSize){
  Button b=new Button(root.getContext());
  b.setText(label);b.setAllCaps(false);b.setTextSize(fontSize);
  b.setTypeface(null,Typeface.BOLD);
  b.setTextColor(color);b.setMinWidth(0);b.setMinimumWidth(0);
  b.setMinHeight(0);b.setMinimumHeight(0);
  b.setPadding(dp(root,3),0,dp(root,3),0);
  b.setGravity(Gravity.CENTER);b.setContentDescription(accessible);
  return b;
 }
 private static void background(LinearLayout root,Button button,int color){
  GradientDrawable surface=shape(root,color,10);
  button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff),surface,null));
 }
 private static void gap(LinearLayout parent,int width){
  View space=new View(parent.getContext());parent.addView(space,new LinearLayout.LayoutParams(dp(parent,width),dp(parent,1)));
 }

 static void populate(LinearLayout bar,int selected,Runnable back,Runnable goDiscover,Runnable goCreation){
  if(selected!=DISCOVER&&selected!=LEARN&&selected!=CREATION)throw new IllegalArgumentException("Invalid Laboratory tab");
  bar.removeAllViews();bar.setOrientation(LinearLayout.HORIZONTAL);bar.setGravity(Gravity.CENTER_VERTICAL);
  bar.setTag("lab-navigation");bar.setBackgroundColor(NAVY);
  int widthDp=Math.round(bar.getResources().getDisplayMetrics().widthPixels/bar.getResources().getDisplayMetrics().density);
  boolean small=widthDp<650,verySmall=widthDp<460;
  bar.setPadding(dp(bar,8),dp(bar,4),dp(bar,8),dp(bar,4));
  Button backButton=button(bar,verySmall?"←":"← Biblioteca","Voltar",WHITE,verySmall?21:12);
  backButton.setTag("lab-nav-back");background(bar,backButton,PANEL);
  backButton.setOnClickListener(v->back.run());
  bar.addView(backButton,new LinearLayout.LayoutParams(dp(bar,verySmall?44:small?108:122),dp(bar,44)));
  gap(bar,8);
  if(!small){
   TextView title=new TextView(bar.getContext());title.setText("♫  Laboratório Musical");
   title.setTypeface(null,Typeface.BOLD);title.setTextColor(WHITE);title.setTextSize(16);
   title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);
   title.setGravity(Gravity.CENTER_VERTICAL);
   title.setContentDescription("Laboratório Musical");
   bar.addView(title,new LinearLayout.LayoutParams(0,dp(bar,44),1));
   gap(bar,12);
  }
  LinearLayout tabs=new LinearLayout(bar.getContext());tabs.setOrientation(LinearLayout.HORIZONTAL);
  tabs.setGravity(Gravity.CENTER_VERTICAL);tabs.setPadding(dp(bar,4),dp(bar,4),dp(bar,4),dp(bar,4));
  tabs.setTag("lab-navigation-tabs");tabs.setBackground(shape(bar,PANEL,13));
  bar.addView(tabs,small?new LinearLayout.LayoutParams(0,dp(bar,48),1):new LinearLayout.LayoutParams(dp(bar,390),dp(bar,48)));
  String[] names={"Descobrir","Aprender","Criação"};
  for(int i=0;i<names.length;i++){
   final int tab=i;boolean active=i==selected;boolean unavailable=i==LEARN;
   Button b=button(bar,names[i],names[i]+(active?" selecionado":unavailable?" indisponível nesta versão":""),unavailable?MUTED:WHITE,verySmall?11:13);
   b.setTag("lab-nav-"+names[i].toLowerCase(java.util.Locale.ROOT));
   background(bar,b,active?ACTIVE:PANEL);
   b.setSelected(active);
   b.setEnabled(!unavailable);
   if(active)b.setClickable(false);
   if(!active&&!unavailable)b.setOnClickListener(v->{if(tab==DISCOVER)goDiscover.run();else goCreation.run();});
   tabs.addView(b,new LinearLayout.LayoutParams(0,dp(bar,40),1));
  }
 }
}

package br.org.pianolivre;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;

/** Three-octave local touchscreen input. A guide is a visual hint, not MIDI output. */
final class LearningKeyboardView extends View {
 interface Listener {void onKeys(Set<Integer> down);}
 private final Listener listener;
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final HashMap<Integer,Integer> pointers=new HashMap<>();
 private final HashSet<Integer> guide=new HashSet<>();
 private final int first=48,last=84;
 LearningKeyboardView(Context ctx,Listener callback){
  super(ctx);listener=callback;
  setContentDescription("Teclado virtual musical de Dó3 a Dó6; toque para responder sem instrumento USB-MIDI");
  setBackgroundColor(Color.WHITE);
 }
 private static boolean black(int key){int i=key%12;return i==1||i==3||i==6||i==8||i==10;}
 private float keyWidth(){return getWidth()/22f;}
 void hint(int[] notes,boolean visible){
  guide.clear();if(visible&&notes!=null)for(int n:notes)guide.add(n);
  invalidate();
 }
 void reset(){pointers.clear();send();}
 Set<Integer> held(){return new HashSet<>(pointers.values());}
 private void send(){listener.onKeys(held());invalidate();}
 private int noteAt(float x,float y){
  if(x<0||x>=getWidth()||y<0||y>=getHeight())return -1;
  float w=keyWidth();int whites=0;
  if(y<getHeight()*.62f){
   for(int midi=first;midi<=last;midi++){
    if(!black(midi)){whites++;continue;}
    float bw=w*.62f,start=whites*w-bw*.5f;
    if(x>=start&&x<start+bw)return midi;
   }
  }
  int index=Math.min(21,Math.max(0,(int)(x/w))),count=0;
  for(int n=first;n<=last;n++)if(!black(n)&&count++==index)return n;
  return -1;
 }
 @Override public boolean onTouchEvent(MotionEvent event){
  int action=event.getActionMasked(),idx=event.getActionIndex(),id=event.getPointerId(idx);
  if(action==MotionEvent.ACTION_DOWN&&getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
  if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
   int key=noteAt(event.getX(idx),event.getY(idx));
   if(key>=0)pointers.put(id,key);send();return true;
  }
  if(action==MotionEvent.ACTION_MOVE){
   pointers.clear();
   for(int i=0;i<event.getPointerCount();i++){
    int key=noteAt(event.getX(i),event.getY(i));
    if(key>=0)pointers.put(event.getPointerId(i),key);
   }
   send();return true;
  }
  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP||
     action==MotionEvent.ACTION_CANCEL){
   if(action==MotionEvent.ACTION_CANCEL)pointers.clear();else pointers.remove(id);
   send();return true;
  }
  return true;
 }
 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);float w=keyWidth(),h=getHeight(),whites=0;
  HashSet<Integer> active=new HashSet<>(pointers.values());
  for(int n=first;n<=last;n++){
   if(black(n))continue;
   float left=whites++*w;
   p.setColor(active.contains(n)?0xff8DE1CC:guide.contains(n)?0xff92C7FF:0xffF9FBFF);
   canvas.drawRect(left+1,1,left+w-1,h-1,p);
   p.setColor(0xff8B9BB0);p.setStyle(Paint.Style.STROKE);
   canvas.drawRect(left+1,1,left+w-1,h-1,p);p.setStyle(Paint.Style.FILL);
   if(n%12==0&&w>=18*getResources().getDisplayMetrics().density){
    p.setColor(0xff324A67);p.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);
    canvas.drawText("C"+(n/12-1),left+2,h-6,p);
   }
  }
  whites=0;
  for(int n=first;n<=last;n++){
   if(!black(n)){whites++;continue;}
   float bw=w*.62f,left=whites*w-bw*.5f;
   p.setColor(active.contains(n)?0xff0C9A85:guide.contains(n)?0xff2878D6:0xff27374D);
   canvas.drawRoundRect(left,0,left+bw,h*.61f,3,3,p);
  }
 }
}

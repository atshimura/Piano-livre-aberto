package br.org.pianolivre;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;

/** Responsive native notation for notes captured from live MIDI. */
final class DiscoveryStaffView extends View {
 static final class NotePosition {
  final int midi,ledgerLines;final float x,y;final String clef,spelling;
  NotePosition(int midi,float x,float y,int ledger,String clef,String spelling){this.midi=midi;this.x=x;this.y=y;ledgerLines=ledger;this.clef=clef;this.spelling=spelling;}
 }
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private List<Integer> notes=Collections.emptyList();private boolean sequence=false;
 private List<NotePosition> positions=Collections.emptyList();
 private float noteStep;
 DiscoveryStaffView(Context c){super(c);setBackgroundColor(Color.WHITE);setContentDescription("Pauta musical");}
 void setNotes(List<Integer> values,boolean isSequence){notes=values==null?Collections.emptyList():new ArrayList<>(values);sequence=isSequence;invalidate();}
 List<NotePosition> layoutSnapshot(){layoutNotes(getWidth(),getHeight());return Collections.unmodifiableList(positions);}
 private static String spelling(int midi){String[] names={"Dó","Dó♯","Ré","Mi♭","Mi","Fá","Fá♯","Sol","Lá♭","Lá","Si♭","Si"};return names[midi%12]+(midi/12-1);}
 private static int letterIndex(int midi){return new int[]{0,0,1,2,2,3,3,4,5,5,6,6}[midi%12];}
 private static int diatonic(int midi){return (midi/12-1)*7+letterIndex(midi);}
 private void layoutNotes(int w,int h){
  if(notes.isEmpty()||w<=0||h<=0){positions=Collections.emptyList();return;}
  int min=Collections.min(notes),max=Collections.max(notes);boolean grand=min<60&&max>=60;
  float density=getResources().getDisplayMetrics().density;noteStep=5*density;
  float top=grand?Math.max(18*density,h*.14f):Math.max(18*density,h*.25f);float gap=grand?Math.max(76*density,h*.43f):0;
  float bottomTreble=top+8*noteStep,bottomBass=top+gap+8*noteStep;float left=56*density,right=14*density;
  int count=notes.size(),uniqueX=sequence?count:1;float usable=Math.max(1,w-left-right);
  float stepX=count<=1?left+usable*.52f:Math.min(42*density,usable/Math.max(1,uniqueX));
  float start=count<=1?left+usable*.52f:left+Math.max(8*density,(usable-stepX*(uniqueX-1))/2);
  ArrayList<NotePosition> out=new ArrayList<>();
  for(int i=0;i<count;i++){
   int midi=notes.get(i);boolean treble=!grand?max>=60:midi>=60;String clef=treble?"sol":"fá";
   int base=treble?4*7+2:2*7+4;float bottom=treble?bottomTreble:bottomBass;
   int delta=diatonic(midi)-base;float y=bottom-delta*noteStep;
   int ledger=delta<0?(-delta)/2:delta>8?(delta-8)/2:0;
   float x=sequence?start+i*stepX:start;out.add(new NotePosition(midi,x,y,ledger,clef,spelling(midi)));
  }
  positions=out;
 }
 @Override protected void onDraw(Canvas c){super.onDraw(c);int w=getWidth(),h=getHeight();if(w<=0||h<=0)return;
  float density=getResources().getDisplayMetrics().density;p.setColor(0xff1b304d);p.setStrokeWidth(density);p.setStyle(Paint.Style.STROKE);
  if(notes.isEmpty()){c.drawRect(0,0,w,h,p);return;}
  int min=Collections.min(notes),max=Collections.max(notes);boolean grand=min<60&&max>=60;
  noteStep=5*density;float top=grand?Math.max(18*density,h*.14f):Math.max(18*density,h*.25f);float gap=grand?Math.max(76*density,h*.43f):0;
  float[] bottoms=grand?new float[]{top+8*noteStep,top+gap+8*noteStep}:new float[]{top+8*noteStep};
  for(float b:bottoms)for(int i=0;i<5;i++)c.drawLine(42*density,b-i*2*noteStep,w-8*density,b-i*2*noteStep,p);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f*density);
  if(grand){drawTrebleClef(c,19*density,bottoms[0],p);drawBassClef(c,19*density,bottoms[1],p);}
  else if(max>=60)drawTrebleClef(c,19*density,bottoms[0],p);
  else drawBassClef(c,19*density,bottoms[0],p);
  layoutNotes(w,h);p.setTextSize(14*density);
  for(NotePosition n:positions){int d=diatonic(n.midi),base=n.clef.equals("sol")?4*7+2:2*7+4,delta=d-base;
   float bottom=n.clef.equals("sol")?bottoms[0]:bottoms[grand?1:0];
   int firstLedger=delta<0?2:10;for(int k=firstLedger;k<=Math.abs(delta);k+=2){float ly=bottom-Math.copySign((float)k,n.y<bottom?1f:-1f)*noteStep;c.drawLine(n.x-11*density,ly,n.x+11*density,ly,p);}
   if(n.midi%12==1||n.midi%12==3||n.midi%12==6||n.midi%12==8||n.midi%12==10){String acc=(n.midi%12==3||n.midi%12==8||n.midi%12==10)?"♭":"♯";p.setStyle(Paint.Style.FILL);c.drawText(acc,n.x-24*density,n.y+5*density,p);p.setStyle(Paint.Style.STROKE);}
   p.setStyle(Paint.Style.FILL);c.save();c.rotate(-18,n.x,n.y);c.drawOval(n.x-7*density,n.y-4*density,n.x+7*density,n.y+4*density,p);c.restore();p.setStyle(Paint.Style.STROKE);
  }
 }
 private void drawTrebleClef(Canvas c,float x,float bottom,Paint paint){
  float d=getResources().getDisplayMetrics().density;Path path=new Path();path.moveTo(x-1*d,bottom+12*d);path.cubicTo(x-14*d,bottom+4*d,x-14*d,bottom-13*d,x-1*d,bottom-18*d);path.cubicTo(x+12*d,bottom-23*d,x+13*d,bottom-6*d,x+1*d,bottom-3*d);path.cubicTo(x-12*d,bottom+1*d,x-14*d,bottom-14*d,x-2*d,bottom-21*d);path.cubicTo(x+6*d,bottom-28*d,x+4*d,bottom-42*d,x+2*d,bottom-50*d);path.cubicTo(x,bottom-60*d,x-10*d,bottom-59*d,x-12*d,bottom-51*d);path.cubicTo(x-14*d,bottom-43*d,x-2*d,bottom-40*d,x+1*d,bottom-47*d);c.drawPath(path,paint);c.drawLine(x,bottom+12*d,x,bottom-61*d,paint);
 }
 private void drawBassClef(Canvas c,float x,float bottom,Paint paint){
  float d=getResources().getDisplayMetrics().density;float f3=bottom-6*noteStep;Path path=new Path();path.moveTo(x+6*d,f3-15*d);path.cubicTo(x-7*d,f3-23*d,x-14*d,f3-13*d,x-14*d,f3);path.cubicTo(x-14*d,f3+12*d,x-5*d,f3+19*d,x+5*d,f3+13*d);path.cubicTo(x+12*d,f3+9*d,x+10*d,f3+3*d,x+2*d,f3);c.drawPath(path,paint);paint.setStyle(Paint.Style.FILL);c.drawCircle(x+12*d,f3-5*d,1.7f*d,paint);c.drawCircle(x+12*d,f3+5*d,1.7f*d,paint);paint.setStyle(Paint.Style.STROKE);
 }
}

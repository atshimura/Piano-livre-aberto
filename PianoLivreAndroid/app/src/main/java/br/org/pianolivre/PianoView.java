package br.org.pianolivre;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;

/**
 * Large landscape grand-staff preview. The two staff systems own almost the
 * entire drawing area; the optional keyboard is hidden by default.
 * This is not yet a full MusicXML notation/engraving engine.
 */
public class PianoView extends View {
 public Score score;
 public double playhead=0;
 public int hand=0;
 public boolean showKeyboard=false;
 public boolean keyboardOnly=false;
 private Score guideScore;
 private KeyboardGuide guide;
 public final Set<Integer> pressed=new HashSet<>();
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final float density;
 private static final int INK=0xff203854, MUTED=0xff688099;
 private static final int GREEN=0xff008F78, GOLD=0xffCE7B19;
 private static final int STAFF=0xff91A4B8;
 public PianoView(Context c){super(c);density=getResources().getDisplayMetrics().density;setBackgroundColor(Color.WHITE);}
 private float d(float n){return n*density;}
 private void line(Canvas c,float x,float y,float x2,float y2,int color,float thick){
  p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(thick);c.drawLine(x,y,x2,y2,p);p.setStyle(Paint.Style.FILL);
 }
 private void text(Canvas c,String str,float x,float baseline,int dp,int color,boolean bold){
  p.setColor(color);p.setStyle(Paint.Style.FILL);p.setTextSize(d(dp));
  p.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
  c.drawText(str,x,baseline,p);
 }
 private void rect(Canvas c,int color,float left,float top,float right,float bottom,float radius){
  p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(left,top,right,bottom,radius,radius,p);
 }
 private static boolean black(int midi){int n=midi%12;return n==1||n==3||n==6||n==8||n==10;}
 private static int degree(int midi){int[] keys={0,0,1,1,2,3,3,4,4,5,5,6};return (midi/12-1)*7+keys[midi%12];}

 @Override protected void onDraw(Canvas canvas){
  super.onDraw(canvas);
  float w=getWidth(),h=getHeight();
  if(w<=0||h<=0)return;
  canvas.drawColor(Color.WHITE);
  if(keyboardOnly){
   if(score!=null)drawKeyboard(canvas,w,0,h);
   return;
  }
  float keyboardHeight=showKeyboard?Math.min(d(108),h*.25f):0;
  float musicHeight=h-keyboardHeight;
  if(score==null){
   text(canvas,"Selecione uma música na biblioteca",d(26),musicHeight*.50f,18,INK,true);
   return;
  }
  drawStaff(canvas,w,musicHeight);
  if(showKeyboard)drawKeyboard(canvas,w,musicHeight,h);
 }
 private void drawStaff(Canvas c,float w,float h){
  float topBanner=Math.min(d(43),h*.13f);
  rect(c,0xffF3F7FB,0,0,w,topBanner,0);
  String title=score.title==null?"Partitura":score.title;
  if(title.length()>58)title=title.substring(0,57)+"…";
  text(c,title,d(25),topBanner*.65f,16,INK,true);
  String meta="VISUALIZAÇÃO SIMPLIFICADA   •   compasso aprox. "+(int)(playhead/4+1);
  float metaX=Math.max(d(280),w-d(355));
  text(c,meta,metaX,topBanner*.65f,10,MUTED,false);

  float area=h-topBanner;
  float spacing=Math.min(d(18),Math.max(d(8),area*.078f));
  float topRight=topBanner+area*.075f;
  float topLeft=topBanner+area*.525f;
  float left=d(138),right=w-d(27);
  float first=(float)Math.max(0,playhead-.70);
  float span=6.0f;
  int[] refs={30,18}; // E4 bottom line of treble; G2 bottom line of bass
  for(int staffIndex=0;staffIndex<2;staffIndex++){
   float y0=staffIndex==0?topRight:topLeft;
   boolean enabled=hand==0||(hand==1&&staffIndex==0)||(hand==2&&staffIndex==1);
   int noteInk=enabled?INK:0xffAAB8C6;
   text(c,staffIndex==0?"MÃO DIREITA":"MÃO ESQUERDA",d(12),y0+spacing*1.35f,11,enabled?INK:MUTED,true);
   text(c,staffIndex==0?"Clave de Sol":"Clave de Fá",d(12),y0+spacing*2.55f,10,MUTED,false);
   for(int i=0;i<5;i++)line(c,left,y0+i*spacing,right,y0+i*spacing,STAFF,d(1.15f));
   // Light beat dividers, with note positions synchronized to the playhead.
   for(int beat=0;beat<=7;beat++){
    double time=Math.ceil(first)+beat;
    if(time>first+span)break;
    float x=left+(float)((time-first)/span)*(right-left);
    line(c,x,y0-d(8),x,y0+4*spacing+d(8),0xffEAF0F5,d(.7f));
   }
   for(Score.Event e:score.events){
    if(e.beat<first-.015||e.beat>first+span+.015)continue;
    if((staffIndex==0&&e.staff!=1)||(staffIndex==1&&e.staff==1))continue;
    float x=left+(float)((e.beat-first)/span)*(right-left);
    int scaleSteps=degree(e.midi)-refs[staffIndex];
    float y=y0+4*spacing-scaleSteps*spacing*.5f;
    if(y<y0-d(56)||y>y0+4*spacing+d(52))continue;
    boolean current=Math.abs(e.beat-playhead)<.012;
    int color=pressed.contains(e.midi)?GREEN:current?GOLD:noteInk;
    // Ledger lines for notes outside the five-line staff.
    if(scaleSteps<0){
     for(int s=-2;s>=scaleSteps;s-=2){
      float ledger=y0+4*spacing-s*spacing*.5f;
      line(c,x-d(12),ledger,x+d(12),ledger,STAFF,d(1.15f));
     }
    }else if(scaleSteps>8){
     for(int s=10;s<=scaleSteps;s+=2){
      float ledger=y0+4*spacing-s*spacing*.5f;
      line(c,x-d(12),ledger,x+d(12),ledger,STAFF,d(1.15f));
     }
    }
    p.setColor(color);p.setStyle(Paint.Style.FILL);
    c.save();c.rotate(-15,x,y);c.drawOval(x-d(9),y-d(6),x+d(9),y+d(6),p);c.restore();
    line(c,x+d(8),y-d(1),x+d(8),y-d(31),color,d(1.8f));
   }
  }
  float cursor=left+(float)((playhead-first)/span)*(right-left);
  line(c,cursor,topRight-d(20),cursor,topLeft+spacing*4+d(21),GREEN,d(2.7f));
  p.setColor(GREEN);c.drawCircle(cursor,topRight-d(21),d(4),p);
 }
 private void drawKeyboard(Canvas c,float w,float yTop,float h){
  rect(c,0xff13263E,0,yTop,w,h,0);
  if(guideScore!=score){
   guideScore=score;guide=score==null?null:new KeyboardGuide(score);
  }
  // Do NOT use PianoView.hand here. The guide always follows BOTH score parts,
  // regardless of which hand is selected in the training controls.
  KeyboardGuide.Frame sounding=guide==null?null:guide.at(playhead);
  Set<Integer> written=sounding==null?Collections.emptySet():sounding.both;
  KeyboardGuide.Range range=KeyboardGuide.range(written,pressed);
  int low=range.low,high=range.high,whiteCount=0;
  for(int n=low;n<=high;n++)if(!black(n))whiteCount++;
  String legend="TECLADO · amarelo: partitura (duas mãos) · verde: MIDI recebido";
  if(range.outsidePiano)legend+=" · nota fora do P-45";
  text(c,legend,d(12),yTop+d(20),10,0xffD4E4F1,true);
  float xStart=d(24),keyWidth=(w-d(48))/Math.max(1,whiteCount);
  float top=yTop+d(28),bottom=h-d(7);
  int whiteIndex=0;
  for(int n=low;n<=high;n++){
   if(black(n))continue;
   float x=xStart+keyWidth*whiteIndex++;
   rect(c,pressed.contains(n)?0xffA3EDD9:written.contains(n)?0xffFFE0A2:0xffF3F7FC,
    x+1,top,x+keyWidth-1,bottom,d(2));
   if(n%12==0&&keyWidth>=d(11))text(c,"Dó",x+d(2),bottom-d(5),8,MUTED,false);
  }
  whiteIndex=0;
  for(int n=low;n<=high;n++){
   if(!black(n)){whiteIndex++;continue;}
   float bw=keyWidth*.63f,x=xStart+whiteIndex*keyWidth-bw*.5f;
   rect(c,pressed.contains(n)?GREEN:written.contains(n)?GOLD:0xff263F59,
    x,top,x+bw,top+(bottom-top)*.61f,d(2));
  }
 }
}

package br.org.pianolivre;

import android.content.Context;
import android.webkit.ValueCallback;
import android.widget.FrameLayout;
import java.util.*;

/**
 * Live MIDI notation rendered by the same bundled OpenSheetMusicDisplay engine
 * used by Estudar. This keeps clefs, noteheads, accidentals and ledger lines
 * consistent with the score view instead of maintaining a second engraving.
 */
final class DiscoveryStaffView extends FrameLayout {
 interface RenderListener {void onRendered();void onError(String message);}
 static final class NotePosition {
  final int midi,ledgerLines;final float x,y;final String clef,spelling;
  NotePosition(int midi,float x,float y,int ledger,String clef,String spelling){this.midi=midi;this.x=x;this.y=y;ledgerLines=ledger;this.clef=clef;this.spelling=spelling;}
 }
 private List<Integer> notes=Collections.emptyList();private boolean sequence;
 private List<NotePosition> positions=Collections.emptyList();
 private float noteStep;
 private NotationView renderer;
 private RenderListener listener;
 private String renderedXml="";

 DiscoveryStaffView(Context c){super(c);setBackgroundColor(0xffffffff);setContentDescription("Pauta musical");}
 void setRenderListener(RenderListener value){listener=value;}
 void setNotes(List<Integer> values,boolean isSequence){
  List<Integer> next=values==null?Collections.emptyList():new ArrayList<>(values);
  if(notes.equals(next)&&sequence==isSequence)return;
  notes=next;sequence=isSequence;requestLayout();
  if(notes.isEmpty()){positions=Collections.emptyList();return;}
  layoutNotes(getWidth(),getHeight());
  renderedXml=DiscoveryScoreBuilder.build(notes,sequence);
  if(renderer==null){
   renderer=new NotationView(getContext(),new NotationView.Listener(){
    @Override public void onRendered(){if(listener!=null)listener.onRendered();}
    @Override public void onError(String message){if(listener!=null)listener.onError(message);}
   });
   addView(renderer,new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
  }
  Score score=new Score();score.title="Descobrir";score.sourceXml=renderedXml;
  renderer.loadScore(score,0);
 }
 String renderedMusicXml(){return renderedXml;}
 void inspectRenderedClefs(ValueCallback<String> callback){
  if(renderer==null){if(callback!=null)callback.onReceiveValue("null");return;}
  renderer.inspectRenderedClefs(callback);
 }
 void onPause(){if(renderer!=null)renderer.onPause();}
 void onResume(){if(renderer!=null)renderer.onResume();}
 void dispose(){if(renderer!=null){renderer.dispose();renderer=null;}}

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

}

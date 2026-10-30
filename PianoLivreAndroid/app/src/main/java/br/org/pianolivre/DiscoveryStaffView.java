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
 private android.webkit.WebView renderer;
 private RenderListener listener;
 private String renderedXml="";
 private boolean ready=false;private int tier=0;private long revision=0;
 DiscoveryStaffView(Context c){super(c);setBackgroundColor(0xffffffff);setContentDescription("Pauta musical");
  renderer=new android.webkit.WebView(c);renderer.setBackgroundColor(0xffffffff);renderer.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE,null);
  renderer.getSettings().setJavaScriptEnabled(true);renderer.getSettings().setAllowContentAccess(false);
  renderer.getSettings().setDomStorageEnabled(false);
  renderer.setWebViewClient(new android.webkit.WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(android.webkit.WebView v,String url){return !url.startsWith("file:///android_asset/");}
   @Override public boolean shouldOverrideUrlLoading(android.webkit.WebView v,android.webkit.WebResourceRequest r){return !r.getUrl().toString().startsWith("file:///android_asset/");}
  });
  renderer.addJavascriptInterface(new Object(){
   @android.webkit.JavascriptInterface public void ready(){post(()->{ready=true;send();});}
   @android.webkit.JavascriptInterface public void rendered(String report){post(()->{try{if(renderer!=null&&new org.json.JSONObject(report).optLong("revision",-1)==revision&&listener!=null)listener.onRendered();}catch(org.json.JSONException e){android.util.Log.e("PianoLivreDiscovery","Invalid render report",e);}});}
   @android.webkit.JavascriptInterface public void error(String message){post(()->{android.util.Log.e("PianoLivreDiscovery",message);if(listener!=null)listener.onError(message);});}
  },"DiscoveryNotation");
  addView(renderer,new FrameLayout.LayoutParams(-1,-1));
  renderedXml=DiscoveryScoreBuilder.build(notes,false);renderer.loadUrl("file:///android_asset/discovery.html");
 }
 void setRenderListener(RenderListener value){listener=value;}
 void resetFraming(){tier=0;setNotes(Collections.emptyList(),false);}
 void setNotes(List<Integer> values,boolean isSequence){
  List<Integer> next=values==null?Collections.emptyList():new ArrayList<>(values);
  notes=next;sequence=isSequence;revision++;
  for(int n:notes)tier=Math.max(tier,n<21||n>108?2:n<36||n>84?1:0);
  renderedXml=DiscoveryScoreBuilder.build(notes,sequence);requestLayout();send();
 }
 private void send(){if(ready&&renderer!=null)renderer.evaluateJavascript("Discovery.render("+org.json.JSONObject.quote(renderedXml)+","+tier+","+sequence+","+notes.size()+","+revision+")",null);}
 String renderedMusicXml(){return renderedXml;}
 void inspectRenderedClefs(ValueCallback<String> callback){renderer.evaluateJavascript("Discovery.clefs()",callback);}
 void inspectGeometry(ValueCallback<String> callback){renderer.evaluateJavascript("Discovery.inspect()",callback);}
 void commitFrame(Runnable action){renderer.postVisualStateCallback(0,new android.webkit.WebView.VisualStateCallback(){public void onComplete(long id){action.run();}});}
 android.graphics.Bitmap rasterize(){
  android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(renderer.getWidth(),renderer.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
  renderer.draw(new android.graphics.Canvas(image));return image;
 }
 void onPause(){if(renderer!=null)renderer.onPause();}
 void onResume(){if(renderer!=null)renderer.onResume();}
 void dispose(){if(renderer!=null){renderer.removeJavascriptInterface("DiscoveryNotation");renderer.destroy();renderer=null;}}

 List<NotePosition> layoutSnapshot(){layoutNotes(getWidth(),getHeight());return Collections.unmodifiableList(positions);}
 private static String spelling(int midi){String[] names={"Dó","Dó♯","Ré","Mi♭","Mi","Fá","Fá♯","Sol","Lá♭","Lá","Si♭","Si"};return names[midi%12]+(midi/12-1);}
 private static int letterIndex(int midi){return new int[]{0,0,1,2,2,3,3,4,5,5,6,6}[midi%12];}
 private static int diatonic(int midi){return (midi/12-1)*7+letterIndex(midi);}
 private void layoutNotes(int w,int h){
  if(notes.isEmpty()||w<=0||h<=0){positions=Collections.emptyList();return;}
  int min=Collections.min(notes),max=Collections.max(notes);boolean grand=true;
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

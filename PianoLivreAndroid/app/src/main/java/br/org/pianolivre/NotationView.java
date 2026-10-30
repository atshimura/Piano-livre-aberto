package br.org.pianolivre;

import android.content.Context;
import android.annotation.SuppressLint;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.webkit.ConsoleMessage;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.view.View;
import android.graphics.Color;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Offline MusicXML score engraving, using the bundled BSD-licensed
 * OpenSheetMusicDisplay library. No network, no remote Javascript.
 */
@SuppressLint("SetJavaScriptEnabled")
public final class NotationView extends WebView {
 public interface Listener { void onRendered(); void onError(String message); default void onStudyTap(){} default void onPresentation(String message){} default void onLoopSelected(double start,double end){} }
 private final Listener callback;
 private boolean htmlReady=false;
 private boolean scoreRendered=false;
 private Score pendingScore;
 private double pendingBeat=0;
 private double lastBeat=Double.NaN;
 private long lastSeekAt=0L;
 private boolean lastMoving=false;
 private double lastBpm=Double.NaN;
 private boolean disposed=false;
 private long pendingWaitDeadline=0L;
 private boolean freeLayout=false;
 // OSMD is normal; failures automatically try temporal, then native schematic.
 private boolean engravingProof=true;
 private boolean rendererFailed=false;
 private boolean loadDispatched=false;
 private long scoreGeneration=0;
 private long resizeSequence=0;
 private int studyHand=0;
 private final int originalLayerType;
 public NotationView(Context c,Listener listener){
  super(c);
  callback=listener;
  originalLayerType=getLayerType();
  setBackgroundColor(Color.WHITE);
  getSettings().setJavaScriptEnabled(true);
  getSettings().setAllowFileAccess(true);
  getSettings().setAllowContentAccess(false);
  getSettings().setDomStorageEnabled(false);
  getSettings().setBuiltInZoomControls(false);
  getSettings().setDisplayZoomControls(false);
  getSettings().setSupportZoom(false);
  getSettings().setLoadsImagesAutomatically(true);
  // All code and MusicXML are local; never load external network resources.
  setWebChromeClient(new WebChromeClient(){
   @Override public boolean onConsoleMessage(ConsoleMessage m){
    Log.i("PianoLivreNotation","JS "+m.messageLevel()+" "+m.message()+" @"+m.lineNumber());
    return true;
   }
  });
  setWebViewClient(new WebViewClient(){
   @Override public void onPageFinished(WebView view,String url){
    Log.i("PianoLivreNotation","PAGE_READY "+url);
    evaluateJavascript("typeof window.PianoNotation + ':' + typeof window.opensheetmusicdisplay", value->Log.i("PianoLivreNotation","JS_TYPES "+value));
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view,String url){
    return !url.startsWith("file:///android_asset/");
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){
    return !"file".equals(req.getUrl().getScheme())||
      !req.getUrl().toString().startsWith("file:///android_asset/");
   }
   @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError error){
    if(req.isForMainFrame())post(()->callback.onError("Falha ao abrir motor de partituras"));
   }
  });
  addJavascriptInterface(new Object(){
   @JavascriptInterface public void onReady(){post(()->{
    if(disposed)return;
    htmlReady=true;Log.i("PianoLivreNotation","BRIDGE_READY");
    if(pendingScore!=null)sendScore();
   });}
   @JavascriptInterface public void onRendered(){post(()->{
    if(disposed)return;
    rendererFailed=false;scoreRendered=true;Log.i("PianoLivreNotation","RENDERED_CALLBACK");
    setVisibility(View.VISIBLE);
    sendStudyHand();
    callback.onRendered();
    sendPendingWaitPreparation();
   });}
   @JavascriptInterface public void onPresentation(String message){post(()->{if(!disposed)callback.onPresentation(message);});}
   @JavascriptInterface public void onStudyTap(){post(()->{if(!disposed&&!freeLayout)callback.onStudyTap();});}
   @JavascriptInterface public void onError(String error){post(()->{
    if(disposed)return;
    scoreRendered=false;loadDispatched=false;scoreGeneration++;
    if(!freeLayout&&engravingProof){
     engravingProof=false;lastBeat=Double.NaN;
     setLayerType(originalLayerType,null);
     Log.w("PianoLivreNotation","AUTO_TEMPORAL_FALLBACK "+error);
     callback.onPresentation("Visualização temporal ativa: a notação principal não pôde ser exibida.");
     sendScore();return;
    }
    engravingProof=false;rendererFailed=true;
    Log.e("PianoLivreNotation","RENDER_ERROR "+error);
    callback.onError(error);
   });}
   @JavascriptInterface public void onLoopSelected(double start,double end){post(()->{
    if(!disposed&&!freeLayout&&scoreRendered)callback.onLoopSelected(start,end);
   });}
  },"AndroidNotation");
  setVisibility(View.INVISIBLE);
  loadUrl("file:///android_asset/notation.html");
 }
 public void loadScore(Score score,double position){
  loadScore(score,position,false);
 }
 public void loadScore(Score score,double position,boolean traditional){
  scoreGeneration++;loadDispatched=false;
  rendererFailed=false;freeLayout=traditional;engravingProof=!traditional;
  if(!traditional&&getContext() instanceof MainActivity){
   MainActivity activity=(MainActivity)getContext();
   if((activity.getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0
       &&activity.getIntent().getBooleanExtra("study_temporal_selftest",false))engravingProof=false;
  }
  if(getLayerType()!= (engravingProof?View.LAYER_TYPE_SOFTWARE:originalLayerType))
   setLayerType(engravingProof?View.LAYER_TYPE_SOFTWARE:originalLayerType,null);
  getSettings().setSupportZoom(traditional);
  getSettings().setBuiltInZoomControls(traditional);
  getSettings().setDisplayZoomControls(false);
  pendingWaitDeadline=0;pendingScore=score;pendingBeat=position;lastBeat=Double.NaN;lastSeekAt=0L;lastMoving=false;lastBpm=Double.NaN;scoreRendered=false;
  setVisibility(View.INVISIBLE);
  if(htmlReady)sendScore();
 }
 private void sendScore(){
  if(disposed||pendingScore==null||loadDispatched||getWidth()<=0||getHeight()<=0)return;
  final long generation=scoreGeneration;loadDispatched=true;
  // A newly shown Study view can receive HTML-ready before Android lays it out.
  // Rendering into a zero-sized viewport produces an invalid initial note map.
  evaluateJavascript("(function(){var v=document.getElementById('viewport');return !!(v&&v.clientWidth>0&&v.clientHeight>0)})()",ready->{
   if(disposed||generation!=scoreGeneration)return;
   if(!"true".equals(ready)){
    loadDispatched=false;postOnAnimation(()->sendScore());return;
   }
   dispatchScore();
  });
 }
 @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){
  super.onSizeChanged(width,height,oldWidth,oldHeight);
  if(width>0&&height>0&&htmlReady&&pendingScore!=null&&!scoreRendered&&!rendererFailed)
   postOnAnimation(()->sendScore());
  else if(width>0&&height>0&&htmlReady&&scoreRendered&&!rendererFailed)
   postOnAnimation(()->resizeToFit());
 }
 private void dispatchScore(){
  if(disposed||pendingScore==null)return;
  String xml=pendingScore.sourceXml;
  if(xml==null||xml.isEmpty()){callback.onError("A partitura não contém MusicXML");return;}
  // Proper JSON quoting prevents XML text being interpreted as JavaScript.
  Log.i("PianoLivreNotation","LOAD_XML "+xml.length());
  String layout=freeLayout?"'free'":(engravingProof?"'engraving-poc'":"'study'");
  String nativeEvents="null";
  if(engravingProof){
   java.util.List<Score.Event> graphicEvents=pendingScore.writtenEvents.isEmpty()
      ?pendingScore.events:pendingScore.writtenEvents;
   // Send the authoritative parsed events only to the opt-in experiment.
   // No persistence or alteration of the musical playback model.
   if(graphicEvents.size()>20000){
    Log.w("PianoLivreNotation","POC_EVENT_LIMIT "+graphicEvents.size());
   }else{
    JSONArray serialized=new JSONArray();
    for(Score.Event event:graphicEvents){
     try{
      JSONObject note=new JSONObject();
      note.put("beat",event.beat);note.put("length",event.length);
      note.put("midi",event.midi);note.put("staff",event.staff);
      note.put("measure",event.measure);note.put("voice",event.voiceId);
      note.put("sourcePart",event.originalPartIndex());
      note.put("tieStart",event.tieStart);note.put("tieStop",event.tieStop);
      note.put("playbackAttack",event.playbackAttack);
      if(event.writtenStep!=null){
       note.put("step",event.writtenStep);note.put("octave",event.writtenOctave);
       if(Double.isFinite(event.writtenAlter))note.put("alter",event.writtenAlter);
      }
      if(event.sourceNoteIndex>=0)note.put("xmlNoteIndex",event.sourceNoteIndex);
      serialized.put(note);
     }catch(org.json.JSONException exception){
      Log.w("PianoLivreNotation","POC_EVENT_SERIALIZATION",exception);
     }
    }
    nativeEvents=serialized.toString();
   }
  }
  evaluateJavascript("PianoNotation.load("+JSONObject.quote(xml)+","+pendingBeat+","+layout+","+nativeEvents+")",null);
 }
 /** Actual renderer state for diagnostics, never a user-selectable control. */
 public boolean isEngravingProof(){return engravingProof;}
 /** Returns the actual rendered clef glyph groups for visual regression tests. */
 void inspectRenderedClefs(ValueCallback<String> callback){
  if(disposed||!htmlReady||!scoreRendered){if(callback!=null)callback.onReceiveValue("null");return;}
  evaluateJavascript("(function(){var a=Array.from(document.querySelectorAll('#score svg .vf-clef'));return JSON.stringify(a.map(function(n){return n.outerHTML}))})()",callback);
 }
 public boolean hasRendererFailure(){return rendererFailed;}
 private void sendStudyHand(){
  if(!disposed&&!freeLayout&&htmlReady&&scoreRendered&&engravingProof)
   evaluateJavascript("PianoNotation.studyHand("+studyHand+")",null);
 }
 public void setStudyHand(int hand){
  if(hand<0||hand>2)return;
  if(studyHand==hand)return;
  studyHand=hand;
  sendStudyHand();
 }
 public void seek(double beat,boolean animate,double bpm,boolean moving){
  pendingBeat=beat;
  if(freeLayout||disposed||!htmlReady||!scoreRendered)return;
  // Timestamp events synchronize a separate requestAnimationFrame visual clock.
  // No throttling to musical note onsets: the visual motion is continuous.
  long now=android.os.SystemClock.uptimeMillis();
  // Keep 60 fps scrolling in requestAnimationFrame, not in the 25 ms native
  // ticker. Throttle redundant Java→WebView IPC and preserve seek/pause events.
  if(moving&&lastMoving&&!animate&&Math.abs(bpm-lastBpm)<0.001&&now-lastSeekAt<110L)return;
  if(!Double.isNaN(lastBeat)&&Math.abs(beat-lastBeat)<0.0001&&!animate&&moving==lastMoving&&Math.abs(bpm-lastBpm)<0.001)return;
  lastBeat=beat;lastMoving=moving;lastBpm=bpm;lastSeekAt=now;
  evaluateJavascript("PianoNotation.seek("+beat+","+(animate?"true":"false")+","+bpm+","+(moving?"true":"false")+")",null);
 }
 private void sendPendingWaitPreparation(){
  if(disposed||freeLayout||!htmlReady||!scoreRendered||pendingWaitDeadline<=0)return;
  long remaining=pendingWaitDeadline-android.os.SystemClock.uptimeMillis();
  if(remaining>0)evaluateJavascript("PianoNotation.waitPrepare("+remaining+")",null);
  else pendingWaitDeadline=0;
 }
 public void startWaitPreparation(long durationMs){
  if(disposed||durationMs<=0)return;
  pendingWaitDeadline=android.os.SystemClock.uptimeMillis()+durationMs;
  sendPendingWaitPreparation();
 }
 public void cancelWaitPreparation(){
  pendingWaitDeadline=0;
  if(!disposed&&!freeLayout&&htmlReady&&scoreRendered)evaluateJavascript("PianoNotation.waitCancel()",null);
 }
 public void resizeToFit(){
  if(!disposed&&!freeLayout&&htmlReady&&scoreRendered)resizeWhenViewportReady(++resizeSequence,scoreGeneration,0);
 }
 private void resizeWhenViewportReady(long sequence,long generation,int attempt){
  if(disposed||sequence!=resizeSequence||generation!=scoreGeneration||!scoreRendered||getWidth()<=0||getHeight()<=0)return;
  float density=getResources().getDisplayMetrics().density;
  String check="(function(){var v=document.getElementById('viewport');return !!(v&&Math.abs(v.clientWidth-"+(getWidth()/density)+")<2&&Math.abs(v.clientHeight-"+(getHeight()/density)+")<2)})()";
  evaluateJavascript(check,ready->{
   if(disposed||sequence!=resizeSequence||generation!=scoreGeneration||!scoreRendered)return;
   if("true".equals(ready))evaluateJavascript("PianoNotation.resize()",null);
   else if(attempt<8)postOnAnimation(()->resizeWhenViewportReady(sequence,generation,attempt+1));
  });
 }
 public void setLoopEdit(boolean enabled){
  if(!disposed&&!freeLayout&&htmlReady&&scoreRendered)
   evaluateJavascript("PianoNotation.loopEdit("+(enabled?"true":"false")+")",null);
 }
 public void clearLoop(){
  if(!disposed&&!freeLayout&&htmlReady&&scoreRendered)
   evaluateJavascript("PianoNotation.clearLoop()",null);
 }
 public void dispose(){disposed=true;
  if(getLayerType()!=originalLayerType)setLayerType(originalLayerType,null);
  removeJavascriptInterface("AndroidNotation");destroy();}
}

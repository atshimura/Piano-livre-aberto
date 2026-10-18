package br.org.pianolivre;

import android.content.Context;
import android.annotation.SuppressLint;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
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
 public interface Listener { void onRendered(); void onError(String message); default void onLoopSelected(double start,double end){} }
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
 // Research switch only, not a shipping replacement for the proportional renderer.
 private boolean engravingProof=false;
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
    scoreRendered=true;Log.i("PianoLivreNotation","RENDERED_CALLBACK");
    setVisibility(View.VISIBLE);
    callback.onRendered();
    sendPendingWaitPreparation();
   });}
   @JavascriptInterface public void onError(String error){post(()->{
    if(disposed)return;
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
  freeLayout=traditional;engravingProof=false;
  if(getLayerType()!=originalLayerType)setLayerType(originalLayerType,null);
  getSettings().setSupportZoom(traditional);
  getSettings().setBuiltInZoomControls(traditional);
  getSettings().setDisplayZoomControls(false);
  pendingWaitDeadline=0;pendingScore=score;pendingBeat=position;lastBeat=Double.NaN;lastSeekAt=0L;lastMoving=false;lastBpm=Double.NaN;scoreRendered=false;
  setVisibility(View.INVISIBLE);
  if(htmlReady)sendScore();
 }
 private void sendScore(){
  if(disposed||pendingScore==null)return;
  String xml=pendingScore.sourceXml;
  if(xml==null||xml.isEmpty()){callback.onError("A partitura não contém MusicXML");return;}
  // Proper JSON quoting prevents XML text being interpreted as JavaScript.
  Log.i("PianoLivreNotation","LOAD_XML "+xml.length());
  String layout=freeLayout?"'free'":(engravingProof?"'engraving-poc'":"'study'");
  String nativeEvents="null";
  if(engravingProof){
   // Send the authoritative parsed events only to the opt-in experiment.
   // No persistence or alteration of the musical playback model.
   if(pendingScore.events.size()>20000){
    Log.w("PianoLivreNotation","POC_EVENT_LIMIT "+pendingScore.events.size());
   }else{
    JSONArray serialized=new JSONArray();
    for(Score.Event event:pendingScore.events){
     try{
      JSONObject note=new JSONObject();
      note.put("beat",event.beat);note.put("length",event.length);
      note.put("midi",event.midi);note.put("staff",event.staff);
      note.put("measure",event.measure);note.put("voice",event.voiceId);
      note.put("tieStart",event.tieStart);note.put("tieStop",event.tieStop);
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
 /** A proof-of-concept preview of the already-bundled OSMD SVG engine.
  * Disabled on normal score load; never drives study playback or event timing.
  */
 public boolean isEngravingProof(){return engravingProof;}
 public void setEngravingProof(boolean enabled){
  if(disposed||freeLayout||pendingScore==null||engravingProof==enabled)return;
  engravingProof=enabled;scoreRendered=false;lastBeat=Double.NaN;
  // Diagnostic only: distinguish GPU SVG tile clipping from musical data loss.
  // Revert to the original WebView layer immediately on leaving the preview.
  setLayerType(enabled?View.LAYER_TYPE_SOFTWARE:originalLayerType,null);
  Log.i("PianoLivreNotation","POC_LAYER "+(enabled?"software":"original"));
  setVisibility(View.INVISIBLE);
  if(htmlReady)sendScore();
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
  if(disposed||freeLayout||engravingProof||!htmlReady||!scoreRendered||pendingWaitDeadline<=0)return;
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
  if(!disposed&&!freeLayout&&!engravingProof&&htmlReady&&scoreRendered)evaluateJavascript("PianoNotation.waitCancel()",null);
 }
 public void resizeToFit(){
  if(!disposed&&!freeLayout&&htmlReady&&scoreRendered)
   evaluateJavascript("PianoNotation.resize()",null);
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

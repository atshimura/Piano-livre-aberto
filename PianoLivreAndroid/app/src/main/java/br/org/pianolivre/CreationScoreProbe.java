package br.org.pianolivre;

import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.util.Arrays;

/**
 * Debug-only fixture for the focused creation smoke test, using the Z restart
 * generator. Never runs in release builds and refuses an attached real MIDI instrument.
 */
final class CreationScoreProbe {
 private CreationScoreProbe(){}
 static void run(MainActivity activity){
  if((activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
  if(activity.midiManager!=null&&activity.midiManager.getDevices().length>0)
   throw new IllegalStateException("Synthetic score probe refuses physical MIDI devices");
  try{
   // Tests run after pm clear in a dedicated emulator debug package.
   // Steady quarter pulse: the generator must recognize it automatically.
   MidiTakeRecorder recorder=new MidiTakeRecorder();
   recorder.start(1,1_000_000_000L);
   recorder.accept(0x90,48,90,1_100_000_000L,1);
   recorder.accept(0x90,60,85,1_100_000_000L,1);
   recorder.accept(0x90,64,74,1_100_000_000L,1);
   recorder.accept(0x80,60,0,1_600_000_000L,1);
   recorder.accept(0x80,64,0,1_600_000_000L,1);
   recorder.accept(0x80,48,0,2_100_000_000L,1);
   recorder.accept(0x90,67,80,2_100_000_000L,1);
   recorder.accept(0x80,67,0,2_600_000_000L,1);
   recorder.stop(2_700_000_000L);
   CreationProjectStore store=new CreationProjectStore(activity.getFilesDir());
   CreationProjectStore.Project saved=store.save("C2-D partitura de teste",activity.user,
      System.currentTimeMillis(),recorder.durationNs(),recorder.events(),recorder.segments());
   byte[] original=store.rawFile(saved.id);
   CreationScoreGenerator.Generation generated=CreationScoreGenerator.generate(
      saved.events,saved.segments,saved.title);
   CreationScoreStore scores=new CreationScoreStore(activity.getFilesDir());
   scores.save(saved.id,activity.user,generated.xml,(int)Math.round(generated.bpm));
   if(!Arrays.equals(original,store.rawFile(saved.id)))
    throw new IllegalStateException("Fixture changed canonical MIDI recording");
   if(!scores.read(saved.id).xml.equals(generated.xml))
    throw new IllegalStateException("Saved MusicXML did not roundtrip");
   activity.screens.showCreation();
   openWhenReady(activity,saved.id,0);
  }catch(Exception error){
   Log.e("PianoLivreCreation","CREATION_SCORE_FIXTURE_FAILED",error);
   throw new IllegalStateException("Creation fixture failed",error);
  }
 }
 private static boolean hasButton(View view,String label){
  if(view instanceof Button&&((Button)view).getText().toString().contains(label))return true;
  if(view instanceof ViewGroup){
   ViewGroup g=(ViewGroup)view;
   for(int i=0;i<g.getChildCount();i++)if(hasButton(g.getChildAt(i),label))return true;
  }
  return false;
 }
 private static void openWhenReady(MainActivity activity,String id,int attempt){
  if(attempt>150)throw new IllegalStateException("Creation entry did not finish");
  // C1's initial journal scan may post showHome asynchronously. It must finish
  // before selecting the fixture, or that callback can overwrite Details.
  if(!activity.screens.creation.debugIdle()){
   activity.handler.postDelayed(()->openWhenReady(activity,id,attempt+1),100);
   return;
  }
  activity.screens.creation.showDetails(id);
  waitForDetails(activity,0);
 }
 private static void waitForDetails(MainActivity activity,int attempt){
  if(hasButton(activity.screens.creation,"Visualizar partitura")){
   Log.i("PianoLivreCreation","C2_D_FIXTURE_PASS synthetic=true stored=true originalUnchanged=true details=true");
   return;
  }
  if(attempt>150)throw new IllegalStateException("Derived score actions not displayed");
  activity.handler.postDelayed(()->waitForDetails(activity,attempt+1),100);
 }
}

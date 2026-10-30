package br.org.pianolivre;

import android.os.Handler;
import android.view.View;
import org.json.*;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/** Debug intent only. Synthetic events use the production MIDI session surface. */
final class DiscoveryLayoutSelfTest {
 private final MainActivity a;private final MusicLabDiscoveryScreen d;private final Handler h=new Handler();
 private int stage=0;private long batch=1000,time=10000;private JSONObject normal;
 private static final String[] NAMES={"empty","note","retained","chord","sequence","low","high","common-after-extreme"};
 private DiscoveryLayoutSelfTest(MainActivity a){this.a=a;d=a.screens.discovery;}
 static void run(MainActivity a){new DiscoveryLayoutSelfTest(a).begin();}
 private void check(boolean b,String m){if(!b)throw new IllegalStateException("Discovery layout: "+m);}
 private void begin(){a.screens.showLab();d.staff.setRenderListener(new DiscoveryStaffView.RenderListener(){public void onRendered(){d.staff.commitFrame(()->capture());}public void onError(String m){throw new IllegalStateException(m);}});d.staff.resetFraming();}
 private void play(int...notes){long conn=a.midiGeneration,ui=d.sessionGeneration();d.session.reconnect(conn,null);for(int n:notes)d.midiAttack(n,time,batch,conn,ui);for(int n:notes)d.midiRelease(n,conn,ui);d.midiBatchEnd(batch,time+1,ui);batch++;time+=2000;}
 private void advance(){
  stage++;if(stage==NAMES.length){d.staff.setRenderListener(null);d.back.performClick();check(!a.screens.isLabOpen(),"back navigation");android.util.Log.i("PianoLivreDiscovery","DISCOVERY_LAYOUT_PASS stages="+stage);return;}
  switch(stage){
   case 1:play(60);break;
   case 2:check(d.session.downNotes().isEmpty()&&d.session.result().notes.equals(Arrays.asList(60)),"release retains note");h.postDelayed(()->capture(),700);break;
   case 3:play(60,64,67);check(d.session.result().kind==MusicDiscoverySession.Kind.CHORD,"chord");break;
   case 4:for(int n:new int[]{60,62,64,65,67,69,71,72})play(n);check(d.session.result().kind==MusicDiscoverySession.Kind.SEQUENCE,"parallel scale analyzer");break;
   case 5:play(0);break;case 6:play(127);break;case 7:play(64);break;
  }
 }
 private boolean capturing=false;
 private void capture(){if(capturing)return;capturing=true;d.staff.inspectGeometry(raw->{try{
  Object decoded=new JSONTokener(raw).nextValue();if(!(decoded instanceof String)){capturing=false;return;}
  JSONObject g=new JSONObject((String)decoded);check(g.getJSONArray("staves").length()==2,"two painted staves");
  if(stage==0)normal=g;
  if(stage<=4){check(g.getInt("tier")==0,"ordinary notes keep standard tier");check(Math.abs(g.getDouble("scale")-normal.getDouble("scale"))<.001,"stable ordinary scale");
   for(int i=0;i<2;i++)check(Math.abs(g.getJSONArray("staves").getJSONArray(i).getJSONObject(0).getDouble("y")-normal.getJSONArray("staves").getJSONArray(i).getJSONObject(0).getDouble("y"))<1,"stable staff origin");}
  if(stage>=5)check(g.getInt("tier")==2,"extreme tier is sticky");
  int[] left=new int[2],right=new int[2];d.staff.getLocationOnScreen(left);d.title.getLocationOnScreen(right);
  check(right[0]>left[0]+d.staff.getWidth(),"text panel is to the right");
  g.put("stage",NAMES[stage]);g.put("notationWidth",d.staff.getWidth());g.put("totalWidth",d.getWidth());
  check(d.staff.getWidth()/(double)(d.getWidth()-20*a.getResources().getDisplayMetrics().density)>.73,"notation predominates");
  File ack=new File(a.getCacheDir(),"discovery-next");Files.deleteIfExists(ack.toPath());
  android.util.Log.i("PianoLivreDiscovery","DISCOVERY_LAYOUT_STAGE "+g);waitAck(ack,0);
 }catch(Exception e){throw new IllegalStateException(e);}});}
 private void waitAck(File ack,int tries){h.postDelayed(()->{if(ack.exists()){capturing=false;advance();}else {check(tries<600,"capture acknowledgement timeout");waitAck(ack,tries+1);}},100);}
}

import br.org.pianolivre.*;
import java.io.*;
import java.util.*;

/** GPL-3.0-only. A deterministic whole-session integration exercise for Study B.
 * No emulator is substituted for a USB-MIDI piano or an Android UI.
 */
public final class StudySessionIntegrationTest {
 static long checks=0;
 static void ok(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static Score load(String filename)throws Exception{
  String path="app/src/main/assets/scores/"+filename;
  try(FileInputStream input=new FileInputStream(path)){return Score.read(input,filename);}
 }
 static Set<Integer> pitchSet(Collection<Score.Event> events){
  Set<Integer> notes=new LinkedHashSet<>();
  for(Score.Event e:events)if(e.midi>=0&&e.midi<=127)notes.add(e.midi);
  return notes;
 }
 static void waitJourney(Score score,String name,int hand){
  WaitRecognizer learner=new WaitRecognizer();
  ok(learner.chordWindowMs()==null,name+" no invented chord calibration");
  SortedSet<Double> beats=new TreeSet<>();
  for(Score.Event event:score.events)
   if(event.length>0&&!event.tieStop&&(hand==0||(hand==1&&event.staff==1)||(hand==2&&event.staff!=1)))
    beats.add(event.beat);
  ok(!beats.isEmpty(),name+" missing hand "+hand);
  int positions=0;
  for(double beat:beats){
   List<Score.Event> written=score.at(beat,hand);
   Set<Integer> required=pitchSet(written);
   if(required.isEmpty())continue;
   learner.arm(written);
   ok(learner.expected().equals(required),name+" armed wrong notes at "+beat);
   int wrong=0;while(required.contains(wrong))wrong++;
   Set<Integer> held=new LinkedHashSet<>();held.add(wrong);
   WaitRecognizer.Result extra=learner.noteOnTimed(wrong,true,held,1200);
   ok(extra.extra&&!extra.advance,name+" wrong attack passed at "+beat);
   long time=10000;int seen=0;
   for(int pitch:required){
    held.add(pitch);seen++;
    WaitRecognizer.Result result=learner.noteOnTimed(pitch,true,held,time);
    ok(result.advance==(seen==required.size()),name+" wrong Wait progression at "+beat);
    if(result.advance)ok(result.extraPitches.contains(wrong),name+" extra suppressed when correct chord passes");
    time+=15000; // no uncalibrated time penalty even for a deliberately slow chord
   }
   for(int pitch:required){learner.noteOff(pitch);held.remove(pitch);}
   learner.clear();
   positions++;
  }
  ok(positions>2,name+" too few Wait positions");
 }
 static void guideJourney(Score score,String name){
  KeyboardGuide guide=new KeyboardGuide(score);
  ok(score.end()>0,name+" duration missing");
  for(double beat=0;beat<score.end();beat+=.5){
   KeyboardGuide.Frame frame=guide.at(beat);
   Set<Integer> joined=new HashSet<>(frame.right);joined.addAll(frame.left);
   ok(joined.equals(frame.both),name+" one hand lost when guide shown at "+beat);
   for(Score.Event e:score.events)if(e.length>0&&e.beat<=beat+1e-8&&e.beat+e.length>beat+1e-8){
    // Same written key may be simultaneously held by both hands.
    ok((e.staff==1?frame.right:frame.left).contains(e.midi),
      name+" guide omitted an active note at "+beat+" midi "+e.midi);
   }
  }
 }
 static class Channel implements MidiPlayback.Sink {
  Set<Integer> sounding=new HashSet<>();
  int ons=0,offs=0;
  public void note(int pitch,boolean on,int velocity){
   if(on){ok(sounding.add(pitch),"MIDI duplicate Note On "+pitch);ons++;}
   else{ok(sounding.remove(pitch),"MIDI Note Off without On "+pitch);offs++;}
  }
 }
 static void playbackJourney(Score score,String name,int hand){
  Channel channel=new Channel();
  MidiPlayback playback=new MidiPlayback(score,hand,channel);
  double first=score.firstOnset(hand);
  playback.seek(first); // enters a phrase without playing everything skipped
  ok(playback.soundingKeys()==channel.sounding.size(),name+" seek active count");
  double position=first;
  for(int i=0;i<100000&&position<score.end();i++){
   double next=Math.min(score.end(),score.advanceBeats(position,125,.8));
   ok(next>position,name+" native tempo clock stalled");
   playback.advance(next);
   position=next;
  }
  ok(position>=score.end()-1e-6,name+" did not finish");
  playback.stop();ok(channel.sounding.isEmpty(),name+" stuck MIDI after final stop");
  // Pausing, changing hands, repeating a segment, and leaving the screen
  // all use the same Note Off safety invariant.
  double selected=Math.min(score.end(),first+2);
  playback.seek(selected);
  playback.advance(Math.min(score.end(),selected+.5));
  playback.stop();ok(channel.sounding.isEmpty(),name+" stuck MIDI after pause");
  playback.seek(selected);
  playback.advance(Math.min(score.end(),selected+1.5));
  playback.seek(selected); // loop wrap must release existing held keys first
  ok(playback.soundingKeys()==channel.sounding.size(),name+" loop did not rearm output");
  playback.stop();ok(channel.sounding.isEmpty(),name+" loop cancellation stuck a key");
  ok(channel.ons==channel.offs,name+" imbalanced MIDI Note On/Off");
 }
 static void durationPreparation(){
  WaitPreparation preparation=new WaitPreparation();
  preparation.start(1000,5);
  ok(preparation.isActive()&&!preparation.finish(5999),"Wait must visibly prepare");
  ok(preparation.finish(6000),"Wait must stop preparation at configured duration");
  preparation.start(9000,5);preparation.cancel();
  ok(!preparation.isActive()&&!preparation.finish(14000),"cancelled Wait must not arm later");
  // Attack-based chords are not penalized before a user calibration exists.
  WaitRecognizer recognizer=new WaitRecognizer();
  recognizer.arm(Arrays.asList(new Score.Event(0,1,60,1,1,1),
                               new Score.Event(0,1,64,1,1,1)));
  Set<Integer> held=new HashSet<>();held.add(60);
  ok(!recognizer.noteOnTimed(60,true,held,1000).advance,"chord not complete");
  held.add(64);
  ok(recognizer.noteOnTimed(64,true,held,61000).advance,"non-calibrated chord must work");
  recognizer.setChordWindowMs(400L);
  recognizer.arm(Arrays.asList(new Score.Event(0,1,60,1,1,1),
                               new Score.Event(0,1,64,1,1,1)));
  held.clear();held.add(60);recognizer.noteOnTimed(60,true,held,1000);
  held.add(64);
  ok(!recognizer.noteOnTimed(64,true,held,1600).advance,"calibrated chord must honor measured limit");
  held.clear();recognizer.noteOff(60);recognizer.noteOff(64);
  recognizer.arm(Arrays.asList(new Score.Event(0,1,60,1,1,1),
                               new Score.Event(0,1,64,1,1,1)));
  held.add(60);recognizer.noteOnTimed(60,true,held,2000);
  held.add(64);
  ok(recognizer.noteOnTimed(64,true,held,2200).advance,"calibrated chord should pass when on time");
 }
 public static void main(String[] args)throws Exception{
  durationPreparation();
  for(String filename:new String[]{"estudo-livre-01.mxl","gymnopedie-no-1-satie.mxl"}){
   Score score=load(filename);
   ok(score.events.size()>=20,filename+" must be a real musical score");
   ok(!score.sourceXml.isEmpty(),filename+" original MusicXML missing");
   int eventCount=score.events.size();
   guideJourney(score,filename);
   for(int hand=0;hand<=2;hand++){
    waitJourney(score,filename,hand);
    playbackJourney(score,filename,hand);
   }
   ok(eventCount==score.events.size(),filename+" source events were mutated by practice");
   Score reopened=load(filename);
   ok(reopened.sourceXml.equals(score.sourceXml),filename+" reopen changed authoritative source");
   ok(reopened.events.size()==eventCount,filename+" reopened notes diverged");
   System.out.println("STUDY_SESSION_FILE_PASS "+filename+" events="+eventCount);
  }
  System.out.println("STUDY_SESSION_INTEGRATION_PASS checks="+checks);
 }
}

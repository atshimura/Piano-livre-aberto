import br.org.pianolivre.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Read-only audit of MIDI note reception and score hand mapping.
 * This is NOT an on-device USB test. Core protocol + Score use production classes;
 * Espera exploit simulation mirrors the guarded predicate in MainActivity.onKey.
 */
public final class RecognitionAudit {
 static void check(boolean ok,String explanation){if(!ok)throw new AssertionError(explanation);}
 static final class Counters {
  int protocolMessages=0, corpusNotes=0, onsetChecks=0, samePitchBothHands=0, repeated=0;
  int notesOutsideGuide=0, partsHandBug=0, waitFalsePositives=0;
  int heldPitchAcrossNeighborOnsets=0, unexpectedHandJumpPositions=0;
 }
 static final Counters c=new Counters();
 static void protocolAudit(){
  MidiProtocol parser=new MidiProtocol();
  List<String> captured=new ArrayList<>();
  MidiProtocol.Listener listener=(st,p,v)->captured.add(st+":"+p+":"+v);
  for(int pass=0;pass<3;pass++)for(int channel=0;channel<16;channel++)for(int midi=0;midi<128;midi++){
   for(int kind=0;kind<3;kind++){
    parser.reset();captured.clear();
    int status=(kind==1?0x80:0x90)|channel, velocity=kind==0?97:0;
    byte[] sequence={(byte)status,(byte)midi,(byte)0xF8,(byte)velocity};
    if((pass+channel+midi+kind)%2==0){
     parser.feed(sequence,0,1,listener);
     parser.feed(sequence,1,2,listener);
     parser.feed(sequence,3,1,listener);
    }else{
     parser.feed(sequence,0,sequence.length,listener);
    }
    check(captured.size()==1,"message count for ch="+channel+" midi="+midi);
    check(captured.get(0).equals(status+":"+midi+":"+velocity),
     "wrong parsed event for ch="+channel+" midi="+midi+" "+captured);
    c.protocolMessages++;
   }
  }
  System.out.println("PASS MIDI_BYTE_DECODER "+c.protocolMessages+" events, 128 pitches, 16 channels, 3 encodings, fragmented and realtime-interleaved, 3 cycles");
 }
 static String twoPartsOneLocalStaff(){
  return "<?xml version=\"1.0\"?><score-partwise version=\"4.0\"><part-list>"+
   "<score-part id=\"P1\"><part-name>Piano RH</part-name></score-part>"+
   "<score-part id=\"P2\"><part-name>Piano LH</part-name></score-part></part-list>"+
   "<part id=\"P1\"><measure number=\"1\"><attributes><divisions>1</divisions>"+
   "<time><beats>4</beats><beat-type>4</beat-type></time></attributes><note>"+
   "<pitch><step>C</step><octave>4</octave></pitch><duration>1</duration><staff>1</staff></note></measure></part>"+
   "<part id=\"P2\"><measure number=\"1\"><attributes><divisions>1</divisions>"+
   "<time><beats>4</beats><beat-type>4</beat-type></time></attributes><note>"+
   "<pitch><step>G</step><octave>3</octave></pitch><duration>1</duration><staff>1</staff></note></measure></part>"+
   "</score-partwise>";
 }
 static void partMappingAudit()throws Exception{
  Score s=Score.read(new ByteArrayInputStream(twoPartsOneLocalStaff().getBytes(StandardCharsets.UTF_8)),"two-parts.musicxml");
  check(s.events.size()==2,"two-part fixture wrong count");
  check(s.at(0,1).size()==1&&s.at(0,2).size()==1,"split MusicXML local staff 1 still misclassified");
  check(s.at(0,1).get(0).midi==60 && s.at(0,2).get(0).midi==55,"wrong split parts mapping");
  System.out.println("PASS HAND_MAPPING: independent parts with local staff 1 map to right and left.");
 }
 static void waitAudit()throws Exception{
  int count=0;
  for(int round=0;round<5;round++)for(int hand=0;hand<3;hand++)for(int pitch=21;pitch<=108;pitch++){
   WaitRecognizer recognizer=new WaitRecognizer();
   recognizer.arm(Collections.singletonList(new Score.Event(0,1,pitch,hand==2?2:1,1,1)));
   int wrong=pitch==108?21:pitch+1;
   Set<Integer> down=new HashSet<>(Arrays.asList(pitch,wrong));
   check(!recognizer.noteOn(wrong,true,down),"wrong note advanced at "+pitch);
   check(!recognizer.noteOn(pitch,false,down),"held note advanced at "+pitch);
   down.remove(pitch);recognizer.noteOff(pitch);
   down.add(pitch);
   check(recognizer.noteOn(pitch,true,down),"fresh correct note failed at "+pitch);
   recognizer.arm(Collections.singletonList(new Score.Event(1,1,pitch,hand==2?2:1,1,1)));
   check(!recognizer.noteOn(wrong,true,down),"stale note advanced after arming");
   count++;
  }
  System.out.println("PASS WAIT_ATTACKS "+count+" repeated fresh/held/wrong note cases across all 88 piano keys and three hand options.");
 }
 static void corpusAudit()throws Exception{
  File dir=new File("app/src/main/assets/scores");
  File[] files=dir.listFiles((d,n)->n.endsWith(".mxl"));
  check(files!=null&&files.length==10,"Expected 10 built-in MXL");
  Arrays.sort(files,Comparator.comparing(File::getName));
  for(File f:files){
   Score s;
   try(FileInputStream stream=new FileInputStream(f)){s=Score.read(stream,f.getName());}
   int right=0,left=0,outside=0,same=0,repeats=0,checks=0;
   HashSet<Double> onsets=new HashSet<>();
   Map<String,Double> last=new HashMap<>();
   for(Score.Event e:s.events){
    c.corpusNotes++;
    check(e.midi>=0&&e.midi<=127,"illegal midi pitch in "+f.getName());
    if(e.staff==1)right++;else left++;
    if(e.midi<48||e.midi>95)outside++;
    onsets.add(e.beat);
    String key=e.staff+":"+e.midi;
    if(last.containsKey(key)&&e.beat>last.get(key))repeats++;
    last.put(key,e.beat);
   }
   for(double onset:onsets){
    List<Score.Event> all=s.at(onset,0),r=s.at(onset,1),l=s.at(onset,2);
    check(all.size()==r.size()+l.size(),"hands not partition at "+onset+" in "+f.getName());
    c.onsetChecks++;checks++;
    Set<Integer> rp=new HashSet<>(),lp=new HashSet<>();
    for(Score.Event e:r)rp.add(e.midi);
    for(Score.Event e:l)lp.add(e.midi);
    rp.retainAll(lp);
    same+=rp.size();
   }
   // A new wrong key can pass the next Wait checkpoint when the prior keys
   // already contain all pitches required by the next musical timestamp.
   int heldRisk=0,handJumps=0;
   for(int hand=0;hand<3;hand++){
    TreeMap<Double,Set<Integer>> perBeat=new TreeMap<>();
    for(Score.Event e:s.events)if(hand==0||(hand==1&&e.staff==1)||(hand==2&&e.staff!=1))
      perBeat.computeIfAbsent(e.beat,x->new HashSet<>()).add(e.midi);
    Set<Integer> previous=null;
    for(Set<Integer> current:perBeat.values()){
      if(previous!=null&&previous.containsAll(current))heldRisk++;
      previous=current;
    }
   }
   for(double onset:onsets){
    if(s.at(onset,1).isEmpty())handJumps++;
    if(s.at(onset,2).isEmpty())handJumps++;
   }
   c.heldPitchAcrossNeighborOnsets+=heldRisk;
   c.unexpectedHandJumpPositions+=handJumps;
   c.notesOutsideGuide+=outside;c.samePitchBothHands+=same;c.repeated+=repeats;
   System.out.println("CORPUS "+f.getName()+" notes="+s.events.size()+" right="+right+" left="+left+
     " onsets="+checks+" pitchesOutsideGuide="+outside+" pitchRecurrences="+repeats+
    " previousHeldPitchExposure="+heldRisk+" handSwitchHistoricalGaps="+handJumps+
    " samePitchBothHands="+same);
  }
  System.out.println("PASS SCORE_EVENTS "+c.corpusNotes+" events at "+c.onsetChecks+" onsets; selected-hand partition is arithmetically consistent.");
  System.out.println("INFO PREVIOUS_GUIDE_RANGE "+c.notesOutsideGuide+" score events outside OLD fixed MIDI 48..95 window; new guide scrolls.");
  System.out.println("INFO CONCURRENT_IDENTICAL_PITCH "+c.samePitchBothHands+" score positions same pitch in both hands (when >0, one-key equality set cannot verify two voices).");
  System.out.println("INFO PREVIOUS_HELD_PITCH_EXPOSURE "+c.heldPitchAcrossNeighborOnsets+
    " adjacent onset pairs (all hands/right/left) previously requiring fresh attacks; protected by WaitRecognizer.");
  System.out.println("INFO PREVIOUS_HAND_SWITCH_GAPS "+c.unexpectedHandJumpPositions+
    " possible gaps where changing hands now advances locally or maintains beat.");
  System.out.println("INFO PITCH_REOCCURRENCES "+c.repeated+
    " later occurrences of previously heard pitches within a staff (NOT necessarily adjacent).");
 }
 public static void main(String[] args)throws Exception{
  protocolAudit();partMappingAudit();waitAudit();corpusAudit();
  System.out.println("RESULT AUDIT_FINISHED; findings are expected reported defects, not guarantees of correct physical Yamaha operation.");
 }
}

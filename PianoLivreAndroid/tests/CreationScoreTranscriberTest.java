package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/** Focused JVM smoke tests; deliberately NOT the full Android/CI suite. */
public final class CreationScoreTranscriberTest {
    private static int checks;
    private static void ok(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    private static MidiTakeRecorder.Event e(int cmd,int key,int value,long ns,int order){
        return new MidiTakeRecorder.Event(cmd,key,value,ns,7,order,false);
    }
    private static MidiTakeRecorder.Event generated(int cmd,int key,int value,long ns,int order){
        return new MidiTakeRecorder.Event(cmd,key,value,ns,7,order,true);
    }
    private static List<MidiTakeRecorder.Segment> seg(long start,long end){
        return Collections.singletonList(new MidiTakeRecorder.Segment(start,end));
    }
    private static CreationScoreTranscriber.Options options(int bpm,int beats,int beatType,int grid,Map<String,Integer> overrides){
        return new CreationScoreTranscriber.Options(bpm,beats,beatType,grid,
                CreationScoreTranscriber.StaffMode.SUGGESTED,60,overrides);
    }
    private static CreationScoreTranscriber.Result read(List<MidiTakeRecorder.Event> events,
            List<MidiTakeRecorder.Segment> segments,CreationScoreTranscriber.Options options)throws Exception{
        CreationScoreTranscriber.Result result=CreationScoreTranscriber.transcribe(events,segments,options,"C2 & Piano <Livre>");
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        Document xml=factory.newDocumentBuilder().parse(new ByteArrayInputStream(result.xml.getBytes(StandardCharsets.UTF_8)));
        ok(xml.getDocumentElement().getNodeName().equals("score-partwise"),"valid MusicXML root");
        ok(xml.getElementsByTagName("measure").getLength()==result.measures,"measure count");
        return result;
    }
    private static void reject(List<MidiTakeRecorder.Event> e,List<MidiTakeRecorder.Segment> s,
            CreationScoreTranscriber.Options o,String why){
        boolean rejected=false;
        try{CreationScoreTranscriber.transcribe(e,s,o,"invalid");}catch(IOException error){rejected=true;}
        ok(rejected,why);
    }
    public static void main(String[] args)throws Exception {
        List<MidiTakeRecorder.Event> tune=Arrays.asList(
            e(0x90,60,89,1_000_000_000L,0),e(0x80,60,0,1_600_000_000L,1),
            e(0x90,62,70,1_600_000_000L,2),e(0x80,62,0,2_200_000_000L,3));
        List<MidiTakeRecorder.Segment> segments=seg(1_000_000_000L,3_000_000_000L);
        List<MidiTakeRecorder.Event> original=new ArrayList<>(tune);
        CreationScoreTranscriber.Options defaults=CreationScoreTranscriber.Options.defaults();
        CreationScoreTranscriber.Result music=read(tune,segments,defaults);
        ok(music.notes.size()==2&&music.notes.get(0).pitch==60&&music.notes.get(1).pitch==62,"melody");
        ok(music.notes.get(0).startTick==0&&music.notes.get(0).endTick==960,"100 bpm quarter");
        ok(music.xml.contains("&amp;")&&music.xml.contains("&lt;Livre&gt;"),"XML escaping");
        ok(music.xml.contains("<staves>2</staves>")&&music.xml.contains("<sign>F</sign>"),"piano staves");
        ok(tune.equals(original)&&tune.get(0).timeNs==1_000_000_000L,"source preserved");
        ok(read(tune,segments,defaults).xml.equals(music.xml),"deterministic output");
        ok("c2-0-7-0-60".equals(music.notes.get(0).sourceId),"stable source identity");
        String sourceId=music.notes.get(0).sourceId;
        CreationScoreTranscriber.Result moved=read(tune,segments,options(100,4,4,16,Collections.singletonMap(sourceId,2)));
        ok(moved.notes.get(0).staff==2&&moved.notes.get(0).pitch==60,"staff override no transposition");
        ok(moved.notes.get(0).sourceId.equals(sourceId),"stable identity across overrides");
        CreationScoreTranscriber.Result three=read(tune,segments,options(120,3,4,8,null));
        ok(three.xml.contains("<beats>3</beats>")&&three.notes.get(0).endTick==960,"3/4");
        ok(read(tune,segments,options(100,6,8,32,null)).xml.contains("<beat-type>8</beat-type>"),"6/8");
        List<MidiTakeRecorder.Event> chord=Arrays.asList(
            e(0x90,60,70,0,0),e(0x90,64,90,0,1),
            e(0x80,60,0,600_000_000L,2),e(0x80,64,0,600_000_000L,3));
        CreationScoreTranscriber.Result c=read(chord,seg(0,1_000_000_000L),defaults);
        ok(c.notes.size()==2&&c.notes.get(0).startTick==c.notes.get(1).startTick,"chord simultaneity");
        ok(c.xml.contains("<chord/>")&&c.notes.get(0).staff==c.notes.get(1).staff,"chord engraving");
        List<MidiTakeRecorder.Event> duplicate=Arrays.asList(
            e(0x90,60,85,0,0),e(0x91,60,86,0,1),
            e(0x80,60,0,600_000_000L,2),e(0x81,60,0,600_000_000L,3));
        CreationScoreTranscriber.Result same=read(duplicate,seg(0,1_000_000_000L),defaults);
        ok(same.notes.size()==2&&!same.notes.get(0).sourceId.equals(same.notes.get(1).sourceId),
                "same-pitch independent channels kept");
        List<MidiTakeRecorder.Event> pedal=Arrays.asList(
            e(0x90,48,80,0,0),e(0xB0,64,127,50_000_000L,1),
            e(0x80,48,0,600_000_000L,2),e(0xB0,64,0,1_500_000_000L,3));
        CreationScoreTranscriber.Result ped=read(pedal,seg(0,1_600_000_000L),defaults);
        ok(ped.notes.get(0).endTick==960&&ped.notes.get(0).staff==2,
                "sustain pedal must not extend written note");
        List<MidiTakeRecorder.Event> longNote=Arrays.asList(e(0x90,60,80,0,0),e(0x80,60,0,4_500_000_000L,1));
        CreationScoreTranscriber.Result tied=read(longNote,seg(0,5_000_000_000L),defaults);
        ok(tied.measures==2&&tied.xml.contains("<tied type=\"start\"/>")&&tied.xml.contains("<tied type=\"stop\"/>"),
                "barline ties");
        List<MidiTakeRecorder.Event> paused=Arrays.asList(
            e(0x90,60,80,0,0),generated(0x80,60,0,600_000_000L,1),
            e(0x90,62,80,10_000_000_000L,2),e(0x80,62,0,10_600_000_000L,3));
        CreationScoreTranscriber.Result gap=read(paused,Arrays.asList(
                new MidiTakeRecorder.Segment(0,600_000_000L),
                new MidiTakeRecorder.Segment(10_000_000_000L,10_700_000_000L)),defaults);
        ok(gap.notes.get(1).startTick==960&&gap.notes.get(0).syntheticRelease,
                "pause compressed; synthetic release signalled");
        List<MidiTakeRecorder.Event> overlapping=Arrays.asList(
            e(0x90,60,80,0,0),e(0x90,64,80,300_000_000L,1),
            e(0x80,64,0,900_000_000L,2),e(0x80,60,0,1_200_000_000L,3));
        CreationScoreTranscriber.Result poly=read(overlapping,seg(0,1_500_000_000L),defaults);
        ok(poly.notes.size()==2&&poly.notes.get(0).voice!=poly.notes.get(1).voice,
                "overlapping notes in independent voices");
        List<MidiTakeRecorder.Event> reattack=Arrays.asList(
            e(0x90,60,80,0,0),e(0x90,60,80,600_000_000L,1),
            e(0x80,60,0,1_200_000_000L,2));
        CreationScoreTranscriber.Result again=read(reattack,seg(0,1_300_000_000L),defaults);
        ok(again.notes.size()==2&&again.notes.get(0).syntheticRelease&&!again.warnings.isEmpty(),
                "rearticulation retained");
        reject(Collections.singletonList(e(0xB0,64,90,0,0)),seg(0,1_000_000L),defaults,
                "take with no attacks rejected");
        reject(tune,segments,options(100,4,4,16,Collections.singletonMap("unknown-id",1)),
                "unknown source note override rejected");
        reject(tune,seg(3_000_000_000L,4_000_000_000L),defaults,"source out of segment rejected");
        boolean bad=false;try{options(100,5,4,16,null);}catch(IllegalArgumentException ex){bad=true;}
        ok(bad,"unsupported meter rejected");
        System.out.println("CREATION_C2_A_BASIC_PASS checks="+checks);
    }
}

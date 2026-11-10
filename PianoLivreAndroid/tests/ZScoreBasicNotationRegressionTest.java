package br.org.pianolivre;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/**
 * ZAI-009: rest DURATIONS stay in MusicXML, but their GLYPHS are invisible.
 * Pitch, Note On/Off, chords, quarter/half rhythms, Study events remain intact.
 */
public final class ZScoreBasicNotationRegressionTest {
    private static int checks;
    private static void ok(boolean value,String label) {
        checks++; if (!value) throw new AssertionError(label);
    }
    private static MidiTakeRecorder.Event on(int pitch,long ns,int order) {
        return new MidiTakeRecorder.Event(0x90,pitch,80,ns,7,order,false);
    }
    private static MidiTakeRecorder.Event off(int pitch,long ns,int order) {
        return new MidiTakeRecorder.Event(0x90,pitch,0,ns,7,order,false);
    }
    private static CreationScoreGenerator.Generation make(List<MidiTakeRecorder.Event> events,long until)
            throws Exception {
        return CreationScoreGenerator.generate(events,
                Collections.singletonList(new MidiTakeRecorder.Segment(0,until)),"Notas e durações");
    }
    private static Document doc(String xml) throws Exception {
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
    private static Score read(String xml) throws Exception {
        return Score.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),"basic");
    }
    private static int count(Document doc) {
        NodeList nodes=doc.getElementsByTagName("note");
        int pitched=0,rests=0;
        for(int i=0;i<nodes.getLength();i++) {
            Element note=(Element)nodes.item(i);
            boolean isRest=note.getElementsByTagName("rest").getLength()>0;
            if(isRest) {
                rests++;
                ok("no".equals(note.getAttribute("print-object")),
                        "REST: invisible but time-bearing MusicXML event #"+i);
                ok(note.getElementsByTagName("duration").getLength()==1,
                        "REST: duration remains available for rhythm #"+i);
            }else{
                pitched++;
                ok(!"no".equals(note.getAttribute("print-object")),
                        "PITCH: notes remain visible #"+i);
            }
        }
        ok(rests>0,"REST: at least one hidden rest (empty bass staff)");
        return pitched;
    }
    private static void check(String name,CreationScoreGenerator.Generation g,int notes)
            throws Exception {
        Document d=doc(g.xml);
        ok(g.notes.size()==notes,name+": preserve original MIDI attack count");
        ok(count(d)==notes,name+": exactly one visible pitch per attack");
        ok(read(g.xml).events.size()==notes,name+": Study decodes exactly all attacks");
        ok(g.xml.contains("<staves>2</staves>"),name+": permanent treble/bass staff");
        ok(!g.xml.contains("<staccato")&&!g.xml.contains("<slur")
                &&!g.xml.contains("<tie")&&!g.xml.contains("<tied"),
                name+": zero articulation or ties");
        ok(ScoreXmlComposer.isCurrentDerivedXml(g.xml),name+": current ZAI-009 revision marker");
    }
    public static void main(String[] args) throws Exception {
        long q=500_000_000L;
        CreationScoreGenerator.Generation scale=make(Arrays.asList(
            on(60,0,0),off(60,75_000_000L,1),
            on(62,q,2),off(62,q+70_000_000L,3),
            on(64,2*q,4),off(64,2*q+90_000_000L,5),
            on(65,3*q,6),off(65,3*q+70_000_000L,7)),4*q);
        check("detached scale",scale,4);
        ok(scale.tempoReliable && Math.abs(scale.bpm-120)<5,
                "SCALE: BPM still inferred");
        ok(scale.xml.contains("<type>quarter</type>"),
                "SCALE: rhythmic figures remain");

        CreationScoreGenerator.Generation overlap=make(Arrays.asList(
            on(60,0,0),on(64,40_000_000L,1),on(67,50_000_000L,2),
            off(60,960_000_000L,3),off(64,965_000_000L,4),
            off(67,975_000_000L,5)),1_100_000_000L);
        check("C chord",overlap,3);
        ok(overlap.xml.contains("<chord/>"),"CHORD: simultaneous pitches remain stacked");

        CreationScoreGenerator.Generation cMinor=make(Arrays.asList(
            on(60,0,0),on(63,6_000_000L,1),on(67,13_000_000L,2),
            off(60,960_000_000L,3),off(63,970_000_000L,4),
            off(67,980_000_000L,5)),1_100_000_000L);
        check("C minor",cMinor,3);
        ok(cMinor.xml.contains("<step>E</step><alter>-1</alter><octave>4</octave>"),
                "Cm: E-flat is still written");

        CreationScoreGenerator.Generation fiveMinutes=make(Arrays.asList(
            on(60,0,0),off(60,300_000_000_000L,1)),300_100_000_000L);
        check("five minutes held",fiveMinutes,1);
        ok(fiveMinutes.notes.get(0).durationNs()==300_000_000_000L,
                "HOLD: Note Off is never shortened in source MIDI");
        ok(fiveMinutes.measures==1,
                "HOLD: visual figure stays simplified, no invented tied onsets");

        // Imported MusicXML is not rewritten. A user-imported visible rest
        // remains visible in its original source XML and correct in Study.
        String imported=scale.xml.replace(" print-object=\"no\"","");
        Score importedScore=read(imported);
        ok(importedScore.events.size()==4 && importedScore.sourceXml.contains("<note><rest/>"),
                "IMPORT: external files stay as supplied, without global rest removal");
        ok(!ScoreXmlComposer.isCurrentDerivedXml(
                scale.xml.replace(ScoreXmlComposer.GENERATOR_MARKER,"")),
                "OLD: prior derivative requires explicit regeneration");
        System.out.println("ZScoreBasicNotationRegressionTest: "+checks+
                " checks PASS; notes visible, rests silent/hidden, BPM+timing preserved");
    }
}

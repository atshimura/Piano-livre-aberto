package br.org.pianolivre;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * ZAI-004: real 'acordes.mid' user performance, reduced to event ticks and pitch
 * numbers (no personal metadata). Verifies source fidelity and NOTATION
 * coherence, not just a successfully rendered MusicXML document.
 *
 * First chord: MIDI 67/60/64, releases in 14.6 ms.
 * Second chord: MIDI 63/60/67, releases in 4.2 ms.
 * Original file PPQN=960 and a 120-BPM technical export clock.
 */
public final class ZScoreChordFidelityRegressionTest {
    private static int checks;
    private static void ok(boolean condition, String detail) {
        checks++;
        if (!condition) throw new AssertionError(detail);
    }
    private static long ns(int tick) { return Math.round(tick * 500_000_000.0 / 960.0); }
    private static MidiTakeRecorder.Event on(int pitch, int tick, int order) {
        return new MidiTakeRecorder.Event(0x90,pitch,73,ns(tick),7,order,false);
    }
    private static MidiTakeRecorder.Event off(int pitch, int tick, int order) {
        return new MidiTakeRecorder.Event(0x90,pitch,0,ns(tick),7,order,false);
    }
    private static CreationScoreGenerator.Generation run(List<MidiTakeRecorder.Event> events, long endNs) throws Exception {
        return CreationScoreGenerator.generate(events,
                Collections.singletonList(new MidiTakeRecorder.Segment(0,endNs)),"Acordes");
    }
    private static Document document(String xml) throws Exception {
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
    private static String inner(Element note,String name) {
        NodeList nodes=note.getElementsByTagName(name);
        return nodes.getLength()==0?"":nodes.item(0).getTextContent().trim();
    }
    private static int pitch(Element note) {
        String step=inner(note,"step");
        int offset=Arrays.asList("C","D","E","F","G","A","B").indexOf(step);
        int[] chromatic={0,2,4,5,7,9,11};
        return (Integer.parseInt(inner(note,"octave"))+1)*12+chromatic[offset]
               +(inner(note,"alter").isEmpty()?0:Integer.parseInt(inner(note,"alter")));
    }
    private static List<Element> playedNotes(Document doc) {
        ArrayList<Element> result=new ArrayList<>();
        NodeList nodes=doc.getElementsByTagName("note");
        for(int i=0;i<nodes.getLength();i++) {
            Element n=(Element)nodes.item(i);
            if(n.getElementsByTagName("rest").getLength()==0)result.add(n);
        }
        return result;
    }
    private static void checkOneChord(List<Element> notes,int start,int[] expected,String label) {
        int voice=Integer.parseInt(inner(notes.get(start),"voice"));
        String type=inner(notes.get(start),"type");
        int duration=Integer.parseInt(inner(notes.get(start),"duration"));
        for(int i=0;i<3;i++) {
            Element note=notes.get(start+i);
            ok(pitch(note)==expected[i],label+": pitch "+i+" expected "+expected[i]+" got "+pitch(note));
            ok((note.getElementsByTagName("chord").getLength()>0)==(i>0),
                    label+": only following two are <chord/>");
            ok(Integer.parseInt(inner(note,"voice"))==voice,label+": one voice");
            ok(inner(note,"type").equals(type)&&Integer.parseInt(inner(note,"duration"))==duration,
                    label+": identical written rhythm for all three keys");
            ok(note.getElementsByTagName("tie").getLength()==0,label+": no grid-induced ties");
        }
        ok(voice==1,label+": chord belongs on the treble staff's primary voice");
        ok("1".equals(inner(notes.get(start),"staff")),label+": treble staff");
    }

    private static List<MidiTakeRecorder.Event> authenticChords(){
        ArrayList<MidiTakeRecorder.Event> events=new ArrayList<>();
        events.add(on(67,6185,0));events.add(on(60,6193,1));events.add(on(64,6207,2));
        events.add(off(67,7263,3));events.add(off(60,7285,4));events.add(off(64,7291,5));
        events.add(on(63,9132,6));events.add(on(60,9138,7));events.add(on(67,9144,8));
        events.add(off(67,10394,9));events.add(off(60,10396,10));events.add(off(63,10402,11));
        return events;
    }

    public static void main(String[] args) throws Exception {
        CreationScoreGenerator.Generation real=run(authenticChords(),ns(11000));
        ok(real.notes.size()==6,"REAL: exactly six paired MIDI notes");
        ok(!real.tempoReliable && real.bpm==120,"REAL: only two attacks cannot establish BPM");
        ok(real.xml.contains("<staves>2</staves>"),"REAL: piano grand staff");
        List<Element> played=playedNotes(document(real.xml));
        ok(played.size()==6,"REAL: six noteheads only, not tied/repeated voices; got "+played.size());
        checkOneChord(played,0,new int[]{60,64,67},"C major real");
        checkOneChord(played,3,new int[]{60,63,67},"C minor real");
        ok(real.xml.contains("<step>E</step><alter>-1</alter><octave>4</octave>"),
                "REAL: E-flat, not D-sharp");
        Score study=Score.read(new ByteArrayInputStream(real.xml.getBytes(StandardCharsets.UTF_8)),"acordes");
        ok(study.events.size()==6,"REAL: Study must have only the six real note attacks");
        ok(real.xml.contains("<backup>"),"REAL: separate bass staff retains score integrity");

        // Genuine different Note Off durations: do not squash a sustained G4
        // into the E4/C4 chord. The first two should stay one coherent chord.
        List<MidiTakeRecorder.Event> sustain=Arrays.asList(
           on(60,0,0),on(64,8,1),on(67,15,2),
           off(60,960,3),off(64,968,4),off(67,2880,5));
        CreationScoreGenerator.Generation stretched=run(sustain,ns(3500));
        List<Element> stretchedNotes=playedNotes(document(stretched.xml));
        ok(stretched.notes.size()==3,"POLY: preserve all three sources");
        ok(stretchedNotes.size()==3,"POLY: three onsets, no accidental duplicate attacks");
        boolean hasLong=false;
        for(Element n:stretchedNotes)
            if(pitch(n)==67 && Integer.parseInt(inner(n,"duration"))>=2880)hasLong=true;
        ok(hasLong,"POLY: sustained G4 not shortened to match release of C4/E4");
        ok(stretched.xml.contains("<voice>2</voice>"),
                "POLY: significantly held note has its own voice");

        // Spelling must agree with the simple, already-correct Discovery writer
        // for all pitch classes. This is a test against future divergence.
        for(int midi=60;midi<72;midi++){
            String expected=ScoreXmlComposer.pitchXml(midi);
            String discovery=DiscoveryScoreBuilder.build(Collections.singletonList(midi),false);
            ok(discovery.contains(expected),"SPELL: both modes agree on MIDI "+midi);
        }

        // Explicit C4/B3 split must not be affected by chord normalization.
        List<MidiTakeRecorder.Event> grand=Arrays.asList(
            on(59,0,0),on(60,0,1),off(59,960,2),off(60,960,3));
        CreationScoreGenerator.Generation both=run(grand,ns(1150));
        ok(both.notes.size()==2 && playedNotes(document(both.xml)).size()==2,
                "STAFF: split retains two sources without duplicates");
        ok(both.xml.contains("<staff>1</staff>")&&both.xml.contains("<staff>2</staff>"),
                "STAFF: permanent treble and bass");

        System.out.println("ZScoreChordFidelityRegressionTest: "+checks
                +" checks PASS; C major/C minor are two intact chords");
    }
}

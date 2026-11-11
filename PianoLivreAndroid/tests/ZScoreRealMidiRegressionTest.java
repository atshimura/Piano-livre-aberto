package br.org.pianolivre;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * ZAI-002: regression from the maintainer's real 14-note MIDI recording, plus
 * independent staccato / legato / sustained-overlap / intra-bar-tie fixtures.
 *
 * The recorded file has SMF PPQN 960 and a technical 120 BPM export clock,
 * which is NOT the quarter tempo played by the pianist. Tick/Note On/Off
 * pairs below are copied from the real capture without identifying metadata.
 */
public final class ZScoreRealMidiRegressionTest {
    static int assertions;
    static void ok(boolean condition, String explanation) {
        assertions++;
        if (!condition) throw new AssertionError(explanation);
    }
    static final int[] SCALE = {60,62,64,65,67,69,71};
    static final int[] REAL_ON = {
        5500,7425,9376,11592,13564,15629,17692,
        21599,22619,23611,24610,25587,26628,27708
    };
    static final int[] REAL_OFF = {
        6340,8343,10418,12561,14633,16664,19128,
        22000,23025,23945,25026,25951,27009,29452
    };
    static long ticksNs(int tick) {
        return Math.round((tick - 5500) * 500_000_000.0 / 960.0);
    }
    static MidiTakeRecorder.Event event(int command, int pitch, int velocity, long ns, int order) {
        return new MidiTakeRecorder.Event(command,pitch,velocity,ns,7,order,false);
    }
    static CreationScoreGenerator.Generation run(List<MidiTakeRecorder.Event> events,long endNs) throws Exception {
        return CreationScoreGenerator.generate(events,
            Arrays.asList(new MidiTakeRecorder.Segment(0,endNs)),"Teste rítmico");
    }
    static Document xml(String score) throws Exception {
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(score.getBytes(StandardCharsets.UTF_8)));
    }
    static int count(String s,String part) {
        int count=0, from=0;
        while ((from=s.indexOf(part,from))>=0) {count++;from+=part.length();}
        return count;
    }
    static String child(Element el,String name) {
        NodeList found=el.getElementsByTagName(name);
        return found.getLength()==0?"":found.item(0).getTextContent().trim();
    }
    static List<Integer> attacks(Document doc) {
        ArrayList<Integer> pitches=new ArrayList<>();
        NodeList notes=doc.getElementsByTagName("note");
        for(int i=0;i<notes.getLength();i++) {
            Element element=(Element)notes.item(i);
            if(element.getElementsByTagName("rest").getLength()>0)continue;
            boolean continuation=false;
            NodeList ties=element.getElementsByTagName("tie");
            for(int j=0;j<ties.getLength();j++)
                if(((Element)ties.item(j)).getAttribute("type").equals("stop"))
                    continuation=true;
            if(continuation)continue;
            String step=child(element,"step");
            String octave=child(element,"octave");
            int accidental=child(element,"alter").isEmpty()?0:Integer.parseInt(child(element,"alter"));
            int offset=Arrays.asList("C","D","E","F","G","A","B").indexOf(step);
            int[] semitones={0,2,4,5,7,9,11};
            pitches.add(12*(Integer.parseInt(octave)+1)+semitones[offset]+accidental);
        }
        return pitches;
    }
    static List<MidiTakeRecorder.Event> uniform(int count,long attackNs,long heldNs) {
        ArrayList<MidiTakeRecorder.Event> events=new ArrayList<>();
        for(int i=0;i<count;i++){
            events.add(event(0x90,SCALE[i%SCALE.length],80,i*attackNs,i*2));
            events.add(event(0x90,SCALE[i%SCALE.length],0,i*attackNs+heldNs,i*2+1));
        }
        return events;
    }
    public static void main(String[] args)throws Exception {
        // --- Actual file: 7 slow half-like notes, then 7 faster quarter-like notes.
        ArrayList<MidiTakeRecorder.Event> real=new ArrayList<>();
        for(int i=0;i<14;i++){
            int pitch=SCALE[i%7];
            real.add(event(0x90,pitch,75,ticksNs(REAL_ON[i]),2*i));
            real.add(event(0x90,pitch,0,ticksNs(REAL_OFF[i]),2*i+1));
        }
        CreationScoreGenerator.Generation actual=run(real,ticksNs(REAL_OFF[13])+300_000_000L);
        ok(actual.notes.size()==14,"REAL: 14 Note On/Off pairs must survive");
        ok(actual.tempoReliable,"REAL: beat inference must not fall back to technical 120");
        ok(actual.bpm>106 && actual.bpm<120,"REAL: quarter tempo around 113, found "+actual.bpm);
        Document realXml=xml(actual.xml);
        List<Integer> expected=new ArrayList<>();
        for(int i=0;i<14;i++)expected.add(SCALE[i%7]);
        List<Integer> actualPitches=attacks(realXml);
        ok(actualPitches.equals(expected),"REAL: exact MIDI pitch/attack sequence, got "+actualPitches);
        ok(count(actual.xml,"<type>half</type>")>=7,
                "REAL: slow section should have seven half-note values");
        ok(count(actual.xml,"<type>quarter</type>")>=6,
                "REAL: fast section should have at least six quarter-note values");
        ok(count(actual.xml,"<staccato/>")==0,
                "REAL: no printed staccato, even though Note Off preserves detached playing");
        ok(actual.xml.contains("<staves>2</staves>"),"REAL: two staves even with only treble notes");
        int upperRests=0;
        NodeList xmlNotes=realXml.getElementsByTagName("note");
        for(int i=0;i<xmlNotes.getLength();i++){
            Element note=(Element)xmlNotes.item(i);
            if(note.getElementsByTagName("rest").getLength()>0 && child(note,"staff").equals("1"))
                upperRests++;
        }
        ok(upperRests<=4,"REAL: avoid rests between each detached note; upper rests="+upperRests);
        Score parsed=Score.read(new ByteArrayInputStream(actual.xml.getBytes(StandardCharsets.UTF_8)),"real-midi");
        ok(parsed.measures>0 && parsed.events.size()==14,"REAL: Study parser must retain all 14 attacks");

        // --- Staccato at stable BPM: 8 quarters, despite 180 ms key presses.
        CreationScoreGenerator.Generation stacc=run(uniform(8,500_000_000L,180_000_000L),4_000_000_000L);
        ok(count(stacc.xml,"<type>quarter</type>")==8,"STACC: eight written quarters");
        ok(count(stacc.xml,"<staccato/>")==0,"STACC: articulation is not printed in basic score");
        ok(attacks(xml(stacc.xml)).size()==8,"STACC: eight and only eight attacks");

        // --- Legato: Note Off 40ms after next attack, no sustained polyphony.
        ArrayList<MidiTakeRecorder.Event> legato=new ArrayList<>();
        for(int i=0;i<4;i++){
            legato.add(event(0x90,SCALE[i],80,i*500_000_000L,2*i));
            legato.add(event(0x80,SCALE[i],0,i*500_000_000L+540_000_000L,2*i+1));
        }
        CreationScoreGenerator.Generation leg=run(legato,2_100_000_000L);
        ok(attacks(xml(leg.xml)).size()==4,"LEGATO: four distinct attacks");
        // ZAI-006: overlapping keys do not prove an expressive slur.
        ok(!leg.xml.contains("<slur"),"LEGATO: no inferred expressive slur");
        Score legStudy=Score.read(new ByteArrayInputStream(leg.xml.getBytes(StandardCharsets.UTF_8)),"leg");
        ok(legStudy.events.size()==4,"LEGATO: notes remain four attacks despite physical overlap");

        // --- Genuine sustained overlap requires another voice, not truncation.
        List<MidiTakeRecorder.Event> overlap=Arrays.asList(
            event(0x90,60,75,0,0),event(0x80,60,0,1_500_000_000L,1),
            event(0x90,64,75,500_000_000L,2),event(0x80,64,0,1_000_000_000L,3),
            event(0x90,67,75,1_000_000_000L,4),event(0x80,67,0,1_500_000_000L,5),
            event(0x90,69,75,1_500_000_000L,6),event(0x80,69,0,2_000_000_000L,7));
        CreationScoreGenerator.Generation held=run(overlap,2_200_000_000L);
        ok(attacks(xml(held.xml)).size()==4,"OVERLAP: every pitch remains one musical attack");
        ok(held.xml.contains("<voice>2</voice>"),"OVERLAP: separate voice for sustained C4");
        ok(held.notes.get(0).durationNs()==1_500_000_000L,
            "OVERLAP: C4 keeps the full three-beat hold in original MIDI");
        ok(!held.xml.contains("<note><pitch><step>C</step><octave>4</octave></pitch><duration>2880</duration>"),
            "OVERLAP: written figure approximates to an undotted value");

        // --- One Note On held for 5/16: one written figure, no tied heads.
        CreationScoreGenerator.Generation five=run(Arrays.asList(
            event(0x90,60,80,0,0),event(0x80,60,0,625_000_000L,1)),800_000_000L);
        ok(!five.xml.contains("<tie"),"SIMPLE: zero automatic ties");
        ok(!five.xml.contains("<slur"),"SIMPLE: zero expressive slurs");
        ok(attacks(xml(five.xml)).size()==1,"SIMPLE: exactly one written head for one Note On");
        ok(five.notes.get(0).durationNs()==625_000_000L,
                "SIMPLE: original MIDI duration remains intact");
        Score fifth=Score.read(new ByteArrayInputStream(five.xml.getBytes(StandardCharsets.UTF_8)),"five16");
        ok(fifth.events.size()==1,"SIMPLE: Study recognises one written attack");

        System.out.println("ZScoreRealMidiRegressionTest: "+assertions+" verificações OK; real BPM="+actual.bpm);
    }
}

package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Z-P2 focused JVM tests: two-staff composition and MusicXML writing against the
 * ZAI-001 §8 matrix (R04–R09), plus validity, voice structure and limits.
 */
public final class ZScoreGeneratorXmlTest {
    private static int checks;
    private static void ok(boolean b, String message) { checks++; if (!b) throw new AssertionError(message); }

    private static long q = 500_000_000L; // 120 BPM reference pulse
    private static MidiTakeRecorder.Event on(int pitch, long ns, int order) {
        return new MidiTakeRecorder.Event(0x90, pitch, 80, ns, 7, order, false);
    }
    private static MidiTakeRecorder.Event off(int pitch, long ns, int order) {
        return new MidiTakeRecorder.Event(0x80, pitch, 0, ns, 7, order, false);
    }
    private static List<MidiTakeRecorder.Segment> seg(long start, long end) {
        return Collections.singletonList(new MidiTakeRecorder.Segment(start, end));
    }
    private static CreationScoreGenerator.Generation generate(List<MidiTakeRecorder.Event> events) throws IOException {
        return CreationScoreGenerator.generate(events, seg(0, 4 * q), "Teste");
    }
    private static CreationScoreGenerator.Generation generate(List<MidiTakeRecorder.Event> events, long endNs) throws IOException {
        return CreationScoreGenerator.generate(events, seg(0, endNs), "Teste");
    }
    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
    private static int count(String xml, String needle) {
        int n = 0, i = 0;
        while ((i = xml.indexOf(needle, i)) >= 0) { n++; i += needle.length(); }
        return n;
    }
    /** Collect (staff,pitch,type) of every written note head (chords included). */
    private static List<String> noteFacts(String xml) throws Exception {
        Document doc = parse(xml);
        List<String> facts = new ArrayList<>();
        NodeList notes = doc.getElementsByTagName("note");
        for (int i = 0; i < notes.getLength(); i++) {
            Element note = (Element) notes.item(i);
            NodeList staff = note.getElementsByTagName("staff");
            NodeList pitch = note.getElementsByTagName("pitch");
            if (pitch.getLength() == 0) continue;
            String step = ((Element) pitch.item(0)).getElementsByTagName("step").item(0).getTextContent();
            String octave = ((Element) pitch.item(0)).getElementsByTagName("octave").item(0).getTextContent();
            String alter = ((Element) pitch.item(0)).getElementsByTagName("alter").getLength() > 0
                    ? ((Element) pitch.item(0)).getElementsByTagName("alter").item(0).getTextContent() : "0";
            String type = ((Element) note.getElementsByTagName("type").item(0)).getTextContent();
            facts.add(staff.item(0).getTextContent().trim() + ":" + step + octave + "a" + alter + ":" + type);
        }
        return facts;
    }

    public static void main(String[] args) throws Exception {
        // --- R04: C3-E3-G3 chord on the bass staff. ---
        List<MidiTakeRecorder.Event> r04 = Arrays.asList(
                on(48, 0, 0), on(52, 0, 1), on(55, 0, 2),
                off(48, q, 3), off(52, q, 4), off(55, q, 5));
        CreationScoreGenerator.Generation r04g = generate(r04);
        List<String> facts = noteFacts(r04g.xml);
        ok(r04g.notes.size() == 3, "R04 three notes");
        ok(facts.contains("2:C3a0:quarter") && facts.contains("2:E3a0:quarter") && facts.contains("2:G3a0:quarter"),
                "R04 three correct quarter pitches on bass staff, got " + facts);
        ok(count(r04g.xml, "<chord/>") == 2, "R04 two chord members stacked");
        ok(r04g.measures == 1, "R04 single measure");
        boolean crossTie = r04g.xml.contains("<tie");
        ok(!crossTie, "R04 no spurious ties");

        // --- R05: C4-E4-G4 chord on the treble staff. ---
        List<MidiTakeRecorder.Event> r05 = Arrays.asList(
                on(60, 0, 0), on(64, 0, 1), on(67, 0, 2),
                off(60, q, 3), off(64, q, 4), off(67, q, 5));
        CreationScoreGenerator.Generation r05g = generate(r05);
        List<String> facts5 = noteFacts(r05g.xml);
        ok(facts5.contains("1:C4a0:quarter") && facts5.contains("1:E4a0:quarter") && facts5.contains("1:G4a0:quarter"),
                "R05 three correct pitches on treble staff, got " + facts5);

        // --- R06: both chords simultaneously: same attacks on both staves, nothing lost. ---
        List<MidiTakeRecorder.Event> r06 = new ArrayList<>(r04);
        List<MidiTakeRecorder.Event> r05copy = new ArrayList<>();
        for (MidiTakeRecorder.Event e : r05) r05copy.add(new MidiTakeRecorder.Event(e.status, e.data1, e.data2, e.timeNs, e.epoch, e.order + 6, e.generated));
        r06.addAll(r05copy);
        CreationScoreGenerator.Generation r06g = generate(r06);
        List<String> facts6 = noteFacts(r06g.xml);
        ok(r06g.notes.size() == 6, "R06 six notes");
        ok(facts6.contains("2:C3a0:quarter") && facts6.contains("1:C4a0:quarter") && facts6.contains("1:G4a0:quarter") && facts6.contains("2:G3a0:quarter"),
                "R06 same attacks on both staves");
        ok(count(r06g.xml, "<chord/>") == 4, "R06 both chords stacked");

        // --- R07: B3 (59) bass staff, C4 (60) treble staff. ---
        List<MidiTakeRecorder.Event> r07 = Arrays.asList(
                on(59, 0, 0), on(60, 0, 1), off(59, q, 2), off(60, q, 3));
        CreationScoreGenerator.Generation r07g = generate(r07);
        List<String> facts7 = noteFacts(r07g.xml);
        ok(facts7.contains("2:B3a0:quarter") && facts7.contains("1:C4a0:quarter"),
                "R07 MIDI 59/60 boundary splits staves, got " + facts7);

        // --- R08: sustained C4 while E4 starts: full durations, separate voices. ---
        List<MidiTakeRecorder.Event> r08 = Arrays.asList(
                on(60, 0, 0), on(64, 2 * q, 1), off(64, 3 * q, 2), off(60, 4 * q, 3));
        CreationScoreGenerator.Generation r08g = generate(r08);
        ok(r08g.xml.contains("<duration>3840</duration>"), "R08 sustained C4 keeps its full two-beat length");
        ok(r08g.notes.size() == 2 && r08g.notes.get(0).durationNs() == 4 * q, "R08 C4 not truncated");
        Document doc8 = parse(r08g.xml);
        NodeList voices8 = doc8.getElementsByTagName("voice");
        boolean hasTwoVoices = false;
        Set<String> distinct = new HashSet<>();
        for (int i = 0; i < voices8.getLength(); i++) distinct.add(voices8.item(i).getTextContent().trim());
        hasTwoVoices = distinct.size() >= 2;
        ok(hasTwoVoices, "R08 overlapping notes use separate voices, got " + distinct);
        ok(r08g.xml.contains("<backup>"), "R08 voices separated by backup");
        // C4 is a whole note (4 beats) and E4 is one quarter (1 beat).
        // Do not require invented quarter fragments for the sustained note.
        ok(count(r08g.xml, "<type>whole</type>") >= 1
                && count(r08g.xml, "<type>quarter</type>") >= 1,
                "R08 whole sustain plus quarter overlay preserved");

        // --- R09: half note starting on beat 4 crosses the barline with a tie. ---
        // First note anchors time zero; the tested half starts on beat 4 of measure 1.
        List<MidiTakeRecorder.Event> r09 = Arrays.asList(
                on(60, 0, 0), off(60, q, 1),
                on(62, 3 * q, 2), off(62, 5 * q, 3));
        CreationScoreGenerator.Generation r09g = generate(r09, 6 * q);
        ok(r09g.measures == 2, "R09 two measures");
        ok(r09g.xml.contains("<tie type=\"start\"/>") && r09g.xml.contains("<tie type=\"stop\"/>")
                && r09g.xml.contains("<tied type=\"start\"/>") && r09g.xml.contains("<tied type=\"stop\"/>"),
                "R09 tie across barline in both encodings");
        ok(count(r09g.xml, "<step>D</step>") == 2, "R09 same pitch on both sides, no new attack");
        Document doc9 = parse(r09g.xml);
        NodeList measures9 = doc9.getElementsByTagName("measure");
        ok(measures9.getLength() == 2, "R09 measure elements");

        // --- Validity, black keys, determinism, limits. ---
        CreationScoreGenerator.Generation det = generate(Arrays.asList(
                on(60, 0, 0), off(60, q, 1), on(61, q, 2), off(61, 2 * q, 3)));
        CreationScoreGenerator.Generation det2 = generate(Arrays.asList(
                on(60, 0, 0), off(60, q, 1), on(61, q, 2), off(61, 2 * q, 3)));
        ok(det.xml.equals(det2.xml), "deterministic output");
        List<String> sharp = noteFacts(det.xml);
        ok(sharp.contains("1:C4a1:quarter"), "black key spelled as C+alter 1, got " + sharp);
        ok(det.xml.contains("&lt;") == false && !det.xml.contains("Teste &"), "escaping applied");
        boolean rejected = false;
        try { ScoreXmlComposer.compose("x<>\"'", null, 120); } catch (IOException e) { rejected = true; }
        ok(rejected, "empty plan rejected");
        String escaped = ScoreXmlComposer.compose("A<B&C>\"'á", det.measures > 0 ? planOf(det) : null, 120);
        ok(escaped.contains("A&lt;B&amp;C&gt;&quot;&apos;á"), "title fully escaped, incl. accent");
        CreationScoreGenerator.Generation longest = generate(Arrays.asList(
                on(60, 0, 0), off(60, 16 * q, 1)), 18 * q);
        ok(longest.measures == 4, "whole-note handling across four measures, got " + longest.measures);
        ok(longest.xml.contains("<tie type=\"start\"/>"), "long note tied across bars");

        System.out.println("ZScoreGeneratorXmlTest: " + checks + " verificações OK");
    }
    private static ScoreRhythmQuantizer.Plan planOf(CreationScoreGenerator.Generation g) {
        // Rebuild the plan through the public pipeline for the escaping test.
        try {
            return ScoreRhythmQuantizer.quantize(g.notes, 500_000_000L);
        } catch (IOException e) { throw new RuntimeException(e); }
    }
}

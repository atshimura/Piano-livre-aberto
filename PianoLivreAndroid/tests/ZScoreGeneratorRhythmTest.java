package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Z-P1 focused JVM tests: MIDI pairing, tempo estimation and rhythm quantization
 * against the ZAI-001 §8 matrix (R01–R03, R10–R13) plus anomalous events.
 * Synthetic fixtures use a reference clock; expected results are exact figures.
 */
public final class ZScoreGeneratorRhythmTest {
    private static int checks;
    private static void ok(boolean b, String message) { checks++; if (!b) throw new AssertionError(message); }

    private static MidiTakeRecorder.Event on(int pitch, long ns, int order) {
        return new MidiTakeRecorder.Event(0x90, pitch, 80, ns, 7, order, false);
    }
    private static MidiTakeRecorder.Event off(int pitch, long ns, int order) {
        return new MidiTakeRecorder.Event(0x80, pitch, 0, ns, 7, order, false);
    }
    private static MidiTakeRecorder.Event offSynthetic(int pitch, long ns, int order) {
        return new MidiTakeRecorder.Event(0x80, pitch, 0, ns, 7, order, true);
    }
    private static List<MidiTakeRecorder.Segment> seg(long start, long end) {
        return Collections.singletonList(new MidiTakeRecorder.Segment(start, end));
    }
    /** Ascending C4..C5 scale of the given figure, one note per attack interval. */
    private static final int[] SCALE = {60, 62, 64, 65, 67, 69, 71, 72};
    private static List<MidiTakeRecorder.Event> scale(long firstNs, long attackNs, long durNs, int startOrder) {
        ArrayList<MidiTakeRecorder.Event> events = new ArrayList<>();
        int order = startOrder;
        for (int i = 0; i < SCALE.length; i++) {
            long at = firstNs + i * attackNs;
            events.add(on(SCALE[i], at, order++));
            events.add(off(SCALE[i], at + durNs, order++));
        }
        return events;
    }
    private static CreationScoreGenerator.Generation generate(List<MidiTakeRecorder.Event> events,
            List<MidiTakeRecorder.Segment> segments) throws IOException {
        return CreationScoreGenerator.generate(events, segments, "Teste & <Piano Livre>");
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
    /** Every voice block between backups must fill exactly one 4/4 measure (3840 ticks). */
    private static void okMeasureBlocksFill(String xml) throws Exception {
        Document doc = parse(xml);
        NodeList measures = doc.getElementsByTagName("measure");
        for (int m = 0; m < measures.getLength(); m++) {
            NodeList children = measures.item(m).getChildNodes();
            long current = 0; boolean sawNote = false;
            ArrayList<Long> sums = new ArrayList<>();
            for (int c = 0; c < children.getLength(); c++) {
                String name = children.item(c).getNodeName();
                if (name.equals("note")) {
                    sawNote = true;
                    NodeList inner = children.item(c).getChildNodes();
                    long dur = 0; boolean chord = false;
                    for (int k = 0; k < inner.getLength(); k++) {
                        String innerName = inner.item(k).getNodeName();
                        if (innerName.equals("duration")) dur = Long.parseLong(inner.item(k).getTextContent().trim());
                        if (innerName.equals("chord")) chord = true;
                    }
                    if (!chord) current += dur; // chord notes are stacked, they do not advance
                } else if (name.equals("backup")) {
                    NodeList inner = children.item(c).getChildNodes();
                    long back = 0;
                    for (int k = 0; k < inner.getLength(); k++)
                        if (inner.item(k).getNodeName().equals("duration"))
                            back = Long.parseLong(inner.item(k).getTextContent().trim());
                    sums.add(current); current -= back;
                }
            }
            if (sawNote) sums.add(current);
            for (long s : sums) ok(s == 3840, "measure " + (m + 1) + " voice block sums to " + s + ", expected 3840");
        }
    }

    public static void main(String[] args) throws Exception {
        // --- R01: C4..C5, 8 quarters at 120 BPM. ---
        long q120 = 500_000_000L;
        CreationScoreGenerator.Generation r01 = generate(
                scale(1_000_000_000L, q120, q120, 0), seg(1_000_000_000L, 6_000_000_000L));
        ok(r01.xml.contains("<staves>2</staves>") && r01.xml.contains("<sign>G</sign>")
                && r01.xml.contains("<sign>F</sign>"), "R01 two staves with G and F clefs");
        ok(count(r01.xml, "<type>quarter</type>") == 8, "R01 exactly eight quarters, got " + count(r01.xml, "<type>quarter</type>"));
        ok(r01.measures == 2, "R01 two measures, got " + r01.measures);
        ok(r01.tempoReliable && r01.bpm >= 115 && r01.bpm <= 125, "R01 tempo ~120, got " + r01.bpm);
        ok(r01.notes.size() == 8 && r01.notes.get(0).pitch == 60 && r01.notes.get(7).pitch == 72, "R01 scale pitches");
        ok(r01.notes.get(0).sourceId.startsWith("zg-"), "R01 stable new source identity");
        ok(r01.xml.contains("&amp;") && r01.xml.contains("&lt;Piano Livre&gt;"), "R01 XML title escaping");
        okMeasureBlocksFill(r01.xml);
        CreationScoreGenerator.Generation r01b = generate(
                scale(1_000_000_000L, q120, q120, 0), seg(1_000_000_000L, 6_000_000_000L));
        ok(r01b.xml.equals(r01.xml), "R01 deterministic output");

        // --- R02: 8 quarters then 8 halves at 120 BPM -> 8 quarters + 8 halves, 6 measures. ---
        ArrayList<MidiTakeRecorder.Event> r02Events = new ArrayList<>(scale(1_000_000_000L, q120, q120, 0));
        for (int i = 0; i < SCALE.length; i++) {
            long at = 1_000_000_000L + 8 * q120 + i * 2 * q120;
            r02Events.add(on(SCALE[i], at, 16 + i * 2));
            r02Events.add(off(SCALE[i], at + 2 * q120, 17 + i * 2));
        }
        CreationScoreGenerator.Generation r02 = generate(r02Events, seg(1_000_000_000L, 14_000_000_000L));
        ok(count(r02.xml, "<type>quarter</type>") == 8, "R02 exactly eight quarters");
        ok(count(r02.xml, "<type>half</type>") == 8, "R02 exactly eight halves");
        ok(r02.measures == 6, "R02 six measures, got " + r02.measures);
        ok(r02.tempoReliable && r02.bpm >= 115 && r02.bpm <= 125, "R02 tempo ~120, got " + r02.bpm);
        ok(count(r02.xml, "<type>whole</type>") == 6, "R02 six whole rests on the silent bass staff");
        okMeasureBlocksFill(r02.xml);

        // --- R03: same at 90 BPM: same figures, ~90 BPM, never 120. ---
        long q90 = 666_666_666L;
        ArrayList<MidiTakeRecorder.Event> r03Events = new ArrayList<>(scale(500_000_000L, q90, q90, 0));
        for (int i = 0; i < SCALE.length; i++) {
            long at = 500_000_000L + 8 * q90 + i * 2 * q90;
            r03Events.add(on(SCALE[i], at, 16 + i * 2));
            r03Events.add(off(SCALE[i], at + 2 * q90, 17 + i * 2));
        }
        CreationScoreGenerator.Generation r03 = generate(r03Events, seg(500_000_000L, 500_000_000L + 24 * q90));
        ok(count(r03.xml, "<type>quarter</type>") == 8, "R03 exactly eight quarters");
        ok(count(r03.xml, "<type>half</type>") == 8, "R03 exactly eight halves");
        ok(r03.measures == 6, "R03 six measures, got " + r03.measures);
        ok(r03.tempoReliable && r03.bpm >= 85 && r03.bpm <= 95, "R03 tempo ~90, got " + r03.bpm);
        ok(!(!r03.tempoReliable && r03.bpm == 120.0), "R03 must not impose provisional 120");
        okMeasureBlocksFill(r03.xml);

        // --- R10: one-beat rest between notes, next attack on beat 3. ---
        List<MidiTakeRecorder.Event> r10 = Arrays.asList(
                on(60, 0, 0), off(60, q120, 1),
                on(62, 2 * q120, 2), off(62, 3 * q120, 3));
        CreationScoreGenerator.Generation r10g = generate(r10, seg(0, 4 * q120));
        String first = r10g.xml.substring(r10g.xml.indexOf("<measure number=\"1\">"));
        int c4 = first.indexOf("<step>C</step>"), rest = first.indexOf("<rest/>"), d4 = first.indexOf("<step>D</step>");
        ok(c4 >= 0 && rest > c4 && d4 > rest, "R10 attack, rest and next attack in order");
        ok(count(r10g.xml, "<rest/>") >= 2, "R10 written rests present");
        ok(r10g.measures == 1, "R10 single measure");
        okMeasureBlocksFill(r10g.xml);

        // --- R11: staccato at 120: keep musical quarter values and mark articulation. ---
        ArrayList<MidiTakeRecorder.Event> r11Events = new ArrayList<>();
        for (int i = 0; i < SCALE.length; i++) {
            long at = 1_000_000_000L + i * q120;
            r11Events.add(on(SCALE[i], at, i * 2));
            r11Events.add(off(SCALE[i], at + 62_000_000L, i * 2 + 1)); // ~62 ms staccato release
        }
        CreationScoreGenerator.Generation r11 = generate(r11Events, seg(1_000_000_000L, 6_000_000_000L));
        ok(r11.notes.size() == 8, "R11 all eight attacks preserved");
        for (ScoreMidiPairer.PairedNote n : r11.notes)
            ok(n.endNs > n.startNs, "R11 no zero-length notes");
        ok(count(r11.xml,"<type>quarter</type>")==8, "R11 staccato retains eight quarter-note rhythmic values");
        ok(count(r11.xml,"<staccato/>")==0, "R11 timing remains quarter-note without printed staccato");
        ok(r11.tempoReliable, "R11 pulse still recognized from attacks, tempo " + r11.bpm);
        okMeasureBlocksFill(r11.xml);

        // --- R12: single note: tempo not safely identifiable, provisional 120 labelled. ---
        List<MidiTakeRecorder.Event> r12 = Arrays.asList(on(60, 0, 0), off(60, 800_000_000L, 1));
        CreationScoreGenerator.Generation r12g = generate(r12, seg(0, 1_000_000_000L));
        ok(!r12g.tempoReliable && r12g.bpm == 120.0, "R12 provisional 120 when unidentifiable");
        boolean labelled = false;
        for (String w : r12g.warnings) if (w.contains("não identificável")) labelled = true;
        ok(labelled, "R12 warning states unidentifiable tempo");
        ok(count(r12g.xml, "<sound tempo=\"120\"/>") == 1, "R12 XML carries provisional tempo");
        okMeasureBlocksFill(r12g.xml);

        // --- R13: pause/resume segments, synthetic releases, orphan Note Off. ---
        List<MidiTakeRecorder.Event> r13 = Arrays.asList(
                off(65, 1_050_000_000L, 0),                 // orphan Note Off before anything
                on(60, 1_100_000_000L, 1), offSynthetic(60, 1_600_000_000L, 2),
                on(62, 10_100_000_000L, 3), off(62, 10_800_000_000L, 4));
        List<MidiTakeRecorder.Segment> r13Segs = Arrays.asList(
                new MidiTakeRecorder.Segment(1_000_000_000L, 3_000_000_000L),
                new MidiTakeRecorder.Segment(10_000_000_000L, 12_000_000_000L));
        CreationScoreGenerator.Generation r13g = generate(r13, r13Segs);
        ok(r13g.notes.size() == 2, "R13 both notes paired across segments");
        ok(r13g.notes.get(0).startNs == 0, "R13 first attack defines time zero");
        ok(r13g.notes.get(1).startNs == 2_000_000_000L, "R13 recording pause removed from musical time");
        ok(r13g.notes.get(0).syntheticEnd, "R13 synthetic release flagged");
        boolean orphanWarned = false;
        for (String w : r13g.warnings) if (w.contains("órfão")) orphanWarned = true;
        ok(orphanWarned, "R13 orphan Note Off reported");
        okMeasureBlocksFill(r13g.xml);

        // --- Anomalies: retrigger, never-released note, take without notes. ---
        List<MidiTakeRecorder.Event> retrigger = Arrays.asList(
                on(60, 0, 0), on(60, 300_000_000L, 1), off(60, 600_000_000L, 2), off(60, 900_000_000L, 3));
        CreationScoreGenerator.Generation re = generate(retrigger, seg(0, 1_000_000_000L));
        ok(re.notes.size() == 2, "retrigger yields two notes");
        boolean retriggerWarned = false;
        for (String w : re.warnings) if (w.contains("sintético")) retriggerWarned = true;
        ok(retriggerWarned, "retrigger reported");
        List<MidiTakeRecorder.Event> held = Arrays.asList(on(60, 0, 0));
        CreationScoreGenerator.Generation he = generate(held, seg(0, 2_000_000_000L));
        ok(he.notes.size() == 1 && he.notes.get(0).endNs == 2_000_000_000L && he.notes.get(0).syntheticEnd,
                "never-released note closed at take end");
        boolean rejected = false;
        try { generate(Arrays.asList(new MidiTakeRecorder.Event(0xB0, 64, 127, 0, 7, 0, false)), seg(0, 1_000_000_000L)); }
        catch (IOException expected) { rejected = true; }
        ok(rejected, "take without notes is rejected, not silently empty");
        rejected = false;
        try {
            ArrayList<MidiTakeRecorder.Event> many = new ArrayList<>();
            for (int i = 0; i <= 8001; i++) { many.add(on(60, i * 1_000_000L, i * 2)); many.add(off(60, i * 1_000_000L + 500_000, i * 2 + 1)); }
            generate(many, seg(0, 10_000_000_000L));
        } catch (IOException expected) { rejected = true; }
        ok(rejected, "note limit rejected with clear error");

        System.out.println("ZScoreGeneratorRhythmTest: " + checks + " verificações OK");
    }
}

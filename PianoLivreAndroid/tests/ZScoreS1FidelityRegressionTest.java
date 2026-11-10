package br.org.pianolivre;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * ZAI-006: sanitized exact pitch/tick pairs from the user-supplied S1.mid.
 * The original user file, identifiers and binary are not redistributed.
 * SMF 960 PPQN, 500000 us/quarter (export timebase, not inferred tempo).
 *
 * 8 C4-C5 detached notes, 8 overlapping notes, then C and Cm chords.
 * Invariants: exactly 22 attacks; actual held notes remain held, notes
 * are not extended to the next chord; no automatic expressive slurs.
 */
public final class ZScoreS1FidelityRegressionTest {
    private static int checks;
    private static void ok(boolean value, String why) {
        checks++;
        if (!value) throw new AssertionError(why);
    }
    // pitch, Note On ticks, Note Off ticks (22 performance attacks).
    private static final int[][] S1 = {
        {60,4785,5482}, {62,5924,6638}, {64,7086,7837}, {65,8245,9004},
        {67,9515,10200}, {69,10671,11385}, {71,11932,12638}, {72,13271,15862},
        {60,18198,19468}, {62,19328,20626}, {64,20539,21819},
        {65,21735,23306}, {67,22959,23987}, {69,24008,25291},
        {71,25184,26435}, {72,26417,29294},
        {67,34117,35886}, {64,34126,35956}, {60,34146,35972},
        {60,37678,39536}, {63,37656,39582}, {67,37671,39659}
    };
    private static long ns(int tick) { return Math.round(tick * 500000000.0 / 960); }
    private static MidiTakeRecorder.Event event(int pitch, boolean attack, int tick, int order) {
        return new MidiTakeRecorder.Event(0x90, pitch, attack ? 85 : 0,
                ns(tick), 7, order, false);
    }
    private static CreationScoreGenerator.Generation make(List<MidiTakeRecorder.Event> events,
            long until) throws Exception {
        return CreationScoreGenerator.generate(events,
                Arrays.asList(new MidiTakeRecorder.Segment(0, until)), "S1");
    }
    private static Score read(CreationScoreGenerator.Generation score) throws Exception {
        return Score.read(new ByteArrayInputStream(score.xml.getBytes(StandardCharsets.UTF_8)), "S1");
    }
    private static int count(String s, String fragment) {
        int result = 0, pos = 0;
        while ((pos = s.indexOf(fragment, pos)) >= 0) { result++; pos += fragment.length(); }
        return result;
    }
    private static List<Score.Event> pitch(Score score, int midi) {
        ArrayList<Score.Event> found = new ArrayList<>();
        for (Score.Event event : score.events)
            if (event.midi == midi) found.add(event);
        found.sort(Comparator.comparingDouble(e -> e.beat));
        return found;
    }
    private static double seconds(double beats, double bpm) { return beats * 60.0 / bpm; }

    public static void main(String[] args) throws Exception {
        ArrayList<MidiTakeRecorder.Event> events = new ArrayList<>();
        for (int i = 0; i < S1.length; i++) {
            int[] n = S1[i];
            events.add(event(n[0], true, n[1], 2*i));
            events.add(event(n[0], false, n[2], 2*i+1));
        }
        CreationScoreGenerator.Generation generated = make(events,ns(42000));
        ok(generated.notes.size() == 22, "S1: 22 MIDI Note On/off pairs");
        ok(generated.tempoReliable, "S1: enough attacks to infer a pulse");
        ok(generated.bpm > 93 && generated.bpm < 105,
                "S1: actual pulse near 99 BPM, found " + generated.bpm);
        Score score = read(generated);
        ok(score.events.size() == 22,
                "S1: 22 musical attacks after merging tied fragments; got "+score.events.size());
        ok(generated.xml.contains("<staves>2</staves>"),"S1: grand staff");
        ok(count(generated.xml, "<slur") == 0,
                "S1: no expressive slur inferred from held keys");
        for (int i = 0; i < 8; i++) {
            ok(generated.notes.get(i).pitch == new int[]{60,62,64,65,67,69,71,72}[i],
                    "S1: detached scale note " + i);
            ok(generated.notes.get(i+8).pitch == new int[]{60,62,64,65,67,69,71,72}[i],
                    "S1: overlapping scale note " + i);
        }
        ok(count(generated.xml, "<staccato/>") >= 6,
                "S1: detached scale remains staccato, not repetitive inserted rests");
        List<Score.Event> c5 = pitch(score, 72);
        ok(c5.size() == 2, "S1: precisely two C5 attacks");
        // The score is intentionally simplified. The source MIDI still
        // retains the long releases even when a note crosses a barline.
        long firstC5=generated.notes.get(7).durationNs();
        long secondC5=generated.notes.get(15).durationNs();
        ok(Math.abs(firstC5/1e9-1.3495) < 0.03,
                "S1: original MIDI holds first C5 for ~1.35 seconds");
        ok(Math.abs(secondC5/1e9-1.4984) < 0.03,
                "S1: original MIDI holds second C5 for ~1.50 seconds");
        List<Score.Event> f4 = pitch(score,65);
        List<Score.Event> g4 = pitch(score,67);
        ok(f4.size() == 2, "S1: two F4 attacks");
        Score.Event heldF4 = f4.get(1);
        // Sol4 in second scale is the second G4 attack; the third belongs to
        // the C-major chord, the fourth to C-minor.
        ok(g4.size() == 4, "S1: four G4 attacks");
        Score.Event followingG4 = g4.get(1);
        ok(generated.notes.get(11).endNs > generated.notes.get(12).startNs,
                "S1: F4 original remains physically held after G4 attack");
        ok(count(generated.xml, "<tie") == 0 && count(generated.xml, "<tied") == 0,
                "S1: zero automatic ties in simplified notation");

        // Both chords must remain stacked and must not be extended until the
        // next chord when their actual Note Off occurs much earlier.
        ArrayList<Score.Event> chordC = new ArrayList<>(), chordCm = new ArrayList<>();
        for (Score.Event e : score.events) {
            if (e.beat >= 24 && e.beat <= 29 && (e.midi==60||e.midi==64||e.midi==67))
                chordC.add(e);
            if (e.beat > 29 && (e.midi==60||e.midi==63||e.midi==67))
                chordCm.add(e);
        }
        // Choose chord groups by the last two *distinct* simultaneous
        // onsets, independent of earlier rhythm/number of measures.
        ArrayList<Score.Event> lastSix = new ArrayList<>(score.events);
        lastSix.sort(Comparator.comparingDouble((Score.Event e)->e.beat)
                .thenComparingInt(e->e.midi));
        lastSix = new ArrayList<>(lastSix.subList(lastSix.size()-6,lastSix.size()));
        double cOnset = lastSix.get(0).beat, cmOnset = lastSix.get(3).beat;
        ok(lastSix.get(0).midi==60 && lastSix.get(1).midi==64 && lastSix.get(2).midi==67,
                "S1: first chord C major pitches");
        ok(lastSix.get(3).midi==60 && lastSix.get(4).midi==63 && lastSix.get(5).midi==67,
                "S1: second chord C minor pitches with E-flat");
        for (int i=0; i<3; i++) {
            Score.Event c = lastSix.get(i), cm = lastSix.get(i+3);
            ok(Math.abs(c.beat-cOnset)<0.001 && Math.abs(cm.beat-cmOnset)<0.001,
                    "S1: each chord is one simultaneous attack group");
            ok(c.length > 0 && cm.length > 0,
                    "S1: both chords retain a positive single written figure");
        }
        ok(cOnset + lastSix.get(0).length + 0.25 < cmOnset,
                "S1: preserve actual silence between C and Cm chords");

        // Five-minute key hold: original MIDI remains five minutes.
        // Simplified written score depicts ONE note in ONE 4/4 measure,
        // with no ties and a deliberately shorter written duration.
        List<MidiTakeRecorder.Event> held = Arrays.asList(
                event(60,true,0,0),
                new MidiTakeRecorder.Event(0x80,60,0,300_000_000_000L,7,1,false));
        CreationScoreGenerator.Generation fiveMinutes=make(held,300_100_000_000L);
        ok(fiveMinutes.notes.size()==1,"HOLD-5M: one MIDI attack");
        ok(fiveMinutes.notes.get(0).durationNs()==300_000_000_000L,
                "HOLD-5M: original Note Off is still 300 seconds later");
        ok(fiveMinutes.measures==1,"HOLD-5M: simplified notation only one measure");
        ok(!fiveMinutes.xml.contains("<tie")&&!fiveMinutes.xml.contains("<slur"),
                "HOLD-5M: no automatic ligatures");
        Score fiveMinutesStudy=read(fiveMinutes);
        ok(fiveMinutesStudy.events.size()==1,
                "HOLD-5M: Study sees exactly one written attack");
        ok(fiveMinutesStudy.events.get(0).length<=4,
                "HOLD-5M: written duration is intentionally capped to a measure");
        System.out.println("ZScoreS1FidelityRegressionTest: "+checks
                +" checks PASS; 22 original attacks, no auto slurs, duration retained");
    }
}

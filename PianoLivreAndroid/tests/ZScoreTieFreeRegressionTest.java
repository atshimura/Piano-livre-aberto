package br.org.pianolivre;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** ZAI-007: one MIDI attack = one written notehead, no ties or slurs.
 * Intentional limitation: visual durations are reduced at bar boundaries.
 */
public final class ZScoreTieFreeRegressionTest {
    private static int checks;
    private static void ok(boolean value,String description) {
        checks++; if (!value) throw new AssertionError(description);
    }
    private static MidiTakeRecorder.Event on(int midi,long when,int order) {
        return new MidiTakeRecorder.Event(0x90,midi,90,when,7,order,false);
    }
    private static MidiTakeRecorder.Event off(int midi,long when,int order) {
        return new MidiTakeRecorder.Event(0x80,midi,0,when,7,order,false);
    }
    private static CreationScoreGenerator.Generation write(List<MidiTakeRecorder.Event> events,long endNs)
            throws Exception {
        return CreationScoreGenerator.generate(events,
                Collections.singletonList(new MidiTakeRecorder.Segment(0,endNs)),"Sem ligaduras");
    }
    private static int count(String xml,String fragment) {
        int total=0,p=0;
        while ((p=xml.indexOf(fragment,p))>=0) { total++;p+=fragment.length(); }
        return total;
    }
    private static Score parse(String xml)throws Exception {
        return Score.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),"no-ties");
    }
    private static void noTies(CreationScoreGenerator.Generation score,int sourceAttacks,String name)
            throws Exception {
        ok(score.notes.size()==sourceAttacks,name+": preserve raw attack count");
        ok(count(score.xml,"<pitch>")==sourceAttacks,
                name+": one written pitched notehead per source attack");
        ok(!score.xml.contains("<tie")&&!score.xml.contains("<tied")
                &&!score.xml.contains("<slur"),name+": no tie/tied/slur");
        ok(parse(score.xml).events.size()==sourceAttacks,name+": parsed attacks unchanged");
        ok(score.xml.contains("<staves>2</staves>"),name+": grand staff remains");
        ok(ScoreXmlComposer.isCurrentDerivedXml(score.xml),name+": current engine revision marker");
    }
    public static void main(String[] args)throws Exception {
        long q=500_000_000L;
        // 5/16 once required 4/16+1/16 linked heads. Now one quarter.
        CreationScoreGenerator.Generation five=write(Arrays.asList(
                on(60,0,0),off(60,625_000_000L,1)),800_000_000L);
        noTies(five,1,"5/16");
        ok(count(five.xml,"<type>quarter</type>")==1,
                "5/16: one conventional approximation (quarter)");
        ok(five.notes.get(0).durationNs()==625_000_000L,"5/16: MIDI unchanged");

        // A half note starting on beat 4 cannot fit the bar. Keep one
        // written quarter (beat 4) without continuation, retain two beats
        // in the source MIDI and let the next measure disappear if unused.
        CreationScoreGenerator.Generation crossed=write(Arrays.asList(
                on(60,0,0),off(60,q,1),
                on(62,3*q,2),off(62,5*q,3)),6*q);
        noTies(crossed,2,"cross-bar");
        ok(crossed.measures==1,"cross-bar: only one written measure");
        ok(crossed.notes.get(1).durationNs()==2*q,"cross-bar: original D4 holds two beats");

        // Two simultaneous chord tones need one head each, not a tied line.
        CreationScoreGenerator.Generation chord=write(Arrays.asList(
                on(60,0,0),on(64,0,1),on(67,0,2),
                off(60,q,3),off(64,q,4),off(67,q,5)),2*q);
        noTies(chord,3,"C major");
        ok(count(chord.xml,"<chord/>")==2,"C major: triad stacked");

        // Repeated key attacks are actual repetitions, not tied copies.
        CreationScoreGenerator.Generation repeated=write(Arrays.asList(
                on(60,0,0),off(60,q/2,1),
                on(60,q,2),off(60,q+q/2,3)),2*q);
        noTies(repeated,2,"same-pitch repeated");
        ok(count(repeated.xml,"<step>C</step>")==2,"retrigger: two actual C4 heads");

        // Even a five-minute key hold creates exactly ONE written note,
        // while its true duration remains five minutes in the source.
        CreationScoreGenerator.Generation longHold=write(Arrays.asList(
                on(60,0,0),off(60,300_000_000_000L,1)),300_100_000_000L);
        noTies(longHold,1,"five-minute hold");
        ok(longHold.measures==1,"five-minute hold: no blank continuation measures");
        ok(longHold.notes.get(0).durationNs()==300_000_000_000L,
                "five-minute hold: 300 seconds in original MIDI");

        // Saved derivations from the prior engine may still exist; they
        // cannot be reused silently by the current Creation preview.
        String oldXml=chord.xml.replace(ScoreXmlComposer.GENERATOR_MARKER,"");
        ok(!ScoreXmlComposer.isCurrentDerivedXml(oldXml),
                "legacy PLS2 XML is marked obsolete without deleting its bytes");
        ok(ScoreXmlComposer.isCurrentDerivedXml(chord.xml),
                "new PLS2 XML is reusable without unnecessary regeneration");
        System.out.println("ZScoreTieFreeRegressionTest: "+checks+
                " checks PASS; one head per attack, no automatic ligatures");
    }
}

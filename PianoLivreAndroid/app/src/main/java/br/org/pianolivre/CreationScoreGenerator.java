package br.org.pianolivre;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Z-P1/Z-P2 facade: turns a saved C1 recording (.plc events + segments) into internal
 * MusicXML with automatic tempo and duration recognition. Pure and deterministic;
 * never writes the source recording (ZAI-001 §1, §3).
 *
 * Pipeline: ScoreMidiPairer (capture) → ScoreTempoEstimator (pulse inference) →
 * ScoreRhythmQuantizer (grid, voices, staves) → ScoreXmlComposer (MusicXML).
 * When the pulse cannot be claimed with safety, the estimate falls back to a clearly
 * labelled provisional 120 BPM and the warnings say so.
 */
public final class CreationScoreGenerator {
    public static final int MAX_NOTES = ScoreMidiPairer.MAX_NOTES;

    /** Complete generation result: MusicXML plus legible musical metadata. */
    public static final class Generation {
        public final String xml;
        public final List<ScoreMidiPairer.PairedNote> notes;
        public final int measures;
        public final double bpm; // always finite; provisional 120 when tempoReliable == false
        public final boolean tempoReliable;
        public final ScoreTempoEstimator.Confidence confidence;
        public final List<String> warnings;
        Generation(String xml, List<ScoreMidiPairer.PairedNote> notes, int measures,
                   double bpm, boolean tempoReliable, ScoreTempoEstimator.Confidence confidence,
                   List<String> warnings) {
            this.xml = xml; this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
            this.measures = measures; this.bpm = bpm; this.tempoReliable = tempoReliable;
            this.confidence = confidence;
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        }
    }

    private CreationScoreGenerator() {}

    public static Generation generate(List<MidiTakeRecorder.Event> events,
                                      List<MidiTakeRecorder.Segment> segments,
                                      String title) throws IOException {
        ScoreMidiPairer.Result paired = ScoreMidiPairer.pair(events, segments);
        ScoreTempoEstimator.Estimate tempo = ScoreTempoEstimator.estimate(paired.notes);
        ScoreRhythmQuantizer.Plan plan = ScoreRhythmQuantizer.quantize(paired.notes, tempo.quarterNs);
        String xml = ScoreXmlComposer.compose(title, plan, tempo.bpm);
        ArrayList<String> warnings = new ArrayList<>();
        warnings.addAll(paired.warnings);
        warnings.addAll(tempo.warnings);
        warnings.addAll(plan.warnings);
        return new Generation(xml, paired.notes, plan.measures, tempo.bpm,
                tempo.reliable(), tempo.confidence, warnings);
    }
}

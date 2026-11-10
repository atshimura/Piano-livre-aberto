package br.org.pianolivre;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ZAI-002: beat inference uses the spacing between MIDI Note On attacks, not
 * key-press durations (Note Off). Durations belong to articulation analysis.
 *
 * Repeated short and long intervals in an approximate 1:2 relationship are
 * interpreted against one pulse, even when the human performance is not exactly
 * aligned to a global sixteenth-note grid. The shorter frequent interval is
 * favoured as the quarter-note pulse; ambiguous cases are labelled.
 */
public final class ScoreTempoEstimator {
    public static final long MIN_QUARTER_NS = 250_000_000L;
    public static final long MAX_QUARTER_NS = 2_000_000_000L;
    public static final int GRID_PER_QUARTER = 4;
    public static final long PROVISIONAL_QUARTER_NS = 500_000_000L;

    public enum Confidence { HIGH, MEDIUM, LOW }

    public static final class Estimate {
        public final long quarterNs;
        public final double bpm;
        public final Confidence confidence;
        public final List<String> warnings;
        public final double gridFitRatio;
        Estimate(long q, double bpm, Confidence conf, List<String> warnings, double fit) {
            quarterNs = q; this.bpm = bpm; confidence = conf;
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
            gridFitRatio = fit;
        }
        public boolean reliable() { return confidence != Confidence.LOW; }
    }

    private ScoreTempoEstimator() {}

    private static Estimate provisional(List<String> warnings, double fit) {
        warnings.add("Prévia provisória a 120 BPM; andamento não identificável com segurança");
        return new Estimate(PROVISIONAL_QUARTER_NS, 120, Confidence.LOW, warnings, fit);
    }

    public static Estimate estimate(List<ScoreMidiPairer.PairedNote> notes) throws IOException {
        if (notes == null || notes.isEmpty()) throw new IOException("Sem notas para estimar andamento");
        ArrayList<String> warnings = new ArrayList<>();
        ArrayList<Long> intervals = new ArrayList<>();
        long previous = -1;
        // Same-time chord tones count as one rhythmic attack.
        for (ScoreMidiPairer.PairedNote note : notes) {
            if (previous >= 0 && note.startNs > previous)
                intervals.add(note.startNs - previous);
            previous = note.startNs;
        }
        if (intervals.size() < 3) {
            warnings.add("Poucos ataques distintos para estimar o pulso");
            return provisional(warnings, 0);
        }

        /*
         * Group observed inter-onset intervals into approximate clusters
         * (18% tolerance), then infer the quarter from the most common cluster.
         * Half-note attacks near 2*q and longer phrase gaps near 4*q contribute
         * musical evidence but MUST NOT force a fixed 120 BPM.
         *
         * Unlike the retired estimator, this does not reject rubato merely
         * because accumulated absolute onsets drift off an ideal beat grid.
         */
        long best = 0;
        int bestExact = -1;
        int bestHarmonics = -1;
        double bestError = Double.POSITIVE_INFINITY;
        for (long seed : intervals) {
            if (seed < MIN_QUARTER_NS || seed > MAX_QUARTER_NS) continue;
            ArrayList<Long> cluster = new ArrayList<>();
            for (long d : intervals)
                if (near(d, seed, 0.18)) cluster.add(d);
            if (cluster.size() < 3) continue;
            Collections.sort(cluster);
            long q = cluster.get(cluster.size() / 2);
            if (q < MIN_QUARTER_NS || q > MAX_QUARTER_NS) continue;
            int exact = 0, harmonics = 0;
            double error = 0;
            for (long d : intervals) {
                if (near(d, q, 0.18)) {
                    exact++; error += Math.abs(d / (double) q - 1);
                } else if (near(d, 2L * q, 0.18)) {
                    harmonics++; error += Math.abs(d / (double)(2L * q) - 1);
                } else if (near(d, 3L * q, 0.18) || near(d, 4L * q, 0.18)) {
                    harmonics++;
                }
            }
            // Exact repetition dominates: select the shortest frequent pulse
            // when the counts tie (e.g. real halfs at 1.06s, quarters at .53s).
            if (exact > bestExact
                    || exact == bestExact && harmonics > bestHarmonics
                    || exact == bestExact && harmonics == bestHarmonics
                    && (q < best || best == 0)) {
                best = q; bestExact = exact; bestHarmonics = harmonics; bestError = error;
            }
        }
        if (best == 0) {
            warnings.add("Intervalos irregulares ou sem pulso suficientemente repetido");
            return provisional(warnings, 0);
        }
        int matched = 0;
        int doubled = 0;
        for (long d : intervals) {
            if (near(d, best, 0.22) || near(d, 2L * best, 0.22)
                    || near(d, 3L * best, 0.22) || near(d, 4L * best, 0.22)) matched++;
            if (near(d, 2L * best, 0.18)) doubled++;
        }
        double fit = matched / (double) intervals.size();
        if (fit < 0.65) {
            warnings.add("Pulsação pouco estável (intervalos entre ataques muito variados)");
            return provisional(warnings, fit);
        }
        if (doubled >= 2)
            warnings.add("Intervalos em proporção 1:2 identificados; duração escrita será deduzida dos ataques, não do tempo de tecla");
        if (fit < 0.9)
            warnings.add("Pequenas variações de tempo normalizadas em torno do pulso estimado");
        Confidence confidence = fit >= 0.90 && intervals.size() >= 6
                ? Confidence.HIGH : Confidence.MEDIUM;
        double bpm = Math.round((60_000_000_000.0 / best) * 10.0) / 10.0;
        return new Estimate(best, bpm, confidence, warnings, fit);
    }

    private static boolean near(long value, long target, double tolerance) {
        return target > 0 && Math.abs(value - (double) target) <= tolerance * target;
    }
}

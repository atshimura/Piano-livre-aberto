package br.org.pianolivre;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Z-P1: estimates the quarter-note pulse of a take from attack intervals and held
 * durations. Musical inference, not a fixed ms→figure conversion (ZAI-001 §3).
 *
 * Method (deterministic, global for the whole take):
 * - Candidate quarter periods are built from observed inter-onset intervals and held
 *   durations, each interpreted as a common note value (16th..whole).
 * - Every candidate is scored by: onsets fitting the sixteenth-note grid, written values
 *   landing on usual figures (including dotted), intervals fitting the same grid, and a
 *   fragmentation penalty for unusual values. A fixed 120 BPM is never assumed.
 * - Ties are broken deterministically: the most common attack interval is interpreted as
 *   one quarter (the natural exercise reading). This keeps 8 quarters + 8 halves at 90 and
 *   at 120 BPM producing the same figures with their own tempo, instead of collapsing
 *   everything into a doubled or halved pulse.
 * - A half/double candidate that scores the same as the winner degrades confidence:
 *   the reading is musically ambiguous and the UI must label the tempo as unproven.
 */
public final class ScoreTempoEstimator {
    /** Accepted quarter range: 30..240 BPM. */
    public static final long MIN_QUARTER_NS = 250_000_000L, MAX_QUARTER_NS = 2_000_000_000L;
    /** Grid resolution: sixteenth notes per quarter (the finest V0 figure, ZAI-001 §3.2
     *  "1/16 grade" — musically the semicolcheia, 4 cells per quarter). */
    public static final int GRID_PER_QUARTER = 4;
    /** Provisional tempo used only when nothing can be claimed. */
    public static final long PROVISIONAL_QUARTER_NS = 500_000_000L;

    public enum Confidence { HIGH, MEDIUM, LOW }

    public static final class Estimate {
        public final long quarterNs;
        public final double bpm;
        public final Confidence confidence;
        public final List<String> warnings;
        public final double gridFitRatio;
        Estimate(long quarterNs, double bpm, Confidence confidence, List<String> warnings, double fit) {
            this.quarterNs = quarterNs; this.bpm = bpm; this.confidence = confidence;
            this.warnings = Collections.unmodifiableList(warnings); this.gridFitRatio = fit;
        }
        /** True when the tempo may be presented as identified; false keeps a labelled 120 BPM preview. */
        public boolean reliable() { return confidence != Confidence.LOW; }
    }

    private ScoreTempoEstimator() {}

    /** Written figures in sixteenth-note grid cells: 16th, 8th, 8th., quarter, quarter.,
     *  half, half., whole. One grid cell is one sixteenth. */
    static final int[] USUAL_CELLS = {1, 2, 3, 4, 6, 8, 12, 16};
    /** Candidate interpretations of an observed interval, as fraction of a quarter. */
    private static final double[] ROLES = {1.0 / 4, 1.0 / 2, 1.0, 2.0, 3.0, 4.0};

    public static Estimate estimate(List<ScoreMidiPairer.PairedNote> notes) throws IOException {
        ArrayList<String> warnings = new ArrayList<>();
        if (notes == null || notes.isEmpty()) throw new IOException("Sem notas para estimar andamento");
        if (notes.size() < 2) {
            warnings.add("Gravação com nota única: andamento não identificável com segurança");
            warnings.add("Prévia provisória a 120 BPM; o valor musical não está provado");
            return new Estimate(PROVISIONAL_QUARTER_NS, 120.0, Confidence.LOW, warnings, 0.0);
        }

        // --- Observed intervals (onset deltas) and held durations, in ns. ---
        ArrayList<Long> deltasList = new ArrayList<>();
        for (int i = 1; i < notes.size(); i++) {
            long d = notes.get(i).startNs - notes.get(i - 1).startNs;
            if (d > 0) deltasList.add(d);
        }
        ArrayList<Long> durationsList = new ArrayList<>();
        for (ScoreMidiPairer.PairedNote n : notes) if (n.durationNs() > 0) durationsList.add(n.durationNs());
        if (deltasList.isEmpty()) {
            warnings.add("Todos os ataques coincidem: andamento não identificável com segurança");
            warnings.add("Prévia provisória a 120 BPM; o valor musical não está provado");
            return new Estimate(PROVISIONAL_QUARTER_NS, 120.0, Confidence.LOW, warnings, 1.0);
        }

        // --- Candidate quarter periods. ---
        ArrayList<Long> candidates = new ArrayList<>();
        for (long d : deltasList)
            for (double role : ROLES) addCandidate(candidates, Math.round(d / role));
        for (long d : durationsList) // sustained durations suggest legato figures; fewer roles.
            for (double role : new double[]{1.0 / 8, 1.0 / 4, 1.0 / 2, 1.0, 2.0, 4.0})
                addCandidate(candidates, Math.round(d / role));
        Collections.sort(candidates);
        if (candidates.isEmpty()) {
            warnings.add("Intervalos fora da faixa de andamento suportada (30–240 BPM)");
            warnings.add("Prévia provisória a 120 BPM; o valor musical não está provado");
            return new Estimate(PROVISIONAL_QUARTER_NS, 120.0, Confidence.LOW, warnings, 0.0);
        }

        // Most common attack interval (fuzzy buckets) = the exercise's natural quarter reading.
        long modalDelta = modalDelta(deltasList);

        // --- Score every candidate. ---
        long bestQuarter = candidates.get(0);
        double bestScore = -1;
        ArrayList<long[]> scored = new ArrayList<>(); // {quarterNs, scoreMillis}
        for (long quarter : candidates) {
            long grid = quarter / GRID_PER_QUARTER;
            if (grid <= 0) continue;
            double onsetFit = 0, valueFit = 0, deltaFit = 0;
            for (ScoreMidiPairer.PairedNote n : notes) {
                long start = Math.round(n.startNs / (double) grid);
                if (Math.abs(n.startNs - start * grid) <= 0.35 * grid) onsetFit += 1;
                long cells = Math.max(1, Math.round(n.durationNs() / (double) grid));
                if (usual(cells)) valueFit += 1;
            }
            for (long d : deltasList) {
                long cells = Math.round(d / (double) grid);
                if (Math.abs(d - cells * grid) <= 0.35 * grid) deltaFit += 1;
            }
            double n = notes.size(), nd = deltasList.size();
            double score = 3 * (onsetFit / n) + 2 * (valueFit / n) + (deltaFit / nd);
            scored.add(new long[]{quarter, Math.round(score * 1_000_000)});
            // Deterministic tie-break: prefer the candidate closest to "modal delta = 1 quarter".
            if (score > bestScore + 1e-9
                    || (Math.abs(score - bestScore) <= 1e-9 && prefer(quarter, bestQuarter, modalDelta))) {
                bestScore = score; bestQuarter = quarter;
            }
        }
        scored.sort((a, b) -> b[1] != a[1] ? Long.compare(b[1], a[1]) : Long.compare(a[0], b[0]));

        // --- Ambiguity and confidence. ---
        double bestFit = fitRatio(notes, bestQuarter);
        Confidence confidence;
        long runnerQuarter = bestQuarter;
        double runnerScore = bestScore;
        if (scored.size() > 1) {
            runnerQuarter = scored.get(1)[0];
            runnerScore = scored.get(1)[1] / 1_000_000.0;
        }
        // A half/double reading often scores the same: the 1/16 grid always fits when the
        // winner fits. The modal-interval tie-break already chose deterministically; the
        // ambiguity is reported, and confidence only drops when even the modal reading
        // could not separate the candidates (equidistant ties) or the fit is weak.
        double bestScorePoints = bestScore;
        double ratio = (double) Math.max(runnerQuarter, bestQuarter) / Math.min(runnerQuarter, bestQuarter);
        boolean halfDoubleTie = scored.size() > 1
                && Math.abs(ratio - 2.0) < 0.05
                && Math.abs(bestScorePoints - runnerScore) <= 0.02;
        double dBest = Math.abs(Math.log(bestQuarter / (double) modalDelta));
        double dRunner = Math.abs(Math.log(runnerQuarter / (double) modalDelta));
        boolean modalTie = halfDoubleTie && Math.abs(dBest - dRunner) < 1e-9;
        if (modalTie) warnings.add("Andamento de metade/dobro indistinguível nesta execução");
        else if (halfDoubleTie)
            warnings.add("Leitura de metade/dobro também é possível; adotou-se o intervalo de ataque mais comum como a semínima");
        if (notes.size() < 4 || bestFit < 0.9 || modalTie) {
            confidence = Confidence.LOW;
            warnings.add("Confiança baixa: andamento não identificável com segurança");
            warnings.add("Prévia provisória a 120 BPM; o valor musical não está provado");
            return new Estimate(PROVISIONAL_QUARTER_NS, 120.0, Confidence.LOW, warnings, bestFit);
        }
        if (notes.size() >= 8 && bestFit >= 0.95) confidence = Confidence.HIGH;
        else confidence = Confidence.MEDIUM;
        double bpm = 60_000_000_000.0 / bestQuarter;
        return new Estimate(bestQuarter, Math.round(bpm * 10) / 10.0, confidence, warnings, bestFit);
    }

    private static boolean usual(long cells) {
        for (int u : USUAL_CELLS) if (u == cells) return true;
        return false;
    }
    private static double fitRatio(List<ScoreMidiPairer.PairedNote> notes, long quarter) {
        long grid = quarter / GRID_PER_QUARTER;
        if (grid <= 0) return 0;
        int fit = 0;
        for (ScoreMidiPairer.PairedNote n : notes) {
            long start = Math.round(n.startNs / (double) grid);
            if (Math.abs(n.startNs - start * grid) <= 0.35 * grid) fit++;
        }
        return notes.isEmpty() ? 0 : fit / (double) notes.size();
    }
    /** True when candidate q should win over incumbent best for the same score. */
    private static boolean prefer(long q, long best, long modalDelta) {
        double dq = Math.abs(Math.log(q / (double) modalDelta));
        double db = Math.abs(Math.log(best / (double) modalDelta));
        if (Math.abs(dq - db) > 1e-12) return dq < db;
        return q < best; // stable final ordering
    }
    /** Group intervals within ±15% buckets; return the bucket centre with the most members. */
    private static long modalDelta(List<Long> deltas) {
        ArrayList<Long> sorted = new ArrayList<>(deltas);
        Collections.sort(sorted);
        long bestCentre = sorted.get(0); int bestCount = -1;
        int i = 0;
        while (i < sorted.size()) {
            long centre = sorted.get(i);
            int j = i;
            while (j < sorted.size() && sorted.get(j) <= centre + centre * 3 / 20) j++;
            if (j - i > bestCount) { bestCount = j - i; bestCentre = centre; }
            i = Math.max(i + 1, j);
        }
        return bestCentre;
    }
    /** Register a candidate inside the accepted tempo range, deduplicated by ±2%. */
    private static void addCandidate(ArrayList<Long> candidates, long quarter) {
        if (quarter < MIN_QUARTER_NS || quarter > MAX_QUARTER_NS) return;
        for (long existing : candidates)
            if (Math.abs(existing - quarter) * 50 < existing) return;
        candidates.add(quarter);
    }
}

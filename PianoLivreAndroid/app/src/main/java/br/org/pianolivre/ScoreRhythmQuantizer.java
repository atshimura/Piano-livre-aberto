package br.org.pianolivre;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ZAI-002: written rhythms are inferred from ATTACK SPACING, while key-release
 * time specifies articulation (staccato / legato) and genuine sustained overlap.
 *
 * A detached note remains one written quarter/half when its Note Off precedes
 * the next Note On. A brief overlap expresses legato; a sustained overlap must
 * retain its duration by allocating another voice. Every fragmentation of ONE
 * source attack (including within a bar) is tied, never a new musical attack.
 */
public final class ScoreRhythmQuantizer {
    public static final int SPLIT_MIDI = 60;
    public static final int BEATS_PER_MEASURE = 4;
    public static final int CELLS_PER_MEASURE = 16;
    public static final int DIVISIONS = 960;
    public static final int TICKS_PER_CELL = 240;
    public static final int MAX_MEASURES = 1024;
    public static final int MAX_VOICES_PER_STAFF = 4;

    public static final class Token {
        public final boolean rest;
        public final int pitch, cells;
        public final boolean chord, tieStart, tieStop;
        public final boolean staccato, slurStart, slurStop;
        public final ScoreMidiPairer.PairedNote source;
        Token(boolean rest, int pitch, int cells, boolean chord, boolean tieStart, boolean tieStop,
              ScoreMidiPairer.PairedNote source) {
            this(rest, pitch, cells, chord, tieStart, tieStop, source, false, false, false);
        }
        Token(boolean rest, int pitch, int cells, boolean chord, boolean tieStart, boolean tieStop,
              ScoreMidiPairer.PairedNote source, boolean staccato, boolean slurStart, boolean slurStop) {
            this.rest = rest; this.pitch = pitch; this.cells = cells;
            this.chord = chord; this.tieStart = tieStart; this.tieStop = tieStop;
            this.source = source; this.staccato = staccato;
            this.slurStart = slurStart; this.slurStop = slurStop;
        }
    }
    public static final class VoicePlan {
        public final int staff, voice;
        public final List<Token> tokens;
        VoicePlan(int staff, int voice, List<Token> tokens) {
            this.staff = staff; this.voice = voice; this.tokens = Collections.unmodifiableList(tokens);
        }
    }
    public static final class MeasurePlan {
        public final int index;
        public final List<VoicePlan> voices;
        MeasurePlan(int index, List<VoicePlan> voices) {
            this.index = index; this.voices = Collections.unmodifiableList(voices);
        }
    }
    public static final class Plan {
        public final int measures;
        public final List<MeasurePlan> measureList;
        public final List<String> warnings;
        public final int noteCount;
        Plan(int measures, List<MeasurePlan> measureList, List<String> warnings, int noteCount) {
            this.measures = measures; this.measureList = Collections.unmodifiableList(measureList);
            this.warnings = Collections.unmodifiableList(warnings); this.noteCount = noteCount;
        }
    }
    private ScoreRhythmQuantizer() {}
    static final int[] FIGURES = {16, 12, 8, 6, 4, 3, 2, 1};

    /** Return a normal figure when sufficiently close; preserve fine-grid values otherwise. */
    private static long snap(long cells, boolean allowingLong) {
        if (cells <= 1) return 1;
        long chosen = cells, bestError = Long.MAX_VALUE;
        for (int figure : FIGURES) {
            long error = Math.abs(cells - figure);
            if (error < bestError && error <= Math.max(0.5, 0.14 * figure)) {
                bestError = error; chosen = figure;
            }
        }
        if (allowingLong && cells > CELLS_PER_MEASURE)
            return Math.max(CELLS_PER_MEASURE, Math.round(cells / 4.0) * 4L);
        return chosen;
    }
    private static long cells(long nanos, long quarterNs) {
        return Math.max(1, Math.round(nanos / (quarterNs / 4.0)));
    }

    public static Plan quantize(List<ScoreMidiPairer.PairedNote> notes, long quarterNs) throws IOException {
        ArrayList<String> warnings = new ArrayList<>();
        if (notes == null || notes.isEmpty()) throw new IOException("Sem notas para escrever a partitura");
        if (quarterNs <= 0) throw new IOException("Andamento inválido para quantização");
        if (notes.size() > ScoreMidiPairer.MAX_NOTES) throw new IOException("Número excessivo de notas");
        ArrayList<ScoreMidiPairer.PairedNote> ordered = new ArrayList<>(notes);
        ordered.sort(Comparator.comparingLong((ScoreMidiPairer.PairedNote n) -> n.startNs)
                .thenComparingInt(n -> n.pitch));
        int n = ordered.size();
        long[] start = new long[n];
        long chordToleranceNs = Math.min(65_000_000L, (long)(quarterNs * 0.12));
        long groupStart = -1, groupCell = 0;
        int noteGroups = 0;
        for (int i = 0; i < n; i++) {
            long onset = ordered.get(i).startNs;
            if (groupStart < 0) {
                groupStart = onset; groupCell = 0; noteGroups++;
            } else if (onset - groupStart > chordToleranceNs) {
                long delta = Math.max(1, onset - groupStart);
                long gridCells = cells(delta, quarterNs);
                groupCell += snap(gridCells, true);
                groupStart = onset; noteGroups++;
            }
            start[i] = groupCell;
        }

        // Map the neighbouring DISTINCT attack on the SAME staff. Chord tones
        // share one rhythmic start and must not look like short notes/rests.
        long[] nextCell = new long[n], afterNextCell = new long[n], prevGap = new long[n];
        long[] nextNs = new long[n];
        int[] nextIndex = new int[n];
        long[] currentCell = {-1, -1}, upcomingCell = {-1, -1}, upcoming2Cell = {-1, -1};
        long[] currentNs = {-1, -1}, upcomingNs = {-1, -1};
        int[] currentIndex = {-1, -1}, upcomingIndex = {-1, -1};
        for (int i = n - 1; i >= 0; i--) {
            int staff = ordered.get(i).pitch >= SPLIT_MIDI ? 0 : 1;
            if (start[i] != currentCell[staff]) {
                upcoming2Cell[staff] = upcomingCell[staff];
                upcomingCell[staff] = currentCell[staff];
                upcomingNs[staff] = currentNs[staff];
                upcomingIndex[staff] = currentIndex[staff];
                currentCell[staff] = start[i];
                currentNs[staff] = ordered.get(i).startNs;
                currentIndex[staff] = i;
            } else {
                currentIndex[staff] = i;
            }
            nextCell[i] = upcomingCell[staff];
            afterNextCell[i] = upcoming2Cell[staff];
            nextNs[i] = upcomingNs[staff];
            nextIndex[i] = upcomingIndex[staff];
        }
        long[] lastStaffStart = {-1, -1}, gapBefore = {0, 0};
        for (int i = 0; i < n; i++) {
            int staff = ordered.get(i).pitch >= SPLIT_MIDI ? 0 : 1;
            if (start[i] != lastStaffStart[staff]) {
                gapBefore[staff] = lastStaffStart[staff] < 0 ? 0 : start[i] - lastStaffStart[staff];
                lastStaffStart[staff] = start[i];
            }
            prevGap[i] = gapBefore[staff];
        }

        ArrayList<Placed> placed = new ArrayList<>();
        long[] noteLength = new long[n];
        boolean[] detached = new boolean[n];
        int detachedCount = 0, overlapCount = 0;
        long maxEnd = 1;
        for (int i = 0; i < n; i++) {
            ScoreMidiPairer.PairedNote note = ordered.get(i);
            long next = nextCell[i];
            long gap = next >= 0 ? next - start[i] : 0;
            long physical = snap(cells(note.durationNs(), quarterNs), false);
            long length;

            // A Note On is an attack. A Note Off ends that SAME key press.
            // Note Off does not become a rest by itself, and the next Note On
            // cannot truncate a key that is still down. Quantize the real
            // duration unless clear attack spacing implies detached playing.
            if (next < 0) {
                // No upcoming attack: use actual key release. The only
                // exception is a clearly detached *very short* final note
                // in a locally regular series (R11: 62 ms on quarter beats).
                long previous = prevGap[i];
                boolean shortFinalStaccato = !note.syntheticEnd
                        && previous >= 2 && previous <= 8
                        && note.durationNs() < previous * (quarterNs / 4.0) * 0.35;
                length = shortFinalStaccato ? previous : physical;
            } else {
                long physicalNs = Math.max(0, note.durationNs());
                long onsetGapNs = Math.max(1, nextNs[i] - note.startNs);
                double ratio = physicalNs / (double) onsetGapNs;
                // A real gap between groups/phrases takes priority over
                // attack-spacing quantization. This is decisive for the
                // separate C/Cm chords and C5 at the end of both S1 scales.
                boolean genuineSilence = gap >= 8 && ratio < 0.80;
                boolean detachedRegular = !note.syntheticEnd
                        && gap <= 8 && ratio < 0.72;
                boolean nearNextAttack = ratio >= 0.80 && ratio <= 1.16;
                if (genuineSilence) {
                    length = physical;
                } else if (detachedRegular || nearNextAttack) {
                    length = gap;
                } else {
                    // Significant held overlap or an atypical duration:
                    // physical time decides the figure, not the next attack.
                    length = physical;
                }
                long overlapNs = note.endNs - nextNs[i];
                if (overlapNs >= Math.max(quarterNs / 8, 65_000_000L)
                        && length > gap) overlapCount++;
            }
            length = Math.max(1, length);
            noteLength[i] = length;
            if (!note.syntheticEnd && note.durationNs() < length * (quarterNs / 4.0) * 0.72
                    && (next >= 0 || prevGap[i] > 0)) {
                detached[i] = true;
                detachedCount++;
            }
            maxEnd = Math.max(maxEnd, start[i] + length);
        }

        // ZAI-004: a chord is one musical attack, even when individual fingers
        // release a few milliseconds apart on opposite sides of a grid boundary.
        // Only near-equal physical releases are normalized. A genuinely held
        // tone keeps its separate duration/voice for polyphonic notation.
        int coherentChords = cohereChordReleases(ordered, start, noteLength, detached,
                nextCell, prevGap, quarterNs);
        maxEnd = 1;
        detachedCount = 0;
        for (int i = 0; i < n; i++) {
            if (detached[i]) detachedCount++;
            maxEnd = Math.max(maxEnd, start[i] + noteLength[i]);
        }
        if (coherentChords > 0)
            warnings.add(coherentChords + " acorde(s) mantidos íntegros apesar de pequenas diferenças de liberação MIDI");
        if (detachedCount > 0)
            warnings.add(detachedCount + " nota(s) destacadas: duração musical inferida dos ataques, articulação staccato pelo Note Off");
        // An overlap is factual MIDI data, not proof of musical phrasing.
        // Never invent an expressive slur from timing. Source Note Off remains
        // available for articulation analysis and significant overlaps are
        // represented via actual sustained durations / independent voices.
        if (overlapCount > 0)
            warnings.add(overlapCount + " sobreposição(ões) MIDI sustentadas preservadas sem ligadura de expressão");
        boolean[] slurStart = new boolean[n], slurStop = new boolean[n];

        for (int i = 0; i < n; i++) {
            ScoreMidiPairer.PairedNote note = ordered.get(i);
            placed.add(new Placed(note, start[i], start[i] + noteLength[i],
                    note.pitch >= SPLIT_MIDI ? 1 : 2, detached[i], slurStart[i], slurStop[i]));
        }
        long count = (maxEnd + CELLS_PER_MEASURE - 1) / CELLS_PER_MEASURE;
        if (count > MAX_MEASURES) throw new IOException("Gravação muito longa para a partitura");
        int measures = (int) count;

        // Deterministic allocation: same onset+end is a chord; genuine overlap
        // with different duration occupies an independent voice.
        placed.sort(Comparator.comparingLong((Placed p) -> p.startCell)
                .thenComparingLong(p -> p.endCell).thenComparingInt(p -> p.note.pitch));
        ArrayList<ArrayList<ArrayList<Slot>>> staffVoices = new ArrayList<>();
        staffVoices.add(new ArrayList<>()); staffVoices.add(new ArrayList<>());
        for (Placed p : placed) {
            ArrayList<ArrayList<Slot>> voices = staffVoices.get(p.staff - 1);
            int target = -1; boolean chord = false;
            for (int v = 0; v < voices.size() && target < 0; v++) {
                Slot tail = voices.get(v).get(voices.get(v).size() - 1);
                if (tail.startCell == p.startCell && tail.endCell == p.endCell
                        && tail.note.pitch != p.note.pitch) {
                    target = v; chord = true;
                }
            }
            for (int v = 0; v < voices.size() && target < 0; v++) {
                Slot tail = voices.get(v).get(voices.get(v).size() - 1);
                if (tail.endCell <= p.startCell) target = v;
            }
            if (target < 0) {
                if (voices.size() >= MAX_VOICES_PER_STAFF)
                    throw new IOException("Mais de quatro vozes simultâneas na pauta " + p.staff);
                target = voices.size(); voices.add(new ArrayList<>());
            }
            voices.get(target).add(new Slot(p, chord));
        }

        ArrayList<ArrayList<ArrayList<Draft>>> work = new ArrayList<>();
        for (int s = 0; s < 2; s++) {
            ArrayList<ArrayList<Draft>> staff = new ArrayList<>();
            for (int v = 0; v < staffVoices.get(s).size(); v++) {
                ArrayList<Draft> fragments = new ArrayList<>();
                for (Slot slot : staffVoices.get(s).get(v)) decompose(slot, fragments);
                staff.add(fragments);
            }
            work.add(staff);
        }
        ArrayList<MeasurePlan> measurePlans = new ArrayList<>();
        int written = 0;
        for (int m = 0; m < measures; m++) {
            ArrayList<VoicePlan> voicePlans = new ArrayList<>();
            for (int s = 0; s < 2; s++) {
                int voiceBase = s == 0 ? 1 : 5;
                for (int v = 0; v < work.get(s).size(); v++) {
                    ArrayList<Draft> drafts = new ArrayList<>();
                    for (Draft d : work.get(s).get(v)) if (d.measure == m) drafts.add(d);
                    if (drafts.isEmpty()) {
                        if (v == 0) voicePlans.add(new VoicePlan(s + 1, voiceBase,
                                Collections.singletonList(new Token(true, 0, CELLS_PER_MEASURE, false, false, false, null))));
                        continue;
                    }
                    drafts.sort(Comparator.comparingLong((Draft d) -> d.pos)
                            .thenComparingInt(d -> d.chord ? 1 : 0));
                    ArrayList<Token> tokens = new ArrayList<>();
                    long cursor = 0, lastHeadPos = -1;
                    for (Draft d : drafts) {
                        boolean chord = !tokens.isEmpty() && lastHeadPos == d.pos;
                        if (d.pos > cursor) { fillRests(tokens, cursor, d.pos); cursor = d.pos; }
                        else if (d.pos < cursor && !chord)
                            throw new IOException("Sobreposição rítmica não resolvida na pauta " + (s + 1));
                        tokens.add(new Token(false, d.note.pitch, d.cells, chord,
                                d.tieStart, d.tieStop, d.note,
                                d.staccato, d.slurStart, d.slurStop));
                        if (!chord) cursor += d.cells;
                        lastHeadPos = d.pos;
                        written++;
                    }
                    if (cursor < CELLS_PER_MEASURE) fillRests(tokens, cursor, CELLS_PER_MEASURE);
                    if (cursor > CELLS_PER_MEASURE) throw new IOException("Compasso excedido na pauta " + (s + 1));
                    voicePlans.add(new VoicePlan(s + 1, voiceBase + v, tokens));
                }
                boolean staffPresent = false;
                for (VoicePlan vp : voicePlans) if (vp.staff == s + 1) staffPresent = true;
                if (!staffPresent)
                    voicePlans.add(new VoicePlan(s + 1, voiceBase,
                            Collections.singletonList(new Token(true, 0, CELLS_PER_MEASURE, false, false, false, null))));
            }
            voicePlans.sort(Comparator.comparingInt((VoicePlan vp) -> vp.staff).thenComparingInt(vp -> vp.voice));
            measurePlans.add(new MeasurePlan(m, voicePlans));
        }
        return new Plan(measures, measurePlans, warnings, written);
    }

    /**
     * Groups only notes on the SAME staff and SAME quantized onset, with real
     * Note Off events within a limited physical tolerance. Small release
     * differences must never manufacture voices or ties in a simple chord.
     *
     * The group is formed from Note Off timestamps, not their rounded durations.
     * A markedly longer note remains outside the group; its sustain/voice is
     * therefore preserved (ZAI-002 polyphony contract).
     */
    private static int cohereChordReleases(List<ScoreMidiPairer.PairedNote> notes,
            long[] starts, long[] lengths, boolean[] staccato,
            long[] nextCell, long[] prevGap, long quarterNs) {
        int count = 0;
        final long toleranceNs = Math.min(70_000_000L,
                Math.max(25_000_000L, (long)(quarterNs * 0.14)));
        for (int begin = 0; begin < notes.size();) {
            int end = begin + 1;
            while (end < notes.size() && starts[end] == starts[begin]) end++;
            for (int staff = 1; staff <= 2; staff++) {
                ArrayList<Integer> indices = new ArrayList<>();
                for (int i = begin; i < end; i++) {
                    int noteStaff = notes.get(i).pitch >= SPLIT_MIDI ? 1 : 2;
                    if (noteStaff == staff) indices.add(i);
                }
                indices.sort(Comparator.comparingLong(i -> notes.get(i).endNs));
                for (int first = 0; first < indices.size();) {
                    int last = first + 1;
                    long earliestRelease = notes.get(indices.get(first)).endNs;
                    while (last < indices.size()
                            && notes.get(indices.get(last)).endNs - earliestRelease <= toleranceNs)
                        last++;
                    if (last - first >= 2) {
                        boolean authentic = true;
                        for (int a = first; a < last; a++) {
                            ScoreMidiPairer.PairedNote current = notes.get(indices.get(a));
                            if (current.syntheticEnd) authentic = false;
                            for (int b = a + 1; b < last; b++)
                                if (current.pitch == notes.get(indices.get(b)).pitch) authentic = false;
                        }
                        if (authentic) {
                            long[] lengthsInGroup = new long[last - first];
                            long[] holdsInGroup = new long[last - first];
                            for (int a = first; a < last; a++) {
                                int i = indices.get(a);
                                lengthsInGroup[a - first] = lengths[i];
                                holdsInGroup[a - first] = notes.get(i).durationNs();
                            }
                            java.util.Arrays.sort(lengthsInGroup);
                            java.util.Arrays.sort(holdsInGroup);
                            long nominal = lengthsInGroup[lengthsInGroup.length / 2];
                            long written = simpleChordDuration(nominal);
                            long physical = holdsInGroup[holdsInGroup.length / 2];
                            for (int a = first; a < last; a++) {
                                int i = indices.get(a);
                                lengths[i] = written;
                                staccato[i] = physical < written * (quarterNs / 4.0) * 0.72
                                        && (nextCell[i] >= 0 || prevGap[i] > 0);
                            }
                            count++;
                        }
                    }
                    first = last;
                }
            }
            begin = end;
        }
        return count;
    }

    /**
     * Prefer a single readable note over a 4+1/16 tie produced solely by
     * jitter near the quantizer grid. When equally close, prefer the shorter
     * figure. Long held chords retain multiple beats and cross-bar ties.
     */
    private static long simpleChordDuration(long cells) {
        if (cells <= 1) return 1;
        if (cells > CELLS_PER_MEASURE)
            return Math.max(CELLS_PER_MEASURE, Math.round(cells / 4.0) * 4L);
        int chosen = FIGURES[0];
        long bestDifference = Long.MAX_VALUE;
        for (int figure : FIGURES) {
            long difference = Math.abs(cells - figure);
            if (difference < bestDifference || (difference == bestDifference && figure < chosen)) {
                chosen = figure;
                bestDifference = difference;
            }
        }
        return chosen;
    }

    private static void decompose(Slot slot, ArrayList<Draft> fragments) {
        long cursor = slot.startCell, end = slot.endCell;
        int fragmentIndex = 0;
        while (cursor < end) {
            int measure = (int) (cursor / CELLS_PER_MEASURE);
            long inMeasure = cursor % CELLS_PER_MEASURE;
            long remainInMeasure = CELLS_PER_MEASURE - inMeasure;
            long remaining = Math.min(end - cursor, remainInMeasure);
            int figure = 1;
            for (int candidate : FIGURES)
                if (candidate <= remaining && inMeasure % candidate == 0) {
                    figure = candidate; break;
                }
            boolean hasPrevious = fragmentIndex > 0;
            boolean hasNext = cursor + figure < end;
            // IMPORTANT: this must tie fragments inside the SAME BAR too.
            fragments.add(new Draft(measure, inMeasure, figure, slot.note,
                    slot.chord, hasNext, hasPrevious,
                    slot.staccato && !hasPrevious,
                    slot.slurStart && !hasPrevious,
                    slot.slurStop && !hasNext));
            fragmentIndex++; cursor += figure;
        }
    }

    private static void fillRests(List<Token> tokens, long from, long to) {
        long cursor = from;
        while (cursor < to) {
            int figure = 1;
            for (int f : FIGURES)
                if (f <= to - cursor && cursor % f == 0) {
                    figure = f; break;
                }
            tokens.add(new Token(true, 0, figure, false, false, false, null));
            cursor += figure;
        }
    }

    private static final class Placed {
        final ScoreMidiPairer.PairedNote note;
        final long startCell, endCell;
        final int staff;
        final boolean staccato, slurStart, slurStop;
        Placed(ScoreMidiPairer.PairedNote n, long from, long to, int staff,
               boolean staccato, boolean slurStart, boolean slurStop) {
            note=n; startCell=from; endCell=to; this.staff=staff;
            this.staccato=staccato; this.slurStart=slurStart; this.slurStop=slurStop;
        }
    }
    private static final class Slot {
        final Placed placed;
        final ScoreMidiPairer.PairedNote note;
        final long startCell, endCell;
        final boolean chord, staccato, slurStart, slurStop;
        Slot(Placed placed, boolean chord) {
            this.placed=placed; note=placed.note;
            startCell=placed.startCell; endCell=placed.endCell; this.chord=chord;
            staccato=placed.staccato; slurStart=placed.slurStart; slurStop=placed.slurStop;
        }
    }
    private static final class Draft {
        final int measure;
        final long pos;
        final int cells;
        final ScoreMidiPairer.PairedNote note;
        final boolean chord, tieStart, tieStop, staccato, slurStart, slurStop;
        Draft(int m, long p, int c, ScoreMidiPairer.PairedNote note,
              boolean chord, boolean tieStart, boolean tieStop,
              boolean staccato, boolean slurStart, boolean slurStop) {
            measure=m; pos=p; cells=c; this.note=note; this.chord=chord;
            this.tieStart=tieStart; this.tieStop=tieStop;
            this.staccato=staccato; this.slurStart=slurStart; this.slurStop=slurStop;
        }
    }
}

package br.org.pianolivre;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Z-P1: quantizes paired notes onto the sixteenth-note grid and produces the written
 * plan for the score: fixed 4/4 measures, staff split at MIDI 60, voice allocation by
 * interval occupancy, chords, rests and bar-crossing ties (ZAI-001 §2, §3.3).
 *
 * - Attack and release are quantized separately; a zero-length written note is forbidden
 *   and becomes one grid cell with a warning.
 * - Grid cell = one sixteenth note (semicolcheia), 4 per quarter; the finest V0 figure.
 * - Same staff (MIDI 60 or above → treble staff 1; below 60 → bass staff 2), same attack
 *   cell and same length, different pitches → one chord. Different lengths → independent
 *   voices, so no note is ever truncated to fit a single voice.
 * - A note still sounding when another starts on the same staff takes the next free voice;
 *   allocation is deterministic (lowest voice first, at most 4 per staff).
 * - Notes crossing a barline are split with tie start/stop; any length is decomposed into
 *   the largest writable figures (whole..16th, with dotted variants) aligned to the grid.
 * - Voice 1 of each staff exists in every measure (whole rest when silent), so both staves
 *   are always visible; higher voices appear only in measures where they sound.
 */
public final class ScoreRhythmQuantizer {
    public static final int SPLIT_MIDI = 60;
    public static final int BEATS_PER_MEASURE = 4;
    public static final int CELLS_PER_MEASURE = ScoreTempoEstimator.GRID_PER_QUARTER * BEATS_PER_MEASURE; // 16 sixteenths
    public static final int DIVISIONS = 960; // MusicXML ticks per quarter, fixed resolution
    public static final int TICKS_PER_CELL = DIVISIONS / ScoreTempoEstimator.GRID_PER_QUARTER; // 240 = one 16th
    public static final int MAX_MEASURES = 1024;
    public static final int MAX_VOICES_PER_STAFF = 4;

    /** One written token inside a voice; notes keep their source for audits. */
    public static final class Token {
        public final boolean rest;
        public final int pitch;
        public final int cells; // writable figure length in sixteenth-note cells (4 = quarter)
        public final boolean chord, tieStart, tieStop;
        public final ScoreMidiPairer.PairedNote source; // null for rests
        Token(boolean rest, int pitch, int cells, boolean chord, boolean tieStart, boolean tieStop,
              ScoreMidiPairer.PairedNote source) {
            this.rest = rest; this.pitch = pitch; this.cells = cells;
            this.chord = chord; this.tieStart = tieStart; this.tieStop = tieStop;
            this.source = source;
        }
    }

    /** One voice inside one measure of one staff, tokens in writing order. */
    public static final class VoicePlan {
        public final int staff, voice; // voice: 1..4 treble staff, 5..8 bass staff
        public final List<Token> tokens;
        VoicePlan(int staff, int voice, List<Token> tokens) {
            this.staff = staff; this.voice = voice; this.tokens = Collections.unmodifiableList(tokens);
        }
    }

    /** One measure: voices per staff, in writing order (staff 1 first). */
    public static final class MeasurePlan {
        public final int index; // 0-based
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

    /** Writable figure lengths in sixteenth cells: whole, half., half, quarter., quarter,
     *  8th., 8th, 16th. Largest first. */
    static final int[] FIGURES = {16, 12, 8, 6, 4, 3, 2, 1};

    public static Plan quantize(List<ScoreMidiPairer.PairedNote> notes, long quarterNs) throws IOException {
        ArrayList<String> warnings = new ArrayList<>();
        if (notes == null || notes.isEmpty()) throw new IOException("Sem notas para escrever a partitura");
        if (quarterNs <= 0) throw new IOException("Andamento inválido para quantização");
        long gridNs = quarterNs / ScoreTempoEstimator.GRID_PER_QUARTER;
        if (gridNs <= 0) throw new IOException("Andamento inválido para quantização");

        // --- Quantize starts and releases separately. ---
        ArrayList<Placed> placed = new ArrayList<>(notes.size());
        long maxEnd = 1; int offGrid = 0;
        for (ScoreMidiPairer.PairedNote n : notes) {
            long startCell = Math.round(n.startNs / (double) gridNs);
            long endCell = Math.round(n.endNs / (double) gridNs);
            if (Math.abs(n.startNs - startCell * gridNs) > 0.35 * gridNs
                    || Math.abs(n.endNs - endCell * gridNs) > 0.35 * gridNs) offGrid++;
            if (endCell <= startCell) {
                endCell = startCell + 1;
                warnings.add("Nota " + n.pitch + " com duração abaixo da grade; escrito o valor mínimo de 1/16");
            }
            if (startCell < 0) startCell = 0;
            placed.add(new Placed(n, startCell, endCell, n.pitch >= SPLIT_MIDI ? 1 : 2));
            maxEnd = Math.max(maxEnd, endCell);
        }
        if (offGrid > 0)
            warnings.add(offGrid + " nota(s) fora da grade de semicolcheia; quantizadas para a célula mais próxima");

        long measureCount = (maxEnd + CELLS_PER_MEASURE - 1) / CELLS_PER_MEASURE;
        if (measureCount > MAX_MEASURES)
            throw new IOException("Gravação muito longa para uma partitura de " + MAX_MEASURES + " compassos");
        int measures = (int) measureCount;

        // --- Voice allocation per staff: chords first, then lowest free voice. ---
        placed.sort(Comparator.comparingLong((Placed p) -> p.startCell)
                .thenComparingLong(p -> p.endCell).thenComparingInt(p -> p.placed.pitch));
        ArrayList<ArrayList<ArrayList<Slot>>> staffVoices = new ArrayList<>(); // per staff, per local voice
        staffVoices.add(new ArrayList<>()); staffVoices.add(new ArrayList<>());
        for (Placed p : placed) {
            ArrayList<ArrayList<Slot>> voices = staffVoices.get(p.staff - 1);
            int target = -1; boolean asChord = false;
            for (int v = 0; v < voices.size() && target < 0; v++) {
                ArrayList<Slot> voice = voices.get(v);
                Slot last = voice.get(voice.size() - 1);
                if (last.startCell == p.startCell && last.endCell == p.endCell && last.pitch != p.placed.pitch) {
                    target = v; asChord = true;
                }
            }
            if (target < 0) {
                for (int v = 0; v < voices.size() && target < 0; v++) {
                    ArrayList<Slot> voice = voices.get(v);
                    Slot last = voice.get(voice.size() - 1);
                    if (last.endCell <= p.startCell) target = v;
                }
            }
            if (target < 0) {
                if (voices.size() >= MAX_VOICES_PER_STAFF)
                    throw new IOException("Notas sobrepostas demais na pauta " + p.staff
                            + "; o limite desta versão é " + MAX_VOICES_PER_STAFF + " vozes");
                target = voices.size();
                voices.add(new ArrayList<>());
            }
            voices.get(target).add(new Slot(p, asChord));
        }

        // --- Decompose each note into measure slices and writable figures with ties. ---
        ArrayList<ArrayList<ArrayList<Draft>>> work = new ArrayList<>(); // [staff][voice] -> drafts
        for (int s = 0; s < 2; s++) {
            ArrayList<ArrayList<Draft>> perStaff = new ArrayList<>();
            for (int v = 0; v < staffVoices.get(s).size(); v++) perStaff.add(new ArrayList<>());
            work.add(perStaff);
        }
        for (int s = 0; s < 2; s++) {
            for (int v = 0; v < staffVoices.get(s).size(); v++) {
                ArrayList<Draft> drafts = work.get(s).get(v);
                for (Slot slot : staffVoices.get(s).get(v))
                    decompose(slot, drafts, warnings);
            }
        }

        // --- Consolidate voices per measure and fill rests. ---
        ArrayList<MeasurePlan> measurePlans = new ArrayList<>();
        int written = 0;
        for (int m = 0; m < measures; m++) {
            ArrayList<VoicePlan> voicePlans = new ArrayList<>();
            for (int s = 0; s < 2; s++) {
                int voiceBase = s == 0 ? 1 : 5;
                for (int v = 0; v < work.get(s).size(); v++) {
                    ArrayList<Draft> drafts = new ArrayList<>();
                    for (Draft d : work.get(s).get(v)) if (d.measure == m) drafts.add(d);
                    boolean isVoiceOne = v == 0;
                    if (drafts.isEmpty()) {
                        ArrayList<Token> tokens = new ArrayList<>();
                        if (isVoiceOne) tokens.add(new Token(true, 0, CELLS_PER_MEASURE, false, false, false, null));
                        else continue;
                        voicePlans.add(new VoicePlan(s + 1, voiceBase + v, tokens));
                        continue;
                    }
                    drafts.sort(Comparator.comparingLong((Draft d) -> d.pos).thenComparingInt(d -> d.chord ? 1 : 0));
                    ArrayList<Token> tokens = new ArrayList<>();
                    long cursor = 0;
                    for (Draft d : drafts) {
                        if (d.chord) { tokens.add(d.token()); continue; } // stacked; does not advance
                        if (d.pos > cursor) { fillRests(tokens, cursor, d.pos, warnings); cursor = d.pos; }
                        else if (d.pos < cursor)
                            throw new IOException("Sobreposição rítmica não resolvida na pauta " + (s + 1));
                        tokens.add(d.token());
                        cursor += d.cells;
                    }
                    if (cursor < CELLS_PER_MEASURE) fillRests(tokens, cursor, CELLS_PER_MEASURE, warnings);
                    if (cursor > CELLS_PER_MEASURE)
                        throw new IOException("Compasso excedido na pauta " + (s + 1));
                    for (Token t : tokens) if (!t.rest) written++;
                    voicePlans.add(new VoicePlan(s + 1, voiceBase + v, tokens));
                }
                // A staff with no allocated voice at all still shows voice 1 with a whole rest.
                boolean staffPresent = false;
                for (VoicePlan vp : voicePlans) if (vp.staff == s + 1) staffPresent = true;
                if (!staffPresent) {
                    ArrayList<Token> tokens = new ArrayList<>();
                    tokens.add(new Token(true, 0, CELLS_PER_MEASURE, false, false, false, null));
                    voicePlans.add(new VoicePlan(s + 1, voiceBase, tokens));
                }
            }
            voicePlans.sort(Comparator.comparingInt((VoicePlan vp) -> vp.staff).thenComparingInt(vp -> vp.voice));
            measurePlans.add(new MeasurePlan(m, voicePlans));
        }
        return new Plan(measures, measurePlans, warnings, written);
    }

    /** Slice one note across barlines, then split every slice into writable figures. */
    private static void decompose(Slot slot, ArrayList<Draft> drafts, List<String> warnings) {
        long from = slot.startCell, to = slot.endCell;
        boolean firstSlice = true;
        while (from < to) {
            int measure = (int) (from / CELLS_PER_MEASURE);
            long sliceEnd = Math.min(to, (measure + 1L) * CELLS_PER_MEASURE);
            long remaining = sliceEnd - from;
            boolean crossing = sliceEnd < to;
            long cursor = from % CELLS_PER_MEASURE;
            boolean firstFigure = true;
            while (remaining > 0) {
                int figure = -1;
                for (int f : FIGURES)
                    if (f <= remaining && cursor % f == 0 && cursor + f <= CELLS_PER_MEASURE) { figure = f; break; }
                if (figure <= 0) { figure = 1; warnings.add("Duração irregular da nota " + slot.pitch + " escrita em 1/16"); }
                boolean isLastOfSlice = remaining == figure;
                boolean tieStart = crossing && isLastOfSlice;
                boolean tieStop = !firstSlice && firstFigure && isLastOfSlice;
                boolean chordHead = slot.chord && firstSlice && firstFigure;
                drafts.add(new Draft(measure, cursor, figure, slot.placed.placed, chordHead, tieStart, tieStop));
                cursor += figure; remaining -= figure; firstFigure = false;
            }
            firstSlice = false;
            from = sliceEnd;
        }
    }

    /** Decompose the gap [from,to) in grid cells into rest figures. */
    private static void fillRests(List<Token> tokens, long from, long to, List<String> warnings) {
        long pos = from, remaining = to - from;
        while (remaining > 0) {
            int figure = -1;
            for (int f : FIGURES)
                if (f <= remaining && pos % f == 0 && pos + f <= CELLS_PER_MEASURE) { figure = f; break; }
            if (figure <= 0) { figure = 1; warnings.add("Pausa irregular normalizada para 1/16"); }
            tokens.add(new Token(true, 0, figure, false, false, false, null));
            pos += figure; remaining -= figure;
        }
    }

    private static final class Placed {
        final ScoreMidiPairer.PairedNote placed; final long startCell, endCell; final int staff;
        Placed(ScoreMidiPairer.PairedNote n, long s, long e, int st) { placed = n; startCell = s; endCell = e; staff = st; }
    }
    private static final class Slot {
        final Placed placed; final boolean chord; final long startCell, endCell; final int pitch;
        Slot(Placed p, boolean chord) {
            placed = p; this.chord = chord; startCell = p.startCell; endCell = p.endCell; pitch = p.placed.pitch;
        }
    }
    private static final class Draft {
        final int measure; final long pos; final int cells;
        final ScoreMidiPairer.PairedNote source;
        final boolean chord, tieStart, tieStop;
        Draft(int measure, long pos, int cells, ScoreMidiPairer.PairedNote source,
              boolean chord, boolean tieStart, boolean tieStop) {
            this.measure = measure; this.pos = pos; this.cells = cells;
            this.source = source; this.chord = chord; this.tieStart = tieStart; this.tieStop = tieStop;
        }
        Token token() {
            boolean rest = source == null;
            return new Token(rest, rest ? 0 : source.pitch, cells, chord, tieStart, tieStop, source);
        }
    }
}

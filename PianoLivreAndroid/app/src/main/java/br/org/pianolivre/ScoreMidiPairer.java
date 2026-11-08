package br.org.pianolivre;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Z-P1: pairs Note On/Off events from a saved C1 take into notation notes on a
 * single musical timeline. Pure, read-only and deterministic; never edits the
 * source events, the .plc or any storage.
 *
 * Rules (ZAI-001 §3.1):
 * - Note On (0x9n, velocity>0) pairs with the next Note Off (0x8n or 0x9n velocity 0)
 *   of the same channel and pitch, in order.
 * - A repeated Note On while the same key is still held closes the open note as a
 *   synthetic release (safety retrigger), with a legible warning.
 * - An orphan Note Off (no open note) is ignored with a warning.
 * - A note still held at the end of the last recorded segment is closed there as a
 *   synthetic release (recorder interruption/stop semantics), with a warning.
 * - Real time is mapped onto musical time by removing the pauses between recording
 *   segments. Positions in the output are relative to the first musical attack
 *   (first attack = 0), so connection setup time never becomes a rest.
 */
public final class ScoreMidiPairer {
    /** Hard cap shared with the old pipeline; beyond it a take is rejected, never truncated. */
    public static final int MAX_NOTES = 8000;

    /** One paired MIDI note on the musical timeline. Immutable. */
    public static final class PairedNote {
        public final String sourceId;
        public final int pitch, channel, velocity, sourceOrder, sourceEpoch;
        /** Musical timeline positions (ns), relative to the first attack. */
        public final long startNs, endNs;
        /** Raw monotonic clock times inside the recording (ns), for audits. */
        public final long rawStartNs, rawEndNs;
        /** True when the release came from a synthetic safety closure. */
        public final boolean syntheticEnd;
        PairedNote(String id, int pitch, int channel, int velocity, int order, int epoch,
                   long startNs, long endNs, long rawStartNs, long rawEndNs, boolean syntheticEnd) {
            this.sourceId = id; this.pitch = pitch; this.channel = channel; this.velocity = velocity;
            this.sourceOrder = order; this.sourceEpoch = epoch;
            this.startNs = startNs; this.endNs = endNs;
            this.rawStartNs = rawStartNs; this.rawEndNs = rawEndNs;
            this.syntheticEnd = syntheticEnd;
        }
        public long durationNs() { return endNs - startNs; }
    }

    /** Segment table: maps raw monotonic time to the take's musical time. */
    public static final class Timeline {
        private final long[] starts, ends, offsets;
        public final long musicalLengthNs;
        Timeline(List<MidiTakeRecorder.Segment> segments) throws IOException {
            if (segments == null || segments.isEmpty() || segments.size() > 10000)
                throw new IOException("Segmentos de gravação ausentes ou excessivos");
            starts = new long[segments.size()]; ends = new long[segments.size()]; offsets = new long[segments.size()];
            long previous = -1, total = 0;
            for (int i = 0; i < segments.size(); i++) {
                MidiTakeRecorder.Segment s = segments.get(i);
                if (s == null || s.startNs < 0 || s.startNs < previous || s.endNs < s.startNs
                        || s.endNs - s.startNs > MidiTakeRecorder.MAX_DURATION_NS - total)
                    throw new IOException("Segmentos MIDI inválidos");
                starts[i] = s.startNs; ends[i] = s.endNs; offsets[i] = total;
                total += s.endNs - s.startNs; previous = s.endNs;
            }
            musicalLengthNs = total;
        }
        /** Musical time of a raw recording instant; events must live inside a segment. */
        long musicalNs(long rawNs) throws IOException {
            int i = Arrays.binarySearch(starts, rawNs);
            if (i < 0) i = -i - 2;
            if (i >= 0 && rawNs <= ends[i]) return offsets[i] + rawNs - starts[i];
            throw new IOException("Evento fora dos segmentos gravados");
        }
        /** Musical time at the end of the take (used to close still-held notes). */
        public long endNs() { return musicalLengthNs; }
    }

    /** Paired notes plus the legible warnings produced while pairing. */
    public static final class Result {
        public final List<PairedNote> notes;
        public final List<String> warnings;
        public final Timeline timeline;
        Result(List<PairedNote> notes, List<String> warnings, Timeline timeline) {
            this.notes = Collections.unmodifiableList(notes);
            this.warnings = Collections.unmodifiableList(warnings);
            this.timeline = timeline;
        }
    }

    private ScoreMidiPairer() {}

    public static Result pair(List<MidiTakeRecorder.Event> events,
                              List<MidiTakeRecorder.Segment> segments) throws IOException {
        if (events == null || events.isEmpty())
            throw new IOException("Gravação sem eventos MIDI");
        if (events.size() > MidiTakeRecorder.MAX_EVENTS)
            throw new IOException("Gravação excede o limite de eventos");
        Timeline timeline = new Timeline(segments);

        // Stable ordering mirrors MidiTakeRecorder.events(): time, then reception order.
        ArrayList<MidiTakeRecorder.Event> ordered = new ArrayList<>(events);
        ordered.sort((a, b) -> a.timeNs != b.timeNs
                ? Long.compare(a.timeNs, b.timeNs) : Integer.compare(a.order, b.order));

        // First pass: musical time of every event, first attack origin.
        long[] musical = new long[ordered.size()];
        long firstAttackNs = -1;
        for (int i = 0; i < ordered.size(); i++) {
            musical[i] = timeline.musicalNs(ordered.get(i).timeNs);
            MidiTakeRecorder.Event e = ordered.get(i);
            if (firstAttackNs < 0 && e.command() == 0x90 && e.data2 > 0) firstAttackNs = musical[i];
        }
        if (firstAttackNs < 0) throw new IOException("Gravação sem notas (nenhum Note On)");

        // Second pass: pair each Note On with the next matching release.
        ArrayList<String> warnings = new ArrayList<>();
        Map<Integer, Builder> open = new HashMap<>();
        ArrayList<PairedNote> notes = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            MidiTakeRecorder.Event e = ordered.get(i);
            int command = e.command(), channel = e.channel();
            int key = (channel << 7) | e.data1;
            long when = musical[i] - firstAttackNs;
            if (command == 0x90 && e.data2 > 0) {
                Builder held = open.remove(key);
                if (held != null) {
                    // Safety retrigger: close the still-held note here, keep the new attack.
                    warnings.add("Nota " + e.data1 + " do canal " + channel
                            + " reativada antes do encerramento; encerramento sintético aplicado");
                    notes.add(held.close(when, true));
                }
                open.put(key, new Builder(e, when, musical[i]));
            } else if (command == 0x80 || (command == 0x90 && e.data2 == 0)) {
                Builder held = open.remove(key);
                if (held == null) {
                    warnings.add("Note Off órfão ignorado (nota " + e.data1 + ", canal " + channel + ")");
                    continue;
                }
                notes.add(held.close(when, e.generated));
            }
            // Pedal (0xBn) and other controller events are preserved in the .plc but
            // deliberately do not extend written durations in this milestone (ZAI-001 §2.7).
        }
        if (!open.isEmpty()) {
            long end = timeline.endNs() - firstAttackNs;
            List<Integer> keys = new ArrayList<>(open.keySet());
            Collections.sort(keys);
            for (int key : keys) {
                Builder held = open.get(key);
                warnings.add("Nota " + (key & 127) + " do canal " + (key >> 7)
                        + " permaneceu pressionada; encerrada no fim da gravação");
                notes.add(held.close(end, true));
            }
        }
        if (notes.size() > MAX_NOTES)
            throw new IOException("Gravação tem " + notes.size() + " notas; limite para partitura é " + MAX_NOTES);
        notes.sort((a, b) -> a.startNs != b.startNs
                ? Long.compare(a.startNs, b.startNs) : Integer.compare(a.pitch, b.pitch));
        return new Result(notes, warnings, timeline);
    }

    /** Mutable pairing state for one held key. */
    static final class Builder {
        final String sourceId; final int pitch, channel, velocity, order, epoch;
        final long startNs, rawStartNs;
        Builder(MidiTakeRecorder.Event on, long musicalStart, long rawStart) {
            // New stable identity namespace for the Z generator; the order is unique per take.
            this.sourceId = "zg-" + on.order + "-" + on.channel() + "-" + on.data1;
            pitch = on.data1; channel = on.channel(); velocity = on.data2;
            order = on.order; epoch = (int) Math.min(Integer.MAX_VALUE, on.epoch);
            startNs = musicalStart; rawStartNs = rawStart;
        }
        PairedNote close(long musicalEnd, boolean synthetic) {
            long end = Math.max(startNs, musicalEnd);
            return new PairedNote(sourceId, pitch, channel, velocity, order, epoch,
                    startNs, end, rawStartNs, rawStartNs + Math.max(0, end - startNs), synthetic);
        }
    }
}

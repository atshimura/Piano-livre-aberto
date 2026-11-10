package br.org.pianolivre;

import java.io.IOException;

/**
 * Z-P2: serializes the quantized plan into deterministic internal MusicXML for OSMD.
 * One piano part, two always-present staves (treble G2 above, bass F4 below), key of
 * C major, fixed 4/4, divisions 960 per quarter (ZAI-001 §2, §4).
 *
 * - Voice blocks are separated by &lt;backup&gt;, each summing exactly one measure;
 *   chords are stacked right after their head note with &lt;chord/&gt;.
 * - Ties across barlines use &lt;tie&gt; plus &lt;notations&gt;&lt;tied&gt;.
 * - The estimated tempo is written as a metronome direction and &lt;sound tempo&gt; so the
 *   Study playback follows the recognized BPM; a provisional 120 is labelled by the UI.
 * - Output is byte-identical for identical inputs; the title is XML-escaped.
 */
public final class ScoreXmlComposer {
    public static final int MAX_XML_CHARS = 8_000_000;

    private ScoreXmlComposer() {}

    /** Figure name and dot count for a writable length in sixteenth cells. */
    static String typeOf(int cells) throws IOException {
        switch (cells) {
            case 16: return "whole";
            case 12: return "half";
            case 8: return "half";
            case 6: return "quarter";
            case 4: return "quarter";
            case 3: return "eighth";
            case 2: return "eighth";
            case 1: return "16th";
            default: throw new IOException("Figura musical não suportada nesta versão: " + cells + " semicolcheias");
        }
    }
    static boolean dotted(int cells) { return cells == 12 || cells == 6 || cells == 3; }

    /** Natural spelling with sharps for the black keys; enharmonic sophistication is out of scope. */
    static String pitchXml(int pitch) {
        String[] steps = {"C", "C", "D", "D", "E", "F", "F", "G", "G", "A", "A", "B"};
        int cls = pitch % 12, octave = pitch / 12 - 1;
        String step = steps[cls];
        String alter = (cls == 1 || cls == 3 || cls == 6 || cls == 8 || cls == 10) ? "<alter>1</alter>" : "";
        return "<pitch><step>" + step + "</step>" + alter + "<octave>" + octave + "</octave></pitch>";
    }

    static String escape(String s) {
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': b.append("&amp;"); break;
                case '<': b.append("&lt;"); break;
                case '>': b.append("&gt;"); break;
                case '\"': b.append("&quot;"); break;
                case '\'': b.append("&apos;"); break;
                default:
                    if (c >= 0x20 && c != 0x7f) b.append(c);
                    else b.append(' ');
            }
        }
        return b.toString();
    }

    public static String compose(String title, ScoreRhythmQuantizer.Plan plan, double bpm) throws IOException {
        if (title == null || title.trim().isEmpty()) title = "Partitura";
        if (plan == null || plan.measureList.isEmpty()) throw new IOException("Plano musical vazio");
        StringBuilder xml = new StringBuilder(16384);
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><score-partwise version=\"4.0\">");
        String safeTitle = escape(title.trim());
        xml.append("<work><work-title>").append(safeTitle).append("</work-title></work><movement-title>")
           .append(safeTitle).append("</movement-title><identification><creator type=\"composer\">Criação MIDI</creator></identification>");
        xml.append("<part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list>");
        xml.append("<part id=\"P1\">");
        long measureTicks = (long) ScoreRhythmQuantizer.CELLS_PER_MEASURE * ScoreRhythmQuantizer.TICKS_PER_CELL;
        for (ScoreRhythmQuantizer.MeasurePlan measure : plan.measureList) {
            xml.append("<measure number=\"").append(measure.index + 1).append("\">");
            if (measure.index == 0) {
                xml.append("<attributes><divisions>").append(ScoreRhythmQuantizer.DIVISIONS)
                   .append("</divisions><key><fifths>0</fifths></key>")
                   .append("<time><beats>").append(ScoreRhythmQuantizer.BEATS_PER_MEASURE)
                   .append("</beats><beat-type>4</beat-type></time><staves>2</staves>")
                   .append("<clef number=\"1\"><sign>G</sign><line>2</line></clef>")
                   .append("<clef number=\"2\"><sign>F</sign><line>4</line></clef></attributes>");
                long shownBpm = Math.round(Math.max(1, Math.min(400, bpm)));
                xml.append("<direction placement=\"above\"><direction-type><metronome>")
                   .append("<beat-unit>quarter</beat-unit><per-minute>").append(shownBpm)
                   .append("</per-minute></metronome></direction-type><sound tempo=\"").append(shownBpm)
                   .append("\"/></direction>");
            }
            boolean firstVoiceOfMeasure = true;
            for (ScoreRhythmQuantizer.VoicePlan voice : measure.voices) {
                if (!firstVoiceOfMeasure) xml.append("<backup><duration>").append(measureTicks).append("</duration></backup>");
                firstVoiceOfMeasure = false;
                for (ScoreRhythmQuantizer.Token token : voice.tokens) {
                    long ticks = (long) token.cells * ScoreRhythmQuantizer.TICKS_PER_CELL;
                    xml.append("<note>");
                    if (token.rest) {
                        xml.append("<rest/>");
                    } else {
                        if (token.chord) xml.append("<chord/>");
                        xml.append(pitchXml(token.pitch));
                    }
                    xml.append("<duration>").append(ticks).append("</duration>");
                    if (token.tieStop) xml.append("<tie type=\"stop\"/>");
                    if (token.tieStart) xml.append("<tie type=\"start\"/>");
                    xml.append("<voice>").append(voice.voice).append("</voice>");
                    xml.append("<type>").append(typeOf(token.cells)).append("</type>");
                    if (dotted(token.cells)) xml.append("<dot/>");
                    xml.append("<staff>").append(voice.staff).append("</staff>");
                    if (!token.rest && (token.tieStart || token.tieStop
                            || token.staccato || token.slurStart || token.slurStop)) {
                        xml.append("<notations>");
                        if (token.tieStop) xml.append("<tied type=\"stop\"/>");
                        if (token.tieStart) xml.append("<tied type=\"start\"/>");
                        // Expressive slurs are NOT ties: different notes stay
                        // separate attacks. Staccato does not shorten the
                        // rhythmic duration of the written note.
                        if (token.slurStop) xml.append("<slur type=\"stop\" number=\"1\"/>");
                        if (token.slurStart) xml.append("<slur type=\"start\" number=\"1\"/>");
                        if (token.staccato) xml.append("<articulations><staccato/></articulations>");
                        xml.append("</notations>");
                    }
                    xml.append("</note>");
                }
            }
            xml.append("</measure>");
        }
        xml.append("</part></score-partwise>");
        if (xml.length() > MAX_XML_CHARS)
            throw new IOException("Partitura gerada excede o limite de tamanho");
        return xml.toString();
    }
}

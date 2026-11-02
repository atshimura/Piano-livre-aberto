package br.org.pianolivre;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * C2-A: pure, bounded transcription of a saved C1 take into internal MusicXML.
 * MIDI events and segments are read-only. The 120 BPM SMF export clock is NOT
 * the user's selected notation tempo. A staff choice is NOT a physical hand ID.
 */
public final class CreationScoreTranscriber {
    public static final int DIVISIONS = 960;
    public static final int MAX_NOTES = 8000, MAX_MEASURES = 1024, MAX_XML_CHARS = 8_000_000;
    private static final int[] DURATIONS = {5760,3840,2880,1920,1440,960,720,480,360,240,180,120};
    private static final String[] TYPES = {"whole","whole","half","half","quarter","quarter","eighth","eighth","16th","16th","32nd","32nd"};
    private static final boolean[] DOTS = {true,false,true,false,true,false,true,false,true,false,true,false};
    private static final String[] STEPS = {"C","C","D","D","E","F","F","G","G","A","A","B"};
    private static final int[] ALTERS = {0,1,0,1,0,0,1,0,1,0,1,0};

    public enum StaffMode { SUGGESTED, PITCH_THRESHOLD }

    public static final class Options {
        public final int bpm, beats, beatType, grid, splitPitch;
        public final StaffMode staffMode;
        public final Map<String,Integer> staffOverrides;
        public Options(int bpm,int beats,int beatType,int grid,StaffMode mode,int splitPitch,Map<String,Integer> overrides) {
            if(bpm<30||bpm>240||!((beats==2||beats==3||beats==4)&&beatType==4||beats==6&&beatType==8)
                    ||!(grid==8||grid==16||grid==32)||mode==null||splitPitch<0||splitPitch>127)
                throw new IllegalArgumentException("Parâmetros de transcrição inválidos");
            this.bpm=bpm;this.beats=beats;this.beatType=beatType;this.grid=grid;
            this.staffMode=mode;this.splitPitch=splitPitch;
            this.staffOverrides=Collections.unmodifiableMap(new HashMap<>(overrides==null?Collections.emptyMap():overrides));
            for(Map.Entry<String,Integer> e:this.staffOverrides.entrySet())
                if(e.getKey()==null||e.getValue()==null||(e.getValue()!=1&&e.getValue()!=2))
                    throw new IllegalArgumentException("Pauta inválida");
        }
        public static Options defaults(){return new Options(100,4,4,16,StaffMode.SUGGESTED,60,null);}
        int barTicks(){return DIVISIONS*beats*4/beatType;}
        int gridTicks(){return DIVISIONS*4/grid;}
    }

    /** Stable reference to the original MIDI Note On, independent of notation. */
    public static final class WrittenNote {
        public final String sourceId;
        public final int pitch, velocity, channel, staff, voice;
        public final long sourceStartNs, sourceEndNs, startTick, endTick;
        public final boolean syntheticRelease;
        WrittenNote(String id,int pitch,int vel,int ch,int staff,int voice,long rs,long re,long s,long e,boolean synth){
            sourceId=id;this.pitch=pitch;velocity=vel;channel=ch;this.staff=staff;this.voice=voice;
            sourceStartNs=rs;sourceEndNs=re;startTick=s;endTick=e;syntheticRelease=synth;
        }
    }
    public static final class Result {
        public final String xml;
        public final List<WrittenNote> notes;
        public final List<String> warnings;
        public final int measures;
        Result(String xml,List<WrittenNote> notes,List<String> warnings,int measures){
            this.xml=xml;this.notes=Collections.unmodifiableList(new ArrayList<>(notes));
            this.warnings=Collections.unmodifiableList(new ArrayList<>(warnings));this.measures=measures;
        }
    }
    private static final class Attack {
        final String id;final int pitch,velocity,channel;final long rawStart,start;
        long rawEnd,end;boolean synthetic;
        Attack(MidiTakeRecorder.Event e,long start){
            id="c2-"+e.order+"-"+e.epoch+"-"+e.channel()+"-"+e.data1;
            pitch=e.data1;velocity=e.data2;channel=e.channel();rawStart=e.timeNs;this.start=start;
        }
        void close(MidiTakeRecorder.Event e,long mapped){rawEnd=e.timeNs;end=Math.max(start,mapped);synthetic=e.generated;}
    }
    private static final class Note {
        final Attack a; final long start,end;int staff,voice;
        Note(Attack a,long start,long end){this.a=a;this.start=start;this.end=end;}
    }
    private static final class Group {
        final int staff;final long start,end;final List<Note> notes=new ArrayList<>();int voice;
        Group(int staff,long start,long end){this.staff=staff;this.start=start;this.end=end;}
    }
    private static final class Timeline {
        final long[] starts,ends,offsets;final long length;
        Timeline(List<MidiTakeRecorder.Segment> segments)throws IOException{
            if(segments==null||segments.isEmpty()||segments.size()>10000)throw new IOException("Segmentos de gravação ausentes ou excessivos");
            starts=new long[segments.size()];ends=new long[segments.size()];
            offsets=new long[segments.size()];long previous=-1,total=0;
            for(int i=0;i<segments.size();i++){
                MidiTakeRecorder.Segment s=segments.get(i);
                if(s==null||s.startNs<0||s.startNs<previous||s.endNs<s.startNs||s.endNs-s.startNs>MidiTakeRecorder.MAX_DURATION_NS-total)
                    throw new IOException("Segmentos MIDI inválidos");
                starts[i]=s.startNs;ends[i]=s.endNs;offsets[i]=total;
                total+=s.endNs-s.startNs;previous=s.endNs;
            }
            length=total;
        }
        long at(long ns)throws IOException{
            int i=Arrays.binarySearch(starts,ns);
            if(i<0)i=-i-2;
            if(i>=0&&ns<=ends[i])return offsets[i]+ns-starts[i];
            throw new IOException("Evento fora dos segmentos gravados");
        }
    }

    private CreationScoreTranscriber(){}

    /** No writes to events, C1 files, MIDI instruments or local storage. */
    public static Result transcribe(List<MidiTakeRecorder.Event> source,List<MidiTakeRecorder.Segment> segments,
                                    Options options,String title)throws IOException {
        if(source==null||options==null||title==null||source.size()>MidiTakeRecorder.MAX_EVENTS)
            throw new IOException("Fonte MIDI ou parâmetros inválidos");
        Timeline timeline=new Timeline(segments);
        List<MidiTakeRecorder.Event> events=new ArrayList<>(source);
        events.sort(Comparator.comparingLong((MidiTakeRecorder.Event e)->e.timeNs).thenComparingInt(e->e.order));
        Map<Integer,Attack> active=new HashMap<>();List<Attack> attacks=new ArrayList<>();List<String> warnings=new ArrayList<>();
        long previous=-1;
        for(MidiTakeRecorder.Event e:events){
            if(e==null||e.timeNs<previous||e.status<0x80||e.status>0xef||e.data1<0||e.data1>127||e.data2<0||e.data2>127)
                throw new IOException("Evento MIDI inválido");
            previous=e.timeNs;
            long now=timeline.at(e.timeNs);int cmd=e.command(),ch=e.channel(),key=ch*128+e.data1;
            if(cmd==0x90&&e.data2>0){
                Attack before=active.remove(key);
                if(before!=null){before.close(e,now);before.synthetic=true;warnings.add("Rearticulação fechada pelo ataque seguinte: "+before.id);}
                Attack a=new Attack(e,now);active.put(key,a);attacks.add(a);
                if(attacks.size()>MAX_NOTES)throw new IOException("A gravação excede o limite de notas para transcrição");
            }else if(cmd==0x80||cmd==0x90&&e.data2==0){
                Attack a=active.remove(key);
                if(a!=null)a.close(e,now);
                else if(!e.generated)warnings.add("Note Off sem ataque correspondente: evento "+e.order);
            }else if(cmd==0xb0&&(e.data1==120||e.data1==123)){
                for(Integer k:new ArrayList<>(active.keySet()))if(k/128==ch){Attack a=active.remove(k);a.close(e,now);a.synthetic=true;}
            }
        }
        if(attacks.isEmpty())throw new IOException("A gravação não contém notas para transcrever");
        for(Attack a:active.values()){
            a.rawEnd=segments.get(segments.size()-1).endNs;a.end=timeline.length;a.synthetic=true;
            warnings.add("Nota sem liberação encerrada no fim da gravação: "+a.id);
        }
        long zero=attacks.get(0).start;
        for(Attack a:attacks)zero=Math.min(zero,a.start);
        int grid=options.gridTicks();List<Note> notes=new ArrayList<>();long latest=0;
        for(Attack a:attacks){
            long start=quantize(a.start-zero,options.bpm,grid);
            long end=Math.max(start+grid,quantize(a.end-zero,options.bpm,grid));
            Note n=new Note(a,start,end);notes.add(n);latest=Math.max(latest,end);
        }
        int bar=options.barTicks();long count=(latest+bar-1)/bar;
        if(count<1||count>MAX_MEASURES)throw new IOException("Partitura muito longa para a transcrição inicial");
        Set<String> sourceIds=new HashSet<>();for(Note n:notes)sourceIds.add(n.a.id);
        if(!sourceIds.containsAll(options.staffOverrides.keySet()))throw new IOException("Ajuste de pauta aponta para nota inexistente");
        // Heuristic is explicitly a notation suggestion, never physical hand detection.
        // Compact same-channel, same-onset clusters remain together on one staff.
        Map<String,List<Note>> simultaneous=new LinkedHashMap<>();
        for(Note n:notes)simultaneous.computeIfAbsent(n.start+":"+n.a.channel,k->new ArrayList<>()).add(n);
        for(List<Note> cluster:simultaneous.values()){
            int min=127,max=0;long sum=0;
            for(Note n:cluster){min=Math.min(min,n.a.pitch);max=Math.max(max,n.a.pitch);sum+=n.a.pitch;}
            boolean compact=options.staffMode==StaffMode.SUGGESTED&&cluster.size()>1&&max-min<=12;
            int groupStaff=sum>=((long)options.splitPitch*cluster.size())?1:2;
            for(Note n:cluster){Integer forced=options.staffOverrides.get(n.a.id);
                n.staff=forced!=null?forced:compact?groupStaff:n.a.pitch>=options.splitPitch?1:2;
            }
        }
        Map<String,Group> groupMap=new LinkedHashMap<>();
        for(Note n:notes){String key=n.staff+":"+n.start+":"+n.end;
            Group g=groupMap.get(key);if(g==null){g=new Group(n.staff,n.start,n.end);groupMap.put(key,g);}g.notes.add(n);
        }
        List<Group> groups=new ArrayList<>(groupMap.values());
        groups.sort(Comparator.comparingLong((Group g)->g.start).thenComparing((Group g)->-g.end).thenComparingInt(g->g.staff));
        @SuppressWarnings("unchecked")
        List<List<Group>>[] voices=new List[]{new ArrayList<List<Group>>(),new ArrayList<List<Group>>()};
        for(Group g:groups){int staff=g.staff-1;boolean assigned=false;
            for(int i=0;i<voices[staff].size();i++){
                List<Group> voice=voices[staff].get(i);
                if(voice.get(voice.size()-1).end<=g.start){g.voice=i+1;voice.add(g);assigned=true;break;}
            }
            if(!assigned){if(voices[staff].size()>=24)throw new IOException("Muitas vozes simultâneas para transcrever");
                g.voice=voices[staff].size()+1;List<Group> v=new ArrayList<>();v.add(g);voices[staff].add(v);
            }
            for(Note n:g.notes)n.voice=g.voice;
        }
        StringBuilder xml=new StringBuilder(Math.min(1024*1024,(int)(notes.size()*150L+1000L)));
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><score-partwise version=\"4.0\">");
        xml.append("<work><work-title>").append(escape(title)).append("</work-title></work><movement-title>")
           .append(escape(title)).append("</movement-title><identification><creator type=\"composer\">Criação MIDI</creator></identification>")
           .append("<part-list><score-part id=\"P1\"><part-name>Piano</part-name><part-abbreviation>Pno</part-abbreviation><score-instrument id=\"I1\"><instrument-name>Piano</instrument-name></score-instrument></score-part></part-list><part id=\"P1\">");
        for(int m=0;m<count;m++){
            long begin=m*(long)bar,stop=begin+bar;
            xml.append("<measure number=\"").append(m+1).append("\">");
            if(m==0){xml.append("<attributes><divisions>").append(DIVISIONS).append("</divisions><time><beats>")
                .append(options.beats).append("</beats><beat-type>").append(options.beatType)
                .append("</beat-type></time><staves>2</staves><clef number=\"1\"><sign>G</sign><line>2</line></clef>")
                .append("<clef number=\"2\"><sign>F</sign><line>4</line></clef></attributes>")
                .append("<direction placement=\"above\"><direction-type><words>Partitura derivada da gravação MIDI</words></direction-type><sound tempo=\"")
                .append(options.bpm).append("\"/></direction>");}
            boolean first=true;
            for(int staff=1;staff<=2;staff++){
                List<List<Group>> staffVoices=voices[staff-1];
                int vc=Math.max(1,staffVoices.size());
                for(int v=0;v<vc;v++){
                    if(!first)xml.append("<backup><duration>").append(bar).append("</duration></backup>");
                    first=false;long cursor=begin;int voiceId=staff*100+v+1;
                    if(v<staffVoices.size())for(Group g:staffVoices.get(v)){
                        if(g.end<=begin)continue;if(g.start>=stop)break;
                        long s=Math.max(begin,g.start),e=Math.min(stop,g.end);
                        if(s>cursor)emitRest(xml,s-cursor,voiceId,staff);
                        long pos=s;
                        while(pos<e){int len=chooseLength(e-pos);boolean stopTie=pos>g.start,startTie=pos+len<g.end;
                            for(int i=0;i<g.notes.size();i++)emitNote(xml,g.notes.get(i),len,voiceId,i>0,stopTie,startTie);
                            pos+=len;
                        }
                        cursor=e;
                    }
                    if(cursor<stop)emitRest(xml,stop-cursor,voiceId,staff);
                }
            }
            xml.append("</measure>");if(xml.length()>MAX_XML_CHARS)throw new IOException("MusicXML gerado excede o limite");
        }
        xml.append("</part></score-partwise>");
        List<WrittenNote> written=new ArrayList<>();
        for(Note n:notes)written.add(new WrittenNote(n.a.id,n.a.pitch,n.a.velocity,n.a.channel,n.staff,n.voice,
                n.a.rawStart,n.a.rawEnd,n.start,n.end,n.a.synthetic));
        written.sort(Comparator.comparingLong((WrittenNote n)->n.startTick).thenComparing(n->n.sourceId));
        return new Result(xml.toString(),written,warnings,(int)count);
    }
    private static long quantize(long ns,int bpm,int step){
        double ticks=(double)ns*bpm*DIVISIONS/60_000_000_000.0;
        return Math.max(0L,Math.round(ticks/step))*step;
    }
    private static int chooseLength(long remaining){for(int d:DURATIONS)if(d<=remaining)return d;return 120;}
    private static void emitRest(StringBuilder xml,long length,int voice,int staff){
        while(length>0){int d=chooseLength(length),idx=durationIndex(d);
            xml.append("<note><rest/><duration>").append(d).append("</duration><voice>").append(voice)
               .append("</voice><type>").append(TYPES[idx]).append("</type>");
            if(DOTS[idx])xml.append("<dot/>");xml.append("<staff>").append(staff).append("</staff></note>");length-=d;
        }
    }
    private static void emitNote(StringBuilder xml,Note n,int len,int voice,boolean chord,boolean stop,boolean start){
        int pitch=n.a.pitch,idx=durationIndex(len);xml.append("<note");
        xml.append(" dynamics=\"").append(Math.min(127,Math.round(n.a.velocity/0.9f))).append("\">");
        if(chord)xml.append("<chord/>");xml.append("<pitch><step>").append(STEPS[pitch%12]).append("</step>");
        if(ALTERS[pitch%12]!=0)xml.append("<alter>1</alter>");
        xml.append("<octave>").append(pitch/12-1).append("</octave></pitch><duration>").append(len)
           .append("</duration>");
        if(stop)xml.append("<tie type=\"stop\"/>");if(start)xml.append("<tie type=\"start\"/>");
        xml.append("<voice>").append(voice).append("</voice><type>").append(TYPES[idx]).append("</type>");
        if(DOTS[idx])xml.append("<dot/>");xml.append("<staff>").append(n.staff).append("</staff>");
        if(stop||start){xml.append("<notations>");if(stop)xml.append("<tied type=\"stop\"/>");
            if(start)xml.append("<tied type=\"start\"/>");xml.append("</notations>");}
        xml.append("</note>");
    }
    private static int durationIndex(int d){for(int i=0;i<DURATIONS.length;i++)if(DURATIONS[i]==d)return i;throw new IllegalArgumentException("Duração não representável");}
    private static String escape(String s){StringBuilder out=new StringBuilder();for(int i=0;i<s.length();i++){
        char c=s.charAt(i);if(c=='&')out.append("&amp;");else if(c=='<')out.append("&lt;");else if(c=='>')out.append("&gt;");
        else if(c=='\"')out.append("&quot;");else if(c=='\'')out.append("&apos;");else if(c<0x20&&c!='\t'&&c!='\n')out.append(' ');else out.append(c);
    }return out.toString();}
}

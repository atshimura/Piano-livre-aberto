package br.org.pianolivre;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * C2-A: pure, bounded transcription of a saved C1 take into internal MusicXML.
 * MIDI events and segments are read-only. The 120 BPM SMF export clock is NOT
 * the user's selected notation tempo. A staff choice is NOT a physical hand ID.
 *
 * "Notação moderna" (Options.modern): armadura de tom escolhida, grafia
 * enarmônica pelo tom, marcação de metrônomo, pausas reais entre ataques,
 * prolongamento pelo pedal sustain (CC64), tríolos detectados, modo fácil
 * (melodia + baixo), transporte e edição de notas. O MIDI original nunca muda.
 * Gravações salvas antes deste modelo (PLS formato 1 e 2) continuam gerando
 * exatamente a mesma partitura de antes (modern=false).
 */
public final class CreationScoreTranscriber {
    public static final int DIVISIONS = 960;
    /** Modelo de grafia escrita: 0 = histórica por sustenidos (PLS 1/2),
     * 1 = Onda 4 (tonalidade maior por quintas; byte a byte com a 0.9.67),
     * 2 = moderna (escala do tom + acidentes explícitos). */
    public static final int SPELLING_LEGACY = 0, SPELLING_ONDA4 = 1, SPELLING_MODERN = 2;
    public static final int MIN_FIFTHS = -7, MAX_FIFTHS = 7;
    public static final int MAX_NOTES = 8000, MAX_MEASURES = 1024, MAX_XML_CHARS = 8_000_000;
    private static final int[] DURATIONS = {5760,3840,2880,1920,1440,960,720,480,360,240,180,120};
    private static final String[] TYPES = {"whole","whole","half","half","quarter","quarter","eighth","eighth","16th","16th","32nd","32nd"};
    private static final boolean[] DOTS = {true,false,true,false,true,false,true,false,true,false,true,false};
    private static final String[] STEPS = {"C","C","D","D","E","F","F","G","G","A","A","B"};
    private static final int[] ALTERS = {0,1,0,1,0,0,1,0,1,0,1,0};
    /** Duração escrita de uma nota de tríade de três no espaço de um tempo. */
    private static final int TRIPLET_UNIT = DIVISIONS/3;

    public enum StaffMode { SUGGESTED, PITCH_THRESHOLD }
    public enum Regularization { RIGOROUS, BALANCED, FLEXIBLE }

    /** Explicit notation override; never changes Note On/Off in the source .plc. */
    public static final class Timing {
        public final long startTick,endTick;
        public Timing(long start,long end){
            if(start<0||end<=start||start%120!=0||end%120!=0||
                end>(long)MAX_MEASURES*DIVISIONS*4)
                throw new IllegalArgumentException("Posição rítmica inválida");
            startTick=start;endTick=end;
        }
        @Override public boolean equals(Object o){
            if(!(o instanceof Timing))return false;
            Timing t=(Timing)o;return startTick==t.startTick&&endTick==t.endTick;
        }
        @Override public int hashCode(){return Long.hashCode(startTick)*31+Long.hashCode(endTick);}
    }

    public static final class Options {
        public final int bpm,beats,beatType,grid,splitPitch;
        public final StaffMode staffMode;
        public final Regularization regularization;
        public final Map<String,Integer> staffOverrides,handOverrides,voiceOverrides;
        public final Map<String,Timing> rhythmOverrides;
        public final boolean legacyTiming;
        /** Tônica da armadura (-1 = sem armadura) e modo (0 maior, 1 menor). */
        public final int keyTonic,keyMode;
        /** Modelo de grafia escrita (ver constantes SPELLING_*). */
        public final int spellingModel;
        /** Modelo de notação novo: armadura, metrônomo, pausas, pedal, tríolos. */
        public final boolean modern;
        /** Transporte escrito em semitons (o MIDI original permanece intacto). */
        public final int transpose;
        /** Modo fácil: mantém só a nota mais aguda e a mais grave de cada ataque. */
        public final boolean easyMode;
        /** Editor: deslocamento escrito por nota (semitons) e notas apagadas. */
        public final Map<String,Integer> pitchOffsets;
        public final Set<String> removedNotes;
        public Options(int bpm,int beats,int beatType,int grid,StaffMode mode,int splitPitch,
                       Map<String,Integer> staffOverrides){
            this(bpm,beats,beatType,grid,mode,splitPitch,staffOverrides,
                 Regularization.BALANCED,null,null,null,false);
        }
        public Options(int bpm,int beats,int beatType,int grid,StaffMode mode,int splitPitch,
                       Map<String,Integer> staffOverrides,Regularization regularization,
                       Map<String,Integer> hands,Map<String,Integer> voices,Map<String,Timing> rhythms,
                       boolean legacyTiming){
            this(bpm,beats,beatType,grid,mode,splitPitch,staffOverrides,regularization,
                 hands,voices,rhythms,legacyTiming,-1,0,false,0,false,null,null,SPELLING_LEGACY);
        }
        public Options(int bpm,int beats,int beatType,int grid,StaffMode mode,int splitPitch,
                       Map<String,Integer> staffOverrides,Regularization regularization,
                       Map<String,Integer> hands,Map<String,Integer> voices,Map<String,Timing> rhythms,
                       boolean legacyTiming,int keyTonic,int keyMode,boolean modern,int transpose,
                       boolean easyMode,Map<String,Integer> pitchOffsets,Set<String> removedNotes){
            this(bpm,beats,beatType,grid,mode,splitPitch,staffOverrides,regularization,
                 hands,voices,rhythms,legacyTiming,keyTonic,keyMode,modern,transpose,easyMode,
                 pitchOffsets,removedNotes,modern?SPELLING_MODERN:
                 (keyTonic>=0?SPELLING_ONDA4:SPELLING_LEGACY));
        }
        public Options(int bpm,int beats,int beatType,int grid,StaffMode mode,int splitPitch,
                       Map<String,Integer> staffOverrides,Regularization regularization,
                       Map<String,Integer> hands,Map<String,Integer> voices,Map<String,Timing> rhythms,
                       boolean legacyTiming,int keyTonic,int keyMode,boolean modern,int transpose,
                       boolean easyMode,Map<String,Integer> pitchOffsets,Set<String> removedNotes,
                       int spellingModel){
            if(bpm<30||bpm>240||!((beats==2||beats==3||beats==4)&&beatType==4||beats==6&&beatType==8)
                    ||!(grid==8||grid==16||grid==32)||mode==null||splitPitch<0||splitPitch>127||regularization==null)
                throw new IllegalArgumentException("Parâmetros de transcrição inválidos");
            this.bpm=bpm;this.beats=beats;this.beatType=beatType;this.grid=grid;
            staffMode=mode;this.splitPitch=splitPitch;this.regularization=regularization;
            this.legacyTiming=legacyTiming;
            this.keyTonic=keyTonic;this.keyMode=keyMode;this.modern=modern;
            this.transpose=transpose;this.easyMode=easyMode;
            if(spellingModel<SPELLING_LEGACY||spellingModel>SPELLING_MODERN)
                throw new IllegalArgumentException("Modelo de grafia inválido");
            this.spellingModel=spellingModel;
            if(!modern&&spellingModel==SPELLING_MODERN)
                throw new IllegalArgumentException("Grafia moderna exige o modelo atual de notação");
            this.staffOverrides=immutable(staffOverrides);
            handOverrides=immutable(hands);voiceOverrides=immutable(voices);
            rhythmOverrides=Collections.unmodifiableMap(new HashMap<>(rhythms==null?Collections.emptyMap():rhythms));
            this.pitchOffsets=Collections.unmodifiableMap(new HashMap<>(pitchOffsets==null?Collections.emptyMap():pitchOffsets));
            this.removedNotes=Collections.unmodifiableSet(new HashSet<>(removedNotes==null?Collections.emptySet():removedNotes));
            if(keyTonic<-1||keyTonic>11||keyMode<0||keyMode>1||transpose<-11||transpose>11)
                throw new IllegalArgumentException("Tom ou transporte inválido");
            if(this.staffOverrides.size()>MAX_NOTES||handOverrides.size()>MAX_NOTES||
                voiceOverrides.size()>MAX_NOTES||rhythmOverrides.size()>MAX_NOTES||
                this.pitchOffsets.size()>MAX_NOTES||this.removedNotes.size()>MAX_NOTES)
                throw new IllegalArgumentException("Muitos ajustes manuais");
            for(Map.Entry<String,Integer> e:this.staffOverrides.entrySet())
                if(e.getValue()==null||(e.getValue()!=1&&e.getValue()!=2))
                    throw new IllegalArgumentException("Pauta inválida");
            for(Map.Entry<String,Integer> e:handOverrides.entrySet())
                if(e.getValue()==null||(e.getValue()!=1&&e.getValue()!=2))
                    throw new IllegalArgumentException("Mão inválida");
            for(Map.Entry<String,Integer> e:voiceOverrides.entrySet())
                if(e.getValue()==null||e.getValue()<1||e.getValue()>4)
                    throw new IllegalArgumentException("Voz inválida");
            for(Map.Entry<String,Timing> e:rhythmOverrides.entrySet())
                if(e.getKey()==null||e.getKey().isEmpty()||e.getValue()==null)
                    throw new IllegalArgumentException("Ajuste rítmico inválido");
            for(Map.Entry<String,Integer> e:this.pitchOffsets.entrySet())
                if(e.getValue()==null||e.getValue()<-24||e.getValue()>24)
                    throw new IllegalArgumentException("Deslocamento de nota inválido");
            for(String key:allKeys())if(key==null||key.isEmpty()||key.length()>110)
                throw new IllegalArgumentException("ID de nota inválido");
        }
        private static Map<String,Integer> immutable(Map<String,Integer> map){
            return Collections.unmodifiableMap(new HashMap<>(map==null?Collections.emptyMap():map));
        }
        private Set<String> allKeys(){
            Set<String> keys=new HashSet<>(staffOverrides.keySet());keys.addAll(handOverrides.keySet());
            keys.addAll(voiceOverrides.keySet());keys.addAll(rhythmOverrides.keySet());
            keys.addAll(pitchOffsets.keySet());keys.addAll(removedNotes);return keys;
        }
        /** Sidecars generated before Implementation 3 still regenerate with their old rhythm model. */
        public static Options legacy(int bpm,int beats,int beatType,int grid,StaffMode mode,int split,Map<String,Integer> staff){
            return new Options(bpm,beats,beatType,grid,mode,split,staff,
                Regularization.BALANCED,null,null,null,true);
        }
        /** Partitura nova, sem tom definido ainda (grafia cromática por sustenidos). */
        public static Options defaults(){return new Options(100,4,4,16,StaffMode.SUGGESTED,60,null,
            Regularization.BALANCED,null,null,null,false,-1,0,true,0,false,null,null,SPELLING_MODERN);}
        /** Opções equivalentes às partituras salvas pela Onda 4 (0.9.67, formato 3):
         * tonalidade maior pelas quintas e grafia Onda 4 byte a byte. */
        public static Options onda4(int keyFifths,int bpm,int beats,int beatType,int grid,StaffMode mode,
                int split,Map<String,Integer> staff,Regularization regularization,
                Map<String,Integer> hands,Map<String,Integer> voices,Map<String,Timing> rhythms,boolean legacy){
            if(keyFifths<MIN_FIFTHS||keyFifths>MAX_FIFTHS)
                throw new IllegalArgumentException("Tom inválido");
            int tonic=fifthsToTonic(keyFifths);
            return new Options(bpm,beats,beatType,grid,mode,split,staff,regularization,hands,voices,
                rhythms,legacy,tonic,0,false,0,false,null,null,SPELLING_ONDA4);
        }
        private static int fifthsToTonic(int fifths){
            int[] sharp={0,7,2,9,4,11,6,1},flat={0,5,10,3,8,1,6,11};
            return fifths>=0?sharp[fifths]:flat[-fifths];
        }
        int barTicks(){return DIVISIONS*beats*4/beatType;}
        int gridTicks(){return DIVISIONS*4/grid;}
        /** Escrita efetiva de um ataque: deslocamento do editor + transporte. */
        int writtenPitch(String id,int sourcePitch){
            int p=sourcePitch;
            Integer offset=pitchOffsets.get(id);
            if(offset!=null)p+=offset;
            p+=transpose;
            return Math.max(0,Math.min(127,p));
        }
        /** Quintas da armadura no modelo Onda 4 (apenas tonalidades maiores). */
        int onda4Fifths(){
            if(keyTonic<0)return 0;
            return keyMode==0?keyFifthsOf(keyTonic):0;
        }
    }

    /** Stable reference to the original MIDI Note On, independent of notation. */
    public static final class WrittenNote {
        public final String sourceId;
        public final int pitch,velocity,channel,staff,voice,hand;
        /** Altura escrita (editor + transporte); o MIDI original permanece em pitch. */
        public final int writtenPitch;
        public final String rhythmLane;
        public final long sourceStartNs, sourceEndNs, startTick, endTick;
        public final boolean syntheticRelease;
        WrittenNote(String id,int pitch,int vel,int ch,int staff,int voice,int hand,int written,String lane,
                    long rs,long re,long s,long e,boolean synth){
            sourceId=id;this.pitch=pitch;velocity=vel;channel=ch;this.staff=staff;this.voice=voice;
            this.hand=hand;writtenPitch=written;rhythmLane=lane;
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
        final Attack a;long start,end;int staff,voice,hand,voiceHint,written,tupletIndex;String lane;
        boolean tuplet,dropped;double exactStart;
        Note(Attack a,long start,long end){this.a=a;this.start=start;this.end=end;
            written=a.pitch;}
    }
    private static final class Group {
        final int staff;final long start,end;final List<Note> notes=new ArrayList<>();int voice;
        final String lane;
        Group(int staff,long start,long end){this(staff,start,end,"");}
        Group(int staff,long start,long end,String lane){this.staff=staff;this.start=start;this.end=end;this.lane=lane;}
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
        // Pedal sustain por canal: períodos [inicioNs, fimNs] com o pedal pressionado.
        Map<Integer,List<long[]>> pedalPeriods=new HashMap<>();
        Map<Integer,Long> pedalOpen=new HashMap<>();
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
            }else if(cmd==0xb0&&e.data1==64){
                if(options.modern){
                    if(e.data2>=64)pedalOpen.putIfAbsent(ch,now);
                    else{Long opened=pedalOpen.remove(ch);
                        if(opened!=null)period(pedalPeriods,ch,opened,now);}
                }
            }else if(cmd==0xb0&&(e.data1==120||e.data1==121||e.data1==123)){
                if(options.modern){Long opened=pedalOpen.remove(ch);
                    if(opened!=null)period(pedalPeriods,ch,opened,now);}
                for(Integer k:new ArrayList<>(active.keySet()))if(k/128==ch){Attack a=active.remove(k);a.close(e,now);a.synthetic=true;}
            }
        }
        if(options.modern)for(Map.Entry<Integer,Long> open:pedalOpen.entrySet())
            period(pedalPeriods,open.getKey(),open.getValue(),timeline.length);
        if(attacks.isEmpty())throw new IOException("A gravação não contém notas para transcrever");
        if(!options.removedNotes.isEmpty())
            for(Iterator<Attack> it=attacks.iterator();it.hasNext();)
                if(options.removedNotes.contains(it.next().id))it.remove();
        if(attacks.isEmpty())throw new IOException("Todas as notas foram apagadas; resta ao menos uma nota");
        for(Attack a:active.values()){
            a.rawEnd=segments.get(segments.size()-1).endNs;a.end=timeline.length;a.synthetic=true;
            warnings.add("Nota sem liberação encerrada no fim da gravação: "+a.id);
        }
        // O pedal prolonga o som depois que a tecla sobe: a nota dura o que se ouve.
        if(options.modern&&!pedalPeriods.isEmpty())
            for(Attack a:attacks){
                List<long[]> periods=pedalPeriods.get(a.channel);
                if(periods==null)continue;
                long heard=pedalReleaseAt(periods,a.rawEnd);
                if(heard>a.rawEnd)a.end=Math.max(a.end,timeline.at(heard));
            }
        long zero=attacks.get(0).start;
        for(Attack a:attacks)zero=Math.min(zero,a.start);
        int grid=options.gridTicks();List<Note> notes=new ArrayList<>();long latest=0;
        for(Attack a:attacks){
            long start=quantize(a.start-zero,options.bpm,grid);
            if(!options.legacyTiming&&options.regularization!=Regularization.RIGOROUS){
                long raw=quantize(a.start-zero,options.bpm,120);
                long beat=Math.round((double)raw/DIVISIONS)*DIVISIONS;
                long tolerance=options.regularization==Regularization.FLEXIBLE?grid:grid/2;
                if(Math.abs(raw-beat)<=tolerance)start=beat;
            }
            long end=Math.max(start+grid,quantize(a.end-zero,options.bpm,grid));
            Note n=new Note(a,start,end);n.written=options.writtenPitch(a.id,a.pitch);
            n.exactStart=(a.start-zero)*options.bpm*(double)DIVISIONS/60_000_000_000.0;
            notes.add(n);latest=Math.max(latest,end);
        }
        int bar=options.barTicks();long count=(latest+bar-1)/bar;
        if(count<1||count>MAX_MEASURES)throw new IOException("Partitura muito longa para a transcrição inicial");
        Set<String> sourceIds=new HashSet<>();for(Note n:notes)sourceIds.add(n.a.id);
        if(!sourceIds.containsAll(options.staffOverrides.keySet())||
           !sourceIds.containsAll(options.handOverrides.keySet())||
           !sourceIds.containsAll(options.voiceOverrides.keySet())||
           !sourceIds.containsAll(options.rhythmOverrides.keySet()))
            throw new IOException("Ajuste aponta para nota inexistente");
        // Heuristic is explicitly a notation suggestion, never physical hand detection.
        // Compact same-channel, same-onset clusters remain together on one staff.
        Map<String,List<Note>> simultaneous=new LinkedHashMap<>();
        for(Note n:notes)simultaneous.computeIfAbsent(n.start+":"+n.a.channel,k->new ArrayList<>()).add(n);
        for(List<Note> cluster:simultaneous.values()){
            if(options.easyMode&&cluster.size()>2){
                cluster.sort(Comparator.comparingInt((Note n)->n.written));
                for(int i=1;i<cluster.size()-1;i++)cluster.get(i).dropped=true;
            }
            int min=127,max=0;long sum=0;
            for(Note n:cluster){
                if(n.dropped)continue;
                min=Math.min(min,n.written);max=Math.max(max,n.written);sum+=n.written;}
            boolean compact=options.staffMode==StaffMode.SUGGESTED&&cluster.size()>1&&max-min<=12;
            int groupStaff=sum>=((long)options.splitPitch*cluster.size())?1:2;
            for(Note n:cluster){
                if(n.dropped)continue;
                Integer forced=options.staffOverrides.get(n.a.id);
                n.staff=forced!=null?forced:compact?groupStaff:n.written>=options.splitPitch?1:2;
            }
        }
        if(options.easyMode)for(Iterator<Note> it=notes.iterator();it.hasNext();)
            if(it.next().dropped)it.remove();

        // A notation staff is a suggestion, never proof of a physical hand.
        // Explicit hand assignment wins; otherwise distinguish provisional
        // voice groups by notation staff and MIDI channel, without labeling hand.
        for(Note n:notes){
            n.hand=options.handOverrides.getOrDefault(n.a.id,0);
            n.voiceHint=options.voiceOverrides.getOrDefault(n.a.id,1);
            n.lane=n.hand>0?"hand"+n.hand+":channel"+n.a.channel+":voice"+n.voiceHint
                   :"unassigned-staff"+n.staff+":channel"+n.a.channel+":voice"+n.voiceHint;
        }
        if(!options.legacyTiming)interpretAttacks(notes,options,grid);
        detectTriplets(notes,options);
        for(Note n:notes){
            Timing fixed=options.rhythmOverrides.get(n.a.id);
            if(fixed!=null){n.start=fixed.startTick;n.end=fixed.endTick;n.tuplet=false;}
        }
        Map<String,Group> groupMap=new LinkedHashMap<>();
        for(Note n:notes){
            String key=(options.legacyTiming?"":n.lane+":")+n.staff+":"+n.start+":"+n.end;
            Group g=groupMap.get(key);
            if(g==null){g=new Group(n.staff,n.start,n.end,n.lane);groupMap.put(key,g);}
            g.notes.add(n);
        }
        List<Group> groups=new ArrayList<>(groupMap.values());
        groups.sort(Comparator.comparingLong((Group g)->g.start).thenComparing((Group g)->-g.end).thenComparingInt(g->g.staff));
        @SuppressWarnings("unchecked")
        List<List<Group>>[] voices=new List[]{new ArrayList<List<Group>>(),new ArrayList<List<Group>>()};
        if(options.legacyTiming){
            // Preserve exactly the original voice allocation for old PLS1 files.
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
        }else{
            @SuppressWarnings("unchecked")
            Map<String,Integer>[] lanes=new Map[]{new LinkedHashMap<String,Integer>(),new LinkedHashMap<String,Integer>()};
            for(Group g:groups){
                int staff=g.staff-1;Integer voice=lanes[staff].get(g.lane);
                if(voice==null){
                    Set<Integer> occupied=new HashSet<>(lanes[staff].values());
                    int explicit=g.notes.get(0).voiceHint;
                    int requested=explicit>1&&!occupied.contains(explicit)?explicit:1;
                    while(occupied.contains(requested))requested++;
                    if(requested>24)throw new IOException("Muitas vozes simultâneas para transcrever");
                    voice=requested;lanes[staff].put(g.lane,voice);
                    while(voices[staff].size()<voice)voices[staff].add(new ArrayList<Group>());
                }
                g.voice=voice;List<Group> list=voices[staff].get(voice-1);
                if(!list.isEmpty()&&list.get(list.size()-1).end>g.start)
                    throw new IOException("Ritmo sobreposto dentro da mesma voz; atribua vozes diferentes");
                list.add(g);
                for(Note n:g.notes)n.voice=voice;
            }
        }
        // A manual rhythm adjustment must not create an impossible score.
        long latestWritten=0;
        for(Group g:groups)latestWritten=Math.max(latestWritten,g.end);
        count=(latestWritten+bar-1)/bar;
        if(count<1||count>MAX_MEASURES)throw new IOException("Partitura ultrapassa os limites musicais");
        // Grafia enarmônica pela armadura escolhida (por padrão, sustenidos).
        String[] spellSteps=STEPS;int[] spellAlters=ALTERS;boolean[] spellDiatonic=DIATONIC_NONE;
        if(options.spellingModel==SPELLING_MODERN&&options.keyTonic>=0){
            int[][] table=keySpelling(options.keyTonic,options.keyMode);
            spellSteps=new String[12];spellAlters=new int[12];spellDiatonic=new boolean[12];
            for(int pc=0;pc<12;pc++){spellSteps[pc]=(char)table[0][pc]==0?null:String.valueOf((char)table[0][pc]);
                spellAlters[pc]=table[1][pc];spellDiatonic[pc]=table[2][pc]==1;}
        }
        StringBuilder xml=new StringBuilder(Math.min(1024*1024,(int)(notes.size()*150L+1000L)));
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><score-partwise version=\"4.0\">");
        xml.append("<work><work-title>").append(escape(title)).append("</work-title></work><movement-title>")
           .append(escape(title)).append("</movement-title><identification><creator type=\"composer\">Criação MIDI</creator></identification>")
           .append("<part-list><score-part id=\"P1\"><part-name>Piano</part-name><part-abbreviation>Pno</part-abbreviation><score-instrument id=\"I1\"><instrument-name>Piano</instrument-name></score-instrument></score-part></part-list><part id=\"P1\">");
        for(int m=0;m<count;m++){
            long begin=m*(long)bar,stop=begin+bar;
            xml.append("<measure number=\"").append(m+1).append("\">");
            if(m==0){xml.append("<attributes><divisions>").append(DIVISIONS).append("</divisions>");
                if(options.spellingModel==SPELLING_ONDA4){
                    if(options.onda4Fifths()!=0)xml.append("<key><fifths>").append(options.onda4Fifths())
                        .append("</fifths><mode>major</mode></key>");
                }else if(options.keyTonic>=0)xml.append("<key><fifths>").append(keyFifths(options.keyTonic,options.keyMode))
                    .append("</fifths><mode>").append(options.keyMode==0?"major":"minor").append("</mode></key>");
                xml.append("<time><beats>")
                .append(options.beats).append("</beats><beat-type>").append(options.beatType)
                .append("</beat-type></time><staves>2</staves><clef number=\"1\"><sign>G</sign><line>2</line></clef>")
                .append("<clef number=\"2\"><sign>F</sign><line>4</line></clef></attributes>");
                if(options.modern){
                    xml.append("<direction placement=\"above\"><direction-type><metronome parentheses=\"no\"><beat-unit>quarter</beat-unit>");
                    if(options.beatType==8)xml.append("<beat-unit-dot/>");
                    xml.append("<per-minute>").append(options.bpm).append("</per-minute></metronome></direction-type><sound tempo=\"")
                       .append(options.bpm).append("\"/></direction>");
                }else if(options.spellingModel==SPELLING_ONDA4){
                    if(options.onda4Fifths()==0)
                        xml.append("<direction placement=\"above\"><direction-type><words>Partitura derivada da gravação MIDI</words></direction-type><sound tempo=\"")
                           .append(options.bpm).append("\"/></direction>");
                    else
                        xml.append("<direction placement=\"above\"><direction-type><words></words></direction-type><sound tempo=\"")
                           .append(options.bpm).append("\"/></direction>");
                }else{
                    xml.append("<direction placement=\"above\"><direction-type><words>Partitura derivada da gravação MIDI</words></direction-type><sound tempo=\"")
                       .append(options.bpm).append("\"/></direction>");
                }
            }
            Map<String,Integer> accidentals=new HashMap<>();
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
                        while(pos<e){int len=g.notes.get(0).tuplet?TRIPLET_UNIT:chooseLength(e-pos);
                            boolean stopTie=pos>g.start,startTie=pos+len<g.end;
                            for(int i=0;i<g.notes.size();i++)emitNote(xml,g.notes.get(i),len,voiceId,i>0,stopTie,startTie,
                                options.spellingModel,options.onda4Fifths(),spellSteps,spellAlters,spellDiatonic,accidentals);
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
                n.hand,n.written,n.lane,n.a.rawStart,n.a.rawEnd,n.start,n.end,n.a.synthetic));
        written.sort(Comparator.comparingLong((WrittenNote n)->n.startTick).thenComparing(n->n.sourceId));
        return new Result(xml.toString(),written,warnings,(int)count);
    }
    private static void period(Map<Integer,List<long[]>> pedalPeriods,int channel,long from,long to){
        if(to<=from)return;
        pedalPeriods.computeIfAbsent(channel,k->new ArrayList<>()).add(new long[]{from,to});
    }
    /** Fim audível de uma nota solta durante o pedal: o fim do período do pedal. */
    private static long pedalReleaseAt(List<long[]> periods,long rawEnd){
        for(long[] p:periods)if(p[0]<=rawEnd&&rawEnd<p[1])return p[1];
        return rawEnd;
    }
    /** Same-lane onset intervals are primary; Note Off only helps identify long
     * rests or the last note. Simultaneous attacks in a lane remain one chord.
     * Na notação moderna a nota termina quando o som termina; o silêncio até o
     * próximo ataque vira pausa escrita. */
    private static void interpretAttacks(List<Note> notes,Options options,int grid){
        Map<String,Map<Long,List<Note>>> lanes=new LinkedHashMap<>();
        for(Note n:notes)lanes.computeIfAbsent(n.lane,k->new LinkedHashMap<>())
            .computeIfAbsent(n.start,k->new ArrayList<>()).add(n);
        for(Map<Long,List<Note>> lane:lanes.values()){
            List<Long> onsets=new ArrayList<>(lane.keySet());Collections.sort(onsets);
            for(int i=0;i<onsets.size();i++){
                long start=onsets.get(i),next=i+1<onsets.size()?onsets.get(i+1):Long.MAX_VALUE;
                long release=start+grid;
                for(Note n:lane.get(start))release=Math.max(release,n.end);
                long end;
                if(next==Long.MAX_VALUE){
                    end=Math.max(start+grid,release);
                    // Last note of a regular phrase has no following attack.
                    // Infer its written value from the two preceding intervals
                    // when the physical key release was conspicuously short.
                    if(!options.modern&&i>=2){
                        long previousGap=start-onsets.get(i-1);
                        long beforeGap=onsets.get(i-1)-onsets.get(i-2);
                        if(Math.abs(previousGap-beforeGap)<=grid&&release-start<previousGap/2)
                            end=start+previousGap;
                    }
                }else{
                    long gap=next-start;
                    if(options.modern){
                        // Som termina antes do próximo ataque com silêncio de ao
                        // menos um semicolcheia: escreve a pausa que se ouve.
                        long minRest=Math.max(grid,DIVISIONS/4);
                        end=(gap-(release-start)>=minRest)?Math.max(start+grid,release):next;
                    }else{
                        // Only a very long gap after key release suggests a rest,
                        // not automatically a multi-beat sustained note.
                        end=(gap>=4L*DIVISIONS&&release-start<=gap/4)
                            ?Math.min(next,Math.max(start+grid,release)):next;
                    }
                }
                end=Math.max(start+grid,end);
                for(Note n:lane.get(start))n.end=end;
            }
        }
    }
    /** Detecta tríades de três notas iguais dentro de um tempo (3:2) e reescreve
     * as posições para a grade exata de tríolo, usando os instantes reais dos
     * ataques (não quantizados) como evidência. Exigências: início no tempo
     * forte, intervalos iguais e próximos de 1/3 de tempo, nenhum ataque da
     * mesma voz dentro do span. Não se aplica a compassos compostos (6/8),
     * onde três colcheias são agrupamento natural. */
    private static void detectTriplets(List<Note> notes,Options options){
        if(options.legacyTiming||!options.modern||options.beatType!=4)return;
        Map<String,Map<Long,List<Note>>> lanes=new LinkedHashMap<>();
        for(Note n:notes)lanes.computeIfAbsent(n.lane,k->new LinkedHashMap<>())
            .computeIfAbsent(n.start,k->new ArrayList<>()).add(n);
        int beat=DIVISIONS;
        for(Map<Long,List<Note>> lane:lanes.values()){
            List<long[]> onsets=new ArrayList<>();
            for(Map.Entry<Long,List<Note>> group:lane.entrySet())
                onsets.add(new long[]{group.getKey(),(long)Math.round(group.getValue().get(0).exactStart)});
            onsets.sort(Comparator.comparingLong(a->a[1]));
            for(int i=0;i+2<onsets.size();i++){
                long first=onsets.get(i)[0];
                if(first%beat!=0)continue;
                double gapOne=onsets.get(i+1)[1]-onsets.get(i)[1];
                double gapTwo=onsets.get(i+2)[1]-onsets.get(i+1)[1];
                if(Math.abs(gapOne-TRIPLET_UNIT)>50||Math.abs(gapOne-gapTwo)>60)continue;
                if(Math.abs((onsets.get(i+2)[1]-onsets.get(i)[1])-2L*TRIPLET_UNIT)>150)continue;
                if(i+3<onsets.size()&&onsets.get(i+3)[0]<first+beat)continue;
                for(int k=0;k<3;k++){
                    long newStart=first+k*(long)TRIPLET_UNIT;
                    long newEnd=k<2?newStart+TRIPLET_UNIT:first+beat;
                    for(Note n:lane.get(onsets.get(i+k)[0])){n.start=newStart;n.end=newEnd;
                        n.tuplet=true;n.tupletIndex=k;}
                }
                i+=2;
            }
        }
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
    private static void emitNote(StringBuilder xml,Note n,int len,int voice,boolean chord,boolean stop,boolean start,
                                 int spellingModel,int onda4Fifths,String[] steps,int[] alters,boolean[] diatonic,Map<String,Integer> accidentals){
        boolean modern=spellingModel==SPELLING_MODERN;
        int pitch=n.written,idx=n.tuplet?6:durationIndex(len);xml.append("<note");
        xml.append(" dynamics=\"").append(Math.min(127,Math.round(n.a.velocity/0.9f))).append("\">");
        if(chord)xml.append("<chord/>");
        int pc=pitch%12;
        String step;int alter;int octave;
        if(modern){
            alter=alters[pc];step=steps[pc];octave=(pitch-alter)/12-1;
        }else if(spellingModel==SPELLING_ONDA4){
            int[] written=onda4Spelling(pc,onda4Fifths);
            step=STEP_NAMES[LETTER_INDEX[written[0]]];alter=written[1];octave=pitch/12-1+written[2];
        }else{
            step=STEPS[pc];alter=ALTERS[pc];octave=pitch/12-1;
        }
        xml.append("<pitch><step>").append(step).append("</step>");
        if(alter!=0)xml.append("<alter>").append(alter).append("</alter>");
        xml.append("<octave>").append(octave).append("</octave></pitch><duration>").append(len)
           .append("</duration>");
        if(stop)xml.append("<tie type=\"stop\"/>");if(start)xml.append("<tie type=\"start\"/>");
        xml.append("<voice>").append(voice).append("</voice><type>").append(n.tuplet?"eighth":TYPES[idx]).append("</type>");
        if(DOTS[idx])xml.append("<dot/>");
        if(modern){
            String key=voice+":"+step+":"+octave;
            Integer state=accidentals.get(key);
            boolean show=state==null?alter!=keyAlterOf(step,diatonic,alters,steps):state!=alter;
            if(show)xml.append("<accidental>").append(accidentalName(alter)).append("</accidental>");
            accidentals.put(key,alter);
            if(n.tuplet)xml.append("<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>");
        }
        xml.append("<staff>").append(n.staff).append("</staff>");
        boolean tupletMark=modern&&n.tuplet;
        if(stop||start||tupletMark){
            xml.append("<notations>");
            if(stop)xml.append("<tied type=\"stop\"/>");
            if(start)xml.append("<tied type=\"start\"/>");
            if(tupletMark){
                if(n.tupletIndex==0)xml.append("<tuplet type=\"start\"/>");
                if(n.tupletIndex==2)xml.append("<tuplet type=\"stop\"/>");
            }
            xml.append("</notations>");
        }
        xml.append("</note>");
    }
    /** Alteração que a armadura implica para uma letra (0 quando a letra não pertence ao tom). */
    private static int keyAlterOf(String step,boolean[] diatonic,int[] alters,String[] steps){
        for(int pc=0;pc<12;pc++)if(diatonic[pc]&&step.equals(steps[pc]))return alters[pc];
        return 0;
    }
    private static String accidentalName(int alter){
        switch(alter){case -2:return "flat-flat";case -1:return "flat";case 0:return "natural";
            case 1:return "sharp";case 2:return "sharp-sharp";default:return "natural";}
    }
    private static int durationIndex(int d){for(int i=0;i<DURATIONS.length;i++)if(DURATIONS[i]==d)return i;throw new IllegalArgumentException("Duração não representável");}
    private static String escape(String s){StringBuilder out=new StringBuilder();for(int i=0;i<s.length();i++){
        char c=s.charAt(i);
        if(c==38)out.append("&amp;");
        else if(c==60)out.append("&lt;");
        else if(c==62)out.append("&gt;");
        else if(c==34)out.append("&quot;");
        else if(c==39)out.append("&apos;");
        else if(c<0x20&&c!=9&&c!=10)out.append(' ');
        else out.append(c);
    }return out.toString();}

    /** Nome brasileiro da altura escrita, respeitando a armadura escolhida
     * (ex.: Mi♭, Sol♯) e o modelo de grafia. Usado nos rótulos de seleção e
     * dentro da bolinha. */
    public static String displayPitchName(int tonic,int mode,int pitch){
        return displayPitchName(tonic>=0?SPELLING_MODERN:SPELLING_LEGACY,tonic,mode,pitch);
    }
    public static String displayPitchName(int spellingModel,int tonic,int mode,int pitch){
        if(pitch<0||pitch>127)return "";
        if(spellingModel==SPELLING_ONDA4){
            int[] written=onda4Spelling(pitch%12,keyFifthsOf(Math.max(0,tonic)));
            String letter=STEP_NAMES[LETTER_INDEX[written[0]]];
            return brazilianName(letter,written[1]);
        }
        if(spellingModel==SPELLING_LEGACY||tonic<0){
            int pc=pitch%12;
            return brazilianName(STEPS[pc],ALTERS[pc]);
        }
        int[][] table=keySpelling(tonic,mode);
        String[] steps=new String[12];for(int i=0;i<12;i++)steps[i]=String.valueOf((char)table[0][i]);
        return brazilianName(steps[pitch%12],table[1][pitch%12]);
    }
    private static String brazilianName(String letter,int alter){
        int letterIndex=0;
        for(int i=0;i<LETTERS.length;i++)if(LETTERS[i].equals(letter)){letterIndex=i;break;}
        String[] names={"Dó","Ré","Mi","Fá","Sol","Lá","Si"};
        String suffix;
        if(alter==0)suffix="";
        else if(alter==-1)suffix="♭";
        else if(alter==-2)suffix="♭♭";
        else if(alter==1)suffix="♯";
        else if(alter==2)suffix="♯♯";
        else suffix=alter>0?"♯":"♭";
        return names[letterIndex]+suffix;
    }
    private static final boolean[] DIATONIC_NONE=new boolean[12];
    private static final int[] MAJOR_SCALE={0,2,4,5,7,9,11};
    private static final int[] MINOR_SCALE={0,2,3,5,7,8,10};
    private static final String[] LETTERS={"C","D","E","F","G","A","B"};
    private static final int[] LETTER_PCS={0,2,4,5,7,9,11};
    // Modelo Onda 4 (byte a byte com a 0.9.67): armaduras por ciclo de quintas.
    private static final String[] STEP_NAMES={"C","D","E","F","G","A","B"};
    private static final int[] LETTER_INDEX={0,0,1,1,2,3,3,4,4,5,5,6};
    private static final int[] ONDA4_SHARP_PCS={6,1,8,3,10,5,0};
    private static final int[] ONDA4_FLAT_PCS={10,3,8,1,6,11,4};
    private static final int[] ONDA4_SHARP_STEP={11,0,2,2,4,4,5,7,7,9,9,11};
    private static final int[] ONDA4_FLAT_STEP={0,2,2,4,4,5,7,7,9,9,11,0};
    private static final int[] ONDA4_FIFTHS_BY_TONIC={0,-5,2,-3,4,-1,6,1,-4,3,-2,5};
    /** Quintas do tom (maior); usado pelo modelo Onda 4 e pela armadura moderna. */
    static int keyFifthsOf(int tonic){return ONDA4_FIFTHS_BY_TONIC[tonic];}
    /** Grafia escrita [letra, alteração, ajuste de oitava] no modelo Onda 4.
     * B# soa uma oitava abaixo da escrita e Cb uma acima; preservado byte a byte
     * para regenerar partituras salvas pela 0.9.67. */
    private static int[] onda4Spelling(int pitchClass,int fifths){
        int pc=((pitchClass%12)+12)%12;
        if(fifths>0){
            for(int i=0;i<Math.min(fifths,7);i++)if(ONDA4_SHARP_PCS[i]==pc)
                return new int[]{ONDA4_SHARP_STEP[pc],1,pc==0?-1:0};
            return new int[]{pc,0,0};
        }
        if(fifths<0){
            for(int i=0;i<Math.min(-fifths,7);i++)if(ONDA4_FLAT_PCS[i]==pc)
                return new int[]{ONDA4_FLAT_STEP[pc],-1,pc==11?1:0};
            return new int[]{pc,0,0};
        }
        return new int[]{pc,ALTERS[pc],0};
    }
    /** Tabela de grafia por tom: para cada classe de altura, letra, alteração e
     * se a altura pertence à escala do tom (define o acidente natural da armadura). */
    private static int[][] keySpelling(int tonic,int mode){
        int[] scale=mode==0?MAJOR_SCALE:MINOR_SCALE;
        boolean preferSharp=keyFifths(tonic,mode)>=0;
        int letterIndex=letterIndexFor(tonic,preferSharp);
        String[] steps=new String[12];int[] alters=new int[12];boolean[] diatonic=new boolean[12];
        for(int d=0;d<7;d++){
            int semitone=tonic+scale[d];
            int pc=semitone%12;
            int letter=(letterIndex+d)%7;
            steps[pc]=LETTERS[letter];
            int natural=LETTER_PCS[letter];
            int alter=semitone-natural;
            if(alter>6)alter-=12;if(alter<-6)alter+=12;
            alters[pc]=alter;diatonic[pc]=true;
        }
        // Notas cromáticas fora da escala: alteração da nota diatônica vizinha,
        // escolhendo a grafia com menos acidentes (empate: sustenido em tons
        // sustenidos, bemol em tons bemóis).
        for(int pc=0;pc<12;pc++)if(steps[pc]==null){
            int below=(pc+11)%12,above=(pc+1)%12;
            int sharpAlter=steps[below]!=null?alters[below]+1:9;
            int flatAlter=steps[above]!=null?alters[above]-1:9;
            boolean useSharp=preferSharp
                ?Math.abs(sharpAlter)<=Math.abs(flatAlter)
                :Math.abs(sharpAlter)<Math.abs(flatAlter);
            if(useSharp&&sharpAlter<=2){steps[pc]=steps[below];alters[pc]=sharpAlter;}
            else if(flatAlter>=-2){steps[pc]=steps[above];alters[pc]=flatAlter;}
            else{steps[pc]=steps[below];alters[pc]=sharpAlter;}
        }
        return new int[][]{encode(steps),alters,toBools(diatonic)};
    }
    /** Letra inicial da escala: tons com bemóis partem do nome bemol (Ré♭ parte
     * de Ré); tons com sustenidos partem do nome sustenido (Dó♯ parte de Dó). */
    private static int letterIndexFor(int tonic,boolean preferSharp){
        for(int i=0;i<7;i++)if(LETTER_PCS[i]==tonic)return i;
        int natural=preferSharp?(tonic+11)%12:(tonic+1)%12;
        for(int i=0;i<7;i++)if(LETTER_PCS[i]==natural)return i;
        return 0;
    }
    private static int[] encode(String[] steps){
        int[] out=new int[12];
        for(int i=0;i<12;i++)out[i]=steps[i]==null?0:steps[i].charAt(0);
        return out;
    }
    private static int[] toBools(boolean[] array){
        int[] out=new int[12];
        for(int i=0;i<12;i++)out[i]=array[i]?1:0;
        return out;
    }
    private static int letterOf(int tonic){for(int i=0;i<7;i++)if(LETTER_PCS[i]==tonic)return i;
        throw new IllegalArgumentException("Tônica não natural");}
    /** Quintas do tom (positivo sustenidos, negativo bemóis). */
    static int keyFifths(int tonic,int mode){
        int[] major={0,-5,2,-3,4,-1,6,1,-4,3,-2,5};
        return mode==0?major[tonic]:major[(tonic+3)%12];
    }
    /**
     * Sugestão determinística de compasso e BPM a partir dos intervalos entre
     * ataques da gravação. Somente sugestão: o usuário confirma ou corrige.
     * Retorna {beats, beatType, bpm} ou null quando não há dados suficientes.
     */
    public static int[] suggestMeterAndBpm(List<MidiTakeRecorder.Event> source,List<MidiTakeRecorder.Segment> segments){
        try{
            Timeline timeline=new Timeline(segments);
            List<Long> onsets=new ArrayList<>();
            for(MidiTakeRecorder.Event e:source)
                if(e!=null&&e.command()==0x90&&e.data2>0&&e.data1>0)
                    onsets.add(timeline.at(e.timeNs));
            Collections.sort(onsets);
            if(onsets.size()<8)return null;
            List<Long> gaps=new ArrayList<>();
            for(int i=1;i<onsets.size();i++){
                long gap=onsets.get(i)-onsets.get(i-1);
                if(gap>0)gaps.add(gap);
            }
            if(gaps.isEmpty())return null;
            double bestScore=-1,bestBeat=0;
            for(int bpm=40;bpm<=200;bpm++){
                double beat=60_000_000_000.0/bpm,score=0;
                for(long gap:gaps){
                    double units=gap/beat,snapped=snap(units),error=Math.abs(units-snapped);
                    score+=Math.exp(-error*error*18);
                }
                // Preferência por pulso na faixa cantável (batida de semínima).
                if(bpm>=70&&bpm<=180)score*=1.08;
                if(bpm<50||bpm>190)score*=0.9;
                if(score>bestScore){bestScore=score;bestBeat=beat;}
            }
            if(!(bestBeat>0))return null;
            double bestBar=-1;int[] bestMeter={4,4};
            double[][] meters={{4,0},{3,0},{2,0},{1.5,1}};
            for(double[] meter:meters){
                double bar=meter[0]*bestBeat,score=0;
                for(long onset:onsets){
                    double position=(onset/bar)-Math.floor(onset/bar);
                    if(position<0.1||position>0.9)score+=1;
                }
                if(score>bestBar){bestBar=score;bestMeter=meter[1]==1?new int[]{6,8}:new int[]{(int)meter[0],4};}
            }
            int bpm=(int)Math.round(60_000_000_000.0/bestBeat);
            if(bestMeter[1]==8)bpm=(int)Math.round(bpm*2.0/3.0);
            if(bpm<30)bpm=30;if(bpm>240)bpm=240;
            return new int[]{bestMeter[0],bestMeter[1],bpm};
        }catch(Exception e){return null;}
    }
    private static double snap(double units){
        if(units<0.55)return Math.round(units*3)/3.0;
        if(units<0.75)return 0.5;
        if(units<1.25)return 1;
        if(units<1.75)return 1.5;
        if(units<2.5)return 2;
        if(units<3.5)return 3;
        return Math.round(units);
    }
}

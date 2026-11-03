package br.org.pianolivre;

import java.io.IOException;
import java.util.*;

/** In-memory, undoable interpretation. The source MIDI recording is immutable. */
public final class CreationScoreReview {
    private static final int HISTORY_LIMIT=48;
    private final List<MidiTakeRecorder.Event> events;
    private final List<MidiTakeRecorder.Segment> segments;
    private final String title;
    private final ArrayDeque<CreationScoreTranscriber.Options> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private final Set<String> selected=new LinkedHashSet<>();
    private CreationScoreTranscriber.Options options;
    private CreationScoreTranscriber.Result result;
    private boolean dirty;

    public static final class Uniformization {
        final CreationScoreTranscriber.Options before,after;
        public final CreationScoreTranscriber.Result preview;
        public final int noteCount,voiceCount;
        public final String description;
        Uniformization(CreationScoreTranscriber.Options old,CreationScoreTranscriber.Options next,
                       CreationScoreTranscriber.Result result,int count,int voices,String description){
            before=old;after=next;preview=result;noteCount=count;voiceCount=voices;this.description=description;
        }
    }

    public CreationScoreReview(List<MidiTakeRecorder.Event> events,List<MidiTakeRecorder.Segment> segments,
                               String title)throws IOException{
        this(events,segments,title,CreationScoreTranscriber.Options.defaults());
    }
    public CreationScoreReview(List<MidiTakeRecorder.Event> events,List<MidiTakeRecorder.Segment> segments,
                               String title,CreationScoreTranscriber.Options stored)throws IOException{
        if(events==null||segments==null||title==null||stored==null)throw new IOException("Composição ausente");
        this.events=Collections.unmodifiableList(new ArrayList<>(events));
        this.segments=Collections.unmodifiableList(new ArrayList<>(segments));
        this.title=title;options=stored;
        result=CreationScoreTranscriber.transcribe(this.events,this.segments,options,title);
    }
    public synchronized CreationScoreTranscriber.Result result(){return result;}
    public synchronized CreationScoreTranscriber.Options options(){return options;}
    public synchronized boolean isDirty(){return dirty;}
    public synchronized void markSaved(){dirty=false;}
    public synchronized int selectedCount(){return selected.size();}
    public synchronized Set<String> selection(){return Collections.unmodifiableSet(new LinkedHashSet<>(selected));}
    public synchronized boolean canUndo(){return !undo.isEmpty();}
    public synchronized boolean canRedo(){return !redo.isEmpty();}
    private void checkpoint(){
        if(undo.size()>=HISTORY_LIMIT)undo.removeFirst();
        undo.addLast(options);redo.clear();
    }
    private void apply(CreationScoreTranscriber.Options next)throws IOException{
        CreationScoreTranscriber.Result generated=CreationScoreTranscriber.transcribe(events,segments,next,title);
        checkpoint();options=next;result=generated;dirty=true;retainSelection();
    }
    private CreationScoreTranscriber.Options copy(int bpm,int beats,int beatType,int grid,
            CreationScoreTranscriber.StaffMode mode,int split,
            Map<String,Integer> staff,CreationScoreTranscriber.Regularization regular,
            Map<String,Integer> hands,Map<String,Integer> voices,
            Map<String,CreationScoreTranscriber.Timing> rhythm,boolean legacy){
        return new CreationScoreTranscriber.Options(bpm,beats,beatType,grid,mode,split,staff,
            regular,hands,voices,rhythm,legacy);
    }
    private CreationScoreTranscriber.Options with(Map<String,Integer> staff,
            Map<String,Integer> hands,Map<String,Integer> voices,
            Map<String,CreationScoreTranscriber.Timing> rhythm){
        return copy(options.bpm,options.beats,options.beatType,options.grid,
            options.staffMode,options.splitPitch,staff,options.regularization,hands,voices,
            rhythm,options.legacyTiming);
    }
    private void retainSelection(){
        Set<String> valid=new HashSet<>();
        for(CreationScoreTranscriber.WrittenNote n:result.notes)valid.add(n.sourceId);
        selected.retainAll(valid);
    }
    public synchronized void configure(int bpm,int beats,int beatType,int grid)throws IOException{
        configure(bpm,beats,beatType,grid,options.regularization);
    }
    public synchronized void configure(int bpm,int beats,int beatType,int grid,
                                        CreationScoreTranscriber.Regularization regular)throws IOException{
        if(bpm==options.bpm&&beats==options.beats&&beatType==options.beatType&&grid==options.grid
           &&regular==options.regularization&&!options.legacyTiming)return;
        apply(copy(bpm,beats,beatType,grid,options.staffMode,options.splitPitch,
            options.staffOverrides,regular,options.handOverrides,options.voiceOverrides,
            options.rhythmOverrides,false));
    }
    public synchronized void split(CreationScoreTranscriber.StaffMode mode,int pitch)throws IOException{
        if(options.staffMode==mode&&options.splitPitch==pitch)return;
        apply(copy(options.bpm,options.beats,options.beatType,options.grid,
            mode,pitch,options.staffOverrides,options.regularization,options.handOverrides,
            options.voiceOverrides,options.rhythmOverrides,options.legacyTiming));
    }
    public synchronized boolean toggle(String sourceId){
        boolean found=false;
        for(CreationScoreTranscriber.WrittenNote n:result.notes)if(n.sourceId.equals(sourceId)){found=true;break;}
        if(!found)return false;
        if(!selected.add(sourceId))selected.remove(sourceId);
        return true;
    }
    public synchronized void selectChord(String sourceId){
        CreationScoreTranscriber.WrittenNote chosen=null;
        for(CreationScoreTranscriber.WrittenNote n:result.notes)if(n.sourceId.equals(sourceId)){chosen=n;break;}
        if(chosen==null)return;
        for(CreationScoreTranscriber.WrittenNote n:result.notes)
            if(n.startTick==chosen.startTick&&n.staff==chosen.staff&&n.rhythmLane.equals(chosen.rhythmLane))
                selected.add(n.sourceId);
    }
    public synchronized void selectMeasures(int first,int last){
        if(first<1||last<first||last>result.measures)throw new IllegalArgumentException("Intervalo de compassos inválido");
        long bar=options.barTicks();
        for(CreationScoreTranscriber.WrittenNote n:result.notes)
            if(n.startTick<(long)last*bar&&n.endTick>(long)(first-1)*bar)selected.add(n.sourceId);
    }
    public synchronized void clearSelection(){selected.clear();}
    public synchronized void moveSelection(int staff)throws IOException{
        if(staff!=1&&staff!=2)throw new IllegalArgumentException("Pauta inválida");
        if(selected.isEmpty())return;
        Map<String,Integer> updated=new HashMap<>(options.staffOverrides);
        for(String id:selected)updated.put(id,staff);
        if(updated.equals(options.staffOverrides))return;
        apply(with(updated,options.handOverrides,options.voiceOverrides,options.rhythmOverrides));
    }
    /** Physical hand is only assigned explicitly by the user, never from pitch or staff. */
    public synchronized void assignSelection(int hand,int voice)throws IOException{
        if(hand<0||hand>2||voice<0||voice>4)throw new IOException("Mão/voz inválida");
        if(selected.isEmpty())throw new IOException("Selecione notas ou compassos");
        Map<String,Integer> hands=new HashMap<>(options.handOverrides),voices=new HashMap<>(options.voiceOverrides);
        for(String id:selected){
            if(hand==0)hands.remove(id);else hands.put(id,hand);
            if(voice==0)voices.remove(id);else voices.put(id,voice);
        }
        if(hands.equals(options.handOverrides)&&voices.equals(options.voiceOverrides))return;
        apply(with(options.staffOverrides,hands,voices,options.rhythmOverrides));
    }
    public synchronized void restoreSuggestion()throws IOException{
        if(options.staffOverrides.isEmpty())return;
        apply(with(Collections.emptyMap(),options.handOverrides,options.voiceOverrides,options.rhythmOverrides));
    }
    public synchronized void restoreAutomaticRhythm()throws IOException{
        if(options.rhythmOverrides.isEmpty())return;
        apply(with(options.staffOverrides,options.handOverrides,options.voiceOverrides,Collections.emptyMap()));
    }

    /**
     * Read-only planning first: selected onsets are rewritten in consecutive
     * runs of each explicit/provisional rhythmic lane. A partial chord, a
     * collision with an unselected onset or crossing the selected bar boundary
     * fails closed; no other note/voice is silently moved.
     */
    public synchronized Uniformization previewUniformize(int duration,int handFilter,int voiceFilter)throws IOException{
        if(!Arrays.asList(3840,1920,960,480,240,120).contains(duration))
            throw new IOException("Figura não suportada");
        if(handFilter<0||handFilter>2||voiceFilter<0||voiceFilter>24)
            throw new IOException("Filtro de mão/voz inválido");
        if(selected.isEmpty())throw new IOException("Selecione notas ou compassos antes de uniformizar");
        Map<String,List<CreationScoreTranscriber.WrittenNote>> lanes=new LinkedHashMap<>();
        Set<String> targets=new HashSet<>();
        for(CreationScoreTranscriber.WrittenNote n:result.notes){
            lanes.computeIfAbsent(n.rhythmLane,k->new ArrayList<>()).add(n);
            if(selected.contains(n.sourceId)&&(handFilter==0||n.hand==handFilter)
                &&(voiceFilter==0||n.voice==voiceFilter))targets.add(n.sourceId);
        }
        if(targets.isEmpty())throw new IOException("Nenhuma nota da mão/voz escolhida; atribua a mão nas opções do trecho");
        Map<String,CreationScoreTranscriber.Timing> next=new HashMap<>(options.rhythmOverrides);
        int changed=0;Set<String> touchedLanes=new HashSet<>();
        StringBuilder summary=new StringBuilder();
        for(Map.Entry<String,List<CreationScoreTranscriber.WrittenNote>> entry:lanes.entrySet()){
            List<CreationScoreTranscriber.WrittenNote> line=entry.getValue();
            line.sort(Comparator.comparingLong((CreationScoreTranscriber.WrittenNote n)->n.startTick)
                .thenComparing(n->n.sourceId));
            List<List<CreationScoreTranscriber.WrittenNote>> onsets=new ArrayList<>();
            for(CreationScoreTranscriber.WrittenNote n:line){
                if(onsets.isEmpty()||onsets.get(onsets.size()-1).get(0).startTick!=n.startTick)
                    onsets.add(new ArrayList<>());
                onsets.get(onsets.size()-1).add(n);
            }
            for(int i=0;i<onsets.size();){
                List<CreationScoreTranscriber.WrittenNote> group=onsets.get(i);
                boolean any=false,all=true;
                for(CreationScoreTranscriber.WrittenNote n:group){
                    any|=targets.contains(n.sourceId);
                    all&=targets.contains(n.sourceId);
                }
                if(any&&!all)throw new IOException("Selecione todas as notas do acorde antes de uniformizar");
                if(!any){i++;continue;}
                int last=i;
                while(last+1<onsets.size()){
                    boolean chosen=false,partial=false;
                    for(CreationScoreTranscriber.WrittenNote n:onsets.get(last+1))
                        if(targets.contains(n.sourceId))chosen=true;else partial=true;
                    if(chosen&&partial)throw new IOException("Selecione o acorde completo");
                    if(!chosen)break;
                    last++;
                }
                long anchor=group.get(0).startTick;
                if(i>0)for(CreationScoreTranscriber.WrittenNote preceding:onsets.get(i-1))
                    if(preceding.endTick>anchor)
                        throw new IOException("A nota anterior se sobrepõe ao trecho; inclua-a na seleção");
                long finalOld=0;
                for(int k=i;k<=last;k++)
                    for(CreationScoreTranscriber.WrittenNote n:onsets.get(k))
                        finalOld=Math.max(finalOld,n.endTick);
                long boundary=((finalOld-1)/options.barTicks()+1)*options.barTicks();
                long nextUnselected=last+1<onsets.size()?onsets.get(last+1).get(0).startTick:Long.MAX_VALUE;
                for(int k=i;k<=last;k++){
                    long newStart=anchor+(long)(k-i)*duration,newEnd=newStart+duration;
                    if(newEnd>boundary||newEnd>nextUnselected)
                        throw new IOException("A figura não cabe no trecho sem deslocar notas fora da seleção");
                    for(CreationScoreTranscriber.WrittenNote n:onsets.get(k)){
                        next.put(n.sourceId,new CreationScoreTranscriber.Timing(newStart,newEnd));
                        if(summary.length()<650)
                            summary.append("Nota MIDI ").append(n.pitch)
                                .append(" · compasso ").append(1+n.startTick/options.barTicks())
                                .append(" · início ")
                                .append(String.format(Locale.ROOT,"%.2f",n.startTick/960.0))
                                .append(" → ").append(String.format(Locale.ROOT,"%.2f",newStart/960.0))
                                .append(" · duração ")
                                .append(String.format(Locale.ROOT,"%.2f",(n.endTick-n.startTick)/960.0))
                                .append(" → ").append(String.format(Locale.ROOT,"%.2f",duration/960.0))
                                .append(" tempos\\n");
                        changed++;
                    }
                }
                touchedLanes.add(entry.getKey());i=last+1;
            }
        }
        if(changed==0)throw new IOException("Não há notas compatíveis na seleção");
        CreationScoreTranscriber.Options updated=with(options.staffOverrides,options.handOverrides,
                                                        options.voiceOverrides,next);
        CreationScoreTranscriber.Result preview=CreationScoreTranscriber.transcribe(events,segments,updated,title);
        // Validate that no nonselected attack moved in the regenerated score.
        Map<String,CreationScoreTranscriber.WrittenNote> old=new HashMap<>();
        for(CreationScoreTranscriber.WrittenNote n:result.notes)old.put(n.sourceId,n);
        for(CreationScoreTranscriber.WrittenNote n:preview.notes){
            CreationScoreTranscriber.WrittenNote before=old.get(n.sourceId);
            if(before==null)throw new IOException("Identidade de nota alterada");
            if(!targets.contains(n.sourceId)&&
                (before.startTick!=n.startTick||before.endTick!=n.endTick))
                throw new IOException("O ajuste deslocaria uma nota não selecionada");
        }
        return new Uniformization(options,updated,preview,changed,touchedLanes.size(),summary.toString());
    }
    public synchronized void applyUniformize(Uniformization plan)throws IOException{
        if(plan==null||plan.before!=options)
            throw new IOException("A prévia está desatualizada. Refaça a seleção.");
        apply(plan.after);
    }
    public synchronized boolean undo()throws IOException{
        if(undo.isEmpty())return false;
        CreationScoreTranscriber.Options previous=undo.peekLast();
        CreationScoreTranscriber.Result generated=CreationScoreTranscriber.transcribe(events,segments,previous,title);
        undo.removeLast();redo.addLast(options);options=previous;result=generated;
        dirty=true;retainSelection();return true;
    }
    public synchronized boolean redo()throws IOException{
        if(redo.isEmpty())return false;
        CreationScoreTranscriber.Options next=redo.peekLast();
        CreationScoreTranscriber.Result generated=CreationScoreTranscriber.transcribe(events,segments,next,title);
        if(undo.size()>=HISTORY_LIMIT)undo.removeFirst();
        undo.addLast(options);redo.removeLast();options=next;result=generated;
        dirty=true;retainSelection();return true;
    }
}

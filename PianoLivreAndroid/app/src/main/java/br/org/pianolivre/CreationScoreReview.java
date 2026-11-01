package br.org.pianolivre;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** C2-B: in-memory editing session. No persistence and no MIDI source mutation. */
public final class CreationScoreReview {
    private static final int HISTORY_LIMIT=48;
    private static final class Snapshot {
        final int bpm,beats,beatType,grid,split;
        final CreationScoreTranscriber.StaffMode mode;
        final Map<String,Integer> overrides;
        Snapshot(CreationScoreTranscriber.Options o) {
            bpm=o.bpm;beats=o.beats;beatType=o.beatType;grid=o.grid;split=o.splitPitch;mode=o.staffMode;
            overrides=new HashMap<>(o.staffOverrides);
        }
        CreationScoreTranscriber.Options options(){
            return new CreationScoreTranscriber.Options(bpm,beats,beatType,grid,mode,split,overrides);
        }
    }
    private final List<MidiTakeRecorder.Event> events;
    private final List<MidiTakeRecorder.Segment> segments;
    private final String title;
    private final ArrayDeque<Snapshot> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private final Set<String> selected=new LinkedHashSet<>();
    private CreationScoreTranscriber.Options options=CreationScoreTranscriber.Options.defaults();
    private CreationScoreTranscriber.Result result;
    private boolean dirty;
    public CreationScoreReview(List<MidiTakeRecorder.Event> events,List<MidiTakeRecorder.Segment> segments,String title)throws IOException{
        if(events==null||segments==null||title==null)throw new IOException("Composição ausente");
        this.events=Collections.unmodifiableList(new ArrayList<>(events));
        this.segments=Collections.unmodifiableList(new ArrayList<>(segments));
        this.title=title;
        result=CreationScoreTranscriber.transcribe(this.events,this.segments,options,title);
    }
    public synchronized CreationScoreTranscriber.Result result(){return result;}
    public synchronized CreationScoreTranscriber.Options options(){return options;}
    public synchronized boolean isDirty(){return dirty;}
    public synchronized int selectedCount(){return selected.size();}
    public synchronized Set<String> selection(){return Collections.unmodifiableSet(new LinkedHashSet<>(selected));}
    public synchronized boolean canUndo(){return !undo.isEmpty();}
    public synchronized boolean canRedo(){return !redo.isEmpty();}
    private void checkpoint(){
        if(undo.size()>=HISTORY_LIMIT)undo.removeFirst();
        undo.addLast(new Snapshot(options));redo.clear();
    }
    private void apply(CreationScoreTranscriber.Options next)throws IOException{
        // Generate before recording a history mutation, so failure is atomic.
        CreationScoreTranscriber.Result generated=CreationScoreTranscriber.transcribe(events,segments,next,title);
        checkpoint();options=next;result=generated;dirty=true;
        retainSelection();
    }
    private void retainSelection(){
        Set<String> existing=new HashSet<>();
        for(CreationScoreTranscriber.WrittenNote n:result.notes)existing.add(n.sourceId);
        selected.retainAll(existing);
    }
    public synchronized void configure(int bpm,int beats,int beatType,int grid)throws IOException{
        if(bpm==options.bpm&&beats==options.beats&&beatType==options.beatType&&grid==options.grid)return;
        apply(new CreationScoreTranscriber.Options(bpm,beats,beatType,grid,options.staffMode,options.splitPitch,options.staffOverrides));
    }
    public synchronized void split(CreationScoreTranscriber.StaffMode mode,int pitch)throws IOException{
        if(options.staffMode==mode&&options.splitPitch==pitch)return;
        apply(new CreationScoreTranscriber.Options(options.bpm,options.beats,options.beatType,options.grid,mode,pitch,options.staffOverrides));
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
        // Selection is by written onset + current staff; moving remains per original attack.
        for(CreationScoreTranscriber.WrittenNote n:result.notes)
            if(n.startTick==chosen.startTick&&n.staff==chosen.staff)selected.add(n.sourceId);
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
        Map<String,Integer> overrides=new HashMap<>(options.staffOverrides);
        for(String id:selected)overrides.put(id,staff);
        if(overrides.equals(options.staffOverrides))return;
        apply(new CreationScoreTranscriber.Options(options.bpm,options.beats,options.beatType,options.grid,options.staffMode,options.splitPitch,overrides));
    }
    public synchronized void restoreSuggestion()throws IOException{
        if(options.staffOverrides.isEmpty())return;
        apply(new CreationScoreTranscriber.Options(options.bpm,options.beats,options.beatType,options.grid,options.staffMode,options.splitPitch,Collections.emptyMap()));
    }
    public synchronized boolean undo()throws IOException{
        if(undo.isEmpty())return false;
        Snapshot old=undo.peekLast();CreationScoreTranscriber.Result generated=
            CreationScoreTranscriber.transcribe(events,segments,old.options(),title);
        Snapshot recent=new Snapshot(options);undo.removeLast();redo.addLast(recent);
        options=old.options();result=generated;dirty=true;retainSelection();return true;
    }
    public synchronized boolean redo()throws IOException{
        if(redo.isEmpty())return false;
        Snapshot next=redo.peekLast();CreationScoreTranscriber.Result generated=
            CreationScoreTranscriber.transcribe(events,segments,next.options(),title);
        if(undo.size()>=HISTORY_LIMIT)undo.removeFirst();
        undo.addLast(new Snapshot(options));redo.removeLast();options=next.options();result=generated;
        dirty=true;retainSelection();return true;
    }
}

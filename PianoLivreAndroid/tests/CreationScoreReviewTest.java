package br.org.pianolivre;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** C2-B basic JVM checks only; no Android emulator or full suite. */
public final class CreationScoreReviewTest {
    private static int checks;
    private static void ok(boolean passed,String label){checks++;if(!passed)throw new AssertionError(label);}
    private static MidiTakeRecorder.Event event(int status,int pitch,int velocity,long ns,int order){
        return new MidiTakeRecorder.Event(status,pitch,velocity,ns,7,order,false);
    }
    private static CreationScoreReview sample()throws Exception{
        List<MidiTakeRecorder.Event> events=Arrays.asList(
            event(0x90,60,80,0,0),event(0x90,64,80,0,1),event(0x90,48,80,0,2),
            event(0x80,60,0,600_000_000,3),event(0x80,64,0,600_000_000,4),
            event(0x80,48,0,600_000_000,5),event(0x90,62,70,900_000_000,6),
            event(0x80,62,0,1_500_000_000,7));
        return new CreationScoreReview(events,
            Collections.singletonList(new MidiTakeRecorder.Segment(0,2_000_000_000)),"Revisão <C2-B>");
    }
    public static void main(String[] args)throws Exception{
        CreationScoreReview s=sample();
        ok(s.result().notes.size()==4,"all note attacks retained");
        ok(!s.isDirty()&&s.selectedCount()==0&&!s.canUndo(),"initial state is clean");
        String originalXml=s.result().xml;
        String id=s.result().notes.get(0).sourceId;
        ok(s.toggle(id)&&s.selectedCount()==1,"note selected by stable event identity");
        ok(s.toggle(id)&&s.selectedCount()==0,"toggle deselect");
        s.selectChord(id);
        ok(s.selectedCount()==2,"simultaneous chord selection on one staff");
        s.moveSelection(2);
        ok(s.result().notes.stream().filter(n->s.selection().contains(n.sourceId)).allMatch(n->n.staff==2),
                "selected chord moved without changing notes");
        ok(s.result().notes.stream().anyMatch(n->n.sourceId.equals(id)&&n.pitch==60),"pitch preserved after moving staff");
        ok(s.canUndo()&&s.isDirty(),"changed transcription has undo history");
        ok(s.undo(),"undo works");
        ok(s.result().xml.equals(originalXml),"undo returns exact MusicXML");
        ok(s.canRedo(),"redo available after undo");
        ok(s.redo(),"redo works");
        ok(!s.result().xml.equals(originalXml),"redo reapplies override");
        s.configure(120,3,4,8);
        ok(s.options().bpm==120&&s.options().beats==3&&s.options().grid==8,"tempo meter grid updated");
        ok(s.result().xml.contains("<beats>3</beats>"),"MusicXML meter updated");
        ok(s.selection().contains(id),"note selection survives re-quantization");
        s.split(CreationScoreTranscriber.StaffMode.PITCH_THRESHOLD,61);
        ok(s.options().splitPitch==61&&s.options().staffMode==CreationScoreTranscriber.StaffMode.PITCH_THRESHOLD,
            "pitch-threshold mode can be applied");
        s.selectMeasures(1,1);
        ok(s.selectedCount()>=2,"measure-range selection");
        int total=s.result().notes.size();
        s.selectMeasures(1,s.result().measures);
        ok(s.selectedCount()==total,"select all measures without losing note IDs");
        s.moveSelection(1);
        ok(s.result().notes.stream().allMatch(n->n.staff==1),"group move affects all selected notes");
        s.restoreSuggestion();
        ok(s.options().staffOverrides.isEmpty(),"restore clears manual overrides");
        ok(s.result().notes.size()==total,"restore never removes source notes");
        s.clearSelection();
        ok(s.selectedCount()==0,"clear selection");
        boolean rejected=false;
        try{s.selectMeasures(0,2);}catch(IllegalArgumentException ex){rejected=true;}
        ok(rejected,"invalid measure range rejected");
        rejected=false;
        try{s.toggle("unknown");}catch(Exception ex){rejected=true;}
        ok(!rejected&&s.selectedCount()==0,"unknown note cannot corrupt selection");
        rejected=false;
        try{s.moveSelection(3);}catch(IllegalArgumentException ex){rejected=true;}
        ok(rejected,"invalid staff rejected");
        ok(!s.result().xml.isEmpty(),"preview remains available after errors");
        System.out.println("CREATION_C2_B_BASIC_PASS checks="+checks);
    }
}

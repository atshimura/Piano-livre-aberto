package br.org.pianolivre;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** ZAI-008: every silence span in 4/4 is compacted without changing time. */
public final class ZScoreRestOptimizationTest {
    private static int checks=0;
    static void ok(boolean test,String context){
        checks++;if(!test)throw new AssertionError(context);
    }
    private static int greedyOld(int from,int to){
        int n=0,cursor=from;
        while(cursor<to){
            int chosen=1;
            for(int f:ScoreRhythmQuantizer.FIGURES)
                if(f<=to-cursor&&cursor%f==0){chosen=f;break;}
            cursor+=chosen;n++;
        }
        return n;
    }
    private static boolean allowed(int pos,int n){
        switch(n){
            case 16: return pos==0;
            case 12: case 8: case 6: case 4: return pos%4==0;
            case 3: return pos%4<=1;
            case 2: return pos%2==0;
            case 1: return true;
            default: return false;
        }
    }
    private static int optimum(int from,int to){
        if(from==to)return 0;
        int min=Integer.MAX_VALUE/4;
        for(int figure:ScoreRhythmQuantizer.FIGURES)
            if(figure<=to-from&&allowed(from,figure))
                min=Math.min(min,1+optimum(from+figure,to));
        return min;
    }
    private static MidiTakeRecorder.Event note(int pitch,int vel,long when,int order){
        return new MidiTakeRecorder.Event(0x90,pitch,vel,when,7,order,false);
    }
    public static void main(String[] args)throws Exception{
        for(int from=0;from<=16;from++)for(int to=from;to<=16;to++){
            List<Integer> figures=ScoreRhythmQuantizer.restFigures(from,to);
            int cursor=from;
            for(int length:figures){
                ok(allowed(cursor,length),"meter grouping "+from+"-"+to+" @"+cursor+" length="+length);
                cursor+=length;
            }
            ok(cursor==to,"exact rest duration "+from+"-"+to);
            ok(figures.size()<=greedyOld(from,to),"never more signs than old notation "+from+"-"+to);
            ok(figures.size()==optimum(from,to),"fewest allowed signs "+from+"-"+to);
        }
        ok(ScoreRhythmQuantizer.restFigures(0,16).equals(Arrays.asList(16)),
                "one full-measure rest on empty staff");
        ok(ScoreRhythmQuantizer.restFigures(1,4).equals(Arrays.asList(3)),
                "one dotted eighth rest replaces 16th+eighth");
        ok(ScoreRhythmQuantizer.restFigures(4,16).equals(Arrays.asList(12)),
                "one dotted half rest replaces multiple glyphs");
        ok(ScoreRhythmQuantizer.restFigures(2,8).equals(Arrays.asList(2,4)),
                "avoid off-beat dotted quarter crossing beat boundaries");

        // User's S1 excerpt: three real attacks, one real pause.
        // Conserving onsets is required, not just conserving sum of rests.
        long q=550_000_000L;
        List<MidiTakeRecorder.Event> notes=Arrays.asList(
                note(60,75,0,0),note(60,0,q/2,1),
                note(62,75,q,2),note(62,0,q+q/2,3),
                note(64,75,3*q,4),note(64,0,3*q+q/2,5));
        CreationScoreGenerator.Generation generated=CreationScoreGenerator.generate(notes,
                Arrays.asList(new MidiTakeRecorder.Segment(0,4*q)),"Pausas");
        ok(generated.notes.size()==3,"three original MIDI onsets");
        Score score=Score.read(new ByteArrayInputStream(generated.xml.getBytes(StandardCharsets.UTF_8)),
                "rest-compact");
        ok(score.events.size()==3,"no invented or lost attacks");
        ok(score.events.get(0).beat<score.events.get(1).beat
                &&score.events.get(1).beat<score.events.get(2).beat,
                "keep onset order and real gap");
        ok(generated.xml.contains("<rest/>"),"necessary silences still exist");
        ok(!generated.xml.contains("<tie")&&!generated.xml.contains("<slur"),
                "conservative ZAI-007 tie-free contract remains");
        System.out.println("ZScoreRestOptimizationTest: "+checks
                +" checks PASS; every silence span exact and minimal");
    }
}

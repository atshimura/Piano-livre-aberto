package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Notação moderna (PLS formato 3): armadura e grafia por tom, pausas reais,
 * tríolos, modo fácil, transporte, editor de notas e marcação de metrônomo.
 * Focado em JVM; o resto da suíte permanece nos contratos existentes. */
public final class CreationModernNotationTest {
    private static int checks;
    private static void ok(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    private static MidiTakeRecorder.Event e(int cmd,int key,int value,long ns,int order){
        return new MidiTakeRecorder.Event(cmd,key,value,ns,7,order,false);
    }
    private static List<MidiTakeRecorder.Segment> seg(long start,long end){
        return Collections.singletonList(new MidiTakeRecorder.Segment(start,end));
    }
    private static Document parse(CreationScoreTranscriber.Result r)throws Exception{
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        return factory.newDocumentBuilder().parse(
            new ByteArrayInputStream(r.xml.getBytes(StandardCharsets.UTF_8)));
    }
    private static String firstNoteStep(CreationScoreTranscriber.Result result,int noteIndex)throws Exception{
        Document xml=parse(result);
        NodeList notes=xml.getElementsByTagName("note");
        for(int i=0;i<notes.getLength();i++){
            Element note=(Element)notes.item(i);
            if(note.getElementsByTagName("rest").getLength()>0)continue;
            if(noteIndex-->0)continue;
            return note.getElementsByTagName("step").item(0).getTextContent()
                +"|"+(note.getElementsByTagName("alter").getLength()>0
                    ?note.getElementsByTagName("alter").item(0).getTextContent():"0");
        }
        return null;
    }
    private static CreationScoreTranscriber.Options modern(int tonic,int mode){
        return new CreationScoreTranscriber.Options(100,4,4,16,
            CreationScoreTranscriber.StaffMode.SUGGESTED,60,null,
            CreationScoreTranscriber.Regularization.BALANCED,null,null,null,false,
            tonic,mode,true,0,false,null,null);
    }
    public static void main(String[] args)throws Exception {
        List<MidiTakeRecorder.Event> tune=Arrays.asList(
            e(0x90,60,89,0,0),e(0x80,60,0,600_000_000L,1),
            e(0x90,63,70,600_000_000L,2),e(0x80,63,0,1_200_000_000L,3),
            e(0x90,58,70,1_200_000_000L,4),e(0x80,58,0,1_800_000_000L,5));
        List<MidiTakeRecorder.Segment> segments=seg(0,2_000_000_000L);
        CreationScoreTranscriber.Result sharp=read(tune,segments,modern(-1,0));
        ok(sharp.xml.contains("<work-title>"),"valid xml");

        // Mi♭ maior: armadura de 3 bemóis, grafia Mi♭/Si♭ e acidente explícito.
        CreationScoreTranscriber.Result eb=read(tune,segments,modern(3,0));
        ok(eb.xml.contains("<key><fifths>-3</fifths><mode>major</mode></key>"),"Eb major key signature");
        ok(eb.xml.contains("<alter>-1</alter>"),"flats written as alter -1");
        ok(firstNoteStep(eb,0).equals("C|0"),"C stays natural");
        ok(firstNoteStep(eb,1).equals("E|-1"),"D# is spelled Eb in Eb major");
        ok(firstNoteStep(eb,2).equals("B|-1"),"A# is spelled Bb in Eb major");
        // Acidentes da armadura NÃO repetem acidente; cromática fora do tom mostra.
        ok(!eb.xml.contains("<accidental>"),"key signature notes show no repeated accidentals");
        ok(sharp.xml.contains("<accidental>sharp</accidental>"),"chromatic note shows an accidental");
        ok(!eb.xml.contains("<direction-type><words>Partitura derivada"),"old warning removed in modern model");
        ok(eb.xml.contains("<metronome parentheses=\"no\"><beat-unit>quarter</beat-unit><per-minute>100</per-minute></metronome>"),
            "written metronome mark");
        // Fá maior: Ré♯ vira Mi♭ pela armadura de 1 bemol.
        CreationScoreTranscriber.Result f=read(tune,segments,modern(5,0));
        ok(f.xml.contains("<key><fifths>-1</fifths><mode>major</mode></key>"),"F major key signature");
        ok(firstNoteStep(f,1).equals("E|-1"),"D# is spelled Eb in F major");
        // Mi menor (relativa): mesmas alterações da armadura de Sol maior.
        CreationScoreTranscriber.Result em=read(tune,segments,modern(4,1));
        ok(em.xml.contains("<key><fifths>1</fifths><mode>minor</mode></key>"),"E minor key signature");
        ok(firstNoteStep(em,1).equals("D|1"),"D# keeps sharp spelling in E minor");
        // Grafia cromática em tom bemol: Sol♯ escrito como Lá♭.
        List<MidiTakeRecorder.Event> chromatic=Arrays.asList(
            e(0x90,68,80,0,0),e(0x80,68,0,600_000_000L,1));
        CreationScoreTranscriber.Result ab=read(chromatic,segments,modern(8,0));
        ok(firstNoteStep(ab,0).equals("A|-1"),"G# is spelled Ab in Ab major");
        // Nomes brasileiros respeitam a armadura.
        ok(CreationScoreTranscriber.displayPitchName(3,0,63).equals("Mi♭"),"displayPitchName Eb");
        ok(CreationScoreTranscriber.displayPitchName(-1,0,63).equals("Ré♯"),"displayPitchName chromatic default");
        ok(CreationScoreTranscriber.displayPitchName(5,0,70).equals("Si♭"),"displayPitchName Bb in F major");

        // Pausa real: nota curta seguida de silêncio escreve <rest/>.
        List<MidiTakeRecorder.Event> withRest=Arrays.asList(
            e(0x90,60,80,0,0),e(0x80,60,0,600_000_000L,1),
            e(0x90,64,80,4_000_000_000L,2),e(0x80,64,0,4_600_000_000L,3));
        CreationScoreTranscriber.Result rests=read(withRest,seg(0,5_000_000_000L),modern(-1,0));
        ok(rests.xml.contains("<rest/>"),"modern rests are written");
        ok(rests.notes.get(0).endTick==960,"note keeps its own length before the rest");

        // Modo fácil: mantém só a nota mais aguda e a mais grave do ataque.
        List<MidiTakeRecorder.Event> thick=Arrays.asList(
            e(0x90,48,80,0,0),e(0x90,55,80,0,1),e(0x90,60,80,0,2),e(0x90,64,80,0,3),
            e(0x80,48,0,600_000_000L,4),e(0x80,55,0,600_000_000L,5),
            e(0x80,60,0,600_000_000L,6),e(0x80,64,0,600_000_000L,7));
        CreationScoreTranscriber.Result full=read(thick,seg(0,1_000_000_000L),modern(-1,0));
        ok(full.notes.size()==4,"thick chord keeps all notes without easy mode");
        CreationScoreTranscriber.Options easy=new CreationScoreTranscriber.Options(100,4,4,16,
            CreationScoreTranscriber.StaffMode.SUGGESTED,60,null,
            CreationScoreTranscriber.Regularization.BALANCED,null,null,null,false,
            -1,0,true,0,true,null,null);
        CreationScoreTranscriber.Result simple=read(thick,seg(0,1_000_000_000L),easy);
        ok(simple.notes.size()==2,"easy mode keeps melody and bass only");
        ok(simple.notes.get(0).pitch==48&&simple.notes.get(1).pitch==64,"easy mode keeps the outer notes");

        // Transporte para Dó maior a partir de Mi♭ maior (3 semitons abaixo).
        CreationScoreTranscriber.Options transported=new CreationScoreTranscriber.Options(100,4,4,16,
            CreationScoreTranscriber.StaffMode.SUGGESTED,60,null,
            CreationScoreTranscriber.Regularization.BALANCED,null,null,null,false,
            0,0,true,-3,false,null,null);
        CreationScoreTranscriber.Result toC=read(tune,segments,transported);
        // 60(C)→60(C) fica na pauta Sol; 63(Mi♭)→60(C); 58(Si♭)→55(Sol).
        ok(firstNoteStep(toC,0).equals("C|0"),"Eb tonic becomes C natural in C major");
        ok(firstNoteStep(toC,1).equals("A|0"),"D# becomes A natural after transport");
        ok(toC.notes.get(0).pitch==60&&toC.notes.get(0).writtenPitch==57,
            "transport keeps the original MIDI pitch intact");

        // Editor: deslocamento de altura, apagar e restaurar.
        String first="c2-0-7-0-60",second="c2-2-7-0-63";
        CreationScoreTranscriber.Options edited=new CreationScoreTranscriber.Options(100,4,4,16,
            CreationScoreTranscriber.StaffMode.SUGGESTED,60,null,
            CreationScoreTranscriber.Regularization.BALANCED,null,null,null,false,
            -1,0,true,0,false,Collections.singletonMap(first,1),
            Collections.singleton(second));
        CreationScoreTranscriber.Result editedScore=read(tune,segments,edited);
        ok(editedScore.notes.size()==2,"removed note is not written");
        ok(editedScore.notes.get(0).writtenPitch==61&&editedScore.notes.get(0).pitch==60,
            "pitch offset moves only the written pitch");
        // Apagar tudo é rejeitado.
        boolean rejected=false;
        try{read(tune,segments,new CreationScoreTranscriber.Options(100,4,4,16,
            CreationScoreTranscriber.StaffMode.SUGGESTED,60,null,
            CreationScoreTranscriber.Regularization.BALANCED,null,null,null,false,
            -1,0,true,0,false,null,
            new HashSet<>(Arrays.asList(first,second,"c2-4-7-0-58"))));}catch(IOException expected){rejected=true;}
        ok(rejected,"erasing every note is rejected");
        // Deslocamento fora do intervalo é rejeitado.
        boolean badOffset=false;
        try{new CreationScoreTranscriber.Options(100,4,4,16,
            CreationScoreTranscriber.StaffMode.SUGGESTED,60,null,
            CreationScoreTranscriber.Regularization.BALANCED,null,null,null,false,
            -1,0,true,0,false,Collections.singletonMap(first,25),null);}catch(IllegalArgumentException expected){badOffset=true;}
        ok(badOffset,"pitch offset out of range is rejected");

        // Tríolo: três colcheias em um tempo viram tríolo com time-modification.
        // 100 BPM → 1 tempo = 600 ms; tríolo de colcheia = 200 ms por nota.
        List<MidiTakeRecorder.Event> triplet=Arrays.asList(
            e(0x90,60,80,0,0),e(0x80,60,0,200_000_000L,1),
            e(0x90,62,80,200_000_000L,2),e(0x80,62,0,400_000_000L,3),
            e(0x90,64,80,400_000_000L,4),e(0x80,64,0,600_000_000L,5));
        CreationScoreTranscriber.Result tuplet=read(triplet,seg(0,1_000_000_000L),modern(-1,0));
        ok(tuplet.xml.contains("<time-modification><actual-notes>3</actual-notes><normal-notes>2</normal-notes></time-modification>"),
            "triplet time modification written");
        ok(tuplet.xml.contains("<tuplet type=\"start\"/>"),"tuplet numeral written");
        ok(tuplet.notes.get(0).startTick==0&&tuplet.notes.get(1).startTick==320
           &&tuplet.notes.get(2).startTick==640,"triplet grid rewritten exactly");
        ok(tuplet.notes.get(2).endTick==960,"triplet fills exactly one beat");
        // Colcheias normais não viram tríolo.
        List<MidiTakeRecorder.Event> straight=Arrays.asList(
            e(0x90,60,80,0,0),e(0x80,60,0,300_000_000L,1),
            e(0x90,62,80,300_000_000L,2),e(0x80,62,0,600_000_000L,3),
            e(0x90,64,80,600_000_000L,4),e(0x80,64,0,900_000_000L,5));
        CreationScoreTranscriber.Result plain=read(straight,seg(0,1_000_000_000L),modern(-1,0));
        ok(!plain.xml.contains("time-modification"),"straight eighths stay straight");

        // Sugestão de compasso/BPM: sequência constante de 500 ms → 120 BPM.
        List<MidiTakeRecorder.Event> steady=new ArrayList<>();
        for(int i=0;i<16;i++){
            steady.add(e(0x90,60+(i%5),80,i*500_000_000L,i*2));
            steady.add(e(0x80,60+(i%5),0,i*500_000_000L+200_000_000L,i*2+1));
        }
        int[] suggestion=CreationScoreTranscriber.suggestMeterAndBpm(steady,seg(0,9_000_000_000L));
        ok(suggestion!=null,"meter suggestion produced");
        ok(suggestion[2]==120,"steady 500 ms gaps suggest 120 BPM");
        int[] empty=CreationScoreTranscriber.suggestMeterAndBpm(
            Collections.singletonList(e(0x90,60,80,0,0)),seg(0,1_000_000_000L));
        ok(empty==null,"too few attacks produce no suggestion");

        // PLS formato 3: ida e volta preserva o modelo moderno inteiro.
        java.io.File root=java.nio.file.Files.createTempDirectory("pls3").toFile();
        try{
            CreationProjectStore projects=new CreationProjectStore(root);
            CreationProjectStore.Project project=projects.save(
                "01234567-89ab-cdef-0123-456789abcdef","Tríolos no Mi♭",0,1_000L,2_000_000_000L,
                tune,segments);
            CreationScoreTranscriber.Options saved=new CreationScoreTranscriber.Options(100,4,4,16,
                CreationScoreTranscriber.StaffMode.SUGGESTED,60,null,
                CreationScoreTranscriber.Regularization.BALANCED,null,null,null,false,
                3,0,true,0,true,Collections.singletonMap(first,1),Collections.singleton(second));
            CreationScoreStore store=new CreationScoreStore(root);
            String xml=CreationScoreTranscriber.transcribe(tune,segments,saved,"Tríolos no Mi♭").xml;
            store.save(project.id,0,saved,xml);
            CreationScoreStore.Saved back=store.read(project.id);
            ok(back.xml.equals(xml)&&back.options.keyTonic==3&&back.options.keyMode==0
               &&back.options.modern&&back.options.transpose==0&&back.options.easyMode
               &&back.options.pitchOffsets.get(first)==1&&back.options.removedNotes.contains(second),
                "PLS format 3 roundtrip preserves the modern model");
            // Partitura do formato 2 continua sendo regenerada exatamente.
            CreationScoreTranscriber.Options legacyOptions=CreationScoreTranscriber.Options.legacy(
                100,4,4,16,CreationScoreTranscriber.StaffMode.SUGGESTED,60,null);
            String legacyXml=CreationScoreTranscriber.transcribe(tune,segments,legacyOptions,"Tríolos no Mi♭").xml;
            ok(!legacyXml.contains("<key>")&&legacyXml.contains("Partitura derivada da gravação MIDI"),
                "legacy model keeps the exact old output");
            ok(!legacyXml.contains("<accidental>")&&legacyXml.contains("<step>A</step><alter>1</alter>"),
                "legacy spelling unchanged (sharps, no accidental elements)");
        }finally{
            for(java.io.File file:root.listFiles())if(file.isDirectory())
                for(java.io.File inner:file.listFiles())inner.delete();
            else file.delete();
            root.delete();
        }

        // Compatibilidade Onda 4: partitura salva pela 0.9.67 (formato 3,
        // quintas da armadura) continua legível e regenera byte a byte.
        java.io.File root3=java.nio.file.Files.createTempDirectory("pls3-onda4").toFile();
        try{
            CreationProjectStore projects=new CreationProjectStore(root3);
            CreationProjectStore.Project project=projects.save(
                "89abcdef-0123-4567-89ab-cdef01234567","Onda quatro",0,1_000L,2_000_000_000L,
                tune,segments);
            CreationScoreTranscriber.Options onda=CreationScoreTranscriber.Options.onda4(-3,
                100,4,4,16,CreationScoreTranscriber.StaffMode.SUGGESTED,60,null,
                CreationScoreTranscriber.Regularization.BALANCED,null,null,null,false);
            String xml=CreationScoreTranscriber.transcribe(tune,segments,onda,"Onda quatro").xml;
            ok(xml.contains("<key><fifths>-3</fifths><mode>major</mode></key>")
               &&xml.contains("<words></words>")
               &&!xml.contains("<accidental>")
               &&firstNoteStep(read(tune,segments,onda),1).equals("E|-1"),
                "onda 4 model writes Eb major like 0.9.67");
            java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream data=new java.io.DataOutputStream(buffer);
            data.writeInt(0x504c5331);data.writeInt(3);
            data.writeUTF(project.id);data.writeInt(project.profileId);
            data.writeUTF(CreationScoreStore.fingerprint(project));data.writeUTF("Onda quatro");
            data.writeInt(onda.bpm);data.writeInt(onda.beats);data.writeInt(onda.beatType);
            data.writeInt(onda.grid);data.writeInt(onda.staffMode.ordinal());data.writeInt(onda.splitPitch);
            data.writeInt(0);
            data.writeBoolean(onda.legacyTiming);data.writeInt(onda.regularization.ordinal());
            data.writeInt(0);data.writeInt(0);
            data.writeInt(0);
            data.writeInt(-3);
            data.writeInt(xml.getBytes(StandardCharsets.UTF_8).length);
            data.write(xml.getBytes(StandardCharsets.UTF_8));
            data.flush();
            byte[] body=buffer.toByteArray();
            java.io.ByteArrayOutputStream withHash=new java.io.ByteArrayOutputStream();
            withHash.write(body);
            withHash.write(java.security.MessageDigest.getInstance("SHA-256").digest(body));
            CreationScoreStore.Saved legacy=CreationScoreStore.verified(withHash.toByteArray(),project);
            ok(legacy.xml.equals(xml)&&legacy.options.spellingModel==CreationScoreTranscriber.SPELLING_ONDA4
               &&legacy.options.keyTonic==3&&legacy.options.keyMode==0&&!legacy.options.modern,
                "PLS format 3 from 0.9.67 stays readable and byte-exact");
        }finally{
            for(java.io.File file:root3.listFiles())if(file.isDirectory())
                for(java.io.File inner:file.listFiles())inner.delete();
            else file.delete();
            root3.delete();
        }
        System.out.println("CREATION_MODERN_NOTATION_PASS checks="+checks);
    }
    private static CreationScoreTranscriber.Result read(List<MidiTakeRecorder.Event> events,
            List<MidiTakeRecorder.Segment> segments,CreationScoreTranscriber.Options options)throws Exception{
        CreationScoreTranscriber.Result result=CreationScoreTranscriber.transcribe(events,segments,options,"Teste");
        parse(result);
        return result;
    }
}

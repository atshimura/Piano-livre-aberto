package br.org.pianolivre;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * C2-B modal, temporary MusicXML preview. Does not persist or modify a C1 take.
 * Head-tap mapping in OSMD is not yet stable; the accessible note list provides
 * the same source-note selection, with chord and measure-range shortcuts.
 */
final class CreationScorePreviewScreen extends LinearLayout {
    private final MainActivity activity;
    private final CreationProjectStore.Project project;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final LinearLayout contents;
    private final ScrollView scroll;
    private Dialog window;
    private NotationView notation;
    private volatile boolean closed,busy;
    private int generation,offset;
    private boolean splitView;
    private CreationScoreReview review;
    private TextView selectionCount;

    static void open(MainActivity activity,CreationProjectStore.Project project) {
        if(project.trash||project.recoverable||project.events.isEmpty()||project.profileId!=activity.user){
            activity.message("Finalize e salve uma composição válida antes de gerar a partitura.");return;
        }
        CreationScorePreviewScreen view=new CreationScorePreviewScreen(activity,project);
        Dialog d=new Dialog(activity);
        view.window=d;d.setContentView(view);
        Window w=d.getWindow();
        if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);}
        d.setOnDismissListener(x->view.shutdown());
        d.show();
        if(d.getWindow()!=null)d.getWindow().setLayout(-1,-1);
        view.generateInitial();
    }
    private CreationScorePreviewScreen(MainActivity a,CreationProjectStore.Project p){
        super(a);activity=a;project=p;setOrientation(VERTICAL);setBackgroundColor(0xffF7F9FC);
        LinearLayout header=horizontal();header.setPadding(dp(12),dp(5),dp(12),dp(5));header.setBackgroundColor(0xff101C31);
        Button close=button("← Voltar",false,()->confirmClose());close.setTextColor(Color.WHITE);
        close.setBackgroundColor(0xff243955);
        header.addView(close,new LayoutParams(dp(110),dp(48)));
        TextView name=label("Transformar em partitura",18,true,Color.WHITE);
        header.addView(name,new LayoutParams(0,dp(56),1));
        addView(header,new LayoutParams(-1,dp(58)));
        contents=vertical();contents.setPadding(dp(14),dp(12),dp(14),dp(24));
        scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(contents);
        addView(scroll,new LayoutParams(-1,0,1));
        addText("Preparando a gravação…",16,true,12);
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout vertical(){LinearLayout v=new LinearLayout(activity);v.setOrientation(VERTICAL);return v;}
    private LinearLayout horizontal(){LinearLayout v=new LinearLayout(activity);v.setOrientation(HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView label(String s,int size,boolean bold,int color){
        TextView t=new TextView(activity);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);return t;
    }
    private void attach(LinearLayout parent,View v,int height,int bottom){
        LayoutParams lp=new LayoutParams(-1,height<0?height:dp(height));lp.bottomMargin=dp(bottom);parent.addView(v,lp);
    }
    private void addText(String s,int size,boolean bold,int margin){
        attach(contents,label(s,size,bold,bold?0xff172A44:0xff57708A),-2,margin);
    }
    private LinearLayout card(){
        LinearLayout c=vertical();c.setPadding(dp(12),dp(11),dp(12),dp(11));
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1),0xffDDE7F2);c.setBackground(bg);return c;
    }
    private Button button(String s,boolean primary,Runnable run){
        Button b=new Button(activity);b.setText(s);b.setTextSize(13);b.setAllCaps(false);
        b.setTextColor(primary?Color.WHITE:0xff172A44);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(primary?0xff286BCB:0xffEEF4FA);bg.setCornerRadius(dp(10));b.setBackground(bg);
        b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));
        b.setOnClickListener(v->{if(!busy&&!closed)run.run();});
        return b;
    }
    private void actions(LinearLayout parent,Button left,Button right){
        LinearLayout row=horizontal();
        LayoutParams one=new LayoutParams(0,dp(48),1);one.rightMargin=dp(8);
        row.addView(left,one);row.addView(right,new LayoutParams(0,dp(48),1));
        attach(parent,row,48,8);
    }
    private void shutdown(){
        closed=true;generation++;if(notation!=null){notation.dispose();notation=null;}
        worker.shutdownNow();
    }
    private void confirmClose(){
        if(review!=null&&review.isDirty()){
            new AlertDialog.Builder(activity).setTitle("Descartar ajustes?")
                .setMessage("A prévia é temporária. Os ajustes não serão salvos, mas a gravação MIDI original continuará intacta.")
                .setPositiveButton("Descartar",(d,w)->window.dismiss())
                .setNegativeButton("Continuar",null).show();
        }else window.dismiss();
    }
    private void generateInitial(){
        final int id=++generation;busy=true;
        worker.execute(()->{
            try{
                CreationScoreReview session=new CreationScoreReview(project.events,project.segments,project.title);
                post(()->{if(closed||id!=generation)return;review=session;busy=false;render(false);});
            }catch(Exception ex){post(()->failure(id,ex));}
        });
    }
    private void failure(int id,Exception ex){
        if(closed||id!=generation)return;busy=false;clear();
        addText("Não foi possível gerar a partitura: "+ex.getMessage(),14,false,12);
        attach(contents,button("Fechar prévia",true,()->window.dismiss()),48,0);
    }
    private void clear(){if(notation!=null){notation.dispose();notation=null;}contents.removeAllViews();}
    private void render(boolean split){
        if(review==null||closed)return;
        splitView=split;final int id=++generation;busy=true;
        clear();addText("Preparando a partitura MusicXML…",14,false,14);
        worker.execute(()->{
            try{
                CreationScoreTranscriber.Result r=review.result();
                Score score=Score.read(new ByteArrayInputStream(r.xml.getBytes(StandardCharsets.UTF_8)),"criacao-c2.musicxml");
                post(()->{if(closed||id!=generation)return;busy=false;paint(score,split);});
            }catch(Exception ex){post(()->failure(id,ex));}
        });
    }
    private void paint(Score score,boolean split){
        clear();CreationScoreTranscriber.Options o=review.options();
        CreationScoreTranscriber.Result r=review.result();
        addText(project.title,21,true,3);
        addText(r.measures+" compassos · "+o.beats+"/"+o.beatType+" · "+o.bpm+" BPM · 1/"+o.grid,13,false,9);
        LinearLayout sheet=card();sheet.setPadding(dp(4),dp(4),dp(4),dp(4));
        NotationView view=new NotationView(activity,new NotationView.Listener(){
            public void onRendered(){}
            public void onError(String text){if(!closed)activity.message("Problema ao desenhar partitura: "+text);}
        });
        notation=view;attach(sheet,view,split?218:310,0);
        attach(contents,sheet,split?238:330,11);
        view.post(()->{if(!closed&&notation==view)view.loadScore(score,0,true);});
        addText("Prévia da escrita musical. O MIDI original não foi modificado.",12,false,9);
        if(!r.warnings.isEmpty())addText(r.warnings.size()+" avisos de interpretação MIDI — consulte a gravação original em caso de dúvida.",12,false,8);
        if(split)splitControls();
        else{
            actions(contents,button("Ajustar ritmo",false,this::editRhythm),
                    button("Divisão entre pautas",true,()->render(true)));
            actions(contents,button("Voltar à composição",false,this::confirmClose),
                    button("Salvar partitura — C2-C",false,()->{}));
            View save=contents.getChildAt(contents.getChildCount()-1);
            if(save instanceof LinearLayout)((LinearLayout)save).getChildAt(1).setEnabled(false);
            addText("Salvar e abrir no Estudar serão implementados no C2-C; esta revisão não é persistida.",12,false,3);
        }
    }
    private void editRhythm(){
        CreationScoreTranscriber.Options o=review.options();
        LinearLayout content=vertical();content.setPadding(dp(16),dp(6),dp(16),0);
        EditText tempo=new EditText(activity);tempo.setSingleLine(true);
        tempo.setInputType(InputType.TYPE_CLASS_NUMBER);tempo.setText(String.valueOf(o.bpm));
        attach(content,label("Andamento (30 a 240 BPM)",14,true,0xff172A44),-2,4);attach(content,tempo,48,10);
        final String[] meters={"2/4","3/4","4/4","6/8"};
        Spinner meter=new Spinner(activity);meter.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,meters));
        meter.setSelection(o.beats==6?3:o.beats==4?2:o.beats==3?1:0);
        attach(content,label("Compasso",14,true,0xff172A44),-2,4);attach(content,meter,48,10);
        final String[] grids={"1/8","1/16","1/32"};
        Spinner grid=new Spinner(activity);grid.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,grids));
        grid.setSelection(o.grid==8?0:o.grid==16?1:2);
        attach(content,label("Precisão rítmica",14,true,0xff172A44),-2,4);attach(content,grid,48,6);
        new AlertDialog.Builder(activity).setTitle("Ajustar partitura").setView(content)
            .setPositiveButton("Atualizar",(d,w)->{
                try{
                    int bpm=Integer.parseInt(tempo.getText().toString().trim());
                    int m=meter.getSelectedItemPosition(),beats=m==3?6:m+2,beatType=m==3?8:4;
                    int step=new int[]{8,16,32}[grid.getSelectedItemPosition()];
                    mutate(()->review.configure(bpm,beats,beatType,step),false);
                }catch(Exception ex){activity.message("Andamento inválido: "+ex.getMessage());}
            }).setNegativeButton("Cancelar",null).show();
    }
    private interface Change {void execute()throws Exception;}
    private void mutate(Change action,boolean remainSplit){
        if(busy||closed)return;
        busy=true;final int id=++generation;clear();addText("Recalculando interpretação…",14,false,9);
        worker.execute(()->{
            try{action.execute();post(()->{if(closed||id!=generation)return;busy=false;render(remainSplit);});}
            catch(Exception ex){post(()->{if(closed||id!=generation)return;busy=false;
                activity.message("Ajuste não aplicado: "+ex.getMessage());render(remainSplit);});}
        });
    }
    private void splitControls(){
        CreationScoreTranscriber.Options o=review.options();
        LinearLayout settings=card();attach(settings,label("Divisão sugerida",16,true,0xff172A44),-2,7);
        Spinner mode=new Spinner(activity);mode.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,
            new String[]{"Sugestão musical","Separação por altura"}));
        mode.setSelection(o.staffMode==CreationScoreTranscriber.StaffMode.SUGGESTED?0:1);
        attach(settings,mode,48,7);
        TextView note=label("Nota-limite MIDI: "+o.splitPitch,13,false,0xff57708A);
        attach(settings,note,-2,3);
        SeekBar threshold=new SeekBar(activity);threshold.setMax(24);threshold.setProgress(Math.min(24,Math.max(0,o.splitPitch-48)));
        threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int value,boolean user){note.setText("Nota-limite MIDI: "+(48+value));}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        attach(settings,threshold,48,7);
        attach(settings,button("Aplicar distribuição",false,()->{
            CreationScoreTranscriber.StaffMode choice=mode.getSelectedItemPosition()==0?
                CreationScoreTranscriber.StaffMode.SUGGESTED:CreationScoreTranscriber.StaffMode.PITCH_THRESHOLD;
            int split=48+threshold.getProgress();mutate(()->review.split(choice,split),true);
        }),48,0);attach(contents,settings,-2,10);

        LinearLayout list=card();
        attach(list,label("Selecionar notas e acordes",16,true,0xff172A44),-2,4);
        selectionCount=label(review.selectedCount()+" selecionada(s)",13,false,0xff57708A);
        attach(list,selectionCount,-2,9);
        List<CreationScoreTranscriber.WrittenNote> notes=review.result().notes;
        int max=Math.min(notes.size(),offset+12);
        for(int i=offset;i<max;i++){
            CreationScoreTranscriber.WrittenNote n=notes.get(i);
            CheckBox select=new CheckBox(activity);
            select.setText(noteLabel(n,o));select.setTextSize(13);select.setTextColor(0xff172A44);
            select.setChecked(review.selection().contains(n.sourceId));
            select.setOnCheckedChangeListener((v,checked)->{
                review.toggle(n.sourceId);selectionCount.setText(review.selectedCount()+" selecionada(s)");
            });attach(list,select,48,1);
            Button chord=button("Marcar acorde desta nota",false,()->{
                review.selectChord(n.sourceId);render(true);
            });chord.setTextSize(11);attach(list,chord,40,5);
        }
        Button previous=button("← Anteriores",false,()->{offset=Math.max(0,offset-12);render(true);});
        Button next=button("Próximas →",false,()->{offset+=12;render(true);});
        previous.setEnabled(offset>0);next.setEnabled(max<notes.size());
        actions(list,previous,next);
        attach(list,button("Selecionar intervalo de compassos",false,this::chooseMeasures),48,7);
        attach(list,button("Desmarcar seleção",false,()->{review.clearSelection();render(true);}),48,0);
        attach(contents,list,-2,9);
        actions(contents,
            button("Pauta superior (Sol)",true,()->mutate(()->review.moveSelection(1),true)),
            button("Pauta inferior (Fá)",false,()->mutate(()->review.moveSelection(2),true)));
        Button undo=button("Desfazer",false,()->mutate(()->review.undo(),true));
        Button redo=button("Refazer",false,()->mutate(()->review.redo(),true));
        undo.setEnabled(review.canUndo());redo.setEnabled(review.canRedo());
        actions(contents,undo,redo);
        actions(contents,
            button("Restaurar sugestão",false,()->new AlertDialog.Builder(activity)
                .setTitle("Restaurar distribuição?").setMessage("Remover todos os ajustes manuais das pautas? A gravação original será preservada.")
                .setPositiveButton("Restaurar",(d,w)->mutate(()->review.restoreSuggestion(),true))
                .setNegativeButton("Cancelar",null).show()),
            button("Concluir ajustes",true,()->render(false)));
        addText("A seleção de notas é acessível pela lista; o toque direto no cabeçote da pauta está previsto para refinamento posterior.",12,false,0);
    }
    private String noteLabel(CreationScoreTranscriber.WrittenNote n,CreationScoreTranscriber.Options o){
        String[] names={"Dó","Dó♯","Ré","Ré♯","Mi","Fá","Fá♯","Sol","Sol♯","Lá","Lá♯","Si"};
        return names[n.pitch%12]+(n.pitch/12-1)+" · compasso "+(1+n.startTick/o.barTicks())+" · pauta "+n.staff+" · voz "+n.voice;
    }
    private void chooseMeasures(){
        LinearLayout panel=vertical();panel.setPadding(dp(18),dp(7),dp(18),0);
        EditText from=new EditText(activity);from.setInputType(InputType.TYPE_CLASS_NUMBER);from.setText("1");
        EditText to=new EditText(activity);to.setInputType(InputType.TYPE_CLASS_NUMBER);
        to.setText(String.valueOf(review.result().measures));
        attach(panel,label("Do compasso",14,true,0xff172A44),-2,4);attach(panel,from,48,7);
        attach(panel,label("Até o compasso (inclusive)",14,true,0xff172A44),-2,4);attach(panel,to,48,7);
        new AlertDialog.Builder(activity).setTitle("Selecionar compassos").setView(panel)
            .setPositiveButton("Marcar",(d,w)->{
                try{review.selectMeasures(Integer.parseInt(from.getText().toString().trim()),
                    Integer.parseInt(to.getText().toString().trim()));render(true);}
                catch(Exception ex){activity.message("Intervalo inválido: "+ex.getMessage());}
            }).setNegativeButton("Cancelar",null).show();
    }
}

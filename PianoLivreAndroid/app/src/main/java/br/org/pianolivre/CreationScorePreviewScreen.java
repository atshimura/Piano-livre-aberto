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
 * OSMD head taps are matched conservatively to original MIDI attack IDs.
 * Ambiguous graphical unisons are selected through the accessible note list.
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
    private boolean saved;
    private TextView selectionCount;
    private final java.util.Map<String,CheckBox> noteBoxes=new java.util.HashMap<>();
    private boolean refreshingSelection;
    private void syncSelection(){
        if(review==null)return;
        if(selectionCount!=null)selectionCount.setText(review.selectedCount()+" selecionada(s)");
        if(notation!=null)notation.setCreationSelected(review.selection());
        refreshingSelection=true;
        try{for(java.util.Map.Entry<String,CheckBox> e:noteBoxes.entrySet())
             e.getValue().setChecked(review.selection().contains(e.getKey()));}
        finally{refreshingSelection=false;}
    }
    private void tapGraphicalNote(String sourceId){
        if(closed||busy||review==null||!review.toggle(sourceId))return;
        if(!splitView){render(true);return;}
        syncSelection();
    }

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
        d.setOnDismissListener(x->{
            view.shutdown();
            if(activity.screens!=null&&activity.screens.creation!=null
                &&activity.screens.creation.getVisibility()==View.VISIBLE)
                activity.screens.creation.showDetails(project.id);
        });
        d.setOnKeyListener((dialog,key,event)->{
            if(key==android.view.KeyEvent.KEYCODE_BACK){
                if(event.getAction()==android.view.KeyEvent.ACTION_UP){
                    if(view.busy)d.dismiss();else view.confirmClose();
                }
                return true;
            }
            return false;
        });
        d.show();
        if(d.getWindow()!=null)d.getWindow().setLayout(-1,-1);
        view.generateInitial();
    }
    private CreationScorePreviewScreen(MainActivity a,CreationProjectStore.Project p){
        super(a);activity=a;project=p;setOrientation(VERTICAL);setBackgroundColor(0xffF7F9FC);
        LinearLayout header=horizontal();header.setPadding(dp(12),dp(5),dp(12),dp(5));header.setBackgroundColor(0xff101C31);
        Button close=button("← Voltar",false,()->confirmClose());
        close.setOnClickListener(v->{if(closed)return;if(busy)window.dismiss();else confirmClose();});
        close.setTextColor(Color.WHITE);
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
        Button b=new Button(activity);b.setText(s);b.setContentDescription(s);b.setTextSize(13);b.setAllCaps(false);
        b.setTextColor(primary?Color.WHITE:0xff172A44);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(primary?0xff286BCB:0xffEEF4FA);bg.setCornerRadius(dp(10));b.setBackground(bg);
        b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));
        b.setOnClickListener(v->{if(!busy&&!closed)run.run();});
        return b;
    }

 // Adaptive rows reserve real text size and respect the dialog's measured width.
 private void actions(LinearLayout parent,Button... buttons){
  LinearLayout group=vertical();group.setTag("creation-score-responsive-actions");attach(parent,group,-2,8);
  final int[] prev={-1};
  java.util.function.IntConsumer reflow=width->{
   int gap=dp(8);float scale=getResources().getConfiguration().fontScale;
   int cell=dp(Math.max(140,Math.round(140*scale)));
   int cols=width<=0?1:Math.max(1,Math.min(buttons.length,(width+gap)/(cell+gap)));
   if(cols==prev[0])return;
   for(Button button:buttons)if(button.getParent() instanceof android.view.ViewGroup)
    ((android.view.ViewGroup)button.getParent()).removeView(button);
   prev[0]=cols;group.removeAllViews();
   for(int start=0;start<buttons.length;start+=cols){
    LinearLayout row=horizontal();LayoutParams rp=new LayoutParams(-1,dp(56));
    rp.bottomMargin=dp(start+cols<buttons.length?6:0);group.addView(row,rp);
    int end=Math.min(buttons.length,start+cols);
    for(int i=start;i<end;i++){
     Button button=buttons[i];button.setSingleLine(false);button.setMaxLines(2);
     LayoutParams cp=new LayoutParams(0,-1,1);cp.rightMargin=dp(i<end-1?8:0);row.addView(button,cp);
    }
   }
  };
  reflow.accept(0);
  group.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
   if(r>l)group.post(()->{if(group.getWidth()>0)reflow.accept(group.getWidth());});
  });
 }

 private void responsiveFields(LinearLayout parent,String[] labels,View[] fields){
  if(labels.length!=fields.length)throw new IllegalArgumentException("Field count mismatch");
  LinearLayout group=vertical();group.setTag("creation-score-responsive-fields");attach(parent,group,-2,8);
  final int[] prev={-1};
  java.util.function.IntConsumer reflow=width->{
   int gap=dp(8);float scale=getResources().getConfiguration().fontScale;
   int cell=dp(Math.max(168,Math.round(168*scale)));
   int cols=width<=0?1:Math.max(1,Math.min(fields.length,(width+gap)/(cell+gap)));
   if(cols==prev[0])return;
   for(View field:fields)if(field.getParent() instanceof android.view.ViewGroup)
    ((android.view.ViewGroup)field.getParent()).removeView(field);
   prev[0]=cols;group.removeAllViews();
   for(int start=0;start<fields.length;start+=cols){
    LinearLayout row=horizontal();row.setGravity(Gravity.TOP);
    LayoutParams rp=new LayoutParams(-1,-2);rp.bottomMargin=dp(8);group.addView(row,rp);
    int end=Math.min(fields.length,start+cols);
    for(int i=start;i<end;i++){
     LinearLayout field=vertical();TextView title=label(labels[i],14,true,0xff172A44);title.setMaxLines(2);
     attach(field,title,-2,4);attach(field,fields[i],48,0);
     LayoutParams cp=new LayoutParams(0,-2,1);cp.rightMargin=dp(i<end-1?8:0);row.addView(field,cp);
    }
   }
  };
  reflow.accept(0);
  group.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
   if(r>l)group.post(()->{if(group.getWidth()>0)reflow.accept(group.getWidth());});
  });
 }
    private void shutdown(){
        closed=true;generation++;if(notation!=null){notation.dispose();notation=null;}
        worker.shutdownNow();
    }
    private void confirmClose(){
        if(review!=null&&review.isDirty()){
            new AlertDialog.Builder(activity).setTitle("Descartar ajustes?")
                .setMessage("Os últimos ajustes ainda não foram salvos. A partitura salva anteriormente e a gravação original serão mantidas.")
                .setPositiveButton("Descartar",(d,w)->window.dismiss())
                .setNegativeButton("Continuar",null).show();
        }else window.dismiss();
    }
    private void generateInitial(){
        final int id=++generation;busy=true;
        worker.execute(()->{
            try{
                CreationScoreStore storage=new CreationScoreStore(activity.getFilesDir());
                CreationScoreStore.Saved prior=null;String recoverWarning="";
                if(storage.has(project.id))try{prior=storage.read(project.id);}
                catch(Exception oldError){recoverWarning="A partitura salva não pôde ser validada. Uma nova prévia será gerada, sem apagar a gravação nem a partitura anterior."; }
                CreationScoreReview session=new CreationScoreReview(project.events,project.segments,
                   project.title,prior==null?CreationScoreTranscriber.Options.defaults():prior.options);
                final boolean previouslySaved=prior!=null;
                final String warning=recoverWarning;
                post(()->{if(closed||id!=generation)return;review=session;saved=previouslySaved;busy=false;render(false);
                   if(!warning.isEmpty())activity.message(warning);
                });
            }catch(Exception ex){post(()->failure(id,ex));}
        });
    }
    private void failure(int id,Exception ex){
        if(closed||id!=generation)return;busy=false;clear();
        addText("Não foi possível gerar a partitura: "+ex.getMessage(),14,false,12);
        attach(contents,button("Fechar prévia",true,()->window.dismiss()),48,0);
    }
    private void clear(){noteBoxes.clear();selectionCount=null;if(notation!=null){notation.dispose();notation=null;}contents.removeAllViews();}
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
        addText(r.measures+" compassos · "+o.beats+"/"+o.beatType+" · "+o.bpm+" BPM · 1/"+o.grid
            +" · Ritmo "+(o.legacyTiming?"legado":o.regularization==CreationScoreTranscriber.Regularization.RIGOROUS?"rigoroso":
                o.regularization==CreationScoreTranscriber.Regularization.FLEXIBLE?"flexível":"equilibrado"),13,false,9);
        LinearLayout sheet=card();sheet.setPadding(dp(4),dp(4),dp(4),dp(4));
        NotationView view=new NotationView(activity,new NotationView.Listener(){
            public void onRendered(){if(!closed&&notation!=null)notation.setCreationSelected(review.selection());}
            public void onCreationNoteTapped(String id){tapGraphicalNote(id);}
            public void onError(String text){if(!closed)activity.message("Problema ao desenhar partitura: "+text);}
        });
        notation=view;view.setCreationSourceNotes(r.notes);view.setCreationSelected(review.selection());
        attach(sheet,view,split?218:310,0);
        attach(contents,sheet,split?238:330,11);
        view.post(()->{if(!closed&&notation==view)view.loadScore(score,0,true);});
        addText("Toque diretamente em uma nota da partitura para selecioná-la; notas sobrepostas ambíguas também podem ser selecionadas na lista. O MIDI original não muda.",12,false,9);
        if(!r.warnings.isEmpty())addText(r.warnings.size()+" avisos de interpretação MIDI — consulte a gravação original em caso de dúvida.",12,false,8);
        if(split)splitControls();
        else{
            actions(contents,button("Ajustar ritmo",false,this::editRhythm),
                    button("Divisão entre pautas",true,()->render(true)));
            Button back=button("Voltar à composição",false,this::confirmClose);
            Button save=button(saved?"Partitura salva ✓":"Salvar partitura",true,this::saveCurrent);
            if(saved){
                boolean added=new LibraryStore(activity.prefs,activity.user).hasCreation(project.id);
                if(added){
                    actions(contents,back,save);
                    addText("✓ Na Biblioteca Musical · abra a Biblioteca para estudar.",13,false,4);
                }else{
                    Button add=button("Adicionar à Biblioteca",true,()->{
                        if(closed||busy)return;busy=true;
                        activity.addCreationScoreToLibrary(project.id,ok->{
                            if(closed)return;busy=false;if(ok)render(false);
                        });
                    });
                    actions(contents,back,save,add);
                }
                addText("Partitura salva neste projeto e incluída no backup v4.",12,false,3);
            }else{
                actions(contents,back,save);
                addText("Salve a partitura antes de adicioná-la à Biblioteca Musical.",12,false,3);
            }
        }
    }
    private void editRhythm(){
        CreationScoreTranscriber.Options o=review.options();
        LinearLayout content=vertical();content.setPadding(dp(16),dp(6),dp(16),0);
        EditText tempo=new EditText(activity);tempo.setSingleLine(true);
        tempo.setInputType(InputType.TYPE_CLASS_NUMBER);tempo.setText(String.valueOf(o.bpm));

        final String[] meters={"2/4","3/4","4/4","6/8"};
        Spinner meter=new Spinner(activity);meter.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,meters));
        meter.setSelection(o.beats==6?3:o.beats==4?2:o.beats==3?1:0);

        final String[] grids={"1/8","1/16","1/32"};
        Spinner grid=new Spinner(activity);grid.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,grids));
        grid.setSelection(o.grid==8?0:o.grid==16?1:2);
        responsiveFields(content,new String[]{"Andamento (BPM)","Compasso","Precisão rítmica"},new View[]{tempo,meter,grid});
        final String[] names={"Rigorosa","Equilibrada","Flexível"};
        Spinner regular=new Spinner(activity);regular.setAdapter(new ArrayAdapter<>(activity,
            android.R.layout.simple_spinner_dropdown_item,names));
        regular.setSelection(o.regularization.ordinal());
        LinearLayout level=vertical();
        attach(level,label("Regularização rítmica",14,true,0xff172A44),-2,4);
        attach(level,regular,48,4);
        attach(level,label("Corrige variações entre ataques da mesma voz. A gravação MIDI original permanece intacta.",
            12,false,0xff57708A),-2,4);
        attach(content,level,-2,4);
        new AlertDialog.Builder(activity).setTitle("Ajustar partitura").setView(content)
            .setPositiveButton("Atualizar",(d,w)->{
                try{
                    int bpm=Integer.parseInt(tempo.getText().toString().trim());
                    int m=meter.getSelectedItemPosition(),beats=m==3?6:m+2,beatType=m==3?8:4;
                    int step=new int[]{8,16,32}[grid.getSelectedItemPosition()];
                    CreationScoreTranscriber.Regularization chosenRegularization=CreationScoreTranscriber.Regularization.values()[regular.getSelectedItemPosition()];
                    mutate(()->review.configure(bpm,beats,beatType,step,chosenRegularization),false);
                }catch(Exception ex){activity.message("Andamento inválido: "+ex.getMessage());}
            }).setNegativeButton("Cancelar",null).show();
    }
    private void saveCurrent(){
        if(closed||busy||review==null)return;
        busy=true;final int id=++generation;
        CreationScoreTranscriber.Options options=review.options();
        String xml=review.result().xml;
        clear();addText("Salvando partitura com segurança…",14,false,10);
        worker.execute(()->{
            try{
                new CreationScoreStore(activity.getFilesDir()).save(project.id,project.profileId,options,xml);
                post(()->{if(closed||id!=generation)return;busy=false;saved=true;
                    review.markSaved();render(false);});
            }catch(Exception error){post(()->{if(closed||id!=generation)return;busy=false;
                activity.message("Partitura não salva: "+error.getMessage());render(false);});}
        });
    }
    private interface Change {void execute()throws Exception;}
    private void mutate(Change action,boolean remainSplit){
        if(busy||closed)return;
        busy=true;final int id=++generation;clear();addText("Recalculando interpretação…",14,false,9);
        worker.execute(()->{
            try{action.execute();post(()->{if(closed||id!=generation)return;busy=false;saved=false;render(remainSplit);});}
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
        mode.setContentDescription("Modo de divisão entre pautas");
        attach(settings,mode,48,7);
        TextView note=label("Nota-limite MIDI: "+o.splitPitch,13,false,0xff57708A);
        attach(settings,note,-2,3);
        SeekBar threshold=new SeekBar(activity);threshold.setMax(24);threshold.setProgress(Math.min(24,Math.max(0,o.splitPitch-48)));
        threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int value,boolean user){note.setText("Nota-limite MIDI: "+(48+value));}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        threshold.setContentDescription("Nota-limite MIDI para separar as pautas");
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
            select.setContentDescription("Nota "+(i+1)+" de "+notes.size()+": "+noteLabel(n,o));
            select.setChecked(review.selection().contains(n.sourceId));
            noteBoxes.put(n.sourceId,select);
            select.setOnCheckedChangeListener((v,checked)->{if(refreshingSelection)return;
                review.toggle(n.sourceId);syncSelection();
            });
            LinearLayout noteRow=horizontal();noteRow.setTag("creation-score-note-row");
            select.setMaxLines(2);select.setMinHeight(dp(48));
            noteRow.addView(select,new LayoutParams(0,dp(52),1));
            Button chord=button("Acorde",false,()->{
                review.selectChord(n.sourceId);render(true);
            });
            chord.setContentDescription("Marcar acorde que contém a nota "+(i+1)+": "+noteLabel(n,o));
            LayoutParams chordLp=new LayoutParams(dp(106),dp(48));chordLp.leftMargin=dp(8);
            noteRow.addView(chord,chordLp);attach(list,noteRow,52,4);
        }
        Button previous=button("← Anteriores",false,()->{offset=Math.max(0,offset-12);render(true);});
        Button next=button("Próximas →",false,()->{offset+=12;render(true);});
        previous.setEnabled(offset>0);next.setEnabled(max<notes.size());
        actions(list,previous,next);
        actions(list,button("Selecionar intervalo de compassos",false,this::chooseMeasures),
                button("Desmarcar seleção",false,()->{review.clearSelection();render(true);}));
        attach(contents,list,-2,9);
        Button upper=button("Pauta superior (Sol)",true,()->mutate(()->review.moveSelection(1),true));
        Button lower=button("Pauta inferior (Fá)",false,()->mutate(()->review.moveSelection(2),true));
        upper.setEnabled(review.selectedCount()>0);lower.setEnabled(review.selectedCount()>0);
        actions(contents,upper,lower);
        Button handVoice=button("Atribuir mão / voz",false,this::chooseHandVoice);
        Button uniform=button("Uniformizar trecho",true,this::chooseUniformization);
        handVoice.setEnabled(review.selectedCount()>0);uniform.setEnabled(review.selectedCount()>0);
        actions(contents,handVoice,uniform);
        Button undo=button("Desfazer",false,()->mutate(()->review.undo(),true));
        Button redo=button("Refazer",false,()->mutate(()->review.redo(),true));
        undo.setEnabled(review.canUndo());redo.setEnabled(review.canRedo());
        actions(contents,undo,redo);
        actions(contents,
            button("Restaurar ritmo automático",false,()->new AlertDialog.Builder(activity)
                .setTitle("Restaurar interpretação automática?")
                .setMessage("Remover as figuras e posições ajustadas manualmente? A gravação MIDI será preservada.")
                .setPositiveButton("Restaurar",(d,w)->mutate(()->review.restoreAutomaticRhythm(),true))
                .setNegativeButton("Cancelar",null).show()),
            button("Restaurar sugestão",false,()->new AlertDialog.Builder(activity)
                .setTitle("Restaurar distribuição?").setMessage("Remover todos os ajustes manuais das pautas? A gravação original será preservada.")
                .setPositiveButton("Restaurar",(d,w)->mutate(()->review.restoreSuggestion(),true))
                .setNegativeButton("Cancelar",null).show()),
            button("Concluir ajustes",true,()->render(false)));
        addText("Toque nas notas da pauta ou marque na lista. Quando duas notas sobrepostas forem indistinguíveis, use a lista para escolher sem ambiguidade.",12,false,0);
    }
    private String noteLabel(CreationScoreTranscriber.WrittenNote n,CreationScoreTranscriber.Options o){
        String[] names={"Dó","Dó♯","Ré","Ré♯","Mi","Fá","Fá♯","Sol","Sol♯","Lá","Lá♯","Si"};
        return names[n.pitch%12]+(n.pitch/12-1)+" · compasso "+(1+n.startTick/o.barTicks())
            +" · pauta "+n.staff+" · voz "+n.voice+" · "
            +(n.hand==1?"mão direita":n.hand==2?"mão esquerda":"mão não atribuída");
    }
    private Spinner choice(LinearLayout panel,String label,String[] values,int current){
        attach(panel,label(label,14,true,0xff172A44),-2,4);
        Spinner spinner=new Spinner(activity);
        spinner.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,values));
        spinner.setSelection(Math.max(0,Math.min(values.length-1,current)));
        attach(panel,spinner,48,8);return spinner;
    }
    private void chooseHandVoice(){
        if(review.selectedCount()==0){activity.message("Selecione notas ou compassos");return;}
        LinearLayout panel=vertical();panel.setPadding(dp(16),dp(8),dp(16),0);
        Spinner hand=choice(panel,"Mão real atribuída pelo compositor",
            new String[]{"Não atribuída","Mão direita","Mão esquerda"},0);
        Spinner voice=choice(panel,"Voz musical",
            new String[]{"Automática (voz 1)","Voz 1","Voz 2","Voz 3","Voz 4"},0);
        attach(panel,label("Pauta superior/inferior não determina qual mão tocou. "
            +"Selecione toda a passagem da mão antes de atribuir, inclusive notas cruzadas.",
            12,false,0xff57708A),-2,4);
        new AlertDialog.Builder(activity).setTitle("Atribuir mão e voz").setView(panel)
            .setPositiveButton("Aplicar",(dialog,which)->mutate(
                ()->review.assignSelection(hand.getSelectedItemPosition(),voice.getSelectedItemPosition()),true))
            .setNegativeButton("Cancelar",null).show();
    }
    private void chooseUniformization(){
        if(review.selectedCount()==0){activity.message("Selecione notas ou compassos");return;}
        LinearLayout panel=vertical();panel.setPadding(dp(16),dp(8),dp(16),0);
        final String[] values={"Semibreve","Mínima","Semínima","Colcheia","Semicolcheia","Fusa"};
        Spinner figure=choice(panel,"Figura musical para o trecho",values,2);
        Spinner hand=choice(panel,"Aplicar à mão",new String[]{"Todas as selecionadas","Mão direita","Mão esquerda"},0);
        Spinner voice=choice(panel,"Aplicar à voz",new String[]{"Todas as vozes","Voz 1","Voz 2","Voz 3","Voz 4"},0);
        attach(panel,label("O ajuste reorganiza somente as notas escolhidas, "
            +"mantém acordes e mostra conflitos antes de alterar a partitura.",
            12,false,0xff57708A),-2,4);
        new AlertDialog.Builder(activity).setTitle("Uniformizar trecho").setView(panel)
            .setPositiveButton("Ver prévia",(dialog,which)->{
                int duration=new int[]{3840,1920,960,480,240,120}[figure.getSelectedItemPosition()];
                int handChoice=hand.getSelectedItemPosition(),voiceChoice=voice.getSelectedItemPosition();
                previewUniformization(duration,handChoice,voiceChoice,values[figure.getSelectedItemPosition()]);
            }).setNegativeButton("Cancelar",null).show();
    }
    private void previewUniformization(int duration,int hand,int voice,String figure){
        if(closed||busy)return;
        busy=true;
        worker.execute(()->{
            try{
                CreationScoreReview.Uniformization plan=review.previewUniformize(duration,hand,voice);
                post(()->{
                    if(closed)return;
                    busy=false;
                    new AlertDialog.Builder(activity).setTitle("Prévia — "+figure)
                        .setMessage("Antes → Depois (posições e durações em tempos de semínima)\n\n"
                            +plan.description+"\nNotas afetadas: "+plan.noteCount
                            +" · linhas rítmicas: "+plan.voiceCount
                            +"\nCompassos após o ajuste: "+plan.preview.measures
                            +"\n\nAs outras notas e o MIDI original não serão alterados.")
                        .setPositiveButton("Aplicar",(d,w)->mutate(()->review.applyUniformize(plan),true))
                        .setNegativeButton("Cancelar",null).show();
                });
            }catch(Exception ex){post(()->{
                if(closed)return;busy=false;
                new AlertDialog.Builder(activity).setTitle("Trecho não uniformizado")
                    .setMessage(ex.getMessage()+"\n\nAmplie a seleção, escolha outra figura ou ajuste a mão/voz.")
                    .setPositiveButton("Fechar",null).show();
            });}
        });
    }
    private void chooseMeasures(){
        LinearLayout panel=vertical();panel.setPadding(dp(18),dp(7),dp(18),0);
        EditText from=new EditText(activity);from.setInputType(InputType.TYPE_CLASS_NUMBER);from.setText("1");
        EditText to=new EditText(activity);to.setInputType(InputType.TYPE_CLASS_NUMBER);
        to.setText(String.valueOf(review.result().measures));
        responsiveFields(panel,new String[]{"Do compasso","Até o compasso (inclusive)"},new View[]{from,to});
        new AlertDialog.Builder(activity).setTitle("Selecionar compassos").setView(panel)
            .setPositiveButton("Marcar",(d,w)->{
                try{review.selectMeasures(Integer.parseInt(from.getText().toString().trim()),
                    Integer.parseInt(to.getText().toString().trim()));render(true);}
                catch(Exception ex){activity.message("Intervalo inválido: "+ex.getMessage());}
            }).setNegativeButton("Cancelar",null).show();
    }
}

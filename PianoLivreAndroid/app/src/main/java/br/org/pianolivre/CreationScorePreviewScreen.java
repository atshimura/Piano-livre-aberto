package br.org.pianolivre;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Z-P3 clean preview: generate from the recording, view, save. No musical
 * adjustments exist in this version (ZAI-001 §5): the tempo and durations are
 * recognized automatically and the preview always shows two staves.
 * Preview, saved bytes and Study share the same MusicXML.
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
    private int generation;
    private boolean saved,legacy;
    private String xml;
    private int measures;
    private double bpm;
    private boolean tempoReliable;
    private List<String> warnings=java.util.Collections.emptyList();
    private float zoom=1.0f;
    private TextView tempoLabel;
    private static final float MIN_ZOOM=0.5f,MAX_ZOOM=2f;

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
                if(event.getAction()==android.view.KeyEvent.ACTION_UP)view.confirmClose();
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
        close.setTextColor(Color.WHITE);
        close.setBackgroundColor(0xff243955);
        header.addView(close,new LayoutParams(dp(110),dp(48)));
        TextView name=label(p.title,18,true,Color.WHITE);
        name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
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
    private void shutdown(){
        closed=true;generation++;if(notation!=null){notation.dispose();notation=null;}
        worker.shutdownNow();
    }
    private void confirmClose(){window.dismiss();}
    /** Reuse a saved derivation when it still matches the recording byte-for-byte. */
    private void generateInitial(){
        final int id=++generation;busy=true;
        worker.execute(()->{
            try{
                CreationScoreStore storage=new CreationScoreStore(activity.getFilesDir());
                String fingerprint=CreationScoreStore.fingerprint(project);
                String reused=null;int reusedBpm=0;String notice="";
                if(storage.has(project.id)){
                    try{
                        CreationScoreStore.Saved prior=storage.read(project.id);
                        if(prior.fingerprint.equals(fingerprint)&&prior.xml.length()>=40){
                            reused=prior.xml;reusedBpm=prior.bpm;saved=true;
                        }else notice="A partitura salva não corresponde mais à gravação; uma nova prévia será gerada.";
                    }catch(CreationScoreStore.LegacyScoreException old){
                        post(()->{if(closed||id!=generation)return;busy=false;legacy=true;renderLegacy();});
                        return;
                    }catch(Exception unreadable){
                        notice="A partitura salva não pôde ser validada. Uma nova prévia será gerada, sem apagar a gravação nem a partitura anterior.";
                    }
                }
                String usedXml=reused;int usedBpm=reusedBpm;int usedMeasures=0;
                boolean reliable=reused!=null&&usedBpm>0;
                double usedBpmValue=reliable?usedBpm:120.0;
                java.util.List<String> usedWarnings=java.util.Collections.emptyList();
                if(usedXml==null){
                    CreationScoreGenerator.Generation generated=CreationScoreGenerator.generate(
                        project.events,project.segments,project.title);
                    usedXml=generated.xml;usedBpmValue=generated.bpm;
                    reliable=generated.tempoReliable;usedMeasures=generated.measures;
                    usedWarnings=generated.warnings;
                    if(!notice.isEmpty())activity.message(notice);
                }else{
                    Score priorScore=Score.read(new ByteArrayInputStream(usedXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                        "creation-score:"+project.id);
                    usedMeasures=priorScore.measures;
                }
                final String finalXml=usedXml;final int finalMeasures=usedMeasures;
                final double finalBpm=usedBpmValue;final boolean finalReliable=reliable;
                final java.util.List<String> finalWarnings=usedWarnings;
                post(()->{
                    if(closed||id!=generation)return;
                    xml=finalXml;measures=finalMeasures;bpm=finalBpm;tempoReliable=finalReliable;
                    warnings=finalWarnings;
                    busy=false;legacy=false;renderPreview();
                });
            }catch(Exception ex){post(()->failure(id,ex));}
        });
    }
    private void failure(int id,Exception ex){
        if(closed||id!=generation)return;busy=false;clear();
        addText("Não foi possível gerar a partitura: "+ex.getMessage(),14,false,12);
        attach(contents,button("Fechar prévia",true,()->window.dismiss()),48,0);
    }
    private void clear(){
        if(notation!=null){notation.dispose();notation=null;}
        contents.removeAllViews();tempoLabel=null;
    }
    /** Old PLS1 derivative: explained state, nothing deleted, regeneration offered. */
    private void renderLegacy(){
        clear();legacy=true;
        addText("Partitura do gerador anterior",21,true,3);
        addText("Esta composição tem uma partitura criada pelo gerador anterior, que foi substituído "
            +"e não mantém os formatos antigos. Nada foi apagado: a gravação original está intacta "
            +"e o arquivo antigo permanece salvo. Toque em Gerar nova partitura para criar uma "
            +"derivação nova a partir da gravação; ela substituirá o arquivo antigo.",14,false,10);
        attach(contents,button("Gerar nova partitura",true,this::regenerate),48,8);
        attach(contents,button("Voltar à composição",false,()->window.dismiss()),48,0);
    }
    /** Force a fresh generation (legacy replacement or stale fingerprint). */
    private void regenerate(){
        if(busy||closed)return;busy=true;final int id=++generation;
        worker.execute(()->{
            try{
                CreationScoreGenerator.Generation generated=CreationScoreGenerator.generate(
                    project.events,project.segments,project.title);
                post(()->{
                    if(closed||id!=generation)return;
                    xml=generated.xml;measures=generated.measures;bpm=generated.bpm;
                    tempoReliable=generated.tempoReliable;warnings=generated.warnings;saved=false;
                    busy=false;legacy=false;renderPreview();
                });
            }catch(Exception ex){post(()->failure(id,ex));}
        });
    }
    private void renderPreview(){
        if(xml==null||closed)return;
        clear();
        attach(contents,label(project.title,21,true,0xff172A44),-2,3);
        String tempoText=tempoReliable
            ?"Andamento estimado: "+Math.round(bpm)+" BPM"
            :"Andamento não identificado com segurança — prévia a 120 BPM (provisória)";
        tempoLabel=label(measures+" compassos · 4/4 · "+tempoText,13,false,0xff57708A);
        attach(contents,tempoLabel,-2,9);
        LinearLayout sheet=card();sheet.setPadding(dp(4),dp(4),dp(4),dp(4));
        int sheetHeight=Math.max(dp(240),Math.round(getResources().getDisplayMetrics().heightPixels*0.42f));
        NotationView view=new NotationView(activity,new NotationView.Listener(){
            public void onRendered(){/* zoom é aplicado sob demanda pelo usuário */}
            public void onError(String text){
                if(!closed)activity.reportNotationIssue("PL-NOT-RENDER",
                    "Prévia de Criação",text,xml,
                    ()->{if(!closed)renderPreview();});
            }
            @Override public void onFailure(NotationFailure failure){
                if(!closed)activity.showNotationFailure(failure,
                    ()->{if(!closed)renderPreview();},
                    ()->{if(window!=null)window.dismiss();});
            }
        });
        notation=view;
        attach(sheet,view,sheetHeight,0);
        attach(contents,sheet,-2,8);
        view.post(()->{if(!closed&&notation==view)loadCurrent();});
        actions(sheet,button("A−",false,()->applyZoom(zoom/1.25f)),
            button("A+",false,()->applyZoom(zoom*1.25f)),
            button("Ajustar à largura",false,()->applyZoom(1.0f)));
        if(warnings.size()>0)
            addText(warnings.size()+" observações da interpretação MIDI registradas; a gravação original permanece intacta.",12,false,8);
        else addText("Nenhuma anomalia MIDI encontrada. A gravação original permanece intacta.",12,false,8);
        Button back=button("Voltar à composição",false,()->confirmClose());
        Button save=button(saved?"Partitura salva ✓":"Salvar partitura",true,this::saveCurrent);
        boolean inLibrary=new LibraryStore(activity.prefs,activity.user).hasCreation(project.id);
        if(saved&&!inLibrary){
            Button add=button("Adicionar à Biblioteca",true,()->{
                if(closed||busy)return;busy=true;
                activity.addCreationScoreToLibrary(project.id,ok->{
                    if(closed)return;busy=false;renderPreview();
                });
            });
            actions(contents,back,save,add);
        }else actions(contents,back,save);
        if(saved&&inLibrary)addText("✓ Na Biblioteca Musical · abra a Biblioteca para estudar.",13,false,4);
        addText(saved?"Partitura salva neste projeto e incluída no backup.":"Salve a partitura antes de adicioná-la à Biblioteca Musical.",12,false,3);
    }
    private void loadCurrent(){
        try{
            Score score=Score.read(new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "creation-score:"+project.id);
            if(notation!=null)notation.loadScore(score,0,true);
        }catch(Exception error){
            if(!closed)activity.message("Partitura ilegível na prévia: "+error.getMessage());
        }
    }
    private void applyZoom(float target){
        zoom=Math.max(MIN_ZOOM,Math.min(MAX_ZOOM,target));
        if(notation!=null)notation.setCreationZoom(zoom);
    }
    private void saveCurrent(){
        if(closed||busy||xml==null)return;
        busy=true;final int id=++generation;
        final int bpmValue=(int)Math.round(tempoReliable?bpm:0);
        clear();addText("Verificando compatibilidade com Estudar…",14,false,10);
        worker.execute(()->{
            try{
                Score target=Score.read(new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                    "creation-score:"+project.id);
                post(()->{
                    if(closed||id!=generation)return;
                    CreationStudyCompatibility.check(activity,target,compatible->{
                        if(closed||id!=generation)return;
                        if(!compatible){
                            busy=false;renderPreview();return;
                        }
                        worker.execute(()->{
                            try{
                                new CreationScoreStore(activity.getFilesDir()).save(
                                    project.id,project.profileId,xml,bpmValue);
                                post(()->{
                                    if(closed||id!=generation)return;
                                    busy=false;saved=true;renderPreview();
                                });
                            }catch(Exception ex){post(()->{
                                if(closed||id!=generation)return;
                                busy=false;activity.message("Partitura não salva: "+ex.getMessage());
                                renderPreview();
                            });}
                        });
                    });
                });
            }catch(Exception error){post(()->{
                if(closed||id!=generation)return;
                busy=false;activity.message("Partitura incompatível: "+error.getMessage());
                renderPreview();
            });}
        });
    }
}

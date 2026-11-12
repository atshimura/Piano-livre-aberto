package br.org.pianolivre;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
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
    private final LinearLayout contents,scoreToolbar,scoreArea,header,headerActionSlot;
    private final ScrollView scroll;
    private final HorizontalScrollView toolbarScroll;
    private final TextView nameLabel;
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
        // One fixed 56dp bar. The toolbar can scroll HORIZONTALLY in very
        // narrow landscape windows; Back and the primary action never scroll.
        header=horizontal();
        header.setTag("creation-score-unified-header");
        header.setPadding(dp(8),dp(4),dp(8),dp(4));
        header.setBackgroundColor(0xff101C31);
        Button close=button("←",false,()->confirmClose());
        close.setTextSize(22);close.setTextColor(Color.WHITE);
        close.setContentDescription("Voltar à composição");
        close.setTooltipText("Voltar à composição");
        close.setPadding(0,0,0,0);
        GradientDrawable backSurface=new GradientDrawable();
        backSurface.setColor(0xff283F5C);backSurface.setCornerRadius(dp(12));
        close.setBackground(new RippleDrawable(ColorStateList.valueOf(0x30ffffff),backSurface,null));
        close.setTag("creation-score-header-back");
        header.addView(close,new LayoutParams(dp(48),dp(48)));

        nameLabel=label(p.title,16,true,Color.WHITE);
        nameLabel.setTag("creation-score-header-title");
        nameLabel.setSingleLine(true);
        nameLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        nameLabel.setContentDescription("Composição: "+p.title);
        nameLabel.setPadding(dp(8),0,dp(7),0);
        header.addView(nameLabel,new LayoutParams(0,-1,1));

        toolbarScroll=new HorizontalScrollView(a);
        toolbarScroll.setHorizontalScrollBarEnabled(false);
        toolbarScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        toolbarScroll.setFillViewport(false);
        toolbarScroll.setVisibility(GONE);
        toolbarScroll.setTag("creation-score-inline-tools-scroll");
        scoreToolbar=horizontal();
        scoreToolbar.setTag("creation-score-tempo-zoom-toolbar");
        scoreToolbar.setPadding(0,0,0,0);
        toolbarScroll.addView(scoreToolbar,new android.widget.FrameLayout.LayoutParams(-2,-1));
        header.addView(toolbarScroll,new LayoutParams(dp(306),-1));

        headerActionSlot=horizontal();
        headerActionSlot.setTag("creation-score-top-action-slot");
        headerActionSlot.setVisibility(GONE);
        LayoutParams actionSlotParams=new LayoutParams(dp(116),-1);
        actionSlotParams.leftMargin=dp(5);
        header.addView(headerActionSlot,actionSlotParams);
        addView(header,new LayoutParams(-1,dp(56)));
        header.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or_,ob)->{
            if(r>l && r-l!=or_-ol)fitUnifiedHeader(r-l);
        });

        // Legacy/outdated/loading states still have their accessible
        // vertical ScrollView, but no extra persistent bars.
        contents=vertical();contents.setPadding(dp(14),dp(12),dp(14),dp(24));
        scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.addView(contents);
        addView(scroll,new LayoutParams(-1,0,1));

        scoreArea=vertical();scoreArea.setPadding(dp(6),dp(4),dp(6),dp(5));
        scoreArea.setTag("creation-score-reading-area");scoreArea.setVisibility(GONE);
        addView(scoreArea,new LayoutParams(-1,0,1));
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
        b.setTypeface(null,android.graphics.Typeface.BOLD);
        GradientDrawable bg=primary
            ?new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xff317FE0,0xff1260C6})
            :new GradientDrawable();
        if(!primary){bg.setColor(0xffF0F5FC);bg.setStroke(dp(1),0xffCAD8E9);}
        bg.setCornerRadius(dp(14));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(
            primary?0x30ffffff:0x180C2445),bg,null));
        b.setPadding(dp(12),0,dp(12),0);
        b.setElevation(dp(2));
        b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));
        b.setOnClickListener(v->{if(!busy&&!closed)run.run();});
        return b;
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
                            if(ScoreXmlComposer.isCurrentDerivedXml(prior.xml)){
                                reused=prior.xml;reusedBpm=prior.bpm;saved=true;
                            }else{
                                // The recording is unchanged, but the old
                                // score may still contain automatic ties.
                                // Never reuse it silently or overwrite it.
                                post(()->{if(closed||id!=generation)return;
                                    busy=false;renderOutdated();});
                                return;
                            }
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
        scoreToolbar.removeAllViews();headerActionSlot.removeAllViews();scoreArea.removeAllViews();
        toolbarScroll.setVisibility(GONE);headerActionSlot.setVisibility(GONE);
        scoreArea.setVisibility(GONE);scroll.setVisibility(VISIBLE);
    }
    /** PLS2 from an older engine: keep the file until the user saves a new score. */
    private void renderOutdated(){
        clear();
        addText("Partitura de uma versão anterior",21,true,3);
        addText("Esta composição possui uma partitura salva com uma versão anterior do gerador, "
            +"que ainda exibe pausas, staccato ou outros símbolos acessórios. A gravação MIDI original permanece intacta. "
            +"Para mostrar somente notas, acordes e valores rítmicos, toque em Gerar partitura básica. "
            +"A partitura antiga não será substituída até que você escolha Salvar partitura.",14,false,10);
        attach(contents,button("Gerar partitura básica",true,this::regenerate),48,8);
        attach(contents,button("Voltar à composição",false,()->window.dismiss()),48,0);
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
    /** A short, high-contrast action with at least a 48dp touch target. */
    private Button compactTool(String text,String description,Runnable action){
        Button b=button(text,false,action);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setPadding(0,0,0,0);
        b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(0xff293F5D);bg.setStroke(dp(1),0xff536A88);
        bg.setCornerRadius(dp(12));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x32ffffff),bg,null));
        b.setElevation(dp(1));
        b.setContentDescription(description);
        b.setTooltipText(description);
        return b;
    }
    private void addTool(View v,int widthDp,int spacingDp){
        LayoutParams p=new LayoutParams(dp(widthDp),dp(48));
        p.rightMargin=dp(spacingDp);
        scoreToolbar.addView(v,p);
    }
    /** Preserve the primary action and Back during horizontal toolbar overflow. */
    private void fitUnifiedHeader(int screenWidthPx){
        int widthDp=Math.round(screenWidthPx/getResources().getDisplayMetrics().density);
        boolean compact=widthDp<560;
        if(nameLabel.getVisibility()!=(compact?GONE:VISIBLE))
            nameLabel.setVisibility(compact?GONE:VISIBLE);
        // Width left after 48dp Back, 116dp primary, horizontal padding
        // and at least 96dp of visible title on a spacious phone/tablet.
        int titleRoom=compact?0:Math.min(180,Math.max(72,(widthDp-460)/2));
        int available=Math.max(112,widthDp-48-116-16-5-titleRoom);
        int target=dp(Math.min(306,available));
        LayoutParams lp=(LayoutParams)toolbarScroll.getLayoutParams();
        if(lp.width!=target){lp.width=target;toolbarScroll.setLayoutParams(lp);}
    }
    private String midiMessage(){
        return warnings.size()>0
            ?warnings.size()+" observações da interpretação MIDI registradas; a gravação original permanece intacta."
            :"Nenhuma anomalia MIDI encontrada. A gravação original permanece intacta.";
    }
    private void showMidiInfo(){
        if(closed)return;
        new AlertDialog.Builder(activity)
            .setTitle("Informações da partitura")
            .setMessage(midiMessage()+"\n\nA partitura usa figuras simples, sem pausas visíveis, staccato ou ligaduras automáticas. "
                +"As durações reais e os eventos MIDI permanecem na gravação original. "
                +"O zoom altera somente a visualização.")
            .setPositiveButton("Fechar",null)
            .show();
    }
    /** All controls share ONE horizontal bar above the entire score viewport. */
    private void renderScoreToolbar(){
        scoreToolbar.removeAllViews();headerActionSlot.removeAllViews();
        String tempoText=tempoReliable
            ?"Andamento estimado: "+Math.round(bpm)+" BPM"
            :"Andamento não identificado com segurança — prévia a 120 BPM (provisória)";
        int shownBpm=(int)Math.round(tempoReliable?bpm:120);
        LinearLayout chip=horizontal();
        chip.setGravity(Gravity.CENTER);
        GradientDrawable chipBg=new GradientDrawable();
        chipBg.setColor(0xff254B74);chipBg.setCornerRadius(dp(12));
        chip.setBackground(chipBg);
        chip.setTag("creation-score-tempo-chip");
        tempoLabel=label("♩ "+shownBpm+" bpm",14,true,Color.WHITE);
        tempoLabel.setSingleLine(true);
        tempoLabel.setContentDescription(measures+" compassos · 4/4 · "+tempoText);
        chip.addView(tempoLabel,new LayoutParams(-2,-1));
        addTool(chip,96,5);

        Button smaller=compactTool("A−","Diminuir zoom da partitura",()->applyZoom(zoom/1.25f));
        smaller.setTag("creation-score-zoom-out");
        Button larger=compactTool("A+","Aumentar zoom da partitura",()->applyZoom(zoom*1.25f));
        larger.setTag("creation-score-zoom-in");
        Button fit=compactTool("↔","Ajustar à largura",()->applyZoom(1.0f));
        fit.setTag("creation-score-zoom-fit");
        Button info=compactTool("ⓘ","Informações da interpretação MIDI",this::showMidiInfo);
        info.setTag("creation-score-midi-info");
        info.setContentDescription("Informações da interpretação MIDI; "+midiMessage());
        addTool(smaller,48,4);addTool(larger,48,4);
        addTool(fit,48,4);addTool(info,48,0);

        boolean inLibrary=saved&&new LibraryStore(activity.prefs,activity.user).hasCreation(project.id);
        Button primary;
        if(!saved){
            primary=button("Salvar",true,this::saveCurrent);
            primary.setContentDescription("Salvar partitura. Salve a partitura antes de adicioná-la à Biblioteca Musical.");
            primary.setTag("creation-score-top-save");
        }else if(!inLibrary){
            primary=button("+ Biblioteca",true,()->{
                if(closed||busy)return;busy=true;
                activity.addCreationScoreToLibrary(project.id,ok->{
                    if(closed)return;busy=false;renderPreview();
                });
            });
            primary.setContentDescription("Adicionar à Biblioteca Musical a partitura salva");
            primary.setTag("creation-score-top-library-add");
        }else{
            primary=button("✓ Biblioteca",false,()->{});
            primary.setContentDescription("Partitura salva e adicionada à Biblioteca Musical");
            primary.setTag("creation-score-top-library-added");
            primary.setEnabled(false);
        }
        primary.setSingleLine(true);primary.setTextSize(13);
        primary.setPadding(dp(3),0,dp(3),0);
        headerActionSlot.addView(primary,new LayoutParams(-1,dp(48)));
        headerActionSlot.setVisibility(VISIBLE);
        toolbarScroll.setVisibility(VISIBLE);
        header.post(()->{if(!closed&&getWidth()>0)fitUnifiedHeader(getWidth());});
    }
    private void renderPreview(){
        if(xml==null||closed)return;
        clear();
        scroll.setVisibility(GONE);
        scoreArea.setVisibility(VISIBLE);
        renderScoreToolbar();

        // No footer, no second toolbar and no Android-level page scroll.
        // The OSMD WebView fills all the height below the 56dp header.
        LinearLayout sheet=card();
        sheet.setTag("creation-score-sheet");
        sheet.setPadding(dp(3),dp(3),dp(3),dp(3));
        scoreArea.addView(sheet,new LayoutParams(-1,0,1));
        NotationView view=new NotationView(activity,new NotationView.Listener(){
            public void onRendered(){
                if(!closed&&notation!=null&&Math.abs(zoom-1.0f)>0.001f)
                    notation.setCreationZoom(zoom);
            }
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
        view.setTag("creation-score-notation-viewport");
        sheet.addView(view,new LayoutParams(-1,0,1));
        view.post(()->{if(!closed&&notation==view)loadCurrent();});
    }
    private void loadCurrent(){
        try{
            Score score=Score.read(new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "creation-score:"+project.id);
            if(notation!=null)notation.loadCreationScore(score,0);
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

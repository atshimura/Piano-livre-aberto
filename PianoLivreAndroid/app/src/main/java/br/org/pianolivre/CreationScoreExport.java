package br.org.pianolivre;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Offline export of the rendered Creation score (NotationView/OSMD) to PDF or
 * PNG. The WebView is scrolled page by page and captured with view.draw; no
 * network, no shared storage, no change to the rendered layout. Whatever the
 * user sees — including pinch zoom — is what gets exported: the CSS↔pixel
 * ratio is measured live, so zoomed sheets still compose correctly.
 */
final class CreationScoreExport {
    static final int PDF_PAGE_WIDTH=595, PDF_PAGE_HEIGHT=842; // A4 points
    static final int MAX_PDF_PAGES=200;
    static final int MAX_PNG_HEIGHT=12000; // px; longer scores must use PDF
    static final int CAPTURE_WAIT_MS=140;
    interface Callback { void done(List<File> files,boolean truncated); void error(String message); }

    private CreationScoreExport(){}

    static void export(NotationView view,File outDir,String baseName,boolean pdf,Callback callback){
        if(view==null||!view.isRendered()){callback.error("A partitura não está pronta para exportar.");return;}
        int savedLayer=view.getLayerType();
        view.setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        view.evaluateJavascript("(function(){var v=document.getElementById('viewport');"
            +"return JSON.stringify({h:v?v.scrollHeight:0,w:v?v.clientWidth:0})})()",
            value->view.postDelayed(()->measure(view,outDir,baseName,pdf,callback,value,savedLayer),150));
    }

    private static void measure(NotationView view,File outDir,String baseName,boolean pdf,
                                Callback callback,String measured,int savedLayer){
        int viewWidth=view.getWidth(),viewHeight=view.getHeight();
        double cssToView=1;
        long totalHeight=0;
        try{
            int open=measured.indexOf('{'),close=measured.lastIndexOf('}');
            org.json.JSONObject stats=new org.json.JSONObject(measured.substring(open,close+1));
            double cssWidth=stats.getDouble("w"),cssHeight=stats.getDouble("h");
            if(cssWidth>0&&viewWidth>0)cssToView=viewWidth/cssWidth;
            totalHeight=Math.round(cssHeight*cssToView);
        }catch(Exception ex){
            restore(view,savedLayer);
            callback.error("Não foi possível medir a partitura para exportar.");
            return;
        }
        if(viewWidth<=0||viewHeight<=0||totalHeight<viewHeight/2){
            restore(view,savedLayer);
            callback.error("Partitura sem conteúdo suficiente para exportar.");
            return;
        }
        File directory=new File(outDir,"partituras");
        if(!directory.isDirectory()&&!directory.mkdirs()){
            restore(view,savedLayer);
            callback.error("Não foi possível preparar a pasta de exportação.");
            return;
        }
        final String base=sanitize(baseName);
        final double ratio=cssToView;
        if(pdf)exportPdf(view,directory,base,totalHeight,viewWidth,viewHeight,ratio,savedLayer,callback);
        else exportPng(view,directory,base,totalHeight,viewWidth,viewHeight,ratio,savedLayer,callback);
    }

    private static String sanitize(String title){
        StringBuilder out=new StringBuilder();
        if(title!=null)for(char c:title.toLowerCase().toCharArray()){
            if(out.length()>=40)break;
            if(c>='a'&&c<='z'||c>='0'&&c<='9')out.append(c);
            else if(out.length()>0&&out.charAt(out.length()-1)!='-')out.append('-');
        }
        if(out.length()==0)out.append("partitura");
        return out.toString();
    }

    private interface Slice { void onSlice(Bitmap slice,long start,long height,boolean last); }
    private interface Finished { void end(); }
    private interface Failure { void fail(String message); }

    /** Scrolls the sheet step by step and hands every viewport capture to the
     * consumer immediately, so memory holds one slice plus the target document.
     * All steps run on the UI thread through postDelayed chaining. */
    private static void captureAll(NotationView view,long totalHeight,int sliceHeight,double cssToView,
                                   Slice consumer,Finished finished,Failure failure){
        long[] position={0};
        boolean[] stopped={false};
        class Step implements Failure {
            void run(){
                if(stopped[0])return;
                final long start=position[0];
                final long height=Math.min(sliceHeight,totalHeight-start);
                view.evaluateJavascript("document.getElementById('viewport').scrollTop="+(start/cssToView),null);
                view.postDelayed(()->{
                    if(stopped[0])return;
                    Bitmap slice;
                    try{slice=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);}
                    catch(Throwable ex){fail("Memória insuficiente para exportar. Tente o PDF.");return;}
                    try{Canvas capture=new Canvas(slice);view.draw(capture);}
                    catch(Throwable ex){slice.recycle();fail("Falha ao capturar a partitura.");return;}
                    boolean last=start+height>=totalHeight;
                    try{consumer.onSlice(slice,start,height,last);}
                    catch(Throwable ex){slice.recycle();fail("Falha ao montar o arquivo: "+ex.getMessage());return;}
                    slice.recycle();
                    if(last){try{finished.end();}catch(Throwable ex){fail("Falha ao gravar o arquivo: "+ex.getMessage());}return;}
                    position[0]=start+height;
                    run();
                },CAPTURE_WAIT_MS);
            }
            public void fail(String message){
                stopped[0]=true;scrollToTop(view);failure.fail(message);
            }
        }
        new Step().run();
    }

    private static void exportPdf(NotationView view,File directory,String base,long totalHeight,
                                  int viewWidth,int viewHeight,double cssToView,int savedLayer,Callback callback){
        int pageHeight=Math.round(viewWidth*(float)PDF_PAGE_HEIGHT/(float)PDF_PAGE_WIDTH);
        long expectedPages=Math.min(MAX_PDF_PAGES,(totalHeight+pageHeight-1)/pageHeight);
        final PdfDocument document=new PdfDocument();
        final Paint paint=new Paint();paint.setFilterBitmap(true);
        final int[] currentPage={-1};
        final int[] emitted={0};
        final Bitmap[] pageBitmap={null};
        final Canvas[] pageCanvas={null};
        final File target=new File(directory,base+".pdf");
        captureAll(view,totalHeight,viewHeight,cssToView,
            (slice,start,height,last)->{
                long pos=start,end=Math.min(start+height,totalHeight);
                while(pos<end){
                    int index=(int)(pos/pageHeight);
                    if(index>=MAX_PDF_PAGES)break;
                    if(index!=currentPage[0]){
                        if(pageBitmap[0]!=null)emitPage(document,paint,pageBitmap[0],emitted);
                        pageBitmap[0]=Bitmap.createBitmap(viewWidth,pageHeight,Bitmap.Config.ARGB_8888);
                        pageCanvas[0]=new Canvas(pageBitmap[0]);
                        pageCanvas[0].drawColor(android.graphics.Color.WHITE);
                        currentPage[0]=index;
                    }
                    long srcTop=pos;
                    long srcBottom=Math.min(Math.min(start+height,(long)(index+1)*pageHeight),end);
                    pageCanvas[0].drawBitmap(slice,
                        new Rect(0,(int)(pos-start),viewWidth,(int)(srcBottom-start)),
                        new Rect(0,(int)(pos-(long)index*pageHeight),viewWidth,
                            (int)(pos-(long)index*pageHeight+(srcBottom-srcTop))),paint);
                    pos=srcBottom;
                    if(pos>=(long)(index+1)*pageHeight||pos>=totalHeight){
                        emitPage(document,paint,pageBitmap[0],emitted);
                        pageBitmap[0].recycle();pageBitmap[0]=null;pageCanvas[0]=null;currentPage[0]=-1;
                    }
                }
            },
            ()->{
                if(pageBitmap[0]!=null){
                    emitPage(document,paint,pageBitmap[0],emitted);
                    pageBitmap[0].recycle();pageBitmap[0]=null;
                }
                scrollToTop(view);
                boolean truncated=emitted[0]<Math.min(MAX_PDF_PAGES,(totalHeight+pageHeight-1)/pageHeight);
                new Thread(()->{
                    try(FileOutputStream out=new FileOutputStream(target)){
                        document.writeTo(out);document.close();
                        List<File> files=new ArrayList<>();files.add(target);
                        view.post(()->{restore(view,savedLayer);callback.done(files,truncated);});
                    }catch(Exception ex){
                        try{document.close();}catch(Exception ignored){}
                        target.delete();
                        view.post(()->{restore(view,savedLayer);
                            callback.error("Falha ao gravar o PDF: "+ex.getMessage());});
                    }
                },"PianoLivre-ExportPdf").start();
            },
            message->{
                try{document.close();}catch(Exception ignored){}
                view.post(()->{restore(view,savedLayer);callback.error(message);});
            });
    }

    private static void emitPage(PdfDocument document,Paint paint,Bitmap page,int[] emitted){
        PdfDocument.Page info=document.startPage(new PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH,
            PDF_PAGE_HEIGHT,document.getPages().size()+1).create());
        info.getCanvas().drawBitmap(page,null,new Rect(0,0,PDF_PAGE_WIDTH,PDF_PAGE_HEIGHT),paint);
        document.finishPage(info);
        emitted[0]++;
    }

    private static void exportPng(NotationView view,File directory,String base,long totalHeight,
                                  int viewWidth,int viewHeight,double cssToView,int savedLayer,Callback callback){
        final long height=Math.min(totalHeight,MAX_PNG_HEIGHT);
        final boolean[] truncated={false};
        final Bitmap[] sheet={null};
        final Canvas[] canvas={null};
        final Paint paint=new Paint();paint.setFilterBitmap(true);
        final File target=new File(directory,base+".png");
        captureAll(view,totalHeight,viewHeight,cssToView,
            (slice,start,heightSlice,last)->{
                if(sheet[0]==null){
                    sheet[0]=Bitmap.createBitmap(viewWidth,(int)height,Bitmap.Config.RGB_565);
                    canvas[0]=new Canvas(sheet[0]);
                    canvas[0].drawColor(android.graphics.Color.WHITE);
                }
                long drawBottom=Math.min(start+heightSlice,height);
                if(drawBottom>start)canvas[0].drawBitmap(slice,
                    new Rect(0,0,viewWidth,(int)(drawBottom-start)),
                    new Rect(0,(int)start,viewWidth,(int)drawBottom),paint);
                if(start+heightSlice>=height)truncated[0]=totalHeight>height;
            },
            ()->{
                scrollToTop(view);
                final Bitmap finalSheet=sheet[0];sheet[0]=null;
                final boolean cut=truncated[0];
                new Thread(()->{
                    try(FileOutputStream out=new FileOutputStream(target)){
                        finalSheet.compress(Bitmap.CompressFormat.PNG,100,out);
                        List<File> files=new ArrayList<>();files.add(target);
                        view.post(()->{restore(view,savedLayer);callback.done(files,cut);});
                    }catch(Exception ex){
                        view.post(()->{restore(view,savedLayer);
                            callback.error("Falha ao gravar a imagem: "+ex.getMessage());});
                    }finally{finalSheet.recycle();}
                },"PianoLivre-ExportPng").start();
            },
            message->view.post(()->{restore(view,savedLayer);callback.error(message);}));
    }

    private static void scrollToTop(NotationView view){
        view.evaluateJavascript("document.getElementById('viewport').scrollTop=0",null);
    }

    private static void restore(NotationView view,int savedLayer){
        try{view.setLayerType(savedLayer,null);}catch(Exception ignored){}
        scrollToTop(view);
    }
}

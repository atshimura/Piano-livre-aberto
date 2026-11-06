package br.org.pianolivre;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

/** Linha Z (Tarefa 2, 2a fatia): concentra o fluxo de falhas de notacao que
 * antes vivia em MainActivity (reportNotationIssue, showNotationFailure,
 * showNotationHistory). Refatoracao pura, sem mudanca de comportamento: mesmos
 * textos, mesmos botes, mesma ordem de chamadas, mesmas gravacoes locais.
 * MainActivity mantem delegados package-private com as assinaturas originais,
 * preservando a API usada por PianoScreens, CreationStudyCompatibility e
 * CreationScorePreviewScreen. Nenhum dado sai do aparelho (off-line).
 * Registro: ZAI-CONTINUIDADE.md (zai/feat/refactor-notation-failures). */
final class ZNotationFailures {
 private ZNotationFailures(){}

 static void reportNotationIssue(MainActivity a,String code,String stage,String details,String xml,Runnable retry){
  String version="beta";
  try{version=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;}
  catch(Exception ignored){}
  NotationFailure failure=new NotationFailure(code,stage,details,"creation",xml,
    0,0,0,0,0,version);
  new Thread(()->{
   try{new NotationDiagnosticsStore(a.getFilesDir()).add(failure);}
   catch(Exception e){android.util.Log.w("PianoLivreNotation","Falha ao registrar incidente",e);}
  },"PianoLivre-NotationError").start();
  showNotationFailure(a,failure,retry);
 }
 static void showNotationFailure(MainActivity a,NotationFailure failure,Runnable retry){
  showNotationFailure(a,failure,retry,null);
 }
 static void showNotationFailure(MainActivity a,NotationFailure failure,Runnable retry,Runnable abort){
  if(a.isFinishing()||a.isDestroyed())return;
  a.stopPlayback();a.finishStudyWindow();
  // Android landscape can hide the entire AlertDialog button bar if a long
  // message and a custom action compete for unrestricted vertical space.
  // Bound only the report viewport; keep the complete report scrollable.
  final float density=a.getResources().getDisplayMetrics().density;
  final int reportHeight=Math.min((int)(140*density+0.5f),
      (int)(a.getResources().getDisplayMetrics().heightPixels*0.28f));
  LinearLayout content=new LinearLayout(a);
  content.setOrientation(LinearLayout.VERTICAL);
  content.setPadding((int)(16*density+0.5f),0,(int)(16*density+0.5f),0);
  ScrollView scroll=new ScrollView(a);
  TextView reportText=new TextView(a);
  reportText.setText("O motor moderno OSMD encontrou um problema. A partitura e o MIDI original foram preservados.\n\n"+failure.report());
  reportText.setTextIsSelectable(true);
  scroll.addView(reportText);
  content.addView(scroll,new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,reportHeight));
  Button copyDiagnostic=new Button(a);
  copyDiagnostic.setText(MainActivity.NOTATION_COPY_BUTTON);
  copyDiagnostic.setContentDescription(MainActivity.NOTATION_COPY_BUTTON);
  copyDiagnostic.setAllCaps(false);
  copyDiagnostic.setOnClickListener(v->{
    android.content.ClipboardManager clipboard=(android.content.ClipboardManager)a.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
    if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("Erro de notação Piano Livre",failure.report()));
    Toast.makeText(a,"Diagnóstico copiado; nenhum dado enviado",Toast.LENGTH_SHORT).show();
  });
  content.addView(copyDiagnostic,new LinearLayout.LayoutParams(
      LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
  AlertDialog dialog=new AlertDialog.Builder(a)
    .setTitle(MainActivity.NOTATION_FAILURE_TITLE)
    .setView(content)
    .setPositiveButton("Tentar novamente",(d,w)->{if(retry!=null)retry.run();})
    .setNegativeButton("Voltar à Biblioteca",(d,w)->{
      a.stopPlayback();a.finishStudyWindow();
      if(abort!=null)abort.run();
      if(a.screens!=null)a.screens.showHome();
    })
    .setCancelable(false).create();
  dialog.show();a.lastMessageDialog=dialog;
 }
 static void showNotationHistory(MainActivity a){
  if(a.isFinishing()||a.isDestroyed())return;
  final NotationDiagnosticsStore store=new NotationDiagnosticsStore(a.getFilesDir());
  try{
   final String report=store.report();
   final boolean empty=store.read().length()==0;
   if(empty){a.message("Ainda não há falhas de notação registradas.");return;}
   AlertDialog dlg=new AlertDialog.Builder(a).setTitle("Erros de partitura (armazenamento local)")
     .setMessage(report.length()>6000?report.substring(0,6000)+"\n[Exibindo somente parte do histórico]":report)
     .setPositiveButton("Exportar por escolha própria",(d,w)->{
       Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("text/plain");
       intent.putExtra(Intent.EXTRA_TEXT,report);
       a.startActivity(Intent.createChooser(intent,"Compartilhar diagnóstico"));
     })
     .setNeutralButton("Apagar histórico",(d,w)->new AlertDialog.Builder(a)
       .setTitle("Apagar somente os diagnósticos?")
       .setMessage("Partituras, MIDI e histórico de estudos não serão alterados.")
       .setPositiveButton("Apagar",(dd,ww)->{try{store.clear();a.message("Histórico de erros apagado.");}
         catch(Exception e){a.message("Não foi possível apagar o histórico de erros.");}})
       .setNegativeButton("Cancelar",null).show())
     .setNegativeButton("Fechar",null).create();
   dlg.show();
  }catch(Exception e){a.message("Não foi possível ler o histórico local de erros.");}
 }
}

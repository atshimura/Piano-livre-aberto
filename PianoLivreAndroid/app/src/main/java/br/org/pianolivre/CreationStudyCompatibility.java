package br.org.pianolivre;

import android.app.AlertDialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Actual study-path OSMD check before a generated score can be saved or linked.
 * Parser success alone is insufficient. The view is attached and measured;
 * it exercises the same optical reconciliation as normal Study, offline.
 */
public final class CreationStudyCompatibility {
 private CreationStudyCompatibility(){}
 public static void check(MainActivity activity,Score score,Consumer<Boolean> finished){
  if(score==null||score.sourceXml==null||activity.isFinishing()||activity.isDestroyed()){
   finished.accept(false);return;
  }
  final AtomicBoolean completed=new AtomicBoolean(false);
  final LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);
  final int pad=Math.round(12*activity.getResources().getDisplayMetrics().density);
  panel.setPadding(pad,pad,pad,pad);
  TextView info=new TextView(activity);
  info.setText("Verificando notas, vozes, compassos e o desenho OSMD do modo Estudar. Nenhum MIDI será alterado.");
  panel.addView(info,new LinearLayout.LayoutParams(-1,-2));
  final AlertDialog[] dialog=new AlertDialog[1];
  final NotationView[] view=new NotationView[1];
  view[0]=new NotationView(activity,new NotationView.Listener(){
   public void onRendered(){
    if(!view[0].isEngravingProof()){
     onError("Validação não utilizou o motor de Estudar");return;
    }
    if(!completed.compareAndSet(false,true))return;
    if(dialog[0]!=null)dialog[0].dismiss();
    finished.accept(true);
   }
   public void onError(String message){
    onFailure(new NotationFailure("PL-NOT-UNKNOWN","Validação da partitura",
      message,"creation",score.sourceXml,score.measures,score.writtenEvents.size(),
      0,view[0].getWidth(),view[0].getHeight(),"beta"));
   }
   @Override public void onFailure(NotationFailure failure){
    if(!completed.compareAndSet(false,true))return;
    if(dialog[0]!=null)dialog[0].dismiss();
    activity.showNotationFailure(failure,()->check(activity,score,finished),()->finished.accept(false));
   }
  });
  panel.addView(view[0],new LinearLayout.LayoutParams(-1,
   Math.round(230*activity.getResources().getDisplayMetrics().density)));
  dialog[0]=new AlertDialog.Builder(activity)
   .setTitle("Validando compatibilidade com Estudar")
   .setView(panel)
   .setNegativeButton("Cancelar",(d,w)->{
    if(completed.compareAndSet(false,true))finished.accept(false);
   }).create();
  dialog[0].setOnDismissListener(d->{
   view[0].dispose();
   if(completed.compareAndSet(false,true))finished.accept(false);
  });
  dialog[0].show();
  view[0].post(()->{if(!completed.get())view[0].loadScore(score,0,false);});
 }
}

package br.org.pianolivre;
import android.app.AlertDialog;
import android.os.SystemClock;
import android.widget.EditText;

/** Bloco E (motor de estudo) da refatoração do MainActivity: seleção de mão,
 * navegação do modo Espera, laço A–B e marcas, rota escrita (voltas/Coda),
 * execução (ticker de 25 ms), modo demonstração MIDI e preparação do Espera.
 * O estado mutável (position, running, waitTraveling, loopStart/loopEnd,
 * waitStarted/waitPreparing, route/routeCursor, midiPlayback, previousTick)
 * permanece no MainActivity, acessado package-private — os autotestes
 * (MidiSessionSelfTest, MidiInterfaceSelfTest) gravam e leem esses campos
 * diretamente. Delegados na casca preservam as chamadas cravadas pelos
 * contratos (a.selectHand/a.toggle/a.stopPlayback/a.seekPrevious em
 * PianoScreens — test_study_compact_chrome_contract) e por outras classes
 * (ZMidiInput chama a.nextWait; PianoScreens e ZChordCalibrationUi chamam
 * a.prepareWaiting; ZNotationFailures chama a.stopPlayback). updateInfo e
 * clearRemovedScoreInMemory permanecem na casca por cravatura de contrato
 * (studyInfoText.update( e void clearRemovedScoreInMemory(String scoreKey)
 * verificados sobre o texto do MainActivity). A cadeia do ticker permanece
 * no Runnable da casca (onCreate/onResume/onPause/onDestroy intocados);
 * tick() executa o corpo e re-agenda via a.ticker. */
final class ZStudyEngine {
 private final MainActivity a;
 ZStudyEngine(MainActivity activity){a=activity;}
 boolean writtenRouteActive(){
  return a.score!=null&&a.routeHasMarkers&&a.route!=null&&a.route.supported()
   &&a.routeCursor!=null&&a.loopEnd==Double.MAX_VALUE;
 }
 void resetWrittenRoute(){
  a.writtenRepeats.reset();
  if(a.routeCursor!=null){a.routeCursor.reset();a.routeCursor.align(a.position);}
 }
 void rearmWaitIfStudying(){
  if(a.foreground&&a.waitStarted&&!a.waitPreparing&&a.score!=null&&a.mode==0&&a.screens!=null&&a.screens.isStudyOpen())prepareWaiting();
 }
 void selectHand(int selected){
  if(selected<0||selected>2)return;
  a.stopPlayback();a.finishStudyWindow();
  a.currentHand=selected;a.studyState.hand=selected;
  if(a.screens!=null&&a.screens.notation!=null)a.screens.notation.setStudyHand(selected);
  if(a.score!=null)a.position=HandNavigation.nextPosition(a.score,a.position,selected,a.loopStart,a.loopEnd);
  if(a.routeCursor!=null)a.routeCursor.align(a.position);
  a.studyState.playhead=a.position;
  if(a.mode==0)prepareWaiting();else if(a.screens!=null)a.screens.followScore(a.position,true);
  a.saveSession();a.updateInfo();a.beginStudyWindow();
 }
 void nextWait(){
  if(a.score==null||a.waitTraveling)return;
  double next=writtenRouteActive()?
   a.routeCursor.nextWait(a.score,a.position,a.currentHand):
   a.writtenRepeats.nextWaitTarget(a.score,a.position,a.currentHand,a.loopStart,a.loopEnd);
  // Returning to an earlier bar is intentional; only an unchanged position
  // means that no further written onset/repeat is available.
  if(Math.abs(next-a.position)<.0001)return;
  a.waitTravelTarget=next;
  if(a.waitTravelTarget<=a.position){
   a.position=a.waitTravelTarget;a.studyState.playhead=a.position;
   prepareWaiting();
   if(a.screens!=null)a.screens.followScore(a.position,true);
   return;
  }
  a.waitTraveling=true;a.previousTick=SystemClock.uptimeMillis();
  if(a.screens!=null)a.screens.followScore(a.position,true);
  android.util.Log.i("PianoLivreTimeline","WAIT_TRAVEL_BEGIN from="+a.position+" to="+a.waitTravelTarget);
 }
 void prepareWaiting(){
  if(a.score==null||a.mode!=0)return;
  a.waitRecognizer.clear();
  if(!a.waitStarted||a.waitPreparing){
   a.waitTraveling=false;
   if(a.screens!=null)a.screens.followScore(a.position,true);
   return;
  }
  double next=a.score.at(a.position,a.currentHand).isEmpty()?a.score.nextForHand(a.position,a.currentHand):a.position;
  if(next>a.position+.0001){a.waitTravelTarget=next;a.waitTraveling=true;a.previousTick=SystemClock.uptimeMillis();}
  else{a.waitTraveling=false;a.waitRecognizer.arm(a.score.at(a.position,a.currentHand));}
  if(a.screens!=null)a.screens.followScore(a.position,true);
 }
 /** Retorna ao início da música e suspende qualquer repetição anterior. */
 void seekPrevious(){
  if(a.score==null)return;
  a.stopPlayback();
  a.loopStart=0;a.loopEnd=Double.MAX_VALUE;a.writtenRepeats.reset();
  a.position=0;
  if(a.routeCursor!=null){a.routeCursor.reset();a.routeCursor.align(a.position);}
  a.studyState.playhead=a.position;
  if(a.mode==0)prepareWaiting();
  else if(a.screens!=null)a.screens.followScore(a.position,true);
  if(a.screens!=null)a.screens.clearLoopSelection();
  a.saveSession();a.updateInfo();
 }
 /** Intervalo escolhido por toque diretamente sobre a partitura temporal. */
 void applyLoopSelection(double start,double end){
  if(a.score==null||!Double.isFinite(start)||!Double.isFinite(end))return;
  start=Math.max(0,Math.min(a.score.end(),start));
  end=Math.max(0,Math.min(a.score.end(),end));
  if(end-start<0.01){a.message("Escolha um trecho com início e fim diferentes.");return;}
  a.stopPlayback();
  a.loopStart=start;a.loopEnd=end;resetWrittenRoute();
  a.position=start;
  a.studyState.playhead=a.position;
  if(a.mode==0)prepareWaiting();
  else if(a.screens!=null)a.screens.followScore(a.position,true);
  a.saveSession();a.updateInfo();
 }
 /** Menu start is a pending A marker; it must not mutate a running MIDI route. */
 void markLoopStart(){
  if(a.score==null)return;
  a.stopPlayback();
  a.loopStart=StudyLoopPosition.start(a.score,a.position);
  a.loopEnd=Double.MAX_VALUE;
  resetWrittenRoute();
  a.saveSession();a.updateInfo();
  android.util.Log.i("PianoLivreUI","STUDY_LOOP_MENU start from="+a.loopStart);
 }
 /** Commit B at a complete musical release using the validated loop path. */
 void markLoopEnd(){
  if(a.score==null)return;
  double release=StudyLoopPosition.end(a.score,a.position);
  applyLoopSelection(Math.min(a.loopStart,release),Math.max(a.loopStart,release));
  android.util.Log.i("PianoLivreUI","STUDY_LOOP_MENU end from="+a.loopStart+" to="+a.loopEnd);
 }
 void clearLoop(){
  // Release active MIDI notes before replacing the route and loop bounds.
  a.stopPlayback();
  a.loopStart=0;a.loopEnd=Double.MAX_VALUE;resetWrittenRoute();
  if(a.screens!=null)a.screens.clearLoopSelection();
  a.saveSession();a.updateInfo();
 }
 void toggle(){
  if(a.score==null){a.chooseScore();return;}
  if(a.mode==0){
   if(!a.waitStarted){beginWaitPreparation();return;}
   a.stopPlayback();
   return;
  }
  if(a.running){
   a.stopPlayback();return;
  }
  if(a.position>=a.score.end()-0.001){
   a.position=a.score.firstOnset(a.currentHand);
   a.studyState.playhead=a.position;
   if(a.screens!=null)a.screens.followScore(a.position,true);
  }
  // A paused session preserves its current repeat/Coda pass.
  // Restart the itinerary only when the piece actually finished (which can
  // occur at Fine BEFORE the end of the written score).
  if(a.routeCursor!=null&&a.routeCursor.finished()){
   a.position=a.score.firstOnset(a.currentHand);
   a.routeCursor.reset();a.routeCursor.align(a.position);
   a.studyState.playhead=a.position;
   if(a.screens!=null)a.screens.followScore(a.position,true);
  }
  if(a.mode==2){
   if(!a.midiConnection.snapshot().canSend){
    a.message("Demonstração requer uma saída MIDI aberta para o instrumento. Toque em USB-MIDI e selecione o piano.");
    return;
   }
   a.midiPlayback=new MidiPlayback(a.score,a.currentHand,(pitch,on,velocity)->a.send(pitch,on,velocity));
   a.midiPlayback.seek(a.position); // Includes notes beginning exactly at position zero.
  }
  a.running=true;
  a.previousTick=SystemClock.uptimeMillis();
  android.util.Log.i("PianoLivreUI","play=true mode="+a.mode);
  a.updateInfo();if(a.screens!=null)a.screens.updateSelections();
 }
 void beginWaitPreparation(){
  if(a.score==null||a.mode!=0||a.waitPreparing)return;
  a.waitStarted=true;a.waitPreparing=true;a.waitTraveling=false;a.waitRecognizer.clear();
  int seconds=WaitPreparation.configuredSeconds(a.prefs.getInt("user_"+a.user+"_wait_prep_seconds",5));
  long now=SystemClock.uptimeMillis();
  a.waitPreparation.start(now,seconds);
  final long run=++a.waitPreparationSequence;
  if(a.screens!=null){a.screens.startWaitPreparation(a.waitPreparation.durationMs());a.screens.updateSelections();}
  a.handler.postDelayed(()->{
   if(run!=a.waitPreparationSequence||!a.waitPreparing||!a.waitPreparation.finish(SystemClock.uptimeMillis()))return;
   a.waitPreparing=false;
   if(a.mode==0&&a.waitStarted&&a.foreground&&a.score!=null&&a.screens!=null&&a.screens.isStudyOpen()){
    prepareWaiting();a.screens.updateSelections();
   }else{a.waitStarted=false;if(a.screens!=null)a.screens.updateSelections();}
  },a.waitPreparation.durationMs());
 }
 void configureWaitPreparation(){
  EditText seconds=new EditText(a);seconds.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
  seconds.setSingleLine(true);
  seconds.setText(String.valueOf(WaitPreparation.configuredSeconds(a.prefs.getInt("user_"+a.user+"_wait_prep_seconds",5))));
  new AlertDialog.Builder(a).setTitle("Preparação do modo Espera")
   .setMessage("Duração da entrada visual antes de aguardar notas (1 a 30 segundos). Não haverá prazo para tocar após a preparação.")
   .setView(seconds).setPositiveButton("Salvar",(d,w)->{
    try{
     int selected=Integer.parseInt(seconds.getText().toString().trim());
     if(selected<1||selected>30)throw new NumberFormatException();
     a.prefs.edit().putInt("user_"+a.user+"_wait_prep_seconds",selected).apply();
    }catch(NumberFormatException ex){a.message("Informe entre 1 e 30 segundos.");}
   }).setNegativeButton("Cancelar",null).show();
 }
 void stopPlayback(){
  a.waitStarted=false;a.waitPreparing=false;a.waitPreparationSequence++;a.waitPreparation.cancel();
  if(a.screens!=null)a.screens.stopWaitPreparation();
  a.running=false;a.waitTraveling=false;a.allOff();a.saveSession();a.updateInfo();
  // A paused clock must stop the renderer's requestAnimationFrame extrapolation.
  if(a.screens!=null){a.screens.followScore(a.position,true);a.screens.updateSelections();}
 }
 /** Corpo do ticker (25 ms): a re-agendação usa o Runnable a.ticker, que
  * permanece na casca; comportamento idêntico (re-agenda sempre, inclusive
  * quando a reprodução está parada). */
 void tick(){
  if((a.running||a.waitTraveling)&&a.score!=null){
   long now=SystemClock.uptimeMillis();
   double elapsedMs=Math.max(0,Math.min(now-a.previousTick,250));
   double target=a.score.advanceBeats(a.position,elapsedMs,a.tempoFactor);
   a.previousTick=now;
   if(a.waitTraveling&&a.mode==0){
    a.position=Math.min(a.waitTravelTarget,target);
    if(a.position>=a.waitTravelTarget-.000001){
     a.position=a.waitTravelTarget;a.waitTraveling=false;
     a.waitRecognizer.arm(a.score.at(a.position,a.currentHand));
     android.util.Log.i("PianoLivreTimeline","WAIT_TRAVEL_END at="+a.position);
    }
   }else if(a.running){
    if(writtenRouteActive()){
     PerformanceRoute.Movement movement=a.routeCursor.tick(a.score,a.position,elapsedMs,a.tempoFactor);
     if(a.mode==2&&a.midiPlayback!=null)for(PerformanceRoute.Move step:movement.moves){
      if(step.seek)a.midiPlayback.seek(step.beat);
      else a.midiPlayback.advance(step.beat);
     }
     a.position=movement.position;
     if(movement.finished){a.running=false;a.allOff();}
    }else{
     double finish=Math.min(a.score.end(),a.loopEnd);
     if(finish<=a.loopStart)finish=a.score.end();
     double next=Math.min(target,finish);
     // A–B loop has priority and the non-routed path is linear.
     if(a.mode==2&&a.midiPlayback!=null)a.midiPlayback.advance(next);
     a.position=next;
     if(a.position>=finish-.000001){
      if(a.loopEnd<Double.MAX_VALUE&&a.loopStart<finish){
       a.position=a.loopStart;
       if(a.mode==2&&a.midiPlayback!=null)a.midiPlayback.seek(a.loopStart);
      }else{a.running=false;a.allOff();}
     }
    }
   }
   a.studyState.playhead=a.position;
   if(a.screens!=null)a.screens.followScore(a.position,false);
   a.updateInfo(!a.running&&!a.waitTraveling);
   if(!a.running&&a.screens!=null)a.screens.updateSelections();
  }
  // Re-agendação adaptativa: 25 ms com reprodução/viagem em curso; 250 ms
  // quando ocioso, para poupar bateria sem mudar o comportamento visível.
  a.handler.postDelayed(a.ticker,(a.running||a.waitTraveling)?25:250);
 }
}

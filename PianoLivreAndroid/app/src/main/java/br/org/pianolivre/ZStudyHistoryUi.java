package br.org.pianolivre;
import android.app.AlertDialog;
import android.os.SystemClock;
import android.widget.EditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Registro e exibição do histórico de estudo: abre/fecha janelas de tempo
 * com a partitura aberta (StudyClock -> StudyHistory) e os diálogos de
 * histórico, períodos e metas. PianoScreens e ZNotationFailures continuam
 * usando os delegados beginStudyWindow/finishStudyWindow/showHistory do
 * MainActivity. Comportamento idêntico à versão inline no MainActivity. */
final class ZStudyHistoryUi{
 private final MainActivity a;
 private String pendingHistoryError;
 ZStudyHistoryUi(MainActivity activity){a=activity;}
 void begin(){
  if(!a.foreground||a.score==null||a.currentFile.isEmpty()||a.screens==null||(!a.screens.isStudyOpen()&&!a.screens.isFreeOpen()))return;
  if(a.screens.isStudyOpen()&&(a.screens.notation==null||!a.screens.notation.isRendered()))return;
  if(a.screens.isFreeOpen()&&(a.screens.freeNotation==null||!a.screens.freeNotation.isRendered()))return;
  a.studyClock.start(SystemClock.uptimeMillis(),System.currentTimeMillis(),a.user,LibrarySearch.key(a.currentFile),
   a.currentName.isEmpty()?a.score.title:a.currentName,a.screens.isFreeOpen()?3:a.mode,a.currentHand,a.position,a.tempoFactor);
 }
 void finish(){
  StudyClock.Window window=a.studyClock.finish(SystemClock.uptimeMillis(),a.position,a.tempoFactor);
  if(window==null||a.prefs==null)return;
  try{new StudyHistory(a.prefs,window.user).record(window);}
  catch(RuntimeException ex){pendingHistoryError=ex.getMessage();android.util.Log.w("PianoLivreHistory","Período não registrado; dados anteriores mantidos");}
 }
 void show(){
  if(pendingHistoryError!=null){String error=pendingHistoryError;pendingHistoryError=null;a.message(error);return;}
  try{
   StudyHistory store=new StudyHistory(a.prefs,a.user);JSONObject data=store.snapshot(),pieces=data.getJSONObject("pieces");
   List<String> keys=new ArrayList<>();Iterator<String> iterator=pieces.keys();long total=0,periods=0,lastAt=0,weekMs=0;
   long weekCut=System.currentTimeMillis()-7*86400000L;
   JSONArray records=data.optJSONArray("records");
   if(records!=null)for(int i=0;i<records.length();i++){JSONObject record=records.getJSONObject(i);
    if(record.optLong("at")>=weekCut)weekMs=Math.addExact(weekMs,record.optLong("elapsedMs"));}
   while(iterator.hasNext()){String key=iterator.next();keys.add(key);JSONObject piece=pieces.getJSONObject(key);
    total=Math.addExact(total,piece.getLong("timeMs"));periods=Math.addExact(periods,piece.getLong("periods"));
    lastAt=Math.max(lastAt,piece.optLong("lastAt"));}
   keys.sort((left,right)->Long.compare(pieces.optJSONObject(right).optLong("lastAt"),pieces.optJSONObject(left).optLong("lastAt")));
   if(keys.isEmpty()){a.message("Ainda não há registros. O tempo é registrado quando uma partitura fica aberta em primeiro plano.");return;}
   java.text.DateFormat shortDate=java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT);
   String[] items=new String[keys.size()+1];items[0]="Tempo na tela de estudo: "+StudyHistory.duration(total)+" · "+periods+" períodos"
    +" · "+keys.size()+(keys.size()==1?" partitura":" partituras")
    +(lastAt>0?" · última prática "+shortDate.format(new Date(lastAt)):"")
    +(weekMs>0?" · últimos 7 dias: "+StudyHistory.duration(weekMs):"");
   for(int i=0;i<keys.size();i++){
    JSONObject piece=pieces.getJSONObject(keys.get(i));long goal=piece.getLong("goalMinutes"),time=piece.getLong("timeMs");
    items[i+1]=piece.getString("title")+"\n"+StudyHistory.duration(time)+(goal>0?" · meta: "+goal+" min"+(time>=goal*60000L?" ✓":""):"");
   }
   new AlertDialog.Builder(a).setTitle("Histórico deste perfil").setItems(items,(d,index)->{
    if(index==0){a.message("Tempo em partitura aberta, incluindo pausas para leitura. Não representa avaliação de notas, ritmo ou esforço.");return;}
    String key=keys.get(index-1);JSONObject piece=pieces.optJSONObject(key);EditText minutes=new EditText(a);
    minutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);minutes.setHint("Minutos escolhidos por você");
    if(piece.optInt("goalMinutes")>0)minutes.setText(String.valueOf(piece.optInt("goalMinutes")));
    new AlertDialog.Builder(a).setTitle("Meta de tempo · "+piece.optString("title")).setView(minutes)
     .setPositiveButton("Salvar",(dd,w)->{try{int value=Integer.parseInt(minutes.getText().toString().trim());if(value<=0)throw new NumberFormatException();store.goal(key,value);}catch(NumberFormatException ex){a.message("Informe um número inteiro positivo de minutos.");}catch(RuntimeException ex){a.message(ex.getMessage());}})
     .setNeutralButton("Remover meta",(dd,w)->{try{store.goal(key,0);}catch(RuntimeException ex){a.message(ex.getMessage());}}).setNegativeButton("Cancelar",null).show();
   }).setNeutralButton("Períodos",(d,w)->showPeriods(data.optJSONArray("records")))
    .setNegativeButton("Apagar histórico e metas",(d,w)->new AlertDialog.Builder(a).setTitle("Apagar histórico deste perfil?")
     .setMessage("Os períodos, o tempo acumulado e as metas serão removidos. Faça backup se quiser conservá-los. As partituras e os favoritos serão mantidos.")
     .setPositiveButton("Apagar",(dd,ww)->{try{store.clear();}catch(RuntimeException ex){a.message(ex.getMessage());}}).setNegativeButton("Cancelar",null).show())
    .setPositiveButton("Fechar",null).show();
  }catch(Exception ex){a.message("Não foi possível ler o histórico: "+ex.getMessage());}
 }
 void showPeriods(JSONArray records){
  try{
   List<String> labels=new ArrayList<>();String[] modes={"Espera","Contínuo","Demonstração","Leitura livre"};
   java.text.DateFormat date=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT);
   for(int i=records.length()-1;i>=0;i--){JSONObject r=records.getJSONObject(i);labels.add(date.format(new Date(r.getLong("at")))+" · "+r.getString("title")+"\n"+modes[r.getInt("mode")]+" · "+StudyHistory.duration(r.getLong("elapsedMs")));}
   new AlertDialog.Builder(a).setTitle("Períodos registrados").setItems(labels.toArray(new String[0]),(d,index)->{}).setPositiveButton("Fechar",null).show();
  }catch(Exception ex){a.message("Não foi possível ler os períodos");}
 }
}

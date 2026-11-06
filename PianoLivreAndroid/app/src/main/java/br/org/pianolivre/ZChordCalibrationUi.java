package br.org.pianolivre;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.TextView;

/** Diálogos e preferência da janela de simultaneidade (acordes): ajuste,
 * calibração guiada por amostras do pianista e remoção. A chave
 * user_N_chord_window_ms e a lógica de medição (ChordCalibration) permanecem
 * as mesmas; o objeto chordCalibration continua no MainActivity porque o
 * caminho MIDI (onKey/onMidiMessage) também o consulta. PianoScreens continua
 * usando o delegado a.showChordSettings(). Comportamento idêntico à versão
 * inline no MainActivity. */
final class ZChordCalibrationUi{
 private final MainActivity a;
 private TextView chordCalibrationStatus;
 ZChordCalibrationUi(MainActivity activity){a=activity;}
 /** Keep the current passage when changing the active musical hand. */
 void refreshWindow(){
  long ms=a.prefs.getLong("user_"+a.user+"_chord_window_ms",-1);
  a.waitRecognizer.setChordWindowMs(ms>0?ms:null);
 }
 void updateStatus(){
  if(chordCalibrationStatus!=null)
   chordCalibrationStatus.setText("Amostras: "+a.chordCalibration.sampleCount()+
    "\nJanela observada: "+(a.chordCalibration.observedWindowMs()==null?"aguardando acordes válidos":
    a.chordCalibration.observedWindowMs()+" ms")+
    "\nToque um acorde com pelo menos duas notas e solte todas as teclas. Repita em ritmos naturais.");
 }
 /** User-confirmed calibration; no preconfigured millisecond values. */
 void settings(){
  String current=a.waitRecognizer.chordWindowMs()==null?"Não calibrada":
   a.waitRecognizer.chordWindowMs()+" ms";
  String[] choices={"Calibrar tocando acordes","Ajustar janela calibrada","Remover calibração"};
  new AlertDialog.Builder(a).setTitle("Janela de acordes · "+current)
   .setItems(choices,(dlg,index)->{
    if(index==0)beginCalibration();
    else if(index==1){
     Long observed=a.waitRecognizer.chordWindowMs();
     if(observed==null)a.message("Toque primeiro os acordes de calibração. Nenhum número será presumido.");
     else editWindow(observed);
    }else{
     a.prefs.edit().remove("user_"+a.user+"_chord_window_ms").apply();
     a.waitRecognizer.setChordWindowMs(null);
     a.message("Calibração removida. Os acordes não terão limite temporal até uma nova medição.");
    }
   }).setNegativeButton("Fechar",null).show();
 }
 void beginCalibration(){
  if(!a.midiConnection.snapshot().canReceive){a.message("Conecte primeiro a entrada MIDI do instrumento.");return;}
  a.stopPlayback();
  a.chordCalibration.start();
  TextView body=new TextView(a);chordCalibrationStatus=body;
  body.setPadding(28,22,28,22);body.setTextSize(15);
  updateStatus();
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Calibrar simultaneidade")
   .setView(body).setPositiveButton("Concluir",null)
   .setNegativeButton("Cancelar",(d,w)->a.chordCalibration.stop()).create();
  dialog.setOnDismissListener(d->{a.chordCalibration.stop();chordCalibrationStatus=null;});
  dialog.show();
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   if(a.chordCalibration.sampleCount()<3){
    body.setText("Faça pelo menos três acordes completos, soltando todas as teclas entre eles.\n\n"+
     "Amostras válidas: "+a.chordCalibration.sampleCount());
    return;
   }
   Long measured=a.chordCalibration.observedWindowMs();
   if(measured==null){body.setText("Ainda não foi possível medir os intervalos. Experimente acordes distintos.");return;}
   dialog.dismiss();
   editWindow(measured);
  });
 }
 void editWindow(long measuredMs){
  EditText ms=new EditText(a);
  ms.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
  ms.setSingleLine(true);ms.setText(String.valueOf(measuredMs));
  ms.setSelectAllOnFocus(true);
  ms.setPadding(28,10,28,10);
  AlertDialog dlg=new AlertDialog.Builder(a).setTitle("Janela de simultaneidade")
   .setMessage("O valor inicial é o maior intervalo observado nos seus acordes, não uma tolerância presumida. Pode ajustá-lo em milissegundos.")
   .setView(ms).setPositiveButton("Salvar",null).setNegativeButton("Cancelar",null).create();
  dlg.show();
  dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   try{
    long value=Long.parseLong(ms.getText().toString().trim());
    if(value<=0||value>60000){ms.setError("Informe um valor positivo de até 60.000 ms.");return;}
    a.prefs.edit().putLong("user_"+a.user+"_chord_window_ms",value).apply();
    a.waitRecognizer.setChordWindowMs(value);
    if(a.mode==0&&a.score!=null)a.prepareWaiting();
    dlg.dismiss();a.message("Janela configurada: "+value+" ms. Notas extras continuam permitidas; duração e pedal são orientativos.");
   }catch(NumberFormatException invalid){ms.setError("Informe milissegundos em número inteiro.");}
  });
 }
}

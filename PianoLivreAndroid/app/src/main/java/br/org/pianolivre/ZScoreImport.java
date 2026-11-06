package br.org.pianolivre;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Bloco I (importação) da refatoração do MainActivity: seletor de documento
 * (importScore), diálogo de mão por trilha do MIDI importado (chooseMidiHands)
 * e o branch IMPORT de onActivityResult (handleImport). O despacho em
 * onActivityResult e o branch EXPORT permanecem no MainActivity; os delegados
 * a.importScore (PianoScreens) e a.chooseMidiHands (HistorySelfTest)
 * preservam as assinaturas originais. Comportamento idêntico à versão inline
 * no MainActivity. */
final class ZScoreImport{
 private final MainActivity a;
 ZScoreImport(MainActivity activity){a=activity;}
 void chooseMidiHands(MidiFileImport.Result midi,String name,List<Integer> tracks,int index,Map<Integer,Integer> hands){
  if(index<tracks.size()){
   int track=tracks.get(index);long count=midi.notes.stream().filter(n->n.track==track).count();
   new AlertDialog.Builder(a).setTitle("Trilha "+(track+1)+" · "+count+" notas")
    .setItems(new String[]{"Mão direita","Mão esquerda","Ignorar esta trilha"},(d,choice)->{
     hands.put(track,choice==0?1:choice==1?2:0);chooseMidiHands(midi,name,tracks,index+1,hands);
    }).setNegativeButton("Cancelar",null).show();return;
  }
  try{
   File dest=MidiImportStorage.save(new File(a.getFilesDir(),"imported"),name,midi,hands);
   a.loadScore(dest.getAbsolutePath(),true);
   a.message("MIDI importado como notação aproximada. Fonte original preservada; pedal e controladores não fazem parte da conversão.");
  }catch(Exception ex){a.message("Não foi possível importar o MIDI: "+ex.getMessage());}
 }
 void importScore(){Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("*/*");in.addCategory(Intent.CATEGORY_OPENABLE);a.startActivityForResult(in,MainActivity.IMPORT);}
 /** Corpo do branch IMPORT de onActivityResult; a exceção alcança o mesmo
  * catch ("Erro: ...") que envolvia o trecho inline. */
 void handleImport(Uri uri)throws Exception{
  String display="Importada.musicxml";
  try(android.database.Cursor cursor=a.getContentResolver().query(uri,null,null,null,null)){
   if(cursor!=null&&cursor.moveToFirst()){
    int idx=cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
    if(idx>=0&&cursor.getString(idx)!=null)display=cursor.getString(idx);
   }
  }
  if(display.toLowerCase(Locale.ROOT).endsWith(".mid")||display.toLowerCase(Locale.ROOT).endsWith(".midi")){
   MidiFileImport.Result midi;try(InputStream in=a.getContentResolver().openInputStream(uri)){midi=MidiFileImport.read(in);}
   final String midiName=display;
   new AlertDialog.Builder(a).setTitle("Importar MIDI como partitura aproximada")
    .setMessage("MIDI não contém a escrita original. A conversão usa compassos 4/4 e grafia aproximada; preserva tempos, ataques, durações e velocidades. Pedal, instrumentos, controladores, tonalidade e compassos originais não serão reproduzidos. O arquivo .mid original será conservado no backup. Escolha a mão de cada trilha; nenhuma mão será inferida pela altura.")
    .setPositiveButton("Escolher trilhas",(dialog,which)->chooseMidiHands(midi,midiName,midi.noteTracks(),0,new HashMap<>()))
    .setNegativeButton("Cancelar",null).show();
  }else{
   File dest;try(InputStream in=a.getContentResolver().openInputStream(uri)){dest=ScoreImport.save(new File(a.getFilesDir(),"imported"),display,in);}
   a.loadScore(dest.getAbsolutePath(),true);
  }
 }
}

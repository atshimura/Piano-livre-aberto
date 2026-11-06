package br.org.pianolivre;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Fluxo de restauração de backup: pedido do documento e tratamento do
 * resultado RESTORE de onActivityResult, além da verificação de composições
 * por perfil. A criação/exportação do arquivo de backup permanece no
 * MainActivity (literais cravados pelo contrato
 * test_creation_score_deletion_contract). Comportamento idêntico à versão
 * inline no MainActivity. */
final class ZBackupFlow{
 private ZBackupFlow(){}
 static void export(MainActivity a){
  Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/json");
  i.putExtra(Intent.EXTRA_TITLE,"pianolivre-backup.json");
  a.startActivityForResult(i,MainActivity.EXPORT);
 }
 static void restore(MainActivity a){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
  i.setType("application/json");
  a.startActivityForResult(i,MainActivity.RESTORE);
 }
 static boolean hasCreationDataForProfile(MainActivity a,int id)throws IOException{
  if(new CreationProjectStore(a.getFilesDir()).hasProjectsForProfile(id))return true;
  File[] journals=new File(a.getFilesDir(),"creation-journals").listFiles((d,n)->n.endsWith(".journal"));
  if(journals!=null)for(File file:journals)if(CreationJournal.read(file).profileId==id)return true;
  return false;
 }
 /** Trata o resultado RESTORE; devolve true somente para esse pedido.
  * Pedido cancelado permanece silencioso, como antes. */
 static boolean onActivityResult(MainActivity a,int request,int result,Intent data){
  if(request!=MainActivity.RESTORE)return false;
  if(result!=MainActivity.RESULT_OK||data==null||data.getData()==null)return true;
  Uri uri=data.getData();
  try{
   JSONObject backup;
   try(InputStream in=a.getContentResolver().openInputStream(uri)){
    String text=new String(BackupArchive.readLimited(in,95*1024*1024),StandardCharsets.UTF_8);
    JsonLimits.check(text,95*1024*1024,64);backup=new JSONObject(text);
   }
   String summary=BackupArchive.summary(backup);
   new AlertDialog.Builder(a).setTitle("Substituir dados locais?")
    .setMessage(summary+"\n\nPreferências e partituras serão substituídas; composições serão mescladas após validação. Preserve um backup do estado atual.")
    .setPositiveButton("Substituir",(dialog,which)->{
     a.stopPlayback();
     try{
      if(a.screens!=null)a.screens.prepareCreationBackup();
      BackupArchive.restore(a.prefs,a.getFilesDir(),backup);
      a.score=null;a.studyState.score=null;a.studyState.pressed.clear();
      a.position=0;a.currentFile="";a.currentName="";a.user=0;
      a.screens.showHome();a.loadProfiles();
      a.message("Backup restaurado e verificado. Os dados anteriores foram substituídos.");
     }catch(Exception restoreError){
      android.util.Log.e("PianoLivreBackup","Restauração não concluída",restoreError);
      a.message("Backup não restaurado: "+restoreError.getMessage()+". Seus dados anteriores foram preservados ou recuperados.");
     }
    }).setNegativeButton("Cancelar",null).show();
  }catch(Exception e){a.message("Erro: "+e.getMessage());}
  return true;
 }
}

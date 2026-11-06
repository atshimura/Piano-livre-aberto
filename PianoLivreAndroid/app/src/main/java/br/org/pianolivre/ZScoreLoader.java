package br.org.pianolivre;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Bloco I (carga) da refatoração do MainActivity: leitura da partitura
 * (loadScore 3-arg). O estado (score, currentFile, currentName,
 * route/routeCursor/routeHasMarkers, position, catalog) permanece no
 * MainActivity, acessado package-private. Dois trechos relacionados
 * permanecem na casca por exigência dos contratos: a atualização da barra
 * de tempo (a.applyTempoFactorToBar — contrato test_workflow_contract_audit)
 * e o método addCreationScoreToLibrary completo (contratos
 * test_creation_library_contract e test_impl4_osmd_contract cravam seus
 * literais no MainActivity). Comportamento idêntico à versão inline. */
final class ZScoreLoader{
 private final MainActivity a;
 ZScoreLoader(MainActivity activity){a=activity;}
 void loadScore(String path,boolean reset,boolean free){
  a.stopPlayback();a.finishStudyWindow();a.writtenRepeats.reset();
  try{path=SessionFiles.resolve(a.getFilesDir(),path);}catch(IllegalArgumentException ex){a.message(ex.getMessage());return;}
  CreationProjectStore.Project original=null;CreationScoreStore.Saved saved=null;
  try{
   if(path.startsWith("creation-score:")){
    String id=path.substring(15);original=new CreationProjectStore(a.getFilesDir()).get(id);
    if(original.profileId!=a.user||original.trash||original.recoverable)
     throw new IOException("Partitura não disponível para o perfil ativo");
    saved=new CreationScoreStore(a.getFilesDir()).read(id);
   }
   try(InputStream input=saved!=null
       ?new ByteArrayInputStream(saved.xml.getBytes(StandardCharsets.UTF_8))
       :path.startsWith("scores/")?a.getAssets().open(path):new FileInputStream(path)){
    a.score=Score.read(input,path);
    a.currentFile=path;a.currentName=original!=null?original.title:a.score.title;
  a.route=PerformanceRoute.build(a.score);
  a.routeCursor=a.route.supported()?new PerformanceRoute.Cursor(a.route):null;
  a.routeHasMarkers=false;
  for(Score.MeasureInfo m:a.score.measureInfo){
   if(m.forwardRepeat||m.backwardRepeat||!m.endings.isEmpty()||
     m.daCapo||m.dalSegno||m.segno||m.toCoda||m.coda||m.fine){
    a.routeHasMarkers=true;break;
   }
  }
  for(int i=0;i<a.catalog.length();i++){JSONObject entry=a.catalog.optJSONObject(i);if(entry!=null&&path.equals("scores/"+entry.optString("file"))){a.currentName=entry.optString("name",a.score.title);break;}}
  ZSessionState.Study restored=ZSessionState.readStudy(a.prefs,a.user);
  if(reset){
   a.position=a.score.firstOnset(a.currentHand);a.loopStart=0;a.loopEnd=Double.MAX_VALUE;
  }else{
   a.currentHand=restored.hand;a.mode=restored.mode;a.tempoFactor=restored.tempo;
   a.loopStart=restored.loopStart;a.loopEnd=restored.loopEnd;
   a.studyState.showKeyboard=restored.showKeyboard;
   a.position=ZSessionState.readPosition(a.prefs,a.user,a.score.firstOnset(a.currentHand));
  }
  a.position=Math.max(0,Math.min(a.position,a.score.end()));
  if(a.routeCursor!=null){
   if(reset||!a.routeCursor.restore(restored.routeVisit,a.position))
    a.routeCursor.align(a.position);
  }
  a.studyState.score=a.score;a.studyState.hand=a.currentHand;a.studyState.playhead=a.position;
  a.applyTempoFactorToBar();
  if(free){a.screens.showFree();}
  else{a.screens.showStudy();a.screens.displayScore(a.score,a.position);}
  a.updateInfo();a.saveSession();
  if(!a.score.importWarnings.isEmpty())a.message("Aviso de interpretação: "+String.join("; ",a.score.importWarnings));
  if(a.routeHasMarkers&&!a.route.supported())
   a.message("Atenção: não é possível determinar com segurança todos os retornos: "+
     String.join(", ",a.route.warnings)+". A reprodução desta partitura será linear.");
  }
 }catch(Exception e){
   final String retryPath=path;
   a.reportNotationIssue("PL-NOT-PARSE","Leitura MusicXML",e.getMessage(),null,
    ()->loadScore(retryPath,true,free));
  }}
}

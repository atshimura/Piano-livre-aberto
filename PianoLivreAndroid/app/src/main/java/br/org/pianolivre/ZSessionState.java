package br.org.pianolivre;
import android.content.SharedPreferences;

/** Sessão de estudo persistida por perfil (chaves user_N_*):
 * ponto único das chaves, defaults e limites históricos. A gravação acontece
 * via MainActivity.saveSession (delegado exigido pelo contrato
 * test_study_compact_chrome_contract) e as leituras via readStudy/readPosition.
 * As linhas de restauração em onCreate cravadas pelo contrato
 * test_workflow_contract_audit permanecem literais no MainActivity.
 * Mesmas invariantes aceitas por SessionPreferences na restauração de backup.
 * Nenhuma mudança de comportamento em relação à versão inline. */
final class ZSessionState{
 private ZSessionState(){}
 private static String prefix(int user){return "user_"+user+"_";}
 /** Última partitura aberta pelo perfil (caminho portátil; "" se nenhuma). */
 static String lastFile(SharedPreferences prefs,int user){
  return prefs.getString(prefix(user)+"file","");
 }
 static int activeProfile(SharedPreferences prefs){return prefs.getInt("active_profile",0);}
 static void setActiveProfile(SharedPreferences prefs,int id){prefs.edit().putInt("active_profile",id).apply();}
 /** Grava a sessão de estudo do perfil com exatamente as chaves históricas. */
 static void save(SharedPreferences prefs,int user,String currentFile,
   double position,int hand,int mode,double tempoFactor,double loopStart,
   double loopEnd,boolean showKeyboard,int routeVisit){
  String key=prefix(user);
  prefs.edit().putString(key+"file",SessionFiles.portable(currentFile))
   .putLong(key+"position",Double.doubleToLongBits(position))
   .putInt(key+"hand",hand).putInt(key+"mode",mode)
   .putLong(key+"tempo",Double.doubleToLongBits(tempoFactor))
   .putLong(key+"loop_start",Double.doubleToLongBits(loopStart))
   .putLong(key+"loop_end",Double.doubleToLongBits(loopEnd))
   .putBoolean(key+"keyboard",showKeyboard)
   .putInt(key+"route_visit",routeVisit).apply();
 }
 /** Instantâneo imutável das preferências de estudo do perfil. */
 static final class Study{
  final int hand,mode,routeVisit;
  final double tempo,loopStart,loopEnd;
  final boolean showKeyboard;
  Study(int hand,int mode,int routeVisit,double tempo,double loopStart,
    double loopEnd,boolean showKeyboard){
   this.hand=hand;this.mode=mode;this.routeVisit=routeVisit;this.tempo=tempo;
   this.loopStart=loopStart;this.loopEnd=loopEnd;this.showKeyboard=showKeyboard;
  }
 }
 /** Preferências de estudo do perfil com os limites históricos de loadScore:
  * tempo entre 0,25 e 2 (padrão 1,0), mão/modo entre 0 e 2, laço validado
  * (finito, início ≥ 0, fim > início) e route_visit bruto (-1 se ausente). */
 static Study readStudy(SharedPreferences prefs,int user){
  String prefix=prefix(user);
  int hand=Math.max(0,Math.min(2,prefs.getInt(prefix+"hand",0)));
  int mode=Math.max(0,Math.min(2,prefs.getInt(prefix+"mode",0)));
  double tempo=Double.longBitsToDouble(prefs.getLong(prefix+"tempo",
   Double.doubleToLongBits(1.0)));
  if(!Double.isFinite(tempo))tempo=1.0;
  tempo=Math.max(.25,Math.min(2,tempo));
  double loopStart=Double.longBitsToDouble(prefs.getLong(prefix+"loop_start",
   Double.doubleToLongBits(0)));
  double loopEnd=Double.longBitsToDouble(prefs.getLong(prefix+"loop_end",
   Double.doubleToLongBits(Double.MAX_VALUE)));
  if(!Double.isFinite(loopStart)||loopStart<0||!Double.isFinite(loopEnd)
     ||loopEnd<=loopStart){loopStart=0;loopEnd=Double.MAX_VALUE;}
  boolean showKeyboard=prefs.getBoolean(prefix+"keyboard",false);
  int routeVisit=prefs.getInt(prefix+"route_visit",-1);
  return new Study(hand,mode,routeVisit,tempo,loopStart,loopEnd,showKeyboard);
 }
 /** Posição persistida do perfil; fallback é o primeiro ataque da mão. */
 static double readPosition(SharedPreferences prefs,int user,double fallback){
  double position=Double.longBitsToDouble(prefs.getLong(prefix(user)+"position",
   Double.doubleToLongBits(fallback)));
  return Double.isFinite(position)?position:fallback;
 }
}

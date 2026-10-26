package br.org.pianolivre;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Bounded local metadata. Never trims old records automatically. */
final class StudyHistory {
 private static final String EMPTY="{\"version\":1,\"records\":[],\"pieces\":{}}";
 private final SharedPreferences prefs;private final String key;private final int user;
 StudyHistory(SharedPreferences prefs,int user){this.prefs=prefs;this.user=user;key="user_"+user+"_history";}
 private static long integer(JSONObject o,String key,long minimum)throws JSONException{
  Object value=o.get(key);if(!(value instanceof Integer||value instanceof Long)||((Number)value).longValue()<minimum)throw new IllegalArgumentException("Valor inteiro inválido no histórico");return ((Number)value).longValue();
 }
 private static double real(JSONObject o,String key,double minimum)throws JSONException{
  Object value=o.get(key);if(!(value instanceof Number)||!Double.isFinite(((Number)value).doubleValue())||((Number)value).doubleValue()<minimum)throw new IllegalArgumentException("Valor inválido no histórico");return ((Number)value).doubleValue();
 }
 private static void title(JSONObject o)throws JSONException{Object t=o.get("title");if(!(t instanceof String)||((String)t).codePointCount(0,((String)t).length())>200)throw new IllegalArgumentException("Título inválido no histórico");}
 static JSONObject decode(String text){
  try{
   if(text.length()>128*1024)throw new IllegalArgumentException("O histórico está cheio. Faça backup antes de limpá-lo para continuar registrando.");
   JsonLimits.check(text,128*1024,16);JSONObject root=new JSONObject(text);if(integer(root,"version",1)!=1)throw new IllegalArgumentException("Versão de histórico incompatível");
   JSONArray records=root.getJSONArray("records");JSONObject pieces=root.getJSONObject("pieces");
   if(records.length()>2000||pieces.length()>2000)throw new IllegalArgumentException("Histórico grande demais");
   for(int i=0;i<records.length();i++){
    JSONObject r=records.getJSONObject(i);LibraryStore.validateScoreKey(r.getString("score"));title(r);
    integer(r,"at",0);integer(r,"elapsedMs",1);
    if(integer(r,"mode",0)>3||integer(r,"hand",0)>2)throw new IllegalArgumentException("Modo de histórico inválido");
    real(r,"fromBeat",0);real(r,"toBeat",0);if(real(r,"startSpeed",0)<=0||real(r,"endSpeed",0)<=0)throw new IllegalArgumentException("Velocidade inválida");
   }
   Iterator<String> it=pieces.keys();while(it.hasNext()){
    String score=it.next();LibraryStore.validateScoreKey(score);JSONObject p=pieces.getJSONObject(score);title(p);
    integer(p,"timeMs",0);integer(p,"periods",0);integer(p,"lastAt",0);real(p,"lastBeat",0);
    if(integer(p,"goalMinutes",0)>Integer.MAX_VALUE)throw new IllegalArgumentException("Meta grande demais");
   }
   return root;
  }catch(JSONException ex){throw new IllegalArgumentException("Histórico inválido",ex);}
 }
 JSONObject snapshot(){return decode(prefs.getString(key,EMPTY));}
 private void save(JSONObject root){
  String value=root.toString();decode(value);String old=prefs.getString(key,null);
  if(!prefs.edit().putString(key,value).commit()){
   SharedPreferences.Editor ed=prefs.edit();if(old==null)ed.remove(key);else ed.putString(key,old);ed.commit();
   throw new IllegalStateException("Não foi possível salvar o histórico; registros anteriores mantidos");
  }
 }
 private static String label(String title){return title.codePointCount(0,title.length())>200?title.substring(0,title.offsetByCodePoints(0,200)):title;}
 void record(StudyClock.Window window){
  if(window==null||window.elapsedMs<=0)return;
  if(window.user!=user)throw new IllegalArgumentException("Perfil incompatível com o período de estudo");
  try{
   JSONObject root=snapshot(),pieces=root.getJSONObject("pieces");String title=label(window.title);
   JSONObject prior=pieces.optJSONObject(window.score);
   if(prior==null)prior=new JSONObject().put("title",title).put("timeMs",0L).put("periods",0L).put("lastAt",0L).put("lastBeat",0.0).put("goalMinutes",0);
   long total=Math.addExact(prior.getLong("timeMs"),window.elapsedMs),periods=Math.addExact(prior.getLong("periods"),1);
   prior.put("title",title).put("timeMs",total).put("periods",periods).put("lastAt",window.at).put("lastBeat",window.toBeat);
   pieces.put(window.score,prior);
   root.getJSONArray("records").put(new JSONObject().put("score",window.score).put("title",title)
    .put("at",window.at).put("elapsedMs",window.elapsedMs).put("mode",window.mode).put("hand",window.hand)
    .put("fromBeat",window.fromBeat).put("toBeat",window.toBeat).put("startSpeed",window.startSpeed).put("endSpeed",window.endSpeed));
   save(root);
  }catch(JSONException|ArithmeticException ex){throw new IllegalArgumentException("Não foi possível registrar este período",ex);}
 }
 void goal(String score,int minutes){
  if(minutes<0)throw new IllegalArgumentException("Meta deve ser positiva ou removida");
  try{JSONObject root=snapshot(),pieces=root.getJSONObject("pieces");JSONObject piece=pieces.optJSONObject(score);
   if(piece==null)throw new IllegalArgumentException("Abra esta partitura antes de definir a meta");piece.put("goalMinutes",minutes);save(root);
  }catch(JSONException ex){throw new IllegalArgumentException(ex);}
 }
 void clear(){String old=prefs.getString(key,null);if(!prefs.edit().remove(key).commit()){
  if(old!=null)prefs.edit().putString(key,old).commit();throw new IllegalStateException("Não foi possível limpar o histórico");
 }}
 static String duration(long ms){long s=Math.max(0,ms)/1000;return (s/60)+" min "+(s%60)+" s";}
}

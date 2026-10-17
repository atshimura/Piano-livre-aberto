package br.org.pianolivre;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Stable profile identity. Legacy array indices retain their existing user_ID keys. */
final class LocalProfiles {
 static final String[] AVATARS={"♫","🎹","♪","★"};
 static final class Profile {
  final int id,avatar;final String name;
  Profile(int id,String name,int avatar){this.id=id;this.name=name;this.avatar=avatar;}
 }
 private final SharedPreferences prefs;
 LocalProfiles(SharedPreferences prefs){this.prefs=prefs;}
 static List<Profile> decode(String json){
  try{
   JsonLimits.check(json,64*1024,16);JSONArray array=new JSONArray(json);List<Profile> out=new ArrayList<>();Set<Integer> ids=new HashSet<>();
   if(array.length()<1||array.length()>20)throw new IllegalArgumentException("Quantidade inválida de perfis");
   for(int i=0;i<array.length();i++){
    JSONObject item=array.getJSONObject(i);Object raw=item.get("id");
    if(!(raw instanceof Number))throw new IllegalArgumentException("Identificador inválido");
    long id=((Number)raw).longValue();
    if(id<0||id>=Integer.MAX_VALUE||((Number)raw).doubleValue()!=id||!ids.add((int)id))throw new IllegalArgumentException("Identificador duplicado ou inválido");
    Object rawName=item.get("name"),rawAvatar=item.get("avatar");
    if(!(rawName instanceof String)||!(rawAvatar instanceof Number))throw new IllegalArgumentException("Nome ou avatar inválido");
    String name=((String)rawName).trim();int avatar=((Number)rawAvatar).intValue();
    if(((Number)rawAvatar).doubleValue()!=avatar)throw new IllegalArgumentException("Avatar inválido");
    if(name.isEmpty()||avatar<0||avatar>=AVATARS.length)throw new IllegalArgumentException("Nome ou avatar inválido");
    out.add(new Profile((int)id,name,avatar));
   }
   return Collections.unmodifiableList(out);
  }catch(JSONException ex){throw new IllegalArgumentException("Perfis inválidos",ex);}
 }
 List<Profile> list(){
  if(prefs.contains("profiles_v2"))return decode(prefs.getString("profiles_v2",""));
  try{
   JSONArray names=new JSONArray(prefs.getString("profiles","[\"Usuário 1\"]"));JSONArray converted=new JSONArray();
   for(int i=0;i<names.length();i++)converted.put(new JSONObject().put("id",i).put("name",names.getString(i)).put("avatar",0));
   return decode(converted.toString());
  }catch(JSONException ex){throw new IllegalArgumentException("Perfis antigos inválidos",ex);}
 }
 int create(String name){
  List<Profile> profiles=new ArrayList<>(list());if(profiles.size()>=20)throw new IllegalArgumentException("Limite de 20 perfis");
  int next=prefs.getInt("profiles_next_id",0);
  for(Profile p:profiles)next=Math.max(next,p.id+1);
  for(String key:prefs.getAll().keySet())if(key.startsWith("user_")){
   int end=key.indexOf('_',5);if(end>5)try{
    long prior=Long.parseLong(key.substring(5,end));
    if(prior>=0&&prior<Integer.MAX_VALUE)next=Math.max(next,(int)prior+1);
   }catch(NumberFormatException ignored){}
  }
  if(next>=Integer.MAX_VALUE-1)throw new IllegalArgumentException("Limite técnico de identificadores");
  profiles.add(new Profile(next,name.trim(),0));write(profiles,null);return next;
 }
 void update(int id,String name,int avatar){
  List<Profile> out=new ArrayList<>();boolean found=false;
  for(Profile p:list()){if(p.id==id){out.add(new Profile(id,name.trim(),avatar));found=true;}else out.add(p);}
  if(!found)throw new IllegalArgumentException("Perfil não encontrado");write(out,null);
 }
 int delete(int id){
  List<Profile> out=new ArrayList<>();boolean found=false;
  for(Profile p:list())if(p.id==id)found=true;else out.add(p);
  if(!found||out.isEmpty())throw new IllegalArgumentException("É necessário manter pelo menos um perfil");
  write(out,id);return prefs.getInt("active_profile",out.get(0).id);
 }
 private void write(List<Profile> profiles,Integer removed){
  try{
   JSONArray records=new JSONArray(),legacy=new JSONArray();int next=prefs.getInt("profiles_next_id",0);
   for(Profile p:profiles){records.put(new JSONObject().put("id",p.id).put("name",p.name).put("avatar",p.avatar));legacy.put(p.name);next=Math.max(next,p.id+1);}
   if(removed!=null)next=Math.max(next,removed+1);
   decode(records.toString()); // All validation precedes any mutation.
   Map<String,?> before=prefs.getAll();SharedPreferences.Editor editor=prefs.edit();
   editor.putString("profiles_v2",records.toString()).putString("profiles",legacy.toString()).putInt("profiles_next_id",next);
   if(removed!=null){
    String prefix="user_"+removed+"_";for(String key:before.keySet())if(key.startsWith(prefix))editor.remove(key);
    int active=prefs.getInt("active_profile",profiles.get(0).id);
    if(active==removed)editor.putInt("active_profile",profiles.get(0).id);
   }
   if(!editor.commit()){
    restore(before);throw new IllegalStateException("Não foi possível salvar os perfis");
   }
  }catch(JSONException ex){throw new IllegalArgumentException("Perfis inválidos",ex);}
 }
 @SuppressWarnings("unchecked") private void restore(Map<String,?> values){
  SharedPreferences.Editor ed=prefs.edit().clear();
  for(Map.Entry<String,?> e:values.entrySet()){
   Object v=e.getValue();String k=e.getKey();
   if(v instanceof String)ed.putString(k,(String)v);else if(v instanceof Integer)ed.putInt(k,(Integer)v);
   else if(v instanceof Long)ed.putLong(k,(Long)v);else if(v instanceof Boolean)ed.putBoolean(k,(Boolean)v);
   else if(v instanceof Float)ed.putFloat(k,(Float)v);else if(v instanceof Set)ed.putStringSet(k,(Set<String>)v);
  }
  ed.commit();
 }
}

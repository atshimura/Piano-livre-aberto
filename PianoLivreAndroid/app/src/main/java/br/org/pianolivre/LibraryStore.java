package br.org.pianolivre;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Local per-profile library metadata; score files remain shared and untouched. */
final class LibraryStore {
 static final class Collection {
  final String id,name;final Set<String> scores;
  Collection(String id,String name,Set<String> scores){this.id=id;this.name=name;this.scores=Collections.unmodifiableSet(scores);}
 }
 static final class Snapshot {
  final Set<String> favorites,addedCreations,hiddenScores;final Map<String,Collection> collections;
  Snapshot(Set<String> f,Map<String,Collection> c,Set<String> added,Set<String> hidden){
   favorites=Collections.unmodifiableSet(f);collections=Collections.unmodifiableMap(c);
   addedCreations=Collections.unmodifiableSet(added);
   hiddenScores=Collections.unmodifiableSet(hidden);
  }
 }
 private final SharedPreferences prefs;private final String key;private final int user;
 LibraryStore(SharedPreferences prefs,int user){this.prefs=prefs;this.user=user;key="user_"+user+"_library";}
 static void validateScoreKey(String key){
  if(key!=null&&key.startsWith("creation-score:")){validateCreationId(key.substring(15));return;}
  if(key==null||key.length()>264||key.indexOf('\u0000')>=0||!(key.startsWith("scores/")||key.startsWith("imported/")))throw new IllegalArgumentException("Partitura inválida");
  String name=key.substring(key.indexOf('/')+1);
  if(name.isEmpty()||name.equals(".")||name.equals("..")||name.indexOf('/')>=0||name.indexOf('\\')>=0)throw new IllegalArgumentException("Partitura inválida");
 }
 static void validateCreationId(String id){
  if(id==null||!id.matches("[0-9a-fA-F-]{36}"))throw new IllegalArgumentException("Identificador de partitura gerada inválido");
 }
 private static Set<String> creationIds(JSONArray data)throws JSONException{
  if(data.length()>CreationProjectStore.MAX_PROJECTS)throw new IllegalArgumentException("Muitas partituras geradas na biblioteca");
  Set<String> ids=new LinkedHashSet<>();
  for(int i=0;i<data.length();i++){
   String id=data.getString(i);validateCreationId(id);
   if(!ids.add(id.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Partitura gerada duplicada");
  }
  return ids;
 }
 private static Set<String> keys(JSONArray array)throws JSONException{
  if(array.length()>256)throw new IllegalArgumentException("Muitas partituras na organização");
  Set<String> out=new LinkedHashSet<>();
  for(int i=0;i<array.length();i++){String key=array.getString(i);validateScoreKey(key);if(!out.add(key))throw new IllegalArgumentException("Partitura duplicada");}return out;
 }
 static Snapshot decode(String text){
  try{
   if(text.length()>256*1024)throw new IllegalArgumentException("Organização da biblioteca grande demais");
   JsonLimits.check(text,256*1024,16);JSONObject root=new JSONObject(text);Object version=root.get("version");
   if(!(version instanceof Number)||((Number)version).doubleValue()!=1)throw new IllegalArgumentException("Versão de biblioteca incompatível");
   Set<String> favorites=keys(root.getJSONArray("favorites"));JSONArray array=root.getJSONArray("collections");
   if(array.length()>40)throw new IllegalArgumentException("Limite de 40 coleções");
   Map<String,Collection> collections=new LinkedHashMap<>();Set<String> names=new HashSet<>();
   for(int i=0;i<array.length();i++){
    JSONObject item=array.getJSONObject(i);String id=item.getString("id"),name=item.getString("name").trim();
    if(id.isEmpty()||id.length()>80||id.equals("*")||name.isEmpty()||name.length()>120||!names.add(name.toLowerCase(Locale.ROOT))||collections.containsKey(id))throw new IllegalArgumentException("Coleção inválida ou duplicada");
    collections.put(id,new Collection(id,name,keys(item.getJSONArray("scores"))));
   }
   Set<String> added=creationIds(root.has("addedCreations")?root.getJSONArray("addedCreations"):new JSONArray());
   Set<String> hidden=keys(root.has("hiddenScores")?root.getJSONArray("hiddenScores"):new JSONArray());
   for(String score:hidden)if(!score.startsWith("scores/"))throw new IllegalArgumentException("Somente partituras do aplicativo podem ser ocultadas");
   return new Snapshot(favorites,collections,added,hidden);
  }catch(JSONException ex){throw new IllegalArgumentException("Organização da biblioteca inválida",ex);}
 }
 private JSONObject read(){
  try{String text=prefs.getString(key,"{\"version\":1,\"favorites\":[],\"collections\":[]}");decode(text);return new JSONObject(text);}
  catch(JSONException ex){throw new IllegalArgumentException("Biblioteca inválida",ex);}
 }
 Snapshot snapshot(){return decode(prefs.getString(key,"{\"version\":1,\"favorites\":[],\"collections\":[]}"));}
 private void save(JSONObject data){save(data,null);}
 private void save(JSONObject data,String detachedScore){
  String text=data.toString();decode(text);String previous=prefs.getString(key,null);
  String sessionKey="user_"+user+"_file",priorSession=prefs.getString(sessionKey,"");
  boolean clearSession=detachedScore!=null&&!priorSession.isEmpty()&&detachedScore.equals(SessionFiles.portable(priorSession));
  SharedPreferences.Editor editor=prefs.edit().putString(key,text);
  if(clearSession)editor.remove(sessionKey);
  if(!editor.commit()){
   SharedPreferences.Editor rollback=prefs.edit();if(previous==null)rollback.remove(key);else rollback.putString(key,previous);
   if(clearSession)rollback.putString(sessionKey,priorSession);rollback.commit();
   throw new IllegalStateException("Não foi possível salvar a biblioteca");
  }
 }
 private static JSONArray toggled(JSONArray array,String score)throws JSONException{
  validateScoreKey(score);JSONArray out=new JSONArray();boolean found=false;
  for(int i=0;i<array.length();i++){String item=array.getString(i);if(item.equals(score))found=true;else out.put(item);}
  if(!found)out.put(score);return out;
 }
 void toggleFavorite(String score){try{JSONObject obj=read();obj.put("favorites",toggled(obj.getJSONArray("favorites"),score));save(obj);}catch(JSONException ex){throw new IllegalArgumentException(ex);}}
 boolean hasCreation(String projectId){validateCreationId(projectId);return snapshot().addedCreations.contains(projectId.toLowerCase(Locale.ROOT));}
 void addCreation(String projectId){
  validateCreationId(projectId);projectId=projectId.toLowerCase(Locale.ROOT);
  try{
   JSONObject obj=read();
   JSONArray previous=obj.optJSONArray("addedCreations");
   if(previous==null)previous=new JSONArray();
   if(creationIds(previous).contains(projectId))return; // idempotent, no duplicates
   JSONArray updated=new JSONArray(previous.toString());updated.put(projectId);
   obj.put("addedCreations",updated);save(obj);
  }catch(JSONException ex){throw new IllegalArgumentException(ex);}
 }
 private static JSONArray removed(JSONArray source,String target)throws JSONException{
  JSONArray out=new JSONArray();
  for(int i=0;i<source.length();i++){String value=source.getString(i);if(!value.equals(target))out.put(value);}
  return out;
 }
 /** Purge stale library memberships, while retaining historic practice records. */
 private static boolean detach(JSONObject obj,String score)throws JSONException{
  boolean changed=false;
  JSONArray favorites=obj.getJSONArray("favorites"),kept=removed(favorites,score);
  if(kept.length()!=favorites.length()){obj.put("favorites",kept);changed=true;}
  JSONArray collections=obj.getJSONArray("collections");
  for(int i=0;i<collections.length();i++){
   JSONObject collection=collections.getJSONObject(i);
   JSONArray members=collection.getJSONArray("scores"),filtered=removed(members,score);
   if(filtered.length()!=members.length()){collection.put("scores",filtered);changed=true;}
  }
  return changed;
 }
 /** Built-in APK assets are immutable: hide in a single profile only. */
 void setHidden(String score,boolean hidden){
  validateScoreKey(score);if(!score.startsWith("scores/"))throw new IllegalArgumentException("Somente partituras do aplicativo podem ser ocultadas");
  try{
   JSONObject obj=read();JSONArray source=obj.optJSONArray("hiddenScores");
   if(source==null)source=new JSONArray();
   Set<String> current=keys(source);if(current.contains(score)==hidden)return;
   JSONArray next=removed(source,score);if(hidden)next.put(score);
   obj.put("hiddenScores",next);save(obj,hidden?score:null);
  }catch(JSONException ex){throw new IllegalArgumentException("Não foi possível atualizar músicas ocultas",ex);}
 }
 /** Unlink a created score but keep its source recording and derived MusicXML. */
 void removeCreation(String projectId){
  validateCreationId(projectId);projectId=projectId.toLowerCase(Locale.ROOT);
  try{
   JSONObject obj=read();JSONArray before=obj.optJSONArray("addedCreations");
   if(before==null)before=new JSONArray();
   JSONArray after=removed(before,projectId);obj.put("addedCreations",after);
   detach(obj,"creation-score:"+projectId);
   save(obj,"creation-score:"+projectId);
  }catch(JSONException ex){throw new IllegalArgumentException("Não foi possível remover partitura criada",ex);}
 }
 /** Imported music is shared: clean all profiles in one atomic prefs transaction. */
 static void removeImportedEveryProfile(SharedPreferences prefs,String score){
  validateScoreKey(score);
  if(!score.startsWith("imported/")||!score.equals(SessionFiles.portable(score)))throw new IllegalArgumentException("Partitura importada inválida");
  Map<String,?> all=prefs.getAll();Map<String,String> originals=new LinkedHashMap<>();
  SharedPreferences.Editor editor=prefs.edit();
  try{
   for(Map.Entry<String,?> item:all.entrySet()){
    String preference=item.getKey();Object raw=item.getValue();
    if(preference.matches("user_\\d+_library")){
     if(!(raw instanceof String))throw new IllegalArgumentException("Biblioteca de perfil inválida");
     JSONObject obj=new JSONObject((String)raw);decode((String)raw);
     if(detach(obj,score)){originals.put(preference,(String)raw);editor.putString(preference,obj.toString());}
    }else if(preference.matches("user_\\d+_file")){
     if(!(raw instanceof String))throw new IllegalArgumentException("Sessão de perfil inválida");
     String stored=(String)raw;
     if(!stored.isEmpty()&&score.equals(SessionFiles.portable(stored))){originals.put(preference,stored);editor.remove(preference);}
    }
   }
   if(originals.isEmpty())return;
   if(!editor.commit()){
    SharedPreferences.Editor rollback=prefs.edit();
    for(Map.Entry<String,String> item:originals.entrySet())rollback.putString(item.getKey(),item.getValue());
    rollback.commit();throw new IllegalStateException("Não foi possível atualizar bibliotecas dos perfis");
   }
  }catch(JSONException ex){throw new IllegalArgumentException("Organização da biblioteca inválida",ex);}
 }
 String create(String name){
  try{JSONObject obj=read();String id=UUID.randomUUID().toString();obj.getJSONArray("collections").put(new JSONObject().put("id",id).put("name",name.trim()).put("scores",new JSONArray()));save(obj);return id;}
  catch(JSONException ex){throw new IllegalArgumentException(ex);}
 }
 void rename(String id,String name){change(id,name,null,false);}
 void delete(String id){change(id,null,null,true);}
 void toggleMember(String id,String score){change(id,null,score,false);}
 private void change(String id,String name,String score,boolean remove){
  try{JSONObject obj=read();JSONArray source=obj.getJSONArray("collections"),out=new JSONArray();boolean found=false;
   for(int i=0;i<source.length();i++){
    JSONObject item=source.getJSONObject(i);
    if(item.getString("id").equals(id)){
     found=true;if(remove)continue;if(name!=null)item.put("name",name.trim());
     if(score!=null)item.put("scores",toggled(item.getJSONArray("scores"),score));
    }
    out.put(item);
   }
   if(!found)throw new IllegalArgumentException("Coleção não encontrada");obj.put("collections",out);save(obj);
  }catch(JSONException ex){throw new IllegalArgumentException(ex);}
 }
}

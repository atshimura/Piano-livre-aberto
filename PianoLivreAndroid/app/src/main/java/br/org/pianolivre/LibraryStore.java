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
  final Set<String> favorites;final Map<String,Collection> collections;
  Snapshot(Set<String> f,Map<String,Collection> c){favorites=Collections.unmodifiableSet(f);collections=Collections.unmodifiableMap(c);}
 }
 private final SharedPreferences prefs;private final String key;
 LibraryStore(SharedPreferences prefs,int user){this.prefs=prefs;key="user_"+user+"_library";}
 private static void scoreKey(String key){
  if(key==null||key.length()>160||key.indexOf('\u0000')>=0||!(key.startsWith("scores/")||key.startsWith("imported/")))throw new IllegalArgumentException("Partitura inválida");
  String name=key.substring(key.indexOf('/')+1);
  if(name.isEmpty()||name.equals(".")||name.equals("..")||name.indexOf('/')>=0||name.indexOf('\\')>=0)throw new IllegalArgumentException("Partitura inválida");
 }
 private static Set<String> keys(JSONArray array)throws JSONException{
  if(array.length()>256)throw new IllegalArgumentException("Muitas partituras na organização");
  Set<String> out=new LinkedHashSet<>();
  for(int i=0;i<array.length();i++){String key=array.getString(i);scoreKey(key);if(!out.add(key))throw new IllegalArgumentException("Partitura duplicada");}return out;
 }
 static Snapshot decode(String text){
  try{
   if(text.length()>256*1024)throw new IllegalArgumentException("Organização da biblioteca grande demais");
   JSONObject root=new JSONObject(text);Object version=root.get("version");
   if(!(version instanceof Number)||((Number)version).doubleValue()!=1)throw new IllegalArgumentException("Versão de biblioteca incompatível");
   Set<String> favorites=keys(root.getJSONArray("favorites"));JSONArray array=root.getJSONArray("collections");
   if(array.length()>40)throw new IllegalArgumentException("Limite de 40 coleções");
   Map<String,Collection> collections=new LinkedHashMap<>();Set<String> names=new HashSet<>();
   for(int i=0;i<array.length();i++){
    JSONObject item=array.getJSONObject(i);String id=item.getString("id"),name=item.getString("name").trim();
    if(id.isEmpty()||id.length()>80||id.equals("*")||name.isEmpty()||name.length()>120||!names.add(name.toLowerCase(Locale.ROOT))||collections.containsKey(id))throw new IllegalArgumentException("Coleção inválida ou duplicada");
    collections.put(id,new Collection(id,name,keys(item.getJSONArray("scores"))));
   }
   return new Snapshot(favorites,collections);
  }catch(JSONException ex){throw new IllegalArgumentException("Organização da biblioteca inválida",ex);}
 }
 private JSONObject read(){
  try{String text=prefs.getString(key,"{\"version\":1,\"favorites\":[],\"collections\":[]}");decode(text);return new JSONObject(text);}
  catch(JSONException ex){throw new IllegalArgumentException("Biblioteca inválida",ex);}
 }
 Snapshot snapshot(){return decode(prefs.getString(key,"{\"version\":1,\"favorites\":[],\"collections\":[]}"));}
 private void save(JSONObject data){
  String text=data.toString();decode(text);String previous=prefs.getString(key,null);
  if(!prefs.edit().putString(key,text).commit()){
   SharedPreferences.Editor rollback=prefs.edit();if(previous==null)rollback.remove(key);else rollback.putString(key,previous);rollback.commit();
   throw new IllegalStateException("Não foi possível salvar a biblioteca");
  }
 }
 private static JSONArray toggled(JSONArray array,String score)throws JSONException{
  scoreKey(score);JSONArray out=new JSONArray();boolean found=false;
  for(int i=0;i<array.length();i++){String item=array.getString(i);if(item.equals(score))found=true;else out.put(item);}
  if(!found)out.put(score);return out;
 }
 void toggleFavorite(String score){try{JSONObject obj=read();obj.put("favorites",toggled(obj.getJSONArray("favorites"),score));save(obj);}catch(JSONException ex){throw new IllegalArgumentException(ex);}}
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

package br.org.pianolivre;
import java.util.*;
/** Preflight known preference types before a restore replaces current data. */
public final class SessionPreferences {
 private SessionPreferences(){}
 public static void validate(Map<String,Object> values){
  for(Map.Entry<String,Object> item:values.entrySet()){
   String key=item.getKey();Object value=item.getValue();
   if(value instanceof Float&&!Float.isFinite((Float)value))throw new IllegalArgumentException("Preferência decimal inválida");
   if(key.matches("user_\\d+_(mode|hand|route_visit)")){
    if(!(value instanceof Integer))throw new IllegalArgumentException("Preferência inteira inválida");int n=(Integer)value;
    if(key.endsWith("route_visit")?n< -1:n<0||n>2)throw new IllegalArgumentException("Modo/mão/retorno inválido");
   }else if(key.matches("user_\\d+_(position|tempo|loop_start|loop_end)")){
    if(!(value instanceof Long))throw new IllegalArgumentException("Posição/velocidade sem tipo long");double d=Double.longBitsToDouble((Long)value);
    if(!Double.isFinite(d)||d<0||(key.endsWith("tempo")&&d<=0))throw new IllegalArgumentException("Posição/velocidade inválida");
   }else if(key.matches("user_\\d+_keyboard")&&!(value instanceof Boolean))throw new IllegalArgumentException("Estado do teclado inválido");
   else if(key.matches("user_\\d+_file")){
    if(!(value instanceof String))throw new IllegalArgumentException("Partitura sem caminho textual");item.setValue(SessionFiles.portable((String)value));
   }
  }
 }
}

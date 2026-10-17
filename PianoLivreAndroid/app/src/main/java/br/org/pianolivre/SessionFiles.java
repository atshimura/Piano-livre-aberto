package br.org.pianolivre;
import java.io.*;
/** Portable sessions, including manual backups from another app data directory. */
public final class SessionFiles {
 private SessionFiles(){}
 public static String portable(String path){
  if(path==null)throw new IllegalArgumentException("Caminho de partitura inválido");if(path.isEmpty())return path;
  String prefix,name;
  if(path.startsWith("scores/")){prefix="scores/";name=path.substring(7);}
  else if(path.startsWith("imported/")){prefix="imported/";name=path.substring(9);}
  else if(path.startsWith("/")&&path.contains("/imported/")){prefix="imported/";name=path.substring(path.lastIndexOf("/imported/")+10);}
  else throw new IllegalArgumentException("Caminho externo de partitura não suportado");
  if(name.isEmpty()||name.length()>255||name.equals(".")||name.equals("..")||!name.matches("[A-Za-z0-9._-]+"))throw new IllegalArgumentException("Nome de partitura inválido");
  return prefix+name;
 }
 public static String resolve(File root,String path){String key=portable(path);return key.startsWith("imported/")?new File(root,key).getAbsolutePath():key;}
}

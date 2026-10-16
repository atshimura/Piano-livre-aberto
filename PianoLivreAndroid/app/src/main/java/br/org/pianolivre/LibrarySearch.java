package br.org.pianolivre;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import java.io.File;

public final class LibrarySearch {
 private static final Pattern MARKS=Pattern.compile("\\p{M}+");
 private static String fold(String value){return MARKS.matcher(Normalizer.normalize(value==null?"":value,Normalizer.Form.NFD)).replaceAll("").toLowerCase(Locale.ROOT);}
 public static boolean matches(String title,String query){
  String text=fold(title);for(String term:fold(query).trim().split("\\s+"))if(!text.contains(term))return false;return true;
 }
 /** Portable keys: bookmarks do not depend on /data/user/0 vs /data/data paths. */
 public static String key(String source){return source.startsWith("scores/")||source.startsWith("imported/")?source:"imported/"+new File(source).getName();}
}

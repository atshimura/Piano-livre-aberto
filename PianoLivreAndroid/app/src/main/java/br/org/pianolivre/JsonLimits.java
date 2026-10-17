package br.org.pianolivre;
/** Lexical nesting bound applied before org.json constructs recursive objects. */
public final class JsonLimits {
 private JsonLimits(){}
 public static void check(String text,int maximum,int maxDepth){
  if(text==null||text.length()>maximum)throw new IllegalArgumentException("JSON excede o limite permitido");
  int depth=0;boolean quoted=false,escape=false;
  for(int i=0;i<text.length();i++){
   char c=text.charAt(i);if(quoted){if(escape)escape=false;else if(c=='\\')escape=true;else if(c=='"')quoted=false;}
   else if(c=='"')quoted=true;
   else if(c=='{'||c=='['){if(++depth>maxDepth)throw new IllegalArgumentException("JSON profundo demais");}
   else if(c=='}'||c==']'){if(--depth<0)throw new IllegalArgumentException("JSON inválido");}
  }
  if(quoted||depth!=0)throw new IllegalArgumentException("JSON incompleto");
 }
}

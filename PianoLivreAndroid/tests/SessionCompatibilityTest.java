import br.org.pianolivre.*;import java.io.*;import java.util.*;
public class SessionCompatibilityTest {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void invalid(Runnable r,String m){boolean bad=false;try{r.run();}catch(IllegalArgumentException e){bad=true;}check(bad,m);}
 public static void main(String[] a){
  File root=new File("/current/files");
  for(String old:new String[]{"/data/user/0/old.package/files/imported/a.xml","/data/data/new.package/files/imported/a.xml","imported/a.xml"})check(SessionFiles.resolve(root,old).equals("/current/files/imported/a.xml"),"portable session "+old);
  check(SessionFiles.resolve(root,"scores/a.mxl").equals("scores/a.mxl"),"asset stays asset");
  for(String bad:new String[]{"scores/../a.xml","/etc/passwd","imported/../../a.xml","imported/..","imported/a/b.xml"})invalid(()->SessionFiles.portable(bad),"reject unsafe path");
  for(String old:new String[]{"_etude..mxl","-file.mxl",".hidden.mxl","_".repeat(240)+".mxl"})check(SessionFiles.portable("/data/data/old/files/imported/"+old).equals("imported/"+old),"legacy flat filename preserved");
  Map<String,Object> prefs=new HashMap<>();prefs.put("user_0_file","/data/data/old/files/imported/a.xml");prefs.put("user_0_position",Double.doubleToLongBits(1.25));prefs.put("user_0_tempo",Double.doubleToLongBits(1));prefs.put("user_0_hand",2);prefs.put("user_0_keyboard",true);SessionPreferences.validate(prefs);check(prefs.get("user_0_file").equals("imported/a.xml"),"normalize session before restore");
  prefs.put("user_0_mode","2");invalid(()->SessionPreferences.validate(prefs),"string mode rejected");prefs.remove("user_0_mode");prefs.put("user_0_tempo",Double.doubleToLongBits(Double.NaN));invalid(()->SessionPreferences.validate(prefs),"NaN tempo rejected");
  JsonLimits.check("{\"text\":\"[\\\"{\\\\\"}",100,2);checks++;
  invalid(()->JsonLimits.check("[".repeat(65)+"]".repeat(65),1000,64),"deep JSON rejected before parser");invalid(()->JsonLimits.check("{",10,64),"unfinished JSON rejected");invalid(()->JsonLimits.check("abc",2,64),"size bounded");
  System.out.println("PASS SessionCompatibilityTest "+checks+" assertions: portable paths, known types and lexical JSON bounds");
 }
}

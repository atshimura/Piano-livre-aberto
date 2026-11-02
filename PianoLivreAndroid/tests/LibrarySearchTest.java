import br.org.pianolivre.LibrarySearch;
public final class LibrarySearchTest {
 public static void main(String[] args){
  String[][] cases={{"Ode à Alegria","ALEGRIA ode","true"},{"Gymnopédie nº 1","gymnopedie 1","true"},{"fur-elise-easy.mxl","fur elise","true"},{"Estudo Livre","livre ausente","false"},{"Qualquer","","true"}};
  for(String[] c:cases)if(LibrarySearch.matches(c[0],c[1])!=Boolean.parseBoolean(c[2]))throw new AssertionError(c[0]);
  if(!LibrarySearch.key("/data/user/0/app/files/imported/teste.mxl").equals("imported/teste.mxl"))throw new AssertionError("portable key");
  if(!LibrarySearch.key("scores/exercicio.mxl").equals("scores/exercicio.mxl"))throw new AssertionError("asset key");
  String created="creation-score:123e4567-e89b-12d3-a456-426614174000";
  if(!LibrarySearch.key(created).equals(created))throw new AssertionError("created score must keep stable portable key");
  try{
   if(!br.org.pianolivre.SessionFiles.portable(created).equals(created))throw new AssertionError("creation session portable");
   if(!br.org.pianolivre.SessionFiles.resolve(new java.io.File("/tmp"),created).equals(created))throw new AssertionError("creation session resolve must keep virtual ID");
  }catch(Exception ex){throw new AssertionError("creation route",ex);}
  System.out.println("PASS LibrarySearchTest: accents, case, tokens, filenames, empty query and portable identity");
 }
}

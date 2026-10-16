import br.org.pianolivre.LibrarySearch;
public final class LibrarySearchTest {
 public static void main(String[] args){
  String[][] cases={{"Ode à Alegria","ALEGRIA ode","true"},{"Gymnopédie nº 1","gymnopedie 1","true"},{"fur-elise-easy.mxl","fur elise","true"},{"Estudo Livre","livre ausente","false"},{"Qualquer","","true"}};
  for(String[] c:cases)if(LibrarySearch.matches(c[0],c[1])!=Boolean.parseBoolean(c[2]))throw new AssertionError(c[0]);
  if(!LibrarySearch.key("/data/user/0/app/files/imported/teste.mxl").equals("imported/teste.mxl"))throw new AssertionError("portable key");
  if(!LibrarySearch.key("scores/exercicio.mxl").equals("scores/exercicio.mxl"))throw new AssertionError("asset key");
  System.out.println("PASS LibrarySearchTest: accents, case, tokens, filenames, empty query and portable identity");
 }
}

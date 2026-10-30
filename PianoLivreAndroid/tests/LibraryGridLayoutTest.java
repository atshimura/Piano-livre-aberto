import br.org.pianolivre.LibraryGridLayout;
/** Deterministic compact-library density regression without Android SDK. */
public final class LibraryGridLayoutTest {
 private static int checks;
 private static void equal(int actual,int expected,int width){
  checks++;if(actual!=expected)throw new AssertionError("width "+width+": expected "+expected+" got "+actual);
 }
 public static void main(String[] args){
  int[][] cases={{320,1},{480,1},{599,1},{600,2},{670,2},{869,2},
   {1020,2},{1159,2},{1160,2},{1400,2}};
  for(int[] c:cases)equal(LibraryGridLayout.columnsForWidthDp(c[0]),c[1],c[0]);
  boolean rejected=false;try{LibraryGridLayout.columnsForWidthDp(0);}catch(IllegalArgumentException expected){rejected=true;}
  if(!rejected)throw new AssertionError("zero width accepted");checks++;
  System.out.println("LIBRARY_GRID_LAYOUT_TEST_PASS checks="+checks);
 }
}

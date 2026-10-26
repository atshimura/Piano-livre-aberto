import br.org.pianolivre.LibraryGridLayout;
/** Deterministic compact-library density regression without Android SDK. */
public final class LibraryGridLayoutTest {
 private static int checks;
 private static void equal(int actual,int expected,int width){
  checks++;if(actual!=expected)throw new AssertionError("width "+width+": expected "+expected+" got "+actual);
 }
 public static void main(String[] args){
  int[][] cases={{320,2},{599,2},{600,3},{670,3},{869,3},{870,4},
   {1020,4},{1159,4},{1160,5},{1400,5}};
  for(int[] c:cases)equal(LibraryGridLayout.columnsForWidthDp(c[0]),c[1],c[0]);
  boolean rejected=false;try{LibraryGridLayout.columnsForWidthDp(0);}catch(IllegalArgumentException expected){rejected=true;}
  if(!rejected)throw new AssertionError("zero width accepted");checks++;
  System.out.println("LIBRARY_GRID_LAYOUT_TEST_PASS checks="+checks);
 }
}

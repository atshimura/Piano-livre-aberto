import br.org.pianolivre.HomeMidiIndicator;

public final class HomeMidiIndicatorTest {
 public static void main(String[] args){
  check("USB-MIDI".equals(HomeMidiIndicator.label(false,"Yamaha P-45")),"disconnected state keeps USB-MIDI label");
  check("Yamaha P-45".equals(HomeMidiIndicator.label(true," Yamaha P-45 ")),"opened device displays trimmed Android name");
  check("USB-MIDI".equals(HomeMidiIndicator.label(true," ")),"unnamed MIDI device does not get an invented name");
  System.out.println("PASS HomeMidiIndicatorTest: disconnected, identified and unnamed MIDI states");
 }
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}

import br.org.pianolivre.StudyChromeState;
public final class StudyChromeStateTest {
 public static void main(String[] args){
  StudyChromeState state=new StudyChromeState();
  check(state.visible(),"initial controls visible");
  check(state.update(true),"playback never hides controls");
  check(state.update(true),"playback ticks preserve controls");
  check(state.reveal()&&state.reveal(),"note taps are idempotent");
  check(state.update(false),"pause preserves controls");
  check(state.update(true),"resume keeps controls visible");
  check(state.update(false),"background and stop keep controls visible");
  check(state.visible(),"final state visible");
  System.out.println("PASS StudyChromeStateTest: controls remain visible through playback, pause, taps, resume and stop");
 }
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}

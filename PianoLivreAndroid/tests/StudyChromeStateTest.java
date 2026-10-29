import br.org.pianolivre.StudyChromeState;
public final class StudyChromeStateTest {
 public static void main(String[] args){
  StudyChromeState state=new StudyChromeState();
  check(state.visible(),"initial controls");
  check(!state.update(true),"start hides");
  check(!state.update(true),"ticks do not reveal");
  check(state.reveal()&&state.reveal(),"repeated taps are idempotent");
  check(state.update(true),"ticks preserve revealed state without a timer");
  check(state.update(false),"pause restores");
  check(!state.update(true),"resume begins a new presentation cycle");
  check(state.update(false),"background/stop restores");
  System.out.println("PASS StudyChromeStateTest: start, repeated taps, ticks, pause and resume");
 }
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
}

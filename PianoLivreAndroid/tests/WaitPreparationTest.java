import br.org.pianolivre.WaitPreparation;
public class WaitPreparationTest {
 static int checks=0;
 static void yes(boolean ok){checks++;if(!ok)throw new AssertionError("check "+checks);}
 public static void main(String[] args){
  yes(WaitPreparation.configuredSeconds(0)==5);
  yes(WaitPreparation.configuredSeconds(31)==5);
  yes(WaitPreparation.configuredSeconds(5)==5);
  yes(WaitPreparation.configuredSeconds(1)==1);
  yes(WaitPreparation.configuredSeconds(30)==30);
  WaitPreparation p=new WaitPreparation();
  yes(!p.isActive());yes(!p.finish(1200));
  p.start(1000,5);yes(p.isActive());yes(p.durationMs()==5000);
  yes(!p.finish(5999));yes(p.isActive());yes(p.finish(6000));yes(!p.isActive());
  yes(!p.finish(6500));
  p.start(8000,2);yes(!p.finish(9999));yes(p.finish(10000));
  p.start(15000,30);p.cancel();yes(!p.isActive());yes(!p.finish(90000));
  p.start(0,99);yes(p.durationMs()==5000);
  System.out.println("WAIT_PREPARATION_PASS checks="+checks);
 }
}

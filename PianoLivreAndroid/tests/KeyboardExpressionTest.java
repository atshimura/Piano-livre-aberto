import br.org.pianolivre.*;
import java.io.*;
import java.util.*;

public final class KeyboardExpressionTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void close(double a,double b){check(Math.abs(a-b)<1e-7,a+" vs "+b);}
 public static void main(String[] args)throws Exception{
  Score s;try(InputStream in=new FileInputStream("tests/fixtures/motor/explicit-dynamics.xml")){s=Score.read(in,"fixture.xml");}
  KeyboardGuide guide=new KeyboardGuide(s);KeyboardGuide.Frame a=guide.at(1.25);
  check(a.rightVelocity.get(62)==113&&a.leftVelocity.get(57)==90,"independent hand intensities");
  close(a.rightRemaining.get(62),.75);close(a.leftRemaining.get(57),2.75/3);
  KeyboardGuide.Frame b=guide.at(2.5);
  check(b.right.contains(64)&&b.rightVelocity.get(64)==0,"silent written note still visible");
  close(b.rightRemaining.get(64),.5);check(a.right.contains(62),"previous snapshot retained");
  try{a.rightVelocity.put(60,1);throw new AssertionError("mutable velocity snapshot");}catch(UnsupportedOperationException expected){}
  try{a.leftRemaining.clear();throw new AssertionError("mutable duration snapshot");}catch(UnsupportedOperationException expected){}
  Score same=new Score();Score.Event right=new Score.Event(0,2,60,1,1,1);right.velocity=40;
  Score.Event left=new Score.Event(0,4,60,2,1,1);left.velocity=100;same.events.add(right);same.events.add(left);
  KeyboardGuide.Frame overlap=new KeyboardGuide(same).at(1);
  check(overlap.both.size()==1&&overlap.right.contains(60)&&overlap.left.contains(60),"same pitch in both staves");
  check(overlap.rightVelocity.get(60)==40&&overlap.leftVelocity.get(60)==100,"same pitch retains both intensities");
  close(overlap.rightRemaining.get(60),.5);close(overlap.leftRemaining.get(60),.75);
  check(new KeyboardGuide(same).at(4).both.isEmpty(),"bars disappear at note end");
  check(guide.at(Double.NaN).rightRemaining.isEmpty(),"invalid beat yields empty frame");
  System.out.println("PASS KeyboardExpressionTest: hands, dynamics, remaining duration, overlapping pitch, zero, boundaries and immutable snapshots");
 }
}

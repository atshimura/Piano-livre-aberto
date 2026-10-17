import br.org.pianolivre.*;import java.util.*;
/** Pathological held bass + expired short notes; validates work, not wall time/FPS. */
public class KeyboardGuideLoadTest {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void compare(Score score,KeyboardGuide guide,double beat){
  Set<Integer> right=new HashSet<>(),left=new HashSet<>();Map<Integer,Integer> rv=new HashMap<>(),lv=new HashMap<>();Map<Integer,Double> rr=new HashMap<>(),lr=new HashMap<>();
  for(Score.Event e:score.events)if(e.length>0&&e.beat<=beat+1e-7&&e.beat+e.length>beat+1e-7){boolean upper=e.staff==1;(upper?right:left).add(e.midi);(upper?rv:lv).merge(e.midi,e.velocity,Math::max);(upper?rr:lr).merge(e.midi,Math.max(0,Math.min(1,(e.beat+e.length-beat)/e.length)),Math::max);}
  KeyboardGuide.Frame f=guide.at(beat);check(f.right.equals(right)&&f.left.equals(left),"active notes "+beat);check(f.rightVelocity.equals(rv)&&f.leftVelocity.equals(lv)&&f.rightRemaining.equals(rr)&&f.leftRemaining.equals(lr),"dynamics/durations "+beat);
 }
 public static void main(String[] args){
  Score score=new Score();score.events.add(new Score.Event(0,20000,36,2,1,1));
  for(int i=0;i<65536;i++){Score.Event n=new Score.Event(i*.25,.125,48+i%40,1,2,i/16+1);n.velocity=30+i%80;score.events.add(n);}
  KeyboardGuide guide=new KeyboardGuide(score);Random random=new Random(20261017);
  for(int i=0;i<200;i++){double beat=random.nextInt(65536)*.25+.0625;compare(score,guide,beat);check(guide.visitedNodesLastQuery()<140,"query work must not grow with expired notes: "+guide.visitedNodesLastQuery());}
  for(double beat:new double[]{-.1,0,.125,.25,16383.75,16383.875,19999.999,20000})compare(score,guide,beat);
  check(guide.at(Double.NaN).both.isEmpty()&&guide.visitedNodesLastQuery()==0,"nonfinite query empty");check(new KeyboardGuide(null).at(0).both.isEmpty(),"empty guide");
  System.out.println("PASS KeyboardGuideLoadTest "+checks+" assertions: 65,537 events, held bass, exact notes/dynamics/durations, fewer than 140 tree visits");
 }
}

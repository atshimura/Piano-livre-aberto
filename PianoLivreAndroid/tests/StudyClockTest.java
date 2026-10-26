import br.org.pianolivre.StudyClock;
public final class StudyClockTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  StudyClock c=new StudyClock();c.start(100,1000,0,"scores/a.mxl","A",0,0,0,1);
  c.start(200,999999,1,"scores/b.mxl","B",2,1,5,.5);
  StudyClock.Window a=c.finish(600,2,.75);
  check(a.user==0&&a.score.equals("scores/a.mxl")&&a.at==1000&&a.elapsedMs==500,"monotonic duration and immutable profile/context despite duplicate start");
  check(a.fromBeat==0&&a.toBeat==2&&a.startSpeed==1&&a.endSpeed==.75,"positions and speed recorded without scoring");
  check(c.finish(1000,3,1)==null,"duplicate finish does not duplicate history");
  c.start(1100,1,1,"scores/b.mxl","B",3,2,3,1);StudyClock.Window b=c.finish(1600,3,1);
  check(b.user==1&&b.mode==3&&b.elapsedMs==500,"wall-clock jump and profile change do not mix intervals");
  c.start(2000,1000,0,"scores/a.mxl","A",0,0,0,1);check(c.finish(1000,0,1).elapsedMs==0,"clock rollback does not invent negative study time");
  System.out.println("PASS StudyClockTest: monotonic timer, wall jumps, duplicate events, profile/context and reading mode");
 }
}

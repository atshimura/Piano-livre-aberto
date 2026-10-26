package br.org.pianolivre;

/** Monotonic foreground-screen timer, not a musical clock or performance grade. */
public final class StudyClock {
 public static final class Window {
  public final int user,mode,hand;public final String score,title;
  public final long at,elapsedMs;public final double fromBeat,toBeat,startSpeed,endSpeed;
  Window(int user,String score,String title,int mode,int hand,long at,long elapsed,double from,double to,double start,double end){
   this.user=user;this.score=score;this.title=title;this.mode=mode;this.hand=hand;
   this.at=at;elapsedMs=elapsed;fromBeat=from;toBeat=to;startSpeed=start;endSpeed=end;
  }
 }
 private Window window;private long started;
 public boolean active(){return window!=null;}
 public void start(long now,long wall,int user,String score,String title,int mode,int hand,double beat,double speed){
  if(window!=null)return;
  if(now<0||wall<0||user<0||score==null||mode<0||mode>3||hand<0||hand>2||!Double.isFinite(beat)||beat<0||!Double.isFinite(speed)||speed<=0)throw new IllegalArgumentException("Invalid study window");
  started=now;window=new Window(user,score,title==null||title.trim().isEmpty()?"Partitura":title,mode,hand,wall,0,beat,beat,speed,speed);
 }
 public Window finish(long now,double beat,double speed){
  if(window==null)return null;Window w=window;window=null;
  return new Window(w.user,w.score,w.title,w.mode,w.hand,w.at,now>=started?now-started:0,w.fromBeat,beat,w.startSpeed,speed);
 }
}

package br.org.pianolivre;

import java.util.*;

/**
 * Safe, finite performance itinerary over the ORIGINAL written-score timeline.
 * The display, virtual keyboard and MIDI all use Score's original beat values;
 * a repeat/DS/DC/Coda jump moves the cursor, never rewrites or copies notes.
 *
 * Supported: nested simple repeats, standard first/second endings and ONE
 * Da Capo/Dal Segno jump, optionally with Fine or To Coda. Combinations that
 * cannot be resolved deterministically are declined, not guessed.
 */
public final class PerformanceRoute {
 public static final class Visit {
  public final int measure,pass;
  public final double start,end;
  Visit(Score.MeasureInfo m,int pass){
   this.measure=m.index;this.pass=pass;this.start=m.start;this.end=m.end;
  }
 }
 public static final class Result {
  public final List<Visit> visits;
  public final List<String> warnings;
  Result(List<Visit> v,List<String> w){
   visits=Collections.unmodifiableList(new ArrayList<>(v));
   warnings=Collections.unmodifiableList(new ArrayList<>(w));
  }
  public boolean supported(){return warnings.isEmpty();}
 }
 private static final class Repeat {
  final int start,end,times;
  int pass=1;
  Repeat(int start,int end,int times){this.start=start;this.end=end;this.times=times;}
 }
 private static final int MAX_MEASURES=512,MAX_VISITS=4096;
 private static void warn(List<String> issues,String reason){
  if(!issues.contains(reason))issues.add(reason);
 }
 public static Result build(Score score){
  List<Score.MeasureInfo> ms=score.measureInfo;
  ArrayList<String> issues=new ArrayList<>();
  ArrayList<Visit> plan=new ArrayList<>();
  if(ms.isEmpty())return new Result(plan,Collections.singletonList("Partitura sem compassos"));
  if(ms.size()>MAX_MEASURES)return new Result(plan,Collections.singletonList("Partitura grande demais para navegação expandida"));
  ArrayList<Repeat> repeats=new ArrayList<>();
  ArrayDeque<Integer> openings=new ArrayDeque<>();
  int dcIndex=-1,dsIndex=-1,segnoIndex=-1,codaIndex=-1,toCodaIndex=-1,fineIndex=-1;
  boolean anyEnding=false,requiresFine=false,requiresCoda=false;
  for(int i=0;i<ms.size();i++){
   Score.MeasureInfo m=ms.get(i);
   if(m.forwardRepeat)openings.push(i);
   if(m.backwardRepeat){
    int open=openings.isEmpty()?0:openings.pop();
    if(m.repeatTimes<1||m.repeatTimes>8)warn(issues,"Repetição com contagem fora de 1–8");
    else repeats.add(new Repeat(open,i,m.repeatTimes));
   }
   if(!m.endings.isEmpty())anyEnding=true;
   requiresFine|=m.expectsFine;
   requiresCoda|=m.expectsCoda;
   if(m.daCapo){if(dcIndex>=0)warn(issues,"Mais de um D.C.");dcIndex=i;}
   if(m.dalSegno){if(dsIndex>=0)warn(issues,"Mais de um D.S.");dsIndex=i;}
   if(m.segno){if(segnoIndex>=0)warn(issues,"Múltiplos Segnos");segnoIndex=i;}
   if(m.coda){if(codaIndex>=0)warn(issues,"Múltiplas Codas");codaIndex=i;}
   if(m.toCoda){if(toCodaIndex>=0)warn(issues,"Mais de um To Coda");toCodaIndex=i;}
   if(m.fine){if(fineIndex>=0)warn(issues,"Mais de um Fine");fineIndex=i;}
  }
  if(!openings.isEmpty())warn(issues,"Barra de repetição aberta sem fechamento");
  double lastMeasureEnd=ms.get(ms.size()-1).end;
  if(score.end()>lastMeasureEnd+.001)
   warn(issues,"Partes musicais com durações incompatíveis para navegação");
  if(dcIndex>=0&&dsIndex>=0)warn(issues,"D.C. e D.S. conflitantes");
  if(dsIndex>=0&&segnoIndex<0)warn(issues,"D.S. sem Segno");
  if(requiresFine&&fineIndex<0)warn(issues,"Retorno al Fine sem indicação de Fine");
  if(requiresCoda&&(toCodaIndex<0||codaIndex<0))
   warn(issues,"Retorno al Coda sem ambos To Coda e Coda");
  if(toCodaIndex>=0&&codaIndex<0)warn(issues,"To Coda sem destino");
  if(dcIndex<0&&dsIndex<0&&toCodaIndex>=0)
   warn(issues,"To Coda sem D.C./D.S.");
  if((dcIndex>=0||dsIndex>=0)&&(!repeats.isEmpty()||anyEnding))
   warn(issues,"Combinação de retorno D.C./D.S. com casas/repetições ainda não suportada");
  if(anyEnding&&repeats.isEmpty())warn(issues,"Casa de repetição sem barra de retorno");
  // Nested repeats with alternate endings are not unambiguously resolved yet.
  if(anyEnding)for(int a=0;a<repeats.size();a++)for(int b=a+1;b<repeats.size();b++){
   Repeat x=repeats.get(a),y=repeats.get(b);
   if((x.start<=y.start&&x.end>=y.end)||(y.start<=x.start&&y.end>=x.end))
    warn(issues,"Casas em repetição aninhada ainda não suportadas");
  }
  for(int i=0;i<ms.size();i++){
   Score.MeasureInfo m=ms.get(i);
   for(int n:m.endings)if(n>2)warn(issues,"Casas acima de 1ª/2ª ainda não suportadas");
  }
  // Disjoint repeat sections are independent; nested sections use stack.
  for(int a=0;a<repeats.size();a++)for(int b=a+1;b<repeats.size();b++){
   Repeat x=repeats.get(a),y=repeats.get(b);
   if(x.start<y.start&&y.start<=x.end&&x.end<y.end ||
      y.start<x.start&&x.start<=y.end&&y.end<x.end)
    warn(issues,"Repetições parcialmente sobrepostas não suportadas");
  }
  if(!issues.isEmpty())return new Result(Collections.<Visit>emptyList(),issues);
  Map<Integer,Repeat> closes=new HashMap<>();
  for(Repeat rep:repeats){
   if(closes.put(rep.end,rep)!=null)
    warn(issues,"Fechamentos de repetição ambíguos");
  }
  if(!issues.isEmpty())return new Result(Collections.<Visit>emptyList(),issues);

  int i=0,recentPass=1;
  boolean returned=false,usedToCoda=false;
  while(i>=0&&i<ms.size()&&plan.size()<MAX_VISITS){
   Score.MeasureInfo m=ms.get(i);
   Repeat owner=null;
   for(Repeat rep:repeats)
    if(rep.start<=i&&rep.end>=i &&
       (owner==null||rep.start>owner.start||rep.start==owner.start&&rep.end<owner.end))
     owner=rep;
   int pass=owner!=null?owner.pass:recentPass;
   // When the first ending contains the repeat bar, pass 2 skips it and
   // proceeds directly to the second ending outside the repeated range.
   if(!m.endings.isEmpty()&&!m.endings.contains(pass)){
    i++;
    continue;
   }
   plan.add(new Visit(m,pass));
   if(returned&&m.fine){break;}
   if(returned&&!usedToCoda&&m.toCoda&&codaIndex>=0){
    usedToCoda=true;i=codaIndex;continue;
   }
   if(!returned&&(m.daCapo||m.dalSegno)){
    returned=true;
    recentPass=1;
    for(Repeat rep:repeats)rep.pass=1;
    i=m.daCapo?0:segnoIndex;
    continue;
   }
   Repeat close=closes.get(i);
   if(close!=null&&!returned){
    if(close.pass<close.times){
     close.pass++;
     recentPass=close.pass;
     // Re-arm all nested repetitions when their outer block restarts.
     for(Repeat child:repeats)
      if(child!=close&&child.start>=close.start&&child.end<=close.end)
       child.pass=1;
     i=close.start;continue;
    }
    recentPass=close.pass;
   }
   i++;
  }
  if(plan.size()>=MAX_VISITS)
   warn(issues,"Percurso musical excedeu o limite de segurança de 4096 compassos");
  // A bogus backwards target can loop with no musical progress. Fail closed.
  if(!issues.isEmpty())return new Result(Collections.<Visit>emptyList(),issues);
  return new Result(plan,issues);
 }

 /** One interval of a playback tick; seek=true means a written jump occurred. */
 public static final class Move {
  public final double beat;public final boolean seek;
  Move(double beat,boolean seek){this.beat=beat;this.seek=seek;}
 }
 public static final class Movement {
  public final List<Move> moves;public final double position;public final boolean finished;
  Movement(List<Move> m,double p,boolean f){moves=m;position=p;finished=f;}
 }
 public static final class Cursor {
  private final Result route;
  private int index=0;
  private boolean finished=false;
  public Cursor(Result route){this.route=route;}
  public void reset(){index=0;finished=false;}
  /** Recover exact pass of a repeated bar after app process recreation. */
  public boolean restore(int visit,double position){
   if(visit<0||visit>=route.visits.size())return false;
   Visit v=route.visits.get(visit);
   if(position<v.start-1e-6||position>v.end+1e-6)return false;
   index=visit;finished=false;return true;
  }
  public void align(double position){
   index=0;finished=false;
   for(int i=0;i<route.visits.size();i++){
    Visit v=route.visits.get(i);
    if(position>=v.start-1e-7&&position<v.end-1e-7){index=i;return;}
   }
   if(position>=route.visits.get(route.visits.size()-1).end-1e-7){
    index=route.visits.size()-1;finished=true;
   }
  }
  private static double millisFor(Score score,double from,double to,double factor){
   double cursor=from,elapsed=0;
   while(cursor<to-1e-9){
    double boundary=to;
    for(Score.TempoChange t:score.tempoChanges)
     if(t.beat>cursor+1e-8){boundary=Math.min(boundary,t.beat);break;}
    elapsed+=(boundary-cursor)*60000.0/(score.tempoAt(cursor)*factor);
    cursor=boundary;
   }
   return elapsed;
  }
  public Movement tick(Score score,double position,double elapsedMs,double factor){
   ArrayList<Move> moves=new ArrayList<>();
   if(route.visits.isEmpty()||finished)return new Movement(moves,position,true);
   double left=Math.max(0,elapsedMs),beat=position;
   for(int guard=0;guard<route.visits.size()+2&&left>=0;guard++){
    Visit current=route.visits.get(index);
    double remainingBeat=Math.max(0,current.end-beat);
    double required=millisFor(score,beat,current.end,factor);
    if(remainingBeat>1e-8&&left<required-1e-8){
     beat=score.advanceBeats(beat,left,factor);
     moves.add(new Move(beat,false));
     break;
    }
    if(index==route.visits.size()-1){
     beat=current.end;moves.add(new Move(beat,false));finished=true;break;
    }
    Visit next=route.visits.get(++index);
    boolean discontinuity=Math.abs(next.start-current.end)>1e-7;
    if(discontinuity){
     // Transition must turn off all old MIDI notes before the next onset.
     moves.add(new Move(Math.max(current.start,current.end-0.00001),false));
     moves.add(new Move(next.start,true));
    }else moves.add(new Move(current.end,false));
    beat=next.start;
    left=Math.max(0,left-required);
    if(left<1e-7)break;
   }
   return new Movement(moves,beat,finished);
  }
  /**
   * Consume score attacks from current occurrence forward; the returned beat
   * can go backwards across a written repeat or jump to a different ending.
   * If no further note exists, return current position unchanged.
   */
  public double nextWait(Score score,double position,int hand){
   if(route.visits.isEmpty()||finished)return position;
   for(int i=index;i<route.visits.size();i++){
    Visit v=route.visits.get(i);
    double found=Double.POSITIVE_INFINITY;
    for(Score.Event e:score.events){
     if(hand==1&&e.staff!=1||hand==2&&e.staff==1)continue;
     if(e.beat<v.start-1e-7||e.beat>=v.end-1e-7)continue;
     if(i==index&&e.beat<=position+.005)continue;
     found=Math.min(found,e.beat);
    }
    if(Double.isFinite(found)){
     index=i;return found;
    }
   }
   finished=true;return position;
  }
  public int index(){return index;}
  public boolean finished(){return finished;}
 }
}

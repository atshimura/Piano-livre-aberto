import br.org.pianolivre.*;
import java.util.*;

public final class MusicDiscoverySessionTest {
 private static int checks;
 private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 private static final class Rig {
  final MusicDiscoverySession s=new MusicDiscoverySession();long gen=1,batch=1,time=0;
  Rig(Long window){s.reconnect(gen,window);}
  void hit(int n){s.attack(n,time,batch,gen);time+=1;s.release(n,gen);s.finishBatch(batch++,time);}
  void on(int n){s.attack(n,time,batch,gen);time+=1;}
  void off(int n){s.release(n,gen);}
 }
 public static void main(String[]args){
  for(Object[] sample:new Object[][]{{36,"DÓ - C2"},{60,"DÓ - C4"},{84,"DÓ - C6"},{61,"DÓ♯ - C♯4"},{63,"MI♭ - E♭4"},{70,"SI♭ - B♭4"}}){
   Rig r=new Rig(null);r.hit((Integer)sample[0]);check(r.s.result()!=null&&r.s.result().title.equals(sample[1]),"spelling/registro "+sample[0]);
  }
  Rig chord=new Rig(null);chord.on(60);chord.on(64);chord.on(67);chord.off(60);chord.off(64);chord.off(67);chord.s.finishBatch(chord.batch,chord.time);
  check(chord.s.result().kind==MusicDiscoverySession.Kind.CHORD,"acorde simultâneo");
  check(chord.s.result().notes.equals(Arrays.asList(60,64,67)),"notas exatas de acorde");
  Rig rolled=new Rig(100L);rolled.on(60);rolled.time=25;rolled.on(64);rolled.off(60);rolled.time=70;rolled.on(67);rolled.off(64);rolled.off(67);
  check(rolled.s.phase()==MusicDiscoverySession.Phase.PENDING,"acorde em formação fica pendente");
  check(!rolled.s.advance(99),"não antecipar janela calibrada");check(rolled.s.advance(100),"concluir na janela medida");
  check(rolled.s.result().kind==MusicDiscoverySession.Kind.CHORD,"acorde com ataques escalonados");
  int[] up={60,62,64,65,67,69,71,72};Rig scaleUp=new Rig(null);for(int n:up)scaleUp.hit(n);
  check(scaleUp.s.result().kind==MusicDiscoverySession.Kind.SEQUENCE,"escala ascendente");
  check(scaleUp.s.result().notes.equals(Arrays.asList(60,62,64,65,67,69,71,72)),"todas notas na ordem ascendente");
  int[] down={72,71,69,67,65,64,62,60};Rig scaleDown=new Rig(null);for(int n:down)scaleDown.hit(n);
  check(scaleDown.s.result().kind==MusicDiscoverySession.Kind.SEQUENCE,"escala descendente");
  int[] arpUp={60,64,67,72};Rig au=new Rig(null);for(int n:arpUp)au.hit(n);
  check(au.s.result().kind==MusicDiscoverySession.Kind.SEQUENCE&&au.s.result().notes.equals(Arrays.asList(60,64,67,72)),"arpejo ascendente");
  int[] arpDown={72,67,64,60};Rig ad=new Rig(null);for(int n:arpDown)ad.hit(n);
  check(ad.s.result().kind==MusicDiscoverySession.Kind.SEQUENCE&&ad.s.result().notes.equals(Arrays.asList(72,67,64,60)),"arpejo descendente");
  Rig incomplete=new Rig(null);for(int n:new int[]{60,62,64,65,67,69})incomplete.hit(n);
  check(incomplete.s.result().kind==MusicDiscoverySession.Kind.NOTE,"sequência incompleta não é escala antes do delimitador");
  check(incomplete.s.advance(incomplete.time+2)&&incomplete.s.result().kind==MusicDiscoverySession.Kind.AMBIGUOUS&&incomplete.s.result().notes.size()==6,"pausa relativa ao andamento observado delimita frase incompleta");
  Rig ambiguous=new Rig(null);ambiguous.on(60);ambiguous.on(61);ambiguous.off(60);ambiguous.off(61);ambiguous.s.finishBatch(ambiguous.batch,ambiguous.time);
  check(ambiguous.s.result().kind==MusicDiscoverySession.Kind.AMBIGUOUS,"conjunto sem acorde defensável é ambíguo");
  Rig repeated=new Rig(null);repeated.on(60);check(!repeated.s.attack(60,1,repeated.batch,repeated.gen),"Note On redundante enquanto pressionada");
  repeated.off(60);repeated.batch++;repeated.hit(60);check(repeated.s.result().notes.equals(Collections.singletonList(60)),"rearticulação depois do Note Off");
  Rig timedSequence=new Rig(120L);for(int n:up){timedSequence.on(n);timedSequence.off(n);timedSequence.time+=25;timedSequence.batch++;}
  check(timedSequence.s.result().kind==MusicDiscoverySession.Kind.SEQUENCE,"ataques sucessivos liberados reconhecem escala mesmo com perfil calibrado");
  Rig cancel=new Rig(null);for(int n:new int[]{60,62,64})cancel.hit(n);cancel.s.cancelPending();
  check(cancel.s.phase()==MusicDiscoverySession.Phase.PRESENTED,"cancelar mantém resultado apresentado");
  Rig persist=new Rig(null);persist.hit(60);int prior=persist.s.result().notes.get(0);persist.on(62);
  check(persist.s.phase()==MusicDiscoverySession.Phase.PENDING_WITH_RESULT&&persist.s.result().notes.get(0)==prior,"resultado preservado durante análise inconclusiva");
  persist.s.cancelPending();check(persist.s.result().notes.get(0)==prior,"cancelamento mantém último resultado");
  persist.s.reconnect(2,null);check(!persist.s.attack(64,20,9,1),"descartar callback da conexão antiga");
  persist.s.attack(64,20,9,2);check(persist.s.phase()==MusicDiscoverySession.Phase.PENDING_WITH_RESULT,"reconectar preserva resultado e aceita sessão nova");
  System.out.println("MUSIC_DISCOVERY_SESSION_PASS checks="+checks);
 }
}

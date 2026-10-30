import br.org.pianolivre.MidiConnection;
import br.org.pianolivre.HomeMidiIndicator;
import java.util.*;

/** Exercises the production connection owner, async completions and output bytes. */
public final class MidiConnectionTest {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static final class Ports implements MidiConnection.Ports {
  boolean rx,tx,failSend,failClose;int closed;List<String> messages=new ArrayList<>();
  Ports(boolean rx,boolean tx){this.rx=rx;this.tx=tx;}
  public boolean canReceive(){return rx;}public boolean canSend(){return tx;}
  public void send(byte[] value)throws Exception{if(failSend)throw new Exception("synthetic output failure");messages.add((value[0]&255)+":"+(value[1]&127)+":"+(value[2]&127));}
  public void close()throws Exception{closed++;if(failClose)throw new Exception("synthetic close failure");}
 }
 static final class Harness implements MidiConnection.Listener,MidiConnection.Opener {
  List<MidiConnection.Completion> pending=new ArrayList<>();List<Long> tokens=new ArrayList<>();
  long invalidated=-1;MidiConnection.Snapshot shown;
  MidiConnection connection=new MidiConnection(this);
  public void invalidated(long token){invalidated=token;}
  public void changed(MidiConnection.Snapshot state){shown=state;}
  public void open(MidiConnection.Target target,long token,MidiConnection.Completion completion){tokens.add(token);pending.add(completion);}
  String label(){MidiConnection.Snapshot s=connection.snapshot();return HomeMidiIndicator.label(s.connected(),s.target==null?null:s.target.name);}
 }
 static MidiConnection.Target target(int id,String name){return new MidiConnection.Target(id,name,true);}
 static void note(MidiConnection c,int channel,int pitch,boolean on){check(c.send(new byte[]{(byte)((on?0x90:0x80)|channel),(byte)pitch,80}),"send note");}
 public static void main(String[] args){
  Harness h=new Harness();check(h.label().equals("USB-MIDI"),"initial disconnected");
  h.connection.reconcile(Collections.emptyList(),h);check(h.pending.isEmpty(),"no fictitious device");
  MidiConnection.Target first=target(7,"Instrumento de teste A");
  h.connection.reconcile(Arrays.asList(first),h);
  check(h.pending.size()==1&&h.label().equals("USB-MIDI"),"available/opening is not connected");
  check(h.shown.phase==MidiConnection.Phase.OPENING,"opening published");
  h.connection.connect(first,h);check(h.pending.size()==1,"same opening is not duplicated");
  Ports p=new Ports(true,true);h.pending.get(0).complete(p);
  check(h.label().equals(first.name)&&h.shown.canReceive&&h.shown.canSend,"connected only after working ports");
  long token=h.shown.generation;
  h.connection.reconcile(Arrays.asList(target(7,"Nome atualizado pelo sistema")),h);
  check(h.label().equals("Nome atualizado pelo sistema")&&h.shown.generation==token,"metadata/foreground reconciliation keeps valid session");
  h.pending.get(0).complete(p);check(p.closed==0,"duplicate callback cannot close active resources");
  note(h.connection,0,60,true);note(h.connection,4,67,true);
  h.connection.send(new byte[]{(byte)0xB4,64,127});
  h.connection.disconnect("cabo removido");
  check(p.closed==1&&h.label().equals("USB-MIDI"),"disconnect closes resources and clears label");
  check(p.messages.contains("128:60:0")&&p.messages.contains("132:67:0"),"all app notes explicitly released");
  check(p.messages.indexOf("180:64:0")<p.messages.indexOf("180:123:0"),"pedal released before all notes off on used channel");
  check(h.shown.generation>token&&h.invalidated==h.shown.generation,"disconnect invalidates prior receiver generation");
  h.connection.connect(target(7,""),h);h.pending.get(1).complete(new Ports(true,false));
  check(h.shown.connected()&&h.shown.canReceive&&!h.shown.canSend&&h.label().equals("USB-MIDI"),"unnamed receive-only connected device");
  h.connection.connect(target(8,"Somente saída"),h);Ports output=new Ports(false,true);h.pending.get(2).complete(output);
  check(h.shown.connected()&&!h.shown.canReceive&&h.shown.canSend,"send-only supported");
  h.connection.connect(target(9,"Não abriu"),h);h.pending.get(3).complete(null);
  check(h.shown.phase==MidiConnection.Phase.FAILED&&!h.shown.connected()&&h.label().equals("USB-MIDI"),"null open failure never claims connection");
  h.connection.connect(target(9,"Sem portas"),h);Ports unavailable=new Ports(false,false);h.pending.get(4).complete(unavailable);
  check(unavailable.closed==1&&!h.shown.connected(),"device with no open ports released");
  h.connection.connect(first,h);int old=h.pending.size()-1;
  h.connection.reconcile(Collections.emptyList(),h);
  h.connection.connect(first,h);int current=h.pending.size()-1;
  Ports stale=new Ports(true,true);h.pending.get(old).complete(stale);
  check(stale.closed==1&&h.shown.phase==MidiConnection.Phase.OPENING,"late completion cannot resurrect removed/reused device ID");
  Ports valid=new Ports(true,true);h.pending.get(current).complete(valid);
  check(h.shown.connected()&&h.shown.generation==h.tokens.get(current),"new generation succeeds");
  note(h.connection,0,72,true);valid.failSend=true;
  check(!h.connection.send(new byte[]{(byte)0x90,74,80}),"send failure reported");
  check(valid.closed==1&&h.shown.phase==MidiConnection.Phase.FAILED&&!h.shown.canReceive&&!h.shown.canSend,"send failure closes/invalidates without recursion");
  h.connection.connect(first,(t,g,done)->{throw new Exception("open exception");});
  check(h.shown.phase==MidiConnection.Phase.FAILED,"synchronous open exception");
  for(int cycle=0;cycle<100;cycle++){
   h.connection.connect(target(100+cycle,"Instrumento "+cycle),h);Ports active=new Ports(true,true);h.pending.get(h.pending.size()-1).complete(active);
   check(h.label().equals("Instrumento "+cycle),"successive connection name");
   active.failClose=true;h.connection.disconnect("fim");check(active.closed==1&&!h.shown.connected(),"close exception cannot retain state");
  }
  h.connection.connect(first,h);MidiConnection.Completion afterDestroy=h.pending.get(h.pending.size()-1);h.connection.destroy();
  Ports late=new Ports(true,true);afterDestroy.complete(late);
  check(late.closed==1&&!h.shown.connected(),"destroy rejects and closes late callback");
  int count=h.pending.size();h.connection.connect(first,h);check(h.pending.size()==count,"destroyed owner cannot reopen");
  System.out.println("MIDI_CONNECTION_PASS checks="+checks);
 }
}

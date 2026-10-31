package br.org.pianolivre;

import android.app.AlertDialog;
import android.content.pm.ApplicationInfo;
import android.graphics.Rect;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;

/** Explicit debug-only, synthetic, real-view journey; refuses physical devices. */
final class CreationSelfTest {
 private final MainActivity a;private final MusicCreationScreen screen;private final File ack;
 private final String label;private final int owner;private int checks;private String savedId,title;
 private Ports ports;private String recoveredId;private int nextTarget=900;
 private static final class Ports implements MidiConnection.Ports {
  final boolean input,output;final List<byte[]> sent=new ArrayList<>();
  Ports(boolean input,boolean output){this.input=input;this.output=output;}public boolean canReceive(){return input;}public boolean canSend(){return output;}
  public void send(byte[] data){sent.add(data.clone());}public void close(){}
 }
 private CreationSelfTest(MainActivity activity){a=activity;screen=a.screens.creation;ack=new File(a.getCacheDir(),"creation-next");label=a.getIntent().getStringExtra("creation_label")==null?"fixture":a.getIntent().getStringExtra("creation_label");owner=a.user;title="Canção Árvore MIDI "+label+" com título longo para leitura e organização";}
 static void run(MainActivity a,boolean reopen){
  if((a.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
  if(a.midiManager!=null&&a.midiManager.getDevices().length>0)throw new IllegalStateException("Creation test refuses physical devices");
  CreationSelfTest test=new CreationSelfTest(a);if(reopen)test.reopen();else test.start();
 }
 private void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError("Creation: "+reason);}
 private void connect(boolean output){connect(true,output);}
 private void connect(boolean input,boolean output){final Ports opened=new Ports(input,output);ports=opened;a.midiConnection.connect(new MidiConnection.Target(nextTarget++,"SIMULADO C1 · entrada de teste",true),(target,generation,completion)->completion.complete(opened));check(a.midiConnection.snapshot().canReceive==input&&a.midiConnection.snapshot().canSend==output,"simulated ports actually changed");}
 private void start(){connect(true);a.screens.showCreation();await(()->screen.debugIdle(),()->ready("home",this::record),0);}
 private static Button findButton(View view,String text){
  if(view instanceof Button&&((Button)view).getText().toString().contains(text))return (Button)view;
  if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){Button b=findButton(((ViewGroup)view).getChildAt(i),text);if(b!=null)return b;}return null;
 }
 private static EditText findEdit(View view){
  if(view instanceof EditText)return (EditText)view;
  if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){EditText b=findEdit(((ViewGroup)view).getChildAt(i));if(b!=null)return b;}return null;
 }
 private static List<Spinner> spinners(View view){List<Spinner> result=new ArrayList<>();if(view instanceof Spinner)result.add((Spinner)view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)result.addAll(spinners(((ViewGroup)view).getChildAt(i)));return result;}
 private void click(String text){Button button=findButton(screen,text);check(button!=null&&button.isEnabled(),"enabled action "+text);button.requestRectangleOnScreen(new Rect(0,0,button.getWidth(),button.getHeight()),true);button.performClick();}
 private void dialog(String value){check(screen.activeDialog!=null&&screen.activeDialog.isShowing(),"real project dialog");EditText input=findEdit(screen.activeDialog.getWindow().getDecorView());check(input!=null,"dialog input");input.setText(value);screen.activeDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();}
 private static int emptyMessages(View view){int count=view instanceof TextView&&((TextView)view).getText().toString().startsWith("Suas composições aparecerão aqui.")?1:0;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)count+=emptyMessages(((ViewGroup)view).getChildAt(i));return count;}
 private void record(){check(emptyMessages(screen)<=1,"stale recent-load callbacks cannot duplicate empty state");click("Nova gravação");click("Gravar");
  MidiTakeRecorder take=screen.debugTake();check(take.state()==MidiTakeRecorder.State.RECORDING,"recording state");
  final android.media.midi.MidiReceiver input=a.createReceiver(a.midiGeneration);
  Thread receiver=new Thread(()->{try{input.send(new byte[]{(byte)0x90,60,91,(byte)0x90,64,75,(byte)0x91,60,66,(byte)0xb0,64,127},0,12,12345);}catch(IOException error){throw new RuntimeException(error);}});
  receiver.start();try{receiver.join(1000);}catch(InterruptedException error){throw new RuntimeException(error);}
  check(!receiver.isAlive()&&take.noteCount()==3,"capture occurs on reception before the UI queue is drained");
  a.handler.postDelayed(()->{check(!findButton(screen,"Gravar").isEnabled(),"cannot replace an unsaved take");ready("recording",this::pause);},150);
 }
 private void pause(){click("Pausar");int n=screen.debugTake().eventCount();a.onMidiMessage(0x90,75,99);check(screen.debugTake().eventCount()==n,"pause rejects capture");ready("paused",this::resume);}
 private void resume(){click("Continuar");a.onMidiMessage(0x92,67,83);a.handler.postDelayed(()->{
  a.onMidiMessage(0x82,67,0);a.onMidiMessage(0xb0,64,0);click("Parar");check(screen.debugTake().state()==MidiTakeRecorder.State.REVIEW,"review state");ready("review",this::preview);
 },150);}
 private void preview(){int raw=screen.debugTake().eventCount();click("Reproduzir gravação");a.onMidiMessage(0x90,80,50);check(screen.debugTake().eventCount()==raw,"replay cannot enter source recording");
  a.handler.postDelayed(()->{check(ports.sent.stream().anyMatch(b->(b[0]&0xf0)==0x90),"actual output backend received replay");click("Parar");click("Salvar composição");dialog(title);await(()->screen.debugIdle()&&findButton(screen,"Renomear")!=null,()->{
   savedId=screen.debugSelected();try{CreationProjectStore.Project p=new CreationProjectStore(a.getFilesDir()).get(savedId);check(p.profileId==owner&&!p.recoverable,"saved ownership/state");check(p.segments.size()==2,"two recording segments");check(p.events.stream().noneMatch(e->e.data1==75||e.data1==80),"pause/echo excluded");check(p.instrument.contains("SIMULADO"),"observed instrument retained");check(p.events.stream().anyMatch(e->!e.generated&&e.sourceId==900),"input identity retained");MidiFileImport.Result imported=MidiFileImport.read(new ByteArrayInputStream(MidiStandardFileWriter.write(p)));check(imported.notes.size()==4,"all channels/chord notes roundtrip");}catch(Exception e){throw new RuntimeException(e);}ready("saved",this::rename);
  },0);},200);
 }
 private void rename(){click("Renomear");dialog(title+" renomeada");title+=" renomeada";await(()->screen.debugIdle()&&"list".equals(screen.debugPage())&&findButton(screen,title)!=null,()->ready("renamed",this::search),0);}
 private void search(){EditText query=findEdit(screen);query.setText("ARVORE MIDI "+label);spinners(screen).get(1).setSelection(2);
  await(()->findButton(screen,title)!=null,()->{click(title);await(()->findButton(screen,"Renomear")!=null,()->{a.onBackPressed();await(()->"list".equals(screen.debugPage())&&findButton(screen,title)!=null,()->{check(findEdit(screen).getText().toString().equals("ARVORE MIDI "+label),"search survives details/back");check(spinners(screen).get(1).getSelectedItemPosition()==2,"sort survives details/back");ready("searched",this::trash);},0);},0);},0);
 }
 private void trash(){click(title);await(()->findButton(screen,"Mover para Lixeira")!=null,()->{click("Mover para Lixeira");await(()->screen.debugIdle()&&"list".equals(screen.debugPage()),()->{spinners(screen).get(0).setSelection(3);await(()->findButton(screen,title)!=null,()->ready("trash",this::restore),0);},0);},0);}
 private void restore(){click(title);await(()->findButton(screen,"Restaurar")!=null,()->{click("Restaurar");await(()->screen.debugIdle()&&"list".equals(screen.debugPage()),()->{spinners(screen).get(0).setSelection(0);await(()->findButton(screen,title)!=null,()->ready("restored",this::withoutOutput),0);},0);},0);}
 private void withoutOutput(){connect(false);click(title);await(()->findButton(screen,"Reproduzir no piano")!=null,()->{click("Reproduzir no piano");check(a.lastMessageDialog!=null&&a.lastMessageDialog.isShowing(),"no-output explanation");check(ports.sent.isEmpty(),"no fake audio/output");ready("no-output",()->{a.lastMessageDialog.dismiss();withoutInput();});},0);}
 private void withoutInput(){connect(false,true);a.screens.showCreation();await(()->screen.debugIdle(),()->{click("Nova gravação");click("Gravar");check(screen.debugTake().state()==MidiTakeRecorder.State.READY,"output alone cannot record");check(a.lastMessageDialog!=null&&a.lastMessageDialog.isShowing(),"no-input explanation");ready("no-input",()->{a.lastMessageDialog.dismiss();a.onBackPressed();background();});},0);}
 private void background(){connect(true);a.screens.showCreation();await(()->screen.debugIdle(),()->{click("Nova gravação");click("Gravar");a.onMidiMessage(0x90,72,84);a.handler.postDelayed(()->{a.onPause();a.onResume();await(()->screen.debugIdle()&&"home".equals(screen.debugPage()),()->{try{check(!new CreationProjectStore(a.getFilesDir()).list(owner,CreationProjectStore.Filter.RECOVERABLE,"",null).isEmpty(),"background retains recoverable take");}catch(Exception e){throw new RuntimeException(e);}ready("background",this::recover);},0);},100);},0);}
 private void recover(){click("Minhas Criações");findEdit(screen).setText("");spinners(screen).get(0).setSelection(2);spinners(screen).get(1).setSelection(0);
  await(()->findButton(screen,"Gravação interrompida")!=null,()->{click("Gravação interrompida");await(()->findButton(screen,"Revisar e salvar trecho recuperado")!=null,()->{recoveredId=screen.debugSelected();click("Revisar e salvar trecho recuperado");await(()->screen.debugIdle()&&"list".equals(screen.debugPage()),()->{try{check(!new CreationProjectStore(a.getFilesDir()).get(recoveredId).recoverable,"recovery finalized under same ID");}catch(Exception e){throw new RuntimeException(e);}spinners(screen).get(0).setSelection(0);await(()->findButton(screen,"Gravação interrompida")!=null,()->ready("recovered",this::delete),0);},0);},0);},0);
 }
 private void delete(){click("Gravação interrompida");await(()->findButton(screen,"Mover para Lixeira")!=null,()->{click("Mover para Lixeira");await(()->screen.debugIdle()&&"list".equals(screen.debugPage()),()->{spinners(screen).get(0).setSelection(3);await(()->findButton(screen,"Gravação interrompida")!=null,()->{click("Gravação interrompida");await(()->findButton(screen,"Excluir definitivamente")!=null,()->{click("Excluir definitivamente");check(screen.activeDialog.isShowing(),"permanent deletion requires confirmation");screen.activeDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();await(()->screen.debugIdle()&&"list".equals(screen.debugPage()),()->{try{new CreationProjectStore(a.getFilesDir()).get(recoveredId);throw new AssertionError("Deleted project still exists");}catch(FileNotFoundException expected){checks++;}catch(IOException error){throw new RuntimeException(error);}ready("deleted",this::profiles);},0);},0);},0);},0);},0);}
 private void profiles(){int another=a.profileStore().create("C1 SIMULADO "+label);a.activateProfile(another);a.screens.showCreation();await(()->screen.debugIdle(),()->{try{CreationProjectStore store=new CreationProjectStore(a.getFilesDir());check(store.list(another,CreationProjectStore.Filter.ALL,"",null).isEmpty(),"profile isolation");check(store.get(savedId).profileId==owner,"immutable owner");check(a.hasCreationDataForProfile(owner),"profile deletion guard");}catch(Exception e){throw new RuntimeException(e);}ready("profile",()->{a.activateProfile(owner);a.screens.showCreation();await(()->screen.debugIdle(),this::finish,0);});},0);}
 private void finish(){try{JSONObject proof=new JSONObject().put("id",savedId).put("title",title).put("owner",owner).put("checks",checks);Files.write(new File(a.getCacheDir(),"creation-proof.json").toPath(),proof.toString().getBytes(StandardCharsets.UTF_8));a.disconnectMidi();Log.i("PianoLivreCreation","CREATION_SELFTEST_PASS checks="+checks+" synthetic=true");}catch(Exception e){throw new RuntimeException(e);}}
 private void reopen(){try{JSONObject proof=new JSONObject(new String(Files.readAllBytes(new File(a.getCacheDir(),"creation-proof.json").toPath()),StandardCharsets.UTF_8));savedId=proof.getString("id");title=proof.getString("title");CreationProjectStore.Project project=new CreationProjectStore(a.getFilesDir()).get(savedId);check(project.profileId==proof.getInt("owner")&&project.title.equals(title),"persisted project survives process restart");a.screens.showCreation();await(()->screen.debugIdle(),()->{click("Minhas Criações");await(()->findButton(screen,title)!=null,()->{click(title);await(()->findButton(screen,"Exportar MIDI")!=null,()->ready("reopened",()->Log.i("PianoLivreCreation","CREATION_REOPEN_PASS checks="+checks)),0);},0);},0);}catch(Exception e){throw new RuntimeException(e);}}
 static void export(MainActivity a){
  if((a.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0)return;
  if(a.midiManager!=null&&a.midiManager.getDevices().length>0)throw new IllegalStateException("Synthetic export refuses physical devices");
  CreationSelfTest test=new CreationSelfTest(a);
  try{JSONObject proof=new JSONObject(new String(Files.readAllBytes(new File(a.getCacheDir(),"creation-proof.json").toPath()),StandardCharsets.UTF_8));test.title=proof.getString("title");a.screens.showCreation();test.await(()->test.screen.debugIdle(),()->{test.click("Minhas Criações");test.await(()->findButton(test.screen,test.title)!=null,()->{test.click(test.title);test.await(()->findButton(test.screen,"Exportar MIDI")!=null,()->{Log.i("PianoLivreCreation","CREATION_EXPORT_PICKER_LAUNCH");test.click("Exportar MIDI");},0);},0);},0);}catch(Exception error){throw new RuntimeException(error);}
 }
 static void verifyExport(MainActivity a,android.net.Uri uri,byte[] expected)throws Exception{
  byte[] received;try(InputStream in=a.getContentResolver().openInputStream(uri)){received=BackupArchive.readLimited(in,MidiStandardFileWriter.MAX_BYTES);}
  if(!Arrays.equals(received,expected))throw new AssertionError("SAF output differs from source");
  MidiFileImport.Result imported=MidiFileImport.read(new ByteArrayInputStream(received));
  if(imported.notes.size()!=4)throw new AssertionError("SAF MIDI roundtrip lost notes");
  JSONObject proof=new JSONObject().put("sha256",MainActivity.sha256Creation(received)).put("bytes",received.length).put("notes",imported.notes.size()).put("safReadBack",true);
  Files.write(new File(a.getCacheDir(),"creation-export-proof.json").toPath(),proof.toString().getBytes(StandardCharsets.UTF_8));
  Log.i("PianoLivreCreation","CREATION_SAF_EXPORT_PASS "+proof);
 }
 private void await(BooleanSupplier condition,Runnable next,int attempt){if(condition.getAsBoolean()){next.run();return;}if(attempt>150)throw new AssertionError("Creation UI timeout: "+screen.debugPage());a.handler.postDelayed(()->await(condition,next,attempt+1),100);}
 private void ready(String name,Runnable next){ack.delete();a.root.postOnAnimation(()->a.root.postOnAnimation(()->{Log.i("PianoLivreCreation","CREATION_STAGE "+name);waitAck(next,0);}));}
 private void waitAck(Runnable next,int count){if(ack.exists()){ack.delete();next.run();return;}if(count>300)throw new AssertionError("Creation capture ACK timeout");a.handler.postDelayed(()->waitAck(next,count+1),100);}
}

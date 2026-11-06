package br.org.pianolivre;
import android.app.AlertDialog;
import android.media.midi.*;
import java.util.ArrayList;

/** Bloco B da refatoração do MainActivity: descoberta, reconciliação e
 * conexão de dispositivos USB-MIDI (setupMidi, reconcileMidiDevices,
 * connectMidi, disconnectMidi, selectMidi, setMidiStatus) e nomes de
 * dispositivo. Todo o estado (midiManager, midiConnection, midiGeneration,
 * midiDestroyed, midiDeviceCallback) permanece no MainActivity, acessado
 * package-private; PianoScreens, MusicCreationScreen e os autotestes
 * continuam usando os delegados a.selectMidi(), a.disconnectMidi() e
 * a.setMidiStatus(). Comportamento idêntico à versão inline no MainActivity. */
final class ZMidiDevices{
 private final MainActivity a;
 ZMidiDevices(MainActivity activity){a=activity;}
 void setupMidi(){
  if(a.midiManager==null){setMidiStatus("MIDI indisponível neste Android");return;}
  a.midiDeviceCallback=new MidiManager.DeviceCallback(){
   @Override public void onDeviceAdded(MidiDeviceInfo info){reconcileMidiDevices();}
   @Override public void onDeviceRemoved(MidiDeviceInfo info){reconcileMidiDevices();}
   @Override public void onDeviceStatusChanged(MidiDeviceStatus status){
    reconcileMidiDevices();
   }
  };
  a.midiManager.registerDeviceCallback(a.midiDeviceCallback,a.handler);
  reconcileMidiDevices();
 }
 MidiConnection.Target midiTarget(MidiDeviceInfo info){return new MidiConnection.Target(info.getId(),midiInstrumentName(info),info.getType()==MidiDeviceInfo.TYPE_USB);}
 void reconcileMidiDevices(){
  if(a.midiDestroyed||a.midiManager==null)return;
  ArrayList<MidiConnection.Target> available=new ArrayList<>();
  for(MidiDeviceInfo info:a.midiManager.getDevices())available.add(midiTarget(info));
  a.midiConnection.reconcile(available,(target,generation,completion)->{
   MidiDeviceInfo selected=null;
   for(MidiDeviceInfo info:a.midiManager.getDevices())if(info.getId()==target.id){selected=info;break;}
   if(selected==null){completion.complete(null);return;}
   a.midiManager.openDevice(selected,opened->{
    // Do not open ports or attach a receiver to an obsolete async completion.
    if(opened==null){completion.complete(null);return;}
    if(a.midiDestroyed||generation!=a.midiGeneration){try{opened.close();}catch(Exception ignored){}return;}
    completion.complete(new AndroidMidiPorts(opened,a.createReceiver(generation)));
   },a.handler);
  });
 }
 String midiName(MidiDeviceInfo info){
  if(info==null)return "Nenhum";
  String name=midiInstrumentName(info);
  return name.isEmpty()?"Dispositivo MIDI "+info.getId():name;
 }
 String midiInstrumentName(MidiDeviceInfo info){
  if(info==null)return "";
  String name=info.getProperties().getString(MidiDeviceInfo.PROPERTY_NAME);
  return name==null?"":name.trim();
 }
 void setMidiStatus(String description){
  a.connectionDescription=description;
  if(a.status!=null)a.status.setText(description);
  if(a.screens!=null)a.screens.updateMidiIndicator();
  a.refreshMidiDiagnostics();
 }
 void selectMidi(){
  if(a.midiManager==null){a.message("O Android não disponibiliza MIDI");return;}
  MidiDeviceInfo[] infos=a.midiManager.getDevices();
  if(infos.length==0){a.showMidiDiagnostics();return;}
  String[] choices=new String[infos.length+1];
  for(int i=0;i<infos.length;i++){
   MidiDeviceInfo info=infos[i];
   choices[i]=midiName(info)+" · entrada "+info.getOutputPortCount()+" / saída "+info.getInputPortCount();
  }
  choices[infos.length]="Mostrar diagnóstico e teste de áudio";
  new AlertDialog.Builder(a).setTitle("Conexão USB-MIDI")
   .setItems(choices,(dialog,which)->{
    if(which==infos.length)a.showMidiDiagnostics();
    else connectMidi(infos[which]);
   }).setNegativeButton("Cancelar",null).show();
 }
 void connectMidi(MidiDeviceInfo info){
  if(info==null||a.midiManager==null||a.midiDestroyed)return;
  a.midiConnection.connect(midiTarget(info),(target,generation,completion)->
   a.midiManager.openDevice(info,opened->{
    if(opened==null){completion.complete(null);return;}
    if(a.midiDestroyed||generation!=a.midiGeneration){try{opened.close();}catch(Exception ignored){}return;}
    completion.complete(new AndroidMidiPorts(opened,a.createReceiver(generation)));
   },a.handler));
 }
 void disconnectMidi(){a.midiConnection.disconnect("Nenhum dispositivo conectado");}
}

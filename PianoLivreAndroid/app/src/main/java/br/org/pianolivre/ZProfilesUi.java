package br.org.pianolivre;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import java.io.IOException;
import java.util.List;

/** Criação, seleção e gestão de perfis locais (LocalProfiles): novos perfis,
 * carga nos seletores (toolbar e Home), troca de perfil com estado de estudo
 * zerado e última partitura do perfil, e gestão (renomear/avatar/excluir).
 * PianoScreens, CreationSelfTest e MusicCreationScreen continuam usando os
 * delegados loadProfiles/activateProfile/profileStore do MainActivity.
 * Comportamento idêntico à versão inline no MainActivity. */
final class ZProfilesUi{
 private final MainActivity a;
 ZProfilesUi(MainActivity activity){a=activity;}
 void create(){
  EditText input=new EditText(a);input.setSingleLine(true);input.setHint("Nome do perfil");
  new AlertDialog.Builder(a).setTitle("Novo perfil").setView(input).setPositiveButton("Criar",(d,w)->{
   try{int id=a.profileStore().create(input.getText().toString());activate(id);load();}
   catch(RuntimeException ex){a.message(ex.getMessage());}
  }).setNegativeButton("Cancelar",null).show();
 }
 void load(){
  try{
   List<LocalProfiles.Profile> list=a.profileStore().list();String[] names=new String[list.size()];int selected=0;
   int active=ZSessionState.activeProfile(a.prefs);
   for(int i=0;i<list.size();i++){LocalProfiles.Profile p=list.get(i);names[i]=LocalProfiles.AVATARS[p.avatar]+" "+p.name;if(p.id==active)selected=i;}
   a.user=list.get(selected).id;
   if(active!=a.user)ZSessionState.setActiveProfile(a.prefs,a.user);
   a.profiles.setOnItemSelectedListener(null);
   a.profiles.setAdapter(new ArrayAdapter<>(a,android.R.layout.simple_spinner_dropdown_item,names));a.profiles.setSelection(selected);
   if(a.screens!=null&&a.screens.homeProfiles!=null){
    a.screens.homeProfiles.setOnItemSelectedListener(null);
    a.screens.homeProfiles.setAdapter(a.screens.homeProfileAdapter(names));a.screens.homeProfiles.setSelection(selected);
   }
   a.refreshChordWindow();if(a.screens!=null)a.screens.refreshLibrary();
   a.profiles.setOnItemSelectedListener(new MainActivity.Listener(index->selectFromSpinner(index,list)));
   if(a.screens!=null&&a.screens.homeProfiles!=null)
    a.screens.homeProfiles.setOnItemSelectedListener(new MainActivity.Listener(index->selectFromSpinner(index,list)));
  }catch(RuntimeException ex){a.message("Não foi possível ler os perfis: "+ex.getMessage());}
 }
 void selectFromSpinner(int index,List<LocalProfiles.Profile> list){
  if(index<0||index>=list.size())return;
  int id=list.get(index).id;
  if(a.user!=id)activate(id);
  if(a.profiles!=null&&a.profiles.getSelectedItemPosition()!=index)a.profiles.setSelection(index);
  if(a.screens!=null&&a.screens.homeProfiles!=null&&a.screens.homeProfiles.getSelectedItemPosition()!=index)a.screens.homeProfiles.setSelection(index);
 }
 void activate(int id){
  if(a.user==id)return;
  if(a.screens!=null)a.screens.connectionLostCreation();
  a.stopPlayback();a.finishStudyWindow();apply(id);
 }
 void apply(int id){
  a.user=id;ZSessionState.setActiveProfile(a.prefs,id);a.refreshChordWindow();
  a.score=null;a.studyState.score=null;a.studyState.pressed.clear();a.currentFile="";a.currentName="";
  a.route=null;a.routeCursor=null;a.routeHasMarkers=false;a.waitRecognizer.clear();
  a.currentHand=0;a.mode=0;a.tempoFactor=1;a.studyState.hand=0;a.studyState.showKeyboard=false;
  String file=ZSessionState.lastFile(a.prefs,id);
  if(!file.isEmpty())a.loadScore(file,false);
  else{a.position=0;a.loopStart=0;a.loopEnd=Double.MAX_VALUE;a.screens.showHome();a.updateInfo();}
 }
 void manage(){
  try{
   LocalProfiles.Profile current=null;for(LocalProfiles.Profile p:a.profileStore().list())if(p.id==a.user)current=p;
   if(current==null)return;final LocalProfiles.Profile selected=current;
   new AlertDialog.Builder(a).setTitle(selected.name).setItems(new String[]{"Renomear perfil","Escolher avatar","Excluir perfil"},(dialog,which)->{
    if(which==0){
     EditText input=new EditText(a);input.setSingleLine(true);input.setText(selected.name);
     new AlertDialog.Builder(a).setTitle("Renomear perfil").setView(input).setPositiveButton("Salvar",(d,w)->{
      try{a.profileStore().update(selected.id,input.getText().toString(),selected.avatar);load();}catch(RuntimeException ex){a.message(ex.getMessage());}
     }).setNegativeButton("Cancelar",null).show();
    }else if(which==1){
     new AlertDialog.Builder(a).setTitle("Avatar").setItems(LocalProfiles.AVATARS,(d,index)->{
      try{a.profileStore().update(selected.id,selected.name,index);load();}catch(RuntimeException ex){a.message(ex.getMessage());}
     }).show();
    }else new AlertDialog.Builder(a).setTitle("Excluir perfil?")
     .setMessage("Excluir este perfil e seus registros de estudo? As partituras compartilhadas serão mantidas.")
     .setPositiveButton("Excluir",(d,w)->{
      try{if(a.hasCreationDataForProfile(selected.id)){a.message("Este perfil ainda possui composições, inclusive itens na Lixeira ou recuperáveis. Exporte ou exclua definitivamente cada projeto antes de excluir o perfil.");return;}a.stopPlayback();a.finishStudyWindow();int active=a.profileStore().delete(selected.id);a.score=null;apply(active);load();}
      catch(RuntimeException ex){a.message(ex.getMessage());}
      catch(IOException ex){a.message("Não foi possível verificar as composições deste perfil; o perfil foi mantido.");}
     }).setNegativeButton("Cancelar",null).show();
   }).show();
  }catch(RuntimeException ex){a.message(ex.getMessage());}
 }
}

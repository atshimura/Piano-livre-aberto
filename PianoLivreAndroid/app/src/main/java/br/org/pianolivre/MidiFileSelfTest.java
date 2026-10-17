package br.org.pianolivre;
import android.content.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.json.*;
/** Synthetic SMF source, never user data or hardware. */
final class MidiFileSelfTest {
 static byte[] sample()throws Exception{
  byte[] track={0,(byte)0x90,60,80,(byte)0x83,0x60,(byte)0x80,60,0,0,(byte)0xff,0x2f,0};
  ByteArrayOutputStream out=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(out);
  d.writeBytes("MThd");d.writeInt(6);d.writeShort(0);d.writeShort(1);d.writeShort(480);d.writeBytes("MTrk");d.writeInt(track.length);d.write(track);return out.toByteArray();
 }
 static int run(Context context)throws Exception{
  SharedPreferences prefs=context.getSharedPreferences("piano_midifile_selftest",Context.MODE_PRIVATE);
  File root=new File(context.getCacheDir(),"midi_file_selftest"),dir=new File(root,"imported");dir.mkdirs();prefs.edit().clear().putString("profiles","[\"Teste\"]").putInt("active_profile",0).commit();
  try{
   byte[] bytes=sample();MidiFileImport.Result midi=MidiFileImport.read(new ByteArrayInputStream(bytes));
   File score=MidiImportStorage.save(dir,"Synthetic.MID",midi,Collections.singletonMap(0,2));File source=new File(dir,score.getName()+".source.mid");
   if(!Arrays.equals(Files.readAllBytes(source.toPath()),bytes))throw new AssertionError("Original MIDI bytes");
   if(Score.read(new FileInputStream(score),score.getName()).events.get(0).staff!=2)throw new AssertionError("Explicit hand");
   File second=MidiImportStorage.save(dir,"Synthetic.MID",midi,Collections.singletonMap(0,1));
   if(second.equals(score)||!source.exists())throw new AssertionError("Unique names preserve pairs");
   JSONObject backup=BackupArchive.create(prefs,root);BackupArchive.restore(prefs,root,backup);
   if(!Arrays.equals(Files.readAllBytes(source.toPath()),bytes)||!score.exists()||!second.exists())throw new AssertionError("Backup source and notation");
   return 4;
  }finally{
   prefs.edit().clear().commit();for(File f:dir.listFiles())f.delete();dir.delete();root.delete();
  }
 }
}

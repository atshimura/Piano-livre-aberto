import br.org.pianolivre.*;

public final class MidiChannelModeTest {
 static int checks;
 static void check(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  for(int ch=0;ch<16;ch++)for(int pitch=21;pitch<=108;pitch++){
   int other=(ch+1)%16;MidiPerformance p=new MidiPerformance();
   p.noteOn(ch,pitch,80,0,0);p.noteOn(other,pitch,90,0,0);p.control64(ch,127,1);
   check(p.channelMode(ch,123,2),"CC123 recognized");
   check(p.physicalKeysDown()==1&&p.soundingVoices()==2,"release physical key but retain channel sustain");
   p.channelMode(ch,121,3);
   check(!p.pedalDown(ch)&&p.soundingVoices()==1,"reset controller releases pedal, preserves other channel");
   p.channelMode(other,120,4);check(p.physicalKeysDown()==0&&p.soundingVoices()==0,"all sound off scoped channel");
   check(p.completed().size()==2,"two independent spans completed");
   p.noteOn(ch,pitch,80,5,1);p.control64(ch,127,6);p.noteOff(ch,pitch,7);p.channelMode(ch,120,8);
   check(p.soundingVoices()==0&&p.pedalDown(ch),"CC120 silences retained sound without changing controller");
   p.noteOn(ch,pitch,80,9,2);p.channelMode(ch,121,10);
   check(p.physicalKeysDown()==1&&p.soundingVoices()==1,"CC121 does not release a held key");
   p.channelMode(ch,123,11);check(p.soundingVoices()==0,"subsequent all notes off releases key");
   check(!p.channelMode(ch,119,12),"unknown controller does not clear state");
  }
  System.out.println("PASS MidiChannelModeTest "+checks+" assertions: 16 channels, 88 pitches, CC120/121/123, same-pitch isolation and pedal");
 }
}

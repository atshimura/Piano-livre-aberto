package br.org.pianolivre;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/** Very short offline tonal reference; deliberately not a sampled piano. */
final class MusicLearningAudio {
 private volatile int generation;
 private static final int RATE=22050;
 void stop(){generation++;}
 void play(int[] pitches,boolean ordered){
  stop();final int current=generation;
  if(pitches==null||pitches.length==0)return;
  final int[] notes=pitches.clone();
  new Thread(()->{
   final int count=ordered?Math.min(notes.length,16):1;
   for(int i=0;i<count&&current==generation;i++){
    int[] audible=ordered?new int[]{notes[i]}:notes;
    AudioTrack track=null;
    try{
     int millis=ordered?270:550;
     int samples=RATE*millis/1000;
     byte[] pcm=new byte[samples*2];
     int limit=Math.min(audible.length,8);
     for(int n=0;n<samples;n++){
      double t=n/(double)RATE;
      double envelope=Math.min(1.0,Math.min(n/(RATE*.025),(samples-n)/(RATE*.045)));
      double value=0;
      for(int k=0;k<limit;k++)
       value+=Math.sin(2*Math.PI*440*Math.pow(2,(audible[k]-69)/12.0)*t);
      short level=(short)(Math.max(-1,Math.min(1,value/Math.max(1,limit)*envelope))*9500);
      pcm[n*2]=(byte)(level&255);pcm[n*2+1]=(byte)((level>>8)&255);
     }
     track=new AudioTrack(AudioManager.STREAM_MUSIC,RATE,
      AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,
      pcm.length,AudioTrack.MODE_STATIC);
     if(track.getState()!=AudioTrack.STATE_INITIALIZED)break;
     track.write(pcm,0,pcm.length);
     if(current!=generation)break;
     track.play();
     Thread.sleep(millis+25);
    }catch(Exception ignored){
     break; // Gracefully skip unavailable local audio output.
    }finally{
     if(track!=null){
      try{track.stop();}catch(IllegalStateException ignored){}
      track.release();
     }
    }
   }
  },"PianoLivre-LearnAudio").start();
 }
}

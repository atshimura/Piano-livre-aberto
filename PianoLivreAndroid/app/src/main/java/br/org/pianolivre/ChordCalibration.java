package br.org.pianolivre;
import java.util.*;

/**
 * A user-started calibration of chord attack spread.
 * A trial begins on the first Note On, includes distinct pitches overlapping
 * physically, and finishes only when all keys have been released.
 * The suggested window is the WIDEST RECORDED spread (last attack - first).
 *
 * No fixed millisecond threshold is assumed or shipped as a default.
 * The user confirms/edits the value after playing real sample chords.
 */
public final class ChordCalibration {
 private final Set<Integer> keys=new HashSet<>();
 private final Set<Integer> distinct=new HashSet<>();
 private final ArrayList<Long> samples=new ArrayList<>();
 private boolean recording=false;
 private long firstMs=-1,lastMs=-1;
 private static final int MAX_SAMPLES=32;
 public void start(){recording=true;keys.clear();distinct.clear();samples.clear();firstMs=-1;lastMs=-1;}
 public void stop(){recording=false;keys.clear();distinct.clear();firstMs=-1;lastMs=-1;}
 public boolean recording(){return recording;}
 public boolean noteOn(int pitch,long nowMs){
  if(!recording||pitch<0||pitch>127)return false;
  if(keys.add(pitch)){
   distinct.add(pitch);
   if(firstMs<0)firstMs=nowMs;
   lastMs=nowMs;
  }
  return false;
 }
 /** Returns true if a new valid recorded chord trial was stored. */
 public boolean noteOff(int pitch,long nowMs){
  if(!recording||!keys.remove(pitch))return false;
  if(!keys.isEmpty())return false;
  boolean accepted=distinct.size()>=2 && firstMs>=0 &&
    lastMs>firstMs && nowMs>=lastMs && samples.size()<MAX_SAMPLES;
  if(accepted)samples.add(lastMs-firstMs);
  distinct.clear();firstMs=-1;lastMs=-1;
  return accepted;
 }
 public int sampleCount(){return samples.size();}
 public Long observedWindowMs(){
  if(samples.isEmpty())return null;
  long maximum=0;for(long v:samples)maximum=Math.max(maximum,v);
  return maximum>0?maximum:null;
 }
 public List<Long> samples(){return Collections.unmodifiableList(samples);}
}

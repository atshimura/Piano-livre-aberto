package br.org.pianolivre;

/** Byte-stream decoder for standard channel MIDI, independent of Android APIs. */
public final class MidiProtocol {
 public interface Listener { void message(int status, int data1, int data2); }
 public interface EpochListener { void message(int status,int data1,int data2,long startedEpoch); }
 private int runningStatus=0, pendingStatus=0, firstData=0, dataCount=0, dataNeeded=0;
 private boolean sysex=false;
 private long firstDataEpoch;
 private static int length(int status){
  if(status>=0x80&&status<0xF0) return (status&0xF0)==0xC0||(status&0xF0)==0xD0?1:2;
  if(status==0xF1||status==0xF3) return 1;
  if(status==0xF2) return 2;
  return 0;
 }
 public synchronized void feed(byte[] bytes,int offset,int count,Listener listener){
  if(listener==null)throw new IllegalArgumentException("Invalid MIDI listener");
  feed(bytes,offset,count,0,(status,a,b,epoch)->listener.message(status,a,b));
 }
 /** Keep framing/running status while tagging the start of each channel message. */
 public synchronized void feed(byte[] bytes,int offset,int count,long epoch,EpochListener listener){
  if(bytes==null||listener==null||offset<0||count<0||offset+count>bytes.length)
   throw new IllegalArgumentException("Invalid MIDI packet");
  for(int i=offset;i<offset+count;i++){
   int b=bytes[i]&0xff;
   if(b>=0xF8)continue; // Real-time messages can occur anywhere.
   if(sysex){if(b==0xF7)sysex=false;continue;}
   if(b>=0x80){
    if(b==0xF0){sysex=true;runningStatus=0;pendingStatus=0;dataNeeded=0;continue;}
    if(b>=0xF0)runningStatus=0;else runningStatus=b;
    pendingStatus=b;dataCount=0;dataNeeded=length(b);
    continue;
   }
   if(dataNeeded==0&&runningStatus!=0){
    pendingStatus=runningStatus;dataNeeded=length(runningStatus);dataCount=0;
   }
   if(dataNeeded==0)continue;
   if(dataCount==0){firstData=b;firstDataEpoch=epoch;dataCount=1;}
   else dataCount++;
   if(dataCount>=dataNeeded){
    int st=pendingStatus,a=firstData,v=dataNeeded==2?b:0;
    dataNeeded=0;dataCount=0;pendingStatus=0;
    if(st<0xF0)listener.message(st,a,v,firstDataEpoch);
   }
  }
 }
 public synchronized void reset(){runningStatus=0;pendingStatus=0;dataNeeded=0;dataCount=0;sysex=false;}
}

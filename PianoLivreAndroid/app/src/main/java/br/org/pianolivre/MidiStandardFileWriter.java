package br.org.pianolivre;

import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;

/** Writes a bounded Standard MIDI File, format 0, at 960 PPQN and 120 BPM. */
public final class MidiStandardFileWriter {
 public static final int PPQN=960,MAX_BYTES=16*1024*1024;private static final long DEFAULT_TEMPO_US=500_000L;
 private MidiStandardFileWriter(){}
 public static byte[] write(CreationProjectStore.Project project)throws IOException{
  if(project==null||project.events.isEmpty()||project.segments.isEmpty())throw new IOException("Projeto sem eventos ou segmentos de gravação");
  ArrayList<MidiTakeRecorder.Event> events=new ArrayList<>(project.events);events.sort(Comparator.comparingLong((MidiTakeRecorder.Event e)->e.timeNs).thenComparingInt(e->e.order));
  ByteArrayOutputStream track=new ByteArrayOutputStream();writeVlq(track,0);track.write(0xff);track.write(0x51);track.write(3);track.write((int)(DEFAULT_TEMPO_US>>16)&255);track.write((int)(DEFAULT_TEMPO_US>>8)&255);track.write((int)DEFAULT_TEMPO_US&255);
  byte[] title=project.title.getBytes(StandardCharsets.UTF_8);writeVlq(track,0);track.write(0xff);track.write(0x03);writeVlq(track,title.length);track.write(title);
  long previousTick=0;
  for(MidiTakeRecorder.Event e:events){long ns=playbackTime(project.segments,e.timeNs);long tick=Math.round((double)ns*PPQN/DEFAULT_TEMPO_US/1000.0);if(tick<previousTick)throw new IOException("Cronologia MIDI inválida");writeVlq(track,tick-previousTick);previousTick=tick;
   int command=e.command();track.write(e.status);
   if(command==0xc0||command==0xd0)track.write(e.data1);else{track.write(e.data1);track.write(e.data2);}
   if(track.size()>MAX_BYTES-64)throw new IOException("Arquivo MIDI excede o limite");
  }
  long endTick=Math.round((double)project.durationNs*PPQN/DEFAULT_TEMPO_US/1000.0);writeVlq(track,Math.max(0,endTick-previousTick));track.write(0xff);track.write(0x2f);track.write(0);
  ByteArrayOutputStream file=new ByteArrayOutputStream(track.size()+22);DataOutputStream out=new DataOutputStream(file);out.writeBytes("MThd");out.writeInt(6);out.writeShort(0);out.writeShort(1);out.writeShort(PPQN);out.writeBytes("MTrk");out.writeInt(track.size());track.writeTo(out);out.flush();if(file.size()>MAX_BYTES)throw new IOException("Arquivo MIDI excede o limite");return file.toByteArray();
 }
 static long playbackTime(List<MidiTakeRecorder.Segment> segments,long raw)throws IOException{
  long elapsed=0;for(MidiTakeRecorder.Segment s:segments){if(raw<s.startNs)break;if(raw<=s.endNs)return elapsed+(raw-s.startNs);elapsed+=s.endNs-s.startNs;}throw new IOException("Evento fora dos segmentos gravados");
 }
 private static void writeVlq(OutputStream out,long value)throws IOException{if(value<0||value>0x0fffffffL)throw new IOException("Intervalo MIDI não representável");int buffer=(int)(value&127);while((value>>=7)>0)buffer=(buffer<<8)|((int)value&127)|128;for(;;){out.write(buffer&255);if((buffer&128)!=0)buffer>>=8;else break;}}
}

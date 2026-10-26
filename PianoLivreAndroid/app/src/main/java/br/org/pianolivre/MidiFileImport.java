package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded Standard MIDI File reader; no pitch-based hand inference. */
public final class MidiFileImport {
 public static final class Note {
  public final int track,channel,pitch,velocity;public final long start,end;
  Note(int t,int c,int p,int v,long s,long e){track=t;channel=c;pitch=p;velocity=v;start=s;end=e;}
 }
 public static final class Tempo {public final long tick;public final int micros;Tempo(long t,int m){tick=t;micros=m;}}
 public static final class Result {
  public final byte[] original;public final int ppqn,tracks;public final List<Note> notes;public final List<Tempo> tempos;
  Result(byte[] bytes,int p,int t,List<Note> n,List<Tempo> tempo){original=bytes.clone();ppqn=p;tracks=t;notes=Collections.unmodifiableList(n);tempos=Collections.unmodifiableList(tempo);}
  public List<Integer> noteTracks(){TreeSet<Integer> out=new TreeSet<>();for(Note n:notes)out.add(n.track);return new ArrayList<>(out);}
 }
 private static final class Reader {
  final byte[] b;int p,end;Reader(byte[] b,int p,int end){this.b=b;this.p=p;this.end=end;}
  int u()throws IOException{if(p>=end)throw new IOException("MIDI truncado");return b[p++]&255;}
  int u16()throws IOException{return (u()<<8)|u();}
  long u32()throws IOException{return ((long)u()<<24)|((long)u()<<16)|(u()<<8)|u();}
  int vlq()throws IOException{int value=0;for(int i=0;i<4;i++){int c=u();value=(value<<7)|(c&127);if(c<128)return value;}throw new IOException("VLQ MIDI inválido");}
  void skip(int n)throws IOException{if(n<0||n>end-p)throw new IOException("Bloco MIDI truncado");p+=n;}
  void tag(String tag)throws IOException{for(int i=0;i<4;i++)if(u()!=tag.charAt(i))throw new IOException("Bloco MIDI inválido: "+tag);}
  int data()throws IOException{int n=u();if(n>127)throw new IOException("Dado MIDI inválido");return n;}
 }
 public static Result read(InputStream input)throws IOException{
  byte[] bytes=Score.readLimited(input,16*1024*1024);Reader r=new Reader(bytes,0,bytes.length);r.tag("MThd");
  long header=r.u32();if(header!=6)throw new IOException("Cabeçalho MIDI não suportado");
  int format=r.u16(),tracks=r.u16(),ppqn=r.u16();
  if(format>1||tracks<1||tracks>64||(format==0&&tracks!=1))throw new IOException("Suportados MIDI formato 0/1, até 64 trilhas");
  if(ppqn==0||(ppqn&0x8000)!=0)throw new IOException("Divisão MIDI SMPTE não suportada; use PPQN");
  List<Note> notes=new ArrayList<>();List<Tempo> tempos=new ArrayList<>();int events=0;
  for(int track=0;track<tracks;track++){
   r.tag("MTrk");long len=r.u32();if(len>r.end-r.p)throw new IOException("Trilha MIDI truncada");
   Reader t=new Reader(bytes,r.p,r.p+(int)len);r.skip((int)len);long tick=0;int running=0;boolean ended=false;
   Map<Integer,long[]> active=new HashMap<>();
   while(t.p<t.end){
    if(++events>200000)throw new IOException("Eventos MIDI demais");
    tick+=t.vlq();if(tick>ppqn*16000L)throw new IOException("MIDI excede 4000 compassos de 4/4");
    int status=t.u();if(status<128){if(running==0)throw new IOException("Running status sem mensagem anterior");t.p--;status=running;}
    if(status==255){
     running=0;int type=t.u(),size=t.vlq();if(size>t.end-t.p)throw new IOException("Metaevento MIDI truncado");
     if(type==0x2f){if(size!=0||t.p!=t.end)throw new IOException("Fim de trilha MIDI inválido");ended=true;break;}
     if(type==0x51){if(size!=3)throw new IOException("Tempo MIDI inválido");int micro=(t.u()<<16)|(t.u()<<8)|t.u();if(micro==0)throw new IOException("Tempo MIDI zero");tempos.add(new Tempo(tick,micro));}else t.skip(size);
    }else if(status==0xf0||status==0xf7){running=0;t.skip(t.vlq());}
    else if(status>=0x80&&status<=0xef){
     running=status;int channel=status&15,kind=status&0xf0,a=t.data(),b=(kind==0xc0||kind==0xd0)?0:t.data();int key=channel*128+a;
     if(kind==0x90&&b>0){if(active.containsKey(key))throw new IOException("Notas sobrepostas de mesma altura/canal na trilha: notação ambígua");active.put(key,new long[]{tick,b});}
     else if(kind==0x80||(kind==0x90&&b==0)){
      long[] start=active.remove(key);if(start==null||tick<=start[0])throw new IOException("Ataque/fim MIDI sem duração válida");
      notes.add(new Note(track,channel,a,(int)start[1],start[0],tick));if(notes.size()>10000)throw new IOException("Notas MIDI demais");
     }
    }else throw new IOException("Status MIDI não suportado");
   }
   if(!ended||!active.isEmpty())throw new IOException("Trilha sem fim ou com notas sem Note Off");
  }
  if(r.p!=r.end||notes.isEmpty())throw new IOException("MIDI vazio ou com dados após as trilhas");
  notes.sort(Comparator.comparingLong((Note n)->n.start).thenComparingInt(n->n.track).thenComparingInt(n->n.pitch));
  tempos.sort(Comparator.comparingLong(x->x.tick));return new Result(bytes,ppqn,tracks,notes,tempos);
 }
 private static String esc(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
 /** Each note uses a stable voice so exact overlaps survive the approximation. */
 public static byte[] musicXml(Result midi,Map<Integer,Integer> hands,String title)throws IOException{
  List<Note> selected=new ArrayList<>();for(Note n:midi.notes){Integer hand=hands.get(n.track);if(hand==null||hand<0||hand>2)throw new IOException("Escolha explícita de mão para cada trilha é obrigatória");if(hand>0)selected.add(n);}
  if(selected.isEmpty())throw new IOException("Selecione pelo menos uma trilha");
  long bar=midi.ppqn*4L,end=0;for(Note n:selected)end=Math.max(end,n.end);int bars=(int)((end+bar-1)/bar);
  StringBuilder xml=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><score-partwise version=\"4.0\"><work><work-title>").append(esc(title)).append("</work-title></work><identification><miscellaneous><miscellaneous-field name=\"PianoLivre-import\">MIDI: notação aproximada em 4/4; fonte .mid preservada; sem pedal/programas/controladores</miscellaneous-field></miscellaneous></identification><part-list><score-part id=\"P1\"><part-name>Piano</part-name></score-part></part-list><part id=\"P1\">");
  for(int m=0;m<bars;m++){
   long begin=m*bar,stop=begin+bar;xml.append("<measure number=\"").append(m+1).append("\"><attributes><divisions>").append(midi.ppqn).append("</divisions>");
   if(m==0)xml.append("<time><beats>4</beats><beat-type>4</beat-type></time><staves>2</staves><clef number=\"1\"><sign>G</sign><line>2</line></clef><clef number=\"2\"><sign>F</sign><line>4</line></clef>");xml.append("</attributes>");
   if(m==0)xml.append("<direction><sound tempo=\"120\"/></direction>");
   for(Tempo tempo:midi.tempos)if(tempo.tick>=begin&&tempo.tick<stop)xml.append("<direction><offset>").append(tempo.tick-begin).append("</offset><sound tempo=\"").append(60000000.0/tempo.micros).append("\"/></direction>");
   for(int id=0;id<selected.size();id++){
    Note n=selected.get(id);if(n.start>=stop||n.end<=begin)continue;long start=Math.max(n.start,begin),finish=Math.min(n.end,stop),duration=finish-start;
    if(start>begin)xml.append("<forward><duration>").append(start-begin).append("</duration></forward>");
    String[] steps={"C","C","D","D","E","F","F","G","G","A","A","B"};boolean sharp=n.pitch%12==1||n.pitch%12==3||n.pitch%12==6||n.pitch%12==8||n.pitch%12==10;
    xml.append("<note dynamics=\"").append(n.velocity/0.9).append("\"><pitch><step>").append(steps[n.pitch%12]).append("</step>");if(sharp)xml.append("<alter>1</alter>");xml.append("<octave>").append(n.pitch/12-1).append("</octave></pitch><duration>").append(duration).append("</duration>");
    if(n.start<begin)xml.append("<tie type=\"stop\"/>");if(n.end>stop)xml.append("<tie type=\"start\"/>");
    double quarters=(double)duration/midi.ppqn;String type=quarters>=4?"whole":quarters>=2?"half":quarters>=1?"quarter":quarters>=.5?"eighth":quarters>=.25?"16th":quarters>=.125?"32nd":"64th";
    xml.append("<voice>").append(id+1).append("</voice><type>").append(type).append("</type><staff>").append(hands.get(n.track)).append("</staff></note><backup><duration>").append(finish-begin).append("</duration></backup>");
    if(xml.length()>16*1024*1024)throw new IOException("Notação MIDI grande demais");
   }
   xml.append("<forward><duration>").append(bar).append("</duration></forward></measure>");
  }
  xml.append("</part></score-partwise>");return xml.toString().getBytes(StandardCharsets.UTF_8);
 }
}

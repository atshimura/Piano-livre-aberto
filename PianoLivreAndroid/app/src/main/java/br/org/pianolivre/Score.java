package br.org.pianolivre;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Independent MusicXML parser. No code or assets from commercial applications. */
public class Score {
 public String title="Música";
 /** Original MusicXML, without external DTD declaration, for faithful engraving. */
 public String sourceXml="";
 public int tempo=100; // Initial tempo only (legacy UI field).
 public static final class TempoChange {
  public final double beat,bpm;
  public TempoChange(double beat,double bpm){this.beat=beat;this.bpm=bpm;}
 }
 public final ArrayList<TempoChange> tempoChanges=new ArrayList<>();
 private static final class DynamicChange {
  final double beat,percent;final int part,staff;final String voice;
  DynamicChange(double b,double p,int part,int staff,String voice){beat=b;percent=p;this.part=part;this.staff=staff;this.voice=voice;}
 }
 private static int velocity(double percent){return (int)Math.round(Math.max(0,Math.min(127,percent*.9)));}

 /** Simple MusicXML forward/backward barline repeat, on written-score beats. */
 public static final class RepeatSection {
  public final double start,end;public final int times;
  public RepeatSection(double start,double end,int times){
   this.start=start;this.end=end;this.times=times;
  }
 }
 public final ArrayList<RepeatSection> repeatSections=new ArrayList<>();
 public final ArrayList<String> playbackWarnings=new ArrayList<>();
 public final ArrayList<String> importWarnings=new ArrayList<>();
 /** Written measure boundaries and navigation symbols; original beat timeline
  * remains unchanged, even when the performance visits measures out of order. */
 public static final class MeasureInfo {
  public final int index; public final double start,end;
  public final boolean forwardRepeat,backwardRepeat;
  public final int repeatTimes;
  public final Set<Integer> endings;
  public final boolean daCapo,dalSegno,segno,toCoda,coda,fine,expectsFine,expectsCoda;
  public MeasureInfo(int index,double start,double end,boolean forward,boolean backward,
    int times,Set<Integer> endings,boolean dc,boolean ds,boolean segno,
    boolean toCoda,boolean coda,boolean fine,boolean expectsFine,boolean expectsCoda){
   this.index=index;this.start=start;this.end=end;
   forwardRepeat=forward;backwardRepeat=backward;repeatTimes=times;
   this.endings=Collections.unmodifiableSet(new HashSet<>(endings));
   daCapo=dc;dalSegno=ds;this.segno=segno;this.toCoda=toCoda;
   this.coda=coda;this.fine=fine;this.expectsFine=expectsFine;
   this.expectsCoda=expectsCoda;
  }
 }
 public final ArrayList<MeasureInfo> measureInfo=new ArrayList<>();
 private static Set<Integer> endingNumbers(String value){
  Set<Integer> out=new HashSet<>();
  if(value!=null)for(String part:value.split("[,; ]+")){
   try{
    int n=Integer.parseInt(part.trim());
    if(n>=1&&n<=8)out.add(n);
   }catch(Exception ignored){}
  }
  return out;
 }

 public RepeatSection firstRepeatBetween(double from,double through){
  RepeatSection selected=null;
  for(RepeatSection e:repeatSections)
   if(e.end>from+0.000001&&e.end<=through+0.000001
     &&(selected==null||e.end<selected.end))selected=e;
  return selected;
 }
 /** Tempo in quarter notes per minute at the given original score position. */
 public double tempoAt(double beat){
  double bpm=100.0;
  for(TempoChange change:tempoChanges){
   if(change.beat>beat+1e-9)break;
   bpm=change.bpm;
  }
  return bpm;
 }
 /**
  * Exact integration of delta elapsed milliseconds through all internal tempo
  * changes, respecting the user speed factor, rather than applying the final
  * tempo to an entire piece or stepping past a tempo marker in one tick.
  */
 public double advanceBeats(double beat,double elapsedMs,double speed){
  if(!Double.isFinite(beat)||!Double.isFinite(elapsedMs)||!Double.isFinite(speed)
     ||elapsedMs<=0||speed<=0)return beat;
  double remaining=elapsedMs,position=beat;
  for(int i=0;i<tempoChanges.size()+2&&remaining>1e-8;i++){
   double bpm=tempoAt(position)*speed;
   double boundary=Double.POSITIVE_INFINITY;
   for(TempoChange change:tempoChanges){
    if(change.beat>position+1e-8){boundary=change.beat;break;}
   }
   double canAdvance=(remaining/60000.0)*bpm;
   if(position+canAdvance<boundary-1e-9||!Double.isFinite(boundary))
    return position+canAdvance;
   double toBoundary=Math.max(0,boundary-position);
   remaining=Math.max(0,remaining-toBoundary*60000.0/bpm);
   position=boundary;
  }
  return position;
 }
 public int measures=0;
 public double durationBeats=0;
 public final ArrayList<Event> events=new ArrayList<>();
 /** Original MusicXML note segments, including tie continuations; never used for MIDI attacks. */
 public final ArrayList<Event> writtenEvents=new ArrayList<>();
 public static class Event {
  public double beat, length;
  public int midi, staff, voice, measure, velocity=80;
  public String voiceId;
  /** Written pitch from MusicXML, independent of enharmonic MIDI spelling. */
  public String writtenStep=null;
  public int writtenOctave=Integer.MIN_VALUE;
  public double writtenAlter=Double.NaN;
  public int sourceNoteIndex=-1;
  /** False when this written head continues an already sounding tied note. */
  public boolean playbackAttack=true;
  private int sourcePart;private boolean velocityOverride;
  public boolean tieStart, tieStop;
  public Event(double b,double l,int n,int s,int v,int m){beat=b;length=l;midi=n;staff=s;voice=v;voiceId=String.valueOf(v);measure=m;}
 }
 private static Event writtenCopy(Event e,boolean attack){
  Event copy=new Event(e.beat,e.length,e.midi,e.staff,e.voice,e.measure);
  copy.voiceId=e.voiceId;copy.sourcePart=e.sourcePart;
  copy.velocity=e.velocity;copy.velocityOverride=e.velocityOverride;
  copy.tieStart=e.tieStart;copy.tieStop=e.tieStop;
  copy.writtenStep=e.writtenStep;copy.writtenAlter=e.writtenAlter;
  copy.writtenOctave=e.writtenOctave;copy.sourceNoteIndex=e.sourceNoteIndex;
  copy.playbackAttack=attack;
  return copy;
 }
 private static String text(Element e,String tag,String def){NodeList n=e.getElementsByTagName(tag);return n.getLength()>0?n.item(0).getTextContent().trim():def;}
 private static int number(String s,int fallback){try{return Integer.parseInt(s.trim());}catch(Exception e){return fallback;}}
 private static double decimal(String s,double fallback){
  try{double value=Double.parseDouble(s.trim());return Double.isFinite(value)?value:fallback;}
  catch(Exception e){return fallback;}
 }
 private static Element child(Element e,String tag){
  if(e==null)return null;NodeList nodes=e.getChildNodes();
  for(int i=0;i<nodes.getLength();i++)if(nodes.item(i) instanceof Element&&((Element)nodes.item(i)).getTagName().equals(tag))return (Element)nodes.item(i);
  return null;
 }
 private static Element first(Element e,String tag){NodeList n=e.getElementsByTagName(tag);return n.getLength()>0?(Element)n.item(0):null;}
 /** Convert a printed beat-unit mark to quarter-notes per minute. */
 private static Double metronomeQuarterBpm(Element direction){
  Element metronome=first(direction,"metronome");
  if(metronome==null)return null;
  try{
   double printed=Double.parseDouble(text(metronome,"per-minute",""));
   String beatUnit=text(metronome,"beat-unit","quarter");
   double factor;
   switch(beatUnit){
    case "whole":factor=4;break;
    case "half":factor=2;break;
    case "quarter":factor=1;break;
    case "eighth":factor=.5;break;
    case "16th":factor=.25;break;
    case "32nd":factor=.125;break;
    default:return null;
   }
   int dots=metronome.getElementsByTagName("beat-unit-dot").getLength();
   double dotFactor=1,part=.5;
   for(int i=0;i<Math.min(3,dots);i++){dotFactor+=part;part/=2;}
   return printed*factor*dotFactor;
  }catch(Exception ignored){return null;}
 }
 private static boolean direct(Element e,String tag){NodeList n=e.getChildNodes();for(int i=0;i<n.getLength();i++)if(n.item(i) instanceof Element && ((Element)n.item(i)).getTagName().equals(tag))return true;return false;}
 public static Score read(InputStream input,String fileName)throws Exception{
  byte[] bytes=readLimited(input,16*1024*1024); if(fileName.toLowerCase(Locale.ROOT).endsWith(".mxl")|| (bytes.length>3 && bytes[0]=='P'&&bytes[1]=='K')){
   Map<String,byte[]> entries=new LinkedHashMap<>();int total=0,count=0;
   try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry entry;
    while((entry=z.getNextEntry())!=null){
     if(++count>256)throw new IOException("MXL com entradas demais");
     String n=entry.getName();if(n.startsWith("/")||n.contains("\\")||Arrays.asList(n.split("/")).contains("..")||entries.containsKey(n))throw new IOException("Caminho MXL inválido ou duplicado");
     byte[] value=readLimited(z,16*1024*1024-total);total+=value.length;entries.put(n,value);
    }
   }
   String target=null;byte[] container=entries.get("META-INF/container.xml");
   if(container!=null){
    DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setExpandEntityReferences(false);
    String description=decodeXml(container);
    if(description.contains("<!DOCTYPE")||description.contains("<!ENTITY"))throw new IOException("Container MXL com DTD não permitido");
    preflight(description);
    NodeList roots=factory.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(description))).getElementsByTagName("rootfile");
    for(int i=0;i<roots.getLength();i++){Element r=(Element)roots.item(i);if(r.getAttribute("media-type").equals("application/vnd.recordare.musicxml+xml")){target=r.getAttribute("full-path");break;}}
    if(target==null&&roots.getLength()==1)target=((Element)roots.item(0)).getAttribute("full-path");
    if(target==null||!entries.containsKey(target))throw new IOException("Rootfile MXL ausente ou incompatível");
   }else{
    for(String n:entries.keySet())if(!n.startsWith("META-INF/")&&(n.toLowerCase(Locale.ROOT).endsWith(".xml")||n.toLowerCase(Locale.ROOT).endsWith(".musicxml"))){
     if(target!=null)throw new IOException("MXL ambíguo sem container.xml");target=n;
    }
   }
   if(target==null)throw new IOException("MXL sem MusicXML válido");bytes=entries.get(target);
  }
  // MusicXML often declares a standard PUBLIC DTD. Strip only the known,
  // single-line external declaration; never resolve or expand external DTDs.
  String xml=decodeXml(bytes);
  int dtdAt=xml.indexOf("<!DOCTYPE");
  if(dtdAt>=0){
   int close=xml.indexOf('>',dtdAt);
   if(close<0)throw new IOException("DOCTYPE inválido");
   String declaration=xml.substring(dtdAt,close+1);
   if(!declaration.startsWith("<!DOCTYPE score-partwise PUBLIC ")||declaration.contains("[")||declaration.indexOf('\n')>=0)throw new IOException("DOCTYPE não permitido");
   xml=xml.substring(0,dtdAt)+xml.substring(close+1);
  }
  if(xml.contains("<!DOCTYPE"))throw new IOException("Declaração DTD não permitida");
  DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
  // Not every Android XML provider implements these SAX features.
  // The XML above has already had the standard external DOCTYPE removed;
  // any other DOCTYPE is rejected before it reaches the parser.
  try{f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);}catch(javax.xml.parsers.ParserConfigurationException ignored){}
  try{f.setFeature("http://xml.org/sax/features/external-general-entities",false);}catch(javax.xml.parsers.ParserConfigurationException ignored){}
  try{f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);}catch(javax.xml.parsers.ParserConfigurationException ignored){}
  f.setExpandEntityReferences(false);
  preflight(xml);
  Document doc=f.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(xml))); Score score=new Score();score.sourceXml=xml;
  Element root=doc.getDocumentElement();if(!root.getTagName().equals("score-partwise"))throw new IOException("Formato MusicXML não suportado: esperado score-partwise");score.title=text(root,"work-title",text(root,"movement-title",fileName));
  NodeList alterations=root.getElementsByTagName("alter");
  for(int i=0;i<alterations.getLength();i++){
   double alteration=decimal(alterations.item(i).getTextContent(),Double.NaN);
   if(!Double.isFinite(alteration)||alteration!=Math.rint(alteration)||Math.abs(alteration)>2)throw new IOException("Alteração microtonal ou fora de duplo acidente não suportada; fonte preservada");
  }
  NodeList transposes=root.getElementsByTagName("transpose");
  for(int i=0;i<transposes.getLength();i++){
   Element transpose=(Element)transposes.item(i);
   for(String tag:new String[]{"chromatic","diatonic","octave-change"})if(decimal(text(transpose,tag,"0"),Double.NaN)!=0)throw new IOException("Instrumento transpositor não suportado; importe versão em som real para piano");
   if(first(transpose,"double")!=null)throw new IOException("Transposição dobrada não suportada");
  }
  NodeList signatures=root.getElementsByTagName("time");
  for(int i=0;i<signatures.getLength();i++){
   Element time=(Element)signatures.item(i);
   if(time.getElementsByTagName("beats").getLength()!=1||time.getElementsByTagName("beat-type").getLength()!=1
     ||!text(time,"beats","").trim().matches("[1-9][0-9]?")||!Arrays.asList("1","2","4","8","16","32","64").contains(text(time,"beat-type","").trim()))
    throw new IOException("Compasso aditivo, misto ou sem métrica não suportado; fonte preservada");
  }
  NodeList parts=root.getElementsByTagName("part");double base=0;
  ArrayList<TempoChange> reportedTempos=new ArrayList<>();
  ArrayList<DynamicChange> reportedDynamics=new ArrayList<>();
  // Staff numbers are local to each MusicXML part, not global hands.
  if(parts.getLength()>2)throw new IOException("Mais de duas partes requerem atribuição manual das mãos");
  boolean splitHands=parts.getLength()==2;
  if(splitHands)for(int pi=0;pi<2;pi++){
   Element p=(Element)parts.item(pi);
   NodeList staves=p.getElementsByTagName("staves");
   for(int i=0;i<staves.getLength();i++)if(number(staves.item(i).getTextContent(),1)>1)
    throw new IOException("Parte com múltiplas pautas requer atribuição manual das mãos");
   NodeList staff=p.getElementsByTagName("staff");
   for(int i=0;i<staff.getLength();i++)if(number(staff.item(i).getTextContent(),1)>1)
    throw new IOException("Parte com múltiplas pautas requer atribuição manual das mãos");
  }
  for(int pi=0;pi<parts.getLength();pi++){
   Element part=(Element)parts.item(pi);NodeList measures=part.getElementsByTagName("measure");base=0;double divisions=1;int lastMidi=60;int beatsPerMeasure=4;int beatType=4;
   double repeatStart=0;boolean openForwardRepeat=false;
   Set<Integer> activeEndings=new HashSet<>();
   for(int mi=0;mi<measures.getLength();mi++){
    Element meas=(Element)measures.item(mi);score.measures=Math.max(score.measures,mi+1);
    if(pi==0){
     NodeList barlines=meas.getElementsByTagName("barline");
     for(int bi=0;bi<barlines.getLength();bi++){
      Element bar=(Element)barlines.item(bi);
      if(first(bar,"ending")!=null&&!score.playbackWarnings.contains("Casas de repetição")){
       score.playbackWarnings.add("Casas de repetição");
      }
      Element repeat=first(bar,"repeat");
      if(repeat!=null&&"forward".equals(repeat.getAttribute("direction"))){
       if(openForwardRepeat){
        score.playbackWarnings.add("Repetições aninhadas não suportadas");
       }else{repeatStart=base;openForwardRepeat=true;}
      }
     }
    }

    boolean navForward=false,navBackward=false,navDc=false,navDs=false;
    boolean navSegno=false,navToCoda=false,navCoda=false,navFine=false;
    boolean navAlFine=false,navAlCoda=false;
    int navTimes=2;
    Set<Integer> currentEndings=new HashSet<>(activeEndings);
    if(pi==0){
     // Left barline starts an ending in THIS measure. Right barline
     // start begins the NEXT measure; stop/discontinue ends this one.
     NodeList bars=meas.getElementsByTagName("barline");
     for(int bi=0;bi<bars.getLength();bi++){
      Element b=(Element)bars.item(bi);
      if(!"left".equals(b.getAttribute("location")))continue;
      Element ending=first(b,"ending");
      if(ending!=null&&"start".equals(ending.getAttribute("type"))){
       currentEndings.addAll(endingNumbers(ending.getAttribute("number")));
      }
     }
    }
    NodeList children=meas.getChildNodes();double cursor=0;double lastOnset=0;double max=0;
    for(int ci=0;ci<children.getLength();ci++){
     Node node=children.item(ci);if(!(node instanceof Element))continue;Element el=(Element)node;String tag=el.getTagName();
     if(tag.equals("attributes")){double newDivisions=decimal(text(el,"divisions",""+divisions),divisions);if(newDivisions>0)divisions=newDivisions;Element time=first(el,"time");if(time!=null){beatsPerMeasure=number(text(time,"beats","4"),4);beatType=number(text(time,"beat-type","4"),4);}}
     if(tag.equals("direction")||tag.equals("sound")){
      if(pi==0&&tag.equals("direction")){
       Element directionType=first(el,"direction-type");
       String words=directionType==null?"":text(directionType,"words","").toLowerCase(Locale.ROOT);
       Element playbackSound=first(el,"sound");
       Element navType=first(el,"direction-type");
       navSegno|=navType!=null&&first(navType,"segno")!=null;
       navCoda|=navType!=null&&first(navType,"coda")!=null;
       navSegno|=playbackSound!=null&&playbackSound.hasAttribute("segno");
       navCoda|=playbackSound!=null&&playbackSound.hasAttribute("coda");
       navDc|=words.contains("d.c.")||words.contains("da capo")||
          playbackSound!=null&&playbackSound.hasAttribute("dacapo");
       navDs|=words.contains("d.s.")||words.contains("dal segno")||
          playbackSound!=null&&playbackSound.hasAttribute("dalsegno");
       navToCoda|=words.contains("to coda")||words.contains("à coda")||
          playbackSound!=null&&playbackSound.hasAttribute("tocoda");
       navAlFine|=words.contains("al fine");
       navAlCoda|=words.contains("al coda");
       navFine|=words.equals("fine")||
          playbackSound!=null&&playbackSound.hasAttribute("fine");
       if((words.contains("d.c.")||words.contains("d.s.")||words.contains("da capo")||words.contains("dal segno")||words.contains("al coda")||words.contains("al fine"))
         ||(playbackSound!=null&&(playbackSound.hasAttribute("dacapo")
           ||playbackSound.hasAttribute("dalsegno")||playbackSound.hasAttribute("tocoda")
           ||playbackSound.hasAttribute("fine")))){
        if(!score.playbackWarnings.contains("D.C./D.S./Coda/Fine"))
         score.playbackWarnings.add("D.C./D.S./Coda/Fine");
       }
      }
      Element sound=tag.equals("sound")?el:first(el,"sound");
      Element soundOffset=child(sound,"offset"),directionOffset=child(el,"offset");
      Element effectiveOffset=soundOffset!=null?soundOffset:directionOffset;
      double musicalOffset=decimal(effectiveOffset==null?"0":effectiveOffset.getTextContent(),0)/divisions;
      if(sound!=null&&sound.hasAttribute("dynamics")){
       double percent=decimal(sound.getAttribute("dynamics"),Double.NaN);
       if(Double.isFinite(percent)&&percent>=0){
        int dynamicStaff=splitHands?pi+1:number(text(el,"staff","0"),0);
        reportedDynamics.add(new DynamicChange(Math.max(0,base+cursor+musicalOffset),percent,pi,dynamicStaff,text(el,"voice","")));
       }
      }
      Double bpm=null;
      if(sound!=null&&sound.hasAttribute("tempo")){
       try{bpm=Double.parseDouble(sound.getAttribute("tempo"));}catch(Exception ignored){}
      }
      if(bpm==null&&tag.equals("direction"))bpm=metronomeQuarterBpm(el);
      if(bpm!=null&&Double.isFinite(bpm)&&bpm>=10&&bpm<=600){
       reportedTempos.add(new TempoChange(Math.max(0,base+cursor+musicalOffset),bpm));
      }
     }
     if(tag.equals("backup")){cursor-=decimal(text(el,"duration","0"),0)/divisions;}
     if(tag.equals("forward")){cursor+=decimal(text(el,"duration","0"),0)/divisions;max=Math.max(max,cursor);}
     if(!tag.equals("note"))continue;
     boolean chord=direct(el,"chord"),rest=direct(el,"rest"),grace=direct(el,"grace");
     double dur=decimal(text(el,"duration","0"),0)/divisions;double onset=chord?lastOnset:cursor;
     if(!chord)lastOnset=cursor;
     if(!rest){Element pitch=first(el,"pitch");if(pitch!=null){
       String step=text(pitch,"step","C");// Map note spelling to MIDI semitone offset.
       int offset=0;switch(step){case "D":offset=2;break;case "E":offset=4;break;case "F":offset=5;break;case "G":offset=7;break;case "A":offset=9;break;case "B":offset=11;break;}
       int n=12*(number(text(pitch,"octave","4"),4)+1)+offset+(int)Math.rint(decimal(text(pitch,"alter","0"),0));lastMidi=n;
       int localStaff=number(text(el,"staff",pi==0?"1":"2"),1);int staff=splitHands?pi+1:localStaff;int voice=number(text(el,"voice","1"),1);
       Event ev=new Event(base+onset,dur,n,staff,voice,mi+1);ev.voiceId=text(el,"voice","1");ev.sourcePart=pi;
       ev.writtenStep=step;ev.writtenOctave=number(text(pitch,"octave","4"),4);
       ev.writtenAlter=decimal(text(pitch,"alter","0"),0);ev.sourceNoteIndex=ci;
       double noteDynamics=decimal(el.getAttribute("dynamics"),Double.NaN);
       if(Double.isFinite(noteDynamics)&&noteDynamics>=0){ev.velocity=velocity(noteDynamics);ev.velocityOverride=true;}
       NodeList ties=el.getElementsByTagName("tie");for(int ti=0;ti<ties.getLength();ti++){Element tie=(Element)ties.item(ti);if("start".equals(tie.getAttribute("type")))ev.tieStart=true;if("stop".equals(tie.getAttribute("type")))ev.tieStop=true;}
       if(!grace)score.events.add(ev);
     }}
     if(!chord&&!grace){cursor+=dur;max=Math.max(max,cursor);}
    }
    double span=Math.max(max,beatsPerMeasure*4.0/beatType);
    if(mi==0&&"yes".equals(meas.getAttribute("implicit"))&&max>0)span=max;
    if(pi==0){
     NodeList barlines=meas.getElementsByTagName("barline");
     for(int bi=0;bi<barlines.getLength();bi++){
      Element bar=(Element)barlines.item(bi);
      Element repeat=first(bar,"repeat");
      if(repeat!=null&&"backward".equals(repeat.getAttribute("direction"))){
       int times=number(repeat.getAttribute("times"),2);
       if(times<1||times>8){
        score.playbackWarnings.add("Quantidade de repetições fora do limite de 1 a 8");
       }else if(base+span>repeatStart+.0001){
        score.repeatSections.add(new RepeatSection(repeatStart,base+span,times));
       }
       openForwardRepeat=false;repeatStart=base+span;
      }
     }
    }
    if(pi==0){
     Set<Integer> nextEndings=new HashSet<>(currentEndings);
     NodeList bars=meas.getElementsByTagName("barline");
     for(int bi=0;bi<bars.getLength();bi++){
      Element b=(Element)bars.item(bi),repeat=first(b,"repeat"),ending=first(b,"ending");
      if(repeat!=null){
       if("forward".equals(repeat.getAttribute("direction")))navForward=true;
       if("backward".equals(repeat.getAttribute("direction"))){
        navBackward=true;navTimes=number(repeat.getAttribute("times"),2);
       }
      }
      if(ending!=null){
       String type=ending.getAttribute("type");
       Set<Integer> values=endingNumbers(ending.getAttribute("number"));
       if("start".equals(type)&&!"left".equals(b.getAttribute("location")))
        nextEndings.addAll(values);
       if("stop".equals(type)||"discontinue".equals(type)){
        if(values.isEmpty())nextEndings.clear();
        else nextEndings.removeAll(values);
       }
      }
     }
     score.measureInfo.add(new MeasureInfo(mi,base,base+span,navForward,navBackward,
       navTimes,currentEndings,navDc,navDs,navSegno,navToCoda,navCoda,navFine,
       navAlFine,navAlCoda));
     activeEndings=nextEndings;
    }
    base+=span;
   }
   if(pi==0)score.durationBeats=base;
  }
  reportedDynamics.sort(Comparator.comparingDouble(d->d.beat));
  for(Event e:score.events)if(!e.velocityOverride){
   for(DynamicChange change:reportedDynamics){
    if(change.beat>e.beat+1e-8)break;
    if(change.part==e.sourcePart&&(change.staff==0||change.staff==e.staff)
       &&(change.voice.isEmpty()||change.voice.equals(e.voiceId)))e.velocity=velocity(change.percent);
   }
  }
  score.events.sort(Comparator.<Event>comparingDouble(e->e.beat).thenComparingInt(e->e.staff));
  // MusicXML ties extend an existing key press, rather than attack the same
  // note again at each measure boundary.
  Map<String,Event> sustained=new HashMap<>();
  ArrayList<Event> merged=new ArrayList<>();
  for(Event e:score.events){
   String key=e.staff+":"+e.voiceId+":"+e.midi;
   Event prior=e.tieStop?sustained.get(key):null;
   // Snapshot before merging/modifying length or tie flags, preserving the
   // graphical segment without manufacturing an extra MIDI attack.
   score.writtenEvents.add(writtenCopy(e,prior==null));
   if(prior!=null){
    prior.length=Math.max(prior.length,e.beat+e.length-prior.beat);
    if(!e.tieStart)sustained.remove(key);
   }else{
    if(e.tieStop){
     // An excerpt can start with a stop whose start is outside the file.
     // Keep this visible note playable instead of silently hiding it in Wait.
     e.tieStop=false;
     String warning="Ligadura sem início correspondente: nota mantida como novo ataque";
     if(!score.importWarnings.contains(warning))score.importWarnings.add(warning);
    }
    merged.add(e);
    if(e.tieStart)sustained.put(key,e);
   }
  }
  score.events.clear();score.events.addAll(merged);
  // Parts often repeat the same direction. Avoid multiple changes at an onset.
  reportedTempos.sort(Comparator.comparingDouble(t->t.beat));
  for(TempoChange change:reportedTempos){
   if(!score.tempoChanges.isEmpty()&&
      Math.abs(score.tempoChanges.get(score.tempoChanges.size()-1).beat-change.beat)<1e-8){
    // The first part owns a conflicting instruction at the same beat.
    continue;
   }
   score.tempoChanges.add(change);
  }
  score.tempo=(int)Math.round(score.tempoAt(0));
  if(!score.playbackWarnings.isEmpty())score.repeatSections.clear();
  return score;
 }
 private static String decodeXml(byte[] bytes)throws IOException{
  java.nio.charset.Charset charset=java.nio.charset.StandardCharsets.UTF_8;boolean detected=false;
  if(bytes.length>=2&&((bytes[0]&255)==255&&(bytes[1]&255)==254||(bytes[0]&255)==254&&(bytes[1]&255)==255)){
   charset=(bytes[0]&255)==255?java.nio.charset.StandardCharsets.UTF_16LE:java.nio.charset.StandardCharsets.UTF_16BE;detected=true;
  }else if(bytes.length>=4&&bytes[0]==0&&bytes[1]=='<'&&bytes[2]==0&&bytes[3]=='?'){
   charset=java.nio.charset.StandardCharsets.UTF_16BE;detected=true;
  }else if(bytes.length>=4&&bytes[0]=='<'&&bytes[1]==0&&bytes[2]=='?'&&bytes[3]==0){
   charset=java.nio.charset.StandardCharsets.UTF_16LE;detected=true;
  }else if(bytes.length>=3&&(bytes[0]&255)==239&&(bytes[1]&255)==187&&(bytes[2]&255)==191)detected=true;
  if(!detected){
   String header=new String(bytes,0,Math.min(bytes.length,512),java.nio.charset.StandardCharsets.ISO_8859_1);
   java.util.regex.Matcher declaration=java.util.regex.Pattern.compile("(?is)^\\s*<\\?xml\\b[^?]*\\bencoding\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(header);
   if(declaration.find()){
    try{charset=java.nio.charset.Charset.forName(declaration.group(1));}catch(IllegalArgumentException ex){throw new IOException("Codificação XML não suportada",ex);}
   }
  }
  if(!Arrays.asList("UTF-8","UTF-16","UTF-16LE","UTF-16BE","ISO-8859-1","US-ASCII").contains(charset.name()))throw new IOException("Codificação XML não suportada: "+charset.name());
  String xml;
  try{xml=charset.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString();}
  catch(java.nio.charset.CharacterCodingException ex){throw new IOException("Bytes inválidos para codificação XML",ex);}
  return xml.startsWith("\ufeff")?xml.substring(1):xml;
 }
 private static void preflight(String xml)throws Exception{
  javax.xml.parsers.SAXParserFactory sax=javax.xml.parsers.SAXParserFactory.newInstance();
  sax.setNamespaceAware(false);
  sax.newSAXParser().parse(new org.xml.sax.InputSource(new StringReader(xml)),new org.xml.sax.helpers.DefaultHandler(){
   int depth=0,nodes=0;
   @Override public void startElement(String uri,String local,String name,org.xml.sax.Attributes attrs)throws org.xml.sax.SAXException{
    if(++depth>128||++nodes>200000)throw new org.xml.sax.SAXException("MusicXML complexo demais");
   }
   @Override public void endElement(String uri,String local,String name){depth--;}
   @Override public org.xml.sax.InputSource resolveEntity(String publicId,String systemId)throws org.xml.sax.SAXException{throw new org.xml.sax.SAXException("Entidades externas não permitidas");}
  });
 }
 public static byte[] readLimited(InputStream in,int limit)throws IOException{
  if(in==null)throw new IOException("Arquivo indisponível");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
  while((n=in.read(buffer))!=-1){if(n>limit-out.size())throw new IOException("Partitura excede limite de 16 MiB");out.write(buffer,0,n);}return out.toByteArray();
 }
 public ArrayList<Event> at(double position,int selectedHand){ArrayList<Event> out=new ArrayList<>();for(Event e:events)if(Math.abs(e.beat-position)<0.01 && (selectedHand==0||(selectedHand==1&&e.staff==1)||(selectedHand==2&&e.staff!=1))&&!e.tieStop)out.add(e);return out;}
 public double firstOnset(int hand){double m=Double.POSITIVE_INFINITY;for(Event e:events)if(matches(e,hand)&&!e.tieStop)m=Math.min(m,e.beat);return Double.isFinite(m)?m:0;}
 private boolean matches(Event e,int hand){return hand==0||(hand==1&&e.staff==1)||(hand==2&&e.staff!=1);}
 public double nextForHand(double position,int hand){for(Event e:events)if(e.beat>position+.005&&matches(e,hand)&&!e.tieStop)return e.beat;return position;}
 public double next(double position){for(Event e:events)if(e.beat>position+.005)return e.beat;return position;}
 public double end(){double m=durationBeats;for(Event e:events)m=Math.max(m,e.beat+e.length);return m;}
}

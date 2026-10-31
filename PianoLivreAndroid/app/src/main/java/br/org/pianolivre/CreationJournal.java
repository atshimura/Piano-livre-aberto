package br.org.pianolivre;

import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;import java.util.concurrent.*;import java.util.zip.CRC32;

/** Incremental checksummed recovery journal; a damaged tail never invalidates its valid prefix. */
public final class CreationJournal implements AutoCloseable {
 private static final int MAGIC=0x504C4A31,VERSION=3,MAX_BYTES=CreationProjectStore.MAX_PROJECT_BYTES;
 private static final class Pending {final byte[] data;final CountDownLatch barrier;Pending(byte[] d,CountDownLatch b){data=d;barrier=b;}}
 public static final class Recovery {public final String sessionId,title,instrument;public final int profileId;public final long createdAt,startNs;public final List<MidiTakeRecorder.Event> events;public final List<MidiTakeRecorder.Segment> segments;public final boolean truncated;Recovery(String id,String t,int p,long c,long start,List<MidiTakeRecorder.Event> e,List<MidiTakeRecorder.Segment> s,boolean x,String source){instrument=source;sessionId=id;title=t;profileId=p;createdAt=c;startNs=start;events=Collections.unmodifiableList(e);segments=Collections.unmodifiableList(s);truncated=x;}}
 private static final Set<String> ACTIVE=ConcurrentHashMap.newKeySet();
 private final File file;private final String instrument;private final BlockingQueue<Pending> queue=new LinkedBlockingQueue<>(4096);private final Thread writer;private volatile IOException failure;private volatile boolean closing;
 public CreationJournal(File root,String sessionId,String title,int profileId,long createdAt,long startNs)throws IOException{this(root,sessionId,title,profileId,createdAt,startNs,"");}
 public CreationJournal(File root,String sessionId,String title,int profileId,long createdAt,long startNs,String source)throws IOException{
  instrument=source==null?"":source;
  if(sessionId==null||!sessionId.matches("[0-9a-fA-F-]{36}")||profileId<0||createdAt<0||startNs<0||title==null||title.length()>120)throw new IOException("Cabeçalho de recuperação inválido");
  File dir=new File(root,"creation-journals");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Não foi possível criar o diário de gravação");file=new File(dir,sessionId.toLowerCase(Locale.ROOT)+".journal");
  if(!file.createNewFile())throw new IOException("Identificador de diário já existe; arquivo anterior mantido");
  ByteArrayOutputStream header=new ByteArrayOutputStream();try(DataOutputStream h=new DataOutputStream(header)){writeText(h,sessionId,64);writeText(h,title,512);h.writeInt(profileId);h.writeLong(createdAt);h.writeLong(startNs);writeText(h,instrument,512);}
  byte[] head=header.toByteArray();CRC32 headerCrc=new CRC32();headerCrc.update(head);
  try(FileOutputStream f=new FileOutputStream(file,false);DataOutputStream d=new DataOutputStream(f)){d.writeInt(MAGIC);d.writeInt(VERSION);d.writeInt(head.length);d.write(head);d.writeLong(headerCrc.getValue());d.flush();f.getFD().sync();}
  ACTIVE.add(file.getAbsolutePath());writer=new Thread(this::runWriter,"PianoLivre-CreationJournal");writer.setDaemon(true);writer.start();
 }
 public String instrument(){return instrument;}
 public String sessionId(){return file.getName().substring(0,file.getName().length()-8);}
 public boolean append(MidiTakeRecorder.Event e){ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream d=new DataOutputStream(b)){d.writeByte(1);d.writeInt(e.status);d.writeByte(e.data1);d.writeByte(e.data2);d.writeLong(e.timeNs);d.writeLong(e.epoch);d.writeInt(e.order);d.writeBoolean(e.generated);d.writeLong(e.androidTimestampNs);d.writeInt(e.sourceId);}catch(IOException impossible){return false;}return enqueue(b.toByteArray());}
 public boolean append(MidiTakeRecorder.Segment s){ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream d=new DataOutputStream(b)){d.writeByte(2);d.writeLong(s.startNs);d.writeLong(s.endNs);}catch(IOException impossible){return false;}return enqueue(b.toByteArray());}
 public boolean beginSegment(long startNs){ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream d=new DataOutputStream(b)){d.writeByte(3);d.writeLong(startNs);}catch(IOException impossible){return false;}return enqueue(b.toByteArray());}
 public boolean checkpoint(long nowNs){ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream d=new DataOutputStream(b)){d.writeByte(4);d.writeLong(nowNs);}catch(IOException impossible){return false;}return enqueue(b.toByteArray());}
 private boolean enqueue(byte[] b){if(closing||failure!=null)return false;return queue.offer(new Pending(b,null));}
 private void runWriter(){try(RandomAccessFile out=new RandomAccessFile(file,"rw")){while(!closing||!queue.isEmpty()){
   Pending first=queue.poll(100,TimeUnit.MILLISECONDS);if(first==null)continue;ArrayList<Pending> batch=new ArrayList<>();batch.add(first);queue.drainTo(batch,128);
   for(Pending p:batch){if(p.barrier!=null){out.getFD().sync();p.barrier.countDown();continue;}if(out.length()+p.data.length+12>MAX_BYTES)throw new IOException("Diário excede o limite de gravação");CRC32 crc=new CRC32();crc.update(p.data);out.seek(out.length());out.writeInt(p.data.length);out.write(p.data);out.writeLong(crc.getValue());}
   out.getFD().sync();
  }}catch(InterruptedException ex){Thread.currentThread().interrupt();failure=new IOException("Escrita do diário interrompida",ex);}catch(IOException ex){failure=ex;for(Pending p; (p=queue.poll())!=null;)if(p.barrier!=null)p.barrier.countDown();}finally{ACTIVE.remove(file.getAbsolutePath());}}
 public void flush()throws IOException{if(failure!=null)throw failure;CountDownLatch latch=new CountDownLatch(1);if(!queue.offer(new Pending(null,latch)))throw new IOException("Fila do diário cheia");try{if(!latch.await(3,TimeUnit.SECONDS))throw new IOException("Tempo excedido ao confirmar o diário");}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IOException("Confirmação do diário interrompida",ex);}if(failure!=null)throw failure;}
 public void deleteAfterCommit()throws IOException{flush();closing=true;try{writer.join(3000);}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new IOException("Fechamento do diário interrompido",ex);}if(writer.isAlive())throw new IOException("Escritor do diário não encerrou");if(!file.delete())throw new IOException("Não foi possível remover o diário confirmado");}
 public void discardAndDelete()throws IOException{close();if(file.exists()&&!file.delete())throw new IOException("Não foi possível remover o diário descartado");}
 @Override public void close(){closing=true;try{writer.join(3000);}catch(InterruptedException ex){Thread.currentThread().interrupt();}}
 public static boolean isActive(File file){return ACTIVE.contains(file.getAbsolutePath());}
 public static Recovery read(File file)throws IOException{
  if(file.length()>MAX_BYTES+1024)throw new IOException("Diário grande demais");try(RandomAccessFile in=new RandomAccessFile(file,"r")){
   int magic=in.readInt(),version=in.readInt();if(magic!=MAGIC||version<1||version>VERSION)throw new IOException("Formato de diário inválido");
   DataInput header=in;if(version>=3){int size=in.readInt();if(size<32||size>2048)throw new IOException("Cabeçalho de diário inválido");byte[] bytes=new byte[size];in.readFully(bytes);CRC32 crc=new CRC32();crc.update(bytes);if(crc.getValue()!=in.readLong())throw new IOException("Integridade do cabeçalho de diário inválida");header=new DataInputStream(new ByteArrayInputStream(bytes));}
   String id=readText(header,64),title=readText(header,512);int profile=header.readInt();long created=header.readLong(),startNs=header.readLong();String instrument=version>=2?readText(header,512):"";if(profile<0||created<0||startNs<0||!file.getName().equalsIgnoreCase(id+".journal"))throw new IOException("Identidade de diário inválida");
   ArrayList<MidiTakeRecorder.Event> events=new ArrayList<>();ArrayList<MidiTakeRecorder.Segment> segments=new ArrayList<>();boolean truncated=false;long last=-1,activeStart=startNs,checkpoint=startNs;
   while(in.getFilePointer()<in.length()){
    try{int n=in.readInt();if(n<1||n>64||in.length()-in.getFilePointer()<n+8L){truncated=true;break;}byte[] rec=new byte[n];in.readFully(rec);long expected=in.readLong();CRC32 crc=new CRC32();crc.update(rec);if(crc.getValue()!=expected){truncated=true;break;}
     try(DataInputStream d=new DataInputStream(new ByteArrayInputStream(rec))){int type=d.readUnsignedByte();if(type==1){int status=d.readInt(),a=d.readUnsignedByte(),b=d.readUnsignedByte();long time=d.readLong(),epoch=d.readLong();int order=d.readInt();boolean generated=d.readBoolean();long androidTime=d.available()>=12?d.readLong():-1;int source=d.available()>=4?d.readInt():-1;if(status<0x80||status>0xef||a>127||b>127||time<startNs||time<last||epoch<0||order<0)throw new IOException("Evento de diário inválido");if(activeStart<0)activeStart=time;last=time;events.add(new MidiTakeRecorder.Event(status,a,b,time,epoch,order,generated,androidTime,source));if(events.size()>MidiTakeRecorder.MAX_EVENTS)throw new IOException("Diário contém eventos demais");}else if(type==2){long s=d.readLong(),e=d.readLong();if(s<0||e<s)throw new IOException("Segmento inválido");segments.add(new MidiTakeRecorder.Segment(s,e));activeStart=-1;checkpoint=-1;}else if(type==3){long begin=d.readLong();if(activeStart>=0||begin<last)throw new IOException("Invalid segment boundary");activeStart=begin;checkpoint=begin;}else if(type==4){long clock=d.readLong();if(activeStart<0||clock<activeStart||clock<checkpoint)throw new IOException("Checkpoint inválido");checkpoint=clock;}else throw new IOException("Tipo de diário desconhecido");if(d.available()!=0)throw new IOException("Registro de diário inválido");}
    }catch(EOFException tail){truncated=true;break;}
   }
   if(!events.isEmpty()){long lastEvent=Math.max(events.get(events.size()-1).timeNs,checkpoint);if(activeStart>=0&&lastEvent>=activeStart)segments.add(new MidiTakeRecorder.Segment(activeStart,lastEvent));}
   return new Recovery(id,title,profile,created,startNs,events,segments,truncated,instrument);
  }
 }
 private static void writeText(DataOutputStream d,String s,int max)throws IOException{byte[] b=s.getBytes(StandardCharsets.UTF_8);if(b.length>max)throw new IOException("Texto longo demais");d.writeInt(b.length);d.write(b);}
 private static String readText(DataInput d,int max)throws IOException{int n=d.readInt();if(n<0||n>max)throw new IOException("Texto de diário inválido");byte[] b=new byte[n];d.readFully(b);return new String(b,StandardCharsets.UTF_8);}
}

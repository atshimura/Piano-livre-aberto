package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.*;
import java.util.zip.CRC32;

/** Local, bounded project repository. Titles are metadata; UUIDs are immutable storage keys. */
public final class CreationProjectStore {
 public static final int MAX_PROJECTS=200,MAX_EVENTS=MidiTakeRecorder.MAX_EVENTS,MAX_PROJECT_BYTES=16*1024*1024;
 private static final int MAGIC=0x504C4331,FORMAT=2;
 public enum Filter { ALL, SAVED, RECOVERABLE, TRASH }
 public static final class Project {
  public final String id,title;public final int profileId;public final long createdAt,editedAt,durationNs;public final boolean trash,recoverable;public final List<MidiTakeRecorder.Event> events;public final List<MidiTakeRecorder.Segment> segments;
  Project(String i,String t,int p,long c,long e,long d,boolean x,boolean r,List<MidiTakeRecorder.Event> v,List<MidiTakeRecorder.Segment> s){id=i;title=t;profileId=p;createdAt=c;editedAt=e;durationNs=d;trash=x;recoverable=r;events=Collections.unmodifiableList(v);segments=Collections.unmodifiableList(s);}
 }
 private final File root;
 public CreationProjectStore(File filesDir)throws IOException{root=new File(filesDir,"creations");if(!root.exists()&&!root.mkdirs())throw new IOException("Não foi possível criar a biblioteca de composições");if(!root.isDirectory())throw new IOException("Biblioteca de composições inválida");}
 public synchronized Project save(String title,int profileId,long createdAt,long durationNs,List<MidiTakeRecorder.Event> events)throws IOException{
  return saveProject(title,profileId,createdAt,durationNs,events,Collections.emptyList(),false);
 }
 public synchronized Project saveRecoverable(String title,int profileId,long createdAt,long durationNs,List<MidiTakeRecorder.Event> events)throws IOException{
  return saveProject(title,profileId,createdAt,durationNs,events,Collections.emptyList(),true);
 }
 public synchronized Project save(String title,int profileId,long createdAt,long durationNs,List<MidiTakeRecorder.Event> events,List<MidiTakeRecorder.Segment> segments)throws IOException{return saveProject(title,profileId,createdAt,durationNs,events,segments,false);}
 public synchronized Project save(String id,String title,int profileId,long createdAt,long durationNs,List<MidiTakeRecorder.Event> events,List<MidiTakeRecorder.Segment> segments)throws IOException{
  if(id==null||!id.matches("[0-9a-fA-F-]{36}"))throw new IOException("Identificador inválido");if(file(id).exists())throw new IOException("Identificador de composição já existe");validateTitle(title);if(profileId<0||createdAt<0||durationNs<0||events==null||events.isEmpty()||events.size()>MAX_EVENTS)throw new IOException("Composição inválida");if(listAll().size()>=MAX_PROJECTS)throw new IOException("Limite de composições atingido");Project p=new Project(id.toLowerCase(Locale.ROOT),title,profileId,createdAt,createdAt,durationNs,false,false,new ArrayList<>(events),new ArrayList<>(segments));writeAtomic(p,false);return p;
 }
 public synchronized Project saveRecoverable(String title,int profileId,long createdAt,long durationNs,List<MidiTakeRecorder.Event> events,List<MidiTakeRecorder.Segment> segments)throws IOException{return saveProject(title,profileId,createdAt,durationNs,events,segments,true);}
 public synchronized Project saveRecoverable(String id,String title,int profileId,long createdAt,long durationNs,List<MidiTakeRecorder.Event> events,List<MidiTakeRecorder.Segment> segments)throws IOException{
  if(id==null||!id.matches("[0-9a-fA-F-]{36}"))throw new IOException("Identificador de recuperação inválido");File dst=file(id);if(dst.exists()){Project existing=read(dst);if(existing.profileId==profileId&&sameEvents(existing.events,closeEvents(events)))return existing;throw new IOException("Colisão de identificador de recuperação");}
  validateTitle(title);if(profileId<0||createdAt<0||durationNs<0||events==null||events.isEmpty()||events.size()>MAX_EVENTS)throw new IOException("Trecho recuperável inválido");if(listAll().size()>=MAX_PROJECTS)throw new IOException("Limite de composições atingido");Project p=new Project(id.toLowerCase(Locale.ROOT),title,profileId,createdAt,createdAt,durationNs,false,true,new ArrayList<>(events),new ArrayList<>(segments));writeAtomic(p,false);return p;
 }
 private Project saveProject(String title,int profileId,long createdAt,long durationNs,List<MidiTakeRecorder.Event> events,List<MidiTakeRecorder.Segment> segments,boolean recoverable)throws IOException{
  validateTitle(title);if(profileId<0||createdAt<0||durationNs<0||events==null||events.isEmpty()||events.size()>MAX_EVENTS)throw new IOException("Composição inválida");
  if(listAll().size()>=MAX_PROJECTS)throw new IOException("Limite de composições atingido");
  String id=UUID.randomUUID().toString();Project p=new Project(id,title,profileId,createdAt,createdAt,durationNs,false,recoverable,new ArrayList<>(events),new ArrayList<>(segments));writeAtomic(p,false);return p;
 }
 public synchronized Project get(String id)throws IOException{File f=file(id);if(!f.isFile())throw new FileNotFoundException("Composição não encontrada");return read(f);}
 public synchronized boolean hasProjectsForProfile(int profileId)throws IOException{File[] files=root.listFiles((d,n)->n.endsWith(".plc"));if(files==null)return false;for(File f:files)if(read(f,false).profileId==profileId)return true;return false;}
 public synchronized List<Project> snapshotAll()throws IOException{File[] files=root.listFiles((d,n)->n.endsWith(".plc"));ArrayList<Project> out=new ArrayList<>();if(files!=null)for(File f:files)out.add(read(f,false));out.sort(Comparator.comparing(p->p.id));return Collections.unmodifiableList(out);}
 public synchronized byte[] rawFile(String id)throws IOException{File f=file(id);if(!f.isFile()||f.length()>MAX_PROJECT_BYTES+12)throw new IOException("Arquivo de composição inválido");try(InputStream in=new FileInputStream(f)){return readLimited(in,MAX_PROJECT_BYTES+12);}}
 public static void validateDirectory(File dir)throws IOException{if(!dir.exists())return;if(!dir.isDirectory())throw new IOException("Diretório de composições inválido");File[] files=dir.listFiles((d,n)->n.endsWith(".plc"));if(files==null)throw new IOException("Não foi possível validar composições");if(files.length>MAX_PROJECTS)throw new IOException("Muitas composições");for(File f:files)readFileForValidation(f);}
 private static void readFileForValidation(File f)throws IOException{if(f.length()>MAX_PROJECT_BYTES+12)throw new IOException("Projeto excede limite");try(InputStream in=new FileInputStream(f)){readLimited(in,MAX_PROJECT_BYTES+12);}new CreationProjectStore(f.getParentFile().getParentFile()).get(f.getName().substring(0,f.getName().length()-4));}
 private static byte[] readLimited(InputStream in,int max)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(b.size()+n>max)throw new IOException("Projeto grande demais");b.write(buf,0,n);}return b.toByteArray();}
 public synchronized List<Project> list(int profileId,Filter filter,String query,Comparator<Project> order)throws IOException{
  String q=fold(query==null?"":query.trim());ArrayList<Project> out=new ArrayList<>();for(Project p:listAll()){
   if(profileId>=0&&p.profileId!=profileId)continue;
   if(filter==Filter.ALL&&p.trash||filter==Filter.SAVED&&(p.trash||p.recoverable)||filter==Filter.RECOVERABLE&&(!p.recoverable||p.trash)||filter==Filter.TRASH&&!p.trash)continue;
   if(!q.isEmpty()&&!fold(p.title).contains(q))continue;out.add(p);
  }
  if(order!=null)out.sort(order);else out.sort(Comparator.comparingLong((Project p)->p.editedAt).reversed().thenComparing(p->p.id));return Collections.unmodifiableList(out);
 }
 public synchronized Project rename(String id,String title,long editedAt)throws IOException{validateTitle(title);Project p=get(id);Project n=copy(p,title,editedAt,p.trash,p.recoverable);writeAtomic(n,true);return n;}
 public synchronized Project moveToTrash(String id,long editedAt)throws IOException{Project p=get(id);Project n=copy(p,p.title,editedAt,true,p.recoverable);writeAtomic(n,true);return n;}
 public synchronized Project restore(String id,long editedAt)throws IOException{Project p=get(id);Project n=copy(p,p.title,editedAt,false,p.recoverable);writeAtomic(n,true);return n;}
 public synchronized Project finalizeRecovery(String id,long editedAt)throws IOException{Project p=get(id);if(!p.recoverable||p.trash)throw new IOException("Este projeto não é recuperável");Project n=copy(p,p.title,editedAt,false,false);writeAtomic(n,true);return n;}
 public synchronized void deletePermanently(String id)throws IOException{File f=file(id);if(!f.isFile())throw new FileNotFoundException("Composição não encontrada");if(!read(f).trash)throw new IOException("A composição precisa estar na Lixeira");if(!f.delete())throw new IOException("Não foi possível excluir a composição");}
 private Project copy(Project p,String title,long edited,boolean trash,boolean recoverable){return new Project(p.id,title,p.profileId,p.createdAt,edited,p.durationNs,trash,recoverable,new ArrayList<>(p.events),new ArrayList<>(p.segments));}
 private List<Project> listAll()throws IOException{File[] files=root.listFiles((d,n)->n.endsWith(".plc"));ArrayList<Project> out=new ArrayList<>();if(files==null)return out;for(File f:files){try{out.add(read(f,false));}catch(IOException invalid){/* Invalid data is never surfaced as a saved project. */}}return out;}
 private File file(String id)throws IOException{if(id==null||!id.matches("[0-9a-fA-F-]{36}"))throw new IOException("Identificador inválido");return new File(root,id.toLowerCase(Locale.ROOT)+".plc");}
 private void writeAtomic(Project p,boolean replace)throws IOException{
  if(p.recoverable)p=new Project(p.id,p.title,p.profileId,p.createdAt,p.editedAt,p.durationNs,p.trash,true,closeEvents(p.events),p.segments);
  File dst=file(p.id),tmp=new File(root,p.id+".tmp");ByteArrayOutputStream payload=new ByteArrayOutputStream();
  try(DataOutputStream d=new DataOutputStream(payload)){
   d.writeInt(MAGIC);d.writeInt(FORMAT);writeText(d,p.id,64);writeText(d,p.title,512);d.writeInt(p.profileId);d.writeLong(p.createdAt);d.writeLong(p.editedAt);d.writeLong(p.durationNs);d.writeBoolean(p.trash);d.writeBoolean(p.recoverable);d.writeInt(p.segments.size());for(MidiTakeRecorder.Segment s:p.segments){d.writeLong(s.startNs);d.writeLong(s.endNs);}d.writeInt(p.events.size());long prev=-1;
   for(MidiTakeRecorder.Event e:p.events){if(e.timeNs<0||e.timeNs<prev||e.status<0x80||e.status>0xef||e.data1<0||e.data1>127||e.data2<0||e.data2>127||e.epoch<0||e.order<0)throw new IOException("Sequência MIDI inválida");prev=e.timeNs;d.writeInt(e.status);d.writeByte(e.data1);d.writeByte(e.data2);d.writeLong(e.timeNs);d.writeLong(e.epoch);d.writeInt(e.order);d.writeBoolean(e.generated);d.writeLong(e.androidTimestampNs);d.writeInt(e.sourceId);}
  }
  byte[] bytes=payload.toByteArray();if(bytes.length>MAX_PROJECT_BYTES)throw new IOException("Composição excede o tamanho permitido");CRC32 crc=new CRC32();crc.update(bytes);
  try(FileOutputStream f=new FileOutputStream(tmp);DataOutputStream d=new DataOutputStream(new BufferedOutputStream(f))){d.writeInt(bytes.length);d.write(bytes);d.writeLong(crc.getValue());d.flush();f.getFD().sync();}
  read(tmp);
  if(dst.exists()&&!replace){tmp.delete();throw new IOException("Identificador de composição já existe");}
  try{Files.move(tmp.toPath(),dst.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
  catch(IOException atomicError){tmp.delete();throw new IOException("Não foi possível publicar a composição salva",atomicError);}
 }
 private Project read(File f)throws IOException{return read(f,true);}
 private Project read(File f,boolean includeEvents)throws IOException{
  if(f.length()>MAX_PROJECT_BYTES+12)throw new IOException("Projeto grande demais");byte[] payload;long expected;
  try(DataInputStream d=new DataInputStream(new BufferedInputStream(new FileInputStream(f)))){int size=d.readInt();if(size<40||size>MAX_PROJECT_BYTES)throw new IOException("Tamanho de projeto inválido");payload=new byte[size];d.readFully(payload);expected=d.readLong();if(d.read()!=-1)throw new IOException("Dados extras no projeto");}
  CRC32 crc=new CRC32();crc.update(payload);if(crc.getValue()!=expected)throw new IOException("Integridade da composição inválida");
  try(DataInputStream d=new DataInputStream(new ByteArrayInputStream(payload))){int magic=d.readInt(),format=d.readInt();if(magic!=MAGIC||(format!=1&&format!=FORMAT))throw new IOException("Formato de composição não suportado");String id=readText(d,64),title=readText(d,512);int profile=d.readInt();long created=d.readLong(),edited=d.readLong(),duration=d.readLong();boolean trash=d.readBoolean(),recover=d.readBoolean();int segmentCount=d.readInt();if(segmentCount<0||segmentCount>10000)throw new IOException("Segmentos inválidos");ArrayList<MidiTakeRecorder.Segment> segments=new ArrayList<>(segmentCount);long segmentEnd=-1;for(int i=0;i<segmentCount;i++){long start=d.readLong(),end=d.readLong();if(start<0||start<segmentEnd||end<start)throw new IOException("Segmento inválido");segmentEnd=end;segments.add(new MidiTakeRecorder.Segment(start,end));}int count=d.readInt();if((!f.getName().equals(id+".plc")&&!f.getName().equals(id+".tmp"))||profile<0||created<0||edited<created||duration<0||count<1||count>MAX_EVENTS)throw new IOException("Metadados de composição inválidos");validateTitle(title);ArrayList<MidiTakeRecorder.Event> events=new ArrayList<>(count);long last=-1;for(int i=0;i<count;i++){int status=d.readInt(),a=d.readUnsignedByte(),b=d.readUnsignedByte();long time=d.readLong(),epoch=d.readLong();int order=d.readInt();boolean generated=d.readBoolean();long androidTime=format>=2?d.readLong():-1;int source=format>=2?d.readInt():-1;if(status<0x80||status>0xef||a>127||b>127||time<0||time<last||epoch<0||order<0)throw new IOException("Evento MIDI inválido");last=time;if(includeEvents)events.add(new MidiTakeRecorder.Event(status,a,b,time,epoch,order,generated,androidTime,source));}if(d.available()!=0)throw new IOException("Projeto contém dados inesperados");return new Project(id,title,profile,created,edited,duration,trash,recover,events,segments);}
 }

 private static boolean sameEvents(List<MidiTakeRecorder.Event> a,List<MidiTakeRecorder.Event> b){if(a.size()!=b.size())return false;for(int i=0;i<a.size();i++){MidiTakeRecorder.Event x=a.get(i),y=b.get(i);if(x.status!=y.status||x.data1!=y.data1||x.data2!=y.data2||x.timeNs!=y.timeNs||x.epoch!=y.epoch||x.generated!=y.generated)return false;}return true;}
 static List<MidiTakeRecorder.Event> closeEvents(List<MidiTakeRecorder.Event> source){
  ArrayList<MidiTakeRecorder.Event> out=new ArrayList<>(source);TreeSet<Integer> held=new TreeSet<>(),pedals=new TreeSet<>();
  long time=0,epoch=0;int order=0;
  for(MidiTakeRecorder.Event e:source){time=e.timeNs;epoch=e.epoch;order=Math.max(order,e.order+1);int key=e.channel()*128+e.data1;
   if(e.command()==0x90&&e.data2>0)held.add(key);
   else if(e.command()==0x80||e.command()==0x90)held.remove(key);
   else if(e.command()==0xb0){if(e.data1==64){if(e.data2>=64)pedals.add(e.channel());else pedals.remove(e.channel());}if(e.data1==121)pedals.remove(e.channel());if(e.data1==120||e.data1==123)held.removeIf(k->k/128==e.channel());}}
  for(int k:held)out.add(new MidiTakeRecorder.Event(0x80|k/128,k%128,0,time,epoch,order++,true));
  for(int ch:pedals)out.add(new MidiTakeRecorder.Event(0xb0|ch,64,0,time,epoch,order++,true));return out;
 }
 private static void validateTitle(String title)throws IOException{if(title==null||title.trim().isEmpty()||title.length()>120)throw new IOException("Título inválido");for(int i=0;i<title.length();i++)if(Character.isISOControl(title.charAt(i)))throw new IOException("Título contém caracteres de controle");}
 private static void writeText(DataOutputStream d,String s,int max)throws IOException{byte[] b=s.getBytes(StandardCharsets.UTF_8);if(b.length>max)throw new IOException("Texto excede limite");d.writeInt(b.length);d.write(b);}
 private static String readText(DataInputStream d,int max)throws IOException{int n=d.readInt();if(n<0||n>max)throw new IOException("Texto inválido");byte[] b=new byte[n];d.readFully(b);return new String(b,StandardCharsets.UTF_8);}
 private static String fold(String s){String n=Normalizer.normalize(s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD);StringBuilder b=new StringBuilder();for(int i=0;i<n.length();i++)if(Character.getType(n.charAt(i))!=Character.NON_SPACING_MARK)b.append(n.charAt(i));return b.toString();}
}

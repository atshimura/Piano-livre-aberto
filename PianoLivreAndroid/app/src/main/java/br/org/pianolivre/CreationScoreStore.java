package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;

/**
 * C2-C offline score derivatives. One private versioned sidecar per C1 project.
 * Never rewrites a .plc, never uses a project title as a storage key.
 */
public final class CreationScoreStore {
 public static final int MAX_SCORE_BYTES=2*1024*1024, MAX_TOTAL_BYTES=8*1024*1024;
 private static final int MAGIC=0x504c5331, FORMAT=3, FORMAT_LEGACY=1, MAX_OVERRIDES=CreationScoreTranscriber.MAX_NOTES;
 public static final String DIRECTORY="creation-scores", EXT=".pls";
 private final File directory;
 private final CreationProjectStore projects;

 public static final class Saved {
  public final String projectId,xml;public final int profileId;
  public final CreationScoreTranscriber.Options options;
  Saved(String id,int profile,CreationScoreTranscriber.Options o,String x){
   projectId=id;profileId=profile;options=o;xml=x;
  }
 }
 public CreationScoreStore(File root)throws IOException {
  projects=new CreationProjectStore(root);directory=new File(root,DIRECTORY);
  if(!directory.exists()&&!directory.mkdirs())throw new IOException("Não foi possível preparar partituras geradas");
  if(!directory.isDirectory())throw new IOException("Diretório de partituras inválido");
  // Finish or roll back an interrupted permanent deletion. Never discard a
  // derived score while its original project still exists.
  File[] interrupted=directory.listFiles((d,n)->n.matches("[0-9a-fA-F-]{36}\\.deleting"));
  if(interrupted==null)throw new IOException("Partituras derivadas indisponíveis");
  for(File pending:interrupted){
   String id=pending.getName().substring(0,36);
   File original=new File(new File(root,"creations"),id+".plc");
   File restored=new File(directory,id+EXT);
   if(original.isFile()&&!restored.exists()){
    try{Files.move(pending.toPath(),restored.toPath(),StandardCopyOption.ATOMIC_MOVE);}
    catch(IOException e){throw new IOException("Não foi possível recuperar partitura após interrupção",e);}
   }else if(!pending.delete())throw new IOException("Não foi possível limpar exclusão interrompida");
  }
 }
 private static String id(String id)throws IOException{
  if(id==null||!id.matches("[0-9a-fA-F-]{36}"))throw new IOException("Identificador de composição inválido");
  return id.toLowerCase(Locale.ROOT);
 }
 private File file(String id)throws IOException{return new File(directory,id(id)+EXT);}
 public boolean has(String id)throws IOException{return file(id).isFile();}
 public Saved read(String id)throws Exception{
  CreationProjectStore.Project p=projects.get(id);
  return verified(readFile(file(id)),p);
 }
 public byte[] raw(String id)throws IOException{return readFile(file(id));}
 public synchronized void save(String id,int owner,CreationScoreTranscriber.Options o,String xml)throws Exception{
  CreationProjectStore.Project project=projects.get(id);
  if(project.profileId!=owner||project.trash||project.recoverable)throw new IOException("Composição indisponível para salvar partitura");
  if(o==null||xml==null)throw new IOException("Partitura vazia");
  String expected=CreationScoreTranscriber.transcribe(project.events,project.segments,o,project.title).xml;
  if(!expected.equals(xml))throw new IOException("Partitura não corresponde à gravação ou aos ajustes");
  byte[] payload=encode(project,o,xml);
  verified(payload,project);
  long total=payload.length;
  File[] list=directory.listFiles((d,n)->n.endsWith(EXT));
  if(list==null)throw new IOException("Diretório de partituras indisponível");
  for(File f:list)if(!f.equals(file(id)))total+=f.length();
  if(total>MAX_TOTAL_BYTES)throw new IOException("Limite de armazenamento de partituras atingido");
  File destination=file(id),temp=new File(directory,id(id)+".tmp");
  try{
   try(FileOutputStream output=new FileOutputStream(temp)){output.write(payload);output.getFD().sync();}
   try{Files.move(temp.toPath(),destination.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
   catch(IOException e){throw new IOException("Não foi possível substituir a partitura com segurança",e);}
  }finally{temp.delete();}
 }
 public synchronized void delete(String id)throws IOException{
  File f=file(id);if(f.exists()&&!f.delete())throw new IOException("Não foi possível excluir partitura derivada");
 }
 public static String fingerprint(CreationProjectStore.Project p)throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();
  DataOutputStream data=new DataOutputStream(bytes);
  data.writeInt(p.profileId);data.writeUTF(id(p.id));
  data.writeInt(p.segments.size());
  for(MidiTakeRecorder.Segment s:p.segments){data.writeLong(s.startNs);data.writeLong(s.endNs);}
  data.writeInt(p.events.size());
  for(MidiTakeRecorder.Event e:p.events){
   data.writeInt(e.status);data.writeInt(e.data1);data.writeInt(e.data2);data.writeLong(e.timeNs);
   data.writeLong(e.epoch);data.writeInt(e.order);data.writeBoolean(e.generated);
   data.writeLong(e.androidTimestampNs);data.writeInt(e.sourceId);
  }
  data.flush();return hex(hash(bytes.toByteArray()));
 }
 private static byte[] hash(byte[] b)throws Exception{return MessageDigest.getInstance("SHA-256").digest(b);}
 private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
 private static byte[] encode(CreationProjectStore.Project p,CreationScoreTranscriber.Options o,String xml)throws Exception{
  byte[] music=xml.getBytes(StandardCharsets.UTF_8);
  if(music.length<40||music.length>MAX_SCORE_BYTES-4096)throw new IOException("Partitura muito grande para salvar");
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();DataOutputStream data=new DataOutputStream(buffer);
  data.writeInt(MAGIC);data.writeInt(FORMAT);data.writeUTF(id(p.id));data.writeInt(p.profileId);
  data.writeUTF(fingerprint(p));data.writeUTF(p.title);
  data.writeInt(o.bpm);data.writeInt(o.beats);data.writeInt(o.beatType);data.writeInt(o.grid);
  data.writeInt(o.staffMode.ordinal());data.writeInt(o.splitPitch);
  if(o.staffOverrides.size()>MAX_OVERRIDES)throw new IOException("Muitos ajustes de pautas");
  data.writeInt(o.staffOverrides.size());
  List<String> keys=new ArrayList<>(o.staffOverrides.keySet());Collections.sort(keys);
  for(String key:keys){if(key.length()>110)throw new IOException("ID de nota inválido");data.writeUTF(key);data.writeInt(o.staffOverrides.get(key));}
  // PLS1 format 2 adds durable, bounded interpretation overrides.
  data.writeBoolean(o.legacyTiming);
  data.writeInt(o.regularization.ordinal());
  writeIntMap(data,o.handOverrides);writeIntMap(data,o.voiceOverrides);
  if(o.rhythmOverrides.size()>MAX_OVERRIDES)throw new IOException("Muitos ajustes rítmicos");
  data.writeInt(o.rhythmOverrides.size());
  List<String> rhythmIds=new ArrayList<>(o.rhythmOverrides.keySet());Collections.sort(rhythmIds);
  for(String key:rhythmIds){
   if(key.length()>110)throw new IOException("ID de ritmo inválido");
   CreationScoreTranscriber.Timing time=o.rhythmOverrides.get(key);
   data.writeUTF(key);data.writeLong(time.startTick);data.writeLong(time.endTick);
  }
  // PLS1 format 3 adds the written key (armadura) chosen for this score.
  if(o.keyFifths<CreationScoreTranscriber.MIN_FIFTHS||o.keyFifths>CreationScoreTranscriber.MAX_FIFTHS)
   throw new IOException("Tom inválido");
  data.writeInt(o.keyFifths);
  data.writeInt(music.length);data.write(music);data.flush();
  byte[] raw=buffer.toByteArray();if(raw.length+32>MAX_SCORE_BYTES)throw new IOException("Partitura excede o limite");
  buffer.write(hash(raw));return buffer.toByteArray();
 }
 public static Saved verified(byte[] bytes,CreationProjectStore.Project p)throws Exception{
  if(bytes==null||bytes.length<100||bytes.length>MAX_SCORE_BYTES)throw new IOException("Arquivo de partitura inválido");
  byte[] body=Arrays.copyOf(bytes,bytes.length-32),digest=Arrays.copyOfRange(bytes,bytes.length-32,bytes.length);
  if(!MessageDigest.isEqual(hash(body),digest))throw new IOException("Integridade da partitura incorreta");
  DataInputStream in=new DataInputStream(new ByteArrayInputStream(body));
  if(in.readInt()!=MAGIC)throw new IOException("Partitura não reconhecida");
  int format=in.readInt();
  if(format!=FORMAT&&format!=2&&format!=FORMAT_LEGACY)throw new IOException("Formato de partitura desconhecido");
  String projectId=id(in.readUTF());int profile=in.readInt();String expected=in.readUTF();String savedTitle=in.readUTF();
  if(savedTitle.length()>255)throw new IOException("Título da transcrição inválido");
  if(!projectId.equals(id(p.id))||profile!=p.profileId||!expected.equals(fingerprint(p)))
   throw new IOException("Partitura não corresponde à gravação ou ao perfil");
  int bpm=in.readInt(),beats=in.readInt(),type=in.readInt(),grid=in.readInt(),mode=in.readInt(),split=in.readInt();
  if(mode<0||mode>=CreationScoreTranscriber.StaffMode.values().length)throw new IOException("Divisão de pautas inválida");
  int count=in.readInt();if(count<0||count>MAX_OVERRIDES)throw new IOException("Ajustes de pauta inválidos");
  Map<String,Integer> overrides=new HashMap<>();
  for(int i=0;i<count;i++){String key=in.readUTF();int value=in.readInt();
   if(key.isEmpty()||key.length()>110||overrides.put(key,value)!=null)throw new IOException("Nota duplicada na partitura");
  }
  CreationScoreTranscriber.Options o;
  if(format==FORMAT_LEGACY){
   // Old derivatives must regenerate byte-for-byte with pre-3 timing.
   o=CreationScoreTranscriber.Options.legacy(bpm,beats,type,grid,
    CreationScoreTranscriber.StaffMode.values()[mode],split,overrides);
  }else{
   boolean legacy=in.readBoolean();
   int reg=in.readInt();
   if(reg<0||reg>=CreationScoreTranscriber.Regularization.values().length)
    throw new IOException("Regularização não suportada");
   Map<String,Integer> hands=readIntMap(in),voices=readIntMap(in);
   int rhythmCount=in.readInt();
   if(rhythmCount<0||rhythmCount>MAX_OVERRIDES)throw new IOException("Ajustes rítmicos inválidos");
   Map<String,CreationScoreTranscriber.Timing> rhythms=new HashMap<>();
   for(int i=0;i<rhythmCount;i++){
    String key=in.readUTF();long start=in.readLong(),end=in.readLong();
    if(key.length()>110||rhythms.put(key,new CreationScoreTranscriber.Timing(start,end))!=null)
     throw new IOException("Ajuste rítmico duplicado");
   }
   int keyFifths=0;
   if(format>=3){
    keyFifths=in.readInt();
    if(keyFifths<CreationScoreTranscriber.MIN_FIFTHS||keyFifths>CreationScoreTranscriber.MAX_FIFTHS)
     throw new IOException("Tom inválido");
   }
   o=new CreationScoreTranscriber.Options(bpm,beats,type,grid,
    CreationScoreTranscriber.StaffMode.values()[mode],split,overrides,
    CreationScoreTranscriber.Regularization.values()[reg],hands,voices,rhythms,legacy,keyFifths);
  }
  int size=in.readInt();if(size<40||size>MAX_SCORE_BYTES||size!=in.available())throw new IOException("Conteúdo MusicXML inválido");
  byte[] music=new byte[size];in.readFully(music);String xml=new String(music,StandardCharsets.UTF_8);
  // Exact regeneration guards against stale/corrupted XML, missing attacks and
  // edits to project source after a snapshot was generated.
  if(!xml.equals(CreationScoreTranscriber.transcribe(p.events,p.segments,o,savedTitle).xml))
   throw new IOException("A partitura não corresponde aos eventos gravados");
  return new Saved(projectId,profile,o,xml);
 }
 private static void writeIntMap(DataOutputStream out,Map<String,Integer> map)throws IOException{
  if(map.size()>MAX_OVERRIDES)throw new IOException("Muitos ajustes de mão/voz");
  out.writeInt(map.size());
  List<String> keys=new ArrayList<>(map.keySet());Collections.sort(keys);
  for(String key:keys){
   if(key.length()>110)throw new IOException("Identificador de nota inválido");
   out.writeUTF(key);out.writeInt(map.get(key));
  }
 }
 private static Map<String,Integer> readIntMap(DataInputStream in)throws IOException{
  int count=in.readInt();if(count<0||count>MAX_OVERRIDES)throw new IOException("Ajustes de mão/voz inválidos");
  Map<String,Integer> result=new HashMap<>();
  for(int i=0;i<count;i++){
   String key=in.readUTF();int value=in.readInt();
   if(key.length()>110||result.put(key,value)!=null)throw new IOException("Identificador duplicado");
  }
  return result;
 }
 public static byte[] readFile(File f)throws IOException{
  if(!f.isFile()||f.length()>MAX_SCORE_BYTES)throw new IOException("Partitura ausente ou excede limite");
  try(InputStream in=new FileInputStream(f)){
   ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
   while((n=in.read(b))!=-1){if(out.size()+n>MAX_SCORE_BYTES)throw new IOException("Partitura excede limite");out.write(b,0,n);}
   return out.toByteArray();
  }
 }
 public List<String> ids()throws IOException{
  File[] list=directory.listFiles();if(list==null)throw new IOException("Não foi possível listar partituras");
  ArrayList<String> ids=new ArrayList<>();
  for(File f:list)if(f.getName().endsWith(EXT)){
   if(!f.isFile()||!f.getName().matches("[0-9a-fA-F-]{36}\\.pls"))throw new IOException("Arquivo derivado inválido");
   ids.add(f.getName().substring(0,36));
  }
  if(ids.size()>CreationProjectStore.MAX_PROJECTS)throw new IOException("Muitas partituras derivadas");
  Collections.sort(ids);return ids;
 }
}
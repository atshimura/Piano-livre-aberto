package br.org.pianolivre;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;

/**
 * Z-P3 offline score derivatives. One private versioned sidecar per C1 project.
 * Never rewrites a .plc, never uses a project title as a storage key.
 *
 * PLS2 format (this generator, ZAI-001): magic 0x504C5332, no musical options —
 * the score is fully determined by the recording. Integrity is the body SHA-256
 * plus the recording fingerprint. Legacy PLS1 files (old generator) are never
 * deleted automatically: reading one raises {@link LegacyScoreException} and the
 * interface offers generating a new derivation, which replaces the file.
 */
public final class CreationScoreStore {
 public static final int MAX_SCORE_BYTES=2*1024*1024, MAX_TOTAL_BYTES=8*1024*1024;
 private static final int MAGIC=0x504c5332, FORMAT=1, LEGACY_MAGIC=0x504c5331;
 public static final String DIRECTORY="creation-scores", EXT=".pls";
 private final File directory;
 private final CreationProjectStore projects;

 /** A derivative written by the retired generator; regenerating replaces it. */
 public static final class LegacyScoreException extends IOException {
  public LegacyScoreException(){super("Partitura gerada pelo gerador anterior; gere novamente a partir da gravação");}
 }

 public static final class Saved {
  public final String projectId,xml,fingerprint;public final int profileId;
  /** Estimated tempo as written in the score; 0 when not safely identified. */
  public final int bpm;
  Saved(String id,int profile,String fingerprint,int bpm,String x){
   projectId=id;profileId=profile;this.fingerprint=fingerprint;this.bpm=bpm;xml=x;
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
 /** True when the stored derivative belongs to the retired generator (PLS1). */
 public boolean isLegacy(String id)throws IOException{
  File f=file(id);if(!f.isFile())return false;
  byte[] head=readFile(f);
  if(head.length<8)return false;
  DataInputStream in=new DataInputStream(new ByteArrayInputStream(head));
  return in.readInt()==LEGACY_MAGIC;
 }
 public Saved read(String id)throws Exception{
  CreationProjectStore.Project p=projects.get(id);
  return verified(readFile(file(id)),p);
 }
 public byte[] raw(String id)throws IOException{return readFile(file(id));}
 public synchronized void save(String id,int owner,String xml,int bpm)throws Exception{
  CreationProjectStore.Project project=projects.get(id);
  if(project.profileId!=owner||project.trash||project.recoverable)throw new IOException("Composição indisponível para salvar partitura");
  if(xml==null||xml.length()<40)throw new IOException("Partitura vazia");
  if(bpm<0||bpm>999)throw new IOException("Andamento inválido");
  byte[] payload=encode(project,xml,bpm);
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
 private static byte[] encode(CreationProjectStore.Project p,String xml,int bpm)throws Exception{
  byte[] music=xml.getBytes(StandardCharsets.UTF_8);
  if(music.length<40||music.length>MAX_SCORE_BYTES-4096)throw new IOException("Partitura muito grande para salvar");
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();DataOutputStream data=new DataOutputStream(buffer);
  data.writeInt(MAGIC);data.writeInt(FORMAT);data.writeUTF(id(p.id));data.writeInt(p.profileId);
  data.writeUTF(fingerprint(p));data.writeUTF(p.title);data.writeInt(bpm);
  data.writeInt(music.length);data.write(music);data.flush();
  byte[] raw=buffer.toByteArray();if(raw.length+32>MAX_SCORE_BYTES)throw new IOException("Partitura excede o limite");
  buffer.write(hash(raw));return buffer.toByteArray();
 }
 public static Saved verified(byte[] bytes,CreationProjectStore.Project p)throws Exception{
  if(bytes==null||bytes.length<100||bytes.length>MAX_SCORE_BYTES)throw new IOException("Arquivo de partitura inválido");
  byte[] head=Arrays.copyOf(bytes,4);
  DataInputStream headIn=new DataInputStream(new ByteArrayInputStream(head));
  if(headIn.readInt()==LEGACY_MAGIC)throw new LegacyScoreException();
  byte[] body=Arrays.copyOf(bytes,bytes.length-32),digest=Arrays.copyOfRange(bytes,bytes.length-32,bytes.length);
  if(!MessageDigest.isEqual(hash(body),digest))throw new IOException("Integridade da partitura incorreta");
  DataInputStream in=new DataInputStream(new ByteArrayInputStream(body));
  if(in.readInt()!=MAGIC)throw new IOException("Partitura não reconhecida");
  int format=in.readInt();
  if(format!=FORMAT)throw new IOException("Formato de partitura desconhecido");
  String projectId=id(in.readUTF());int profile=in.readInt();String expected=in.readUTF();String savedTitle=in.readUTF();
  if(savedTitle.length()>255)throw new IOException("Título da transcrição inválido");
  int bpm=in.readInt();if(bpm<0||bpm>999)throw new IOException("Andamento inválido");
  if(!projectId.equals(id(p.id))||profile!=p.profileId||!expected.equals(fingerprint(p)))
   throw new IOException("Partitura não corresponde à gravação ou ao perfil");
  int size=in.readInt();if(size<40||size>MAX_SCORE_BYTES||size!=in.available())throw new IOException("Conteúdo MusicXML inválido");
  byte[] music=new byte[size];in.readFully(music);String xml=new String(music,StandardCharsets.UTF_8);
  return new Saved(projectId,profile,expected,bpm,xml);
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

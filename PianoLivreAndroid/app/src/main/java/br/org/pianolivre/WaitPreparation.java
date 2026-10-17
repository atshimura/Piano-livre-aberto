package br.org.pianolivre;

/** Preparação visual pré-estudo, sem prazo para o aluno tocar depois. */
public final class WaitPreparation {
 private long startedAt=-1L, durationMs;
 public static int configuredSeconds(int seconds){
  return seconds>=1&&seconds<=30?seconds:5;
 }
 public void start(long now,int seconds){
  if(now<0)throw new IllegalArgumentException("Instante inválido");
  startedAt=now;durationMs=configuredSeconds(seconds)*1000L;
 }
 public long durationMs(){return durationMs;}
 public boolean isActive(){return startedAt>=0;}
 public boolean finish(long now){
  if(!isActive()||now<startedAt||now-startedAt<durationMs)return false;
  cancel();return true;
 }
 public void cancel(){startedAt=-1L;}
}

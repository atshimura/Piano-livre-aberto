package br.org.pianolivre;

/** Presentation only: hide once per transport start, reveal until the next start. */
public final class StudyChromeState {
 private boolean active, revealed;
 public boolean update(boolean transportActive){
  if(transportActive!=active){active=transportActive;revealed=false;}
  return visible();
 }
 public boolean reveal(){revealed=true;return visible();}
 public boolean visible(){return !active||revealed;}
}

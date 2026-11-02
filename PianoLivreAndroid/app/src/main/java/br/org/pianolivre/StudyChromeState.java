package br.org.pianolivre;

/** Study presentation: both upper control bars stay visible for the entire session. */
public final class StudyChromeState {
 public boolean update(boolean transportActive){return true;}
 public boolean reveal(){return true;}
 public boolean visible(){return true;}
}

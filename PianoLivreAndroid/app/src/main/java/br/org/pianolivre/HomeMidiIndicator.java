package br.org.pianolivre;

/** Shared header label; names come from the effective connection's Android metadata. */
public final class HomeMidiIndicator {
 private HomeMidiIndicator(){}

 public static String label(boolean deviceOpened,String actualInstrumentName){
  if(!deviceOpened||actualInstrumentName==null||actualInstrumentName.trim().isEmpty())return "USB-MIDI";
  return actualInstrumentName.trim();
 }
}

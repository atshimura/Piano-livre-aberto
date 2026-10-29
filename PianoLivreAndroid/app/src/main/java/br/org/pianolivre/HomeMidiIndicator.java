package br.org.pianolivre;

/** Display rule for the home header; names are supplied only by Android MIDI metadata. */
public final class HomeMidiIndicator {
 private HomeMidiIndicator(){}

 public static String label(boolean deviceOpened,String actualInstrumentName){
  if(!deviceOpened||actualInstrumentName==null||actualInstrumentName.trim().isEmpty())return "USB-MIDI";
  return actualInstrumentName.trim();
 }
}

package br.org.pianolivre;

import java.util.Locale;

/** Presentation-only cache. Never gates the transport, MIDI or score drawing. */
public final class StudyInfoText {
 private String text;
 private long lastUpdate;
 private long lastTempo,lastPercent;
 private boolean lastHasScore;
 public String update(boolean hasScore,double bpm,double factor,double beat,long now,boolean immediate){
  long tempo=Math.round(bpm),percent=Math.round(factor*100);
  // Musical/setting changes bypass the display cadence, including tempo maps.
  if(!immediate&&text!=null&&hasScore==lastHasScore&&tempo==lastTempo&&percent==lastPercent
    &&now>=lastUpdate&&now-lastUpdate<100)return text;
  text="♫ "+(hasScore?String.valueOf(tempo):"—")+" BPM × "+percent+
    "% · posição "+String.format(Locale.US,"%.1f",beat);
  lastUpdate=now;lastTempo=tempo;lastPercent=percent;lastHasScore=hasScore;
  return text;
 }
}

package br.org.pianolivre;

/** Conventional fixed spelling for uncontextualized live MIDI pitches. */
public final class MusicNoteSpelling {
 private MusicNoteSpelling(){}
 private static final String[] PT={"DÓ","DÓ♯","RÉ","MI♭","MI","FÁ","FÁ♯","SOL","LÁ♭","LÁ","SI♭","SI"};
 private static final String[] EN={"C","C♯","D","E♭","E","F","F♯","G","A♭","A","B♭","B"};
 public static String portuguese(int midi){check(midi);return PT[midi%12];}
 public static String international(int midi){check(midi);return EN[midi%12]+(midi/12-1);}
 private static void check(int n){if(n<0||n>127)throw new IllegalArgumentException("Nota MIDI fora de 0..127");}
}

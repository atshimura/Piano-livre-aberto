package br.org.pianolivre;

import java.util.HashSet;
import java.util.Set;

/**
 * Native study state, not a drawing engine or View. Keeps legacy persisted
 * preferences without bundling the removed schematic staff renderer.
 */
final class StudyInputState {
 Score score;
 double playhead;
 int hand;
 boolean showKeyboard;
 final Set<Integer> pressed=new HashSet<>();
}

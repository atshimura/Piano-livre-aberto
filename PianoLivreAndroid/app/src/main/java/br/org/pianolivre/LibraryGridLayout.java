package br.org.pianolivre;

/** Readable home score cards: two columns when width permits, one on phones. */
public final class LibraryGridLayout {
 private LibraryGridLayout(){}
 public static int columnsForWidthDp(int widthDp){
  if(widthDp<=0)throw new IllegalArgumentException("Largura de tela inválida");
  return widthDp>=600?2:1;
 }
}

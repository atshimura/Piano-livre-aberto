package br.org.pianolivre;

/** Responsive card density for the landscape music library (dp, not pixels). */
public final class LibraryGridLayout {
 private LibraryGridLayout(){}
 public static int columnsForWidthDp(int widthDp){
  if(widthDp<=0)throw new IllegalArgumentException("Largura de tela inválida");
  return widthDp>=1160?5:widthDp>=870?4:widthDp>=600?3:2;
 }
}

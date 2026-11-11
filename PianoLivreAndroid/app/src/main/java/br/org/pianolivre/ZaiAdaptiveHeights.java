package br.org.pianolivre;

/**
 * Layout heights in dp, independent of screen pixels and Android dependencies.
 * Short landscape windows receive compact controls. Accessibility fonts and
 * exceptionally short windows can still scroll rather than clipping text.
 */
final class ZaiAdaptiveHeights {
 private ZaiAdaptiveHeights(){}

 static final class Landing {
  final boolean compact;
  final int topPadding,bottomPadding,titleHeight,descriptionHeight,gap,cardHeight;
  final boolean fitsWithoutScroll;
  Landing(boolean compact,int topPadding,int bottomPadding,int titleHeight,
    int descriptionHeight,int gap,int cardHeight,boolean fitsWithoutScroll){
   this.compact=compact;this.topPadding=topPadding;this.bottomPadding=bottomPadding;
   this.titleHeight=titleHeight;this.descriptionHeight=descriptionHeight;
   this.gap=gap;this.cardHeight=cardHeight;this.fitsWithoutScroll=fitsWithoutScroll;
  }
 }
 static Landing landing(int usableHeightDp,float fontScale){
  boolean compact=usableHeightDp<440;
  int top=compact?5:8,bottom=compact?6:14,title=compact?32:43;
  int description=compact?22:30,gap=compact?5:9;
  int free=usableHeightDp-56-top-bottom-title-description-gap;
  int minimum=Math.round((compact?172:260)*Math.max(1f,fontScale));
  int cards=Math.min(compact?260:320,Math.max(minimum,free));
  return new Landing(compact,top,bottom,title,description,gap,cards,free>=cards);
 }
 static final class Menu {
  final int record,projects;
  Menu(int record,int projects){this.record=record;this.projects=projects;}
 }
 static Menu creationMenu(int paneHeightDp,boolean recoveryWarning){
  int available=Math.max(0,paneHeightDp-(recoveryWarning?38:0));
  int projectH=available<230?48:64;
  int recordH=Math.max(78,Math.min(152,available-projectH-22));
  return new Menu(recordH,projectH);
 }
}

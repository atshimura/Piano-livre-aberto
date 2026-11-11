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
 /**
  * The previous layout subtracted a *nominal* 56dp header from the parent
  * onSizeChanged height. In a real Android window the ScrollView can be shorter
  * (system navigation, insets, layout). That discrepancy made the initial page
  * scroll and focused the lower buttons, hiding "Escolha um modo".
  *
  * Input is now the measured CONTENT viewport, with the top bar already removed.
  */
 static Landing landingContent(int viewportHeightDp,float fontScale){
  boolean compact=viewportHeightDp<384;
  int top=compact?4:8,bottom=compact?5:14,title=compact?30:43;
  int description=compact?24:30,gap=compact?4:9;
  int free=viewportHeightDp-top-bottom-title-description-gap;
  int minimum=Math.round((compact?168:240)*Math.max(1f,fontScale));
  // Leave 2dp for view/pixel rounding, rather than relying on an exact fit.
  // Accessibility minimum always wins over the cosmetic maximum card size.
  int cards=Math.max(minimum,Math.min(compact?260:320,free-2));
  return new Landing(compact,top,bottom,title,description,gap,cards,free>=cards);
 }
 /** Legacy whole-screen calculation retained for previous regression coverage. */
 static Landing landing(int usableHeightDp,float fontScale){
  return landingContent(usableHeightDp-56,fontScale);
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

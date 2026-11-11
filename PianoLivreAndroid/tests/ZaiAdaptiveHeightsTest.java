package br.org.pianolivre;

public final class ZaiAdaptiveHeightsTest {
 private static int assertions=0;
 private static void verify(boolean value,String description){
  assertions++;
  if(!value)throw new AssertionError(description);
 }
 private static void landing(int height,float scale,boolean expectFit){
  ZaiAdaptiveHeights.Landing plan=ZaiAdaptiveHeights.landing(height,scale);
  verify(plan.fitsWithoutScroll==expectFit,
    "unexpected vertical scrolling at "+height+"dp, fontScale="+scale);
  verify(plan.cardHeight>0,"card height must be positive");
  verify(plan.titleHeight>0&&plan.descriptionHeight>0,"title and description remain accessible");
  int used=56+plan.topPadding+plan.bottomPadding+plan.titleHeight+
   plan.descriptionHeight+plan.gap+plan.cardHeight;
  verify(plan.fitsWithoutScroll?used<=height:used>height,
    "viewport accounting must explain whether scrolling is necessary");
 }
 public static void main(String[] args){
  // Window height in dp AFTER status/nav bars, not full physical display.
  for(int height:new int[]{300,310,320,340,360,420,600,800})
   landing(height,1f,true);
  for(int height:new int[]{270,280,290})landing(height,1f,false);
  landing(300,1.4f,false); // Larger accessibility font must never clip a card.
  ZaiAdaptiveHeights.Landing compact=ZaiAdaptiveHeights.landing(310,1f);
  verify(compact.compact&&compact.cardHeight<260,
    "short landscape must actually shrink the three mode cards");
  ZaiAdaptiveHeights.Landing tablet=ZaiAdaptiveHeights.landing(800,1f);
  verify(!tablet.compact&&tablet.cardHeight<=320,
    "tall tablets retain comfortable card heights");
  for(int pane:new int[]{150,160,175,200,225,280,500}){
   for(boolean warning:new boolean[]{false,true}){
    ZaiAdaptiveHeights.Menu menu=ZaiAdaptiveHeights.creationMenu(pane,warning);
    verify(menu.record>=78&&menu.record<=152,"main recording touch area");
    verify(menu.projects>=48&&menu.projects<=64,"projects touch area");
    if(pane>=150&&(warning==false||pane>=188))
     verify(menu.record+menu.projects+22+(warning?38:0)<=pane,
       "main actions should not cause unnecessary scrolling");
   }
  }
  System.out.println("ZAI011_ADAPTIVE_HEIGHTS_PASS checks="+assertions);
 }
}

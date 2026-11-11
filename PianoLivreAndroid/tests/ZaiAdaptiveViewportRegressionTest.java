package br.org.pianolivre;

/**
 * ZAI-012: regression for the actual viewport BELOW the fixed toolbar.
 * Unlike the legacy test, inputs do not include a nominal 56dp parent header.
 */
public final class ZaiAdaptiveViewportRegressionTest {
 private static int checks=0;
 private static void check(boolean condition,String message){
  checks++;
  if(!condition)throw new AssertionError(message);
 }
 private static void viewport(int heightDp,float fontScale,boolean shouldFit){
  ZaiAdaptiveHeights.Landing p=ZaiAdaptiveHeights.landingContent(heightDp,fontScale);
  int actualContent=p.topPadding+p.bottomPadding+p.titleHeight+
    p.descriptionHeight+p.gap+p.cardHeight;
  check(p.fitsWithoutScroll==shouldFit,
   "incorrect fit for CONTENT viewport "+heightDp+"dp scale="+fontScale);
  check(p.fitsWithoutScroll?actualContent<=heightDp:actualContent>heightDp,
   "content height accounting mismatched at "+heightDp+"dp: "+actualContent);
  check(p.cardHeight>=168,"accessible button/content minimum");
  check(p.titleHeight>0&&p.descriptionHeight>0,"both headings remain visible");
 }
 public static void main(String[] args){
  for(int height:new int[]{235,245,260,280,291,300,330,365,400,600,720})
   viewport(height,1f,true);
  for(int height:new int[]{170,190,210,230})viewport(height,1f,false);
  viewport(250,1.4f,false);
  viewport(305,1.4f,true);
  viewport(330,1.65f,false);
  ZaiAdaptiveHeights.Landing phone=ZaiAdaptiveHeights.landingContent(291,1f);
  check(phone.compact&&phone.cardHeight<260,
   "screenshot-like window should shrink cards below original 260dp");
  check(phone.cardHeight<=291-(
   phone.topPadding+phone.bottomPadding+phone.titleHeight+
   phone.descriptionHeight+phone.gap),
   "title and description must not scroll behind the header");
  ZaiAdaptiveHeights.Landing large=ZaiAdaptiveHeights.landingContent(650,1f);
  check(!large.compact&&large.cardHeight<=320,"tablet capped height");
  System.out.println("ZAI012_REAL_VIEWPORT_PASS checks="+checks);
 }
}

package br.org.pianolivre;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

/** Linha Z: compatibilidade com o edge-to-edge forcado do Android 15 (targetSdk 35).
 * Em Android 15+ o sistema desenha a janela sob as barras do sistema e ignora os
 * flags legados (SYSTEM_UI_FLAG_FULLSCREEN). Este ajuste esconde a barra de status
 * via WindowInsetsController e aplica as barras/cutout como padding do conteudo,
 * preservando a aparencia imersiva do app. Em Android 14 ou anterior, nao faz nada
 * (comportamento legado permanece intocado). Sem alteracoes visuais em versoes antigas.
 * Registro: ZAI-CONTINUIDADE.md / docs da linha Z (zai/feat/target-sdk-35). */
public final class ZEdgeInsets {
 private ZEdgeInsets(){}

 /** Aplica o tratamento de insets quando a versao e Android 15+ (API 35+). */
 public static void apply(Activity a){
  if(Build.VERSION.SDK_INT<35)return;
  View content=a.findViewById(android.R.id.content);
  if(content==null)return;
  a.getWindow().setDecorFitsSystemWindows(false);
  content.setOnApplyWindowInsetsListener((v,insets)->{
   android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
   v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
   return WindowInsets.CONSUMED;
  });
  if(a.getWindow().getInsetsController()!=null){
   a.getWindow().getInsetsController().hide(WindowInsets.Type.statusBars());
  }
 }
}

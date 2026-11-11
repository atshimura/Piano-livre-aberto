# ZAI-012 — Correção do seletor Laboratório, evidência Android GUI e APK 0.9.73

**Data:** 11/11/2026. **Motivo:** evidência visual real enviada pelo mantenedor após 0.9.72, com a tela Laboratório Musical deslocada verticalmente: título "Escolha um modo" oculto, explicação parcialmente escondida pela barra azul. **Status:** correção funcional integrada exclusivamente a `zai/lab`; APK principal assinado 0.9.73/code82 disponível em `zai/downloads`. Nenhuma alteração em `main`, repositório histórico privado, tag, Release oficial ou Play Store. Testes físicos pendentes.

## Causa e mudança

A ZAI-011 calculava a altura dos cartões com base no tamanho do painel-pai, subtraindo uma barra superior nominal de 56dp. O tamanho real do `ScrollView` pode ser menor devido aos insets e à janela Android. Além disso, o foco de um botão ou a reentrada podia deixar a rolagem deslocada.

A ZAI-012 passa a medir `scroll.getHeight()` pelo `OnLayoutChangeListener` da região realmente disponível **abaixo** da barra azul. `ZaiAdaptiveHeights.landingContent` recebe a altura desse viewport e reserva 2dp para arredondamentos, dimensionando título, explicação e cartões. `showAtTop()` reposiciona a tela ao entrar e depois de retornar de Descobrir/Criação, com foco inicial no contêiner. **Os três cartões e a indicação Aprender — Em breve permanecem funcionais como antes; Criação/MIDI/MusicXML/dados não foram modificados.** Se o texto de acessibilidade aumentar demais, existe rolagem segura em vez de cortar informações.

Detalhamento técnico: [ZAI-012 — Diagnóstico e plano](ZAI-012-CORRECAO-ROLAGEM-SELETOR-20261111.md), [PR funcional #146](https://github.com/atshimura/Piano-livre-aberto/pull/146), merge `089e27f562ebaba5d07ea1b372eabef833be388e`.

## Testes comprovados

- **PR #146:** **12/12 checks SUCCESS**, inclusive workflows de regressão ZAI-002/004/006/007/008/009/010/011/012.
- **GitHub Actions [#43011380691](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43011380691): SUCCESS.** Contratos Python de navegação e persistência, teste `ZaiAdaptiveHeightsTest` (89 verificações) e **`ZaiAdaptiveViewportRegressionTest` (75 verificações)**, compilação Android debug de pacote isolado `br.org.pianolivre.opensource.zai`.
- **Emulador Android GUI real, API 29:** `ZAI012_ANDROID_VIEWPORT_PASS user-screenshot`, `ZAI012_ANDROID_VIEWPORT_PASS short-phone` e `ZAI012_ANDROID_VIEWPORT_PASS landscape-tablet`. Em cada uma das três geometrias/densidades/fontes, a automação abriu o Laboratório pela Biblioteca, conferiu coordenadas reais (`uiautomator dump`) do título, descrição e botões dentro do viewport, tentou rolar verticalmente e confirmou título fixo; entrou em Descobrir e retornou para verificar o deslocamento inicial. Capturas de tela e medições foram preservadas no artefato GitHub Actions `ZAI012-viewport-gui-5e5601e4e73636704d1a0c569cb4565f156bfc5d`.
- **Build principal [#43011986981](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43011986981): SUCCESS.** Regressões JVM musicais e de viewport, contratos Python, compilação Android release. APK `br.org.pianolivre.opensource`, `versionCode=82`, `versionName=0.9.73-open-beta`, `minSdk=26`, `targetSdk=36`, **sem debug e sem permissão INTERNET**. Certificado SHA-256 permanente `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`; chave acessada apenas por GitHub Actions Secrets, sem divulgação.
- **Distribuição [#43012311871](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43012311871): SUCCESS.** Recuperou artefato assinado original `PianoLivre-PRINCIPAL-0.9.73-APK-assinado-27ead05b456a051ee46657d44c5a556f85750a70` e verificou SHA, certificado, versão e ausência de INTERNET/debug. Publicou os mesmos bytes em `zai/downloads`, commit de download `67f2268bc9474adedbf234cc468be6909cc670c0`, comparando novamente SHA no Git remoto.

## Download principal de instalação

- **Versão:** 0.9.73-open-beta; **code:** 82; **tamanho:** 802.744 bytes.
- **SHA-256:** `9e64ee5fabb55c514053acbd7b14a2214be8f2905f4244ff83de1dabbe5571ce`.
- **Download direto imutável:** https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/67f2268bc9474adedbf234cc468be6909cc670c0/apks/principal/PianoLivre-Principal-0.9.73-open-beta-assinado.apk
- **Página no GitHub:** https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.73-open-beta-assinado.apk
- **Comprovante:** https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.73-open-beta-SHA256.txt

## Limitações e próximos passos

1. Antes de atualizar, efetuar backup e instalar **sobre** a 0.9.72, sem desinstalar; verificar composições, perfis e partituras.
2. No **Realme 7 5G** e **Lenovo Tab M9**, em paisagem, confirmar que `Escolha um modo`, descrição e os três cartões cabem sem rolagem; entrar em Descobrir e voltar; verificar escala de fontes e barra Android à direita.
3. As verificações de emulador não substituem testes físicos de toque, densidade, recortes e visibilidade; não declarar garantia universal para qualquer dispositivo.
4. `main`, publicação de Release e Play Store permanecem decisões separadas. A nova implementação não exige nem altera o Yamaha P-45 ou o motor de gravação.

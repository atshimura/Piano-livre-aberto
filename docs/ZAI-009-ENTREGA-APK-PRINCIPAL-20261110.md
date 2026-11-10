# ZAI-009 — Entrega: transcrição básica sem pausas e staccato, APK principal 0.9.70

**Data:** 10/11/2026. **Situação:** código integrado a `zai/lab`, CI JVM/OSMD/Android aprovada, APK release principal assinado e publicado no GitHub para download direto. **`main` permanece intocada**; nenhuma tag, Release, Play Store, serviço externo ou telemetria. Repositório histórico privado não acessado. Testes físicos ainda pendentes.

## Objetivo e decisão de produto

- Planejamento ZAI-009: [Notas e durações sem símbolos acessórios](ZAI-009-NOTAS-DURACOES-SEM-SIMBOLOS.md).
- PR funcional [#134](https://github.com/atshimura/Piano-livre-aberto/pull/134), integrado exclusivamente à `zai/lab` com merge commit **`113ae5035b0363f66bec6f3ce83197c91d9c7478`**.
- A Criação escreve **altura, ataque, acorde, figura rítmica, andamento, acidentes, claves e compassos**. Não exibe pausas, staccato, ligaduras de expressão, ligaduras de prolongamento ou outros símbolos de articulação automáticos.
- Os silêncios permanecem como `<note print-object="no"><rest/><duration>...` no **MusicXML**, preservando posição de ataques, sincronização entre vozes, 4/4, alinhamento dos compassos e leitura em Estudar. Isso não é exclusão destrutiva das pausas, apenas ocultação visual.
- O OSMD offline embutido usa SVG com glifos de pausas transparentes. O teste browser distingue **nós SVG existentes** de **glifos efetivamente visíveis**: os tempos continuam no modelo gráfico, mas os símbolos desaparecem da partitura.
- O código deixa de emitir `<notations><articulations><staccato/></articulations></notations>`. A duração física de cada nota, inclusive eventos breves e sobreposições, continua acessível nos `Note On/Off` do `.plc` original. Nenhum MIDI é reescrito.
- A notação básica mantém a restrição ZAI-007 de uma cabeça visível por ataque. **Limitação explícita:** uma sustentação que ultrapassa um compasso pode aparecer abreviada, pois não se usam ligaduras. O XML/Estudar reflete essa simplificação, enquanto a gravação MIDI mantém a duração exata. Não confundir o valor musical escrito com toda a duração física da tecla.
- A quebra automática de sistemas `creation-page` (ZAI-008) continua; `free` e `engraving-poc` não são alterados. Importações MusicXML de outras fontes mantêm suas pausas e articulações originais; não há CSS global que as oculte.
- Novo marcador `ZAI-009-basic-notes-duration` em `ScoreXmlComposer`. A tela reconhece PLS2 de gerador anterior e oferece **Gerar partitura básica** antes de substituir; só regrava `.pls` quando **Salvar partitura** é acionado. Mantém formato PLS2, `.plc`, perfis, biblioteca e backups.

## Implementação e testes

Alterações principais:
- `PianoLivreAndroid/app/src/main/java/br/org/pianolivre/ScoreXmlComposer.java`: pausas não imprimíveis e remoção de staccato gerado.
- `CreationScorePreviewScreen.java`: informação simplificada e regeneração segura com nova revisão.
- `ZScoreBasicNotationRegressionTest.java`: JVM para 4 notas destacadas, C maior, C menor/Mi♭, sustentação de 5 minutos, figuras, BPM, pausas ocultas mas temporais, 2 pautas, eventos no Estudar e MusicXML importado com pausas originalmente visíveis.
- `CreationBasicNotationBrowserTest.js`: browser Chromium real (sem internet) com OSMD empacotado, compara XML com/sem `print-object="no"`, 360 e 800 pixels, confirmação de glifos efetivamente pintados, ausência de corte/rolagem horizontal.
- `test_zai009_basic_contract.py`: contratos estáticos de revisão, MIDI fonte intacto, modo Criação sem articulações e compatibilidade de prévia.
- Testes de ZAI-001/002/004/006/007/008 continuam na CI. Atualizadas apenas as expectativas históricas contraditórias (antes exigiam staccato desenhado), sem remover verificações de ataques, BPM, acordes e durações.

**GitHub Actions ZAI-009 [#42919689080](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42919689080), SUCCESS**, mesmo código que o PR #134: 
- JVM — `ZScoreGeneratorRhythmTest` 86; `ZScoreGeneratorXmlTest` 29; `CreationScoreStoreTest` 27; `CreationC2DCorpusTest` 45; `ZScoreRealMidiRegressionTest` 24; `ZScoreChordFidelityRegressionTest` 59; `ZScoreS1FidelityRegressionTest` 45; `ZScoreTieFreeRegressionTest` 40; `ZScoreRestOptimizationTest` 710; **`ZScoreBasicNotationRegressionTest` 59**: total **1.124 verificações JVM aprovadas**.
- Teste OSMD: XML de controle com 24 notas e 16 pausas apresentou 40 glifos; XML básico com `print-object="no"` exibiu **24 notas visíveis e zero pausas visíveis**, em 360 e 800 px. Os nós transparentes do SVG continuam para cálculo de layout; nenhum glifo visível cortado, sem rolagem horizontal, sem rede.
- O teste anterior de layout responsivo ZAI-008 também passou com 360/800/1100 px e zoom. Contratos Python e compilação Android debug isolada: SUCCESS.
- Checks finais do PR no HEAD `1848d7cf60d32ce8d76faf88820c893f072c4706`: todos SUCCESS (ZAI-002/004/006/007/008/009).
- Não houve teste físico no Realme 7 5G, Lenovo Tab M9 ou Yamaha P-45. Browser Chromium e JVM não equivalem à validação no aparelho.

## APK principal 0.9.70 assinado

- GitHub Actions de build **[#42920324941](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42920324941)**: SUCCESS. Commit exclusivo de empacotamento `3006ea5a06ebaf6330fa1705fd8b65215d2a001d`. Workflow dedicado: [PR #135](https://github.com/atshimura/Piano-livre-aberto/pull/135).
- Artefato temporário GitHub Actions ID **13810928742**, nome `PianoLivre-PRINCIPAL-0.9.70-APK-assinado-3006ea5a06ebaf6330fa1705fd8b65215d2a001d`.
- APK release `app/build/outputs/apk/release/app-release.apk`, **796.264 bytes**.
- **SHA-256:** `20240158e8d686d3f7637fcb1a31f3d715d13ed37a090aabcff7078642408ee2` — confirmado no workflow de assinatura e novamente no Git remoto após publicação.
- **Pacote Android:** `br.org.pianolivre.opensource`, rótulo `Piano Livre`, `versionCode=79`, `versionName=0.9.70-open-beta`, minSdk 26, targetSdk 36, não depurável e sem permissão `INTERNET`.
- **Certificado permanente SHA-256:** `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`, mesmo das anteriores; apenas Secrets existentes do Actions foram utilizados, sem divulgação de chaves ou senhas.
- Identidade e versão foram parametrizadas durante build, sem alterar versões de `main`. Atualiza a edição principal 0.9.69/code78; não substitui o app de testes `.opensource.zai`.

## Download GitHub raw sem Library file not found

- GitHub Actions de distribuição **[#42920884259](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42920884259)**: SUCCESS, mesmo APK já assinado, antes/depois do push à branch `zai/downloads`, hash e certificado conferidos. Workflow dedicado: [PR #136](https://github.com/atshimura/Piano-livre-aberto/pull/136).
- **Commit exato do arquivo público:** `3af6748e51ca49fb392be368a68dd85b267c5c88`. Objeto Git APK 796.264 bytes, blob `c3cc6bad1f34bb4f0c80e460806f876a84729e3b`.
- **URL direta (preferencial):**
  `https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/3af6748e51ca49fb392be368a68dd85b267c5c88/apks/principal/PianoLivre-Principal-0.9.70-open-beta-assinado.apk`
- **Página GitHub:** `https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.70-open-beta-assinado.apk`
- **Comprovante de SHA e certificado:** `https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.70-open-beta-SHA256.txt`
- O método não depende da Library do ChatGPT nem de artefatos Actions com expiração; depende da permanência do repositório público no GitHub.

## Pendências

1. Fazer backup validado e instalar 0.9.70 **por cima da edição principal**, sem desinstalar. Não declarar compatibilidade de migração física sem ensaio em aparelho.
2. Abrir `S1` no modo Criação e selecionar **Gerar partitura básica** quando o aplicativo identificar XML derivado antigo. Conferir as duas escalas, C/Cm, pontuação de duração e ausência visual de pausas e staccato. Depois escolher Salvar se estiver correto.
3. Não exigir que um Dó segurado por cinco minutos seja desenhado por cinco minutos na partitura sem ligadura; a gravação MIDI original é a fonte exata. A edição rítmica/ligaduras manuais ficam para etapa posterior.
4. Validar aparelhos Realme 7 5G, Lenovo Tab M9, Yamaha P-45. Testes de tela automatizados não substituem essa etapa.
5. `main` e histórico privado permanecem intocados; integração ZAi→main exigiria PR e autorização próprios.

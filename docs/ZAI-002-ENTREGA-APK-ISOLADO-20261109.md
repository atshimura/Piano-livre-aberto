# ZAI-002 — Entrega validada em CI e APK de teste isolado

**Registro:** 09/11/2026 (horário de Brasília; execução GitHub Actions em 10/11/2026 UTC). **Somente `zai/lab`; `main` não modificada.** Este registro documenta resultados observados; não há aprovação física.

## 1. Entregas concluídas

- Plano: [ZAI-002](ZAI-002-CORRECAO-RITMO-ARTICULACAO-APK-ISOLADO.md).
- PR de implementação [#114](https://github.com/atshimura/Piano-livre-aberto/pull/114) **integrado à `zai/lab`**, commit `788f3bb611edd6820f158794c2d70aa10b246bd3`.
- `ScoreTempoEstimator`: estimativa de pulso robusta por intervalos entre os ataques de notas; distinguir duração da tecla e ritmo notado.
- `ScoreRhythmQuantizer`: detecção de staccato, legato, pausas reais e duração sustentada sobre o ataque seguinte; vozes polifônicas quando necessário; correção de ligadura quando uma única nota é desdobrada em figuras **dentro e fora** do compasso.
- `ScoreXmlComposer`: articulação `staccato` e ligadura de expressão `slur`, distintas de ligadura de prolongamento `tie`.
- `ZScoreRealMidiRegressionTest`: teste com 14 pares de eventos extraídos da tomada MIDI real do mantenedor, incluindo duas escalas Dó–Si em velocidades próximas de 1:2, mais testes de staccato, legato, sustain e ligação de 5/16.
- Build ZAi separado: `-Ppianolivre.zai.test=true` define `br.org.pianolivre.opensource.zai` com rótulo `Piano Livre ZAi`; o build padrão sem propriedade mantém a identidade principal. Nenhuma alteração em versão, publicações ou `main`.
- Teste de pré-integração exclusivo [run #42727137337](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42727137337) e [run #42727141252](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42727141252): ambos **SUCCESS**, incluindo JVM, contratos Python e compilação debug do pacote isolado, **sem distribuição de APK debug**.

## 2. APK assinado: evidência verificada

O workflow dedicado `zai-apk-teste.yml` foi executado na `zai/lab` (commit `788f3bb611edd6820f158794c2d70aa10b246bd3`):

- [GitHub Actions #42727363672](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42727363672) — **SUCCESS**.
- Artefato `PianoLivre-ZAi-APK-teste-788f3bb611edd6820f158794c2d70aa10b246bd3`, ID GitHub **13723675991**.
- APK `app/build/outputs/apk/release/app-release.apk`, **793.596 bytes**.
- SHA-256 do APK, conferido novamente após descarregar o ZIP e extrair o APK: `f0b3da678ed6190c544f295997bde9290a04012c8fa5f8fc6ff22aface900fcc`.
- Certificado de assinatura permanente: `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290` (GitHub Actions Secrets; chaves e senhas nunca expostas).
- Android **package/applicationId**: `br.org.pianolivre.opensource.zai` (NÃO é `br.org.pianolivre.opensource`).
- Rótulo: **Piano Livre ZAi**; launcher: `br.org.pianolivre.MainActivity`.
- `versionCode 73`; `versionName 0.9.64-open-beta`; `minSdkVersion 26`; `targetSdkVersion 36`.
- APK **release não depurável**, sem permissão `INTERNET`. Uso exclusivamente para testes, sem criar GitHub Release, tag ou publicação na Play Store.

**Atenção:** o pacote ZAi independente impede que a instalação desse APK substitua o app principal. O identificador do antigo APK ZAi instalado separadamente no aparelho não foi documentado, portanto **não há comprovação de que o novo pacote atualize aquela instalação**; ele poderá instalar uma nova entrada ZAi ao lado dela. Não desinstalar nenhum aplicativo com dados sem backup verificado.

## 3. Testes executados na execução assinada (logs GitHub Actions)

| Teste | Resultado |
|---|---|
| `ZScoreGeneratorRhythmTest` | 86 verificações OK |
| `ZScoreGeneratorXmlTest` | 26 verificações OK |
| `CreationScoreStoreTest` | 27 verificações OK |
| `CreationC2DCorpusTest` | 45 verificações OK |
| `ZScoreRealMidiRegressionTest` | 23 verificações OK; **andamento real estimado 112,9 BPM** |
| **Total JVM acima** | **207 verificações aprovadas** |
| Auditoria/contratos Python | SUCCESS |
| Gradle `:app:assembleRelease -Ppianolivre.zai.test=true` | BUILD SUCCESSFUL |
| `aapt` package, label, versão, min/target SDK | Verificados com sucesso |
| `apksigner verify --print-certs` | Assinatura permanente verificada |
| SHA-256 do ZIP descarregado / APK extraído | ZIP íntegro e SHA do APK confere com CI |

## 4. Não executado e próximos passos

1. Testes **físicos** do novo APK com Yamaha P-45, Realme 7 5G e Lenovo Tab M9, com comparação manual da partitura e do exercício realizado — ainda pendentes.
2. Smoke de interface completo **no emulador Android**, incluindo Criação→Gerar→Salvar→Estudar e impacto visual de staccato/slur — **não executado nesta entrega**. A compilação e testes de Java não substituem esse ensaio.
3. O reconhecimento de pulsação permanece heurístico. Execuções muito irregulares ou ambíguas ainda podem exigir referência opcional de pulso no futuro. Nenhuma afirmação de transcrição universal.
4. Confirmar instalação paralela sem afetar dados existentes no aparelho físico; package ID isolado foi comprovado por `aapt`, mas a instalação física não foi observada.
5. O APK anterior da ZAi que utilizou indevidamente o ID do app principal não é reutilizado como base; o novo pacote é explicitamente outro.

**A branch `main` foi verificada inalterada em `55426cf79cbbd652ea5035fb6774d7a31b5380e3`.**

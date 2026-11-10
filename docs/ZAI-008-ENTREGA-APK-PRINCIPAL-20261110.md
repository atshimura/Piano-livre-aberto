# ZAI-008 — Entrega: pausas otimizadas e partitura responsiva, APK 0.9.69

**Registro:** 10/11/2026. **Repositório:** `atshimura/Piano-livre-aberto`. **Branch funcional:** `zai/lab`. **Branch principal GitHub `main`: não alterada**. Nenhuma tag, Release ou publicação na Play Store. Repositório histórico privado intocado. Teste físico ainda pendente.

## 1. Decisões, plano e código

- Plano funcional: [ZAI-008 — Pausas legíveis e partitura responsiva](ZAI-008-PAUSAS-LEGIVEIS-PARTITURA-RESPONSIVA.md).
- [PR #130](https://github.com/atshimura/Piano-livre-aberto/pull/130) **integrado à `zai/lab`** no commit `af64241425f5d09a9e2bc1105a9369b2cc49c020`.
- `ScoreRhythmQuantizer.restFigures`: troca a decomposição gulosa por programação dinâmica minimizando número de pausas, com preferência por agrupamentos e figuras simples em caso de empate. Preserva exatamente o intervalo escrito em semicolcheias, não desloca ataques, não altera duração de notas nem muda acordes/vozes. A regra admite todos os alinhamentos já suportados anteriormente para **nunca aumentar** a quantidade de pausas. Compasso vazio na pauta inferior continua com uma única pausa de semibreve.
- `NotationView`: novo método `loadCreationScore`, que solicita o layout específico `creation-page`; os layouts `free` e `engraving-poc` preservam seus comportamentos anteriores.
- `notation.html`: o modo `creation-page` usa 100% da largura visível, CSS sem overflow horizontal, `newSystemFromXML:false` e `renderSingleHorizontalStaffline:false`. O OSMD reengrava com quebras de sistema conforme largura, zoom e rotação; rolagem **vertical** continua permitida. Zoom A−/A+/Ajustar à largura preservado.
- `CreationScorePreviewScreen`: painel de prévia vertical ampliado para melhor visualização; partitura carregada pelo método específico. Derivados PLS2 antigos sem marcador `ZAI-008-rests-wrap` não são reutilizados silenciosamente; a tela oferece **Gerar prévia atualizada**, sem sobrescrever arquivos até **Salvar partitura**. Gravações `.plc` não foram modificadas.
- A notação simplificada ZAI-007 continua **sem ligaduras automáticas**, com uma cabeça de nota por Note On. Sustentações acima do compasso permanecem completas apenas na gravação MIDI original; a sua duração visual é aproximada. Edição manual continua para fase futura.

## 2. Testes dirigidos executados, com resultados

- [GitHub Actions #42903545626](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42903545626) **SUCCESS** após correção da admissibilidade dos intervalos de pausa; regressões antigas, auditorias, Android debug isolado e Chromium OSMD real. Verificações de PR final realizadas no mesmo código mais alterações de testes e documentação (por exemplo, #42904102965 e #42904117756).
- JVM: `ZScoreGeneratorRhythmTest` 86, `ZScoreGeneratorXmlTest` 29, `CreationScoreStoreTest` 27, `CreationC2DCorpusTest` 45, `ZScoreRealMidiRegressionTest` 24, `ZScoreChordFidelityRegressionTest` 59, `ZScoreS1FidelityRegressionTest` 45, `ZScoreTieFreeRegressionTest` 40, **`ZScoreRestOptimizationTest` 710**. Total **1.065 verificações JVM aprovadas**.
- O teste novo verifica exaustivamente os intervalos possíveis dentro do compasso 4/4, soma exata de células, alinhamento rítmico, menor número de pausas, nenhuma regressão de quantidade e preservação dos ataques reais. Os 22 ataques do S1 e os acordes C/Cm são cobertos por regressões anteriores que continuam passando.
- Browser real headless usando OSMD offline empacotado, `CreationResponsiveLayoutBrowserTest.js`: 14 compassos com 56 notas e 14 pausas da pauta inferior. Renderização em viewport 360x660, 800x420, 1100x720; zoom 1,5 e 0,7; todas com `scrollWidth == clientWidth`, nenhum glifo cortado e quebras de sistema verificadas. Em 360 px padrão, **14 linhas de pauta**; em 800 e 1100 px, **4 linhas**; zoom 1,5 aumenta as linhas, zoom 0,7 reduz. O layout `free` preservou o `overflow-x:auto`, evitando regressão fora da Criação.
- Contratos Python, verificação de cópia pública e `:app:assembleDebug -Ppianolivre.zai.test=true` concluídos com sucesso em CI. **O browser Chromium não equivale ao Android físico** e nenhuma validação com Yamaha P-45, Realme 7 5G ou Lenovo Tab M9 foi declarada.

## 3. APK principal assinado

- [GitHub Actions #42904666989](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42904666989) **SUCCESS**, SHA do código de empacotamento `8bde9526acbe3610f7b9168e0b8b5e9db0d74886`.
- APK `app/build/outputs/apk/release/app-release.apk`, **796.260 bytes**, artefato temporário GitHub Actions **ID 13804936234** (`PianoLivre-PRINCIPAL-0.9.69-APK-assinado-8bde9526acbe3610f7b9168e0b8b5e9db0d74886`).
- `applicationId=br.org.pianolivre.opensource`, launcher **Piano Livre**, `versionName=0.9.69-open-beta`, **versionCode=78** (superior à 0.9.68/code77), `minSdk 26`, `targetSdk 36`. APK release não depurável, sem permissão `INTERNET`.
- **SHA-256 APK:** `7654acf9ce5840d9a1a85c91534b64f28ca77810e85d27c3dbb9f4a0d5a1512a`. Validado pelo próprio workflow e pelo segundo workflow após publicar, sem reconstruir o binário.
- **Certificado permanente SHA-256:** `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`; mesmo certificado da versão principal anterior, obtido somente pelos GitHub Actions Secrets, sem revelar segredos.
- Build parametrizado `-Ppianolivre.zai.test=false -Ppianolivre.beta.versionCode=78 -Ppianolivre.beta.versionName=0.9.69-open-beta`. Não foi alterada a versão persistida da branch `main`. Workflow dedicado do [PR #131](https://github.com/atshimura/Piano-livre-aberto/pull/131).

## 4. Arquivo permanente para download sem Library

- [GitHub Actions #42905086514](https://github.com/atshimura/Piano-livre-aberto/actions/runs/42905086514) **SUCCESS**: recuperou o mesmo APK assinado, verificou o checksum, package, versão e assinatura, fez push ao GitHub e confirmou novamente o SHA-256 **nos bytes do arquivo remoto**.
- Branch `zai/downloads`, **commit imutável de publicação `ff8e8a978cf984c776dfa1afe9e24ee3a4bdc6f5`**, arquivo `apks/principal/PianoLivre-Principal-0.9.69-open-beta-assinado.apk` (796.260 bytes, objeto Git `4cd0c1e05d9f056fcb982bbff626980c610e116b`).
- **URL principal direta:**
  `https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/ff8e8a978cf984c776dfa1afe9e24ee3a4bdc6f5/apks/principal/PianoLivre-Principal-0.9.69-open-beta-assinado.apk`
- **Página alternativa:**
  `https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.69-open-beta-assinado.apk`
- **Comprovante de checksum:**
  `https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.69-open-beta-SHA256.txt`
- O workflow dedicado está no [PR #132](https://github.com/atshimura/Piano-livre-aberto/pull/132). O método de download não depende da biblioteca do ChatGPT nem da validade de 30 dias dos artefatos do Actions, embora dependa da permanência do GitHub e do repositório.

## 5. Pendências e próximos passos

1. **Backup verificado antes da instalação** no aparelho principal; instalar 0.9.69 por cima do 0.9.68, **sem desinstalar**, pois dados `.plc/.pls` precisam permanecer.
2. Ao abrir a partitura S1 salva por gerador anterior, selecionar **Gerar prévia atualizada**, conferir as pausas e a quebra automática e salvar somente após aprovar. A gravação original é preservada.
3. Validar fisicamente em Realme 7 5G, Lenovo Tab M9 e Yamaha P-45, especialmente em orientação paisagem/retrato, sem rolagem lateral e com ritmo legível. A CI Chromium prova a disposição visual no navegador, **não** a instalação física.
4. Não reduzir sistematicamente símbolos se isso deslocar notas ou apagar pausas reais. Não reintroduzir ligaduras automáticas. A rotina de edição manual de ligaduras fica para fase futura.
5. `main` e repositório histórico privado continuam intocados. Uma eventual integração futura da linha ZAi na `main` requer autorização e gates separados.

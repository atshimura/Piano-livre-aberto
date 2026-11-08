# ZAI-CONTINUIDADE.md — Linha de desenvolvimento Z (Z.ai/GLM)

Criada em **2026-11-06**, por decisão do mantenedor **Alexandre Shimura**.

## Propósito

Esta é a linha de desenvolvimento **paralela** conduzida com o agente Z.ai (GLM),
preservando **integralmente** a linha original de desenvolvimento (conduzida com
ChatGPT), documentada em [AGENTS.md](AGENTS.md). Nenhuma decisão, regra ou registro
do AGENTS.md é alterado por esta carta; as duas linhas convivem em namespaces
separados.

## Relação com a linha original

- A linha original (`main` + branches `feat/`, `fix/`, `ci/`, `docs/`, releases)
  permanece **intocada** pela linha Z. Nenhum push da linha Z se dirige a essas
  branches sem autorização expressa do mantenedor.
- **Sincronização unidirecional**: `main` → `zai/lab`, por merge periódico. A linha
  Z sempre contém tudo o que a linha original já entregou.
- O caminho reverso (Z → `main`) ocorre **somente** via pull request expressamente
  autorizado pelo mantenedor, sujeito aos gates `build`/`emulator` da CI.
- A linha Z **não** altera `versionCode`, `versionName`, releases, assinatura ou
  quaisquer publicações; numeração e releases permanecem sob comando da linha
  original.

## Regras da linha Z

1. Toda branch da linha Z usa o prefixo `zai/` (ex.: `zai/lab`, `zai/feat/...`).
2. Commits da linha Z usam o prefixo `[zai]` na mensagem, mantendo o histórico
   identificável linha por linha.
3. Sem force-push; sem reescrita de histórico público; sem tocar em branches da
   linha original.
4. Sem alterar workflows, testes ou proteções existentes; sem desativar gates.
5. Trabalho em **português do Brasil**; licença **GPL-3.0-only** do código próprio
   preservada; licenças de terceiros respeitadas (ver
   [LICENCAS-PARTITURAS.md](docs/LICENCAS-PARTITURAS.md) e `score-rights.json`).
6. Nunca enviar credenciais, chaves, certificados ou dados privados de alunos ao
   repositório; segredos apenas via GitHub Actions Secrets.
7. Validações da linha Z executadas localmente (JVM/JS/Python) e, quando
   aprovadas pelo mantenedor, via workflow dedicado próprio (ex.: `zai-basic.yml`),
   sem alterar os workflows existentes. Gates completos `build`/`emulator`
   permanecem exigidos **somente** antes de qualquer integração futura à `main`,
   sempre com autorização expressa.
8. Produto permanece **local/offline**: sem login, servidor, telemetria ou
   sincronização automática; sem permissão `INTERNET`; sem dependências externas
   novas sem autorização.
9. Regras de produto do [AGENTS.md](AGENTS.md) (modo Espera, teclado guia, MIDI,
   backup, dados `.plc`/`.pls`) valem integralmente para a linha Z.

## Decisão de produto ZAi — gerador de partituras (08/11/2026)

O mantenedor aprovou **reiniciar do zero o gerador de partituras da Criação**, sem preservar o motor de transcrição, seus ajustes ou a compatibilidade de leitura das antigas derivações .pls. O novo motor deverá reconhecer automaticamente **durações musicais** (especialmente semínima versus mínima) e pulsação a partir do MIDI, sem exigir ajustes do usuário; duas pautas Sol/Fá sempre, com divisão fixa **Dó4 (MIDI 60) ou acima → superior; abaixo de Dó4 → inferior** (inclui Si3/Si♭3 na inferior). Escalas e acordes são os primeiros alvos. A gravação fonte .plc e os demais dados/recursos não relacionados **não** podem ser apagados silenciosamente.

**Contrato vigente apenas na linha ZAi:** [ZAI-001 — Reinício do gerador de partituras e reconhecimento rítmico](docs/ZAI-001-REINICIO-GERADOR-PARTITURAS.md). A decisão revoga, no escopo do novo gerador ZAi, as regras históricas de interpretação e edição C2 que conflitam com ela; não altera a main nem o restante do aplicativo. Documento de planejamento, não implementação/testes.

## Estado

- Base inicial: `main` em **0.9.64-open-beta** (versionCode 73), commit `55426cf`.
- Ambiente de trabalho do agente Z: sandbox com histórico completo, identidade
  de commits `Alexandre Shimura <70826980+atshimura@users.noreply.github.com>`.

## Registro de tarefas da linha Z

| Data | Branch | Descrição | Estado |
|---|---|---|---|
| 2026-11-08 | `zai/docs/reinicio-partitura-ritmo-20261108` | Plano ZAI-001: novo gerador sem ajustes antigos, reconhecimento automático de semínimas/mínimas e duas pautas Sol/Fá com corte em C4. | Documento; código e testes não executados |
| 2026-11-06 | `zai/lab` | Criação da base da linha Z a partir de `main` (0.9.64). | Concluído |
| 2026-11-06 | `zai/lab` | Criação desta carta de protocolo (`ZAI-CONTINUIDADE.md`). | Concluído |
| 2026-11-06 | `zai/feat/target-sdk-35` | `targetSdk` 28→35; novo `ZEdgeInsets` (Android 15+: oculta barra de status via `WindowInsetsController` e aplica `systemBars`/`displayCutout` como padding; Android ≤14 inalterado); `MainActivity.onCreate` ganhou 1 chamada. Validação: `verify_public_snapshot` + 4 contratos Python + `py_compile` PASS; `compileDebugJavaWithJavac` e `assembleDebug` OK (APK debug 951 KB). Pendências de validação física: comportamento do lock `landscape` em telas grandes (Android 12+ pode ignorar) e aparência edge-to-edge no Android 15+. | Concluído (aguarda validação física) |
| 2026-11-06 | `zai/feat/refactor-selftest-launcher` | Tarefa 2 (1ª fatia): novo `ZDebugSelfTests.java` concentra o disparo dos self-tests de depuração (backup/perfis/biblioteca/histórico/MIDI-file/XML/upgrade/redirecionamento de prefs MIDI/fixtures de gravação/creação/discovery) antes espalhados em `MainActivity.onCreate`; 1265→1166 linhas, chamadas na mesma ordem e posição, strings de log preservadas (contrato dos emuladores). Produção intocada (tudo gated por `FLAG_DEBUGGABLE`). Validação: `javac` 17 completo (76 classes) contra android-35 PASS; `verify_public_snapshot` PASS; 11 contratos Python OK (incl. `test_workflow_contract_audit`). `assembleDebug` local bloqueado por rate limit 429 do Maven Central (código não afetado: sem mudanças em recursos/manifest); revalidação de build completo acontece na tarefa target-sdk-36. | Concluído |
| 2026-11-06 | `zai/feat/refactor-notation-failures` | Tarefa 2 (2ª fatia): novo `ZNotationFailures.java` concentra o fluxo de falhas de notação (`reportNotationIssue`, `showNotationFailure` ×2, `showNotationHistory`) antes em `MainActivity`; 1166→1094 linhas. `MainActivity` mantém delegados package-private com as assinaturas originais (API usada por `PianoScreens`, `CreationStudyCompatibility`, `CreationScorePreviewScreen`); textos verificados pelo contrato (`test_impl4_osmd_contract.py`) viram constantes nomeadas `NOTATION_FAILURE_TITLE`/`NOTATION_COPY_BUTTON` no `MainActivity` — teste existente intocado e passando. Comportamento idêntico: mesmos textos, botões, ordem e gravação local off-line (`NotationDiagnosticsStore`). Validação: `javac` 17 completo (80 classes) contra android-35 PASS; `verify_public_snapshot` PASS; 11 contratos Python PASS (76 testes, 7 subtests). | Concluído |
| 2026-11-06 | `zai/feat/target-sdk-36` | Tarefa 2-b: `targetSdk`/`compileSdk` 35→36 (Android 16); AGP 8.6.1→8.10.1 (suporte oficial ao SDK 36). Predictive back: `android:enableOnBackInvokedCallback="true"` no manifest; `MainActivity` compartilha `onSystemBack()` entre `onBackPressed` (API 26-32, caminho legado inalterado) e `OnBackInvokedCallback` (API 33+, registrado em `onCreate` com guard `SDK_INT<33`, cancelado em `onDestroy`); ordem histórica preservada (Lab → Estudo/Leitura livre → finish, equivalente ao antigo `super.onBackPressed()`). Telas grandes: lock `landscape` mantido (válido em telas <600dp); em telas ≥600dp o Android 16 ignora o atributo por design de plataforma e `PianoScreens` já responde por largura em dp (`compact` <620/650/900, `shortLandscape()`). `versionCode`/`versionName`/assinatura/workflows intocados. Validação: `javac` 17 completo contra android-36 PASS; `verify_public_snapshot` PASS; 11 contratos Python PASS (76 testes, 7 subtests); build Gradle completo local (Gradle 8.11.1 + SDK 36): `bundleRelease` OK (AAB 815 KB) e `assembleRelease` OK (APK 811 KB); `aapt2 dump badging` confirma `targetSdkVersion 36`, `minSdkVersion 26`, `compileSdkVersion 36`, `versionCode 73`, `versionName 0.9.64-open-beta`; `zipalign` OK. Notas: (a) build local contornou rate limit 429 do Maven Central com mirror do Google via init script em `GRADLE_USER_HOME` — nenhum arquivo do repo alterado; (b) artefatos locais assinados com chave debug efêmera (design do `build.gradle` público sem secrets) — não são artefatos de publicação, servem à validação física; (c) integração futura à `main` exigirá autorização do mantenedor para `gradle-version: '8.11.1'` e `platforms;android-36` em `build.yml`/`emulator.yml`. | Concluído (aguarda validação física) |

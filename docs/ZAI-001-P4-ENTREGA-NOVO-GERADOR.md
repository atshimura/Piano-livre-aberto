# ZAI-001 / Z-P4 — Entrega do novo gerador de partituras

**Data:** 08/11/2026. **Branch:** `zai/feat/reinicio-gerador-partituras` sobre `zai/lab` (`79766b9`).
**Contrato cumprido:** [ZAI-001-REINICIO-GERADOR-PARTITURAS.md](ZAI-001-REINICIO-GERADOR-PARTITURAS.md), etapas Z-P0 a Z-P4.

## 1. Commits desta entrega

| Commit | Etapa | Conteúdo |
|---|---|---|
| `4de9925` | Z-P0 | Inventário do motor C2, mapa de remoções, modelo novo e PLS2 (sem modificações destrutivas) |
| `169bcb9` | Z-P1/P2 | Pareador, estimador de pulso, quantizador, compositor MusicXML + testes JVM |
| `ea4000a` | Z-P2 | Fixtures OSMD validados no parser Score e no Chromium real |
| `631001f` | Z-P3 | Prévia limpa, PLS2, remoção física do motor C2 e adaptação de UI/backup/workflows/contratos |
| `4eae250` | Z-P4 | Correções da validação Android e compilação real (compile + assembleDebug) |

**PR:** [#111](https://github.com/atshimura/Piano-livre-aberto/pull/111) — `zai/feat/reinicio-gerador-partituras` → `zai/lab`, aberto em 08/11/2026 (branch enviada e PR criado via API com token efêmero do mantenedor, revogado em seguida).

## 2. O que foi implementado

**Motor novo (puro, determinístico, sem `android.*`):**
- `ScoreMidiPairer` — pareia Note On/Off por canal+pitch em tempo musical de segmentos
  (pausas de gravação removidas; primeiro ataque = tempo 0); retrigger, Note Off sintético
  e órfão tratados com avisos legíveis; `sourceId` estável `zg-<ordem>-<canal>-<pitch>`.
- `ScoreTempoEstimator` — candidatos de semínima derivados de intervalos de ataque e
  durações sustentadas; pontuação por encaixe na grade de semicolcheia, figuras usuais e
  ajuste dos intervalos; desempate determinístico (intervalo de ataque mais comum = semínima);
  confiança HIGH/MEDIUM/LOW; sem referência segura → 120 BPM rotulado provisório (nunca
  120 BPM universal); aviso explícito na ambiguidade metade/dobro.
- `ScoreRhythmQuantizer` — 4/4 fixo, grade de semicolcheia (4/semínima), divisão de pauta
  fixa em MIDI 60 (≥60 pauta de Sol), vozes por ocupação (máx. 4/pauta; sobreposição nunca
  é truncada), acordes por ataque comum + duração igual, pausas por voz, pauta silenciosa
  sempre visível com pausa inteira, ligaduras ao cruzar compasso, duração zero proibida.
- `ScoreXmlComposer` — MusicXML `score-partwise` 4.0, 1 parte Piano, `staves 2`, claves
  G2/F4, key 0, 4/4, divisions 960, `<backup>` entre vozes, `<chord/>`, `<tie>`/`<tied>`,
  direção de metrônomo + `sound tempo`, título escapado, saída byte-idêntica por entrada.
- `CreationScoreGenerator` — fachada `generate(events, segments, title)`.

**Interface (ZAI-001 §5):** Minhas Criações → Detalhes → Gerar partitura → prévia com duas
pautas, cabeçalho compacto com "Andamento estimado: X BPM" ou aviso de provisório, zoom
A−/A+/Ajustar à largura, Salvar (com prova OSMD Criação→Estudar) e Voltar. Nenhum controle
antigo: regularização, BPM manual, fórmula de compasso, grade, divisão de pautas, mão/voz,
uniformizar, seleção de notas, desfazer/refazer e restauração foram eliminados do código.

**Persistência PLS2:** `creation-scores/<uuid>.pls` mantém nome e quarentena; formato novo
sem opções (0x504C5332, SHA-256 + fingerprint). Gravações `.plc` nunca são escritas.
Derivadas PLS1 antigas: reconhecidas como legadas (`LegacyScoreException`), exibidas com
estado explicativo e botão "Gerar nova partitura"; nunca apagadas silenciosamente; no
backup v4 são preservadas com flag `legacy` e saltadas na restauração com aviso no resumo.

**Remoção física:** `CreationScoreTranscriber`, `CreationScoreReview`, `CreationC2DProbe`,
`creation-note-picker.js`, API de seleção do `NotationView`, bloco C2 do `notation.html`,
testes do motor antigo, contrato `test_creation_rhythm_contract.py` e o workflow
`dialog-scroll-basic.yml`. Workflows `c2-basic`, `creation-rhythm-basic`, `impl4-osmd-basic`
e `build` tiveram apenas as listas de compilação/testes atualizados; triggers, gates,
assinatura, versionCode/versionName e publicação intocados.

## 3. Testes executados (resultados reais desta sessão)

**JVM (JDK 17.0.21, `javac`+`java`, padrão do repositório):**

| Teste | Resultado |
|---|---|
| `ZScoreGeneratorRhythmTest` | **85 verificações OK** — R01 (8 semínimas @120), R02 (8 semínimas+8 mínimas @120, 6 compassos), R03 (igual @90, BPM≈90, nunca 120 imposto), R10 (pausa de 1 tempo), R11 (staccato literal sem cortes), R12 (nota isolada: provisório rotulado), R13 (pausa/retomada, sintéticos, órfão), retrigger, nota nunca solta, gravação sem notas, limite de 8000 notas; validade DOM e preenchimento exato de cada compasso (3840 ticks por voz) |
| `ZScoreGeneratorXmlTest` | **26 verificações OK** — R04/R05 (acordes em Fá/Sol), R06 (acordes simultâneos nas duas pautas), R07 (Si3/Dó4 no limite 59/60), R08 (sustentada + nova nota em vozes separadas, duração integral), R09 (ligadura de compasso), sustenidos, determinismo, escape, rejeições |
| `CreationScoreStoreTest` | **27 verificações OK** — roundtrip PLS2, `.plc` intocado, rejeições de perfil/legado, lixeira, quarentena `.deleting`, corrupção, PLS1 → `LegacyScoreException`, substituição por regeneração |
| `CreationC2DCorpusTest` | **45 verificações OK** (corpus 320 notas em 152 ms) — interop com `Score.read`, polifonia cruzada, determinismo byte-a-byte, rejeições |
| `CreationOSMDCompatibilityFixture` | **9 fixtures PASS** — 100% das notas sobrevivem ao parser do Estudo |
| `CreationScoreDeletionTest` / `CreationProjectStoreTest` / `CreationJournalTest` / `MidiTakeRecorderTest` / `CreationLimitsTest` | **13 / 10 / 6 / 24 verificações OK** + limites — C1, journals e quarentena inalterados |

**Browser OSMD real (Playwright/Chromium headless, OSMD 1.9.8 offline):**
`IMPL4_OSMD_CREATION_STUDY_BROWSER_PASS cases=18` — 9 partituras × layouts `free` e
`engraving-poc`, incluindo `quarters-halves-120` e `quarters-halves-90`; sem rede, sem
fallback temporal, erro estruturado verificado.

**Contratos Python:** 11 suítes **OK** (deletion, library, impl4-OSMD, responsive,
six-changes, workflow-audit, library-removal, study-chrome, home-banner, position-labels,
classify-pr-risk) + `py_compile` dos smokes + `verify_public_snapshot.py` **PASS**.

**Compilação Android real (Gradle 8.11.1, AGP 8.10.1, SDK 36, JDK 17):**
- `:app:compileDebugJavaWithJavac` — **BUILD SUCCESSFUL**
- `:app:assembleDebug` — **BUILD SUCCESSFUL**; APK debug 938.337 bytes; `aapt2 dump badging`:
  `br.org.pianolivre.opensource`, `versionCode 73`, `versionName 0.9.64-open-beta`,
  `targetSdkVersion 36` — versionamento e pacote intocados.

## 4. Correções feitas durante a validação (registradas)

- Cursor interno não avançava com pausas entre notas (compasso excedido/mal preenchido).
- Tokens de acorde não devem avançar posição nem colidir com o cursor.
- Acordes detectados por posição escrita na voz (continuações de ligadura de acordes
  permanecem empilhadas após a divisão de compasso).
- Pauta sem voz alocada recebe pausa inteira (duas pautas sempre visíveis).
- `Validated.legacyScores` no backup e `onRendered()` do listener da prévia.

## 5. Limitações e pendências de validação física

1. **Não executado:** suíte completa `build.yml`/`emulator.yml` em CI, emulador Android e
   smoke `c2_d_ui_smoke.py` real (o sandbox não tem emulador/KVM). O smoke foi atualizado e
   compila (`py_compile`), mas precisa rodar num run de emulador.
2. **Não executado:** testes em Yamaha P-45, Realme 7 5G e Lenovo Tab M9 (aceite físico do
   contrato; CI/emulador nunca os substitui).
3. **Não publicado:** o sandbox não tem credenciais GitHub — o push da branch e a abertura
   do PR para `zai/lab` (e o run da CI nesse SHA) ficam para o mantenedor ou para uma sessão
   com credenciais. Nenhum force-push; `main` e releases intocadas.
4. **Ambiguidade musical inerente:** leituras de metade/dobro do pulso são matematicamente
   equivalentes; o estimador adota o intervalo de ataque mais comum como semínima, avisa
   quando a alternativa empata e degrada a confiança quando nada separa as hipóteses
   (R12 cobre o caso extremo). Execuções com rubato amplo podem quantizar com aviso.
5. **Fora do marco (conforme contrato):** articulações escritas, pedal gráfico, quiálteras,
   anacruses, fórmulas variáveis, soletração enarmônica refinada (pretas soletradas com
   sustenidos simples), edição musical.
6. **Derivadas PLS1 antigas** deixam de ser legíveis por design (decisão aprovada); o app
   exibe estado explicativo, preserva o arquivo e oferece regenerar. Backups antigos com
   derivadas legadas restauram tudo, exceto essas derivadas (aviso no resumo).

## 6. Próximos passos

1. ~~Push da branch e PR `zai/feat/reinicio-gerador-partituras` → `zai/lab`~~ — **concluído: PR #111**.
2. Rerun local dos comandos acima ou acionar um workflow dedicado da linha Z (ex.: `zai-basic.yml`),
   se autorizado, para reproduzir a bateria no SHA do PR.
3. Smoke de emulador (`c2_d_ui_smoke.py`) num runner com KVM.
4. Validação física pelo mantenedor (R14/R15 em aparelho real: Salvar → Reabrir → Estudar;
   bibliotecas, backup e exclusão de derivado).

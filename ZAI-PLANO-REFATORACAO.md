# ZAI-PLANO-REFATORACAO.md — Decomposição do `MainActivity` (linha Z)

Criado em **2026-11-06** pelo agente Z, sob protocolo de
[ZAI-CONTINUIDADE.md](ZAI-CONTINUIDADE.md), por decisão do mantenedor
**Alexandre Shimura**. Branch da tarefa: `zai/feat/refactor-mainactivity`.

## 1. Contexto e decisão de cenário

A linha Z é um **desenvolvimento independente** — não há merge previsto com a
`main`. Consequências que moldam este plano:

- **Não precisamos preservar compatibilidade de merge** com a linha original.
  A restrição relevante não é "manter o `MainActivity` parecido com o da
  `main`", e sim: **os 11 contratos Python da linha Z continuam verdes** e o
  comportamento observável do app permanece **idêntico**.
- O benefício da refatoração é **para a própria linha Z**: velocidade de
  iteração (cada tema num arquivo), menor risco de regressão em edições
  futuras (predictive back, telas grandes, fluxos de criação), onboarding
  trivial de qualquer sessão futura do agente e preparação para a Play Store.
- Custo aceito: divergência adicional em relação à `main`. Se um dia houver
  integração, ela será seletiva (por tema), e arquivos bem separados tornam
  essa seleção **mais fácil**, não mais difícil.

## 2. Princípios fixos (valem para toda fatia)

1. **Zero mudança de comportamento**: mesmos textos, mesmas ordens de
   chamada, mesmos timings, mesmos logs. Refatoração pura.
2. **Contratos intocados**: os 11 arquivos em `PianoLivreAndroid/tests/`
   (76 testes, 7 subtests) e `scripts/verify_public_snapshot.py` não são
   editados. Antes de mover qualquer símbolo, `grep` nos contratos; onde o
   contrato crava a chamada em `PianoScreens` (`a.saveSession()`,
   `a.loadScore(...)`, `a.showHistory()`, `a.stopPlayback()`,
   `a.toggle()`, `a.selectHand(idx)`, `a.seekPrevious()`,
   `a.selectMidi()`, `a.manageProfile()`, `a.openFreeScore(...)`) ou a
   estrutura entre `addCreationScoreToLibrary` e `loadScore`, o
   `MainActivity` mantém **delegado package-private** com assinatura
   idêntica (padrão já validado na fatia 2 / `ZNotationFailures`).
3. **Classes novas com prefixo `Z`**, package-private, mesmo pacote
   `br.org.pianolivre`, sem novas dependências externas, sem novos recursos
   (strings/drawables), sem alterar `AndroidManifest.xml`.
4. **Áreas reservadas intocadas**: `versionCode`, `versionName`, assinatura,
   releases, workflows, branches da linha original.
5. **Um commit por fatia** (`[zai] refactor: ...`) + commit de registro
   (`[zai] docs: ...`) em `ZAI-CONTINUIDADE.md`. Sem force-push.
6. **Português do Brasil** em comentários, diálogos e mensagens de commit.

## 3. Ponto de partida (medido em 2026-11-06)

`MainActivity.java`: **1121 linhas**, ~76 classes no pacote. Fatias já
concluídas: 1 (`ZDebugSelfTests`), 2 (`ZNotationFailures`). Resíduo
inventariado por responsabilidade (linhas aproximadas):

| Bloco | Conteúdo | ~Linhas |
|---|---|---|
| A. Casca da Activity | `onCreate/onResume/onPause/onDestroy/onWindowFocusChanged`, predictive back, `onSaveInstanceState` | 95 |
| B. Dispositivos MIDI | `setupMidi`, `reconcileMidiDevices`, `connectMidi/disconnectMidi`, `createReceiver`, nomes de dispositivo | 120 |
| C. Eventos MIDI + diagnóstico | `onMidiMessage`, `onKey`, `send/allOff`, sustain, `midiDiagnosticReport`, `showMidiDiagnostics`, `testMidiOutput` | 200 |
| D. Calibração de acordes | `showChordSettings`, `beginChordCalibration`, `editChordWindow`, janela por perfil | 80 |
| E. Motor de estudo | `selectHand`, `nextWait`, `prepareWaiting`, `seekPrevious`, loop A–B, marcas, `toggle`, preparação do Espera, `ticker` (25 ms), `stopPlayback`, `updateInfo` | 230 |
| F. Perfis | `newProfile`, `loadProfiles`, `activateProfile/applyProfile`, `manageProfile` | 70 |
| G. Histórico de estudo | `begin/finishStudyWindow`, `showHistory`, `showHistoryPeriods` | 55 |
| H. Sessão (prefs) | `saveSession` + restauração `user_N_*` em `onCreate`/`loadScore`/`applyProfile` | 50 |
| I. Partitura: carga/importação | `loadScore` ×3, `addCreationScoreToLibrary`, `importScore`, `chooseMidiHands`, branch IMPORT de `onActivityResult` | 200 |
| J. Export MIDI de criação | `prepareCreationExport`, cache+SHA-256, branch CREATION_MIDI_EXPORT | 55 |
| K. Backup/restauração | `makeBackup`, `exportBackup`, `restoreBackup`, branches EXPORT/RESTORE | 45 |
| L. Delegados de notação | já finos (fatia 2) | 10 |

## 4. Arquitetura-alvo

`MainActivity` vira **casca fina** (meta ≤ 500 linhas, esperado ~400):
ciclo de vida, criação do `PianoScreens`, registro de callbacks e os
delegados package-private exigidos pelos contratos. Cada bloco acima vira
uma colaboradora única, dona do seu estado:

```
MainActivity (casca: lifecycle + wiring + delegados)
├── PianoScreens        (UI, já existe)
├── ZSessionState       (H — sessão user_N_* em SharedPreferences)
├── ZCreationExport     (J)  ┐ fluxos de documento,
├── ZBackupFlow         (K)  ┘ quase autônomos
├── ZStudyHistoryUi     (G)  ┐ diálogos de perfil/histórico,
├── ZProfilesUi         (F)  ┘ baixo acoplamento
├── ZChordCalibrationUi (D)
├── ZMidiDevices        (B — descoberta/conexão/portas)
├── ZMidiInput          (C — interpretação de eventos + diagnóstico)
├── ZScoreLoader        (I — carga de partitura + importação)
└── ZStudyEngine        (E — timeline/ticker/Espera/loop/rota escrita)
```

Nada disso é API pública do app (tudo package-private na prática, pelo
mesmo pacote); `MainActivity` continua sendo a única `Activity`.

## 5. Fatias, ordem e risco

A ordem vai do **menor acoplamento ao maior**: primeiro os blocos cujo
estado é quase privado; o motor de estudo — dono do estado mutável mais
entrelaçado — por último, já com todas as colaboradoras existentes.

| Fatia | Classe(s) nova(s) | Blocos | Risco | Observações |
|---|---|---|---|---|
| **3** | `ZSessionState` | H | baixo | Remove o acoplamento de prefs que as fatias 7-9 herdariam; delegado `saveSession()` permanece (contrato). |
| **4** | `ZCreationExport`, `ZBackupFlow` | J, K | baixo | I/O em disco com `ExecutorService` próprio; branches de `onActivityResult` delegam. Estado `pendingCreationExport*` migra junto (com `onSaveInstanceState`). |
| **5** | `ZStudyHistoryUi`, `ZProfilesUi` | G, F | baixo | Delegados `showHistory()`/`manageProfile()` permanecem; `begin/finishStudyWindow` migram para `ZStudyHistoryUi` com hooks do lifecycle. |
| **6** | `ZChordCalibrationUi` | D | baixo | Diálogos + janela por perfil; lógica de medição continua em `ChordCalibration` (intocada). |
| **7** | `ZMidiDevices`, `ZMidiInput` | B, C | **médio** | Threads, guards de `generation`/`epoch`, callback de status. **Marco 1: build Gradle completo (APK debug) após esta fatia.** |
| **8** | `ZScoreLoader`, `ZScoreImport` | I | médio | Contrato veda `MidiImportStorage.save(` entre `addCreationScoreToLibrary` e `loadScore` — a estrutura de delegados respeita isso. |
| **9** | `ZStudyEngine` | E | **alto** | Estado mutável (`position`, `loop`, rota escrita, Espera) + `ticker`. Se necessário, divide-se em 9a (timeline/loop/ticker) e 9b (Espera). |
| **10** | — (consolidação) | A, limpeza | baixo | Remoção de helpers órfãos (`OnSelected`/`Listener`/`spin`/`button`, se sem uso), `onCreate` enxuto, revisão final. **Marco 2: AAB + APK finais para validação física.** |

## 6. Protocolo autônomo por fatia (o que o agente executa sozinho)

1. `grep` dos símbolos a mover em `PianoLivreAndroid/tests/` → definir
   delegados/constantes necessários **antes** de editar.
2. Extração: classe `Z*` package-private, mesmo pacote; `MainActivity`
   mantém delegados com assinaturas originais.
3. Gates obrigatórios, nesta ordem:
   - `javac` 17 completo (todas as classes + stub `R`) contra `android.jar`
     do SDK 36;
   - `scripts/verify_public_snapshot.py` → PASS;
   - 11 contratos `pytest` (76 testes, 7 subtests) → PASS.
4. Commit único `[zai] refactor: <fatia>` + push em
   `zai/feat/refactor-mainactivity`.
5. Commit `[zai] docs: registra fatia N no registro da linha Z` atualizando
   `ZAI-CONTINUIDADE.md`.
6. Artefatos instaláveis **somente** nos Marcos 1 e 2 (builds Gradle são
   lentos e as fatias intermediárias já ficam cobertas pelos gates).

## 7. Intervenção do mantenedor (mínimo absoluto)

O agente **não pede aprovação por fatia**; a revisão é assíncrona, pelos
commits no GitHub. O agente **para e consulta** somente se:

1. algum contrato só passar com alteração no **teste** (proibido — indica
   erro de plano, não de código);
2. surgir decisão de **produto** (mudança visível ao usuário — fora de
   escopo por definição);
3. for inevitável tocar **área reservada** (não é esperado em fatia
   nenhuma);
4. o ambiente falhar de forma não contornável (ex.: rede/mirror).

Intervenções ativas restantes do mantenedor no ciclo todo:

- **Uma única validação física** ao final (Marco 2): instalar o APK em
  aparelho Android 13+, piano USB P-45, Espera/Contínuo/Demonstração,
  importação e backup;
- dizer "continue" para a próxima fatia em cada sessão (ou agrupar várias
  fatias por sessão);
- ao encerrar o trabalho com o agente: **revogar o token** de acesso.

## 8. Critérios de conclusão e rollback

- Conclusão: fatias 3-10 concluídas, `MainActivity` ≤ 500 linhas, todos os
  gates verdes, artefatos finais gerados, registro completo em
  `ZAI-CONTINUIDADE.md`, validação física registrada.
- Rollback: cada fatia é um commit isolado; `git revert <sha>` desfaz uma
  fatia sem tocar as outras. A branch é independente; `zai/lab` só a recebe
  por merge explícito quando a fatia estiver concluída e validada.

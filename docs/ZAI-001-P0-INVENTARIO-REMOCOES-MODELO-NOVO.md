# ZAI-001 / Z-P0 — Inventário do gerador antigo, mapa de remoções e modelo novo

**Data:** 08/11/2026. **Etapa:** Z-P0 (inventário; nenhuma modificação destrutiva).
**Branch:** `zai/feat/reinicio-gerador-partituras` sobre `zai/lab` em `79766b9`.
**Contrato:** [ZAI-001-REINICIO-GERADOR-PARTITURAS.md](ZAI-001-REINICIO-GERADOR-PARTITURAS.md).

## 1. Inventário de referências ao motor C2 antigo

Motor a eliminar: `CreationScoreTranscriber` e `CreationScoreReview`, com os controles
de ajuste em `CreationScorePreviewScreen`.

### 1.1 Código de produção que referencia o motor antigo

| Arquivo | Referências | Destino |
|---|---|---|
| `CreationScoreTranscriber.java` (419 lin.) | o próprio motor (Options, Regularization, StaffMode, Timing, interpretAttacks, transcribe) | **Remover** |
| `CreationScoreReview.java` (266 lin.) | usa Options/Result/WrittenNote em toda a classe; seleção, undo/redo, uniformizar | **Remover** |
| `CreationScorePreviewScreen.java` (565 lin.) | instancia review; editRhythm (regularização/BPM/compasso/grade), splitControls (divisão de pautas, seleção), chooseHandVoice, chooseUniformization/previewUniformize, undo/redo, restaurar sugestão | **Reescrever** sem controles |
| `CreationScoreStore.java` (209 lin.) | valida o `.pls` regenerando via `transcribe`; persiste Options (v1 legado + v2) | **Reescrever** (formato PLS2, §3) |
| `NotationView.java` | `setCreationSourceNotes(List<WrittenNote>)`, `setCreationSelected`, `sendCreationSource`, `onCreationNoteTapped` | **Remover métodos de seleção**; resto intacto |
| `CreationC2DProbe.java` | gera fixture C2-D com o motor antigo | **Remover** (substituído por fixtures do novo motor) |
| `BackupArchiveSelfTest.java` | cria derivado com overrides do motor antigo | **Ajustar** para o novo gerador |
| `MusicCreationScreen.java` | rótulo "Visualizar e ajustar partitura"; diálogo de exclusão menciona "ajustes manuais" | **Ajustar rótulos** |
| `MidiFileImport.java` linha 68 | comentário citando o transcritor | **Ajustar comentário** |

`MainActivity` não referencia diretamente o motor antigo: usa `CreationScoreStore`,
`CreationStudyCompatibility`, `CreationScoreDeletion` e `LibraryStore` — todos permanecem.

### 1.2 Testes e contratos que cobrem comportamentos revogados

| Arquivo | Conteúdo | Destino |
|---|---|---|
| `tests/CreationScoreTranscriberTest.java` | 27 verificações do motor C2-A | **Remover** |
| `tests/CreationScoreReviewTest.java` | seleção/mover/undo/redo C2-B | **Remover** |
| `tests/CreationRhythmUniformizeTest.java` | interpretAttacks, uniformizar trecho (Impl. 3) | **Remover** |
| `tests/test_creation_rhythm_contract.py` | contrato estático do motor antigo e dos botões de ajuste | **Remover** |
| `tests/CreationNotePickerTest.js` + `assets/creation-note-picker.js` | seleção de notas na prévia | **Remover** (V0 sem edição) |
| `tests/CreationScoreStoreTest.java` | roundtrip v1/v2 + regeneração pelo motor antigo | **Substituir** (formato PLS2; quarentena `.deleting` permanece) |
| `tests/CreationC2DCorpusTest.java` | corpus 320 notas pelo motor antigo | **Substituir** por corpus do novo gerador |
| `tests/CreationOSMDCompatibilityFixture.java` | fixtures MusicXML gerados pelo motor antigo | **Substituir** geração pelo novo gerador |
| `tests/CreationOSMDStudyBrowserTest.js` | consome os fixtures acima | Estrutura mantida; fixtures novos |
| `tests/test_creation_score_deletion_contract.py` | exclusão só-da-partitura | **Manter**; ajustar rótulos/mensagens |
| `tests/test_creation_library_contract.py` | biblioteca por referência + gate OSMD | **Manter**; revisar asserts da prévia |
| `tests/test_impl4_osmd_contract.py` | OSMD único, gate Criação→Estudar | **Manter**; gate passa a existir no novo fluxo Salvar |
| `tests/test_creation_responsive_contract.py`, `tests/test_suite_six_changes_contract.py` | tags de UI da prévia antiga | **Ajustar** |
| `tests/c2_d_ui_smoke.py` | fluxo emulador com seção de ajustes (141–231) | **Reescrever** seção de prévia; manter biblioteca/exclusão |
| `tests/CreationScoreDeletionTest.java`, `tests/CreationProjectStoreTest.java`, `tests/CreationJournalTest.java`, `tests/MidiTakeRecorderTest.java`, `tests/CreationSelfTest.java` | C1, storage, quarentena | **Manter intactos** |

### 1.3 Workflows

| Workflow | Referência | Ação |
|---|---|---|
| `c2-basic.yml` | compila `CreationScoreTranscriber/Review`, roda TranscriberTest/ReviewTest/RhythmUniformizeTest/NotePickerTest e o probe C2-D | **Atualizar listas** para o novo motor (ligação de compilação substituída) |
| `creation-rhythm-basic.yml` | contratos + JVM do ritmo antigo | **Atualizar** |
| `dialog-scroll-basic.yml` | exclusivo do diálogo de ajustes antigo | **Remover** |
| `impl4-osmd-basic.yml` | fixtures impl4 pelo motor antigo | **Atualizar** geração de fixtures |
| `build.yml`, `emulator.yml` | suíte completa menciona testes removidos; `emulator.yml` roda `c2_d_ui_smoke.py` | **Atualizar apenas as listas** de arquivos; gates, triggers, proteções e publicação intocados |

Restrição da ZAI-001 §7 ("não alterar workflows antigos") é interpretada assim: **gates,
triggers, assinatura, publicação e proteção da `main` intocados**; as listas de arquivos
que compilam o motor removido precisam acompanhar a remoção física para não quebrar a CI
— são as "ligações de compilação" cuja substituição a própria ZAI-001 §6 exige.

## 2. Dados que permanecem intocados

- **Gravações `.plc`** (`creations/<uuid>.plc`, CRC32, formatos 1/2), journals
  (`creation-journals/`), `MidiTakeRecorder`, `CreationProjectStore`, `CreationJournal`:
  fonte do novo gerador; **nenhuma escrita**.
- **Biblioteca, perfis, favoritos, coleções, histórico, backups, partituras importadas.**
- `CreationScoreDeletion` (quarentena `creation-score.delete-pending/` + `.deleting`)
  permanece; opera sobre nomes de arquivo, que não mudam (`creation-scores/<uuid>.pls`).
- `CreationStudyCompatibility.check` (prova OSMD engraving) permanece como gate de
  Salvar e de Adicionar à Biblioteca.
- Transporte MIDI, exportação `.mid`, reprodução da gravação: intocados.

## 3. Estrutura fechada do modelo novo (implementado em Z-P1/Z-P2)

Componentes puros (sem `android.*`), determinísticos, com testes independentes:

| Classe | Responsabilidade | Saída |
|---|---|---|
| `ScoreMidiPairer` | parear Note On/Off por canal+pitch em tempo musical de segmentos; tratar retrigger, Note Off sintético, órfãos, notas abertas no fim; início relativo ao primeiro ataque | `List<PairedNote>{sourceId,pitch,channel,velocity,startNs,endNs,syntheticEnd,sourceOrder,sourceEpoch}` + avisos |
| `ScoreTempoEstimator` | candidatos de período de semínima a partir de diferenças de ataques e durações; encaixe na grade 1/16, figuras usuais, fragmentação, estabilidade; desempate determinístico (moda dos intervalos = 1 semínima); confiança | `TempoEstimate{quarterNs,bpm,confidence(HIGH/MEDIUM/LOW),warnings}` |
| `ScoreRhythmQuantizer` | quantizar início e fim separadamente na grade 1/16; duração escrita (semibreve…semicolcheia + ponto); zero duração proibido; alocação de vozes por intervalos (mesma pauta); acordes por ataque comum+duração igual; divisão de pauta fixa MIDI 60; fragmentação por compasso 4/4 com ties | `List<WrittenNote>{staff,voice,startTick,durationTicks,ties…}` + pausas por voz/compasso |
| `ScoreXmlComposer` | serializar `score-partwise`, 1 parte Piano, `staves 2`, claves G2/F4, key 0, time 4/4, divisions 960, direção de andamento, backup entre vozes, chord/tie/rest; escape de título; limites | MusicXML determinístico |
| `CreationScoreGenerator` | fachada: `generate(events,segments,title)` = pair→estimate→quantize→compose | `Generation{xml,notes,measures,bpm,confidence,warnings}` |

Constantes compartilhadas: `MAX_NOTES=8000`, `MAX_MEASURES=1024`, `MAX_XML_CHARS=8_000_000`,
`DIVISIONS=960`, `SPLIT_MIDI=60` (≥60 → pauta de Sol), `GRID_PER_QUARTER=16`.

## 4. Formato novo de persistência da partitura derivada (PLS2)

Mesmo diretório/extensão (`creation-scores/<uuid>.pls`), mesmo esquema de integridade
(corpo + SHA-256), **sem opções de ajustes** (V0 sem edição musical):

```
magic 0x504C5332 ("PLS2") | format 1
writeUTF projectId | int profileId | writeUTF fingerprint(gravação) | writeUTF título
int bpmEstimado (0 = não identificado com segurança)
int xmlLen + bytes MusicXML
SHA-256(body) [32 bytes]
```

- `CreationScoreStore.Saved {projectId, profileId, xml, bpm, fingerprint}` (sem `options`).
- `save` valida dono/projeto ativo e tamanho; `read` valida hash, fingerprint e campos.
- **Derivados PLS1 (antigos)**: nunca apagados automaticamente. `read` reconhece o magic
  antigo e lança `LegacyScoreException`; a interface exibe estado próprio ("gerada pelo
  gerador anterior; gere novamente") e o botão Gerar substitui o arquivo pelo PLS2.
  No backup v4, uma derivada legada é **saltada com aviso** na restauração, sem falhar
  o backup nem apagar dados. Documento a incompatibilidade assumida nas notas da entrega.
- `fingerprint(Project)` continua SHA-256 de perfil+id+segmentos+eventos (fonte de verdade
  do vínculo derivado↔gravação).
- `BackupArchive` v4: mantém embutir `creation-scores` base64; validação passa a usar o
  `verified` novo; exceção de legado não derruba a restauração.

## 5. Fixtures de entrada/resultado (matriz ZAI-001 §8)

Fixtures sintéticos construídos em memória com `MidiTakeRecorder.Event` + `Segment`
(tempos em ns, relógio de referência; sem depender de execução humana):

| Fixture | Entrada | Resultado exigido |
|---|---|---|
| R01 | Dó4→Dó5, 8 semínimas @120 BPM | 8×`<type>quarter</type>`; claves G2/F4 |
| R02 | 8 semínimas + 8 mínimas @120 | exatamente 8 quarter + 8 half; 6 compassos |
| R03 | igual R02 @90 | mesmas figuras; BPM ≈90 (não 120) |
| R04/R05 | acordes Dó3-Mi3-Sol3 / Dó4-Mi4-Sol4 | `<chord/>` com 3 alturas na pauta correta |
| R06 | R04+R05 simultâneos | mesmos ataques nas duas pautas; 6 notas |
| R07 | Si3 + Dó4 juntos | 59→pauta Fá; 60→pauta Sol |
| R08 | nota sustentada + nova nota | durações integrais; vozes separadas |
| R09 | nota cruzando barra | tie start/stop, sem novo ataque |
| R10 | pausa de 1 tempo | `<rest>` escrita; ataque seguinte na posição certa |
| R11 | staccato/legato/jitter | sem corte arbitrário; avisos registrados |
| R12 | nota isolada | confiança LOW; 120 BPM provisório rotulado |
| R13 | pausa/retomada, Note Off sintético/órfão | eventos tratados; sem crash; avisos |

## 6. Interface de destino (Z-P3)

`Minhas Criações → Detalhes → Gerar partitura → Prévia (pautas, cabeçalho com "Andamento
estimado: X BPM" ou aviso de estimativa incerta, zoom +/−, ajustar à largura) → Salvar / Voltar`.
Sem regularização, BPM manual, fórmula de compasso, grade, divisão de pautas, mão/voz,
uniformizar, seleção, undo/redo ou restauração de sugestões.

## 7. Estado da etapa

**Feito:** inventário completo; estrutura do modelo novo fechada; formato PLS2 definido;
política de dados legados definida; fixtures especificados.
**Não feito (etapas seguintes):** implementação (Z-P1/P2), troca de interface e remoção
física (Z-P3), regressão e validação Android (Z-P4).

# 104 — Criação C2-C: persistência, backup v4 e Estudar

**Estado verificado (02/11/2026):** [PR #98](https://github.com/atshimura/Piano-livre-aberto/pull/98) integrada à branch técnica C2-A/B, squash `7ef0b13d1f78ea210b5278f3a05029377cf38cf7`. **Não está na `main` nem em Release pública.** Rastreio [issue #95](https://github.com/atshimura/Piano-livre-aberto/issues/95); especificação C2 [PR documental #96](https://github.com/atshimura/Piano-livre-aberto/pull/96); histórico C2-A [102](102-CRIACAO-C2-A-MOTOR-IMPLEMENTACAO.md), C2-B [103](103-CRIACAO-C2-B-PREVIA-AJUSTES.md).

## História do usuário implementada

1. Em **Laboratório Musical → Criação → Minhas Criações → Detalhes**, uma composição salva apresenta **Gerar partitura**, ou **Visualizar e ajustar partitura / Estudar esta partitura** quando a partitura derivada já existe.
2. A prévia de C2-B é gerada em memória e mantém notas, seleção de pautas, BPM, compasso, quantização, acordes, undo/redo. **Salvar partitura** agora grava uma **derivação privada versionada** sem alterar o arquivo `.plc` nem reescrever o MIDI.
3. A janela de revisão abre novamente as escolhas salvas. Se for cancelada após edições, a versão salva anteriormente permanece intacta. Se a derivação anterior estiver corrompida/incompatível, uma nova prévia pode ser gerada sem apagar o original; o usuário é informado.
4. **Estudar esta partitura** encaminha ao modo Estudar existente usando um identificador `creation-score:<uuid>`, sem duplicar a partitura na biblioteca `imported/`. O carregamento valida ID, perfil proprietário, estado da composição, hash da fonte e integridade antes de abrir MusicXML. A sessão usa uma chave portátil e o botão Voltar retorna aos detalhes da Criação.
5. O projeto na Lixeira mantém sua partitura derivada, mas não permite novas revisões/estudo pela interface. A exclusão definitiva remove também a derivação usando quarentena recuperável após interrupção inesperada.

## Armazenamento e segurança

- Novo `CreationScoreStore.java` gerencia `filesDir/creation-scores/<UUID>.pls`, formato binário `PLS1` v1 (não confundir com versão do backup), tamanho individual máximo **2 MiB** e conjunto máximo **8 MiB**.
- Registra ID/proprietário, hash SHA-256 dos eventos canônicos/segmentos (independente de renomear título ou Lixeira), título usado para gerar o XML, parâmetros, ajustes manuais por `sourceId`, XML interno e SHA-256 da própria derivação. Releitura regenera e confere exatamente o MusicXML; recusa erros e não elimina a gravação.
- Escrita em arquivo temporário, `sync` e substituição com `ATOMIC_MOVE`. Se a substituição falhar, a partitura anterior não é sobrescrita; não há fallback inseguro para arquivos parcialmente gravados.
- Ao apagar definitivamente uma composição, a derivação é colocada em quarentena `.deleting`. Se o projeto permanecer após uma interrupção, o construtor do store recupera a derivação; se o projeto já tiver sido apagado, o resto é descartado.
- Nenhum serviço, servidor, telemetria, síntese de áudio, dependência paga ou acesso a dados reais é introduzido.

## Backup v4 — contrato

- `BackupArchive.VERSION=4`; preserva leitura de **v1, v2 e v3**. Exportação manual acrescenta `creationScores`, cada elemento com UUID, conteúdo base64 e SHA-256, além do array `creations` de C1. Uma derivação inválida, órfã, com proprietário não existente ou acima de orçamento **impede o backup**, sem silenciosamente omitir dados.
- Na restauração, decodifica e verifica todos os arquivos **antes de mutar os dados**. Os `.pls` são validados em diretório de staging contra a composição correspondente no backup ou a composição local com o mesmo UUID. Se projeto/derivação local divergir, o processo aborta antes de substituir algo. Derivações locais inexistentes no backup são preservadas. Não é feito merge destrutivo por título.
- Jornal de restauração `restore-pending.json` agora inclui `addedScores`; em falha ou término de processo durante a transação, `recover` remove apenas partituras adicionadas pela operação interrompida, assim como já fazia com composições, arquivos importados e preferências. Não elimina partituras preexistentes.
- Backups v1–v3 não trazem `creationScores`; a restauração **mantém** qualquer derivação local válida. Backups v4 recuperam tanto o projeto quanto a derivação vinculada.
- `BackupArchiveSelfTest` foi atualizado com casos para criar/restaurar v4, recuperar sidecar removido, preservar versões locais divergentes, checksum alterado e v3 legado; **autoteste isolado executado no emulador com aprovação: 51 assertivas no run #41369082904**.

## Testes dirigidos e CI

Somente workflows básicos na branch:
- [Run #41368010376](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41368010376): **success**; `CREATION_C2_A_BASIC_PASS checks=47`, `CREATION_C2_B_BASIC_PASS checks=27`, `CREATION_C2_C_BASIC_PASS checks=22`, `CREATION_C2_B_NOTEHEAD_BASIC_PASS checks=14`, `assembleDebug BUILD SUCCESSFUL`. Total **110 asserts básicas**. Rodou antes dos últimos ajustes documentais e do smoke direcionado; o SHA final deve ser novamente verificado.
- [Workflows C2 da branch](https://github.com/atshimura/Piano-livre-aberto/actions/workflows/c2-basic.yml): compilação `javac` dos fontes reais, quatro testes dirigidos, `node CreationNotePickerTest.js` e `gradle :app:assembleDebug`. Um job adicional **`backup-smoke`** executa **exclusivamente** o autoteste `backup_selftest` de `BackupArchiveSelfTest` em emulador Android debug, sem a suíte de interface ou de regressão completa. O resultado deve ser registrado na issue após confirmação.
- **Não rodar** `emulator-suite` nem a suíte completa de `build.yml`; respeitar a regra explícita do usuário. Os testes físicos Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 não foram executados e continuam pendentes.

- [**Run final de C2-C #41369082904**](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41369082904), SHA `94636babb91ece910b2180d3d5ae27b773ed7285`: jobs c2-basic e backup-smoke **success**; C2-A=47, C2-B=27, C2-C=22, JavaScript=14, assembleDebug aprovado, `BACKUP_SELFTEST_PASS assertions=51`. Nenhuma suíte completa executada.

## Limitações e próximos passos

- A verificação de compatibilidade do MusicXML e o renderizador OSMD foram compilados no APK, mas geometria/touch em aparelhos reais exigem testes físicos posteriores.
- O MIDI canônico segue preservado. A interpretação rítmica é derivada/quantizada; para playback fiel da execução, usar **Reproduzir no piano** da C1, e não confundir com reprodução da partitura escrita.
- C2-C **não incorpora** exportação MusicXML/PDF (C3) nem editor livre de alturas/notas (C4).
- A `main` protegida ainda exige gates que disparam a suíte completa quando um PR Java a tem como base. Portanto, **a integração à `main` permanece bloqueada pela incompatibilidade processual**, enquanto a regra de apenas testes básicos estiver vigente. Branch+PR técnico sobre C2-A/B não modifica os gates existentes.
- Bloco **C2-D**: ver [relatório 105](105-CRIACAO-C2-D-VALIDACAO-ENTREGA.md). Testes visuais dirigidos não substituem a CI exigida pela main. Release específica não aprovada.

# 114 — Biblioteca Musical: exclusão de partituras com preservação de dados

**Data:** 03/11/2026. **Estado:** implementação em `feat/library-safe-remove-20261103`, sobre `main` 0.9.62-open-beta / versionCode 71, **não publicada**. Pedido explícito: testes básicos apenas, nenhuma suíte completa, nenhum APK.

## Interface

Manter pressionado um cartão da Biblioteca Musical abre `Organizar em coleção` e `Excluir da biblioteca`. A exclusão solicita confirmação e adapta a mensagem segundo a origem:

1. **Partitura que vem com o aplicativo (`scores/...`)**: ocultação por perfil, sem apagar assets; favoritas e coleções são preservadas. `Coleções → Restaurar músicas ocultas` mostra todas as ocultas e permite restaurá-las individualmente.
2. **Partitura criada (`creation-score:<UUID>`)**: remove apenas o vínculo da Biblioteca do perfil atual e referências de favoritos/coleções à partitura; preserva projeto C1, gravação, XML derivado e histórico. A composição continua acessível em Minhas Criações e pode ser adicionada novamente.
3. **MusicXML/MXL importado (`imported/<arquivo>`)**: exclui o arquivo da área privada de importados e, quando existir, seu sidecar `<arquivo>.source.mid`. Como os importados são compartilhados, remove referências de favoritos, coleções e última partitura aberta **de todos os perfis** numa única transação de preferências. Histórico de estudo permanece, sem alterar os registros anteriores. Antes da exclusão definitiva, o diálogo recomenda fazer backup.

**Observação:** `Excluir coleção` continua excluindo somente a coleção, sem apagar partituras; o novo comando é separado.

## Integridade e recuperação

- `LibraryStore` mantém o JSON `version:1`, introduz campo opcional `hiddenScores` (padrão lista vazia) e valida que só contém chaves `scores/`. Backups antigos v1–v4 e bibliotecas legadas sem campo novo continuam aceitos; não há migração destrutiva.
- `ImportedScoreDeletion` aceita apenas nomes MusicXML internos válidos e rejeita travessia de diretórios e caminhos simbólicos. Antes de mexer nas preferências, move o score e o MIDI correspondente para `files/imported.delete-pending`; escreve um manifesto durável; atualiza referências; grava marcador `committed`; então descarta o staging.
- Se falhar antes de `committed`, tenta restaurar os arquivos. Na inicialização e antes de gerar backup, `recover` restaura um estágio **não confirmado** ou termina a exclusão **confirmada**. Conflitos e estados inesperados falham fechados para evitar perda silenciosa.
- Sessões em memória referentes à partitura removida são limpas. O histórico acumulado não é excluído; entradas antigas podem indicar uma partitura que não está mais disponível, sem reimportação.
- Uma exclusão importada confirmada não é reversível pelo botão Restaurar músicas ocultas: para recuperar o arquivo, reimportar a cópia pessoal ou restaurar backup anterior. O catálogo de partituras do APK nunca é alterado.

## Testes direcionados

- `tests/ImportedScoreDeletionTest.java`: JVM, 10 verificações de exclusão de par XML+MIDI, isolamento de outros arquivos, callback único, erro de gravação e rollback, recuperação antes/depois do commit, rejeição de chaves inválidas e XML sem sidecar.
- `LibrarySelfTest.java`: verificações adicionais da ocultação por perfil, favoritos preservados, restauração, desvinculação de criação, remoção compartilhada com MIDI e limpeza de coleções, favoritos e sessão de dois perfis.
- `tests/test_library_removal_contract.py`: contratos da UI, fluxo dos três tipos, retrocompatibilidade, segurança e ausência de exclusão do histórico.
- `.github/workflows/library-basic.yml`: somente testes Python, JVM e `gradle :app:compileDebugJavaWithJavac`, com asserção explícita de **nenhum APK**; sem `assemble`, sem emulador, sem assinatura e sem suíte completa.

**Limitações até validação posterior:** a compilação Java não substitui uso em Android real. Os testes de UI com toque prolongado e diálogos em telefone/tablet, backup/restauração pós-exclusão em hardware e atualização do APK ainda precisam ocorrer antes de publicação. Main e Release 0.9.62 seguem inalteradas. A integração posterior necessita das autorizações e verificações cabíveis da branch protegida.

## Estado dos checks

Registrar no PR os resultados do **SHA final** do workflow básico. Não descrever sucessos de builds antigos como validação deste patch; o workflow completo `build.yml`/`emulator.yml` não deve ser acionado por este PR técnico.

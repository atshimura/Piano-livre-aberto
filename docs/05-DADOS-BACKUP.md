> **Arquivo de referência técnica transferido para a edição aberta.** Parte das afirmações sobre versão, CI, aplicativo privado ou certificados descreve apenas o histórico de desenvolvimento. Para a situação vigente no repositório público, consulte [AGENTS.md](../AGENTS.md), [build e testes](06-BUILD-TESTES.md) e [estado](07-ESTADO-E-ROADMAP.md). Links para execuções privadas podem estar inacessíveis a terceiros.

# 05 — Dados, perfis, importação e backup

**Estado técnico verificado na branch da beta privada 0.9.11, 15/10/2026.** Base: `MainActivity.java`, `BackupArchive.java`, `BackupArchiveSelfTest.java`, [doc 16](16-BACKUP-MIDI-CONSISTENCIA.md) e [teste de atualização](25-CONTINUIDADE-0.9.9-0.9.11.md). Este documento distingue funcionalidades implementadas de requisitos futuros.

## 1. Princípios e armazenamento atual

Dados ficam **localmente no dispositivo**, sem contas, servidor ou sincronização automática. Backup e restauração são **manuais**. `AndroidManifest.xml` declara `android:allowBackup="false"`: não depender de backup automático do Android.

`MainActivity` mantém `SharedPreferences("piano_data", MODE_PRIVATE)`, incluindo `profiles` como array JSON, `active_profile`, e chaves por perfil `user_<índice>_...`. A sessão grava, para cada perfil, **file, position, hand, mode, tempo, loop_start, loop_end, keyboard e route_visit**. Posição, velocidade e extremos de loop são números double serializados como bits long. Há **até 20 perfis simples** e importados em `getFilesDir()/imported/`. Os arquivos de música originais importados permanecem no armazenamento privado.

A **persistência dessas chaves não significa histórico completo de aprendizagem**: não estão implementados o registro de metas, avaliações, favoritos, correções musicais editáveis nem histórico analítico abrangente. O estado recuperado em cenários de troca de perfil, loop, navegação ou encerramento inesperado ainda precisa de testes físicos exploratórios.

## 2. Importação existente

A interface usa o seletor de arquivos do Android para MusicXML `.xml`, `.musicxml` e `.mxl`, copiando bytes para a área privada e interpretando com `Score.read`. Não há importação de **MIDI .mid como partitura**. A implementação não é uma validação editorial exaustiva de qualquer MusicXML/MXL; manter testes de arquivos adversariais, tamanho excessivo, ZIP, entidades XML, caminhos inválidos e arquivos duplicados.

## 3. Backup e restauração atuais — schema v2

**Exportar:** Biblioteca → Opções → Fazer backup abre `ACTION_CREATE_DOCUMENT`, nome sugerido `pianolivre-backup.json`. `MainActivity.makeBackup()` primeiro executa `saveSession()` e chama `BackupArchive.create`. O JSON **version 2** contém:

- `prefs`: todas as preferências presentes, com tipo explícito (`boolean`, `int`, `long`, `float`, `string`, `strings`), inclusive os bits long da posição;
- `imported`: arquivos privados com `name`, conteúdo codificado em Base64 e **SHA-256** individual.

**Restaurar:** Biblioteca → Opções → Restaurar backup abre um JSON via seletor. Antes de escrever dados, `BackupArchive.validate` verifica formato, tipos, perfis, limites, nomes, duplicidade, integridade de arquivos v2 e todos os dados importados; backups **legados version 1** também são aceitos por conversão de preferências. A tela exibe **resumo dos dados e confirmação explícita** de substituição; não faz mesclagem. A escrita é **transacional com diretórios de estágio e rollback**, diário persistente (`restore-pending.json`) e recuperação após interrupções no início do aplicativo. A rotina anterior insegura de substituição direta da 0.9.0 é apenas histórica; **não** afirmar que a beta atual não possui checksum/rollback/confirmação.

Limites codificados: até **200 arquivos**, **16 MiB por arquivo**, **64 MiB totais de importados** e **95 MiB para o JSON**. O formato não é criptografado. Dados de funcionalidades futuras inexistentes não aparecerão magicamente no arquivo. A recuperação e validação diminuem o risco, mas não demonstram confiabilidade absoluta em todos os dispositivos nem substituem uma cópia independente.

## 4. Migração da 0.9.9 para a 0.9.11

O CI baixa o **APK real 0.9.9/code18**, instala no emulador, injeta perfil de teste em `SharedPreferences` e arquivo MXL importado, instala por cima a **0.9.11/code20** com `adb install -r`, compara os dados byte a byte e verifica perfil, UI e `BACKUP_SELFTEST_PASS`. Evidências: [smoke CI 38730155782](https://github.com/atshimura/piano-livre/actions/runs/38730155782) e [linhagem de APK 38730155811](https://github.com/atshimura/piano-livre/actions/runs/38730155811). Essa evidência **não cobre todos os estados e volumes de dados reais**.

**Procedimento para pessoa testando:** exporte backup e preserve outra cópia; instale 0.9.11 **sobre** a 0.9.9, sem desinstalar. Se assinatura não conferir, **não** apague o app para forçar atualização. Se precisar restaurar um backup, saiba que a operação substitui integralmente os dados existentes.

## 5. Limites e trabalho futuro

Pendências do produto: biblioteca com favoritos/coleções, nomes/avatares e operações completas em perfis, metas e avaliações, histórico detalhado, correções do MusicXML em camada separada, importação MIDI, restauração e interrupções sob cenários extremos, auditoria de segurança de entradas não confiáveis e ensaios em Realme 7 5G, Lenovo Tab M9 e Yamaha P-45.

Não criar contas online, telemetria ou sincronização automática sem autorização. Não incluir dados pessoais em logs, testes, issues ou artefatos. Ver [01](01-VISAO-E-REQUISITOS.md), [07](07-ESTADO-E-ROADMAP.md), [09](09-SEGURANCA-LICENCAS.md), [16](16-BACKUP-MIDI-CONSISTENCIA.md) e [25](25-CONTINUIDADE-0.9.9-0.9.11.md).

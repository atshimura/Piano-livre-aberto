# Nomes de importações antigas — 0.9.34 / code 43

O importador anterior podia gerar nomes ASCII iniciados por sublinhado/hífen/ponto e manter pontos consecutivos. O backup e a retomada mais recentes exigiam primeiro caractere alfanumérico, limite de 120 caracteres e ausência de dois pontos; isso recusava nomes válidos existentes, como _etude..mxl, embora o arquivo não fosse apagado.

Validação passa a aceitar componentes ASCII simples de até 255 caracteres, incluindo esses nomes legados. Nomes exatamente . e .., barras, contrabarras e demais caracteres fora do conjunto continuam rejeitados. Dois pontos dentro de um nome simples não representam diretório pai. Toda restauração continua confinada à pasta imported local. Não há renomeação nem alteração de fonte. Metadados da biblioteca/histórico aceitam a chave portátil correspondente de até 264 caracteres.

A exportação também prepara o arquivo completo antes de abrir o destino externo: falha na preparação deixa de poder truncar um backup existente. Falha durante a escrita do provedor ainda pode deixar arquivo incompleto; use destino novo e preserve outra cópia.

Importações novas também respeitam os mesmos limites do backup (200 arquivos/64 MiB); MIDI conta o par original + notação antes de escrever qualquer arquivo. Atingir o limite rejeita somente a nova importação, sem apagar arquivos. Isso evita aceitar mais dados do que o backup consegue preservar.

SessionCompatibilityTest passou 20 verificações locais. BackupArchiveSelfTest acrescenta backup/restauração byte a byte de quatro nomes legados (incluindo um de 244 caracteres), retomada sem renomear e favorito longo. ScoreImportTest e MidiFileImportTest cobrem limites e preservação do acervo ao rejeitar adições. Testes também exigem que erro de preparação não abra o destino e que exportação válida escreva/feche o arquivo. Exigir BACKUP_SELFTEST_PASS ampliado no Android e regressões/CI da própria entrega antes de integrar. Nenhum teste físico declarado.

Estado confirmado: PR #29 integrado em 5bc251e4fa9ea53c5c325a25a5d6cd9617ecc36a, atualização no emulador aprovada; pré-release v0.9.31-open-beta existe com APK, certificado, metadados e SHA256SUMS. PR #30 integrado em edc504c6829e6230174276bcfad7e4e034c7032d após build 38986443966/emulador 38986444139. PR #31 passou build 38986656222/emulador 38986656236; integrar antes desta entrega.

Próxima ação: confirmar CI desta compatibilidade, integrar sob proteção, verificar assinatura e pré-release do merge. Consolidar issue #11/#4 com resultados e próxima tarefa física (Realme/Lenovo/Yamaha), sem declarar 1.0. Continuar qualquer correção independente constatada; nunca confundir emulador com aparelhos reais.

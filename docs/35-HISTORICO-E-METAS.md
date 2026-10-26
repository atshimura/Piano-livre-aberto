# Histórico e metas locais — 0.9.26 / code 35

Tempo de partitura aberta em primeiro plano por perfil estável, incluindo leitura e pausas. Biblioteca e segundo plano não contam. Mudar perfil, modo, mão, partitura ou tela fecha o período; o relógio é monotônico, sem depender de ajustes de data. Não é avaliação de notas, ritmo ou esforço. Metas de minutos são escolhidas e removidas pelo usuário, sem penalidade.

Registros incluem partitura portátil, data, modo, mão, posições e velocidades inicial/final. Escrita somente nas transições, fora do ticker musical. JSON local limitado a 128 KiB/perfil, sem descarte automático; ao atingir limite, registros antigos permanecem e o usuário pode fazer backup e limpar explicitamente. Encerramento abrupto do processo pode perder o período ainda aberto. Não há sincronização nem telemetria.

Backup v2 inclui histórico e metas e pré-valida conteúdo antes de restaurar. Exclusão confirmada remove apenas histórico e metas daquele perfil, preservando partituras e favoritos. Tests: StudyClockTest (JVM), HistorySelfTest (persistência/isolamento/backup inválido), Activity (estudo → leitura livre → biblioteca). CI Android e emulador devem ser confirmadas no PR antes de integração; nenhum teste físico declarado.

Próxima ação: confirmar os checks do PR desta versão, corrigir eventuais falhas e integrar sob proteção. Depois concluir auditoria da Etapa 4 e iniciar Etapa 5: importação sem sobrescrita, limites de leitura/MXL e preservação de dados; importação MIDI ainda pendente. Aceite físico segue na issue #11.

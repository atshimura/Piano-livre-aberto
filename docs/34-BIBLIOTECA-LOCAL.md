# Etapa 4 — Busca, favoritos e coleções (0.9.25/code34)

Busca local por título/nome de arquivo ignora maiúsculas/acentos e aceita vários termos. Filtros Todas/Favoritos/Coleção exibem contagem de resultados; não alteram arquivos nem o catálogo. O atalho da Ode continua disponível.

Cada ID estável de perfil possui favoritos/coleções próprios em user_ID_library (JSON version 1). Os arquivos continuam compartilhados. Estrela no cartão alterna favorito. Botão Coleções cria/renomeia/exclui organização; manter cartão pressionado adiciona/remove de uma coleção. Excluir coleção exige confirmação e não exclui partituras.

Identidade da música é portátil: scores/arquivo ou imported/arquivo, sem depender do caminho privado /data/user/0 versus /data/data. Metadados recebem limites técnicos (256 favoritos/membros por coleção, 40 coleções e 256 KiB por perfil), nomes/IDs únicos e validação antes de gravar. Snapshots são imutáveis. Nenhum servidor, login, pagamento ou telemetria.

Backup v2 inclui esses dados como preferências tipadas; valida o schema antes de restaurar. Versões futuras desconhecidas e caminhos inconsistentes são recusados sem apagar dados. Excluir perfil remove seu prefixo exato, incluindo a organização, mantendo a biblioteca compartilhada.

## Testes

LibrarySearchTest passou localmente: acentos, caixa, múltiplos termos, nomes de arquivo, consulta vazia e identidade portátil. LibrarySelfTest Android usa preferências/cache/arquivo sintético isolados: perfis separados, snapshots, nomes duplicados sem efeitos, membros/renomeação/exclusão, favoritos, backup/restauração com bytes do arquivo e rejeição antes da escrita. Emulador exige LIBRARY_SELFTEST_PASS e verifica busca nativa/limpeza do campo. Compilação e Android precisam da CI do próprio commit.

## Continuidade

Próxima tarefa: histórico/progresso de estudos por perfil, sem nota global ou avaliação inventada, compatível com sessões e backup. A importação ainda exige auditoria de colisão de nomes, tamanho/XML/MXL e suporte MIDI na Etapa 5. Favoritos de arquivo ausente permanecem como metadados, sem recriar/apagar arquivo automaticamente.

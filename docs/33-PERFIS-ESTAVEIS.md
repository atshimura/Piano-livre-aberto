# Etapa 4 — Perfis com identidade estável (0.9.24/code33)

## Compatibilidade e dados

Os índices dos nomes antigos tornam-se IDs permanentes (0,1,...), conservando todas as chaves user_ID de sessão, posição, calibração e preferências. Renomear/avatar não muda ID. Excluir remove somente o prefixo exato do perfil escolhido, não renumera os demais e conserva partituras compartilhadas. O último perfil não pode ser excluído; a interface exige confirmação da exclusão.

profiles_v2 contém objetos id/name/avatar; profiles_next_id mantém sequência sem reutilização. Namespaces user_ID órfãos também ficam reservados, impedindo que um perfil novo herde dados antigos. profiles permanece como lista de nomes para compatibilidade estrutural do arquivo de backup; identidade canônica nova está em profiles_v2. A seleção active_profile passa a guardar ID estável, que coincide com o índice antigo durante a migração.

Leitura antiga não reescreve automaticamente metadados; a primeira edição/criação/exclusão valida os registros antes de gravar. Mutação usa uma transação de preferências e tenta restaurar snapshot anterior se o commit falhar. Não se afirma garantia contra falha física de armazenamento. Ao trocar para perfil sem sessão, modo/mãos/velocidade/teclado/trecho recebem padrões próprios, sem herdar o perfil anterior.

## Backup

O envelope continua v2 e inclui preferências tipadas. Aceita backups antigos sem profiles_v2 e valida IDs únicos, avatar, perfil ativo e sequência quando os novos campos estão presentes, antes de restaurar. Backups novos com IDs não contíguos requerem esta versão ou posterior para interpretar as identidades; downgrade não é uma migração suportada. Não desinstalar versões nem remover dados para contornar assinatura.

## Interface

Botão + cria e seleciona perfil. Biblioteca → Opções → Gerenciar perfil oferece renomear, avatar e excluir com confirmação. As ações alteram somente o perfil local; não criam conta/login/senha.

## Testes e limites

ProfilesSelfTest usa preferências e diretório cache isolados: IDs antigos, leitura sem regravação, nome/avatar, posição/calibração preservadas, mutação inválida sem efeitos, exclusão com prefixo exato, ID não reutilizado, reserva de órfãos, backup/restore e rejeição de duplicação antes da escrita. O último perfil fica protegido. O emulador exige PROFILES_SELFTEST_PASS e também testa criação/avatar/exclusão na UI real.

Auditoria pública/sintaxe local aprovadas; compilação e testes Android dependem da CI do próprio commit. Nenhum dado real de usuário foi usado nos testes. Próxima tarefa da Etapa 4: busca/favoritos/coleções na biblioteca e histórico/progresso por ID, compatíveis com backup e sem pontuação arbitrária.

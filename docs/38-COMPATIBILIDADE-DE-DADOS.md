# Compatibilidade e pré-validação — 0.9.29 / code 38

Sessões passam a salvar scores/nome ou imported/nome, sem diretório específico do pacote. Caminhos absolutos antigos com segmento /imported/ são remapeados para a biblioteca local atual, sem ler o pacote antigo. Apenas nomes simples são aceitos; travessia ou arquivos externos arbitrários são rejeitados. Fontes reais do repositório privado não são acessadas.

Backup valida tipos conhecidos de modo/mão/teclado/posição/velocidade/loop antes de substituir dados; bits IEEE-754 long são preservados inclusive no legado v1. NaN e infinito são rejeitados. Arquivos originais e preferências permanecem na rejeição. Recuperação do journal preserva os valores anteriores, sem aplicar a migração de caminhos ao rollback.

JSON recebe limite lexical de profundidade antes do parser recursivo: 64 níveis para arquivo de backup/journal, 16 para perfis/biblioteca/histórico. Limites de tamanho existentes são preservados. SessionCompatibilityTest passou 16 verificações; BackupArchiveSelfTest ampliado para tipos inválidos, NaN e caminho de outro diretório de pacote. CI Android e regressões ainda precisam ser confirmadas no commit desta entrega.

Emulador recebe teste do seletor MIDI com fixture sintética: escolhe mão esquerda, verifica staff 2 no XML e presença do SMF original. Remove somente o par sintético conhecido na instalação nova do emulador. Histórico nativo também é exercitado. Nada disso constitui teste físico.

Estado confirmado: PR #24 integrado em 9a2fd7703dbc09b64596324a2611b9a6513e65d9; build 38984954246/emulador 38984954232 aprovados. Etapa 4 encerrada no escopo técnico na issue #9. PRs #25/#26 aguardam CI dos últimos commits e integração sequencial. Próximo trabalho: confirmar esta CI, resolver falhas, ampliar entradas MusicXML não suportadas explicitamente e consolidar matriz de aceite/Etapa 6. Hardware continua pendente em #11; V1 não concluída.

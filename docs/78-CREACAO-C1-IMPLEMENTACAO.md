# 78 — Criação C1: implementação e validação

**Data:** 31/10/2026. **Base:** `main` em `cf4b24b` (0.9.54-open-beta / code63). **Branch:** `feat/criacao-c1`. **Candidato:** 0.9.55-open-beta / code64. Não é Release nem autorização de publicação. O escopo normativo permanece no [documento 76](76-CRIACAO-C1-ESCOPO-E-CONTINUIDADE.md).

## Estado por subetapa

| Etapa | Implementação nesta branch | Validação/gates |
|---|---|---|
| C1.1 — Interface e navegação | Opção Criação no Laboratório; Aprender permanece visível e desabilitado; início, gravação, biblioteca, detalhes, Lixeira e recuperáveis; retorno da lista preserva busca/filtro/ordenação; indicador de entrada e saída separado; sem cartão na Home. | Suíte Android de interface/emulador ainda precisa executar nesta branch. Celular/tablet, paisagem e leitor de tela precisam ser conferidos pelos jobs automatizados disponíveis. |
| C1.2 — Gravação | Tomada única, relógio monotônico, eventos por canal/época, velocity-zero Note On, CC64, rearticulação, simultaneidade, pausa/continuação, fechamento seguro e bloqueio de mensagens antigas; journal incremental fora da tela e sem gravação oculta em segundo plano. | Testes JVM novos adicionados; precisam passar no CI. Há validação física pendente. |
| C1.3 — Projetos | Arquivos locais UUID, CRC, escrita temporária/atômica, limites explícitos, busca sem acentos, filtros, ordenação, renomeação, Lixeira, restauração e recuperação do prefixo íntegro do journal. Projetos continuam vinculados ao ID do perfil. | Testes JVM novos adicionados; reinício, journal truncado/corrompido e limites aguardam CI/emulador. Exclusão de perfil que ainda possui projetos é bloqueada. |
| C1.4 — Reprodução/exportação | Player agenda eventos na saída MIDI funcional, pausa/cancelamento liberam notas e pedal; reconexão não retoma; opção de trim não altera dados. SAF grava SMF formato 0 com resolução 960 PPQN e mapa explícito de 120 BPM. | Teste de roundtrip pelo importador existente e teste do scheduler adicionados; aguardam CI. Nenhum sintetizador/áudio local. |
| C1.5 — Backup/compatibilidade | Backup v3 inclui projetos e metadados com SHA-256; restore de v1/v2 continua legível, faz merge sem apagar projetos locais mais novos e usa staging/journal para remover apenas adições incompletas em rollback. | Autoteste isolado atualizado com merge, compatibilidade e rollback; precisa passar na suíte Android completa em emulador no SHA do PR. |

## Limites do formato e segurança

- Gravação: máximo 200.000 eventos e quatro horas ativas por tomada.
- Projeto: máximo 16 MiB; biblioteca: máximo 200 projetos. O backup v3 limita o conjunto serializado de composições a 6 MiB para caber no envelope atual do backup.
- O SMF exportado usa formato 0, PPQN 960 e meta-evento de tempo 500.000 µs por semínima (120 BPM). Os tempos monotônicos originais e segmentos continuam no projeto local; o exportador converte para ticks sem quantizar a fonte.
- Backup não abre o destino externo se validação local ou de integridade falhar. Colisão de ID com conteúdo divergente bloqueia a restauração antes da mutação. Backup antigo sem um perfil proprietário de projeto local também é bloqueado para impedir perda silenciosa.
- Os IDs de perfil seguem o armazenamento público existente (índices estáveis enquanto perfil existe); exclusão é impedida enquanto houver projeto salvo, recuperável ou na Lixeira para esse perfil.

## Verificação e pendências

`git diff --check` passou no estado local inicial. O ambiente de trabalho não dispõe de `javac`, Gradle ou Android SDK executável; portanto não há resultado local de compilação. A conclusão depende de executar e inspecionar os jobs completos de `build` e `emulator` no commit do PR, corrigir quaisquer falhas e repetir ambos no SHA final. Não integrar antes dos gates obrigatórios. Não publicar APK: isso depende de integração aprovada, checks completos e autorização específica de release.

Testes físicos Yamaha P-45 + Realme 7 5G e Lenovo Tab M9 ainda não foram realizados. A Criação C1 não declara 1.0 nem inicia recursos da C2/C3.

## Histórico desta implementação

- Base conferida: branch parte do commit consolidado `cf4b24b` da main, 0.9.54/code63.
- Branch `feat/criacao-c1` criada para a implementação ordenada C1.1–C1.5.
- Código, testes, PR, SHA final, URLs de Actions e decisão de integração devem ser acrescentados aqui após o CI completar; não preencher como aprovado antes da evidência.

# 78 — Criação C1: implementação e validação

**Data:** 31/10/2026. **Base:** `main` em `cf4b24b` (0.9.54-open-beta / code63). **Branch:** `feat/criacao-c1`. **Candidato:** 0.9.55-open-beta / code64. Não é Release nem autorização de publicação. O escopo normativo permanece no [documento 76](76-CRIACAO-C1-ESCOPO-E-CONTINUIDADE.md).

## Estado por subetapa

| Etapa | Implementação nesta branch | Validação/gates |
|---|---|---|
| C1.1 — Interface e navegação | Opção Criação no Laboratório; Aprender permanece visível e desabilitado; início, gravação, biblioteca, detalhes, Lixeira e recuperáveis; retorno da lista preserva busca/filtro/ordenação; indicador de entrada e saída separado; sem cartão na Home. | `ui-fast` e a suíte integral `emulator-suite` passaram no SHA final `a86ea4d6d57e8cfb3dc834d03ee989ef742a022d`; o smoke automatizado percorreu telefone/tablet em paisagem. Acessibilidade por leitor de tela não foi validada especificamente. |
| C1.2 — Gravação | Tomada única, relógio monotônico, eventos por canal/época, velocity-zero Note On, CC64, rearticulação, simultaneidade, pausa/continuação, fechamento seguro e bloqueio de mensagens antigas; journal incremental fora da tela e sem gravação oculta em segundo plano. | Testes JVM passaram no job `build` #752; há validação física pendente. |
| C1.3 — Projetos | Arquivos locais UUID, CRC, escrita temporária/atômica, limites explícitos, busca sem acentos, filtros, ordenação, renomeação, Lixeira, restauração e recuperação do prefixo íntegro do journal. Projetos continuam vinculados ao ID do perfil. | Testes JVM e autoteste Android passaram; o backup recebeu 44 assertions, incluindo merge, integridade, v1/v2, colisão, perfil ausente e rollback. Exclusão de perfil que ainda possui projetos é bloqueada. |
| C1.4 — Reprodução/exportação | Player agenda eventos na saída MIDI funcional, pausa/cancelamento liberam notas e pedal; reconexão não retoma; opção de trim não altera dados. SAF grava SMF formato 0 com resolução 960 PPQN e mapa explícito de 120 BPM. | Roundtrip SMF pelo importador existente e scheduler passaram no job `build` #752. Nenhum sintetizador/áudio local. |
| C1.5 — Backup/compatibilidade | Backup v3 inclui projetos e metadados com SHA-256; restore de v1/v2 continua legível, faz merge sem apagar projetos locais mais novos e usa staging/journal para remover apenas adições incompletas em rollback. | Autoteste isolado passou no emulador completo #729 (`BACKUP_SELFTEST_PASS assertions=44`), além da verificação de upgrade da beta anterior. |

## Limites do formato e segurança

- Gravação: máximo 200.000 eventos e quatro horas ativas por tomada.
- Projeto: máximo 16 MiB; biblioteca: máximo 200 projetos. O backup v3 limita o conjunto serializado de composições a 6 MiB para caber no envelope atual do backup.
- O SMF exportado usa formato 0, PPQN 960 e meta-evento de tempo 500.000 µs por semínima (120 BPM). Os tempos monotônicos originais e segmentos continuam no projeto local; o exportador converte para ticks sem quantizar a fonte.
- Backup não abre o destino externo se validação local ou de integridade falhar. Colisão de ID com conteúdo divergente bloqueia a restauração antes da mutação. Backup antigo sem um perfil proprietário de projeto local também é bloqueado para impedir perda silenciosa.
- Os IDs de perfil seguem o armazenamento público existente (índices estáveis enquanto perfil existe); exclusão é impedida enquanto houver projeto salvo, recuperável ou na Lixeira para esse perfil.

## Verificação e pendências

Validação final no SHA `a86ea4d6d57e8cfb3dc834d03ee989ef742a022d`: `build` #752 passou; `emulator` #729 passou com `debug-apk`, `ui-fast` e `emulator-suite` verdes. O APK de validação e evidências estão nos artefatos do run [#729](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41177941440). APK de validação é de depuração e não é uma Release oficial. PR #82 está pronto para revisão em https://github.com/atshimura/Piano-livre-aberto/pull/82. Não houve merge nem publicação; o push na main aciona publicação beta e é necessária autorização explícita de release.

Testes físicos Yamaha P-45 + Realme 7 5G e Lenovo Tab M9 ainda não foram realizados. A Criação C1 não declara 1.0 nem inicia recursos da C2/C3.

## Histórico desta implementação

- Base conferida: branch parte do commit consolidado `cf4b24b` da main, 0.9.54/code63.
- Branch `feat/criacao-c1` criada para a implementação ordenada C1.1–C1.5.
- Código C1.1–C1.5 e testes adicionados à branch; PR #82 aberto para revisão. SHA validado: `a86ea4d6d57e8cfb3dc834d03ee989ef742a022d`. Build #752: https://github.com/atshimura/Piano-livre-aberto/actions/runs/41177942271 — sucesso. Emulador #729: https://github.com/atshimura/Piano-livre-aberto/actions/runs/41177941440 — sucesso. Artefato APK de validação: https://github.com/atshimura/Piano-livre-aberto/actions/runs/41177941440/artifacts/13034403866. Evidências de UI: https://github.com/atshimura/Piano-livre-aberto/actions/runs/41177941440/artifacts/13034094737. Sem merge/Release até autorização de publicação. Próxima etapa: C2, após integração e testes físicos; C1 não declara versão 1.0.

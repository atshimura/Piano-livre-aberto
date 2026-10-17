# 10 — Como continuar o Piano Livre aberto

**Repositório autoritativo para mudanças futuras:** `atshimura/Piano-livre-aberto`. Não fazer novos commits em `atshimura/piano-livre` como parte do fluxo aberto; aquele repositório histórico permanece privado.

## Ponto de retomada vigente

Etapas 1–5 encerradas no escopo técnico, sem aceite físico. Etapa 6 consolidou perfis/biblioteca/histórico, importação/backup e distribuição; consulte documentos 40–44. A versão está em gradle.properties e o registro final de commits/runs/Release nas issues #4/#11.

Primeira ação: verificar main, PR #33 e seus checks, certificado e pré-release da versão atual. Não refazer #29–#32 se já integrados. Quando a sequência estiver aprovada e sem falha independente documentada, a próxima ação é executar o roteiro físico Realme/Lenovo/Yamaha de 40 e registrar o resultado em #11. Não declarar 1.0 nem eliminação das travadas físicas sem esse aceite. Correções independentes constatadas continuam autorizadas; não encerrar automaticamente por concluir uma versão.

As seções por versão abaixo são histórico de execução: pendências nelas descritas podem ter sido resolvidas em versões posteriores. O estado verificado no GitHub e este ponto de retomada prevalecem.

1. Verifique a `main`, a versão em `PianoLivreAndroid/gradle.properties`, PRs abertos e os últimos runs do GitHub Actions.
2. Leia `AGENTS.md`, `docs/01`, `docs/07`, `docs/08` e o arquivo pertinente à alteração. Documentos marcados históricos não anulam o estado verificado no código atual.
3. Crie uma branch para cada correção; inclua testes compatíveis e atualize os documentos afetados. Use PR. Não realize rebase de histórico privado, nem importe `signing/`.
4. Rode `python3 PianoLivreAndroid/scripts/verify_public_snapshot.py` e as regressões adequadas. Valide CI do **próprio commit** antes de considerar a alteração aprovada.
5. Ao modificar o aplicativo distribuído, aumente versionCode e versionName efetivos, mantendo o appId público. Pacote público e privado são instalações independentes; backup e restauração exigem prova no dispositivo.
6. Cada partitura precisa de licença/justificativa documental e registro no manifesto `score-rights.json`; não presumir que uma transcrição própria afasta direitos da composição.
7. Se faltar teste no Yamaha P-45, Lenovo ou Realme, declare expressamente. Emulador não mede latência USB-MIDI.

**Assinatura:** segredos cadastrados pelo mantenedor, apenas em workflows de push confiável. Não divulgar JKS, senhas ou artefatos de assinaturas antigas. Ver [assinatura](ASSINATURA.md).

Relatório de cada intervenção: objetivo, HEAD e PR/commit, alterações, testes com links e conclusões, limitações, riscos e próxima etapa. O guia visual do teclado reproduz posições e duração **visualmente**; demonstração sonora é função de saída MIDI, não de sintetizador embutido.

## Retomada do desenvolvimento por etapas

Consultar [28 — Etapas até V1](28-ETAPAS-V1.md) e a issue #4 antes de escolher a próxima tarefa. Registrar o estado do PR e os Actions do próprio commit. Etapa 1 iniciada na 0.9.13; não marcar as etapas completas apenas por existir implementação parcial.

## Retomada 0.9.14

A integração e a assinatura da 0.9.13 foram confirmadas; não repetir o PR #5. A segunda entrega elimina callbacks visuais ociosos em pausa/fim e amplia TemporalRenderingTest. Consulte a seção final do documento 28 e a issue #6 para os Actions do commit efetivo. Próxima implementação: custo de updateInfo no ticker; depois carga longa e lifecycle. A Etapa 1 ainda não foi encerrada.

## Continuação 0.9.15

Implementado cache de apresentação StudyInfoText; ticker musical mantém frequência original, rótulo usa 100 ms durante movimento e atualizações imediatas nos comandos/término. Conferir PR e CI da entrega na issue #6. Próxima implementação: diagnóstico de carga longa/frames e lifecycle; depois reavaliar critérios de fechamento da Etapa 1.

## Continuação 0.9.16

Diagnóstico local e suspensão/retomada de ticker/WebViews implementados; procedimento no documento 29. Confirmar CI/PR e assinatura na issue #6. Depois concluir critérios técnicos automatizados da Etapa 1, mantendo aceite físico pendente, e iniciar Etapa 2 (#8): fixtures de diferenças de interpretação Java/JS. Não encerrar execução apenas por terminar uma versão; continuar enquanto houver trabalho independente e o ambiente permitir.

## Continuação 0.9.17

Fixtures e correções de decimais/vozes em desenvolvimento (documento 30). Confirmar PR #14 de lifecycle e CI da entrega musical antes de avançar. Depois: ligaduras incompletas e fixtures de percurso. Não confundir cobertura musical automatizada com certificação editorial ou Yamaha físico.

## Continuação 0.9.18

Tratamento explícito de ligadura incompleta: novo ataque com aviso, fonte preservada. Fixtures também consultam Score.at (contrato do Espera). Validar dependências/CI e depois auditar percurso e limitações MusicXML conforme documento 30. Persistir em execução entre entregas; não aguardar hardware quando houver tarefas independentes.

## Continuação 0.9.19

Etapa 1 encerrada apenas no escopo técnico (#6); aceite físico em #11. Etapa 2 amplia sound/offset com sete fixtures. Acompanhar PRs #15/#16 e a entrega sound em sequência; validar CI/assinatura do commit efetivo. Depois auditar expressão explícita e limites para avançar à Etapa 3. Não parar por concluir uma versão.

## Continuação 0.9.20

Dinâmica numérica explícita implementada; ver documento 30. Confirmar PRs/CI e assinatura de cada push. Em seguida reavaliar fechamento técnico da Etapa 2 e continuar Etapa 3 com guia visual de mãos/dinâmica/duração, preservando regras de aprendizagem. Validações físicas permanecem na issue #11.

## Continuação 0.9.21

Guia expressivo preparado com amarelo/verde preservados; documento 31. Confirmar PR #18 de dinâmica e CI da entrega de guia. Próxima tarefa da Etapa 3: modos/troca de mão/desconexão/notas presas, sem alterar regras pedagógicas. Depois avançar biblioteca/perfis/progresso. Não encerrar a execução somente por concluir essas entregas.

## Continuação 0.9.22

Etapas 1/2 encerradas no escopo técnico (#6/#8), sem aceite físico. Guia integrado no PR #19. Correção de sessão MIDI/foreground preparada; confirmar MIDI_SESSION_SELFTEST_PASS e CI do próprio commit antes de integrar. Próxima implementação: CC120/121/123 e notas presas (documento 32), depois biblioteca/perfis/progresso. Não interromper automaticamente após a versão.

## Continuação 0.9.23

Controles MIDI de canal implementados e testados em JVM. Confirmar PR de sessão MIDI e PR de CC120/121/123 com MIDI_SESSION_SELFTEST_PASS; conferir assinatura permanente e CI dos commits. Próxima implementação independente: biblioteca/perfis/progresso da Etapa 4, com compatibilidade dos IDs existentes e backup.

## Continuação 0.9.24

Perfis estáveis preparados; confirmar PROFILES_SELFTEST_PASS, UI e backup nos Actions do commit. Etapas 1–3 encerradas apenas tecnicamente; aceite físico em #11. Próxima implementação: busca/favoritos/coleções e histórico/progresso por perfil, preservando IDs e backup. Não encerrar porque uma versão ficou pronta.

## Continuação 0.9.25

Busca/favoritos/coleções preparados; documento 34. Confirmar CI dos perfis (#22) e da biblioteca, incluindo PROFILES_SELFTEST_PASS/LIBRARY_SELFTEST_PASS e UI. Próxima implementação: histórico/progresso por ID, depois Etapa 5 com importação não destrutiva/compatibilidade/MIDI. Continuar após cada entrega enquanto o ambiente permitir.

## Continuação 0.9.26

Histórico e metas por perfil implementados; detalhes e limites no documento 35. Confirmar StudyClockTest, HISTORY_SELFTEST_PASS e HISTORY_ACTIVITY_SELFTEST_PASS no commit do PR antes de integrar. Próxima ação independente: auditoria e correções de importação não destrutiva da Etapa 5; não encerrar ao concluir uma versão.

## Continuação 0.9.27

Importação não destrutiva e limites XML/MXL preparados; documento 36. Teste local ScoreImportTest passou. Confirmar CI e emulador do próprio commit, integrar após PR #24. Próxima tarefa: importação MIDI limitada com fonte preservada e escolha explícita de mãos; não encerrar por concluir esta versão.

## Continuação 0.9.28

Importação SMF 0/1 preparada com mãos explícitas por trilha, fonte original preservada e notação aproximada documentada em 37. Testes JVM locais passaram. Confirmar CI própria, MIDIFILE_SELFTEST_PASS e PRs #24/#25 antes de integrar em sequência. Próximas tarefas: seletor no emulador, remapeamento de caminhos restaurados e compatibilidade/adversariais de backup; ainda não concluir Etapa 5 nem V1.

## Continuação 0.9.29

PR #24 integrado após build/emulador aprovados; Etapa 4 encerrada tecnicamente na issue #9. PRs #25/#26 em validação. Caminhos portáteis e pré-validação de tipos/profundidade JSON preparados; documento 38. Teste JVM de compatibilidade passou; confirmar CI própria e UI do seletor MIDI/histórico. Próxima tarefa: integrar sequência aprovada, tratar MusicXML incompatível explicitamente e avançar consolidação da Etapa 6 sem declarar aceite físico.

## Continuação 0.9.30

Limites musicais passam a ser explícitos; -1.0 preservado como semitom integral. Preflight também protege container MXL. Documento 39. Regressões locais JVM/JS passaram; confirmar CI da sequência #25–#27 e desta entrega antes de integrar. Próxima tarefa: resolver emuladores pendentes e consolidar Etapa 5/Etapa 6; sem aceite físico em #11 e sem declarar V1 pronta.

## Continuação 0.9.31

PR #27 integrado em 36eedce5dc3be9d4b664312eb8f0600222844993 após build 38985550165/emulador 38985550170 aprovados. UI MIDI e histórico passaram; backup Android 28 verificações. Preparados ensaio de atualização desde a 0.9.25 e publicação durável da beta com checks do mesmo commit/assinatura permanente obrigatória. Documento 40. Confirmar CI própria, integrar sequência e verificar assinatura/pré-release; não declarar V1 sem aceite físico (#11).

## Continuação 0.9.32

Correção adicional de desempenho do guia: árvore de intervalos evita varrer notas encerradas quando há baixo muito longo. Documento 41. Teste de 65.537 eventos passou 618 verificações e limite de trabalho <140 nós/consulta; regressões do guia/expressão/timeline passaram. Confirmar CI desta entrega e PR #29, integrar sequência sob proteção e validar assinatura/pré-release; hardware segue em #11, sem V1 concluída.

## Continuação 0.9.33

Codificação XML corrigida e aplicada ao container antes da validação: UTF-8/UTF-16/Latin-1 sem substituição silenciosa; fonte original preservada. Documento 42, ScoreImportTest local passou; exigir XML_INPUT_SELFTEST_PASS Android e CI própria antes de integrar. Confirmar sequência #29/#30 e assinatura/pré-releases. Próxima ação concreta: resolver qualquer falha da atualização no emulador e concluir consolidação técnica, mantendo aceite físico pendente em #11.

## Continuação 0.9.34

Compatibilidade de nomes de importações antigas corrigida sem renomear arquivos. Documento 43; SessionCompatibilityTest local passou 20 verificações; exigir backup Android ampliado. Pré-release durável 0.9.31 confirmada, atualização offline no emulador aprovada; PR #30 integrado e #31 aprovado. Próxima ação: integrar sequência aprovada, verificar APK/assinatura/pré-release do merge e registrar em #11/#4; hardware permanece pendente e V1 não concluída.

A mesma entrega 0.9.34 prepara backup antes de abrir o destino externo, prevenindo truncamento em falha de preparação. Teste Android verifica abertura zero em falha e exportação válida completa; confirmar CI do HEAD atualizado de #32 antes de integrar.

## Continuação 0.9.35

PR #32 integrado em 175e9e9c0ce5e6acdbc6606318b9f44e73501cad com CI aprovada, backup Android 36 verificações. Preparada distribuição release não depurável, pacote aberto e mesmo certificado; emulador exige atualização debug→release preservando perfil/teclado/velocidade e ignorando flags de teste. Documento 44. Confirmar CI própria, integrar e verificar assinatura/pré-release do merge. Próxima ação após consolidação: aceite físico em #11/#4, sem declarar V1 por CI.

## Nova linha de produto: Laboratório Musical (17/10/2026)

A issue [#34](https://github.com/atshimura/Piano-livre-aberto/issues/34) concentra as melhorias aprovadas e o novo Laboratório; a especificação autoritativa, distinção implementado/pendente e próximos passos estão em [doc 45](45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md). Primeira entrega em branch `feat/laboratorio-musical-primeira-entrega` (versão proposta code45). Conferir PR/Actions do SHA exato antes de declarar integrada. Não declarar V1: aceite físico continua em #11.

## Entrega incremental: biblioteca compacta

Branch de trabalho: `feat/biblioteca-grade-compacta`; documento [46](46-BIBLIOTECA-GRADE-COMPACTA.md). A branch foi iniciada da primeira entrega do Laboratório (`feat/laboratorio-musical-primeira-entrega`) e só deve entrar na `main` quando a base anterior (#35) estiver integrada com todos os checks aprovados. Alterações propostas: layout, responsividade e densidade da grade; dados, backups, MIDI e arquivos importados não se alteram. Confirmar o SHA exato do novo PR e seus Actions antes do merge; seguir com #34 e #11. Versão proposta 0.9.37-open-beta/code46.

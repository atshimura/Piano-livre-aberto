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

## Entrega incremental: etiqueta de notas sobre a partitura

Branch `feat/partitura-nome-nota-ao-toque`; consultar [47](47-NOME-DA-NOTA-TOQUE.md). Branch criada a partir da segunda entrega, já integrada no PR #36; abrir PR para `main`, conferir no **mesmo SHA** todos os testes e o emulador antes de integrar. A funcionalidade é apenas do estudo temporal, lê a grafia real das notas e cifra explícita MusicXML quando disponível, não inventa acorde. Próximas tarefas seguem em #34 e #11. Versão proposta `0.9.38-open-beta` / code47.

## Entrega incremental: análise de sequências do Laboratório

Branch `feat/lab-analise-sequencias`, construída da entrega de tooltip (#37). Consulte [doc 48](48-LAB-SEQUENCIAS.md), a issue #34 e o documento 45. Implementa apenas análise **delimitada por Iniciar/Concluir** de padrões completos de uma oitava no Descobrir, por ataques USB-MIDI/virtual Tocar. Automaticidade espontânea, mão/dedos, áudio, progresso e práticas de sequências permanecem pendentes. Verificar CI e emulador do SHA do PR antes de merge; V1 só após #11. Versão proposta 0.9.39-open-beta/code48.

## Retomada 0.9.40 — cursor visual

A alteração inicial da branch `fix/espera-cursor-antes-notas` foi reaproveitada sobre a `main` no PR seguinte (verificar CI antes de integrar). O cursor fixo precede as notas por distância limitada, sem alterar o tempo musical. Documento 49. Próxima tarefa: preparação visual gradual do Espera comandada por Iniciar com duração configurável e sem penalidade; depois notação e funcionalidades do Laboratório da issue #34.

## Retomada 0.9.41 — Iniciar e preparação do Espera

Implementação proposta na branch `feat/espera-iniciar-preparacao` sobre o cursor do PR #39. Ver documento 50: cinco segundos iniciais configuráveis por perfil, bloqueio de reconhecimento apenas durante a entrada, animação desacoplada do tempo musical, cancelamento seguro. Exigir CI (JVM/JS/APK) e emulador no mesmo head antes de integrar. Seguir para claves/hastes/vigas na notação temporal, depois catálogo/demonstração/prática/histórico do Laboratório (#34). Hardware físico em #11.

## Retomada 0.9.42 — vigas MusicXML ausentes

Função `inferConventionalBeams` introduz agrupamento conservador por compasso/voz/pulso quando faltam marcas de vigas explícitas. Documento 51, testes JS. Confirmar CI/Android da branch `feat/notacao-vigas-inferidas` e dos PRs anteriores; depois aprimorar claves/hastes e seguir Laboratório (catálogo, análise espontânea, demonstração, prática, histórico) da issue #34.

## Retomada 0.9.43 — reconhecimento livre

`MusicPatternTracker` analisa sufixos monofônicos completos sem início/fim manual. Documento 52, PR empilhado `feat/laboratorio-reconhecimento-livre`. Validar Actions na HEAD correta após integrar PRs dependentes. Próximos passos: catálogo pesquisável de notas, acordes, escalas e arpejos, demonstração/ritmo offline e histórico por perfil. Hardware pendente #11.

### Revisão corretiva do PR #42 (0.9.43)

Conferir a revisão de `MusicPatternTracker` e `MusicLabScreen` na branch `feat/laboratorio-reconhecimento-livre`: a verificação de monofonia considera MIDI e teclado virtual em conjunto; status sem correspondência não deve preservar resultado anterior; reentrar no Laboratório ou alternar Tocar/Montar reinicia o rastreio. `MusicPatternTrackerTest` cobre esses casos. Exigir CI de build, MusicXML/JS/MIDI, compilação Android e emulador do **último SHA** após as correções; merge protegido e assinatura/release só após sucesso dos checks. Atualizar a issue #34 com evidências. Não integrar o PR #43 nesta tarefa: sua branch é empilhada e deverá ser rebaseada/retargeteada para `main` após o #42. Nenhum ensaio físico foi feito nesta revisão (#11).

## Retomada 0.9.44 — catálogo pesquisável (PR #43)

Entrega proposta na branch `feat/laboratorio-catalogo-consulta`, reconciliada por merge não destrutivo com a `main` 0.9.43. Mantém reconhecimento automático USB-MIDI/teclado virtual, prevenção de sobreposição e resultados obsoletos, botão manual Iniciar/Concluir e reinício Tocar/Montar/reencontro do Laboratório. `MusicLabCatalog` oferece consulta offline de 12 classes cromáticas, notas, 13 intervalos, acordes/inversões, escalas/modos, arpejos e os cinco exercícios antigos. Busca tolera acentos e sustenido `#`/`♯`; resultados vazios desabilitam prática e a alteração da busca reinicia exercício.

**Não confundir consulta com avaliação:** a prática apenas compara conjuntos de classes de altura, sem ordem, quantidade de notas, duração, ritmo, pedal, oitava, dedilhado ou mão. Grafia enarmônica não é inferida da tonalidade. Teste `MusicLabCatalogTest` ampliado e smoke UI do emulador cobrem busca e reset, além da regressão `MusicPatternTrackerTest`. Conferir ações no SHA final do PR e novamente no merge da `main`, certificado permanente e pré-release `0.9.44-open-beta` / code53. #44 demonstração MIDI, #45 prática sequencial e hardware físico #11 permanecem futuros; **não incluir antecipadamente**.

## Retomada 0.9.45 — demonstração musical offline (PR #44)

Branch `feat/laboratorio-demonstracao-midi` reconciliada com `main` 0.9.44 sem reescrever commits. Demonstração do catálogo pela saída USB-MIDI existente: nota/acorde/intervalo juntos, escalas/arpejos em sequência, velocidade 50–150% e repetição opcional, sem sintetizador Android. `MusicLabPlaybackPlan.Session` distingue reprodução ativa de notas sustentadas, invalida callbacks após parada (inclusive entre notas e repetições), troca de busca/exercício/modo, saída para Biblioteca/Estudo, segundo plano e desconexão. Libera notas ligadas **antes** do fechamento da porta USB. Tests JVM cobrem pausas entre notas, sustentações, repetição, mudança e callbacks antigos. A perda física da porta pode impedir Note Off final; não afirmar ausência de notas presas em piano real sem ensaio. Confirmar checks do PR/merge, release assinada `0.9.45-open-beta` / code54, e atualizar issue #34. PR #45 permanece intocado para próxima tarefa; Yamaha P-45, Realme 7 5G e Lenovo Tab M9 aguardam #11.

## Retomada 0.9.46 — prática sequencial (PR #45)

Branch `feat/laboratorio-pratica-sequencial` reconciliada por merge não destrutivo com a `main` 0.9.45. Preservados reconhecimento espontâneo/manual, catálogo, USB-MIDI/virtual e demonstração MIDI/cancelamento do PR #44. Escalas e arpejos avançam por ataque **novo, isolado** na ordem da altura MIDI; extras isoladas são indicadas sem bloquear e demora não penaliza. Notas repetidas exigem novo ataque; ataques simultâneos/sobrepostos e mistura com notas já mantidas no MIDI/virtual não fabricam ordenação. Reprodução de demonstração nunca avança prática. Busca, troca de exercício/modo, saída/pausa/desconexão cancelam estado; mão Ambas/Direita/Esquerda é referência, não inferência. Acordes/intervalos preservam a comparação por classes cromáticas. Sem migração de dados, serviços ou telemetria. Ver [55](55-LAB-PRATICA-SEQUENCIAL.md), teste JVM e emulador. Exigir CI no SHA da branch/merge e release assinada 0.9.46/code55. Faltam dedilhado, métricas rítmicas/metrônomo, calibração, histórico do Lab e testes físicos #11. Não declarar V1.

## Prioridade de produto — pauta profissional no Estudo (pesquisa PR #46)

**O usuário reprovou visualmente a pauta do Estudo**, apesar dos PRs de claves/hastes/vigas. Confirmado na `main` 0.9.46: estudo usa SVG manual `temporal-notation.js`, leitura livre usa OSMD 1.9.8. PR draft `research/estudo-gravura-osmd-poc` isola prévia OSMD horizontal via long-press, preserva a pauta temporal usual e prepara comparativos de screenshot/performance. **Não mesclar como solução final nem emitir 0.9.47**: ainda faltam mapeamento gráfico de notas/MIDI, cursor temporal com espaçamento óptico, hit-test, polyphony, long scores, revisão editorial, física e decisão do usuário sobre abandonar proporcionalidade **estrita**. Ver [56](56-ESTUDO-GRAVURA-PROFISSIONAL-POC.md), issue #34 e aceite #11. Biblioteca é outro problema, fora deste PR.


## Retomada da prova de gravura OSMD (18/10/2026, PR #46 draft)

A conversa que iniciou a pesquisa foi interrompida; o trabalho encontra-se integralmente no repositório aberto, branch `research/estudo-gravura-osmd-poc`. A HEAD inicial conferida nesta retomada foi `15edfe56bb2b40956ae7243ee1a310d8ef7e02da`, com build/emulador aprovados [39085951671](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39085951671) e [39085951669](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39085951669). Atenção: estas execuções NÃO validam novos commits.

Após ler `AGENTS.md`, a arquitetura, a notação e o [diagnóstico 56](56-ESTUDO-GRAVURA-PROFISSIONAL-POC.md), a retomada acrescentou ao protótipo mapeamento conservador de cabeças OSMD para eventos MIDI do parser nativo, diagnósticos de correspondência/ambiguidade, atualização do hit-test após resize e sincronismo visual rAF contínuo com correção de fase. Foram ampliados JS, contrato e ensaio de toque no emulador. Confira o HEAD atual e os dois workflows do **mesmo SHA** antes de afirmar que passaram. Novos registros de execução devem ser adicionados ao PR #46; não usar logs antigos para declarar conclusão.

Posteriormente, o PR recebeu uma **primeira seleção gráfica de loop por dois toques** em notas já cruzadas com o motor MIDI, usando o callback nativo existente, sem alterações de dados ou motor. O emulador ganhou cenário com registro `POC_LOOP_SELECTED` e captura `piano-osmd-poc-loop.png` quando executar. É experimental: não equivale à experiência com arrasto, alças e seleção de notas polifônicas.

**Próximo trabalho independente:** revisar resultados de CI, corrigir falhas, depois completar interação de loop com arrasto/alças, preparo do Espera, filtragem de mãos, cursor sob mudanças de tempo, complexidade de vozes e testes de longa duração. Guardar capturas reais (pauta antiga, prévia estática e em movimento) para inspeção humana. O app normal continua na notação temporal antiga, o preview é opt-in, e o PR deve permanecer draft até qualidade editorial aceita e regressões funcionais comprovadas. Nenhum teste de hardware real foi feito nesta retomada. Não tocar no repositório privado.


### Continuação do Espera na camada OSMD (18/10/2026)

A prévia opt-in agora recebe os comandos já existentes de preparação/cancelamento do Espera e apresenta contador visual baseado na duração nativa configurada, sem tocar no relógio de execução MIDI, na seleção de mãos nem no arquivo original. A simulação do emulador testa `POC_WAIT_PREPARE`, `POC_WAIT_COMPLETE` e salva `piano-osmd-poc-wait.png`; o workflow inclui também `piano-osmd-poc-loop.png`. **Confira os dois workflows do commit final antes de declarar a interface aprovada**. Ainda pendem trajetória espacial preparatória, aceite editorial e ensaios físicos.

### Estudo profissional — comparação longa e adaptação vertical (18/10/2026)

A branch do [PR #46](https://github.com/atshimura/Piano-livre-aberto/pull/46) continuou **somente como protótipo opt-in**, após autorização do usuário para espaçamento óptico. A execução Android da `Gymnopédie nº 1` (partitura já incluída e autorizada, **78 compassos**) expôs falha editorial importante: SVG OSMD horizontal ~7749 × 382 CSS px sobre viewport de 196 px de altura, mostrando **apenas a pauta superior**, embora a CI estivesse verde. O novo código ajusta **somente a dimensão CSS do SVG da prévia**, preservando seu `viewBox`, alturas, eventos MusicXML originais, mapa nativo MIDI e ponte de identificação por toque. A escala resulta ~0,493, com SVG exibido ~3818 × 188 px e **ambas as pautas visíveis** no [emulador do commit anterior](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39125229195). O primeiro render da partitura longa levou ~1,30s; o monitor de callbacks JS observou média ~30,11ms, pico ~113ms e 22 intervalos acima de 50ms: ainda não significa FPS visual ou estabilidade em aparelho real.

O modo Estudo **normal permanece no renderizador temporal anterior**, e a `main` na 0.9.46/code55. A troca de altura do teclado guia agora não faz regravação de todo SVG quando a largura permanece igual. Adicionado teste de toque real na primeira cabeça da pauta superior **após a escala CSS**, validando identificação pelo `Score.java`; verificar sucesso no **HEAD atual**, não atribuir os resultados do commit anterior ao cenário posterior. Capturas de comparativo da Gymnopédie e relatório OSMD constam como artefatos da CI (expiram em 3 dias). Para aceitar ainda faltam: tempo/scroll fluido em aparelho real, pauta/claves fixas à esquerda no novo motor, sobreposições, 14 cabeças sem correspondente nativo nessa Gymnopédie, notação de várias vozes, loop completo com alças, gestos de estudo e revisão editorial/hardware (#11). O usuário **não aprovou visualmente** a nova pauta nem autorizou novas dependências. Consulte [doc 56](56-ESTUDO-GRAVURA-PROFISSIONAL-POC.md); não integrar/publicar APK definitivo com estes limites.


## Item 1.2 — gravura profissional: causa das 14 notas e representação dual (18/10/2026)

O protótipo OSMD continua em `research/estudo-gravura-osmd-poc` / PR #46 draft, **sem mudança da apresentação padrão** na `main`. Os 14 itens da Gymnopédie não são ambiguidade de vozes: o relato `POC_LONG_MAPPING` do emulador [39132999090](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39132999090) indica 469 gráficas, 455 conciliadas, 14 `noNativeCandidate`, correspondentes a compassos com provável continuação de tie. Inspeção do código encontrou a causa estrutural: `Score.java` funde ties em um único ataque musical, mas o OSMD mostra cada cabeça escrita.

**Correção de arquitetura em pesquisa:** `Score.writtenEvents` mantém uma cópia antes da fusão, distinguindo `playbackAttack`. `Score.events` permanece **inalterado como relógio/ataques/MIDI**; o bridge da prévia usa `writtenEvents`, preservando parte, pauta, compasso, voz, índice XML, grafia enharmônica, beat, duração e tie. JS apresenta nota exata e identifica continuação sem permitir novo ataque no início de loop. JVM testa dois segmentos escritos para um ataque de Fá♯ ligado; JS testa ambas as cabeças e associação conservadora por voz; emulador exige todas as 469 cabeças. **Consultar resultados definitivos dos workflows no SHA final, não assumir êxito antecipado.**

O experimento separado de recorte `getBBox` foi revertido, pois não melhorou a altura visível da Gymnopédie e duas execuções intermediárias de emulador terminaram na tela inicial do Android após reprodução longa. O teste agora registra `dumpsys activity`/processo para diferenciar queda do aplicativo de variação do emulador. A escala antiga volta a ser referência até alternativa comprovada.

**Ainda por fazer antes de concluir item 1.2:** confirmar CI e tela/capturas de todas as notas; contexto esquerdo de clave/armadura/fórmula fixo e correto nas mudanças; legibilidade sem escala excessivamente pequena; seleção de trecho por arrasto/alças e visualização por mãos/realce; estabilidade e fluidez sob partitura longa, rolagem, mudança de andamento, Espera, teclado aberto, retomada e loops; regressões ampliadas; aprovação editorial do usuário; testes físicos posteriores em Realme 7 5G, Lenovo Tab M9 e Yamaha P-45. Não mesclar PR, publicar nova beta ou marcar issue #34 completa sem atender os critérios técnicos e visuais. Não tocar no repositório histórico privado.


## Retomada do item 1.2 — estado confirmado do PR #46 (18/10/2026)

Repositório oficial `atshimura/Piano-livre-aberto`; branch `research/estudo-gravura-osmd-poc`; [PR #46](https://github.com/atshimura/Piano-livre-aberto/pull/46) aberto como **draft**. A retomada começou do head `f1d3dc1`; o problema do emulador foi reproduzido e diagnosticado antes de qualquer alteração musical: ADB entregava o toque prolongado; OSMD carregava e renderizava; Chromium 74 do Android WebView falhava em `SVGElement.replaceChildren()`. Troca por remoção DOM compatível corrigiu a causa no commit `42ef86d69460c17a0b50fad97692ea7cd48d06a0`.

Actions do SHA `42ef86d`: [build, JVM/JS/MusicXML/MIDI](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39144872504) **success**; [emulador Android](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39144872440) **success**. A Gymnopédie de 78 compassos conciliou **469/469 cabeças**, sem ambiguidade/faltantes; Ode à Alegria 81/81. Passaram também toque gráfico, seleção de mãos, resize, scroll, loop experimental, preparação Espera, restauração do renderer temporal, longa duração/captura e regressões UI. Artefato do emulador: 26 arquivos, incluindo capturas antes/OSMD/referência, Gymnopédie, JSON e vídeo; disponibilidade do artefato por três dias.

Medições longas: render 1.366 ms, SVG 4.927 nós, escala CSS 0,4927 e dimensão exibida 3818×188 CSS px em viewport 738×196. As duas pautas aparecem na captura, mas o aceite de legibilidade continua pendente. Intervalos rAF foram irregulares (médias registradas de 53,61 ms na Ode e 33,09 ms na Gymnopédie; picos 157/231 ms); **não chamar isso de FPS**. PSS consultado só no processo principal; renderer WebView possivelmente separado.

**Próxima ação exata:** implementar/instrumentar contexto de clave, armadura e fórmula fixo à esquerda no scroll OSMD sem duplicar nem alterar a partitura MusicXML; executar emulador com captura de início/meio/fim; então avaliar o tamanho legível e colisões antes de expandir para o corpus musical adversarial. Manter o PR draft e `main` em 0.9.46/code55. Continuar a pesquisar as pendências documentadas em [56](56-ESTUDO-GRAVURA-PROFISSIONAL-POC.md); biblioteca compacta é tarefa separada. Hardware #11 e aceite visual do usuário continuam explicitamente pendentes.

### Prova da árvore de instruções OSMD

No SHA `c4b17f1`, build e emulador passaram com 469/469 cabeças. O SHA `1d7d2e0` incluiu o relatório no artefato e passou nos dois workflows, mas a linha Android continuava truncada. No SHA `dbb914c`, build e emulador passaram; o artefato (27 arquivos) incluiu o TXT completo com 24 registros JSON válidos abaixo de 3.500 caracteres, e Gymnopédie registrou 469/469. A revisão do relatório encontrou seleção errada de `POC_CONTEXT_NODES_2` (measure 2 da pauta superior, não a grave). O SHA seguinte muda a coleta para `staffline`→primeira `vf-measure` e exige que as duas primeiras sondagens sejam compasso 1 em `Piano0-1`/`Piano0-2`. A faixa fixa ainda não foi implementada. Depois, testar fixture MusicXML com alterações de clave/armadura/fórmula antes de declarar comportamento contextual.

O SHA `f60fbc5d517f315b61645b63fc994f46a5ed255c` passou no build `39148592764` e emulador `39148592883`. O artefato `12143774284` contém 27 arquivos previstos, incluindo capturas e TXT de contexto. JSON longo: 78 compassos, 469/469 cabeças, 1.167 ms, 4.927 elementos e escala 0,4927. A prova foi corrigida: `POC_CONTEXT_NODES_1/2` agora representam `Piano0-1/1` e `Piano0-2/1`.

**Estado do commit `4c5b654`:** build `39149525963` e emulador `39149525971` passaram. O artefato `12144024626` (27 arquivos) foi inspecionado e mostra Gymnopédie 78 compassos/469 de 469 cabeças, 1.287 ms de render, 4.927 nós e escala 0,4927. A faixa atualizou medidas 1→2→3, mas a captura em movimento confirmou claves duplicadas e posição fixa em aproximadamente 30% da tela. Portanto, passou a verificação funcional inicial e **falhou na posição editorial solicitada**.

**Atualização após conclusão do SHA `286defc28c044ffaa3d648fa447def789d5ea809`:** build `39150432492` e emulador `39150432495` passaram. Artefato `12145276242`, 27 arquivos, foi baixado e inspecionado. Gymnopédie: 78 compassos, **469/469 cabeças**, 1.166 ms, 4.927 nós, escala 0,4927; a captura confirma o rail fixo à esquerda e o score se movendo. A legibilidade nessa escala continua pendente.

**SHA `8fa2242e2fa85c4048954c1d9f0994d82e209c35`:** build `39151195201` passou na suíte completa, APKs debug/release e isolamento de distribuição. O emulador `39151195211` falhou somente ao procurar o título do fixture depois dos cenários anteriores; Gymnopédie manteve 469/469. Artefato `12145706113` tem 25 arquivos e a hierarquia final mostra aviso de sessão salva tentando abrir `imported/upgrade.mxl` ausente. Não há log que prove causalidade com o intent. Próxima revisão independente do teste: não carregar sessão anterior quando o intent de fixture debug estiver ativo, usar `am start -S`, registrar aceitação/carga e anexar screenshot, hierarquia, logs específicos e Activity se falhar.

**SHA `87f1d77`:** build passou; o intent da fixture foi aceito e o título “Context changes test” apareceu. O emulador falhou ao procurar “Partitura Livre”; a hierarquia genérica do artefato incluía o rótulo, sem captura específica. **SHAs `f847824` e `94236b0`:** builds completos passaram (`39152801867`, `39153579431`). Em ambos, emulador passou Gymnopédie 469/469, toque de nota, rolagem, seleção de trecho, Espera, captura de movimento e retorno do segundo plano, depois falhou antes de abrir a prévia. Artefatos `12145823282` e `12145539015` incluem screenshot, XML e logs; a hierarquia e a mensagem de erro listam “Partitura Livre”. Causa encontrada: `next(..., None)` retornava corretamente o nó, mas `if context_button` tratava como ausente o `ElementTree.Element` folha, cujo valor booleano é falso quando não tem filhos. Não era falha do gesto, OSMD nem Unicode. O artefato `12145539015` confirmou que o rótulo bruto era exatamente “Partitura Livre” (U+0050...U+0065), normalizado como `partituralivre`; o erro era só a truthiness do nó folha. No SHA `0c87040`, a busca foi corrigida e a interface registrou “Tapped Partitura Livre”; a prova carregou e a execução chegou às mudanças de compasso. O emulador (`39154409349`) falhou na expectativa do compasso 2. O SHA `943e304` registrou estados tardios de outra pauta misturados ao fixture. Entre os eventos do fixture, a medida 2 mostrou origens `clef/key/time=2/2/2`, e a medida 3 `2/3/3`. O teste escolhia o primeiro evento de medida 2, antigo, e o fixture ainda mostra clave de origem 2 onde espera 3 no compasso 3. O próximo ajuste limpa logcat após comprovar renderização, seleciona estados pela origem esperada e mantém a asserção estrita da clave 3. Artefato `12146378167` inclui capturas início/meio/fim. A falha atual é de contexto musical, separada do seletor já corrigido. PR #46 continua draft.


### Retomada do diagnóstico de contexto — SHA `ace0d0f`

O build passou (`39156493985`) e o emulador falhou na verificação de clave no compasso 3. A captura de probes ainda misturou registros de partituras anteriores e não provou que o `POC_OSMD_READY` encontrado correspondia ao gesto da fixture. Os estados `1/null/1`, `2/2/2`, `2/3/3` não são atribuídos à fixture sem correlação. Artefato do emulador: `12147131689`. O commit seguinte exige `CONTEXT_FIXTURE_LOAD_OK` com três compassos, limpa logcat imediatamente antes do toque prolongado, exige novo `POC_OSMD_READY` e arquiva o inventário OSMD/SVG e estados antes das asserções. Nenhuma transição de contexto da fixture está validada neste ponto.


### Fixture correlacionada — SHA `373972c`

Build completo `39157236327` passou. O emulador `39157236347` confirmou a carga Java da fixture, limpou log antes do toque prolongado e encontrou um `POC_OSMD_READY` novo de 3 compassos. Assim, os estados são desta fixture: m1 `1/null/1`; m2 `2/2/2`; m3 `2/3/3` (origens de clave/armadura/compasso). Falha real confirmada: o rail não avança a clave de sol de retorno no m3. Artefato `12147393066` inclui JSON e probes pré-asserção. Próxima ação: imprimir esse probe no log do Actions para verificar os atributos OSMD de m3 e se o glifo clef aparece no SVG; corrigir o extrator/modelo sem reduzir a asserção.


### Diagnóstico de contexto — SHA `2416e9d`

O build `39157939341` passou; o emulador `39157939343` falhou somente na asserção de troca de clave do compasso 3. A suíte percorreu antes a Gymnopédie de 78 compassos e conciliou `469/469` cabeças escritas, sem ambiguidades ou divergências, e reconheceu toque real em nota. A clave da régua reportou origem 2 no compasso 3 apesar de a fixture declarar nova clave de sol; armadura e compasso reportaram origem 3. O arquivo de probe foi criado no diretório do projeto, mas o passo de log no workflow usou prefixos incompatíveis com o `working-directory`, então seus dados não apareceram no log. Próximo SHA corrige o caminho e registra os filhos SVG dos primeiros quatro compassos por pauta para identificar se a clave está ausente no modelo ou apenas no seletor. A falha e a asserção permanecem intactas.


### Contrato do probe — SHA `e2e4ee1`

Build `39158718398` falhou no contrato estático porque `POC_CONTEXT_NODES` agora armazena uma lista de compassos por pauta, enquanto a verificação ainda esperava um único nó de compasso. Os testes JVM anteriores passaram. O emulador `39158718300` está executando com o caminho corrigido do passo de diagnóstico. O SHA seguinte atualiza o contrato para exigir as duas pautas e os compassos até 4, sem alterar a asserção funcional da clave.


### Ajuste do contrato da leitura do probe — SHA `95dcb01`

Build `39158885775` alcançou o teste OSMD, mas falhou porque o contrato procurava a comparação antiga `measure == "1"`; o teste de UI passou a construir listas `first_measures` e `second_measures`. O contrato será atualizado para conferir a igualdade dos três primeiros compassos nas duas pautas. O emulador desse SHA segue ativo.


### Probe limitado pelo tamanho do logcat — SHA `e2e4ee1`

O emulador `39158718300` passou pelos fluxos principais, incluindo a Gymnopédie 469/469 e toque real, mas falhou antes da fixture porque a nova linha `POC_CONTEXT_NODES_1` ultrapassou o limite de registro do logcat. O próximo SHA limita o inventário a glifos de clave, armadura e fórmula de compasso nos quatro primeiros compassos de cada pauta, mantendo caixas SVG e o conteúdo da clave. Isso conserva a evidência necessária sem serializar os grupos de notas.


### Restaurar saída dos probes limitados — SHA `88b4749`

Build `39159267925` falhou no contrato de contexto porque a substituição que limitou os objetos SVG também removeu as linhas que os escreviam no log. O conteúdo resumido continua no código, mas não era emitido. O próximo SHA restaura os marcadores `POC_CONTEXT_SVG_ROOT` e `POC_CONTEXT_NODES_*` com apenas glifos de contexto, sem grupos de notas.


### Causa da clave contextual localizada — SHA `117e044`

O emulador `39159409919` confirmou fixture OSMD pronta (3 compassos, 19 cabeças) e preservou no modelo `InitiallyActiveClef`: compasso 2 = clave de dó (`clefType:2`), compasso 3 = clave de sol (`clefType:0`). O SVG VexFlow, porém, guarda a mudança de clave de dó ao fim do grupo SVG do compasso 1 (x=226.4, perto do limite 254.4) e a mudança para sol ao fim do grupo do compasso 2 (x=470.7, perto do limite 498.7); os compassos 3 não têm grupos `vf-clef`. Portanto a partitura fonte e o OSMD estão corretos, mas o extrator confundia grupo gráfico com compasso musical. O próximo SHA atribui glifos de clave junto à borda direita ao compasso seguinte, mantém o glifo OSMD original, registra os remapeamentos `1→2` e `2→3`, e exige o rail correto nos compassos 2 e 3.


### Contrato de origem musical — SHA `8a317a4`

Build `39160134890` falhou no contrato estático ao exigir `sourceMeasure:measureNumber`, a antiga equivalência entre grupo SVG e compasso. A lógica agora preenche `sourceMeasure` com o compasso musical seguinte quando o glifo está junto à borda direita. O teste do emulador no mesmo SHA segue em andamento; o próximo contrato verifica `sourceMeasure:targetMeasure` e o registro de remapeamentos.


### Isolamento do teste de upgrade release — SHA `8a4e278`

O teste de contexto no emulador passou: m1 `1/null/1`, m2 `2/2/2`, m3 `3/3/3`; remapeamentos SVG `1→2` e `2→3` confirmados. A etapa seguinte `emulator_release.py` falhou em `Ocultar teclado` porque `emulator_ui.py` havia alterado a mesma preferência sintética de upgrade e deixado uma partitura debug-only ativa. Corrigirei a contaminação do cenário usando um único construtor de preferências sintéticas e resemeando o estado determinístico antes da instalação do APK release, preservando as verificações de perfil, teclado e andamento.


### Estado completo da retomada — SHA `d1060ea`

No SHA `d1060eac30b7116e47e82a990a9a3348d98cec89`, build `39160958903` e emulador `39160958918` passaram. O emulador conciliou 469/469 cabeças da Gymnopédie, manteve o toque gráfico, confirmou remapeamentos SVG `1→2` e `2→3` e rail m1 `1/null/1`, m2 `2/2/2`, m3 `3/3/3`. O upgrade para APK release não depurável preservou perfil, teclado e andamento após a fixture de upgrade ser reaplicada em cenário isolado. Artifact `12148742058` foi enviado (4,693,195 bytes). A próxima alteração adiciona um relatório pré-upload que valida assinatura PNG e dimensões das capturas esperadas, para comprovar nominalmente que as imagens comparativas estão presentes.


## Tarefa 03C/20 — navegação e geometria responsiva

Código validado `0cc670c2513eea37202eb63e4cdfb734a2a07d5d`, PR #46 ainda draft: build 39272746781 e emulador 39272746710 success. 03C concluída tecnicamente por botões de navegação vertical sem compressão, com testes de quatro partituras, teclado fechado/aberto e Gymnopédie em três displays. Abrir o guia recuperou 22 CSS px líquidos; em celular baixo a Gymnopédie ainda exige pan, em tablet 883×409/431 cabe integralmente. Ver seção final de 03C no documento 56 e docs/evidence/tarefa-03c/metrics.json. A aprovação editorial/física está pendente. Próxima tarefa é 04 (contexto fixo e texto expressivo), **não iniciada nesta entrega**. Não promover OSMD ao padrão, não mesclar PR nem publicar versão por estes resultados.

## Decisão funcional vigente — Tarefa 03D/20

O usuário rejeitou definitivamente a rolagem vertical da partitura na prévia OSMD: as duas pautas do piano devem permanecer simultaneamente visíveis. A escala da gravura completa é recalculada automaticamente, de forma proporcional, pela altura CSS efetivamente disponível no WebView, incluindo mudanças ao abrir/fechar o teclado guia e redimensionar a tela. A navegação vertical e os botões introduzidos na 03C deixam de valer nessa prévia; o movimento horizontal sincronizado permanece. A prévia segue experimental e o renderizador temporal segue padrão. Estado de execução, commit, workflows e limitações da 03D devem ser lidos na seção final da tarefa no documento 56.

### Tarefa 03D/20 — resultado técnico

Código validado no head `e7a8367e92ea5180e88dac8f8fcb0ea472c0a1a4`; build `39296856369` e emulador `39296856406` success. Gymnopédie conciliou 469/469 cabeças, fixture complexa 27/27, sem ambiguidade/faltantes; quatro partituras passaram teclado fechado/aberto/restaurado. A escala uniforme mantém ambas as pautas visíveis nos perfis testados; rolagem vertical máxima 0. Corrigida e coberta por teste a medição invariável da separação entre pautas: 15,036 espaços na Gymnopédie em todas as escalas. Artifact Android [12210230462](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39296856406/artifacts/12210230462) contém 18 capturas 03D e JSONs; métricas finais no documento 56. A legibilidade no perfil baixo (4,927 CSS px por espaço entre linhas), aceite editorial e testes físicos Realme 7 5G/Lenovo Tab M9/Yamaha P-45 continuam pendentes. PR #46 fica draft; renderer temporal permanece padrão, main/versão pública inalterados. Próxima etapa recomendada: Tarefa 04/20 (contexto e texto expressivo); não iniciada.


## Continuação do estudo de gravura — Tarefa 04/20 (19/10/2026)

PR experimental #46, branch `research/estudo-gravura-osmd-poc`: o contexto musical fixo recebeu máscara restrita às linhas OSMD, eliminando o fundo opaco integral. A Gymnopédie mostra “Lent et douloureux” integralmente nas capturas de celular baixo/alto, teclado aberto/fechado e tablet do artefato [12220678144](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39321825761/artifacts/12220678144). A fixture contextual apresentou transições de claves/armaduras/compassos e a nova captura do retorno ao início foi inspecionada no [artefato 12222302435](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39325266308/artifacts/12222302435). Testes finais do código `25790ef9`: [build 39325266465](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39325266465) e [emulador 39325266308](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39325266308), ambos `success`. Gymnopédie 469/469, complexa 27/27, rolagem vertical zero, duas pautas e escala proporcional preservadas.

A Tarefa 04 está **concluída tecnicamente nas amostras e perfis do emulador**, não constitui certificação editorial universal nem teste físico. Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 pendentes. Não integrar PR #46, não alterar `main` ou publicar release. Próxima tarefa planejável: 05/20, respeitando o escopo e aprovação independentes. Consultar `docs/56-ESTUDO-GRAVURA-PROFISSIONAL-POC.md` para detalhes e histórico.


## Encerramento da Tarefa 05/20 — 20/10/2026

Tarefa 05 concluída tecnicamente no escopo auditado de acordes/polifonia, com ressalva editorial: etiqueta transitória de toque cobre parte da pauta inferior em duas capturas (posicionamento reservado à Tarefa 08). Seção final do documento 56 prevalece sobre indicações históricas de que a 05 não iniciou. Código 1e21db2a1affaada81771cccb8b432f2bcceebb5; PR #46 permanece aberto em draft na branch research/estudo-gravura-osmd-poc.

Verificados diretamente [build 39344297440](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39344297440) e [emulador 39344297545](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39344297545), ambos success; checkout dos Actions: merge sintético c01b032e0e7c94bd0ffcf08dbb7fb60ca0e1504b da HEAD acima com main 15cafe3c8da7c67b9752d14ba0fb6f8f6141177d. As seis capturas da 05 do [artefato 12229834809](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39344297545/artifacts/12229834809) foram abertas e examinadas. Fonte 30 notas escritas, 30 cabeças reconciliadas por identidade, 29 ataques (uma continuação ligada); uníssono com duas vozes preservado e cenários ambíguos cobertos conservadoramente em JS. Três toques reais ADB selecionaram MIDI 72, 74 e 48 nas pautas esperadas. Gymnopédie 469/469 e complexa 27/27; duas pautas, escala proporcional, rolagem vertical zero e barras ocultadas/restauradas pelo teclado preservadas.

Entrega atual exclusivamente documental; código/testes/workflows/versão inalterados. Pode reutilizar os workflows acima para esse código conforme a ordem de encerramento. Próximo passo: Tarefa 06/20, não iniciada nesta conversa. Não promover OSMD ao padrão, integrar main ou publicar distribuição. Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 pendentes; sem certificação editorial universal. Consultar documento 56 para detalhes, commits e limites de cobertura.

## Tarefa 06/20 — execução em andamento

Base confirmada 0d529794b116b663032a4741a6dd705c2a6629e9; PR #46 aberto/draft. Diagnóstico e cinco fixtures debug-only adicionados; consulte seção Tarefa 06 em docs/56. Teste racional de fonte passou; testes JS de mapa/polifonia/contrato/ornamentos/spanners passaram. JVM, APK, WebView e revisão visual precisam de Actions do novo commit. Atenção: duration=2/divisions=4 na tercina da fixture complexa antiga não corresponde à duração de colcheia 3:2; não usar esse ritmo como referência. Não alterar arquivo antigo para mascarar divergência. Não iniciar 07 nem declarar conclusão antes de evidências.

Build do primeiro commit 716cabf passou (run 39424976030). Correção posterior: grafia original desambigua enarmonia simultânea na mesma voz em OpticalTimeMap; duplicatas permanecem conservadoras. Não reutilizar build anterior para afirmar aprovação desta correção. Grace agora inclui outra voz/pauta (5 ornamentos, 6 ataques). Novas capturas devem incluir o fim da fixture de acidentes para alcançar duplos sinais e acorde denso.

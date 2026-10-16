# Plano sequencial até a versão 1.0

Plano aprovado nas instruções do Projeto em 16/10/2026. Acompanhamento: [issue #4](https://github.com/atshimura/Piano-livre-aberto/issues/4).

| Etapa | Escopo | Critério de passagem | Situação |
|---|---|---|---|
| 1 | Estabilização, desempenho e fluidez da partitura | Regressões de movimento, pausa, seek, loop e resize; custo por frame reduzido e diagnóstico reproduzível | Escopo técnico concluído; aceite físico em #11 |
| 2 | Aprimoramento do motor musical e da fidelidade MusicXML | Fixtures musicais, comparação Java/JS e limitações explícitas | Escopo técnico concluído; limites no documento 30 |
| 3 | Aperfeiçoamento dos modos de aprendizagem, reconhecimento MIDI e teclado guia | Regressões dos modos, novos ataques, extras, calibração, pedal e duas mãos | Escopo técnico concluído; aceite físico em #11 |
| 4 | Biblioteca, perfis, histórico e progresso de estudos | Busca/organização e progresso local compatíveis com retomada e backup | Em desenvolvimento |
| 5 | Importação MIDI, compatibilidade musical e preservação de dados | Importação documentada, entradas adversariais e recuperação sem perda | Pendente |
| 6 | Consolidação, testes e preparação da versão 1.0 | Matriz de aceite, APKs assinados, instalação/atualização/backup e testes reais | Pendente |

Cada etapa terá entregas incrementais em branches e PRs; não representa promessa de conclusão em uma única sessão. Avançar nas tarefas independentes enquanto aguardam testes físicos, sem transformar pendência de hardware em aprovação. Nunca alterar o histórico privado, expor segredos ou introduzir servidores, telemetria ou serviços pagos.

## Entrega inicial da Etapa 1 — 0.9.13/code22

Base: main `3ba5368` (0.9.12/code21). Branch: `perf/etapa-1-partitura`.

- Indexa as cabeças das notas uma vez por render, incluindo cópias nas bordas dos tiles. Destacar uma nota deixa de pesquisar a árvore SVG inteira.
- Usa largura capturada no render para mover tiles e emitir amostras, removendo leituras de geometria da animação.
- Resize sem mudança de dimensões não reconstrói a partitura. Mudança apenas de altura (teclado guia) atualiza escala vertical da pauta e do rail, conservando notas, controles, trecho e relógio.
- Mudança de largura reconstrói a geometria preservando posição projetada, BPM, execução e seleção. Antes, reconstruir pausava o relógio visual até outra amostra nativa.
- Corrige limite visual indevido de 10–300 BPM: o andamento efetivo enviado pelo Android já inclui a velocidade de estudo e passa a ser seguido integralmente. Regressão nova falhou antes da correção em 6 BPM e passa em 6/600 BPM após ela.
- Mantém regras pedagógicas, parser, eventos MIDI, dados e assinatura. O teste novo executa render/sync/resize reais em DOM e relógio determinísticos; não mede FPS nem substitui WebView/emulador.

Verificação local: auditoria pública, sintaxe JavaScript e seis suítes JS passaram, incluindo TemporalRenderingTest (sem leituras de viewport ou buscas DOM durante os frames, destaques, resize, pausa, retorno, loop e teardown). A tentativa local das suítes JVM encontrou ambiente sem `javac`; Gradle/Android SDK também não estão instalados. JVM, comparação Java/JS, auditoria MIDI, compilação e emulador deverão ser comprovados pelo Actions do commit desta entrega.

Próxima tarefa: confirmar CI do PR, integrar respeitando proteção de main e obter APK com assinatura permanente pelo workflow confiável. Depois ampliar diagnóstico de frames/carga de partituras longas e lifecycle antes de fechar Etapa 1. Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 permanecem sem teste físico nesta entrega. Não se declara resolvida a microtravada real apenas por reduzir trabalho no código.

## Retomada verificada em 16/10/2026 — 0.9.14/code23

O PR #5 foi integrado em `5ce33130fcad3d040e38f51a6742a6a13148cca5`. Os Actions da main [build 38938312341](https://github.com/atshimura/Piano-livre-aberto/actions/runs/38938312341) e [emulador 38938312383](https://github.com/atshimura/Piano-livre-aberto/actions/runs/38938312383) passaram. O log do build confirma 0.9.13-open-beta/code22 e assinatura persistente com SHA-256 `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`, igual à 0.9.12. A API confirma main protegida; detalhes administrativos da proteção retornam 403 ao plugin, portanto não foram auditados diretamente. Não há PR aberto anterior nem Release publicada nesta retomada.

Segunda entrega da Etapa 1: a partitura passa a solicitar frames sob demanda quando pausada. Após o primeiro desenho ou a última atualização de pausa, a fila fica vazia; seek pausado agenda um desenho; retomada e retorno do fim reativam uma única cadeia. Ao alcançar o limite visual da música, a animação aguarda outra amostra nativa. Não altera avanço Java, MIDI, reconhecimento, dados ou regras pedagógicas.

Regressão determinística: o teste novo falha contra o renderer da 0.9.13 (`initial paused render sleeps`, uma solicitação em vez de zero) e passa com a correção. Verifica 240 oportunidades de frame sem trabalho ocioso, amostra pausada idêntica, seek, retomada, retorno/loop, BPM efetivo 6/600, resize e teardown/substituição da partitura. Isso comprova a ausência de callbacks ociosos no cenário simulado, não redução medida de consumo nem FPS no Realme. Auditoria pública, sintaxe e seis suítes JS passaram localmente. JDK/Gradle/SDK indisponíveis localmente; JVM, APK e Android dependem dos Actions deste novo commit.

Etapa 1 continua aberta. Próxima tarefa concreta: reduzir formatação/aplicação redundante do texto em MainActivity.updateInfo, com regressões; depois diagnóstico reproduzível de frames/carga longa e lifecycle. Hardware Realme/Lenovo/P-45 permanece pendente, sem bloquear essas tarefas. Evidências de integração e CI desta entrega devem ser registradas na issue #6 e no PR.

A primeira CI desta entrega compilou corretamente code23 e passou JVM/JS/MIDI, mas revelou verificação de metadados fixada em code22/0.9.13 no workflow. Corrigida para ler a fonte de versão gradle.properties e comparar o APK efetivo, mantendo a verificação de pacote e versão obrigatória. Aguardar nova CI; a execução anterior não aprova o novo commit.

## Terceira entrega da Etapa 1 — 0.9.15/code24

MainActivity mantém o transporte em ~25 ms, mas a apresentação textual usa StudyInfoText para formatar no máximo a cada 100 ms durante movimento estável. Mudanças de andamento/velocidade/música, comandos explícitos e término do movimento atualizam imediatamente. TextView.setText só é aplicado quando o conteúdo difere. Esse cache não participa do relógio musical, MIDI, reconhecimento ou pintura da partitura/teclado.

StudyInfoTextTest exercita 40 ticks em um segundo (dez formatações), seek imediato, mudança de andamento e velocidade, remoção da música, locale e reinício do relógio. Auditoria pública local aprovada; JDK ausente, portanto o teste JVM novo e a compilação precisam de comprovação na CI do commit. Não atribuir os resultados da 0.9.14 a esta versão.

Próxima tarefa concreta após CI/integração: diagnóstico reproduzível de frames e carga longa, incluindo lifecycle da WebView e passagem biblioteca/estudo/leitura livre. Testes físicos ainda pendentes. A Etapa 1 não está encerrada.

Evidências 0.9.14: [PR #12](https://github.com/atshimura/Piano-livre-aberto/pull/12) integrado em `04ee7fb70d2006922776f824d991bafa7557568b`, após [build 38939921990](https://github.com/atshimura/Piano-livre-aberto/actions/runs/38939921990) e [emulador 38939921996](https://github.com/atshimura/Piano-livre-aberto/actions/runs/38939921996) aprovados no commit `309397d1f992f62506aaf11b47c4c7b6eb4f739c`. A assinatura do push da main deve ser conferida separadamente.

Na 0.9.15, TemporalRenderingTest acrescenta partitura sintética de 64 compassos, mais de 50 tiles: busca adiante/atrás conserva no máximo três tiles visíveis e não realiza leitura de dimensões ou busca DOM por frame no cenário determinístico. Teste passou localmente. Não mede carga inicial real, FPS ou rasterização da WebView.

## 0.9.16/code25 — diagnóstico e lifecycle

0.9.15 integrada no PR #13 em ad2f29b; build main 38940939099 e emulador main 38940938990 aprovados, assinatura persistente/code24 confirmados.

Adicionados contadores locais de render/callbacks com memória fixa, snapshots e relatórios ao pausar/desmontar. Corrigido ticker nativo que continuava sendo agendado em background; WebViews recebem pause/resume e retorno conserva a execução pausada. Regressões determinísticas passam localmente; nova validação Android exige CI do próprio commit. Procedimento/métricas/limites em [29](29-DESEMPENHO-LIFECYCLE.md). Não se declara eliminada a travada física.

Após comprovar CI e integrar esta entrega: reavaliar o fechamento técnico da Etapa 1, manter validação física pendente e iniciar fixtures/auditoria Java versus JS da Etapa 2 (#8), sem aguardar hardware.

## Etapa 2 iniciada — 0.9.17/code26

Enquanto a CI de lifecycle da Etapa 1 é acompanhada, auditoria independente reproduziu duas falhas: durações decimais perdidas e colisão de vozes textuais em ligaduras. Corrigidas com cinco fixtures compartilhadas e resultados esperados; ver documento 30. Etapa 1 mantém aceite físico pendente e não se presume encerrada antes da CI do PR #14. Etapa 2 segue incremental.

## Etapa 2 — 0.9.18/code27

Corrige nota omitida no Espera quando há tie stop sem início no arquivo. Fixture verifica três ataques (antes, apenas um estava disponível), aviso de interpretação único e fonte intacta. Ligaduras completas preservadas e regressões existentes aprovadas localmente. Ver documento 30. Dependências: validar/integrar PR #14 e #15 antes desta entrega; conferir CI do próprio commit.

## Fechamento técnico da Etapa 1 e continuação 0.9.19

PR #14 integrado em e4f4f511dc8ebb39941e3afa8febbb14ef36c28b após build 38943065678 e emulador 38943065686 aprovados no commit 36bb35f. Teste Android confirma pausa em background, posição conservada e retomada manual com avanço real. Corrigida leitura de XML antigo pelo harness UIAutomator antes do aceite. Issue #6 encerrada no escopo técnico; validação física/microtravadas permanece em #11, sem alegar eliminação no Realme/Lenovo.

0.9.19/code28 amplia a Etapa 2 com andamento sound direto, precedência de offset e offsets globais negativos. Ver fixture e limites no documento 30. Dependências de integração/CI dos PRs musicais continuam obrigatórias.

## Etapa 2 — 0.9.20/code29

Dinâmica numérica explícita no motor/JS e demonstração MIDI, com escopos cronológicos e velocity zero silenciosa sem ocultar a nota escrita do Espera. Nove fixtures e teste integrado MIDI/guia; fonte preservada. Ver documento 30. Depois de validar/integrar a sequência musical e confirmar critérios técnicos, avançar ao guia visual da Etapa 3. Não aguardar hardware para tarefas independentes.

## Preparação da Etapa 3 — 0.9.21/code30

Guia ganha marcas D/E, intensidade numérica representada no tom amarelo e barras de duração restante por pauta; preserva ambas as mãos e verde prioritário para MIDI. Não adiciona exigências ao Espera. Metadados e regressões em documento 31. Entrega depende da validação da dinâmica do PR #18 e da própria CI Android. Prosseguir depois com auditoria de modos, desconexão e notas presas.

## Etapa 3 — 0.9.22/code31

Etapa 2 encerrada no escopo técnico (#8) após PRs #15–#18 e nove fixtures; limites remanescentes explicitados e vinculados à Etapa 5/#10 e aceite físico/#11. PR #19 do guia integrado em 5a98f2547b7435c2f1311e1f2c68cd33c262e205 após build 38945148110 e emulador 38945148122 aprovados.

Correção preparada para rearmar Espera após reconexão, isolar mensagens antigas por geração e impedir avanço na biblioteca/background. Self-test debug usa Activity/Handler reais e preferências isoladas; resultado Android depende da CI da própria entrega. Documento 32 explica testes e limites. Próxima tarefa: controles de canal/desligamento e notas presas; depois Etapa 4.

## Etapa 3 — 0.9.23/code32

Tratamento de entrada CC120/121/123 com isolamento de canais, teclas e vozes retidas por pedal. Regressão JVM 12.672 asserções aprovada e self-test Android ampliado, pendente da própria CI. Documento 32 registra semântica, fonte e limites. Após validar/integrar a sequência MIDI, consolidar escopo técnico da Etapa 3 e seguir Etapa 4 (busca/organização, perfis estáveis, histórico local compatível com backup).

## Etapa 4 iniciada — 0.9.24/code33

Etapa 3 encerrada tecnicamente (#7) após PR #21 integrado em a5592f0395f619a5ee8789463d03f527fa236990; build 38946323401 e emulador 38946323387 aprovados. Hardware segue pendente em #11.

Perfis com IDs estáveis, renomear/avatar/excluir sem renumerar dados de outros perfis e validação nova do backup preparados. Documento 33 descreve compatibilidade, testes isolados e limites. Confirmar CI desta entrega antes do merge; depois implementar busca/organização e histórico por perfil.

## Etapa 4 — 0.9.25/code34

Busca sem distinção de acentos/caixa, filtros, favoritos e coleções por perfil implementados. Chaves portáteis de partituras, metadados limitados/validados e backup v2 compatível. Documento 34 registra UI, testes e próxima tarefa. LibrarySearchTest passou localmente; Android exige LIBRARY_SELFTEST_PASS e UI no commit da entrega. Depois seguir histórico/progresso, antes de fechar Etapa 4.

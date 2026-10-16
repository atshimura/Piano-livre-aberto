# Plano sequencial até a versão 1.0

Plano aprovado nas instruções do Projeto em 16/10/2026. Acompanhamento: [issue #4](https://github.com/atshimura/Piano-livre-aberto/issues/4).

| Etapa | Escopo | Critério de passagem | Situação |
|---|---|---|---|
| 1 | Estabilização, desempenho e fluidez da partitura | Regressões de movimento, pausa, seek, loop e resize; custo por frame reduzido e diagnóstico reproduzível | Em desenvolvimento |
| 2 | Aprimoramento do motor musical e da fidelidade MusicXML | Fixtures musicais, comparação Java/JS e limitações explícitas | Pendente |
| 3 | Aperfeiçoamento dos modos de aprendizagem, reconhecimento MIDI e teclado guia | Regressões dos modos, novos ataques, extras, calibração, pedal e duas mãos | Pendente |
| 4 | Biblioteca, perfis, histórico e progresso de estudos | Busca/organização e progresso local compatíveis com retomada e backup | Pendente |
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

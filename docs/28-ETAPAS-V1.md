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
- Mantém regras pedagógicas, parser, eventos MIDI, dados e assinatura. O teste novo executa render/sync/resize reais em DOM e relógio determinísticos; não mede FPS nem substitui WebView/emulador.

Verificação local: auditoria pública, sintaxe JavaScript e seis suítes JS passaram, incluindo TemporalRenderingTest (sem leituras de viewport ou buscas DOM durante os frames, destaques, resize, pausa, retorno, loop e teardown). A tentativa local das suítes JVM encontrou ambiente sem `javac`; Gradle/Android SDK também não estão instalados. JVM, comparação Java/JS, auditoria MIDI, compilação e emulador deverão ser comprovados pelo Actions do commit desta entrega.

Próxima tarefa: confirmar CI do PR, integrar respeitando proteção de main e obter APK com assinatura permanente pelo workflow confiável. Depois ampliar diagnóstico de frames/carga de partituras longas e lifecycle antes de fechar Etapa 1. Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 permanecem sem teste físico nesta entrega. Não se declara resolvida a microtravada real apenas por reduzir trabalho no código.

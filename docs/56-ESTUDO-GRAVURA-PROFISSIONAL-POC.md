# 56 — Estudo: auditoria da gravura e prova de conceito OSMD (PR #46)

**Situação:** pesquisa/diagnóstico e prova de conceito em **PR draft**, não substituição aceita nem versão instalável distribuída. Base examinada: `main` `15cafe3c8da7c67b9752d14ba0fb6f8f6141177d` (0.9.46/code55). O usuário observou presencialmente que a pauta do Estudo continua amadora apesar dos PRs 20/21/22/23/41. Essa constatação prevalece sobre os antigos testes de geometria.

## Referências profissionais e parâmetros de aceite

- [SMuFL 1.4 — W3C](https://www.w3.org/2021/03/smufl14/): mapa de glifos, cabeças, pausas, acidentes, claves, bandeirolas e ancoragem; [métricas de gravação](https://smufl.formats.music/latest/specification/scoring-metrics-glyph-registration.html) usam a unidade **staff-space**, não contornos improvisados.
- [LilyPond — espaçamento óptico](https://lilypond.org/doc/v2.26/Documentation/essay/optical-spacing): intervalos musicais influenciam horizontalidade, mas hastes, acidentes, colunas e barras exigem correções ópticas; [referência de espaçamento](https://lilypond.org/doc/v2.27/Documentation/notation/horizontal-spacing-overview).
- [OpenSheetMusicDisplay](https://github.com/opensheetmusicdisplay/opensheetmusicdisplay) ([opções](https://github.com/opensheetmusicdisplay/opensheetmusicdisplay/blob/develop/src/OpenSheetMusicDisplay/OSMDOptions.ts)): biblioteca **já presente**, versão fixa 1.9.8, backend SVG e opção `renderSingleHorizontalStaffline`. A Partitura Livre já a utiliza; não foi incorporada biblioteca nova.
- [Verovio](https://www.verovio.org/): alternativa de pesquisa MusicXML→SVG, **não instalada**, cuja licença, tamanho, WebView, custo de integração e semântica temporal exigiriam decisão própria. Não incluir fontes ou código externo sem aprovação.

**Critérios editoriais verificáveis** (não substituíveis por presença de elementos SVG): glifos coerentes de semibreves/mínimas/figuras menores; cinco linhas uniformes; clave G com espiral na 2ª linha de baixo (G4) e F enquadrando a 4ª de baixo (F3); haste/bandeirola na altura/voz correta; vigas primárias e secundárias alinhadas, seguindo `<beam>` explícito; agrupamentos inferidos só na falta da fonte, por fórmula/pulso/voz/pausa; acidentes e armaduras não sobrepostos; pontos e pausas no lugar convencional; ties e slurs distintos, atravessando barras e janelas; quiálteras, grace e linhas suplementares; colunas de notas e duas pautas alinhadas por evento; tamanho/contraste de celular/tablet legíveis. Revisão humana lado a lado **obrigatória**.

## Diagnóstico de código da 0.9.46

| Tema | Achado verificável | Consequência |
|---|---|---|
| Motor do Estudo | `notation.html` envia `study` para `TemporalNotation.render` e `free` para OSMD | O Estudo não usa o gravador existente. |
| Glifos | `temporal-notation.js` tem `drawNote`, `drawRest`, `drawClef`, `drawScoreBeams`, linhas SVG, elipses e caminhos manuais | Formas, espessuras e símbolos permanecem esquemáticos apesar de correções isoladas. |
| Claves | `staffLineY`/`clefAnchor` foram ancorados em G4/F3 no PR anterior | Posição geométrica não garante desenho tipográfico editorial. |
| Hastes/vigas | `inferConventionalBeams`, `collectBeamGroups` e `drawScoreBeams` com tiles de 1160 px | Regras especiais de vozes, uníssonos, cruzamentos e fragmentação precisam de inspeção musical. |
| Acidentes/pontos/pausas | São calculados e desenhados pelo render próprio | Testes anteriores verificam valores/sinais, não colisão visual global nem kerning. |
| Ties, slurs, grace, tuplet | Elementos gráficos próprios por span/tile | Conferir curvas, números, linhas de apoio e cortes entre trechos. |
| Piano grand staff | Pautas em Y fixo; índice de notas e escala vertical comprimida | Espaçamento entre pautas e densidade no horizontal podem ficar artificialmente ampliados. |
| Tempo | `x=origin+beat*pxPerBeat`; scroll pelo relógio da MainActivity | É uma decisão funcional vigente e impede simplesmente adotar espaçamento óptico não linear. |
| MusicXML original vs desenho | `Score.java` (MIDI) e JS (grafismo) são parsers independentes | Necessário mapeamento de IDs MusicXML, beats, pauta/voz e coordenadas OSMD, com testes cruzados. |
| Toque na nota | Identificação existe parcialmente e o usuário reportou falhas | O novo motor precisará hit-test por glifo e validação manual, sem descartar cliques confiáveis já existentes. |

**Capturas reais de referência:** GitHub Actions da `main` em `39079438655` armazena `piano-engraved.png` (Estudo artesanal) e `piano-free-reading.png` (mesma Ode à Alegria, OSMD convencional). Essas duas imagens são **antes versus referência dentro do próprio app**, e não provam que OSMD contínuo cabe no Estudo.

## Prova isolada (não disponibilizada como Estudo definitivo)

No PR #46, **somente por toque prolongado em “Partitura Livre” quando o Estudo está aberto**, `NotationView` recarrega o MusicXML original no **OSMD já empacotado**, backend SVG, opção horizontal `renderSingleHorizontalStaffline:true`. O mesmo gesto retorna ao renderizador temporal. A abertura normal de cada partitura **sempre repõe o render temporal antigo**. O relógio MIDI/espera, perfis, backups, importações, modo Livre, tela da biblioteca e estudo habitual não são modificados. O preview é *estático* para comparação: intentionally não valida seek contínuo, cursor anterior à nota, loop nem clique em cabeças, portanto é **inadequado para produção** sem nova implementação.

O smoke no emulador captura **antes** (`piano-engraved.png`), **prévia OSMD** (`piano-osmd-poc.png`) e a **referência OSMD tradicional** (`piano-free-reading.png`), além de `piano-engraving-poc.json` com SVG node count, largura/altura, tempo do primeiro render e amostras beat→posição de `graphic.measureList` (amostras não equivalem a um mapa aprovado de notas individuais). Esses arquivos estarão no artefato do Actions do **SHA exato** do PR se o teste finalizar. Não usar capturas sintéticas/fabricadas para alegar aprovação.

## Mapeamento que ainda precisa ser comprovado

Tabela proposta para cada **evento musical original**, inclusive dois eventos simultâneos: identidade de parte/compasso/voz/índice XML, onset e duração em semínimas, altura MIDI e notação escrita, tie/start/stop, `GraphicalNote` correspondente, `getBoundingClientRect` da cabeça após zoom, `x` de sua pauta, cursor no beat e hit-test. Verificar monotonicidade dos beats, colunas simultâneas, inconsistências de parser, repetição/casas, graça, tablaturas e Compassos 2/4, 3/4, 4/4, 6/8, 9/8 e irregulares. Somente então tentar cursor fixo, deslocamento horizontal e toque confiável.

## Viabilidade/desempenho: limites e decisão pendente

OSMD 1.9.8 imprime por convenções de gravura; **distância entre eventos não é estritamente linear em beats**. O Estudo atual exige linearidade estrita e rail/cursor próprios. Existe uma decisão de produto pendente: aceitar deslocamento não uniforme da partitura, com relógio MIDI **inalterado** e cursor seguindo um mapa de eventos interpolado, em troca da legibilidade editorial? Ou exigir proporcionalidade linear estrita e desenvolver outra estratégia de compensação óptica? **Não escolher implicitamente**.

SVG horizontal da partitura inteira pode criar um desenho largo demais ou renderização cara em scores longos. Medir primeiro render, SVG nodes, PSS WebView/aplicativo, scroll/frames, zoom/resize, retorno do background em ambas as telas, usando **as mesmas partituras** e dimensões. Investigar janela por sistemas ou segmentos OSMD sem partir ties/vigas. Não interpretar redução de elementos ou ausência de exceções como aceite visual. Emulador não substitui Realme 7 5G, Lenovo Tab M9 e Yamaha P-45, além da revisão editorial pelo usuário.

### Matriz de regressão requerida antes da troca

**Ainda a ampliar:** fixtures MusicXML originais/sintéticas de G/F e suas mudanças; 2/4,3/4,4/4,6/8,9/8 e irregulares; colcheias/semicolcheias/fusas, beaming explícito versus implícito; vozes opostas, acordes de segundos, uníssonos, acidentes locais, síncopes, pausas/pontos; ties, slurs, tuplets, grace, ledger lines e mudança de andamento; 1–2 pautas/mãos; casos longos, zoom, teclado/altura, plano de reprodução MIDI, Espera e seleção de trecho. Registrar captura antiga, OSMD de prova, referência editorial do **mesmo trecho**, pontos de diferença e correções. Não trazer imagens de partituras protegidas ao repositório.

## Estado e próximos passos

A presente entrega é **prova de conceito em branch de pesquisa**. CI/auditoria devem ser verificados no SHA final; enquanto o PR for draft, **não declarar item 1.2 concluído**, não mesclar na `main`, não publicar APK de distribuição, não marcar checkbox da issue #34. Próxima etapa depende de avaliação visual/temporal e escolha sobre espaçamento óptico. A reorganização da Biblioteca continua fora de escopo.

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

## Decisão do usuário — espaçamento óptico aprovado

Em 17/10/2026 (diálogo da pesquisa) o usuário **autorizou** a substituição da proporcionalidade gráfica estrita por **espaçamento óptico profissional**, com as condições de que o relógio musical, as notas corretas no cursor nos instantes correspondentes, USB-MIDI, Espera, teclado, trecho, identificação ao toque e dados permanecem preservados. Isso **não significa aceite visual ou autorização para baixar outro motor/fonte**. As regras temporais continuam existentes na `main` e no Estudo comum do PR draft.

### Evolução da prova de conceito do PR #46

O arquivo `optical-time-map.js` é um módulo offline novo **de código próprio** que extrai `getAbsoluteTimestamp()` (em frações de semibreve, convertido para semínimas por `×4`) e `PositionAndShape.AbsolutePosition` (unidades gráficas OSMD, convertidas para SVG px por `×10`) de cada entrada de pauta, preservando um vetor separado de notas gráficas. Múltiplas vozes e mãos no mesmo onset formam **uma âncora temporal**; eventos seguintes podem ter distâncias ópticas diferentes. A interpolação por segmentos e sua inversa, limites e rejeição de regressos horizontais são cobertas por `OpticalTimeMapTest.js`. O `halfTone+12` de cada nota gráfica é apenas **estimativa diagnóstica** de MIDI e não confirma transposição nem substitui `Score.java`.

Na **prévia opcional**, `PianoNotation.seek` recebe os beats enviados pela mesma `MainActivity` e interpola a posição óptica, de modo que a linha de referência permaneça fixa e o movimento use `requestAnimationFrame`, sem alterar ticker nativo ou mensagens MIDI. Foi adicionada uma comparação experimental da WebView com camada de software para investigar a falha visual de corte do SVG em dispositivos Android; a camada é restaurada ao voltar à apresentação original. O emulador deve demonstrar avanço de beat/scroll, tirar capturas antes, durante e depois, observar a continuidade das duas pautas, restaurar a camada e repetir todas as regressões anteriores. Esta experiência passou a oferecer um **hit-test exploratório de cabeça de nota** por coordenadas `GraphicalNote` e tooltip de altura estimada (não respeita ainda grafia enarmônica nem transposição); os testes unitários cobrem nota correta, região vazia, alturas sobrepostas e ambas as pautas. **O toque sobre uma cabeça específica foi executado por ADB no emulador** e identificou a primeira nota Mi4 da pauta superior; continuam ausentes testes com acordes, vozes, acidentes, notas de graça e hardware físico, além de selecionador gráfico de loops e destaque por mão: não promover para produção.

**Condição de integração:** ainda exige resolver visualmente a pauta cortada e sua eventual causa, provar eventos individualizados e correspondência com MIDI, toque sobre cabeças, Espera/trechos/loop, movimento fluido sob alteração de tempo e benchmarks físicos e emulador; o PR continua draft sem versionCode/release novos até cumprir esses requisitos.

### Resultados observados de interface no SHA anterior do PR

O [build de pesquisa](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39084494689) e o [emulador](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39084494695) passaram no head `7a71e1c`. O artefato contém `piano-osmd-poc.png` e `piano-osmd-poc-moving.png`, com as **duas pautas visíveis no início e após avanço do Contínuo na Ode à Alegria**, sem o corte anterior observado nesse trecho. A camada de software da WebView foi usada **somente na prévia** e restaurada depois. Uma trajetória de movimento guiada pelo relógio nativo mudou posição musical e scroll; não foi medido FPS real, consumo de RAM ou tempo sustentado de software rendering. Foram observados ~224 ms de render inicial, 876 nós SVG, 63 âncoras temporais e 81 notas gráficas de um arquivo com 16 compassos no emulador (uma execução, não benchmark representativo). A captura é evidência de avanço, não de precisão visual validada nota-a-nota. A abordagem requer limites para scores extensos, teste físico e avaliação editorial.

A implementação posterior de identificação por toque teve [build/JVM/JS/MusicXML/MIDI](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39085410526) **success** (`OPTICAL_TIME_MAP_PASS checks=24`, `OSMD_POC_CONTRACT_PASS checks=17`) e [emulador](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39085410506) **success** no head de código `f41adfa`. A bateria usou posição da `GraphicalNote`, limites reais do componente Android WebView, toque `adb shell input tap` sobre a nota e confirmou `POC_NOTE_TAP` com MIDI estimado 64, pauta 1 e compasso 1. A captura `piano-osmd-poc-note-tap.png` no artefato mostra o tooltip **Mi4**. Tempo de render da mesma Ode nessa execução: ~386 ms (amostra isolada; variação expressiva frente a 224 ms anteriores). Memória PSS não medida com confiabilidade; não afirmar desempenho adequado em aparelho real. Os resultados são de head de código; CI do **head definitivo** deverá ser reconfirmada.

## Estado e próximos passos

A presente entrega é **prova de conceito em branch de pesquisa**. CI/auditoria devem ser verificados no SHA final; enquanto o PR for draft, **não declarar item 1.2 concluído**, não mesclar na `main`, não publicar APK de distribuição, não marcar checkbox da issue #34. Próxima etapa depende de avaliação visual/temporal e escolha sobre espaçamento óptico. A reorganização da Biblioteca continua fora de escopo.


## Retomada de 18/10/2026 — reconciliação de eventos, hit-test e relógio

Recuperação a partir de `15edfe56bb2b40956ae7243ee1a310d8ef7e02da`, com build [39085951671](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39085951671) e emulador [39085951669](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39085951669), ambos `success` **no commit antigo**. O arquivo de evidências do emulador contém `piano-engraved.png`, `piano-osmd-poc.png`, `piano-osmd-poc-moving.png`, `piano-osmd-poc-note-tap.png` e `piano-engraving-poc.json`. São capturas reais, mas não aceite visual de produção.

**Correções implementadas nesta continuidade (a confirmar pela CI do HEAD final):**

- `NotationView` envia um vetor JSON de eventos **já interpretados por `Score.java`** apenas ao modo de prova `engraving-poc`: onset, comprimento, MIDI, pauta, medida e voz original, além do início/fim de tie. Sem alterar o modelo persistido, a saída USB-MIDI ou os dados de usuário. A serialização possui limite explícito de 20.000 eventos e diagnóstico em log.
- `OpticalTimeMap.reconcile` cruza cada `GraphicalNote` com eventos Android mediante beat (milésimo), pauta, compasso e altura MIDI. Associa somente correspondências **unívocas**: vozes em uníssono, duplicatas, notas gráficas sem ataque (como continuações de tie) e divergências entre parsers ficam sinalizadas como `ambiguous`/`unmatched`. Não inventa voz por ordem espacial, não infere mão por altura, nem reescreve MusicXML. A propriedade `midiEstimate` do OSMD permanece **diagnóstica** e não é autorizada como som de referência confiável.
- O tooltip de toque expõe `MIDI conferido` ou `MIDI não confirmado`; o log `POC_NOTE_TAP` inclui `nativeMidi`, `sourceEventIndex` e `mapped`. `POC_MIDI_MAPPING` registra contagens e o emulador exige tocar uma nota que coincida com o motor Android. Após `osmd.render()` de resize, o hit-test é associado novamente ao SVG e a identidade musical é recalculada.
- O movimento óptico mantém um único ciclo `requestAnimationFrame` durante execução e corrige fase pequena em relação ao relógio nativo sem reiniciar todo quadro; saltos, mudanças de andamento, pausa e loop reancoram o beat. Eliminado limite de 8.000 quadros que poderia paralisar partituras longas. Ainda faltam medição de drift real e FPS/PSS confiáveis em hardware.
- Testes JS cobrem correspondência correta, mãos/pautas, divergências de beat/compasso, duplicidade polifônica, invalidação de mapeamento anterior, dados inválidos e arredondamento; contrato estático e teste Android com toque físico simulado por ADB foram ampliados. **Não confundir regressões automatizadas com revisão visual.**

**Seleção de trechos, fase experimental:** o botão já existente `Trecho ↻` agora permite apontar duas cabeças OSMD previamente reconciliadas com `Score.java`: o primeiro toque escolhe o início, o segundo escolhe a última nota e acrescenta a duração nativa à borda final. O mesmo callback `AndroidNotation.onLoopSelected` aplica e persiste o loop no controlador do Estudo, sem modificar MusicXML. Toques em alturas não confirmadas são recusados e rotulados. O emulador passa a ensaiar dois toques físicos via ADB e capturar `piano-osmd-poc-loop.png` (resultado ainda depende de CI). Não há ainda as alças, realce visual persistente, seleção contínua por arrasto ou cobertura completa de ties e vozes duplicadas.

**Pendências de aceitação:** resultado dos Actions do HEAD desta implementação; seleção gráfica por arrasto/alças e preparo visual da Espera sobre a camada OSMD (ainda não integrados à prévia); destaque por mãos e acordes de múltiplas vozes; ties, acidentes, transposição, repetições e compassos irregulares; partituras grandes, zoom/rotação, consumo e fluidez; evidências físicas Realme 7 5G, Lenovo Tab M9 e Yamaha P-45. O recorte das pautas deve ser reavaliado com repertório adversarial, embora a captura da Ode na POC apresente as duas linhas. Manter PR #46 **draft**, sem merge, sem release, sem marcar item 1.2 concluído até avaliação editorial pelo usuário.

### Preparação do Espera na prévia OSMD — continuação experimental

A ponte Android `NotationView` passou a encaminhar `startWaitPreparation(durationMs)` e `cancelWaitPreparation()` também ao modo opt-in de gravura. O Java continua sendo autoridade do período configurável de 1 a 30 segundos; o OSMD apenas exibe a contagem regressiva e a barra de progressão visual, descartando `requestAnimationFrame` no cancelamento, no término ou na troca de partitura. Log `POC_WAIT_PREPARE`/`POC_WAIT_COMPLETE`, teste de contrato e sequência no emulador com imagem `piano-osmd-poc-wait.png` foram adicionados ao workflow; a aprovação só pode ser atribuída aos Actions do HEAD final.

**Limite:** a sobreposição experimental é uma contagem visual e ainda não reproduz a trajetória espacial gradual do Estudo antigo durante a preparação; o reconhecimento MIDI, notas extras e avanço por acordes permanecem controlados pelo Android. Não confundir a preparação visual com prova de fidelidade do cursor à polifonia, sincronismo real USB-MIDI ou qualidade editorial final.

## Auditoria adicional de desempenho no Estudo óptico — 18/10/2026

O trabalho continuou **sem necessidade de uma decisão adicional do usuário**. Foi incluída instrumentação **local** da prévia para diagnosticar um dos bloqueios reais à integração: custo de SVG horizontal grande, regularidade das animações e uso de memória. O app segue **offline e sem telemetria**.

- `notation.html` emite `POC_OPTICAL_PERF` ao pausar ou desmontar a prévia: quadros JS solicitados/observados, intervalo médio e máximo entre callbacks, intervalos acima de 50 ms, número de nós SVG e largura do conteúdo. **Intervalo do callback de `requestAnimationFrame` NÃO equivale a FPS renderizados pela GPU ou ausência de microtravadas**.
- `emulator_ui.py` extrai `TOTAL PSS` de formatos distintos do `dumpsys meminfo`, inclui antes/depois no `piano-engraving-poc.json` e registra o resumo dos callbacks no mesmo artefato de emulador. Trata-se do **PSS do processo consultado**, possivelmente sem o processo de renderização WebView; são duas amostras isoladas, não prova de ausência de regressão nem limite de memória.
- `OpticalTimeMapTest.js` acrescentou corpus **sintético de 12.000 entradas musicais** com intervalos ópticos variáveis e testes de interpolação/inversão em diversos pontos. Reporta tempo do mapa e heap do Node, com limite de execução para detectar regressão algorítmica extrema. **Não é benchmark de diagramação MusicXML de 12.000 notas no Android**.
- Manter `main` 0.9.46 sem novas versões. O PR #46 permanece draft. Verificar no GitHub Actions o SHA específico deste acréscimo antes de citar sucesso; não substituir ensaio em partituras reais longas, zoom, polifonia adversarial nem hardware #11.

**Pendências de produção:** gravura profissional sem recortes em todo o corpus; desempenho/memória real no Realme 7 5G e Lenovo Tab M9; coerência de playback/MIDI no Yamaha P-45; grafia acidental e enarmonia, vozes concorrentes, tie/slur, rolagem em partituras extensas e gesto de seleção completo. A aprovação do usuário para espaçamento óptico não equivale a aceite editorial da interface.


## Ensaio de gravura longa e redimensionamento conservador (18/10/2026)

A pesquisa foi ampliada no mesmo PR #46 **sem modificar a interface padrão ou os dados do usuário**:

- **Reutilização de SVG:** a prévia OSMD agora distingue redimensionamento apenas de altura (teclado guia aberto/fechado) de mudança efetiva de largura. Se a largura permaneceu igual, mantém o DOM SVG, posições gráficas de cabeças, mapa de evento reconciliado e estado de execução. O relógio nativo, a rolagem e os controles continuam separados. Re-diagramação do OSMD é mantida quando a largura muda. O cenário Android verifica o diagnóstico `POC_RESIZE_HEIGHT_ONLY` e que o número de renderizações completas não aumenta ao alternar teclado.
- **Corpus público autorizado e comprimento real:** o teste `emulator_ui.py` passa a abrir a `Gymnopédie nº 1` da biblioteca incluída (partitura mais longa e polifônica), capturar `piano-temporal-long.png` da pauta original, `piano-osmd-long.png` da prova horizontal e `piano-osmd-long-moving.png` durante playback no WebView Android. Exige pelo menos 40 compassos, 50 notas gráficas e 1.000 elementos SVG; salva `piano-osmd-long.json` com medidas de render e intervalos de callbacks de animação. São **critérios estruturais preliminares**, não juízo de gravura editorial; o teste não mede FPS renderizados nem estabilidade prolongada.
- **Regressão crítica:** os cenários anteriores para Espera, USB/MIDI, loop por cabeças OSMD, toque identificado, teclado guia, biblioteca e restauração do renderizador temporal permanecem. Não alterar `main` nem publicar nova release enquanto faltarem review visual, cobertura adversarial e desempenho físico.

**Resultados e medidas:** consultar os dois workflows do **HEAD exato** do PR após execução e seus artefatos. Falha do SVG longo, recorte ou irregularidade observada é bloqueante à promoção do protótipo. Em testes Android, não interpretar tempo de callback ou PSS de um só processo como validação plena de fluidez.

## Falha visual identificada na Gymnopédie e correção de ajuste vertical

A auditoria da [execução de emulador 39124401337](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39124401337) sobre a **Gymnopédie nº 1** (arquivo MXL público autorizado, 78 compassos) encontrou uma **regressão editorial real apesar da CI verde**: o OSMD horizontal produziu SVG de **7749 × 382 CSS px**, mas a área da partitura tinha apenas **196 CSS px de altura**; a captura `piano-osmd-long.png` mostrou **somente a pauta superior**, com a pauta inferior fora da viewport. A renderização durou ~**1137 ms**, criou **4925 elementos SVG** e reconciliou **455 de 469 cabeças gráficas** com eventos nativos (14 gráficas sem correspondência inequívoca; não ignorar esse desvio). Durante o movimento, os callbacks JS apresentaram média **46,46 ms** e 37 intervalos superiores a 50 ms em amostra com 91 callbacks. Esses números são de Android WebView no emulador, não de aparelhos físicos nem FPS de composição real.

**Correção restrita ao modo de prova:** `notation.html` passou a aplicar `fitOpticalSvg` à SVG do OSMD, preservando `viewBox`, glifos e identificadores, com fator `displayScale = min(1, (viewportHeight-8)/svgOriginalHeight)`. O mesmo fator é aplicado a `x(beat)` e ao deslocamento inicial do cursor, mas **não** à duração/altura dos eventos musicais. Coordenadas de toque são convertidas pelo `viewBox` já dimensionado. Alterações só de altura reaplicam o fator sem rediagramar o MusicXML; alterações de largura recriam o SVG e o mapeamento nativo. A saída MIDI, seleção de mãos, dados e a Partitura Livre não foram alterados.

O teste de emulador exige que o SVG de 78 compassos apresente `displayScale<1` e `height<=viewportHeight` após adaptação; a documentação **não considera essa melhoria aprovada até examinar captura real posterior e CI do commit final**. Mesmo com duas pautas visíveis, ajustes excessivos podem prejudicar legibilidade de números, acidentes e ligaduras; a revisão visual e benchmarks de software rasterizado seguem obrigatórios.

### Inspeção da correção vertical e teste de toque com escala

A [execução Android 39125229195](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39125229195) do commit `8d4e2d8` terminou **success**. O artefato inclui `piano-osmd-long.png` com as **duas pautas visíveis em captura real do emulador** para a `Gymnopédie nº 1`, em contraste com a captura anterior que mostrava apenas a pauta superior. O fator de exibição aplicado foi **0,4927**, com dimensão SVG visível **3818 × 188 CSS px**, altura disponível 196 px (antes: 7749 × 382). O MusicXML original não foi alterado, os 78 compassos se mantiveram, o render levou ~**1303 ms** em uma execução, e 455 das 469 cabeças gráficas correspondiam univocamente a eventos MIDI/Java; 14 divergências ainda demandam análise musical. A amostra de animação longa registrou 148 callbacks, intervalo médio **30,11 ms**, pico **113,4 ms** e 22 intervalos acima de 50 ms, contra média 46,46 ms e 37 acima de 50 ms em outra execução sem zoom; **não se trata de comparação controlada de FPS da GPU ou prova de fluidez**.

O teste Android foi ampliado para tocar uma **cabeça de nota dentro do SVG já reduzido**, conferir pauta/altura contra o evento MIDI reconciliado e registrar `piano-osmd-long-tap.png`. Até a CI do commit que introduz esse cenário encerrar, **não declarar o toque escalonado validado**. Qualidade editorial continua dependente de inspeção humana nos dispositivos reais e de expansão do corpus com alterações de clave/armadura, vozes e vigas complexas. A seleção por arrasto/alças, pauta de cabeçalho fixa e movimento de preparação do Espera na nova camada ainda não foram concluídos.


### Continuidade 18/10/2026 — redução de escrita gráfica redundante

A prévia OSMD agora evita atribuições repetidas a `viewport.scrollLeft` quando a posição solicitada difere menos de **0,5 pixel CSS** da posição vigente. O cálculo de beats e a fonte temporal nativa não mudaram; a proteção apenas suprime atualizações gráficas que não resultam em deslocamento perceptível. O teste de contrato verifica a presença do limite, e a CI/emulador do commit final precisam ser consultados antes de afirmar melhoria de desempenho. Esta otimização **não equivale** a reduzir custo de rasterização do grande SVG nem corrige por si só alterações ópticas, colisões ou fluidez física. Manter PR draft e a versão pública intacta.


### Diagnóstico incremental de divergências OSMD × Score.java (18/10/2026)

Na mesma branch experimental, `OpticalTimeMap.reconcile` passou a registrar categorias distintas para cabeças não reconciliadas: `invalidGraphical` (metadados gráficos incompletos), `noNativeCandidate` (ausência de evento nativo com mesma chave beat/pauta/compasso/MIDI), `ambiguous` (mais de uma voz/cabeça candidata) e `beatDisagreement` (diferença temporal após chave quantizada). São incluídas até 20 amostras estruturadas no `POC_MIDI_MAPPING`, evitando logs ilimitados em partituras longas. Os testes Node cobrem divergência de altura, duplicação polifônica, gráfico inválido, limite de amostragem e limpeza após nova reconciliação. **Não houve correção automática das 14 cabeças da Gymnopédie**: a categorização serve para investigar a causa sem associar uma nota errada ao MIDI. Confirmar build e emulador no SHA da branch; nenhuma revisão editorial ou teste físico ocorreu nesta alteração. PR permanece draft.


**Correção posterior:** categorias diagnósticas agora são mutuamente exclusivas por cabeça de nota. Duplicações gráficas sem evento nativo contam como `ambiguous`, sem também incrementar `noNativeCandidate`; cada nova reconciliação limpa o diagnóstico anterior. Um teste garante que a soma das categorias corresponde ao total de cabeças não reconciliadas. Verificar Actions no novo SHA `b37b8a2` antes de atribuir êxito; não inferir correção musical das 14 divergências somente pela classificação.


## Continuação 18/10/2026 — preparação gradual do Espera no protótipo

Os Actions do commit `6a9a53ff69b2ed1b878d337138734453dc64f953` foram consultados: build `39131105661` e emulador `39131105654` encerraram com conclusão `success`. Esses resultados não validam os commits posteriores.

A prévia OSMD passa a mostrar apenas a indicação **Preparação**, sem número regressivo. Durante o intervalo definido pela camada Android, a partitura recebe um deslocamento horizontal de apresentação inicial (até 100 px CSS ou 18% da largura visível) que decresce suavemente até zero, por `requestAnimationFrame` e curva smoothstep. Ao terminar ou cancelar, a transformação é removida. Não se altera beat, MIDI, reconhecimento, tempo configurado, XML, perfil ou persistência; a pauta temporal convencional mantém seu comportamento anterior. Testes de contrato verificam ausência do contador numérico, avanço visual e limpeza no cancelamento.

**Limitação importante:** trata-se de uma primeira aproximação visual da preparação, **não aceite funcional**. O deslocamento é espacial e ainda exige capturas e ensaio de sincronização com os comandos de seek nativos, pausas, mudança de orientação, abertura do teclado e eventual cancelamento; confirmar também se o cursor e as notas terminam no lugar correto. A revisão visual no emulador e em hardware físico ainda é obrigatória. Não promover o PR #46 a produção por esta alteração isolada.

O teste Android foi posteriormente fortalecido para registrar amostras `POC_WAIT_FRAME` em progressos sucessivos e exigir deslocamento horizontal decrescente sem retorno durante a preparação. Captura `piano-osmd-poc-wait.png` permanece no cenário; os resultados dependem dos workflows do novo HEAD. Esses logs aferem movimento solicitado pelo JavaScript, não pixels efetivamente compostos pela GPU.

A execução de build `39131895533` da versão intermediária falhou no teste de contrato OSMD: a asserção buscava literalmente `score.style.transform='none'`, enquanto o código utilizava a expressão equivalente `document.getElementById('score').style.transform='none'`. O teste foi corrigido para examinar a instrução real no commit `a60c216b3f0d894f3bbaf2aa2e1ee33d7501e9d8`. A falha de CI não foi ignorada nem o teste desativado. **Ainda não atribuir sucesso ao HEAD após esse ajuste.**


## Continuação técnica — identidade escrita e enarmonia no Estudo óptico (18/10/2026)

Os Actions do commit `654c28f9fd6b56314b0130019abf1dbab53f89b1` terminaram **success** no build `39132027606` e emulador `39132027588`. O log de emulador reportou posições de preparação aproximadas 100 → 82,1 → 49,48 → 15,51 → 0 pixels CSS. São amostras de animação JavaScript, não medição de FPS de composição ou avaliação visual.

Nesta evolução, `Score.Event` preserva `writtenStep`, `writtenAlter`, `writtenOctave` e `sourceNoteIndex` (posição do elemento-filho MusicXML dentro do compasso), além do MIDI preexistente. Na ponte experimental Android→OSMD, esses campos seguem em JSON **somente quando a prévia OSMD está habilitada**; o MusicXML original e a semântica de MIDI não são modificados. `OpticalTimeMap.reconcile` transporta os atributos escritos exclusivamente para uma cabeça gráfica **univocamente conciliada**. O tooltip prefere o nome escrito real (ex.: Dó♯4 versus Ré♭4) e não converte MIDI em uma grafia presumida. Cabeças sem correspondência e grafias inválidas são mostradas como **Grafia a conferir**, sem aprovação fictícia. A ponte conserva `xmlNoteIndex` para auditorias posteriores de fonte e voz.

Os testes JVM incluem um MusicXML sintético com Dó♯4 e Ré♭4 de MIDI idêntico e texto distinto; os testes JS abrangem enarmonia, acidentes duplos, oitavas e recusa de dados inválidos. O emulador foi ampliado para conferir nomes efetivos e identidade XML após toque ADB. O relatório `POC_LONG_MAPPING`, limitado a 20 exemplos, passa a expor categorias de divergência da Gymnopédie nos logs do workflow. **Esses novos checks aguardam CI do HEAD correspondente.** Ainda não foram investigadas individualmente as 14 notas, nem integrada a nova pauta como padrão; seguimentos de tie, vozes sobrepostas, accidentals e repetições precisam de ensaio específico. Sem aceite físico ou editorial, manter PR #46 em draft.


### Conservação de espaço editorial sem distorcer glifos

A rotina experimental `fitOpticalSvg` recebeu um teste conservador de **recorte apenas de margens externas vazias** do SVG, com base em `getBBox()` e folga de 12 unidades do SVG para preservar linhas, acidentes e marcações. O recorte é aplicado somente quando encontra pelo menos 8% de espaço total recuperável; caso contrário mantém o `viewBox` original. Não se alteram coordenadas horizontais nem o MusicXML. A escala óptica, os alvos do toque e o cursor continuam usando a mesma transformação de visualização; resize somente vertical reutiliza o recorte calculado. O relatório `POC_OSMD_READY` agora apresenta `croppedSvgHeight`, além da altura natural e da escala.

**Critério de aceite pendente:** comparar screenshots reais (inclusive Gymnopédie, escalas de dispositivos e alternância do teclado), confirmar ausência de glifos cortados e avaliar efetivo aumento de legibilidade. Um `getBBox()` pode incluir elementos não musicais ou falhar, por isso há fallback sem recorte. Não afirmar ganho de leitura ou desempenho só com o teste de contrato; CI do HEAD ainda deve ser verificada. O render temporal de produção não foi modificado.


### Auditoria de vozes e prevenção de toques ambíguos

A documentação de API do OSMD (GraphicalVoiceEntry → parentVoiceEntry → ParentVoice.VoiceId) foi examinada. A extração do protótipo passou a ler o ID de voz **quando ele existe**; `OpticalTimeMap.reconcile` agora admite associação de uníssonos polifônicos somente se há igual número de cabeças/eventos nativos, todos têm IDs de voz inteiros distintos e existe bijeção exata por voz, compasso, pauta, beat e altura. Caso contrário, mantém `ambiguous`. Uma única cabeça para duas vozes nunca é promovida automaticamente a duas identificações. O `hitTest` retorna `null` em toques exatamente sobrepostos/equidistantes, evitando atribuir arbitrariamente uma das cabeças. Testes de voz correta, voz trocada, duplicação e alvo de toque ambíguo foram adicionados. **Não houve migração de dados ou mudança no motor MIDI.**

Fontes da interface existente: [OSMD Voice.VoiceId](https://opensheetmusicdisplay.github.io/classdoc/classes/Voice.html) e [VexFlowVoiceEntry.parentVoiceEntry](https://opensheetmusicdisplay.github.io/classdoc/classes/VexFlowVoiceEntry.html). A presença documental dessas propriedades não prova sua correspondência real em cada MusicXML: confirmar contagens por classe de divergência na Gymnopédie e em partituras com vozes simultâneas após a CI do HEAD. Em caso de grafo de vozes diferente do original, manter estado não confirmado; jamais inferir mão por altura.


## Correção da divergência estrutural em ligaduras de prolongamento — 18/10/2026

Diagnóstico causal após o relatório Android `39132999090`: as **14** cabeças não conciliadas na Gymnopédie se concentram no início de compassos selecionados (10–12, 20–21, 26, 31, 49–51, 59–60, 65, 70). Em `Score.java`, a rotina de fusão de **ties** transforma as continuações em prolongamento de uma nota MIDI iniciada antes, removendo corretamente eventos de novo ataque de `score.events`. O OSMD, corretamente, continua exibindo cada cabeça escrita. Portanto, comparar suas cabeças com **eventos de ataque já fundidos** era estruturalmente incorreto. A ausência das 14 cabeças no vetor de ataque **não constitui, por si só, erro de altura do arquivo**.

**Correção implementada, aguardando CI específica:** `Score.writtenEvents` recebe instantâneos de cada nota do MusicXML **antes da fusão das ligaduras**. Conservar `length`, `beat`, `tieStart`, `tieStop`, `writtenStep`, `writtenAlter`, `writtenOctave` e posição XML originais evita alterar os segmentos gráficos quando `score.events` é estendido para MIDI. Cada instantâneo marca `playbackAttack=false` se é prolongamento de nota já soando; quando não existe início de tie válido, o ataque continua elegível. O bridge da prova envia `writtenEvents` (com fallback de compatibilidade para Score criado sem esse vetor) e `OpticalTimeMap` reconcilia cabeças **escritas** independentemente de reataques MIDI. Tooltip indica **Ligadura: sem novo ataque MIDI** e o início de trecho recusa uma cabeça de continuação sem ataque. Nenhum evento extra é enviado à porta USB-MIDI e não se modifica a contagem de notas esperadas no Espera.

Testes: fixture JVM MusicXML de Fá♯ ligado por dois compassos exige **duas cabeças escritas, somente um ataque e duração musical estendida**; JS exige ambas as cabeças reconciliadas com flags diferentes; contrato de bridge e emulador da Gymnopédie exigem correspondência de **todas as cabeças gráficas**. Se a CI constatar exceções restantes, investigar caso a caso com amostras de eventos por onset, sem forçar associação. Ainda faltam revisão editorial, contexto fixo, interação completa e ensaios físicos. **Não promover o PR a produção somente com esta correção.**


### Reversão do recorte SVG por falta de benefício

As execuções de emulador [39133183309](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39133183309) e [39133561751](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39133561751) falharam ao encerrar o cenário longo: antes do comando de retorno à Biblioteca, a UI apareceu na tela inicial do Android. Os logs filtrados disponíveis **não comprovam a causa** (nenhuma stack trace conclusiva foi capturada). O ensaio de `getBBox()` não recuperou altura na Gymnopédie: `cropRatio=1`, escala ~0,493, resultado editorial igual ao anterior. Por precaução, o recorte e as asserções relacionadas foram **revertidos**, restaurando o ajuste SVG anterior, que tinha execuções aprovadas. O cenário Android foi reforçado para logar `dumpsys activity` e falhar em perda do processo após a partitura longa, sem reiniciar o app e ocultar o problema. Verificar a CI posterior para distinguir regressão repetível de instabilidade do emulador. Não declarar melhoria de escala nem resolver a questão de legibilidade com esta experiência.


### Estudo óptico: indicação do próximo ataque por mão

A prévia OSMD recebeu uma camada SVG **não-interativa** de anéis discretos sobre as próximas cabeças com ataque efetivo, ancoradas ao vetor de notas escritas univocamente conciliadas. `OpticalTimeMap.nextCueHeads` escolhe por onset original e **número da pauta**, nunca por altura MIDI; filtra continuações de tie sem novo ataque e oferece ambas/direita/esquerda. A camada é reconstruída apenas quando muda a seleção do próximo onset ou a raiz SVG, não a cada `requestAnimationFrame`; não intercepta toques e não transmite sons. `NotationView.setStudyHand`, `PianoScreens.displayScore` e `MainActivity.selectHand` propagam a escolha nativa à prévia opt-in (a biblioteca, a Partitura Livre, a lógica Espera e a visualização de ambas as mãos no teclado-guia permanecem inalteradas). Teste JS cobre acordes entre pautas, seleção separada, tie e fim de música; contrato estático e cenário Android com botões Direita/Esquerda/Ambas verificam a comunicação por `POC_HAND_CUES`.

**Ainda experimental:** confirmar no SHA final, comparar os anéis com imagens reais no emulador para garantir que não escondam figuras/acidentes; a representação de notas simultâneas em vozes ambíguas permanece conservadora, e não há avaliação física. A pista visual não substitui o contexto de clave/armadura fixo, nem a seleção de trechos completa.


### Adaptação do toque à escala do SVG

O toque no OSMD deixou de usar raio fixo de 11×7 unidades gráficas quando a pauta é reduzida para caber no viewport. `notation.html` calcula `hitRadiusX` e `hitRadiusY` com base na relação `viewBox/getBoundingClientRect`, com limites para não aceitar posições arbitrariamente distantes. O algoritmo `OpticalTimeMap.hitTest` preserva escolha do menor desvio normalizado e rejeita distância empatada, sem alterar o posicionamento da música, altura MIDI ou o arquivo original. Testes de unidade/contrato verificam alcance do alvo após redução, sem perder a identificação da cabeça correta. O emulador ainda deve confirmar o comportamento sobre acordes e cabeças muito próximas; raio maior não prova interatividade adequada em aparelho físico.


### Medição de geometria de pautas do OSMD (sem mudança editorial)

Para entender por que a Gymnopédie exige escala ~0,493 apesar das duas pautas visíveis, `POC_OSMD_READY` passa a registrar `firstStaffOrigins` (posição vertical OSMD da primeira medida em cada pauta) junto de altura nativa e escala exibida. Esta instrumentação **não move pautas** nem altera MusicXML, glifos ou o cálculo de beats; permite determinar se há espaço excessivo entre pautas antes de experimentar `EngravingRules`. Os parâmetros da biblioteca de gravação não foram alterados, porque compactar pautas sem inspeção musical pode causar colisões de linhas suplementares, ligaduras e dinâmica. Revisar as amostras e screenshots da CI atual para decidir o ajuste editorial.


## Interrupção do emulador no SHA `f1d3dc1` — diagnóstico em andamento (18/10/2026)

A execução `39135829973` falhou em `tests/emulator_ui.py` na busca de `POC_OSMD_READY`. O PR permanece aberto como draft. O artefato `PianoLivre-public-emulator-evidence` existe (1.261.670 bytes), mas contém somente nove arquivos, entre eles `piano-song.png`, `piano-engraved.png` e `window.xml`; **não contém captura OSMD nem logcat**. A captura e a hierarquia mostram “Partitura Livre” visível, clicável e `long-clickable=true`, limites `[1073,201][1409,289]`. Isso confirma disponibilidade visual, mas não confirma entrega do toque.

O log do job termina no traceback e não imprime estado da Activity, PID, mensagens `PianoLivreNotation`, erros Chromium/WebView ou estado do renderer, pois a asserção anterior não coletava esses dados em caso de falha. Portanto a evidência daquela execução **não permite atribuir a falha à camada Java, ao gesto, ao JavaScript, ao OSMD ou ao tempo de renderização**.

Instrumentação diagnóstica foi executada em `b9e4412`. No emulador, o PID da Activity continuou ativo (`3875`), o toque `ACTION_DOWN` chegou ao botão e `OnLongClick` entregou `enabled=true`; `setEngravingProof` despachou com `htmlReady=true`. `osmd.load()` e `osmd.render()` resolveram, e o mapa foi criado (63 âncoras, 81 cabeças). O erro exato foi `TypeError: overlay.replaceChildren is not a function` em `paintOpticalCues` (`notation.html:182`), seguido de `RENDER_ERROR` e fallback. O log Chromium informa WebView **74.0.3729.185**. Portanto, a falha era incompatibilidade de API DOM do `SVGElement`, não gesto, serialização, OSMD, processo encerrado ou tempo de espera.

O artefato `PianoLivre-public-emulator-evidence` da execução `39144272342` contém 12 arquivos, inclusive `piano-osmd-failure.png`, `piano-osmd-failure-window.xml` e `piano-osmd-failure-diagnostics.txt`; a captura mostra a tela de Estudo ainda ativa com a mensagem de fallback, e o diagnóstico preserva Activity/processos/logcat. Correção preparada: substituir `SVGElement.replaceChildren()` por remoção iterativa via `firstChild/removeChild`, disponível na baseline antiga e sem dependências; o teste de contrato impede regressão para a API incompatível. **A correção ainda aguarda build e emulador no SHA seguinte.**


## Correção da incompatibilidade Android WebView e revalidação do mapeamento (18/10/2026)

**Causa confirmada e corrigida:** o primeiro emulador `39135829973` falhava porque o projeto não registrava `logcat` no ponto da asserção. A execução instrumentada `39144272342` confirmou que `ACTION_DOWN`, `OnLongClick`, `NotationView.setEngravingProof`, `osmd.load()` e `osmd.render()` funcionavam. A Activity continuava viva. O erro em `paintOpticalCues` era `TypeError: overlay.replaceChildren is not a function`: o Android WebView fornecido pelo emulador usa Chromium `74.0.3729.185`, que não oferece essa API no `SVGElement`. Substituí a chamada por `while(overlay.firstChild) overlay.removeChild(overlay.firstChild)`, operação DOM compatível, e acrescentei contrato JS contra regressão. O log e as capturas da falha estão no artefato da execução `39144272342`.

**Revalidação no SHA `42ef86d69460c17a0b50fad97692ea7cd48d06a0`:** [build/testes](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39144872504) **success**; [emulador Android](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39144872440) **success**. Contrato OSMD: 54 verificações; mapa óptico: 160 verificações e stress JS com 12.000 âncoras. O emulador passou pela ativação longa, OSMD, toque na nota, mão direita/esquerda/ambas, resize de altura, rolagem óptica, loop de duas notas, preparação Espera, renderizador temporal restaurado, retorno ao primeiro plano e regressões de interface/release.

**Fidelidade Gymnopédie confirmada no WebView:** no log `POC_LONG_MAPPING`, 78 compassos, 188 âncoras e **469 cabeças gráficas; 469 conciliadas, 0 ambíguas e 0 sem correspondência** com os eventos MusicXML nativos. O teste Android também tocou uma cabeça após a redução e aprovou a associação à nota/pauta nativa. Para Ode à Alegria, 81/81 cabeças conciliadas. Isso confirma a reconciliação deste arquivo e cenário; não prova todo MusicXML polifônico importado.

O SVG OSMD da Gymnopédie levou **1.366 ms** no emulador e criou **4.927 nós**. Original: ~7.749×381,6 CSS px. Após ajuste: **3.818×188 CSS px**, escala **0,4927**, viewport de score 738×196 CSS px; as duas pautas aparecem na captura real. Esse ajuste vertical não é aceite editorial: a leitura em tela pequena e contraste/colisões ainda precisam de avaliação humana. O cursor e o mapa beat↔x passaram o ensaio automatizado, mas a clave, a armadura e o compasso ainda não têm contexto fixo à esquerda durante toda a rolagem horizontal.

**Desempenho observado, sem inferir FPS:** amostra rAF da Ode teve média de callback 53,61 ms e máximo 157,2 ms; a Gymnopédie, 33,09 ms e máximo 231 ms (28 gaps acima de 50 ms na amostra longa). São callbacks JavaScript, não quadros compostos pela GPU e tampouco medição controlada entre versões. PSS do processo principal variou de 108.924 para 105.322 KB em duas consultas; o processo renderer WebView pode ser separado, portanto esses valores não medem memória total nem demonstram redução.

O artefato do emulador `39144872440` contém **26 arquivos**, inclusive `piano-engraved.png`, `piano-osmd-poc.png`, `piano-free-reading.png`, `piano-osmd-long.png`, capturas de movimento/toque/loop/Espera, vídeos e JSON. O artefato expira em três dias. A inspeção lado a lado confirma a pauta artesanal atual, a aparência convencional OSMD e a escala longa; nenhuma imagem foi fabricada. A avaliação visual do usuário ainda está pendente.

### Auditoria de contexto e trilho fixo (18/10/2026)

O SHA `f60fbc5d517f315b61645b63fc994f46a5ed255c` passou no [build](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39148592764) e no [emulador Android](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39148592883). O artefato (27 arquivos) foi baixado e verificado; a Gymnopédie marca 78 compassos, 469/469 cabeças conciliadas, 1.167 ms de render, 4.927 elementos SVG, escala 0,4927 em viewport de 738×196. A captura mostra as duas pautas e o tamanho ainda precisa de aceite visual. A sondagem agora seleciona corretamente compasso 1 em ambas as pautas (`Piano0-1` e `Piano0-2`).

O SHA `4c5b654e32d3c60ba8176d7398e822de9d3ff903` passou no [build](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39149525963) e no [emulador](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39149525971). O artefato `12144024626` foi baixado, tinha 27 arquivos e contém os screenshots de pauta antiga, OSMD, leitura livre, Gymnopédie e movimento. O relatório do OSMD mediu Gymnopédie em 78 compassos, **469/469** conciliadas, 1.287 ms, 4.927 nós, dimensão exibida 3818×188 e escala 0,4927. A faixa começou a funcionar e atualizou as medidas 1→2→3 no teste de reprodução, mas a revisão da captura em movimento mostrou **claves duplicadas e faixa no x inicial do SVG (aprox. 30% da tela), em vez da margem esquerda**. A cobertura inicial não detectava o erro geométrico.

O SHA `286defc28c044ffaa3d648fa447def789d5ea809` corrige esse resultado na prévia de pesquisa: a faixa fica em `left:0`, reaproveita brace e glifos nativos, oculta as cópias que rolam e reposiciona o cursor logo após o painel. A instrumentação compara início do SVG, x fixo e x móvel durante reprodução. A seleção é genérica para instrumentos com duas pautas, usa busca binária do compasso e mapeia grupos pelo ID real da medida. [Build `39150432492`](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39150432492) e [emulador `39150432495`](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39150432495) passaram. O artefato `12145276242` foi baixado e contém 27 arquivos esperados. A Gymnopédie teve 78 compassos, **469/469 cabeças** conciliadas, 1.166 ms de render, 4.927 nós SVG e escala 0,4927; o rail ficou em x=0, e o SVG se moveu sob ele. A pauta antiga, a nova OSMD e a Gymnopédie longa constam em capturas reais. A escala resultante ainda requer revisão de legibilidade; a evidência não demonstra aceite editorial.

Após essa execução, foi acrescentada uma fixture **apenas em `src/debug/assets`** com três compassos (4/4 e dó maior; 3/4 e ré maior com clave de dó; 6/8 e mi♭ maior com retorno à clave de sol). O modo de teste é explicitamente gated por `FLAG_DEBUGGABLE` e intent extra. O rail agora registra a medida de origem de cada clave/armadura/compasso, e o emulador exige transições 1→2→3 e inclui PNG/JSON no artefato. No commit `8fa2242e2fa85c4048954c1d9f0994d82e209c35`, o [build completo `39151195201`](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39151195201) passou, incluindo JVM, JavaScript, verificação cruzada MusicXML/MIDI, compilação debug/release e isolamento do APK; publicação beta foi ignorada pelo workflow de PR. O [emulador `39151195211`](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39151195211) falhou após os cenários anteriores passarem: a verificação do título do fixture não foi satisfeita. O artefato `12145706113` tem 25 arquivos; `window.xml` mostra uma mensagem de sessão apontando para `files/imported/upgrade.mxl` ausente. Isso é evidência de estado salvo residual, mas ainda não prova que essa mensagem bloqueou o intent nem por que o fixture não apareceu. No SHA `87f1d77`, o build passou, a sessão foi isolada e o título da fixture apareceu; o emulador avançou até a busca de “Partitura Livre”, que falhou. A hierarquia final disponível lista esse botão, apesar da asserção, então o segundo artefato ainda não resolve a discrepância temporal entre UI e busca do teste. Os SHAs `f847824` e `94236b0` passaram no build e falharam no emulador antes da prévia; Gymnopédie 469/469 e os fluxos de toque, scroll, trecho e Espera passaram. A hierarquia lista “Partitura Livre”. A causa era a verificação de truthiness em um nó folha `xml.etree.ElementTree.Element`: o nó existia, mas era falso por não ter filhos. O SHA `0c87040` corrigiu comparações explícitas com `None`; o emulador confirmou “Tapped Partitura Livre” e abriu a prévia, portanto o seletor está resolvido. A execução seguinte registrou os estados completos: compasso 2 com origem 2 para clave/armadura/compasso; compasso 3 com clave origem 2, armadura e fórmula origem 3. O SHA seguinte limpa logs antigos após renderizar a fixture, seleciona estados pela origem esperada e mantém a exigência de clave 3 no compasso 3. Artefato `12146378167` contém as capturas. Os 65 checks de contrato, 160 de mapa, stress 12.000 âncoras, sintaxe Python/JS e XML passaram localmente. Neste ambiente local não há Gradle nem `javac`; alguns testes adicionais que dependem de `@xmldom/xmldom` também não puderam executar por falta do pacote local. A aparência comum de Estudo segue intacta.

**Estado de produto:** PR #46 segue aberto em draft; `main` permanece em 0.9.46/code55. Não publicar nova beta. Permanecem: contexto esquerdo fixo, revisão editorial e de legibilidade em celulares/tablets, seleção de trecho editável com arrasto/alças, corpus de acordes/vozes/alterações/quiálteras/síncopes, investigação do custo de scroll/render WebView, lifecycle/andamento/loop prolongado, eventual segmentação segura, aceite visual e testes físicos Realme 7 5G, Lenovo Tab M9 e Yamaha P-45. A captura do emulador demonstra funcionalidade técnica nos casos exercitados, não conclusão do item 1.2.

### Sondagem para o contexto fixo

A alteração atual acrescenta `POC_CONTEXT_MEASURES` e `POC_CONTEXT_SVG_ROOT` à prévia experimental. O registro limitado lê, sem mutar nem duplicar o SVG, a geometria `beginInstructionsWidth`, claves ativas, flags de armadura/fórmula e os primeiros grupos do SVG. O emulador exige esses dois registros e os guarda em `piano-osmd-context-probe.txt` no artefato. A finalidade é identificar os grupos de glifos OSMD que podem ser reaproveitados em uma faixa esquerda fixa e acompanhar a instrução ativa por compasso; não renderiza uma interface nova nem declara a pendência resolvida. A decisão de implementar a faixa depende da evidência real do WebView, incluindo uma peça com mudança de contexto.

No SHA `c4b17f1`, build e emulador passaram; a Gymnopédie conciliou 469/469 cabeças. A primeira medida relata `beginInstructionsWidth=8.9` em cada pauta; medidas seguintes, `0.7`. O SVG contém `g.staffline` por pauta (`Piano0-1`, `Piano0-2`) e filhos `g.vf-measure` com ID do compasso. As claves ativas foram lidas (tipo 0 e tipo 1 para as duas pautas da Ode). O teste imprimiu os dados na Action, mas o manifesto de upload omitiu o TXT. No SHA `1d7d2e0`, o manifesto foi corrigido, a extração de modificadores/grupos ampliada, e build/emulador passaram. O artefato de 27 arquivos passou a incluir o TXT, mas continha linhas Android truncadas (4.132 caracteres).

**Revalidação do registro curto (`dbb914c`):** build e emulador passaram. O artefato `12144192238` tem 27 arquivos, incluindo imagens `piano-engraved.png`, `piano-osmd-poc.png`, `piano-free-reading.png`, `piano-osmd-long.png` e `piano-osmd-context-probe.txt` (18.784 bytes). Todos os 24 registros JSON ficaram abaixo de 3.500 caracteres e foram analisados com sucesso. O JSON longo confirma 78 compassos, 469/469 cabeças conciliadas, 1.050 ms de render, 4.927 nós e escala vertical 0,4927. A sondagem da Gymnopédie relata largura de contexto 11,87 na primeira medida e 0,7 em medidas sem alteração.

**Correção de amostragem necessária:** ao revisar os grupos, `POC_CONTEXT_NODES_2` apontava para a medida 2 da pauta superior; o seletor CSS linear não coletava a pauta grave como pretendido. A próxima correção escolhe a primeira `vf-measure` dentro de cada `staffline` e exige compasso 1 em `Piano0-1` e `Piano0-2`. Ainda não se clonou ou fixou glifo algum. Depois dessa verificação, usar as caixas dos modificadores `clefs`, `keysignatures` e `timesignatures` para prototipar a faixa, cobrindo mudanças de contexto em fixture MusicXML.


**SHA `ace0d0f` — evidência inconclusiva:** o build passou, mas o emulador falhou na clave do compasso 3. O probe arquivado ainda continha registros de score anterior e não prova que o evento `POC_OSMD_READY` pertencia à fixture após o gesto. Não atribuir os estados do rail à fixture. O SHA seguinte sincroniza a carga Java `CONTEXT_FIXTURE_LOAD_OK`, o gesto, um novo `POC_OSMD_READY` e o inventário OSMD/SVG; preserva JSON/capturas antes da asserção para identificar a origem do defeito sem enfraquecer o requisito.


**SHA `373972c`:** build passou; emulador provou a carga nativa (3 compassos) e um novo `POC_OSMD_READY` depois do gesto, com log limpo. Os estados do fixture são m1 `1/null/1`, m2 `2/2/2`, m3 `2/3/3` (clave/armadura/compasso); m3 falha porque a clave não muda para origem 3. Artefato `12147393066` guarda o JSON e os probes OSMD/SVG antes da asserção. O próximo commit imprime o probe completo no log e usa os dados do modelo OSMD/SVG para localizar a falta da clave no rail.


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


## Tarefa 01/20 — auditoria visual direta das capturas OSMD (19/10/2026)

**Escopo estritamente documental.** HEAD examinada: `c9e3c2ffb5f2cf46939900bec062f74f05d8f98d`, branch `research/estudo-gravura-osmd-poc`, PR [#46](https://github.com/atshimura/Piano-livre-aberto/pull/46) aberto como *draft* sobre a `main` `15cafe3c8da7c67b9752d14ba0fb6f8f6141177d`. Foram consultados `AGENTS.md`, `README.md`, `docs/10-CONTINUIDADE.md` e este documento **antes** do registro. [Build/testes da HEAD 39161693510](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39161693510) e [emulador da HEAD 39161693531](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39161693531): ambos *success* na data da consulta. O [artefato real 12149460909](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39161693531/artifacts/12149460909) foi **baixado e aberto diretamente**: ZIP com 34 arquivos, dos quais 15 capturas PNG, com dimensões 2160×1080 e tamanhos conferidos contra `piano-visual-evidence.json`. São capturas do emulador Android, **não** ilustrações ou aprovação em aparelho físico. O JSON `piano-visual-evidence.json` inclui um SHA de checkout `310d2ca4…` diferente do `head_sha` `c9e3c2ff…` informado pelo workflow; isso, isoladamente, não prova divergência do código (um workflow de PR pode usar commit de merge de teste). Para rastreabilidade do artefato, considerar o `head_sha` e a execução associados no GitHub Actions, não tomar o campo interno como HEAD da branch.

### Comparação editorial efetivamente observável

| Critério | Pauta temporal anterior | Prévia OSMD no Estudo | Limite da conclusão |
|---|---|---|---|
| Claves | Em `piano-engraved.png` as claves G/F são grandes, esquemáticas e de espessura/estilo distintos dos restantes símbolos. Na captura `piano-temporal-long.png` elas **não estão visíveis no enquadramento fotografado**. | `piano-osmd-poc.png` e `piano-osmd-long.png` exibem claves de sol/fá convencionais e coerentes com a pauta; visualmente a espiral de G incide sobre a segunda linha inferior (G4) e os pontos da de F enquadram a quarta (F3). A faixa esquerda as mantém legíveis nas capturas de movimento. | É evidência geométrica/visual desses enquadramentos, não validação de todas as trocas de clave em MusicXML importado. |
| Cabeças, hastes e proporções | `piano-engraved.png` exibe elipses simplificadas e espaçamento horizontal rigidamente ligado ao tempo; na `Gymnopédie` temporal aparecem semibreves/acordes volumosos, haste isolada e pausas/ornamentos heterogêneos. | Em `piano-osmd-poc.png`, cabeças preenchidas e brancas, hastes retas, pausas, barras e símbolos de compasso seguem uma gramática tipográfica uniforme. Em `piano-osmd-long.png`, OSMD preserva acordes, dinâmicas, arcos e sinais de expressão no mesmo desenho. | Melhora editorial evidente **na amostra**. Não confundir glifos bonitos com garantia de todas as durações, direções de hastes ou vozes. |
| Acidentes e armaduras | As capturas antigas selecionadas não oferecem o **mesmo trecho com alterações** para comparação direta. | `piano-osmd-long.png` mostra dois sustenidos na armadura; `piano-osmd-context-middle.png` exibe sustenidos e bequadro; `piano-osmd-context-end.png` exibe cancelamentos/novos bemóis, bequadros e compasso atualizado. Símbolos são reconhecíveis e o contexto permanece à esquerda. | **Não foi constatada colisão inequívoca** nas amostras; o agrupamento de cancelamentos/armadura em `context-end` é denso e merece teste por escala, não correção presumida do arquivo-fonte. |
| Vigas e bandeirolas | O estilo artesanal exige regressão específica. | Bandeirolas isoladas são visíveis no fixture `piano-osmd-context-middle/end.png`. | **Não há passagem com vigas primárias/secundárias e vozes contrapostas suficiente** nas capturas para afirmar se beaming é correto ou defeituoso. |
| Ligaduras, crescendo e expressão | A `Gymnopédie` temporal inicial mostra texto de expressão visualmente desorganizado e pouca evidência de arcos no trecho visível. | Em `piano-osmd-long.png` e `piano-osmd-long-moving.png`, arcos superiores abrangem sucessões de notas, curvas inferiores ligam notas através de barras, e aparecem `pp`, `p`, `f` e *hairpins*. | Presença e aspecto dos traçados estão comprovados; **não está comprovada** a correspondência semântica de cada *tie* versus *slur*, ponto exato de ancoragem nem continuidade em todos os 78 compassos. |
| Espaçamento e leitura | A `Gymnopédie` temporal exibe cerca de 1–2 compassos na região visível, com cabeças grandes e espaço entre eventos; não representa referência editorial ideal. | A prévia da `Gymnopédie` mostra aproximadamente os compassos 1–15 na mesma região horizontal, com colunas de notas opticamente organizadas, mas significativamente menores. | A comparação confirma **troca entre alcance horizontal e tamanho legível**; os dois layouts não estão normalizados para uma mesma escala física. Não inferir superioridade funcional apenas por caber mais música. |

### Gymnopédie nº 1 — análise prioritária dos 78 compassos

**Medições extraídas de `piano-osmd-long.json`:** 78 compassos; 469 cabeças gráficas e 469 eventos nativos conciliados, sem ambiguidade reportada nesse teste; 188 âncoras temporais; 4.927 nós SVG; primeiro render **921 ms**; SVG apresentado com largura **3.818** e altura **188** CSS px, ante altura nativa de **381,59** CSS px; `displayScale=0.49267420725783834` (~49,3%); viewport de notação **738×196** CSS px. Os valores pertencem **somente a essa execução**; os 469/469 são resultado de mapeamento, não aceite visual. As amostras de callbacks de animação não são FPS compostos nem medição de aparelho real.

**Defeitos/limitações comprovados pelas imagens, em ordem de impacto:**

1. **P1 — legibilidade editorial reduzida, bloqueio para aceite visual:** `piano-osmd-long.png` apresenta notas de acordes, pausas, pontos, dinâmicas `pp/p/f` e ligaduras em tamanho muito pequeno em comparação com `piano-temporal-long.png`. Na captura de 2160×1080, as cinco linhas de cada pauta OSMD ocupam aproximadamente **45 px de altura**, contra ~78 px no temporal, apesar de haver uma distância vertical expressiva entre as duas pautas. A redução a 49,3% é objetivamente medida; a dificuldade para distinguir detalhes pequenos é observável, mas **não foi medida a acuidade de leitores em celulares** nem fixado um limiar universal de tamanho mínimo.
2. **P2 — corte de indicação expressiva no limite esquerdo:** `piano-osmd-long.png` mostra apenas a parte final de **“Lent et douloureux”** (visível como “ent et douloureux”, aproximadamente) ao lado do contexto fixo; em `piano-osmd-long-moving.png` o início continua parcialmente oculto. É um defeito de apresentação/enquadramento observável na captura, **não** prova de texto ausente do MusicXML.
3. **P2 — tooltip intrusivo durante leitura:** em `piano-osmd-long-tap.png` e `piano-osmd-long-moving.png`, a caixa “Fá♯5 · compasso 5 · pauta 1 · MIDI conferido” ocupa parte da pauta superior e encobre notas/arcos/compassos. O tooltip confirma a intenção de identificação, mas sua colocação atual interfere na leitura enquanto visível.
4. **P3 — distribuição vertical requer estudo, sem presumir espaço descartável:** `firstStaffOrigins` registra Y nativos **86,23** e **276,59** (diferença ~190,36 antes de escala). Mesmo reduzida a altura total para 188 px, a captura mantém um intervalo substancial entre pautas, onde também aparecem dinâmicas, acordes e arcos. Há oportunidade de **medir** se o layout pode aumentar o tamanho das notas sem colisões; a mera distância não comprova excesso removível.

**Aspectos que não devem ser classificados como defeitos comprovados:** posicionamento exato dos acidentes em acordes densos; alinhamento ou colisão de vigas de colcheias/semicolcheias, sobretudo multivozes; separação semântica de ties/slurs, quiálteras, notas de graça e linhas suplementares ao longo da obra; correspondência global da notação com a partitura-fonte; desempenho sustentado, flicker, integridade após zoom/orientação e legibilidade/touch em tela física. Os PNG são estados estáticos e não substituem inspeção musical do SVG + MusicXML e testes de interação em resolução real. O fixture de contexto (`piano-osmd-context-start/middle/end.png`, `piano-osmd-context-changes.json`) documenta transições 4/4→3/4→6/8 e mudança de claves/armaduras **nesse cenário sintético**, sem generalizar para partituras arbitrárias.

### Priorização e encaminhamento para a tarefa 02/20

**Prioridade máxima: tamanho útil da pauta da Gymnopédie sem perda do contexto musical.** Na próxima tarefa **separada**, investigar quantitativamente de onde vêm os ~381,59 px nativos (margens, distância das pautas, áreas reservadas para dinâmicas/ligaduras, alcance de notas) e prototipar **apenas se houver espaço comprovadamente seguro** uma escala/altura mais legível, comparando novos PNG com os mesmos compassos e uma captura de zoom/viewport equivalente. Incluir no aceite da próxima tarefa o texto “Lent et douloureux” não cortado e tooltip fora da área crítica de leitura. Preservar glifos/identidade dos eventos, contexto fixo, relógio MIDI, mapa óptico, dados do usuário e o modo temporal original. **Não começar essa experiência nesta Tarefa 01.**

**Verificações desta Tarefa 01:** integridade dos 15 PNG (assinatura/abertura, bytes e dimensões), metadados da Gymnopédie, leitura visual lado a lado e links das duas execuções concluídas na HEAD anterior ao commit. Alteração **somente em Markdown**, sem modificação de fonte, gradle/versionCode, assets musicais, APK ou `main`. A revisão documental pede conferência de Markdown, links/evidências e escopo do diff; **não exige recompilação nem execução do emulador por si só**. Se o push disparar CI, reportar seu estado no SHA documental e não reaproveitar os resultados anteriores como execução desse novo commit. Yamaha P-45, Realme 7 5G e Lenovo Tab M9 seguem sem aceite físico.


## Tarefa 02/20 — avaliação quantitativa da geometria editorial (19/10/2026)

**Natureza:** documentação e medição; nenhuma alteração visual, funcional, de dependência ou versão. HEAD inicial do PR #46: \`056ccbba39ce66df24ca0c1e28f57c76a998b493\`; PR aberto em *draft*, sem merge. Consultados \`AGENTS.md\`, \`README.md\`, \`docs/10-CONTINUIDADE.md\`, documento 56 e \`PianoLivreAndroid/app/src/main/assets/notation.html\`. Evidência original: [run 39161693531, artefato 12149460909](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39161693531/artifacts/12149460909), 34 arquivos, incluindo 15 PNG de 2160×1080 e JSON de captura. *O run é anterior ao presente commit documental; não é teste da nova HEAD.*

### Método, unidades e amostras

Foram confrontados os registros \`piano-osmd-long.json\`, \`piano-engraving-poc.json\`, \`piano-visual-evidence.json\` e as capturas reais \`piano-osmd-long.png\`, \`piano-temporal-long.png\`, \`piano-osmd-long-tap.png\`, \`piano-osmd-long-moving.png\`, \`piano-osmd-poc.png\` e \`piano-engraved.png\`. A análise prioriza Gymnopédie (78 compassos), com Ode à Alegria (16 compassos) como controle. Para medir pauta, foi examinada a distribuição de pixels escuros nas colunas x=1000–1150 e 1400–1500 das imagens originais, fora das claves: picos horizontais contínuos indicam as linhas; coordenadas são **pixels da captura**, não CSS px nem dp. Tolerância estimada ~1 px para centros das linhas, decorrente de antialiasing/espessura; o método não é um detector validado de contornos de glifos. Unidade relativa \`e\` = distância entre linhas adjacentes da **mesma** pauta. Para comparar tamanhos físicos, seria preciso conhecer densidade efetiva e escala de apresentação de cada aparelho; todas as imagens deste artefato vêm do mesmo emulador.

### Tabela quantitativa (M = medido em PNG/JSON, C = calculado, ND = não determinado)

| Propriedade | Gymnopédie OSMD | Gymnopédie temporal | Ode OSMD | Tipo/evidência |
|---|---:|---:|---:|---|
| Resolução da captura | 2160×1080 px | 2160×1080 px | 2160×1080 px | M, manifesto + PNG |
| Centros das cinco linhas da pauta superior (x≈1400) | y≈532,5; 546; 559,5; 573; 586,5 px | y≈506; 530; 554; 579; 603 px | y≈461; 488; 516; 543; 571 px | M, PNG; ±1 px |
| Espaço \`e\` médio | ~13,5 px | ~24,25 px | ~27,5 px | C, quatro intervalos |
| Altura das cinco linhas (1ª→5ª) | ~54 px = 4e | ~97 px = 4e | ~110 px = 4e | M/C |
| Centros das cinco linhas da pauta inferior (x≈1400) | y≈790,5; 803,5; 817,5; 831; 844,5 px | y≈730; 755; 779; 803; 828 px | y≈681; 708; 736; 763; 791 px | M, PNG; ±1 px |
| Entre os limites internos das pautas (última linha superior→primeira inferior) | ~204 px ≈ 15,1e | ~127 px ≈ 5,24e | ~110 px ≈ 4,0e | C, regiões livres entre linhas; **não** equivalem a espaço totalmente disponível para notas |
| Origem da pauta inferior menos origem superior (modelo OSMD) | 276,593−86,233 = 190,36 unidades registradas de Y | ND | 96,5−16,5 = 80 unidades registradas de Y | M no JSON, em coordenadas do SVG/modelo; não equiparar diretamente a px do PNG |
| Escala SVG CSS | 0,4926742073 | não se aplica | 1,0 | M, JSON |
| Altura SVG nativa→exibida | 381,5909→188 CSS px | ND | 136,5→137 CSS px (arred.) | M, JSON |
| Viewport OSMD | 738×196 CSS px | ND | 738×196 CSS px | M, JSON |
| Largura SVG exibida | 3818 CSS px | ND | 2103 CSS px | M, JSON |
| Compassos / nós SVG / render inicial | 78 / 4927 / 921 ms | ND | 16 / 879 / 317 ms | M, execução prévia do emulador |
| Proporção \`e\` OSMD Gymnopédie/Ode | ~0,49 | — | referência 1,0 | C, PNG; consistente com 0,4927 |
| Proporção \`e\` OSMD Gymnopédie/temporal | ~0,56 | referência 1,0 | — | C, PNG; motores e bases tipográficas diferentes |

**Medições não confiáveis nesta amostra:** largura/altura da cabeça de nota isolada, espessura/altura das claves e acidentes, distância entre sistemas musicais (layout horizontal de um sistema), margens físicas de celular/tablet e área livre para símbolos expressivos. Estimativas visuais de glifos por imagem raster sem segmentar as hastes, linhas e ligaduras seriam falsamente precisas. Devem ser obtidas em investigação futura via \`getBBox()\`/caixas SVG por glifo, com transformação SVG→viewport explicitada e capturas com mesma seção/zoom. As linhas oferecem um padrão reprodutível em \`e\`, mas **não** há nesta tarefa cota editorial universal validada para tamanho mínimo de cabeça de nota/clave/acidente.

### Escala e causa comprovada no código

Em \`notation.html\`, função \`fitOpticalSvg\` (linhas 150–170 da HEAD inicial), a altura nativa é salva em \`svg.dataset.opticalNativeHeight\`, e a escala da prévia é literalmente \`Math.min(1, Math.max(0.05, (viewport.clientHeight-8)/baseHeight))\`. Para \`viewportHeight=196\` e \`nativeSvgHeight=381,5909119\`, o cálculo \`188/381,5909119=0,4926742\` reproduz **exatamente** o fator do JSON. Largura e altura CSS do SVG são multiplicadas por essa mesma escala. A extensão horizontal total **não entra diretamente** na fórmula; os 78 compassos afetam principalmente a largura (3818 CSS px exibidos), enquanto notas extremas, distâncias entre pautas, ligaduras, dinâmicas e limites verticais do SVG podem aumentar sua altura e, consequentemente, comprimir globalmente os glifos. Não há nesta amostra prova de qual elemento isolado causou o excesso de altura; tampouco existe relação causal demonstrada entre o número 78 e a redução. A medida musical \`e\` da Gymnopédie OSMD (~13,5 px) é ~49% da Ode OSMD (~27,5 px), consistente com a escala; esta comparação é de capturas do mesmo emulador. **A dimensão de símbolos fica indevidamente acoplada à altura global da gravura**, mesmo quando o usuário está lendo somente uma janela horizontal de 738 CSS px.

### Recortes, sobreposições e geometrias não demonstradas

- **Confirmado, P1 — redução:** detalhes da Gymnopédie OSMD são cerca de metade do tamanho linear da Ode OSMD na captura de mesma resolução; aceitar tal tamanho em hardware físico demanda ensaio de leitura e toque.
- **Confirmado, P2 — recorte de texto:** \`piano-osmd-long.png\` e \`piano-osmd-long-moving.png\` mostram o começo de “Lent et douloureux” ausente no limite esquerdo da região musical, junto à faixa fixa. A largura do fragmento cortado em px **não foi determinada** com segurança sem caixa de texto SVG e transformação/recorte do DOM; o MusicXML não foi diagnosticado como defeituoso.
- **Confirmado, P2 — tooltip:** \`piano-osmd-long-tap.png\`/ \`piano-osmd-long-moving.png\` mostram a etiqueta “Fá♯5 · compasso 5 · pauta 1 · MIDI conferido” ocupando visualmente a região da pauta superior e ocultando grafia; medir área de interseção exata exigiria caixas da etiqueta e dos glifos no viewport.
- **Não confirmado:** colisão inequívoca de cabeça/haste/acidente, vigas defeituosas, ligaduras semanticamente incorretas ou truncamento de notas em todos os 78 compassos. A distância de ~204 px entre limites das pautas **não é margem dispensável**: arcos, dinâmicas, acordes e hastes inferiores/superiores ocupam parte desse espaço.

### Critérios editoriais e orientação *sem implementar*

1. Tomar \`e\` como unidade de normalização; verificar cinco linhas uniformes, claves ancoradas G4/F3 e glifos com proporções internas estáveis. Comparar a mesma passagem e zoom/viewport em revisões futuras.
2. Inspecionar limites reais de conteúdo por pauta: caixas de cabeças, hastes, acidentes, dinâmicas, ligaduras e textos em coordenadas comuns. Reservar afastamento vertical conforme as caixas e evitar qualquer contato material, sem fixar redução arbitrária.
3. Considerar aceitável uma escala somente após leitura confortável e toque preciso em celular/tablet físicos, mantendo conteúdo textual e os dois pentagramas íntegros; não adotar o teto de viewport como único requisito.
4. Garantir que o texto de expressão tenha caixa integral visível diante da faixa esquerda fixa e em movimento, com deslocamento/recorte documentado. Posicionar o tooltip fora das caixas musicais ocupadas ou aplicar comportamento de ocultação sem obstrução.
5. Não comparar valores absolutos de pixels entre aparelhos sem densidade, zoom e dimensões de viewport; usar \`e\` e captura do mesmo compasso. Na próxima tarefa, caso autorizada, focalizar caixas SVG e diagnóstico de altura/colisões antes de escolher parâmetros; preservar timeline óptica e associação de notas.

**Verificações nesta tarefa:** leitura documental e de código na branch indicada; abertura de PNG e inspeção de linhas em duas janelas X; cruzamento dos registros JSON; cálculo independente da fórmula; revisão para restringir a alteração a este Markdown. Não se executaram testes Gradle, Android/emulador ou em hardware: esta tarefa é estritamente documental e não modifica binários. Os resultados históricos da Action não são apresentados como nova execução. O PR #46 deve permanecer *draft* e não integrado; nenhuma tarefa 03/20 é iniciada.

 
## Tarefa 03/20 — tentativa controlada de correção vertical (19/10/2026)

**Situação provisória: PARCIAL, aguardando evidência independente de escala/colisão.** Branch exclusiva do PR #46; implementação experimental em `PianoLivreAndroid/app/src/main/assets/notation.html`, commit `c3b55c768ad389501c5d90d528a325f5f1115b76`. Sem alterações de MusicXML, MIDI, motor temporal, dependências, versão ou dados.

### Diagnóstico e alteração aplicada

A função `fitOpticalSvg()` normalizava a altura completa do SVG pelo viewport: `(viewport.clientHeight-8)/baseHeight`, comprimindo a Gymnopédie a 0,492674 com 381,59 CSS px nativos em área útil de 188 CSS px. A correção experimental verifica `svg.getBBox()` **antes** da primeira escala, mede o intervalo vertical ocupado pelos elementos desenhados e, exclusivamente se todo esse conteúdo estiver contido no `viewBox` original, recorta apenas **margens exteriores comprovadamente vazias**. Preserva dez unidades SVG de proteção em cima/embaixo, mantém a largura do `viewBox`, reduz a altura CSS nativa proporcionalmente e recalcula a escala uniforme. Se as caixas estiverem inválidas ou houver conteúdo além do `viewBox`, preserva a geometria anterior. Emite `POC_VERTICAL_TRIM` ou `POC_VERTICAL_TRIM_SKIP`. Não altera a distância entre pautas, nenhum elemento da gravura nem a semântica musical.

**Limite central:** a investigação original não comprovou que as grandes distâncias **internas** entre pautas eram vazias; por segurança não foram comprimidas. O eventual ganho só pode vir das margens *externas* livres, que ainda precisam de medição na nova execução. Não se afirma previamente melhoria positiva nem ausência de defeitos. O recorte de “Lent et douloureux” e o tooltip continuam pendentes, pois a correção exigiria determinar caixas de colisão e offsets reais, evitando deslocamento arbitrário.

### Critérios comparativos e verificações

| Parâmetro | Antes (artefato 12149460909) | Depois |
|---|---|---|
| Gymnopédie: altura SVG nativa | 381,59 CSS px | **a medir** em `POC_VERTICAL_TRIM` + `POC_SVG_FIT` |
| Gymnopédie: escala CSS | 0,492674 | **a medir** |
| Gymnopédie: espaçamento de pauta | ~13,5 px | **a medir** na captura PNG nova |
| Ode: escala CSS | 1,0 | **a revalidar** |
| Colisões/recortes/legibilidade | P1 escala; P2 expressão e etiqueta | **sem novo aceite visual** |

A nova alteração foi registrada no PR sem merge. Workflows acionados no commit de código: [build/testes 39247393371](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39247393371) e [emulador 39247393326](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39247393326); a conclusão deles precisa ser verificada **nesse SHA**. Não reutilizar 469/469 da execução antiga como prova atual. Testes físicos Realme 7 5G / Lenovo Tab M9 / Yamaha P-45 ainda não realizados. Sem novas capturas verificadas, os critérios de conclusão da Tarefa 03 não estão preenchidos; não avançar à Tarefa 04.

**Recomendação:** verificar evidência de trim e PNG pós-commit, corrigir regressões se identificadas; caso o trim seja insuficiente, estudar os limites individuais dos SVG das duas pautas, dinâmicas e ligaduras antes de escolher compressão interstaff segura.

### Encerramento e regressão do experimento (Tarefa 03/20)

**Estado final: PARCIAL; implementação experimental revertida.** No commit [`c3b55c7`](https://github.com/atshimura/Piano-livre-aberto/commit/c3b55c768ad389501c5d90d528a325f5f1115b76), a tentativa de aparar margens exteriores por `svg.getBBox()` foi rejeitada pelo teste de contrato `EngravingProofContractTest.js` (linha 149), que registra explicitamente **“Failed crop experiment is not left in the OSMD rendering path”**. O [workflow 39247393371](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39247393371) falhou no passo de teste JavaScript OSMD; os testes JVM anteriores passaram, mas os passos posteriores, inclusive montagem de APK, foram pulados. A rejeição indica uma restrição técnica preexistente que não deve ser contornada alterando o teste sem prova de segurança. Não há comprovação de ganho de escala nem captura antes/depois válida. O [emulador 39247393326](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39247393326) foi disparado no SHA experimental e não deve ser tomado como aceite sem conclusão verificada.

O commit [`bb33cb7`](https://github.com/atshimura/Piano-livre-aberto/commit/bb33cb781f6bf02a3c843dba28b5bd31d2418fe4) **restaurou integralmente `notation.html` à versão inicial** da Tarefa 03, preservando o contrato e a funcionalidade OSMD anteriores. Nenhuma correção visual é reivindicada. Não foram alteradas as decisões do teste e não foram iniciadas tarefas posteriores. A próxima tentativa dentro da **própria Tarefa 03** deverá identificar restrições existentes sobre `getBBox`/recorte, medir com sondas não mutantes de grupos SVG e procurar opções seguras de arranjo vertical **antes** de modificar a geometria. Texto expressivo e etiqueta continuam pendentes.


## Tarefa 03A/20 — diagnóstico vertical aprofundado, sem mutação da gravura (19/10/2026)

**Resultado: PARCIAL quanto a medições por elemento; investigação estática e comparativa concluída.** Esta seção distingue evidências anteriores (Tarefa 02), relações aritméticas derivadas e medições SVG por glifo que não foram obtidas nesta execução. Não se altera o aplicativo, os testes, o MusicXML nem o SVG de produção. Nenhum ganho visual é reivindicado.

### 1. Restrição técnica de recorte: constatação versus risco

O contrato real está em [PianoLivreAndroid/tests/EngravingProofContractTest.js](../PianoLivreAndroid/tests/EngravingProofContractTest.js). Sua asserção é literal: \`ok(!html.includes("svg.getBBox()"),"Failed crop experiment is not left in the OSMD rendering path")\`. Trata-se de um **guardrail estático de regressão**: impede reintroduzir essa chamada textual no caminho HTML após o recuo da Tarefa 03. Não é ensaio geométrico, não demonstra que todo uso de caixas SVG seja incorreto e não é prova experimental de corrupção de coordenadas. Já existiam chamadas de \`getBBox()\` em nós individuais no trilho de contexto; o veto específico é à expressão de recorte do SVG inteiro. O contrato permanece intacto. A falha comprovada é a violação dessa asserção pelo commit \`c3b55c7\`, revertida em \`bb33cb7\`; não há no histórico uma falha visual de glifos causada pelo recorte que tenha sido efetivamente medida.

**Riscos inferidos, ainda não observados após recorte:** alterar \`viewBox\` ou dimensões nativas pode deslocar os fatores \`viewBox/getBoundingClientRect\` usados no hit-test; desacoplar a posição gráfica dos anéis, contexto fixo e cursor; esconder extremos de ligaduras, textos, dinâmica e mudanças de clave, sobretudo durante rolagem/resize; causar divergência entre a cabeça selecionada e o evento MIDI escrito conciliado. A correspondência musical \`OpticalTimeMap.reconcile\` é independente do recorte, mas sua projeção para pixels/toques depende da transformação SVG correta. Qualquer proposta deve preservar invariantes de projeção e verificar cliques em ambas as pautas e em trechos extremos. Nenhum desses riscos é aqui apresentado como defeito reproduzido.

### 2. Método e medidas reproduzíveis disponíveis

A fonte quantificada continua sendo o artefato real do emulador documentado na Tarefa 02 (run 39161693531, artefato 12149460909), não uma nova sonda DOM nesta tarefa. A amostra é uma janela de 2160×1080 px. Seja \`e\` a distância entre linhas adjacentes de uma mesma pauta, em **pixels da captura**. Seja \`H\` a altura CSS completa do SVG *antes* de \`fitOpticalSvg()\`; o fator é \`s=min(1,max(0,05,(viewportHeight-8)/H))\`. Para Gymnopédie, \`s=188/381,5909≈0,492674\`. Esse fator opera nas dimensões CSS, não autoriza substituir diretamente coordenadas absolutas de modelo OSMD ou SVG por pixels da captura. Para um ponto no SVG, a transformação correta inclui \`viewBox\`, tamanho CSS efetivamente renderizado, posição da WebView, offset de rolagem e transformações ancestrais. Em caso de \`getBBox()\`, conferir \`getCTM()\` ou equivalente, stroke, clip, fontes carregadas e caminhos/textos transformados; a união de caixas locais não garante a totalidade dos pixels visíveis.

| Grandeza | Gymnopédie, 78 compassos | Ode, 16 compassos | Natureza |
|---|---:|---:|---|
| SVG nativo, altura CSS | 381,5909 | 136,5 | medido anteriormente por logs |
| Viewport, altura / reserva | 196 / 8 CSS px | 196 / 8 CSS px | medido anteriormente / código |
| Altura após fit | 188 CSS px | aproximadamente 137 CSS px | medido anteriormente |
| Fator uniforme de fit | 0,492674 | 1,0 | confirmado por código e logs |
| Centros pauta superior, PNG | 532,5–586,5 px | 461–571 px | medição raster anterior |
| Centros pauta inferior, PNG | 790,5–844,5 px | 681–791 px | medição raster anterior |
| Intervalo \`e\` | 13,5 px | 27,5 px | derivado dos centros |
| Distância entre pautas (5ª linha superior à 1ª inferior) | 204 px ≈15,11e | 110 px =4,0e | derivado; **não é vazio livre** |
| Deslocamento entre 1ª linha superior e 1ª linha inferior | 258 px ≈19,11e | 220 px =8,0e | derivado da captura |
| Origens registradas OSMD (pauta superior → inferior) | 86,233 → 276,593 | 16,5 → 96,5 | logs em unidades do modelo; conversão não aferida |
| Margem externa superior/inferior **sem qualquer glifo** | ND / ND | ND / ND | não medido |
| Menor \`y\`/maior \`y\` de texto, slur, dinâmica, haste e acidentes | ND | ND | não medido |
| Ocupação efetiva da faixa entre as pautas | ND | ND | não medido |
| Separação mínima editorial segura | ND | ND | não demonstrada |

**Cálculo adicional verificável:** se hipoteticamente a Gymnopédie pudesse ter altura nativa de 300 CSS px *sem perda de conteúdo*, a fórmula daria \`s=188/300≈0,627\`. Este é apenas exemplo de sensibilidade, **não recomendação**, porque não há evidência de que 81,59 CSS px sejam dispensáveis. A diferença de separação Gymnopédie/Ode no raster é 94 px, mas a comparação entre partituras com conteúdo diferente não identifica espaço removível. A razão das alturas de pauta (54/110≈0,491) acompanha a escala e não mede colisões.

### 3. Diagnóstico da faixa entre pautas

É demonstrável que a Gymnopédie exibida possui separação entre pentagramas desproporcional à Ode: 15,11e contra 4e. Não é demonstrável que 11,11e sejam supérfluos; as dinâmicas, hastes e ligaduras podem ocupar a região. Tampouco se pode atribuir essa separação exclusivamente a margens, regras nativas OSMD ou um compasso extremo: faltam as caixas de conteúdo e a distribuição longitudinal ao longo dos 78 compassos. A captura em x≈1400 mede **um corte** do sistema horizontal, enquanto uma regra de afastamento pode resultar de extremos em outros compassos. Não extrapolar a vacância observada numa coluna para todo o sistema.

### 4. Comparação e alternativas para a Tarefa 03B

| Alternativa | Benefício plausível | Risco/contrato | Evidência exigida antes de implementar |
|---|---|---|---|
| **A — configurações nativas do OSMD/EngravingRules** | OSMD recalcula arranjo e caixas de símbolos de forma coordenada | opções e comportamento da versão 1.9.8 ainda não auditados; colisões se folgas inadequadas | identificar propriedades realmente presentes no bundle instalado; comparar limites globais com diversas partituras e alterações de clave/dinâmica |
| **B — espaçamento interstaff gerido pelo motor** | atacar diretamente os ~15,11e visuais | possível colisão de símbolos entre pautas e efeito transversal em todo o sistema | caixas por elemento, por compasso/pauta, interseção vertical e folga musical justificada |
| **C — margens externas sem recorte do \`viewBox\`** | aproveitar espaço periférico sem mudar semântica musical | contrato veda a tentativa \`svg.getBBox()\`; CSS/viewport também podem ocultar glifos/textos | provar margens externas em coordenadas comuns, textos e strokes inclusive, sem modificar caminho de produção |
| **D — área útil do aplicativo** | aumento de tamanho sem alterar geometria musical | interferência no teclado guia, controles, telas menores e posições de toque | medir WebView real em configurações com e sem teclado; testar responsividade e overlays |

**Diretriz para 03B, não autorização de implementação nesta etapa:** começar por uma sonda geométrica temporária de leitura, fora de \`notation.html\`, registrando \`viewBox\`, caixas transformadas de cada pauta/grupo por categorias (stafflines, noteheads, stems, beams, ties/slurs, textos/dinâmicas, claves e armaduras), strokes e limites extremos ao longo dos 78 compassos, e repetir na Ode e numa terceira fixture existente. Capturar valores por compasso em coordenadas homogêneas, calcular intervalo ocupado entre pautas e mínima folga segura **antes** de escolher qualquer parâmetro. O procedimento pode ser executado em browser/emulador de investigação, sem adicionar dependência ao APK nem alterar o contrato. Uma proposta por regras nativas OSMD é preliminarmente mais defensável do que alterar \`viewBox\` externamente, porém **não está validada como segura**. Caso não se obtenham as caixas completas, não realizar a compressão.

### 5. Verificações, rastreabilidade e limitações

- \`notation.html\` consultado na branch, contendo \`fitOpticalSvg\` apenas com redimensionamento CSS uniforme e mantendo o \`viewBox\` original; o blob lido era \`87d6a1df84abc197a232c4d7c49828f465a1158e\`.
- \`EngravingProofContractTest.js\` lido integralmente: a asserção negativa de \`svg.getBBox()\` foi localizada; os contratos **não foram editados**.
- Workflow histórico da HEAD documental \`d4e784f71f3c5dd830e1ea0f6f698cfd6df3f629\`: [build 39247653311](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39247653311) **success**, incluindo teste isolado OSMD, e [emulador 39247653203](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39247653203) **success**. Workflow experimental [39247393371](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39247393371) falhou no contrato. Estes são **testes históricos, não novos testes da Tarefa 03A**.
- Não houve coleta nova de \`getBBox\` dos glifos, inspeção global de 78 compassos, validação de configurações \`EngravingRules\`, screenshot nova, benchmark, execução local de Gradle/Node ou teste físico. Logo não foi quantificada margem recuperável, nem demonstrada separação mínima livre de colisões.
- Permanecem sem correção a legenda “Lent et douloureux” encoberta à esquerda e o tooltip sobre a pauta. PR #46 deve continuar **aberto em draft**, sem merge; Tarefas 03B e 04 não iniciadas.

**Conclusão técnica:** a causa imediata da miniaturização é o ajuste de altura global \`188/381,5909\`; a causa editorial do excesso de altura é **indeterminada**. A investigação estática caracteriza a restrição e a ordem das medições restantes, mas não satisfaz o critério da Tarefa 03A de caixas individuais verificadas. Não declarar correção segura, nem conclusão plena de 03A.

## Tarefa 03A.1/20 — Coleta geométrica instrumentada

**Situação: CONCLUÍDA no escopo de investigação geométrica; nenhuma correção implementada.** HEAD de partida confirmada `6435255d93d4cc782a54274851003b1c4f77f4c0`, PR #46 aberto em draft. Foram lidos AGENTS, README, continuidade, histórico deste documento, HTML de notação, contrato e preparação do bundle antes de criar evidências. O resultado desta seção é novo e não reutiliza medidas raster das tarefas anteriores. Não conclui a Tarefa 03, o item 1.2 ou o aceite editorial.

### Método e proveniência

Sonda externa de leitura com Playwright/Chrome for Testing **131.0.6778.204**, Linux, viewport **738×196 CSS px**. Renderizou as fontes MXL originais da biblioteca (Gymnopédie/Ode) e a fixture debug `engraving-context-change.musicxml`, sem modificá-las, com as opções existentes de `notation.html`: SVG, `compacttight`, horizontal único, títulos/compositor/nomes desativados, sem novas quebras XML ou cursor. **Todas as medidas das duas pautas ao longo dos 78/16/3 compassos foram percorridas**, não uma janela extrapolada. Foram coletadas 2.418/394/134 folhas geométricas respectivamente, incluindo textos e curvas fora dos grupos de compasso.

O bundle foi localizado na tarefa Gradle `prepareOfflineNotation`: OSMD **1.9.8** fixado em URL jsDelivr, convertido pelo build via esbuild 0.25.10 para chrome60 e empacotado offline. A sonda usou o **mesmo upstream 1.9.8 antes dessa conversão**, não um asset extraído de APK. Hash SHA256 do upstream e das fontes XML constam em [geometry.json](evidence/tarefa-03a1/geometry.json). Nenhuma dependência foi incorporada ao aplicativo. A versão antiga do WebView não foi reproduzida.

Cada folha foi medida por `getBBox()` e seus quatro cantos convertidos ao SVG raiz pela composição da inversa da matriz da raiz com a matriz de tela do elemento. A operação não altera DOM, viewBox ou parâmetros. São caixas geométricas, sem espessura de traço; coordenadas de modelos, SVG e CSS estão separadas. Outra execução abriu o **HTML de produção inalterado**, serviu o upstream apenas pela interceptação do navegador e registrou o fit/contexto em [production-check.json](evidence/tarefa-03a1/production-check.json). Essa execução não enviou eventos MIDI: seus logs de reconciliação não são teste musical.

Instrumentos, reprodução e limitações: [README da evidência](evidence/tarefa-03a1/README.md), [probe.cjs](evidence/tarefa-03a1/probe.cjs), [production-check.cjs](evidence/tarefa-03a1/production-check.cjs), [summarize.py](evidence/tarefa-03a1/summarize.py) e [summary.json](evidence/tarefa-03a1/summary.json). Uma repetição com o script arquivado confirmou os números da primeira execução. Nenhum APK/compilação Android foi necessário.

### Novas medidas e unidades

Nesta configuração, **1 espaço entre linhas = 10 unidades SVG = 1 unidade do modelo OSMD**. Os cinco centros são uniformes; não confundir esses números com pixels de captura Android. Valores arredondados abaixo a 0,001; dados originais preservam precisão completa.

| Grandeza nova | Gymnopédie, 78 compassos | Ode, 16 compassos | Fixture, 3 compassos |
|---|---:|---:|---:|
| ViewBox, largura × altura, SVG | 7748,596 × 383,495 | 2103,438 × 136,500 | 880,973 × 151,500 |
| Altura CSS nativa lida no DOM | 383,484 | 136,500 | 151,500 |
| Centros das cinco linhas superiores, SVG | 86,233; 96,233; 106,233; 116,233; 126,233 | 16,500; 26,500; 36,500; 46,500; 56,500 | mesmos centros da Ode |
| Centros das cinco linhas inferiores, SVG | 278,495; 288,495; 298,495; 308,495; 318,495 | 96,500; 106,500; 116,500; 126,500; 136,500 | mesmos centros da Ode |
| Entre a última linha superior e a primeira inferior, SVG | **152,261** | **40,000** | **40,000** |
| Mesmo intervalo em espaços de pauta | **15,226** | **4,000** | **4,000** |
| Entre primeiras linhas das pautas, SVG | 192,261 | 80,000 | 80,000 |
| União de todas as folhas: mínimo Y → máximo Y, SVG | 1,199 → 383,495 | −0,500 → 137,500 | −0,500 → 151,500 |
| Margem geométrica superior / inferior ao viewBox, SVG | 1,199 / ~0,000 | −0,500 / −1,000 | −0,500 / ~0,000 |
| Escala calculada e confirmada no log de fit do HTML | **0,490242** | **1,000000** | **1,000000** |
| Altura CSS final no HTML existente | 188,000 | 136,500 | 151,500 |
| Cabeças de notas, excluídas pausas pelo modelo | 469 | 81 | 19 |

A escala acima é `min(1,188/alturaCSSnativa)`; não foi escolhida pela sonda. A CTM raiz do render primário da Gymnopédie é aproximadamente `[0,99997358,0,0,0,99997358,0,10113735,0]`; a razão difere ligeiramente de 1 por arredondamento CSS e preservação de aspecto. No HTML após fit, a CTM completa, incluindo translação, está no JSON adicional; usar essa matriz para projetar no viewport, sem somar coordenadas do modelo a CSS.

**Comparação histórica, sem misturar execuções:** 381,5909 CSS px / 0,492674 eram medidas do Android WebView; **383,484375 CSS px / 0,490241617** são as novas medidas deste Chrome. A pauta superior permanece em 86,233 SVG, mas a inferior foi medida aqui em 278,495 SVG, contra origem equivalente anterior próxima de 276,593. Há variação de ambiente/fontes/backend. Isso não é regressão de código: nenhum arquivo funcional mudou. O novo resultado não substitui uma futura medição no WebView real.

### Ocupação vertical quantitativa da Gymnopédie

| Categoria / pauta | Folhas medidas | Y mínimo → máximo, SVG | Observação |
|---|---:|---:|---|
| Cabeças de notas superiores | 152 | 71,010 → 151,484 | pausas excluídas via `isRestFlag` e ID VexFlow |
| Cabeças de notas inferiores | 317 | 238,272 → 353,745 | total das duas pautas: 469 |
| Hastes superiores | 134 | 61,233 → 181,233 | extremos em grupos 39 e 37 |
| Hastes inferiores | 154 | 208,495 → **383,495** | extremo inferior em grupos **22 e 61** |
| Linhas suplementares superiores | 14 | 76,233 → 146,233 | não inclui stroke |
| Linhas suplementares inferiores | 203 | 248,495 → 348,495 | extensão confirma notas fora do pentagrama |
| Slurs superiores (`vf-curve`) | 10 | **6,358** → 86,233 | arcos amplos consomem a região acima da pauta |
| Ties superiores (`vf-stavetie`) | 16 | 83,233 → 139,233 | folhas, não número de ligações musicais |
| Textos superiores | 35 | **1,199** → **201,593** | expressão, dinâmica e números de origem XML |
| Textos inferiores | 2 | **193,694** → 219,695 | dois sinais `f`, aproximados por faixa X |
| Cunhas/linhas superiores (`vf-line`) | 50 | 148,033 → **197,233** | ocupam substancialmente o espaço interno |

As 557 folhas sob `vf-notehead` incluem 88 pausas; não foram apresentadas como 557 notas. IDs do modelo permitem separar as 469 cabeças reais. Não há grupos `vf-beam` identificados nessas três renderizações; **não foi obtida cobertura representativa de vigas**. Demais classes/textos estão integralmente no JSON. Elementos fora de `vf-measure` são relacionados a pautas e intervalos X; sua atribuição musical exata pode exigir o modelo de expressão.

**Extremos exteriores:** o menor Y geométrico pertence ao texto `46` perto de X=7601,051; a primeira linha superior em Y=86,233 não representa 86 unidades de margem vazia. O slur `vf-auto5409-slur` chega a Y=6,358, abrangendo X≈6850,896…7681,697. Os limites inferiores são hastes `vf-auto2602-stem` (grupo 22) e `vf-auto4846-stem` (grupo 61). A margem geométrica superior é apenas 1,199 e a inferior praticamente zero, **antes de stroke**: não existe evidência de grande margem externa recuperável ou de recorte seguro.

### Faixa entre pautas e causa do afastamento

O intervalo de **15,226 espaços entre pentagramas** não é uma faixa vazia. Na projeção global Y, textos superiores chegam a 201,593 enquanto textos inferiores começam em 193,694: as projeções se sobrepõem, embora em X distintos. Portanto nem sequer há uma faixa horizontal inteira de vazio demonstrada pela união global desses símbolos.

Comparando somente caixas de elementos musicais de pautas opostas cujos intervalos X se sobrepõem, a menor folga encontrada foi **11,261 SVG = 1,126 espaços**. Exemplo auditável: folha **908**, segmento de cunha superior em X≈3356,973…3491,729 e Y≈191,233…197,233, e folha **1597**, haste inferior do grupo **35**, em X≈3468,073 e Y≈208,495…268,495. Há ocorrências equivalentes diante de hastes dos grupos **36, 72 e 74**. A caixa da cunha é um envelope retangular: sua folga não equivale à distância exata entre contornos. Stroke de 1,2/1,5, antialiasing e critérios editoriais não entram nessa conta. **Não se recomenda remover 11,261 SVG.**

Foi também lido o skyline/bottomline do próprio OSMD: **2310 amostras por pauta**, com `SamplingUnit=3`. O algoritmo nativo em `MusicSystemBuilder.optimizeDistanceBetweenStaffLines` considera, em cada índice, o bottomline superior e uma vizinhança de ±6 amostras do skyline inferior. A maior exigência medida foi **18,226125 unidades do modelo**, em índices **2108–2110**, X≈**7046,667…7053,333 SVG**, próximo do grupo **72**. Componentes: bottomline superior **11,326125** e skyline inferior **−6,9**. Somando a folga nativa de **1,0**, o resultado é **19,226125**, **exatamente a diferença de origens do modelo**: 27,849451−8,623326. Convertido a SVG e descontados os 4 espaços de altura da pauta superior, reproduz os **152,261 SVG** entre pentagramas.

Há ainda um pico próximo do grupo **36**, X≈3563,333, exigindo **18,0** antes da folga. Assim, o máximo global é localizado, mas não é a única região exigente; corrigir ou retirar um único extremo não prova que a pauta poderá ser reduzida a 4 espaços. A associação índice→grupo é geométrica, não prova de qual objeto individual foi responsável pelo pixel que alimentou o skyline. Os dados por compasso permitem aprofundar essa atribuição sem novo palpite global.

**Controles:** Ode e fixture têm máximo skyline calculado de **6,4**; somada folga 1,0 → 7,4. O piso nativo `StaffHeight+MinimumStaffLineDistance = 4+4 = 8` prevalece, e as origens realmente distam **8 espaços**. As regras efetivas são iguais entre as partituras, enquanto o conteúdo/linhas de ocupação diferem. O backend preferido registrado foi 1 na Gymnopédie e 0 nos controles, seleção automática existente do motor; a parcela da diferença numérica entre ambientes causada pelo backend não foi isolada.

### Parâmetros nativos confirmados na versão 1.9.8

Verificados tanto em propriedades do objeto real `osmd.EngravingRules` quanto na fonte **da tag 1.9.8**, sem presumir APIs de versões posteriores: [EngravingRules.ts](https://github.com/opensheetmusicdisplay/opensheetmusicdisplay/blob/1.9.8/src/MusicalScore/Graphical/EngravingRules.ts), [MusicSystemBuilder.ts](https://github.com/opensheetmusicdisplay/opensheetmusicdisplay/blob/1.9.8/src/MusicalScore/Graphical/MusicSystemBuilder.ts), [SkyBottomLineCalculator.ts](https://github.com/opensheetmusicdisplay/opensheetmusicdisplay/blob/1.9.8/src/MusicalScore/Graphical/SkyBottomLineCalculator.ts). Valores abaixo são **efetivos em compacttight**, não necessariamente defaults da classe.

| Propriedade / valor efetivo | Efeito possível / limite | Risco a verificar em 03B |
|---|---|---|
| `BetweenStaffDistance=2.5` | afastamento inicial entre pautas do mesmo instrumento | otimização skyline pode substituir o valor; diminuir sozinho não demonstra ganho |
| `StaffDistance=3.5` | afastamento inicial entre instrumentos diferentes | não é a alavanca específica deste piano de duas pautas |
| `MinimumStaffLineDistance=4` com `StaffHeight=4` | piso de 8 espaços entre primeiras linhas | explica os controles, mas não limita a Gymnopédie: skyline exige mais |
| `MinSkyBottomDistBetweenStaves=1` | folga adicionada à exigência skyline/bottomline | diminuir retira proteção de colisões; nesta coleta a soma é a restrição ativa |
| `PageTopMarginNarrow=0`, `PageBottomMargin=0`, `PageTopMargin=5` | margem do modo compacto versus normal | já não há reserva externa relevante comprovada; não resolve slurs/textos acima |
| `MinimumDistanceBetweenSystems`, `MinSkyBottomDistBetweenSystems=1` | organização de múltiplos sistemas | não confundir com separação interna do sistema horizontal único |
| `WedgeVerticalMargin=0.5` | folga de cunhas de dinâmica | pode afetar conteúdo que participa do limite; auditar crescendo/decrescendo e hastes |
| `DynamicExpressionMaxDistance=2`, `DynamicExpressionSpacer=0.5` | posicionamento/espaço de dinâmicas | pode mover colisões para outra expressão; não foi variado |
| `SlurNoteHeadYOffset=0.5`, `SlurStemXOffset=0.3`, `SlurPlacementUseSkyBottomLine=false` | ancoragem/colocação de slurs; não simples margem de página | preservar ligação correta, arcos e cabeças em todo o trecho |
| `LabelMarginBorderFactor=0.1`, `LyricsYMarginToBottomLine=0.2`, alturas de texto na classe | reservas de texto/letras | alterar tamanho/reserva pode prejudicar legibilidade; fixture sem letras não valida esse caso |

**Nenhuma regra foi alterada ou ensaiada com valores alternativos.** A existência da propriedade e sua participação estática não provam que a alteração seja editorialmente segura. A fonte mostra que o motor pode substituir distâncias iniciais pelo envelope de ocupação, justificando investigar expressões/skyline antes de tentar números arbitrários.

### Recomendação objetiva para 03B

1. Reproduzir a sonda no bundle transpilado/WebView Android e comparar os pontos críticos **35/36/72/74**, as hastes extremas **22/61** e o slur final; atribuir os máximos de skyline às expressões correspondentes, sem editar MusicXML.
2. Avaliar, em experimento isolado com o motor, posicionamento nativo de cunhas/dinâmicas antes de reduzir a folga entre pautas. Investigar se o envelope reservado excede a tinta visível, preservando margem editorial e todos os símbolos. **Não há evidência para prometer ganho substancial apenas com `BetweenStaffDistance`.**
3. Qualquer proposta deverá comparar caixas/contornos com strokes, screenshots do sistema completo e janelas início/meio/fim, polifonia, ligaduras, vigas e textos; validar novo render, hit-test nas duas pautas, rail, cursores, resize e reconciliação MIDI. Não declarar distância mínima segura com estes dados.
4. Se o conteúdo realmente exigir a altura, investigar a área útil/tamanho de apresentação como alternativa de produto em etapa própria. Nenhuma alteração da interface é executada aqui.

### Verificações, estado de CI e limitações

- Executado nesta tarefa: sonda geométrica completa, repetição do instrumento arquivado, leitura adicional do HTML de produção e derivação quantitativa; `EngravingProofContractTest.js` **PASS, 68 verificações**, sem edição do teste. Scripts/JSON passaram conferência de sintaxe e consistência. Comparação de árvore confirma alterações restritas a este documento e `docs/evidence/tarefa-03a1/`.
- Na HEAD inicial, [build 39249363432](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39249363432) e [emulador 39249363490](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39249363490) estavam **success**. São resultados anteriores ao commit desta entrega, não testes novos da sonda nem prova do commit documental posterior. Não foi iniciado build Android deliberadamente.
- Limites: browser/fontes diferentes do Android; upstream não transpilado; caixas sem stroke; ausência de cobertura representativa de vigas, letras e importações adversariais; atribuição de expressões por X aproximada; nenhum ensaio de redução de parâmetros, benchmark, APK ou hardware. Essas limitações não impedem a conclusão da **coleta quantitativa**, mas impedem afirmar compressão segura ou ganho visual.
- Aplicativo, contratos, versão 0.9.46/code55, XML/MIDI e dados preservados. Sem merge, release ou acesso ao repositório privado. PR #46 deve permanecer aberto em draft. Texto “Lent et douloureux” e tooltip não foram corrigidos. **03B e 04/20 não iniciadas.**

## Tarefa 03B/20 — geometria vertical adaptativa (19/10/2026)

**Situação: CONCLUÍDA no escopo da Tarefa 03B, em prévia experimental.** Build e emulador passaram para o código final do SHA `7618d72bc1d59952be6e7f889552c4ab724dc3c4`; capturas e JSONs foram inspecionados. PR #46 permanece draft e a apresentação OSMD não se tornou padrão do Estudo. A intervenção segue a evidência da 03A.1: a Gymnopédie tem picos reais do skyline/bottomline e cerca de 1,126 espaços de folga geométrica mínima em caixas musicais opostas com projeções X sobrepostas. Não era seguro reduzir a distância calculada pelo OSMD.

### Estratégia e escopo

`fitOpticalSvg()` preserva a largura e altura nativas do SVG, define escala uniforme `1.0` e não altera `viewBox`, `EngravingRules`, MusicXML nem o layout interno. Quando a altura nativa excede a área da prévia, `#viewport` permite gesto vertical. A movimentação horizontal continua dirigida pelo mapa óptico/relógio; a rolagem vertical não muda beat, cursor, eventos MIDI, posição das cabeças ou colunas. Fora do topo, a faixa duplicada de contexto é ocultada para não sugerir alinhamento incorreto; o tooltip de toque é ocultado durante a rolagem e a identificação reabre ao tocar outra cabeça. Ao voltar ao topo a faixa é redesenhada. O OSMD 1.9.8 continua offline e a prévia permanece opcional.

Arquivos funcionais/testes: `PianoLivreAndroid/app/src/main/assets/notation.html`, `PianoLivreAndroid/app/src/main/java/br/org/pianolivre/MainActivity.java` (seleção de fixture só em build debug), `PianoLivreAndroid/tests/EngravingProofContractTest.js`, `PianoLivreAndroid/tests/emulator_ui.py` e `.github/workflows/emulator.yml`. Nova fixture original e sem conteúdo de terceiros: `PianoLivreAndroid/app/src/debug/assets/scores/engraving-complex-synthetic.musicxml`; não é empacotada no APK público. Contém grand staff, acorde, vozes, vigas, acidentes explícitos, ties, slur, dinâmicas/cunhas, notas pontuadas e quiáltera.

### Comparação Android antes/depois

Gymnopédie, Ode e mudanças de contexto foram medidas no mesmo Android API 29 emulador, viewport CSS 738×196 (contexto: 738×220), baseline do workflow 39251809096 e nova captura desta tarefa. A altura nativa OSMD não muda: o ganho vem de deixar de reduzi-la. `staffSpace` é a distância entre linhas adjacentes; `staffGap` é a distância entre a última linha superior e a primeira linha inferior, em espaços de pauta.

| Partitura / grandeza | Antes | Depois | Evidência / leitura |
|---|---:|---:|---|
| Gymnopédie — altura nativa | 381,591 CSS px | 381,591 CSS px | SVG nativo preservado |
| Gymnopédie — altura CSS / escala | 188 px / 0,492674 | 382 px / 1,000000 | mesma viewport; excedente rolável |
| Gymnopédie — espaço entre linhas | ~4,927 CSS px | 10 CSS px | derivado da escala antiga × medida nativa do WebView; símbolos ~2,03× maiores |
| Gymnopédie — distância interstaff | 15,036 espaços | 15,036 espaços | sem compressão |
| Gymnopédie — separação interstaff exibida | ~74,08 CSS px | ~150,36 CSS px | escala multiplicada pela mesma distância normalizada |
| Gymnopédie — rolagem vertical máxima | 0 px | 189 CSS px | gesto revelou a pauta inferior sem rerender |
| Gymnopédie — cabeças / correspondência | 469 gráficas | 469/469 conciliadas | 0 ambíguas/não conciliadas; toque mapeado |
| Ode — altura nativa / escala | 136,5 px / 1,000000 | 136,5 px / 1,000000 | não foi redimensionada nem ampliada |
| Ode — espaço / distância entre pautas | 10 CSS px / 4 espaços | 10 CSS px / 4 espaços | sem espaçamento adicional |
| Fixture complexa — altura nativa / escala | 195,898 px / 1,000000¹ | 195,898 px / 1,000000 | fixture nova, sem screenshot baseline arquivado |
| Fixture complexa — espaço / distância entre pautas | 10 CSS px / 6,090 espaços¹ | 10 CSS px / 6,090 espaços | acordes, vozes e expressões preservados |
| Fixture complexa — cabeças / correspondência | 27 gráficas¹ | 27/27 conciliadas | 0 ambíguas ou ausentes |
| Mudanças de contexto — altura nativa / escala | 151,5 px / 1,000000 | 151,5 px / 1,000000 | 19/19 cabeças e contexto 1→2→3 |
| Mudanças de contexto — espaço / distância entre pautas | 10 CSS px / 4 espaços | 10 CSS px / 4 espaços | geometria interna inalterada |

¹ Sem screenshot anterior da fixture complexa. A estimativa antiga aplica `min(1,(viewport−8)/nativeHeight)` à altura nativa medida, que também dá 1 neste viewport; não é uma execução baseline nem comparação visual. Nos outros três casos foram usados JSONs anteriores do emulador e novas capturas. A altura final é arredondada no JSON; o teste tolera 0,5 CSS px diante de `nativeSvgHeight` fracionária.

**Leitura visual:** as capturas reais Android mostram a Gymnopédie a escala nativa; o gesto vertical revela a pauta inferior e oculta o tooltip que antes cobria notas. A fixture mostra acorde, sustenido/bemol, vigas, ties, slur, dinâmica/cunha e algarismos de quiáltera. Os contadores DOM identificam beams/ties/slurs/dynamics, mas não os acidentes nem os números de quiáltera, que o OSMD converte em caminhos sem classe semântica. Para esses símbolos, o contrato verifica os dados MusicXML e a captura renderizada é inspecionada; zero no seletor CSS não é tratado como ausência. “Lent et douloureux” continua parcialmente atrás da faixa esquerda fixa, problema anterior documentado em 03A.1, não introduzido por esta tarefa.

### Testes, artefatos e limitações

- [Build/JVM/JS/MusicXML/MIDI e APK de teste — success](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39264341639); job de publicação pública pulado. `EngravingProofContractTest.js`: 73 verificações; `OpticalTimeMapTest.js`: 160; testes temporais, claves, spanners, notação avançada e ornamentos; `verify_public_snapshot.py`; sintaxe Python/XML.
- [Emulador Android — success](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39264341640): regressões de interface e playback, resize/teclado guia, toque na cabeça, mãos, Gymnopédie 469/469, rolagem vertical (189 px máximos, `noteTipVisible=false`), fixture de contexto e sintética 27/27. `POC_COMPLEX_GEOMETRY`: altura nativa 195,898 px, espaço 10 px, separação 6,090 espaços, 4 vigas, 2 ties, 1 slur e 4 símbolos de dinâmica. O XML fonte da fixture contém 2 acidentes explícitos, 2 marcações de quiáltera, 3 time-modifications e 10 beams.
- Artifact [PianoLivre-public-emulator-evidence](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39264341640/artifacts/12192182408), ID `12192182408`: 37 arquivos, incluindo 17 PNGs reais de 2160×1080, `piano-osmd-long.png`, `piano-osmd-long-vertical-pan.png`, `piano-osmd-long-tap.png`, `piano-osmd-context-changes.png`, `piano-osmd-complex-synthetic.png` e JSONs. Captura Gymnopédie baseline: artifact 12186590129, workflow 39251809096. Pixels comparados somente dentro do WebView Android, sem misturar com o Chrome de 03A.1.
- Limites: Android API 29 emulador, sem aparelho físico. Resize por teclado guia foi exercitado; Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 pendentes. Em altura curta, o usuário rola para alternar pautas, em vez de vê-las simultaneamente. Revisão editorial física, legibilidade sustentada, gestos reais, desempenho em partituras/importações grandes, polifonia adversarial e clipping da expressão à esquerda continuam pendentes.

Implementação principal entrou no commit [`f819c77`](https://github.com/atshimura/Piano-livre-aberto/commit/f819c77bb10df85862bff0dd282dd5f151bd4956), “Tarefa 03B/20 — geometria vertical adaptativa”; correções limitadas de arredondamento, fixture e visibilidade/medição foram adicionadas em commits seguintes até a HEAD `7618d72`. Esta seção documental será versionada no mesmo PR. Renderizador temporal, interpretação MusicXML/MIDI, perfis, biblioteca, histórico e dados não mudaram. Sem merge, release, APK público, alteração de `main` ou início da Tarefa 04.


## Tarefa 03C/20 — Legibilidade e apresentação responsiva das duas pautas

**Situação final: CONCLUÍDA tecnicamente no escopo da 03C, por melhoria de navegação vertical validada.** Build e emulador do código final `0cc670c` passaram, incluindo os três perfis. A aprovação editorial e física permanece pendente. Não houve ganho de simultaneidade na Gymnopédie em celular baixo. Não iniciar 04 nesta entrega. As subseções de validação provisória abaixo são histórico; a conclusão e tabela finais prevalecem.

### Diagnóstico e alternativas

Leitura de AGENTS, README, continuidade, HTML, contratos, evidência 03A.1 e seção 03B realizada na HEAD `fc3e89592bd3c68fcd404cca916be2ba1200b2de`. A separação da Gymnopédie é limitada pelo conteúdo: o skyline documentado exige 19,226 espaços entre origens; a menor folga de caixas opostas é aproximadamente 1,126 espaços, sem stroke. Margens externas não oferecem reserva relevante. Portanto não se reduziram proteções de colisão, dinâmicas, slurs, hastes ou geometria OSMD. A escala anterior de ~0,493 já foi rejeitada por legibilidade; não se introduziu outro piso arbitrário de redução.

A inspeção de `PianoScreens.setKeyboardOpen` confirma que abrir o guia oculta título, controles superiores e header, além de mostrar o teclado; fechar restaura esses componentes. Essa lógica nativa está preservada. Nenhuma hipótese de menor altura ao abrir o teclado foi adotada. Os testes novos exigem medições reais dos dois estados e retorno.

### Melhoria implementada

Quando a altura nativa excede a altura do WebView, a prévia reserva uma faixa lateral de 48 CSS px com botões acessíveis Subir/Descer e estado Topo/Entre pautas/Base. A faixa não consome altura nem cobre música. O custo é menor janela horizontal, mantendo a navegação horizontal musical existente. O passo vertical de 80% mantém sobreposição de contexto; não representa escala mínima musical. As extremidades desabilitam seus botões. Toque esconde a etiqueta e altera somente scrollTop: não busca beat, não modifica MIDI/MusicXML, não muda escala, não comprime pentagramas. O gesto vertical continua disponível. Quando a gravura cabe, a faixa desaparece e a largura integral retorna.

`fitOpticalSvg` continua preservando escala 1 e registra largura total do WebView, largura musical útil, altura, escala, altura nativa e se o SVG inteiro cabe. A largura total é usada pelo teste ADB para converter coordenadas CSS, evitando confundir a faixa reservada com mudança de escala. A comparação de resize considera a largura musical após reservar/remover a faixa.

Arquivos: `notation.html`, `tests/emulator_ui.py`, novo `tests/VerticalNavigationTest.js`, workflows build/emulator e este documento. Sem alteração de versão pública, renderizador padrão, dados, interpretações musicais ou ocultação nativa das barras.

### Verificação e evidências

- Contratos existentes: 73 verificações aprovadas; OpticalTimeMap: 160, incluindo 12.000 âncoras. Auditoria pública aprovada.
- Novo teste DOM executa 24 combinações de largura (738/1024), altura (196/300/500) e alturas representativas das quatro partituras, navegação, preservação de beat/escala e restauração de largura após maior altura. **É teste com DOM simulado, não renderização dessas partituras nem emulador/tablet.**
- Novo teste Android registra `piano-03c-{ode,gymnopedie,complex,context}.json` e PNGs para guia fechado/aberto/restaurado; exige ocultação/restauração de ambas as barras e altura restaurada. Gymnopédie também exercita perfis de display mais alto (`wm size 1440x2160`) e representativo de tablet (`1600x2560`), com restauração do display original; são overrides no mesmo emulador, não aparelhos distintos. Exige ainda toque real no botão inferior e captura `piano-03c-navigation.png`. Workflows arquivam esses arquivos.
- Falta executar e inspecionar CI do novo commit, capturas e reconciliação 469/469 e 27/27. Não reutilizar os resultados de 03B como comprovação nova.
- Ambiente local sem adb/Gradle/Chrome instalado. Instalação de Chromium falhou com arquivo de download inválido; não houve ensaio visual local nem build Android local.

### Comparação quantitativa e limites atuais

| Indicador | Antes (03B Android, histórico) | Depois (03C) |
|---|---|---|
| Gymnopédie viewport musical | 738×196 CSS px | Medição Android pendente |
| Escala / espaço entre linhas | 1 / 10 CSS px | Código preserva 1; medição pendente |
| Altura nativa / separação | 381,591 px / 15,036 espaços | Geometria não alterada; confirmação pendente |
| Navegação explícita | gesto vertical | botões + gesto; teste Android pendente |
| Duas pautas completas em altura curta | não | não se promete ganho de simultaneidade |
| Teclado aberto / fechado | sem tabela comparativa dedicada | testes acrescentados, resultados pendentes |

A melhoria visa **navegação**, não ganho de visualização simultânea em altura insuficiente. A escala 1 permanece por preservar uma geometria já validada em 03B; não é afirmada como única solução responsiva possível. Não se alterou a experiência padrão: faixa apenas na prévia experimental. Não há nova aprovação editorial, prova de ausência de colisões por captura nem validação física Realme/Lenovo/Yamaha. A 04 continua responsável por contexto fixo durante rolagem e texto “Lent et douloureux”; a 08, etiqueta de notas. A conclusão técnica depende da CI e evidências novas.

### Primeira validação Android e correção do ensaio de perfis

Código funcional `3ddb50e`: [build 39270958758](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39270958758) e [emulador 39270958845](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39270958845) **success**. Artifact [12196095709](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39270958845/artifacts/12196095709), 54 arquivos, inclui capturas/JSONs teclado fechado, aberto e restaurado das quatro partituras. Inspecionadas capturas Gymnopédie com teclado e navegação: os botões ocupam faixa própria; a pauta inferior é alcançada sem comprimir símbolos. Texto expressivo atrás do rail é pendência anterior da 04, preservada. Sem aprovação editorial integral.

| Partitura | Altura útil fechado → aberto → restaurado (CSS px) | Escala | Gravura (CSS px) | Duas pautas / navegação |
|---|---|---|---|---|
| Gymnopédie | 196 → 218 → 196 | 1 | 381,591 | exige navegação nos três estados |
| Ode | 196 → 218 → 196 | 1 | 136,5 | duas pautas integralmente visíveis |
| Complexa | 220 → 242 → 220 | 1 | 195,898 | duas pautas integralmente visíveis |
| Contexto | 220 → 242 → 220 | 1 | 151,5 | duas pautas integralmente visíveis |

**Abrir o teclado recuperou 22 CSS px líquidos**, já descontada sua própria altura. As duas barras e o header desapareceram e retornaram nos testes. Não existe baseline anterior dedicado ao teclado aberto para atribuir esse ganho à 03C: é comportamento nativo anterior, agora medido.

No mesmo perfil Android da 03B, a área musical da Gymnopédie passou de 738×196 para **690×196**, reservando 48 px laterais sem perder altura. Altura 381,591, espaço 10 px, separação 15,036 espaços e escala 1 permaneceram iguais; a rolagem real máxima continuou 189 px. O campo `verticalOverflow` registra excedente geométrico de 185,591; não confundir com scrollHeight arredondado/extensão real de 189. No teclado aberto, excedente geométrico 163,591. Não houve ganho de simultaneidade na Gymnopédie deste perfil; houve botão de navegação funcional adicional ao gesto.

Em `b077500`, [build 39271249068](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39271249068) passou; [emulador 39271249240](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39271249240) falhou ao aplicar `wm size` durante a Activity iniciada com extras de autotestes. Android recriou a Activity e repetiu `upgrade_selftest`, cujo arquivo temporário `files/imported/upgrade.mxl` já havia sido removido legitimamente no fluxo anterior. Não foi falha de fit/scroll/MIDI; o teste revelou contaminação do ensaio de configuração pelos extras iniciais. Os logs e artifact 12196400135 preservam a falha.

Correção: novo `tests/emulator_responsive.py` executado **depois** de todos os testes UI existentes, com processos novos sem extras de autotestes, display configurado com app parado e restauração em `finally`. Cada perfil abre a Gymnopédie por UI, ativa a prévia e exige 469/469 antes das medições teclado fechado/aberto/restaurado. Testes de atualização, lifecycle e seus critérios permanecem intactos; não houve mudança em Java nem supressão de asserção.


### Conclusão técnica e medições finais

Código validado: `0cc670c2513eea37202eb63e4cdfb734a2a07d5d`. [Build 39272746781 — success](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39272746781) e [emulador 39272746710 — success](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39272746710). Todos os testes anteriores permaneceram ativos. Atualização release não depurável, lifecycle/foreground, playback/relógio, mãos, loop, toque, Espera, contexto, resize e teclado passaram. Gymnopédie **469/469**, complexa **27/27**, ambas sem ambiguidades/ausências. Cada perfil adicional também exigiu 469/469. Contratos JS 73, OpticalTimeMap 160 e novo DOM 24 cenários aprovados no workflow. Não houve benchmark novo de fluidez nem validação física.

Artifact final [12196097730](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39272746710/artifacts/12196097730). São **19 capturas específicas da 03C**, além das capturas de regressão; verificadas assinatura PNG/dimensões. Inspecionadas Gymnopédie teclado aberto, navegação inferior, tablet fechado/aberto, perfil alto aberto, Ode, complexa e contexto. As capturas de perfis adicionais medem 2160×1440 e 2560×1600 pixels físicos; métricas abaixo são CSS px. JSONs originais, mapas dos perfis e inventário das 19 PNGs foram preservados em [`docs/evidence/tarefa-03c/metrics.json`](evidence/tarefa-03c/metrics.json).

| Perfil Gymnopédie / teclado | Viewport musical CSS | Escala / espaço entre linhas | Gravura / separação | Excedente geométrico vertical | Classificação |
|---|---|---|---|---:|---|
| Celular baixo / fechado | 690×196 | 1 / 10 px | 381,591 px / 15,036 espaços | 185,591 px | necessita navegação vertical |
| Celular baixo / aberto | 690×218 | 1 / 10 px | iguais | 163,591 px | necessita navegação vertical |
| Maior altura / fechado | 690×351 | 1 / 10 px | iguais | 30,591 px | envelope musical global parcialmente visível |
| Maior altura / aberto | 690×373 | 1 / 10 px | iguais | 8,591 px | envelope global ainda exige navegação |
| Display de tablet / fechado | 883×409 | 1 / 10 px | iguais | 0 | duas pautas e envelope vertical integralmente visíveis |
| Display de tablet / aberto | 883×431 | 1 / 10 px | iguais | 0 | duas pautas e envelope vertical integralmente visíveis |

Fechar o guia restaurou exatamente alturas 196, 351 e 409 respectivamente e as duas barras; quando o SVG cabe, a faixa lateral some e a largura integral é restituída. Em todos os perfis, abrir o teclado recuperou 22 px líquidos. Ode, complexa e contexto foram testadas no perfil baixo com os dois estados, conforme tabela anterior, e mantiveram ambas as pautas completas. Os dois perfis adicionais exercitaram a Gymnopédie; não se afirma matriz completa de quatro partituras × três displays.

**Critério de simultaneidade conservador:** o campo `simultaneous` exige que a altura nativa inteira do sistema horizontal caiba. No perfil de 373 px, as duas pautas do início estão visíveis na captura, mas os extremos verticais de outros compassos ainda excedem 8,591 px. Não se declara sistema integralmente visível nesse caso. A necessidade de rolagem decorre do envelope global, não de todas as janelas musicais individualmente.

**Antes/depois no mesmo ambiente:** escala 1, espaço 10 px, altura 381,591 e distância 15,036 espaços não mudaram. A Gymnopédie passou de gesto isolado a gesto + botões, com largura útil 738→690 e altura 196→196. Rolagem DOM máxima fechada 189→189 px. O excedente geométrico 185,591 não inclui todo arredondamento/overflow DOM; não é usado como sinônimo de scrollTop máximo. Não existe baseline 03B dedicado aos dois displays maiores ou ao teclado aberto: os números desses cenários descrevem a 03C e não um ganho causal frente à 03B.

Conclusão: a melhoria comprovada é de navegação, com controles externos à música, tamanho preservado e funcionamento musical mantido. A simultaneidade fica disponível quando a altura permite; não foi resolvida integralmente para celular baixo. Não foram observadas colisões novas nas capturas inspecionadas, mas isso não certifica todo o repertório nem aprova editorialmente todos os símbolos. O recorte expressivo e o contexto durante pan vertical continuam na 04; posicionamento da etiqueta, na 08. A validação Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 permanece pendente. Prévia experimental, versão 0.9.46/code55 e padrão temporal preservados; sem merge, release ou APK público de distribuição.

# ZAI-008 — Pausas legíveis e partitura responsiva sem rolagem horizontal

**10/11/2026 — Plano aprovado para execução nesta solicitação.** Implementação na linha `zai/feat/zai008-pausas-e-paginacao-20261110`, PR para `zai/lab`, respeitando `AGENTS.md`, GPL-3.0, fonte MIDI e dados, e mantendo **`main` intocada**. O usuário solicitou explicitamente o aperfeiçoamento das pausas e que a partitura gerada ocupe a largura do aparelho, quebrando automaticamente as linhas, sem rolagem horizontal. Preservar a experiência do modo Estudar e o modo de partitura importada.

## Diagnóstico

- Na 0.9.68 (ZAI-007), `ScoreRhythmQuantizer.fillRests()` usa escolha gulosa de figuras e restrições de alinhamento, podendo decompor um intervalo vazio em múltiplas pausas pequenas. Com vozes polifônicas, essas pausas podem aparecer lado a lado. Não se deve apagar silêncios reais, deslocar Note Ons ou desenhar pausas de menos tempos.
- `NotationView` chama `PianoNotation.load(...,'free',...)` tanto na prévia de Criação quanto em outros modos de leitura. A página `notation.html` usa `body.free #score {width:153.846%; transform:scale(.65)}`, e o visor livre permite `overflow:auto`; em paisagem, o resultado pode ser uma única linha comprida exigindo rolagem lateral. O código tem opção OSMD `renderSingleHorizontalStaffline` destinada exclusivamente ao modo Estudar: **não alterá-la**.
- `CreationScorePreviewScreen` usa `NotationView.loadScore(...,true)`, tornando difícil configurar comportamento responsivo apenas na Criação. A prévia já oferece A−/A+/Ajustar à largura e um `ScrollView` vertical.
- O XML salvo mantém o marcador da revisão antiga `ZAI-007-tie-free`, logo uma derivação antiga pode ser reutilizada sem aplicar a nova lógica de pausas. A prévia deve oferecer regeneração explícita, sem apagar o arquivo antigo ou reescrever o `.plc`.

## Decisões de produto e invariantes

1. **Pausas não são enfeites:** toda pausa escrita corresponde exatamente a um intervalo não ocupado naquela voz, preservando a posição dos ataques na grade de semicolcheias e o total de 4 tempos por compasso. A única simplificação é **trocar várias pausas contíguas por menos figuras equivalentes**, respeitando limites de compasso e agrupamentos rítmicos. Manter uma única pausa de compasso inteiro na pauta sem notas.
2. **Preferir o mínimo de símbolos** quando musicalmente possível; usar semínimas, mínimas, semibreves ou colcheias antes de semicolcheias redundantes. Pausas pontuadas são permitidas se reduzirem símbolos sem criar duração incorreta; evitar agrupamentos que cruzem grandes fronteiras métricas sem necessidade. Os pontos de staccato da primeira escala continuam sendo articulação, e não introduzem pausas artificiais.
3. **Polifonia autêntica:** não esconder pausas de uma segunda voz se isso comprometer a leitura do tempo; não inventar ou eliminar vozes apenas para melhorar o visual. Verificar caso S1 com 22 Note Ons, C/Cm, passagem sobreposta, 5min de tecla e formatos de backup.
4. **Enquadramento horizontal de Criação:** um modo de desenho OSMD específico `creation-page`, distinto de `free` e `engraving-poc`. Renderizar com contêiner de **100% da largura disponível**, `newSystemFromXML:false`, `renderSingleHorizontalStaffline:false`; OSMD reengrava e **quebra em sistemas** à medida que a tela diminui. A prévia rola **apenas verticalmente**, sem rolagem horizontal. Não usar um gigantesco sistema encolhido artificialmente com CSS. Os controles de zoom mudam tamanho/quantidade de compassos por linha e devem continuar sem rolagem lateral.
5. **Responsividade:** ao mudar largura (incluindo rotação), executar `PianoNotation.resize()`/reengravação; evitar geometria obsoleta. As notas, pausas e compassos MusicXML permanecem idênticos e são apenas diagramados de outro modo. Aumentar a largura da tela deve permitir mais compassos por linha.
6. **Isolamento:** preservação integral dos modos `free` (partituras tradicionais/importadas) e `engraving-poc` (Estudar), incluindo navegação e cursor. Os layouts XML continuam compatíveis com OSMD offline. Não adicionar rede, telemetria nem bibliotecas pagas.
7. **Arquivos antigos:** novo marcador `ZAI-008-rests-wrap` no MusicXML; identificar PLS2 antigo e oferecer **Gerar prévia atualizada**, só substituindo a derivação ao salvar explicitamente. Preservar `.plc`, cópias anteriores e dados de backup, sem mudar o formato PLS2.
8. **Assinatura e downloads:** caso a entrega inclua APK, usar código 78 e nome `0.9.69-open-beta` do pacote principal `br.org.pianolivre.opensource`, assinatura permanente dos Secrets; distribuir na branch `zai/downloads` com link GitHub raw fixado ao commit e SHA-256, jamais depender do erro `Library file not found`. Não publicar tag/Release sem autorização.

## Etapas

**P8-0 — Documento e baseline.** Inspecionar `AGENTS.md`, documentação e CI. Registrar comportamento anterior e novo modo separado.

**P8-1 — Motor de pausas.** Substituir `fillRests` guloso por escolha determinística que reduz número de figuras, minimiza fragmentos, respeita a grade e não cruza barras. Testes exaustivos de todos os intervalos possíveis de pausa (0..16 semicolcheias por compasso), soma exata e comparação com o algoritmo anterior.

**P8-2 — Responsividade.** Separar a prévia da Criação da leitura livre; ampliar quadro de partitura conforme área da tela, sem deslocamento horizontal. OSMD com quebra de linha automática. Zoom e rotação devem reengravar e continuar legíveis.

**P8-3 — Validação.** Testes JVM para invariância dos 22 ataques do S1, notas longas, acorde e pausas; auditoria Python para contratos de interface; **browser Chromium real com OSMD empacotado**, formatos 360/800/1100 px e orientação, contando cabeças e conferindo o `scrollWidth` horizontal e as múltiplas linhas. Check de que `free` e `engraving-poc` não foram alterados.

**P8-4 — Integração.** Corrigir falhas de CI, criar PR somente para `zai/lab`, registrar testes e limitações. Não confundir browser/headless com teste em aparelho Android real.

**P8-5 — APK e download, quando produzido.** Compilar versão incremental assinada, validar metadados/certificado, publicar **os mesmos bytes** diretamente no GitHub sem biblioteca ChatGPT, verificar hash remoto.

## Critérios de aceite

- **REST-CONSERVATION:** em cada voz, soma de notas + pausas = 16 células por compasso; os ataques e durações das notas escritas não mudam; não aumentar número de pausas em qualquer intervalo; uma pausa integral no compasso vazio.
- **REST-MINIMAL:** para cada intervalo a ser preenchido, nenhuma decomposição válida com menos pausas e convenções equivalentes (comparando o conjunto permitido de figuras).
- **WRAP-WIDTH:** prévia Criação em 360px, 800px e 1100px: `viewport.scrollWidth <= viewport.clientWidth + 2` e cabeças de nota visíveis dentro da largura, diversas pautas/sistemas em músicas suficientemente longas, sem reduzir a pontuação musical.
- **WRAP-ISOLATION:** modos Estudar/Free seguem disponíveis e não migram para a configuração exclusiva da Criação.
- **S1-22:** 22 Note Ons correspondem a 22 cabeças, sem `tie` ou `slur`, acordes C/Cm corretos, Mi♭ escrito corretamente, staccato e pausas verdadeiras preservadas.
- **SAFE-REGENERATE:** arquivo PLS2 anterior não é mostrado como atualizado; a gravação continua íntegra, o botão para regeneração é explícito, sem sobrescrever até Salvar.
- **DATA/CI:** todos os testes ZAI anteriores necessários e build Android passam. Teste físico Yamaha P-45, Realme 7 5G e Lenovo Tab M9 **pendente** até o mantenedor testar.

**Limite importante:** formato simplificado ZAI-007 continua abreviando sustentações que atravessam compassos; esta fase melhora apenas pausas e diagramação, não recompõe ties. Musicalmente, uma pausa necessária não pode ser eliminada apenas para reduzir símbolos.

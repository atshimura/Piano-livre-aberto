# ZAI-012 — Correção da rolagem residual do seletor Laboratório Musical

**Data:** 11/11/2026. **Origem:** captura real do mantenedor após a ZAI-011/versão 0.9.72. A frase "Selecione como deseja usar..." aparece cortada na transição com a barra azul; "Escolha um modo" fica acima do limite visível, mesmo com três cartões apresentados. O fato confirma uma regressão **não detectada pelos testes anteriores**.

## Diagnóstico do defeito anterior

1. O cálculo de ZAI-011 usava `onSizeChanged` da **tela-pai inteira** e assumia que a área de rolagem era `alturaPai-56dp`. Em Android, a altura medida de `ScrollView` pode diferir devido à janela real e às barras/sobreposições do sistema. Resultado: cartões altos demais para a área útil real em alguns aparelhos em paisagem.
2. O `ScrollView` mantinha a posição de rolagem e não tinha foco inicial controlado. Uma chamada de foco ou entrada após navegação podia deslocar automaticamente o conteúdo para os botões inferiores, ocultando o título de escolha e cortando a descrição.
3. As 89 verificações JVM de ZAI-011 simulavam somente alturas abstratas **do componente pai** e não testavam frames Android reais do seletor. Portanto não comprovavam ausência de scroll/corte no aparelho.

## Solução

- Escutar o layout de `scroll.addOnLayoutChangeListener` e usar a altura **medida diretamente do viewport da ScrollView**, já descontadas barra superior e insets. O planejador `ZaiAdaptiveHeights.landingContent` recebe somente a altura útil da área de conteúdo, sem subtrair novamente a barra de 56dp, e deixa uma pequena reserva de arredondamento.
- Dar foco ao contêiner rolável (antes dos botões filhos) e, ao entrar/voltar para o seletor em `PianoScreens.showLab`, aplicar `showAtTop()`, recalcular a altura real e restaurar `scrollTo(0,0)` após o layout. Não alterar os botões, opções ou navegação de outros módulos.
- Manter rolagem de acessibilidade se a janela real for curta demais ou houver ampliação de fontes, para não recortar textos. Sem alterações no motor MIDI, MusicXML, armazenamento, assinatura ou permissões.

## Aceite e regressões

- JVM puro: `ZaiAdaptiveViewportRegressionTest` cobre altura **interna do ScrollView** (incluindo uma janela de proporções próximas às da captura), escalas de fonte e altura consumida por título, descrição e cartão; `ZaiAdaptiveHeightsTest` da ZAI-011 continua obrigatório.
- Contratos Python: fluxo de navegação, ausência de regressão nas outras telas e verificação explícita de `landingContent`, `scroll.getHeight`, controle de foco e retorno ao topo.
- **Emulador Android com GUI real** (workflow ZAI-012 dedicado): compilar pacote debug isolado e abrir Laboratório em paisagem sob três geometrias/densidades/fontes; medir `uiautomator dump` para conferir título, descrição e três cartões inteiros dentro da ScrollView; executar swipe, confirmar ausência de deslocamento, navegar por Descobrir e voltar. Preservar screenshots/artifacts para auditoria.
- Testes de emulador **não substituem** a validação física no Realme 7 5G e Lenovo Tab M9. Conferir especialmente altura útil com barra de navegação Android à direita.

## Integração e distribuição

Integrar o PR apenas à `zai/lab` após os checks de JVM, Android GUI e CI legada aprovados. Gerar APK principal **0.9.73-open-beta/versionCode 82** usando os **Secrets permanentes** e sem alterar versões persistidas/`main`. Publicar os mesmos bytes em `zai/downloads` e comparar SHA-256 remoto. Sem release, tag, Play Store ou acesso ao repositório privado. Documentar hash, runs, limitações e resultado físico pendente.

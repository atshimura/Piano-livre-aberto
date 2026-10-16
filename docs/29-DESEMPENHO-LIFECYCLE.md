# Diagnóstico local de desempenho e ciclo de vida

Disponível desde 0.9.16/code25. Sem rede, telemetria ou armazenamento de música/dados pessoais. `TEMPORAL_PERF` é emitido no logcat ao pausar movimento ou desmontar um renderer. Contadores têm tamanho fixo, sem lista por frame. `TemporalNotation.diagnostics()` devolve uma cópia para inspeção local.

## Reprodução

1. Abrir uma partitura, iniciar Contínuo, aguardar alguns segundos e pausar.
2. Alternar teclado, retornar ao início, repetir um trecho e abrir partitura longa importada localmente.
3. Sair para a tela inicial do Android durante reprodução, voltar e verificar que a posição permanece pausada; iniciar manualmente. Navegar biblioteca/estudo/leitura livre e repetir.
4. Capturar somente os diagnósticos necessários com `adb logcat -d -s PianoLivreNotation:I PianoLivreUI:I`. Não publicar logs privados ou XML importado.

`renderMs`: parsing, layout e construção inicial do DOM. `firstPaintMs`: tempo até o primeiro callback, incluindo espera da fila. `maxDrawMs`: duração JS do callback. `movingFrames`, `meanGapMs`, `maxGapMs` e `gapsOver50Ms`: espaçamento entre callbacks móveis; pausas são excluídas. 50 ms é um limiar descritivo do diagnóstico, sem efeito pedagógico. `peakVisibleTiles` e `tiles`: carga visível versus total.

Esses valores não medem o tempo da GPU, rasterização/composição nem confirmam FPS físico. Ausência de intervalos longos no emulador não demonstra ausência de microtravadas no Realme/Lenovo. Comparar a mesma música, aparelho, velocidade e configuração antes de atribuir melhorias.

## Ciclo de vida

Ao ir para segundo plano, transporte/MIDI são interrompidos pelo fluxo existente, sessão é salva, ticker é removido e ambas as WebViews recebem onPause. Ao retornar, WebViews recebem onResume e a posição Java é reenviada, sem iniciar reprodução automaticamente. O ticker é removido antes de ser postado para impedir cadeias duplicadas.

## Evidências automatizadas

TemporalRenderingTest verifica contadores copiados, intervalos 16/80 ms, exclusão de pausa longa e descarte ao teardown; mantém regressões de partitura sintética longa, resize, loop e velocidade. emulator_ui.py verifica retorno pausado, posição estável e reprodução manual após foreground, salvando `piano-performance.json` no artefato do emulador. Conferir os Actions do commit/PR atual na issue #6.

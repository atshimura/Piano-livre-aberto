# 61 — Entrega B: integração funcional de sessões de Estudo

## Alcance e justificativa

Este PR adiciona prova **integrada e determinística do núcleo musical** para as duas dimensões exigidas pela Entrega B: um exercício curto (`estudo-livre-01.mxl`) e uma partitura longa (`gymnopedie-no-1-satie.mxl`), ambas provenientes do catálogo público do projeto.

O teste `StudySessionIntegrationTest.java` usa os **mesmos módulos reais** do aplicativo (`Score.read`, `WaitPreparation`, `WaitRecognizer`, `KeyboardGuide`, `MidiPlayback`) sem Android, sem Yamaha e sem modificar arquivos de música. Exercita cada peça com mãos direita/esquerda/ambas, todos os ataques esperados, indicação de notas extras, acordes sem calibração inicial presumida, limite de acorde calibrado, visualização simultânea das duas pautas no guia, playback até o fim, pausa com liberação de notas, repetição com seek, parada final sem notes on pendentes e releitura do MusicXML preservado.

**Delimitação:** não é, por si só, uma prova da interface Android ponta a ponta, do posicionamento visual da escrita ou do hardware físico. A UI do emulador permanece em `tests/emulator_ui.py`, e sua validação deve ser avaliada em conjunto. Se a suíte identificar defeito funcional, corrigir em PR delimitado e reaplicar os mesmos ensaios.

## Independência dos PRs

- **Base:** `main` / 0.9.47-open-beta, Entrega A integrada.
- **PR #51:** B1 com câmera adaptativa, não integrada; não presumir sua disponibilidade.
- **PR #52:** etiquetas por toque, separado; este teste não depende dele.
- **PRs #46/#47/#48:** preservar.

## Aceite pendente

A Entrega B completa ainda exige demonstrar abertura/fechamento, toque, cursor, controles, teclado guia aberto/fechado, loops e background em Android para ambas as peças, inclusive com B1 quando integrada por autorização. Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 continuam exclusivamente para teste do usuário. **Não fazer merge, Release nem distribuir APK** sem autorização.


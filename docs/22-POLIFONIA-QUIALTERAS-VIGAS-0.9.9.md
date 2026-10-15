> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 22 — Polifonia, quiálteras aninhadas e vigas (0.9.9-beta)

**Data:** 14–15/10/2026. **Versão:** 0.9.9-beta, versionCode 18. Continuação incremental da versão 0.9.8. O projeto permanece beta, e os testes do Yamaha P-45 físico em Realme e Lenovo continuam pendentes.

## 1. Quatro vozes independentes na pauta temporal

O renderer `arrangeVoices(notes,rests)` reconhece até quatro vozes simultâneas em um compasso/pauta. Usa hastes alternadamente para cima/baixo, espaça cabeças de alturas coincidentes e afasta pausas de camadas superiores/inferiores. Em uníssono de quatro vozes, as cabeças são deslocadas 0, 12, 24, 36 unidades SVG; as pausas em -15, +15, -29, +29. Isso é uma convenção de legibilidade do estudo temporal, não diagramação profissional certificada. Nenhuma alteração em MusicXML `<pitch>`, `<duration>`, `staff`, `voice` ou na saída MIDI.

## 2. Quiálteras aninhadas

O leitor JS passou de `tupletStart/Stop` singular para `tupletMarks[]`: cada nota pode receber múltiplas instruções `<tuplet type='start|stop' number='...'>`. A chave para pareamento de grupos inclui pauta, voz e número do grupo; os limites são globais à partitura, não ao tile SVG. O número impresso de cada nível usa `<tuplet-actual><tuplet-number>` quando existe; só depois recorre a `time-modification`, para não exibir erroneamente o produto composto (por exemplo, 15) no lugar de 3 e 5. Grupos aninhados recebem pistas verticais independentes. O `<duration>` continua sendo a autoridade temporal, sem introduzir tolerância em ms ou mudanças de andamento.

## 3. Vigas e ganchos em vários níveis atravessando tiles

O problema: `drawBeamGroups` recebia apenas as notas próximas de um tile de 1160px. Se a viga começava num tile e terminava em outro, podia não aparecer.

`collectBeamGroups(plan.notes,layout.x)` agora reúne a sequência musical completa, separada por staff/voice/beam-level (1–5), com sequências begin/continue/end e forward/backward hook. Primeiro calcula a reta suavemente inclinada da viga primária e atribui `_beamTip` às notas; a haste SVG passa a alcançar esse ponto, sem se apoiar em uma altura arbitrária. Os níveis superiores são posicionados em relação à viga primária. `drawScoreBeams` recorta polígonos preenchidos nas fronteiras dos tiles, evitando criar um SVG enorme e mantendo as notas nas coordenadas de beats originais.

## 4. Testes novos e regressões exigidas

- `tests/NotationAdvancedTest.js`: quatro vozes uníssonas e quatro pausas independentes, preservação de beat/pitch, quiálteras aninhadas 3:5, dois marcos start/stop na mesma nota, vigas de níveis 1 e 2 e ganchos atravessando tile, coordenadas SVG finitas e ligação da haste à viga.
- `tests/musicxml_visual_crosscheck.js`: fixture MusicXML de quatro vozes com `<backup>`, duas quiálteras numeradas independentemente, marcações no mesmo `<notations>` e teste da proporção impressa; mantém a auditoria original das dez peças, 2.335 ataques.
- Mantidos: `NotationSpannersTest`, `ClefEngravingTest`, `TempoMapTest`, `WaitRecognizerTest`, auditoria das mãos, PerformanceRoute e demais testes JVM/JS.
- Antes de distribuir: CI do commit final com assinatura APK e teste de interface Android/emulador; execução no Yamaha P-45 físico não foi realizada.

## 5. Limitações e próximas tarefas

Há situações que ainda requerem trabalho: espessuras/colisões entre 4 vozes densas, vigas cruzando pautas, notas de graça, vigas com alterações do tipo fan/feathering, relações mais incomuns de quiálteras aninhadas, alterações simultâneas por voz, espaçamento horizontal editorial independente do relógio e revisão visual em aparelhos com densidades diferentes. O modo Leitura Livre conserva o OSMD para pautas convencionais.

Não alterar contratos pedagógicos consolidados: no Espera notas extras são indicadas sem impedir progresso; janela de simultaneidade dos acordes só após calibração, duração e pedal são orientativos; teclado virtual sempre apresenta ambas as mãos.
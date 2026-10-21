> **Arquivo de referência técnica transferido para a edição aberta.** Parte das afirmações sobre versão, CI, aplicativo privado ou certificados descreve apenas o histórico de desenvolvimento. Para a situação vigente no repositório público, consulte [AGENTS.md](../AGENTS.md), [build e testes](06-BUILD-TESTES.md) e [estado](07-ESTADO-E-ROADMAP.md). Links para execuções privadas podem estar inacessíveis a terceiros.

# 03 — Partitura, relógio visual e seleção de trechos

> **Estado vigente 0.9.11:** o documento original iniciou no código 0.9.0 e preserva descrições históricas. Desde a 0.9.5, `Score.tempoChanges` e `advanceBeats` consideram mudanças internas de andamento; desde a 0.9.6, `PerformanceRoute` dirige repetições e casas suportadas; na 0.9.9 há até quatro vozes e vigas/quiálteras complexas; 0.9.10 acrescentou notas de graça e acidentes; **0.9.11 acrescentou bandeirolas/vigas próprias para grace**, sem mudar beat/MIDI. `saveSession()` já persiste **loop_start/loop_end**, embora a retomada integral continue sujeita a ensaio. Consultar [18](18-MOTOR-MUSICAL-0.9.5.md), [19](19-PERCURSO-MUSICAL-0.9.6.md), [22](22-POLIFONIA-QUIALTERAS-VIGAS-0.9.9.md), [24](24-AUDITORIA-NOTACAO-MIDI-ETAPAS-1-3.md) e [25](25-CONTINUIDADE-0.9.9-0.9.11.md). A fidelidade editorial completa segue **não validada**.

## 1. Dois modos de notação distintos

**Estudo temporal:** assets/temporal-notation.js desenha SVG próprio, com distância horizontal proporcional à duração em beats. **Leitura livre:** assets/notation.html usa OpenSheetMusicDisplay (OSMD 1.9.8 offline) para uma partitura mais convencional, com rolagem e zoom. **Não presumir fidelidade musical equivalente**: o renderizador temporal foi implementado sob medida, apresenta cobertura parcial do MusicXML e requer ampliação.

A implementação gráfica em JS **reanalisa o MusicXML** de forma independente de Score.java. Correções futuras devem criar um modelo musical compartilhado ou testes que detectem divergências.

## 2. Unidade de tempo e geometria

- Um **beat** na cronologia interna equivale a **uma semínima (quarter note)** no MusicXML normalizado por divisions, não necessariamente ao pulso percebido no denominador da fórmula.
- Posição gráfica: **x(beat) = origin + beat × pixelsPerBeat**.
- **cursorPx(width) = 0,4 × width**; origin = máximo entre essa posição e **railWidth + 32**. Logo, em tela estreita, o cursor pode ficar **além de 40%** para não colidir com pauta fixa (diagnóstico atualmente reporta nominal 0,4).
- **pixelsPerBeat** é calculado por menor intervalo entre eventos: limitado aproximadamente a **104–360 px/beat**. Evitar distorcer por duração de nota ou largura de glifo.
- **fixedRailWidth = 140 + 14 × número máximo de acidentes da armadura (até 7)**. Revisar no caso de armaduras não convencionais.
- **verticalScale(viewport)**: usa altura disponível e limita escala vertical a **0,25–0,67**, permitindo adaptação quando teclado guia fica visível. A versão com teclado ampliado recupera espaço ocultando 127 dp de barras superiores.

### Regra fundamental

Não confundir geometria do desenho e progressão do tempo: notas simultâneas devem compartilhar x; movimento uniforme de uma semínima deve percorrer exatamente um valor **pixelsPerBeat**, para determinado BPM, sem “pulos” corretivos.

## 3. Pipeline de renderização

1. **xmlTimeMap(xml)** usa DOMParser; exige **score-partwise**.
2. Extrai partes, compassos, divisions, fórmula, armadura, claves, notas, acordes, vozes, pausas, beaming, ties, slurs/direções e marcações de compasso.
3. Produz **plan** com notes/rests/bars/marks/times/keys/clefs/duration/tempo/unsupported.
4. **buildLayout** converte beat em coordenada e reserva a área da pauta fixa.
5. **render** divide a partitura em SVGs menores, tiles de **1160 px**, desenha notas/pausas e cria cursor e rail separados.
6. **paintRail(beat)** mantém claves, armaduras e fórmula de compasso fixas à esquerda. Quando há mudança no MusicXML, o conteúdo do rail se atualiza com o beat. Notas, pausas, acidentes locais e marcações associadas ao trecho permanecem móveis.
7. **moveTiles** exibe somente tiles próximos ao viewport e os desloca com translate3d.
8. **highlightNotes** destaca discretamente notas futuras correspondentes ao beat.
9. **requestAnimationFrame** atualiza posição visual e sobreposição da seleção de trecho.

### Limitações explicitamente conhecidas

- Renderizador **não é gravador profissional completo**; desenho próprio de glifos, beams, acidentes e ligaduras é aproximado. Verificar múltiplas vozes, acordes deslocados, ties que atravessam tiles e mudanças de armadura/clave.
- **Partituras com mais de duas pautas**: a renderização temporal gera erro. Pauta simples e casos de partes múltiplas requerem auditoria musical.
- **Repetições, casas e retornos** podem aparecer como símbolos, mas **não são expandidos como ordem de execução musical** em Score.java.
- **Andamento:** desde 0.9.5, o núcleo Java mantém **`Score.tempoChanges`** e avança beats segundo o andamento vigente, não somente `Score.tempo` fixo. A adequação a todos os elementos MusicXML e ao hardware real continua pendente.
- Pintura das notas usa nome, oitava, acidente; vozes, polirritmos, quiálteras, ornamentos e pedal precisam de cobertura/validação.
- A fórmula/clave são mantidas fixas por decisão pedagógica, porém convenções de notação e posição de alteração precisam ser comparadas com referências musicais reais.
- A renderização em fallback **PianoView** é esquemática, não deve ser aceita como sucesso de notação.

## 4. Relógio e suavidade

**Fonte de verdade musical:** MainActivity.position. No Contínuo/Demonstração, ticker soma **deltaMs / 60000 × Score.tempo × tempoFactor**, com delta de 25 ms aprox. (limitado a 250 ms). No modo Espera, position só avança ao reconhecer condição de nota e pode animar deslocamento até próxima posição.

**Relógio visual:** TemporalNotation.sync(beat,bpm,playing) ancora posição em **performance.now()**. Enquanto moving, requestAnimationFrame calcula beat projetado e desloca os tiles. Correção de fase: erro pequeno recebe ajuste progressivo de até **±0,003 beat** por atualização; erro maior que **0,36 beat**, mudança de andamento, início ou seek fazem reancoragem. NotationView limita envios móveis repetitivos a ~110 ms.

Esses valores são **implementação atual**, não normas pedagógicas definitivas; precisam de benchmarks em Realme 7 5G e Lenovo Tab M9 (60 Hz, perdas de frame, velocidade e impacto da WebView). Um emulador pode aprovar a proporcionalidade e ainda apresentar microtravadas no dispositivo real.

**Diagnósticos logcat:** PianoLivreNotation (JS/erros/callbacks), PianoLivreTimeline (Espera), **TIMELINE_READY**, **TEMPORAL_TILES**, **TEMPORAL_SAMPLE**, **TEMPORAL_RENDER_SUCCESS**. Atenção: amostragem TEMPORAL_SAMPLE em JS registra poucos exemplos iniciais, **não é um profiler completo de FPS/jitter**.

## 5. Interação de trecho

- Usuário toca **Trecho ↻** na barra inferior. **PianoScreens.editingLoop** muda e chama **NotationView.setLoopEdit** → **PianoNotation.loopEdit** → **TemporalNotation.setLoopEditing**.
- Sobre a partitura surge um intervalo realçado, normalmente no compasso atual, com alças inicial/final e opções **Repetir ✓** e **Cancelar**.
- Arrasto passa por **snapLoopBeat**: prefere início de notas/compassos próximos e aceita granularidade básica de ¼ beat.
- Ao confirmar, JS chama **AndroidNotation.onLoopSelected(start,end)**; **MainActivity.applyLoopSelection** valida intervalo, pausa, define loopStart/loopEnd, volta ao início do trecho e atualiza o relógio.
- Um toque prolongado no botão **Trecho** abre opções manuais para marcar início/fim/remover repetição.
- Ao tocar **Início**, o loop é cancelado e a música volta ao beat 0.
- O **loop_start/loop_end é salvo** por perfil em `SharedPreferences` desde a evolução 0.9.3; a recuperação de todos os estados visuais e cenários de retomada ainda depende de validação.

## 6. Contratos de interface a preservar

- Com teclado fechado, cabeçalhos e controles musicais superiores normalmente visíveis.
- Com teclado aberto, ocultar os três conjuntos de barras superiores; preservar partitura, teclado e controles inferiores (inclusive Ocultar teclado).
- Em qualquer mudança de altura, chamar **NotationView.resizeToFit** depois do layout; **TemporalNotation.resize** recria tiles mantendo a seleção visual.
- Manter o rail legível e imóvel, inclusive após resize/seek/loop.
- As notas destacadas devem permanecer relacionadas à posição exata em beats e ao filtro de mãos (o destaque visual completo por mãos ainda é passível de aprimoramento).
- Em telas pequenas o cursor deve ficar à direita da pauta fixa sem ocultar notas. Evitar texto cortado e barras que não possam ser alcançadas por toque.

## 7. Testes e depuração

Os testes existentes ficam em **PianoLivreAndroid/tests/TemporalLayoutTest.js** (conversão beat→pixel, velocidade) e **emulator_ui.py** (WebView render, temporização, reprodução visual, teclado, rolagem). Antes de mudar geometria, criar fixtures MusicXML para: simultaneidade, notas fora da pauta, mudança de claves/armadura, fórmulas irregulares, beams, pausas, ties, slurs, trechos atravessando múltiplos tiles, rotação/resize e alternância repetida do teclado. **Não declarar fidelidade por aprovação de uma só partitura.**

## Evolução da partitura temporal na beta 0.9.7

As claves agora usam desenhos SVG próprios **ancorados por nota/linha real da pauta**: Sol (G4) na segunda linha de baixo e Fá (F3) entre dois pontos que enquadram a quarta linha. A armadura acompanha a clave do trecho, acidentes locais diferenciam <alter> de <accidental>, e figuras e pausas usam <type> para conservar sua escrita nas quiálteras. Slurs simples são desenhados sem alterar o MIDI. As regras e limitações estão em [20](20-NOTACAO-CLAVES-0.9.7.md).

## Quiálteras, ligaduras e duas vozes

O render temporal cria spans globais das notações MusicXML e desenha cada segmento ao longo dos tiles, sem perder a continuidade dos arcos. A geometria visual de hastes/cabeças/pausas de vozes simultâneas é ajustada sem afetar o tempo e altura musical. [Documento 21](21-QUIALTERAS-LIGADURAS-VOZES.md).

## Notação complexa 0.9.9

A pauta temporal agora conserva múltiplas marcas `<tuplet>` por nota, incluindo `tuplet-actual` de cada nível para exibir quiálteras aninhadas, sem confundir proporção com andamento MIDI. `collectBeamGroups` reconstrói vigas primárias e secundárias de cada pauta/voz e calcula `beamTip` do stem; `drawScoreBeams` gera polígonos recortados por tile. A separação de quatro vozes é exclusivamente geométrica. Ver [22](22-POLIFONIA-QUIALTERAS-VIGAS-0.9.9.md).

## 0.9.10: grace e acidentes

Notas de graça não consomem beats; usam um grupo SVG pequeno (67%), deslocado antes da nota principal com slash opcional. Acidentes simultâneos são arranjados por colunas verticais, removendo apenas glifos verdadeiramente duplicados, nunca sons diferentes. **Na 0.9.11, as vigas de execução ainda ignoram grace, mas há vigas/bandeirolas específicas dos ornamentos**; não há emissão MIDI de nota de graça por esse desenho. Consulte [doc 23](23-NOTAS-DE-GRACA-ACIDENTES-0.9.10.md).

## Revisão editorial em 0.9.46 — problema não resolvido (PR #46 em pesquisa)

A **regra vigente de proporcionalidade estrita** `x = origem + beats × pxPorBeat`, a pauta esquerda fixa e a posição do cursor continuam ativas na versão publicada. A comparação real `piano-engraved.png` versus `piano-free-reading.png` do Actions revelou símbolos artificiais no Estudo comparados com o OSMD da leitura tradicional. A prova de conceito OSMD horizontal por ativação expressa é apenas **visualmente exploratória** e **não é substituição**: sua geometria óptica não é linear e ainda não houve prova de hit-test, timeline, loop, sincronismo ou estabilidade de scores grandes. Ver diagnóstico completo e critérios de decisão em [doc 56](56-ESTUDO-GRAVURA-PROFISSIONAL-POC.md). Não marcar qualidade editorial como concluída pela CI.

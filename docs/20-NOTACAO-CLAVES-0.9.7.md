> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 20 — Fidelidade da partitura: claves, armaduras, acidentes e quiálteras (0.9.7-beta)

**Versão:** 0.9.7-beta, versionCode 16. **Data:** 14/10/2026. Etapa incremental de qualidade da escrita musical, **não** certificação de todas as convenções MusicXML nem teste físico Yamaha P-45.

## 1. Referências de teoria musical e objetivos

A posição da clave **define uma nota de referência da pauta**, não é apenas decorativa:

- **Clave de Sol (G clef)**: o centro da espiral identifica o **Sol4 (G4; MIDI 67)** na **segunda linha contando de baixo**, a **quarta linha contando de cima**. Com espaçamento fixo de 13,2 unidades SVG, o centro da espiral coincide com \`staffTop + 3 × 13.2\`.
- **Clave de Fá (F clef)**: seus **dois pontos devem enquadrar a linha do Fá3 (F3; MIDI 53)**, a **quarta linha contando de baixo**, **segunda contando de cima**. O ponto superior fica a meia distância entre linhas acima de F3; o inferior, a meia distância abaixo, com a média das duas posições **exatamente no y da linha F3**: \`staffTop + 1 × 13.2\`.
- **Clave de Dó (C clef, alto)**: a convergência central identifica o **Dó4 (C4; MIDI 60)** na terceira linha, como referência auxiliar para mudanças de clave.

**Referências verificadas:**
- [Music Theory — treble and bass landmark lines (Luke Carlson)](https://resources.lukecarlsonmusic.com/theory1-chapter1/)
- [SMuFL — registro tipográfico de claves e alinhamento com pautas (W3C)](https://www.w3.org/2019/03/smufl13/specification/text-metrics-glyph-registration.html)
- [Bravura, fonte de referência SMuFL (Steinberg)](https://github.com/steinbergmedia/bravura) — consultada como referência técnica **sem incorporar ou redistribuir arquivos de fonte**.
- [MusicXML 4.0 — escrita simbólica de figuras e quiálteras](https://www.w3.org/2021/06/musicxml40/tutorial/notation-basics/)
- [MusicXML — accidental é sinal gráfico, não sinônimo de alteração de altura](https://www.w3.org/2021/06/musicxml40/musicxml-reference/elements/accidental/)
- [MusicXML — time-modification e tuplet](https://www.w3.org/2021/06/musicxml40/musicxml-reference/elements/time-modification/)

## 2. Claves corrigidas no renderer temporal

Arquivo **\`PianoLivreAndroid/app/src/main/assets/temporal-notation.js\`**:

- \`drawClef\` ganhou **contornos vetoriais originais**, mais próximos da forma convencional e sem depender de a versão do Android possuir uma fonte com símbolos musicais.
- \`staffLineY(staff,row)\` e \`clefAnchor(staff,sign)\` definem a linha de referência matemática (row é indexado a partir da linha superior).
- Para G, o centro do desenho da espiral está relacionado à linha de Sol4. Para F, o código gera os dois círculos **exatamente simétricos em torno de Fá3** e o corpo arredondado se prende àquela linha.
- A mesma função \`drawClef\` é usada pela pauta **fixa** e pelas **mudanças de clave dentro da música**. Não usar uma versão menor/inadequadamente alinhada para o trilho fixo.
- Cada grupo SVG declara o tipo de clave e o valor da linha/nota de referência em atributos verificáveis por testes.

## 3. Armaduras e alterações locais

- \`keySignatureNotes\` passou a escolher as oitavas convencionais para os 7 sustenidos/bemóis conforme a clave ativa (Sol/Fá/Dó); os símbolos **não** são mais colocados mediante deslocamentos arbitrários por índice.
- A **armadura fixa** acompanha a clave **vigente no beat atual**; numa troca G→F, o sustenido de F é reposicionado de F5 para F3 na respectiva pauta, sem afetar o som.
- Quando falta um \`<accidental>\` explícito, a renderização pode reconstruir o sinal **natural/sustenido/bemol** usando \`<alter>\`, a armadura e acidentes anteriores da mesma altura/oitava no compasso. Uma alteração impressa explícita mantém precedência. O estado é reiniciado no compasso ou ao trocar armadura.
- O valor \`<alter>\` ainda controla a **altura real** dos eventos na camada Java/JS. **Não inferir uma nova altura MIDI a partir do desenho da armadura**: no MusicXML, altura sonora e marca gráfica são informações distintas.
- Intervalos microtonais, armaduras não tradicionais, alterações editoriais complexas e convenções de acidentes para vozes cruzadas exigem pesquisa e fixtures adicionais.

## 4. Figuras, quiálteras, ligaduras e pausas

- **\`writtenDuration\`** diferencia a **figura escrita** (\`<type>\`) da duração temporal real (\`<duration>\`). Em uma tercina, por exemplo, três semínimas podem caber no tempo de duas: cada figura deve continuar desenhada como **semínima**, mesmo com duração curta na linha musical.
- \`time-modification\` guarda quantidade de notas reais/normais; \`notations/tuplet\` identifica início/fim; o render mostra a indicação numérica de quiáltera no início da figuração.
- Pausas e bandeirolas utilizam a figura \`<type>\` quando presente.
- **Ligadura de frase (slur)** ganha um arco gráfico entre início e fim encontrados no mesmo tile SVG, separado da **ligadura de união (tie)** que prolonga a nota no MIDI e no Score.
- Quiálteras aninhadas, colchetes de quiáltera completos, ligaduras atravessando limites de tile SVG, vozes densas com colisões e todas as convenções de notação ainda **não** estão integralmente finalizadas.


### Posicionamento das pausas (ajuste complementar)

A **pausa de semibreve** foi reposicionada para ficar suspensa pela **quarta linha da pauta** (segunda contando de cima). A **pausa de mínima** é apoiada sobre a **terceira linha**, com o retângulo acima dela. A posição e a figura impressa são testadas também quando a duração executada foi alterada por uma quiáltera (`ClefEngravingTest.js`).

## 5. Testes de regressão e aceite

- \`tests/ClefEngravingTest.js\`: verifica G4, F3, C4 contra as linhas da pauta, alinhamento verdadeiro do SVG da espiral e dos dois pontos, posicionamento mesmo com a clave transferida para outra pauta, armadura G→F na pauta fixa, tipologia de figuras e arco de slur.
- \`tests/musicxml_visual_crosscheck.js\`: inclui fixture com **F♯ na armadura, F♮ explícito ou inferido, repetição de acidente no mesmo compasso, restabelecimento no compasso seguinte e tercina 3:2 com <type> quarter**. Além disso mantém validação dos 2.335 ataques das dez partituras (início, pitch, pauta).
- \`tests/TemporalLayoutTest.js\`: proporcionalidade horizontal por semínimas; os testes anteriores de Score/MIDI/Espera/calibração/backup e navegador de compassos devem continuar aprovados.
- Compilação Android e assinatura da beta precisam passar **no commit exato**; o emulador verifica instalação, biblioteca, teclado e partitura. A prova de desenho por coordenadas **não equivale** a revisão visual em tamanho real no Realme e Lenovo; telas e densidades podem exigir ajustes de espessura/cor.

## 6. Próximos trabalhos

Avaliação visual detalhada das dez partituras, quiálteras com colchetes e agrupamentos complexos, ligaduras que atravessam tiles ou sistemas, colisões de vozes e pausas, alterações de armadura específicas de cada pauta, dinâmica, acentuação e expressividade. Continuar validação física Yamaha P-45 nos dois aparelhos.

**Contratos preservados:** o modo Espera permite notas extras com aviso; acordes só exigem simultaneidade configurada após calibração real; duração/pedal são orientativos; teclado guia representa sempre as **duas mãos** independentemente da seleção de estudo.

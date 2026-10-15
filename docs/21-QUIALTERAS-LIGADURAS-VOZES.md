> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 21 — Quiálteras, ligaduras e vozes simultâneas

Data de referência: 14/10/2026. Versão interna 0.9.8-beta, versionCode 17, após a 0.9.7. Este marco é incremental e não conclui o suporte ao MusicXML.

## Alterações implementadas

**Quiálteras:** o leitor MusicXML preserva number, bracket, placement, actual-notes e normal-notes, e identifica os eventos start/stop. O colchete e o número são calculados para toda a quiáltera, não para cada nota isolada. Quando bracket=no, mantém o número sem linha. A duração executada continua independente da figura escrita.

**Ligaduras:** collectSpanners identifica slurs pela pauta, voz e número, e ties pela pauta, voz e altura. O par de extremos é calculado na partitura inteira. drawScoreSpanners desenha arcos quadráticos em segmentos por bloco de 1160px, subdivididos matematicamente nas fronteiras; evita que ligaduras longas desapareçam ou exijam SVG gigantes. Slur indica fraseado e não prolonga MIDI; tie representa união, com duração musical tratada no modelo Java.

**Polifonia:** arrangeVoices diferencia visualmente duas vozes independentes em uma mesma pauta. Hastes preferencialmente em direções opostas, cabeças coincidentes afastadas horizontalmente e pausas deslocadas para lados diferentes. A função não altera pitch MIDI, beat, duração, pauta ou escolha de mãos. Arcos usam o deslocamento de cabeças quando necessário.

## Testes e critérios

NotationSpannersTest.js verifica uníssono polifônico, hastes, cabeças e pausas separadas; slur/tie em três blocos SVG; colchete de tercina que atravessa a fronteira e número único; bracket=no e coordenadas válidas.

musicxml_visual_crosscheck.js contém fixture MusicXML adicional com backup entre vozes, slur do primeiro ao terceiro compasso e quiáltera 3:2 com bracket=yes; conserva a auditoria das dez músicas embarcadas (2.335 ataques).

Não há evidência física de teste no Yamaha P-45. Para aceitar uma versão nova, exigir workflows aprovados de testes Java/JS, compilação/assinatura e smoke de emulador relativos ao mesmo commit.

## Limitações e continuação

O renderizador temporal não substitui ainda um gravador profissional: polifonia com 3–4 vozes densas, beaming entre tiles, armaduras não convencionais, quiálteras aninhadas e colisões complexas exigem melhorias. Arcos por mudança de sistema na Leitura Livre permanecem a cargo do OSMD. Próxima fase: corpus de partituras polifônicas difíceis, capturas reais de telas e verificação no Lenovo/Realme.

Decisões pedagógicas vigentes: notas extras no Espera são indicadas sem bloquear; janela de acordes somente após calibração; duração/pedal orientativos; teclado virtual sempre apresenta as duas mãos.
> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 14 — Teclado guia independente da seleção de mãos (beta 0.9.2)

**Data:** 14/10/2026. **Código de referência:** commit c099960876bcf5ff48f897c8e49ed3645da23fcf. **Versão interna:** versionCode 11, versionName 0.9.2-beta. O teclado é uma **representação gráfica**, não sintetizador de áudio.

## Decisão de produto

O usuário determinou que o teclado virtual **sempre represente as duas mãos da partitura**, mesmo se selecionar somente mão direita, somente esquerda ou ambas para **praticar**. O seletor de mãos continua comandando quais notas são exigidas no modo Espera e quais são enviadas ao Yamaha na Demonstração, **mas jamais filtra as notas amarelas do teclado guia**.

As teclas amarelas são as notas que deveriam estar soando naquele ponto da partitura **em ambas as pautas**; as verdes são notas efetivamente detectadas por USB-MIDI no instrumento. Uma mesma tecla pode aparecer uma vez quando ambas as mãos compartilham a mesma altura. A cor verde tem precedência quando há pressão real.

## Defeitos corrigidos em relação à 0.9.1

1. Antes, PianoView.drawKeyboard consultava score.nextForHand(position, hand), selecionando **uma única mão** e somente o próximo ataque. Agora usa KeyboardGuide.at(position), que examina as duas pautas e respeita o intervalo real da nota desde seu início até o término. Inclui acordes e notas longas.
2. Antes, ao abrir o teclado enquanto a música estava pausada, o playhead do guia podia estar no zero. Agora setKeyboardOpen define playhead a partir de MainActivity.position antes de renderizar.
3. Em displayScore, a partitura recém-carregada passa a definir score e beat inicial também no guia.
4. Durante deslocamento, followScore atualiza o beat do guia, redesenhando-o quando o teclado está visível. O filtro de mão na MainActivity não altera mais virtualKeys.hand; o guia é sempre de ambas.
5. O teclado pode usar uma janela mais ampla que quatro oitavas se necessário para mostrar **simultaneamente** extremos graves e agudos de ambas as mãos, além de teclas recebidas pelo MIDI. Respeita a faixa física do Yamaha P-45 (MIDI 21–108), sinalizando alturas fora do instrumento. As 88 teclas podem ficar pequenas quando o trecho exige extremos muito distantes.

## Arquitetura e sincronização

- **Novo componente Java puro KeyboardGuide.java**, usado pelo PianoView.
- O constructor ordena os eventos de Score.events por onset e cria índice prefixado de máximo fim. Em cada redraw, uma busca binária encontra as entradas com onset não posterior ao beat; o prefixo permite interromper a busca reversa quando não há mais notas sustentadas possíveis. Isso evita percorrer toda a música a cada frame.
- A coleção Frame tem conjuntos **right**, **left** e **both**, independentes do hand selecionado. 'both' é a união das notas efetivamente soando nos intervalos [onset, onset+duration). Partituras com ties já vêm consolidadas por Score.read.
- KeyboardGuide.range seleciona limites de visualização incluindo todas as alturas esperadas e pressionadas **dentro das 88 teclas físicas**. A janela padrão tem cerca de quatro oitavas, mas cresce quando necessário.
- **PianoScreens** continua enviando o beat atual via followScore e desenha guia opcional na parte inferior; ao abri-lo, cabeçalhos superiores ocultam-se para preservar a altura da partitura.
- O modo Espera e a Demonstração continuam com **filtros pedagógicos próprios** e **não recebem mudanças de lógica** nesta atualização.

## Limites de interpretação

- **Não há som produzido pelo teclado desenhado.** As teclas amarelas representam musicalmente ambas as mãos, e as verdes indicam teclas físicas recebidas. A saída MIDI de demonstração no Yamaha ainda respeita a mão de estudo escolhida; isso é propositalmente independente da imagem.
- O renderizado amarelo mostra intervalos musicais da partitura conforme Score.events, não reproduz dinâmica sonora real, pedal CC64, fraseado/articulação ou efeitos de reverberação.
- A política pedagógica de notas extras, acordes em milissegundos e sustain continua pendente; o visual de ambas as mãos não implica que o usuário tenha de tocar ambas.
- O usuário deve confirmar visualmente o comportamento no Realme 7 5G e Lenovo Tab M9, especialmente teclas muito graves e agudas e acordes separados por muitas oitavas.
- Não confundir validação de dados musicais em testes JVM e funcionamento de WebView/Canvas no emulador com medições físicas no Yamaha.

## Plano de testes e resultados

O novo teste automatizado **KeyboardGuideTest.java** compara, sobre as dez partituras integradas, o conjunto de alturas soando em cada beat-chave contra uma implementação simples de referência. Abrange onsets, interior das notas, término, acordes com ambas as mãos, notas longas, repetição e teclas pressionadas fora do conjunto esperado. O cálculo de ambas as mãos não tem parâmetro 'hand', impedindo esse acoplamento acidental.

GitHub Actions:
- [Build, assinatura e testes JVM](https://github.com/atshimura/piano-livre/actions/runs/38590540700) — verificar conclusão do run (a execução usa o commit de código c099960...).
- [Emulador Android](https://github.com/atshimura/piano-livre/actions/runs/38590540922) — verificar conclusão de interface.
- [Auditoria MIDI](https://github.com/atshimura/piano-livre/actions/runs/38590540570) — teste independente de reconhecimento, sem modificações pedagógicas.

## Aceite presencial

1. Abrir Ode à Alegria com teclado virtual e observar as notas simultâneas nas duas pautas.
2. Selecionar Direita, Esquerda e Ambas na mesma posição e confirmar que **as teclas amarelas continuam representando as duas mãos**.
3. Pausar, deslocar/reiniciar e reabrir o teclado: a indicação deve ser da posição **atual**, não sempre do início.
4. Usar Gymnopédie, Asa Branca e Bella Ciao: conferir notas graves e agudas, acordes e sustentações.
5. Tocar algumas notas MIDI: verde deve reagir às teclas físicas sem apagar os destaques amarelos das outras notas esperadas, salvo sobreposição de cores no mesmo pitch.
6. Confirmar que apenas as notas exigidas da mão selecionada habilitam o avanço no modo Espera, **sem prejudicar** a representação gráfica das duas pautas.

**Se o objetivo passar a ser ouvir o teclado virtual tocar as duas mãos**, será um requisito novo e separado de **reprodução de áudio/MIDI**, não uma consequência da exibição gráfica. O modo Demonstração atual continua filtrado por mão.

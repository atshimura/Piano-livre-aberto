> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 18 — Evolução do motor musical (Piano Livre 0.9.5-beta)

Data: 14/10/2026. Versão interna: **0.9.5-beta / versionCode 14**. Este documento descreve uma entrega incremental do motor musical e não a V1 concluída. O Yamaha P-45 ainda não foi testado fisicamente com os dois aparelhos-alvo.

## 1. Andamento variável

O parser Android **Score.java** conserva uma lista de mudanças de andamento (TempoChange) associadas ao beat absoluto em semínimas. Interpreta MusicXML direction/sound tempo e indicações impressas de metrônomo (unidades whole, half, quarter, eighth, 16th e 32nd; com até três pontos de aumento), convertendo para semínimas por minuto.

Os métodos **tempoAt(beat)** e **advanceBeats(beat, elapsedMs, tempoFactor)** consultam os trechos e integram corretamente os milissegundos quando uma atualização do relógio cruza uma mudança de BPM. A variável antiga score.tempo representa o BPM inicial apenas. O ticker de **MainActivity** usa esses métodos nos modos Espera, Contínuo e Demonstração. **PianoScreens.followScore** informa ao WebView o BPM do instante atual, e **temporal-notation.js** registra os tempos no relógio absoluto de semínimas. O indicador de BPM da tela também acompanha o andamento local.

Não se presume suporte a ritardando progressivo sem indicações temporais explícitas, fermatas, swing inferido ou expressão humana.

## 2. Repetições escritas

**Score.repeatSections** registra início, término e quantidade de passagens de barras MusicXML repeat forward/backward. **RepeatNavigator** conta os ciclos para saltar na execução apenas as vezes indicadas e depois prosseguir. Uma abertura implícita no início da peça é reconhecida.

O motor consulta as repetições simples nos modos Espera, Contínuo e Demonstração. Na Demonstração, efetua seek MIDI na volta, desligando notas ativas conforme as garantias do MidiPlayback. A seleção manual **A–B tem precedência** sobre repetições escritas. Início e nova execução reiniciam os contadores.

**Limitação importante:** casas de 1ª/2ª vez, repetições aninhadas, D.C., D.S., Segno, Coda e Fine ainda não têm grafo de ordem de execução. O código registra warnings e desativa a repetição simplificada quando há estruturas incompatíveis; a peça é executada linearmente, com aviso ao abrir. Não anunciar que essas músicas complexas são reproduzidas fielmente.

## 3. Compassos, vozes, pausas e ligaduras

O parser preserva staff/voice e respeita MusicXML backup/forward para vozes simultâneas. Ligaduras de união (tie start/stop) da mesma altura, pauta e voz compõem um único ataque sustentado, sem novo Note On artificial na continuação. Pausas não geram Note On. A duração escrita global **durationBeats** inclui pausas finais que anteriormente eram cortadas, e a primeira medida implícita pode ter duração de anacruse em vez de compasso completo.

Continua necessária validação ampla de vozes cruzadas, ornamentação, quiálteras, dinâmica, transposição e notação multissistema. Não confundir ligadura expressiva (slur) com união (tie).

## 4. Testes automatizados

- **TempoMapTest.java:** mudança de BPM dentro do compasso e na transição para o próximo, velocidade percentual, integração ao cruzar várias velocidades, metrônomo de mínima e semínima pontuada, valores inválidos.
- **RepeatNavigatorTest.java:** abertura explícita/implícita, múltiplas passagens, fim da repetição, reset e aviso de casas de repetição não suportadas.
- **MusicStructureTest.java:** duas vozes independentes com backup, pausas, duração de tie atravessando compasso, anacruse, pausa final e advertências D.C./Fine.
- **musicxml_visual_crosscheck.js:** partitura Java versus JS em dez MXL e fixtures de direção de andamento em beats absolutos e metrônomo com unidade/ponto.
- Conservar também testes de notas, mãos, teclado guia nas duas pautas, Espera/calibração, backup v2, MIDI de saída e emulador.

É indispensável verificar o run de build/assinatura e o emulador relativos ao commit de entrega, não a uma revisão antiga.

## 5. Próximos problemas musicais

1. Criar grafo completo de ordem de compassos para casas alternadas, D.C./D.S./Coda/Fine e repetições aninhadas, compartilhado entre partitura, teclado e saída MIDI.
2. Consolidar o modelo de eventos Java e JS, hoje interpretados em paralelo e comparados em testes.
3. Aperfeiçoar a fidelidade da notação: alterações locais de clave/armadura, vozes/pausas complexas, notas de graça, ornamentos, dinâmica, pedal e polirritmia.
4. Medir temporização real, latência e ausência de notas presas no Yamaha P-45 com Realme 7 5G e Lenovo Tab M9.

**Não mudar regras aprovadas do modo Espera:** notas extras avisadas sem bloqueio, acorde com limite somente depois da calibração e duração/pedal orientativos.

> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

> **Atualização posterior à auditoria:** as falhas F1–F4 foram abordadas na beta **0.9.1**. Este relatório permanece como diagnóstico histórico do código anterior; os números aqui são exposição potencial da versão anterior. Consulte [13 — Correções e validação 0.9.1](13-CORRECOES-MIDI-0.9.1.md) para o estado atualizado. O Yamaha P-45 físico ainda não foi testado.

# 12 — Auditoria do reconhecimento MIDI e das mãos (14/10/2026)

**Conclusão: NÃO aprovado como inteiramente correto.** Foram constatados problemas reproduzíveis no modo Espera, na troca de mão, na cobertura do teclado guia e em partituras MusicXML importadas com partes separadas. **O código funcional não foi alterado nesta auditoria:** somente testes, workflow e documentação.

## Evidências e metodologia

- Código inspecionado: MainActivity.java, MidiProtocol.java, MidiPlayback.java, Score.java, PianoScreens.java e PianoView.java; dez arquivos MXL incluídos.
- Testes adicionados: **PianoLivreAndroid/tests/RecognitionAudit.java** e **tests/recognition_corpus_audit.py**.
- Execução verificada: **[GitHub Actions — Audit MIDI notes and hand selection, run 38581575918](https://github.com/atshimura/piano-livre/actions/runs/38581575918)**, resultado do workflow success.
- **Atenção:** success significa que a auditoria rodou; o próprio script registra falhas encontradas como FINDING. **Não significa que o produto passou.**
- **Sem acesso físico:** não houve teste USB real com Yamaha P-45, Realme 7 5G ou Lenovo Tab M9. Tampouco foi executada a lógica inteira da Activity com mensagens USB sintéticas: o decodificador e o parser de eventos são classes reais, enquanto as falhas no Espera foram simuladas reproduzindo o predicado literal encontrado na Activity.

## Resultado favorável dos testes

| Verificação | Resultado real | Limite |
|---|---|---|
| Decodificador de bytes MIDI | **18.432 de 18.432** mensagens sintéticas corretas | 128 alturas, 16 canais, 3 tipos (Note On, Off e On com velocity zero), 3 ciclos, fragmentação e real-time intercalado; não valida conexão física |
| Parser das dez MXL | **2.335 eventos** normalizados, para **2.372 notas** com altura no XML bruto | Junção de ligaduras altera contagem; não é comparação musical nota a nota no instrumento |
| Divisão Ambas/Direita/Esquerda | **1.374 instantes** analisados | Ambas = união de eventos de staff 1 e staff 2 nos arquivos embutidos |
| Estrutura do acervo | **10 de 10** músicas com uma parte musical e duas pautas | Não garante atribuição correta em qualquer MusicXML importado |

## F1 — Falha no modo Espera: tecla errada pode avançar

**Local:** MainActivity.onKey, condição de aprovação por inclusão do conjunto de teclas pressionadas no conjunto das notas esperadas (**downKeys.containsAll(required)**).

**Falha:** não se exige que a tecla recém-pressionada pertença às notas exigidas. Exemplo: a posição anterior e a atual exigem Dó4; o aluno mantém Dó4 pressionado; na posição atual, toca **Ré4 errada**. A condição ainda encontra Dó4 pressionado e avança indevidamente.

- **264 casos sintéticos reproduzíveis**: 88 teclas do P-45 × três seleções de mão, reproduzindo exatamente o teste de inclusão do código atual.
- **286 pares adjacentes de posições** nas dez músicas (somados os três filtros de mão) apresentam condição de vulnerabilidade **se** o aluno mantiver as teclas anteriores. Não são 286 falhas observadas fisicamente.
- **Notas extras:** o sistema aceita conjunto correto + tecla extra errada (88 de 88 casos sintéticos). Determinar a regra pedagógica para notas extras **depende de decisão**, não presumir critério.
- **Acordes:** não há janela definida para simultaneidade; teclas pressionadas em instantes distantes podem satisfazer o acorde.
- **Duração/pedal:** não se verifica adequadamente duração escrita ou sustentação CC64 para permitir o avanço.

**Encaminhamento:** projetar avaliador de eventos com histórico de ataques/liberações, rastrear mudanças reais de nota, ties, notas repetidas, pedal, simultaneidade com limiar aprovado e casos negativos. Testar com MIDI físico após correção.

## F2 — Trocar a mão selecionada pode reiniciar o trecho incorretamente

**Local:** PianoScreens.buildStudio, seleção dos botões Ambas/Direita/Esquerda. Quando não há nota da mão escolhida exatamente na posição atual, a posição volta ao **primeiro evento daquela mão na peça**.

**Teste:** **866 combinações posição/mão** nas dez partituras satisfazem essa condição; nesses pontos, trocar a mão pode causar retrocesso ao começo. São oportunidades condicionais, não 866 eventos observados.

**Encaminhamento:** preservar beat atual ou posicionar no próximo onset apropriado **a partir do trecho atual**, respeitando loop; testar no início, meio, fim e em pausas.

## F3 — Teclado guia não exibe toda a faixa do piano

**Local:** PianoView.drawKeyboard apresenta apenas MIDI **48 a 95** (Dó3 a Si6). O Yamaha P-45 tem 88 teclas (MIDI **21 a 108**, Lá0 a Dó8).

**Teste:** **265 notas-eventos**, presentes em **8 das dez partituras**, estão fora do teclado gráfico. Podem ser lidas pelo MIDI, mas **não aparecem destacadas no guia**.

**Encaminhamento:** teclado com faixa móvel/rolável ou indicação clara de notas fora da janela, preservando espaço da partitura e comportamento imersivo.

## F4 — Partituras MusicXML de múltiplas partes podem confundir as mãos

**Local:** Score.read identifica mão pela propriedade numérica staff, sem considerar a parte MusicXML à qual a pauta pertence. O número de pauta é **local a cada parte**.

**Reprodução:** um XML com **P1 Dó4 (staff 1)** e **P2 Sol3 (staff 1)** resulta em **ambas as notas classificadas como mão direita**. Direita recebe as duas; Esquerda não recebe nenhuma.

**Escopo:** falha para partituras importadas nessa estrutura; **não ocorre nas dez músicas pré-instaladas**, que usam uma parte com duas pautas.

**Encaminhamento:** mapear ID da parte + staff local para atribuição explícita de mão; nunca deduzir mão apenas pela altura.

## Riscos adicionais encontrados no código (não medidos fisicamente)

- **Canais sobrepostos:** downKeys é um conjunto somente de alturas, não de canal+nota com contagem; duas fontes que seguram mesma nota podem produzir liberação indevida quando uma delas envia Note Off.
- **Pedal:** CC64 muda apenas um estado de diagnóstico; o modo Espera não avalia a sustentação segundo as regras musicais aprovadas.
- **Desconexão:** disconnectMidi limpa downKeys e fallback, mas não atualiza explicitamente as teclas pressionadas em virtualKeys; pode persistir destaque gráfico até atualização posterior.
- **Mão física:** mensagem MIDI não revela qual mão humana pressionou uma tecla. As opções do app filtram as pautas musicais; não detectam diretamente direita/esquerda física.
- **Latência real, perda de mensagens, velocity, simultaneidade e notas presas** dependem de ensaios com instrumento e adaptador OTG.

## Quantificação por partitura

| Música | Eventos normalizados | Fora do guia | Pares adjacentes vulneráveis | Possíveis retrocessos por mudança de mão |
|---|---:|---:|---:|---:|
| Aquarela | 286 | 0 | 77 | 130 |
| Asa Branca | 330 | 76 | 134 | 90 |
| Bella Ciao | 328 | 44 | 17 | 102 |
| Canon em Ré | 160 | 18 | 2 | 96 |
| Für Elise — Easy | 104 | 1 | 2 | 71 |
| Gymnopédie nº 1 | 455 | 82 | 4 | 102 |
| Lago dos Cisnes | 172 | 31 | 14 | 68 |
| Minueto em Sol maior | 219 | 9 | 10 | 91 |
| Minueto em Sol menor | 200 | 4 | 4 | 77 |
| Ode à Alegria | 81 | 0 | 22 | 39 |
| **Total** | **2.335** | **265** | **286** | **866** |

**Leitura adequada:** números de pares/combinações são locais em que um comportamento pode falhar se o usuário realizar aquela sequência. Não representam falhas reais observadas. Os 286 pares somam seleções Ambas, Direita e Esquerda; 866 contam duas opções individuais de mão em instantes do corpus.

## Condições para uma conclusão positiva futura

1. Corrigir F1 e F2 com testes automatizados diretamente ligados ao novo motor de Espera/seleção.
2. Corrigir F3, manter partitura/teclado ampliado, testar representação de toda faixa usada.
3. Corrigir F4 e adicionar fixtures XML com duas partes, vozes múltiplas, cruzamento de vozes e claves.
4. Definir por aprovação do responsável as regras para nota extra, janela do acorde, notas repetidas, ties e pedal.
5. Testar Note Off por canal, desconexão e falta de notas presas.
6. Repetir o protocolo de **88 teclas**, acordes, pedal, 3 seleções, velocidades, trechos e retomada no **P-45 físico**, com **Realme 7 5G** e **Lenovo Tab M9**, guardando evidências e relatório de regressão.
7. Novos resultados devem distinguir **passou testes automáticos**, **passou no emulador** e **passou em instrumento real**.

**Conclusão honesta:** reconhece e interpreta corretamente os eventos MIDI simulados de byte nas condições verificadas, e filtra estruturalmente as mãos no acervo atual. Ainda **não é correto declarar todas as notas, ambas as mãos e o modo Espera plenamente funcionais**. A auditoria não teve como meta alterar a aplicação.

Referências: [requisitos](01-VISAO-E-REQUISITOS.md), [lógica MIDI](04-MIDI-ESTUDO.md), [estado e roadmap](07-ESTADO-E-ROADMAP.md), [AGENTS](../AGENTS.md).

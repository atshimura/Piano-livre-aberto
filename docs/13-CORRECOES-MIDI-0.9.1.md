> **Histórico técnico de uma beta anterior.** Não é evidência de teste do commit atual. Consulte [AGENTS](../AGENTS.md), [estado](07-ESTADO-E-ROADMAP.md) e [CI pública](06-BUILD-TESTES.md). Links históricos para o repositório privado podem não estar disponíveis.

# 13 — Correções de reconhecimento MIDI e mãos: beta 0.9.1

Data: 14/10/2026. Código validado: commit 4bd1e4b57c6ff6f08b7cf4063b01f5fbd1b3187f. Versão interna: 0.9.1-beta, versionCode 10. É uma beta, não a V1 aprovada como completa. **Sem validação física no Yamaha P-45, Lenovo Tab M9 ou Realme 7 5G.**

## Problemas corrigidos

**Modo Espera: ataques novos.** O novo componente WaitRecognizer.java exige que cada nota esperada receba um Note On genuinamente novo depois de armar a posição musical. Uma nota mantida desde a posição anterior não conta; uma tecla errada não avança se o requisito só estava satisfeito por teclas antigas. Acordes são completados quando todas as alturas exigidas receberam ataques novos e continuam pressionadas ao completar a condição. A janela temporal de simultaneidade ainda não foi definida, tampouco a política de notas extras, duração, articulação e pedal.

**Separação de canais MIDI.** MainActivity agora acompanha fontes por canal e altura: soltar uma nota em um canal não apaga outra fonte da mesma altura. Ao desconectar, limpa o registro de fontes, teclado guia e estado de reconhecimento.

**Mudar Direita/Esquerda/Ambas.** HandNavigation.java mantém o trecho corrente, buscando a próxima nota pertinente da mão selecionada; não volta ao primeiro evento da música como ocorria anteriormente. Se houver repetição ativa, pode retornar ao começo do próprio trecho.

**Demonstração.** MidiPlayback aplica a seleção de mão aos Note On/Off enviados ao piano. Ambas inclui as duas pautas; Direita usa pauta 1; Esquerda usa pauta 2 ou as demais pautas suportadas.

**MusicXML em duas partes separadas.** Score.java e temporal-notation.js mapeiam corretamente duas partes independentes com uma pauta cada, mesmo quando ambas usam o número local de pauta 1. Mais de duas partes ou partes com múltiplas pautas ainda exigem atribuição manual e são rejeitadas, em vez de classificadas silenciosamente. A convenção de parte 1 à direita e parte 2 à esquerda não cobre todo arranjo importado.

**Teclado guia.** PianoView.java escolhe uma janela móvel de aproximadamente quatro oitavas, acompanhando as notas esperadas, dentro do âmbito das 88 teclas do Yamaha (MIDI 21–108). Notas fora da janela são indicadas, sem prometer exibir as 88 teclas simultaneamente. Modo ampliado do teclado preservado.

## Testes realmente concluídos

| Teste | Resultado | Evidência |
|---|---|---|
| Decodificação MIDI | 18.432 mensagens simuladas em 128 alturas, 16 canais, três formas, três ciclos | [Auditoria aprovada](https://github.com/atshimura/piano-livre/actions/runs/38586118824) |
| Espera: teclas erradas, mantidas e ataques novos | 1.320 casos sintéticos cobrindo as 88 teclas e três modos de mãos em cinco repetições | [Auditoria aprovada](https://github.com/atshimura/piano-livre/actions/runs/38586118824) |
| Regressão musical | 18.121 asserções; 4.122 combinações posição/mão nas dez MXL; acordes, reataques, partituras com partes separadas e demonstração por mão | [Build e testes aprovados](https://github.com/atshimura/piano-livre/actions/runs/38586119125) |
| APK Android e assinatura | Compilação e verificação da assinatura da beta concluídas | [Build aprovado](https://github.com/atshimura/piano-livre/actions/runs/38586119125) |
| Emulador Android | Instalação, atualização preservando sonda de dados e smoke test de interface aprovados | [Emulador aprovado](https://github.com/atshimura/piano-livre/actions/runs/38586119067) |

**Importante:** 18.121 asserções são verificações de software, não 18.121 notas recebidas do piano real. Testes de MIDI foram simulados, sem hardware conectado. O resultado verde do workflow não equivale a compatibilidade física nem prova absoluta de ausência de erros.

## Verificações ainda indispensáveis no P-45

1. Conectar Yamaha ao Realme e ao Lenovo e percorrer as 88 teclas individualmente, verificando correspondência das notas, Note On/Off e ausência de teclas presas.
2. Testar escalas, acordes de 2–5 notas, notas repetidas, sustentações e pedal, em velocidades distintas.
3. No modo Espera, manter a tecla da nota anterior e pressionar outra errada: não deve avançar. Liberar e tocar corretamente: deve avançar; repetir também com acordes.
4. Alternar Direita, Esquerda e Ambas em Ode à Alegria, Asa Branca e Gymnopédie, no início, no meio e perto do final, incluindo trechos em repetição.
5. Ouvir a Demonstração por mão isolada e em ambas, verificar Note Off ao pausar e desconectar.
6. Medir latência, eventuais notas perdidas e resposta de tela nos dois dispositivos; registrar modelo do cabo/adaptador, música, BPM, logs e gravação.

## Riscos pendentes

- MIDI informa a tecla e o canal, não identifica a mão física que a pressionou.
- Não há limite pedagógico aprovado em milissegundos para acordes. O modo Espera exige teclas ainda pressionadas, mas aceita ataques sequenciais distantes no tempo.
- Pedal CC64, análise rigorosa de duração e articulação, vozes cruzadas e repetições complexas precisam de implementação e testes adicionais.
- O guia exibe apenas uma janela móvel, com aviso para alturas fora dela.
- A atribuição das mãos em MusicXML com arranjos mais complexos precisa de configuração explícita.
- O backup/restauração manual ainda exige melhorias de integridade e recuperação; ver docs/05.

Referências: [auditoria histórica anterior à correção](12-AUDITORIA-RECONHECIMENTO-MIDI-MAOS.md), [arquitetura MIDI](04-MIDI-ESTUDO.md), [estado e roadmap](07-ESTADO-E-ROADMAP.md) e [orientações para IAs](../AGENTS.md).

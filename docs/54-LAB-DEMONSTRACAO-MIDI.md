# 54 — Demonstração sonora offline por USB-MIDI (0.9.45)

## Objetivo
Reproduzir uma referência musical no Yamaha conectado, com controles Demonstrar/Parar, velocidade 50–150% e repetição opcional. Não adicionar servidor, biblioteca sonora, síntese enganosa nem latência calculada sem calibração.

## Implementação
`MusicLabPlaybackPlan` gera eventos Nota Ligada/Desligada determinísticos para catálogo do Laboratório. Acordes e intervalos tocam juntos, escalas e arpejos em sequência ascendente; a duração demonstrativa é uma semínima convencional a 100 BPM inicialmente, com 90% de duração ligada, e **não é critério pedagógico**. `MusicLabScreen` envia eventos pela saída USB-MIDI atual e mostra teclas ativas, com agendamento por Handler Android. Repetição e velocidade são opcionais. Saída ausente é avisada, não há synthesizer. Saída do Laboratório, desconexão e app em segundo plano cancelam sessões e liberam notas para reduzir risco de notas presas.

## Limites e testes
Teste JVM `MusicLabPlaybackPlanTest`: ordem, ataques de acorde, velocidades, finais e entradas inválidas. O relógio do Handler não é temporizador MIDI de áudio de precisão; interrupções/latência em dispositivos físicos não foram medidas. A demonstração **não** reproduz as durações editoriais de MusicXML: usa padrões esquemáticos do catálogo. Validar CI/Android e testar Yamaha P-45/Realme/Lenovo conforme #11.

## Revisão de integração do PR #44 (17/10/2026)

A branch foi reconciliada com a `main` 0.9.44 por merge de dois pais sem reescrever o commit original, preservando o catálogo, a busca, a prática por classes de altura e a identificação espontânea monofônica do PR #42 (inclusive sobreposição de entrada USB-MIDI/virtual e limpeza de mensagens).

### Cancelamento e estados

A implementação inicial confundia `demoHeld.isEmpty()` com reprodução parada: entre duas notas, ou no intervalo entre repetições, o botão podia iniciar uma segunda demonstração. `MusicLabPlaybackPlan.Session` possui estado ativo independente do conjunto de notas MIDI ligadas e número de geração. **Parar** invalida imediatamente os callbacks agendados, mesmo quando nenhuma tecla está ativa. No meio de nota/acorde, retorna as notas ligadas para Note Off antes de fechar a conexão. Troca de exercício ou filtro, velocidade, alternância Tocar/Montar, Descobrir/Aprender, saída do Laboratório, pausa da Activity ou desconexão USB cancelam a demonstração. Ao desmarcar Repetir, a execução corrente termina e não reinicia; Parar durante a repetição interrompe imediatamente. Uma sessão nova ignora callbacks antigos.

Intervalo de uníssono não cria duas mensagens simultâneas Note On para a mesma altura; uma ligação e um desligamento são suficientes. Os eventos são **padrões didáticos convencionais**, não correspondem a ritmo, articulação, dinâmica ou dedilhado da partitura. O destaque visual da demonstração se soma às teclas pressionadas fisicamente, mas não entra na pontuação da prática. O áudio requer saída real USB-MIDI (o app não contém sintetizador).

### Verificações e restrições

O teste JVM `MusicLabPlaybackPlanTest` cobre ordenação, taxa 50/150%, acordes, nota/off, uníssono, parada em pausa silenciosa, parada durante nota, repetição, troca de sessão e callbacks atrasados. O smoke Android verifica que o botão informa ausência de saída MIDI no emulador sem tentar sintetizar áudio. Testes gerais JVM, JavaScript, MusicXML, MIDI, build debug/release, assinaturas e atualização no emulador devem ser conferidos no SHA do PR e no merge. Não existem testes físicos do Yamaha P-45, Realme 7 5G ou Lenovo Tab M9: perda abrupta de energia/porta USB pode impedir que o instrumento receba Note Off, e não é possível garantir ausência absoluta de notas presas sem validação física (#11). Recursos do PR #45 (prática sequencial) não foram antecipados.

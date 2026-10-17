# 54 — Demonstração sonora offline por USB-MIDI (0.9.45)

## Objetivo
Reproduzir uma referência musical no Yamaha conectado, com controles Demonstrar/Parar, velocidade 50–150% e repetição opcional. Não adicionar servidor, biblioteca sonora, síntese enganosa nem latência calculada sem calibração.

## Implementação
`MusicLabPlaybackPlan` gera eventos Nota Ligada/Desligada determinísticos para catálogo do Laboratório. Acordes e intervalos tocam juntos, escalas e arpejos em sequência ascendente; a duração demonstrativa é uma semínima convencional a 100 BPM inicialmente, com 90% de duração ligada, e **não é critério pedagógico**. `MusicLabScreen` envia eventos pela saída USB-MIDI atual e mostra teclas ativas, com agendamento por Handler Android. Repetição e velocidade são opcionais. Saída ausente é avisada, não há synthesizer. Saída do Laboratório, desconexão e app em segundo plano cancelam sessões e liberam notas para reduzir risco de notas presas.

## Limites e testes
Teste JVM `MusicLabPlaybackPlanTest`: ordem, ataques de acorde, velocidades, finais e entradas inválidas. O relógio do Handler não é temporizador MIDI de áudio de precisão; interrupções/latência em dispositivos físicos não foram medidas. A demonstração **não** reproduz as durações editoriais de MusicXML: usa padrões esquemáticos do catálogo. Validar CI/Android e testar Yamaha P-45/Realme/Lenovo conforme #11.

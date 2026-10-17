# 55 — Prática livre de escalas/arpejos com ordem (0.9.46)

## Entrega
A prática do Laboratório agora distingue escalas e arpejos de acordes. Escalas/arpejos usam `MusicLabPracticeSequence` para conferir *em ordem* cada ataque MIDI/tecla virtual Tocar: nota correta avança; notas fora da sequência são contadas como extras sem bloquear o avanço e sem limite de tempo. Mostra próxima nota, quantas foram tocadas e extras. Acordes/intervalos continuam por coleção de classes de altura, com as limitações já descritas. A seleção Ambas/Direita/Esquerda é apenas **referência do usuário**, não inferência da mão física a partir da altura das notas. Não há pontuação global ou dedilhado obrigatório.

## Testes e limites
`MusicLabPracticeSequenceTest` verifica progresso, repetição, nota errada não bloqueante, reset e entradas inválidas. O estudo é sem avaliação rítmica, sem pedal/duração julgados, sem dedilhado inferido. Ainda faltam histórico persistido, indicador rítmico/metrônomo opcional e calibração real de tolerâncias. Verificar CI/emulador do head antes de merge. Yamaha/Realme/Lenovo em #11.

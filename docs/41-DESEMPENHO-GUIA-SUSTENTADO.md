# Teclado guia com notas longas — 0.9.32 / code 41

A busca anterior usava máximos de término por prefixo. Uma nota grave sustentada por muito tempo mantinha o prefixo ativo, obrigando percorrer todas as notas curtas intermediárias já encerradas a cada frame do guia. Isso podia crescer linearmente com o avanço de uma partitura longa.

A busca agora usa árvore de máximos de término e poda subárvores inteiramente encerradas. O índice é construído uma vez por partitura, fora do ticker. Mantém as mesmas condições de início/fim e máximos por altura/mão para intensidade e duração. Ambas as mãos, sobreposição de alturas e prioridade do verde MIDI permanecem iguais. Há maior memória de índice: até quatro doubles por evento, alocados uma vez; não se cria árvore em cada frame.

KeyboardGuideLoadTest usa 65.537 eventos sintéticos, incluindo um baixo longo e 65.536 notas curtas. São 618 verificações: igualdade com varredura de referência para notas/intensidades/durações, bordas e consultas vazias. Nas 200 posições de carga, cada consulta visita menos de 140 nós; não depende de limiar de tempo da máquina. Não é medição de FPS/GPU nem prova de fluidez em hardware.

Regressões locais aprovadas: KeyboardGuideTest 15.228 asserções, KeyboardExpressionTest e MusicTimelineConsistencyTest 26.237 asserções/8.172 amostras. Confirmar Actions do commit e emulador antes de integrar. Esta correção adicional continua a consolidação sem reabrir nem invalidar os testes anteriores; aceite físico permanece na issue #11.

Próxima ação: confirmar/ajustar ensaio de atualização e publicação no PR #29; confirmar CI desta correção, integrar sob proteção e verificar APK/pré-release do novo merge. Se surgir falha independente, corrigir; não declarar V1 sem os testes físicos documentados em 40.

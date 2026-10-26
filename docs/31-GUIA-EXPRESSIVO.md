# Etapa 3 — Guia visual expressivo (0.9.21/code30)

A seleção pedagógica de mãos continua sem filtrar o teclado visual. Amarelo conserva as notas escritas de ambas as pautas; verde conserva prioridade para MIDI recebido. Não há sintetizador novo nem exigência adicional de intensidade/duração/pedal.

KeyboardGuide.Frame acrescenta mapas imutáveis de intensidade e fração de duração restante, separados por pauta/mão. Para notas sobrepostas na mesma tecla, conserva os dois mapas; no desenho a tecla continua única. Intensidade agregada usa a maior velocity ativa e as barras usam a maior fração restante por mão. Identificadores D/E e duas barras distinguem a participação das pautas sem mudar a convenção amarela aprovada. Em teclas muito estreitas as letras são omitidas para evitar sobreposição, mas as barras permanecem.

O tom amarelo representa a intensidade do modelo; quando o XML não tem dinâmica numérica, vale a intensidade histórica padrão. Letras p/mf/f sem valor numérico não ganham uma conversão inventada. Barras encolhem com o relógio musical e permanecem orientativas no Espera.

## Testes

KeyboardExpressionTest verifica intensidades distintas por mão, frações restantes, nota escrita com dinâmica zero ainda visível, snapshots imutáveis, mesma altura nas duas pautas com durações/intensidades distintas e término/beat inválido. KeyboardGuideTest e MusicTimelineConsistencyTest continuam aprovados localmente (15.228 e 26.237 asserções, respectivamente). A compilação Android e o smoke com abertura/fechamento do teclado precisam dos Actions da própria entrega.

## Limites e continuidade

Ainda precisa inspeção física em Realme/Lenovo para legibilidade das 88 teclas e das barras pequenas, além de Yamaha para intensidade/duração reais. Verde indica pressão MIDI, não certificação da execução. Não infere mão humana pela altura.

Depois da validação desta entrega, auditar desconexão/notas presas e troca de modo/mão mantendo novos ataques, extras não bloqueantes, janela somente após calibração e CC64 por canal. Testes físicos permanecem em #11 e não impedem tarefas independentes.

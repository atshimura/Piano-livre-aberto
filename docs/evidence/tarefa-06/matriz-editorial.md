# Matriz editorial autoral — Tarefa 06

Referência: MusicXML literal debug-only, independente dos objetos OSMD. Durações abaixo são beats de semínima; todas as amostras usam divisions=60, compasso 4/4 e duas pautas. A grafia não pode ser substituída por equivalentes MIDI.

| Fixture | Passagem de referência | O que a imagem deve permitir verificar |
|---|---|---|
| accidentals, compasso 1 | C♯5, D♭5, E♯5, F5; uma semínima por beat | Dois pares de mesma altura, posições diatônicas diferentes; sinais ♯/♭ |
| accidentals, compasso 2 | B♯4, C5, C♭5, B4 | Enarmonia atravessando limite de oitava |
| accidentals, compasso 3 | F𝄪5, G𝄫5, F♯5, F♮5 | Duplos sinais, alteração e cancelamento |
| accidentals, compasso 4 | Acorde C♯6/D♭6/E♭6, F♮5 de precaução, F♯5, F♮5 | Segundas em linhas suplementares; coluna de acidentes; precaução e cancelamento; captura tardia necessária |
| beams | C5/D5/E5/F5 em semicolcheias; G5/A5 em colcheias; pausa de semínima; B5 | Dois níveis de vigas, grupo separado de colcheias e interrupção pela pausa |
| beams, voz 2 | E4 e F4 semínimas; G4 mínima | Hastes opostas à voz 1 sem alterar onset |
| tuplets, voz 1 | C5/D5/E5 em 3:2 (0, 1/3, 2/3); C5/D5/E5/F5/G5 em 5:4 (1, 1,4, 1,8, 2,2, 2,6); A5 semínima no beat 3 | Número 3 sem colchete com viga; número 5 e colchete em notas sem viga; extremidades corretas |
| tuplets, voz 2 | C4/D4/E4 em 3:2 com colchete; G4 mínima pontuada no beat 1 | Independência de voz, proporção e duração de 3 beats do G4 |
| grace | D♯5 com barra antes de E5; F5/G5 em semicolcheias sem barra e com vigas antes de A5; C6 sem barra antes do acorde B5/D6 | Cabeças pequenas, barra, duas vigas e separação da nota/acorde principal |
| grace, pauta 2/voz 5 | B♭2 sem barra antes de C3 semibreve | Outra voz e pauta; acidente ornamental; nenhum ataque inventado |
| adversarial | D♭5 grace com barra; tercina C5/D5/E5; F♯5/G5 semicolcheias e A5 colcheia; B♭4 e C♯5 semínimas; voz 2 E4/F4/G4 | Combinação de acidentes, vigas secundárias parciais, tercina, graça e polifonia |

Todas as amostras têm C3 semibreve na pauta inferior. Totais principais/ornamentais: 22/0, 11/0, 14/0, 6/5 e 12/1. O MusicXML original é o oráculo gráfico; JVM verifica eventos principais e grafias, Python verifica frações e preenchimento, WebView verifica modelo e SVG real. Capturas precisam de inspeção visual; nenhuma contagem DOM equivale a aceite editorial.

Acidentes de precaução dependem do suporte real OSMD; atributos presentes na fonte não bastam para afirmar parênteses visíveis. Agrupamento inferido sem beams explícitos deve ser descrito como limitação separada se não exercitado. Não usar o número de compasso 3 como se fosse número de quiáltera.

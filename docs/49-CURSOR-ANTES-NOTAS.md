# 49 — Cursor do modo estudo à esquerda das notas (0.9.40)

## Objetivo
Impedir que o cursor vertical cubra a cabeça da nota exigida. A geometria musical mantém o mesmo espaçamento proporcional ao tempo; apenas o indicador de leitura fica antecipado.

## Implementação
- `buildLayout` calcula `cursorX` fixo, preservando a margem da pauta/clave, e um afastamento positivo entre indicador e notas (45–70 px, limitado por viewport).
- O início da linha temporal (`origin`) continua a ser o ponto em que as notas se alinham, agora separado da posição visual da linha do cursor.
- O cursor é elemento fixo sobreposto, não atrelado ao deslocamento do SVG. Largura de viewport é recalculada pelo ciclo de renderização usual.
- Testes determinísticos acrescentados a `TemporalLayoutTest.js` e `TemporalRenderingTest.js`. Sem alteração em MusicXML, MIDI, dados ou acordes.

## Verificações e limites
Confirmar Actions de build/JVM/JS e emulador no SHA do PR antes de integrar, e novamente na `main`. A distância é visual; a posição de estudo e o instante reconhecido continuam ancorados na nota, não na linha visual do cursor. A preparação temporal de 5 s por comando Iniciar continua pendente e deve ter PR/testes próprios. Testes físicos Realme/Lenovo/Yamaha continuam pendentes.

# Etapa 2 — Fixtures compartilhadas do motor musical

## 0.9.17/code26

Primeira entrega incremental; Etapa 2 não encerrada. Corrige perda de durações/offsets decimais em Score.java e clamp indevido de divisions menores que 1 em JS. Preserva identificador textual de voz para unir ligaduras somente na mesma voz. O arquivo fonte importado permanece intacto.

### Fundamentação

MusicXML 4.0 define [divisions como decimal](https://www.w3.org/2021/06/musicxml40/musicxml-reference/data-types/divisions/), [positive-divisions](https://www.w3.org/2021/06/musicxml40/musicxml-reference/data-types/positive-divisions/) para divisions/duration e [voice como string](https://www.w3.org/2021/06/musicxml40/musicxml-reference/elements/voice/). Inteiros são preferidos para interoperabilidade, mas não obrigatórios.

### Casos independentes

Fixtures sintéticas próprias em tests/fixtures/motor; não são músicas redistribuídas nem alteram catálogo. MusicXmlFixtureInventory executa Score.java de produção. MusicXmlFixturesTest executa parser JS e compara ambos com ataques, alturas, pautas, durações, tempo e fim definidos explicitamente, não apenas um parser contra outro.

- decimal-divisions: divisions 0.5/2.5, durações, backup/forward/offset decimais e mudança de compasso; início 0/0.25/0.5/4, fim 7 e andamento no beat 0.75.
- integer-equivalent: mesmos tempos com resolução multiplicada por oito; os resultados devem permanecer iguais.
- named-voices-ties: duas vozes upper/lower na mesma altura; duas notas sustentadas até beat 2, sem fundir vozes ou gerar terceiro ataque.
- ties-voices-grace: duas pautas/vozes, pausa, união entre compassos e nota de graça visual sem ataque/duração no motor.
- tuplet-pickup: durações de um terço, anacruse implícita e início do próximo compasso em 2/3.

### Reprodução das falhas

Antes da correção, fixture decimal falha no Java (primeira duração 0 em vez de 0.5); no JS, durações/inícios ficam pela metade quando divisions=0.5. Fixture named-voices-ties falha no Java com três ataques em vez de dois. Após correções, cinco fixtures passam.

O compilador Java local não tem binário javac no PATH, mas o módulo jdk.compiler está disponível: `java -m jdk.compiler/com.sun.tools.javac.Main`. Assim foram executadas também regressões JVM, reconhecimento MIDI, comparação das dez músicas e guias/rotas. Compilação Android e emulador continuam nos Actions do commit da entrega.

### Limitações e próxima tarefa

Essa cobertura não certifica todo MusicXML. Transposição, microtons, notação complexa, métricas compostas/aditivas, entradas inválidas, tie stop sem início e partes com compassos de durações divergentes ainda precisam de auditoria explícita. A fidelidade editorial e temporização Yamaha permanecem sem validação física. Próxima tarefa: ampliar fixtures de ligaduras incompletas e ordem musical, com expectativas independentes e sem modificar XML original.

## 0.9.18/code27 — ligação incompleta e contrato do Espera

Fixture incomplete-ties reproduziu dois finais de ligadura sem início correspondente: havia três eventos no modelo, mas apenas um ataque disponível no Espera. O modelo derivado agora mantém essas notas como ataques e emite um aviso de interpretação único ao abrir o arquivo. XML original preservado. Não altera a interpretação histórica de ligaduras completas (inclusive a fixture existente que sustenta a nota entre compassos); não cria exigência de duração/pedal no Espera.

O inventário das fixtures passa a verificar também Score.at em cada onset, avisos sem duplicação e igualdade byte/texto do XML sintético original. As seis fixtures com expectativas independentes passam localmente. Regressões JVM e comparação musical também passaram. CI Android/PR da própria entrega ainda precisam ser confirmados.

Próxima tarefa: fixtures de percurso com expectativas de Note On/Off e revisão das limitações de transposição/microtons/métricas compostas; não declarar toda a Etapa 2 concluída pela correção desses casos.

## 0.9.19/code28 — sound e offsets de andamento

Fixture tempo-sound-offset reproduziu somente uma mudança de tempo no Java, em vez das quatro esperadas: sound diretamente em measure era ignorado e o offset de sound não prevalecia sobre o da direction. Ambos os leitores agora interpretam sound direto e a precedência oficial do [offset de sound](https://www.w3.org/2021/06/musicxml40/musicxml-reference/elements/sound/). O JS aplica o limite inicial zero após converter o offset para beat global, permitindo offset negativo relativo ao compasso.

Expectativa independente: tempos 120/60/90/80 nos beats 0/1.5/1.75/3.5, quatro notas e fim 8. Sete fixtures passam localmente. Esta entrega trata de andamento; demais atributos sound (dinâmica, pedal, saltos diretos, instrumento) não passam a ser suportados por esta correção. Próxima tarefa musical: auditar parâmetros de expressão explícitos e limitações antes de fechar o escopo técnico da Etapa 2.

## 0.9.20/code29 — dinâmica numérica explícita

Score e renderer passam a conservar dinâmica numérica de sound e do atributo dynamics da nota. A [especificação de sound](https://www.w3.org/2021/06/musicxml40/musicxml-reference/elements/sound/) define percentual de forte MIDI 90: por exemplo, 50 vira velocity 45, 125 vira 113 e valores altos são limitados a 127. A nota explícita prevalece sobre a direção; escopos de parte, pauta e voz são respeitados em ordem musical, inclusive com backup. Sem indicação numérica conserva-se velocity 80 histórica; não se inventa conversão universal de p/mf/f.

Velocity zero significa silêncio na demonstração, portanto não gera Note On zero (que MIDI interpreta como Note Off), mas a nota escrita continua no Espera e no teclado visual. Ligadura conserva o ataque inicial. Não adiciona sintetizador, pedal automático nem avaliação de intensidade.

Fixtures explicit-dynamics e two-part-dynamics verificam escopos, override por nota, zero, limite 127 e independência entre partes. O teste novo falhou contra a base (velocity fixa 80) e passou após a correção. MusicXmlDynamicsTest verifica Note On/Off, silêncio e manutenção das notas no Espera/guia. Nove fixtures compartilham expectativas independentes Java/JS.

Próxima etapa técnica: aproveitar esses metadados para tornar o guia de duas mãos mais informativo (Etapa 3), sem transformar intensidade/duração em bloqueio. Pedal automatizado, crescendos sem valores explícitos e humanização continuam fora desta entrega.

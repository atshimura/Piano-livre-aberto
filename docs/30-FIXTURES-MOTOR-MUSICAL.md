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

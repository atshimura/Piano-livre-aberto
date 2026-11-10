# ZAI-006 — Fidelidade das notas e sobreposições: S1, sem ligadura de expressão automática

**Data:** 10/11/2026. **Autorização:** mantenedor aprovou regra conservadora de não gerar ligaduras de expressão automaticamente; exige planejamento, implementação, regressões e APK principal assinado, publicado por GitHub raw para evitar “Library file not found”. **Linhas:** somente `zai/*` e PR para `zai/lab`; a `main` GitHub permanece intocada. Nenhuma publicação em Play Store ou Release.

## 1. Evidência real

`S1.mid` foi fornecido para diagnóstico; **não incorporar o arquivo original** ao repo. MIDI SMF de 1 pista, PPQN 960, exportação técnica com 500000 µs/semínima. Extrair apenas números MIDI e instantes do conjunto de teste, sem metadados pessoais. O arquivo contém **22 Note On** e respectivos **22 Note Off**:

- Primeiro trecho: Dó4, Ré4, Mi4, Fá4, Sol4, Lá4, Si4, Dó5 (60,62,64,65,67,69,71,72); sete notas curtas destacadas ~0,36–0,40 s, separadas por ~0,60–0,66 s; Dó5 final ~1,35 s.
- Segundo trecho: mesmas oito alturas, ataques a cada ~0,59–0,64 s, liberações próximas ou depois dos ataques seguintes (sobreposições reais). Fá4 começa em tick 21735, solta em 23306, depois do ataque Sol4 em 22959: ~181 ms de sobreposição; Dó5 final ~1,50 s.
- Primeiro acorde: C4/E4/G4, ataques entre ticks 34117 e 34146, liberação ~0,92–0,95 s depois; segundo acorde: C4/E♭4/G4, ataques ticks 37656–37678 e liberação ~0,97–1,04 s. Há silêncio verdadeiro entre acordes, não sustentação até o próximo ataque.
- As imagens mostram curva longa de ligadura de expressão sobre a segunda escala e figuras ligadas excessivamente em ambos os acordes. A interpretação rítmica estima ~99 BPM, plausível para este trecho, mas a notação de durações diverge.

## 2. Diagnóstico de implementação

Arquivos analisados: `ScoreMidiPairer.java`, `ScoreTempoEstimator.java`, `ScoreRhythmQuantizer.java`, `ScoreXmlComposer.java`, `Score.java`.

- A captura e o pareamento representam **22 ataques originais**. A regra `<tie>` (prolongamento) do MusicXML **não cria novos ataques**: um único Note On pode exigir várias cabeças ligadas quando cruza compassos ou unidades rítmicas; o parser `Score.read` deve fundi-las em um evento musical.
- `ScoreRhythmQuantizer.quantize` usa por padrão o intervalo entre ataques como duração notada (`length = gap`). Em intervalos longos entre frases, isso prolonga artificialmente acordes e notas, gerando fragmentos/ties/pautas extras e omitindo pausas reais.
- A regra de fim de frase compara `prevGap` com a duração física e pode truncar uma nota longa ou usar uma duração que não corresponde ao Note Off real.
- `connected[]` transforma um Note Off próximo de outro Note On em `slurStart`/`slurStop` **sem conhecer a intenção do pianista**. A grande curva apresentada em S1 é uma inferência inadequada.
- Sobreposição de nota longa precisa preservar pelo menos a duração quantizada aproximada; o toque da próxima tecla não é `Note Off` da anterior.

## 3. Regras musicais aprovadas

1. **Invariante primário:** um Note On válido corresponde a **exatamente um ataque musical**, independentemente de a tecla ficar pressionada 100 ms ou 5 minutos. Fragmentos ligados de prolongamento pertencem **à mesma origem** e são unificados pelo parser do modo Estudar.
2. **Ligaduras de prolongamento (`tie`):** gerar quando **uma mesma nota** precisa ser dividida em figuras, inclusive cruzando a barra de compasso; `tie start/stop` corretamente pareadas. Não permitir fragmentação sem tie.
3. **Ligaduras de expressão (`slur`):** **não gerar automaticamente** a partir de sobreposição, proximidade de Note On/Off ou suposta frase musical. Preservar a capacidade de importar MusicXML que já contenha slurs, sem removê-las de partituras externas.
4. **Ataque, duração real e duração notada são distintos.** Usar intervalos regulares de ataques para deduzir valores escritos em staccato; distinguir pausas reais de notas curtas tocadas dentro de uma pulsação. Nunca estender por padrão um acorde até o próximo acorde quando o Note Off ocorreu muito antes.
5. **Sobreposição:** preservar os `endNs` do `ScoreMidiPairer`. Quando uma nota ultrapassar o início da próxima por duração representável na grade, mantê-la sustentada e usar outra voz se necessário. Sobreposição subgrade é limitação de notação, não autorização para inventar slurs.
6. **Última nota de frase:** deduzir a duração pela liberação real, sem herdá-la mecanicamente do intervalo anterior. Dó5 de ~1,35/1,50 s não pode ser reduzido a um tempo de ~0,6 s porque outra nota começa depois.
7. **Acordes coerentes:** cada grupo de notas de ataque simultâneo e liberações quase iguais ocupa uma unidade vertical, com duração musical correspondente à execução, e pausas de verdade entre grupos. Mantêm-se a correção C/Cm e a grafia E♭ (ZAI-004).
8. **Preservação:** manter duas pautas G/F, divisor C4 MIDI60, 4/4, XML válido, `.plc` e projetos originais, biblioteca, backups e modo Estudar. Sem telemetria, servidor ou recursos pagos.
9. **Incerteza:** quando o ritmo/pulso não puder ser inferido, explicitar estimativa provisória, sem fabricar intenção de legato.

## 4. Implementação ordenada

**Z6-P0 — Teste fiel:** codificar fixture somente com os 22 pares (pitch, Note On/Off em ticks) de S1, gerar a partitura real pela mesma pipeline, checar via `Score.read` os 22 ataques e métricas de durações. Nunca fazer o teste passar reduzindo o número de notas originais.

**Z6-P1 — Ritmo/pausas:** substituir `length = gap` indiscriminado por seleção contextual: próximo ataque regular + soltura destacada → valor rítmico do intervalo e staccato; próxima nota distante e soltura anterior → usar duração da tecla e pausa verdadeira; quando Note Off excede o próximo ataque de maneira notável → quantizar e preservar sustentação em outra voz. Para a última nota de frase, usar a liberação e não reduzir indevidamente. Aplicar na notação escala longa, acordes e última nota.

**Z6-P2 — Expressão:** desativar a geração de slur automática no compositor pela origem de Criação. Testes exigem zero `<slur>` nas partituras geradas, mesmo com teclas sobrepostas. Manter `tie` quando musicalmente necessária.

**Z6-P3 — Verificação:** regressões de ZAI-001/002/004 + S1 22 ataques + staccato + duas escalas + pausa entre acordes + cinco minutos de uma tecla (uma nota mesmo com múltiplas ligaduras de prolongamento); MusicXML parse/Estudar; contratos; Gradle Android. **Não declarar testes físicos executados.**

**Z6-P4 — APK principal:** versão `0.9.67-open-beta`, código **76**, pacote principal `br.org.pianolivre.opensource`, mesma assinatura permanente via GitHub Actions Secrets, build release não depurável sem INTERNET. `main` preservada. **Distribuir pelos mesmos bytes em `zai/downloads`** com nome versionado, SHA-256, certificado, publicação GitHub raw fixada ao commit — link Library/sandbox jamais como canal primário (ZAI-005).

## 5. Critérios de aceite

- **S1-22:** 22 Note On em `S1.mid`, 22 eventos musicais pós-unificação de ties em `Score.read`; altura e ordem corretas (duas oitavas C4→C5, dois acordes C/Cm), nunca duplicar ou eliminar ataques.
- **S1-DURATION:** Dó5 longo nas duas escalas preservado como duração notada próxima de ~1,35 e ~1,50 s; Fá4 na segunda escala não se encerra artificialmente no início de Sol4; acordes não são alongados até o próximo.
- **S1-REST:** pausa verdadeira entre os dois acordes aparece; notação de staccato da primeira escala não ganha uma pausa em toda nota.
- **S1-NOSLUR:** nenhum slur criado automaticamente; `tie` é permitida e usada apenas para a mesma nota.
- **HOLD-5M:** apenas uma nota MIDI/um evento ao estudar após cinco minutos pressionados; a duração pode atravessar vários compassos com ties, sem reataques artificiais.
- **REGRESSION:** ZAI-001/002/004 e os contratos de projetos/backup continuam aprovados.
- **APK:** versão 76 superior a 75, package principal e certificado permanente verificados antes da publicação em GitHub raw; sem publicar Release nem alterar `main`.

**Limites:** discretização rítmica em semicolcheias impossibilita preservar matematicamente cada milissegundo de sobreposição curta; não confundir fidelidade de eventos MIDI com equivalência exata de reprodução acústica (pedal não implementado na duração notada). Testes reais com P-45, Realme 7 5G e Tab M9 permanecem pendentes.

# ZAI-007 — Partitura simplificada, sem ligaduras automáticas

**Data:** 10/11/2026. **Autorização do mantenedor:** eliminar todas as ligaduras automáticas da partitura gerada em Criação, preservar os dados da captura MIDI e preparar edição manual futura. Implementar, testar e gerar **APK de atualização principal** com assinatura permanente e download direto pelo GitHub, sem usar a biblioteca do ChatGPT. Desenvolvimento em `zai/*` com PR para `zai/lab`, **`main` intocada**, sem Release ou Play Store.

## 1. Contexto: decisão e causa

Testes físicos na versão 0.9.67 ainda mostram pequenas curvas unindo notas consecutivas na segunda escala e acordes. A grande ligadura de expressão foi removida em ZAI-006; o defeito visual remanescente é a **ligadura de prolongamento** criada por `ScoreRhythmQuantizer.decompose()` sempre que a duração quantizada não cabe em uma única figura ou atravessa barra de compasso. No mesmo compasso, p.ex. duração de 5/16 gera 4/16+1/16 ligados. Para durações muito longas, a notação cria dezenas de figuras ligadas para uma única tecla. A rotina de prévia também reutiliza o PLS2 salvo anterior quando a gravação `.plc` é igual, mesmo após trocar o algoritmo.

## 2. Decisão funcional do produto

1. **Não gerar ligaduras automaticamente** em Criação: nem de expressão (`<slur>`) nem de prolongamento (`<tie>` ou `<tied>`). Regras de leitura de MusicXML importado ou de outros modos não devem ser removidas.
2. **Um único ataque MIDI → uma única cabeça notada por altura**, sem dividir a nota em figuras sucessivas. Ataques repetidos legítimos continuam repetidos. Acordes continuam três cabeças alinhadas por ataque.
3. **Uma única figura convencional por ataque**, escolhida como a mais próxima possível da duração musical inferida, **limitada ao espaço disponível até a barra de compasso**; quando necessário, aproximar ou encurtar a duração visual, em vez de criar ligaduras. Apresentar informação honesta de que é partitura simplificada, não transcrição completa das sustentações.
4. **O MIDI original permanece fonte de verdade**: manter `Note On`, `Note Off`, duração da tecla, sobreposições, sessões e arquivos `.plc`. Não modificar os eventos MIDI de origem, não introduzir novos ataques e não apagar dados ou derivados.
5. **Representação visual limitada:** uma nota de cinco minutos pode aparecer como uma semibreve curta em um compasso (com um único ataque). Não afirmar que a reprodução da partitura simplificada no Estudar mantém cinco minutos de duração; esses cinco minutos continuam disponíveis apenas na captura original MIDI. Quando a nota cruza o compasso, a parte excedente não é mostrada. Isso é uma **perda intencional de fidelidade da notação**, aprovada para esta fase, em favor de legibilidade.
6. **Partitura antiga detectada:** se a derivação PLS2 corresponde à gravação mas não contém o marcador de revisão do novo motor, a tela deve mostrar um aviso com opção explícita **“Gerar prévia sem ligaduras”**, sem reutilização silenciosa e sem apagar `.pls`/ `.plc` nesse momento. Só substituir `.pls` após ação explícita de **Salvar partitura**. Preservar leitura de derivados anteriores para biblioteca, backups e compatibilidade; não mudar magic/format/fingerprint do armazenamento.
7. **Pauta dupla G/F** e corte MIDI60, MusicXML/OSMD, 4/4, pulso automático, staccato e convenção de Mi♭ do Descobrir mantidos. Sem servidores, telemetria, serviços pagos ou `INTERNET`.
8. **Futura edição manual** de ligaduras de expressão/prolongamento será um marco separado, não implementado nesta entrega. Não oferecer botão que não funcione.

## 3. Plano executável

- **Z7-P0 — Registros:** documentar ZAI-007 e preservar decisões precedentes como histórico. Verificar `AGENTS.md`, `ZAI-CONTINUIDADE.md`, ZAI-005 e o pipeline de testes.
- **Z7-P1 — Escrita sem ligaduras:** modificar a construção dos tokens da Criação para não fragmentar nenhum Note On. Cada figura deve caber integralmente em um único compasso e todos os blocos de vozes/restos devem somar 4/4 sem `<tie>`, `<tied>` nem `<slur>`. Recalcular comprimento visual dos compassos após a simplificação; uma nota de cinco minutos não produz dezenas de compassos vazios.
- **Z7-P2 — Regeração segura:** incluir marca estável de motor `ZAI-007-tie-free` no MusicXML gerado; detectar derivados antigos na prévia e oferecer regeneração explicitamente. Não destruir gravação nem derivados anteriores sem confirmação de salvar.
- **Z7-P3 — Contratos:** atualizar testes históricos que exigiam prolongamento visual (agora desautorizado), sem reduzir o controle dos 22 ataques `S1.mid`, dos 14 ataques da escala, acordes C/Cm, 5min de nota única, sucessivos Note Ons e deduplicação. Criar teste isolado para figuras de 5/16, 6 tempos cruzando barra, acorde, sobreposição e a marca da nova revisão. Validar `Score.read`, 4/4 e ausência total de `<tie>`, `<tied>`, `<slur>`. Os testes de preservação de MIDI devem comparar durações originais; os testes da partitura devem aceitar duração visual truncada quando atravessa barra.
- **Z7-P4 — APK:** depois de PR e CI aprovados, gerar APK principal `0.9.68-open-beta`, `versionCode 77`, `br.org.pianolivre.opensource`, assinatura permanente via GitHub Actions Secrets, não depurável/sem internet. Reutilizar a infraestrutura versionada de CI da ZAi sem mexer na `main`.
- **Z7-P5 — Download sem Library:** usar workflow de publicação na branch `zai/downloads` que transfere exatamente os bytes do artefato assinado, verifica pacote/versão/certificado e SHA-256 antes e após publicar; disponibilizar URL raw fixada ao **commit de publicação**, com página GitHub alternativa e registro da entrega em `ZAI-CONTINUIDADE.md`. Sem sandbox como link principal.

## 4. Critérios de aceite

- **VIS-0-TIES:** qualquer XML novo da Criação contém **zero** `<tie>`, `<tied>` e `<slur>`, inclusive para uma única nota de 5 minutos.
- **ONE-ON-ONE-HEAD:** um Note On válido (uma altura) gera exatamente uma cabeça de nota, mesmo quando cruzaria barra. Nenhuma nota extra; `Score.read.events.size()` igual ao total de Note Ons.
- **S1:** 22 notas originais, escalas C4–C5, acordes C/Cm, nenhuma ligadura; pausas musicais, andamento aproximado, staccato e alturas preservados. A duração visual pode diferir da soltura e deverá ser explicitamente indicada como simplificação.
- **CHORDS:** C/E/G e C/E♭/G permanecem acordes (com membros `<chord/>`), sem vozes espúrias causadas por poucos milissegundos.
- **SCORE-REUSE:** XML antigo (sem marcador) não é apresentado silenciosamente como prévia nova; mensagem oferece regeneração manual. O arquivo antigo permanece inalterado até salvar a nova derivação.
- **REGRESSÃO:** contratos JVM, projetos, backups, MusicXML, criação/biblioteca/estudo, Android build passam. Testes do emulador e do Yamaha P-45/Realme 7 5G/Lenovo Tab M9 continuam pendentes.
- **APK-77:** pacote principal, versão 77, assinatura permanente, sem `INTERNET`, SHA-256 publicado e link GitHub raw comprovado; `main` e repo privado histórico intocados.

**Importante:** este é um modo de notação deliberadamente **simplificado**. Se uma nota cruzar barra ou permanecer 5 minutos, o MIDI original preserva a duração, mas a notação sem ligadura não pode representá-la integralmente em 4/4. A edição manual futura poderá permitir notação completa.

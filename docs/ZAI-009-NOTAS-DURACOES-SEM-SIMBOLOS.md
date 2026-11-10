# ZAI-009 — Escrita inicial básica: notas, durações e acordes sem símbolos acessórios

**Data:** 10/11/2026. **Decisão do mantenedor:** o gerador da Criação deverá se concentrar exclusivamente em reconhecer alturas, ataques, durações, acordes, ritmo e andamento. Na partitura inicial **não mostrar pausas, pontos de staccato, ligaduras ou outras indicações expressivas**. Preservar as duas pautas, claves Sol/Fá, compasso, acidentes de altura e figuras rítmicas necessárias. Permitir a futura notação musical avançada em outra etapa.

**Escopo:** somente branches `zai/...`, PR para `zai/lab`, **`main` do GitHub intocada**; sem modificar o repositório histórico privado, sem Release/tag/Play Store, telemetria, internet ou serviços novos. Preservar GPL-3.0, `.plc`, `.pls`, biblioteca e backups. Ler `AGENTS.md` e `ZAI-CONTINUIDADE.md` antes da implementação.

## Problema mostrado no S1

A versão 0.9.69 passou a mostrar a partitura em sistemas responsivos e sem ligaduras, mas ainda exibiu um excesso de símbolos de pausa, pontos de staccato e pausas de compasso inteiro na pauta inferior, atrapalhando a identificação das notas. A captura MIDI tem **22 ataques reais**: duas escalas C4–C5, tríade C maior e tríade C menor (E♭). A partitura deve permitir ver cada ataque com altura e figura sem símbolos acessórios.

## Regra principal: visualização mínima, tempo preservado

1. **A altura é determinada exclusivamente pelo número MIDI**; nenhuma pausa ou simplificação pode mudar pitch ou ordem de ataques. Preservar simultaneidade real de acordes, divisão permanente MIDI 60+ na pauta de Sol e abaixo de 60 na de Fá e 4/4.
2. **O início e a duração física são preservados na gravação original `.plc`** (Note On/Note Off, canais, ordem, segmentos, sobreposição). Uma tecla segurada por cinco minutos é um único ataque.
3. **O motor de ritmo ainda escolhe figuras convencionais**, estima BPM e mantém a cronologia dos ataques, sem criar novos Note Ons. Figuras pontuadas quando necessárias permanecem, pois **o ponto de aumento faz parte da duração**, diferente do ponto de staccato (articulação). A simplificação conservadora de uma figura por ataque ZAI-007 continua: sustain que cruza compasso não cabe integralmente na partitura sem ligadura; não ocultar esta limitação nem afirmar que o XML preserva cinco minutos. O MIDI original permanece intacto.
4. **Não desenhar qualquer pausa na partitura inicial**, inclusive as pausas da pauta inferior vazia. Porém, manter os **intervalos de silêncio no modelo temporal MusicXML como pausas não imprimíveis** (e não apagá-los do XML), para preservar posição dos ataques, alinhamento entre vozes, soma exata do compasso e compatibilidade Estudar. Verificar em navegador **OSMD offline real** que `print-object="no"` oculta os símbolos; se a versão local do OSMD não respeitar, empregar técnica alternativa no renderer e testar o DOM.
5. **Não gerar staccato** (`<articulations><staccato/>`), slurs, ties nem outros símbolos expressivos. A articulação física ainda é reconhecida no par MIDI para inferência de figura/duração, mas não vira indicação visual. Nunca remover essas indicações de MusicXML externo importado ou de partituras de outras funções.
6. **Não esconder acidentes de altura, claves, figuras de nota, barras, andamento nem assinatura de compasso:** são indispensáveis à identificação rítmica e de pitch. A pauta inferior continua visível, mas vazia sem símbolo de pausa.
7. **Persistência e derivados:** elevar o identificador de geração no MusicXML para `ZAI-009-basic-notes-duration`; ao abrir `.pls` anterior, o modo Criação oferece **Gerar prévia básica** explicitamente, sem reutilizar XML antigo silenciosamente. Somente `Salvar partitura` substitui o derivado; `.plc` permanece sempre intacto. Formato PLS2, fingerprints, integridade e backups não mudam.
8. **Layout:** manter layout responsivo `creation-page` de ZAI-008, ocupando a largura real, sem rolagem horizontal, com sistemas quebrados para caber na tela. Isolar de modos Estudar e partituras importadas.

## Plano de implementação

**P9-0 — Documento e branch:** registrar decisões; não mudar versão ou workflow global, seguir PR exclusivo.

**P9-1 — XML:** alterar `ScoreXmlComposer` para escrever repousos como elementos MusicXML não imprimíveis (incluindo bass vazio) e remover somente articulação staccato gerada. **Não retirar tokens de pausa do plano**: sua duração continua imprescindível para a linha do tempo. Manter figuras e pitch com acidentes.

**P9-2 — Prévia:** atualizar aviso e marcador de revisão; informar que a imagem é básica e as durações físicas detalhadas permanecem no MIDI original.

**P9-3 — Regressões:** garantir 22 Note Ons de S1 e apenas 22 cabeças de nota musical, C/Cm íntegros, andamento, duração textual/figuras coerente, staccato e ligaduras ausentes, todas as pausas invisíveis e conservação de tempo em XML/Estudar. Verificar que importações externas não perdem suas pausas. Validar uma nota sustentada por cinco minutos (1 ataque).
- Testar via JVM/DOM e browser Chromium real com OSMD embutido, no modo `creation-page`, em 360 e 800 px, com contagem de glifos e eventuais símbolos de pausa.
- Testar o fluxo de revisão PLS2 antiga de maneira não destrutiva, com contratos Python.
- Compilar Android debug isolado. Testes físicos **não equivalem a testes em navegador**.

**P9-4 — CI/PR:** revisão, correções, PR para `zai/lab`, aprovação dos checks, documentação final.

**P9-5 — APK principal e download:** próxima versão **0.9.70-open-beta, code79**, package `br.org.pianolivre.opensource`, certificado permanente dos GitHub Actions Secrets; publicar **exatamente o APK testado** na branch `zai/downloads` por workflow, com SHA-256 conferido depois do push. **Entregar link GitHub raw por commit, não Library**. Sem tag/Release e `main` intocada.

## Critérios de aceite

- **S1-NOTES:** 22 fontes MIDI → exatamente 22 notas musicais, ordem/pitch preservados, duas escalas, C maior e C menor com Mi♭; teclas longas não geram reataques.
- **VISUAL-NO-REST:** nenhuma pausa desenhada no OSMD `creation-page`, nem na pauta inferior vazia. O MusicXML contém pausas com `print-object="no"` e durações íntegras para manter a cronologia; na reprodução de Estudar não criar eventos de pausa como nota.
- **NO-ARTICULATION:** zero `<staccato>`, `<slur>`, `<tie>`, `<tied>` ou ornamentação expressiva gerada. Pontos **de aumento** de duração continuam lícitos.
- **MUSIC-TIME:** inicio relativo dos ataques, ritmo, andamento e acordes inalterados em relação à ZAI-008; MIDI original não sofre mutação.
- **REGEN:** XML derivado anterior não é apresentado como atualizado; a regeneração é oferecida e `.pls` só é substituído ao salvar.
- **WRAP:** sem rolagem horizontal e com quebra de sistemas em telas estreitas; arquivos externos importados e modo Estudar continuam com suas regras existentes.
- **APK:** assinatura permanente, pacote principal, code79, ausência INTERNET, não depurável, SHA remoto comprovado; limites e testes físicos pendentes registrados.

**Limitação incontornável da decisão visual:** em notação de compasso regular, remover a figura visível de pausa e suprimir ligaduras impede representar integralmente todas as sustentações e silêncios na página. Os dados fonte MIDI preservam a informação exata; a partitura é **uma transcrição rítmica inicial simplificada**, não uma edição completa.

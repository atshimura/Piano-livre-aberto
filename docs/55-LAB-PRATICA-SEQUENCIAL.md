# 55 — Prática guiada sequencial de escalas e arpejos (0.9.46 / code55)

## Escopo implementado

Em **Aprender/Praticar**, escalas e arpejos do catálogo entram em prática sequencial quando o usuário aciona **Praticar** no modo **Tocar**. Cada ataque novo da altura MIDI exata prevista avança imediatamente à próxima altura; ataques extras isolados são contabilizados e sinalizados, sem impedir a continuação. A progressão informa quantidade correta, total, próxima nota e conclusão. Após concluir, **Reiniciar prática** permite repetir o mesmo exercício; alternar exercício, filtro, Descobrir/Aprender, Tocar/Montar, sair do Laboratório, colocar o app em segundo plano ou desconectar MIDI cancela a sessão. Não existe timeout, penalidade por demora ou avaliação de duração/pedal.

O motor `MusicLabPracticeSequence` guarda a **ordem e o registro MIDI** (incluindo eventuais alturas iguais consecutivas); notas repetidas exigem novo ataque após soltar. Um evento virtual Tocar só representa ataque na transição de tecla não pressionada para pressionada; o encaminhamento MIDI já filtra Note Ons redundantes. `observe` rejeita ataques simultâneos ou sobrepostos, em vez de inventar uma ordem; considera conjuntamente os estados USB-MIDI e virtual, para não contabilizar uma mesma altura de duas fontes como dois ataques. Esses eventos indeterminados são orientados na interface, não classificados como erro. Notas extras **isoladas** são somadas. O motor rejeita ataques durante a demonstração USB-MIDI, incluindo o silêncio entre notas/voltas, para que a saída MIDI ou o retorno local do instrumento não avance a prática. A demonstração permanece independente, com a sessão e o cancelamento seguro implementados no PR #44.

**Acordes e intervalos** continuam com a prática preexistente por conjuntos de classes de altura, sem verificação de ritmo, ordenação, registro nem ataques simultâneos temporizados. O seletor **Ambas/Direita/Esquerda (referência)** exprime somente a intenção do estudante: não detecta a mão física ou o dedilhado por altura.

## Compatibilidade

A entrega mantém reconhecimento espontâneo e manual do modo Descobrir, catálogo pesquisável com cinco exercícios legados, pauta esquemática e teclado juntos, demonstração com velocidade/repetição e liberação de notas, funcionamento offline, perfis, histórico geral e backup sem migração de esquema. A prática sequencial **não** cria histórico persistente e **não** reproduz/avalia durações editoriais MusicXML.

## Validação exigida e limitações

`MusicLabPracticeSequenceTest` cobre ataques corretos/extras, ordem, registro, alturas repetidas com novo ataque, teclas mantidas, fonte MIDI/virtual, sobreposição, chord simultâneo, interrupção, reinício, conclusão e supressão durante demonstração. O smoke Android verifica seleção, primeira nota, filtro e encerramento no contexto do Laboratório. Exigir auditoria pública, testes JVM, JS, MusicXML/MIDI, build debug/release, emulador e assinatura/publicação em **SHA exato** do PR e da `main`; resultados históricos não substituem isso.

Não implementados: dedilhado sugerido ou obrigatório, identificação de mão, metrônomo e avaliação rítmica opcionais, calibração de tolerâncias, histórico do Laboratório por perfil, persistência da sequência. Os testes físicos com Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 ainda dependem da issue #11, inclusive rearticulação/latência real de notas USB-MIDI. Não declarar versão 1.0 concluída.

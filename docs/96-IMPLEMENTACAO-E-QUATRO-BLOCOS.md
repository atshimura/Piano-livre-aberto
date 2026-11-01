# Implementação E — plano de execução acelerado em quatro blocos

**Estado:** E-A (E0+E1) executado/documentado; **E-B (E2+E3) implementado e testado na branch funcional do PR #91**; **E-C (E4) implementado e validado em JVM/Chromium/UI Android**, mas ainda sem merge; **E-D em execução, aguardando os gates do SHA final e integração**. [Registro E-D](100-IMPLEMENTACAO-E-E-D-VALIDACAO-ENTREGA.md). [Relatório E-C](99-IMPLEMENTACAO-E-E-C-ENQUADRAMENTO-REVERSIVEL.md). [Registro E-B](98-IMPLEMENTACAO-E-E-B-PAUTA-ALINHADA.md) · [E-A](97-IMPLEMENTACAO-E-E-A-BASELINE.md). [Registro E-A e capturas](97-IMPLEMENTACAO-E-E-A-BASELINE.md) · [PR funcional #91](https://github.com/atshimura/Piano-livre-aberto/pull/91).  
**Registro:** 01/11/2026.  
**Especificação integral:** [95 — Implementação E](95-IMPLEMENTACAO-E-DESCOBRIR-PAUTA.md).  
**Acompanhamento:** [issue #89](https://github.com/atshimura/Piano-livre-aberto/issues/89).  
**Planejamento documental:** [PR #90](https://github.com/atshimura/Piano-livre-aberto/pull/90).  

Este documento organiza as sete etapas técnicas **E0–E6** da especificação 95 em **quatro blocos solicitáveis**, sem alterar as três decisões aprovadas nem reduzir os critérios de aceite. Os nomes abaixo são comandos inequívocos de retomada para outras conversas ou agentes. Quando o usuário solicitar um bloco, **executar o bloco** (não responder apenas com um novo planejamento), com autonomia técnica, checks e registro do que foi feito; respeitar os impedimentos reais e não afirmar resultados não executados.

## Nomes oficiais e comandos exatos de retomada

| Bloco | Nome oficial | Correspondência | Meta de esforço técnico | Comando sugerido |
| --- | --- | --- | --- | --- |
| **E-A** | **Diagnóstico Direto** | E0 + E1 | **0,5–1 h** | **Implemente o Bloco E-A da Implementação E.** |
| **E-B** | **Pauta Grande e Alinhada** | E2 + E3 | **1,5–2,5 h** | **Implemente o Bloco E-B da Implementação E.** |
| **E-C** | **Escala Inteligente Reversível** | E4 | **2–3 h** | **Implemente o Bloco E-C da Implementação E.** |
| **E-D** | **Validação e Entrega** | E5 + E6 | **4–5,5 h** | **Implemente o Bloco E-D da Implementação E.** |

**Estimativa otimizada total:** aproximadamente **8–12 horas de esforço técnico ativo**. É **meta**, não garantia de duração nem promessa de execução assíncrona. Os testes de CI podem demorar sem exigir trabalho humano, e falhas reais podem estender o esforço. Não reduzir confiabilidade para forçar a estimativa.

## Política de execução para poupar tempo

1. **Uma branch funcional e um PR funcional**, atravessando os blocos E-A → E-D. Nomes propostos: branch `feat/implementacao-e-descobrir-pauta` e PR `Implementação E — pauta ampliada e enquadramento reversível`, inicialmente *draft* para acompanhamento. **Não confundir com o PR #90, exclusivamente documental**.
2. Ao iniciar qualquer bloco, conferir `main`, PR #90, issue #89, `AGENTS.md`, docs 66/75/93/95/96 e versão vigente. Não reutilizar SHA, estado de PR ou relatório de testes de outra execução como comprovação atual.
3. Se a branch funcional **já existir**, continuá-la a partir do último commit confirmado, após conferir árvore e checks; **não abrir branches ou PRs duplicados**. Se houver alterações concorrentes na `main`, conciliar com segurança, sem forçar merge.
4. **E-A** produz diagnóstico mínimo dirigido e referência comparável; **E-B** e **E-C** executam testes direcionados de renderização/código nos arquivos alterados. **Somente E-D** realiza a validação completa obrigatória de build, regressão e emulador antes de integrar. Em caso de falhas, corrigir e repetir os gates pertinentes; não desativar checks nem remover testes. Esta divisão não modifica a política de proteção da `main`.
5. Preservar o layout externo do Descobrir (~75%/25%, barra, painel de informações), o reconhecimento USB-MIDI, o último resultado, o OSMD offline e as demais telas. **Nenhum novo botão de zoom**. Não tocar no repositório histórico privado, em serviços externos ou segredos.
6. Ao final de **cada bloco**, registrar no PR funcional/issue #89: tarefas realizadas, commits, testes executados e seus resultados, limitações, arquivos de evidência e próximo bloco. Registrar explicitamente **não testado** quando couber; não marcar E como concluída antes da validação final.
7. O trabalho não precisa ficar aguardando testes físicos. O usuário poderá conferir em **Yamaha P-45**, **Realme 7 5G** e **Lenovo Tab M9** quando possível. Testes sintéticos e emulador **não equivalem** a aceite físico.
8. Preferir mudanças pequenas e rastreáveis. Se uma hipótese de simplificação falhar (por exemplo, faixas atuais ainda cortarem extremos), ampliar **apenas** a parte técnica indispensável, mantendo os requisitos, em vez de tentar uma reescrita global ou reduzir o escopo.

## Bloco E-A — Diagnóstico Direto (E0 + E1)

**Objetivo:** preparar uma única base reproduzível e eliminar investigação redundante.

**Entrada:** issue #89, docs 95/96 e `main` vigente; código atual de `DiscoveryStaffView.java`, `discovery-notation.js` e testes associados.

**Execução:**
- Ler documentação obrigatória e conferir a versão, proteção e status das branches/PRs. Abrir/continuar **a branch funcional única**; abrir PR funcional *draft* se já houver commit material.
- Confirmar no código, de forma objetiva, três mecanismos: câmera/pauta padrão, centralização da nota isolada (deslocamento ao centro) e progressão monotônica do nível de câmera.
- Reproduzir e registrar **um pequeno baseline** em telefone e tablet paisagem para: pauta vazia, Dó4, primeira nota de sequência e **extremos simultâneos** MIDI 21+108. Anotar medidas da área da pauta, tamanho gráfico, coluna inicial e eventuais cortes. Reaproveitar os scripts de captura disponíveis; **não executar a suíte inteira nesta fase**.
- Criar ou preparar as fixtures de extremos juntos **somente se necessárias** para a medição do baseline, evitando código duplicado.
- Registrar um diagnóstico conciso com os valores e arquivos a alterar, usando evidência real; não gastar tempo com novo desenho visual ou pesquisa ampla já feita no documento 95.

**Critério de saída:** baseline identificável por SHA e dispositivo, problemas confirmados ou refutados por evidência, branch funcional preparada, próximo bloco E-B pronto. Não declarar bug “reproduzido” se só houver leitura de código.

**Exclusões temporárias:** não corrigir ainda E2/E3/E4, não alterar versão, não gerar Release ou APK final.

**Comando para o usuário:** **“Implemente o Bloco E-A da Implementação E.”**

## Bloco E-B — Pauta Grande e Alinhada (E2 + E3)

**Objetivo:** entregar juntas as duas mudanças visuais de menor risco no mesmo renderizador.

**Entrada:** baseline e branch funcional de E-A.

**Execução:**
- Aumentar a escala gráfica **real** das duas pautas padrão, com glifos OSMD proporcionais e margens seguras; não confundir aumentar o espaço branco do contêiner com aumentar linhas/notas.
- Remover a centralização isolada e definir a mesma **coluna de início após as claves** para nota, acorde e primeiro evento de sequência; considerar largura real dos símbolos acidentais, linhas suplementares e rail fixo.
- Preservar claves de Sol/Fá, posições verticais, proporção esquerda/direita, painel textual, barra superior, eventos MIDI, último resultado e scroll horizontal.
- Validar com testes **direcionados** de navegador/renderização: vazio, nota isolada, acorde comum, primeira nota sequencial, longa sequência com scroll, redimensionamento e alinhamento de ledgers. Comparar medidas/capturas com E-A.
- Atualizar testes de dimensões antigas **por critérios de legibilidade e ausência de corte**, nunca enfraquecendo asserções.

**Critério de saída:** pauta normal demonstravelmente maior, início horizontal consistente, nenhum corte nos casos normais, commits e provas dos testes direcionados registrados. O ajuste reversível dos extremos continua **pendente para E-C**.

**Comando para o usuário:** **“Implemente o Bloco E-B da Implementação E.”**

## Bloco E-C — Escala Inteligente Reversível (E4)

**Objetivo:** tornar a adaptação aos extremos automática, temporária e visualmente estável.

**Entrada:** E-B em funcionamento na mesma branch funcional.

**Execução:**
- Substituir seleção acumulativa do tier (`tier=Math.max(...)`) por escolha baseada no **conjunto de notas atualmente apresentado**, usando inicialmente as faixas existentes (normal / ampliado / extremo). **Não desenvolver câmera dinâmica nova se as faixas funcionarem**.
- Caso as faixas atuais não comportem o envelope completo, ajustar apenas câmera/margens/cálculo uniforme indispensável; verificar bounds de todas as cabeças, hastes, acidentes e linhas suplementares para ambos os sistemas.
- Preservar o último resultado após liberar teclas. **Restaurar automaticamente** a pauta grande ao exibir nova nota/acorde/sequência que caiba na câmera normal; se notas extremas ainda estiverem ativas na mesma apresentação, mantê-las visíveis.
- Evitar oscilação entre eventos do mesmo acorde e descarte indevido de resultados; preservar geração/revisão e publicação atômica do SVG.
- Executar testes **direcionados** para ciclos normal→extremo→nota normal, extremo retido após Note Off, extremos **MIDI 21+108 simultâneos** e **MIDI 0+127** técnico, sequência que muda de registro, repetição de ciclos e alterações de viewport, em celular/tablet.

**Critério de saída:** ajuste excepcional sem cortes demonstrado, retorno automático comprovado, nota comum retoma tamanho de E-B, último resultado permanece legível e sem oscilação perceptível nas fixtures. Registrar limitações se a legibilidade de MIDI 0+127 for intrinsecamente baixa; **não omitir notas**.

**Comando para o usuário:** **“Implemente o Bloco E-C da Implementação E.”**

## Bloco E-D — Validação e Entrega (E5 + E6)

**Objetivo:** fechar uma única entrega completa, sem testes redundantes durante as etapas anteriores.

**Entrada:** três mudanças implementadas na mesma branch funcional, testes direcionados de E-B/E-C registrados.

**Execução:**
- Revisar diff apenas dos arquivos necessários, corrigir regressões e otimizar re-render/oscilações se houver evidência de problema.
- Executar **testes completos obrigatórios** pela CI atual de `build` e `emulator`, além dos cenários específicos de Descobrir; conferir resultados do **SHA final** e correções subsequentes. Executar novos gates se o código mudar após uma execução.
- Produzir/inspecionar capturas do celular e tablet: vazio, Dó4, acorde, sequência curta e longa, extremos 21+108 juntos, último resultado após soltar e retorno à pauta normal; confirmar a barra/painel direito intactos. Comparar diretamente baseline E-A x resultado final.
- Atualizar docs 95/96, ADR/changelog, testes e versão **somente se houver mudança de APK**; registrar resultados exatos, limitações e pendências de hardware.
- Mudar PR funcional de *draft* para pronto **após** gates; integrar na `main` **somente** com checks exigidos e proteção preservada, sem force merge. Validar CI da `main` antes de classificar entrega como integrada.
- Gerar APK por workflow existente com a assinatura permanente configurada em GitHub Actions Secrets, **sem revelar credenciais** e sem aprovar automaticamente uma nova Release se a política exigir autorização específica. Quando a publicação não for autorizada/possível, deixar o APK/artefato de build e os próximos passos documentados sem alegar distribuição.
- Atualizar issue #89 com resultado e evidências; fechar **somente quando** os critérios funcionais e de integração aplicáveis estiverem comprovados. Testes físicos permanecem expressamente pendentes até o usuário executá-los.

**Critério de saída:** três mudanças juntas, regressões completas aprovadas e registradas, PR integrado se permitido, APK verificável se produzido, documentação final e limitações/hardware pendentes. Se gate falhar, **não** declarar bloco concluído; manter estado retomável com a falha precisa.

**Comando para o usuário:** **“Implemente o Bloco E-D da Implementação E.”**

## Dependências, retomada e interpretação de solicitações

- **Ordem recomendada:** **E-A → E-B → E-C → E-D**. Ao pedir um bloco posterior sem registro comprovado dos anteriores, **verificar o GitHub primeiro**. Recuperar evidências existentes ou executar os pré-requisitos técnicos estritamente necessários, sem solicitar que o usuário repita decisões. Se a etapa anterior ficou parcial, concluir sua pendência antes da seguinte; registrar o desvio.
- **Cada pedido de bloco significa executar apenas aquele bloco e seus pré-requisitos inseparáveis**, registrando o resultado. Não publicar nem integrar antes de E-D. Se o usuário pedir explicitamente “implemente toda a E”, executar os quatro em sequência, respeitando gates e limitações do ambiente.
- **Não abrir um PR por bloco:** isso multiplicaria a CI e o trabalho de integração. Um único PR funcional em evolução oferece rastreabilidade pelos commits/marcadores `E-A`, `E-B`, `E-C` e `E-D`, além do registro de conclusão em cada passagem.
- **Se o PR documental #90 ainda não estiver integrado**, a especificação continua disponível naquela branch; não confundir sua aprovação/CI com evidência da implementação funcional. Ao retomar E-A, preferir iniciar após o merge seguro da documentação, se ele ainda estiver pendente.
- **Situação atual (01/11/2026):** A = **concluído como diagnóstico/baseline CI**; B = **código implementado, testes Chromium e capturas concluídos na branch #91**; C = **implementado, testes JVM/Chromium e UI-focada Android aprovados na branch #91**; D = **em validação final para a candidata 0.9.59/code68, sem merge nem Release até os gates**. [Evidência E-A](97-IMPLEMENTACAO-E-E-A-BASELINE.md). A criação original deste plano não havia executado os blocos.

## Checklist final resumido

A Implementação E só pode ser declarada concluída se:
- [ ] A pauta normal está realmente maior, em ambos os perfis, sem alterar a tela ao redor.
- [ ] Nota isolada, acorde e sequência iniciam na mesma coluna depois das claves.
- [ ] Lá0 + Dó8 simultâneos cabem integralmente; extremos técnicos também são testados.
- [ ] Após um extremo, nova entrada comum restaura automaticamente a pauta maior; Note Off não apaga o resultado.
- [ ] Não houve regressão de posições musicais, MIDI, análise, scroll ou interface.
- [ ] Testes completos do SHA final, capturas e APK/estado de publicação estão registrados.
- [ ] A issue/PR/versão/documentação indicam precisamente **concluído, parcial, pendente e testes físicos não realizados**.

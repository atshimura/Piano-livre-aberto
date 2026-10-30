# 07 — Estado atual e prioridades da linha aberta

## Prompt 4 — consolidação 0.9.52-open-beta (61)

Integração sequencial de #71/#72, seguida de PR consolidado contendo integralmente #73/#74 por ancestralidade, sem publicar 0.9.51. [Registro vigente e protocolo de fechamento](73-CONSOLIDACAO-PUBLICACAO-PROMPT-04.md), com links para o registro efetivo do SHA, checks, assinatura e APK oficial. A publicação exige build/emulador do merge final, certificado permanente e upgrade público. Não basta CI de PR. Os estados “sem merge/Release” abaixo pertencem ao histórico dos Prompts 1–3. Próxima etapa após beta: aceite físico Realme/Lenovo/Yamaha, correções em PR e critérios restantes de V1.

Histórico de versões do ciclo: 0.9.50 (59), base pública e Inicial; 0.9.51 (60), validação de Estudar em PR, sem publicação intermediária; 0.9.52 (61), consolidação Inicial/Estudar/MIDI para distribuição permanente. Referências originais e dados preservados.

## Consolidação MIDI — Prompt 3 em revisão

Branch `feat/consolidacao-midi-interface-piano-livre`, base Prompt 2 `2739bb2`: `MidiConnection` centraliza os estados e recursos reais de conexão para Inicial/Estudar, rejeita callbacks de gerações antigas e exige portas funcionais. Desconexão/troca pausa sem rebobinar; saída encerra notas/pedal; foreground reconcilia inventário e descarta lotes/fragmentos antigos. `StudyChromeState` e layouts preservados. Versão 0.9.52-open-beta (61). [Documento 72](72-CONSOLIDACAO-MIDI-INTERFACES-PROMPT-03.md) e registro final do PR distinguem resultados por HEAD, evidência sintética e hardware pendente. Sem merge/Release; Prompt 4 é a próxima etapa separada, após validação técnica deste prompt. Os blocos anteriores abaixo permanecem como histórico das entregas.

## Tela Estudar — entrega do Prompt 2 em PR dependente

Branch `feat/tela-estudar-piano-livre`, no [PR #73](https://github.com/atshimura/Piano-livre-aberto/pull/73), baseada no Prompt 1 e na preparação documental do PR #72. Cabeçalho unificado, remoção do guia do Estudar, renderização automática, faixa MODO/MÃOS e transporte permanente. Os três estados seguem a imagem aprovada; medições observaram +29 px de pauta no estado normal e +179 px com barras ocultas, nos viewports comparáveis de celular/tablet. Versão de validação `0.9.51-open-beta` (60). [Documento 71](71-TELA-ESTUDAR-PROMPT-02.md), com decisões, regressões e proveniência; SHA/checks e encerramento técnico no registro do PR #73. Próxima etapa após aceite técnico: Prompt 3 — consolidação dinâmica e USB-MIDI entre telas. Sem merge, Release ou início do Prompt 3 nesta entrega; aceite físico e V1 continuam pendentes.

## Implementado no código público

- Android em paisagem, local/offline, sem permissão INTERNET. MusicXML/XML/MXL e MIDI SMF 0/1 PPQN com notação aproximada, escolha explícita de mão por trilha e fonte original preservada.
- Partitura temporal SVG em tiles, claves Sol/Fá fixas, leitura livre via OSMD. Teclado guia visual de ambas as mãos com intensidade/duração; índice evita varrer notas encerradas sob baixo longo.
- Modos Espera/Contínuo/Demonstração e rotas musicais. Sessões MIDI isoladas por conexão, retomada em foreground e CC120/121/123; USB real ainda requer aceite.
- Extras não bloqueiam Espera; acordes dependem de calibração medida; duração/pedal são orientação.
- Perfis com IDs estáveis, nome/avatar/exclusão confirmada, busca/favoritos/coleções, histórico de tempo de tela e metas voluntárias, sem nota global.
- Backup v2 pré-validado com rollback/journal, caminhos portáteis e nomes legados; preparação antes de abrir destino externo. Importações novas preservam capacidade do backup. Limites no documento 43.
- CI JVM/JS/Android, corpus musical, interface offline, atualização desde APK público 0.9.25 e ensaio debug→release. Conferir sempre os resultados do próprio SHA.
- Distribuição release sem depuração, pacote aberto e certificado permanente; publicação de beta aguarda checks do merge. PRs/emuladores usam somente chave descartável. Documento 44.

## O que ainda impede declarar 1.0

Aceite físico na issue #11: Yamaha P-45 (USB, reconexão, calibração, pedal, Demonstração/notas presas), Realme 7 5G e Lenovo Tab M9 (fluidez, resize, background, instalação/atualização e backup reais). Na 0.9.12 houve pequenas travadas no Realme; não há confirmação física de eliminação nas versões posteriores.

Escopo musical continua limitado: MIDI não reconstrói a escrita original nem pedal/programas/controladores; mãos são escolhidas por trilha inteira. Microtons/transposição/compassos MusicXML incompatíveis são rejeitados explicitamente. Codificações suportadas e limites estão nos documentos 36–44. Validação automatizada não representa fidelidade universal nem revisão editorial de toda partitura externa.

Migração entre pacotes é manual por backup validado; não há leitura do pacote/repositório privado nem atualização automática da edição histórica. A assinatura permanente da linha aberta tem fingerprint registrada no documento ASSINATURA; conferir em cada build distribuído.

## Roteiro e retomada

Etapas 1–5 encerradas no escopo técnico automatizado (#6/#8/#7/#9/#10), com limites explícitos. Etapa 6 em consolidação (#11), incluindo PRs #29–#33 e aceite físico pendente. Fonte de versão: gradle.properties. Estado atual de merges/Actions/Releases: issues #4/#11 e GitHub; não repetir entregas já integradas.

GPL-3.0-only para o código próprio; direitos das músicas e de terceiros separados em seus manifestos. Sem login, servidor, telemetria ou serviços pagos. Continuar correções independentes quando houver evidência, mantendo dados offline e decisões pedagógicas.

## Nova linha funcional: Laboratório Musical

Produto aprovou nova área Descobrir/Aprender com teclado virtual+USB-MIDI, pauta sempre visível, catálogo amplo, práticas, demonstração, ritmo opcional e histórico por perfil, além de melhorias em Espera, biblioteca, notação e toque na cabeça da nota. A implementação ocorre incrementalmente na [issue #34](https://github.com/atshimura/Piano-livre-aberto/issues/34) e no [documento 45](45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md). A primeira entrega inicia a análise offline de acordes e uma interface experimental, **sem equivaler ao Laboratório completo ou a aceite físico**. O texto anterior sobre V1 permanece válido e #11 continua pendente.

## Redesenho da tela inicial — Prompt 1 tecnicamente concluído

Branch `feat/tela-inicial-piano-livre`, no [PR #71](https://github.com/atshimura/Piano-livre-aberto/pull/71), preserva o Prompt 1 e a referência visual do PR #70. Implementação técnica concluída no escopo automatizado e visual: cabeçalho próprio, banner sem duplicação, quatro atalhos, biblioteca dinâmica, miniaturas ilustradas e fallback, busca/favoritos/coleções/importação e indicador USB-MIDI. Os workflows do código `31258025` passaram no checkout temporário `b387a038`; APK debug e capturas foram conferidos. Consulte [70](70-TELA-INICIAL-PROMPT-01.md) para proveniência, links, capturas preservadas, checks documentais e limitações.

A entrega está pronta para integração posterior, com o PR aberto e separado da `main`. Nenhuma Release oficial foi publicada nesta entrega e a versão permanece `0.9.50-open-beta` (59). A V1 não está concluída: aceite físico no Lenovo Tab M9, Realme 7 5G e Yamaha P-45 continua pendente. Próxima etapa: Prompt 2 — reformulação da tela Estudar, não implementada aqui; integração/publicação consolidadas pertencem ao Prompt 4.

# 58 — Plano Geral de Desenvolvimento do Piano Livre até a versão 1.0

**Proposta consolidada em 20/10/2026.** Base: [inventário 57](57-INVENTARIO-FUNCIONAL-V1.md), [requisitos](01-VISAO-E-REQUISITOS.md), [decisões](08-DECISOES.md), [issue geral #4](https://github.com/atshimura/Piano-livre-aberto/issues/4), [consolidação #11](https://github.com/atshimura/Piano-livre-aberto/issues/11), [Laboratório #34](https://github.com/atshimura/Piano-livre-aberto/issues/34) e [estudo de gravura #46](https://github.com/atshimura/Piano-livre-aberto/pull/46).

> **Versões atuais são de desenvolvimento; não existe versão estável final.** Este documento substitui **a execução rígida das 20 tarefas do item 1.2** por três entregas com aceitação prática e reúne os demais recursos num só roteiro. Não anula os requisitos/decisões aprovados: consolida prioridades e separa o que está pronto do que ainda exige implementação. Etapas 1–5 antigas têm **fechamento técnico delimitado**, não precisam ser refeitas; a Etapa 6/V1 exige aceite físico e de produto.

> **Atualização de estado — 21/10/2026:** Entrega A concluída na `main` pelo PR #49 (`0.9.47-open-beta`, code56), com build assinado, testes no emulador incluindo atualização sintética code55→56 preservando dados e pré-release publicada. Nenhum desses resultados equivale a teste físico. A sequência abaixo foi atualizada: agora cabe avaliar o APK no aparelho, seguir com B pelos problemas observados e preparar o Laboratório Descobrir em PR independente.

## 1. Objetivo da versão 1.0

Disponibilizar um aplicativo Android offline para aprender e estudar piano por MusicXML/MIDI, com duas pautas legíveis, funcionamento previsível do Estudo, reconhecimento USB-MIDI com Yamaha P-45, guia visual para as duas mãos, biblioteca/perfis/histórico, importação segura e backup/atualização preservando dados. **Na versão 1.0, o Laboratório será centrado exclusivamente em Descobrir por teclado MIDI físico**, com identificação de nota/acorde/escala ou arpejo e pauta correspondente; a aba Aprender ficará visível e desabilitada. O motor de Aprender já existente fica preservado para evolução posterior. A interface aprovada de Descobrir ainda precisa ser implementada.

**Princípio:** a próxima entrega deve ser uma melhoria que o usuário possa instalar, observar ou exercitar, não apenas uma coleção crescente de fixtures raras. Um teste falho ou erro musical silencioso é sempre tratado; suporte universal a todo MusicXML não é pré-condição da V1.

## 2. Política de escopo e qualidade

| Prioridade | Regra |
|---|---|
| **P0 — bloqueador de APK/segurança/dados** | Package/certificado incompatível com atualização; risco de perda de dados; crash frequente; falha de parsing sem aviso; notas MIDI presas; quebra do modo Espera; falha grave de direitos/licenças |
| **P1 — essencial para experiência de Estudo/V1** | Legibilidade e fluidez, contexto e duas pautas, cursor/teclado, toque e loop comuns, calibração USB-MIDI, recuperação/fallback, biblioteca e progresso confiáveis |
| **P2 — melhoria incremental** | Casos editoriais raros, otimizações não justificadas por medição, refinamentos amplos do Laboratório, ergonomia e acessibilidade adicionais |
| **Fora do compromisso imediato da V1** | Windows, nuvem/conta, telemetria, servidor, conteúdo audiovisual não autorizado, sintetizador Android obrigatório, editor completo de partituras, avaliação automática rígida sem calibração |

A classificação pode mudar diante de defeitos observados pelo usuário. Não adiar P0 por ser caso raro. Não inventar métricas/tempo de tolerância MIDI, dedilhado obrigatório nem nota global.

## 3. Três entregas do item 1.2 — partitura OSMD

A **decisão de produto foi implementada na beta 0.9.47**: OSMD é o renderizador padrão do Estudo; o motor temporal anterior permanece acessível como alternativa. `Partitura Livre` convencional é outro modo, não um botão oculto para a troca.

| Entrega | Objetivo e escopo delimitado | Critérios de aceite | Situação no momento do planejamento |
|---|---|---|---|
| **A — OSMD padrão + APK atualizável** | Inicializar Estudo diretamente em OSMD, opção visível de voltar ao temporal, compatibilidade do mesmo aplicativo, build/release assinado permanentemente | `applicationId` público `br.org.pianolivre.opensource`, certificado permanente, `versionCode` 56 maior que code55; atualização sintética in-place preservou dados; CI, emulador, APK publicado | **Concluída:** PR [#49](https://github.com/atshimura/Piano-livre-aberto/pull/49) integrado; build assinado [39531053142](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39531053142), emulador/atualização [39531053402](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39531053402), pré-release [0.9.47](https://github.com/atshimura/Piano-livre-aberto/releases/tag/v0.9.47-open-beta). Teste físico e pacote realmente instalado no aparelho do usuário ainda precisam de confirmação.
| **B — Estudo utilizável de ponta a ponta** | Avaliação visual pelo usuário primeiro; corrigir etiqueta que cobre pauta, toque e cursores, modo Espera, mãos, teclado guia, reprodução, seleção e loop **usuais**, ligaduras/repetições que afetem o repertório de teste | Uma partitura curta e uma longa realmente estudáveis, cursor e notas sincronizados, transições teclado/pausa/início corretas, fallback; testes dos defeitos + emulador e aceite do usuário do tamanho/contraste | **Próxima entrega de experiência:** observar a beta 0.9.47 no aparelho do usuário e tratar problemas concretos em PRs pequenos. Testes físicos aguardam disponibilidade, sem bloquear trabalho independente |
| **C — Consolidação OSMD** | Medir travadas e consumo, longo MusicXML, background/retorno, mudanças de dimensão, atualização/backup, regressões integradas; corrigir falhas observadas | Sem regressões P0/P1, desempenho aceitável nos aparelhos-alvo, restauração/atualização sem perda, métricas e limitações registradas; usuário aprova experiência | **A fazer após B**, sem afirmar fluidez física só por WebView no emulador |

**Invariantes do Estudo:** duas pautas visíveis simultaneamente, **rolagem vertical zero**, escala uniforme; claves/armaduras/compassos à esquerda e atualização de contexto; expressão musical sem recorte; movimento horizontal no relógio nativo; ao abrir teclado esconder barras superiores e restaurar ao fechar; manter alternativas seguras e não alterar MusicXML importado. A adaptação visual no celular deve ser aprovada pelo usuário: encaixar não é sinônimo de legibilidade.

**Escopo que não deve atrasar A:** certificação de todos os D.C./D.S./casas, todas as ligaduras e grace, novo motor musical, arrasto sofisticado, segmentação preventiva, benchmark sem hipótese concreta e reexecução integral das antigas 20 tarefas. Esses itens serão abordados na B/C **se necessários**, com um teste adversarial por defeito real. Parte da notação avançada já foi examinada na [Tarefa 06, doc 56](https://github.com/atshimura/Piano-livre-aberto/blob/research/estudo-gravura-osmd-poc/docs/56-ESTUDO-GRAVURA-PROFISSIONAL-POC.md).

**Histórico de compatibilidade da Entrega A:** a pesquisa anterior continha variante `osmdTrial`/`.osmdtest` com assinatura debug; ela não foi distribuída como atualização. O PR #49 manteve o pacote público `br.org.pianolivre.opensource`, code56 e certificado permanente, sem app separado. A atualização code55→56 foi comprovada com dados sintéticos no emulador; ainda não foi confirmado qual pacote está instalado no aparelho do usuário. Se for o histórico `br.org.pianolivre`, não é atualizável sobre ele; não desinstalar nem apagar dados para contornar. Segredos de assinatura permanecem exclusivamente nos Actions confiáveis.

## 4. Demais funcionalidades: desenvolvimento após e em paralelo às A/B/C

Estas trilhas **não** exigem reabrir tarefas antigas já integradas. Dependências de A/B/C devem ser respeitadas; melhorias autônomas sem dependências podem prosseguir em PRs isolados.

| Trilha | Estado integrado e trabalho restante | Prioridade / critério |
|---|---|---|
| **D1 — Aprendizagem e MIDI** | Espera/Contínuo/Demonstração, mãos, guia e calibração por perfil implementados. Consolidar com OSMD, calibração medida, USB/reconexão/pedal, prevenção de notas presas; revisar UI com piano real | **P0/P1.** Não bloquear por demora, extras apenas indicadas, duração/pedal orientativos; medir hardware |
| **D2 — Laboratório Descobrir (1.0)** | Adaptar a interface existente à decisão aprovada: botão **Laboratório Musical** na tela inicial; barra Voltar, Descobrir ativo e Aprender desabilitado; instrução única antes do MIDI; **entrada física MIDI sem teclado virtual**, nota/cifra/oitava com pauta correta; acorde por ataques próximos e escala/arpejo por sequência observada automaticamente | **P1 obrigatório para 1.0.** Testes de UI, notas/oitavas/claves, simultaneidade, escalas e arpejos, estados ambíguos e Yamaha físico |
| **D3 — Laboratório Aprender (pós-1.0)** | Catálogo pesquisável, teclado e pauta esquemática **já existentes**, porém a **aba Aprender ficará visível, desabilitada na UI da 1.0**. Preservar código e dados, sem iniciar novos recursos de dedilhado/consulta para liberar 1.0 | **Adiado**, salvo nova decisão explícita |
| **D4 — Laboratório Demonstração/Prática (pós-1.0)** | Demonstração USB-MIDI e prática sequencial de escalas/arpejos **já implementadas**, mas associadas à futura experiência Aprender. Não apagar implementação; não exigir metrônomo, feedback pedagógico ou prática guiada para V1 | **Adiado** sem destruir funcionalidades existentes |
| **D5 — Histórico do Laboratório (pós-1.0)** | Histórico geral de tempo/metas por perfil já existe; histórico específico de exercícios do Lab **não foi implementado** e deixou de ser requisito da V1 após aprovação da UI Descobrir mínima | **Adiado**, definir em versão posterior |
| **D6 — Biblioteca e perfis** | Busca, favoritos, coleções, grade compacta, perfis com ID, histórico/metas e backup implementados. Aprimorar usabilidade/acessibilidade a partir do teste físico e corrigir achados | **P1** para regressões/dados; cosmética **P2** |
| **D7 — Importações e compatibilidade** | XML/MXL, MIDI SMF 0/1 aproximado com arquivo fonte, validação/caminhos portáteis/backup presentes. Reforçar entradas adversariais e mensagem de limitações, verificar restauração e nomes em Android real | **P0** se perda/silêncio; restante **P1/P2** |
| **D8 — Avaliação musical rigorosa** | Há concepção de avaliação separada da aprendizagem, mas faltam produto e calibração. Não somar nota global nem confundir tempo estudado com qualidade | **Decisão de escopo pendente**; não bloquear lançamento de Estudo competente nem criar métricas sem aprovação |

**Pendências da [issue #34](https://github.com/atshimura/Piano-livre-aberto/issues/34)** são um backlog funcional, não prova de que as entregas recentes estejam ausentes; conferir código e docs 50–55 antes de novos commits. Por exemplo, catálogo, demonstração e prática sequencial estão na `main` 0.9.47, embora alguns checkboxes históricos sigam abertos.

### Fluxo de aceitação — Laboratório Musical 1.0

**Nova decisão de produto aprovada em 20/10/2026; tem prioridade sobre propostas anteriores do doc 45.** A existência de componentes experimentais do Laboratório na `main` não prova que a UI exigida esteja pronta.

1. **Tela inicial:** ação visível `Laboratório Musical`. O clique abre o Laboratório.
2. **Barra da tela:** `Voltar`, `Descobrir`, `Aprender`; Descobrir é o padrão. Aprender é visível **mas desabilitado** na 1.0.
3. **Estado vazio inicial:** conteúdo **somente** `toque uma nota, acorde ou escala musical`, sem teclado virtual, catálogo, botão iniciar/gravar ou painel adicional. A barra superior permanece visível.
4. **Entrada:** teclado físico MIDI; não exigir nem exibir teclado virtual nesta experiência 1.0.
5. **Nota isolada:** após observação breve, exibir nome em português, notação internacional e oitava (ex.: `DÓ - C4`) e **pauta efetiva** com a nota na posição musical correta, usando clave de sol/fá de acordo com registro. Não confundir altura MIDI, grafia escrita e tonalidade presumida.
6. **Acorde:** aguardar uma janela curta de ataques próximos para não relatar falsamente a primeira nota como resultado definitivo; indicar acorde identificado com grau de confiança conservador e as notas na pauta. Não adotar valor fixo arbitrário em ms.
7. **Escala ou arpejo:** monitorar notas **sucessivas** de modo automático, sem controle manual de gravação. Uma pausa/inatividade ou outro delimitador robusto permitirá conclusão de sequência. Quando for identificado um padrão, apresentar **seu nome e todas as notas reconhecidas na pauta, em ordem de execução e com as oitavas corretas**, usando clave(s) adequadas ao registro; acordes mostram suas notas simultaneamente. Não desenhar apenas a tônica ou um diagrama genérico. O intervalo necessário para uma frase não é igual à curta janela de acorde: identificar classe musical só com material suficiente; ambiguidades devem ser apresentadas como possibilidades ou sequências, não acertos forçados.
8. **Persistência do resultado:** manter o reconhecimento exibido (texto e notas na pauta) **até a próxima identificação válida**. Não usar timeout de ocultação; ao tocar novas notas, conservar a identificação anterior enquanto o motor decide se a nova entrada forma acorde, escala ou arpejo. Substituir de uma só vez ao concluir o novo reconhecimento; mostrar a instrução inicial somente antes do primeiro resultado.
9. **Regressões:** reaproveitar testes do `MusicTheory`, `MusicSequenceAnalyzer`, `MusicPatternTracker`, entrada USB, e acrescentar Android UI com `Aprender` indisponível, pauta correta por altura/clave, acordes e sequências; comparar **cada nota desenhada** com a sequência MIDI capturada e validar no Yamaha P-45 posteriormente.
10. **Compatibilidade:** não eliminar nem migrar destrutivamente catálogo, práticas ou demonstração já codificados; apenas deixá-los indisponíveis na experiência 1.0, preservados para versões posteriores.

**Entrega dessa funcionalidade:** PR e testes próprios; não constitui parte da **Entrega A do OSMD** nem justifica atrasar o APK de teste. Implementação ainda pendente.

## 5. Ordem de execução recomendada e dependências

1. **A — concluída:** PR #49 integrado, build assinado e atualização sintética code55→56 validados; APK [0.9.47-open-beta](https://github.com/atshimura/Piano-livre-aberto/releases/tag/v0.9.47-open-beta) disponível. Não gerar variante separada nem repetir a entrega.
2. **Agora — avaliação do usuário:** instalar/abrir no Realme 7 5G, se o app presente for a edição pública `br.org.pianolivre.opensource`; diagnosticar pacote/certificado antes caso haja dúvida e nunca desinstalar/apagar dados. Avaliar leitura das pautas, expressão, teclado, conforto, cursor e microtravadas. Usar Lenovo Tab M9 quando possível. Registrar problemas concretos.
3. **B — correções de experiência** em pequenos PRs priorizados pelos resultados reais; manter alternativa temporal e evidência de teste. Não iniciar automaticamente auditorias MusicXML exaustivas sem defeito.
4. **Em paralelo à avaliação e às correções B:** preparar em PR próprio a interface mínima aprovada do **Laboratório Descobrir 1.0**, reaproveitando `MusicTheory`, `MusicSequenceAnalyzer`, `MusicPatternTracker` e a entrada USB-MIDI existentes. Não reconstruir reconhecedores sem evidência de lacuna; acrescentar testes de UI, pauta/clave/oitava, acordes, sequências, ambiguidades, persistência do resultado e estado desabilitado de Aprender. Dedilhado, prática guiada, metrônomo e histórico específico ficam pós-1.0. Continuar correções reais de backup/importação/USB em PRs independentes.
5. **C — consolidação:** benchmarks comparáveis e regressões integradas no SHA final, atualização do APK real e restauração não destrutiva; investigação de compatibilidade física e MIDI.
6. **Candidato 1.0:** fechar pendências P0/P1 aceitas, declarar limitações documentadas, obter testes reais e aceite de produto. Só então decidir tag/versão 1.0 e publicação.

**Não colocar o desenvolvimento independente em espera** por hardware ainda indisponível; entretanto, não fechar aceite físico ou certificar 1.0 em seu lugar.

## 6. Matriz de aceitação da V1 (evidências mínimas)

| Teste de saída | Evidência exigida |
|---|---|
| Identidade/distribuição | APK release sem depuração, mesmo `applicationId`/certificado da linha pública, code maior que o instalado; assinatura exclusivamente via Secrets de workflow confiável; metadados e hashes |
| Atualização/dados | Ensaio `adb install -r` com APK anterior **de mesmo pacote/certificado**, perfis, histórico, favoritos, importações e configurações sobrevivem; backup/restauração com pré-validação e rollback; ensaio posterior em telefone/tablet |
| Estudo/partitura | OSMD padrão, temporal alternativo, leitura confortável em Realme/Lenovo, duas pautas, sem scroll vertical, contexto correto e cursor/tap coerentes |
| Modos/MIDI | Espera, Contínuo e Demonstração; extras não bloqueantes, janela calibrada, ambas as mãos, teclado guia, loop usual, notas ligadas, desconexão/CC e Note Off, testes físicos no Yamaha |
| Importação e conteúdo | Corpus MusicXML/MXL/MIDI de regressão, rejeições com aviso, origem musical intacta, direitos das partituras auditados |
| Biblioteca/perfis/Laboratório | Sem contaminação entre perfis nem regressão de dados; **Laboratório Musical > Descobrir** selecionado por padrão, **Aprender** presente mas inativo, instrução única, reconhecimento MIDI físico automático de nota/acorde/escala/arpejo e representação adequada na pauta; aceite Yamaha físico |
| Estabilidade | CI JVM/JS/Android no SHA correto, emulador e medições físicas representativas, pausa/retomada, partitura longa, mudança de tamanho e ausência de regressões graves |
| Encerramento | Limitações públicas, manual atualizado, pendências classificadas, aceite do usuário e registro na issue #11; nada de declarar teste físico por inferência |

**Roteiro físico prioritário:** Realme 7 5G (microtravadas relatadas em versão antiga), Lenovo Tab M9 (espaço/legibilidade), Yamaha P-45 (USB, acordes, pedal e saída sem notas presas). Registrar modelo, versão Android, versão APK, arquivo, modo, passos e observação, sem dados privados.

## 7. Regras de execução para desenvolvimento contínuo

- Consultar [AGENTS](../AGENTS.md), [inventário 57](57-INVENTARIO-FUNCIONAL-V1.md), este plano e os docs de subsistema antes de mexer no código. **PR + proteção main**, sem forçar merge, sem repositório histórico.
- Para cada mudança: hipótese/problema, implementação mínima, testes adequados, CI/artefato do SHA exato, impacto em dados/versão, evidência visual quando pertinente, limites, próximo trabalho. Documentar em commit recuperável, para resistir a interrupções do Work.
- Se app distribuído mudar: `versionCode` crescente, APK da mesma linha, assinatura permanente dos Actions. Nunca usar PR com chave temporária como release atualizável. Não remover dados nem modificar o XML-fonte para mascarar problemas gráficos.
- Nenhuma incorporação de servidores, sincronização automática, telemetria, serviços pagos, mídia/partituras sem licença. GPL-3.0-only no código próprio; licenças de bibliotecas e direitos musicais independentes.
- A qualidade é aferida por **funcionar e ser utilizável**, além de testes. Falha conhecida documentada não é certificação universal; riscos P0 não são dispensados por limitação de tempo.

## 8. Decisões aprovadas e sequência após a Entrega A

**Laboratório 1.0 já definido pelo usuário:** apenas Descobrir com entrada MIDI física, instrução inicial, nome escrito e pauta, detecção automática; Aprender desabilitado. A documentação detalhada e definitiva está em docs 45 e nos critérios de D2/fluxo de aceitação deste plano. Não reabrir proposta de histórico de exercícios, dedilhado ou metrônomo para V1 sem nova decisão. A avaliação do OSMD no aparelho pode ocorrer enquanto o Laboratório é preparado em PR separado. Avaliação formal de ritmo/duração/articulação permanece posterior e depende de novas decisões.

## 9. Controle e manutenção

O [inventário 57](57-INVENTARIO-FUNCIONAL-V1.md) contém os identificadores INV-01…INV-23 e suas evidências. Atualizar ambos quando uma entrega mudar o status ou os limites. Em cada retomada, verificar primeiro HEAD da `main`, PR #46, PRs abertos, CI e última Release; depois ajustar o plano, **sem reproduzir as antigas tarefas 07–20 automaticamente**.

**Condição de conclusão do planejamento:** documentação revisável e auditável publicada por PR próprio, **não** confundida com implementação nova do aplicativo, teste de regressão ou aceite 1.0.
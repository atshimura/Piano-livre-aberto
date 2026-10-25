# 65 — Entrega B: auditoria e reconciliação técnica

**Auditoria em 25/10/2026.** Repositório `atshimura/Piano-livre-aberto`. `main` conferida em `801543cc8c26d2c1c216cc7a43b01211dc07a4ad`, versão `0.9.47-open-beta` / `versionCode 56`. Nenhuma integração foi feita. Esta revisão complementa os documentos 57–64 e preserva o inventário e o plano do PR #48.

## Causa da falha de jornada Android

O run [40022067356](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40022067356), SHA `143cc041089c2653fe621f7d5be510480c60a72c`, falhou no `emulator_study_session.py` ao procurar `Estudo livre 01`; a hierarquia retornou somente `♫ Upgrade A` e `★ Upgrade B`. O run [40022296949](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40022296949), SHA `931fe1798b548a75c9319e614ddedf73d41e4284`, teve a mesma falha.

O primeiro exame apontou contaminação de **estado de interface** entre cenários: o ensaio de atualização instala perfis sintéticos A/B e outros roteiros reutilizavam Activity/janelas. A primeira correção passou a encerrar/reabrir a Activity sem apagar preferências ou arquivos. A execução posterior confirmou o defeito imediato restante: `emulator_study_session.py` procurava o primeiro widget `Spinner` da tela, mas há dois — o seletor de perfil fixo no cabeçalho e o filtro da biblioteca. Com os perfis sintéticos, o primeiro mostra `Upgrade A/B`; o teste o tocava e depois procurava `Todas` no menu de perfis. Isso explica exatamente a hierarquia registrada. O seletor não substitui o catálogo; o teste mirava no controle errado. As fixtures de perfis/arquivo continuam necessárias para testar atualização e não devem ser apagadas antes das asserções.

Não se encontrou evidência de que os dados sintéticos substituam o catálogo ou de que perfil ativo/favorito altere o catálogo básico: `PianoScreens.refreshLibrary()` lê `catalog.json`, busca estado da coleção por usuário e inicia o filtro em `Todas`; `LibraryStore` salva favoritos/coleções em preferência separada do catálogo. A falha observada, portanto, não demonstra defeito de dados ou filtro em produção. Como a tela não foi alcançada de modo confiável, o histórico não permite excluir definitivamente uma falha de navegação; a nova jornada precisa comprová-la no emulador.

## Correção e diagnósticos da jornada

A branch experimental `ci/entrega-b-integral-20261025`, iniciada da prova combinada #57, altera o roteiro e documentação nesta revisão. Antes de iniciar, o roteiro faz `force-stop` e relança a Activity. Isso descarta somente janela/overlay transitório do emulador; preserva arquivos e SharedPreferences, inclusive a fixture de atualização. A navegação aguarda a biblioteca, retorna do Estudo quando necessário, rola a lista ao início e só envia Back para popup de perfil/filtro positivamente identificado; durante carregamento, espera sem fechar o app. Verifica o catálogo empacotado e classifica: fixture curta ausente (`SCORE_MISSING`), falha de navegação (`NAVIGATION_FAILED`), filtro/coleção ocultando partitura (`SCORE_HIDDEN_BY_FILTER`), catálogo não carregado (`LIBRARY_NOT_LOADED`) ou partitura existente sem visibilidade (`SCORE_NOT_VISIBLE`). A espera é limitada; não torna a asserção mais permissiva.

O script passou `python3 -m py_compile` localmente. O ambiente desta execução não dispõe de `adb`, Gradle ou Android SDK; portanto ainda não há reprodução local do emulador nem prova do resultado corrigido. A validação exigida é o workflow completo de emulador, incluindo upgrade, auto-testes, roteiro existente e jornadas curta/longa, no SHA final desta branch.

## Reconciliação dos PRs

| PRs | Decisão técnica para revisão | Estado observado |
|---|---|---|
| #52 / #58 | A composição conserva uma só API `StudyNoteTip` compartilhada pelos renderizadores OSMD e temporal; grafia/oitava só é apresentada quando reconciliada com evento MusicXML e MIDI. O posicionamento avalia caixas visíveis, cabeças e contexto para evitar ocultar a escrita; etiquetas são de linha única/temporárias e há cancelamento em troca de tela/trecho. Essa é a seleção adotada para a prova; não entram renderizadores ou timers concorrentes. A suíte JS de etiqueta passou 34 verificações localmente; inspeção visual e expiração no Android continuam dependendo da CI. | #52 e #58 abertos. #58 SHA `d6e0556`; Actions individuais recentes passaram, mas não são prova desta composição. |
| #54 / #59 | A composição agora usa `StudyLoopPosition` de #59 para o menu A/B: A alinha a ataque; B inclui liberações de vozes sustentadas; as operações param reprodução antes de substituir limites/rota. A classe/teste obsoleto `StudyLoopEditing` foi removido; preservou-se a seleção gráfica A/B com MusicXML intacto. Há regressão JVM musical, contrato JS de transporte e passos Android A/B/remover em reprodução. | Portado à branch experimental; aprovação composta ainda pendente. PR #59 continua aberto, SHA `e5b52cd`, seus checks individuais não certificam esta composição. |
| #53 / #60 | Consolidar uma jornada curta/longa no núcleo, preservando cobertura equivalente ou superior a `STUDY_JOURNEY_PASS assertions=2510`; manter casos explícitos de UI Android separados. | #53 e #60 abertos; #60 SHA `54b9165`, aprovação individual declarada no histórico, não é prova B1+B. |
| #55 | Preservar a cobertura Android real de UI; sua falha não é substituída por teste JVM. Reexecutar com isolamento de janelas transitórias. | #55 SHA `143cc04`, emulador falhou; build passou. |
| #56 / #57 / #61 | Somente provas experimentais. Manter intocados; não integrar. Recriar prova atualizada e identificada. | #56 é baseada em B1; #57 e #61 baseadas em main para acionar workflows e contêm integração experimental. |

Os atuais PRs #46, #47, #48 e #51 foram preservados. `docs/57` e `docs/58` mantêm o conteúdo existente do PR #48 e recebem apenas adendos de rastreabilidade; docs 59–64 e evidências B1 permanecem disponíveis na branch de CI. Nenhum fixture original foi alterado nesta correção.

## Branch e evidência composta

`ci/entrega-b-integral-20261025` foi criada da branch experimental #57 (SHA inicial `931fe1798b548a75c9319e614ddedf73d41e4284`), que contém a B1 `15f05d7463794409b64ed9b5efdfc7692ecb4127` e os componentes de estudo copiados para teste. Ela é apenas disparador de CI: não é PR integrável nem muda o propósito de #57. Base documental da B1: [build 40015226806](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40015226806) e [emulador 40015226812](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40015226812), ambos SHA `15f05d7`. Prova experimental #61 no SHA `2fe927c` passou build/emulador, mas não cobre a jornada #55 corrigida nem as revisões posteriores e por isso não é a prova final.

**Validação final pendente:** o build/Chromium/JVM do HEAD `349a7fd` passou no run [40210751911](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40210751911), workflow de pull request avaliado sobre o merge SHA `11e6065`. A jornada JVM curta/longa passou com 15.127 asserções; a prova Chromium do rótulo temporal passou com captura; MusicXML/MIDI e build do APK de verificação também passaram. O emulador [40210751936](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40210751936), no mesmo merge ref, percorreu perfis, biblioteca, OSMD, tempo, teclado guia, background, Gymnopédie de 78 compassos, contexto e loop A/B. A jornada B ainda falhou antes de abrir o exercício: o resultado visível era `10 de 10 partituras`, mas o teste usava substring `"0 de "`, que também casa com o zero final de `10`. A captura `window.xml` mostrou que `Estudo livre 01` está na oitava posição, abaixo do viewport inicial, com filtro `Todas`; a biblioteca está carregada e não há evidência de defeito no app. A correção atual analisa contagens com regex numérica exata, consulta cada título pela busca nativa da biblioteca e inclui regressões para `10 de 10` e `0 de 10`. Reexecutar todo o build e emulador no HEAD final; confirmar jornadas Android curta/longa e artifacts antes de declarar conclusão. Preservar a captura e o diagnóstico do run 40210751936 como prova da classificação correta desse caso.

## Limitações e próximas ações

- Aceite editorial da escala/legibilidade da B1 continua pendente; métricas Chromium não são decisão editorial.
- Testes físicos Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 não foram executados. A simulação MIDI automatizada não prova conexão física nem liberação após desconexão.
- A UI Android validou rótulo OSMD (Mi4 · E4, viewport e expiração); Chromium validou também o clique real no temporal, geometria, expiração e captura. A regressão do localizador do filtro passou; a jornada chegou ao filtro correto, mas expôs o falso positivo da contagem `10 de 10` e a necessidade de pesquisar títulos abaixo da dobra. A análise de hierarquia confirmou o item no catálogo e abaixo do viewport.
- Próxima ação: executar de novo build/Chromium/JVM e emulador com regressão de contagem exata e busca por título; confirmar ambas as jornadas Android e recolher artifacts; depois registrar hashes e resultados finais.

**Atualização após o run 40212363484:** o `window.xml` do artifact ainda mostra ambos os nós `Spinner` com `text=''`, mas a captura `piano-library.png` mostra claramente o filtro da biblioteca em `Todas` e o perfil sintético selecionado no cabeçalho. Portanto, não há evidência de coleção errada no app; a API de acessibilidade não fornece o rótulo selecionado. O teste agora abre o Spinner alinhado à busca, confirma a opção visível `Todas`, seleciona explicitamente, limpa a consulta, exige total de resultados igual ou superior aos itens do catálogo, e só então busca o título. A fixture verifica o caso de texto vazio. O emulador ainda precisa ser reexecutado no próximo HEAD para validar esse percurso e ambas as sessões curtas/longas.
- Integração à `main`, Release, distribuição APK e aceite físico/editorial permanecem pendentes e exigem as condições/autorização já definidas.


## Evidência posterior: codificação da busca Android

A execução `40213805638` do HEAD `04c2bd9bad68e795b48903c35f5a732979a79f1d` falhou após confirmar filtro `Todas`: o teste transmitiu a query literal `Estudo livre%01`, que não representa o espaço antes de `01` no comando Android `input text`. O pacote tinha `Estudo livre 01`; filtro, perfil e carregamento da biblioteca não explicam esse resultado. O defeito estava na codificação da query de UI do próprio roteiro. A jornada agora usa uma função que codifica cada espaço como `%s` e inclui asserção para `Estudo%slivre%s01`. Resultado dos workflows no novo SHA será registrado após a conclusão; a execução anterior não certifica a jornada.


## Resultado do filtro no emulador — 2026-10-25

No SHA `52441848911a23fc430af18e773d56237a8357a3`, o workflow de build passou (run 40215362948), mas a jornada Android falhou no run 40215362946 depois de abrir o Spinner da biblioteca e selecionar `Todas`: a captura imediata da hierarquia não continha novamente o controle de filtro. A busca ainda não havia começado; portanto a correção de query não foi exercitada nessa execução. Como os logs não demonstram que a biblioteca deixou a tela, não classificamos isso como defeito de produção nem como simples falha do teste sem nova evidência. A regressão agora espera a linha de busca voltar após a escolha, dispensa apenas popup identificado e captura XML/screenshot se permanecer ausente. A jornada curta e longa continua pendente do próximo SHA.


## Estado residual da busca em execução Android

O run 40216924861 no SHA `9e0cb5a2ba4f910e73343305ef1d9b558d03e9d1` confirmou que a biblioteca exibia `Todas`, mas o contador era `1 de 10 partituras`. A consulta digitada antes persistiu na interface entre cenários e removeu o hint do EditText; o localizador baseado no hint deixou de reconhecer tanto o campo quanto o Spinner na mesma linha. O registro não prova bug funcional de catálogo: o roteiro havia deixado estado temporário de busca no Activity. A correção busca o único EditText pelo tipo/posição, apaga somente seu texto de consulta, exige que o contador inclua as dez partituras do catálogo empacotado e acrescenta fixture para campo preenchido sem hint. Preservam-se arquivos e dados persistentes. Build verde em 40216924903; a jornada Android completa segue pendente.


## Navegação do título para Estudo

No SHA `dac396fd5bc9f9934efc6d28cf8c6ae67124a494`, a busca determinística passou, localizando `Estudo livre 01` com o catálogo completo. A sequência tocou o texto do título e aguardou `POC_OSMD_READY`, que não apareceu (run 40218278046). O log não trouxe tela/estado suficiente para decidir entre clique sem navegação e abertura sem prontidão. A regressão agora aciona explicitamente o controle `Estudar` e captura screenshot/labels em timeout. Não classificar como resolvido até a jornada Android passar.


## Playhead residual entre cenários

No run 40219775224, o botão explícito `Estudar` levou ao OSMD e `SCORE_DRAWN` foi registrado. A falha ocorreu ao buscar `Pausar`; a interface mostrava `posição 16.0` e `Iniciar`, que corresponde ao fim da partitura curta de 16 tempos. A reprodução iniciada no fim terminou imediatamente. O roteiro passa a usar o controle visual `Início` e verificar posição `0.0` antes de iniciar, sem remover dados ou reduzir cobertura.


## Duração do fixture curto e teste de pausa

No run 40221311869, o teste reiniciou o playhead em zero, iniciou o exercício de 16 tempos e registrou `play=true mode=1`; na leitura seguinte, a música já estava em `16.0` e o botão `Pausar` havia desaparecido. Isso indica fim de execução durante a latência acumulada do fluxo UI no emulador. O teste agora altera o andamento pelo SeekBar para `25%`, confirma esse valor visível e só depois verifica pausa/retomada. A validação do gesto real e da posição inicial permanece assertada.

# 65 — Entrega B: auditoria e reconciliação técnica

**Auditoria em 25/10/2026.** Repositório `atshimura/Piano-livre-aberto`. `main` conferida em `801543cc8c26d2c1c216cc7a43b01211dc07a4ad`, versão `0.9.47-open-beta` / `versionCode 56`. Nenhuma integração foi feita. Esta revisão complementa os documentos 57–64 e preserva o inventário e o plano do PR #48.

## Causa da falha de jornada Android

O run [40022067356](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40022067356), SHA `143cc041089c2653fe621f7d5be510480c60a72c`, falhou no `emulator_study_session.py` ao procurar `Estudo livre 01`; a hierarquia retornou somente `♫ Upgrade A` e `★ Upgrade B`. O run [40022296949](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40022296949), SHA `931fe1798b548a75c9319e614ddedf73d41e4284`, teve a mesma falha.

O exame da sequência do workflow e do código encontrou contaminação de **estado de interface** entre cenários no mesmo emulador: o ensaio de atualização instala perfis sintéticos A/B e deixa estado de sessão, e outros roteiros reutilizam a mesma Activity/árvore de acessibilidade. A jornada iniciava a Activity sem encerrá-la nem descartar popups transientes. Assim, `uiautomator dump` podia inspecionar a lista suspensa de perfis em vez da tela da biblioteca. O perfil e arquivo sintéticos são intencionais para testar atualização; não devem ser removidos antes de suas asserções.

Não se encontrou evidência de que os dados sintéticos substituam o catálogo ou de que perfil ativo/favorito altere o catálogo básico: `PianoScreens.refreshLibrary()` lê `catalog.json`, busca estado da coleção por usuário e inicia o filtro em `Todas`; `LibraryStore` salva favoritos/coleções em preferência separada do catálogo. A falha observada, portanto, não demonstra defeito de dados ou filtro em produção. Como a tela não foi alcançada de modo confiável, o histórico não permite excluir definitivamente uma falha de navegação; a nova jornada precisa comprová-la no emulador.

## Correção e diagnósticos da jornada

A branch experimental `ci/entrega-b-integral-20261025`, iniciada da prova combinada #57, altera somente o roteiro e documentação nesta revisão. Antes de iniciar, o roteiro faz `force-stop` e relança a Activity. Isso descarta somente janela/overlay transitório do emulador; preserva arquivos e SharedPreferences, inclusive a fixture de atualização. A navegação aguarda a biblioteca, retorna do Estudo quando necessário, rola a lista ao início, verifica o catálogo empacotado e classifica: fixture curta ausente (`SCORE_MISSING`), falha de navegação (`NAVIGATION_FAILED`), filtro/coleção ocultando partitura (`SCORE_HIDDEN_BY_FILTER`), catálogo não carregado (`LIBRARY_NOT_LOADED`) ou partitura existente sem visibilidade (`SCORE_NOT_VISIBLE`). A espera é limitada; não torna a asserção mais permissiva.

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

**Validação final pendente:** workflows de build, Chromium, JVM, MusicXML/MIDI, build Android e emulador precisam passar no mesmo SHA desta branch. Confirmar no artifact do mesmo run: jornada curta `Estudo livre 01`, longa `Gymnopédie nº 1`, capturas e JSON; 27 fixtures B1; comparações de Gymnopédie; Canon e Für Elise; OSMD e temporal; liberação MIDI e loops; MusicXML sem alteração. Não declarar sucesso até esse resultado existir. No ambiente local só foram executados `py_compile` e inspeção estática.

## Limitações e próximas ações

- Aceite editorial da escala/legibilidade da B1 continua pendente; métricas Chromium não são decisão editorial.
- Testes físicos Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 não foram executados. A simulação MIDI automatizada não prova conexão física nem liberação após desconexão.
- Ainda falta comparar/portar as correções de apresentação tardias de #58 para a identificação que veio de #52 na composição. As jornadas #53/#55/#60 também devem manter cobertura equivalente, e a UI Android precisa passar após a nova isolação.
- Próxima ação: validar a reconciliação de loop, concluir a comparação do rótulo #52/#58 e das jornadas #53/#55/#60; executar Actions de build e emulador no mesmo SHA; atualizar este documento com IDs de runs e resultado real.
- Integração à `main`, Release, distribuição APK e aceite físico/editorial permanecem pendentes e exigem as condições/autorização já definidas.

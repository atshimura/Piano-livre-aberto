# 10 — Como continuar o Piano Livre aberto

## B1 — continuidade operacional (23/10/2026)

Trabalho ativo no [PR #51](https://github.com/atshimura/Piano-livre-aberto/pull/51). A câmera usa amostragem contínua com envelope de tinta verificado e antecipa a entrada de símbolos, evitando saltos de célula. No Chromium 738×196, o maior salto entre beats adjacentes da Gymnopédie caiu de 6,94% para ~1,52%. O teste de 376 buscas registrou mediana 0,7 ms e p95 6,5 ms no executor Linux. O extremo beat 181 passou de ~4,85 para 5,10 CSS px, graças a um ajuste reversível de duas ligaduras longas a 60% da curvatura original; controles mais agressivos (40%) causaram colisão com cabeças de notas e foram descartados. MusicXML e MIDI permanecem intocados, e a geometria não é mantida se a inspeção constatar nova colisão.

Resultados do [build/Chromium/JVM 39973685094](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39973685094): sucesso; corpus de 27 partituras, Gymnopédie 469/469 notas, recortes/rolagem vertical não detectados nos cenários testados. **Não confundir CI com aceite editorial/físico**. Os estudos de compactação nativa OSMD e as evidências no SHA final ainda precisam ser analisados. Não há autorização para merge, Release ou distribuição. Consultar [doc. 59](59-ESTUDO-ADAPTAVEL.md) antes de mudar os critérios.

---


## B1 — experimento de amostragem espacial e transições (23/10/2026)

A inspeção do `study-window.js` identificou um fator conservador mensurável: cada janela anterior considerava meia largura visível adicional de avanço, além do próprio enquadramento horizontal. Assim, símbolos extremos ainda fora da tela podiam entrar antecipadamente no envelope vertical, reduzindo o tamanho das pautas antes da passagem por eles. A grade experimental agora usa células espaciais menores (um sexto, em vez de metade, da largura projetada no alvo), preservando a inspeção de **todos** os símbolos que possam aparecer em cada célula, as duas pautas e a proibição de recorte vertical. Esta mudança de amostragem **não equivale** a solucionar o extremo: é necessário medir o novo mínimo e as variações, inclusive possível aumento de recálculos/oscilações.

O teste unitário conserva a checagem de recorte em todo o percurso; o teste Chromium produz `window-traversal.json` com `diagnostic` e `transitions` para localizar a maior variação medida entre notas sucessivas. Comparar também abertura, meio, extremo e final nas três dimensões e inspecionar capturas. **Nenhum ganho, suavidade ou conforto editorial é declarado antes do resultado dos Actions do SHA final e da revisão das imagens.** Não foi alterado o MusicXML, o motor MIDI, o pacote ou a assinatura. O PR #51 continua aberto e B1 permanece **PARCIAL**.

A limitação estrutural segue relevante: a compactação vertical global foi recusada em amostra anterior porque deslocar a pauta inferior além de ~9 unidades SVG introduzia colisões de `vf-line` com hastes (`vf-stem`); não é seguro impor maior aproximação sem regravação ou análise por região. Permanecem pendentes o ganho efetivo nos extremos, a continuidade visual e a verificação de contexto fixo em movimento. O diagnóstico de `native-mapping` em vozes ambíguas e os fallbacks explicitados não foram alterados.


### Evidência medida da reserva do contexto fixo (SHA b8ed11e, build 39875463421)

O teste da fixture `engraving-context-change.musicxml` encontrou na implementação anterior uma faixa fixa que podia ocupar 168,94 CSS px enquanto o cursor permanecia próximo de 167 px no terceiro compasso. A revisão passou a reservar, antes do planejamento de janela e cursor, a **largura máxima do contexto efetivamente gravado** na partitura. No Chromium 738×196 a faixa mede 168,94 CSS px e o cursor está em 180,94 px, com 12 px livres e sem recorte nas transições 1→2→3→2→1. No tablet 883×409, faixa 360,34 px e cursor 372,34 px, igualmente com 12 px de folga. Ambos preservam a origem vertical. Isso demonstra ausência da colisão detectada nessa fixture, não prova legibilidade editorial universal.

A nova rodada de 27 arquivos, incluindo Gymnopédie, corpus nativo e fallback temporal, passou no [build 39875463421](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39875463421) do código `b8ed11e63f54d83afc64cdfeb2c8848b2fc2bc46`. As 12 capturas comparáveis da Gymnopédie mantiveram os espaçamentos 5,40241 / 5,16247 / **4,85294** / 5,03093 CSS px (abertura/meio/extremo/final) em 738×196; as telas 738×351 e 883×409 mediram 8,85439 e 10,35163 em todas as regiões. Cortes detectados: zero; rolagem vertical: zero; mapeamento: 469/469. A mudança no extremo baixo segue **insuficiente**, e transições da escala variável, bem como validação real, continuam pendentes. Ver [relatório versionado](evidence/study-profile/b1-window/comparison-validated-2026-10-22.json) e o artifact do run correspondente. Não atribuir esta medição a outro SHA nem ao hardware.

## B1 — continuação: câmera ancorada, contexto e vozes nomeadas (22/10/2026)

**PR #51 permanece aberto, sem merge.** A câmera horizontal agora utiliza a faixa efetivamente alcançável pela área pintada, levando em conta a posição inicial, a margem e a linha de referência. A escala é reavaliada contra o envelope no enquadramento final, com preservação de ambas as pautas, MusicXML e MIDI. O guardião anterior considerava tinta à direita além do que efetivamente poderia entrar na viewport; isso podia diminuir desnecessariamente a escala local.

A faixa de contexto fixo à esquerda agora adapta sua largura ao conteúdo ativo (clave, armadura e fórmula de compasso) e sincroniza o viewBox após deslocamento ou mudança de escala. O corpus passou a pedir capturas de transições 1→2→3→2→1 na fixture engraving-context-change.musicxml e a conferir limites dos glifos, área visível e alinhamento. Testes de janela conferem os extremos, ausência de recorte, integridade do mapeamento e redimensionamentos nos três perfis. Essas verificações não certificam fluidez ou legibilidade subjetiva.

**native-mapping:** em named-voices-ties.xml há quatro eventos nativos C4/MIDI 60 e quatro cabeças OSMD nos mesmos tempos; as vozes nomeadas upper/lower não obtiveram identidade gráfica verificável na evidência anterior (matched=0, ambiguous=4, unmatchedNative=4). Não existe demonstração de nota, andamento ou duração divergente pelo multiconjunto de eventos; existe ambiguidade de voz. Não forçar emparelhamento nem suprimir alerta. O teste exige explicitamente a ambiguidade e compara também a sequência da alternativa temporal. Os quatro fixtures incompletos para OSMD preservam fallback temporal explícito.

**Evidências:** StudyProfileBrowserTest.js produz no artifact adaptive-study-corpus as capturas gymnopedie.window-{opening,middle,extreme,ending}.{738x196,738x351,883x409}.png, b1-viewport-comparison.json, context-change.measure-*.png e b1-context-transitions.json. O script e este protocolo ficam versionados; os novos binários produzidos pela CI não são automaticamente versionados. Consultar o SHA e as medições no PR antes de declarar aprovação. O valor de 6 CSS px continua referência experimental, nunca padrão aceito pelo usuário.

**Checklist físico pendente:** Realme 7 5G — abertura, meio e extremos da Gymnopédie, variação de escala, cursor e retorno; Lenovo Tab M9 — contexto, mudança de compasso, teclado guia ligado/desligado, redimensionamento, repetição e trechos; Yamaha P-45 — USB-MIDI, ataques, mãos, Espera, pausa/retomada e sincronização. Registrar percepção de leitura e movimentos com consentimento. **Nenhum teste físico foi realizado.** B1 e V1 não foram concluídas. Sem alteração de dados, versão, pacote, certificado, APK de distribuição ou release.

## Estado B1 vigente — janela horizontal (22/10/2026)

**E/IP; B1 ainda parcial**, no [PR #51](https://github.com/atshimura/Piano-livre-aberto/pull/51), branch `feat/analise-estudo-adaptavel`. Esta atualização substitui as métricas e pendências históricas de B1 abaixo, afetando INV-02/03/15. Código aprovado: `1e8ca06bae8270ad66e8586d5ec731b81ac818ed`; [build/JVM/JS/XML/MIDI/corpus 39828730389](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39828730389) e [emulador Android 39828730333](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39828730333), ambos `success`. O SHA final documental e seus próprios workflows ficam registrados no PR; não substituir essa conferência por resultados antigos.

A auditoria passou a incluir tinta das hastes e a restaurar barras/chave no resize; a compactação antiga ignorava hastes de largura geométrica zero. A janela horizontal conserva o SVG e os eventos completos, ajustando escala uniforme pelos símbolos visíveis e contexto das duas pautas, sem scroll vertical. Gymnopédie em 738×196: abertura 4,85313→5,40241 px (+11,32%); percurso 4,85294–5,40241, ainda com `small-staff-space`. Em 738×351/883×409: 8,85439/10,35163 px. Não basta para declarar a leitura confortável.

Canon: listas de solfejo/dedilhado em `words` ocultas somente na apresentação de Estudo; cifras `D A Bm F#m` e `G D G A`, andamento e crédito mantidos nas três dimensões, sem cifra escondida. Für Elise: `p` do compasso 13/beat 36 preservado e reposicionado por colisão real de tinta com G♯3, sem corte/colisão depois; ajuste depende da escala. Partitura Livre e fontes permanecem completas.

Corpus aprovado: 27 arquivos, 23 OSMD e quatro alternativas temporais explícitas; assinatura musical conferida por viewport. Gymnopédie: 469/469 notas, 376 posições de ida/volta sem corte, resize reversível com claves preservadas. `named-voices-ties` conserva alerta `native-mapping`. [Métricas, implementação, capturas e limites](59-ESTUDO-ADAPTAVEL.md#estado-verificado-da-continuação--22102026).

**Falta para encerrar B1:** legibilidade dos trechos extremos em tela baixa e avaliação editorial da escala variável; ensaios físicos Realme 7 5G, Lenovo Tab M9 e Yamaha P-45 continuam a cargo do usuário. Não houve teste físico, merge, release ou APK de distribuição. Pacote/certificado/versão e dados não foram alterados nesta continuação. PRs #46/#48 permanecem intocados; reconciliar os adendos 57/58 antes de eventual integração. B/C e V1 não concluídas.


**Repositório autoritativo para mudanças futuras:** `atshimura/Piano-livre-aberto`. Não fazer novos commits em `atshimura/piano-livre` como parte do fluxo aberto; aquele repositório histórico permanece privado.

## Implementação em revisão — análise automática

Branch `feat/analise-estudo-adaptavel`, sem merge: consultar [59](59-ESTUDO-ADAPTAVEL.md), critérios experimentais, testes do SHA e limitações. Main permanece 0.9.47; a branch propõe 0.9.48/code57. Não alterar os PRs #46/#48 nem declarar aceite físico ou V1.

## Ponto de retomada vigente

**Etapa A — OSMD padrão CONCLUÍDA (PR #49 integrado na main em 21/10/2026):** o Estudo abre OSMD automaticamente; `OSMD · nova` / `Anterior · temporal` alternam sem alterar posição musical ou dados. Build assinado: [Actions 39531053142](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39531053142); testes Android, inclusive atualização do APK público code55 para code56 com dados sintéticos preservados: [Actions 39531053402](https://github.com/atshimura/Piano-livre-aberto/actions/runs/39531053402). Pré-release: [v0.9.47-open-beta](https://github.com/atshimura/Piano-livre-aberto/releases/tag/v0.9.47-open-beta), APK [PianoLivre-0.9.47-open-beta.apk](https://github.com/atshimura/Piano-livre-aberto/releases/download/v0.9.47-open-beta/PianoLivre-0.9.47-open-beta.apk). Pacote `br.org.pianolivre.opensource`, versionCode 56, certificado SHA-256 `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`. Não iniciar a antiga Tarefa 07; não declarar 1.0 nem aceite físico. PR #46 permanece separado e não foi integrado em bloco.

Versão entregue: `0.9.47-open-beta`, `versionCode 56`, commit integrado `535e8ea94d674a886381a5312d71839274ce358b`, a partir da `main` pública `15cafe3c8da7c67b9752d14ba0fb6f8f6141177d`. Pacote `br.org.pianolivre.opensource`. `aapt` confirmou pacote/versão e `apksigner verify` confirmou a fingerprint permanente documentada em `docs/ASSINATURA.md`; não copiar nem expor os segredos.

A confirmação automática de CI e a prova no emulador não substituem testes físicos no Realme 7 5G, Lenovo Tab M9 ou Yamaha P-45. Após a avaliação do usuário, reorganizar as próximas tarefas; não declarar as antigas tarefas 07–20 concluídas.


Etapas 1–5 encerradas no escopo técnico, sem aceite físico. Etapa 6 consolidou perfis/biblioteca/histórico, importação/backup e distribuição; consulte documentos 40–44. A versão está em gradle.properties e o registro final de commits/runs/Release nas issues #4/#11.

Etapa A integrada e validada. O teste `PUBLIC_APK_UPDATE_PRESERVED_DATA_PASS` instalou a release pública anterior code55 e atualizou sobre ela para code56 no emulador API29; perfil sintético ID7, teclado, andamento a 80% e partitura importada foram preservados com o certificado igual. Após a avaliação do usuário, reorganizar o roadmap. O roteiro físico de Realme/Lenovo/Yamaha continua pendente e necessário para aceite em hardware; não declarar 1.0 sem esse aceite.

As seções por versão abaixo são histórico de execução: pendências nelas descritas podem ter sido resolvidas em versões posteriores. O estado verificado no GitHub e este ponto de retomada prevalecem.

1. Verifique a `main`, a versão em `PianoLivreAndroid/gradle.properties`, PRs abertos e os últimos runs do GitHub Actions.
2. Leia `AGENTS.md`, `docs/01`, `docs/07`, `docs/08` e o arquivo pertinente à alteração. Documentos marcados históricos não anulam o estado verificado no código atual.
3. Crie uma branch para cada correção; inclua testes compatíveis e atualize os documentos afetados. Use PR. Não realize rebase de histórico privado, nem importe `signing/`.
4. Rode `python3 PianoLivreAndroid/scripts/verify_public_snapshot.py` e as regressões adequadas. Valide CI do **próprio commit** antes de considerar a alteração aprovada.
5. Ao modificar o aplicativo distribuído, aumente versionCode e versionName efetivos, mantendo o appId público. Pacote público e privado são instalações independentes; backup e restauração exigem prova no dispositivo.
6. Cada partitura precisa de licença/justificativa documental e registro no manifesto `score-rights.json`; não presumir que uma transcrição própria afasta direitos da composição.
7. Se faltar teste no Yamaha P-45, Lenovo ou Realme, declare expressamente. Emulador não mede latência USB-MIDI.

**Assinatura:** segredos cadastrados pelo mantenedor, apenas em workflows de push confiável. Não divulgar JKS, senhas ou artefatos de assinaturas antigas. Ver [assinatura](ASSINATURA.md).

Relatório de cada intervenção: objetivo, HEAD e PR/commit, alterações, testes com links e conclusões, limitações, riscos e próxima etapa. O guia visual do teclado reproduz posições e duração **visualmente**; demonstração sonora é função de saída MIDI, não de sintetizador embutido.

## Retomada do desenvolvimento por etapas

Consultar [28 — Etapas até V1](28-ETAPAS-V1.md) e a issue #4 antes de escolher a próxima tarefa. Registrar o estado do PR e os Actions do próprio commit. Etapa 1 iniciada na 0.9.13; não marcar as etapas completas apenas por existir implementação parcial.

## Retomada 0.9.14

A integração e a assinatura da 0.9.13 foram confirmadas; não repetir o PR #5. A segunda entrega elimina callbacks visuais ociosos em pausa/fim e amplia TemporalRenderingTest. Consulte a seção final do documento 28 e a issue #6 para os Actions do commit efetivo. Próxima implementação: custo de updateInfo no ticker; depois carga longa e lifecycle. A Etapa 1 ainda não foi encerrada.

## Continuação 0.9.15

Implementado cache de apresentação StudyInfoText; ticker musical mantém frequência original, rótulo usa 100 ms durante movimento e atualizações imediatas nos comandos/término. Conferir PR e CI da entrega na issue #6. Próxima implementação: diagnóstico de carga longa/frames e lifecycle; depois reavaliar critérios de fechamento da Etapa 1.

## Continuação 0.9.16

Diagnóstico local e suspensão/retomada de ticker/WebViews implementados; procedimento no documento 29. Confirmar CI/PR e assinatura na issue #6. Depois concluir critérios técnicos automatizados da Etapa 1, mantendo aceite físico pendente, e iniciar Etapa 2 (#8): fixtures de diferenças de interpretação Java/JS. Não encerrar execução apenas por terminar uma versão; continuar enquanto houver trabalho independente e o ambiente permitir.

## Continuação 0.9.17

Fixtures e correções de decimais/vozes em desenvolvimento (documento 30). Confirmar PR #14 de lifecycle e CI da entrega musical antes de avançar. Depois: ligaduras incompletas e fixtures de percurso. Não confundir cobertura musical automatizada com certificação editorial ou Yamaha físico.

## Continuação 0.9.18

Tratamento explícito de ligadura incompleta: novo ataque com aviso, fonte preservada. Fixtures também consultam Score.at (contrato do Espera). Validar dependências/CI e depois auditar percurso e limitações MusicXML conforme documento 30. Persistir em execução entre entregas; não aguardar hardware quando houver tarefas independentes.

## Continuação 0.9.19

Etapa 1 encerrada apenas no escopo técnico (#6); aceite físico em #11. Etapa 2 amplia sound/offset com sete fixtures. Acompanhar PRs #15/#16 e a entrega sound em sequência; validar CI/assinatura do commit efetivo. Depois auditar expressão explícita e limites para avançar à Etapa 3. Não parar por concluir uma versão.

## Continuação 0.9.20

Dinâmica numérica explícita implementada; ver documento 30. Confirmar PRs/CI e assinatura de cada push. Em seguida reavaliar fechamento técnico da Etapa 2 e continuar Etapa 3 com guia visual de mãos/dinâmica/duração, preservando regras de aprendizagem. Validações físicas permanecem na issue #11.

## Continuação 0.9.21

Guia expressivo preparado com amarelo/verde preservados; documento 31. Confirmar PR #18 de dinâmica e CI da entrega de guia. Próxima tarefa da Etapa 3: modos/troca de mão/desconexão/notas presas, sem alterar regras pedagógicas. Depois avançar biblioteca/perfis/progresso. Não encerrar a execução somente por concluir essas entregas.

## Continuação 0.9.22

Etapas 1/2 encerradas no escopo técnico (#6/#8), sem aceite físico. Guia integrado no PR #19. Correção de sessão MIDI/foreground preparada; confirmar MIDI_SESSION_SELFTEST_PASS e CI do próprio commit antes de integrar. Próxima implementação: CC120/121/123 e notas presas (documento 32), depois biblioteca/perfis/progresso. Não interromper automaticamente após a versão.

## Continuação 0.9.23

Controles MIDI de canal implementados e testados em JVM. Confirmar PR de sessão MIDI e PR de CC120/121/123 com MIDI_SESSION_SELFTEST_PASS; conferir assinatura permanente e CI dos commits. Próxima implementação independente: biblioteca/perfis/progresso da Etapa 4, com compatibilidade dos IDs existentes e backup.

## Continuação 0.9.24

Perfis estáveis preparados; confirmar PROFILES_SELFTEST_PASS, UI e backup nos Actions do commit. Etapas 1–3 encerradas apenas tecnicamente; aceite físico em #11. Próxima implementação: busca/favoritos/coleções e histórico/progresso por perfil, preservando IDs e backup. Não encerrar porque uma versão ficou pronta.

## Continuação 0.9.25

Busca/favoritos/coleções preparados; documento 34. Confirmar CI dos perfis (#22) e da biblioteca, incluindo PROFILES_SELFTEST_PASS/LIBRARY_SELFTEST_PASS e UI. Próxima implementação: histórico/progresso por ID, depois Etapa 5 com importação não destrutiva/compatibilidade/MIDI. Continuar após cada entrega enquanto o ambiente permitir.

## Continuação 0.9.26

Histórico e metas por perfil implementados; detalhes e limites no documento 35. Confirmar StudyClockTest, HISTORY_SELFTEST_PASS e HISTORY_ACTIVITY_SELFTEST_PASS no commit do PR antes de integrar. Próxima ação independente: auditoria e correções de importação não destrutiva da Etapa 5; não encerrar ao concluir uma versão.

## Continuação 0.9.27

Importação não destrutiva e limites XML/MXL preparados; documento 36. Teste local ScoreImportTest passou. Confirmar CI e emulador do próprio commit, integrar após PR #24. Próxima tarefa: importação MIDI limitada com fonte preservada e escolha explícita de mãos; não encerrar por concluir esta versão.

## Continuação 0.9.28

Importação SMF 0/1 preparada com mãos explícitas por trilha, fonte original preservada e notação aproximada documentada em 37. Testes JVM locais passaram. Confirmar CI própria, MIDIFILE_SELFTEST_PASS e PRs #24/#25 antes de integrar em sequência. Próximas tarefas: seletor no emulador, remapeamento de caminhos restaurados e compatibilidade/adversariais de backup; ainda não concluir Etapa 5 nem V1.

## Continuação 0.9.29

PR #24 integrado após build/emulador aprovados; Etapa 4 encerrada tecnicamente na issue #9. PRs #25/#26 em validação. Caminhos portáteis e pré-validação de tipos/profundidade JSON preparados; documento 38. Teste JVM de compatibilidade passou; confirmar CI própria e UI do seletor MIDI/histórico. Próxima tarefa: integrar sequência aprovada, tratar MusicXML incompatível explicitamente e avançar consolidação da Etapa 6 sem declarar aceite físico.

## Continuação 0.9.30

Limites musicais passam a ser explícitos; -1.0 preservado como semitom integral. Preflight também protege container MXL. Documento 39. Regressões locais JVM/JS passaram; confirmar CI da sequência #25–#27 e desta entrega antes de integrar. Próxima tarefa: resolver emuladores pendentes e consolidar Etapa 5/Etapa 6; sem aceite físico em #11 e sem declarar V1 pronta.

## Continuação 0.9.31

PR #27 integrado em 36eedce5dc3be9d4b664312eb8f0600222844993 após build 38985550165/emulador 38985550170 aprovados. UI MIDI e histórico passaram; backup Android 28 verificações. Preparados ensaio de atualização desde a 0.9.25 e publicação durável da beta com checks do mesmo commit/assinatura permanente obrigatória. Documento 40. Confirmar CI própria, integrar sequência e verificar assinatura/pré-release; não declarar V1 sem aceite físico (#11).

## Continuação 0.9.32

Correção adicional de desempenho do guia: árvore de intervalos evita varrer notas encerradas quando há baixo muito longo. Documento 41. Teste de 65.537 eventos passou 618 verificações e limite de trabalho <140 nós/consulta; regressões do guia/expressão/timeline passaram. Confirmar CI desta entrega e PR #29, integrar sequência sob proteção e validar assinatura/pré-release; hardware segue em #11, sem V1 concluída.

## Continuação 0.9.33

Codificação XML corrigida e aplicada ao container antes da validação: UTF-8/UTF-16/Latin-1 sem substituição silenciosa; fonte original preservada. Documento 42, ScoreImportTest local passou; exigir XML_INPUT_SELFTEST_PASS Android e CI própria antes de integrar. Confirmar sequência #29/#30 e assinatura/pré-releases. Próxima ação concreta: resolver qualquer falha da atualização no emulador e concluir consolidação técnica, mantendo aceite físico pendente em #11.

## Continuação 0.9.34

Compatibilidade de nomes de importações antigas corrigida sem renomear arquivos. Documento 43; SessionCompatibilityTest local passou 20 verificações; exigir backup Android ampliado. Pré-release durável 0.9.31 confirmada, atualização offline no emulador aprovada; PR #30 integrado e #31 aprovado. Próxima ação: integrar sequência aprovada, verificar APK/assinatura/pré-release do merge e registrar em #11/#4; hardware permanece pendente e V1 não concluída.

A mesma entrega 0.9.34 prepara backup antes de abrir o destino externo, prevenindo truncamento em falha de preparação. Teste Android verifica abertura zero em falha e exportação válida completa; confirmar CI do HEAD atualizado de #32 antes de integrar.

## Continuação 0.9.35

PR #32 integrado em 175e9e9c0ce5e6acdbc6606318b9f44e73501cad com CI aprovada, backup Android 36 verificações. Preparada distribuição release não depurável, pacote aberto e mesmo certificado; emulador exige atualização debug→release preservando perfil/teclado/velocidade e ignorando flags de teste. Documento 44. Confirmar CI própria, integrar e verificar assinatura/pré-release do merge. Próxima ação após consolidação: aceite físico em #11/#4, sem declarar V1 por CI.

## Nova linha de produto: Laboratório Musical (17/10/2026)

A issue [#34](https://github.com/atshimura/Piano-livre-aberto/issues/34) concentra as melhorias aprovadas e o novo Laboratório; a especificação autoritativa, distinção implementado/pendente e próximos passos estão em [doc 45](45-LABORATORIO-ESCOPO-E-CONTINUIDADE.md). Primeira entrega em branch `feat/laboratorio-musical-primeira-entrega` (versão proposta code45). Conferir PR/Actions do SHA exato antes de declarar integrada. Não declarar V1: aceite físico continua em #11.

## Entrega incremental: biblioteca compacta

Branch de trabalho: `feat/biblioteca-grade-compacta`; documento [46](46-BIBLIOTECA-GRADE-COMPACTA.md). A branch foi iniciada da primeira entrega do Laboratório (`feat/laboratorio-musical-primeira-entrega`) e só deve entrar na `main` quando a base anterior (#35) estiver integrada com todos os checks aprovados. Alterações propostas: layout, responsividade e densidade da grade; dados, backups, MIDI e arquivos importados não se alteram. Confirmar o SHA exato do novo PR e seus Actions antes do merge; seguir com #34 e #11. Versão proposta 0.9.37-open-beta/code46.

## Entrega incremental: etiqueta de notas sobre a partitura

Branch `feat/partitura-nome-nota-ao-toque`; consultar [47](47-NOME-DA-NOTA-TOQUE.md). Branch criada a partir da segunda entrega, já integrada no PR #36; abrir PR para `main`, conferir no **mesmo SHA** todos os testes e o emulador antes de integrar. A funcionalidade é apenas do estudo temporal, lê a grafia real das notas e cifra explícita MusicXML quando disponível, não inventa acorde. Próximas tarefas seguem em #34 e #11. Versão proposta `0.9.38-open-beta` / code47.

## Entrega incremental: análise de sequências do Laboratório

Branch `feat/lab-analise-sequencias`, construída da entrega de tooltip (#37). Consulte [doc 48](48-LAB-SEQUENCIAS.md), a issue #34 e o documento 45. Implementa apenas análise **delimitada por Iniciar/Concluir** de padrões completos de uma oitava no Descobrir, por ataques USB-MIDI/virtual Tocar. Automaticidade espontânea, mão/dedos, áudio, progresso e práticas de sequências permanecem pendentes. Verificar CI e emulador do SHA do PR antes de merge; V1 só após #11. Versão proposta 0.9.39-open-beta/code48.

## Retomada 0.9.40 — cursor visual

A alteração inicial da branch `fix/espera-cursor-antes-notas` foi reaproveitada sobre a `main` no PR seguinte (verificar CI antes de integrar). O cursor fixo precede as notas por distância limitada, sem alterar o tempo musical. Documento 49. Próxima tarefa: preparação visual gradual do Espera comandada por Iniciar com duração configurável e sem penalidade; depois notação e funcionalidades do Laboratório da issue #34.

## Retomada 0.9.41 — Iniciar e preparação do Espera

Implementação proposta na branch `feat/espera-iniciar-preparacao` sobre o cursor do PR #39. Ver documento 50: cinco segundos iniciais configuráveis por perfil, bloqueio de reconhecimento apenas durante a entrada, animação desacoplada do tempo musical, cancelamento seguro. Exigir CI (JVM/JS/APK) e emulador no mesmo head antes de integrar. Seguir para claves/hastes/vigas na notação temporal, depois catálogo/demonstração/prática/histórico do Laboratório (#34). Hardware físico em #11.

## Retomada 0.9.42 — vigas MusicXML ausentes

Função `inferConventionalBeams` introduz agrupamento conservador por compasso/voz/pulso quando faltam marcas de vigas explícitas. Documento 51, testes JS. Confirmar CI/Android da branch `feat/notacao-vigas-inferidas` e dos PRs anteriores; depois aprimorar claves/hastes e seguir Laboratório (catálogo, análise espontânea, demonstração, prática, histórico) da issue #34.

## Retomada 0.9.43 — reconhecimento livre

`MusicPatternTracker` analisa sufixos monofônicos completos sem início/fim manual. Documento 52, PR empilhado `feat/laboratorio-reconhecimento-livre`. Validar Actions na HEAD correta após integrar PRs dependentes. Próximos passos: catálogo pesquisável de notas, acordes, escalas e arpejos, demonstração/ritmo offline e histórico por perfil. Hardware pendente #11.

### Revisão corretiva do PR #42 (0.9.43)

Conferir a revisão de `MusicPatternTracker` e `MusicLabScreen` na branch `feat/laboratorio-reconhecimento-livre`: a verificação de monofonia considera MIDI e teclado virtual em conjunto; status sem correspondência não deve preservar resultado anterior; reentrar no Laboratório ou alternar Tocar/Montar reinicia o rastreio. `MusicPatternTrackerTest` cobre esses casos. Exigir CI de build, MusicXML/JS/MIDI, compilação Android e emulador do **último SHA** após as correções; merge protegido e assinatura/release só após sucesso dos checks. Atualizar a issue #34 com evidências. Não integrar o PR #43 nesta tarefa: sua branch é empilhada e deverá ser rebaseada/retargeteada para `main` após o #42. Nenhum ensaio físico foi feito nesta revisão (#11).

## Retomada 0.9.44 — catálogo pesquisável (PR #43)

Entrega proposta na branch `feat/laboratorio-catalogo-consulta`, reconciliada por merge não destrutivo com a `main` 0.9.43. Mantém reconhecimento automático USB-MIDI/teclado virtual, prevenção de sobreposição e resultados obsoletos, botão manual Iniciar/Concluir e reinício Tocar/Montar/reencontro do Laboratório. `MusicLabCatalog` oferece consulta offline de 12 classes cromáticas, notas, 13 intervalos, acordes/inversões, escalas/modos, arpejos e os cinco exercícios antigos. Busca tolera acentos e sustenido `#`/`♯`; resultados vazios desabilitam prática e a alteração da busca reinicia exercício.

**Não confundir consulta com avaliação:** a prática apenas compara conjuntos de classes de altura, sem ordem, quantidade de notas, duração, ritmo, pedal, oitava, dedilhado ou mão. Grafia enarmônica não é inferida da tonalidade. Teste `MusicLabCatalogTest` ampliado e smoke UI do emulador cobrem busca e reset, além da regressão `MusicPatternTrackerTest`. Conferir ações no SHA final do PR e novamente no merge da `main`, certificado permanente e pré-release `0.9.44-open-beta` / code53. #44 demonstração MIDI, #45 prática sequencial e hardware físico #11 permanecem futuros; **não incluir antecipadamente**.

## Retomada 0.9.45 — demonstração musical offline (PR #44)

Branch `feat/laboratorio-demonstracao-midi` reconciliada com `main` 0.9.44 sem reescrever commits. Demonstração do catálogo pela saída USB-MIDI existente: nota/acorde/intervalo juntos, escalas/arpejos em sequência, velocidade 50–150% e repetição opcional, sem sintetizador Android. `MusicLabPlaybackPlan.Session` distingue reprodução ativa de notas sustentadas, invalida callbacks após parada (inclusive entre notas e repetições), troca de busca/exercício/modo, saída para Biblioteca/Estudo, segundo plano e desconexão. Libera notas ligadas **antes** do fechamento da porta USB. Tests JVM cobrem pausas entre notas, sustentações, repetição, mudança e callbacks antigos. A perda física da porta pode impedir Note Off final; não afirmar ausência de notas presas em piano real sem ensaio. Confirmar checks do PR/merge, release assinada `0.9.45-open-beta` / code54, e atualizar issue #34. PR #45 permanece intocado para próxima tarefa; Yamaha P-45, Realme 7 5G e Lenovo Tab M9 aguardam #11.

## Retomada 0.9.46 — prática sequencial (PR #45)

Branch `feat/laboratorio-pratica-sequencial` reconciliada por merge não destrutivo com a `main` 0.9.45. Preservados reconhecimento espontâneo/manual, catálogo, USB-MIDI/virtual e demonstração MIDI/cancelamento do PR #44. Escalas e arpejos avançam por ataque **novo, isolado** na ordem da altura MIDI; extras isoladas são indicadas sem bloquear e demora não penaliza. Notas repetidas exigem novo ataque; ataques simultâneos/sobrepostos e mistura com notas já mantidas no MIDI/virtual não fabricam ordenação. Reprodução de demonstração nunca avança prática. Busca, troca de exercício/modo, saída/pausa/desconexão cancelam estado; mão Ambas/Direita/Esquerda é referência, não inferência. Acordes/intervalos preservam a comparação por classes cromáticas. Sem migração de dados, serviços ou telemetria. Ver [55](55-LAB-PRATICA-SEQUENCIAL.md), teste JVM e emulador. Exigir CI no SHA da branch/merge e release assinada 0.9.46/code55. Faltam dedilhado, métricas rítmicas/metrônomo, calibração, histórico do Lab e testes físicos #11. Não declarar V1.

## Checkpoint — análise automática em PR #51

Código `e1fac0ec`, branch `feat/analise-estudo-adaptavel`: build/JVM/JS/XML/MIDI/corpus aprovados no run 39676817138. Documentos 57–59 e métricas em `docs/evidence/study-profile/summary.json`. Roteiro Android da HEAD acompanhado no PR; a correção posterior é do helper de toque do novo teste. Conferir resultado do SHA exato antes de integrar. Sem merge, release, aceite físico ou V1.

### Retomada B1 — legibilidade adaptável no Estudo (22/10/2026)

Continuação no PR #51, ainda aberto, baseada na `main` `801543c`; #46 e #48 seguem intocados. O Chromium do head `d11a85845d7fa7d40f0fd7495a800502d62e7354` passou a regressão de 27 arquivos. Gymnopédie 738×196 melhorou 4,77976→5,07480 CSS px (+6,17%), com overflow vertical zero e alerta de pauta pequena ainda ativo. Canon foi validada pelo OSMD SVG, cifras preservadas. Em Für Elise, colisão do `p` de dinâmica com G♯3 (compasso 13, beat 36) foi confirmada em 3,15 px e corrigida movendo a anotação 7,15 px para cima só na apresentação do Estudo. Quatro fixtures incompletas seguem em fallback temporal explícito e conferido.

O artefato durável e as capturas estão em `docs/evidence/study-profile/b1/`; detalhes atualizados em docs 57/58/59. Build completo e emulador devem ser registrados somente após conclusão para o head final que também inclua este adendo documental. Nenhum ensaio físico: aceite editorial e issue #11 continuam pendentes; não integrar, publicar APK, concluir B/C ou declarar V1.

## Auditoria Entrega B — 25/10/2026

`main` conferida em `801543cc8c26d2c1c216cc7a43b01211dc07a4ad`, `0.9.47-open-beta`. Falha do roteiro Android #55 confirmada pela evidência do Actions: a jornada procura Estudo livre 01 enquanto `uiautomator` mostra somente Upgrade A/B. A investigação atribui a causa provável a pop-up/estado transitório de UI reutilizado entre roteiros na mesma sessão de emulador; o upgrade sintético não altera `catalog.json` e os filtros da Biblioteca são estado da tela. Isso não prova que a navegação real está livre de defeito: a jornada corrigida precisa demonstrá-lo.

Criada branch experimental `ci/entrega-b-integral-20261025` a partir da prova #57, sem alterar #56/#57/#61 nem #46/#47/#48/#51. O roteiro curto/longo reinicia somente a Activity no emulador, preserva SharedPreferences e arquivos, normaliza a navegação para Biblioteca, verifica o catálogo empacotado e distingue falhas de filtro/coleção, navegação, carga e item ausente. `python3 -m py_compile tests/emulator_study_session.py` passou. Esta máquina não tem `adb`, Gradle ou Android SDK; ainda não há Actions da branch nova nem prova B1+B final. #52/#58, #54/#59 e #53/#60 continuam exigindo reconciliação de código, não apenas CI individual. Ver [doc 65](65-ENTREGA-B-AUDITORIA-E-RECONCILIACAO.md).

**Continuação da auditoria B — resultado da primeira prova composta:** build/Chromium/JVM passou no Actions 40209082130; UI Android no Actions 40209082077 chegou às jornadas longas, mas falhou antes de abrir o primeiro exercício. A causa exata foi o localizador do teste escolher o primeiro `android.widget.Spinner`, que é o seletor de perfil (“Upgrade A/B”), em vez do filtro “Todas” na mesma linha do campo de busca. Corrigido o roteiro para localizar o filtro pela posição relativa ao campo de busca, verificar que o menu aberto contém “Todas” e falhar com diagnóstico específico se não. Adicionada fixture de regressão local `LIBRARY_FILTER_LOCATOR_PASS`; `py_compile` também passou. O código do aplicativo não foi modificado por este defeito de teste. A próxima execução de CI precisa validar a jornada no emulador e o build do SHA final; ainda não declarar conclusão B. Detalhes e evidências em [doc 65](65-ENTREGA-B-AUDITORIA-E-RECONCILIACAO.md).

Para completar a evidência do subsistema de identificação, acrescentei `TemporalNoteTipBrowserTest.js`: clique real no elemento `<ellipse>` do renderizador temporal em Chromium, conferência de Mi4 · E4, limites do viewport, não sobreposição, expiração e screenshot no artifact de medidas. O Android já prova o clique OSMD por `adb`; este browser test complementa a jornada. Execução e artifact aguardam CI no próximo HEAD. O filtro corrigido e este teste são mudanças de roteiro/prova, sem alteração dos arquivos MusicXML.

**Continuação da jornada Android:** no HEAD `349a7fd`, build/Chromium/JVM passou (15.127 asserções de sessão, rótulo temporal Chromium aprovado), mas o emulador 40210751936 parou no verificador do catálogo. A hierarquia `window.xml` da captura mostra o filtro `Todas`, `10 de 10 partituras` e `Estudo livre 01` na oitava posição, fora do primeiro viewport. O parser antigo procurava a substring `0 de ` e confundiu `10 de 10` com resultado zero. Corrigido para interpretar inteiros com correspondência total; a jornada agora pesquisa o título pela busca nativa, limpa a consulta anterior entre exercício curto e longo e diferencia contagem zero de card fora da dobra. Fixture local testa `10 de 10` e `0 de 10`. A biblioteca e a navegação do app funcionaram no caso registrado; a jornada final curta/longa ainda não passou e deve ser reexecutada no próximo SHA antes da conclusão B.

**Continuação da jornada Android:** o run `40212363484` no HEAD `e8d9ada` passou build/Chromium/JVM e repetiu a falha na verificação de filtro. A hierarquia expõe `text=''` para ambos os Spinners, enquanto a captura `piano-library.png` mostra visualmente perfil no cabeçalho e `Todas` no filtro. Corrigi o roteiro para abrir o Spinner identificado pela linha do campo de busca, escolher explicitamente `Todas`, limpar a busca, validar o total do catálogo e então pesquisar o exercício. A regressão local também cobre Spinner sem texto acessível. Não há defeito funcional da biblioteca reproduzido até agora; a jornada final curta/longa continua pendente de CI no novo SHA.


### Atualização da investigação da jornada Android — 2026-10-25

No HEAD `04c2bd9bad68e795b48903c35f5a732979a79f1d`, o emulador chegou à biblioteca com o filtro explicitamente em `Todas`, mas a busca encontrou zero porque o roteiro enviou `Estudo livre%01`: o comando `adb shell input text` usa `%s` para representar espaço, e a query codificava somente o primeiro espaço. O catálogo empacotado continha o exercício e o filtro estava correto; a causa reproduzida foi a codificação do texto de entrada no teste, não uma falha de biblioteca ou de perfil. O roteiro agora converte todos os espaços via `adb_search_text`, e há regressão para a string exata `Estudo%slivre%s01`. A jornada no emulador precisa passar no SHA final atualizado antes de concluir a validação técnica. Run que reproduziu: https://github.com/atshimura/Piano-livre-aberto/actions/runs/40213805638 .


### Atualização da jornada — resultado do SHA 5244184

O build/Chromium/JVM passou no SHA `52441848911a23fc430af18e773d56237a8357a3` (run https://github.com/atshimura/Piano-livre-aberto/actions/runs/40215362948). O emulador completou as verificações anteriores de UI e renderização responsiva, mas falhou na jornada quando, após abrir o filtro e tocar `Todas`, a hierarquia imediata não expôs o Spinner da linha de busca (`FILTER_CONTROL_MISSING`; run https://github.com/atshimura/Piano-livre-aberto/actions/runs/40215362946). Isso ainda não prova defeito da biblioteca: a jornada não esperou a dispensa do popup do Spinner. O roteiro agora aguarda o retorno da busca, fecha somente um popup identificado e guarda XML/captura se o controle continuar ausente. A validação curta/longa permanece pendente no próximo SHA.


### Causa confirmada de estado residual da busca — execução do SHA 9e0cb5a

O log do run Android https://github.com/atshimura/Piano-livre-aberto/actions/runs/40216924861 mostrou a biblioteca aberta, `Todas` visível e contador `1 de 10 partituras`; o localizador falhou porque o campo de busca mantinha texto de um cenário anterior, fazendo o Android ocultar o hint `Buscar título`. Isso confirma estado de consulta residual entre cenários no mesmo emulador; não foi evidenciada perda de partitura nem corrupção de coleção. O roteiro passou a localizar o campo pelo tipo e posição, limpar somente a consulta visual, preservar dados de perfil/arquivos/coleções e exigir que o contador cubra ao menos todas as partituras empacotadas. Fixtures agora cobrem EditText preenchido sem hint. O build do mesmo SHA passou (https://github.com/atshimura/Piano-livre-aberto/actions/runs/40216924903); a jornada curta/longa ainda precisa ser repetida no SHA seguinte.


### Roteiro de abertura da sessão — execução do SHA dac396f

A jornada passou pela busca isolada: encontrou `Estudo livre 01`, após limpar a consulta de tela e validar o catálogo. O run Android https://github.com/atshimura/Piano-livre-aberto/actions/runs/40218278046 falhou no passo seguinte: o teste tocou o rótulo do título e não recebeu `POC_OSMD_READY` em 25 s. O log não demonstra se a tela permaneceu na biblioteca ou se abriu sem renderização; portanto isso segue como navegação/renderização não resolvida, não como causa já atribuída. O roteiro foi ajustado para tocar o botão visível `Estudar` e salvar screenshot/labels se a prontidão não chegar. Build/JVM/Chromium passou (https://github.com/atshimura/Piano-livre-aberto/actions/runs/40218278030); a jornada curta/longa segue pendente.


### Reprodução em início determinístico — execução do SHA df16327

A abertura explícita pelo botão `Estudar` funcionou e o OSMD desenhou as duas pautas; o workflow falhou depois, ao procurar `Pausar`. As labels mostravam posição `16.0`, fim do exercício curto de 16 tempos, e `Iniciar`/ `Parar`: a execução residual anterior havia deixado o playhead no final, fazendo o start terminar imediatamente. Run: https://github.com/atshimura/Piano-livre-aberto/actions/runs/40219775224 . O roteiro agora aciona `Início` na UI e exige que a posição mostrada volte a `0.0` antes de iniciar. Esse estado de navegação/playhead é reiniciado sem limpar dados do perfil ou da biblioteca.


### Tempo suficiente para testar pausa no exercício curto — execução do SHA 773026d

O emulador confirmou que o botão `Início` levou a posição a zero, mas o playback alcançou o fim em `16.0` antes de a consulta seguinte à hierarquia encontrar `Pausar` (run https://github.com/atshimura/Piano-livre-aberto/actions/runs/40221311869). O OSMD carregou e a reprodução começou; o log marca `play=true mode=1`. Para evitar que o overhead de UI do emulador consuma o fixture curto de 16 tempos, o roteiro agora move o SeekBar de andamento até `25%`, confirma o rótulo visível e então testa pausa/retomada. Isso exercita o ajuste real de andamento e mantém a asserção de pausa.


### Toque durante playback sem esperar a árvore UI — execução do SHA 36cff100

O emulador confirmou o SeekBar em `25%`, mas duas capturas de hierarquia falharam no `adb pull` enquanto a música tocava; juntas consumiram cerca de 38 s, tempo suficiente para o exercício atingir `16.0` e encerrar (run https://github.com/atshimura/Piano-livre-aberto/actions/runs/40222887035). Não foi um timeout de reprodução do app: o log mostra início da execução e o fim normal antes de aparecer o botão de pausa. O teste agora captura previamente as coordenadas do controle visível e envia toques reais `adb input tap` para iniciar, pausar e retomar, verificando visualmente `Iniciar` quando pausado. A árvore não é consultada enquanto o relógio corre.


### Aviso MIDI sem saída conectada — execução do SHA 569bda5

No run Android https://github.com/atshimura/Piano-livre-aberto/actions/runs/40224464175, a jornada passou por posição inicial, andamento, renderizadores e retomada, mas ao entrar em Demonstração o app exibiu o aviso correto de ausência de saída MIDI. O roteiro não fechou o diálogo e então procurou o controle `Espera`, falhando porque só estavam acessíveis o aviso e `OK`. A regressão agora confirma o aviso, compara o contador `play=true mode=2` antes/depois para garantir que não iniciou saída MIDI inexistente e fecha com `OK` antes de continuar. O CI não possui porta Yamaha física; execução de saída MIDI é coberta por testes sintéticos da suíte JVM.


### Pausa/retomada e momento do aviso MIDI — execução do SHA 252de2f

O run Android https://github.com/atshimura/Piano-livre-aberto/actions/runs/40226052690 passou por biblioteca, busca, OSMD, mãos, teclado guia, retorno a `0.0`, andamento `25%`, pausa e retomada usando toques ADB. Falhou ao esperar o aviso MIDI imediatamente após selecionar Demonstração. A repetição anterior mostrou que o próprio app abre esse aviso somente quando o usuário toca `Iniciar` sem saída conectada; a verificação foi reposicionada para executar esse gesto, confirmar o aviso e ausência de `play=true mode=2`, fechar `OK` e prosseguir.



### Prova final da jornada Android B — 2026-10-25

No SHA de código `ffdda2fcc7081cee363560cb8f79336a5030f2da`, os workflows passaram no mesmo commit: build/JVM/Chromium [run 40227586693](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40227586693) e emulador [run 40227586698](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40227586698). O emulador registrou `STUDY_B_ANDROID_SESSION_PASS` para `Estudo livre 01` e `Gymnopédie nº 1`; em ambas: Espera, Contínuo, Demonstração sem saída física conectada, seleção das três opções de mãos, teclado guia, renderizadores OSMD/temporal/OSMD, início, pausa, retomada, posição inicial e retorno do background. A UI confirmou também o aviso ao tentar iniciar Demonstração sem MIDI; não houve playback fingido. Não houve Yamaha P-45, Realme 7 5G ou Lenovo Tab M9 neste CI.

Build log: `STUDY_SESSION_INTEGRATION_PASS checks=15127`, `STUDY_LOOP_POSITION_PASS assertions=11`, `STUDY_NOTE_TIP_PASS checks=34`, `TEMPORAL_NOTE_TIP_BROWSER_PASS Mi4 · E4 viewport-safe and expires`, `BUILD SUCCESSFUL`. O emulador também passou por OSMD de 78 compassos, contexto fixo, mapeamento de 30 cabeças/eventos em polifonia, responsive baixo/alto/tablet, controles Android e `PUBLIC_ANDROID_EMULATOR_SMOKE_PASS`.

Esta prova não integra PRs nem substitui aceite físico/editorial. A atualização documental subsequente mantém os mesmos arquivos de código; seu SHA precisa reexecutar os dois workflows antes de se declarar prova final do branch.


## Fechamento da validação automatizada B1 + Entrega B — 2026-10-25

Estado final de CI desta prova: SHA `89541ba7f0ffb9f76ca2446320cfc83ba3665fd9`. Build/JVM/Chromium/APK de CI passou em [run 40229478373](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40229478373); emulador passou no mesmo SHA em [run 40229478320](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40229478320). Esse resultado supersede neste documento todas as notas anteriores que diziam que a reexecução documental estava pendente. A branch #62 segue experimental e não deve ser integrada. Main, Release e distribuição não foram alteradas; aceite físico/editorial segue pendente.


## Consolidação revisável B1 + B — 2026-10-25

A implementação revisável está no PR draft #63, baseado no HEAD da B1 #51. A branch #64 é apenas sombra CI com o mesmo SHA de código `9ddbd2749055d6fb95a2911225d50359e03f97a4`. Build/JVM/Chromium: [40231690624](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40231690624), success; emulador: [40231690719](https://github.com/atshimura/Piano-livre-aberto/actions/runs/40231690719), success. A jornada Android passou para Estudo livre 01 e Gymnopédie nº 1. O PR #62 continua prova histórica CI-only, sem merge. Nenhuma integração à main, Release ou distribuição foi feita; aceite físico/editorial continua pendente.

## Laboratório Descobrir 1.0 — implementação em PR próprio

Branch `feat/laboratorio-descobrir-1.0`, iniciada após o commit main `14489deef06d02e85a8bd43ecfc24f53aed22a03`. O botão inicial e a tela pública agora seguem a decisão de 20/10: barra Descobrir/Aprender desabilitado, instrução vazia única, entrada USB-MIDI, pauta nativa e retenção do último resultado. A `MusicLabScreen` antiga permanece no código para os recursos posteriores. `MusicDiscoverySessionTest` e `MusicLabDiscoverySelfTest` foram incluídos; a validação remota e o SHA final serão registrados após as execuções da branch. A beta 0.9.48 existente não é republicada, e esta tarefa não autoriza merge nem Release. Aceite físico Yamaha/Realme/Lenovo permanece pendente.

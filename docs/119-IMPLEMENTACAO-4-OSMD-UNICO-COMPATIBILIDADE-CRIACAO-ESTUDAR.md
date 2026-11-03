# 119 — Implementação 4: OSMD único, compatibilidade Criação → Biblioteca → Estudar, diagnóstico de falhas

**Decisões do mantenedor — 03/11/2026.**
**Estado: INVESTIGAÇÃO E PLANEJAMENTO DOCUMENTAL, NÃO IMPLEMENTADO.**
Branch `docs/impl4-osmd-only-diagnostics-20261103`, baseada na Implementação 3 `feat/creation-rhythm-uniformize-20261103` (PR #105), dependente dos PRs #104, #103 e #102. `main` e publicação não são alteradas.
Objetivo fundamental: **100% das partituras que o Piano Livre declare como geradas com êxito e prontas para estudo devem ser musicalmente compatíveis com o OSMD moderno no modo Estudar**, dentro do conjunto de recursos musicais suportados. Nenhum fallback de desenho antigo é permitido; erros precisam de janela detalhada, diagnóstico local e recuperação explícita.

## 1. Diagnóstico: confirmado versus hipótese

### Confirmado pelo código (inclusive `main`)

1. **Uma partitura gerada não é desviada por sua origem ao motor temporal.** `CreationScorePreviewScreen` converte o MusicXML gerado via `Score.read` e `NotationView.loadScore(score,0,true)`: **OSMD no modo free**. `MainActivity.loadScore("creation-score:<UUID>")` lê o mesmo MusicXML da derivada `.pls`, valida com `Score.read` e passa por `PianoScreens.displayScore`; esse método chama `NotationView.loadScore(score,beat)` e pede inicialmente **OSMD modo engraving-poc**.
2. **Há exibição de pauta antiga mesmo antes de uma falha:** `PianoScreens.displayScore` torna `legacyFallback` (`PianoView`) **visível durante o carregamento** e o oculta só após `onRendered`. Assim, há uma aparência antiga transitória sem troca definitiva do motor.
3. **Há fallback temporal automático real:** `NotationView.onError` registra `AUTO_TEMPORAL_FALLBACK`, troca `engravingProof` para falso e reenvia o MusicXML como layout `study`; `notation.html` interpreta esse layout com `TemporalNotation.render`, não com OSMD. Depois de nova falha, `PianoScreens.onError` exibe `PianoView` esquemática e um Toast. Isso viola a decisão atual.
4. **Criação e Estudar não seguem exatamente o mesmo processamento:** `notation.html` em `engraving-poc` usa `StudyProfile.analyze`, transformações de apresentação, `fitOpticalSvg`, `adaptStudySvg`, `OpticalTimeMap.reconcile`, `StudyProfile.compare` e gestos/âncoras adicionais. Uma exceção em qualquer uma dessas operações provoca erro e fallback. A prévia `free` não passa por todas essas etapas.
5. **Salvar e adicionar à Biblioteca não validam renderização OSMD no Estudar:** `CreationScoreStore.save` revalida correspondência do MusicXML aos dados/ajustes e checksum; `MainActivity.addCreationScoreToLibrary` revalida propriedade/integridade, **não** realiza uma validação de desenho e mapeamento no motor de Estudar. `c2_d_ui_smoke.py` entra em Estudar mas não verifica qual engine desenhou a pauta.
6. **O XML da Criação é um caso musical particular que merece regressão própria:** `CreationScoreTranscriber` escreve MusicXML `score-partwise` com **uma parte P1, duas pautas, diversas vozes, backups, pausas, acordes e ligaduras**. `Score.read` conserva `writtenEvents` para o desenho e `events` para execução (com ligaduras consolidadas). A validação OSMD deve usar as notas **escritas**, não comparar simplesmente com ataques de execução.

### Hipóteses a testar, sem diagnóstico definitivo

- O OSMD pode desenhar a partitura gerada corretamente, mas o modo Estudar rejeitá-la na reconciliação entre `StudyProfile.compare` e `Score.writtenEvents`: especialmente `voiceId`, `staff`, `sourcePart`, `measure`, tempos fragmentados e ligaduras.
- O OSMD pode falhar ao diagramar determinados compassos, acordes, vozes, `backup` ou grande extensão; só um teste de renderização real distingue erro musical de erro geométrico.
- O usuário pode ter visto a pauta `PianoView` durante a carga e não uma falha permanente. A ausência atual de diagnóstico persistente impede afirmar qual caso ocorreu em seu dispositivo.

**Não confirmado:** não foi lida a partitura concreta do aparelho nem seu log de erro. Não afirmar que ela foi gerada com motor de transcrição antigo ou que o OSMD é incapaz de desenhá-la.

## 2. Regras obrigatórias da Implementação 4

### R1 — Um único motor moderno

- **OSMD local (OpenSheetMusicDisplay) é o único motor de notação** de Criação, Partitura Livre e Estudar. Usar a mesma versão empacotada e a mesma interpretação de MusicXML em todas as rotas; diferenças de organização visual (leitura paginada/horizontal, tamanho, controles) não podem alterar notas, claves, acidentes, figuras, vozes, ligaduras e compassos.
- **Eliminar completamente da execução do app** `TemporalNotation.render` e `PianoView` como renderizadores de partitura, incluindo fallback, pré-carregamento, transição de telas, erro, retomada e caminho de testes. Eliminar arquivos e código órfãos após migrar todas as referências. `Score`, `Score.writtenEvents`, relógio MIDI, modelo de estudo, histórico e `CreationScoreTranscriber.Options.legacyTiming` **não são motores antigos de desenho** e **precisam ser preservados** para dados antigos.
- Não simplesmente ocultar seletores antigos: nenhuma chamada Java/JS de produção, asset carregado ou teste de expectativa pode manter fallback de desenho. Não remover sem substituir mecanismos de estudo que dependem do `scoreView` legado (estado de mãos/notas tocadas, avanço, espera, preparação, calibração); migrar estado não visual a objeto próprio e adaptar os chamadores.
- Enquanto não houver SVG OSMD válido, a tela apresenta estado **neutro** “Preparando partitura...”, com qualquer partitura antiga/incompleta invisível. Antes de confirmar pronto, não iniciar contagem de estudo/execução que dependa de pauta que o usuário não consegue ver.

### R2 — Compatibilidade de geração 100% para partituras aceitas

**Contrato de aceitação:** toda geração declarada concluída e oferecida como “Salvar partitura”, “Adicionar à Biblioteca” e “Estudar” deve passar pelo **mesmo contrato musical e pelo mesmo caminho OSMD de Estudar**, não apenas pelo parser `Score.read`.

- **Uma origem musical canônica:** criar representação/validador compartilhado que receba o MusicXML produzido pela Criação, o modelo `Score` e seus `writtenEvents`. Verificar alturas e oitavas (incluindo enarmonia escrita), número/ordem de notas, compassos, staffs, vozes, inícios e durações, acordes, ligaduras, pausas e sincronização das mãos. Comparar segmentos escritos, não ataques consolidados nem pixels arbitrários. Não inferir mão física da pauta.
- **Mesmo material antes e depois de salvar:** o MusicXML validado da prévia deve corresponder exatamente ao salvo em `.pls` e àquele que `MainActivity.loadScore` entrega ao Estudar, sem reinterpretação silenciosa. PLS1 formato 1 e 2, ajustes rítmicos, `Uniformizar trecho`, notas não atribuídas e score da Biblioteca entram no escopo.
- **Pré-validação de compatibilidade:** antes de finalizar a partitura como pronta e antes de vinculá-la à Biblioteca, executar validação determinística de MusicXML e compatibilidade do modelo, com ensaio real de renderização OSMD no ambiente disponível (WebView em execução; Chromium offline em CI). Somente retorno explícito de OSMD pronto + mapa confiável habilita o estado “pronto para Estudar”. Não confundir aprovação de parser JVM com render OSMD ou com sucesso real de dispositivo.
- **Tratamento de geração incompatível:** recusar a promoção para derivada de estudo quando a partitura não puder ser renderizada de forma fiel; explicar qual compasso/voz/figura gerou o bloqueio, permitir corrigir e tentar novamente. Manter a gravação original `.plc`, ajustes editáveis e derivados antigos sem exclusão. Evitar criar um item de Biblioteca que leva a uma tela quebrada.
- **Partituras pré-existentes:** não apagá-las/regravá-las automaticamente. Em abrir, validar compatibilidade atual; se falhar, informar com diagnóstico e oferecer retorno à Criação/correção, preservando a derivada e backups. Corrigir o conversor, o XML ou o caminho Estudar, conforme a causa; não reescrever música validada só para satisfazer um bug de comparação.
- **Domínio suportado:** as notas/recursos que o aplicativo oferece para geração precisam ser cobertos integralmente. Novos recursos somente podem entrar no gerador com testes OSMD correspondentes; recursos não suportados precisam falhar explicitamente *antes* de se anunciar “geração concluída”. “100%” aqui é **zero partitura gerada e aceita incompatível com o OSMD na matriz suportada**, não promessa de ausência de falhas de hardware/WebView/memória em todos os dispositivos.
- **Verificação em uso:** revalidar a finalização do desenho no dispositivo atual, com tratamento de falhas transitórias, mesmo quando uma mesma partitura já passou no corpus CI. Cache de compatibilidade, se existir, deve ter chave da versão do validador/motor e hash do XML; não assumir que um sucesso de versão anterior garante o atual.

### R3 — Em falha, janela detalhada, nunca desenho antigo

O usuário verá um **AlertDialog bloqueante de erro** com, no mínimo: título **“Não foi possível exibir a partitura”**, etapa (leitura MusicXML, preparação OSMD, desenho SVG, correspondência das notas, geometria, WebView/tempo limite), descrição compreensível, código estável `PL-NOT-...`, identificador curto de ocorrência e confirmação explícita de que a gravação/partitura continuam preservadas.

Ações: **Tentar novamente** (nova tentativa explícita; sem ciclo infinito ou troca de motor), **Copiar diagnóstico**, **Voltar à Biblioteca**. Quando apropriado: **Abrir Criação para corrigir** e **Exportar relatório** por arquivo local compartilhado somente por escolha do usuário; oferecer “Detalhes técnicos” recolhidos, com dados limitados/sanitizados. Em falha, suspender o modo de estudo e transportar o estado com segurança sem avançar playhead/progresso falsamente. Falhas na **própria geração/salvamento** exigem a mesma clareza, adaptando o texto ao contexto.

### R4 — Diagnóstico local, privado e recuperável

Planejar `NotationFailure`, `NotationDiagnosticsStore` e ligação Java ↔ JS com evento estruturado:
- `incidentId` curto, `generationId` de cada abertura (para rejeitar callback atrasado e eventos de outra música), data/hora, versão do app e OSMD, família de dispositivo/WebView quando disponível, código/etapa/classe da falha, variante (Criação/Estudar/Partitura Livre), hash SHA-256 do XML **sem conteúdo musical**, tipo da fonte (gerada/importada/interna), contagem de compassos/notas, metrificação de viewport, mensagem técnica e stack saneados e limitados. Campo “voz/compasso” apenas quando realmente identificados pela análise.
- Categorias propostas: `PL-NOT-PARSE` (MusicXML), `PL-NOT-STORAGE` (PLS), `PL-NOT-LOAD` (OSMD), `PL-NOT-RENDER` (SVG), `PL-NOT-MAP` (reconciliação), `PL-NOT-GEOMETRY`, `PL-NOT-WEBVIEW`, `PL-NOT-TIMEOUT` e `PL-NOT-UNKNOWN`; manter mensagem original limitada com sanitização, nunca dado bruto não filtrado.
- Salvar **somente no armazenamento privado do app**, sob limite de tamanho/quantidade, rotação e recuperação segura se houver falha de escrita; sugestão inicial: **50 ocorrências / 256 KiB no total**. Sem rede, telemetria, analytics, SDK externo, credenciais, URLs privadas, caminho de arquivo do usuário, título, nome do compositor, dado pessoal ou XML/MIDI integral no relatório padrão. O relatório opcional para compartilhamento deve permitir revisão humana antes do envio, com **exportação consentida**. Não coletar imagens/screenshot automaticamente.
- Ter tela acessível para **ver/copiar/exportar/apagar histórico local de erros**. Evitar logs repetidos por ticks/resize/loops; erro por geração e falha relevante, com antirreplicação. Distinção entre erro do XML, OSMD, código Android, memória/tempo, com correlação de janela e reprodução.
- Segurança do canal JS/Java: bridge disponível só para `file:///android_asset/`, nenhuma carga externa, apenas mensagens de erro de esquema esperado e com limites. Não exibir HTML/JS bruto em diálogo; texto puro sanitizado.

## 3. Etapas planejadas, sem implementação nesta entrega

**4-A — Reproduzir, auditar e classificar.** Montar fixture de partitura realmente derivada da Criação `PLS1 v1` e `v2`, incluindo uma parte/duas pautas, acordes, notas sobrepostas, mão/voz, ligaduras, pausas, divisão de compasso, regularização, Uniformizar trecho, tempos extremos e alterações; comparar os resultados das rotas OSMD `free` e `engraving-poc`. Instrumentar provisoriamente apenas os testes para saber o local exato do erro; classificar falhas reais vs falso positivo de `StudyProfile.compare`. Registrar hashes/medidas não pessoais e correções necessárias. Uma amostra específica da instalação do usuário poderá aprimorar o diagnóstico, mas **não será exigida** para iniciar a implementação.

**4-B — Unificar contrato musical e corrigir divergências.** Ajustar `CreationScoreTranscriber`, `Score.read`, `StudyProfile.compare`, `EngravingAudit`, `OpticalTimeMap` e funções de desenho, **somente onde o corpus demonstrar divergência**, respeitando a semântica musical; garantir que render horizontal e leitura exibam a mesma escrita, inclusive múltiplas vozes/ligaduras. Distinguir um desenho correto com correspondência ambígua de um desenho errado; não forçar mapeamento físico de mãos.

**4-C — Geração com validação obrigatória para Estudar.** Introduzir `CreationScoreStudyCompatibility` ou equivalente entre revisão/salvamento/vínculo à Biblioteca e a renderização OSMD real. Não confirmar “pronta para Estudar” sem os checks e sem desenho validado; manter a `.plc` e o trabalho em caso de erro. Validação não bloqueante na UI e com limites contra anexos grandes. Acrescentar marcador de versão do contrato apenas se necessário, com migração não destrutiva e backups v4 legíveis.

**4-D — Falhas visíveis e registros locais.** Implementar modelo de erros, ponte de eventos, armazenamento privado limitado, janela de erro, exportação opcional e tentativas explícitas. Resolver erros de WebView não carregada, JS indisponível, SVG vazio, timeout, `onReceivedError`, perda de contexto e callbacks atrasados. Proteger sessão de estudo em pausa.

**4-E — Retirada definitiva dos desenhos antigos.** Remover `TemporalNotation.render` e `temporal-notation.js` do produto, eliminar `PianoView` como fallback e migrar suas dependências não visuais para um estado de estudo, remover flags/layout `study` antigos e testar código Java/JS/Gradle/assets/manifest. Preservar exclusivamente suporte ao **formato de dados** histórico, que não requer manter o renderizador histórico.

**4-F — Atualizar suíte completa, validar e preparar integração.** Adaptar `EngravingProofContractTest.js`, `emulator_study_failure.py`, `StudyProfileBrowserTest.js`, `c2_d_ui_smoke.py`, `TemporalRenderingTest.js`, `TemporalLayoutTest.js`, `emulator_ui.py`, testes de backup/versões e workflows `build.yml`/`emulator.yml`. Não eliminar um teste musical relevante: migrar para assertivas do OSMD ou erro estruturado. Executar **apenas testes básicos nas entregas cujo comando assim exigir**, sem suíte completa ou APK por conta própria. Antes de merge protegido/publicação, obter os gates exigidos por autorização específica. Não confundir evidência de CI com testes físicos Realme 7 5G, Lenovo Tab M9, Yamaha P-45.

## 4. Matriz mínima de aceitação — bloqueadora de entrega

| Cenário | Resultado obrigatório |
| --- | --- |
| Toda derivada gerada dentro do domínio suportado, com regulagens da Implementação 3 | `Score.read` + OSMD **Estudar** bem-sucedidos; 100% de notas/pautas/vozes/tempos escritos preservados; nenhum fallback |
| Criação → Salvar → Biblioteca → Estudar → Partitura Livre | **Mesmo MusicXML**, mesmas alturas, figuras, acordes, vozes e ligaduras nos dois modos OSMD, mesmo que o enquadramento visual mude |
| Partituras derivadas PLS1 v1 e v2, projetos antigos e backup/restauração v4 | Ler/estudar ou informar erro sem perda, alterações implícitas ou regravação do `.plc` |
| Notas sustentadas com outras vozes, mãos cruzadas e acordes, `backup` por voz | Interpretação e sincronismo preservados no OSMD, sem duplicar ataques nem ocultar notas |
| Falha forçada de `osmd.load`, `osmd.render`, `StudyProfile.compare`, viewport e WebView | **Janela detalhada** e um incidente local por tentativa; nenhuma invocação dos desenhos antigos |
| Falha durante geração antes de “pronta” | Projeto MIDI preservado; derivada não promovida indevidamente nem adicionada silenciosamente à Biblioteca |
| Retry, volta, troca de música, rotação, sair/voltar e callbacks atrasados | Sem diálogo repetido, pauta velha, UI bloqueada permanentemente, corrida ou sessão de estudo inventada |
| Sem rede e sem consentimento para exportar | Nenhum dado transmitido; diagnóstico local sem XML/MIDI/título; copiar/exportar só por ação do usuário |
| Variedade telefone/tablet e baixo espaço/memória | Carregamento neutro, erro recuperável; sem crash ou fallback. Teste físico separado da CI |
| Análise estática do pacote e testes de regressão | **Zero** referência executável a `TemporalNotation.render`, fallback `PianoView`, selector/asset legado no app; testes da suíte atualizados sem mascarar erros |

### Definição operacional de “100%”

A meta significa **todas as partituras efetivamente aceitas como geradas e estudáveis dentro das capacidades documentadas passam no mesmo contrato OSMD de Estudar**, sem exceção silenciosa. A CI exige **100% de aprovação de uma matriz crescente de fixtures**; **qualquer** falha de renderização ou perda musical bloqueia a entrega. Como não é tecnicamente possível prometer comportamento infalível do Android/WebView em cada aparelho, esses erros de ambiente são mostrados ao usuário, registrados e recuperáveis, nunca disfarçados com renderizador diferente.

## 5. Status e próxima ação

Esta entrega **apenas documenta** diagnóstico e plano, sem alterar Java, JavaScript, testes, workflows ou modelos de dados. Não rodou testes, não gerou APK, não executou suíte completa, não iniciou emulador, não alterou `main`. Próxima ação, quando solicitada a implementação, é começar pela etapa 4-A, reproduzir falha com MusicXML gerado e travar os testes de equivalência; seguir sequencialmente até o critério de engine único e compatibilidade integral.

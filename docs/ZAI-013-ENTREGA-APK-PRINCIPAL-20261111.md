# ZAI-013 — Entrega da prévia de partitura: pauta final, zoom no topo e APK 0.9.74

**Data:** 11/11/2026. **Origem:** proposta visual aprovada pelo mantenedor após captura da prévia, em que a última linha da pauta ficava encoberta pelos controles de zoom. **Estado:** funcionalidade integrada à `zai/lab`, APK principal 0.9.74/code83 assinado e arquivado no GitHub; `main`, repositório histórico privado, tag/Release e Play Store intocados. **Teste físico da nova versão pendente.**

## Implementação

- **Cabeçalho:** botão de voltar com superfície arredondada, título da composição preservado.
- **Barra superior própria:** chip de andamento `NNN bpm` (com informação acessível sobre estimativa/120 BPM provisório), `A−`, `A+` e `Ajustar à largura`. A barra fica **fora do cartão de partitura** e possui rolagem apenas **horizontal** quando houver pouquíssima largura/fonte ampliada.
- **Visualizador:** substituição do WebView com altura fixa `heightPixels*0.65f` por `NotationView` que ocupa a **altura restante da tela**, dentro de cartão próprio. O OSMD `creation-page` mantém scroll vertical do próprio conteúdo e dispõe de 64 CSS px de margem inferior após a última pauta, permitindo revelar o último sistema integralmente em diferentes zooms. Não altera o OSMD Estudar/Descobrir/Free.
- **Rodapé:** informação resumida de observações MIDI e ações fixas `Voltar à composição` (secundário) e `Salvar partitura` (primário), com cantos arredondados, contraste, sombra, feedback ripple e área mínima de toque 48dp. Condição `Adicionar à Biblioteca` após salvar e mensagens acessíveis preservadas. Fluxos de prévia antiga, regeneração e compatibilidade com Estudar mantidos.
- **Música e dados:** os mesmos `.plc` MIDI e `.pls` derivados, geração MusicXML, figuras simples, BPM estimado, propriedade por perfil, backups e armazenamento permanecem **inalterados**. O zoom continua entre 0,5 e 2,0, sem modificar duração musical.

Planejamento: [ZAI-013 — escopo, causas e critérios](ZAI-013-PREVIA-PAUTA-FINAL-ZOOM-TOPO-20261111.md). PR funcional: [#150](https://github.com/atshimura/Piano-livre-aberto/pull/150), merge `dbadd040a8c61b1eae9dc794cfd359fa01b80e70`, **14 verificações SUCCESS** no HEAD final `7a535316650ce8480805692a5d4be50b780a2fee`.

## Testes automatizados — evidências reais

- [CI #43122465329](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43122465329): **SUCCESS** para contratos Python, auditoria pública, compilação Android debug isolada `br.org.pianolivre.opensource.zai`, browser OSMD real e GUI Android.
- **Browser Chromium/OSMD** (testes `CreationScoreLastStaffBrowserTest.js`): **9/9 cenários PASS** — viewports 950×240, 800×170 e 1200×310 CSS px, com zoom de 70%, 100% e 150%. Em todos: 192 cabeças de nota preservadas, nenhuma rolagem horizontal, última linha alcançada ao descer ao final e distância positiva da borda inferior (85–105 CSS px). Regressão anterior `CreationResponsiveLayoutBrowserTest.js` passou.
- **Emulador Android API 29** (teste `emulator_zai013_score_preview.py`): cria uma tomada MIDI sintética no fluxo debug já existente, salva a composição e abre a prévia real; mede via UiAutomator posições do chip BPM, zoom, WebView e botões inferiores. **`ZAI013_ANDROID_SCORE_TOOLBAR_PASS viewport_reserved=true zoom_restores=true`** após clicar `A+` e `Ajustar à largura`. A regressão foi corrigida no próprio teste para distinguir o WebView da prévia do visualizador da tela subjacente. Capturas e medidas anexadas ao Actions.
- [Build principal #43123078910](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43123078910): **SUCCESS**. Rodou auditoria, contratos, regressões do motor em JVM e gerou APK release com identidade principal e Secrets permanentes.
- [Publicação de download #43123386258](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43123386258): **SUCCESS**. Recuperou **o mesmo artefato assinado**, conferiu SHA-256, certificado, versão, pacote, ausência de debug e de permissão INTERNET, publicou em `zai/downloads` e verificou novamente o hash no Git remoto (commit `8e7afb5425c7850df03f6e23f6a8d89262ebde58`).
- Workflows técnicos documentados nos PRs [#151](https://github.com/atshimura/Piano-livre-aberto/pull/151) (build assinado) e [#152](https://github.com/atshimura/Piano-livre-aberto/pull/152) (distribuição), destinados exclusivamente à `zai/lab`.

## APK instalável

- Versão **0.9.74-open-beta**, `versionCode=83`, pacote `br.org.pianolivre.opensource`, tamanho **804.272 bytes**.
- Assinatura permanente verificada, certificado SHA-256 público `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`; não depurável e sem permissão INTERNET.
- **SHA-256:** `865d13387644d8ebf5d35e2a989b549327570b727c8400832b1bd7d3a5adb7d5`.
- **Link fixado ao commit GitHub:** https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/8e7afb5425c7850df03f6e23f6a8d89262ebde58/apks/principal/PianoLivre-Principal-0.9.74-open-beta-assinado.apk
- **Página do APK:** https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.74-open-beta-assinado.apk
- **Comprovante:** https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.74-open-beta-SHA256.txt

## Limitações e próximos passos

1. **Teste físico ainda não realizado:** atualizar **por cima** da 0.9.73 no Realme 7 5G e/ou Lenovo Tab M9, sem desinstalar e após backup. Conferir preservação de projetos, gravações e partituras.
2. Em orientação paisagem, abrir composição longa, entrar na prévia, rolar até a **última pauta**, confirmar visualização completa **sem controles sobrepostos**. Repetir após `A+`, `A−` e `Ajustar à largura`; verificar o topo junto ao BPM.
3. Conferir `Salvar partitura`, `Voltar à composição`, `Adicionar à Biblioteca` e leitura em fonte ampliada. Testes em emulador e Chromium não equivalem à validação em aparelhos reais e não provam cobertura universal.
4. Nenhuma integração à `main`, publicação oficial de Release/Play Store ou alteração de chave está incluída nesta entrega.

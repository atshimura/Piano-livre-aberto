# ZAI-014 — Entrega da barra única de prévia e APK principal 0.9.75

**Data (Brasil):** 11/11/2026. **Status:** funcionalidade integrada a `zai/lab`, APK 0.9.75/code84 compilado com assinatura permanente e disponibilizado em `zai/downloads`. `main` e repositório histórico privado inalterados; **não** há tag/Release/Play Store. Validação física da nova tela pendente.

## Alteração aprovada

- **Uma só barra fixa de 56dp:** Voltar (esquerda), título da composição, andamento BPM, zoom `A−`/`A+`, ajuste à largura `↔`, ícone de informações MIDI `ⓘ` e ação primária (direita). Supressão das duas faixas adicionais de zoom e rodapé de observações/salvamento da ZAI-013.
- **Partitura em todo o espaço restante:** área musical com `LayoutParams(-1,0,1)`, única rolagem vertical interna do OSMD e margem de segurança inferior de 64px CSS preservada. Última pauta pode ser exibida totalmente sem sobreposição dos botões.
- **Botões e responsividade:** estilo coeso azul-marinho/azul, ripple, contraste e 48dp de alvo tátil, título truncável com descrição acessível. Em largura reduzida as ferramentas rolam **horizontalmente**, sem ocultar os botões de Voltar e Salvar.
- **Fluxo único da ação primária:** antes do salvamento, `Salvar`; após salvar, `+ Biblioteca` (adiciona a derivação por referência sem copiar); já vinculada, `✓ Biblioteca` (indicador sem ação). O ícone `ⓘ` apresenta as informações MIDI em caixa acessível, incluindo avisos e preservação do áudio/MIDI fonte.
- **Compatibilidade e segurança:** sem mudanças em `.plc`, `.pls`, modelos MusicXML, geração rítmica, dados/perfis/backup, exclusão ou licença. Confirmação de compatibilidade do MusicXML com Estudar antes do salvamento, conforme fluxo anterior.

Planejamento e decisões em [ZAI-014 — Barra única](ZAI-014-PREVIA-BARRA-UNICA-20261111.md). Código: [PR #154](https://github.com/atshimura/Piano-livre-aberto/pull/154) integrado à `zai/lab` no commit `03450355b8eec5a5d639d33e3f44bf374e6827a2`.

## Testes executados

- **PR funcional #154:** **16/16 checks aprovados**, sem falhas, HEAD `c6fd4b5c540b58bd9bab2318d1dbbcbcd9b6ee45`.
- **CI dirigida [#43140996949](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43140996949): SUCCESS.** Auditoria de código público, contratos Python (inclusive `ZAI014_UNIFIED_TOOLBAR_CONTRACT_PASS checks=45`), Android debug isolado e renderização Chromium/OSMD.
- **OSMD real em Chromium:** 9 cenários com três alturas/larguras de viewport e zoom de 70%, 100% e 150%; última pauta alcançável e sem recorte, notas preservadas, margem inferior positiva e sem pan horizontal. Sem mudanças na função de geração de partitura.
- **Android GUI em emulador API 29:** teste com composição MIDI **sintética** percorreu gravação, prévia, ícone de informações, A+, ajuste à largura e **Salvar → + Biblioteca**, com `ZAI014_ANDROID_UNIFIED_BAR_PASS score_full_height=true info_dialog=true zoom=true save_transition=true`. Capturas/medições preservadas no artefato [ZAI014 Android GUI](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43140996949), e nenhum dado particular de alunos foi carregado.
- **Build release [#43141666241](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43141666241): SUCCESS.** Contratos, regressões JVM do motor, compilação Gradle, auditoria do release, certificado SHA-256 permanente, package/version, ausência de INTERNET e debug, assinatura por Secrets existentes.
- **Publicação [#43142105798](https://github.com/atshimura/Piano-livre-aberto/actions/runs/43142105798): SUCCESS.** Recuperou o APK original do build, **sem recompilação**, conferiu SHA-256, versão, certificado, permissões e integridade após push em `zai/downloads` (commit `528e13a3cc0fb4b8af3077da26c38f48480d1e42`). PRs técnicos [#155](https://github.com/atshimura/Piano-livre-aberto/pull/155) e [#156](https://github.com/atshimura/Piano-livre-aberto/pull/156).

## APK de atualização

- **Versão:** 0.9.75-open-beta; **versionCode:** 84; **pacote:** `br.org.pianolivre.opensource`; **tamanho:** 804.376 bytes.
- **SHA-256:** `54647c1a708939155999178fb2423a4a2b736f85afe8a89cc1d25ed68df82135`.
- **Certificado permanente (SHA-256):** `7bf1e9fa2d6cf1468463a2990e0a0699acc7c5efac45a77070c4d084e7798290`.
- **Download fixo por commit:** https://raw.githubusercontent.com/atshimura/Piano-livre-aberto/528e13a3cc0fb4b8af3077da26c38f48480d1e42/apks/principal/PianoLivre-Principal-0.9.75-open-beta-assinado.apk
- **Página do APK:** https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.75-open-beta-assinado.apk
- **Comprovante:** https://github.com/atshimura/Piano-livre-aberto/blob/zai/downloads/apks/principal/PianoLivre-Principal-0.9.75-open-beta-SHA256.txt

## Testes físicos pendentes e próximos passos

1. Fazer backup e **instalar sobre** a 0.9.74, sem desinstalar. Conferir projetos, perfis, gravações, biblioteca e partituras.
2. No **Realme 7 5G** e **Lenovo Tab M9**, em paisagem, conferir que a barra única fica legível e tocável, o título aparece quando há largura e o botão Salvar/Adicionar à Biblioteca permanece visível à direita.
3. Usar A−/A+/↔, abrir informações MIDI, rolar até a **última pauta** (sem cortes/controles sobrepostos), salvar e depois adicionar à Biblioteca; conferir modo de acessibilidade/fontes ampliadas se disponível.
4. Testes no emulador e Chromium **não equivalem** à validação física nesses aparelhos; não há garantia comprovada para todos os tamanhos de tela. Não alterar `main` ou publicar Release/Play Store sem decisão separada.

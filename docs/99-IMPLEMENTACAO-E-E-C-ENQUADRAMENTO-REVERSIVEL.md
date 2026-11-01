# Implementação E — Bloco E-C (E4): enquadramento automático reversível do Descobrir

**Status:** E4 implementada, validada com testes JVM, Chromium e Android de interface focada na branch funcional; **não integrada à main e sem publicação de APK**.  
**Data:** 01/11/2026. **Issue:** [#89](https://github.com/atshimura/Piano-livre-aberto/issues/89).  
**PR funcional único:** [#91](https://github.com/atshimura/Piano-livre-aberto/pull/91) (draft). **Anterior:** [E-A](97-IMPLEMENTACAO-E-E-A-BASELINE.md) e [E-B](98-IMPLEMENTACAO-E-E-B-PAUTA-ALINHADA.md).  
**Próximo bloco:** **E-D — Validação e Entrega (E5+E6)**, com gates do SHA final, revisão, integração e APK segundo a política do repositório.

## Mudança aprovada e efetivamente codificada

A pauta padrão, ampliada no E2, permanece o tamanho preferencial. Quando as notas exibidas exigem mais espaço, a câmera usa temporariamente um dos dois enquadramentos mais amplos já existentes. **Uma nova nota/acorde/trecho que caiba no padrão restaura a pauta maior automaticamente**. **Soltar notas não elimina o último resultado nem força um enquadramento que o cortaria**.

O cálculo considera o **conjunto musical atualmente desenhado**, jamais apenas a última nota MIDI ou o histórico dos registros já substituídos. Se uma nota extrema continuar visível junto a uma nota comum, usa o enquadramento da extrema. Para extremos do piano físico de 88 teclas, usar faixa 1; para valores MIDI fora desse alcance, usar faixa 2. Não há botão de zoom e nenhuma nova permissão, dependência, rede, telemetria, migração, mudança musical ou alteração na barra/painel.

### Arquivos de produção

- `PianoLivreAndroid/app/src/main/java/br/org/pianolivre/DiscoveryFraming.java` (novo): função pura que escolhe o menor nível que engloba todas as alturas exibidas. Nível 0: MIDI 36–84; 1: MIDI 21–108; 2: MIDI 0–127 fora da faixa do piano. Lista vazia implica câmera normal.
- `PianoLivreAndroid/app/src/main/java/br/org/pianolivre/DiscoveryStaffView.java`: em `setNotes`, substituído o antigo `tier=Math.max(tier,...)`, que jamais diminuía ao longo da sessão, por `tier=DiscoveryFraming.tierFor(notes)`. O mesmo `revision` protege os callbacks e o SVG é renderizado fora da tela antes de substituir o anterior.
- **Não foi alterado** o conteúdo de `MusicDiscoverySession`, `MusicLabDiscoveryScreen`, layout horizontal, renderizador do Estudar, `discovery-notation.js` (câmeras já existentes usadas sem redesenhar o motor), biblioteca ou funções de Criação.

## Cenários e resultados medidos

### JVM — política da câmera

O novo teste `PianoLivreAndroid/tests/DiscoveryFramingTest.java` executou **56 asserções com sucesso**: notas comuns, acorde, escala, extremos da faixa padrão, Lá0+Dó8 simultâneos, MIDI 0+127, extrema junto de nota comum, retorno ao normal e 20 ciclos repetidos. Resultado `DISCOVERY_FRAMING_REVERSIBLE_PASS checks=56 range=0..127 repeated=20` no [build #41318276669](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276669), revisão funcional `a1c14d82ec3aca30491adb4c7c64ab820e61ae05`.

### Chromium/OSMD offline — renderização verdadeira

`DiscoveryEReversibleBrowserTest.js` usa o OSMD local em dois viewports e verifica, com **12 capturas** e medições SVG, a sequência normal → MIDI21+108 → mesmo resultado retido → normal restaurado → MIDI0+127 → normal restaurado. Executa **seis ciclos extra** por perfil, sem acumular offsets ou zoom. Confirmado pelo log `DISCOVERY_E4_BROWSER_ALL_PASS profiles=2 captures=12 zero_clip=true`. Artefato `implementacao-e-visual-ec` do mesmo [run de build](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276669).

| Perfil da pauta branca | Espaço entre linhas normal | Espaço no par Lá0+Dó8 | Retorno após nova nota |
| --- | ---: | ---: | ---: |
| Telefone 625×356 CSS px | **11,972 px** | 6,667 px | **11,972 px** |
| Tablet 945×596 CSS px | **20,423 px** | 11,373 px | **20,423 px** |

Para MIDI0+127, o espaço entre linhas foi de 4,857 px (telefone) e 8,286 px (tablet): resultado **tecnicamente completo, mas com leitura naturalmente pequena**, pois ultrapassa o teclado de 88 teclas. Cabeças, linhas suplementares e claves foram examinadas nas imagens, sem corte visível nos estados avaliados. Sem extrapolar isso a todos os dispositivos.

### Android — tela real e MIDI sintético

O teste de interface `DiscoveryLayoutSelfTest.java` agora possui **11 estágios por perfil**, adicionando explicitamente um quadro de extremos retidos após Note Off. `emulator_discovery_layout.py` preserva **22 capturas (11 por perfil)** e `piano-discovery-layout.json`. No [workflow Android #41318276483](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276483), `classify`, `debug-apk` e `ui-fast` passaram na revisão funcional indicada. Artefato `PianoLivre-ui-fast-65ca4cd06534642b629dacd04f0407afcdafb175` (checkout sintético de PR).

| Estado | Celular (625×356 CSS px) | Tablet (945×596 CSS px) |
| --- | --- | --- |
| Nota comum no padrão | tier 0, escala **1,1972** | tier 0, escala **2,0423** |
| MIDI0 / MIDI127 | tier 2, escala 0,4857 | tier 2, escala 0,8286 |
| Lá0+Dó8 juntos | tier 1, escala **0,6667** | tier 1, escala **1,1373** |
| Par extremo após soltar | **tier 1 mantido** | **tier 1 mantido** |
| Nova nota comum Mi4 | **tier 0, escala 1,1972** | **tier 0, escala 2,0423** |

As imagens Android da tela completa do par extremo, retenção e restauração foram abertas e comparadas; **barra superior, painel de informações e proporção entre áreas permaneceram intactos**. O painel informou corretamente `Cifras: A0 · C8` para o par e `MI - E4` após o retorno; não há nova funcionalidade de reconhecimento.

**Resultado CI:** [build #41318276669](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276669) **success**; [Android #41318276483](https://github.com/atshimura/Piano-livre-aberto/actions/runs/41318276483), check `ui-fast` **success**. O teste completo `emulator-suite` desse run tem resultado próprio e pode ser cancelado por atualização documental; **não contar isso como gate integral aprovado**. O bloco E-D exige aprovação de todos os checks obrigatórios no SHA **final** do código e pós-merge. Verificar o estado efetivo no GitHub.

## Testes e fixtures adicionados

- `DiscoveryFramingTest.java` — cálculo puro da câmera, inclusive 20 ciclos.
- `DiscoveryEReversibleBrowserTest.js` — renderização de extremos juntos, retenção, restauração e 12 imagens, mais loops.
- `DiscoveryNotationFixtures.java` — novo MusicXML com MIDI0+127 no mesmo acorde (sem alterar entradas antigas).
- `DiscoveryNotationBrowserTest.js` — renderiza o par MIDI0+127 na câmera 2, com contagem correta.
- `DiscoveryLayoutSelfTest.java` — estados `extremes-together`, `extremes-retained`, `common-after-extreme`; espera tiers 1/1/0; níveis 2 apenas para MIDI0/127.
- `emulator_discovery_layout.py` — 11 estágios por perfil e reporte de 22 capturas.
- `.github/workflows/build.yml` — executa os testes JVM/Chromium e publica `implementacao-e-visual-ec`.

## Limites, riscos e continuidade

- **E-C não é a versão final da Implementação E**: E5/E6 (regressão completa do SHA final, revisão, merge autorizado e APK assinado permanente via Secrets) ficam para **E-D**. Este documento **não aprova nem publica Release**.
- Ensaios em Yamaha P-45, Realme 7 5G e Lenovo Tab M9 seguem **pendentes do usuário** e não são substituídos por MIDI sintético ou emulador.
- Não se introduziu timer manual nem mudança em metadados de sessões, arquivos de usuário ou reconhecimento MIDI. O componente somente recebe o conjunto de notas apresentado e escolhe a câmera adequada.
- O E-D deve garantir que nenhum commit documental posterior produza falsa alegação de checks aprovados no SHA final; o check `emulator-suite` da revisão E-C em execução/cancelado não é justificativa para integrar.
- **Próxima solicitação:** **“Implemente o Bloco E-D da Implementação E.”** Continuar a **mesma branch e PR #91**.
